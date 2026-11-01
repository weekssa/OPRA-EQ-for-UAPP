#!/usr/bin/env python3
"""Validate the explicit OPRA EQ Library v0.9.0 beta release profile.

This module owns only the v0.9.0-beta.1 release identity and candidate metadata
contract. It does not build, sign, dispatch workflows, or publish releases.

The pinned release profile stays frozen at v0.9.0-beta.1 while a recovery candidate
advances the Android version for private testing. Such a candidate is accepted only
when it is declared verbatim in docs/PROJECT_STATE.md, advances the versionCode, and
stays inside the 0.9.0 beta line. The publisher still accepts only the pinned beta
tag, so an advancing candidate is not publishable.
"""

from __future__ import annotations

import argparse
import hashlib
import json
import re
import sys
from dataclasses import asdict, dataclass
from pathlib import Path
from typing import Any


REPOSITORY = "weekssa/OPRA-EQ-for-UAPP"
PACKAGE_ID = "com.weekssa.opraeqforuapp"
BETA_TAG = "v0.9.0-beta.1"
BETA_VERSION_NAME = "0.9.0-beta.1"
BETA_VERSION_CODE = 15
STABLE_LATEST_TAG = "v0.8.3"
STABLE_LATEST_VERSION_CODE = 14
STABLE_LATEST_APK_SHA256 = "396244ffc26cec108b64c523a49779261656b41ab02f1bc2b63197327c576247"
PLAN_SHA256 = "a108a2182a28a82252c64956216d31b6dcba9c67bf522a51d6d86a073d6e729b"
SEAL_MANIFEST_SHA256 = "b258bc43c156f247cb13ef4372286aafe20e643b52980bd6a7bf4a035f912a6b"
BASELINE_COMMIT = "35f0c90a0e55e701d4343ed978a440d34aae13aa"
CANDIDATE_WORKFLOW_PATH = ".github/workflows/github-release.yml"
PUBLISHER_WORKFLOW_PATH = ".github/workflows/publish-beta-prerelease.yml"
LEGACY_PUBLISHER_PATH = "tools/publish_beta_prerelease.py"
SHA256_RE = re.compile(r"^[0-9a-f]{64}$")
SOURCE_SHA_RE = re.compile(r"^[0-9a-f]{40}$")
CANDIDATE_STATE_RE = re.compile(r"Candidate build: \*\*v([^*]+)\*\*, versionCode \x60([0-9]+)\x60")
RECOVERY_VERSION_RE = re.compile(r"0\.9\.0-beta\.[0-9]+")
STABLE_V09_VERSION = "0.9.0"


class V090ReleaseError(RuntimeError):
    """The v0.9.0 beta profile failed a release-identity assertion."""


def require(condition: bool, message: str) -> None:
    if not condition:
        raise V090ReleaseError(message)


@dataclass(frozen=True)
class BetaProfile:
    tag: str = BETA_TAG
    version_name: str = BETA_VERSION_NAME
    version_code: int = BETA_VERSION_CODE
    package_id: str = PACKAGE_ID
    release_channel: str = "beta"
    prerelease: bool = True
    make_latest: bool = False
    stable_latest_tag: str = STABLE_LATEST_TAG
    stable_latest_version_code: int = STABLE_LATEST_VERSION_CODE
    stable_latest_apk_sha256: str = STABLE_LATEST_APK_SHA256


@dataclass(frozen=True)
class CandidateRecord:
    tag: str
    source_sha: str
    run_id: int
    artifact_id: int
    artifact_name: str
    artifact_digest: str
    apk_sha256: str
    signer_sha256: str


PROFILE = BetaProfile()


def sha256_hex(data: bytes) -> str:
    return hashlib.sha256(data).hexdigest()


def parse_android_version(project_root: Path) -> tuple[str, int]:
    gradle = (project_root / "app/build.gradle.kts").read_text(encoding="utf-8")
    name_match = re.search(r'^\s*versionName\s*=\s*"([^"\r\n]+)"', gradle, re.MULTILINE)
    code_match = re.search(r"^\s*versionCode\s*=\s*([0-9]+)\b", gradle, re.MULTILINE)
    require(name_match is not None and code_match is not None, "Android versionName/versionCode are missing")
    assert name_match is not None and code_match is not None
    return name_match.group(1), int(code_match.group(1))


