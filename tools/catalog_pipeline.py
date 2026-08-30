#!/usr/bin/env python3
"""EQ Library v0.3 external catalog currentness pipeline.

The Android app consumes only validated published snapshots. This module owns the
source-registry state machine, incremental scan bookkeeping, revision detection,
and atomic last-known-good publication primitives used by source-specific jobs.
It intentionally uses only the Python standard library so GitHub Actions can run
it without a dependency bootstrap.
"""

from __future__ import annotations

import argparse
import hashlib
import json
import os
import tempfile
import unicodedata
from dataclasses import dataclass, asdict
from datetime import datetime, timezone
from pathlib import Path
from typing import Any, Iterable

from acoustic_fingerprint import acoustic_fingerprint as canonical_acoustic_fingerprint

VALID_LIFECYCLES = {"proposed", "reviewing", "active", "link-only", "paused", "retired"}
VALID_REDISTRIBUTION = {"allowed", "structured-data-only", "link-only", "review-required"}
VALID_CADENCES = {"hourly", "daily", "weekly", "monthly", "manual"}
VALID_PROFILE_SCOPES = {"headphone", "general"}
HEADPHONE_PRESET_PURPOSES = {"correction_tuning", "personal_community"}
GENERAL_PRESET_PURPOSES = {"effect", "genre"}
VALID_PRESET_PURPOSES = HEADPHONE_PRESET_PURPOSES | GENERAL_PRESET_PURPOSES


def utc_now() -> str:
    return datetime.now(timezone.utc).replace(microsecond=0).isoformat().replace("+00:00", "Z")


def stable_json(value: Any) -> str:
    return json.dumps(value, sort_keys=True, separators=(",", ":"), ensure_ascii=False)


def sha256_json(value: Any) -> str:
    return hashlib.sha256(stable_json(value).encode("utf-8")).hexdigest()


def normalize_identity(value: Any) -> str:
    text = unicodedata.normalize("NFKC", str(value or "")).casefold()
    return "".join(ch for ch in text if ch.isalnum())


@dataclass(frozen=True)
class SourceHealth:
    source_id: str
    lifecycle: str
    last_successful_scan_at: str | None = None
    last_attempt_at: str | None = None
    cursor: str | None = None
    parser_version: str | None = None
    consecutive_failures: int = 0
    last_error: str | None = None
    last_content_fingerprint: str | None = None
    last_terms_review_at: str | None = None


class RegistryError(ValueError):
    pass


def load_json(path: Path) -> Any:
    with path.open("r", encoding="utf-8") as handle:
        return json.load(handle)


def validate_registry(registry: dict[str, Any]) -> list[str]:
    errors: list[str] = []
    if registry.get("schema_version") != 1:
        errors.append("schema_version must be 1")
    if not str(registry.get("registry_version", "")).strip():
        errors.append("registry_version is required")
    sources = registry.get("sources")
    if not isinstance(sources, list) or not sources:
        errors.append("sources must be a non-empty list")
        return errors

    seen: set[str] = set()
    required = {
        "id", "kind", "name", "scope", "lifecycle", "cadence", "parser",
        "parser_version", "cursor_strategy", "redistribution", "attribution_required",
    }
    for index, source in enumerate(sources):
        prefix = f"sources[{index}]"
        if not isinstance(source, dict):
            errors.append(f"{prefix} must be an object")
            continue
        missing = sorted(field for field in required if field not in source)
        if missing:
            errors.append(f"{prefix} missing: {', '.join(missing)}")
        source_id = str(source.get("id", "")).strip()
        if not source_id:
            errors.append(f"{prefix}.id must be non-empty")
        elif source_id in seen:
            errors.append(f"duplicate source id: {source_id}")
        seen.add(source_id)
        if source.get("lifecycle") not in VALID_LIFECYCLES:
            errors.append(f"{prefix}.lifecycle is invalid: {source.get('lifecycle')}")
        if source.get("redistribution") not in VALID_REDISTRIBUTION:
            errors.append(f"{prefix}.redistribution is invalid: {source.get('redistribution')}")
        if source.get("cadence") not in VALID_CADENCES:
            errors.append(f"{prefix}.cadence is invalid: {source.get('cadence')}")
        if source.get("attribution_required") not in (True, False):
            errors.append(f"{prefix}.attribution_required must be boolean")
    return errors


def load_health(path: Path) -> dict[str, SourceHealth]:
    if not path.exists():
        return {}
    raw = load_json(path)
    items = raw.get("sources", []) if isinstance(raw, dict) else []
    return {item["source_id"]: SourceHealth(**item) for item in items}


def write_health(path: Path, health: dict[str, SourceHealth]) -> None:
    payload = {
        "schema_version": 1,
        "updated_at": utc_now(),
        "sources": [asdict(health[key]) for key in sorted(health)],
    }
    atomic_write_json(path, payload)


