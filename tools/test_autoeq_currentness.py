import unittest

from autoeq_currentness import refresh
from autoeq_ingest import build_candidate, parse_parametric_eq
from catalog_pipeline import SourceHealth


OLD = """Preamp: -4.0 dB
Filter 1: ON PK Fc 100 Hz Gain 3.0 dB Q 0.70
Filter 2: ON PK Fc 3000 Hz Gain -2.0 dB Q 1.20
"""
NEW = """Preamp: -4.2 dB
Filter 1: ON PK Fc 100 Hz Gain 3.2 dB Q 0.70
Filter 2: ON PK Fc 3000 Hz Gain -2.0 dB Q 1.20
"""


def registry():
    return {
        "registry_version": "test",
        "sources": [
            {
                "id": "autoeq",
                "lifecycle": "active",
                "parser_version": "2",
            }
        ],
    }


def candidate(text, sha):
    item = build_candidate(
        parse_parametric_eq(text),
        manufacturer="HIFIMAN",
        model="Edition XS",
        measurement_source="oratory1990",
        target=None,
        source_url="https://example.com/autoeq",
        source_record_id="results/oratory1990/over-ear/HIFIMAN Edition XS/HIFIMAN Edition XS ParametricEQ.txt",
        source_version=f"AutoEq blob {sha}",
        discovered_at_epoch_seconds=1,
    )
    item.pop("publication_eligible", None)
    return item


class AutoEqCurrentnessTest(unittest.TestCase):
    def test_unchanged_cursor_does_not_rewrite_catalog(self):
        original = candidate(OLD, "same-sha")
        catalog = {
            "schema_version": 1,
            "generated_at": "2026-08-29T00:00:00Z",
            "source_registry_version": "test",
            "profiles": [original],
        }
        fingerprint = original["revisions"][0]["acoustic_fingerprint"]
        health = {
            "autoeq": SourceHealth(
                source_id="autoeq",
                lifecycle="active",
                cursor="same-sha",
                parser_version="2",
                last_content_fingerprint=fingerprint,
            )
        }

        updated, updated_health, outcome = refresh(
            catalog=catalog,
            registry=registry(),
            health=health,
            upstream_sha="same-sha",
            upstream_text=OLD,
            now_iso="2026-08-30T00:00:00Z",
            now_epoch=2,
        )

        self.assertEqual("unchanged_cursor", outcome)
        self.assertEqual(catalog, updated)
        self.assertEqual("same-sha", updated_health["autoeq"].cursor)
        self.assertEqual("2026-08-30T00:00:00Z", updated_health["autoeq"].last_successful_scan_at)

    def test_material_change_becomes_new_immutable_revision(self):
        original = candidate(OLD, "old-sha")
        catalog = {
            "schema_version": 1,
            "generated_at": "2026-08-29T00:00:00Z",
            "source_registry_version": "test",
            "profiles": [original],
        }
        health = {
            "autoeq": SourceHealth(
                source_id="autoeq",
                lifecycle="active",
                cursor="old-sha",
                parser_version="2",
                last_content_fingerprint=original["revisions"][0]["acoustic_fingerprint"],
            )
        }

        updated, updated_health, outcome = refresh(
            catalog=catalog,
            registry=registry(),
            health=health,
            upstream_sha="new-sha",
            upstream_text=NEW,
            now_iso="2026-08-30T00:00:00Z",
            now_epoch=2,
        )

        self.assertEqual("new_revision", outcome)
        revisions = updated["profiles"][0]["revisions"]
        self.assertEqual(2, len(revisions))
        self.assertEqual(1, sum(1 for revision in revisions if revision["is_latest"]))
        self.assertEqual("new-sha", updated_health["autoeq"].cursor)
        self.assertNotEqual(revisions[0]["acoustic_fingerprint"], revisions[1]["acoustic_fingerprint"])


if __name__ == "__main__":
    unittest.main()
