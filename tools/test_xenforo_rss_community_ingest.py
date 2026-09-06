import unittest

from xenforo_rss_community_ingest import discover, parse_feed, parse_thread_posts, refresh


RSS = """<?xml version="1.0" encoding="UTF-8"?>
<rss version="2.0"><channel><title>Headphones</title>
<item><title>Sennheiser HD 650 impressions</title><link>https://forum.example/threads/hd650.1/</link><guid>thread-1</guid><author>Alice</author></item>
</channel></rss>
"""

THREAD = """<!doctype html><html><body>
<article class="message message--post" data-author="Alice" data-content="post-123">
  <div class="message-body"><div class="bbWrapper">
    My settings:<br>
    Preamp: -4.0 dB<br>
    Filter 1: ON PK Fc 100 Hz Gain 3.0 dB Q 1.00<br>
    Filter 2: ON PK Fc 2500 Hz Gain -2.0 dB Q 2.00
  </div></div>
</article>
</body></html>
"""


def snapshot():
    return {
        "schema_version": 1,
        "profiles": [
            {
                "canonical_profile_id": "existing-hd650",
                "headphone": {"manufacturer": "Sennheiser", "model": "HD 650"},
                "revisions": [],
            }
        ],
    }


def source_config():
    return {
        "source_id": "head-fi",
        "base_url": "https://forum.example",
        "feeds": ["https://forum.example/forums/headphones/index.rss"],
        "max_threads_per_scan": 10,
    }


def registry():
    return {
        "schema_version": 1,
        "registry_version": "test",
        "sources": [
            {
                "id": "head-fi",
                "kind": "community",
                "name": "Head-Fi",
                "scope": "public test forum",
                "lifecycle": "active",
                "cadence": "weekly",
                "currentness_mode": "scheduled",
                "parser": "xenforo-rss-community-peq",
                "parser_version": "3",
                "cursor_strategy": "feed entries and post ids",
                "redistribution": "structured-data-only",
                "attribution_required": True,
            }
        ],
    }


class XenforoRssCommunityIngestTest(unittest.TestCase):
    def test_parses_rss_items(self):
        items = parse_feed(RSS)
        self.assertEqual(1, len(items))
        self.assertEqual("Sennheiser HD 650 impressions", items[0]["title"])
        self.assertEqual("thread-1", items[0]["guid"])

    def test_parses_post_author_id_and_body(self):
        posts = parse_thread_posts(THREAD)
        self.assertEqual(1, len(posts))
        self.assertEqual("Alice", posts[0]["author"])
        self.assertEqual("123", posts[0]["post_id"])
        self.assertIn("Filter 2: ON PK", posts[0]["text"])

    def test_exact_public_peq_becomes_unverified_candidate(self):
        def fetcher(url):
            if url.endswith("index.rss"):
                return RSS
            return THREAD

        candidates, report = discover(
            snapshot(), source_id="head-fi", source_config=source_config(), fetcher=fetcher
        )
        self.assertEqual(1, len(candidates))
        candidate = candidates[0]
        self.assertEqual("Sennheiser", candidate["headphone"]["manufacturer"])
        self.assertEqual("HD 650", candidate["headphone"]["model"])
        self.assertEqual("Alice", candidate["creator"])
        revision = candidate["revisions"][0]
        self.assertEqual("unverified", revision["verification_status"])
        self.assertEqual("head-fi", revision["source_references"][0]["source_id"])
        self.assertEqual(2, len(revision["filters"]))
        self.assertEqual(1, report["candidates"])

    def test_multiple_filter_blocks_are_quarantined(self):
        thread = THREAD.replace(
            "</div></div>",
            "<br>Filter 1: ON PK Fc 500 Hz Gain 1.0 dB Q 1.00</div></div>",
        )

        def fetcher(url):
            return RSS if url.endswith("index.rss") else thread

        candidates, report = discover(
            snapshot(), source_id="head-fi", source_config=source_config(), fetcher=fetcher
        )
        self.assertEqual([], candidates)
        self.assertEqual(1, report["ambiguous_peq_blocks"])

    def test_all_thread_fetches_failed_records_source_failure(self):
        def fetcher(url):
            if url.endswith("index.rss"):
                return RSS
            raise OSError("blocked")

        merged, health, report, success = refresh(
            snapshot(), registry(), {}, source_id="head-fi", source_config=source_config(), fetcher=fetcher
        )
        self.assertFalse(success)
        self.assertEqual(snapshot(), merged)
        self.assertEqual("degraded", report["status"])
        self.assertEqual(1, health["head-fi"].consecutive_failures)
        self.assertIsNone(health["head-fi"].last_successful_scan_at)


if __name__ == "__main__":
    unittest.main()
