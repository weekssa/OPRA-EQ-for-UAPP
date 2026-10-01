#!/usr/bin/env python3
"""Keep Favorite resolver samples synchronized with canonical catalog source IDs."""

from __future__ import annotations

import argparse
import json
import sys
from pathlib import Path
from typing import Any


ROOT = Path(__file__).resolve().parents[1]
CATALOG_PATH = ROOT / "catalog" / "catalog.json"
REGISTRY_PATH = ROOT / "config" / "source_registry.json"
FIXTURE_PATH = ROOT / "app" / "src" / "test" / "resources" / "catalog" / "favorite-source-samples.json"

# These are explicit policy decisions, not inferred from an empty catalog search. If one of these
# sources starts publishing canonical PEQ records, remove its exclusion and add its real sample.
NO_PROFILE_EXCLUSIONS = {
    "squiglink": (
        "Measurement ecosystem and provenance monitor; it does not publish source-authored PEQ. "
        "Do not derive filters from response curves."
    ),
    "topping-community": (
        "Paused pending an authorized retrieval path; no canonical profile is currently ingested."
    ),
}

# Keep the historical catalog spelling as its own tested sample while requiring new source IDs to
# match the registry or receive a separately reviewed alias decision here.
SOURCE_ID_ALIASES = {
    "headphone-community": {
        "registry_id": "headphones-community",
        "reason": "Historical singular catalog spelling; retain sample coverage while the registry uses the canonical plural ID.",
    },
}

FIXTURE_SCHEMA_VERSION = 1
SNAPSHOT_GENERATED_AT = "fixture:favorite-source-samples"
SNAPSHOT_REGISTRY_VERSION = "fixture:favorite-source-samples-v1"


class CoverageError(ValueError):
    """Raised when the candidate catalog, registry, and checked-in samples disagree."""


def _require_dict(value: Any, where: str) -> dict[str, Any]:
    if not isinstance(value, dict):
        raise CoverageError(f"{where} must be an object")
    return value


def _source_registry_ids(registry: dict[str, Any]) -> set[str]:
    sources = registry.get("sources")
    if not isinstance(sources, list):
        raise CoverageError("source registry must contain a sources list")
    ids: list[str] = []
    for index, source_value in enumerate(sources):
        source = _require_dict(source_value, f"source registry sources[{index}]")
        source_id = source.get("id")
        if not isinstance(source_id, str) or not source_id.strip():
            raise CoverageError(f"source registry sources[{index}] has no nonblank id")
        ids.append(source_id)
    if len(ids) != len(set(ids)):
        raise CoverageError("source registry contains duplicate source IDs")
    return set(ids)


def _catalog_candidates(catalog: dict[str, Any]) -> dict[str, list[tuple[dict[str, Any], dict[str, Any], dict[str, Any]]]]:
    profiles = catalog.get("profiles")
    if not isinstance(profiles, list):
        raise CoverageError("canonical catalog must contain a profiles list")

    candidates: dict[str, list[tuple[dict[str, Any], dict[str, Any], dict[str, Any]]]] = {}
    for profile_index, profile_value in enumerate(profiles):
        profile = _require_dict(profile_value, f"catalog profiles[{profile_index}]")
        profile_id = profile.get("canonical_profile_id")
        if not isinstance(profile_id, str) or not profile_id.strip():
            raise CoverageError(f"catalog profiles[{profile_index}] has no canonical_profile_id")
        revisions = profile.get("revisions")
        if not isinstance(revisions, list):
            raise CoverageError(f"catalog profile {profile_id} has no revisions list")
        for revision_index, revision_value in enumerate(revisions):
            revision = _require_dict(revision_value, f"profile {profile_id} revisions[{revision_index}]")
            revision_id = revision.get("revision_id")
            if not isinstance(revision_id, str) or not revision_id.strip():
                raise CoverageError(f"catalog profile {profile_id} has a revision without revision_id")
            references = revision.get("source_references")
            if not isinstance(references, list):
                raise CoverageError(f"catalog revision {revision_id} has no source_references list")
            for reference_index, reference_value in enumerate(references):
                reference = _require_dict(
                    reference_value,
                    f"profile {profile_id} revision {revision_id} source_references[{reference_index}]",
                )
                source_id = reference.get("source_id")
                if not isinstance(source_id, str) or not source_id.strip():
                    raise CoverageError(f"catalog revision {revision_id} has a source reference without source_id")
                candidates.setdefault(source_id, []).append((profile, revision, reference))
    return candidates


