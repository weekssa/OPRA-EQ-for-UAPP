#!/usr/bin/env python3
"""Auto-publish mechanically valid GitHub EQ submissions as Unverified.

The submission staging parser only proves that fields and PEQ text are mechanically valid.
This publisher adds the remaining automatic trust boundary: the original URL must belong
to one of the explicitly qualified public community domains, that source must currently be
active with structured-data redistribution in the registry, and the submitted headphone
must resolve to an existing canonical/reviewed-alias identity. Ambiguous revisions stay in
review rather than being guessed.
"""
from __future__ import annotations

import argparse
import json
from pathlib import Path
from typing import Any
from urllib.parse import urlparse

from catalog_merge import merge_candidates
from community_peq_ingest import ParsedPeq, build_candidate
from headphone_identity_decisions import normalize

SOURCE_DOMAINS = {
    "reddit.com": "reddit-audio",
    "head-fi.org": "head-fi",
    "audiosciencereview.com": "audio-science-review",
    "forum.headphones.com": "headphones-community",
}


def source_id_for_url(url: str) -> str | None:
    host = (urlparse(url).hostname or "").casefold().rstrip(".")
    if not host:
        return None
    for domain, source_id in SOURCE_DOMAINS.items():
        if domain == "forum.headphones.com":
            if host == domain:
                return source_id
        elif host == domain or host.endswith("." + domain):
            return source_id
    return None


def _registry_source(registry: dict[str, Any], source_id: str) -> dict[str, Any] | None:
    return next(
        (item for item in registry.get("sources") or [] if str(item.get("id") or "") == source_id),
        None,
    )


def _profile_identity(profile: dict[str, Any]) -> tuple[str, str, str]:
    headphone = profile.get("headphone") or {}
    return (
        normalize(str(headphone.get("manufacturer") or "")),
        normalize(str(headphone.get("model") or "")),
        normalize(str(headphone.get("variant") or "")),
    )


def resolve_headphone_identity(
    snapshot: dict[str, Any],
    manufacturer: str,
    model: str,
    variant: str | None,
) -> tuple[str, str, str | None] | None:
    wanted_manufacturer = normalize(manufacturer)
    wanted_model = normalize(model)
    wanted_variant = normalize(variant or "")

    for profile in snapshot.get("profiles") or []:
        headphone = profile.get("headphone") or {}
        if _profile_identity(profile) == (wanted_manufacturer, wanted_model, wanted_variant):
            return (
                str(headphone.get("manufacturer") or "").strip(),
                str(headphone.get("model") or "").strip(),
                str(headphone.get("variant") or "").strip() or None,
            )

    # Reviewed model aliases are safe only when the submission does not introduce an
    # unreviewed variant. Variants/revisions/pads/modes remain distinct until evidence.
    if wanted_variant:
        return None
    for group in snapshot.get("headphone_aliases") or []:
        group_manufacturer = str(group.get("manufacturer") or "").strip()
        if normalize(group_manufacturer) != wanted_manufacturer:
            continue
        canonical = str(group.get("canonical_model") or "").strip()
        names = [canonical, *(str(value) for value in group.get("aliases") or [])]
        if wanted_model not in {normalize(name) for name in names}:
            continue
        canonical_key = (wanted_manufacturer, normalize(canonical), "")
        for profile in snapshot.get("profiles") or []:
            headphone = profile.get("headphone") or {}
            if _profile_identity(profile) == canonical_key:
                return (
                    str(headphone.get("manufacturer") or group_manufacturer).strip(),
                    str(headphone.get("model") or canonical).strip(),
                    None,
                )
    return None


def _hold(reason: str, submission: dict[str, Any]) -> dict[str, Any]:
    return {
        "submission_id": submission.get("submission_id"),
        "decision": "hold-needs-review",
        "reason": reason,
        "verification_status": "unverified",
    }


