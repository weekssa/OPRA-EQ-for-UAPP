#!/usr/bin/env python3
"""Publish curated community EQ candidates with arbitrary filter counts and tonal summaries.

This importer never invents missing filters. When a source omits preamp, it calculates
conservative playback headroom from the combined biquad response and records that fact
in the generated sound-impact summary. Publication remains gated by source registry
redistribution policy and canonical dedupe.

Mirror/reference posts are retained as secondary provenance on the original tuning rather
than being published as duplicate presets.
"""

from __future__ import annotations

import argparse
import cmath
import json
import math
from pathlib import Path
from typing import Any

from catalog_merge import merge_candidates
from community_peq_ingest import ParsedPeq, build_candidate

TYPE_MAP = {"PK": "peak", "PEQ": "peak", "LSC": "low_shelf", "LS": "low_shelf", "HSC": "high_shelf", "HS": "high_shelf"}
SOURCE_ID = {
    "reddit": "reddit-audio",
    "head-fi": "head-fi",
    "headphones-community": "headphones-community",
    "audio-science-review": "audio-science-review",
    "topping-community": "topping-community",
}
ELIGIBLE_STATUSES = {"publish-candidate", "manual-review"}
REFERENCE_STATUSES = {"duplicate-reference"}
BANDS = [
    ("sub-bass", 20.0, 80.0),
    ("bass", 80.0, 250.0),
    ("lower mids", 250.0, 1000.0),
    ("upper mids", 1000.0, 3000.0),
    ("presence", 3000.0, 6000.0),
    ("treble", 6000.0, 10000.0),
    ("air", 10000.0, 20000.0),
]


def registry_policies(payload: dict[str, Any]) -> dict[str, str]:
    return {str(item.get("id")): str(item.get("redistribution")) for item in payload.get("sources") or []}


def normalized_filters(record: dict[str, Any]) -> list[dict[str, Any]]:
    result: list[dict[str, Any]] = []
    for item in record.get("filters") or []:
        kind = TYPE_MAP.get(str(item.get("type") or "").upper())
        if not kind:
            raise ValueError(f"unsupported filter type {item.get('type')!r}")
        result.append({
            "type": kind,
            "frequency_hz": float(item["frequency_hz"]),
            "gain_db": float(item["gain_db"]),
            "q": float(item["q"]),
            "slope": None,
        })
    if not result:
        raise ValueError("no filters")
    return result


def biquad(kind: str, f0: float, gain_db: float, q: float, fs: float = 96000.0) -> tuple[list[float], list[float]]:
    w0 = 2.0 * math.pi * min(max(f0, 1.0), fs * 0.49) / fs
    cw, sw = math.cos(w0), math.sin(w0)
    q = max(q, 0.05)
    alpha = sw / (2.0 * q)
    A = 10.0 ** (gain_db / 40.0)
    if kind == "peak":
        b0, b1, b2 = 1 + alpha * A, -2 * cw, 1 - alpha * A
        a0, a1, a2 = 1 + alpha / A, -2 * cw, 1 - alpha / A
    elif kind == "low_shelf":
        s = 2.0 * math.sqrt(A) * alpha
        b0 = A * ((A + 1) - (A - 1) * cw + s)
        b1 = 2 * A * ((A - 1) - (A + 1) * cw)
        b2 = A * ((A + 1) - (A - 1) * cw - s)
        a0 = (A + 1) + (A - 1) * cw + s
        a1 = -2 * ((A - 1) + (A + 1) * cw)
        a2 = (A + 1) + (A - 1) * cw - s
    elif kind == "high_shelf":
        s = 2.0 * math.sqrt(A) * alpha
        b0 = A * ((A + 1) + (A - 1) * cw + s)
        b1 = -2 * A * ((A - 1) + (A + 1) * cw)
        b2 = A * ((A + 1) + (A - 1) * cw - s)
        a0 = (A + 1) - (A - 1) * cw + s
        a1 = 2 * ((A - 1) - (A + 1) * cw)
        a2 = (A + 1) - (A - 1) * cw - s
    else:
        raise ValueError(kind)
    return [b0 / a0, b1 / a0, b2 / a0], [1.0, a1 / a0, a2 / a0]


def response_db(filters: list[dict[str, Any]], freq: float, fs: float = 96000.0) -> float:
    z1 = cmath.exp(-1j * 2.0 * math.pi * freq / fs)
    z2 = z1 * z1
    total = 1.0 + 0j
    for filt in filters:
        b, a = biquad(filt["type"], filt["frequency_hz"], filt["gain_db"], filt["q"], fs)
        total *= (b[0] + b[1] * z1 + b[2] * z2) / (a[0] + a[1] * z1 + a[2] * z2)
    return 20.0 * math.log10(max(abs(total), 1e-12))


def frequency_grid() -> list[float]:
    return [20.0 * (1000.0 ** (i / 240.0)) for i in range(241)]


