#!/usr/bin/env python3
from pathlib import Path

path = Path(__file__).with_name("apply_v03_release_polish.py")
text = path.read_text(encoding="utf-8")

helper_anchor = "def insert_after(rel: str, anchor: str, addition: str) -> None:\n    replace_once(rel, anchor, anchor + addition)\n\n"
helper = helper_anchor + "def insert_after_first(rel: str, anchor: str, addition: str) -> None:\n    target = path_fn(rel)\n    text = target.read_text(encoding=\"utf-8\")\n    if anchor not in text:\n        raise RuntimeError(f\"Expected a match in {rel}: {anchor[:120]!r}\")\n    target.write_text(text.replace(anchor, anchor + addition, 1), encoding=\"utf-8\")\n\n"

# The implementation script already has a function named path(). Avoid shadowing its
# module-level variable by referring to that helper through a small alias inserted below.
helper = helper.replace("target = path_fn(rel)", "target = path(rel)")

if "def insert_after_first(" not in text:
    if text.count(helper_anchor) != 1:
        raise RuntimeError("Could not locate insert_after helper")
    text = text.replace(helper_anchor, helper, 1)

old = 'insert_after(\n    "CHANGELOG.md",'
new = 'insert_after_first(\n    "CHANGELOG.md",'
count = text.count(old)
if count != 4:
    raise RuntimeError(f"Expected four CHANGELOG insertions, found {count}")
text = text.replace(old, new)

path.write_text(text, encoding="utf-8")
