#!/usr/bin/env python3
"""Restore immutable catalog revisions that cannot be rediscovered from live upstream state.

Bulk currentness scans are intentionally monotonic, but a previously published historical
revision can be lost if a generated snapshot is replaced after upstream has moved on. This
repair layer reapplies reviewed immutable revision seeds before live ingestion. Existing
latest revisions remain latest, matching revisions only gain missing provenance, and
missing historical revisions are restored as non-latest records.
"""
from __future__ import annotations

import argparse
import copy
import json
from pathlib import Path
from typing import Any

from catalog_pipeline import validate_snapshot


def _source_key(source: dict[str, Any]) -> tuple[str, str, str]:
    return (
        str(source.get("source_id") or ""),
        str(source.get("source_record_id") or ""),
        str(source.get("url") or ""),
    )


def _merge_sources(existing: list[dict[str, Any]], seeded: list[dict[str, Any]]) -> list[dict[str, Any]]:
    merged = {_source_key(source): copy.deepcopy(source) for source in existing}
    for source in seeded:
        key = _source_key(source)
        if key in merged:
            # Live/current metadata wins when both records describe the same source.
            merged[key] = {**copy.deepcopy(source), **merged[key]}
        else:
            merged[key] = copy.deepcopy(source)
    return sorted(merged.values(), key=lambda item: (_source_key(item), not bool(item.get("is_primary"))))


def repair_history(snapshot: dict[str, Any], seeds: dict[str, Any]) -> tuple[dict[str, Any], dict[str, int]]:
    if seeds.get("schema_version") != 1:
        raise ValueError("immutable revision seeds schema_version must be 1")

    result = copy.deepcopy(snapshot)
    profile_by_id = {
        str(profile.get("canonical_profile_id") or ""): profile
        for profile in result.get("profiles", [])
        if str(profile.get("canonical_profile_id") or "")
    }
    restored = 0
    provenance_updates = 0

    for profile_seed in seeds.get("profiles", []):
        profile_id = str(profile_seed.get("canonical_profile_id") or "").strip()
        profile = profile_by_id.get(profile_id)
        if profile is None:
            raise ValueError(f"immutable history seed profile is absent from catalog: {profile_id}")

        revisions = profile.setdefault("revisions", [])
        latest_before = [revision.get("revision_id") for revision in revisions if revision.get("is_latest")]
        if len(latest_before) != 1:
            raise ValueError(f"catalog profile must have exactly one latest revision before repair: {profile_id}")

        for seeded_revision in profile_seed.get("revisions", []):
            fingerprint = str(seeded_revision.get("acoustic_fingerprint") or "").strip()
            if not fingerprint:
                raise ValueError(f"immutable history seed missing fingerprint: {profile_id}")
            matching = next(
                (revision for revision in revisions if revision.get("acoustic_fingerprint") == fingerprint),
                None,
            )
            if matching is not None:
                before = len(matching.get("source_references") or [])
                matching["source_references"] = _merge_sources(
                    list(matching.get("source_references") or []),
                    list(seeded_revision.get("source_references") or []),
                )
                if len(matching["source_references"]) > before:
                    provenance_updates += 1
                continue

            restored_revision = copy.deepcopy(seeded_revision)
            restored_revision["is_latest"] = False
            revisions.append(restored_revision)
            restored += 1

        revisions.sort(
            key=lambda revision: (
                not bool(revision.get("is_latest")),
                -(revision.get("source_updated_at_epoch_seconds") or revision.get("first_seen_at_epoch_seconds") or 0),
                str(revision.get("revision_id") or ""),
            )
        )
        latest_after = [revision.get("revision_id") for revision in revisions if revision.get("is_latest")]
        if latest_after != latest_before:
            raise ValueError(f"immutable history repair changed the latest revision: {profile_id}")

    errors = validate_snapshot(result)
    if errors:
        raise ValueError("history-repaired catalog is invalid: " + "; ".join(errors))
    return result, {"restored_revisions": restored, "provenance_updates": provenance_updates}


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--catalog", type=Path, required=True)
    parser.add_argument("--seeds", type=Path, default=Path("config/immutable_revision_seeds.json"))
    parser.add_argument("--output", type=Path, required=True)
    parser.add_argument("--report", type=Path)
    args = parser.parse_args()

    snapshot = json.loads(args.catalog.read_text(encoding="utf-8"))
    seeds = json.loads(args.seeds.read_text(encoding="utf-8"))
    repaired, report = repair_history(snapshot, seeds)
    args.output.parent.mkdir(parents=True, exist_ok=True)
    args.output.write_text(json.dumps(repaired, sort_keys=True, separators=(",", ":"), ensure_ascii=False) + "\n", encoding="utf-8")
    if args.report:
        args.report.parent.mkdir(parents=True, exist_ok=True)
        args.report.write_text(json.dumps(report, indent=2, sort_keys=True) + "\n", encoding="utf-8")
    print(json.dumps(report, sort_keys=True))
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
