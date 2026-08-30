import unittest

from catalog_merge import merge_candidate


def revision(revision_id, fingerprint, frequency, *, latest=True, version=None, verification=None):
    result = {
        "revision_id": revision_id,
        "acoustic_fingerprint": fingerprint,
        "preamp_gain_db": -4.0,
        "filters": [{"type": "peak", "frequency_hz": frequency, "gain_db": 2.0, "q": 1.0, "slope": None}],
        "source_references": [{
            "source_id": "autoeq",
            "source_kind": "measurement_derived",
            "source_record_id": "result.txt",
            "source_vendor_id": "Maker",
            "source_product_id": "Model",
            "url": "https://example.com/result.txt",
            "creator": "AutoEq",
            "provenance_tier": "measurement_derived",
            "redistribution_policy": "structured-data-only",
            "published_at_epoch_seconds": None,
            "updated_at_epoch_seconds": None,
            "discovered_at_epoch_seconds": 10,
            "last_verified_at_epoch_seconds": 10,
            "is_primary": True,
        }],
        "source_version_label": version,
        "sound_impact_summary": None,
        "first_seen_at_epoch_seconds": 10,
        "source_updated_at_epoch_seconds": 10,
        "is_latest": latest,
    }
    if verification is not None:
        result["verification_status"] = verification
    return result


def profile(revisions):
    return {
        "canonical_profile_id": "autoeq-profile",
        "headphone": {"manufacturer": "Maker", "model": "Model", "variant": None, "pads_or_mode": None},
        "creator": "AutoEq",
        "target": {"name": None, "kind": "unknown"},
        "tuning_label": "AutoEq (measurement)",
        "revisions": revisions,
    }


def snapshot(revisions=None):
    return {
        "schema_version": 1,
        "generated_at": "2026-08-29T00:00:00Z",
        "source_registry_version": "old",
        "profiles": [profile(revisions)] if revisions is not None else [],
        "sources": [],
    }


