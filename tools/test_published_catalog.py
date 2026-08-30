import json
import unittest
from pathlib import Path

from acoustic_fingerprint import acoustic_fingerprint
from headphone_identity_decisions import normalize


ROOT = Path(__file__).resolve().parents[1]
CATALOG = ROOT / "catalog" / "catalog.json"
IDENTITY_DECISIONS = ROOT / "config" / "headphone_identity_decisions.json"
EDITION_XS_AUTOEQ_ORATORY_CANONICAL_ID = "autoeq-09f4d8d5d5288ccfdf6ddeeb"


class PublishedCatalogTest(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.snapshot = json.loads(CATALOG.read_text(encoding="utf-8"))
        cls.identity_decisions = json.loads(IDENTITY_DECISIONS.read_text(encoding="utf-8"))

    def _edition_xs_autoeq_oratory(self):
        profile = next(
            profile
            for profile in self.snapshot["profiles"]
            if profile["canonical_profile_id"] == EDITION_XS_AUTOEQ_ORATORY_CANONICAL_ID
        )
        self.assertEqual("HIFIMAN", profile["headphone"]["manufacturer"])
        self.assertEqual("Edition XS", profile["headphone"]["model"])
        self.assertEqual("AutoEq", profile["creator"])
        self.assertIn("oratory1990", profile["tuning_label"])
        return profile

    def test_android_snapshot_metadata_is_present(self):
        self.assertGreaterEqual(self.snapshot["schema_version"], 1)
        self.assertTrue(self.snapshot["generated_at"])
        self.assertTrue(self.snapshot["source_registry_version"])

    def test_reviewed_headphone_aliases_are_in_android_snapshot(self):
        published = {
            (normalize(group["manufacturer"]), normalize(group["canonical_model"])): group
            for group in self.snapshot.get("headphone_aliases") or []
        }
        self.assertTrue(published)

        for decision in self.identity_decisions.get("aliases") or []:
            key = (normalize(decision["manufacturer"]), normalize(decision["canonical_model"]))
            self.assertIn(key, published, decision)
            published_aliases = {normalize(value) for value in published[key].get("aliases") or []}
            for alias in decision.get("aliases") or []:
                self.assertIn(normalize(alias), published_aliases, decision)

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
        creators = {profile["creator"] for profile in profiles}
        self.assertIn("oratory1990", creators)
        self.assertIn("AutoEq", creators)

        autoeq = self._edition_xs_autoeq_oratory()
        latest = next(revision for revision in autoeq["revisions"] if revision["is_latest"])
        latest_refs = latest["source_references"]
        self.assertEqual("autoeq", next(ref for ref in latest_refs if ref["is_primary"])["source_id"])

        # OPRA mirrors a specific AutoEq acoustic revision. When upstream AutoEq changes,
        # the mirror must remain on the matching immutable revision rather than being
        # copied onto a newer, materially different tuning.
        mirrored_revisions = [
            revision
            for revision in autoeq["revisions"]
            if any(ref["provenance_tier"] == "mirror" for ref in revision["source_references"])
        ]
        self.assertTrue(mirrored_revisions)
        self.assertTrue(
            any(
                any(ref["source_id"] == "autoeq" for ref in revision["source_references"])
                for revision in mirrored_revisions
            )
        )

    def test_canary_contains_real_immutable_revision_history(self):
        autoeq = self._edition_xs_autoeq_oratory()
        revisions = autoeq["revisions"]
        self.assertGreaterEqual(len(revisions), 2)
        self.assertEqual(1, sum(1 for revision in revisions if revision["is_latest"]))
        latest = next(revision for revision in revisions if revision["is_latest"])
        previous = next(
            revision
            for revision in revisions
            if revision["source_version_label"]
            == "AutoEq commit 853360a1626b387e1d3d87f3f7ad8c7514d30839"
        )
        self.assertFalse(previous["is_latest"])
        self.assertEqual(1698572942, previous["source_updated_at_epoch_seconds"])
        self.assertNotEqual(previous["acoustic_fingerprint"], latest["acoustic_fingerprint"])

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