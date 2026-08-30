#!/usr/bin/env python3
"""Ingest explicitly qualified GitHub PEQ sources into the canonical catalog.

Only repositories listed in config/qualified_github_sources.json are eligible.
Each source must use an explicitly allowed license and each profile must point to
an exact structured PEQ record. Discovery alone never reaches this adapter.
"""
from __future__ import annotations

import argparse
import base64
import hashlib
import json
import re
import time
import urllib.parse
import urllib.request
from pathlib import Path
from typing import Any

from catalog_merge import merge_candidates
from catalog_pipeline import (
    load_health,
    load_json,
    reconcile_health,
    record_scan_failure,
    record_scan_success,
)
from community_peq_ingest import build_candidate, parse_peq

FENCE_RE = re.compile(r"```(?:text|txt)?\s*\n(?P<body>.*?)\n```", re.IGNORECASE | re.DOTALL)
ALLOWED_LICENSES = {"MIT", "BSD-2-Clause", "BSD-3-Clause", "0BSD", "CC0-1.0", "CC-BY-4.0", "CC-BY-SA-4.0"}
ALLOWED_EXTRACTIONS = {"fenced_after_marker", "whole_file"}


def extract_fenced_peq(text: str, marker: str) -> str:
    marker_index = text.casefold().find(marker.casefold())
    if marker_index < 0:
        raise ValueError(f"marker not found: {marker}")
    tail = text[marker_index:]
    match = FENCE_RE.search(tail)
    if not match:
        raise ValueError(f"structured PEQ code block not found after marker: {marker}")
    return match.group("body").strip()


def extract_profile_peq(text: str, profile: dict[str, Any]) -> str:
    extraction = str(profile.get("extraction") or "fenced_after_marker").strip()
    if extraction == "whole_file":
        rendered = text.strip()
        if not rendered:
            raise ValueError("qualified whole-file PEQ source is empty")
        return rendered
    if extraction == "fenced_after_marker":
        marker = str(profile.get("marker") or "").strip()
        if not marker:
            raise ValueError("fenced_after_marker extraction requires marker")
        return extract_fenced_peq(text, marker)
    raise ValueError(f"unsupported qualified GitHub extraction: {extraction}")


def github_contents(repository: str, path: str, ref: str, token: str | None = None) -> tuple[str, str]:
    quoted_path = "/".join(urllib.parse.quote(segment, safe="") for segment in path.split("/"))
    url = f"https://api.github.com/repos/{repository}/contents/{quoted_path}?ref={urllib.parse.quote(ref, safe='')}"
    headers = {
        "Accept": "application/vnd.github+json",
        "User-Agent": "EQ-Library-currentness/0.3",
        "X-GitHub-Api-Version": "2022-11-28",
    }
    if token:
        headers["Authorization"] = "Bearer " + token
    request = urllib.request.Request(url, headers=headers)
    with urllib.request.urlopen(request, timeout=30) as response:
        payload = json.loads(response.read().decode("utf-8"))
    encoded = str(payload.get("content") or "").replace("\n", "")
    if not encoded:
        raise ValueError(f"GitHub contents response missing content for {repository}/{path}")
    return base64.b64decode(encoded).decode("utf-8"), str(payload.get("sha") or "")


def validate_manifest(manifest: dict[str, Any]) -> None:
    if manifest.get("schema_version") != 1:
        raise ValueError("qualified GitHub manifest schema_version must be 1")
    seen_source_ids: set[str] = set()
    for source in manifest.get("sources", []):
        source_id = str(source.get("id") or "").strip()
        if not source_id:
            raise ValueError("qualified GitHub source id is required")
        if source_id in seen_source_ids:
            raise ValueError(f"duplicate qualified GitHub source id: {source_id}")
        seen_source_ids.add(source_id)
        license_spdx = str(source.get("license_spdx") or "").strip()
        if license_spdx not in ALLOWED_LICENSES:
            raise ValueError(f"{source_id}: license is not on the explicit allow-list: {license_spdx}")
        if not str(source.get("license_url") or "").startswith("https://"):
            raise ValueError(f"{source_id}: license_url must be https")
        if not str(source.get("repository") or "").strip() or not str(source.get("creator") or "").strip():
            raise ValueError(f"{source_id}: repository and creator are required")
        if not source.get("profiles"):
            raise ValueError(f"{source_id}: at least one profile is required")
        for profile in source.get("profiles", []):
            extraction = str(profile.get("extraction") or "fenced_after_marker").strip()
            if extraction not in ALLOWED_EXTRACTIONS:
                raise ValueError(f"{source_id}: unsupported extraction: {extraction}")
            if extraction == "fenced_after_marker" and not str(profile.get("marker") or "").strip():
                raise ValueError(f"{source_id}: fenced_after_marker profile requires marker")
            required = ("source_path", "manufacturer", "model", "tuning_label", "source_url", "source_record_id")
            missing = [key for key in required if not str(profile.get(key) or "").strip()]
            if missing:
                raise ValueError(f"{source_id}: profile missing required fields: {', '.join(missing)}")
            aliases = profile.get("model_aliases") or []
            if not isinstance(aliases, list) or any(not str(alias).strip() for alias in aliases):
                raise ValueError(f"{source_id}: model_aliases must be a list of non-empty strings")


