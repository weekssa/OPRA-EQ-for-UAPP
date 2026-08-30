#!/usr/bin/env python3
"""Ingest the qualified AutoEq ParametricEQ corpus into the canonical EQ Library.

The caller provides a local partial/sparse checkout of jaakkopasanen/AutoEq containing
`results/**/* ParametricEQ.txt` and `dbtools/manufacturers.tsv`. Only structured PEQ
values plus source-path metadata are consumed. Raw measurement CSV data and upstream
README prose are never copied into the EQ Library catalog.

The importer mirrors AutoEq's own manufacturer-prefix resolution, preserves exact model
labels, carries measurement-source/rig context, and uses the upstream repository commit
as the source-health cursor. Per-file content hashes avoid rewriting every profile when
an unrelated upstream file changes. Existing catalog entries are never removed merely
because an upstream record disappears; last-known-good publication remains monotonic.
"""
from __future__ import annotations

import argparse
import hashlib
import json
import subprocess
from dataclasses import asdict
from datetime import datetime, timezone
from pathlib import Path
from typing import Any, Iterable
from urllib.parse import quote

from autoeq_ingest import build_candidate, parse_parametric_eq
from catalog_merge import merge_candidates
from catalog_pipeline import (
    SourceHealth,
    load_health,
    reconcile_health,
    record_scan_failure,
    record_scan_success,
    stable_json,
)

AUTOEQ_REPOSITORY = "jaakkopasanen/AutoEq"
AUTOEQ_BRANCH = "master"
GENERIC_CONTEXTS = {"in-ear", "over-ear", "earbud"}
PARAMETRIC_SUFFIX = " ParametricEQ.txt"


def utc_now() -> str:
    return datetime.now(timezone.utc).replace(microsecond=0).isoformat().replace("+00:00", "Z")


def epoch_now() -> int:
    return int(datetime.now(timezone.utc).timestamp())


def load_manufacturer_index(path: Path) -> dict[str, str]:
    """Return case-folded alias -> AutoEq canonical manufacturer name."""
    aliases: dict[str, str] = {}
    for raw_line in path.read_text(encoding="utf-8").splitlines():
        line = raw_line.strip()
        if not line:
            continue
        names = [item.strip() for item in line.split("\t") if item.strip()]
        if not names:
            continue
        canonical = names[0]
        for name in names:
            aliases[name.casefold()] = canonical
    if not aliases:
        raise ValueError("AutoEq manufacturer index is empty")
    return aliases


def resolve_manufacturer(full_name: str, aliases: dict[str, str]) -> tuple[str | None, str | None, str | None]:
    """Mirror AutoEq ManufacturerIndex.find/model using longest leading word match."""
    words = full_name.split(" ")
    for count in range(len(words), 0, -1):
        prefix = " ".join(words[:count])
        canonical = aliases.get(prefix.casefold())
        if canonical:
            model = " ".join(words[count:]).strip()
            return canonical, model or None, prefix
    return None, None, None


def measurement_context(relative_path: Path) -> str | None:
    """Extract rig/configuration context while ignoring generic form-factor folders."""
    parts = relative_path.parts
    # results / measurement-source / [context ...] / model-dir / preset-file
    context_parts = list(parts[2:-2])
    if not context_parts:
        return None
    rendered = " / ".join(context_parts).strip()
    if rendered.casefold() in GENERIC_CONTEXTS:
        return None
    return rendered or None


def source_commit(root: Path) -> str:
    try:
        return subprocess.check_output(
            ["git", "-C", str(root), "rev-parse", "HEAD"],
            text=True,
            stderr=subprocess.DEVNULL,
        ).strip()
    except (OSError, subprocess.CalledProcessError) as exc:
        raise ValueError("AutoEq checkout commit could not be resolved; pass --source-commit") from exc


def source_url(relative_path: Path) -> str:
    encoded = quote(relative_path.as_posix(), safe="/")
    return f"https://github.com/{AUTOEQ_REPOSITORY}/blob/{AUTOEQ_BRANCH}/{encoded}"


def compact_size_bytes(payload: Any) -> int:
    return len(stable_json(payload).encode("utf-8"))


def pretty_size_bytes(payload: Any) -> int:
    return len((json.dumps(payload, indent=2, sort_keys=True, ensure_ascii=False) + "\n").encode("utf-8"))


def write_json(path: Path, payload: Any, *, compact_threshold_bytes: int = 8 * 1024 * 1024) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    compact = stable_json(payload)
    if len(compact.encode("utf-8")) >= compact_threshold_bytes:
        path.write_text(compact + "\n", encoding="utf-8")
    else:
        path.write_text(json.dumps(payload, indent=2, sort_keys=True, ensure_ascii=False) + "\n", encoding="utf-8")


def write_health(path: Path, health: dict[str, SourceHealth], updated_at: str) -> None:
    write_json(
        path,
        {
            "schema_version": 1,
            "updated_at": updated_at,
            "sources": [asdict(health[key]) for key in sorted(health)],
        },
    )


