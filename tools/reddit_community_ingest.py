#!/usr/bin/env python3
"""Discover and merge structured public Reddit PEQ posts into the canonical catalog.

Only numeric PEQ coefficients are normalized. Surrounding post prose is never copied.
A post is publication-eligible only when it contains parseable PEQ lines and can be
matched unambiguously to exactly one headphone already known to the catalog.

For controlled community pilots, --headphone-model narrows discovery and publication
to one headphone model at a time. This lets us validate quality, provenance, matching,
and deduplication before expanding community ingestion to additional headphones.
"""

from __future__ import annotations

import argparse
import json
import re
import time
import urllib.parse
import urllib.request
from pathlib import Path
from typing import Any

from catalog_merge import merge_candidates
from community_peq_ingest import build_candidate, parse_peq

DEFAULT_SUBREDDITS = ("headphones", "oratory1990")
SEARCH_TERMS = ("parametric EQ", "PEQ", '"Filter 1"', '"Preamp:"')
PEQ_LINE_RE = re.compile(r"^(?:Preamp:|Filter\s+\d+:)", re.IGNORECASE)
NON_ALNUM_RE = re.compile(r"[^a-z0-9]+")


def normalize(value: str) -> str:
    return " ".join(part for part in NON_ALNUM_RE.split(value.lower()) if part)


def fetch_json(url: str) -> dict[str, Any]:
    request = urllib.request.Request(
        url,
        headers={
            "User-Agent": "EQ-Library-currentness/0.3 (public structured PEQ discovery)",
            "Accept": "application/json",
        },
    )
    with urllib.request.urlopen(request, timeout=30) as response:
        return json.loads(response.read().decode("utf-8"))


def reddit_listing_urls(subreddit: str, limit: int, headphone_model: str | None = None) -> list[str]:
    base = f"https://www.reddit.com/r/{urllib.parse.quote(subreddit)}"
    urls: list[str] = []

    if headphone_model:
        # A controlled pilot should search for the named headphone directly rather than
        # depending on it appearing in the newest generic EQ results.
        targeted_terms = (
            f'"{headphone_model}" "parametric EQ"',
            f'"{headphone_model}" PEQ',
            f'"{headphone_model}" "Filter 1"',
            f'"{headphone_model}" "Preamp:"',
            f'"{headphone_model}" EQ',
        )
        for term in targeted_terms:
            query = urllib.parse.urlencode(
                {
                    "q": term,
                    "restrict_sr": "on",
                    "sort": "relevance",
                    "t": "all",
                    "limit": limit,
                    "raw_json": 1,
                }
            )
            urls.append(f"{base}/search.json?{query}")
        return urls

    urls.append(f"{base}/new.json?limit={limit}&raw_json=1")
    for term in SEARCH_TERMS:
        query = urllib.parse.urlencode(
            {
                "q": term,
                "restrict_sr": "on",
                "sort": "new",
                "t": "all",
                "limit": limit,
                "raw_json": 1,
            }
        )
        urls.append(f"{base}/search.json?{query}")
    return urls


def extract_posts(payload: dict[str, Any]) -> list[dict[str, Any]]:
    posts: list[dict[str, Any]] = []
    for child in ((payload.get("data") or {}).get("children") or []):
        data = child.get("data") if isinstance(child, dict) else None
        if isinstance(data, dict):
            posts.append(data)
    return posts


def extract_peq_text(text: str) -> str | None:
    lines: list[str] = []
    for raw in text.splitlines():
        line = raw.strip().strip("`> ")
        if PEQ_LINE_RE.match(line):
            lines.append(line)
    if not any(line.lower().startswith("filter") for line in lines):
        return None
    return "\n".join(lines) + "\n"


def catalog_headphones(snapshot: dict[str, Any], headphone_model: str | None = None) -> list[tuple[str, str]]:
    seen: set[tuple[str, str]] = set()
    values: list[tuple[str, str]] = []
    requested_model = normalize(headphone_model or "")
    for profile in snapshot.get("profiles") or []:
        headphone = profile.get("headphone") or {}
        manufacturer = str(headphone.get("manufacturer") or "").strip()
        model = str(headphone.get("model") or "").strip()
        if not manufacturer or not model:
            continue
        if requested_model and normalize(model) != requested_model:
            continue
        key = (manufacturer, model)
        if key not in seen:
            seen.add(key)
            values.append(key)
    return values


