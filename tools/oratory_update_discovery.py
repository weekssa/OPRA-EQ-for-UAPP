#!/usr/bin/env python3
"""Discover direct oratory1990 EQ-list updates without redistributing presets.

The creator source is link-only unless explicit redistribution permission exists.
This adapter watches public update posts and records provenance/currentness only.
It never downloads PDF contents or turns creator-hosted filter values into a
publishable catalog entry.
"""

from __future__ import annotations

import argparse
import json
import re
import urllib.request
from pathlib import Path
from typing import Any

TITLE_RE = re.compile(r"oratory1990.?s\s+list\s+of\s+eq\s+presets.*?update\s+([^\]]+)\]", re.IGNORECASE)
DEFAULT_URL = "https://www.reddit.com/r/oratory1990/new.json?limit=100&raw_json=1"


def _children(payload: dict[str, Any]) -> list[dict[str, Any]]:
    data = payload.get("data", {}) if isinstance(payload, dict) else {}
    children = data.get("children", []) if isinstance(data, dict) else []
    return [item.get("data", {}) for item in children if isinstance(item, dict) and isinstance(item.get("data"), dict)]


def discover_from_listing(payload: dict[str, Any]) -> dict[str, Any]:
    matches: list[dict[str, Any]] = []
    for post in _children(payload):
        title = str(post.get("title") or "").strip()
        match = TITLE_RE.search(title)
        if not match:
            continue
        permalink = str(post.get("permalink") or "").strip()
        url = "https://www.reddit.com" + permalink if permalink.startswith("/") else str(post.get("url") or "").strip()
        matches.append(
            {
                "post_id": str(post.get("id") or "").strip(),
                "title": title,
                "update_label": match.group(1).strip(),
                "url": url,
                "created_utc": int(float(post.get("created_utc") or 0)),
            }
        )
    matches.sort(key=lambda item: (item["created_utc"], item["post_id"]), reverse=True)
    latest = matches[0] if matches else None
    return {
        "source_id": "oratory1990",
        "source_kind": "creator",
        "redistribution_policy": "link-only",
        "publication_eligible": False,
        "status": "ok" if latest else "no_update_post_found",
        "latest": latest,
        "matched_update_posts": len(matches),
    }


def fetch_listing(url: str, timeout: int = 30) -> dict[str, Any]:
    request = urllib.request.Request(
        url,
        headers={
            "Accept": "application/json",
            "User-Agent": "EQ-Library-currentness/0.3 (+https://github.com/weekssa/OPRA-EQ-for-UAPP)",
        },
    )
    with urllib.request.urlopen(request, timeout=timeout) as response:
        return json.loads(response.read().decode("utf-8"))


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    source = parser.add_mutually_exclusive_group(required=True)
    source.add_argument("--listing", type=Path)
    source.add_argument("--fetch-url", default=None)
    parser.add_argument("--output", type=Path, required=True)
    parser.add_argument("--allow-degraded", action="store_true")
    args = parser.parse_args()

    try:
        if args.listing:
            payload = json.loads(args.listing.read_text(encoding="utf-8"))
        else:
            payload = fetch_listing(args.fetch_url or DEFAULT_URL)
        result = discover_from_listing(payload)
    except (OSError, ValueError, json.JSONDecodeError) as exc:
        if not args.allow_degraded:
            raise
        result = {
            "source_id": "oratory1990",
            "source_kind": "creator",
            "redistribution_policy": "link-only",
            "publication_eligible": False,
            "status": "degraded",
            "error": str(exc)[:500],
            "latest": None,
            "matched_update_posts": 0,
        }

    args.output.parent.mkdir(parents=True, exist_ok=True)
    args.output.write_text(json.dumps(result, indent=2, sort_keys=True, ensure_ascii=False) + "\n", encoding="utf-8")
    print(json.dumps(result, sort_keys=True, ensure_ascii=False))
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
