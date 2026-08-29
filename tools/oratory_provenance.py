#!/usr/bin/env python3
"""Enrich OPRA-carried oratory1990 presets with direct creator provenance.

EQ Library may legally consume structured EQ values through OPRA while also
preserving the original creator/source link. This helper adds an authoritative,
link-only oratory1990 source reference to profiles that are actually authored by
oratory1990. It intentionally does NOT relabel AutoEq results derived from an
oratory1990 measurement as oratory-authored presets.

The direct reference is provenance only: no filters are copied from the creator
page/PDF and OPRA remains the primary structured-data source for the revision.
"""

from __future__ import annotations

import argparse
import copy
import hashlib
import json
from pathlib import Path
from typing import Any

ORATORY_SOURCE_ID = "oratory1990"
ORATORY_CREATOR = "oratory1990"


def _is_oratory_creator(value: Any) -> bool:
    return str(value or "").strip().casefold() == ORATORY_CREATOR


def _direct_record_id(url: str, fallback: str | None) -> str:
    identity = url.strip() or str(fallback or "").strip()
    digest = hashlib.sha256(identity.encode("utf-8")).hexdigest()[:20]
    return f"oratory1990:{digest}"


def _eligible_base_reference(revision: dict[str, Any]) -> dict[str, Any] | None:
    refs = revision.get("source_references")
    if not isinstance(refs, list):
        return None
    preferred: list[dict[str, Any]] = []
    fallback: list[dict[str, Any]] = []
    for ref in refs:
        if not isinstance(ref, dict):
            continue
        if ref.get("source_id") == ORATORY_SOURCE_ID:
            continue
        if not _is_oratory_creator(ref.get("creator")):
            continue
        url = str(ref.get("url") or "").strip()
        if not url.startswith(("https://", "http://")):
            continue
        if ref.get("source_id") == "opra":
            preferred.append(ref)
        else:
            fallback.append(ref)
    return (preferred or fallback or [None])[0]


def enrich_catalog(snapshot: dict[str, Any]) -> tuple[dict[str, Any], dict[str, int]]:
    enriched = copy.deepcopy(snapshot)
    added = 0
    touched_revisions = 0
    for profile in enriched.get("profiles", []):
        if not isinstance(profile, dict) or not _is_oratory_creator(profile.get("creator")):
            continue
        for revision in profile.get("revisions", []):
            if not isinstance(revision, dict):
                continue
            refs = revision.setdefault("source_references", [])
            if any(isinstance(ref, dict) and ref.get("source_id") == ORATORY_SOURCE_ID for ref in refs):
                continue
            base = _eligible_base_reference(revision)
            if base is None:
                continue
            url = str(base.get("url") or "").strip()
            refs.append(
                {
                    "source_id": ORATORY_SOURCE_ID,
                    "source_kind": "creator",
                    "source_record_id": _direct_record_id(url, base.get("source_record_id")),
                    "source_vendor_id": base.get("source_vendor_id"),
                    "source_product_id": base.get("source_product_id"),
                    "url": url,
                    "creator": ORATORY_CREATOR,
                    "provenance_tier": "authoritative",
                    "redistribution_policy": "link-only",
                    "published_at_epoch_seconds": base.get("published_at_epoch_seconds"),
                    "updated_at_epoch_seconds": base.get("updated_at_epoch_seconds"),
                    "discovered_at_epoch_seconds": base.get("discovered_at_epoch_seconds"),
                    "last_verified_at_epoch_seconds": base.get("last_verified_at_epoch_seconds"),
                    "is_primary": False,
                }
            )
            added += 1
            touched_revisions += 1
    return enriched, {"added_references": added, "touched_revisions": touched_revisions}


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--catalog", type=Path, required=True)
    parser.add_argument("--output", type=Path, required=True)
    parser.add_argument("--report", type=Path)
    args = parser.parse_args()

    with args.catalog.open("r", encoding="utf-8") as handle:
        snapshot = json.load(handle)
    enriched, report = enrich_catalog(snapshot)
    args.output.parent.mkdir(parents=True, exist_ok=True)
    args.output.write_text(json.dumps(enriched, indent=2, ensure_ascii=False) + "\n", encoding="utf-8")
    if args.report:
        args.report.parent.mkdir(parents=True, exist_ok=True)
        args.report.write_text(json.dumps(report, indent=2, sort_keys=True) + "\n", encoding="utf-8")
    else:
        print(json.dumps(report, sort_keys=True))
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
