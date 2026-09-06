#!/usr/bin/env python3
"""Discover structured public PEQ posts from a Discourse community.

The adapter uses only public Discourse JSON endpoints, extracts numeric PEQ lines,
retains username/original-post provenance, and requires an unambiguous match to one
headphone already present in the canonical catalog. A post is auto-publication eligible
only when it contains exactly one structurally coherent PEQ block: no repeated preamp,
no repeated Filter numbers, and contiguous Filter 1..N numbering. Multi-profile posts,
partial copied blocks, screenshots, curves, and ambiguous headphone identity are
quarantined rather than flattened into invented tunings.
"""

from __future__ import annotations

import argparse
import html
import json
import re
import urllib.parse
import urllib.request
from pathlib import Path
from typing import Any, Callable

from catalog_merge import merge_candidates
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
from community_peq_ingest import build_candidate, parse_peq
from reddit_community_ingest import catalog_headphones, normalize

SEARCH_TERMS = ("parametric EQ", "PEQ", '"Filter 1"', '"Preamp:"')
TAG_RE = re.compile(r"<[^>]+>")
BREAK_RE = re.compile(r"(?i)<(?:br\s*/?|/p|/div|/li)>")
PEQ_LINE_RE = re.compile(r"^(?:Preamp:|Filter\s+\d+:)", re.IGNORECASE)
FILTER_NUMBER_RE = re.compile(r"^Filter\s+(\d+):", re.IGNORECASE)


def fetch_json(url: str) -> dict[str, Any]:
    request = urllib.request.Request(
        url,
        headers={
            "User-Agent": "EQ-Library-currentness/0.4 (public structured PEQ discovery)",
            "Accept": "application/json",
        },
    )
    with urllib.request.urlopen(request, timeout=30) as response:
        return json.loads(response.read().decode("utf-8"))


def plain_post_text(post: dict[str, Any]) -> str:
    raw = str(post.get("raw") or "").strip()
    if raw:
        return raw
    cooked = str(post.get("cooked") or post.get("blurb") or "")
    cooked = BREAK_RE.sub("\n", cooked)
    return html.unescape(TAG_RE.sub(" ", cooked))


def single_peq_text(text: str) -> tuple[str | None, str | None]:
    lines: list[str] = []
    filter_numbers: list[int] = []
    preamp_count = 0
    for raw in text.splitlines():
        line = raw.strip().strip("`> ")
        if not PEQ_LINE_RE.match(line):
            continue
        lines.append(line)
        if line.lower().startswith("preamp:"):
            preamp_count += 1
            continue
        match = FILTER_NUMBER_RE.match(line)
        if match:
            filter_numbers.append(int(match.group(1)))

    if not filter_numbers:
        return None, None
    if preamp_count > 1:
        return None, "multiple_preamp_lines"
    expected = list(range(1, len(filter_numbers) + 1))
    if filter_numbers != expected:
        return None, "non_contiguous_or_repeated_filter_numbers"
    return "\n".join(lines) + "\n", None


def unique_headphone_match(
    title: str,
    text: str,
    headphones: list[tuple[str, str]],
) -> tuple[str, str] | None:
    normalized_title = normalize(title)
    normalized_text = normalize(text)
    title_haystack = f" {normalized_title} "
    full_haystack = f" {normalized_title} {normalized_text} "
    matches: list[tuple[str, str]] = []
    for manufacturer, model in headphones:
        normalized_model = normalize(model)
        if len(normalized_model) < 4 or f" {normalized_model} " not in full_haystack:
            continue
        normalized_manufacturer = normalize(manufacturer)
        manufacturer_present = bool(
            normalized_manufacturer and f" {normalized_manufacturer} " in full_haystack
        )
        model_in_topic_title = f" {normalized_model} " in title_haystack
        if manufacturer_present or model_in_topic_title:
            matches.append((manufacturer, model))
    distinct = list(dict.fromkeys(matches))
    return distinct[0] if len(distinct) == 1 else None


