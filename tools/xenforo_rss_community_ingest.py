#!/usr/bin/env python3
"""Discover exact public PEQ from bounded XenForo RSS/thread surfaces.

This adapter is intentionally narrow. It polls configured public RSS feeds for recently
active headphone/IEM threads, uses exact PEQ included in the public feed when available,
and fetches only those public thread pages for additional post-level discovery. It never
logs in, bypasses access controls, crawls arbitrary pagination, or turns screenshots or
curves into invented filters.
"""

from __future__ import annotations

import argparse
import hashlib
import html
import json
import re
import urllib.request
import xml.etree.ElementTree as ET
from html.parser import HTMLParser
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
from discourse_community_ingest import single_peq_text, unique_headphone_match
from reddit_community_ingest import catalog_headphones

USER_AGENT = "EQ-Library-currentness/0.4 (bounded public RSS PEQ discovery)"
POST_ID_RE = re.compile(r"(?:js-)?post[-_](\d+)$", re.IGNORECASE)
BREAK_RE = re.compile(r"(?i)<(?:br\s*/?|/p|/div|/li|/pre)\s*>")
TAG_RE = re.compile(r"<[^>]+>")


def fetch_text(url: str) -> str:
    request = urllib.request.Request(
        url,
        headers={
            "User-Agent": USER_AGENT,
            "Accept": "application/rss+xml, application/atom+xml, text/html;q=0.9, */*;q=0.5",
        },
    )
    with urllib.request.urlopen(request, timeout=30) as response:
        return response.read().decode("utf-8", errors="replace")


def _local_name(tag: str) -> str:
    return tag.rsplit("}", 1)[-1].lower()


def _node_text(node: ET.Element | None) -> str:
    if node is None:
        return ""
    return "".join(node.itertext()).strip()


def html_fragment_text(value: str) -> str:
    value = html.unescape(value or "")
    value = BREAK_RE.sub("\n", value)
    value = TAG_RE.sub(" ", value)
    value = html.unescape(value)
    return "\n".join(line.strip() for line in value.splitlines() if line.strip())


def parse_feed(payload: str) -> list[dict[str, str]]:
    root = ET.fromstring(payload)
    entries: list[dict[str, str]] = []
    for node in root.iter():
        if _local_name(node.tag) not in {"item", "entry"}:
            continue
        values: dict[str, str] = {}
        content_parts: list[str] = []
        for child in list(node):
            name = _local_name(child.tag)
            if name == "link":
                href = str(child.attrib.get("href") or "").strip()
                text = _node_text(child)
                if href or text:
                    values.setdefault("link", href or text)
            elif name in {"title", "guid", "id"}:
                text = _node_text(child)
                if text:
                    values.setdefault(name, text)
            elif name in {"description", "encoded", "content", "summary"}:
                text = _node_text(child)
                if text:
                    content_parts.append(text)
            elif name in {"creator", "author"}:
                text = _node_text(child)
                if text:
                    values.setdefault("author", text)
        link = values.get("link", "").strip()
        if not link:
            continue
        entries.append(
            {
                "title": html.unescape(values.get("title", "")).strip(),
                "link": html.unescape(link),
                "guid": values.get("guid") or values.get("id") or link,
                "author": html.unescape(values.get("author", "")).strip(),
                "content": html_fragment_text("\n".join(content_parts)),
            }
        )
    return entries


class XenforoPostParser(HTMLParser):
    """Extract conventional XenForo message articles without site-specific JS."""

    def __init__(self) -> None:
        super().__init__(convert_charrefs=True)
        self.posts: list[dict[str, str]] = []
        self.current: dict[str, Any] | None = None
        self.capture_depth = 0
        self.article_depth = 0

    @staticmethod
    def _attrs(attrs: list[tuple[str, str | None]]) -> dict[str, str]:
        return {key: value or "" for key, value in attrs}

    @staticmethod
    def _classes(attrs: dict[str, str]) -> set[str]:
        return {item for item in attrs.get("class", "").split() if item}

    def handle_starttag(self, tag: str, attrs_raw: list[tuple[str, str | None]]) -> None:
        attrs = self._attrs(attrs_raw)
        classes = self._classes(attrs)
        if self.current is None and tag == "article" and "message--post" in classes:
            raw_id = attrs.get("data-content") or attrs.get("id") or ""
            match = POST_ID_RE.search(raw_id)
            self.current = {
                "post_id": match.group(1) if match else raw_id.strip(),
                "author": attrs.get("data-author", "").strip(),
                "text": [],
            }
            self.article_depth = 1
            self.capture_depth = 0
            return

        if self.current is None:
            return
        if tag == "article":
            self.article_depth += 1
        if self.capture_depth:
            self.capture_depth += 1
            if tag in {"br", "p", "div", "li", "pre"}:
                self.current["text"].append("\n")
        elif "bbWrapper" in classes or "message-body" in classes:
            self.capture_depth = 1

    def handle_startendtag(self, tag: str, attrs_raw: list[tuple[str, str | None]]) -> None:
        if self.current is not None and self.capture_depth and tag == "br":
            self.current["text"].append("\n")

    def handle_data(self, data: str) -> None:
        if self.current is not None and self.capture_depth:
            self.current["text"].append(data)

    def handle_endtag(self, tag: str) -> None:
        if self.current is None:
            return
        if self.capture_depth:
            if tag in {"p", "div", "li", "pre"}:
                self.current["text"].append("\n")
            self.capture_depth -= 1
        if tag == "article":
            self.article_depth -= 1
            if self.article_depth <= 0:
                text = html.unescape("".join(self.current["text"]))
                text = "\n".join(line.strip() for line in text.splitlines() if line.strip())
                if text:
                    self.posts.append(
                        {
                            "post_id": str(self.current.get("post_id") or "").strip(),
                            "author": str(self.current.get("author") or "").strip(),
                            "text": text,
                        }
                    )
                self.current = None
                self.capture_depth = 0
                self.article_depth = 0


