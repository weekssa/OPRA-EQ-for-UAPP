#!/usr/bin/env python3
"""Check canonical documentation links and consistency with current release evidence."""
from __future__ import annotations

import re
import sys
from pathlib import Path
from urllib.parse import unquote

ROOT = Path(__file__).resolve().parents[1]
CANONICAL_DOCS = (
    "README.md",
    "AGENTS.md",
    "CONTRIBUTING.md",
    "docs/PROJECT_STATE.md",
    "docs/GOALS_AND_SCOPE.md",
    "docs/OPERATING_RUNBOOK.md",
    "docs/KNOWN_ISSUES.md",
    "docs/CHATGPT_PROJECT_RUNBOOK.md",
    ".github/PULL_REQUEST_TEMPLATE.md",
)
APP_ID = "com.weekssa.opraeqforuapp"


def main() -> None:
    errors: list[str] = []
    texts: dict[str, str] = {}
    task_docs = sorted(path.relative_to(ROOT).as_posix() for path in (ROOT / "docs/maintenance").glob("*/*.md"))
    for relative in (*CANONICAL_DOCS, *task_docs):
        path = ROOT / relative
        if not path.is_file():
            errors.append(f"required canonical document is missing: {relative}")
            continue
        texts[relative] = path.read_text(encoding="utf-8")

    markdown_link = re.compile(r"!?\[[^\]]*\]\(([^)]+)\)")
    for relative, text in texts.items():
        source = ROOT / relative
        for match in markdown_link.finditer(text):
            target = match.group(1).strip()
            if target.startswith("<") and ">" in target:
                target = target[1 : target.index(">")]
            if not target or target.startswith(("http://", "https://", "mailto:", "tel:")):
                continue
            target = unquote(target.split("#", 1)[0].split("?", 1)[0])
            if not target:
                continue
            resolved = (source.parent / target).resolve()
            try:
                resolved.relative_to(ROOT)
            except ValueError:
                errors.append(f"{relative}: local link escapes repository: {match.group(1)}")
                continue
            if not resolved.exists():
                errors.append(f"{relative}: missing local link target: {match.group(1)}")

    state = texts.get("docs/PROJECT_STATE.md", "")
    readme = texts.get("README.md", "")
    agents = texts.get("AGENTS.md", "")
    checklist = (ROOT / "docs/PUBLIC_RELEASE_CHECKLIST.md").read_text(encoding="utf-8")
    build_file = (ROOT / "app/build.gradle.kts").read_text(encoding="utf-8")

    version_match = re.search(r"Current public stable release: \*\*v([^*]+)\*\*, versionCode `([0-9]+)`", state)
    source_match = re.search(r"Release source: `([0-9a-f]{40})`", state)
    apk_match = re.search(r"Exact stable APK SHA-256: `([0-9a-f]{64})`", state)
    signer_match = re.search(r"Pinned signer SHA-256: `([0-9a-f]{64})`", state)
    if not all((version_match, source_match, apk_match, signer_match)):
        errors.append("PROJECT_STATE.md must define the current version/code, release source, APK hash and signer hash")
    else:
        version, version_code = version_match.groups()
        source_sha = source_match.group(1)
        apk_sha = apk_match.group(1)
        signer_sha = signer_match.group(1)
        artifact_doc = ROOT / f"docs/release-engineering/v{version}-stable/ARTIFACTS.md"
        for required, label in (
            (f"v{version}", "README current release"),
            (apk_sha, "README APK SHA-256"),
            (source_sha, "release checklist source SHA"),
            (apk_sha, "release checklist APK SHA-256"),
            (signer_sha, "release checklist signer SHA-256"),
        ):
            target = readme if label.startswith("README") else checklist
            if required not in target:
                errors.append(f"canonical current release evidence is inconsistent: missing {label}")
        if not artifact_doc.is_file():
            errors.append(f"current release artifact record is missing: {artifact_doc.relative_to(ROOT)}")
        else:
            artifact_text = artifact_doc.read_text(encoding="utf-8")
            for value, label in ((source_sha, "release source"), (apk_sha, "APK SHA-256"), (signer_sha, "signer SHA-256")):
                if value not in artifact_text:
                    errors.append(f"current release artifact record does not contain the Project State {label}")
        if not re.search(rf'applicationId\s*=\s*"{re.escape(APP_ID)}"', build_file):
            errors.append("Gradle applicationId does not match the canonical package id")
        if not re.search(rf'versionCode\s*=\s*{version_code}\b', build_file):
            errors.append("Gradle versionCode does not match the current Project State")
        if not re.search(rf'versionName\s*=\s*"{re.escape(version)}"', build_file):
            errors.append("Gradle versionName does not match the current Project State")
        installed_state = state.partition("## Installed device state")[2].partition("\n## ")[0]
        if "UNKNOWN / NOT VERIFIED" not in installed_state and apk_sha not in installed_state:
            errors.append("PROJECT_STATE.md must state the installed-device evidence boundary")
    if re.search(r"current EW300 milestone targets `?versionName 0\.7\.0", agents):
        errors.append("AGENTS.md still names v0.7.0 as the current milestone")

    if errors:
        for error in errors:
            print(f"ERROR: {error}", file=sys.stderr)
        raise SystemExit(1)
    print("repository foundation validation passed")


if __name__ == "__main__":
    main()
