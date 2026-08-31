import unittest

from catalog_merge import merge_candidate
from general_preset_ingest import build_candidate, canonical_profile_id
from community_peq_ingest import parse_peq


class GeneralPresetIngestTest(unittest.TestCase):
    def peq(self, count=3, include_preamp=True):
        lines = []
        if include_preamp:
            lines.append("Preamp: -4.5 dB")
        for index in range(1, count + 1):
            lines.append(
                f"Filter {index}: ON PK Fc {100 * index} Hz Gain {(-1) ** index * 1.5} dB Q 1.0"
            )
        return parse_peq("\n".join(lines))

    def candidate(self, *, purpose="effect", count=3, include_preamp=True, policy="structured-data-only"):
        return build_candidate(
            self.peq(count=count, include_preamp=include_preamp),
            purpose=purpose,
            creator="Example Creator",
            tuning_label="Bass Boost",
            source_id="headphones-community",
            source_kind="community",
            source_url="https://example.test/general-preset",
            source_record_id=f"record-{purpose}-{count}",
            redistribution_policy=policy,
            source_version="v1",
            discovered_at_epoch_seconds=1_788_000_000,
        )

    def test_effect_candidate_is_general_and_keeps_all_source_filters(self):
        candidate = self.candidate(count=14)

        self.assertEqual("general", candidate["scope"])
        self.assertEqual("effect", candidate["purpose"])
        self.assertIsNone(candidate["headphone"])
        self.assertEqual(14, len(candidate["revisions"][0]["filters"]))
        self.assertEqual(-4.5, candidate["revisions"][0]["preamp_gain_db"])
        self.assertTrue(candidate["publication_eligible"])

    def test_missing_source_preamp_remains_null(self):
        candidate = self.candidate(include_preamp=False)
        self.assertIsNone(candidate["revisions"][0]["preamp_gain_db"])

    def test_effect_and_genre_ids_are_distinct_even_with_same_creator_and_label(self):
        effect_id = canonical_profile_id(
            purpose="effect",
            creator="Example Creator",
            tuning_label="Bass Boost",
        )
        genre_id = canonical_profile_id(
            purpose="genre",
            creator="Example Creator",
            tuning_label="Bass Boost",
        )
        self.assertNotEqual(effect_id, genre_id)

    def test_mirror_source_id_does_not_change_lineage_id(self):
        expected = canonical_profile_id(
            purpose="effect",
            creator="Example Creator",
            tuning_label="Bass Boost",
        )
        candidate = self.candidate()
        self.assertEqual(expected, candidate["canonical_profile_id"])

    def test_link_only_candidate_retains_provenance_but_is_not_publishable(self):
        candidate = self.candidate(policy="link-only")
        self.assertFalse(candidate["publication_eligible"])
        self.assertEqual([], candidate["revisions"][0]["filters"])
        self.assertEqual(
            "https://example.test/general-preset",
            candidate["revisions"][0]["source_references"][0]["url"],
        )

    def test_invalid_or_implicit_purpose_is_rejected(self):
        with self.assertRaisesRegex(ValueError, "effect or genre"):
            build_candidate(
                self.peq(),
                purpose="personal_community",
                creator="Example Creator",
                tuning_label="Bass Boost",
                source_id="headphones-community",
                source_kind="community",
                source_url="https://example.test/general-preset",
                source_record_id="record-invalid",
                redistribution_policy="structured-data-only",
                source_version=None,
                discovered_at_epoch_seconds=None,
            )

    def test_candidate_merges_into_valid_snapshot_without_fake_headphone(self):
        snapshot = {
            "schema_version": 1,
            "generated_at": "2026-08-30T00:00:00Z",
            "source_registry_version": "test",
            "profiles": [],
        }
        merged, outcome = merge_candidate(
            snapshot,
            self.candidate(),
            generated_at="2026-08-31T00:00:00Z",
        )

        self.assertEqual("new_profile", outcome)
        profile = merged["profiles"][0]
        self.assertEqual("general", profile["scope"])
        self.assertEqual("effect", profile["purpose"])
        self.assertIsNone(profile["headphone"])
        self.assertEqual(3, len(profile["revisions"][0]["filters"]))


if __name__ == "__main__":
    unittest.main()
