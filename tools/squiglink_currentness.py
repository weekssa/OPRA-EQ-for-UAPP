#!/usr/bin/env python3
"""Track the public Squiglink ecosystem without fabricating PEQ from measurements.

The official Squiglink site registry is a structured public discovery surface. This job
records real currentness for the ecosystem and its database list. Measurement records
remain provenance/discovery data; source-authored PEQ still has to arrive with exact
frequency/gain/Q/filter type through an eligible community source.
"""

from __future__ import annotations

import argparse
import json
import urllib.request
from pathlib import Path
from typing import Any, Callable

from catalog_pipeline import (
    load_health,
    load_json,
    reconcile_health,
    record_scan_failure,
    record_scan_success,
    sha256_json,
    validate_registry,
    write_health,
)

SITES_URL = "https://squig.link/squigsites.json"


def fetch_json(url: str = SITES_URL) -> Any:
    request = urllib.request.Request(
        url,
        headers={
            "User-Agent": "EQ-Library-currentness/0.4 (public Squiglink registry check)",
            "Accept": "application/json",
        },
    )
    with urllib.request.urlopen(request, timeout=30) as response:
        return json.loads(response.read().decode("utf-8"))


def normalize_sites(payload: Any) -> list[dict[str, Any]]:
    if not isinstance(payload, list):
        raise ValueError("Squiglink site registry must be a list")
    normalized: list[dict[str, Any]] = []
    for item in payload:
        if not isinstance(item, dict):
            continue
        username = str(item.get("username") or "").strip()
        name = str(item.get("name") or "").strip()
        dbs = item.get("dbs")
        if not username or not name or not isinstance(dbs, list):
            continue
        normalized_dbs: list[dict[str, str]] = []
        for db in dbs:
            if not isinstance(db, dict):
                continue
            folder = str(db.get("folder") or "").strip()
            db_type = str(db.get("type") or "").strip()
            if folder and db_type:
                normalized_dbs.append({"folder": folder, "type": db_type})
        normalized.append(
            {
                "username": username,
                "name": name,
                "base_url": f"https://{username}.squig.link",
                "dbs": sorted(normalized_dbs, key=lambda value: (value["type"], value["folder"])),
            }
        )
    if not normalized:
        raise ValueError("Squiglink site registry did not contain any valid sites")
    return sorted(normalized, key=lambda value: (value["name"].casefold(), value["username"].casefold()))


def _source(registry: dict[str, Any]) -> dict[str, Any]:
    for source in registry.get("sources") or []:
        if source.get("id") == "squiglink":
            return source
    raise ValueError("source registry is missing squiglink")


def refresh(
    registry: dict[str, Any],
    current_health: dict[str, Any],
    *,
    fetcher: Callable[[str], Any] = fetch_json,
) -> tuple[dict[str, Any], dict[str, Any], bool]:
    errors = validate_registry(registry)
    if errors:
        raise ValueError("invalid source registry: " + "; ".join(errors))
    source = _source(registry)
    if source.get("lifecycle") != "active" or source.get("currentness_mode") != "scheduled":
        raise ValueError("squiglink must be active/scheduled for currentness")
    health = reconcile_health(registry, current_health)
    report: dict[str, Any] = {"source_id": "squiglink", "registry_url": SITES_URL}
    try:
        sites = normalize_sites(fetcher(SITES_URL))
    except Exception as exc:
        health["squiglink"] = record_scan_failure(health["squiglink"], str(exc))
        report.update({"status": "degraded", "error": str(exc)})
        return health, report, False

    fingerprint = sha256_json(sites)
    report.update(
        {
            "status": "ok",
            "site_count": len(sites),
            "database_count": sum(len(site["dbs"]) for site in sites),
            "site_names": [site["name"] for site in sites],
            "content_fingerprint": fingerprint,
            "publication_note": "measurement metadata only; no PEQ filters are synthesized",
        }
    )
    health["squiglink"] = record_scan_success(
        health["squiglink"], cursor=fingerprint, content_fingerprint=fingerprint
    )
    return health, report, True


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--registry", type=Path, required=True)
    parser.add_argument("--health", type=Path, required=True)
    parser.add_argument("--health-output", type=Path, required=True)
    parser.add_argument("--report", type=Path, required=True)
    parser.add_argument("--allow-degraded", action="store_true")
    args = parser.parse_args()

    health, report, success = refresh(load_json(args.registry), load_health(args.health))
    write_health(args.health_output, health)
    args.report.parent.mkdir(parents=True, exist_ok=True)
    args.report.write_text(json.dumps(report, indent=2, sort_keys=True) + "\n", encoding="utf-8")
    print(json.dumps(report, sort_keys=True))
    return 0 if success or args.allow_degraded else 1


if __name__ == "__main__":
    raise SystemExit(main())
