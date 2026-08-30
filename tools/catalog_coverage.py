#!/usr/bin/env python3
"""Measure and enforce publication coverage for the EQ Library catalog.

A catalog is considered publication-complete only when every active source that is
eligible to publish structured data is represented by at least one source reference,
and every explicitly qualified GitHub profile is present. Reviewing/link-only sources
are reported but are never treated as permission to redistribute their data.

Coverage also reports manufacturer/headphone breadth globally and per source so a
whole-library publication cannot silently regress into a single pilot headphone.
"""
from __future__ import annotations

import argparse
import json
from collections import Counter, defaultdict
from pathlib import Path
from typing import Any


PUBLICATION_POLICIES = {"allowed", "structured-data-only"}


def load_json(path: Path) -> Any:
    return json.loads(path.read_text(encoding="utf-8"))


def headphone_identity(profile: dict[str, Any]) -> tuple[str, str, str, str] | None:
    headphone = profile.get("headphone") or {}
    manufacturer = str(headphone.get("manufacturer") or "").strip()
    model = str(headphone.get("model") or "").strip()
    if not manufacturer or not model:
        return None
    return (
        manufacturer,
        model,
        str(headphone.get("variant") or "").strip(),
        str(headphone.get("pads_or_mode") or "").strip(),
    )


def iter_source_references(catalog: dict[str, Any]):
    for profile in catalog.get("profiles", []):
        profile_id = str(profile.get("canonical_profile_id") or "")
        identity = headphone_identity(profile)
        for revision in profile.get("revisions", []):
            revision_id = str(revision.get("revision_id") or "")
            for ref in revision.get("source_references", []):
                yield profile_id, revision_id, identity, ref


def autoeq_measurement_source(ref: dict[str, Any]) -> str | None:
    if ref.get("source_id") != "autoeq":
        return None
    record_id = str(ref.get("source_record_id") or "")
    parts = record_id.replace("\\", "/").split("/")
    if len(parts) >= 3 and parts[0] == "results" and parts[1]:
        return parts[1]
    return None


def build_report(
    catalog: dict[str, Any],
    registry: dict[str, Any],
    qualified_manifest: dict[str, Any] | None = None,
) -> dict[str, Any]:
    source_refs: Counter[str] = Counter()
    source_profiles: dict[str, set[str]] = defaultdict(set)
    source_headphones: dict[str, set[tuple[str, str, str, str]]] = defaultdict(set)
    source_manufacturers: dict[str, set[str]] = defaultdict(set)
    measurement_sources: Counter[str] = Counter()
    source_record_ids: dict[str, set[str]] = defaultdict(set)

    catalog_headphones = {
        identity
        for profile in catalog.get("profiles", [])
        if (identity := headphone_identity(profile)) is not None
    }
    catalog_manufacturers = {identity[0] for identity in catalog_headphones}

    for profile_id, _revision_id, identity, ref in iter_source_references(catalog):
        source_id = str(ref.get("source_id") or "")
        if not source_id:
            continue
        source_refs[source_id] += 1
        source_profiles[source_id].add(profile_id)
        if identity is not None:
            source_headphones[source_id].add(identity)
            source_manufacturers[source_id].add(identity[0])
        record_id = str(ref.get("source_record_id") or "")
        if record_id:
            source_record_ids[source_id].add(record_id)
        measurement_source = autoeq_measurement_source(ref)
        if measurement_source:
            measurement_sources[measurement_source] += 1

    active_publishable = [
        source
        for source in registry.get("sources", [])
        if source.get("lifecycle") == "active"
        and source.get("redistribution") in PUBLICATION_POLICIES
    ]
    missing_active_sources = sorted(
        str(source.get("id"))
        for source in active_publishable
        if source_refs[str(source.get("id"))] == 0
    )

    qualified_missing: list[dict[str, str]] = []
    if qualified_manifest:
        for source in qualified_manifest.get("sources", []):
            source_id = str(source.get("id") or "")
            for profile in source.get("profiles", []):
                record_id = str(profile.get("source_record_id") or "")
                if record_id and record_id not in source_record_ids.get(source_id, set()):
                    qualified_missing.append({"source_id": source_id, "source_record_id": record_id})

    lifecycle_counts = Counter(
        str(source.get("lifecycle") or "unknown") for source in registry.get("sources", [])
    )
    report = {
        "profile_count": len(catalog.get("profiles", [])),
        "revision_count": sum(len(profile.get("revisions", [])) for profile in catalog.get("profiles", [])),
        "headphone_identity_count": len(catalog_headphones),
        "manufacturer_count": len(catalog_manufacturers),
        "source_reference_count": sum(source_refs.values()),
        "registry_lifecycle_counts": dict(sorted(lifecycle_counts.items())),
        "active_publishable_sources": sorted(str(source.get("id")) for source in active_publishable),
        "missing_active_publishable_sources": missing_active_sources,
        "qualified_manifest_missing_records": qualified_missing,
        "source_coverage": {
            source_id: {
                "profile_count": len(source_profiles[source_id]),
                "reference_count": source_refs[source_id],
                "record_count": len(source_record_ids[source_id]),
                "headphone_identity_count": len(source_headphones[source_id]),
                "manufacturer_count": len(source_manufacturers[source_id]),
            }
            for source_id in sorted(source_refs)
        },
        "autoeq_measurement_sources": dict(sorted(measurement_sources.items())),
        "autoeq_measurement_source_count": len(measurement_sources),
    }
    report["complete"] = not missing_active_sources and not qualified_missing
    return report


def validate_report(report: dict[str, Any]) -> list[str]:
    errors: list[str] = []
    missing_sources = report.get("missing_active_publishable_sources", [])
    if missing_sources:
        errors.append("active publishable sources missing from catalog: " + ", ".join(missing_sources))
    missing_records = report.get("qualified_manifest_missing_records", [])
    if missing_records:
        rendered = ", ".join(
            f"{item['source_id']}:{item['source_record_id']}" for item in missing_records
        )
        errors.append("qualified records missing from catalog: " + rendered)
    if "autoeq" in report.get("active_publishable_sources", []) and report.get("autoeq_measurement_source_count", 0) == 0:
        errors.append("AutoEq is active but no upstream measurement-source provenance was found")
    return errors


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--catalog", type=Path, default=Path("catalog/catalog.json"))
    parser.add_argument("--registry", type=Path, default=Path("config/source_registry.json"))
    parser.add_argument("--qualified-manifest", type=Path, default=Path("config/qualified_github_sources.json"))
    parser.add_argument("--output", type=Path)
    args = parser.parse_args()

    catalog = load_json(args.catalog)
    registry = load_json(args.registry)
    manifest = load_json(args.qualified_manifest) if args.qualified_manifest.is_file() else None
    report = build_report(catalog, registry, manifest)
    payload = json.dumps(report, indent=2, sort_keys=True, ensure_ascii=False) + "\n"
    if args.output:
        args.output.parent.mkdir(parents=True, exist_ok=True)
        args.output.write_text(payload, encoding="utf-8")
    else:
        print(payload, end="")

    errors = validate_report(report)
    for error in errors:
        print(f"ERROR: {error}")
    return 1 if errors else 0


if __name__ == "__main__":
    raise SystemExit(main())