def _candidate_sort_key(
    candidate: tuple[dict[str, Any], dict[str, Any], dict[str, Any]],
) -> tuple[Any, ...]:
    profile, revision, reference = candidate
    recency = revision.get("source_updated_at_epoch_seconds")
    if recency is None:
        recency = revision.get("first_seen_at_epoch_seconds")
    if not isinstance(recency, int):
        recency = 0
    return (
        not bool(revision.get("is_latest")),
        not bool(reference.get("is_primary")),
        -recency,
        profile["canonical_profile_id"],
        revision["revision_id"],
        reference.get("source_record_id") or "",
    )


def build_expected_fixture(catalog: dict[str, Any], registry: dict[str, Any]) -> dict[str, Any]:
    registry_ids = _source_registry_ids(registry)
    candidates = _catalog_candidates(catalog)
    catalog_source_ids = set(candidates)

    stale_aliases = set(SOURCE_ID_ALIASES) - catalog_source_ids
    if stale_aliases:
        raise CoverageError(f"source ID alias policy is stale for: {', '.join(sorted(stale_aliases))}")
    for source_id, alias in SOURCE_ID_ALIASES.items():
        target = alias["registry_id"]
        if target not in registry_ids:
            raise CoverageError(f"source ID alias {source_id} targets unregistered ID {target}")

    unregistered_ids = catalog_source_ids - registry_ids - set(SOURCE_ID_ALIASES)
    if unregistered_ids:
        raise CoverageError(
            "catalog source IDs are missing from the registry or explicit alias policy: "
            + ", ".join(sorted(unregistered_ids))
        )

    missing_profile_ids = registry_ids - catalog_source_ids
    expected_exclusion_ids = set(NO_PROFILE_EXCLUSIONS)
    if missing_profile_ids != expected_exclusion_ids:
        unexpected = missing_profile_ids - expected_exclusion_ids
        resolved = expected_exclusion_ids - missing_profile_ids
        details = []
        if unexpected:
            details.append("new registry IDs need a real catalog sample or reviewed exclusion: " + ", ".join(sorted(unexpected)))
        if resolved:
            details.append("remove no-longer-needed exclusions: " + ", ".join(sorted(resolved)))
        raise CoverageError("; ".join(details) or "registered source/profile coverage is inconsistent")

    samples: list[dict[str, Any]] = []
    profile_by_id: dict[str, dict[str, Any]] = {}
    for source_id in sorted(catalog_source_ids):
        profile, revision, reference = sorted(candidates[source_id], key=_candidate_sort_key)[0]
        # Earlier catalog revisions omitted the scope field on headphone profiles; a present
        # headphone identity is the established representation for that backward-compatible case.
        scope = profile.get("scope")
        if scope is None and profile.get("headphone") is not None:
            scope = "headphone"
        if scope not in {"headphone", "general"}:
            raise CoverageError(f"sample {source_id} has unsupported profile scope {scope!r}")
        samples.append(
            {
                "source_id": source_id,
                "canonical_profile_id": profile["canonical_profile_id"],
                "revision_id": revision["revision_id"],
                "source_record_id": reference.get("source_record_id"),
                "source_kind": reference.get("source_kind"),
                "scope": scope,
                "is_latest_revision": bool(revision.get("is_latest")),
                "is_primary_reference": bool(reference.get("is_primary")),
            }
        )
        profile_by_id[profile["canonical_profile_id"]] = profile

    excluded = [
        {"source_id": source_id, "reason": NO_PROFILE_EXCLUSIONS[source_id]}
        for source_id in sorted(NO_PROFILE_EXCLUSIONS)
    ]
    return {
        "schema_version": FIXTURE_SCHEMA_VERSION,
        "samples": samples,
        "excluded_registered_sources": excluded,
        "snapshot": {
            "schema_version": catalog.get("schema_version", 1),
            "generated_at": SNAPSHOT_GENERATED_AT,
            "source_registry_version": SNAPSHOT_REGISTRY_VERSION,
            "profiles": [profile_by_id[profile_id] for profile_id in sorted(profile_by_id)],
            "sources": [],
            "headphone_aliases": [],
        },
    }


