#!/usr/bin/env python3
"""Merge one qualified canonical source candidate into an EQ Library snapshot.

A candidate with the same acoustic fingerprint updates provenance/verification metadata
without creating a cosmetic revision. A materially different fingerprint for the same
canonical profile becomes the new immutable latest revision and the previous latest is
retained. Distinct canonical profiles are appended. Review-only candidates are rejected.
"""

from __future__ import annotations

import argparse
import copy
import json
from datetime import datetime, timezone
from pathlib import Path
from typing import Any

from catalog_pipeline import validate_snapshot


def utc_now() -> str:
    return datetime.now(timezone.utc).replace(microsecond=0).isoformat().replace("+00:00", "Z")


def _source_key(source: dict[str, Any]) -> tuple[str, str, str]:
    return (
        str(source.get("source_id") or ""),
        str(source.get("source_record_id") or ""),
        str(source.get("url") or ""),
    )


def _merge_sources(existing: list[dict[str, Any]], incoming: list[dict[str, Any]]) -> list[dict[str, Any]]:
    merged: dict[tuple[str, str, str], dict[str, Any]] = {}
    for source in existing:
        merged[_source_key(source)] = copy.deepcopy(source)
    for source in incoming:
        key = _source_key(source)
        previous = merged.get(key, {})
        combined = {**previous, **copy.deepcopy(source)}
        for timestamp_key in (
            "published_at_epoch_seconds",
            "updated_at_epoch_seconds",
            "discovered_at_epoch_seconds",
            "last_verified_at_epoch_seconds",
        ):
            values = [value for value in (previous.get(timestamp_key), source.get(timestamp_key)) if isinstance(value, int)]
            if values:
                combined[timestamp_key] = max(values)
        merged[key] = combined
    return sorted(merged.values(), key=lambda item: (_source_key(item), not bool(item.get("is_primary"))))


def _merge_same_revision(existing: dict[str, Any], incoming: dict[str, Any]) -> dict[str, Any]:
    result = copy.deepcopy(existing)
    result["source_references"] = _merge_sources(
        list(existing.get("source_references") or []),
        list(incoming.get("source_references") or []),
    )
    for key in (
        "source_version_label",
        "sound_impact_summary",
        "source_updated_at_epoch_seconds",
        "first_seen_at_epoch_seconds",
    ):
        incoming_value = incoming.get(key)
        if incoming_value is not None and incoming_value != "":
            if key == "first_seen_at_epoch_seconds" and isinstance(result.get(key), int) and isinstance(incoming_value, int):
                result[key] = min(result[key], incoming_value)
            else:
                result[key] = incoming_value
    result["is_latest"] = True
    return result


def merge_candidate(
    snapshot: dict[str, Any],
    candidate: dict[str, Any],
    *,
    generated_at: str | None = None,
    source_registry_version: str | None = None,
) -> tuple[dict[str, Any], str]:
    if candidate.get("publication_eligible") is False:
        raise ValueError("review-only candidate cannot be merged into the published catalog")

    clean_candidate = copy.deepcopy(candidate)
    clean_candidate.pop("publication_eligible", None)
    profile_id = str(clean_candidate.get("canonical_profile_id") or "").strip()
    revisions = clean_candidate.get("revisions")
    if not profile_id or not isinstance(revisions, list) or len(revisions) != 1:
        raise ValueError("candidate must contain one canonical profile revision")
    incoming_revision = revisions[0]
    incoming_fingerprint = str(incoming_revision.get("acoustic_fingerprint") or "").strip()
    if not incoming_fingerprint or not incoming_revision.get("filters"):
        raise ValueError("candidate revision must contain a fingerprint and filters")

    result = copy.deepcopy(snapshot)
    profiles = result.setdefault("profiles", [])
    existing_profile = next((profile for profile in profiles if profile.get("canonical_profile_id") == profile_id), None)

    if existing_profile is None:
        incoming_revision["is_latest"] = True
        profiles.append(clean_candidate)
        outcome = "new_profile"
    else:
        existing_revisions = existing_profile.setdefault("revisions", [])
        matching = next(
            (revision for revision in existing_revisions if revision.get("acoustic_fingerprint") == incoming_fingerprint),
            None,
        )
        for key in ("headphone", "creator", "target", "tuning_label"):
            if key in clean_candidate:
                existing_profile[key] = clean_candidate[key]
        if matching is not None:
            for revision in existing_revisions:
                revision["is_latest"] = revision is matching
            merged_revision = _merge_same_revision(matching, incoming_revision)
            existing_revisions[existing_revisions.index(matching)] = merged_revision
            outcome = "metadata_update"
        else:
            for revision in existing_revisions:
                revision["is_latest"] = False
            incoming_revision["is_latest"] = True
            existing_revisions.append(incoming_revision)
            outcome = "new_revision"

        existing_revisions.sort(
            key=lambda revision: (
                not bool(revision.get("is_latest")),
                -(revision.get("source_updated_at_epoch_seconds") or revision.get("first_seen_at_epoch_seconds") or 0),
                str(revision.get("revision_id") or ""),
            )
        )

    profiles.sort(key=lambda profile: str(profile.get("canonical_profile_id") or ""))
    result["generated_at"] = generated_at or utc_now()
    if source_registry_version:
        result["source_registry_version"] = source_registry_version

    errors = validate_snapshot(result)
    if errors:
        raise ValueError("merged catalog is invalid: " + "; ".join(errors))
    return result, outcome


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--catalog", type=Path, required=True)
    parser.add_argument("--candidate", type=Path, required=True)
    parser.add_argument("--output", type=Path, required=True)
    parser.add_argument("--generated-at")
    parser.add_argument("--source-registry-version")
    args = parser.parse_args()

    snapshot = json.loads(args.catalog.read_text(encoding="utf-8"))
    candidate = json.loads(args.candidate.read_text(encoding="utf-8"))
    merged, outcome = merge_candidate(
        snapshot,
        candidate,
        generated_at=args.generated_at,
        source_registry_version=args.source_registry_version,
    )
    args.output.parent.mkdir(parents=True, exist_ok=True)
    args.output.write_text(json.dumps(merged, indent=2, ensure_ascii=False) + "\n", encoding="utf-8")
    print(outcome)
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
