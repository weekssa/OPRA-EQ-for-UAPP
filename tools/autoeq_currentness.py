#!/usr/bin/env python3
"""Refresh the qualified AutoEQ canary into the canonical EQ Library catalog.

This is the first executable known-source currentness adapter. It fetches the exact
upstream AutoEQ ParametricEQ.txt currently represented by the v0.3 canary, uses the
upstream blob SHA as an incremental cursor, parses/fingerprints structured PEQ data,
and merges a material acoustic change as an immutable revision. Review/licensing
qualification remains outside this adapter; it only operates on the already-qualified
AutoEQ source entry and exact configured canary record.
"""

from __future__ import annotations

import argparse
import base64
import json
import urllib.error
import urllib.parse
import urllib.request
from dataclasses import asdict
from datetime import datetime, timezone
from pathlib import Path
from typing import Any

from autoeq_ingest import build_candidate, parse_parametric_eq
from catalog_merge import merge_candidate
from catalog_pipeline import SourceHealth, load_health, reconcile_health, record_scan_failure, record_scan_success

AUTOEQ_REPO = "jaakkopasanen/AutoEq"
AUTOEQ_PATH = "results/oratory1990/over-ear/HIFIMAN Edition XS/HIFIMAN Edition XS ParametricEQ.txt"
AUTOEQ_API = f"https://api.github.com/repos/{AUTOEQ_REPO}/contents/{urllib.parse.quote(AUTOEQ_PATH, safe='/')}?ref=master"
AUTOEQ_HTML = "https://github.com/jaakkopasanen/AutoEq/blob/master/results/oratory1990/over-ear/HIFIMAN%20Edition%20XS/HIFIMAN%20Edition%20XS%20ParametricEQ.txt"


def utc_now() -> str:
    return datetime.now(timezone.utc).replace(microsecond=0).isoformat().replace("+00:00", "Z")


def epoch_now() -> int:
    return int(datetime.now(timezone.utc).timestamp())


def fetch_upstream(token: str | None = None) -> tuple[str, str]:
    headers = {
        "Accept": "application/vnd.github+json",
        "User-Agent": "EQ-Library-currentness/0.3",
        "X-GitHub-Api-Version": "2022-11-28",
    }
    if token:
        headers["Authorization"] = f"Bearer {token}"
    request = urllib.request.Request(AUTOEQ_API, headers=headers)
    with urllib.request.urlopen(request, timeout=30) as response:
        payload = json.loads(response.read().decode("utf-8"))
    sha = str(payload.get("sha") or "").strip()
    encoded = str(payload.get("content") or "").replace("\n", "")
    if not sha or not encoded:
        raise ValueError("AutoEQ API response is missing blob SHA or content")
    return sha, base64.b64decode(encoded).decode("utf-8")


def _write_json(path: Path, value: Any) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(value, indent=2, sort_keys=True, ensure_ascii=False) + "\n", encoding="utf-8")


def _write_health(path: Path, health: dict[str, SourceHealth], updated_at: str) -> None:
    _write_json(
        path,
        {
            "schema_version": 1,
            "updated_at": updated_at,
            "sources": [asdict(health[key]) for key in sorted(health)],
        },
    )


def refresh(
    *,
    catalog: dict[str, Any],
    registry: dict[str, Any],
    health: dict[str, SourceHealth],
    upstream_sha: str,
    upstream_text: str,
    now_iso: str,
    now_epoch: int,
) -> tuple[dict[str, Any], dict[str, SourceHealth], str]:
    health = reconcile_health(registry, health)
    autoeq_health = health["autoeq"]
    if autoeq_health.cursor == upstream_sha:
        health["autoeq"] = record_scan_success(
            autoeq_health,
            cursor=upstream_sha,
            content_fingerprint=autoeq_health.last_content_fingerprint,
            attempted_at=now_iso,
        )
        return catalog, health, "unchanged_cursor"

    parsed = parse_parametric_eq(upstream_text)
    candidate = build_candidate(
        parsed,
        manufacturer="HIFIMAN",
        model="Edition XS",
        measurement_source="oratory1990",
        target=None,
        source_url=AUTOEQ_HTML,
        source_record_id=AUTOEQ_PATH,
        source_version=f"AutoEq blob {upstream_sha}",
        discovered_at_epoch_seconds=now_epoch,
    )
    candidate["publication_eligible"] = True
    revision = candidate["revisions"][0]
    for source in revision["source_references"]:
        source["last_verified_at_epoch_seconds"] = now_epoch
    merged, outcome = merge_candidate(
        catalog,
        candidate,
        generated_at=now_iso,
        source_registry_version=str(registry["registry_version"]),
    )
    health["autoeq"] = record_scan_success(
        autoeq_health,
        cursor=upstream_sha,
        content_fingerprint=str(revision["acoustic_fingerprint"]),
        attempted_at=now_iso,
    )
    return merged, health, outcome


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--catalog", type=Path, default=Path("catalog/catalog.json"))
    parser.add_argument("--registry", type=Path, default=Path("config/source_registry.json"))
    parser.add_argument("--health", type=Path, default=Path("catalog/source_health.json"))
    parser.add_argument("--catalog-output", type=Path, required=True)
    parser.add_argument("--health-output", type=Path, required=True)
    parser.add_argument("--github-token")
    args = parser.parse_args()

    catalog = json.loads(args.catalog.read_text(encoding="utf-8"))
    registry = json.loads(args.registry.read_text(encoding="utf-8"))
    health = load_health(args.health)
    now_iso = utc_now()
    now_epoch = epoch_now()

    try:
        upstream_sha, upstream_text = fetch_upstream(args.github_token)
        updated_catalog, updated_health, outcome = refresh(
            catalog=catalog,
            registry=registry,
            health=health,
            upstream_sha=upstream_sha,
            upstream_text=upstream_text,
            now_iso=now_iso,
            now_epoch=now_epoch,
        )
    except (OSError, ValueError, KeyError, urllib.error.URLError, json.JSONDecodeError) as exc:
        reconciled = reconcile_health(registry, health)
        reconciled["autoeq"] = record_scan_failure(reconciled["autoeq"], str(exc), attempted_at=now_iso)
        _write_json(args.catalog_output, catalog)
        _write_health(args.health_output, reconciled, now_iso)
        print(f"ERROR: {exc}")
        return 1

    _write_json(args.catalog_output, updated_catalog)
    _write_health(args.health_output, updated_health, now_iso)
    print(outcome)
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
