#!/usr/bin/env python3
"""Probe qualified GitHub-backed General EQ sources without silently reinterpreting changes.

A successful probe records durable source health. If the upstream file blob differs from
the reviewed/qualified blob, the source is reported as changed_needs_review and the
existing canonical manifest remains untouched. This preserves acoustic history and avoids
silently treating arbitrary upstream source-code edits as verified EQ revisions.
"""

from __future__ import annotations

import argparse
import json
import urllib.parse
import urllib.request
from pathlib import Path
from typing import Any, Callable

from catalog_pipeline import (
    load_health,
    load_json,
    reconcile_health,
    record_scan_failure,
    record_scan_success,
    validate_registry,
    write_health,
)


def fetch_github_json(repository: str, path: str, token: str | None = None) -> dict[str, Any]:
    encoded_path = "/".join(urllib.parse.quote(part, safe="") for part in path.split("/"))
    url = f"https://api.github.com/repos/{repository}/contents/{encoded_path}"
    headers = {
        "Accept": "application/vnd.github+json",
        "User-Agent": "EQ-Library-currentness/0.4",
        "X-GitHub-Api-Version": "2022-11-28",
    }
    if token:
        headers["Authorization"] = "Bearer " + token
    request = urllib.request.Request(url, headers=headers)
    with urllib.request.urlopen(request, timeout=30) as response:
        return json.loads(response.read().decode("utf-8"))


def probe(
    registry: dict[str, Any],
    manifest: dict[str, Any],
    current_health: dict[str, Any],
    *,
    token: str | None = None,
    fetcher: Callable[[str, str, str | None], dict[str, Any]] = fetch_github_json,
) -> tuple[dict[str, Any], list[dict[str, Any]]]:
    errors = validate_registry(registry)
    if errors:
        raise ValueError("invalid source registry: " + "; ".join(errors))
    health = reconcile_health(registry, current_health)
    registry_by_id = {source["id"]: source for source in registry.get("sources") or []}
    reports: list[dict[str, Any]] = []

    for item in manifest.get("sources") or []:
        source_id = str(item.get("source_id") or "").strip()
        repository = str(item.get("repository") or "").strip()
        path = str(item.get("path") or "").strip()
        qualified_blob = str(item.get("qualified_blob_sha") or "").strip()
        if not source_id or not repository or not path or not qualified_blob:
            raise ValueError("every qualified General source needs source_id, repository, path, and qualified_blob_sha")
        source = registry_by_id.get(source_id)
        if source is None:
            raise ValueError(f"qualified General source missing from registry: {source_id}")
        if source.get("lifecycle") != "active":
            raise ValueError(f"qualified General source is not active: {source_id}")

        report = {
            "source_id": source_id,
            "repository": repository,
            "path": path,
            "qualified_blob_sha": qualified_blob,
        }
        try:
            payload = fetcher(repository, path, token)
            current_blob = str(payload.get("sha") or "").strip()
            if not current_blob:
                raise ValueError("GitHub contents response did not include a blob SHA")
            report["current_blob_sha"] = current_blob
            report["status"] = "current" if current_blob == qualified_blob else "changed_needs_review"
            report["html_url"] = payload.get("html_url")
            health[source_id] = record_scan_success(
                health[source_id],
                cursor=current_blob,
                content_fingerprint=current_blob,
            )
        except Exception as exc:
            report["status"] = "degraded"
            report["error"] = str(exc)
            health[source_id] = record_scan_failure(health[source_id], str(exc))
        reports.append(report)

    return health, reports


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--registry", type=Path, required=True)
    parser.add_argument("--sources", type=Path, required=True)
    parser.add_argument("--health", type=Path, required=True)
    parser.add_argument("--health-output", type=Path, required=True)
    parser.add_argument("--report", type=Path, required=True)
    parser.add_argument("--github-token")
    parser.add_argument("--allow-degraded", action="store_true")
    args = parser.parse_args()

    health, reports = probe(
        load_json(args.registry),
        load_json(args.sources),
        load_health(args.health),
        token=args.github_token,
    )
    write_health(args.health_output, health)
    args.report.parent.mkdir(parents=True, exist_ok=True)
    args.report.write_text(json.dumps(reports, indent=2, sort_keys=True) + "\n", encoding="utf-8")
    print(json.dumps(reports, sort_keys=True))
    degraded = any(item.get("status") == "degraded" for item in reports)
    return 0 if not degraded or args.allow_degraded else 1


if __name__ == "__main__":
    raise SystemExit(main())
