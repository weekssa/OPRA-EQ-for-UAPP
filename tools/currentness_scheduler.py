#!/usr/bin/env python3
"""Plan recurring EQ source scans without coupling them to the Android app.

This module turns the machine-readable source registry plus persisted source-health
state into deterministic scan plans. It keeps three concerns separate:

1. known-source refreshes on their configured cadence,
2. health/degradation checks for sources that are stale or repeatedly failing,
3. a periodic discovery slot for qualifying entirely new public sources.

Source-specific adapters perform the actual network work. This scheduler only
answers what is due and why, making the currentness policy testable in CI.
"""

from __future__ import annotations

import argparse
import json
from dataclasses import asdict, dataclass
from datetime import datetime, timedelta, timezone
from pathlib import Path
from typing import Any

from catalog_pipeline import SourceHealth, load_health, load_json, reconcile_health, validate_registry

CADENCE_SECONDS = {
    "hourly": 60 * 60,
    "daily": 24 * 60 * 60,
    "weekly": 7 * 24 * 60 * 60,
    "monthly": 30 * 24 * 60 * 60,
}

SCANNABLE_LIFECYCLES = {"active", "link-only", "reviewing"}
DISCOVERY_INTERVAL_SECONDS = 7 * 24 * 60 * 60
DEFAULT_FAILURE_WARNING_THRESHOLD = 3
DEFAULT_STALE_MULTIPLIER = 3


def parse_utc(value: str | None) -> datetime | None:
    if not value:
        return None
    normalized = value.strip()
    if normalized.endswith("Z"):
        normalized = normalized[:-1] + "+00:00"
    parsed = datetime.fromisoformat(normalized)
    if parsed.tzinfo is None:
        parsed = parsed.replace(tzinfo=timezone.utc)
    return parsed.astimezone(timezone.utc)


def utc_iso(value: datetime) -> str:
    return value.astimezone(timezone.utc).replace(microsecond=0).isoformat().replace("+00:00", "Z")


@dataclass(frozen=True)
class ScanPlanItem:
    source_id: str
    reason: str
    cadence: str
    lifecycle: str
    cursor: str | None
    parser: str
    parser_version: str


@dataclass(frozen=True)
class HealthWarning:
    source_id: str
    kind: str
    detail: str


def is_source_due(source: dict[str, Any], health: SourceHealth, now: datetime) -> bool:
    if source.get("lifecycle") not in SCANNABLE_LIFECYCLES:
        return False
    cadence = source.get("cadence")
    if cadence == "manual":
        return False
    interval_seconds = CADENCE_SECONDS.get(str(cadence))
    if interval_seconds is None:
        return False
    last_success = parse_utc(health.last_successful_scan_at)
    if last_success is None:
        return True
    return now - last_success >= timedelta(seconds=interval_seconds)


def plan_known_source_scans(
    registry: dict[str, Any],
    health: dict[str, SourceHealth],
    *,
    now: datetime,
) -> list[ScanPlanItem]:
    reconciled = reconcile_health(registry, health)
    result: list[ScanPlanItem] = []
    for source in registry["sources"]:
        source_id = source["id"]
        state = reconciled[source_id]
        if not is_source_due(source, state, now):
            continue
        reason = "never_scanned" if state.last_successful_scan_at is None else "cadence_due"
        result.append(
            ScanPlanItem(
                source_id=source_id,
                reason=reason,
                cadence=source["cadence"],
                lifecycle=source["lifecycle"],
                cursor=state.cursor,
                parser=source["parser"],
                parser_version=str(source["parser_version"]),
            )
        )
    return sorted(result, key=lambda item: item.source_id)


def source_health_warnings(
    registry: dict[str, Any],
    health: dict[str, SourceHealth],
    *,
    now: datetime,
    failure_threshold: int = DEFAULT_FAILURE_WARNING_THRESHOLD,
    stale_multiplier: int = DEFAULT_STALE_MULTIPLIER,
) -> list[HealthWarning]:
    reconciled = reconcile_health(registry, health)
    warnings: list[HealthWarning] = []
    source_by_id = {source["id"]: source for source in registry["sources"]}
    for source_id, state in sorted(reconciled.items()):
        source = source_by_id[source_id]
        if source["lifecycle"] not in SCANNABLE_LIFECYCLES:
            continue
        if state.consecutive_failures >= failure_threshold:
            warnings.append(
                HealthWarning(
                    source_id=source_id,
                    kind="repeated_failures",
                    detail=f"{state.consecutive_failures} consecutive scan failures",
                )
            )
        cadence_seconds = CADENCE_SECONDS.get(source["cadence"])
        last_success = parse_utc(state.last_successful_scan_at)
        if cadence_seconds is not None and last_success is not None:
            stale_after = timedelta(seconds=cadence_seconds * stale_multiplier)
            if now - last_success >= stale_after:
                warnings.append(
                    HealthWarning(
                        source_id=source_id,
                        kind="stale",
                        detail=f"last successful scan was {utc_iso(last_success)}",
                    )
                )
    return warnings


def discovery_due(last_discovery_at: str | None, *, now: datetime) -> bool:
    last = parse_utc(last_discovery_at)
    if last is None:
        return True
    return now - last >= timedelta(seconds=DISCOVERY_INTERVAL_SECONDS)


def build_currentness_plan(
    registry: dict[str, Any],
    health: dict[str, SourceHealth],
    *,
    now: datetime,
    last_discovery_at: str | None = None,
) -> dict[str, Any]:
    errors = validate_registry(registry)
    if errors:
        raise ValueError("invalid source registry: " + "; ".join(errors))
    scans = plan_known_source_scans(registry, health, now=now)
    warnings = source_health_warnings(registry, health, now=now)
    return {
        "generated_at": utc_iso(now),
        "known_source_scans": [asdict(item) for item in scans],
        "health_warnings": [asdict(item) for item in warnings],
        "new_source_discovery_due": discovery_due(last_discovery_at, now=now),
    }


def command_plan(args: argparse.Namespace) -> int:
    registry = load_json(Path(args.registry))
    health = load_health(Path(args.state))
    now = parse_utc(args.now) if args.now else datetime.now(timezone.utc)
    assert now is not None
    plan = build_currentness_plan(
        registry,
        health,
        now=now,
        last_discovery_at=args.last_discovery_at,
    )
    print(json.dumps(plan, indent=2, sort_keys=True))
    return 0


def build_parser() -> argparse.ArgumentParser:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--registry", default="config/source_registry.json")
    parser.add_argument("--state", default="catalog/source_health.json")
    parser.add_argument("--last-discovery-at")
    parser.add_argument("--now", help="UTC/offset ISO timestamp, useful for deterministic CI tests")
    parser.set_defaults(func=command_plan)
    return parser


def main() -> int:
    args = build_parser().parse_args()
    try:
        return int(args.func(args))
    except (OSError, json.JSONDecodeError, ValueError, KeyError) as exc:
        print(f"ERROR: {exc}")
        return 1


if __name__ == "__main__":
    raise SystemExit(main())
