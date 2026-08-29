import unittest
from datetime import datetime, timezone

from catalog_pipeline import SourceHealth
from currentness_scheduler import (
    build_currentness_plan,
    discovery_due,
    is_source_due,
    plan_known_source_scans,
    source_health_warnings,
)


def source(
    source_id: str,
    *,
    lifecycle: str = "active",
    cadence: str = "daily",
) -> dict:
    return {
        "id": source_id,
        "kind": "community",
        "name": source_id,
        "scope": "test",
        "lifecycle": lifecycle,
        "cadence": cadence,
        "parser": "peq",
        "parser_version": "1",
        "cursor_strategy": "id",
        "redistribution": "link-only",
        "attribution_required": True,
    }


class CurrentnessSchedulerTest(unittest.TestCase):
    def setUp(self):
        self.now = datetime(2026, 8, 29, 15, 0, tzinfo=timezone.utc)

    def test_never_scanned_active_source_is_due(self):
        item = source("a")
        health = SourceHealth(source_id="a", lifecycle="active")
        self.assertTrue(is_source_due(item, health, self.now))

    def test_recently_scanned_source_waits_for_cadence(self):
        item = source("a", cadence="daily")
        health = SourceHealth(
            source_id="a",
            lifecycle="active",
            last_successful_scan_at="2026-08-29T14:30:00Z",
        )
        self.assertFalse(is_source_due(item, health, self.now))

    def test_paused_and_manual_sources_are_never_scheduled(self):
        paused = source("paused", lifecycle="paused")
        manual = source("manual", cadence="manual")
        self.assertFalse(is_source_due(paused, SourceHealth("paused", "paused"), self.now))
        self.assertFalse(is_source_due(manual, SourceHealth("manual", "active"), self.now))

    def test_plan_preserves_cursor_and_parser_version(self):
        registry = {
            "schema_version": 1,
            "registry_version": "1",
            "sources": [source("a")],
        }
        health = {"a": SourceHealth("a", "active", cursor="post-42", parser_version="0")}
        planned = plan_known_source_scans(registry, health, now=self.now)
        self.assertEqual(1, len(planned))
        self.assertEqual("post-42", planned[0].cursor)
        self.assertEqual("1", planned[0].parser_version)
        self.assertEqual("never_scanned", planned[0].reason)

    def test_health_warns_after_repeated_failures_and_staleness(self):
        registry = {
            "schema_version": 1,
            "registry_version": "1",
            "sources": [source("a", cadence="daily")],
        }
        health = {
            "a": SourceHealth(
                "a",
                "active",
                last_successful_scan_at="2026-08-20T15:00:00Z",
                consecutive_failures=4,
            )
        }
        warnings = source_health_warnings(registry, health, now=self.now)
        self.assertEqual({"repeated_failures", "stale"}, {warning.kind for warning in warnings})

    def test_discovery_loop_is_weekly(self):
        self.assertFalse(discovery_due("2026-08-28T15:00:00Z", now=self.now))
        self.assertTrue(discovery_due("2026-08-20T15:00:00Z", now=self.now))
        self.assertTrue(discovery_due(None, now=self.now))

    def test_combined_plan_keeps_new_source_discovery_independent(self):
        registry = {
            "schema_version": 1,
            "registry_version": "1",
            "sources": [source("active"), source("paused", lifecycle="paused")],
        }
        plan = build_currentness_plan(
            registry,
            {},
            now=self.now,
            last_discovery_at="2026-08-01T00:00:00Z",
        )
        self.assertEqual(["active"], [item["source_id"] for item in plan["known_source_scans"]])
        self.assertTrue(plan["new_source_discovery_due"])


if __name__ == "__main__":
    unittest.main()