def validate_version(project_root: Path, profile: BetaProfile = PROFILE) -> None:
    version_name, version_code = parse_android_version(project_root)
    if version_name == profile.version_name and version_code == profile.version_code:
        return
    declared = declared_candidate(project_root)
    if declared is not None and declared == (version_name, version_code):
        require(version_code > profile.version_code,
                f"recovery candidate versionCode must advance {profile.version_code}, found {version_code}")
        require(
            RECOVERY_VERSION_RE.fullmatch(version_name) is not None or version_name == STABLE_V09_VERSION,
                f"candidate versionName must be the 0.9.0 beta line or the stable {STABLE_V09_VERSION}, found {version_name}")
        return
    if version_name == profile.version_name:
        raise V090ReleaseError(f"Android versionCode must be {profile.version_code}, found {version_code}")
    raise V090ReleaseError(
        f"Android versionName must be {profile.version_name} or an advancing recovery candidate "
        f"declared in Project State, found {version_name}",
    )


def declared_candidate(project_root: Path) -> tuple[str, int] | None:
    """The candidate identity maintained in Project State, when present."""
    state_path = project_root / "docs/PROJECT_STATE.md"
    if not state_path.is_file():
        return None
    match = CANDIDATE_STATE_RE.search(state_path.read_text(encoding="utf-8"))
    if match is None:
        return None
    return match.group(1), int(match.group(2))


def validate_candidate_tag(tag: str, profile: BetaProfile = PROFILE) -> None:
    require(tag == profile.tag,
            f"candidate workflow accepts only exact beta tag {profile.tag}, found {tag}")


def candidate_metadata(source_sha: str, profile: BetaProfile = PROFILE) -> dict[str, Any]:
    require(SOURCE_SHA_RE.fullmatch(source_sha) is not None, "candidate source SHA is malformed")
    return {
        "releaseTag": profile.tag,
        "releaseChannel": profile.release_channel,
        "prerelease": profile.prerelease,
        "makeLatest": profile.make_latest,
        "publishOnSign": False,
        "sourceSha": source_sha,
        "versionName": profile.version_name,
        "versionCode": profile.version_code,
        "packageId": profile.package_id,
    }


def expected_candidate_artifact_name(source_sha: str, profile: BetaProfile = PROFILE) -> str:
    require(SOURCE_SHA_RE.fullmatch(source_sha) is not None, "candidate source SHA is malformed")
    return f"EQ-Library-{profile.tag}-signed-{source_sha}"


def validate_candidate_record(
    record: CandidateRecord,
    *,
    expected_source_sha: str | None = None,
    expected_signer_sha256: str | None = None,
    profile: BetaProfile = PROFILE,
) -> None:
    require(record.tag == profile.tag, "candidate record has the wrong release tag")
    require(SOURCE_SHA_RE.fullmatch(record.source_sha) is not None, "candidate record source SHA is malformed")
    if expected_source_sha is not None:
        require(record.source_sha == expected_source_sha, "candidate record source differs from the expected main source")
    require(record.run_id > 0, "candidate run ID must be positive")
    require(record.artifact_id > 0, "candidate artifact ID must be positive")
    require(record.artifact_name == expected_candidate_artifact_name(record.source_sha, profile),
            "candidate artifact name does not match the exact beta/source tuple")
    require(SHA256_RE.fullmatch(record.artifact_digest) is not None,
            "candidate artifact digest is malformed")
    require(SHA256_RE.fullmatch(record.apk_sha256) is not None, "candidate APK digest is malformed")
    require(SHA256_RE.fullmatch(record.signer_sha256) is not None, "candidate signer digest is malformed")
    if expected_signer_sha256 is not None:
        require(record.signer_sha256 == expected_signer_sha256,
                "candidate signer differs from the repository-pinned release signer")


def candidate_record_from_manifest(
    manifest: dict[str, Any],
    *,
    run_id: int,
    artifact_id: int,
    artifact_name: str,
    artifact_digest: str,
    pinned_signer_sha256: str,
    expected_source_sha: str | None = None,
    profile: BetaProfile = PROFILE,
) -> CandidateRecord:
    required = {
        "releaseTag": profile.tag,
        "releaseChannel": profile.release_channel,
        "prerelease": profile.prerelease,
        "makeLatest": profile.make_latest,
        "publishOnSign": False,
        "packageId": profile.package_id,
        "versionName": profile.version_name,
        "versionCode": profile.version_code,
    }
    for field, expected in required.items():
        require(manifest.get(field) == expected,
                f"candidate manifest field {field} does not match the v0.9.0 beta profile")
    source_sha = manifest.get("sourceSha")
    apk_sha256 = manifest.get("apkSha256")
    signer_sha256 = manifest.get("signerSha256")
    require(isinstance(source_sha, str), "candidate manifest sourceSha is missing")
    require(isinstance(apk_sha256, str), "candidate manifest apkSha256 is missing")
    require(isinstance(signer_sha256, str), "candidate manifest signerSha256 is missing")
    record = CandidateRecord(
        tag=profile.tag,
        source_sha=source_sha,
        run_id=run_id,
        artifact_id=artifact_id,
        artifact_name=artifact_name,
        artifact_digest=artifact_digest,
        apk_sha256=apk_sha256,
        signer_sha256=signer_sha256,
    )
    validate_candidate_record(
        record,
        expected_source_sha=expected_source_sha,
        expected_signer_sha256=pinned_signer_sha256,
        profile=profile,
    )
    return record


