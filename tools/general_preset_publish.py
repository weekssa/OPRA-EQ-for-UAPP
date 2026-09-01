#!/usr/bin/env python3
"""Publish qualified source-authored General EQ manifests into the canonical catalog."""

from __future__ import annotations

import argparse
import json
from pathlib import Path
from typing import Any

from catalog_merge import merge_candidates
from community_peq_ingest import parse_peq
from general_preset_ingest import build_candidate


def _registry_source(registry: dict[str, Any], source_id: str) -> dict[str, Any]:
    for source in registry.get("sources") or []:
        if str(source.get("id") or "") == source_id:
            return source
    raise ValueError(f"General preset source missing from registry: {source_id}")


def manifest_candidates(manifest: dict[str, Any], registry: dict[str, Any]) -> list[dict[str, Any]]:
    source_id = str(manifest.get("source_id") or "").strip()
    source = _registry_source(registry, source_id)
    if source.get("lifecycle") != "active":
        raise ValueError(f"General preset source is not active: {source_id}")
    if source.get("redistribution") != "structured-data-only":
        raise ValueError(f"General preset source is not publication-qualified: {source_id}")

    source_kind = str(manifest.get("source_kind") or source.get("kind") or "").strip()
    if source_kind != str(source.get("kind") or "").strip():
        raise ValueError(f"General preset source kind does not match registry: {source_id}")

    creator = str(manifest.get("creator") or "").strip()
    source_url = str(manifest.get("source_url") or "").strip()
    source_version = str(manifest.get("source_version") or "").strip() or None
    verification_status = str(manifest.get("verification_status") or "verified").strip().lower()
    discovered_at = manifest.get("discovered_at_epoch_seconds")
    presets = manifest.get("presets") or []
    if not presets:
        raise ValueError("General preset manifest contains no presets")

    candidates: list[dict[str, Any]] = []
    seen_record_ids: set[str] = set()
    for preset in presets:
        record_id = str(preset.get("source_record_id") or "").strip()
        if not record_id or record_id in seen_record_ids:
            raise ValueError("Every General preset needs a unique source_record_id")
        seen_record_ids.add(record_id)
        candidate = build_candidate(
            parse_peq(str(preset.get("peq_text") or "")),
            purpose=str(preset.get("purpose") or ""),
            creator=str(preset.get("creator") or creator),
            tuning_label=str(preset.get("tuning_label") or ""),
            source_id=source_id,
            source_kind=source_kind,
            source_url=str(preset.get("source_url") or source_url),
            source_record_id=record_id,
            redistribution_policy="structured-data-only",
            source_version=str(preset.get("source_version") or source_version or "") or None,
            discovered_at_epoch_seconds=discovered_at if isinstance(discovered_at, int) else None,
            verification_status=str(preset.get("verification_status") or verification_status),
        )
        summary = str(preset.get("sound_impact_summary") or "").strip()
        if summary:
            candidate["revisions"][0]["sound_impact_summary"] = summary
        candidates.append(candidate)
    return candidates


def publish_manifest(
    snapshot: dict[str, Any],
    manifest: dict[str, Any],
    registry: dict[str, Any],
) -> tuple[dict[str, Any], dict[str, int]]:
    generated_at = max(
        str(snapshot.get("generated_at") or ""),
        str(manifest.get("catalog_generated_at") or ""),
    ) or None
    return merge_candidates(
        snapshot,
        manifest_candidates(manifest, registry),
        generated_at=generated_at,
        source_registry_version=str(registry.get("registry_version") or "") or None,
    )


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--catalog", type=Path, required=True)
    parser.add_argument("--manifest", type=Path, required=True)
    parser.add_argument("--registry", type=Path, required=True)
    parser.add_argument("--output", type=Path, required=True)
    parser.add_argument("--report", type=Path)
    args = parser.parse_args()

    snapshot = json.loads(args.catalog.read_text(encoding="utf-8"))
    manifest = json.loads(args.manifest.read_text(encoding="utf-8"))
    registry = json.loads(args.registry.read_text(encoding="utf-8"))
    merged, outcomes = publish_manifest(snapshot, manifest, registry)
    args.output.parent.mkdir(parents=True, exist_ok=True)
    args.output.write_text(json.dumps(merged, indent=2, ensure_ascii=False) + "\n", encoding="utf-8")

    report = {
        "source_id": manifest.get("source_id"),
        "candidate_count": len(manifest.get("presets") or []),
        "outcomes": outcomes,
    }
    if args.report:
        args.report.parent.mkdir(parents=True, exist_ok=True)
        args.report.write_text(json.dumps(report, indent=2, sort_keys=True) + "\n", encoding="utf-8")
    print(json.dumps(report, sort_keys=True))
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
