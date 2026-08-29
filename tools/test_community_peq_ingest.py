import unittest

from community_peq_ingest import build_candidate, parse_peq


SAMPLE = """Preamp: -4.0 dB
Filter 1: ON PK Fc 100 Hz Gain 3.0 dB Q 0.70
Filter 2: ON PK Fc 3000 Hz Gain -2.0 dB Q 1.20
"""


class CommunityPeqIngestTest(unittest.TestCase):
    def test_structured_data_policy_keeps_filters_and_provenance(self):
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
        self.assertEqual(2, len(revision["filters"]))
        self.assertEqual("Original Author", candidate["creator"])
        self.assertEqual("structured-data-only", revision["source_references"][0]["redistribution_policy"])
        self.assertEqual("Diffuse Field", candidate["target"]["name"])

    def test_link_only_keeps_fingerprint_but_does_not_redistribute_filters(self):
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


if __name__ == "__main__":
    unittest.main()
