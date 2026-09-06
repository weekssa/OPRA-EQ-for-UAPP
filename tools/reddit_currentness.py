#!/usr/bin/env python3
"""Run whole-library Reddit PEQ discovery with durable source-health bookkeeping.

This adapter deliberately reuses reddit_community_ingest's conservative parser and
headphone matcher. It does not invent new headphone identities: community posts are
published only when they match exactly one headphone already known to the canonical
catalog. Network degradation preserves the input catalog and records a source-health
failure instead of replacing last-known-good data.
"""

from __future__ import annotations

import argparse
import json
from pathlib import Path
from typing import Any

from catalog_merge import merge_candidates
from catalog_pipeline import (
    load_health,
    load_json,
    reconcile_health,
    record_scan_failure,
    record_scan_success,
    sha256_json,
    validate_registry,
    write_health,
)
from reddit_community_ingest import DEFAULT_SUBREDDITS, discover

SOURCE_ID = "reddit-audio"


def _source(registry: dict[str, Any]) -> dict[str, Any]:
    for source in registry.get("sources") or []:
        if source.get("id") == SOURCE_ID:
            return source
    raise ValueError(f"source registry is missing {SOURCE_ID}")


def _cursor(candidates: list[dict[str, Any]], report: dict[str, Any]) -> str:
    record_ids = sorted(
        str(item.get("post_id") or "")
        for item in report.get("candidate_sources") or []
        if str(item.get("post_id") or "").strip()
    )
    if record_ids:
        return sha256_json(record_ids)
    return sha256_json(
        {
            "posts_seen": report.get("posts_seen", 0),
            "posts_with_peq": report.get("posts_with_peq", 0),
            "candidate_count": len(candidates),
        }
    )


def refresh(
    snapshot: dict[str, Any],
    registry: dict[str, Any],
    current_health: dict[str, Any],
    *,
    subreddits: list[str],
    limit: int,
) -> tuple[dict[str, Any], dict[str, Any], dict[str, Any], bool]:
    errors = validate_registry(registry)
    if errors:
        raise ValueError("invalid source registry: " + "; ".join(errors))
    source = _source(registry)
    if source.get("lifecycle") != "active":
        raise ValueError(f"{SOURCE_ID} must be active for publication")
    if source.get("redistribution") != "structured-data-only":
        raise ValueError(f"{SOURCE_ID} is not qualified for structured publication")

    health = reconcile_health(registry, current_health)
    candidates, report = discover(snapshot, subreddits, limit, headphone_model=None)
    listings_attempted = int(report.get("listings_attempted") or 0)
    listing_errors = len(report.get("errors") or [])
    listings_succeeded = max(0, listings_attempted - listing_errors)
    report["listings_succeeded"] = listings_succeeded

    if listings_attempted == 0 or listings_succeeded == 0:
        reason = "all Reddit discovery requests failed" if listings_attempted else "no Reddit listings were attempted"
        health[SOURCE_ID] = record_scan_failure(health[SOURCE_ID], reason)
        report["status"] = "degraded"
        report["publication_skipped"] = True
        return snapshot, health, report, False

    if candidates:
        merged, outcomes = merge_candidates(
            snapshot,
            candidates,
            source_registry_version=str(registry.get("registry_version") or "") or None,
        )
    else:
        merged, outcomes = snapshot, {}
    report["merge_outcomes"] = outcomes
    report["status"] = "partial" if listing_errors else "ok"
    report["publication_skipped"] = False
    health[SOURCE_ID] = record_scan_success(
        health[SOURCE_ID],
        cursor=_cursor(candidates, report),
        content_fingerprint=sha256_json(candidates),
    )
    return merged, health, report, True


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--catalog", type=Path, required=True)
    parser.add_argument("--registry", type=Path, required=True)
    parser.add_argument("--health", type=Path, required=True)
    parser.add_argument("--catalog-output", type=Path, required=True)
    parser.add_argument("--health-output", type=Path, required=True)
    parser.add_argument("--report", type=Path, required=True)
    parser.add_argument("--subreddit", action="append", dest="subreddits")
    parser.add_argument("--limit", type=int, default=100)
    parser.add_argument("--allow-degraded", action="store_true")
    args = parser.parse_args()

    snapshot = load_json(args.catalog)
    registry = load_json(args.registry)
    merged, health, report, success = refresh(
        snapshot,
        registry,
        load_health(args.health),
        subreddits=args.subreddits or list(DEFAULT_SUBREDDITS),
        limit=args.limit,
    )

    args.catalog_output.parent.mkdir(parents=True, exist_ok=True)
    args.catalog_output.write_text(json.dumps(merged, indent=2, ensure_ascii=False) + "\n", encoding="utf-8")
    write_health(args.health_output, health)
    args.report.parent.mkdir(parents=True, exist_ok=True)
    args.report.write_text(json.dumps(report, indent=2, sort_keys=True) + "\n", encoding="utf-8")
    print(json.dumps(report, sort_keys=True))
    return 0 if success or args.allow_degraded else 1


if __name__ == "__main__":
    raise SystemExit(main())
