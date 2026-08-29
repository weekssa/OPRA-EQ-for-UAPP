#!/usr/bin/env python3
"""Convert attributed community PEQ text into a canonical EQ Library candidate.

This adapter is intentionally conservative. It consumes only structured PEQ values
and caller-supplied provenance metadata. It never copies surrounding post prose.
Redistribution policy must be explicitly supplied by the source registry/caller so
link-only sources remain discovery/provenance candidates but are not publishable
Android catalog profiles until structured filters may legally be redistributed.
"""

from __future__ import annotations

import argparse
import hashlib
import json
import re
from dataclasses import dataclass
from pathlib import Path
from typing import Any

from acoustic_fingerprint import acoustic_fingerprint as canonical_acoustic_fingerprint

PREAMP_RE = re.compile(r"^Preamp:\s*([+-]?\d+(?:\.\d+)?)\s*dB\s*$", re.IGNORECASE)
FILTER_RE = re.compile(
    r"^Filter\s+\d+:\s+(?:ON\s+)?(?P<type>\S+)\s+Fc\s+(?P<frequency>[+-]?\d+(?:\.\d+)?)\s+Hz\s+"
    r"Gain\s+(?P<gain>[+-]?\d+(?:\.\d+)?)\s+dB\s+Q\s+(?P<q>[+-]?\d+(?:\.\d+)?)\s*$",
    re.IGNORECASE,
)
TYPE_MAP = {
    "PK": "peak",
    "PEQ": "peak",
    "LSC": "low_shelf",
    "LS": "low_shelf",
    "HSC": "high_shelf",
    "HS": "high_shelf",
}
ALLOWED_POLICIES = {"link-only", "structured-data-only"}
ALLOWED_SOURCE_KINDS = {
    "community",
    "community_repository",
    "creator",
    "device_community",
}
CANONICAL_SOURCE_KIND = {
    "community": "community",
    "community_repository": "repository",
    "creator": "creator",
    "device_community": "device_community",
}
PROVENANCE_TIER = {
    "community": "traceable_community",
    "community_repository": "traceable_community",
    "creator": "authoritative",
    "device_community": "traceable_community",
}


@dataclass(frozen=True)
class ParsedPeq:
    preamp_db: float | None
    filters: list[dict[str, Any]]


def parse_peq(text: str) -> ParsedPeq:
    preamp: float | None = None
    filters: list[dict[str, Any]] = []
    for raw_line in text.splitlines():
        line = raw_line.strip()
        if not line:
            continue
        preamp_match = PREAMP_RE.match(line)
        if preamp_match:
            preamp = float(preamp_match.group(1))
            continue
        filter_match = FILTER_RE.match(line)
        if not filter_match:
            raise ValueError(f"Unsupported/non-PEQ line: {line}")
        filter_type = TYPE_MAP.get(filter_match.group("type").upper())
        if filter_type is None:
            raise ValueError(f"Unsupported PEQ filter type: {filter_match.group('type')}")
        filters.append(
            {
                "type": filter_type,
                "frequency_hz": float(filter_match.group("frequency")),
                "gain_db": float(filter_match.group("gain")),
                "q": float(filter_match.group("q")),
                "slope": None,
            }
        )
    if not filters:
        raise ValueError("Community preset contains no supported parametric filters")
    return ParsedPeq(preamp_db=preamp, filters=filters)


def acoustic_fingerprint(parsed: ParsedPeq) -> str:
    return canonical_acoustic_fingerprint(parsed.preamp_db, parsed.filters)


