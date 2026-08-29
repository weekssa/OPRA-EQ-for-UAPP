#!/usr/bin/env python3
"""Canonical acoustic fingerprint shared by EQ Library ingestion tools.

This intentionally mirrors Android's AcousticFingerprint implementation byte-for-byte
at the normalized string level so external ingestion, currentness jobs, and the app
agree on acoustic identity across sources.
"""

from __future__ import annotations

import hashlib
from typing import Any, Iterable

TYPE_MAP = {
    "peak": "PK",
    "low_shelf": "LS",
    "high_shelf": "HS",
    "low_pass": "LP",
    "high_pass": "HP",
    "other": "OTHER",
}


def _format(value: float | int | None, decimals: int) -> str:
    return f"{float(value or 0.0):.{decimals}f}"


def acoustic_fingerprint(preamp_db: float | None, filters: Iterable[dict[str, Any]]) -> str:
    normalized: list[str] = []
    for item in filters:
        filter_type = TYPE_MAP.get(str(item.get("type", "other")).strip().lower(), "OTHER")
        normalized.append(
            "|".join(
                [
                    filter_type,
                    _format(item.get("frequency_hz"), 3),
                    _format(item.get("gain_db"), 3),
                    _format(item.get("q"), 4),
                    _format(item.get("slope"), 4),
                ]
            )
        )
    payload = f"preamp={_format(preamp_db, 3)};" + "".join(f"{item};" for item in sorted(normalized))
    return hashlib.sha256(payload.encode("utf-8")).hexdigest()
