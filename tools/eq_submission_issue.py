#!/usr/bin/env python3
"""Normalize `Submit an EQ source` GitHub Issue Form events into the submission queue.

This tool performs conservative mechanical validation only. It never guesses headphone
identity, target, provenance, licensing, or missing EQ values. When structured PEQ text
is supplied, it is parsed with the same community PEQ parser used by the catalog pipeline
so arbitrary source filter counts and a missing source preamp are preserved exactly.

A clean structured submission is marked ``ready_for_source_policy`` so an automatic
publisher can apply the registered source/domain, identity and dedupe policy. This staging
step alone never publishes a catalog profile. Invalid or incomplete submissions remain
``needs_review`` with explicit diagnostics rather than being silently discarded.
"""
from __future__ import annotations

import argparse
import hashlib
import json
import re
from pathlib import Path
from typing import Any

from community_peq_ingest import parse_peq

HEADING_RE = re.compile(r"^###\s+(.+?)\s*$")
NO_RESPONSE_VALUES = {"_No response_", "No response", "N/A"}
URL_RE = re.compile(r"^https?://", re.IGNORECASE)
DATE_RE = re.compile(r"^\d{4}-\d{2}-\d{2}$")


def parse_issue_form_fields(body: str) -> dict[str, str]:
    """Parse GitHub Issue Form Markdown headings without interpreting field values."""
    fields: dict[str, str] = {}
    current_heading: str | None = None
    buffer: list[str] = []

    def flush() -> None:
        nonlocal buffer
        if current_heading is None:
            buffer = []
            return
        value = "\n".join(buffer).strip()
        if value in NO_RESPONSE_VALUES:
            value = ""
        fields[current_heading] = value
        buffer = []

    for line in body.splitlines():
        match = HEADING_RE.match(line)
        if match:
            flush()
            current_heading = match.group(1).strip()
        elif current_heading is not None:
            buffer.append(line)
    flush()
    return fields


def unwrap_code_fence(value: str) -> str:
    """Remove the single fenced block GitHub emits for textarea fields with `render`."""
    stripped = value.strip()
    lines = stripped.splitlines()
    if len(lines) >= 2 and lines[0].startswith("```") and lines[-1].strip() == "```":
        return "\n".join(lines[1:-1]).strip()
    return stripped


def checkbox_checked(value: str, label_fragment: str) -> bool:
    wanted = label_fragment.casefold()
    for line in value.splitlines():
        normalized = line.strip().casefold()
        if re.match(r"^-\s*\[[x]\]\s*", normalized) and wanted in normalized:
            return True
    return False


def _value(fields: dict[str, str], label: str) -> str | None:
    value = fields.get(label, "").strip()
    return value or None


