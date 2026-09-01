#!/usr/bin/env python3
from pathlib import Path
import subprocess

root = Path(__file__).resolve().parents[1]
helper = root / "tools/sync_v03_release_polish_docs.py"
text = helper.read_text(encoding="utf-8")
old = '''    """8. regression validation against the prior catalog\\n\\nPublication must be atomic.\\n""",
    """8. regression validation against the prior catalog, including a hard living-archive check that previously published canonical profiles/revisions have not disappeared or changed acoustically in place\\n\\nPublication must be atomic.\\n""",
'''
new = '''    """8. regression validation against the prior catalog\\n""",
    """8. regression validation against the prior catalog, including a hard living-archive check that previously published canonical profiles/revisions have not disappeared or changed acoustically in place\\n""",
'''
if text.count(old) != 1:
    raise SystemExit("documentation helper publication-discipline target not found exactly once")
helper.write_text(text.replace(old, new, 1), encoding="utf-8")
subprocess.run(["python3", str(helper)], cwd=root, check=True)