def publish_submission(
    snapshot: dict[str, Any],
    submission: dict[str, Any],
    registry: dict[str, Any],
) -> tuple[dict[str, Any], dict[str, Any]]:
    if submission.get("candidate_state") != "ready_for_source_policy" or not submission.get("mechanically_valid"):
        return snapshot, _hold("submission is not mechanically qualified", submission)

    original_url = str(submission.get("original_source_url") or "").strip()
    source_id = source_id_for_url(original_url)
    if source_id is None:
        return snapshot, _hold("original source domain is not in the automatic community publication allowlist", submission)

    source = _registry_source(registry, source_id)
    if source is None:
        return snapshot, _hold(f"source {source_id} is not registered", submission)
    if source.get("lifecycle") != "active":
        return snapshot, _hold(f"source {source_id} is not active", submission)
    if source.get("redistribution") != "structured-data-only":
        return snapshot, _hold(f"source {source_id} is not qualified for structured-data publication", submission)

    headphone = submission.get("headphone") or {}
    manufacturer = str(headphone.get("manufacturer") or "").strip()
    model = str(headphone.get("model") or "").strip()
    variant = str(headphone.get("variant") or "").strip() or None
    resolved = resolve_headphone_identity(snapshot, manufacturer, model, variant)
    if resolved is None:
        return snapshot, _hold("headphone identity is not an existing canonical identity or reviewed alias", submission)
    canonical_manufacturer, canonical_model, canonical_variant = resolved

    parsed_payload = submission.get("parsed_peq") or {}
    filters = parsed_payload.get("filters") or []
    if parsed_payload.get("status") != "parsed" or not filters:
        return snapshot, _hold("structured PEQ is not available", submission)

    creator = str(submission.get("creator") or "").strip()
    explicit_tuning_label = str(submission.get("tuning_label") or "").strip()
    target = str(submission.get("target") or "").strip() or None
    tuning_label = explicit_tuning_label or f"{creator} community tuning"
    candidate = build_candidate(
        ParsedPeq(
            preamp_db=parsed_payload.get("preamp_db"),
            filters=filters,
        ),
        manufacturer=canonical_manufacturer,
        model=canonical_model,
        variant=canonical_variant,
        creator=creator,
        tuning_label=tuning_label,
        target=target,
        source_id=source_id,
        source_kind="community",
        source_url=original_url,
        source_record_id=str(submission.get("submission_id") or original_url),
        redistribution_policy="structured-data-only",
        source_version=str(submission.get("source_date") or "").strip() or None,
        discovered_at_epoch_seconds=None,
        verification_status="unverified",
    )
    candidate["publication_eligible"] = True
    incoming_revision = candidate["revisions"][0]
    sound_impact = str(submission.get("sound_impact") or "").strip() or None
    if sound_impact:
        incoming_revision["sound_impact_summary"] = sound_impact

    existing_profile = next(
        (
            profile
            for profile in snapshot.get("profiles") or []
            if profile.get("canonical_profile_id") == candidate.get("canonical_profile_id")
        ),
        None,
    )
    if existing_profile is not None:
        incoming_fingerprint = incoming_revision.get("acoustic_fingerprint")
        existing_fingerprints = {
            revision.get("acoustic_fingerprint")
            for revision in existing_profile.get("revisions") or []
        }
        if incoming_fingerprint not in existing_fingerprints:
            return snapshot, _hold(
                "same creator/headphone/tuning lineage has different acoustic data; revision intent requires review",
                submission,
            )

    merged, outcomes = merge_candidates(
        snapshot,
        [candidate],
        source_registry_version=str(registry.get("registry_version") or ""),
    )
    report = {
        "submission_id": submission.get("submission_id"),
        "decision": "published-unverified",
        "source_id": source_id,
        "canonical_profile_id": candidate["canonical_profile_id"],
        "verification_status": "unverified",
        "filter_count": len(filters),
        "merge_outcomes": outcomes,
    }
    return merged, report


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--catalog", type=Path, required=True)
    parser.add_argument("--submission", type=Path, required=True)
    parser.add_argument("--registry", type=Path, required=True)
    parser.add_argument("--output", type=Path, required=True)
    parser.add_argument("--report", type=Path, required=True)
    args = parser.parse_args()

    snapshot = json.loads(args.catalog.read_text(encoding="utf-8"))
    submission = json.loads(args.submission.read_text(encoding="utf-8"))
    registry = json.loads(args.registry.read_text(encoding="utf-8"))
    merged, report = publish_submission(snapshot, submission, registry)

    args.output.parent.mkdir(parents=True, exist_ok=True)
    args.output.write_text(json.dumps(merged, indent=2, ensure_ascii=False) + "\n", encoding="utf-8")
    args.report.parent.mkdir(parents=True, exist_ok=True)
    args.report.write_text(json.dumps(report, indent=2, sort_keys=True, ensure_ascii=False) + "\n", encoding="utf-8")
    print(json.dumps(report, sort_keys=True))
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
