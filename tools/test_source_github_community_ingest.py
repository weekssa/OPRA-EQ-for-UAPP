import unittest

import github_community_ingest
from community_peq_ingest import build_candidate, parse_peq


def source_entry():
    return {
        "id": "github-community",
        "kind": "community_repository",
        "name": "Public GitHub repositories and Gists",
        "scope": "public structured community PEQ",
        "lifecycle": "active",
        "cadence": "daily",
        "currentness_mode": "scheduled",
        "parser": "github-community-peq",
        "parser_version": "1",
        "cursor_strategy": "discovery queue plus source blob SHA",
        "redistribution": "structured-data-only",
        "attribution_required": True,
    }


def registry():
    return {"schema_version": 1, "registry_version": "test-1", "sources": [source_entry()]}


def existing_profile(peq_text="Preamp: -2 dB\nFilter 1: ON PK Fc 1000 Hz Gain 2 dB Q 1\n"):
    candidate = build_candidate(
        parse_peq(peq_text),
        manufacturer="Sony",
        model="WH-1000XM4",
        creator="Existing Creator",
        tuning_label="Existing tuning",
        source_id="seed",
        source_kind="community",
        source_url="https://example.test/original",
        source_record_id="seed-1",
        redistribution_policy="structured-data-only",
        target=None,
        variant=None,
        source_version=None,
        discovered_at_epoch_seconds=None,
        verification_status="verified",
    )
    candidate.pop("publication_eligible", None)
    return candidate


def snapshot():
    return {
        "schema_version": 1,
        "generated_at": "2026-09-07T00:00:00Z",
        "source_registry_version": "test-1",
        "headphone_aliases": [],
        "profiles": [existing_profile()],
    }


def discovery(path="Sony WH-1000XM4 Community ParametricEQ.txt"):
    return {
        "schema_version": 1,
        "source_id": "github-community",
        "candidates": [
            {
                "candidate_id": "github-test",
                "content_sha": "abc123",
                # Legacy queues used repository ownership as `creator`. The production
                # adapter must reinterpret that as source-account provenance only unless
                # creator_is_explicit is true.
                "creator": "listener",
                "path": path,
                "repository": "listener/eq-presets",
                "source_record_id": f"listener/eq-presets:{path}:abc123",
                "url": "https://github.com/listener/eq-presets/blob/deadbeef/" + path.replace(" ", "%20"),
            }
        ],
    }


