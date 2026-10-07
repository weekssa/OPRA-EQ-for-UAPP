#!/usr/bin/env python3
"""Fail-closed extraction of Android's current resumed-activity checkpoint."""

from __future__ import annotations

import argparse
import re
import sys
from pathlib import Path


class ActivityCheckpointError(ValueError):
    pass


_ACTIVITY_LINE = re.compile(
    r"^\s*(?P<field>topResumedActivity|ResumedActivity|mResumedActivity)\s*(?:=|:)\s*(?P<record>\S.*)$"
)
_ACTIVITY_COMPONENT = re.compile(
    r"\bu\d+\s+"
    r"(?P<package>[A-Za-z_][A-Za-z0-9_]*(?:\.[A-Za-z_][A-Za-z0-9_]*)*)/"
    r"(?P<class>(?:\.[A-Za-z_][A-Za-z0-9_.$]*|[A-Za-z_][A-Za-z0-9_.$]*(?:\.[A-Za-z_][A-Za-z0-9_.$]*)*))"
    r"\s+t\d+\b"
)


def _is_activity_record(value: str) -> bool:
    prefix = "ActivityRecord{"
    if not value.startswith(prefix) or not value.endswith("}"):
        return False

    depth = 0
    for index, character in enumerate(value[len("ActivityRecord") :], start=len("ActivityRecord")):
        if character == "{":
            depth += 1
        elif character == "}":
            depth -= 1
            if depth < 0 or (depth == 0 and index != len(value) - 1):
                return False
    return depth == 0 and bool(value[len(prefix) : -1].strip()) and bool(_ACTIVITY_COMPONENT.search(value))


def extract_activity(text: str, package: str, expect_present: bool) -> str:
    top: list[str] = []
    platform_resumed: list[str] = []
    resumed: list[str] = []
    for line in text.splitlines():
        match = _ACTIVITY_LINE.match(line)
        if not match:
            continue
        record = match.group("record")
        if not _is_activity_record(record):
            raise ActivityCheckpointError("current resumed-activity field did not contain a complete ActivityRecord")
        field = match.group("field")
        target = top if field == "topResumedActivity" else platform_resumed if field == "ResumedActivity" else resumed
        target.append(f"{field}={record}")

    # Android 17 exposes the singular current entry as ResumedActivity. Older
    # releases use topResumedActivity or mResumedActivity. Prefer the more
    # explicit display-level current record when a dump contains multiple
    # diagnostic sections.
    selected = top or platform_resumed or resumed
    if len(selected) != 1:
        raise ActivityCheckpointError(
            f"expected one current resumed-activity record; found {len(selected)}"
        )
    selected_record = selected[0].split("=", 1)[1]
    component = _ACTIVITY_COMPONENT.search(selected_record)
    if component is None:
        raise ActivityCheckpointError("current resumed-activity record has no valid package/component token")
    contains_package = component.group("package") == package
    if expect_present and not contains_package:
        raise ActivityCheckpointError("current resumed activity does not match the app package")
    if not expect_present and contains_package:
        raise ActivityCheckpointError("app package is unexpectedly resumed")
    return selected[0]


def main(argv: list[str] | None = None) -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("dump", type=Path, help="temporary local dumpsys activity capture")
    parser.add_argument("--package", required=True, help="expected app package")
    expectation = parser.add_mutually_exclusive_group(required=True)
    expectation.add_argument("--expect-present", action="store_true")
    expectation.add_argument("--expect-absent", action="store_true")
    args = parser.parse_args(argv)
    try:
        record = extract_activity(args.dump.read_text(encoding="utf-8", errors="replace"), args.package, args.expect_present)
    except (OSError, ActivityCheckpointError) as exc:
        print(f"APP_ACTIVITY_CHECKPOINT_NOT_PASS: {exc}", file=sys.stderr)
        return 2
    print(record)
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
