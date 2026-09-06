#!/usr/bin/env python3
"""Ingest public GitHub structured-PEQ discovery candidates as community EQs.

This adapter turns the existing GitHub discovery queue into a production ingestion lane.
It fetches only the exact public candidate blob/raw file, parses supported Equalizer
APO-style parametric EQ, resolves the headphone against canonical catalog identities,
and publishes mechanically valid tunings as Unverified.

Exact acoustic duplicates for the same headphone attach GitHub provenance to the
existing canonical revision instead of creating another tuning. Unparseable,
non-headphone, ambiguous-identity, or inaccessible candidates are quarantined with an
explicit reason and never block unrelated valid candidates.

A GitHub repository/Gist owner is source-account provenance, not proof of EQ authorship.
The canonical creator therefore remains null unless a candidate explicitly marks creator
metadata as authorship evidence. The immutable source URL/record still preserves where
the preset was discovered.
"""

from __future__ import annotations

import argparse
import base64
import copy
import json
import re
import urllib.error
import urllib.request
from pathlib import Path
from typing import Any, Callable

from catalog_merge import merge_candidates
from catalog_pipeline import (
    load_health,
    load_json,
    reconcile_health,
    record_scan_failure,
    record_scan_success,
    sha256_json,
    validate_registry,
    validate_snapshot,
    write_health,
)
from community_peq_ingest import build_candidate, parse_peq
from reddit_community_ingest import catalog_headphones, normalize

SOURCE_ID = "github-community"
PARAMETRIC_SUFFIX_RE = re.compile(r"\s*parametric\s*eq\s*$", re.IGNORECASE)
CHANNEL_SUFFIX_RE = re.compile(r"(?:[_\s-]+[lr])$", re.IGNORECASE)


def _source(registry: dict[str, Any]) -> dict[str, Any]:
    for source in registry.get("sources") or []:
        if source.get("id") == SOURCE_ID:
            return source
    raise ValueError(f"source registry is missing {SOURCE_ID}")


def _source_account(candidate: dict[str, Any]) -> str | None:
    explicit = str(candidate.get("source_account") or "").strip()
    if explicit:
        return explicit
    repository = str(candidate.get("repository") or "").strip()
    if "/" in repository:
        owner = repository.split("/", 1)[0].strip()
        if owner:
            return owner
    # Backward compatibility for discovery queues written before source_account existed:
    # those queues populated `creator` directly from repository/Gist ownership. Retain
    # that value only as source-account provenance, never as canonical creator metadata.
    if candidate.get("creator_is_explicit") is not True:
        legacy_owner = str(candidate.get("creator") or "").strip()
        return legacy_owner or None
    return None


def _explicit_creator(candidate: dict[str, Any]) -> str | None:
    if candidate.get("creator_is_explicit") is not True:
        return None
    value = str(candidate.get("creator") or "").strip()
    return value or None


def _candidate_blob_url(candidate: dict[str, Any]) -> str | None:
    repository = str(candidate.get("repository") or "").strip()
    sha = str(candidate.get("content_sha") or "").strip()
    if repository and sha:
        return f"https://api.github.com/repos/{repository}/git/blobs/{sha}"
    raw_url = str(candidate.get("raw_url") or "").strip()
    return raw_url or None


def fetch_candidate_text(candidate: dict[str, Any], github_token: str | None) -> str:
    url = _candidate_blob_url(candidate)
    if not url:
        raise ValueError("candidate has no fetchable blob/raw URL")
    headers = {
        "Accept": "application/vnd.github+json",
        "User-Agent": "EQ-Library-community-ingest/0.4",
        "X-GitHub-Api-Version": "2022-11-28",
    }
    if github_token:
        headers["Authorization"] = "Bearer " + github_token
    request = urllib.request.Request(url, headers=headers)
    with urllib.request.urlopen(request, timeout=30) as response:
        payload = response.read()

    if "/git/blobs/" in url:
        blob = json.loads(payload.decode("utf-8"))
        if blob.get("encoding") != "base64":
            raise ValueError("GitHub blob did not use base64 encoding")
        payload = base64.b64decode(str(blob.get("content") or "").encode("ascii"))
    return payload.decode("utf-8-sig")


def _stem(path: str) -> str:
    name = Path(path).name
    value = str(Path(name).with_suffix(""))
    value = PARAMETRIC_SUFFIX_RE.sub("", value)
    return value.strip(" _-")


def _headphone_matches(
    text: str,
    headphones: list[tuple[str, str]],
) -> list[tuple[str, str]]:
    haystack = f" {normalize(text)} "
    matches: list[tuple[str, str]] = []
    for manufacturer, model in headphones:
        normalized_model = normalize(model)
        if len(normalized_model) < 2 or f" {normalized_model} " not in haystack:
            continue
        normalized_manufacturer = normalize(manufacturer)
        manufacturer_present = bool(
            normalized_manufacturer and f" {normalized_manufacturer} " in haystack
        )
        if manufacturer_present or len(normalized_model) >= 4:
            matches.append((manufacturer, model))
    return list(dict.fromkeys(matches))