def _existing_fingerprints(catalog: dict[str, Any]) -> dict[str, set[str]]:
    result: dict[str, set[str]] = {}
    for profile in catalog.get("profiles", []):
        profile_id = str(profile.get("canonical_profile_id") or "")
        if not profile_id:
            continue
        result[profile_id] = {
            str(revision.get("acoustic_fingerprint") or "")
            for revision in profile.get("revisions", [])
            if str(revision.get("acoustic_fingerprint") or "")
        }
    return result


def build_corpus_candidates(
    *,
    autoeq_root: Path,
    catalog: dict[str, Any],
    now_epoch: int,
    report: dict[str, Any],
) -> Iterable[dict[str, Any]]:
    manufacturers_path = autoeq_root / "dbtools" / "manufacturers.tsv"
    results_root = autoeq_root / "results"
    if not manufacturers_path.is_file():
        raise ValueError("AutoEq sparse checkout is missing dbtools/manufacturers.tsv")
    if not results_root.is_dir():
        raise ValueError("AutoEq sparse checkout is missing results/")

    aliases = load_manufacturer_index(manufacturers_path)
    preset_paths = sorted(results_root.rglob(f"*{PARAMETRIC_SUFFIX}"))
    if not preset_paths:
        raise ValueError("AutoEq sparse checkout contains no ParametricEQ presets")

    report["preset_file_count"] = len(preset_paths)
    report["unknown_manufacturer_count"] = 0
    report["parse_failure_count"] = 0
    report["candidate_count"] = 0
    report["unknown_manufacturer_samples"] = []
    report["parse_failure_samples"] = []
    existing = _existing_fingerprints(catalog)

    for preset_path in preset_paths:
        relative = preset_path.relative_to(autoeq_root)
        if len(relative.parts) < 4 or relative.parts[0] != "results":
            continue
        measurement_source = relative.parts[1]
        filename = preset_path.name
        if not filename.endswith(PARAMETRIC_SUFFIX):
            continue
        full_name = filename[: -len(PARAMETRIC_SUFFIX)].strip()
        manufacturer, model, _matched_prefix = resolve_manufacturer(full_name, aliases)
        if not manufacturer or not model:
            report["unknown_manufacturer_count"] += 1
            if len(report["unknown_manufacturer_samples"]) < 25:
                report["unknown_manufacturer_samples"].append(relative.as_posix())
            continue

        try:
            text = preset_path.read_text(encoding="utf-8")
            parsed = parse_parametric_eq(text)
        except (OSError, UnicodeError, ValueError) as exc:
            report["parse_failure_count"] += 1
            if len(report["parse_failure_samples"]) < 25:
                report["parse_failure_samples"].append(
                    {"path": relative.as_posix(), "error": str(exc)[:300]}
                )
            continue

        file_content_hash = hashlib.sha256(text.encode("utf-8")).hexdigest()
        candidate = build_candidate(
            parsed,
            manufacturer=manufacturer,
            model=model,
            measurement_source=measurement_source,
            measurement_context=measurement_context(relative),
            target=None,
            source_url=source_url(relative),
            source_record_id=relative.as_posix(),
            source_version=f"AutoEq content sha256 {file_content_hash}",
            discovered_at_epoch_seconds=None,
        )
        candidate["publication_eligible"] = True
        revision = candidate["revisions"][0]
        profile_id = str(candidate["canonical_profile_id"])
        fingerprint = str(revision["acoustic_fingerprint"])

        # Verification time lives in source health for stable records. Timestamp only
        # newly seen profiles/revisions so an unrelated upstream commit does not churn
        # thousands of immutable source references.
        known = existing.setdefault(profile_id, set())
        if fingerprint not in known:
            revision["first_seen_at_epoch_seconds"] = now_epoch
            for source in revision["source_references"]:
                source["discovered_at_epoch_seconds"] = now_epoch
                source["last_verified_at_epoch_seconds"] = now_epoch
            known.add(fingerprint)

        report["candidate_count"] += 1
        yield candidate


def _registry_parser_version(registry: dict[str, Any]) -> str:
    source = next((item for item in registry.get("sources", []) if item.get("id") == "autoeq"), None)
    if source is None:
        raise ValueError("AutoEq source missing from registry")
    return str(source.get("parser_version") or "")


