import unittest

from squiglink_currentness import normalize_sites, refresh


def registry():
    return {
        "schema_version": 1,
        "registry_version": "test",
        "sources": [
            {
                "id": "squiglink",
                "kind": "structured_measurement",
                "name": "Squiglink-compatible sources",
                "scope": "public measurement registry",
                "lifecycle": "active",
                "cadence": "weekly",
                "currentness_mode": "scheduled",
                "parser": "squiglink-ecosystem-currentness",
                "parser_version": "4",
                "cursor_strategy": "public site registry fingerprint",
                "redistribution": "review-required",
                "attribution_required": True,
            }
        ],
    }


SITES = [
    {
        "username": "example",
        "name": "Example Measurements",
        "dbs": [
            {"folder": "/", "type": "IEMs"},
            {"folder": "/hp/", "type": "Headphones"},
        ],
    }
]


class SquiglinkCurrentnessTest(unittest.TestCase):
    def test_normalizes_public_site_registry(self):
        sites = normalize_sites(SITES)
        self.assertEqual(1, len(sites))
        self.assertEqual("https://example.squig.link", sites[0]["base_url"])
        self.assertEqual(2, len(sites[0]["dbs"]))

    def test_success_records_real_health_without_publishing_peq(self):
        health, report, success = refresh(registry(), {}, fetcher=lambda _url: SITES)
        self.assertTrue(success)
        self.assertEqual("ok", report["status"])
        self.assertEqual(1, report["site_count"])
        self.assertEqual(2, report["database_count"])
        self.assertIsNotNone(health["squiglink"].last_successful_scan_at)
        self.assertEqual(0, health["squiglink"].consecutive_failures)
        self.assertIn("no PEQ filters", report["publication_note"])

    def test_failure_preserves_last_success_and_increments_failure(self):
        healthy, _report, _success = refresh(registry(), {}, fetcher=lambda _url: SITES)

        def fail(_url):
            raise OSError("unavailable")

        health, report, success = refresh(registry(), healthy, fetcher=fail)
        self.assertFalse(success)
        self.assertEqual("degraded", report["status"])
        self.assertEqual(1, health["squiglink"].consecutive_failures)
        self.assertIsNotNone(health["squiglink"].last_successful_scan_at)


if __name__ == "__main__":
    unittest.main()
