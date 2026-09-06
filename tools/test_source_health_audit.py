import unittest
from datetime import datetime, timezone

from catalog_pipeline import SourceHealth
from source_health_audit import audit_sources, source_mode


def source(
    source_id: str,
    *,
    lifecycle: str = "active",
    cadence: str = "daily",
    currentness_mode: str | None = "scheduled",
) -> dict:
    item = {
        "id": source_id,
        "kind": "community",
        "name": source_id,
        "scope": "test",
        "lifecycle": lifecycle,
        "cadence": cadence,
        "parser": "peq",
        "parser_version": "1",
        "cursor_strategy": "id",
        "redistribution": "structured-data-only",
        "attribution_required": True,
    }
    if currentness_mode is not None:
        item["currentness_mode"] = currentness_mode
    return item


def registry(*sources: dict) -> dict:
    return {"schema_version": 1, "registry_version": "test", "sources": list(sources)}


class SourceHealthAuditTest(unittest.TestCase):
    def setUp(self):
        self.now = datetime(2026, 9, 7, 12, 0, tzinfo=timezone.utc)

    def test_scheduled_source_with_recent_success_is_healthy(self):
        report = audit_sources(
            registry(source("scheduled")),
            {
                "scheduled": SourceHealth(
                    "scheduled",
                    "active",
                    last_successful_scan_at="2026-09-06T12:30:00Z",
                )
            },
            now=self.now,
        )
        self.assertTrue(report["strict_ok"])
        self.assertEqual("healthy", report["sources"][0]["status"])

    def test_scheduled_source_that_never_succeeded_is_blocking(self):
        report = audit_sources(registry(source("never")), {}, now=self.now)
        self.assertFalse(report["strict_ok"])
        self.assertEqual("never_successful", report["sources"][0]["status"])

    def test_overdue_scheduled_source_is_blocking_after_two_cadences(self):
        report = audit_sources(
            registry(source("old", cadence="daily")),
            {
                "old": SourceHealth(
                    "old",
                    "active",
                    last_successful_scan_at="2026-09-04T11:59:59Z",
                )
            },
            now=self.now,
        )
        self.assertFalse(report["strict_ok"])
        self.assertEqual("overdue", report["sources"][0]["status"])

    def test_repeated_failures_are_blocking_even_with_recent_success(self):
        report = audit_sources(
            registry(source("degraded")),
            {
                "degraded": SourceHealth(
                    "degraded",
                    "active",
                    last_successful_scan_at="2026-09-07T11:00:00Z",
                    consecutive_failures=3,
                    last_error="offline",
                )
            },
            now=self.now,
        )
        self.assertFalse(report["strict_ok"])
        self.assertEqual("repeated_failures", report["sources"][0]["status"])

    def test_runtime_manual_review_and_paused_modes_do_not_require_repo_scan_history(self):
        report = audit_sources(
            registry(
                source("runtime", currentness_mode="runtime"),
                source("manual", cadence="manual", currentness_mode="manual"),
                source("review", cadence="manual", lifecycle="reviewing", currentness_mode="review"),
                source("paused", lifecycle="paused", currentness_mode="paused"),
            ),
            {},
            now=self.now,
        )
        self.assertTrue(report["strict_ok"])
        self.assertEqual(
            {"runtime", "manual", "review", "paused"},
            {row["status"] for row in report["sources"]},
        )

    def test_scheduled_mode_rejects_manual_cadence(self):
        report = audit_sources(
            registry(source("bad", cadence="manual", currentness_mode="scheduled")),
            {},
            now=self.now,
        )
        self.assertFalse(report["strict_ok"])
        self.assertEqual("invalid", report["sources"][0]["status"])

    def test_legacy_registry_infers_existing_behavior(self):
        self.assertEqual("scheduled", source_mode(source("a", currentness_mode=None)))
        self.assertEqual(
            "manual",
            source_mode(source("b", cadence="manual", currentness_mode=None)),
        )
        self.assertEqual(
            "paused",
            source_mode(source("c", lifecycle="paused", currentness_mode=None)),
        )


if __name__ == "__main__":
    unittest.main()
