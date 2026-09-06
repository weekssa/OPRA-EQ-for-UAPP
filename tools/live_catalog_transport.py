#!/usr/bin/env python3
"""Validate and render the repository catalog for stable Android transport.

Stable `catalog-live` publication must be derived from the latest validated `main`
catalog, not from a workflow's run-start candidate. Multiple catalog writers can be
serialized yet still carry an older candidate snapshot if `main` advanced while that
workflow was running. This helper validates the authoritative repository snapshot and
renders the deterministic compact JSON consumed by Android.
"""

from __future__ import annotations

import argparse
import json
from pathlib import Path
from typing import Any

from catalog_pipeline import validate_snapshot


def render_transport(snapshot: dict[str, Any]) -> str:
    errors = validate_snapshot(snapshot)
    if errors:
        raise ValueError("live catalog snapshot is invalid: " + "; ".join(errors))
    return json.dumps(
        snapshot,
        sort_keys=True,
        separators=(",", ":"),
        ensure_ascii=False,
    ) + "\n"


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--input", type=Path, required=True)
    parser.add_argument("--output", type=Path, required=True)
    args = parser.parse_args()

    payload = json.loads(args.input.read_text(encoding="utf-8"))
    if not isinstance(payload, dict):
        raise ValueError("live catalog input must be a JSON object")

    rendered = render_transport(payload)
    args.output.parent.mkdir(parents=True, exist_ok=True)
    args.output.write_text(rendered, encoding="utf-8")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
