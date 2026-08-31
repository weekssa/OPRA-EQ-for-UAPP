#!/usr/bin/env python3
"""Validate and summarize the focused EQ Library community-discovery coverage ledger."""
from __future__ import annotations

import argparse
import json
from pathlib import Path
from typing import Any

ALLOWED_STATES = {"searched", "source-qualification", "not_yet_searched", "paused", "error"}
ALLOWED_HEADPHONE_STATUS = {"active-coverage", "hard-to-find-review", "complete-for-current-sources", "paused"}
REQUIRED_PRIORITY = {
    ("HIFIMAN", "Edition XS"),
    ("AFUL", "Explorer"),
    ("SIMGOT", "EW300"),
    ("Sennheiser", "HD 650"),
    ("Sony", "MDR-V6"),
    ("Sony", "WH-1000XM4"),
    ("Sony", "WF-1000XM5"),
    ("Sony", "MDR-7506"),
}
COUNT_FIELDS = ("candidates_found", "publishable_or_published", "duplicates", "held")


def _text(value: Any, label: str) -> str:
    text = str(value or "").strip()
    if not text:
        raise ValueError(f"{label} must be non-empty")
    return text


def validate_coverage(payload: dict[str, Any]) -> dict[str, int]:
    if payload.get("schema_version") != 1:
        raise ValueError("coverage schema_version must be 1")
    _text(payload.get("updated_at"), "updated_at")

    rows = payload.get("priority_headphones")
    if not isinstance(rows, list) or not rows:
        raise ValueError("priority_headphones must be a non-empty list")

    seen_headphones: set[tuple[str, str]] = set()
    source_rows = 0
    searched_rows = 0
    candidate_total = 0
    published_total = 0
    held_total = 0

    for index, row in enumerate(rows):
        if not isinstance(row, dict):
            raise ValueError(f"priority_headphones[{index}] must be an object")
        manufacturer = _text(row.get("manufacturer"), f"priority_headphones[{index}].manufacturer")
        model = _text(row.get("model"), f"priority_headphones[{index}].model")
        identity = (manufacturer, model)
        if identity in seen_headphones:
            raise ValueError(f"duplicate priority headphone {manufacturer} {model}")
        seen_headphones.add(identity)

        status = _text(row.get("status"), f"{manufacturer} {model} status")
        if status not in ALLOWED_HEADPHONE_STATUS:
            raise ValueError(f"unsupported priority status {status!r} for {manufacturer} {model}")

        sources = row.get("sources")
        if not isinstance(sources, list) or not sources:
            raise ValueError(f"{manufacturer} {model} must contain source coverage rows")
        seen_sources: set[str] = set()
        for source in sources:
            if not isinstance(source, dict):
                raise ValueError(f"{manufacturer} {model} source coverage must be objects")
            source_id = _text(source.get("source_id"), f"{manufacturer} {model} source_id")
            if source_id in seen_sources:
                raise ValueError(f"duplicate source {source_id} for {manufacturer} {model}")
            seen_sources.add(source_id)
            state = _text(source.get("state"), f"{manufacturer} {model} {source_id} state")
            if state not in ALLOWED_STATES:
                raise ValueError(f"unsupported source state {state!r} for {manufacturer} {model}/{source_id}")

            counts: dict[str, int] = {}
            for field in COUNT_FIELDS:
                value = source.get(field, 0)
                if not isinstance(value, int) or isinstance(value, bool) or value < 0:
                    raise ValueError(f"{manufacturer} {model}/{source_id} {field} must be a non-negative integer")
                counts[field] = value
            if counts["publishable_or_published"] + counts["duplicates"] + counts["held"] > counts["candidates_found"]:
                raise ValueError(
                    f"{manufacturer} {model}/{source_id} disposition counts exceed candidates_found"
                )
            if state == "not_yet_searched" and any(counts.values()):
                raise ValueError(f"not_yet_searched row {manufacturer} {model}/{source_id} cannot have candidate counts")

            source_rows += 1
            if state in {"searched", "source-qualification"}:
                searched_rows += 1
            candidate_total += counts["candidates_found"]
            published_total += counts["publishable_or_published"]
            held_total += counts["held"]

    missing = REQUIRED_PRIORITY - seen_headphones
    extra = seen_headphones - REQUIRED_PRIORITY
    if missing:
        rendered = ", ".join(f"{m} {n}" for m, n in sorted(missing))
        raise ValueError(f"priority coverage is missing approved targets: {rendered}")
    if extra:
        rendered = ", ".join(f"{m} {n}" for m, n in sorted(extra))
        raise ValueError(f"priority coverage contains unexpected targets: {rendered}")

    return {
        "headphones": len(seen_headphones),
        "source_rows": source_rows,
        "searched_or_qualifying_rows": searched_rows,
        "candidates": candidate_total,
        "publishable_or_published": published_total,
        "held": held_total,
    }


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--coverage", type=Path, required=True)
    args = parser.parse_args()
    payload = json.loads(args.coverage.read_text(encoding="utf-8"))
    print(json.dumps(validate_coverage(payload), sort_keys=True))
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