def reconcile_health(registry: dict[str, Any], current: dict[str, SourceHealth]) -> dict[str, SourceHealth]:
    reconciled: dict[str, SourceHealth] = {}
    for source in registry["sources"]:
        source_id = source["id"]
        previous = current.get(source_id)
        if previous is None:
            previous = SourceHealth(source_id=source_id, lifecycle=source["lifecycle"])
        reconciled[source_id] = SourceHealth(
            **{
                **asdict(previous),
                "lifecycle": source["lifecycle"],
                "parser_version": str(source["parser_version"]),
            }
        )
    return reconciled


def record_scan_success(
    health: SourceHealth,
    *,
    cursor: str | None,
    content_fingerprint: str | None,
    attempted_at: str | None = None,
) -> SourceHealth:
    when = attempted_at or utc_now()
    return SourceHealth(
        **{
            **asdict(health),
            "last_attempt_at": when,
            "last_successful_scan_at": when,
            "cursor": cursor,
            "consecutive_failures": 0,
            "last_error": None,
            "last_content_fingerprint": content_fingerprint,
        }
    )


def record_scan_failure(health: SourceHealth, error: str, *, attempted_at: str | None = None) -> SourceHealth:
    return SourceHealth(
        **{
            **asdict(health),
            "last_attempt_at": attempted_at or utc_now(),
            "consecutive_failures": health.consecutive_failures + 1,
            "last_error": error[:1000],
        }
    )


def acoustic_fingerprint(preamp_db: float | None, filters: Iterable[dict[str, Any]]) -> str:
    """Canonical fingerprint shared with Android and all source adapters."""
    return canonical_acoustic_fingerprint(preamp_db, filters)


def classify_candidate(previous_fingerprints: Iterable[str], new_fingerprint: str) -> str:
    previous = set(previous_fingerprints)
    return "duplicate" if new_fingerprint in previous else ("new_candidate" if not previous else "new_revision")


def validate_headphone_aliases(snapshot: dict[str, Any]) -> list[str]:
    errors: list[str] = []
    groups = snapshot.get("headphone_aliases", [])
    if not isinstance(groups, list):
        return ["snapshot headphone_aliases must be a list"]

    seen_groups: set[tuple[str, str]] = set()
    for index, group in enumerate(groups):
        prefix = f"headphone_aliases[{index}]"
        if not isinstance(group, dict):
            errors.append(f"{prefix} must be an object")
            continue
        manufacturer = str(group.get("manufacturer") or "").strip()
        canonical = str(group.get("canonical_model") or "").strip()
        aliases = group.get("aliases")
        evidence = group.get("evidence")
        if not manufacturer or not canonical:
            errors.append(f"{prefix} requires manufacturer and canonical_model")
            continue
        group_key = (normalize_identity(manufacturer), normalize_identity(canonical))
        if not all(group_key):
            errors.append(f"{prefix} has an invalid normalized identity")
        elif group_key in seen_groups:
            errors.append(f"duplicate headphone alias group: {manufacturer} / {canonical}")
        seen_groups.add(group_key)

        if not isinstance(aliases, list) or not aliases:
            errors.append(f"{prefix}.aliases must be a non-empty list")
        else:
            alias_keys: list[str] = []
            for alias in aliases:
                alias_key = normalize_identity(alias)
                if not str(alias or "").strip() or not alias_key:
                    errors.append(f"{prefix}.aliases contains a blank or invalid alias")
                else:
                    alias_keys.append(alias_key)
            if len(alias_keys) != len(set(alias_keys)):
                errors.append(f"{prefix}.aliases contains normalized duplicates")

        if not isinstance(evidence, list) or not evidence:
            errors.append(f"{prefix}.evidence must be a non-empty list")
        elif any(not str(item or "").strip() for item in evidence):
            errors.append(f"{prefix}.evidence contains a blank value")
    return errors


def validate_profile_classification(profile: dict[str, Any], prefix: str) -> list[str]:
    """Validate backward-compatible scope/purpose metadata without changing acoustic data."""
    errors: list[str] = []
    scope = str(profile.get("scope") or "headphone").strip()
    purpose = str(profile.get("purpose") or "correction_tuning").strip()
    if scope not in VALID_PROFILE_SCOPES:
        errors.append(f"{prefix}.scope is invalid: {scope}")
        return errors
    if purpose not in VALID_PRESET_PURPOSES:
        errors.append(f"{prefix}.purpose is invalid: {purpose}")
        return errors

    headphone = profile.get("headphone")
    if scope == "headphone":
        if not isinstance(headphone, dict):
            errors.append(f"{prefix} headphone scope requires headphone identity")
        else:
            manufacturer = str(headphone.get("manufacturer") or "").strip()
            model = str(headphone.get("model") or "").strip()
            if not manufacturer or not model:
                errors.append(f"{prefix} headphone identity requires manufacturer and model")
        if purpose not in HEADPHONE_PRESET_PURPOSES:
            errors.append(f"{prefix} {purpose} presets must use general scope")
    else:
        if headphone not in (None, {}):
            errors.append(f"{prefix} general scope must not require headphone identity")
        if purpose not in GENERAL_PRESET_PURPOSES:
            errors.append(f"{prefix} general scope cannot use {purpose} purpose")

    return errors


