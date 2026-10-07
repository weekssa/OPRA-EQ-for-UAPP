#!/usr/bin/env python3
"""Publish the one qualified v0.8.0-beta artifact as a non-latest prerelease.

This publisher is intentionally pinned to the already-qualified main source and
signed-beta run. It never builds or signs an APK, moves a tag, or updates the
stable latest-release pointer.
"""
from __future__ import annotations

import argparse
import datetime as dt
import hashlib
import json
import os
import re
import stat
import subprocess
import sys
import urllib.parse
import zipfile
from dataclasses import dataclass
from pathlib import Path
from typing import Any

import promote_release_candidate as stable

REPOSITORY = "weekssa/OPRA-EQ-for-UAPP"
TAG = "v0.8.0-beta"
SOURCE_SHA = "4190c6ca51694ea0a80583a83fd3cb09b5088a7d"
SOURCE_TREE = "80822fea05dd8845a4dac23c19d090eeface00a9"
APP_MAIN_TREE = "857d02a53d0df44fb0bd46e5ddad3b319dc48dab"
APP_TEST_TREE = "470979fb22a91c4514534badda6c2cc2032abfb6"
APP_ANDROID_TEST_TREE = "da2b4f31e18025ebd00f0a84f274e0e8a8624800"
CANDIDATE_WORKFLOW = ".github/workflows/signed-beta.yml"
PUBLISHER_WORKFLOW = ".github/workflows/publish-beta-prerelease.yml"
CANDIDATE_WORKFLOW_ID = 345032156
CANDIDATE_RUN_ID = 37697940678
CANDIDATE_RUN_NUMBER = 1378
CANDIDATE_ARTIFACT_ID = 11517325354
CANDIDATE_ARTIFACT_NAME = f"EQ-Library-signed-beta-{SOURCE_SHA}"
CANDIDATE_ARTIFACT_DIGEST = "67a2e9135442559211a1627a94a7adbdd87a7ea7a325dade9965e88f62da4ff1"
CANDIDATE_APK_NAME = "EQ-Library-v0.8.0-beta-beta-4190c6c.apk"
PUBLIC_APK_NAME = "EQ-Library-v0.8.0-beta.apk"
APK_SHA256 = "b352c4d10a91f9c378a1a18012054a803a23807d4c454fb9b2dde613b7729782"
CANDIDATE_MANIFEST_SHA256 = "b8c146787f35a3ebb951f68e5e6de205c80d52ff3d393c66d2fcaeece440f0e3"
R8_MAPPING_SHA256 = "ccfa65ef1137dcc6bea31d1109759c3559f0da0fc501f5b67835236c11fb729e"
SIGNER_SHA256 = "65c1c1256dae3c49e3548f334c91f0ba991969e9be9e0b223ba4e253d2114747"
PACKAGE_ID = "com.weekssa.opraeqforuapp"
VERSION_NAME = "0.8.0-beta"
VERSION_CODE = 10
LATEST_STABLE_TAG = "v0.7.2"
LATEST_STABLE_APK_SHA256 = "efdd63ddb305d0624f805cc53e4ce27aae7d1ddeb169e8f965302d0f290ba64a"
C05_MANIFEST_SHA256 = "e09084b77bf1e1e702628b51195acd4e394786b6f56e076cdeb69bcc211f825e"
POST_MERGE_REPORT_SHA256 = "6a0fbace2c9e95131cad32fcb01154961c2ca10ab8bb9e4d9d9dc2b769412324"
POST_MERGE_EVIDENCE_MANIFEST_SHA256 = "172dceb7354fdc9327e081694c9356d1bb7b69a86497e00d13904c664f6e8454"
ARTIFACT_MAX_BYTES = 64 * 1024 * 1024
TOTAL_UNCOMPRESSED_MAX_BYTES = 160 * 1024 * 1024
SHA256_RE = re.compile(r"^[0-9a-f]{64}$")
SOURCE_RE = re.compile(r"^[0-9a-f]{40}$")


@dataclass(frozen=True)
class CandidatePin:
    tag: str = TAG
    source_sha: str = SOURCE_SHA
    run_id: int = CANDIDATE_RUN_ID
    run_number: int = CANDIDATE_RUN_NUMBER
    artifact_id: int = CANDIDATE_ARTIFACT_ID
    artifact_name: str = CANDIDATE_ARTIFACT_NAME
    artifact_digest: str = CANDIDATE_ARTIFACT_DIGEST
    apk_name: str = CANDIDATE_APK_NAME
    apk_sha256: str = APK_SHA256
    manifest_sha256: str = CANDIDATE_MANIFEST_SHA256
    signer_sha256: str = SIGNER_SHA256
    version_name: str = VERSION_NAME
    version_code: int = VERSION_CODE
    package_id: str = PACKAGE_ID
    candidate_target: str = "black-pearl-ja11"
    r8_mapping_sha256: str = R8_MAPPING_SHA256


BETA = CandidatePin()


def require(condition: bool, message: str) -> None:
    stable.require(condition, message)


def sha256_hex(data: bytes) -> str:
    return hashlib.sha256(data).hexdigest()


def expected_archive_names(pin: CandidatePin) -> set[str]:
    return {
        pin.apk_name,
        f"{pin.apk_name}.sha256",
        f"{pin.apk_name}.idsig",
        "candidate-manifest.json",
        "apksigner-verification.txt",
        "zipalign-verification.txt",
        "r8-mapping.txt",
    }


def _manifest_object(pairs: list[tuple[str, Any]]) -> dict[str, Any]:
    return stable._unique_json_object(pairs)


