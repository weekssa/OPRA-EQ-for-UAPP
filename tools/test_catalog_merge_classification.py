import unittest

from catalog_merge import merge_candidate


class CatalogMergeClassificationTest(unittest.TestCase):
    def snapshot(self, profiles=None):
        return {
            "schema_version": 1,
            "generated_at": "2026-08-30T00:00:00Z",
            "source_registry_version": "test",
            "profiles": list(profiles or []),
        }

    def revision(self, fingerprint="acoustic-a", revision_id="rev-a", source_id="source-a"):
        return {
            "revision_id": revision_id,
            "acoustic_fingerprint": fingerprint,
            "preamp_gain_db": -3.0,
            "filters": [
                {"type": "low_shelf", "frequency_hz": 100.0, "gain_db": 3.0, "q": 0.7},
            ],
            "source_references": [
                {
                    "source_id": source_id,
                    "source_kind": "community",
                    "source_record_id": revision_id,
                    "url": f"https://example.test/{revision_id}",
                },
            ],
            "verification_status": "unverified",
            "is_latest": True,
        }

    def general_candidate(self, purpose="effect", fingerprint="acoustic-a", revision_id="rev-a"):
        return {
            "canonical_profile_id": "general:creator:bass-boost",
            "scope": "general",
            "purpose": purpose,
            "headphone": None,
            "creator": "Creator",
            "target": {"name": None, "kind": "unknown"},
            "tuning_label": "Bass Boost",
            "revisions": [self.revision(fingerprint, revision_id)],
            "publication_eligible": True,
        }

    def legacy_headphone_profile(self):
        return {
            "canonical_profile_id": "legacy-headphone",
            "headphone": {"manufacturer": "Example", "model": "Headphone"},
            "creator": "Creator",
            "target": {"name": "Target", "kind": "explicit_target"},
            "tuning_label": "Neutral",
            "revisions": [self.revision()],
        }

    def test_new_general_effect_profile_preserves_explicit_classification(self):
        merged, outcome = merge_candidate(
            self.snapshot(),
            self.general_candidate(),
            generated_at="2026-08-31T00:00:00Z",
        )

        self.assertEqual("new_profile", outcome)
        profile = merged["profiles"][0]
        self.assertEqual("general", profile["scope"])
        self.assertEqual("effect", profile["purpose"])
        self.assertIsNone(profile["headphone"])

    def test_general_profile_accepts_new_revision_without_losing_classification(self):
        existing, _ = merge_candidate(
            self.snapshot(),
            self.general_candidate(),
            generated_at="2026-08-31T00:00:00Z",
        )
        merged, outcome = merge_candidate(
            existing,
            self.general_candidate(fingerprint="acoustic-b", revision_id="rev-b"),
            generated_at="2026-08-31T01:00:00Z",
        )

        self.assertEqual("new_revision", outcome)
        profile = merged["profiles"][0]
        self.assertEqual("general", profile["scope"])
        self.assertEqual("effect", profile["purpose"])
        self.assertEqual(2, len(profile["revisions"]))
        self.assertEqual(1, sum(1 for revision in profile["revisions"] if revision["is_latest"]))

    def test_same_canonical_id_cannot_change_effect_to_genre(self):
        existing, _ = merge_candidate(
            self.snapshot(),
            self.general_candidate(),
            generated_at="2026-08-31T00:00:00Z",
        )

        with self.assertRaisesRegex(ValueError, "classification cannot change"):
            merge_candidate(
                existing,
                self.general_candidate(purpose="genre"),
                generated_at="2026-08-31T01:00:00Z",
            )

    def test_legacy_headphone_profile_can_backfill_explicit_default_classification(self):
        existing = self.snapshot([self.legacy_headphone_profile()])
        candidate = self.legacy_headphone_profile()
        candidate["scope"] = "headphone"
        candidate["purpose"] = "correction_tuning"
        candidate["publication_eligible"] = True

        merged, outcome = merge_candidate(
            existing,
            candidate,
            generated_at="2026-08-31T00:00:00Z",
        )

        self.assertEqual("metadata_update", outcome)
        profile = merged["profiles"][0]
        self.assertEqual("headphone", profile["scope"])
        self.assertEqual("correction_tuning", profile["purpose"])

    def test_general_profile_rejects_legacy_unclassified_candidate_for_same_id(self):
        existing, _ = merge_candidate(
            self.snapshot(),
            self.general_candidate(),
            generated_at="2026-08-31T00:00:00Z",
        )
        candidate = self.general_candidate()
        candidate.pop("scope")
        candidate.pop("purpose")

        with self.assertRaisesRegex(ValueError, "classification cannot change"):
            merge_candidate(
                existing,
                candidate,
                generated_at="2026-08-31T01:00:00Z",
            )


if __name__ == "__main__":
    unittest.main()
