#!/usr/bin/env python3
"""Audit OPRA product identity for duplicate/alias cleanup.

The audit is intentionally conservative:
- exact normalized manufacturer + model duplicates are auto-safe;
- broader near-matches are review candidates only;
- explicit alias groups already present in the canonical catalog are marked covered.

The report is designed for CI/currentness use and never mutates source data by itself.
"""
from __future__ import annotations

import argparse
import io
import json
import re
import unicodedata
import urllib.request
from collections import defaultdict
from dataclasses import dataclass
from datetime import datetime, timezone
from difflib import SequenceMatcher
from pathlib import Path
from typing import Iterable, TextIO

DEFAULT_OPRA_URL = "https://raw.githubusercontent.com/opra-project/OPRA/main/dist/database_v1.jsonl"


@dataclass(frozen=True)
class Product:
    product_id: str
    vendor_id: str
    vendor_name: str
    name: str


def normalize(value: str) -> str:
    value = unicodedata.normalize("NFKC", value).casefold()
    return "".join(ch for ch in value if ch.isalnum())


def digits(value: str) -> tuple[str, ...]:
    return tuple(re.findall(r"\d+", value.casefold()))


def parse_opra(stream: TextIO) -> tuple[dict[str, str], list[Product]]:
    vendors: dict[str, str] = {}
    raw_products: list[tuple[str, str, str]] = []
    for line_number, line in enumerate(stream, 1):
        if not line.strip():
            continue
        record = json.loads(line)
        record_type = record.get("type")
        record_id = str(record.get("id") or "").strip()
        data = record.get("data") or {}
        if record_type == "vendor":
            name = str(data.get("name") or "").strip()
            if record_id and name:
                vendors[record_id] = name
        elif record_type == "product":
            vendor_id = str(data.get("vendor_id") or "").strip()
            name = str(data.get("name") or "").strip()
            if record_id and vendor_id and name:
                raw_products.append((record_id, vendor_id, name))
    products = [
        Product(pid, vid, vendors.get(vid, vid), name)
        for pid, vid, name in raw_products
    ]
    return vendors, products


def load_alias_groups(catalog_path: Path | None) -> list[dict]:
    if catalog_path is None or not catalog_path.is_file():
        return []
    catalog = json.loads(catalog_path.read_text(encoding="utf-8"))
    groups: list[dict] = []
    for group in catalog.get("headphone_aliases") or []:
        manufacturer = str(group.get("manufacturer") or "").strip()
        canonical = str(group.get("canonical_model") or "").strip()
        aliases = [str(x).strip() for x in group.get("aliases") or [] if str(x).strip()]
        if manufacturer and canonical:
            groups.append({
                "manufacturer": manufacturer,
                "canonical_model": canonical,
                "aliases": aliases,
                "source": "catalog.headphone_aliases",
            })
    for profile in catalog.get("profiles") or []:
        headphone = profile.get("headphone") or {}
        manufacturer = str(headphone.get("manufacturer") or "").strip()
        canonical = str(headphone.get("model") or "").strip()
        aliases = [str(x).strip() for x in headphone.get("model_aliases") or [] if str(x).strip()]
        if manufacturer and canonical and aliases:
            groups.append({
                "manufacturer": manufacturer,
                "canonical_model": canonical,
                "aliases": aliases,
                "source": "profile.model_aliases",
            })
    return groups


def alias_keysets(groups: list[dict]) -> list[tuple[str, set[str], dict]]:
    result = []
    for group in groups:
        manufacturer_key = normalize(group["manufacturer"])
        names = {normalize(group["canonical_model"])}
        names.update(normalize(alias) for alias in group.get("aliases") or [])
        names.discard("")
        if manufacturer_key and names:
            result.append((manufacturer_key, names, group))
    return result


def pair_is_covered(a: Product, b: Product, alias_sets: list[tuple[str, set[str], dict]]) -> dict | None:
    vendor_key = normalize(a.vendor_name)
    if vendor_key != normalize(b.vendor_name):
        return None
    a_key, b_key = normalize(a.name), normalize(b.name)
    for manufacturer_key, names, group in alias_sets:
        if vendor_key == manufacturer_key and a_key in names and b_key in names:
            return group
    return None


