import json
import unittest
from unittest import mock

import discourse_community_ingest
import general_github_discovery
import qualified_general_source_probe
import reddit_currentness


def source_entry(source_id, *, kind="community", lifecycle="active", cadence="daily", parser="test", redistribution="structured-data-only"):
    return {
        "id": source_id,
        "kind": kind,
        "name": source_id,
        "scope": "test source",
        "lifecycle": lifecycle,
        "cadence": cadence,
        "parser": parser,
        "parser_version": "1",
        "cursor_strategy": "test cursor",
        "redistribution": redistribution,
        "attribution_required": True,
    }


def registry(*sources):
    return {
        "schema_version": 1,
        "registry_version": "test-1",
        "sources": list(sources),
    }


class RedditCurrentnessTest(unittest.TestCase):
    def test_successful_empty_scan_records_source_health_without_mutating_catalog(self):
        snapshot = {"schema_version": 1, "generated_at": "2026-09-07T00:00:00Z", "source_registry_version": "test-1", "headphone_aliases": [], "profiles": []}
        report = {
            "listings_attempted": 2,
            "errors": [],
            "posts_seen": 0,
            "posts_with_peq": 0,
            "candidate_sources": [],
        }
        with mock.patch("reddit_currentness.discover", return_value=([], report)):
            merged, health, returned, success = reddit_currentness.refresh(
                snapshot,
                registry(source_entry("reddit-audio")),
                {},
                subreddits=["headphones"],
                limit=10,
            )
        self.assertTrue(success)
        self.assertEqual(snapshot, merged)
        self.assertEqual("ok", returned["status"])
        self.assertIsNotNone(health["reddit-audio"].last_successful_scan_at)
        self.assertEqual(0, health["reddit-audio"].consecutive_failures)

    def test_all_failed_listings_preserve_catalog_and_record_failure(self):
        snapshot = {"profiles": []}
        report = {
            "listings_attempted": 3,
            "errors": [{"error": "offline"}] * 3,
            "posts_seen": 0,
            "posts_with_peq": 0,
            "candidate_sources": [],
        }
        with mock.patch("reddit_currentness.discover", return_value=([], report)):
            merged, health, returned, success = reddit_currentness.refresh(
                snapshot,
                registry(source_entry("reddit-audio")),
                {},
                subreddits=["headphones"],
                limit=10,
            )
        self.assertFalse(success)
        self.assertEqual(snapshot, merged)
        self.assertEqual("degraded", returned["status"])
        self.assertEqual(1, health["reddit-audio"].consecutive_failures)


class DiscourseCommunityIngestTest(unittest.TestCase):
    def test_public_post_with_structured_peq_becomes_candidate_for_known_headphone(self):
        snapshot = {
            "profiles": [
                {"headphone": {"manufacturer": "Sony", "model": "WH-1000XM4"}}
            ]
        }
        search = {
            "topics": [{"id": 7, "slug": "sony-wh-1000xm4-eq", "title": "Sony WH-1000XM4 EQ"}],
            "posts": [{"id": 42, "topic_id": 7, "username": "listener"}],
        }
        post = {
            "id": 42,
            "topic_id": 7,
            "post_number": 3,
            "username": "listener",
            "raw": "Sony WH-1000XM4\nPreamp: -3 dB\nFilter 1: ON PK Fc 170 Hz Gain -3 dB Q 1.0",
        }

        def fetcher(url):
            return post if "/posts/42.json" in url else search

        candidates, report = discourse_community_ingest.discover(
            snapshot,
            base_url="https://forum.example.test",
            source_id="headphones-community",
            terms=["PEQ"],
            fetcher=fetcher,
        )
        self.assertEqual(1, report["candidates"])
        self.assertEqual(1, len(candidates))
        revision = candidates[0]["revisions"][0]
        self.assertEqual(-3.0, revision["preamp_gain_db"])
        self.assertEqual(1, len(revision["filters"]))
        reference = revision["source_references"][0]
        self.assertEqual("headphones-community", reference["source_id"])
        self.assertIn("/t/sony-wh-1000xm4-eq/7/3", reference["url"])

    def test_html_post_text_is_reduced_to_plain_text(self):
        text = discourse_community_ingest.plain_post_text({
            "cooked": "<p>Preamp: -2 dB<br>Filter 1: ON PK Fc 1000 Hz Gain 2 dB Q 1</p>"
        })
        self.assertIn("Preamp: -2 dB", text)
        self.assertIn("Filter 1:", text)


class GeneralGithubDiscoveryTest(unittest.TestCase):
    def test_general_candidate_is_review_only_and_does_not_require_headphone_identity(self):
        payload = {
            "items": [
                {
                    "path": "Sources/EQPresets.swift",
                    "html_url": "https://github.com/example/eq/blob/main/Sources/EQPresets.swift",
                    "sha": "abc123",
                    "repository": {
                        "full_name": "example/eq",
                        "owner": {"login": "example"},
                    },
                }
            ]
        }
        candidates = general_github_discovery.discover([payload])
        self.assertEqual(1, len(candidates))
        candidate = candidates[0]
        self.assertEqual("general", candidate["scope_hint"])
        self.assertFalse(candidate["publication_eligible"])
        self.assertIn("explicit_general_eq_intent_and_category", candidate["qualification_required"])
        self.assertNotIn("headphone_identity", candidate["qualification_required"])


class QualifiedGeneralSourceProbeTest(unittest.TestCase):
    def test_changed_upstream_blob_is_review_gated_but_scan_is_successful(self):
        reg = registry(
            source_entry(
                "milciossq-eq-general",
                kind="community_repository",
                cadence="weekly",
                parser="general-preset-qualified-peq",
            )
        )
        manifest = {
            "schema_version": 1,
            "sources": [
                {
                    "source_id": "milciossq-eq-general",
                    "repository": "MilcioSSQ/eq",
                    "path": "index.html",
                    "qualified_blob_sha": "old",
                }
            ],
        }

        def fetcher(repository, path, token):
            self.assertEqual("MilcioSSQ/eq", repository)
            self.assertEqual("index.html", path)
            return {"sha": "new", "html_url": "https://example.test/source"}

        health, reports = qualified_general_source_probe.probe(
            reg,
            manifest,
            {},
            token="test-token",
            fetcher=fetcher,
        )
        self.assertEqual("changed_needs_review", reports[0]["status"])
        self.assertEqual("new", health["milciossq-eq-general"].cursor)
        self.assertEqual(0, health["milciossq-eq-general"].consecutive_failures)


if __name__ == "__main__":
    unittest.main()