class GithubCommunityIngestTest(unittest.TestCase):
    def test_valid_public_peq_publishes_as_unverified_without_inventing_creator(self):
        peq = "Preamp: -4 dB\nFilter 1: ON PK Fc 180 Hz Gain 3 dB Q 0.9\n"
        merged, health, report, success = github_community_ingest.ingest_candidates(
            snapshot(),
            discovery(),
            registry(),
            {},
            github_token=None,
            fetcher=lambda item, token: peq,
        )
        self.assertTrue(success)
        self.assertEqual(2, len(merged["profiles"]))
        self.assertEqual(1, report["publishable"])
        self.assertEqual(0, report["quarantined"])
        new_profile = next(
            profile for profile in merged["profiles"]
            if profile["canonical_profile_id"] != existing_profile()["canonical_profile_id"]
        )
        revision = new_profile["revisions"][0]
        self.assertIsNone(new_profile["creator"])
        self.assertIsNone(revision["source_references"][0]["creator"])
        self.assertEqual("listener", report["records"][0]["source_account"])
        self.assertIsNone(report["records"][0]["creator"])
        self.assertEqual("unverified", revision["verification_status"])
        self.assertEqual(-4.0, revision["preamp_gain_db"])
        self.assertEqual("github-community", revision["source_references"][0]["source_id"])
        self.assertIsNotNone(health["github-community"].last_successful_scan_at)

    def test_explicit_creator_is_preserved_when_authorship_is_marked_explicit(self):
        input_discovery = discovery()
        input_discovery["candidates"][0]["creator"] = "Actual EQ Author"
        input_discovery["candidates"][0]["creator_is_explicit"] = True
        input_discovery["candidates"][0]["source_account"] = "listener"
        merged, _, report, success = github_community_ingest.ingest_candidates(
            snapshot(),
            input_discovery,
            registry(),
            {},
            github_token=None,
            fetcher=lambda item, token: "Filter 1: ON PK Fc 180 Hz Gain 3 dB Q 0.9\n",
        )
        self.assertTrue(success)
        new_profile = next(
            profile for profile in merged["profiles"]
            if profile["canonical_profile_id"] != existing_profile()["canonical_profile_id"]
        )
        self.assertEqual("Actual EQ Author", new_profile["creator"])
        self.assertEqual("Actual EQ Author", new_profile["revisions"][0]["source_references"][0]["creator"])
        self.assertEqual("listener", report["records"][0]["source_account"])

    def test_missing_creator_uses_source_record_for_stable_distinct_identity(self):
        peq = "Filter 1: ON PK Fc 180 Hz Gain 3 dB Q 0.9\n"
        first = discovery()
        second = discovery()
        second["candidates"][0].update(
            {
                "candidate_id": "github-test-2",
                "repository": "another/presets",
                "creator": "another",
                "source_record_id": "another/presets:Sony WH-1000XM4 Community ParametricEQ.txt:def456",
                "content_sha": "def456",
                "url": "https://github.com/another/presets/blob/deadbeef/Sony%20WH-1000XM4%20Community%20ParametricEQ.txt",
            }
        )
        first_merged, _, _, _ = github_community_ingest.ingest_candidates(
            snapshot(), first, registry(), {}, github_token=None,
            fetcher=lambda item, token: peq,
        )
        second_merged, _, _, _ = github_community_ingest.ingest_candidates(
            snapshot(), second, registry(), {}, github_token=None,
            fetcher=lambda item, token: peq.replace("3 dB", "2.5 dB"),
        )
        first_id = next(
            p["canonical_profile_id"] for p in first_merged["profiles"]
            if p["canonical_profile_id"] != existing_profile()["canonical_profile_id"]
        )
        second_id = next(
            p["canonical_profile_id"] for p in second_merged["profiles"]
            if p["canonical_profile_id"] != existing_profile()["canonical_profile_id"]
        )
        self.assertNotEqual(first_id, second_id)

    def test_missing_preamp_remains_null(self):
        peq = "Filter 1: ON PK Fc 500 Hz Gain -1.5 dB Q 1.1\n"
        merged, _, _, success = github_community_ingest.ingest_candidates(
            snapshot(), discovery(), registry(), {}, github_token=None,
            fetcher=lambda item, token: peq,
        )
        self.assertTrue(success)
        new_profile = next(
            profile for profile in merged["profiles"]
            if profile["canonical_profile_id"] != existing_profile()["canonical_profile_id"]
        )
        self.assertIsNone(new_profile["revisions"][0]["preamp_gain_db"])

    def test_exact_duplicate_attaches_provenance_without_duplicate_profile(self):
        peq = "Preamp: -2 dB\nFilter 1: ON PK Fc 1000 Hz Gain 2 dB Q 1\n"
        merged, _, report, success = github_community_ingest.ingest_candidates(
            snapshot(), discovery(), registry(), {}, github_token=None,
            fetcher=lambda item, token: peq,
        )
        self.assertTrue(success)
        self.assertEqual(1, len(merged["profiles"]))
        self.assertEqual(1, report["exact_duplicates"])
        profile = merged["profiles"][0]
        self.assertEqual("Existing Creator", profile["creator"])
        refs = profile["revisions"][0]["source_references"]
        self.assertEqual({"seed", "github-community"}, {ref["source_id"] for ref in refs})
        github_ref = next(ref for ref in refs if ref["source_id"] == "github-community")
        self.assertIsNone(github_ref["creator"])

    def test_unknown_headphone_is_quarantined_without_blocking_scan(self):
        merged, health, report, success = github_community_ingest.ingest_candidates(
            snapshot(),
            discovery("Mystery Device ParametricEQ.txt"),
            registry(),
            {},
            github_token=None,
            fetcher=lambda item, token: "Filter 1: ON PK Fc 1000 Hz Gain 1 dB Q 1\n",
        )
        self.assertTrue(success)
        self.assertEqual(snapshot()["profiles"], merged["profiles"])
        self.assertEqual(1, report["quarantined"])
        self.assertEqual(1, report["quarantine_reasons"]["unmatched_headphone_identity"])
        self.assertEqual(0, health["github-community"].consecutive_failures)

    def test_malformed_filter_is_quarantined_not_partially_imported(self):
        malformed = "Preamp: -3 dB\nFilter 1: ON LP Fc 1000 Hz Gain 1 dB Q 1\n"
        merged, _, report, success = github_community_ingest.ingest_candidates(
            snapshot(), discovery(), registry(), {}, github_token=None,
            fetcher=lambda item, token: malformed,
        )
        self.assertTrue(success)
        self.assertEqual(snapshot()["profiles"], merged["profiles"])
        self.assertEqual(1, report["quarantine_reasons"]["unsupported_or_malformed_peq"])

    def test_target_folder_is_metadata_not_headphone_identity(self):
        candidate = discovery("CCA FLA Target/EPZ Q1 R ParametricEQ.txt")["candidates"][0]
        matched, reason = github_community_ingest.resolve_headphone(
            candidate,
            [("CCA", "FLA"), ("EPZ", "Q1")],
        )
        self.assertEqual(("EPZ", "Q1"), matched)
        self.assertIsNone(reason)
        self.assertEqual("CCA FLA Target", github_community_ingest.explicit_target(candidate["path"]))


if __name__ == "__main__":
    unittest.main()
