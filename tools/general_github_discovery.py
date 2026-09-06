#!/usr/bin/env python3
"""Normalize public GitHub code-search results into review-only General EQ candidates.

This is discovery, not publication. Results are deliberately broad enough to find source
code and structured preset files, but every candidate remains blocked on originality,
license/redistribution, creator attribution, explicit General EQ intent/category,
structured filter parsing, and canonical deduplication.
"""

from __future__ import annotations

import argparse
import hashlib
import json
from pathlib import Path
from typing import Any

SUPPORTED_EXTENSIONS = {
    ".txt", ".json", ".csv", ".yaml", ".yml", ".js", ".ts", ".swift", ".kt", ".java", ".html"
}
PATH_SIGNALS = ("eq", "equalizer", "preset", "filter", "audio")


def candidate_id(url: str) -> str:
    return "github-general-" + hashlib.sha256(url.encode("utf-8")).hexdigest()[:24]


def plausible_path(path: str) -> bool:
    lowered = path.lower()
    if Path(lowered).suffix not in SUPPORTED_EXTENSIONS:
        return False
    return any(signal in lowered for signal in PATH_SIGNALS)


def discover(payloads: list[dict[str, Any]]) -> list[dict[str, Any]]:
    candidates: dict[str, dict[str, Any]] = {}
    for payload in payloads:
        for item in payload.get("items") or []:
            if not isinstance(item, dict):
                continue
            path = str(item.get("path") or item.get("name") or "").strip()
            url = str(item.get("html_url") or "").strip()
            if not url or not plausible_path(path):
                continue
            repository = item.get("repository") if isinstance(item.get("repository"), dict) else {}
            repo_name = str(repository.get("full_name") or "").strip() or None
            owner = repository.get("owner") if isinstance(repository.get("owner"), dict) else {}
            creator = str(owner.get("login") or "").strip() or None
            sha = str(item.get("sha") or "").strip() or None
            record_id = f"{repo_name or 'unknown'}:{path}:{sha or 'unknown'}"
            result = {
                "candidate_id": candidate_id(url),
                "source_id": "github-community",
                "source_kind": "community_repository",
                "scope_hint": "general",
                "platform": "github_code",
                "repository": repo_name,
                "path": path,
                "url": url,
                "creator": creator,
                "source_record_id": record_id,
                "content_sha": sha,
                "status": "new_candidate",
                "redistribution": "review-required",
                "publication_eligible": False,
                "license_review_required": True,
                "qualification_required": [
                    "originality",
                    "license_or_redistribution_terms",
                    "creator_attribution",
                    "explicit_general_eq_intent_and_category",
                    "structured_eq_parse",
                    "canonical_dedupe",
                ],
            }
            candidates[result["candidate_id"]] = result
    return sorted(candidates.values(), key=lambda item: (item.get("repository") or "", item["path"], item["url"]))


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("input", nargs="+", type=Path)
    parser.add_argument("--output", type=Path, required=True)
    args = parser.parse_args()

    payloads = [json.loads(path.read_text(encoding="utf-8")) for path in args.input]
    if any(not isinstance(payload, dict) for payload in payloads):
        raise ValueError("every input must be a GitHub code-search response object")
    candidates = discover(payloads)
    result = {
        "schema_version": 1,
        "source_id": "github-community",
        "scope_hint": "general",
        "candidate_count": len(candidates),
        "candidates": candidates,
    }
    args.output.parent.mkdir(parents=True, exist_ok=True)
    args.output.write_text(json.dumps(result, indent=2, sort_keys=True, ensure_ascii=False) + "\n", encoding="utf-8")
    print(json.dumps({"candidate_count": len(candidates)}, sort_keys=True))
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