def signature(filters: list[dict[str, Any]]) -> tuple[str, float, dict[str, float]]:
    grid = frequency_grid()
    values = [(f, response_db(filters, f)) for f in grid]
    max_boost = max(db for _, db in values)
    means: dict[str, float] = {}
    for name, low, high in BANDS:
        band = [db for f, db in values if low <= f < high or (name == "air" and f <= high)]
        means[name] = sum(band) / len(band)
    lifted = sorted(means.items(), key=lambda item: item[1], reverse=True)
    cut = sorted(means.items(), key=lambda item: item[1])
    descriptors: list[str] = []
    if means["sub-bass"] > 1.0 or means["bass"] > 1.0:
        descriptors.append("fuller/stronger bass")
    if means["upper mids"] > 1.0:
        descriptors.append("more forward upper mids")
    if means["presence"] < -1.0:
        descriptors.append("softer presence")
    if means["treble"] < -1.0 or means["air"] < -1.0:
        descriptors.append("reduced treble energy")
    if means["treble"] > 1.0 or means["air"] > 1.0:
        descriptors.append("brighter upper treble")
    tone = ", ".join(descriptors) if descriptors else "primarily localized corrections rather than a broad tonal tilt"
    summary = (
        f"Estimated signature: {tone}. Strongest broad lift: {lifted[0][0]} {lifted[0][1]:+.1f} dB; "
        f"strongest broad reduction: {cut[0][0]} {cut[0][1]:+.1f} dB. "
        f"Analysis preserves the source's original {len(filters)} filters; no missing bands were invented."
    )
    return summary, max_boost, means


def shape_rms(a: list[dict[str, Any]], b: list[dict[str, Any]]) -> float:
    diffs = [response_db(a, f) - response_db(b, f) for f in frequency_grid()]
    offset = sum(diffs) / len(diffs)
    return math.sqrt(sum((value - offset) ** 2 for value in diffs) / len(diffs))


def edition_xs_revisions(snapshot: dict[str, Any]) -> list[tuple[str, list[dict[str, Any]]]]:
    result: list[tuple[str, list[dict[str, Any]]]] = []
    for profile in snapshot.get("profiles") or []:
        hp = profile.get("headphone") or {}
        if str(hp.get("manufacturer") or "").lower() != "hifiman" or str(hp.get("model") or "").lower() != "edition xs":
            continue
        for rev in profile.get("revisions") or []:
            filters = rev.get("filters") or []
            if filters:
                result.append((str(profile.get("canonical_profile_id")), filters))
    return result


