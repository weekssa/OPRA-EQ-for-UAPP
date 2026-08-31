#!/usr/bin/env python3
"""Build explicit General Effect/Genre EQ Library candidates from structured PEQ text.

This is deliberately separate from headphone community ingestion. General presets are
not assigned a headphone identity, and callers must explicitly classify the source
preset as either an Effect or Genre preset. The adapter never infers category from prose,
never invents filters, and never applies device band limits.
"""

from __future__ import annotations

import argparse
import hashlib
import json
from pathlib import Path
from typing import Any

from community_peq_ingest import (
    ALLOWED_POLICIES,
    ALLOWED_SOURCE_KINDS,
    ALLOWED_VERIFICATION,
    CANONICAL_SOURCE_KIND,
    DEFAULT_VERIFICATION,
    PROVENANCE_TIER,
    ParsedPeq,
    acoustic_fingerprint,
    parse_peq,
)

ALLOWED_PURPOSES = {"effect", "genre"}


def canonical_profile_id(*, purpose: str, creator: str, tuning_label: str) -> str:
    """Stable lineage ID: category + creator + source-authored preset label.

    Source ID is intentionally excluded so mirrors of the same creator preset can merge as
    provenance rather than becoming duplicates. Purpose is included so Effect and Genre
    lineages can never collide merely because their labels or coefficients match.
    """
    identity = "|".join(
        [
            "general",
            purpose.strip().lower(),
            creator.strip().lower(),
            tuning_label.strip().lower(),
        ]
    )
    return "general-" + hashlib.sha256(identity.encode("utf-8")).hexdigest()[:24]


def build_candidate(
    parsed: ParsedPeq,
    *,
    purpose: str,
    creator: str,
    tuning_label: str,
    source_id: str,
    source_kind: str,
    source_url: str,
    source_record_id: str,
    redistribution_policy: str,
    source_version: str | None,
    discovered_at_epoch_seconds: int | None,
    verification_status: str | None = None,
) -> dict[str, Any]:
    resolved_purpose = purpose.strip().lower()
    if resolved_purpose not in ALLOWED_PURPOSES:
        raise ValueError("General preset purpose must be explicitly effect or genre")
    if source_kind not in ALLOWED_SOURCE_KINDS:
        raise ValueError(f"Unsupported General preset source kind: {source_kind}")
    if redistribution_policy not in ALLOWED_POLICIES:
        raise ValueError(
            "General preset ingestion requires an explicit safe redistribution policy: "
            "link-only or structured-data-only"
        )
    resolved_creator = creator.strip()
    if not resolved_creator:
        raise ValueError("Creator attribution is required for General preset ingestion")
    resolved_label = tuning_label.strip()
    if not resolved_label:
        raise ValueError("A source-authored General preset label is required")
    if not source_id.strip():
        raise ValueError("source_id is required for provenance")
    if not source_record_id.strip():
        raise ValueError("source_record_id is required for provenance")
    if not source_url.startswith(("https://", "http://")):
        raise ValueError("A source URL is required for provenance")

    resolved_verification = (
        str(verification_status).strip().lower()
        if verification_status is not None
        else DEFAULT_VERIFICATION[source_kind]
    )
    if resolved_verification not in ALLOWED_VERIFICATION:
        raise ValueError("verification_status must be verified or unverified")

    fingerprint = acoustic_fingerprint(parsed)
    publication_eligible = redistribution_policy == "structured-data-only"
    return {
        "canonical_profile_id": canonical_profile_id(
            purpose=resolved_purpose,
            creator=resolved_creator,
            tuning_label=resolved_label,
        ),
        "scope": "general",
        "purpose": resolved_purpose,
        "headphone": None,
        "creator": resolved_creator,
        "target": {"name": None, "kind": "unknown"},
        "tuning_label": resolved_label,
        "publication_eligible": publication_eligible,
        "revisions": [
            {
                "revision_id": "rev-" + fingerprint[:24],
                "acoustic_fingerprint": fingerprint,
                "preamp_gain_db": parsed.preamp_db,
                "filters": parsed.filters if publication_eligible else [],
                "source_references": [
                    {
                        "source_id": source_id.strip(),
                        "source_kind": CANONICAL_SOURCE_KIND[source_kind],
                        "source_record_id": source_record_id.strip(),
                        "url": source_url,
                        "creator": resolved_creator,
                        "provenance_tier": PROVENANCE_TIER[source_kind],
                        "redistribution_policy": redistribution_policy,
                        "published_at_epoch_seconds": None,
                        "updated_at_epoch_seconds": None,
                        "discovered_at_epoch_seconds": discovered_at_epoch_seconds,
                        "last_verified_at_epoch_seconds": (
                            discovered_at_epoch_seconds if resolved_verification == "verified" else None
                        ),
                        "is_primary": True,
                    }
                ],
                "source_version_label": source_version,
                "sound_impact_summary": None,
                "verification_status": resolved_verification,
                "first_seen_at_epoch_seconds": discovered_at_epoch_seconds,
                "source_updated_at_epoch_seconds": None,
                "is_latest": True,
            }
        ],
    }


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("preset", type=Path)
    parser.add_argument("--purpose", required=True, choices=sorted(ALLOWED_PURPOSES))
    parser.add_argument("--creator", required=True)
    parser.add_argument("--tuning-label", required=True)
    parser.add_argument("--source-id", required=True)
    parser.add_argument("--source-kind", required=True, choices=sorted(ALLOWED_SOURCE_KINDS))
    parser.add_argument("--source-url", required=True)
    parser.add_argument("--source-record-id", required=True)
    parser.add_argument("--redistribution-policy", required=True, choices=sorted(ALLOWED_POLICIES))
    parser.add_argument("--verification-status", choices=sorted(ALLOWED_VERIFICATION))
    parser.add_argument("--source-version")
    parser.add_argument("--discovered-at", type=int)
    parser.add_argument("--output", type=Path)
    args = parser.parse_args()

    parsed = parse_peq(args.preset.read_text(encoding="utf-8"))
    candidate = build_candidate(
        parsed,
        purpose=args.purpose,
        creator=args.creator,
        tuning_label=args.tuning_label,
        source_id=args.source_id,
        source_kind=args.source_kind,
        source_url=args.source_url,
        source_record_id=args.source_record_id,
        redistribution_policy=args.redistribution_policy,
        source_version=args.source_version,
        discovered_at_epoch_seconds=args.discovered_at,
        verification_status=args.verification_status,
    )
    payload = json.dumps(candidate, indent=2, sort_keys=True, ensure_ascii=False) + "\n"
    if args.output:
        args.output.parent.mkdir(parents=True, exist_ok=True)
        args.output.write_text(payload, encoding="utf-8")
    else:
        print(payload, end="")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