def parse_thread_posts(payload: str) -> list[dict[str, str]]:
    parser = XenforoPostParser()
    parser.feed(payload)
    parser.close()
    return parser.posts


def _source(registry: dict[str, Any], source_id: str) -> dict[str, Any]:
    for source in registry.get("sources") or []:
        if source.get("id") == source_id:
            return source
    raise ValueError(f"source registry is missing {source_id}")


def _source_config(config: dict[str, Any], source_id: str) -> dict[str, Any]:
    for item in config.get("sources") or []:
        if item.get("source_id") == source_id:
            return item
    raise ValueError(f"XenForo source config is missing {source_id}")


def _post_source_url(thread_url: str, post_id: str) -> str:
    clean = thread_url.split("#", 1)[0]
    return clean + (f"#post-{post_id}" if post_id else "")


def _candidate_from_text(
    text: str,
    *,
    title: str,
    creator: str,
    source_id: str,
    source_url: str,
    source_record_id: str,
    headphones: list[tuple[str, str]],
    report: dict[str, Any],
) -> dict[str, Any] | None:
    peq_text, peq_rejection = single_peq_text(text)
    if peq_rejection:
        report["ambiguous_peq_blocks"] += 1
        return None
    if peq_text is None:
        return None
    report["posts_with_peq"] += 1
    matched = unique_headphone_match(title, text, headphones)
    if matched is None:
        report["unmatched_or_ambiguous_headphone"] += 1
        return None
    try:
        parsed = parse_peq(peq_text)
    except ValueError:
        report["parse_failures"] += 1
        return None
    if not creator.strip():
        report["errors"].append({"url": source_url, "error": "PEQ record missing public author"})
        return None
    manufacturer, model = matched
    candidate = build_candidate(
        parsed,
        manufacturer=manufacturer,
        model=model,
        creator=creator,
        tuning_label=(title or "Community PEQ")[:160],
        source_id=source_id,
        source_kind="community",
        source_url=source_url,
        source_record_id=source_record_id,
        redistribution_policy="structured-data-only",
        target=None,
        variant=None,
        source_version=None,
        discovered_at_epoch_seconds=None,
    )
    report["candidate_sources"].append(
        {
            "creator": creator,
            "manufacturer": manufacturer,
            "model": model,
            "title": (title or "")[:160],
            "source_url": source_url,
            "filter_count": len(parsed.filters),
            "source_record_id": source_record_id,
        }
    )
    return candidate


