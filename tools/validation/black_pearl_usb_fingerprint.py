#!/usr/bin/env python3
"""Extract exactly one Black Pearl device record from Android `dumpsys usb`.

The parser prints only the descriptor fields needed for exact identity. It is
intended to consume a temporary, ignored `dumpsys usb` capture and never
rewrites the source capture.
"""

from __future__ import annotations

import argparse
import re
import sys
from pathlib import Path


EXPECTED = {
    "vendor_id": "13058",
    "product_id": "17384",
    "manufacturer_name": "TTGK Technology",
    "product_name": "TE-C",
    "serial_number": "330243E8260129",
}
OUTPUT_KEYS = ("name", *EXPECTED)
SIGNALS = tuple(EXPECTED)


class DescriptorError(ValueError):
    pass


def _records(text: str) -> list[list[str]]:
    lines = text.splitlines()
    in_host_manager = False
    in_devices = False
    depth = 0
    current: list[str] = []
    found: list[list[str]] = []

    for line in lines:
        if not in_host_manager:
            if re.match(r"^\s*host_manager\s*=\s*\{\s*$", line):
                in_host_manager = True
            continue

        if not in_devices:
            if re.match(r"^\s*devices\s*=\s*\{\s*$", line):
                in_devices = True
                depth = 1
            continue

        if depth == 0:
            break
        if depth == 1 and re.match(r"^\s*name\s*=", line):
            if current:
                found.append(current)
            current = [line]
        elif current:
            current.append(line)

        depth += line.count("{") - line.count("}")
        if depth < 0:
            raise DescriptorError("malformed host-manager device nesting")
        if depth == 0:
            if current:
                found.append(current)
            break

    if not in_host_manager or not in_devices:
        raise DescriptorError("dumpsys output lacks host_manager.devices")
    return found


def _field_values(record: list[str]) -> dict[str, list[str]]:
    result = {key: [] for key in OUTPUT_KEYS}
    if not record:
        return result
    base_indent = len(record[0]) - len(record[0].lstrip())
    for line in record:
        indent = len(line) - len(line.lstrip())
        if indent != base_indent:
            continue
        match = re.match(r"^\s*(name|vendor_id|product_id|manufacturer_name|product_name|serial_number)\s*=\s*(.*?)\s*$", line)
        if match:
            result[match.group(1)].append(match.group(2))
    return result


def extract_black_pearl(text: str) -> list[str]:
    matches: list[dict[str, str]] = []
    for record in _records(text):
        values = _field_values(record)
        observed = {key: entries[0] for key, entries in values.items() if entries}
        has_signature = any(observed.get(key) == expected for key, expected in EXPECTED.items())
        if not has_signature:
            continue
        if any(len(values[key]) != 1 for key in OUTPUT_KEYS):
            raise DescriptorError("a Black Pearl-signature record has missing or duplicate identity fields")
        if any(observed.get(key) != expected for key, expected in EXPECTED.items()):
            raise DescriptorError("a Black Pearl-signature record has conflicting identity fields")
        if not re.fullmatch(r"/dev/bus/usb/[0-9]{3}/[0-9]{3}", observed["name"]):
            raise DescriptorError("Black Pearl record has an invalid Android USB device path")
        matches.append(observed)

    if len(matches) != 1:
        raise DescriptorError(f"expected exactly one Black Pearl descriptor record; found {len(matches)}")
    return [f"{key}={matches[0][key]}" for key in OUTPUT_KEYS]


def main(argv: list[str] | None = None) -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("dump", type=Path, help="temporary local dumpsys usb capture")
    args = parser.parse_args(argv)
    try:
        result = extract_black_pearl(args.dump.read_text(encoding="utf-8", errors="replace"))
    except (OSError, DescriptorError) as exc:
        print(f"BLACK_PEARL_FINGERPRINT_NOT_PASS: {exc}", file=sys.stderr)
        return 2
    print("\n".join(result))
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