def validate_candidate_archive(
    archive_bytes: bytes,
    *,
    artifact_digest: str,
    project_root: Path,
    pin: CandidatePin = BETA,
) -> dict[str, Any]:
    """Validate the exact signed-beta ZIP contract without rebuilding or resigning."""
    require(len(archive_bytes) <= ARTIFACT_MAX_BYTES, "signed-beta archive exceeds the size limit")
    stable.require_matching_sha256_digest(
        sha256_hex(archive_bytes), artifact_digest, "signed-beta ZIP differs from the immutable Actions digest"
    )
    require(SOURCE_RE.fullmatch(pin.source_sha) is not None, "pinned beta source SHA is malformed")
    require(SHA256_RE.fullmatch(pin.apk_sha256) is not None, "pinned beta APK SHA-256 is malformed")
    try:
        archive = zipfile.ZipFile(__import__("io").BytesIO(archive_bytes), "r")
    except (zipfile.BadZipFile, OSError) as exc:
        raise stable.PromotionError("signed-beta artifact is not a valid ZIP") from exc
    with archive:
        infos = archive.infolist()
        names = [info.filename for info in infos]
        require(len(names) == len(set(names)), "signed-beta archive contains duplicate file names")
        require(set(names) == expected_archive_names(pin), "signed-beta archive file set differs from the pinned contract")
        require(sum(info.file_size for info in infos) <= TOTAL_UNCOMPRESSED_MAX_BYTES,
                "signed-beta archive expands beyond the size limit")
        files = {info.filename: stable._bounded_member(archive, info) for info in infos}

    manifest_bytes = files["candidate-manifest.json"]
    require(sha256_hex(manifest_bytes) == pin.manifest_sha256,
            "signed-beta candidate-manifest bytes differ from the independently recorded SHA-256")
    try:
        manifest = json.loads(manifest_bytes.decode("utf-8", errors="strict"), object_pairs_hook=_manifest_object)
    except (UnicodeDecodeError, json.JSONDecodeError) as exc:
        raise stable.PromotionError("signed-beta candidate manifest is invalid UTF-8 JSON") from exc
    require(isinstance(manifest, dict), "signed-beta candidate manifest must be an object")
    expected_manifest = {
        "sourceSha": pin.source_sha,
        "candidateTarget": pin.candidate_target,
        "apk": pin.apk_name,
        "apkSha256": pin.apk_sha256,
        "packageId": pin.package_id,
        "versionName": pin.version_name,
        "versionCode": pin.version_code,
        "signerSha256": pin.signer_sha256,
        "r8MinificationEnabled": True,
        "r8MappingSha256": pin.r8_mapping_sha256,
        "capabilityProfile": "TRN Black Pearl final-native-readback-gated Direct Flash; FiiO JA11 exact model; five-band User 1 editor; global EQ gain",
        "testPlan": "docs/UX_REVIEW_WORKFLOW/context/black-pearl-ja11-remediation-20260927/05-release-handoff.md",
        "evidenceIds": ["BLACK-PEARL-SOFTWARE", "JA11-SOFTWARE"],
        "outstanding": "owner Pixel 9 TRN Black Pearl and FiiO JA11 physical validation; final public release/public-support approval",
    }
    require(manifest == expected_manifest, "signed-beta manifest differs from the pinned official run contract")
    require(stable.read_pinned_signer(project_root) == pin.signer_sha256,
            "repository pinned signer differs from the qualified beta signer")
    apk_bytes = files[pin.apk_name]
    require(sha256_hex(apk_bytes) == pin.apk_sha256, "signed-beta APK bytes differ from the qualified SHA-256")
    checksum = files[f"{pin.apk_name}.sha256"].decode("ascii", errors="strict")
    require(checksum == f"{pin.apk_sha256}  dist/{pin.apk_name}\n",
            "signed-beta checksum file differs from the pinned APK bytes")
    require(files[f"{pin.apk_name}.idsig"], "signed-beta APK v4 sidecar is empty")
    stable.validate_saved_signer_report(files["apksigner-verification.txt"], pin.signer_sha256)
    stable.validate_saved_alignment_report(files["zipalign-verification.txt"])
    require(sha256_hex(files["r8-mapping.txt"]) == pin.r8_mapping_sha256,
            "private R8 mapping digest differs from the candidate manifest")
    stable.validate_r8_mapping(files["r8-mapping.txt"])
    return {
        "pin": pin,
        "files": files,
        "manifest": manifest,
        "apk_name": pin.apk_name,
        "apk_sha256": pin.apk_sha256,
        "signer_sha256": pin.signer_sha256,
        "artifact_digest": stable.normalize_sha256(artifact_digest),
        "candidate_manifest_sha256": pin.manifest_sha256,
        "version_name": pin.version_name,
        "version_code": pin.version_code,
        "package_id": pin.package_id,
        "source_sha": pin.source_sha,
    }


def verify_android(candidate: dict[str, Any], build_tools: Path) -> dict[str, bytes]:
    logs, actual_code = stable.verify_android_apk(
        candidate["files"][candidate["apk_name"]],
        build_tools=build_tools,
        signer_sha256=candidate["signer_sha256"],
        expected_version_name=candidate["version_name"],
        expected_version_code=candidate["version_code"],
    )
    require(actual_code == VERSION_CODE, "signed-beta APK version code is not 10")
    return logs