def resolve_headphone(
    candidate: dict[str, Any],
    headphones: list[tuple[str, str]],
) -> tuple[tuple[str, str] | None, str | None]:
    path = str(candidate.get("path") or "")
    stem = _stem(path)
    probes = [stem]
    channel_trimmed = CHANNEL_SUFFIX_RE.sub("", stem).strip(" _-")
    if channel_trimmed and channel_trimmed != stem:
        probes.append(channel_trimmed)

    for probe in probes:
        matches = _headphone_matches(probe, headphones)
        if not matches:
            continue
        longest = max(len(normalize(model)) for _, model in matches)
        strongest = [item for item in matches if len(normalize(item[1])) == longest]
        if len(strongest) == 1:
            return strongest[0], None
        return None, "ambiguous_headphone_identity"

    # Do not fall back to the full parent path: target folders can name a different
    # headphone/target and would create false identity assignments.
    return None, "unmatched_headphone_identity"


def explicit_target(path: str) -> str | None:
    parts = Path(path).parts[:-1]
    for part in reversed(parts):
        cleaned = str(part).strip()
        if cleaned.lower().endswith(" target") and len(cleaned) > len(" target"):
            return cleaned[: -len(" target")].strip() + " Target"
    return None


def tuning_label(candidate: dict[str, Any]) -> str:
    stem = _stem(str(candidate.get("path") or ""))
    target = explicit_target(str(candidate.get("path") or ""))
    if target:
        return f"{target} — {stem}"[:160]
    return (stem or "GitHub community PEQ")[:160]


def _same_headphone(profile: dict[str, Any], manufacturer: str, model: str) -> bool:
    headphone = profile.get("headphone") or {}
    return (
        str(headphone.get("manufacturer") or "").strip().casefold() == manufacturer.casefold()
        and str(headphone.get("model") or "").strip().casefold() == model.casefold()
        and not str(headphone.get("variant") or "").strip()
    )


def adapt_exact_duplicate(
    snapshot: dict[str, Any],
    candidate: dict[str, Any],
) -> tuple[dict[str, Any], str | None]:
    revision = candidate["revisions"][0]
    fingerprint = str(revision.get("acoustic_fingerprint") or "")
    headphone = candidate.get("headphone") or {}
    manufacturer = str(headphone.get("manufacturer") or "")
    model = str(headphone.get("model") or "")
    for profile in snapshot.get("profiles") or []:
        if not _same_headphone(profile, manufacturer, model):
            continue
        for existing_revision in profile.get("revisions") or []:
            if str(existing_revision.get("acoustic_fingerprint") or "") != fingerprint:
                continue
            adapted = copy.deepcopy(candidate)
            adapted["canonical_profile_id"] = profile["canonical_profile_id"]
            for key in ("headphone", "creator", "target", "tuning_label", "scope", "purpose"):
                if key in profile:
                    adapted[key] = copy.deepcopy(profile[key])
                else:
                    adapted.pop(key, None)
            return adapted, str(profile["canonical_profile_id"])
    return candidate, None