def validate_snapshot(snapshot: dict[str, Any]) -> list[str]:
    errors: list[str] = []
    if int(snapshot.get("schema_version", 0)) < 1:
        errors.append("snapshot schema_version must be >= 1")
    if not str(snapshot.get("generated_at", "")).strip():
        errors.append("snapshot generated_at is required")
    if not str(snapshot.get("source_registry_version", "")).strip():
        errors.append("snapshot source_registry_version is required")
    errors.extend(validate_headphone_aliases(snapshot))
    profiles = snapshot.get("profiles")
    if not isinstance(profiles, list):
        errors.append("snapshot profiles must be a list")
        return errors
    ids: set[str] = set()
    for index, profile in enumerate(profiles):
        profile_id = str(profile.get("canonical_profile_id", "")).strip()
        profile_prefix = f"profiles[{index}]"
        if not profile_id:
            errors.append(f"{profile_prefix} missing canonical_profile_id")
        elif profile_id in ids:
            errors.append(f"duplicate canonical_profile_id: {profile_id}")
        ids.add(profile_id)
        errors.extend(validate_profile_classification(profile, profile_id or profile_prefix))
        revisions = profile.get("revisions", [])
        if not revisions:
            errors.append(f"{profile_id or index} must have at least one revision")
            continue
        latest = [r for r in revisions if r.get("is_latest") is True]
        if len(latest) != 1:
            errors.append(f"{profile_id or index} must have exactly one latest revision")
        revision_ids: set[str] = set()
        for revision in revisions:
            revision_id = str(revision.get("revision_id", "")).strip()
            if not revision_id:
                errors.append(f"{profile_id or index} has revision without revision_id")
            elif revision_id in revision_ids:
                errors.append(f"{profile_id or index} duplicate revision_id: {revision_id}")
            revision_ids.add(revision_id)
            if not str(revision.get("acoustic_fingerprint", "")).strip():
                errors.append(f"{profile_id or index}/{revision_id or '?'} missing acoustic_fingerprint")
            if not revision.get("filters"):
                errors.append(f"{profile_id or index}/{revision_id or '?'} must contain filters")
            if not revision.get("source_references"):
                errors.append(f"{profile_id or index}/{revision_id or '?'} must contain source references")
    return errors


def atomic_write_json(path: Path, payload: Any) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    with tempfile.NamedTemporaryFile("w", encoding="utf-8", dir=path.parent, delete=False) as handle:
        json.dump(payload, handle, indent=2, sort_keys=True, ensure_ascii=False)
        handle.write("\n")
        temp_name = handle.name
    os.replace(temp_name, path)


def publish_snapshot(candidate: Path, published: Path, last_known_good: Path) -> str:
    snapshot = load_json(candidate)
    errors = validate_snapshot(snapshot)
    if errors:
        raise RegistryError("candidate snapshot rejected: " + "; ".join(errors))
    snapshot_hash = sha256_json(snapshot)
    if published.exists():
        old = load_json(published)
        if not validate_snapshot(old):
            atomic_write_json(last_known_good, old)
    atomic_write_json(published, snapshot)
    atomic_write_json(last_known_good, snapshot)
    return snapshot_hash


def command_validate(args: argparse.Namespace) -> int:
    registry = load_json(Path(args.registry))
    errors = validate_registry(registry)
    if errors:
        for error in errors:
            print(f"ERROR: {error}")
        return 1
    print(f"registry OK: {len(registry['sources'])} sources; version={registry['registry_version']}")
    return 0


def command_reconcile(args: argparse.Namespace) -> int:
    registry = load_json(Path(args.registry))
    errors = validate_registry(registry)
    if errors:
        raise RegistryError("; ".join(errors))
    state_path = Path(args.state)
    health = reconcile_health(registry, load_health(state_path))
    write_health(state_path, health)
    print(f"reconciled {len(health)} source-health records")
    return 0


def command_publish(args: argparse.Namespace) -> int:
    digest = publish_snapshot(Path(args.candidate), Path(args.published), Path(args.last_known_good))
    print(f"published validated snapshot sha256={digest}")
    return 0


def build_parser() -> argparse.ArgumentParser:
    parser = argparse.ArgumentParser(description=__doc__)
    sub = parser.add_subparsers(dest="command", required=True)

    validate = sub.add_parser("validate-registry")
    validate.add_argument("--registry", default="config/source_registry.json")
    validate.set_defaults(func=command_validate)

    reconcile = sub.add_parser("reconcile-health")
    reconcile.add_argument("--registry", default="config/source_registry.json")
    reconcile.add_argument("--state", default="catalog/source_health.json")
    reconcile.set_defaults(func=command_reconcile)

    publish = sub.add_parser("publish")
    publish.add_argument("--candidate", required=True)
    publish.add_argument("--published", default="catalog/catalog.json")
    publish.add_argument("--last-known-good", default="catalog/catalog.last-known-good.json")
    publish.set_defaults(func=command_publish)
    return parser


def main() -> int:
    args = build_parser().parse_args()
    try:
        return int(args.func(args))
    except (OSError, json.JSONDecodeError, RegistryError, ValueError, KeyError) as exc:
        print(f"ERROR: {exc}")
        return 1


if __name__ == "__main__":
    raise SystemExit(main())