def validate_latest_snapshot(release: dict[str, Any], profile: BetaProfile = PROFILE) -> None:
    require(release.get("tag_name") == profile.stable_latest_tag,
            f"stable latest must remain {profile.stable_latest_tag}")
    require(release.get("draft") is False and release.get("prerelease") is False,
            "stable latest must remain a published stable release")
    require(release.get("immutable") is True, "stable latest must remain immutable")
    assets = release.get("assets")
    require(isinstance(assets, list), "stable latest asset list is malformed")
    apk_name = f"EQ-Library-{profile.stable_latest_tag}.apk"
    apks = [asset for asset in assets if isinstance(asset, dict) and asset.get("name") == apk_name]
    require(len(apks) == 1 and apks[0].get("digest") == f"sha256:{profile.stable_latest_apk_sha256}",
            "stable latest APK no longer matches the v0.8.3 release identity")


def check_contract(project_root: Path) -> None:
    validate_version(project_root)
    candidate_workflow = (project_root / CANDIDATE_WORKFLOW_PATH).read_text(encoding="utf-8")
    publisher_workflow = (project_root / PUBLISHER_WORKFLOW_PATH).read_text(encoding="utf-8")
    required_candidate = (
        "github.repository == 'weekssa/OPRA-EQ-for-UAPP' && github.ref == 'refs/heads/main'",
        "tools/v090_release.py validate-candidate",
        "release_channel=beta",
        "releaseChannel:$releaseChannel",
        "prerelease:$prerelease",
        "makeLatest:false",
        "persist-credentials: false",
    )
    for marker in required_candidate:
        require(marker in candidate_workflow,
                f"candidate workflow is missing v0.9.0 contract marker {marker!r}")
    require("permissions:\n  contents: read" in candidate_workflow,
            "candidate workflow must remain read-only at the top level")
    require("contents: write" not in candidate_workflow,
            "candidate signing workflow may not receive contents-write")
    for line in candidate_workflow.splitlines():
        if re.match(r"^\s+uses:\s+", line):
            require(re.search(r"@[0-9a-f]{40}(?:\s|$)", line) is not None,
                    "candidate workflow actions must be pinned to full commit SHAs")
    require("tools/publish_beta_prerelease.py verify" in publisher_workflow and
            "tools/publish_beta_prerelease.py publish" in publisher_workflow,
            "legacy v0.8.0 beta publisher contract was not preserved")
    print("V090_RELEASE_CONTRACT_PASSED")


def build_parser() -> argparse.ArgumentParser:
    parser = argparse.ArgumentParser(description=__doc__)
    sub = parser.add_subparsers(dest="command", required=True)
    version_parser = sub.add_parser("check-version")
    version_parser.add_argument("--project-root", type=Path, default=Path("."))
    candidate_parser = sub.add_parser("validate-candidate")
    candidate_parser.add_argument("--tag", required=True)
    candidate_parser.add_argument("--project-root", type=Path, default=Path("."))
    contract_parser = sub.add_parser("check-contract")
    contract_parser.add_argument("--project-root", type=Path, default=Path("."))
    return parser


def main(argv: list[str] | None = None) -> int:
    args = build_parser().parse_args(argv)
    root = args.project_root.resolve()
    if args.command == "check-version":
        validate_version(root)
        print(f"V090_VERSION_OK {PROFILE.version_name} ({PROFILE.version_code})")
        return 0
    if args.command == "validate-candidate":
        validate_candidate_tag(args.tag)
        validate_version(root)
        print("V090_CANDIDATE_METADATA_OK")
        return 0
    if args.command == "check-contract":
        check_contract(root)
        return 0
    raise AssertionError("argparse returned an unknown command")


if __name__ == "__main__":
    try:
        raise SystemExit(main())
    except V090ReleaseError as exc:
        print(f"V090_RELEASE_FAILED: {exc}", file=sys.stderr)
        raise SystemExit(1)