def validate_official_run_and_artifact(api: stable.GitHubApi, pin: CandidatePin = BETA) -> dict[str, Any]:
    workflow = api.json("GET", api.repo_path + "/actions/workflows/signed-beta.yml")
    require(workflow.get("id") == CANDIDATE_WORKFLOW_ID and workflow.get("path") == CANDIDATE_WORKFLOW,
            "pinned official Signed EQ Library Beta Candidate workflow identity changed")
    require(workflow.get("state") == "active", "official signed-beta workflow is not active")
    run = api.json("GET", api.repo_path + f"/actions/runs/{pin.run_id}")
    require(run.get("id") == pin.run_id and run.get("run_number") == pin.run_number,
            "official signed-beta workflow run identity changed")
    require(run.get("workflow_id") == CANDIDATE_WORKFLOW_ID and run.get("path") == CANDIDATE_WORKFLOW,
            "signed-beta run did not come from the pinned official workflow")
    require(run.get("status") == "completed" and run.get("conclusion") == "success" and
            run.get("run_attempt") == 1, "pinned official signed-beta run did not succeed on its first attempt")
    require(run.get("event") == "workflow_dispatch" and run.get("head_branch") == "main" and
            run.get("head_sha") == pin.source_sha,
            "signed-beta run was not dispatched from the exact merged main source")
    repository = run.get("head_repository") or {}
    require(repository.get("full_name", "").casefold() == REPOSITORY.casefold(),
            "signed-beta run belongs to another repository")
    artifact = api.json("GET", api.repo_path + f"/actions/artifacts/{pin.artifact_id}")
    require(artifact.get("id") == pin.artifact_id and artifact.get("name") == pin.artifact_name,
            "signed-beta artifact identity or name changed")
    require(artifact.get("expired") is False and int(artifact.get("size_in_bytes", 0)) > 0 and
            int(artifact.get("size_in_bytes", 0)) <= ARTIFACT_MAX_BYTES,
            "signed-beta artifact is expired, empty, or exceeds the size limit")
    require(artifact.get("digest") == f"sha256:{pin.artifact_digest}",
            "GitHub artifact digest differs from the independently verified signed-beta artifact")
    artifact_run = artifact.get("workflow_run") or {}
    require(artifact_run.get("id") == pin.run_id and artifact_run.get("head_sha") == pin.source_sha and
            artifact_run.get("head_branch") == "main",
            "signed-beta artifact is not attached to the exact official workflow run")
    return {"run": run, "artifact": artifact}


def read_candidate_archive(api: stable.GitHubApi, pin: CandidatePin = BETA) -> bytes:
    path = api.repo_path + f"/actions/artifacts/{pin.artifact_id}/zip"
    _, data, _ = api.request_bytes(api.api_url + path, max_bytes=ARTIFACT_MAX_BYTES)
    require(sha256_hex(data) == pin.artifact_digest,
            "downloaded signed-beta archive does not match GitHub's immutable artifact digest")
    return data


def validate_checkout(project_root: Path, publisher_sha: str) -> None:
    require(SOURCE_RE.fullmatch(publisher_sha) is not None, "publisher workflow SHA is malformed")
    require(os.environ.get("GITHUB_REPOSITORY", "").casefold() == REPOSITORY.casefold(),
            "beta publisher is restricted to the canonical repository")
    require(os.environ.get("GITHUB_REF") == "refs/heads/main", "beta publisher may run only from main")
    head = subprocess.run(["git", "rev-parse", "HEAD"], cwd=project_root, check=True,
                         text=True, stdout=subprocess.PIPE).stdout.strip()
    require(head == publisher_sha, "publisher checkout does not match the workflow's exact main SHA")
    ancestor = subprocess.run(["git", "merge-base", "--is-ancestor", SOURCE_SHA, publisher_sha],
                              cwd=project_root, check=False, stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL)
    require(ancestor.returncode == 0, "qualified beta source is not an ancestor of the publisher workflow source")
    expected_trees = {
        "app/src/main": APP_MAIN_TREE,
        "app/src/test": APP_TEST_TREE,
        "app/src/androidTest": APP_ANDROID_TEST_TREE,
    }
    for path, expected in expected_trees.items():
        actual = subprocess.run(["git", "rev-parse", f"{publisher_sha}:{path}"], cwd=project_root,
                                check=True, text=True, stdout=subprocess.PIPE).stdout.strip()
        require(actual == expected, f"publisher main changed the frozen app/test tree at {path}")
    version_name, version_code = stable.parse_app_version(project_root)
    require(version_name == VERSION_NAME and version_code == VERSION_CODE,
            "publisher source version differs from the official signed-beta artifact")
    require(stable.read_pinned_signer(project_root) == SIGNER_SHA256,
            "publisher source pinned signer differs from the qualified beta artifact")


def github_api_from_environment() -> stable.GitHubApi:
    return stable.GitHubApi(
        os.environ.get("GITHUB_REPOSITORY", ""),
        os.environ.get("GH_TOKEN", ""),
        os.environ.get("GITHUB_API_URL", ""),
    )


def current_main_sha(api: stable.GitHubApi) -> str:
    return stable.current_main_sha(api)


def verify_main_snapshot(api: stable.GitHubApi, publisher_sha: str) -> None:
    require(current_main_sha(api) == publisher_sha,
            "main moved during beta publication; leave any exact private draft for a later verified resume")


def latest_stable(api: stable.GitHubApi) -> dict[str, Any]:
    latest = api.json("GET", api.repo_path + "/releases/latest")
    require(latest.get("tag_name") == LATEST_STABLE_TAG and latest.get("draft") is False and
            latest.get("prerelease") is False,
            "the stable latest release is not the expected v0.7.2")
    require(latest.get("immutable") is True, "stable v0.7.2 release is not read back as immutable")
    assets = latest.get("assets")
    require(isinstance(assets, list), "stable latest release asset list is malformed")
    apk_assets = [asset for asset in assets if isinstance(asset, dict) and asset.get("name") == "EQ-Library-v0.7.2.apk"]
    require(len(apk_assets) == 1 and apk_assets[0].get("digest") == f"sha256:{LATEST_STABLE_APK_SHA256}",
            "stable latest APK identity does not match the qualified v0.7.2 baseline")
    return latest


def release_notes(project_root: Path) -> bytes:
    notes = (project_root / "docs/releases/v0.8.0-beta.md").read_bytes()
    require(notes.startswith(b"# EQ Library v0.8.0 beta\n"), "beta release notes have an unexpected title")
    decoded = notes.decode("utf-8", errors="strict")
    for marker in (SOURCE_SHA, CANDIDATE_ARTIFACT_DIGEST, APK_SHA256, SIGNER_SHA256,
                   "in-place upgrade and separate signed clean-install/core smoke both passed",
                   "Black Pearl C05-C qualification was a read-only", "Class B remains",
                   "Stable latest remains v0.7.2"):
        require(marker in decoded, f"final beta release notes omit required verified fact {marker!r}")
    return notes


