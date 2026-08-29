#!/usr/bin/env python3
"""Parse AutoEq parametric EQ text into an EQ Library canonical candidate.

This adapter intentionally consumes only the structured filter values plus explicit
metadata supplied by the source watcher. It does not copy prose from upstream
README files. The caller is responsible for selecting an AutoEq result path and
supplying measurement/source metadata. Target metadata is optional because some
precomputed AutoEq result files do not identify the target inside the preset itself.
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
    r"^Filter\s+\d+:\s+ON\s+(?P<type>\S+)\s+Fc\s+(?P<frequency>[+-]?\d+(?:\.\d+)?)\s+Hz\s+"
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


@dataclass(frozen=True)
class ParsedAutoEq:
    preamp_db: float | None
    filters: list[dict[str, Any]]


def parse_parametric_eq(text: str) -> ParsedAutoEq:
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
            raise ValueError(f"Unsupported AutoEq line: {line}")
        filter_type = TYPE_MAP.get(filter_match.group("type").upper())
        if filter_type is None:
            raise ValueError(f"Unsupported AutoEq filter type: {filter_match.group('type')}")
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
        raise ValueError("AutoEq preset contains no enabled parametric filters")
    return ParsedAutoEq(preamp_db=preamp, filters=filters)


def acoustic_fingerprint(parsed: ParsedAutoEq) -> str:
    return canonical_acoustic_fingerprint(parsed.preamp_db, parsed.filters)


def build_candidate(
    parsed: ParsedAutoEq,
    *,
    manufacturer: str,
    model: str,
    measurement_source: str,
    target: str | None,
    source_url: str,
    source_record_id: str,
    source_version: str | None,
    discovered_at_epoch_seconds: int | None,
) -> dict[str, Any]:
    fingerprint = acoustic_fingerprint(parsed)
    target_name = target.strip() if target and target.strip() else None
    identity_target = target_name or "unknown-target"
    identity = f"{manufacturer.strip()}|{model.strip()}|AutoEq|{measurement_source.strip()}|{identity_target}"
    canonical_id = "autoeq-" + hashlib.sha256(identity.lower().encode("utf-8")).hexdigest()[:24]
    revision_id = "rev-" + fingerprint[:24]
    return {
        "canonical_profile_id": canonical_id,
        "headphone": {
            "manufacturer": manufacturer.strip(),
            "model": model.strip(),
            "variant": None,
            "pads_or_mode": None,
        },
        "creator": "AutoEq",
        "target": {
            "name": target_name,
            "kind": "explicit_target" if target_name else "unknown",
        },
        "tuning_label": f"AutoEq ({measurement_source.strip()} measurement)",
        "revisions": [
            {
                "revision_id": revision_id,
                "acoustic_fingerprint": fingerprint,
                "preamp_gain_db": parsed.preamp_db,
                "filters": parsed.filters,
                "source_references": [
                    {
                        "source_id": "autoeq",
                        "source_kind": "measurement_derived",
                        "source_record_id": source_record_id,
                        "source_vendor_id": manufacturer.strip(),
                        "source_product_id": model.strip(),
                        "url": source_url,
                        "creator": "AutoEq",
                        "provenance_tier": "measurement_derived",
                        "redistribution_policy": "structured-data-only",
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
    parser.add_argument("--measurement-source", required=True)
    parser.add_argument("--target")
    parser.add_argument("--source-url", required=True)
    parser.add_argument("--source-record-id", required=True)
    parser.add_argument("--source-version")
    parser.add_argument("--discovered-at", type=int)
    parser.add_argument("--output", type=Path)
    args = parser.parse_args()

    parsed = parse_parametric_eq(args.preset.read_text(encoding="utf-8"))
    candidate = build_candidate(
        parsed,
        manufacturer=args.manufacturer,
        model=args.model,
        measurement_source=args.measurement_source,
        target=args.target,
        source_url=args.source_url,
        source_record_id=args.source_record_id,
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