def candidate_from_text(
    source: dict[str, Any],
    profile: dict[str, Any],
    text: str,
    blob_sha: str,
    discovered_at_epoch_seconds: int | None,
) -> dict[str, Any]:
    peq_text = extract_profile_peq(text, profile)
    parsed = parse_peq(peq_text)
    candidate = build_candidate(
        parsed,
        manufacturer=str(profile["manufacturer"]),
        model=str(profile["model"]),
        variant=profile.get("variant"),
        creator=str(source["creator"]),
        tuning_label=str(profile["tuning_label"]),
        target=profile.get("target"),
        source_id=str(source["id"]),
        source_kind="community_repository",
        source_url=str(profile["source_url"]),
        source_record_id=str(profile["source_record_id"]),
        redistribution_policy="structured-data-only",
        source_version=f"GitHub blob {blob_sha}",
        discovered_at_epoch_seconds=discovered_at_epoch_seconds,
    )
    aliases = [str(alias).strip() for alias in profile.get("model_aliases") or [] if str(alias).strip()]
    if aliases:
        candidate["headphone"]["model_aliases"] = list(dict.fromkeys(aliases))
    return candidate


def refresh(
    catalog: dict[str, Any],
    registry: dict[str, Any],
    health_path: Path,
    manifest: dict[str, Any],
    *,
    github_token: str | None = None,
    now_epoch: int | None = None,
) -> tuple[dict[str, Any], dict[str, Any], list[dict[str, Any]]]:
    validate_manifest(manifest)
    now_epoch = now_epoch or int(time.time())
    health = reconcile_health(registry, load_health(health_path))
    source_by_id = {item["id"]: item for item in registry.get("sources", [])}
    result_catalog = catalog
    outcomes: list[dict[str, Any]] = []

    for source in manifest.get("sources", []):
        source_id = str(source["id"])
        source_registry = source_by_id.get(source_id)
        if source_registry is None:
            raise ValueError(f"qualified GitHub source missing from registry: {source_id}")
        if source_registry.get("lifecycle") != "active" or source_registry.get("redistribution") != "structured-data-only":
            raise ValueError(f"qualified GitHub source is not active/structured-data-only: {source_id}")
        try:
            file_cache: dict[str, tuple[str, str]] = {}
            cursor_parts: list[str] = []
            prepared: list[tuple[dict[str, Any], str, str]] = []
            for profile in source.get("profiles", []):
                path = str(profile["source_path"])
                if path not in file_cache:
                    file_cache[path] = github_contents(
                        str(source["repository"]),
                        path,
                        str(source.get("branch") or "main"),
                        github_token,
                    )
                text, blob_sha = file_cache[path]
                cursor_parts.append(f"{path}:{blob_sha}")
                prepared.append((profile, text, blob_sha))

            cursor = hashlib.sha256("|".join(sorted(cursor_parts)).encode("utf-8")).hexdigest()
            source_changed = health[source_id].cursor != cursor
            candidates: list[dict[str, Any]] = []
            fingerprints: list[str] = []
            for profile, text, blob_sha in prepared:
                candidate = candidate_from_text(
                    source,
                    profile,
                    text,
                    blob_sha,
                    now_epoch if source_changed else None,
                )
                candidates.append(candidate)
                fingerprints.append(candidate["revisions"][0]["acoustic_fingerprint"])

            result_catalog, merge_outcomes = merge_candidates(
                result_catalog,
                candidates,
                source_registry_version=str(registry.get("registry_version") or ""),
            )
            content_fingerprint = hashlib.sha256("|".join(sorted(fingerprints)).encode("utf-8")).hexdigest()
            health[source_id] = record_scan_success(
                health[source_id],
                cursor=cursor,
                content_fingerprint=content_fingerprint,
            )
            outcomes.append(
                {
                    "source_id": source_id,
                    "status": "ok",
                    "source_changed": source_changed,
                    "outcomes": merge_outcomes,
                }
            )
        except Exception as exc:  # source degradation must preserve the last-known-good catalog
            health[source_id] = record_scan_failure(health[source_id], str(exc))
            outcomes.append({"source_id": source_id, "status": "degraded", "error": str(exc)[:500]})

    health_payload = {
        "schema_version": 1,
        "sources": [health[key].__dict__ for key in sorted(health)],
    }
    return result_catalog, health_payload, outcomes


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--catalog", type=Path, required=True)
    parser.add_argument("--registry", type=Path, default=Path("config/source_registry.json"))
    parser.add_argument("--health", type=Path, default=Path("catalog/source_health.json"))
    parser.add_argument("--manifest", type=Path, default=Path("config/qualified_github_sources.json"))
    parser.add_argument("--catalog-output", type=Path, required=True)
    parser.add_argument("--health-output", type=Path, required=True)
    parser.add_argument("--report", type=Path)
    parser.add_argument("--github-token")
    args = parser.parse_args()

    catalog = load_json(args.catalog)
    registry = load_json(args.registry)
    manifest = load_json(args.manifest)
    refreshed, health_payload, outcomes = refresh(
        catalog,
        registry,
        args.health,
        manifest,
        github_token=args.github_token,
    )
    args.catalog_output.parent.mkdir(parents=True, exist_ok=True)
    # Large catalogs stay compact through the rest of the pipeline.
    args.catalog_output.write_text(
        json.dumps(refreshed, separators=(",", ":"), sort_keys=True, ensure_ascii=False) + "\n",
        encoding="utf-8",
    )
    args.health_output.parent.mkdir(parents=True, exist_ok=True)
    args.health_output.write_text(json.dumps(health_payload, indent=2, sort_keys=True) + "\n", encoding="utf-8")
    if args.report:
        args.report.parent.mkdir(parents=True, exist_ok=True)
        args.report.write_text(json.dumps(outcomes, indent=2, sort_keys=True) + "\n", encoding="utf-8")
    print(json.dumps(outcomes, sort_keys=True))
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