def candidate_tag_message(notes_sha: str, pin: CandidatePin = BETA) -> str:
    require(SHA256_RE.fullmatch(notes_sha) is not None, "beta release notes SHA-256 is malformed")
    return (
        "OPRA EQ Library beta release binding\n"
        f"Release-Tag: {pin.tag}\n"
        f"Source-SHA: {pin.source_sha}\n"
        f"Candidate-Workflow: {CANDIDATE_WORKFLOW}\n"
        f"Candidate-Run-ID: {pin.run_id}\n"
        f"Candidate-Artifact-ID: {pin.artifact_id}\n"
        f"Candidate-Artifact-SHA256: {pin.artifact_digest}\n"
        f"Candidate-Manifest-SHA256: {pin.manifest_sha256}\n"
        f"APK-SHA256: {pin.apk_sha256}\n"
        f"Release-Notes-SHA256: {notes_sha}\n"
    )


def resolve_annotated_tag(api: stable.GitHubApi, notes_sha: str, pin: CandidatePin = BETA) -> str | None:
    path = api.repo_path + "/git/ref/tags/" + urllib.parse.quote(pin.tag, safe="")
    ref = api.optional_json(path)
    if ref is None:
        return None
    obj = ref.get("object") or {}
    require(obj.get("type") == "tag", "beta tag exists as a lightweight tag; refusing to adopt it")
    tag_sha = obj.get("sha", "")
    require(SOURCE_RE.fullmatch(tag_sha) is not None, "annotated beta tag object SHA is malformed")
    annotated = api.json("GET", api.repo_path + f"/git/tags/{tag_sha}")
    target = annotated.get("object") or {}
    require(annotated.get("tag") == pin.tag and annotated.get("message") == candidate_tag_message(notes_sha, pin) and
            target.get("type") == "commit" and target.get("sha") == pin.source_sha,
            "existing beta tag does not bind the exact source, artifact, and release notes")
    return pin.source_sha


def ensure_annotated_tag(api: stable.GitHubApi, *, notes_sha: str, publisher_sha: str,
                         pin: CandidatePin = BETA) -> None:
    verify_main_snapshot(api, publisher_sha)
    latest_stable(api)
    release = api.optional_json(api.repo_path + "/releases/tags/" + urllib.parse.quote(pin.tag, safe=""))
    resolved = resolve_annotated_tag(api, notes_sha, pin)
    if resolved is not None:
        require(release is None or release.get("draft") is True or release.get("prerelease") is True,
                "v0.8.0-beta tag is already used by an incompatible release")
        return
    require(release is None, "a release already uses v0.8.0-beta but its immutable tag ref is absent")
    tag_object = api.json("POST", api.repo_path + "/git/tags", {
        "tag": pin.tag,
        "message": candidate_tag_message(notes_sha, pin),
        "object": pin.source_sha,
        "type": "commit",
        "tagger": {
            "name": "github-actions[bot]",
            "email": "41898282+github-actions[bot]@users.noreply.github.com",
            "date": dt.datetime.now(dt.timezone.utc).isoformat(timespec="seconds").replace("+00:00", "Z"),
        },
    })
    tag_sha = tag_object.get("sha", "")
    require(SOURCE_RE.fullmatch(tag_sha) is not None and tag_object.get("tag") == pin.tag and
            tag_object.get("message") == candidate_tag_message(notes_sha, pin) and
            (tag_object.get("object") or {}).get("type") == "commit" and
            (tag_object.get("object") or {}).get("sha") == pin.source_sha,
            "GitHub did not create the exact annotated beta tag object")
    try:
        api.json("POST", api.repo_path + "/git/refs", {"ref": f"refs/tags/{pin.tag}", "sha": tag_sha})
    except stable.PromotionError:
        # A timed-out/refused response may follow a successful write; read the ref once.
        pass
    require(resolve_annotated_tag(api, notes_sha, pin) == pin.source_sha,
            "annotated beta tag readback did not bind the exact qualified commit")
    verify_main_snapshot(api, publisher_sha)


def _asset_download(api: stable.GitHubApi, asset: dict[str, Any], max_bytes: int = 2 * 1024 * 1024) -> bytes:
    asset_id = asset.get("id")
    require(isinstance(asset_id, int) and asset_id > 0, "beta release asset has no valid ID")
    url = api.api_url + api.repo_path + f"/releases/assets/{asset_id}"
    _, data, _ = api.request_bytes(url, accept="application/octet-stream", max_bytes=max_bytes)
    require(sha256_hex(data) == stable.normalize_sha256(asset.get("digest", "")),
            "beta release asset bytes do not match GitHub's recorded digest")
    return data


def _publication_run_id(api: stable.GitHubApi, release: dict[str, Any], pin: CandidatePin,
                        notes_sha: str, current_run_id: str) -> str:
    names = [asset.get("name") for asset in release.get("assets", []) if isinstance(asset, dict)]
    require(len(names) == len(set(names)), "beta draft release contains duplicate asset names")
    existing_run_ids: set[str] = set()
    for asset in release.get("assets", []):
        if not isinstance(asset, dict) or asset.get("name") not in {"beta-release-manifest.json", "release-provenance.json"}:
            continue
        data = _asset_download(api, asset)
        try:
            record = json.loads(data.decode("utf-8", errors="strict"), object_pairs_hook=_manifest_object)
        except (UnicodeDecodeError, json.JSONDecodeError) as exc:
            raise stable.PromotionError("existing beta release evidence asset is invalid JSON") from exc
        require(isinstance(record, dict), "existing beta release evidence must be JSON objects")
        _validate_publication_record(record, pin, notes_sha)
        existing_run_ids.add(record["publisherWorkflowRunId"])
    require(len(existing_run_ids) <= 1, "partial beta draft assets disagree on publisher run identity")
    return next(iter(existing_run_ids)) if existing_run_ids else current_run_id