def review_score(a: Product, b: Product) -> tuple[float, str] | None:
    a_key, b_key = normalize(a.name), normalize(b.name)
    if not a_key or not b_key or a_key == b_key:
        return None
    a_digits, b_digits = digits(a.name), digits(b.name)
    if a_digits != b_digits:
        return None

    ratio = SequenceMatcher(None, a_key, b_key).ratio()
    shorter, longer = sorted((a_key, b_key), key=len)
    containment = len(shorter) >= 4 and shorter in longer
    vendor_key = normalize(a.vendor_name)
    stripped_a = a_key.removeprefix(vendor_key).removesuffix(vendor_key) if vendor_key else a_key
    stripped_b = b_key.removeprefix(vendor_key).removesuffix(vendor_key) if vendor_key else b_key
    vendor_redundant = bool(stripped_a and stripped_b and stripped_a == stripped_b)

    if vendor_redundant:
        return 0.99, "manufacturer name repeated in one model label"
    if containment and a_digits:
        extra = abs(len(a_key) - len(b_key))
        score = max(0.90, 0.98 - min(extra, 16) * 0.005)
        return score, "one normalized model name contains the other with matching numbers"
    if ratio >= 0.90:
        return ratio, "high normalized-name similarity with matching numbers"
    return None


def audit_products(products: list[Product], alias_groups: list[dict], max_review: int = 500) -> dict:
    exact_groups: dict[tuple[str, str], list[Product]] = defaultdict(list)
    by_vendor: dict[str, list[Product]] = defaultdict(list)
    for product in products:
        vendor_key = normalize(product.vendor_name)
        model_key = normalize(product.name)
        exact_groups[(vendor_key, model_key)].append(product)
        by_vendor[vendor_key].append(product)

    auto_safe = []
    for (_, model_key), group in exact_groups.items():
        if model_key and len(group) > 1:
            auto_safe.append({
                "manufacturer": group[0].vendor_name,
                "normalized_model": model_key,
                "products": [
                    {"id": item.product_id, "name": item.name}
                    for item in sorted(group, key=lambda p: (p.name.casefold(), p.product_id))
                ],
            })

    alias_sets = alias_keysets(alias_groups)
    covered: list[dict] = []
    review: list[dict] = []
    seen_pairs: set[tuple[str, str]] = set()
    for vendor_products in by_vendor.values():
        ordered = sorted(vendor_products, key=lambda p: p.product_id)
        for index, a in enumerate(ordered):
            for b in ordered[index + 1:]:
                if normalize(a.name) == normalize(b.name):
                    continue
                pair = tuple(sorted((a.product_id, b.product_id)))
                if pair in seen_pairs:
                    continue
                seen_pairs.add(pair)
                covered_group = pair_is_covered(a, b, alias_sets)
                if covered_group is not None:
                    covered.append({
                        "manufacturer": a.vendor_name,
                        "left": {"id": a.product_id, "name": a.name},
                        "right": {"id": b.product_id, "name": b.name},
                        "canonical_model": covered_group["canonical_model"],
                        "source": covered_group["source"],
                    })
                    continue
                scored = review_score(a, b)
                if scored is None:
                    continue
                score, reason = scored
                review.append({
                    "manufacturer": a.vendor_name,
                    "left": {"id": a.product_id, "name": a.name},
                    "right": {"id": b.product_id, "name": b.name},
                    "confidence": round(score, 3),
                    "reason": reason,
                })

    review.sort(key=lambda item: (-item["confidence"], item["manufacturer"].casefold(), item["left"]["name"].casefold()))
    review = review[:max_review]
    return {
        "product_count": len(products),
        "auto_safe_exact_group_count": len(auto_safe),
        "auto_safe_exact_groups": auto_safe,
        "covered_explicit_alias_pair_count": len(covered),
        "covered_explicit_alias_pairs": covered,
        "review_candidate_count": len(review),
        "review_candidates": review,
    }


def open_opra(input_path: Path | None, url: str) -> TextIO:
    if input_path is not None:
        return input_path.open("r", encoding="utf-8")
    response = urllib.request.urlopen(url, timeout=90)
    return io.TextIOWrapper(response, encoding="utf-8")


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--input", type=Path)
    parser.add_argument("--opra-url", default=DEFAULT_OPRA_URL)
    parser.add_argument("--catalog", type=Path, default=Path("catalog/catalog.json"))
    parser.add_argument("--output", type=Path, required=True)
    parser.add_argument("--max-review", type=int, default=500)
    args = parser.parse_args()

    with open_opra(args.input, args.opra_url) as stream:
        _, products = parse_opra(stream)
    aliases = load_alias_groups(args.catalog)
    report = audit_products(products, aliases, max_review=max(1, args.max_review))
    report = {
        "generated_at": datetime.now(timezone.utc).isoformat().replace("+00:00", "Z"),
        "opra_source": str(args.input) if args.input else args.opra_url,
        "alias_group_count": len(aliases),
        **report,
    }
    args.output.parent.mkdir(parents=True, exist_ok=True)
    args.output.write_text(json.dumps(report, indent=2, ensure_ascii=False) + "\n", encoding="utf-8")
    print(
        "Identity audit: "
        f"{report['product_count']} products; "
        f"{report['auto_safe_exact_group_count']} exact groups auto-safe; "
        f"{report['covered_explicit_alias_pair_count']} alias pairs covered; "
        f"{report['review_candidate_count']} candidates need review"
    )
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