def search_url(base_url: str, term: str) -> str:
    query = urllib.parse.urlencode({"q": term})
    return base_url.rstrip("/") + "/search.json?" + query


def post_url(base_url: str, post_id: int | str) -> str:
    return base_url.rstrip("/") + f"/posts/{post_id}.json"


def canonical_topic_url(base_url: str, topic: dict[str, Any], post: dict[str, Any]) -> str:
    topic_id = int(post.get("topic_id") or topic.get("id") or 0)
    post_number = int(post.get("post_number") or 1)
    slug = str(topic.get("slug") or "topic").strip() or "topic"
    return base_url.rstrip("/") + f"/t/{slug}/{topic_id}/{post_number}"


def _source(registry: dict[str, Any], source_id: str) -> dict[str, Any]:
    for source in registry.get("sources") or []:
        if source.get("id") == source_id:
            return source
    raise ValueError(f"source registry is missing {source_id}")


def discover(
    snapshot: dict[str, Any],
    *,
    base_url: str,
    source_id: str,
    terms: list[str],
    fetcher: Callable[[str], dict[str, Any]] = fetch_json,
) -> tuple[list[dict[str, Any]], dict[str, Any]]:
    headphones = catalog_headphones(snapshot)
    candidates: list[dict[str, Any]] = []
    seen_posts: set[str] = set()
    report: dict[str, Any] = {
        "source_id": source_id,
        "base_url": base_url,
        "search_terms": terms,
        "searches_attempted": 0,
        "searches_succeeded": 0,
        "posts_seen": 0,
        "posts_with_peq": 0,
        "ambiguous_peq_blocks": 0,
        "unmatched_or_ambiguous_headphone": 0,
        "parse_failures": 0,
        "candidates": 0,
        "candidate_sources": [],
        "errors": [],
    }

    for term in terms:
        url = search_url(base_url, term)
        report["searches_attempted"] += 1
        try:
            payload = fetcher(url)
            report["searches_succeeded"] += 1
        except Exception as exc:
            report["errors"].append({"url": url, "error": str(exc)})
            continue

        topics = {
            int(topic.get("id")): topic
            for topic in payload.get("topics") or []
            if isinstance(topic, dict) and str(topic.get("id") or "").isdigit()
        }
        for hit in payload.get("posts") or []:
            if not isinstance(hit, dict):
                continue
            post_id = str(hit.get("id") or "").strip()
            if not post_id or post_id in seen_posts:
                continue
            seen_posts.add(post_id)
            report["posts_seen"] += 1
            try:
                full_post = fetcher(post_url(base_url, post_id))
            except Exception as exc:
                report["errors"].append({"url": post_url(base_url, post_id), "error": str(exc)})
                continue

            text = plain_post_text(full_post)
            peq_text, peq_rejection = single_peq_text(text)
            if peq_rejection:
                report["ambiguous_peq_blocks"] += 1
                continue
            if peq_text is None:
                continue
            report["posts_with_peq"] += 1
            topic_id = int(full_post.get("topic_id") or hit.get("topic_id") or 0)
            topic = topics.get(topic_id, {})
            title = str(topic.get("title") or hit.get("topic_title") or "")
            matched = unique_headphone_match(title, text, headphones)
            if matched is None:
                report["unmatched_or_ambiguous_headphone"] += 1
                continue
            try:
                parsed = parse_peq(peq_text)
            except ValueError:
                report["parse_failures"] += 1
                continue

            creator = str(full_post.get("username") or hit.get("username") or "").strip()
            if not creator:
                continue
            manufacturer, model = matched
            source_url = canonical_topic_url(base_url, topic, full_post)
            candidate = build_candidate(
                parsed,
                manufacturer=manufacturer,
                model=model,
                creator=creator,
                tuning_label=title[:160] or "Community PEQ",
                source_id=source_id,
                source_kind="community",
                source_url=source_url,
                source_record_id=f"post-{post_id}",
                redistribution_policy="structured-data-only",
                target=None,
                variant=None,
                source_version=None,
                discovered_at_epoch_seconds=None,
            )
            candidates.append(candidate)
            report["candidate_sources"].append(
                {
                    "post_id": post_id,
                    "creator": creator,
                    "manufacturer": manufacturer,
                    "model": model,
                    "title": title[:160],
                    "source_url": source_url,
                    "filter_count": len(parsed.filters),
                }
            )

    report["candidates"] = len(candidates)
    return candidates, report