def beta_release_manifest(pin: CandidatePin, notes_sha: str, publisher_sha: str,
                          publisher_run_id: str) -> bytes:
    value = {
        "schemaVersion": 1,
        "repository": REPOSITORY,
        "releaseTag": pin.tag,
        "prerelease": True,
        "makeLatest": False,
        "releaseSourceSha": pin.source_sha,
        "releaseSourceTree": SOURCE_TREE,
        "appProductionTree": APP_MAIN_TREE,
        "appUnitTestTree": APP_TEST_TREE,
        "appInstrumentationTestTree": APP_ANDROID_TEST_TREE,
        "candidateWorkflow": CANDIDATE_WORKFLOW,
        "candidateWorkflowRunNumber": pin.run_number,
        "candidateWorkflowRunId": pin.run_id,
        "candidateArtifactId": pin.artifact_id,
        "candidateArtifactName": pin.artifact_name,
        "candidateArtifactSha256": pin.artifact_digest,
        "candidateManifestSha256": pin.manifest_sha256,
        "candidateTarget": pin.candidate_target,
        "apk": PUBLIC_APK_NAME,
        "candidateApkName": pin.apk_name,
        "apkSha256": pin.apk_sha256,
        "packageId": pin.package_id,
        "versionName": pin.version_name,
        "versionCode": pin.version_code,
        "signerSha256": pin.signer_sha256,
        "r8MappingSha256": pin.r8_mapping_sha256,
        "gates": {"C05-C": "PASS", "G7": "PASS", "G8": "PASS", "G9": "PASS"},
        "evidence": {
            "c05ManifestSha256": C05_MANIFEST_SHA256,
            "postMergeReportSha256": POST_MERGE_REPORT_SHA256,
            "postMergeEvidenceManifestSha256": POST_MERGE_EVIDENCE_MANIFEST_SHA256,
        },
        "releaseNotesSha256": notes_sha,
        "publisherWorkflow": PUBLISHER_WORKFLOW,
        "publisherCommitSha": publisher_sha,
        "publisherWorkflowRunId": publisher_run_id,
        "latestStableTagAfterPublication": LATEST_STABLE_TAG,
    }
    return (json.dumps(value, indent=2, sort_keys=True) + "\n").encode("utf-8")


def release_provenance(pin: CandidatePin, notes_sha: str, publisher_sha: str,
                       publisher_run_id: str, beta_manifest_sha: str) -> bytes:
    value = {
        "schemaVersion": 1,
        "repository": REPOSITORY,
        "releaseTag": pin.tag,
        "releasePrerelease": True,
        "releaseMakeLatest": False,
        "releaseSourceSha": pin.source_sha,
        "candidateWorkflow": CANDIDATE_WORKFLOW,
        "candidateWorkflowRunId": pin.run_id,
        "candidateWorkflowRunNumber": pin.run_number,
        "candidateArtifactId": pin.artifact_id,
        "candidateArtifactName": pin.artifact_name,
        "candidateArtifactSha256": pin.artifact_digest,
        "candidateManifestSha256": pin.manifest_sha256,
        "candidateApkName": pin.apk_name,
        "publicApkName": PUBLIC_APK_NAME,
        "apkSha256": pin.apk_sha256,
        "packageId": pin.package_id,
        "versionName": pin.version_name,
        "versionCode": pin.version_code,
        "signerSha256": pin.signer_sha256,
        "r8MappingSha256": pin.r8_mapping_sha256,
        "releaseNotesSha256": notes_sha,
        "betaReleaseManifestSha256": beta_manifest_sha,
        "publisherWorkflow": PUBLISHER_WORKFLOW,
        "publisherCommitSha": publisher_sha,
        "publisherWorkflowRunId": publisher_run_id,
    }
    return (json.dumps(value, indent=2, sort_keys=True) + "\n").encode("utf-8")


def _validate_publication_record(record: dict[str, Any], pin: CandidatePin, notes_sha: str) -> None:
    expected = {
        "repository": REPOSITORY,
        "releaseTag": pin.tag,
        "releaseSourceSha": pin.source_sha,
        "candidateWorkflow": CANDIDATE_WORKFLOW,
        "candidateWorkflowRunId": pin.run_id,
        "candidateWorkflowRunNumber": pin.run_number,
        "candidateArtifactId": pin.artifact_id,
        "candidateArtifactName": pin.artifact_name,
        "candidateArtifactSha256": pin.artifact_digest,
        "candidateManifestSha256": pin.manifest_sha256,
        "apkSha256": pin.apk_sha256,
        "packageId": pin.package_id,
        "versionName": pin.version_name,
        "versionCode": pin.version_code,
        "signerSha256": pin.signer_sha256,
        "r8MappingSha256": pin.r8_mapping_sha256,
        "releaseNotesSha256": notes_sha,
        "publisherWorkflow": PUBLISHER_WORKFLOW,
    }
    require(all(record.get(key) == value for key, value in expected.items()),
            "existing beta release evidence asset does not bind the exact qualified candidate")
    publisher_run = record.get("publisherWorkflowRunId")
    require(isinstance(publisher_run, str) and re.fullmatch(r"[1-9][0-9]*", publisher_run) is not None,
            "beta release evidence contains an invalid publisher workflow run ID")
    publisher_sha = record.get("publisherCommitSha")
    require(isinstance(publisher_sha, str) and SOURCE_RE.fullmatch(publisher_sha) is not None,
            "beta release evidence contains an invalid publisher commit SHA")
    if "prerelease" in record:
        require(record.get("prerelease") is True and record.get("makeLatest") is False,
                "beta release manifest does not explicitly preserve prerelease/latest semantics")
    if "releasePrerelease" in record:
        require(record.get("releasePrerelease") is True and record.get("releaseMakeLatest") is False,
                "beta release provenance does not explicitly preserve prerelease/latest semantics")


