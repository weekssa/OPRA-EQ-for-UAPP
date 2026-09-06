import unittest

from oratory_update_discovery import discover_from_listing, is_paused_reddit_url, manual_result


class OratoryUpdateDiscoveryTest(unittest.TestCase):
    def test_picks_latest_update_and_remains_link_only(self):
        payload = {
            "data": {
                "children": [
                    {
                        "data": {
                            "id": "old",
                            "title": "oratory1990’s list of EQ Presets [update 24.02.26]",
                            "permalink": "/r/oratory1990/comments/old/example/",
                            "created_utc": 1770000000,
                        }
                    },
                    {
                        "data": {
                            "id": "new",
                            "title": "oratory1990’s list of EQ Presets [update 13.04.26]",
                            "permalink": "/r/oratory1990/comments/new/example/",
                            "created_utc": 1776000000,
                        }
                    },
                    {"data": {"id": "other", "title": "Weekly EQ Thread", "created_utc": 1777000000}},
                ]
            }
        }
        result = discover_from_listing(payload)
        self.assertEqual("ok", result["status"])
        self.assertEqual("new", result["latest"]["post_id"])
        self.assertEqual("13.04.26", result["latest"]["update_label"])
        self.assertEqual("link-only", result["redistribution_policy"])
        self.assertFalse(result["publication_eligible"])
        self.assertEqual(2, result["matched_update_posts"])

    def test_no_matching_post_is_not_publishable(self):
        result = discover_from_listing({"data": {"children": []}})
        self.assertEqual("no_update_post_found", result["status"])
        self.assertIsNone(result["latest"])
        self.assertFalse(result["publication_eligible"])

    def test_reddit_fetch_urls_are_classified_as_paused(self):
        self.assertTrue(is_paused_reddit_url("https://www.reddit.com/r/oratory1990/new.json?limit=100"))
        self.assertTrue(is_paused_reddit_url("https://old.reddit.com/r/oratory1990/new.json"))
        self.assertFalse(is_paused_reddit_url("https://example.test/listing.json"))

    def test_manual_result_preserves_link_only_provenance_without_claiming_scan_success(self):
        result = manual_result()
        self.assertEqual("manual", result["status"])
        self.assertEqual("link-only", result["redistribution_policy"])
        self.assertFalse(result["publication_eligible"])
        self.assertIsNone(result["latest"])
        self.assertIn("HTTP 403", result["note"])


if __name__ == "__main__":
    unittest.main()