def build_candidate(
    parsed: ParsedPeq,
    *,
    manufacturer: str,
    model: str,
    creator: str,
    tuning_label: str,
    source_id: str,
    source_kind: str,
    source_url: str,
    source_record_id: str,
    redistribution_policy: str,
    target: str | None,
    variant: str | None,
    source_version: str | None,
    discovered_at_epoch_seconds: int | None,
) -> dict[str, Any]:
    if source_kind not in ALLOWED_SOURCE_KINDS:
        raise ValueError(f"Unsupported community source kind: {source_kind}")
    if redistribution_policy not in ALLOWED_POLICIES:
        raise ValueError(
            "Community ingestion requires an explicit safe redistribution policy: "
            "link-only or structured-data-only"
        )
    if not creator.strip():
        raise ValueError("Creator attribution is required for community ingestion")
    if not source_url.startswith(("https://", "http://")):
        raise ValueError("A source URL is required for provenance")

    fingerprint = acoustic_fingerprint(parsed)
    identity = "|".join(
        [
            manufacturer.strip(),
            model.strip(),
            variant.strip() if variant else "",
            creator.strip(),
            tuning_label.strip(),
        ]
    )
    canonical_id = "community-" + hashlib.sha256(identity.lower().encode("utf-8")).hexdigest()[:24]
    revision_id = "rev-" + fingerprint[:24]
    target_name = target.strip() if target and target.strip() else None
    publication_eligible = redistribution_policy == "structured-data-only"
    return {
        "canonical_profile_id": canonical_id,
        "headphone": {
            "manufacturer": manufacturer.strip(),
            "model": model.strip(),
            "variant": variant.strip() if variant else None,
            "pads_or_mode": None,
        },
        "creator": creator.strip(),
        "target": {
            "name": target_name,
            "kind": "explicit_target" if target_name else "unknown",
        },
        "tuning_label": tuning_label.strip(),
        "publication_eligible": publication_eligible,
        "revisions": [
            {
                "revision_id": revision_id,
                "acoustic_fingerprint": fingerprint,
                "preamp_gain_db": parsed.preamp_db,
                "filters": parsed.filters if publication_eligible else [],
                "source_references": [
                    {
                        "source_id": source_id,
                        "source_kind": CANONICAL_SOURCE_KIND[source_kind],
                        "source_record_id": source_record_id,
                        "source_vendor_id": manufacturer.strip(),
                        "source_product_id": model.strip(),
                        "url": source_url,
                        "creator": creator.strip(),
                        "provenance_tier": PROVENANCE_TIER[source_kind],
                        "redistribution_policy": redistribution_policy,
                        "published_at_epoch_seconds": None,
                        "updated_at_epoch_seconds": None,
                        "discovered_at_epoch_seconds": discovered_at_epoch_seconds,
                        "last_verified_at_epoch_seconds": discovered_at_epoch_seconds,
                        "is_primary": True,
                    }
                ],
                "source_version_label": source_version,
                "sound_impact_summary": None,
                "first_seen_at_epoch_seconds": discovered_at_epoch_seconds,
                "source_updated_at_epoch_seconds": None,
                "is_latest": True,
            }
        ],
    }


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("preset", type=Path)
    parser.add_argument("--manufacturer", required=True)
    parser.add_argument("--model", required=True)
    parser.add_argument("--variant")
    parser.add_argument("--creator", required=True)
    parser.add_argument("--tuning-label", required=True)
    parser.add_argument("--target")
    parser.add_argument("--source-id", required=True)
    parser.add_argument("--source-kind", required=True, choices=sorted(ALLOWED_SOURCE_KINDS))
    parser.add_argument("--source-url", required=True)
    parser.add_argument("--source-record-id", required=True)
    parser.add_argument("--redistribution-policy", required=True, choices=sorted(ALLOWED_POLICIES))
    parser.add_argument("--source-version")
    parser.add_argument("--discovered-at", type=int)
    parser.add_argument("--output", type=Path)
    args = parser.parse_args()

    parsed = parse_peq(args.preset.read_text(encoding="utf-8"))
    candidate = build_candidate(
        parsed,
        manufacturer=args.manufacturer,
        model=args.model,
        variant=args.variant,
        creator=args.creator,
        tuning_label=args.tuning_label,
        target=args.target,
        source_id=args.source_id,
        source_kind=args.source_kind,
        source_url=args.source_url,
        source_record_id=args.source_record_id,
        redistribution_policy=args.redistribution_policy,
        source_version=args.source_version,
        discovered_at_epoch_seconds=args.discovered_at,
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