def normalize_submission_event(event: dict[str, Any]) -> dict[str, Any]:
    issue = event.get("issue") or {}
    body = str(issue.get("body") or "")
    fields = parse_issue_form_fields(body)
    issue_number = issue.get("number")
    if not isinstance(issue_number, int):
        raise ValueError("GitHub event does not contain an issue number")

    manufacturer = _value(fields, "Manufacturer")
    model = _value(fields, "Exact model")
    legacy_headphone_label = _value(fields, "Headphone / IEM")
    variant = _value(fields, "Variant / revision / pads / mode")
    creator = _value(fields, "EQ creator / username")
    original_url = _value(fields, "Original source URL")
    platform = _value(fields, "Source platform")
    target = _value(fields, "Target / curve (only if explicitly stated)")
    source_date = _value(fields, "Source published / updated date")
    notes = _value(fields, "Notes")
    authorship = fields.get("Authorship", "")
    submitted_peq = unwrap_code_fence(_value(fields, "Parametric EQ / preset data") or "") or None

    errors: list[str] = []
    warnings: list[str] = []
    if not manufacturer or not model:
        if legacy_headphone_label:
            errors.append(
                "Legacy combined headphone field requires identity review; manufacturer/model were not guessed."
            )
        else:
            errors.append("Manufacturer and exact model are required.")
    if not creator:
        errors.append("EQ creator / username is required.")
    if not original_url:
        errors.append("Original source URL is required.")
    elif not URL_RE.match(original_url):
        errors.append("Original source URL must be HTTP(S).")
    if not platform:
        errors.append("Source platform is required.")
    if source_date and not DATE_RE.match(source_date):
        warnings.append("Source date is not YYYY-MM-DD and requires review.")

    peq_parse: dict[str, Any]
    if submitted_peq is None:
        peq_parse = {
            "status": "not_provided",
            "preamp_db": None,
            "filters": [],
            "error": None,
        }
    elif URL_RE.match(submitted_peq) and "\n" not in submitted_peq:
        peq_parse = {
            "status": "preset_link_needs_fetch",
            "preamp_db": None,
            "filters": [],
            "error": None,
        }
    else:
        try:
            parsed = parse_peq(submitted_peq)
            peq_parse = {
                "status": "parsed",
                "preamp_db": parsed.preamp_db,
                "filters": parsed.filters,
                "error": None,
            }
        except ValueError as exc:
            peq_parse = {
                "status": "invalid_needs_review",
                "preamp_db": None,
                "filters": [],
                "error": str(exc),
            }
            warnings.append("Submitted PEQ text could not be normalized automatically.")

    mechanically_valid = not errors and peq_parse["status"] == "parsed"
    candidate_state = "ready_for_source_policy" if mechanically_valid else "needs_review"

    issue_user = issue.get("user") or {}
    issue_url = str(issue.get("html_url") or issue.get("url") or "").strip() or None
    body_fingerprint = hashlib.sha256(body.encode("utf-8")).hexdigest()
    return {
        "schema_version": 1,
        "submission_id": f"github-issue-{issue_number}",
        "candidate_state": candidate_state,
        "mechanically_valid": mechanically_valid,
        "verification_status": "unverified",
        # Final publication eligibility is assigned only by the source-policy publisher.
        "publication_eligible": False,
        "issue": {
            "number": issue_number,
            "url": issue_url,
            "title": str(issue.get("title") or ""),
            "submitter": str(issue_user.get("login") or "").strip() or None,
            "created_at": issue.get("created_at"),
            "updated_at": issue.get("updated_at"),
            "body_sha256": body_fingerprint,
        },
        "headphone": {
            "manufacturer": manufacturer,
            "model": model,
            "variant": variant,
            "legacy_combined_label": legacy_headphone_label,
        },
        "creator": creator,
        "original_source_url": original_url,
        "source_platform": platform,
        "target": target,
        "source_date": source_date,
        "submitter_is_creator": checkbox_checked(authorship, "I am the creator of this EQ"),
        "submitted_peq": submitted_peq,
        "parsed_peq": peq_parse,
        "notes": notes,
        "validation_errors": errors,
        "review_warnings": warnings,
    }


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--event", type=Path, required=True, help="GitHub event JSON (for example GITHUB_EVENT_PATH)")
    parser.add_argument("--output", type=Path, required=True)
    args = parser.parse_args()

    event = json.loads(args.event.read_text(encoding="utf-8"))
    submission = normalize_submission_event(event)
    args.output.parent.mkdir(parents=True, exist_ok=True)
    args.output.write_text(
        json.dumps(submission, indent=2, sort_keys=True, ensure_ascii=False) + "\n",
        encoding="utf-8",
    )
    print(
        json.dumps(
            {
                "submission_id": submission["submission_id"],
                "candidate_state": submission["candidate_state"],
                "mechanically_valid": submission["mechanically_valid"],
                "peq_status": submission["parsed_peq"]["status"],
                "filter_count": len(submission["parsed_peq"]["filters"]),
                "validation_error_count": len(submission["validation_errors"]),
            },
            sort_keys=True,
        )
    )
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