def public_assets(candidate: dict[str, Any], pin: CandidatePin, notes_sha: str,
                  publisher_sha: str, publisher_run_id: str) -> dict[str, bytes]:
    files = candidate["files"]
    apk_bytes = files[pin.apk_name]
    checksum = f"{pin.apk_sha256}  {PUBLIC_APK_NAME}\n".encode("ascii")
    manifest_bytes = beta_release_manifest(pin, notes_sha, publisher_sha, publisher_run_id)
    provenance_bytes = release_provenance(pin, notes_sha, publisher_sha, publisher_run_id,
                                           sha256_hex(manifest_bytes))
    return {
        PUBLIC_APK_NAME: apk_bytes,
        f"{PUBLIC_APK_NAME}.sha256": checksum,
        "beta-release-manifest.json": manifest_bytes,
        "release-provenance.json": provenance_bytes,
        "apksigner-verification.txt": files["apksigner-verification.txt"],
        "zipalign-verification.txt": files["zipalign-verification.txt"],
    }


def _verify_publication_metadata_assets(assets: dict[str, bytes], pin: CandidatePin, notes_sha: str) -> None:
    for name in ("beta-release-manifest.json", "release-provenance.json"):
        try:
            record = json.loads(assets[name].decode("utf-8", errors="strict"), object_pairs_hook=_manifest_object)
        except (KeyError, UnicodeDecodeError, json.JSONDecodeError) as exc:
            raise stable.PromotionError(f"public beta asset {name} is not valid UTF-8 JSON") from exc
        require(isinstance(record, dict), f"public beta asset {name} must be a JSON object")
        _validate_publication_record(record, pin, notes_sha)
    manifest = json.loads(assets["beta-release-manifest.json"].decode("utf-8"))
    provenance = json.loads(assets["release-provenance.json"].decode("utf-8"))
    require(sha256_hex(assets["beta-release-manifest.json"]) == provenance.get("betaReleaseManifestSha256"),
            "public beta provenance does not bind the published beta manifest bytes")
    require(manifest.get("publisherWorkflowRunId") == provenance.get("publisherWorkflowRunId") and
            manifest.get("publisherCommitSha") == provenance.get("publisherCommitSha"),
            "beta manifest and release provenance disagree on the publisher run")


def release_asset_names() -> set[str]:
    return {
        PUBLIC_APK_NAME,
        f"{PUBLIC_APK_NAME}.sha256",
        "beta-release-manifest.json",
        "release-provenance.json",
        "apksigner-verification.txt",
        "zipalign-verification.txt",
    }


def _check_release_header(release: dict[str, Any], notes: str, *, published: bool) -> None:
    require(release.get("tag_name") == TAG and release.get("name") == "EQ Library v0.8.0 beta" and
            stable.normalize_release_body(release.get("body", "")) == stable.normalize_release_body(notes),
            "beta release tag, title, or notes differ from the reviewed release file")
    require(release.get("draft") is (not published) and release.get("prerelease") is True,
            "beta release state does not match draft/prerelease expectations")
    if published:
        require(release.get("immutable") is True, "published beta release is not read back as immutable")


def verify_published_release(api: stable.GitHubApi, release: dict[str, Any], *, candidate: dict[str, Any],
                             notes: str, notes_sha: str, build_tools: Path, pin: CandidatePin = BETA) -> None:
    _check_release_header(release, notes, published=True)
    require(resolve_annotated_tag(api, notes_sha, pin) == pin.source_sha,
            "published beta tag does not resolve to the exact qualified merge")
    assets = release.get("assets")
    require(isinstance(assets, list), "published beta release assets are malformed")
    by_name = {asset.get("name"): asset for asset in assets if isinstance(asset, dict)}
    require(set(by_name) == release_asset_names() and len(by_name) == len(assets),
            "published beta release does not have the exact approved asset set")
    expected = public_assets(candidate, pin, notes_sha, candidate["publisher_sha"],
                             candidate["publisher_run_id"])
    # The publisher run recorded by the first draft is stable across retries.
    expected["beta-release-manifest.json"] = _asset_download(api, by_name["beta-release-manifest.json"])
    expected["release-provenance.json"] = _asset_download(api, by_name["release-provenance.json"])
    _verify_publication_metadata_assets(expected, pin, notes_sha)
    stable._verify_release_assets(api, release, expected)
    stable._verify_public_asset_downloads(api, by_name, expected)
    require(sha256_hex(expected[PUBLIC_APK_NAME]) == pin.apk_sha256,
            "published beta APK bytes differ from the signed candidate")
    checksum = expected[f"{PUBLIC_APK_NAME}.sha256"].decode("ascii", errors="strict")
    require(checksum == f"{pin.apk_sha256}  {PUBLIC_APK_NAME}\n",
            "published beta checksum does not bind the public APK filename")
    signer = expected["apksigner-verification.txt"]
    alignment = expected["zipalign-verification.txt"]
    stable.validate_saved_signer_report(signer, pin.signer_sha256)
    stable.validate_saved_alignment_report(alignment)
    stable.verify_android_apk(expected[PUBLIC_APK_NAME], build_tools=build_tools,
                               signer_sha256=pin.signer_sha256, expected_version_name=pin.version_name,
                               expected_version_code=pin.version_code)
    latest = latest_stable(api)
    require(latest.get("tag_name") == LATEST_STABLE_TAG,
            "v0.8.0 beta changed the public stable/latest release")


def create_or_resume_release(api: stable.GitHubApi, *, notes: str, publisher_sha: str,
                             pin: CandidatePin = BETA) -> dict[str, Any]:
    existing = api.optional_json(api.repo_path + "/releases/tags/" + urllib.parse.quote(pin.tag, safe=""))
    if existing is not None:
        _check_release_header(existing, notes, published=existing.get("draft") is False)
        require(existing.get("draft") is True or existing.get("immutable") is True,
                "existing beta release is neither a draft nor an immutable published release")
        return existing
    verify_main_snapshot(api, publisher_sha)
    latest_stable(api)
    created = api.json("POST", api.repo_path + "/releases", {
        "tag_name": pin.tag,
        "name": "EQ Library v0.8.0 beta",
        "body": notes,
        "draft": True,
        "prerelease": True,
        "make_latest": "false",
        "generate_release_notes": False,
    })
    _check_release_header(created, notes, published=False)
    return created


