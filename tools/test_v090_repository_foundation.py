from __future__ import annotations

import importlib.util
import unittest
from pathlib import Path


ROOT = Path(__file__).resolve().parents[1]
SPEC = importlib.util.spec_from_file_location(
    "validate_repository_foundation",
    ROOT / "tools/validate-repository-foundation.py",
)
assert SPEC is not None and SPEC.loader is not None
foundation = importlib.util.module_from_spec(SPEC)
SPEC.loader.exec_module(foundation)

SOURCE = "a" * 40
APK = "b" * 64
SIGNER = "c" * 64


def state(*, candidate: str | None = None) -> str:
    candidate_line = f"{candidate}\n" if candidate is not None else ""
    return (
        "# Current Project State\n\n"
        "## v0.9 workstream\n\n"
        f"{candidate_line}\n"
        "## Public release and source identity\n\n"
        "- Current public stable release: **v0.8.3**, versionCode `14`\n"
        f"- Release source: `{SOURCE}`\n"
        f"- Exact stable APK SHA-256: `{APK}`\n"
        f"- Pinned signer SHA-256: `{SIGNER}`\n\n"
        "## Installed device state\n\nUNKNOWN / NOT VERIFIED\n"
    )


def build(name: str, code: int) -> str:
    return (
        'android { defaultConfig { applicationId = "com.weekssa.opraeqforuapp"\n'
        f'versionCode = {code}\nversionName = "{name}" }} }}'
    )


class FoundationReleaseModeTest(unittest.TestCase):
    def validate(self, *, project_state: str, build_text: str):
        return foundation.validate_release_metadata(
            state=project_state,
            readme="v0.8.3 " + APK,
            agents="# AGENTS\n",
            checklist=SOURCE + " " + APK + " " + SIGNER,
            build_file=build_text,
            artifact_text=SOURCE + " " + APK + " " + SIGNER,
        )

    def test_stable_only_mode_keeps_legacy_build_match(self):
        self.assertEqual([], self.validate(project_state=state(), build_text=build("0.8.3", 14)))

    def test_stable_only_mode_rejects_candidate_build(self):
        errors = self.validate(project_state=state(), build_text=build("0.9.0-beta.1", 15))
        self.assertTrue(any("current Project State" in error for error in errors))

    def test_candidate_mode_uses_candidate_for_gradle_and_keeps_stable_evidence(self):
        candidate_state = state(candidate="- Candidate build: **v0.9.0-beta.1**, versionCode `15`")
        self.assertEqual([], self.validate(
            project_state=candidate_state,
            build_text=build("0.9.0-beta.1", 15),
        ))

    def test_candidate_mode_rejects_nonadvancing_or_malformed_metadata(self):
        malformed = state(candidate="- Candidate build: v0.9.0-beta.1, versionCode 15")
        errors = self.validate(project_state=malformed, build_text=build("0.9.0-beta.1", 15))
        self.assertTrue(any("malformed" in error or "current Project State" in error for error in errors))

        nonadvancing = state(candidate="- Candidate build: **v0.8.3**, versionCode `14`")
        errors = self.validate(project_state=nonadvancing, build_text=build("0.8.3", 14))
        self.assertIn("candidate versionCode must advance the current public stable versionCode", errors)


if __name__ == "__main__":
    unittest.main()
