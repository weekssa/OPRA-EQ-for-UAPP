#!/usr/bin/env python3
from pathlib import Path

path = Path(__file__).with_name("apply_v03_release_polish.py")
text = path.read_text(encoding="utf-8")

helper_anchor = "def insert_after(rel: str, anchor: str, addition: str) -> None:\n    replace_once(rel, anchor, anchor + addition)\n\n"
helper = helper_anchor + "def insert_after_first(rel: str, anchor: str, addition: str) -> None:\n    target = path(rel)\n    text = target.read_text(encoding=\"utf-8\")\n    if anchor not in text:\n        raise RuntimeError(f\"Expected a match in {rel}: {anchor[:120]!r}\")\n    target.write_text(text.replace(anchor, anchor + addition, 1), encoding=\"utf-8\")\n\n"

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

old_test = "run: python3 -m unittest tools/test_general_preset_ingest.py tools/test_general_preset_publish.py"
new_test = "run: PYTHONPATH=tools python3 -m unittest tools/test_general_preset_ingest.py tools/test_general_preset_publish.py"
if text.count(old_test) != 1:
    raise RuntimeError("Could not locate generated General EQ workflow test command")
text = text.replace(old_test, new_test, 1)

old_cleanup = 'for rel in (\n    "tools/apply_v03_release_polish.py",\n    ".github/workflows/apply-v03-release-polish.yml",\n):'
new_cleanup = 'for rel in (\n    "tools/apply_v03_release_polish.py",\n    "tools/fix_apply_v03_release_polish.py",\n    ".github/workflows/apply-v03-release-polish.yml",\n):'
if text.count(old_cleanup) != 1:
    raise RuntimeError("Could not locate temporary-file cleanup list")
text = text.replace(old_cleanup, new_cleanup, 1)

path.write_text(text, encoding="utf-8")