def ingest_candidates(
    snapshot: dict[str, Any],
    discovery: dict[str, Any],
    registry: dict[str, Any],
    current_health: dict[str, Any],
    *,
    github_token: str | None,
    fetcher: Callable[[dict[str, Any], str | None], str] = fetch_candidate_text,
) -> tuple[dict[str, Any], dict[str, Any], dict[str, Any], bool]:
    errors = validate_registry(registry)
    if errors:
        raise ValueError("invalid source registry: " + "; ".join(errors))
    source = _source(registry)
    if source.get("lifecycle") != "active":
        raise ValueError(f"{SOURCE_ID} must be active for publication")
    if source.get("redistribution") != "structured-data-only":
        raise ValueError(f"{SOURCE_ID} is not qualified for structured publication")

    health = reconcile_health(registry, current_health)
    headphones = catalog_headphones(snapshot)
    candidates = discovery.get("candidates") or []
    report: dict[str, Any] = {
        "source_id": SOURCE_ID,
        "discovered": len(candidates),
        "fetched": 0,
        "parsed": 0,
        "publishable": 0,
        "exact_duplicates": 0,
        "quarantined": 0,
        "merge_outcomes": {},
        "quarantine_reasons": {},
        "records": [],
    }
    publish_candidates: list[dict[str, Any]] = []
    dedupe_snapshot = copy.deepcopy(snapshot)

    def quarantine(row: dict[str, Any], reason: str) -> None:
        row["decision"] = "quarantine"
        row["reason"] = reason
        report["quarantined"] += 1
        reasons = report["quarantine_reasons"]
        reasons[reason] = int(reasons.get(reason) or 0) + 1
        report["records"].append(row)

    for item in candidates:
        if not isinstance(item, dict):
            continue
        source_account = _source_account(item)
        creator = _explicit_creator(item)
        row = {
            "candidate_id": item.get("candidate_id"),
            "repository": item.get("repository"),
            "path": item.get("path"),
            "url": item.get("url"),
            "source_account": source_account,
            "creator": creator,
        }
        try:
            text = fetcher(item, github_token)
            report["fetched"] += 1
        except (OSError, ValueError, UnicodeError, json.JSONDecodeError, urllib.error.URLError) as exc:
            quarantine(row, "fetch_failed")
            row["detail"] = str(exc)[:240]
            continue

        matched, identity_error = resolve_headphone(item, headphones)
        if matched is None:
            quarantine(row, identity_error or "unmatched_headphone_identity")
            continue

        try:
            parsed = parse_peq(text)
            report["parsed"] += 1
        except ValueError as exc:
            quarantine(row, "unsupported_or_malformed_peq")
            row["detail"] = str(exc)[:240]
            continue

        manufacturer, model = matched
        try:
            canonical = build_candidate(
                parsed,
                manufacturer=manufacturer,
                model=model,
                creator=creator,
                tuning_label=tuning_label(item),
                source_id=SOURCE_ID,
                source_kind="community_repository",
                source_url=str(item.get("url") or ""),
                source_record_id=str(item.get("source_record_id") or item.get("candidate_id") or ""),
                redistribution_policy="structured-data-only",
                target=explicit_target(str(item.get("path") or "")),
                variant=None,
                source_version=str(item.get("content_sha") or "") or None,
                discovered_at_epoch_seconds=None,
                verification_status="unverified",
                allow_missing_creator=True,
            )
        except ValueError as exc:
            quarantine(row, "canonical_candidate_invalid")
            row["detail"] = str(exc)[:240]
            continue

        canonical, duplicate_profile_id = adapt_exact_duplicate(dedupe_snapshot, canonical)
        if duplicate_profile_id:
            report["exact_duplicates"] += 1
            row["decision"] = "attach_exact_duplicate_provenance"
            row["canonical_profile_id"] = duplicate_profile_id
        else:
            row["decision"] = "publish_unverified"
            row["canonical_profile_id"] = canonical["canonical_profile_id"]
        row["manufacturer"] = manufacturer
        row["model"] = model
        row["filter_count"] = len(parsed.filters)
        report["records"].append(row)
        publish_candidates.append(canonical)
        if not duplicate_profile_id:
            dedupe_snapshot.setdefault("profiles", []).append(copy.deepcopy(canonical))

    merged, outcomes = merge_candidates(
        snapshot,
        publish_candidates,
        source_registry_version=str(registry.get("registry_version") or "") or None,
    )
    report["publishable"] = len(publish_candidates)
    report["merge_outcomes"] = outcomes

    validation_errors = validate_snapshot(merged)
    if validation_errors:
        raise ValueError("GitHub community merge produced invalid catalog: " + "; ".join(validation_errors))

    # A reachable queue is a successful source scan even if every individual record is
    # quarantined for structural/identity reasons. A complete network outage is not.
    if candidates and report["fetched"] == 0:
        health[SOURCE_ID] = record_scan_failure(
            health[SOURCE_ID], "all GitHub community candidate fetches failed"
        )
        report["status"] = "degraded"
        return snapshot, health, report, False

    health[SOURCE_ID] = record_scan_success(
        health[SOURCE_ID],
        cursor=sha256_json(
            sorted(
                str(item.get("source_record_id") or item.get("candidate_id") or "")
                for item in candidates
                if isinstance(item, dict)
            )
        ),
        content_fingerprint=sha256_json(
            {
                "records": [
                    {
                        "candidate_id": row.get("candidate_id"),
                        "decision": row.get("decision"),
                        "canonical_profile_id": row.get("canonical_profile_id"),
                    }
                    for row in report["records"]
                ],
                "merge_outcomes": outcomes,
            }
        ),
    )
    report["status"] = "ok" if report["quarantined"] == 0 else "partial"
    return merged, health, report, True


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--catalog", type=Path, required=True)
    parser.add_argument("--discovery", type=Path, required=True)
    parser.add_argument("--registry", type=Path, required=True)
    parser.add_argument("--health", type=Path, required=True)
    parser.add_argument("--catalog-output", type=Path, required=True)
    parser.add_argument("--health-output", type=Path, required=True)
    parser.add_argument("--report", type=Path, required=True)
    parser.add_argument("--github-token")
    parser.add_argument("--allow-degraded", action="store_true")
    args = parser.parse_args()

    snapshot = load_json(args.catalog)
    discovery = load_json(args.discovery)
    registry = load_json(args.registry)
    merged, health, report, success = ingest_candidates(
        snapshot,
        discovery,
        registry,
        load_health(args.health),
        github_token=args.github_token,
    )
    args.catalog_output.parent.mkdir(parents=True, exist_ok=True)
    args.catalog_output.write_text(
        json.dumps(merged, indent=2, ensure_ascii=False) + "\n", encoding="utf-8"
    )
    write_health(args.health_output, health)
    args.report.parent.mkdir(parents=True, exist_ok=True)
    args.report.write_text(
        json.dumps(report, indent=2, sort_keys=True, ensure_ascii=False) + "\n",
        encoding="utf-8",
    )
    print(json.dumps(report, sort_keys=True))
    return 0 if success or args.allow_degraded else 1


if __name__ == "__main__":
    raise SystemExit(main())