def discover(
    snapshot: dict[str, Any],
    *,
    source_id: str,
    source_config: dict[str, Any],
    fetcher: Callable[[str], str] = fetch_text,
) -> tuple[list[dict[str, Any]], dict[str, Any]]:
    feeds = [str(item).strip() for item in source_config.get("feeds") or [] if str(item).strip()]
    if not feeds:
        raise ValueError(f"{source_id} needs at least one public RSS feed")
    max_threads = max(1, min(int(source_config.get("max_threads_per_scan") or 30), 100))
    headphones = catalog_headphones(snapshot)
    report: dict[str, Any] = {
        "source_id": source_id,
        "feeds": feeds,
        "feeds_attempted": 0,
        "feeds_succeeded": 0,
        "feed_entries_seen": 0,
        "feed_entries_with_content": 0,
        "feed_content_peq_candidates": 0,
        "thread_pages_attempted": 0,
        "thread_pages_succeeded": 0,
        "posts_parsed": 0,
        "posts_with_peq": 0,
        "ambiguous_peq_blocks": 0,
        "unmatched_or_ambiguous_headphone": 0,
        "parse_failures": 0,
        "candidates": 0,
        "candidate_sources": [],
        "thread_markup_counts": {"message--post": 0, "data-author": 0, "bbWrapper": 0, "message-body": 0},
        "errors": [],
    }

    entries_by_link: dict[str, dict[str, str]] = {}
    for feed_url in feeds:
        report["feeds_attempted"] += 1
        try:
            parsed_entries = parse_feed(fetcher(feed_url))
            report["feeds_succeeded"] += 1
            report["feed_entries_seen"] += len(parsed_entries)
            report["feed_entries_with_content"] += sum(1 for entry in parsed_entries if entry.get("content"))
            for entry in parsed_entries:
                entries_by_link.setdefault(entry["link"], entry)
        except Exception as exc:
            report["errors"].append({"url": feed_url, "error": str(exc)})

    candidates: list[dict[str, Any]] = []
    seen_records: set[str] = set()
    feed_cursor_items: list[str] = []
    for entry in list(entries_by_link.values())[:max_threads]:
        thread_url = entry["link"]
        guid = str(entry.get("guid") or thread_url)
        feed_cursor_items.append(guid)

        feed_content = str(entry.get("content") or "")
        feed_author = str(entry.get("author") or "").strip()
        if feed_content and feed_author:
            feed_record = "feed-" + hashlib.sha256(guid.encode("utf-8")).hexdigest()[:24]
            candidate = _candidate_from_text(
                feed_content,
                title=entry.get("title", ""),
                creator=feed_author,
                source_id=source_id,
                source_url=thread_url,
                source_record_id=feed_record,
                headphones=headphones,
                report=report,
            )
            if candidate is not None:
                candidates.append(candidate)
                seen_records.add(feed_record)
                report["feed_content_peq_candidates"] += 1

        report["thread_pages_attempted"] += 1
        try:
            page = fetcher(thread_url)
            report["thread_pages_succeeded"] += 1
            for marker in report["thread_markup_counts"]:
                report["thread_markup_counts"][marker] += page.count(marker)
            posts = parse_thread_posts(page)
        except Exception as exc:
            report["errors"].append({"url": thread_url, "error": str(exc)})
            continue
        report["posts_parsed"] += len(posts)
        for post in posts:
            post_id = str(post.get("post_id") or "").strip()
            dedupe_key = post_id or hashlib.sha256(
                (thread_url + "\n" + post.get("author", "") + "\n" + post.get("text", "")).encode("utf-8")
            ).hexdigest()
            record_id = f"post-{post_id}" if post_id else f"thread-{dedupe_key[:24]}"
            if record_id in seen_records:
                continue
            seen_records.add(record_id)
            source_url = _post_source_url(thread_url, post_id)
            candidate = _candidate_from_text(
                post.get("text", ""),
                title=entry.get("title", ""),
                creator=str(post.get("author") or "").strip(),
                source_id=source_id,
                source_url=source_url,
                source_record_id=record_id,
                headphones=headphones,
                report=report,
            )
            if candidate is not None:
                candidates.append(candidate)

    report["candidates"] = len(candidates)
    report["feed_cursor_items"] = sorted(feed_cursor_items)
    return candidates, report


def refresh(
    snapshot: dict[str, Any],
    registry: dict[str, Any],
    current_health: dict[str, Any],
    *,
    source_id: str,
    source_config: dict[str, Any],
    fetcher: Callable[[str], str] = fetch_text,
) -> tuple[dict[str, Any], dict[str, Any], dict[str, Any], bool]:
    errors = validate_registry(registry)
    if errors:
        raise ValueError("invalid source registry: " + "; ".join(errors))
    source = _source(registry, source_id)
    if source.get("lifecycle") != "active":
        raise ValueError(f"{source_id} must be active for publication")
    if source.get("currentness_mode") != "scheduled":
        raise ValueError(f"{source_id} must be scheduled for XenForo ingestion")
    if source.get("redistribution") != "structured-data-only":
        raise ValueError(f"{source_id} is not qualified for structured publication")

    health = reconcile_health(registry, current_health)
    candidates, report = discover(
        snapshot,
        source_id=source_id,
        source_config=source_config,
        fetcher=fetcher,
    )

    feeds_usable = int(report.get("feeds_succeeded") or 0) > 0
    feed_content_usable = int(report.get("feed_entries_with_content") or 0) > 0
    thread_parser_usable = int(report.get("posts_parsed") or 0) > 0
    thread_requests_usable = (
        not report.get("thread_pages_attempted")
        or int(report.get("thread_pages_succeeded") or 0) > 0
    )
    unusable = not feeds_usable or not thread_requests_usable or not (feed_content_usable or thread_parser_usable)
    if unusable:
        health[source_id] = record_scan_failure(health[source_id], "public XenForo feed/thread retrieval was not usable")
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
        cursor=sha256_json(report.get("feed_cursor_items") or []),
        content_fingerprint=sha256_json(candidates),
    )
    return merged, health, report, True


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--catalog", type=Path, required=True)
    parser.add_argument("--registry", type=Path, required=True)
    parser.add_argument("--health", type=Path, required=True)
    parser.add_argument("--source-config", type=Path, required=True)
    parser.add_argument("--source-id", required=True)
    parser.add_argument("--catalog-output", type=Path, required=True)
    parser.add_argument("--health-output", type=Path, required=True)
    parser.add_argument("--report", type=Path, required=True)
    parser.add_argument("--allow-degraded", action="store_true")
    args = parser.parse_args()

    registry = load_json(args.registry)
    config = load_json(args.source_config)
    merged, health, report, success = refresh(
        load_json(args.catalog),
        registry,
        load_health(args.health),
        source_id=args.source_id,
        source_config=_source_config(config, args.source_id),
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
