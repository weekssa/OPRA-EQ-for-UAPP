#!/usr/bin/env python3
"""Publish curated community EQ candidates with source-authentic canonical data.

The curated input declares one headphone identity and any number of attributed public
community records for that headphone. The importer preserves the source filter list and
source preamp exactly. If the source omits preamp, EQ Library calculates conservative
playback headroom separately and stores it as ``eq_library_safety_headroom_db``; the
canonical ``preamp_gain_db`` remains null.

Mirror/reference posts are retained as secondary provenance on an existing tuning rather
than becoming duplicate presets. Publication remains gated by source-registry policy and
canonical acoustic deduplication.
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

TYPE_MAP = {
    "PK": "peak",
    "PEQ": "peak",
    "LSC": "low_shelf",
    "LS": "low_shelf",
    "HSC": "high_shelf",
    "HS": "high_shelf",
}
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
    return {
        str(item.get("id")): str(item.get("redistribution"))
        for item in payload.get("sources") or []
    }


def curated_headphone_identity(curated: dict[str, Any]) -> tuple[str, str, str | None]:
    headphone = curated.get("headphone") or {}
    manufacturer = str(headphone.get("manufacturer") or "").strip()
    model = str(headphone.get("model") or "").strip()
    variant = str(headphone.get("variant") or "").strip() or None
    if not manufacturer or not model:
        raise ValueError("curated input must declare headphone manufacturer and model")
    return manufacturer, model, variant


def normalized_filters(record: dict[str, Any]) -> list[dict[str, Any]]:
    result: list[dict[str, Any]] = []
    for item in record.get("filters") or []:
        kind = TYPE_MAP.get(str(item.get("type") or "").upper())
        if not kind:
            raise ValueError(f"unsupported filter type {item.get('type')!r}")
        result.append(
            {
                "type": kind,
                "frequency_hz": float(item["frequency_hz"]),
                "gain_db": float(item["gain_db"]),
                "q": float(item["q"]),
                "slope": None,
            }
        )
    if not result:
        raise ValueError("no filters")
    return result


def biquad(
    kind: str,
    f0: float,
    gain_db: float,
    q: float,
    fs: float = 96000.0,
) -> tuple[list[float], list[float]]:
    w0 = 2.0 * math.pi * min(max(f0, 1.0), fs * 0.49) / fs
    cw, sw = math.cos(w0), math.sin(w0)
    q = max(q, 0.05)
    alpha = sw / (2.0 * q)
    amplitude = 10.0 ** (gain_db / 40.0)

    if kind == "peak":
        b0, b1, b2 = 1 + alpha * amplitude, -2 * cw, 1 - alpha * amplitude
        a0, a1, a2 = 1 + alpha / amplitude, -2 * cw, 1 - alpha / amplitude
    elif kind == "low_shelf":
        shelf = 2.0 * math.sqrt(amplitude) * alpha
        b0 = amplitude * ((amplitude + 1) - (amplitude - 1) * cw + shelf)
        b1 = 2 * amplitude * ((amplitude - 1) - (amplitude + 1) * cw)
        b2 = amplitude * ((amplitude + 1) - (amplitude - 1) * cw - shelf)
        a0 = (amplitude + 1) + (amplitude - 1) * cw + shelf
        a1 = -2 * ((amplitude - 1) + (amplitude + 1) * cw)
        a2 = (amplitude + 1) + (amplitude - 1) * cw - shelf
    elif kind == "high_shelf":
        shelf = 2.0 * math.sqrt(amplitude) * alpha
        b0 = amplitude * ((amplitude + 1) + (amplitude - 1) * cw + shelf)
        b1 = -2 * amplitude * ((amplitude - 1) + (amplitude + 1) * cw)
        b2 = amplitude * ((amplitude + 1) + (amplitude - 1) * cw - shelf)
        a0 = (amplitude + 1) - (amplitude - 1) * cw + shelf
        a1 = 2 * ((amplitude - 1) - (amplitude + 1) * cw)
        a2 = (amplitude + 1) - (amplitude - 1) * cw - shelf
    else:
        raise ValueError(kind)
    return [b0 / a0, b1 / a0, b2 / a0], [1.0, a1 / a0, a2 / a0]


def response_db(filters: list[dict[str, Any]], freq: float, fs: float = 96000.0) -> float:
    z1 = cmath.exp(-1j * 2.0 * math.pi * freq / fs)
    z2 = z1 * z1
    total = 1.0 + 0j
    for filt in filters:
        b, a = biquad(
            filt["type"],
            filt["frequency_hz"],
            filt["gain_db"],
            filt["q"],
            fs,
        )
        total *= (b[0] + b[1] * z1 + b[2] * z2) / (a[0] + a[1] * z1 + a[2] * z2)
    return 20.0 * math.log10(max(abs(total), 1e-12))


def frequency_grid() -> list[float]:
    return [20.0 * (1000.0 ** (index / 240.0)) for index in range(241)]


def signature(filters: list[dict[str, Any]]) -> tuple[str, float, dict[str, float]]:
    values = [(frequency, response_db(filters, frequency)) for frequency in frequency_grid()]
    max_boost = max(db for _, db in values)
    means: dict[str, float] = {}
    for name, low, high in BANDS:
        band = [
            db
            for frequency, db in values
            if low <= frequency < high or (name == "air" and frequency <= high)
        ]
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

    tone = (
        ", ".join(descriptors)
        if descriptors
        else "primarily localized corrections rather than a broad tonal tilt"
    )
    summary = (
        f"Estimated signature: {tone}. Strongest broad lift: {lifted[0][0]} {lifted[0][1]:+.1f} dB; "
        f"strongest broad reduction: {cut[0][0]} {cut[0][1]:+.1f} dB. "
        f"Analysis preserves the source's original {len(filters)} filters; no missing bands were invented."
    )
    return summary, max_boost, means


def safety_headroom_db(max_boost_db: float) -> float:
    """Round conservative generated headroom downward to the next 0.1 dB."""
    return -math.ceil(max(0.0, max_boost_db) * 10.0) / 10.0


def shape_rms(a: list[dict[str, Any]], b: list[dict[str, Any]]) -> float:
    diffs = [response_db(a, frequency) - response_db(b, frequency) for frequency in frequency_grid()]
    offset = sum(diffs) / len(diffs)
    return math.sqrt(sum((value - offset) ** 2 for value in diffs) / len(diffs))


def profile_matches_headphone(
    profile: dict[str, Any],
    manufacturer: str,
    model: str,
    variant: str | None = None,
) -> bool:
    headphone = profile.get("headphone") or {}
    if str(headphone.get("manufacturer") or "").strip().casefold() != manufacturer.strip().casefold():
        return False
    if str(headphone.get("model") or "").strip().casefold() != model.strip().casefold():
        return False
    profile_variant = str(headphone.get("variant") or "").strip()
    if variant is None:
        return not profile_variant
    return profile_variant.casefold() == variant.strip().casefold()


def headphone_revisions(
    snapshot: dict[str, Any],
    manufacturer: str,
    model: str,
    variant: str | None = None,
) -> list[tuple[str, list[dict[str, Any]]]]:
    result: list[tuple[str, list[dict[str, Any]]]] = []
    for profile in snapshot.get("profiles") or []:
        if not profile_matches_headphone(profile, manufacturer, model, variant):
            continue
        for revision in profile.get("revisions") or []:
            filters = revision.get("filters") or []
            if filters:
                result.append((str(profile.get("canonical_profile_id")), filters))
    return result


def attach_mirror_reference(
    snapshot: dict[str, Any],
    record: dict[str, Any],
    source_id: str,
    manufacturer: str,
    model: str,
    variant: str | None = None,
) -> str | None:
    """Attach a forum mirror as secondary provenance without creating a duplicate EQ."""
    lineage = str(record.get("lineage") or "").lower()
    if "oratory1990" not in lineage:
        return None
    for profile in snapshot.get("profiles") or []:
        if not profile_matches_headphone(profile, manufacturer, model, variant):
            continue
        for revision in profile.get("revisions") or []:
            refs = revision.get("source_references") or []
            if not any(str(ref.get("source_id") or "") == "oratory1990" for ref in refs):
                continue
            key = (source_id, str(record.get("id") or ""), str(record.get("source_url") or ""))
            if any(
                (
                    str(ref.get("source_id") or ""),
                    str(ref.get("source_record_id") or ""),
                    str(ref.get("url") or ""),
                )
                == key
                for ref in refs
            ):
                return str(profile.get("canonical_profile_id") or "")
            refs.append(
                {
                    "source_id": source_id,
                    "source_kind": "community",
                    "source_record_id": str(record.get("id") or ""),
                    "source_vendor_id": manufacturer,
                    "source_product_id": model,
                    "url": str(record.get("source_url") or ""),
                    "creator": str(record.get("creator") or "Community"),
                    "provenance_tier": "mirror",
                    "redistribution_policy": "structured-data-only",
                    "published_at_epoch_seconds": None,
                    "updated_at_epoch_seconds": None,
                    "discovered_at_epoch_seconds": None,
                    "last_verified_at_epoch_seconds": None,
                    "is_primary": False,
                }
            )
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


def build_curated_candidate(
    record: dict[str, Any],
    *,
    manufacturer: str,
    model: str,
    variant: str | None,
    source_id: str,
) -> tuple[dict[str, Any], dict[str, Any]]:
    filters = normalized_filters(record)
    sound_summary, max_boost, band_means = signature(filters)
    raw_preamp = record.get("preamp_db")
    source_preamp = float(raw_preamp) if raw_preamp is not None else None
    generated_headroom = safety_headroom_db(max_boost) if source_preamp is None else None

    parsed = ParsedPeq(preamp_db=source_preamp, filters=filters)
    candidate = build_candidate(
        parsed,
        manufacturer=manufacturer,
        model=model,
        creator=str(record.get("creator") or "Community"),
        tuning_label=f"{record.get('creator')} community tuning",
        source_id=source_id,
        source_kind="community",
        source_url=str(record.get("source_url")),
        source_record_id=str(record.get("id")),
        redistribution_policy="structured-data-only",
        target=None,
        variant=variant,
        source_version=str(record.get("source_date") or "") or None,
        discovered_at_epoch_seconds=None,
    )
    revision = candidate["revisions"][0]
    revision["eq_library_safety_headroom_db"] = generated_headroom
    if generated_headroom is not None:
        sound_summary += (
            f" Source omitted preamp; EQ Library calculated {generated_headroom:+.1f} dB "
            "safety headroom separately."
        )
    revision["sound_impact_summary"] = sound_summary
    diagnostics = {
        "filter_count": len(filters),
        "source_preamp_db": source_preamp,
        "eq_library_safety_headroom_db": generated_headroom,
        "preamp_origin": "eq-library-safe-headroom" if generated_headroom is not None else "source",
        "estimated_band_changes_db": {
            key: round(value, 2) for key, value in band_means.items()
        },
        "sound_impact_summary": sound_summary,
    }
    return candidate, diagnostics


def publish_curated(
    snapshot: dict[str, Any],
    curated: dict[str, Any],
    registry: dict[str, Any],
    *,
    near_duplicate_rms_db: float = 0.35,
) -> tuple[dict[str, Any], dict[str, Any]]:
    manufacturer, model, variant = curated_headphone_identity(curated)
    policies = registry_policies(registry)
    existing = headphone_revisions(snapshot, manufacturer, model, variant)
    candidates: list[dict[str, Any]] = []
    report: dict[str, Any] = {
        "headphone": curated.get("headphone"),
        "records": [],
        "published_candidates": 0,
        "provenance_references_added": 0,
    }

    for record in curated.get("records") or []:
        row: dict[str, Any] = {
            "id": record.get("id"),
            "creator": record.get("creator"),
            "surface": record.get("surface"),
            "input_status": record.get("status"),
        }
        source_id = SOURCE_ID.get(str(record.get("surface") or ""))

        if record.get("status") in REFERENCE_STATUSES:
            if source_id and policies.get(source_id) == "structured-data-only":
                profile_id = attach_mirror_reference(
                    snapshot,
                    record,
                    source_id,
                    manufacturer,
                    model,
                    variant,
                )
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
            candidate, diagnostics = build_curated_candidate(
                record,
                manufacturer=manufacturer,
                model=model,
                variant=variant,
                source_id=source_id,
            )
        except (ValueError, KeyError, TypeError) as exc:
            row["decision"] = "reject-invalid-structure"
            row["reason"] = str(exc)
            report["records"].append(row)
            continue

        filters = candidate["revisions"][0]["filters"]
        nearest_id, nearest_rms = None, None
        if existing:
            nearest_id, nearest_filters = min(
                existing,
                key=lambda item: shape_rms(filters, item[1]),
            )
            nearest_rms = shape_rms(filters, nearest_filters)
        row.update(diagnostics)
        row["nearest_existing_profile_id"] = nearest_id
        row["nearest_shape_rms_db"] = round(nearest_rms, 3) if nearest_rms is not None else None

        if (
            record.get("status") == "manual-review"
            and nearest_rms is not None
            and nearest_rms < near_duplicate_rms_db
        ):
            row["decision"] = "skip-near-duplicate"
            report["records"].append(row)
            continue

        candidates.append(candidate)
        row["decision"] = "publish-candidate"
        report["records"].append(row)

    if candidates:
        merged, outcomes = merge_candidates(
            snapshot,
            candidates,
            source_registry_version=str(registry.get("registry_version") or ""),
        )
    else:
        merged, outcomes = snapshot, {}
    report["published_candidates"] = len(candidates)
    report["merge_outcomes"] = outcomes
    return merged, report


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
    merged, report = publish_curated(
        snapshot,
        curated,
        registry,
        near_duplicate_rms_db=args.near_duplicate_rms_db,
    )

    args.output.parent.mkdir(parents=True, exist_ok=True)
    args.output.write_text(
        json.dumps(merged, indent=2, ensure_ascii=False) + "\n",
        encoding="utf-8",
    )
    args.report.parent.mkdir(parents=True, exist_ok=True)
    args.report.write_text(
        json.dumps(report, indent=2, sort_keys=True, ensure_ascii=False) + "\n",
        encoding="utf-8",
    )
    print(json.dumps(report, sort_keys=True))
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