def refresh(
    snapshot: dict[str, Any],
    registry: dict[str, Any],
    current_health: dict[str, Any],
    *,
    base_url: str,
    source_id: str,
    terms: list[str],
    fetcher: Callable[[str], dict[str, Any]] = fetch_json,
) -> tuple[dict[str, Any], dict[str, Any], dict[str, Any], bool]:
    errors = validate_registry(registry)
    if errors:
        raise ValueError("invalid source registry: " + "; ".join(errors))
    source = _source(registry, source_id)
    if source.get("lifecycle") != "active":
        raise ValueError(f"{source_id} must be active for publication")
    if source.get("redistribution") != "structured-data-only":
        raise ValueError(f"{source_id} is not qualified for structured publication")

    health = reconcile_health(registry, current_health)
    candidates, report = discover(
        snapshot,
        base_url=base_url,
        source_id=source_id,
        terms=terms,
        fetcher=fetcher,
    )
    if int(report.get("searches_succeeded") or 0) == 0:
        health[source_id] = record_scan_failure(health[source_id], "all Discourse searches failed")
        report["status"] = "degraded"
        report["publication_skipped"] = True
        return snapshot, health, report, False

    if candidates:
        merged, outcomes = merge_candidates(
            snapshot,
            candidates,
            source_registry_version=str(registry.get("registry_version") or "") or None,
        )
    else:
        merged, outcomes = snapshot, {}
    report["merge_outcomes"] = outcomes
    report["status"] = "partial" if report.get("errors") else "ok"
    report["publication_skipped"] = False
    health[source_id] = record_scan_success(
        health[source_id],
        cursor=sha256_json(sorted(str(item.get("post_id")) for item in report.get("candidate_sources") or [])),
        content_fingerprint=sha256_json(candidates),
    )
    return merged, health, report, True


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--catalog", type=Path, required=True)
    parser.add_argument("--registry", type=Path, required=True)
    parser.add_argument("--health", type=Path, required=True)
    parser.add_argument("--catalog-output", type=Path, required=True)
    parser.add_argument("--health-output", type=Path, required=True)
    parser.add_argument("--report", type=Path, required=True)
    parser.add_argument("--base-url", required=True)
    parser.add_argument("--source-id", required=True)
    parser.add_argument("--term", action="append", dest="terms")
    parser.add_argument("--allow-degraded", action="store_true")
    args = parser.parse_args()

    snapshot = load_json(args.catalog)
    registry = load_json(args.registry)
    merged, health, report, success = refresh(
        snapshot,
        registry,
        load_health(args.health),
        base_url=args.base_url,
        source_id=args.source_id,
        terms=args.terms or list(SEARCH_TERMS),
    )
    args.catalog_output.parent.mkdir(parents=True, exist_ok=True)
    args.catalog_output.write_text(json.dumps(merged, indent=2, ensure_ascii=False) + "\n", encoding="utf-8")
    write_health(args.health_output, health)
    args.report.parent.mkdir(parents=True, exist_ok=True)
    args.report.write_text(json.dumps(report, indent=2, sort_keys=True) + "\n", encoding="utf-8")
    print(json.dumps(report, sort_keys=True))
    return 0 if success or args.allow_degraded else 1


if __name__ == "__main__":
    raise SystemExit(main())
