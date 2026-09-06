#!/usr/bin/env python3
"""Audit EQ source currentness without confusing scheduled, runtime, and manual lanes.

The source registry may describe sources that are maintained in different ways:

- scheduled: repository automation is responsible for source-health freshness;
- runtime: the Android client checks the source directly (currently OPRA);
- manual: curated/manual publication has no scheduled source-health SLA;
- review: discovery/qualification-only source, not a publication-currentness SLA;
- paused: intentionally disabled live access while archived data is retained.

Strict mode fails only for genuinely scheduled sources whose health proves that the
scheduled currentness contract is broken. It never deletes or mutates catalog data.
"""

from __future__ import annotations

import argparse
import json
from dataclasses import asdict, dataclass
from datetime import datetime, timedelta, timezone
from pathlib import Path
from typing import Any

from catalog_pipeline import SourceHealth, load_health, load_json, validate_registry
from currentness_scheduler import CADENCE_SECONDS, parse_utc, utc_iso

VALID_CURRENTNESS_MODES = {"scheduled", "runtime", "manual", "review", "paused"}
DEFAULT_OVERDUE_MULTIPLIER = 2
DEFAULT_FAILURE_THRESHOLD = 3


@dataclass(frozen=True)
class SourceAuditRow:
    source_id: str
    lifecycle: str
    cadence: str
    currentness_mode: str
    status: str
    last_successful_scan_at: str | None
    last_attempt_at: str | None
    consecutive_failures: int
    detail: str


def source_mode(source: dict[str, Any]) -> str:
    explicit = str(source.get("currentness_mode") or "").strip()
    if explicit:
        return explicit
    if source.get("lifecycle") == "paused":
        return "paused"
    if source.get("cadence") == "manual":
        return "manual"
    return "scheduled"


def audit_sources(
    registry: dict[str, Any],
    health: dict[str, SourceHealth],
    *,
    now: datetime,
    overdue_multiplier: int = DEFAULT_OVERDUE_MULTIPLIER,
    failure_threshold: int = DEFAULT_FAILURE_THRESHOLD,
) -> dict[str, Any]:
    registry_errors = validate_registry(registry)
    rows: list[SourceAuditRow] = []
    errors: list[str] = []
    warnings: list[str] = []

    if overdue_multiplier < 1:
        raise ValueError("overdue_multiplier must be at least 1")
    if failure_threshold < 1:
        raise ValueError("failure_threshold must be at least 1")

    for source in sorted(registry.get("sources", []), key=lambda item: str(item.get("id", ""))):
        source_id = str(source.get("id") or "").strip()
        lifecycle = str(source.get("lifecycle") or "")
        cadence = str(source.get("cadence") or "")
        mode = source_mode(source)
        state = health.get(source_id) or SourceHealth(source_id=source_id, lifecycle=lifecycle)

        if mode not in VALID_CURRENTNESS_MODES:
            message = f"{source_id}: invalid currentness_mode {mode!r}"
            errors.append(message)
            rows.append(
                SourceAuditRow(
                    source_id, lifecycle, cadence, mode, "invalid", state.last_successful_scan_at,
                    state.last_attempt_at, state.consecutive_failures, message,
                )
            )
            continue

        if mode != "scheduled":
            detail = {
                "runtime": "currentness is maintained by the Android runtime rather than repository source-health",
                "manual": "source is curated/manual and has no scheduled source-health SLA",
                "review": "source is discovery/qualification-only and has no publication-currentness SLA",
                "paused": "live scanning is intentionally paused; archived EQs remain preserved",
            }[mode]
            rows.append(
                SourceAuditRow(
                    source_id, lifecycle, cadence, mode, mode, state.last_successful_scan_at,
                    state.last_attempt_at, state.consecutive_failures, detail,
                )
            )
            continue

        cadence_seconds = CADENCE_SECONDS.get(cadence)
        if cadence_seconds is None:
            message = f"{source_id}: scheduled currentness requires a non-manual supported cadence"
            errors.append(message)
            rows.append(
                SourceAuditRow(
                    source_id, lifecycle, cadence, mode, "invalid", state.last_successful_scan_at,
                    state.last_attempt_at, state.consecutive_failures, message,
                )
            )
            continue

        last_success = parse_utc(state.last_successful_scan_at)
        if last_success is None:
            message = f"{source_id}: scheduled source has never recorded a successful scan"
            errors.append(message)
            rows.append(
                SourceAuditRow(
                    source_id, lifecycle, cadence, mode, "never_successful", None,
                    state.last_attempt_at, state.consecutive_failures, message,
                )
            )
            continue

        overdue_after = timedelta(seconds=cadence_seconds * overdue_multiplier)
        age = now - last_success
        if age >= overdue_after:
            message = (
                f"{source_id}: last successful scan {utc_iso(last_success)} is overdue "
                f"for {cadence} cadence with {overdue_multiplier}x grace"
            )
            errors.append(message)
            status = "overdue"
        elif state.consecutive_failures >= failure_threshold:
            message = f"{source_id}: {state.consecutive_failures} consecutive scan failures"
            errors.append(message)
            status = "repeated_failures"
        elif state.consecutive_failures > 0:
            message = (
                f"{source_id}: {state.consecutive_failures} recent scan failure(s); "
                f"last success {utc_iso(last_success)} remains within SLA"
            )
            warnings.append(message)
            status = "degraded"
        else:
            message = f"last successful scan {utc_iso(last_success)} is within SLA"
            status = "healthy"

        rows.append(
            SourceAuditRow(
                source_id, lifecycle, cadence, mode, status, state.last_successful_scan_at,
                state.last_attempt_at, state.consecutive_failures, message,
            )
        )

    errors = registry_errors + errors
    return {
        "schema_version": 1,
        "generated_at": utc_iso(now),
        "strict_ok": not errors,
        "errors": errors,
        "warnings": warnings,
        "sources": [asdict(row) for row in rows],
    }