def _verify_draft_assets(api: stable.GitHubApi, release: dict[str, Any], expected: dict[str, bytes]) -> dict[str, dict[str, Any]]:
    assets = release.get("assets", [])
    require(isinstance(assets, list), "beta draft asset list is malformed")
    names = [asset.get("name") for asset in assets if isinstance(asset, dict)]
    require(len(names) == len(assets) and len(names) == len(set(names)),
            "beta draft contains malformed or duplicate asset names")
    require(set(names).issubset(expected), "beta draft contains an unapproved asset")
    existing = {name: expected[name] for name in names}
    by_name = stable._verify_release_assets(api, release, existing) if existing else {}
    for name, asset in by_name.items():
        if name in {"beta-release-manifest.json", "release-provenance.json"}:
            downloaded = _asset_download(api, asset)
            require(downloaded == expected[name], f"resumed beta draft {name} differs from the exact candidate")
    return by_name


def upload_release_asset(api: stable.GitHubApi, release: dict[str, Any], name: str, data: bytes) -> dict[str, Any]:
    upload_url = release.get("upload_url", "").split("{", 1)[0]
    parsed = urllib.parse.urlsplit(upload_url)
    require(parsed.scheme == "https" and parsed.hostname == "uploads.github.com" and
            parsed.path.startswith(api.repo_path + "/releases/"),
            "GitHub returned an unsupported beta asset upload URL")
    content_type = "application/vnd.android.package-archive" if name.endswith(".apk") else (
        "application/json" if name.endswith(".json") else "text/plain; charset=utf-8")
    url = upload_url + "?" + urllib.parse.urlencode({"name": name})
    _, data_json, _ = api.request_bytes(url, method="POST", body=data, content_type=content_type,
                                        max_bytes=2 * 1024 * 1024)
    result = json.loads(data_json.decode("utf-8", errors="strict"))
    require(isinstance(result, dict), "GitHub returned an invalid beta asset upload response")
    return result


def publish(api: stable.GitHubApi, *, project_root: Path, build_tools: Path,
            pin: CandidatePin = BETA) -> dict[str, Any]:
    publisher_sha = os.environ.get("GITHUB_SHA", "")
    publisher_run_id = os.environ.get("GITHUB_RUN_ID", "")
    require(re.fullmatch(r"[1-9][0-9]*", publisher_run_id) is not None,
            "beta publisher workflow run ID is invalid")
    validate_checkout(project_root, publisher_sha)
    run_artifact = validate_official_run_and_artifact(api, pin)
    archive = read_candidate_archive(api, pin)
    candidate = validate_candidate_archive(archive, artifact_digest=pin.artifact_digest,
                                           project_root=project_root, pin=pin)
    verify_android(candidate, build_tools)
    notes_bytes = release_notes(project_root)
    notes = notes_bytes.decode("utf-8")
    notes_sha = sha256_hex(notes_bytes)
    candidate.update(publisher_sha=publisher_sha, publisher_run_id=publisher_run_id,
                     official_run_id=run_artifact["run"].get("id"),
                     official_artifact_id=run_artifact["artifact"].get("id"))
    latest_stable(api)
    existing_release = api.optional_json(api.repo_path + "/releases/tags/" + urllib.parse.quote(pin.tag, safe=""))
    if existing_release is not None and existing_release.get("draft") is False:
        verify_published_release(api, existing_release, candidate=candidate, notes=notes,
                                 notes_sha=notes_sha, build_tools=build_tools, pin=pin)
        print(f"BETA_PRERELEASE_ALREADY_VERIFIED tag={pin.tag} source={pin.source_sha}")
        return existing_release

    ensure_annotated_tag(api, notes_sha=notes_sha, publisher_sha=publisher_sha, pin=pin)
    release = create_or_resume_release(api, notes=notes, publisher_sha=publisher_sha, pin=pin)
    if release.get("draft") is False:
        verify_published_release(api, release, candidate=candidate, notes=notes,
                                 notes_sha=notes_sha, build_tools=build_tools, pin=pin)
        print(f"BETA_PRERELEASE_ALREADY_VERIFIED tag={pin.tag} source={pin.source_sha}")
        return release

    publication_run = _publication_run_id(api, release, pin, notes_sha, publisher_run_id)
    assets = public_assets(candidate, pin, notes_sha, publisher_sha, publication_run)
    _verify_publication_metadata_assets(assets, pin, notes_sha)
    existing = _verify_draft_assets(api, release, assets)
    for name, data in assets.items():
        if name not in existing:
            upload_release_asset(api, release, name, data)
    by_name = stable._verify_release_assets(api, release, assets)
    require(set(by_name) == set(assets) == release_asset_names(),
            "beta draft asset set is incomplete")
    stable._verify_public_asset_downloads(api, by_name, assets)
    for name, asset in by_name.items():
        if name in {"beta-release-manifest.json", "release-provenance.json"}:
            require(_asset_download(api, asset) == assets[name],
                    f"beta draft evidence asset {name} failed exact-byte readback")
    require(resolve_annotated_tag(api, notes_sha, pin) == pin.source_sha,
            "beta tag binding changed before publication")
    verify_main_snapshot(api, publisher_sha)
    latest_stable(api)
    published = api.json("PATCH", api.repo_path + f"/releases/{release['id']}", {
        "draft": False,
        "prerelease": True,
        "make_latest": "false",
    })
    _check_release_header(published, notes, published=True)
    final = api.json("GET", api.repo_path + "/releases/tags/" + urllib.parse.quote(pin.tag, safe=""))
    candidate["publisher_run_id"] = publication_run
    verify_published_release(api, final, candidate=candidate, notes=notes,
                             notes_sha=notes_sha, build_tools=build_tools, pin=pin)
    require(isinstance(final.get("html_url"), str) and final["html_url"].startswith("https://github.com/"),
            "beta release page URL is missing")
    print(f"BETA_PRERELEASE_PUBLISHED tag={pin.tag} source={pin.source_sha} apk_sha256={pin.apk_sha256} url={final['html_url']}")
    return final


