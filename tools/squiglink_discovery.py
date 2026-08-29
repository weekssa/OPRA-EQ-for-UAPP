#!/usr/bin/env python3
"""Normalize Squiglink/CrinGraph-compatible phone-book metadata into review candidates.

This is a discovery/qualification adapter, not a measurement-data republisher. The
Squiglink server software is permissively licensed, but measurement/database rights
can belong to individual creators or hosted databases. Therefore discovered records
are never Android-catalog publishable until the specific source terms and attribution
are qualified separately.
"""

from __future__ import annotations

import argparse
import hashlib
import json
from pathlib import Path
from typing import Any
from urllib.parse import quote


def stable_candidate_id(source_id: str, manufacturer: str, model: str, file_stem: str) -> str:
    identity = "|".join((source_id.strip().lower(), manufacturer.strip().lower(), model.strip().lower(), file_stem.strip().lower()))
    return "squiglink-" + hashlib.sha256(identity.encode("utf-8")).hexdigest()[:24]


def measurement_url(base_url: str, file_stem: str) -> str:
    return base_url.rstrip("/") + "/data/" + quote(file_stem.strip(), safe="") + ".txt"


def discover_phone_book(
    payload: Any,
    *,
    source_id: str,
    source_name: str,
    base_url: str,
    creator: str | None = None,
    source_revision: str | None = None,
) -> list[dict[str, Any]]:
    if not isinstance(payload, list):
        raise ValueError("Squiglink phone_book payload must be a list of brand objects")
    if not source_id.strip() or not source_name.strip():
        raise ValueError("source_id and source_name are required")
    if not base_url.startswith(("https://", "http://")):
        raise ValueError("base_url must be an http(s) URL")

    candidates: dict[str, dict[str, Any]] = {}
    for brand in payload:
        if not isinstance(brand, dict):
            continue
        manufacturer = str(brand.get("name") or "").strip()
        phones = brand.get("phones")
        if not manufacturer or not isinstance(phones, list):
            continue
        for phone in phones:
            if not isinstance(phone, dict):
                continue
            model = str(phone.get("name") or "").strip()
            file_stem = str(phone.get("file") or "").strip()
            if not model or not file_stem:
                continue
            record_url = measurement_url(base_url, file_stem)
            candidate_id = stable_candidate_id(source_id, manufacturer, model, file_stem)
            candidates[candidate_id] = {
                "candidate_id": candidate_id,
                "source_id": source_id.strip(),
                "source_name": source_name.strip(),
                "source_kind": "structured_measurement",
                "manufacturer": manufacturer,
                "model": model,
                "file_stem": file_stem,
                "url": record_url,
                "creator": creator.strip() if creator and creator.strip() else None,
                "source_revision": source_revision,
                "status": "new_candidate",
                "redistribution": "review-required",
                "publication_eligible": False,
                "license_review_required": True,
                "qualification_required": [
                    "database_or_creator_terms",
                    "creator_attribution",
                    "headphone_identity",
                    "measurement_format_validation",
                    "eq_derivation_policy",
                    "canonical_dedupe",
                ],
            }
    return sorted(candidates.values(), key=lambda item: (item["manufacturer"].lower(), item["model"].lower(), item["url"]))


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("phone_book", type=Path)
    parser.add_argument("--source-id", required=True)
    parser.add_argument("--source-name", required=True)
    parser.add_argument("--base-url", required=True)
    parser.add_argument("--creator")
    parser.add_argument("--source-revision")
    parser.add_argument("--output", type=Path)
    args = parser.parse_args()

    payload = json.loads(args.phone_book.read_text(encoding="utf-8"))
    candidates = discover_phone_book(
        payload,
        source_id=args.source_id,
        source_name=args.source_name,
        base_url=args.base_url,
        creator=args.creator,
        source_revision=args.source_revision,
    )
    result = {
        "schema_version": 1,
        "source_id": args.source_id,
        "candidate_count": len(candidates),
        "candidates": candidates,
    }
    rendered = json.dumps(result, indent=2, sort_keys=True, ensure_ascii=False) + "\n"
    if args.output:
        args.output.parent.mkdir(parents=True, exist_ok=True)
        args.output.write_text(rendered, encoding="utf-8")
    else:
        print(rendered, end="")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
