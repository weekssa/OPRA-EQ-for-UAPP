import json
import tempfile
import unittest
from pathlib import Path

from catalog_pipeline import (
    RegistryError,
    SourceHealth,
    acoustic_fingerprint,
    classify_candidate,
    publish_snapshot,
    reconcile_health,
    record_scan_failure,
    record_scan_success,
    validate_registry,
)


class CatalogPipelineTest(unittest.TestCase):
    def test_registry_validation_rejects_duplicate_source_ids(self):
        source = {
            "id": "same",
            "kind": "community",
            "name": "A",
            "scope": "test",
            "lifecycle": "reviewing",
            "cadence": "weekly",
            "parser": "x",
            "parser_version": "1",
            "cursor_strategy": "id",
            "redistribution": "link-only",
            "attribution_required": True,
        }
        errors = validate_registry({"schema_version": 1, "registry_version": "x", "sources": [source, source]})
        self.assertTrue(any("duplicate source id" in error for error in errors))

    def test_reconcile_preserves_cursor_but_updates_lifecycle_and_parser(self):
        registry = {
            "schema_version": 1,
            "registry_version": "x",
            "sources": [{
                "id": "source",
                "kind": "community",
                "name": "Source",
                "scope": "test",
                "lifecycle": "active",
                "cadence": "daily",
                "parser": "peq",
                "parser_version": "2",
                "cursor_strategy": "id",
                "redistribution": "link-only",
                "attribution_required": True,
            }],
        }
        current = {"source": SourceHealth(source_id="source", lifecycle="reviewing", cursor="abc", parser_version="1")}
        reconciled = reconcile_health(registry, current)["source"]
        self.assertEqual("active", reconciled.lifecycle)
        self.assertEqual("2", reconciled.parser_version)
        self.assertEqual("abc", reconciled.cursor)

    def test_scan_success_resets_failure_without_losing_state(self):
        failed = record_scan_failure(SourceHealth(source_id="a", lifecycle="active"), "boom", attempted_at="2026-01-01T00:00:00Z")
        self.assertEqual(1, failed.consecutive_failures)
        ok = record_scan_success(failed, cursor="42", content_fingerprint="hash", attempted_at="2026-01-02T00:00:00Z")
        self.assertEqual(0, ok.consecutive_failures)
        self.assertIsNone(ok.last_error)
        self.assertEqual("42", ok.cursor)
        self.assertEqual("hash", ok.last_content_fingerprint)

    def test_acoustic_fingerprint_ignores_filter_order_and_minor_precision(self):
        a = [
            {"type": "peak", "frequency_hz": 1000.0001, "gain_db": -2.0001, "q": 1.00001},
            {"type": "peak", "frequency_hz": 80, "gain_db": 3, "q": 0.7},
        ]
        b = list(reversed([
            {"type": "PEAK", "frequency_hz": 1000, "gain_db": -2, "q": 1},
            {"type": "peak", "frequency_hz": 80.0001, "gain_db": 3.0001, "q": 0.70001},
        ]))
        self.assertEqual(acoustic_fingerprint(-5.0001, a), acoustic_fingerprint(-5, b))

    def test_classification_distinguishes_duplicate_and_revision(self):
        self.assertEqual("new_candidate", classify_candidate([], "a"))
        self.assertEqual("duplicate", classify_candidate(["a"], "a"))
        self.assertEqual("new_revision", classify_candidate(["a"], "b"))

    def test_invalid_candidate_never_replaces_last_known_good(self):
        valid = {
            "schema_version": 1,
            "profiles": [{
                "canonical_profile_id": "p1",
                "revisions": [{"revision_id": "r1", "acoustic_fingerprint": "abc", "is_latest": True}],
            }],
        }
        invalid = {
            "schema_version": 1,
            "profiles": [{
                "canonical_profile_id": "p1",
                "revisions": [{"revision_id": "r2", "acoustic_fingerprint": "def", "is_latest": False}],
            }],
        }
        with tempfile.TemporaryDirectory() as temp:
            root = Path(temp)
            published = root / "published.json"
            good = root / "good.json"
            candidate = root / "candidate.json"
            candidate.write_text(json.dumps(valid), encoding="utf-8")
            publish_snapshot(candidate, published, good)
            before = good.read_text(encoding="utf-8")
            candidate.write_text(json.dumps(invalid), encoding="utf-8")
            with self.assertRaises(RegistryError):
                publish_snapshot(candidate, published, good)
            self.assertEqual(before, good.read_text(encoding="utf-8"))
            self.assertEqual(valid, json.loads(published.read_text(encoding="utf-8")))


if __name__ == "__main__":
    unittest.main()
