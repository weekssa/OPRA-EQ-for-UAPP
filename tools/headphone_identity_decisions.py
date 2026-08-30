#!/usr/bin/env python3
"""Validate and apply reviewed headphone identity decisions.

Confirmed aliases become catalog-level headphone_aliases consumed by Android at refresh time.
Confirmed-distinct pairs remain audit metadata only and are never merged.
"""
from __future__ import annotations

import argparse
import json
import unicodedata
from pathlib import Path
from typing import Any

from catalog_pipeline import validate_snapshot


def normalize(value: str) -> str:
    value = unicodedata.normalize("NFKC", value).casefold()
    return "".join(ch for ch in value if ch.isalnum())


def load_decisions(path: Path) -> dict[str, Any]:
    return json.loads(path.read_text(encoding="utf-8"))


def validate_decisions(decisions: dict[str, Any]) -> list[str]:
    errors: list[str] = []
    if decisions.get("schema_version") != 1:
        errors.append("schema_version must be 1")

    aliases = decisions.get("aliases")
    distinct_pairs = decisions.get("distinct_pairs")
    if not isinstance(aliases, list):
        errors.append("aliases must be a list")
        aliases = []
    if not isinstance(distinct_pairs, list):
        errors.append("distinct_pairs must be a list")
        distinct_pairs = []

    seen_alias_groups: set[tuple[str, str]] = set()
    for index, group in enumerate(aliases):
        prefix = f"aliases[{index}]"
        if not isinstance(group, dict):
            errors.append(f"{prefix} must be an object")
            continue
        manufacturer = str(group.get("manufacturer") or "").strip()
        canonical = str(group.get("canonical_model") or "").strip()
        alias_values = group.get("aliases")
        evidence = group.get("evidence")
        if not manufacturer or not canonical:
            errors.append(f"{prefix} requires manufacturer and canonical_model")
            continue
        if not isinstance(alias_values, list) or not alias_values:
            errors.append(f"{prefix}.aliases must be a non-empty list")
            continue
        if not isinstance(evidence, list) or not evidence or any(not str(item).strip() for item in evidence):
            errors.append(f"{prefix}.evidence must be a non-empty list of strings")
        key = (normalize(manufacturer), normalize(canonical))
        if key in seen_alias_groups:
            errors.append(f"duplicate alias group: {manufacturer} / {canonical}")
        seen_alias_groups.add(key)
        alias_keys = [normalize(str(value)) for value in alias_values if str(value).strip()]
        if len(alias_keys) != len(alias_values) or any(not key for key in alias_keys):
            errors.append(f"{prefix}.aliases contains blank values")
        if len(set(alias_keys)) != len(alias_keys):
            errors.append(f"{prefix}.aliases contains normalized duplicates")

    seen_distinct: set[tuple[str, str, str]] = set()
    for index, pair in enumerate(distinct_pairs):
        prefix = f"distinct_pairs[{index}]"
        if not isinstance(pair, dict):
            errors.append(f"{prefix} must be an object")
            continue
        manufacturer = str(pair.get("manufacturer") or "").strip()
        left = str(pair.get("left_model") or "").strip()
        right = str(pair.get("right_model") or "").strip()
        evidence = pair.get("evidence")
        if not manufacturer or not left or not right:
            errors.append(f"{prefix} requires manufacturer, left_model, and right_model")
            continue
        if normalize(left) == normalize(right):
            errors.append(f"{prefix} cannot declare the same normalized model distinct")
        if not isinstance(evidence, list) or not evidence or any(not str(item).strip() for item in evidence):
            errors.append(f"{prefix}.evidence must be a non-empty list of strings")
        sides = tuple(sorted((normalize(left), normalize(right))))
        key = (normalize(manufacturer), sides[0], sides[1])
        if key in seen_distinct:
            errors.append(f"duplicate distinct pair: {manufacturer} / {left} / {right}")
        seen_distinct.add(key)
    return errors