def refresh(
    *,
    autoeq_root: Path,
    catalog: dict[str, Any],
    registry: dict[str, Any],
    health: dict[str, SourceHealth],
    upstream_commit: str,
    now_iso: str,
    now_epoch: int,
    max_compact_catalog_bytes: int,
) -> tuple[dict[str, Any], dict[str, SourceHealth], dict[str, Any]]:
    previous_autoeq = health.get("autoeq")
    expected_parser_version = _registry_parser_version(registry)
    parser_changed = previous_autoeq is None or str(previous_autoeq.parser_version or "") != expected_parser_version
    health = reconcile_health(registry, health)
    autoeq_health = health["autoeq"]
    report: dict[str, Any] = {
        "source_id": "autoeq",
        "upstream_commit": upstream_commit,
        "parser_version": expected_parser_version,
        "parser_changed": parser_changed,
        "status": "ok",
    }

    if autoeq_health.cursor == upstream_commit and not parser_changed:
        health["autoeq"] = record_scan_success(
            autoeq_health,
            cursor=upstream_commit,
            content_fingerprint=autoeq_health.last_content_fingerprint,
            attempted_at=now_iso,
        )
        report.update(
            {
                "outcome": "unchanged_cursor",
                "profile_count": len(catalog.get("profiles", [])),
                "compact_catalog_bytes": compact_size_bytes(catalog),
                "pretty_catalog_bytes": pretty_size_bytes(catalog),
            }
        )
        return catalog, health, report

    candidates = build_corpus_candidates(
        autoeq_root=autoeq_root,
        catalog=catalog,
        now_epoch=now_epoch,
        report=report,
    )
    merged, outcomes = merge_candidates(
        catalog,
        candidates,
        generated_at=now_iso,
        source_registry_version=str(registry.get("registry_version") or ""),
    )

    preset_count = int(report.get("preset_file_count") or 0)
    failures = int(report.get("parse_failure_count") or 0) + int(report.get("unknown_manufacturer_count") or 0)
    failure_budget = max(25, max(1, preset_count // 100))
    if failures > failure_budget:
        raise ValueError(
            f"AutoEq corpus qualification failures exceeded safety budget: {failures} > {failure_budget} "
            f"across {preset_count} preset files"
        )

    compact_bytes = compact_size_bytes(merged)
    pretty_bytes = pretty_size_bytes(merged)
    if compact_bytes > max_compact_catalog_bytes:
        raise ValueError(
            f"AutoEq corpus would exceed catalog compact-size safety limit: "
            f"{compact_bytes} > {max_compact_catalog_bytes} bytes"
        )

    aggregate = hashlib.sha256()
    for profile in merged.get("profiles", []):
        for revision in profile.get("revisions", []):
            if any(source.get("source_id") == "autoeq" for source in revision.get("source_references", [])):
                aggregate.update(str(profile.get("canonical_profile_id") or "").encode("utf-8"))
                aggregate.update(str(revision.get("acoustic_fingerprint") or "").encode("utf-8"))
    content_fingerprint = aggregate.hexdigest()
    health["autoeq"] = record_scan_success(
        autoeq_health,
        cursor=upstream_commit,
        content_fingerprint=content_fingerprint,
        attempted_at=now_iso,
    )
    report.update(
        {
            "outcome": "refreshed",
            "merge_outcomes": outcomes,
            "profile_count": len(merged.get("profiles", [])),
            "compact_catalog_bytes": compact_bytes,
            "pretty_catalog_bytes": pretty_bytes,
            "failure_budget": failure_budget,
        }
    )
    return merged, health, report


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--autoeq-root", type=Path, required=True)
    parser.add_argument("--catalog", type=Path, required=True)
    parser.add_argument("--registry", type=Path, default=Path("config/source_registry.json"))
    parser.add_argument("--health", type=Path, default=Path("catalog/source_health.json"))
    parser.add_argument("--catalog-output", type=Path, required=True)
    parser.add_argument("--health-output", type=Path, required=True)
    parser.add_argument("--report", type=Path)
    parser.add_argument("--source-commit")
    parser.add_argument("--max-compact-catalog-bytes", type=int, default=60 * 1024 * 1024)
    args = parser.parse_args()

    catalog = json.loads(args.catalog.read_text(encoding="utf-8"))
    registry = json.loads(args.registry.read_text(encoding="utf-8"))
    health = load_health(args.health)
    now_iso = utc_now()
    now_epoch = epoch_now()
    upstream_commit = (args.source_commit or source_commit(args.autoeq_root)).strip()

    try:
        updated_catalog, updated_health, report = refresh(
            autoeq_root=args.autoeq_root,
            catalog=catalog,
            registry=registry,
            health=health,
            upstream_commit=upstream_commit,
            now_iso=now_iso,
            now_epoch=now_epoch,
            max_compact_catalog_bytes=args.max_compact_catalog_bytes,
        )
    except (OSError, ValueError, KeyError, json.JSONDecodeError) as exc:
        reconciled = reconcile_health(registry, health)
        reconciled["autoeq"] = record_scan_failure(reconciled["autoeq"], str(exc), attempted_at=now_iso)
        write_json(args.catalog_output, catalog)
        write_health(args.health_output, reconciled, now_iso)
        failure_report = {"source_id": "autoeq", "status": "degraded", "error": str(exc)[:1000]}
        if args.report:
            write_json(args.report, failure_report)
        print(json.dumps(failure_report, sort_keys=True))
        return 1

    write_json(args.catalog_output, updated_catalog)
    write_health(args.health_output, updated_health, now_iso)
    if args.report:
        write_json(args.report, report)
    print(json.dumps(report, sort_keys=True))
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