def markdown_summary(report: dict[str, Any]) -> str:
    lines = ["## Source currentness audit", ""]
    lines.append(f"- strict status: **{'PASS' if report['strict_ok'] else 'FAIL'}**")
    lines.append(f"- errors: {len(report['errors'])}")
    lines.append(f"- warnings: {len(report['warnings'])}")
    lines.extend(["", "| Source | Mode | Cadence | Status |", "|---|---|---|---|"])
    for row in report["sources"]:
        lines.append(
            f"| `{row['source_id']}` | {row['currentness_mode']} | {row['cadence']} | {row['status']} |"
        )
    if report["errors"]:
        lines.extend(["", "### Blocking currentness errors", ""])
        lines.extend(f"- {item}" for item in report["errors"])
    if report["warnings"]:
        lines.extend(["", "### Currentness warnings", ""])
        lines.extend(f"- {item}" for item in report["warnings"])
    return "\n".join(lines) + "\n"


def build_parser() -> argparse.ArgumentParser:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--registry", default="config/source_registry.json")
    parser.add_argument("--state", default="catalog/source_health.json")
    parser.add_argument("--output")
    parser.add_argument("--summary")
    parser.add_argument("--now", help="UTC/offset ISO timestamp for deterministic validation")
    parser.add_argument("--overdue-multiplier", type=int, default=DEFAULT_OVERDUE_MULTIPLIER)
    parser.add_argument("--failure-threshold", type=int, default=DEFAULT_FAILURE_THRESHOLD)
    parser.add_argument("--strict", action="store_true", help="return nonzero when blocking errors exist")
    return parser


def main() -> int:
    args = build_parser().parse_args()
    try:
        registry = load_json(Path(args.registry))
        health = load_health(Path(args.state))
        now = parse_utc(args.now) if args.now else datetime.now(timezone.utc)
        assert now is not None
        report = audit_sources(
            registry,
            health,
            now=now,
            overdue_multiplier=args.overdue_multiplier,
            failure_threshold=args.failure_threshold,
        )
        rendered = json.dumps(report, indent=2, sort_keys=True) + "\n"
        if args.output:
            Path(args.output).write_text(rendered, encoding="utf-8")
        else:
            print(rendered, end="")
        if args.summary:
            Path(args.summary).write_text(markdown_summary(report), encoding="utf-8")
        return 1 if args.strict and not report["strict_ok"] else 0
    except (OSError, json.JSONDecodeError, ValueError, KeyError) as exc:
        print(f"ERROR: {exc}")
        return 1


if __name__ == "__main__":
    raise SystemExit(main())