def alias_groups_from_decisions(decisions: dict[str, Any]) -> list[dict[str, Any]]:
    return [
        {
            "manufacturer": str(group["manufacturer"]).strip(),
            "canonical_model": str(group["canonical_model"]).strip(),
            "aliases": [str(value).strip() for value in group["aliases"]],
            "evidence": [str(value).strip() for value in group["evidence"]],
        }
        for group in decisions.get("aliases") or []
    ]


def distinct_keys_from_decisions(decisions: dict[str, Any]) -> dict[tuple[str, str, str], dict[str, Any]]:
    result: dict[tuple[str, str, str], dict[str, Any]] = {}
    for pair in decisions.get("distinct_pairs") or []:
        sides = tuple(sorted((normalize(str(pair["left_model"])), normalize(str(pair["right_model"])))))
        result[(normalize(str(pair["manufacturer"])), sides[0], sides[1])] = pair
    return result


def apply_decisions(snapshot: dict[str, Any], decisions: dict[str, Any]) -> dict[str, Any]:
    errors = validate_decisions(decisions)
    if errors:
        raise ValueError("identity decisions invalid: " + "; ".join(errors))

    result = json.loads(json.dumps(snapshot))
    existing = result.get("headphone_aliases") or []
    merged: dict[tuple[str, str], dict[str, Any]] = {}
    for group in existing + alias_groups_from_decisions(decisions):
        manufacturer = str(group.get("manufacturer") or "").strip()
        canonical = str(group.get("canonical_model") or "").strip()
        if not manufacturer or not canonical:
            continue
        key = (normalize(manufacturer), normalize(canonical))
        previous = merged.get(key)
        if previous is None:
            merged[key] = {
                "manufacturer": manufacturer,
                "canonical_model": canonical,
                "aliases": [],
                "evidence": [],
            }
            previous = merged[key]
        alias_seen = {normalize(value) for value in previous["aliases"]}
        for alias in group.get("aliases") or []:
            alias = str(alias).strip()
            alias_key = normalize(alias)
            if alias and alias_key and alias_key not in alias_seen:
                previous["aliases"].append(alias)
                alias_seen.add(alias_key)
        for evidence in group.get("evidence") or []:
            evidence = str(evidence).strip()
            if evidence and evidence not in previous["evidence"]:
                previous["evidence"].append(evidence)

    result["headphone_aliases"] = sorted(
        merged.values(),
        key=lambda item: (normalize(item["manufacturer"]), normalize(item["canonical_model"])),
    )
    snapshot_errors = validate_snapshot(result)
    if snapshot_errors:
        raise ValueError("catalog invalid after identity decisions: " + "; ".join(snapshot_errors))
    return result


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    sub = parser.add_subparsers(dest="command", required=True)

    validate = sub.add_parser("validate")
    validate.add_argument("--decisions", type=Path, default=Path("config/headphone_identity_decisions.json"))

    apply = sub.add_parser("apply")
    apply.add_argument("--decisions", type=Path, default=Path("config/headphone_identity_decisions.json"))
    apply.add_argument("--catalog", type=Path, required=True)
    apply.add_argument("--output", type=Path, required=True)

    args = parser.parse_args()
    try:
        decisions = load_decisions(args.decisions)
        errors = validate_decisions(decisions)
        if errors:
            for error in errors:
                print("ERROR:", error)
            return 1
        if args.command == "validate":
            print(
                f"identity decisions OK: {len(decisions.get('aliases') or [])} alias groups; "
                f"{len(decisions.get('distinct_pairs') or [])} distinct pairs"
            )
            return 0
        snapshot = json.loads(args.catalog.read_text(encoding="utf-8"))
        result = apply_decisions(snapshot, decisions)
        args.output.parent.mkdir(parents=True, exist_ok=True)
        args.output.write_text(json.dumps(result, indent=2, ensure_ascii=False) + "\n", encoding="utf-8")
        print(f"applied {len(decisions.get('aliases') or [])} reviewed alias groups")
        return 0
    except (OSError, json.JSONDecodeError, KeyError, ValueError) as exc:
        print("ERROR:", exc)
        return 1


if __name__ == "__main__":
    raise SystemExit(main())