def attach_mirror_reference(snapshot: dict[str, Any], record: dict[str, Any], source_id: str) -> str | None:
    """Attach a forum mirror as secondary provenance without creating a duplicate EQ."""
    lineage = str(record.get("lineage") or "").lower()
    if "oratory1990" not in lineage:
        return None
    for profile in snapshot.get("profiles") or []:
        hp = profile.get("headphone") or {}
        if str(hp.get("manufacturer") or "").lower() != "hifiman" or str(hp.get("model") or "").lower() != "edition xs":
            continue
        for revision in profile.get("revisions") or []:
            refs = revision.get("source_references") or []
            if not any(str(ref.get("source_id") or "") == "oratory1990" for ref in refs):
                continue
            key = (source_id, str(record.get("id") or ""), str(record.get("source_url") or ""))
            if any((str(ref.get("source_id") or ""), str(ref.get("source_record_id") or ""), str(ref.get("url") or "")) == key for ref in refs):
                return str(profile.get("canonical_profile_id") or "")
            refs.append({
                "source_id": source_id,
                "source_kind": "community",
                "source_record_id": str(record.get("id") or ""),
                "source_vendor_id": "HIFIMAN",
                "source_product_id": "Edition XS",
                "url": str(record.get("source_url") or ""),
                "creator": str(record.get("creator") or "Community"),
                "provenance_tier": "mirror",
                "redistribution_policy": "structured-data-only",
                "published_at_epoch_seconds": None,
                "updated_at_epoch_seconds": None,
                "discovered_at_epoch_seconds": None,
                "last_verified_at_epoch_seconds": None,
                "is_primary": False,
            })
            revision["source_references"] = sorted(
                refs,
                key=lambda item: (
                    str(item.get("source_id") or ""),
                    str(item.get("source_record_id") or ""),
                    str(item.get("url") or ""),
                ),
            )
            return str(profile.get("canonical_profile_id") or "")
    return None


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--catalog", type=Path, required=True)
    parser.add_argument("--curated", type=Path, required=True)
    parser.add_argument("--registry", type=Path, required=True)
    parser.add_argument("--output", type=Path, required=True)
    parser.add_argument("--report", type=Path, required=True)
    parser.add_argument("--near-duplicate-rms-db", type=float, default=0.35)
    args = parser.parse_args()

    snapshot = json.loads(args.catalog.read_text(encoding="utf-8"))
    curated = json.loads(args.curated.read_text(encoding="utf-8"))
    registry = json.loads(args.registry.read_text(encoding="utf-8"))
    policies = registry_policies(registry)
    existing = edition_xs_revisions(snapshot)
    candidates: list[dict[str, Any]] = []
    report: dict[str, Any] = {"headphone": curated.get("headphone"), "records": [], "published_candidates": 0, "provenance_references_added": 0}

    for record in curated.get("records") or []:
        row: dict[str, Any] = {"id": record.get("id"), "creator": record.get("creator"), "surface": record.get("surface"), "input_status": record.get("status")}
        source_id = SOURCE_ID.get(str(record.get("surface") or ""))
        if record.get("status") in REFERENCE_STATUSES:
            if source_id and policies.get(source_id) == "structured-data-only":
                profile_id = attach_mirror_reference(snapshot, record, source_id)
                if profile_id:
                    row["decision"] = "attach-secondary-provenance"
                    row["canonical_profile_id"] = profile_id
                    report["provenance_references_added"] += 1
                else:
                    row["decision"] = "skip-reference-no-canonical-match"
            else:
                row["decision"] = "hold-source-policy"
                row["source_id"] = source_id
                row["policy"] = policies.get(source_id)
            report["records"].append(row)
            continue
        if record.get("status") not in ELIGIBLE_STATUSES:
            row["decision"] = "skip-status"
            report["records"].append(row)
            continue
        if not source_id or policies.get(source_id) != "structured-data-only":
            row["decision"] = "hold-source-policy"
            row["source_id"] = source_id
            row["policy"] = policies.get(source_id)
            report["records"].append(row)
            continue
        try:
            filters = normalized_filters(record)
        except (ValueError, KeyError, TypeError) as exc:
            row["decision"] = "reject-invalid-structure"
            row["reason"] = str(exc)
            report["records"].append(row)
            continue

        sound_summary, max_boost, band_means = signature(filters)
        source_preamp = record.get("preamp_db")
        generated_headroom = source_preamp is None
        preamp = float(source_preamp) if source_preamp is not None else -math.ceil(max(0.0, max_boost) * 10.0) / 10.0
        nearest_id, nearest_rms = None, None
        if existing:
            nearest_id, nearest_filters = min(existing, key=lambda item: shape_rms(filters, item[1]))
            nearest_rms = shape_rms(filters, nearest_filters)
        row["filter_count"] = len(filters)
        row["preamp_db"] = preamp
        row["preamp_origin"] = "eq-library-safe-headroom" if generated_headroom else "source"
        row["estimated_band_changes_db"] = {key: round(value, 2) for key, value in band_means.items()}
        row["nearest_existing_profile_id"] = nearest_id
        row["nearest_shape_rms_db"] = round(nearest_rms, 3) if nearest_rms is not None else None
        if record.get("status") == "manual-review" and nearest_rms is not None and nearest_rms < args.near_duplicate_rms_db:
            row["decision"] = "skip-near-duplicate"
            report["records"].append(row)
            continue

        parsed = ParsedPeq(preamp_db=preamp, filters=filters)
        label = f"{record.get('creator')} community tuning"
        candidate = build_candidate(
            parsed,
            manufacturer="HIFIMAN",
            model="Edition XS",
            creator=str(record.get("creator") or "Community"),
            tuning_label=label,
            source_id=source_id,
            source_kind="community",
            source_url=str(record.get("source_url")),
            source_record_id=str(record.get("id")),
            redistribution_policy="structured-data-only",
            target=None,
            variant=None,
            source_version=str(record.get("source_date") or "") or None,
            discovered_at_epoch_seconds=None,
        )
        if generated_headroom:
            sound_summary += f" Source omitted preamp; EQ Library calculated {preamp:+.1f} dB safety headroom from the combined response."
        candidate["revisions"][0]["sound_impact_summary"] = sound_summary
        candidates.append(candidate)
        row["decision"] = "publish-candidate"
        row["sound_impact_summary"] = sound_summary
        report["records"].append(row)

    if candidates:
        merged, outcomes = merge_candidates(snapshot, candidates, source_registry_version=str(registry.get("registry_version") or ""))
    else:
        merged, outcomes = snapshot, {}
    report["published_candidates"] = len(candidates)
    report["merge_outcomes"] = outcomes
    args.output.parent.mkdir(parents=True, exist_ok=True)
    args.output.write_text(json.dumps(merged, indent=2, ensure_ascii=False) + "\n", encoding="utf-8")
    args.report.parent.mkdir(parents=True, exist_ok=True)
    args.report.write_text(json.dumps(report, indent=2, sort_keys=True, ensure_ascii=False) + "\n", encoding="utf-8")
    print(json.dumps(report, sort_keys=True))
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
