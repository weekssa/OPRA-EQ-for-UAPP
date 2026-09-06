#!/usr/bin/env python3
"""Audit broad GitHub General-EQ discovery through the production community rules.

Broad General-EQ searches intentionally find source-code and graphic-EQ preset tables as
well as true PEQ. This processor fetches every candidate and records why it can or cannot
be canonicalized. It never invents Q, filter type, preamp, or a General category.

A future candidate that is exact Equalizer APO-style PEQ is reported as
``structured_peq_needs_explicit_general_category`` until the source itself supplies a
clear Sound/Genre/Utility intent that the General publisher can preserve.
"""

from __future__ import annotations

import argparse
import json
from pathlib import Path
from typing import Any, Callable

from community_peq_ingest import parse_peq
from github_community_ingest import fetch_candidate_text


def audit(
    discovery: dict[str, Any],
    *,
    github_token: str | None,
    fetcher: Callable[[dict[str, Any], str | None], str] = fetch_candidate_text,
) -> dict[str, Any]:
    records: list[dict[str, Any]] = []
    counts: dict[str, int] = {}
    for item in discovery.get("candidates") or []:
        if not isinstance(item, dict):
            continue
        row = {
            "candidate_id": item.get("candidate_id"),
            "repository": item.get("repository"),
            "path": item.get("path"),
            "url": item.get("url"),
        }
        try:
            text = fetcher(item, github_token)
        except Exception as exc:
            decision = "fetch_failed"
            row["detail"] = str(exc)[:240]
        else:
            try:
                parsed = parse_peq(text)
            except ValueError:
                decision = "no_exact_parametric_structure"
            else:
                decision = "structured_peq_needs_explicit_general_category"
                row["filter_count"] = len(parsed.filters)
                row["preamp_db"] = parsed.preamp_db
        row["decision"] = decision
        counts[decision] = counts.get(decision, 0) + 1
        records.append(row)
    return {
        "source_id": str(discovery.get("source_id") or "github-community"),
        "scope": "general",
        "processed": len(records),
        "decision_counts": dict(sorted(counts.items())),
        "records": records,
    }


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--discovery", type=Path, required=True)
    parser.add_argument("--report", type=Path, required=True)
    parser.add_argument("--github-token")
    args = parser.parse_args()
    discovery = json.loads(args.discovery.read_text(encoding="utf-8"))
    result = audit(discovery, github_token=args.github_token)
    args.report.parent.mkdir(parents=True, exist_ok=True)
    args.report.write_text(
        json.dumps(result, indent=2, sort_keys=True, ensure_ascii=False) + "\n",
        encoding="utf-8",
    )
    print(json.dumps(result, sort_keys=True))
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
