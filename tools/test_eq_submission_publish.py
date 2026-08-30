import unittest

from eq_submission_publish import publish_submission, source_id_for_url


def source_reference():
    return {
        "source_id": "opra",
        "source_kind": "structured_catalog",
        "source_record_id": "base",
        "source_vendor_id": "Example Audio",
        "source_product_id": "Model One",
        "url": "https://example.com/base",
        "creator": "Base",
        "provenance_tier": "authoritative",
        "redistribution_policy": "structured-data-only",
        "published_at_epoch_seconds": None,
        "updated_at_epoch_seconds": None,
        "discovered_at_epoch_seconds": None,
        "last_verified_at_epoch_seconds": None,
        "is_primary": True,
    }


def snapshot(*, aliases=None):
    return {
        "schema_version": 1,
        "generated_at": "2026-08-30T00:00:00Z",
        "source_registry_version": "old",
        "headphone_aliases": aliases or [],
        "sources": [],
        "profiles": [
            {
                "canonical_profile_id": "base-profile",
                "headphone": {
                    "manufacturer": "Example Audio",
                    "model": "Model One",
                    "variant": None,
                    "pads_or_mode": None,
                },
                "creator": "Base",
                "target": {"name": None, "kind": "unknown"},
                "tuning_label": "Base",
                "revisions": [
                    {
                        "revision_id": "base-revision",
                        "acoustic_fingerprint": "base-fingerprint",
                        "preamp_gain_db": -1.0,
                        "filters": [
                            {"type": "peak", "frequency_hz": 1000.0, "gain_db": -1.0, "q": 1.0, "slope": None}
                        ],
                        "source_references": [source_reference()],
                        "source_version_label": None,
                        "sound_impact_summary": None,
                        "first_seen_at_epoch_seconds": None,
                        "source_updated_at_epoch_seconds": None,
                        "is_latest": True,
                    }
                ],
            }
        ],
    }


def registry(*, headfi_lifecycle="active"):
    return {
        "schema_version": 1,
        "registry_version": "test-registry",
        "sources": [
            {"id": "reddit-audio", "lifecycle": "active", "redistribution": "structured-data-only"},
            {"id": "head-fi", "lifecycle": headfi_lifecycle, "redistribution": "structured-data-only"},
            {"id": "audio-science-review", "lifecycle": "active", "redistribution": "structured-data-only"},
            {"id": "headphones-community", "lifecycle": "active", "redistribution": "structured-data-only"},
        ],
    }


def submission(*, url="https://www.head-fi.org/threads/example.123/post-456", model="Model One", filters=None):
    return {
        "schema_version": 1,
        "submission_id": "github-issue-42",
        "candidate_state": "ready_for_source_policy",
        "mechanically_valid": True,
        "verification_status": "unverified",
        "publication_eligible": False,
        "headphone": {"manufacturer": "Example Audio", "model": model, "variant": None},
        "creator": "CreatorName",
        "original_source_url": url,
        "source_platform": "Head-Fi",
        "target": "Neutral Target",
        "tuning_label": "Neutral community tuning",
        "source_date": "2026-08-29",
        "sound_impact": "Source describes a warmer balance with reduced treble energy.",
        "parsed_peq": {
            "status": "parsed",
            "preamp_db": None,
            "filters": filters or [
                {"type": "peak", "frequency_hz": 100.0, "gain_db": 2.0, "q": 1.0, "slope": None},
                {"type": "peak", "frequency_hz": 3000.0, "gain_db": -2.0, "q": 1.2, "slope": None},
            ],
        },
        "validation_errors": [],
        "review_warnings": [],
    }


class EqSubmissionPublishTest(unittest.TestCase):
    def test_recognizes_only_approved_community_domains(self):
        self.assertEqual("reddit-audio", source_id_for_url("https://www.reddit.com/r/headphones/comments/abc"))
        self.assertEqual("head-fi", source_id_for_url("https://www.head-fi.org/threads/example.1/"))
        self.assertEqual("audio-science-review", source_id_for_url("https://audiosciencereview.com/forum/index.php?threads/x.1/"))
        self.assertEqual("headphones-community", source_id_for_url("https://forum.headphones.com/t/example/1"))
        self.assertIsNone(source_id_for_url("https://example.com/preset"))
        self.assertIsNone(source_id_for_url("https://fakehead-fi.org/preset"))

    def test_valid_headfi_submission_publishes_as_unverified(self):
        merged, report = publish_submission(snapshot(), submission(), registry())

        self.assertEqual("published-unverified", report["decision"])
        self.assertEqual("head-fi", report["source_id"])
        published = next(profile for profile in merged["profiles"] if profile["canonical_profile_id"] != "base-profile")
        revision = published["revisions"][0]
        self.assertEqual("unverified", revision["verification_status"])
        self.assertEqual(2, len(revision["filters"]))
        self.assertIsNone(revision["preamp_gain_db"])
        self.assertEqual(
            "Source describes a warmer balance with reduced treble energy.",
            revision["sound_impact_summary"],
        )

    def test_unapproved_source_domain_stays_review_only(self):
        original = snapshot()
        merged, report = publish_submission(
            original,
            submission(url="https://example.com/preset"),
            registry(),
        )

        self.assertEqual("hold-needs-review", report["decision"])
        self.assertIn("allowlist", report["reason"])
        self.assertIs(merged, original)

    def test_unresolved_headphone_identity_stays_review_only(self):
        merged, report = publish_submission(snapshot(), submission(model="Brand New Model"), registry())

        self.assertEqual("hold-needs-review", report["decision"])
        self.assertIn("headphone identity", report["reason"])
        self.assertEqual(1, len(merged["profiles"]))

    def test_reviewed_alias_resolves_to_existing_canonical_identity(self):
        aliases = [
            {
                "manufacturer": "Example Audio",
                "canonical_model": "Model One",
                "aliases": ["M1"],
                "evidence": ["https://example.com/evidence"],
            }
        ]
        merged, report = publish_submission(
            snapshot(aliases=aliases),
            submission(model="M1"),
            registry(),
        )

        self.assertEqual("published-unverified", report["decision"])
        published = next(profile for profile in merged["profiles"] if profile["canonical_profile_id"] != "base-profile")
        self.assertEqual("Model One", published["headphone"]["model"])

    def test_inactive_registered_source_does_not_auto_publish(self):
        merged, report = publish_submission(snapshot(), submission(), registry(headfi_lifecycle="reviewing"))

        self.assertEqual("hold-needs-review", report["decision"])
        self.assertIn("not active", report["reason"])
        self.assertEqual(1, len(merged["profiles"]))

    def test_different_acoustic_data_in_same_lineage_requires_revision_review(self):
        first, first_report = publish_submission(snapshot(), submission(), registry())
        self.assertEqual("published-unverified", first_report["decision"])
        changed_filters = [
            {"type": "peak", "frequency_hz": 100.0, "gain_db": 4.0, "q": 1.0, "slope": None}
        ]

        second, second_report = publish_submission(
            first,
            submission(filters=changed_filters),
            registry(),
        )

        self.assertEqual("hold-needs-review", second_report["decision"])
        self.assertIn("revision intent", second_report["reason"])
        community = next(profile for profile in second["profiles"] if profile["canonical_profile_id"] != "base-profile")
        self.assertEqual(1, len(community["revisions"]))


if __name__ == "__main__":
    unittest.main()
