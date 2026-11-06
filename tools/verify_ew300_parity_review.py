#!/usr/bin/env python3
"""Verify the gate's independent-review record is complete."""

from __future__ import annotations

import argparse
import re
import subprocess
import sys


def main(argv: list[str]) -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("gate")
    parser.add_argument("--head", default=None)
    args = parser.parse_args(argv[1:])

    text = open(args.gate, encoding="utf-8").read()
    if "## Review" not in text:
        print("gate has no review section", file=sys.stderr)
        return 1
    review = text.split("## Review", 1)[1]

    problems: list[str] = []
    if not re.search(r"^verdict:\s*PASS\s*$", review, re.M):
        problems.append("verdict is not PASS")
    if not re.search(r"^reviewer:\s*gpt-6-luna\s*$", review, re.M):
        problems.append("reviewer is not gpt-6-luna")
    if not re.search(r"^account:\s*other\s*$", review, re.M):
        problems.append("review was not from the other account")
    if not re.search(r"^confirmation:\s*PASS\s*$", review, re.M):
        problems.append("findings-only confirmation pass is not PASS")
    if re.search(r"^- none yet\s*$", review, re.M):
        problems.append("review findings were not recorded")

    head = args.head
    if head is None:
        head = subprocess.run(["git", "rev-parse", "HEAD"], capture_output=True, text=True).stdout.strip()
    match = re.search(r"^reviewed_head:\s*([0-9a-f]{7,40})\s*$", review, re.M)
    if not match:
        problems.append("reviewed_head is missing")
    elif not head.startswith(match.group(1)) and not match.group(1).startswith(head):
        problems.append("reviewed_head does not match the current candidate")

    if problems:
        for problem in problems:
            print(f"FAIL {problem}", file=sys.stderr)
        return 1
    print("independent review record PASS")
    return 0


if __name__ == "__main__":
    sys.exit(main(sys.argv))

