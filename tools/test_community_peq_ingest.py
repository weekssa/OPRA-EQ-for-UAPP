import unittest

from community_peq_ingest import build_candidate, parse_peq


SAMPLE = """Preamp: -4.0 dB
Filter 1: ON PK Fc 100 Hz Gain 3.0 dB Q 0.70
Filter 2: ON PK Fc 3000 Hz Gain -2.0 dB Q 1.20
"""


class CommunityPeqIngestTest(unittest.TestCase):
    def test_structured_data_policy_keeps_filters_and_canonical_provenance(self):
        parsed = parse_peq(SAMPLE)
        candidate = build_candidate(
            parsed,
            manufacturer="Example",
            model="Headphone",
            variant=None,
            creator="Original Author",
            tuning_label="Community tuning",
            target="Diffuse Field",
            source_id="github-community",
            source_kind="community_repository",
            source_url="https://example.com/preset",
            source_record_id="abc123",
            redistribution_policy="structured-data-only",
            source_version="abc123",
            discovered_at_epoch_seconds=123456,
        )
        revision = candidate["revisions"][0]
        source = revision["source_references"][0]
        self.assertEqual(2, len(revision["filters"]))
        self.assertEqual("Original Author", candidate["creator"])
        self.assertEqual("structured-data-only", source["redistribution_policy"])
        self.assertEqual("repository", source["source_kind"])
        self.assertEqual("traceable_community", source["provenance_tier"])
        self.assertEqual("Diffuse Field", candidate["target"]["name"])
        self.assertEqual("verified", revision["verification_status"])
        self.assertEqual(123456, source["last_verified_at_epoch_seconds"])
        self.assertTrue(candidate["publication_eligible"])

    def test_public_forum_tuning_defaults_to_unverified_but_publishable(self):
        parsed = parse_peq(SAMPLE)
        candidate = build_candidate(
            parsed,
            manufacturer="Example",
            model="Headphone",
            variant=None,
            creator="Forum User",
            tuning_label="Forum preset",
            target=None,
            source_id="reddit-audio",
            source_kind="community",
            source_url="https://example.com/post",
            source_record_id="post-1",
            redistribution_policy="structured-data-only",
            source_version=None,
            discovered_at_epoch_seconds=123456,
        )

        revision = candidate["revisions"][0]
        source = revision["source_references"][0]
        self.assertTrue(candidate["publication_eligible"])
        self.assertEqual("unverified", revision["verification_status"])
        self.assertIsNone(source["last_verified_at_epoch_seconds"])
        self.assertEqual(2, len(revision["filters"]))

    def test_explicit_verification_can_promote_forum_candidate(self):
        candidate = build_candidate(
            parse_peq(SAMPLE),
            manufacturer="Example",
            model="Headphone",
            variant=None,
            creator="Forum User",
            tuning_label="Forum preset",
            target=None,
            source_id="head-fi",
            source_kind="community",
            source_url="https://example.com/post",
            source_record_id="post-1",
            redistribution_policy="structured-data-only",
            source_version=None,
            discovered_at_epoch_seconds=123456,
            verification_status="verified",
        )
        self.assertEqual("verified", candidate["revisions"][0]["verification_status"])
        self.assertEqual(123456, candidate["revisions"][0]["source_references"][0]["last_verified_at_epoch_seconds"])

    def test_link_only_keeps_fingerprint_but_is_not_catalog_publishable(self):
        parsed = parse_peq(SAMPLE)
        candidate = build_candidate(
            parsed,
            manufacturer="Example",
            model="Headphone",
            variant="2026",
            creator="Forum User",
            tuning_label="Forum preset",
            target=None,
            source_id="reddit-audio",
            source_kind="community",
            source_url="https://example.com/post",
            source_record_id="post-1",
            redistribution_policy="link-only",
            source_version=None,
            discovered_at_epoch_seconds=123456,
        )
        revision = candidate["revisions"][0]
        self.assertEqual([], revision["filters"])
        self.assertTrue(revision["acoustic_fingerprint"])
        self.assertEqual("link-only", revision["source_references"][0]["redistribution_policy"])
        self.assertEqual("unverified", revision["verification_status"])
        self.assertEqual({"name": None, "kind": "unknown"}, candidate["target"])
        self.assertFalse(candidate["publication_eligible"])

    def test_creator_source_maps_to_authoritative_provenance(self):
        candidate = build_candidate(
            parse_peq(SAMPLE),
            manufacturer="Example",
            model="Headphone",
            variant=None,
            creator="Creator",
            tuning_label="Creator preset",
            target=None,
            source_id="creator-source",
            source_kind="creator",
            source_url="https://example.com/creator",
            source_record_id="preset-1",
            redistribution_policy="structured-data-only",
            source_version=None,
            discovered_at_epoch_seconds=None,
        )
        revision = candidate["revisions"][0]
        source = revision["source_references"][0]
        self.assertEqual("creator", source["source_kind"])
        self.assertEqual("authoritative", source["provenance_tier"])
        self.assertEqual("verified", revision["verification_status"])

    def test_rejects_unspecified_redistribution_policy(self):
        parsed = parse_peq(SAMPLE)
        with self.assertRaisesRegex(ValueError, "explicit safe redistribution policy"):
            build_candidate(
                parsed,
                manufacturer="Example",
                model="Headphone",
                variant=None,
                creator="Author",
                tuning_label="Preset",
                target=None,
                source_id="source",
                source_kind="community",
                source_url="https://example.com/post",
                source_record_id="1",
                redistribution_policy="review-required",
                source_version=None,
                discovered_at_epoch_seconds=None,
            )

    def test_rejects_missing_creator_attribution(self):
        parsed = parse_peq(SAMPLE)
        with self.assertRaisesRegex(ValueError, "Creator attribution is required"):
            build_candidate(
                parsed,
                manufacturer="Example",
                model="Headphone",
                variant=None,
                creator=" ",
                tuning_label="Preset",
                target=None,
                source_id="source",
                source_kind="community",
                source_url="https://example.com/post",
                source_record_id="1",
                redistribution_policy="link-only",
                source_version=None,
                discovered_at_epoch_seconds=None,
            )

    def test_rejects_invalid_verification_status(self):
        with self.assertRaisesRegex(ValueError, "verification_status"):
            build_candidate(
                parse_peq(SAMPLE),
                manufacturer="Example",
                model="Headphone",
                variant=None,
                creator="Author",
                tuning_label="Preset",
                target=None,
                source_id="source",
                source_kind="community",
                source_url="https://example.com/post",
                source_record_id="1",
                redistribution_policy="structured-data-only",
                source_version=None,
                discovered_at_epoch_seconds=None,
                verification_status="maybe",
            )


if __name__ == "__main__":
    unittest.main()