def match_headphone(post: dict[str, Any], headphones: list[tuple[str, str]]) -> tuple[str, str] | None:
    title = normalize(str(post.get("title") or ""))
    body = normalize(str(post.get("selftext") or ""))
    haystack = f" {title} {body} "
    matches: list[tuple[int, str, str]] = []
    for manufacturer, model in headphones:
        normalized_model = normalize(model)
        if len(normalized_model) < 4 or f" {normalized_model} " not in haystack:
            continue
        normalized_manufacturer = normalize(manufacturer)
        score = len(normalized_model)
        if normalized_manufacturer and f" {normalized_manufacturer} " in haystack:
            score += len(normalized_manufacturer) + 1000
        matches.append((score, manufacturer, model))
    if not matches:
        return None
    matches.sort(reverse=True)
    best = matches[0]
    if len(matches) > 1 and matches[1][0] == best[0] and matches[1][1:] != best[1:]:
        return None
    return best[1], best[2]


def discover(
    snapshot: dict[str, Any],
    subreddits: list[str],
    limit: int,
    headphone_model: str | None = None,
) -> tuple[list[dict[str, Any]], dict[str, Any]]:
    headphones = catalog_headphones(snapshot, headphone_model=headphone_model)
    if headphone_model and not headphones:
        raise ValueError(f"Requested headphone model is not present in the catalog: {headphone_model}")

    candidates: list[dict[str, Any]] = []
    seen_posts: set[str] = set()
    report: dict[str, Any] = {
        "target_headphone_model": headphone_model,
        "catalog_headphones_in_scope": [
            {"manufacturer": manufacturer, "model": model}
            for manufacturer, model in headphones
        ],
        "subreddits": subreddits,
        "listings_attempted": 0,
        "posts_seen": 0,
        "posts_with_peq": 0,
        "unmatched_headphone": 0,
        "parse_failures": 0,
        "candidates": 0,
        "candidate_sources": [],
        "errors": [],
    }

    for subreddit in subreddits:
        for url in reddit_listing_urls(subreddit, limit, headphone_model=headphone_model):
            report["listings_attempted"] += 1
            try:
                payload = fetch_json(url)
            except Exception as exc:  # network degradation must not invalidate last-known-good
                report["errors"].append({"url": url, "error": str(exc)})
                continue
            for post in extract_posts(payload):
                post_id = str(post.get("name") or post.get("id") or "").strip()
                if not post_id or post_id in seen_posts:
                    continue
                seen_posts.add(post_id)
                report["posts_seen"] += 1
                peq_text = extract_peq_text(str(post.get("selftext") or ""))
                if peq_text is None:
                    continue
                report["posts_with_peq"] += 1
                matched = match_headphone(post, headphones)
                if matched is None:
                    report["unmatched_headphone"] += 1
                    continue
                try:
                    parsed = parse_peq(peq_text)
                except ValueError:
                    report["parse_failures"] += 1
                    continue
                manufacturer, model = matched
                creator = str(post.get("author") or "").strip()
                permalink = str(post.get("permalink") or "").strip()
                if not creator or not permalink:
                    continue
                source_url = "https://www.reddit.com" + permalink
                title = str(post.get("title") or "Community PEQ").strip()
                created = post.get("created_utc")
                discovered_at = int(created) if isinstance(created, (int, float)) else int(time.time())
                candidate = build_candidate(
                    parsed,
                    manufacturer=manufacturer,
                    model=model,
                    creator=creator,
                    tuning_label=title[:160] or "Community PEQ",
                    source_id="reddit-audio",
                    source_kind="community",
                    source_url=source_url,
                    source_record_id=post_id,
                    redistribution_policy="structured-data-only",
                    target=None,
                    variant=None,
                    source_version=None,
                    discovered_at_epoch_seconds=discovered_at,
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
                    }
                )

    report["candidates"] = len(candidates)
    return candidates, report


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--catalog", type=Path, required=True)
    parser.add_argument("--output", type=Path, required=True)
    parser.add_argument("--report", type=Path, required=True)
    parser.add_argument("--subreddit", action="append", dest="subreddits")
    parser.add_argument("--limit", type=int, default=100)
    parser.add_argument("--headphone-model")
    parser.add_argument("--source-registry-version")
    args = parser.parse_args()

    snapshot = json.loads(args.catalog.read_text(encoding="utf-8"))
    candidates, report = discover(
        snapshot,
        args.subreddits or list(DEFAULT_SUBREDDITS),
        args.limit,
        headphone_model=args.headphone_model,
    )
    if candidates:
        merged, outcomes = merge_candidates(
            snapshot,
            candidates,
            source_registry_version=args.source_registry_version,
        )
    else:
        merged, outcomes = snapshot, {}
    report["merge_outcomes"] = outcomes

    args.output.parent.mkdir(parents=True, exist_ok=True)
    args.output.write_text(json.dumps(merged, indent=2, ensure_ascii=False) + "\n", encoding="utf-8")
    args.report.parent.mkdir(parents=True, exist_ok=True)
    args.report.write_text(json.dumps(report, indent=2, sort_keys=True) + "\n", encoding="utf-8")
    print(json.dumps(report, sort_keys=True))
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