def validate_fixture(actual: dict[str, Any], expected: dict[str, Any]) -> None:
    if actual.get("schema_version") != expected["schema_version"]:
        raise CoverageError("Favorite source sample fixture schema_version is stale")

    actual_samples = actual.get("samples")
    expected_samples = expected["samples"]
    if not isinstance(actual_samples, list):
        raise CoverageError("Favorite source sample fixture must contain a samples list")
    actual_ids = {row.get("source_id") for row in actual_samples if isinstance(row, dict)}
    expected_ids = {row["source_id"] for row in expected_samples}
    missing = expected_ids - actual_ids
    extra = actual_ids - expected_ids
    if missing or extra:
        details = []
        if missing:
            details.append("missing samples: " + ", ".join(sorted(missing)))
        if extra:
            details.append("stale samples: " + ", ".join(sorted(str(item) for item in extra)))
        raise CoverageError("Favorite source sample IDs do not match the canonical catalog: " + "; ".join(details))
    if actual_samples != expected_samples:
        raise CoverageError("Favorite source sample profile/revision/source-reference identities are stale")

    if actual.get("excluded_registered_sources") != expected["excluded_registered_sources"]:
        raise CoverageError("registered sources without canonical PEQ must retain the reviewed exclusion list and reasons")

    actual_snapshot = actual.get("snapshot")
    if actual_snapshot != expected["snapshot"]:
        raise CoverageError("Favorite source sample canonical profile data is stale; regenerate from catalog/catalog.json")


def _read_json(path: Path) -> dict[str, Any]:
    try:
        return _require_dict(json.loads(path.read_text(encoding="utf-8")), str(path))
    except OSError as exc:
        raise CoverageError(f"cannot read {path}: {exc}") from exc
    except json.JSONDecodeError as exc:
        raise CoverageError(f"invalid JSON in {path}: {exc}") from exc


def _write_fixture(path: Path, fixture: dict[str, Any]) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(fixture, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")


def main(argv: list[str] | None = None) -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    mode = parser.add_mutually_exclusive_group(required=True)
    mode.add_argument("--check", action="store_true", help="verify checked-in samples against current catalog and registry")
    mode.add_argument("--update", action="store_true", help="regenerate checked-in samples from current catalog and registry")
    args = parser.parse_args(argv)

    try:
        catalog = _read_json(CATALOG_PATH)
        registry = _read_json(REGISTRY_PATH)
        expected = build_expected_fixture(catalog, registry)
        if args.update:
            _write_fixture(FIXTURE_PATH, expected)
        else:
            validate_fixture(_read_json(FIXTURE_PATH), expected)
    except CoverageError as exc:
        print(f"Favorite source sample coverage failed: {exc}", file=sys.stderr)
        return 1

    print(
        "FAVORITE_SOURCE_SAMPLE_COVERAGE_PASS "
        f"samples={len(expected['samples'])} profiles={len(expected['snapshot']['profiles'])} "
        f"explicit_exclusions={len(expected['excluded_registered_sources'])}"
    )
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
