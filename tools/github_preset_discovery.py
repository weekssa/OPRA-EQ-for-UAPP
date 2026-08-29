#!/usr/bin/env python3
"""Normalize public GitHub code-search/Gist results into EQ source candidates.

Discovery is deliberately separate from ingestion and publication. A matching public
file is only a candidate until its origin, license/redistribution terms, headphone
identity and creator attribution are qualified. This tool performs no network calls;
a scheduled job can feed authenticated GitHub API responses into it deterministically.
"""

from __future__ import annotations

import argparse
import hashlib
import json
from pathlib import Path
from typing import Any, Iterable

SUPPORTED_EXTENSIONS = {".txt", ".json", ".csv", ".yaml", ".yml"}
STRONG_NAME_SIGNALS = (
    "parametriceq",
    "parametric_eq",
    "parametric-eq",
    "equalizerapo",
    "equalizer_apo",
    "peace",
    "peq",
    "preset",
)


def stable_candidate_id(url: str) -> str:
    return "github-" + hashlib.sha256(url.encode("utf-8")).hexdigest()[:24]


def is_structured_eq_path(path: str) -> bool:
    lowered = path.lower()
    suffix = Path(lowered).suffix
    if suffix not in SUPPORTED_EXTENSIONS:
        return False
    compact = lowered.replace(" ", "")
    return any(signal in compact for signal in STRONG_NAME_SIGNALS)


def _candidate(
    *,
    url: str,
    raw_url: str | None,
    creator: str | None,
    repository: str | None,
    path: str,
    record_id: str,
    content_sha: str | None,
    updated_at: str | None,
    platform: str,
) -> dict[str, Any]:
    return {
        "candidate_id": stable_candidate_id(url),
        "source_id": "github-community",
        "source_kind": "community_repository",
        "platform": platform,
        "repository": repository,
        "path": path,
        "url": url,
        "raw_url": raw_url,
        "creator": creator,
        "source_record_id": record_id,
        "content_sha": content_sha,
        "source_updated_at": updated_at,
        "status": "new_candidate",
        "redistribution": "review-required",
        "publication_eligible": False,
        "license_review_required": True,
        "qualification_required": [
            "originality",
            "license_or_redistribution_terms",
            "creator_attribution",
            "headphone_identity",
            "structured_eq_parse",
            "canonical_dedupe",
        ],
    }


def discover_code_search(payload: dict[str, Any]) -> list[dict[str, Any]]:
    candidates: dict[str, dict[str, Any]] = {}
    for item in payload.get("items", []):
        if not isinstance(item, dict):
            continue
        path = str(item.get("path") or item.get("name") or "").strip()
        url = str(item.get("html_url") or "").strip()
        if not url or not is_structured_eq_path(path):
            continue
        repository = item.get("repository") if isinstance(item.get("repository"), dict) else {}
        repo_name = str(repository.get("full_name") or "").strip() or None
        owner = repository.get("owner") if isinstance(repository.get("owner"), dict) else {}
        creator = str(owner.get("login") or "").strip() or None
        sha = str(item.get("sha") or "").strip() or None
        record_id = f"{repo_name or 'unknown'}:{path}:{sha or 'unknown'}"
        candidate = _candidate(
            url=url,
            raw_url=None,
            creator=creator,
            repository=repo_name,
            path=path,
            record_id=record_id,
            content_sha=sha,
            updated_at=None,
            platform="github_code",
        )
        candidates[candidate["candidate_id"]] = candidate
    return sorted(candidates.values(), key=lambda item: (item["repository"] or "", item["path"], item["url"]))


def discover_gists(payload: Iterable[dict[str, Any]]) -> list[dict[str, Any]]:
    candidates: dict[str, dict[str, Any]] = {}
    for gist in payload:
        if not isinstance(gist, dict):
            continue
        gist_url = str(gist.get("html_url") or "").strip()
        gist_id = str(gist.get("id") or "").strip()
        if not gist_url or not gist_id:
            continue
        owner = gist.get("owner") if isinstance(gist.get("owner"), dict) else {}
        creator = str(owner.get("login") or "").strip() or None
        updated_at = str(gist.get("updated_at") or "").strip() or None
        files = gist.get("files") if isinstance(gist.get("files"), dict) else {}
        for filename, file_info in files.items():
            if not isinstance(file_info, dict) or not is_structured_eq_path(str(filename)):
                continue
            raw_url = str(file_info.get("raw_url") or "").strip() or None
            file_url = f"{gist_url}#file-{str(filename).lower().replace('.', '-').replace(' ', '-')}"
            record_id = f"gist:{gist_id}:{filename}"
            candidate = _candidate(
                url=file_url,
                raw_url=raw_url,
                creator=creator,
                repository=None,
                path=str(filename),
                record_id=record_id,
                content_sha=None,
                updated_at=updated_at,
                platform="github_gist",
            )
            candidates[candidate["candidate_id"]] = candidate
    return sorted(candidates.values(), key=lambda item: (item["creator"] or "", item["path"], item["url"]))


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("input", type=Path)
    parser.add_argument("--kind", required=True, choices=("code-search", "gist-list"))
    parser.add_argument("--output", type=Path)
    args = parser.parse_args()

    payload = json.loads(args.input.read_text(encoding="utf-8"))
    if args.kind == "code-search":
        if not isinstance(payload, dict):
            raise ValueError("code-search input must be a GitHub search response object")
        candidates = discover_code_search(payload)
    else:
        if not isinstance(payload, list):
            raise ValueError("gist-list input must be a list of GitHub Gist objects")
        candidates = discover_gists(payload)

    result = {
        "schema_version": 1,
        "source_id": "github-community",
        "candidate_count": len(candidates),
        "candidates": candidates,
    }
    rendered = json.dumps(result, indent=2, sort_keys=True, ensure_ascii=False) + "\n"
    if args.output:
        args.output.parent.mkdir(parents=True, exist_ok=True)
        args.output.write_text(rendered, encoding="utf-8")
    else:
        print(rendered, end="")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
