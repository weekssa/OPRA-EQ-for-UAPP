import json
import tempfile
import unittest
from pathlib import Path

from autoeq_corpus_ingest import (
    load_manufacturer_index,
    measurement_context,
    refresh,
    resolve_manufacturer,
)
from catalog_pipeline import SourceHealth


SAMPLE_A = """Preamp: -6.0 dB
Filter 1: ON LSC Fc 105 Hz Gain 6.0 dB Q 0.70
Filter 2: ON PK Fc 2000 Hz Gain 2.0 dB Q 1.20
"""

SAMPLE_B = """Preamp: -4.0 dB
Filter 1: ON PK Fc 100 Hz Gain 2.0 dB Q 0.70
Filter 2: ON PK Fc 3000 Hz Gain -3.0 dB Q 2.00
"""


def registry():
    return {
        "schema_version": 1,
        "registry_version": "test-corpus-1",
        "sources": [
            {
                "id": "autoeq",
                "kind": "measurement_derived",
                "name": "AutoEq",
                "scope": "test",
                "lifecycle": "active",
                "cadence": "daily",
                "parser": "autoeq-parametric-text",
                "parser_version": "3",
                "cursor_strategy": "repository commit and per-file hash",
                "redistribution": "structured-data-only",
                "attribution_required": True,
            }
        ],
    }


def empty_catalog():
    return {
        "schema_version": 1,
        "generated_at": "2026-08-29T00:00:00Z",
        "source_registry_version": "test-corpus-1",
        "headphone_aliases": [],
        "profiles": [],
    }


class AutoEqCorpusIngestTest(unittest.TestCase):
    def setUp(self):
        self.temp = tempfile.TemporaryDirectory()
        self.root = Path(self.temp.name)
        (self.root / "dbtools").mkdir(parents=True)
        (self.root / "dbtools" / "manufacturers.tsv").write_text(
            "HIFIMAN\tHiFiMAN Electronics\nSennheiser\tMassdrop x Sennheiser\n",
            encoding="utf-8",
        )

    def tearDown(self):
        self.temp.cleanup()

    def _preset(self, relative: str, content: str) -> None:
        path = self.root / relative
        path.parent.mkdir(parents=True, exist_ok=True)
        path.write_text(content, encoding="utf-8")

    def test_matches_autoeq_manufacturer_aliases(self):
        index = load_manufacturer_index(self.root / "dbtools" / "manufacturers.tsv")
        manufacturer, model, matched = resolve_manufacturer("Massdrop x Sennheiser HD 6XX", index)
        self.assertEqual("Sennheiser", manufacturer)
        self.assertEqual("HD 6XX", model)
        self.assertEqual("Massdrop x Sennheiser", matched)

    def test_generic_form_factor_is_not_canonical_context(self):
        self.assertIsNone(
            measurement_context(
                Path("results/oratory1990/over-ear/HIFIMAN Edition XS/HIFIMAN Edition XS ParametricEQ.txt")
            )
        )
        self.assertEqual(
            "GRAS 43AG-7 over-ear",
            measurement_context(
                Path("results/crinacle/GRAS 43AG-7 over-ear/Sennheiser HD 650/Sennheiser HD 650 ParametricEQ.txt")
            ),
        )

    def test_ingests_multiple_measurement_contexts_without_colliding(self):
        self._preset(
            "results/oratory1990/over-ear/HIFIMAN Edition XS/HIFIMAN Edition XS ParametricEQ.txt",
            SAMPLE_A,
        )
        self._preset(
            "results/crinacle/GRAS 43AG-7 over-ear/Sennheiser HD 650/Sennheiser HD 650 ParametricEQ.txt",
            SAMPLE_A,
        )
        self._preset(
            "results/crinacle/Bruel & Kjaer 5128 over-ear/Sennheiser HD 650/Sennheiser HD 650 ParametricEQ.txt",
            SAMPLE_B,
        )
        health = {"autoeq": SourceHealth(source_id="autoeq", lifecycle="active", parser_version="3")}
        catalog, updated_health, report = refresh(
            autoeq_root=self.root,
            catalog=empty_catalog(),
            registry=registry(),
            health=health,
            upstream_commit="abc123",
            now_iso="2026-08-30T00:00:00Z",
            now_epoch=1788048000,
            max_compact_catalog_bytes=10_000_000,
        )
        self.assertEqual(3, len(catalog["profiles"]))
        self.assertEqual(3, report["candidate_count"])
        self.assertEqual(3, report["merge_outcomes"]["new_profile"])
        self.assertEqual("abc123", updated_health["autoeq"].cursor)
        hd650 = [p for p in catalog["profiles"] if p["headphone"]["model"] == "HD 650"]
        self.assertEqual(2, len(hd650))
        self.assertNotEqual(hd650[0]["canonical_profile_id"], hd650[1]["canonical_profile_id"])

    def test_unchanged_commit_does_not_rewrite_catalog(self):
        health = {
            "autoeq": SourceHealth(
                source_id="autoeq",
                lifecycle="active",
                parser_version="3",
                cursor="same",
                last_content_fingerprint="fingerprint",
            )
        }
        before = empty_catalog()
        catalog, updated_health, report = refresh(
            autoeq_root=self.root,
            catalog=before,
            registry=registry(),
            health=health,
            upstream_commit="same",
            now_iso="2026-08-30T00:00:00Z",
            now_epoch=1788048000,
            max_compact_catalog_bytes=10_000_000,
        )
        self.assertEqual(before, catalog)
        self.assertEqual("unchanged_cursor", report["outcome"])
        self.assertEqual("same", updated_health["autoeq"].cursor)

    def test_unknown_manufacturer_is_bounded_and_reported(self):
        self._preset(
            "results/example/over-ear/UnknownCo Model/UnknownCo Model ParametricEQ.txt",
            SAMPLE_A,
        )
        health = {"autoeq": SourceHealth(source_id="autoeq", lifecycle="active", parser_version="3")}
        catalog, _updated_health, report = refresh(
            autoeq_root=self.root,
            catalog=empty_catalog(),
            registry=registry(),
            health=health,
            upstream_commit="unknown-one",
            now_iso="2026-08-30T00:00:00Z",
            now_epoch=1788048000,
            max_compact_catalog_bytes=10_000_000,
        )
        self.assertEqual(0, len(catalog["profiles"]))
        self.assertEqual(1, report["unknown_manufacturer_count"])


if __name__ == "__main__":
    unittest.main()