class CatalogMergeTest(unittest.TestCase):
    def test_same_fingerprint_updates_metadata_without_new_revision(self):
        existing = revision("old-id", "same", 100.0, version="old")
        incoming = profile([revision("new-id", "same", 100.0, version="new")])
        incoming["revisions"][0]["source_references"][0]["last_verified_at_epoch_seconds"] = 20

        merged, outcome = merge_candidate(
            snapshot([existing]),
            incoming,
            generated_at="2026-08-29T01:00:00Z",
            source_registry_version="new-registry",
        )

        revisions = merged["profiles"][0]["revisions"]
        self.assertEqual("metadata_update", outcome)
        self.assertEqual(1, len(revisions))
        self.assertEqual("old-id", revisions[0]["revision_id"])
        self.assertEqual("new", revisions[0]["source_version_label"])
        self.assertEqual(20, revisions[0]["source_references"][0]["last_verified_at_epoch_seconds"])
        self.assertEqual("new-registry", merged["source_registry_version"])

    def test_same_fingerprint_promotes_unverified_to_verified_without_new_revision(self):
        existing = revision("unverified-id", "same", 100.0, verification="unverified")
        incoming = profile([revision("verified-id", "same", 100.0, verification="verified")])

        merged, outcome = merge_candidate(snapshot([existing]), incoming)

        revisions = merged["profiles"][0]["revisions"]
        self.assertEqual("metadata_update", outcome)
        self.assertEqual(1, len(revisions))
        self.assertEqual("unverified-id", revisions[0]["revision_id"])
        self.assertEqual("verified", revisions[0]["verification_status"])

    def test_same_fingerprint_unverified_sighting_does_not_demote_verified_revision(self):
        existing = revision("verified-id", "same", 100.0, verification="verified")
        incoming = profile([revision("unverified-id", "same", 100.0, verification="unverified")])

        merged, outcome = merge_candidate(snapshot([existing]), incoming)

        revisions = merged["profiles"][0]["revisions"]
        self.assertEqual("metadata_update", outcome)
        self.assertEqual(1, len(revisions))
        self.assertEqual("verified", revisions[0]["verification_status"])

    def test_same_fingerprint_updates_generated_safety_headroom_in_place(self):
        existing = revision("old-id", "same", 100.0)
        existing["preamp_gain_db"] = None
        existing["eq_library_safety_headroom_db"] = -3.0
        incoming_revision = revision("new-id", "same", 100.0)
        incoming_revision["preamp_gain_db"] = None
        incoming_revision["eq_library_safety_headroom_db"] = -4.5

        merged, outcome = merge_candidate(
            snapshot([existing]),
            profile([incoming_revision]),
        )

        revisions = merged["profiles"][0]["revisions"]
        self.assertEqual("metadata_update", outcome)
        self.assertEqual(1, len(revisions))
        self.assertEqual("old-id", revisions[0]["revision_id"])
        self.assertIsNone(revisions[0]["preamp_gain_db"])
        self.assertEqual(-4.5, revisions[0]["eq_library_safety_headroom_db"])

    def test_legacy_generated_preamp_revision_is_repaired_not_preserved_as_history(self):
        legacy = revision("legacy-bug", "legacy-fingerprint", 100.0)
        legacy["preamp_gain_db"] = -3.0
        incoming_revision = revision("correct-source", "correct-fingerprint", 100.0)
        incoming_revision["preamp_gain_db"] = None
        incoming_revision["eq_library_safety_headroom_db"] = -3.0

        merged, outcome = merge_candidate(
            snapshot([legacy]),
            profile([incoming_revision]),
        )

        revisions = merged["profiles"][0]["revisions"]
        self.assertEqual("history_repair", outcome)
        self.assertEqual(1, len(revisions))
        self.assertEqual("correct-source", revisions[0]["revision_id"])
        self.assertIsNone(revisions[0]["preamp_gain_db"])
        self.assertEqual(-3.0, revisions[0]["eq_library_safety_headroom_db"])
        self.assertTrue(revisions[0]["is_latest"])

    def test_legacy_generated_preamp_repair_requires_identical_source_reference(self):
        legacy = revision("legacy-bug", "legacy-fingerprint", 100.0)
        legacy["preamp_gain_db"] = -3.0
        incoming_revision = revision("correct-source", "correct-fingerprint", 100.0)
        incoming_revision["preamp_gain_db"] = None
        incoming_revision["eq_library_safety_headroom_db"] = -3.0
        incoming_revision["source_references"][0]["source_record_id"] = "different.txt"

        merged, outcome = merge_candidate(
            snapshot([legacy]),
            profile([incoming_revision]),
        )

        revisions = merged["profiles"][0]["revisions"]
        self.assertEqual("new_revision", outcome)
        self.assertEqual(2, len(revisions))
        self.assertEqual({"legacy-bug", "correct-source"}, {item["revision_id"] for item in revisions})

    def test_legacy_generated_preamp_repair_requires_exact_filters(self):
        legacy = revision("legacy-bug", "legacy-fingerprint", 100.0)
        legacy["preamp_gain_db"] = -3.0
        incoming_revision = revision("correct-source", "correct-fingerprint", 101.0)
        incoming_revision["preamp_gain_db"] = None
        incoming_revision["eq_library_safety_headroom_db"] = -3.0

        merged, outcome = merge_candidate(
            snapshot([legacy]),
            profile([incoming_revision]),
        )

        revisions = merged["profiles"][0]["revisions"]
        self.assertEqual("new_revision", outcome)
        self.assertEqual(2, len(revisions))

    def test_changed_fingerprint_creates_new_immutable_revision(self):
        existing = revision("rev-old", "old-fingerprint", 100.0, latest=True)
        incoming = profile([revision("rev-new", "new-fingerprint", 110.0, latest=True)])

        merged, outcome = merge_candidate(snapshot([existing]), incoming)

        revisions = merged["profiles"][0]["revisions"]
        self.assertEqual("new_revision", outcome)
        self.assertEqual(2, len(revisions))
        self.assertEqual("rev-new", revisions[0]["revision_id"])
        self.assertTrue(revisions[0]["is_latest"])
        self.assertFalse(revisions[1]["is_latest"])
        self.assertEqual("rev-old", revisions[1]["revision_id"])

    def test_distinct_profile_is_appended(self):
        candidate = profile([revision("rev-1", "fingerprint", 100.0)])

        merged, outcome = merge_candidate(snapshot(None), candidate)

        self.assertEqual("new_profile", outcome)
        self.assertEqual(1, len(merged["profiles"]))
        self.assertTrue(merged["profiles"][0]["revisions"][0]["is_latest"])

    def test_review_only_candidate_cannot_enter_published_catalog(self):
        candidate = profile([revision("rev-1", "fingerprint", 100.0)])
        candidate["publication_eligible"] = False

        with self.assertRaisesRegex(ValueError, "review-only"):
            merge_candidate(snapshot(None), candidate)

    def test_invalid_verification_status_is_rejected(self):
        candidate = profile([revision("rev-1", "fingerprint", 100.0, verification="maybe")])

        with self.assertRaisesRegex(ValueError, "verification_status"):
            merge_candidate(snapshot(None), candidate)


if __name__ == "__main__":
    unittest.main()
