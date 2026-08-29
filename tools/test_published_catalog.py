import json
import unittest
from pathlib import Path

from acoustic_fingerprint import acoustic_fingerprint


CATALOG = Path(__file__).resolve().parents[1] / "catalog" / "catalog.json"


class PublishedCatalogTest(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.snapshot = json.loads(CATALOG.read_text(encoding="utf-8"))

    def test_android_snapshot_metadata_is_present(self):
        self.assertGreaterEqual(self.snapshot["schema_version"], 1)
        self.assertTrue(self.snapshot["generated_at"])
        self.assertTrue(self.snapshot["source_registry_version"])

    def test_canary_is_non_empty_and_multisource(self):
        profiles = self.snapshot["profiles"]
        self.assertGreaterEqual(len(profiles), 2)
        source_kinds = {
            source["source_kind"]
            for profile in profiles
            for revision in profile["revisions"]
            for source in revision["source_references"]
        }
        self.assertIn("structured_catalog", source_kinds)
        self.assertIn("measurement_derived", source_kinds)

    def test_edition_xs_contains_authored_and_measurement_derived_tunings(self):
        profiles = [
            profile
            for profile in self.snapshot["profiles"]
            if profile["headphone"]["manufacturer"] == "HIFIMAN"
            and profile["headphone"]["model"] == "Edition XS"
        ]
        self.assertEqual({"oratory1990", "AutoEq"}, {profile["creator"] for profile in profiles})
        autoeq = next(profile for profile in profiles if profile["creator"] == "AutoEq")
        refs = autoeq["revisions"][0]["source_references"]
        self.assertEqual("autoeq", next(ref for ref in refs if ref["is_primary"])["source_id"])
        self.assertTrue(any(ref["provenance_tier"] == "mirror" for ref in refs))

    def test_published_fingerprints_match_android_algorithm(self):
        for profile in self.snapshot["profiles"]:
            for revision in profile["revisions"]:
                self.assertEqual(
                    revision["acoustic_fingerprint"],
                    acoustic_fingerprint(revision["preamp_gain_db"], revision["filters"]),
                    profile["canonical_profile_id"],
                )

    def test_active_sources_are_reported(self):
        statuses = {status["source_id"]: status for status in self.snapshot["sources"]}
        self.assertEqual("active", statuses["opra"]["lifecycle"])
        self.assertEqual("active", statuses["autoeq"]["lifecycle"])


if __name__ == "__main__":
    unittest.main()