def verify_read_only(api: stable.GitHubApi, *, project_root: Path, build_tools: Path,
                     pin: CandidatePin = BETA) -> None:
    publisher_sha = os.environ.get("GITHUB_SHA", "")
    validate_checkout(project_root, publisher_sha)
    validate_official_run_and_artifact(api, pin)
    archive = read_candidate_archive(api, pin)
    candidate = validate_candidate_archive(archive, artifact_digest=pin.artifact_digest,
                                           project_root=project_root, pin=pin)
    verify_android(candidate, build_tools)
    notes_bytes = release_notes(project_root)
    notes_sha = sha256_hex(notes_bytes)
    latest_stable(api)
    existing_tag = resolve_annotated_tag(api, notes_sha, pin)
    existing_release = api.optional_json(api.repo_path + "/releases/tags/" + urllib.parse.quote(pin.tag, safe=""))
    require(existing_release is None or existing_tag == pin.source_sha,
            "an unexpected v0.8.0-beta release exists without its exact annotated tag")
    if existing_release is not None:
        _check_release_header(existing_release, notes_bytes.decode("utf-8"),
                              published=existing_release.get("draft") is False)
    print("BETA_CANDIDATE_VERIFY_PASSED")


def check_contract(project_root: Path) -> None:
    workflow = (project_root / PUBLISHER_WORKFLOW).read_text(encoding="utf-8")
    ci = (project_root / ".github/workflows/android-ci.yml").read_text(encoding="utf-8")
    signing = (project_root / "docs/RELEASE_SIGNING.md").read_text(encoding="utf-8")
    notes = (project_root / "docs/releases/v0.8.0-beta.md").read_text(encoding="utf-8")
    release_notes(project_root)
    for marker in ("workflow_dispatch:", "refs/heads/main", "tools/publish_beta_prerelease.py verify",
                   "tools/publish_beta_prerelease.py publish", "contents: read", "contents: write",
                   "persist-credentials: false", "fetch-depth: 0"):
        require(marker in workflow, f"beta publisher workflow is missing required marker {marker!r}")
    verify_job = workflow.split("  verify-candidate:", 1)[1].split("  publish:", 1)[0]
    publish_job = workflow.split("  publish:", 1)[1]
    require("contents: write" not in verify_job and "permissions:\n      actions: read\n      contents: read" in verify_job,
            "beta artifact verification must remain read-only")
    require("permissions:\n      actions: read\n      contents: write" in publish_job,
            "only beta publication may receive contents-write")
    top = workflow.split("jobs:", 1)[0]
    require("contents: write" not in top and "actions: write" not in top,
            "beta publisher must not grant workflow-wide write permission")
    for line in workflow.splitlines():
        if re.match(r"^\s+uses:\s+", line):
            require(re.search(r"@[0-9a-f]{40}(?:\s|$)", line) is not None,
                    "beta publisher actions must be pinned to full commit SHAs")
    for marker in ("\"prerelease\": True", "\"make_latest\": \"false\"",
                   "\"tag\": pin.tag", "\"type\": \"commit\"", "stable latest release"):
        require(marker in Path(__file__).read_text(encoding="utf-8"),
                f"beta publisher source omits release-safety contract marker {marker!r}")
    require(SOURCE_SHA in Path(__file__).read_text(encoding="utf-8") and
            CANDIDATE_ARTIFACT_DIGEST in Path(__file__).read_text(encoding="utf-8") and
            str(CANDIDATE_RUN_ID) in Path(__file__).read_text(encoding="utf-8") and
            str(CANDIDATE_ARTIFACT_ID) in Path(__file__).read_text(encoding="utf-8"),
            "beta publisher is not pinned to the authorized source/run/artifact tuple")
    require("test_publish_beta_prerelease.py" in ci and
            "tools/publish_beta_prerelease.py check-contract" in ci,
            "Android CI does not verify the beta publisher contract")
    require("publish-beta-prerelease.yml" in signing and "make_latest:false" in signing,
            "release-signing policy does not document the dedicated beta publisher")
    require("POST_MERGE_TESTS.md" in notes and "prerelease" in notes.lower() and
            "Stable latest remains v0.7.2" in notes,
            "beta release notes omit completed verification or stable/latest policy")
    print("BETA_PUBLISHER_CONTRACT_PASSED")


def make_parser() -> argparse.ArgumentParser:
    parser = argparse.ArgumentParser(description=__doc__)
    sub = parser.add_subparsers(dest="command", required=True)
    verify_parser = sub.add_parser("verify", help="read-only candidate verification")
    verify_parser.add_argument("--project-root", type=Path, default=Path("."))
    verify_parser.add_argument("--build-tools", type=Path, required=True)
    publish_parser = sub.add_parser("publish", help="publish the pinned beta prerelease")
    publish_parser.add_argument("--project-root", type=Path, default=Path("."))
    publish_parser.add_argument("--build-tools", type=Path, required=True)
    contract_parser = sub.add_parser("check-contract", help="check workflow and release policy source")
    contract_parser.add_argument("--project-root", type=Path, default=Path("."))
    return parser


def main(argv: list[str] | None = None) -> int:
    args = make_parser().parse_args(argv)
    root = args.project_root.resolve()
    if args.command == "check-contract":
        check_contract(root)
        return 0
    api = github_api_from_environment()
    if args.command == "verify":
        verify_read_only(api, project_root=root, build_tools=args.build_tools.resolve())
        return 0
    if args.command == "publish":
        publish(api, project_root=root, build_tools=args.build_tools.resolve())
        return 0
    raise AssertionError("argparse returned an unknown command")


if __name__ == "__main__":
    try:
        raise SystemExit(main())
    except stable.PromotionError as exc:
        print(f"BETA_PUBLISHER_FAILED: {exc}", file=sys.stderr)
        raise SystemExit(1)
