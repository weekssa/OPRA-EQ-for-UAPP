#!/usr/bin/env python3
"""Verify and promote the exact v0.9.0-beta.1 signed candidate.

This publisher is separate from the pinned v0.8.0-beta publisher. It never
builds, signs, or substitutes an APK, and it publishes only a prerelease that
must not become GitHub's latest stable release.
"""

from __future__ import annotations

import argparse
import hashlib
import io
import json
import os
import re
import subprocess
import sys
import urllib.parse
import zipfile
from pathlib import Path
from typing import Any

import promote_release_candidate as stable
import v090_release as profile


PUBLISHER_WORKFLOW_PATH = ".github/workflows/publish-beta-prerelease.yml"
QUALIFICATION_PATH = "docs/releases/v0.9.0/QUALIFICATION.json"
MAX_ARCHIVE_BYTES = 64 * 1024 * 1024
SHA256_RE = re.compile(r"^[0-9a-f]{64}$")


def require(condition: bool, message: str) -> None:
    if not condition:
        raise stable.PromotionError(message)


def sha256_hex(data: bytes) -> str:
    return hashlib.sha256(data).hexdigest()


def normalize_digest(value: str) -> str:
    normalized = stable.normalize_sha256(value)
    require(SHA256_RE.fullmatch(normalized) is not None, "candidate artifact digest is malformed")
    return normalized


def is_allowed_post_candidate_doc(path: str) -> bool:
    return path.startswith("docs/") or path in {"README.md", "CHANGELOG.md"}


def candidate_asset_names() -> set[str]:
    apk_name = f"EQ-Library-{profile.BETA_TAG}.apk"
    return {
        apk_name,
        f"{apk_name}.sha256",
        "candidate-manifest.json",
        "apksigner-verification.txt",
        "zipalign-verification.txt",
        "r8-mapping.txt",
    }


def optional_asset_names() -> set[str]:
    return {f"EQ-Library-{profile.BETA_TAG}.apk.idsig"}


def release_asset_names() -> set[str]:
    apk_name = f"EQ-Library-{profile.BETA_TAG}.apk"
    return {
        apk_name,
        f"{apk_name}.sha256",
        "beta-release-manifest.json",
        "release-provenance.json",
        "apksigner-verification.txt",
        "zipalign-verification.txt",
    }


def parse_positive(value: str) -> int:
    require(re.fullmatch(r"[1-9][0-9]*", value) is not None,
            "candidate run and artifact IDs must be positive integers")
    return int(value)


def android_version(project_root: Path) -> tuple[str, int]:
    return profile.parse_android_version(project_root)


def validate_checkout(project_root: Path, publisher_sha: str, candidate_source_sha: str) -> None:
    require(os.environ.get("GITHUB_REPOSITORY", "").casefold() == profile.REPOSITORY.casefold(),
            "v0.9 beta publisher is restricted to the canonical repository")
    require(os.environ.get("GITHUB_REF") == "refs/heads/main",
            "v0.9 beta publisher may run only from main")
    require(re.fullmatch(r"[0-9a-f]{40}", publisher_sha) is not None,
            "publisher workflow SHA is malformed")
    require(re.fullmatch(r"[0-9a-f]{40}", candidate_source_sha) is not None,
            "candidate source SHA is malformed")
    head = subprocess.run(
        ["git", "rev-parse", "HEAD"], cwd=project_root, check=True, text=True, stdout=subprocess.PIPE
    ).stdout.strip()
    require(head == publisher_sha, "publisher checkout does not match the workflow source SHA")
    ancestor = subprocess.run(
        ["git", "merge-base", "--is-ancestor", candidate_source_sha, publisher_sha],
        cwd=project_root,
        check=False,
        stdout=subprocess.DEVNULL,
        stderr=subprocess.DEVNULL,
    )
    require(ancestor.returncode == 0, "candidate source is not an ancestor of publisher main")
    changed = subprocess.run(
        ["git", "diff", "--name-only", candidate_source_sha, publisher_sha],
        cwd=project_root,
        check=True,
        text=True,
        stdout=subprocess.PIPE,
    ).stdout.splitlines()
    require(all(is_allowed_post_candidate_doc(path) for path in changed),
            "publisher main contains executable, build, signing, or tooling changes after the candidate source")
    profile.validate_version(project_root)


def github_api_from_environment() -> stable.GitHubApi:
    return stable.GitHubApi(
        os.environ.get("GITHUB_REPOSITORY", ""),
        os.environ.get("GH_TOKEN", ""),
        os.environ.get("GITHUB_API_URL", ""),
    )


def validate_candidate_run_and_artifact(
    api: stable.GitHubApi,
    *,
    run_id: int,
    artifact_id: int,
    artifact_digest: str,
    source_sha: str,
    publisher_sha: str,
) -> tuple[dict[str, Any], dict[str, Any], dict[str, Any]]:
    require(run_id > 0 and artifact_id > 0, "candidate run and artifact IDs must be positive")
    require(re.fullmatch(r"[0-9a-f]{40}", source_sha) is not None, "candidate source SHA is malformed")
    digest = normalize_digest(artifact_digest)
    workflow = api.json("GET", api.repo_path + "/actions/workflows/github-release.yml")
    require(workflow.get("path") == profile.CANDIDATE_WORKFLOW_PATH and workflow.get("state") == "active",
            "signed candidate workflow path or state is not trusted")
    run = api.json("GET", api.repo_path + f"/actions/runs/{run_id}")
    require(run.get("id") == run_id and run.get("workflow_id") == workflow.get("id"),
            "candidate run does not belong to the trusted candidate workflow")
    require(run.get("status") == "completed" and run.get("conclusion") == "success" and
            run.get("run_attempt") == 1,
            "candidate run did not succeed on its first attempt")
    require(run.get("event") == "workflow_dispatch" and run.get("head_branch") == "main" and
            run.get("head_sha") == source_sha,
            "candidate run was not manually dispatched from the exact main source")
    require((run.get("head_repository") or {}).get("full_name", "").casefold() ==
            profile.REPOSITORY.casefold(), "candidate run belongs to another repository")
    branch = api.json("GET", api.repo_path + "/branches/main")
    require((branch.get("commit") or {}).get("sha") == publisher_sha,
            "publisher main moved during beta verification")
    artifact = api.json("GET", api.repo_path + f"/actions/artifacts/{artifact_id}")
    require(artifact.get("id") == artifact_id and artifact.get("expired") is False,
            "candidate artifact ID is wrong or the artifact expired")
    require(artifact.get("name") == profile.expected_candidate_artifact_name(source_sha),
            "candidate artifact name does not match the exact beta/source tuple")
    require(artifact.get("digest") == f"sha256:{digest}",
            "GitHub artifact digest differs from the explicitly approved digest")
    attached = artifact.get("workflow_run") or {}
    require(attached.get("id") == run_id and attached.get("head_sha") == source_sha and
            attached.get("head_branch") == "main",
            "candidate artifact is not attached to the approved main run")
    return workflow, run, artifact


def read_candidate_archive(api: stable.GitHubApi, artifact_id: int, expected_digest: str) -> bytes:
    path = api.repo_path + f"/actions/artifacts/{artifact_id}/zip"
    _, data, _ = api.request_bytes(api.api_url + path, max_bytes=MAX_ARCHIVE_BYTES)
    require(sha256_hex(data) == normalize_digest(expected_digest),
            "downloaded candidate archive does not match the approved digest")
    return data


def validate_candidate_archive(
    archive_bytes: bytes,
    *,
    project_root: Path,
    source_sha: str,
    run_id: int,
    artifact_id: int,
    artifact_digest: str,
) -> dict[str, Any]:
    require(len(archive_bytes) <= MAX_ARCHIVE_BYTES, "candidate archive exceeds the size limit")
    try:
        archive = zipfile.ZipFile(io.BytesIO(archive_bytes), "r")
    except (zipfile.BadZipFile, OSError) as exc:
        raise stable.PromotionError("candidate artifact is not a valid ZIP archive") from exc
    with archive:
        infos = archive.infolist()
        names = [info.filename for info in infos]
        require(len(names) == len(set(names)), "candidate archive contains duplicate names")
        required = candidate_asset_names()
        require(set(names) in (required, required | optional_asset_names()),
                "candidate archive file set does not match the v0.9 beta contract")
        files = {info.filename: stable._bounded_member(archive, info) for info in infos}
    try:
        manifest = json.loads(
            files["candidate-manifest.json"].decode("utf-8", errors="strict"),
            object_pairs_hook=stable._unique_json_object,
        )
    except (UnicodeDecodeError, json.JSONDecodeError) as exc:
        raise stable.PromotionError("candidate manifest is not valid UTF-8 JSON") from exc
    require(isinstance(manifest, dict), "candidate manifest must be a JSON object")
    pinned_signer = stable.read_pinned_signer(project_root)
    record = profile.candidate_record_from_manifest(
        manifest,
        run_id=run_id,
        artifact_id=artifact_id,
        artifact_name=profile.expected_candidate_artifact_name(source_sha),
        artifact_digest=normalize_digest(artifact_digest),
        pinned_signer_sha256=pinned_signer,
        expected_source_sha=source_sha,
    )
    apk_name = f"EQ-Library-{profile.BETA_TAG}.apk"
    require(manifest.get("apk") == apk_name, "candidate manifest APK filename is wrong")
    apk_sha = sha256_hex(files[apk_name])
    require(apk_sha == record.apk_sha256, "candidate APK bytes differ from the approved APK hash")
    mapping = files["r8-mapping.txt"]
    require(manifest.get("r8MinificationEnabled") is True and
            manifest.get("r8MappingSha256") == sha256_hex(mapping),
            "candidate R8 evidence is missing or has changed")
    stable.validate_r8_mapping(mapping)
    checksum = files[f"{apk_name}.sha256"].decode("ascii", errors="strict").strip()
    require(checksum in {f"{apk_sha}  {apk_name}", f"{apk_sha}  dist/{apk_name}"},
            "candidate checksum sidecar does not bind the APK bytes")
    stable.validate_saved_signer_report(files["apksigner-verification.txt"], pinned_signer)
    stable.validate_saved_alignment_report(files["zipalign-verification.txt"])
    return {"files": files, "manifest": manifest, "record": record, "apk_name": apk_name,
            "apk_sha256": apk_sha, "signer_sha256": pinned_signer}


def verify_android(candidate: dict[str, Any], build_tools: Path) -> dict[str, bytes]:
    logs, actual_code = stable.verify_android_apk(
        candidate["files"][candidate["apk_name"]],
        build_tools=build_tools,
        signer_sha256=candidate["signer_sha256"],
        expected_version_name=profile.BETA_VERSION_NAME,
        expected_version_code=profile.BETA_VERSION_CODE,
    )
    require(actual_code == profile.BETA_VERSION_CODE, "candidate APK versionCode is not 15")
    return logs


def validate_stable_latest(api: stable.GitHubApi) -> dict[str, Any]:
    latest = api.json("GET", api.repo_path + "/releases/latest")
    profile.validate_latest_snapshot(latest)
    return latest


def release_notes(project_root: Path) -> bytes:
    path = project_root / "docs/releases/v0.9.0-beta.1.md"
    require(path.is_file(), "curated v0.9.0-beta.1 release notes are missing")
    notes = path.read_bytes()
    require(notes.startswith(b"# EQ Library v0.9.0-beta.1\n"),
            "v0.9.0-beta.1 release notes have an unexpected title")
    text = notes.decode("utf-8", errors="strict")
    require("v0.8.3" in text and "prerelease" in text.lower() and "latest" in text.lower(),
            "v0.9.0-beta.1 release notes omit stable-latest or prerelease policy")
    return notes


def load_qualification(project_root: Path, candidate: dict[str, Any]) -> dict[str, Any]:
    path = project_root / QUALIFICATION_PATH
    require(path.is_file(), "v0.9.0 beta qualification record is missing")
    try:
        record = json.loads(path.read_text(encoding="utf-8"), object_pairs_hook=stable._unique_json_object)
    except (OSError, UnicodeDecodeError, json.JSONDecodeError) as exc:
        raise stable.PromotionError("v0.9.0 beta qualification record is not valid UTF-8 JSON") from exc
    require(isinstance(record, dict), "v0.9.0 beta qualification record must be a JSON object")
    require(record.get("schemaVersion") == 1 and record.get("releaseTag") == profile.BETA_TAG,
            "qualification record schema or release tag is wrong")
    seal = record.get("planSeal")
    require(isinstance(seal, dict), "qualification record planSeal is malformed")
    plan = project_root / "docs/releases/v0.9.0/PLAN.md"
    seal_file = project_root / "docs/releases/v0.9.0/SEAL.sha256"
    require(plan.is_file() and seal_file.is_file(), "v0.9.0 plan or seal is missing")
    require(sha256_hex(plan.read_bytes()) == profile.PLAN_SHA256,
            "current PLAN.md no longer matches the pinned v0.9 plan")
    require(sha256_hex(seal_file.read_bytes()) == profile.SEAL_MANIFEST_SHA256,
            "current SEAL.sha256 no longer matches the pinned v0.9 seal")
    require(seal.get("planSha256") == profile.PLAN_SHA256,
            "qualification record plan SHA-256 does not match the pinned v0.9 plan")
    require(seal.get("sealManifestSha256") == profile.SEAL_MANIFEST_SHA256,
            "qualification record seal-manifest SHA-256 does not match the pinned v0.9 seal")
    require(seal.get("baselineCommit") == profile.BASELINE_COMMIT,
            "qualification record baseline commit is wrong")
    expected_candidate = {
        "sourceSha": candidate["source_sha"],
        "runId": candidate["record"].run_id,
        "artifactId": candidate["record"].artifact_id,
        "artifactArchiveSha256": candidate["artifact_digest"],
        "apkSha256": candidate["record"].apk_sha256,
        "signerSha256": candidate["record"].signer_sha256,
    }
    require(record.get("candidate") == expected_candidate,
            "qualification record candidate tuple differs from the verified signed candidate")
    require(record.get("qualificationStatus") == "PASS" and record.get("readiness") == "READY FOR BETA",
            "qualification record is not PASS / READY FOR BETA")
    for field in ("computerQualification", "hardwareQualification", "restoration"):
        result = record.get(field)
        require(isinstance(result, dict) and result.get("status") == "PASS",
                f"qualification record {field} is not PASS")
        refs = result.get("evidenceRefs")
        require(isinstance(refs, list) and refs and
                all(isinstance(ref, str) and ref.strip() for ref in refs),
                f"qualification record {field} lacks evidence references")
    return record


def verified_candidate(
    api: stable.GitHubApi,
    *,
    project_root: Path,
    build_tools: Path,
    run_id: int,
    artifact_id: int,
    artifact_digest: str,
    candidate_source_sha: str,
) -> dict[str, Any]:
    publisher_sha = os.environ.get("GITHUB_SHA", "")
    validate_checkout(project_root, publisher_sha, candidate_source_sha)
    workflow, run, artifact = validate_candidate_run_and_artifact(
        api,
        run_id=run_id,
        artifact_id=artifact_id,
        artifact_digest=artifact_digest,
        source_sha=candidate_source_sha,
        publisher_sha=publisher_sha,
    )
    archive = read_candidate_archive(api, artifact_id, artifact_digest)
    candidate = validate_candidate_archive(
        archive,
        project_root=project_root,
        source_sha=candidate_source_sha,
        run_id=run_id,
        artifact_id=artifact_id,
        artifact_digest=artifact_digest,
    )
    logs = verify_android(candidate, build_tools)
    validate_stable_latest(api)
    notes_bytes = release_notes(project_root)
    result = {
        **candidate,
        "source_sha": candidate_source_sha,
        "publisher_source_sha": publisher_sha,
        "workflow": workflow,
        "run": run,
        "artifact": artifact,
        "artifact_digest": normalize_digest(artifact_digest),
        "android_logs": logs,
        "notes_bytes": notes_bytes,
        "notes_sha": sha256_hex(notes_bytes),
    }
    result["qualification"] = load_qualification(project_root, result)
    return result


def tag_message(notes_sha: str, candidate: dict[str, Any]) -> str:
    record = candidate["record"]
    return (
        "OPRA EQ Library v0.9.0 beta release binding\n"
        f"Release-Tag: {profile.BETA_TAG}\n"
        f"Source-SHA: {candidate['source_sha']}\n"
        f"Candidate-Run-ID: {record.run_id}\n"
        f"Candidate-Artifact-ID: {record.artifact_id}\n"
        f"Candidate-Artifact-SHA256: {record.artifact_digest}\n"
        f"APK-SHA256: {record.apk_sha256}\n"
        f"Signer-SHA256: {record.signer_sha256}\n"
        f"Release-Notes-SHA256: {notes_sha}\n"
    )


def resolve_tag(api: stable.GitHubApi, message: str) -> str | None:
    ref = api.optional_json(api.repo_path + "/git/ref/tags/" + urllib.parse.quote(profile.BETA_TAG, safe=""))
    if ref is None:
        return None
    require(ref.get("ref") == f"refs/tags/{profile.BETA_TAG}", "tag ref has an unexpected name")
    obj = ref.get("object") or {}
    require(obj.get("type") == "tag", "v0.9 beta release tag must be annotated")
    tag = api.json("GET", api.repo_path + f"/git/tags/{obj.get('sha')}")
    require(tag.get("tag") == profile.BETA_TAG and tag.get("message") == message and
            (tag.get("object") or {}).get("type") == "commit",
            "annotated v0.9 beta tag does not bind the approved candidate record")
    return (tag.get("object") or {}).get("sha")


def ensure_tag(api: stable.GitHubApi, candidate: dict[str, Any], notes_sha: str) -> str:
    message = tag_message(notes_sha, candidate)
    source = candidate["source_sha"]
    existing = resolve_tag(api, message)
    if existing is not None:
        require(existing == source, "existing annotated tag resolves to another source")
        return source
    created = api.json("POST", api.repo_path + "/git/tags", {
        "tag": profile.BETA_TAG, "message": message, "object": source, "type": "commit",
    })
    tag_sha = created.get("sha")
    require(isinstance(tag_sha, str) and re.fullmatch(r"[0-9a-f]{40}", tag_sha) is not None,
            "GitHub did not return an annotated tag object")
    api.json("POST", api.repo_path + "/git/refs", {"ref": f"refs/tags/{profile.BETA_TAG}", "sha": tag_sha})
    require(resolve_tag(api, message) == source, "created beta tag does not resolve to the exact source")
    return source


def publication_assets(candidate: dict[str, Any], notes_sha: str, publisher_run_id: str) -> dict[str, bytes]:
    record = candidate["record"]
    apk_name = candidate["apk_name"]
    manifest = {
        "repository": profile.REPOSITORY,
        "releaseTag": profile.BETA_TAG,
        "releaseChannel": "beta",
        "prerelease": True,
        "makeLatest": False,
        "releaseSourceSha": candidate["source_sha"],
        "publisherSourceSha": candidate["publisher_source_sha"],
        "candidateRunId": record.run_id,
        "candidateArtifactId": record.artifact_id,
        "candidateArtifactSha256": record.artifact_digest,
        "apk": apk_name,
        "apkSha256": record.apk_sha256,
        "signerSha256": record.signer_sha256,
        "versionName": profile.BETA_VERSION_NAME,
        "versionCode": profile.BETA_VERSION_CODE,
        "publisherWorkflowRunId": publisher_run_id,
    }
    manifest_bytes = (json.dumps(manifest, indent=2, sort_keys=True) + "\n").encode("utf-8")
    provenance = {
        **manifest,
        "betaReleaseManifestSha256": sha256_hex(manifest_bytes),
        "releaseNotesSha256": notes_sha,
        "publisherWorkflow": PUBLISHER_WORKFLOW_PATH,
    }
    return {
        apk_name: candidate["files"][apk_name],
        f"{apk_name}.sha256": f"{record.apk_sha256}  {apk_name}\n".encode("ascii"),
        "beta-release-manifest.json": manifest_bytes,
        "release-provenance.json": (json.dumps(provenance, indent=2, sort_keys=True) + "\n").encode("utf-8"),
        "apksigner-verification.txt": candidate["android_logs"]["apksigner-verification.txt"],
        "zipalign-verification.txt": candidate["android_logs"]["zipalign-verification.txt"],
    }


def _parse_metadata(name: str, data: bytes) -> dict[str, Any]:
    try:
        record = json.loads(data.decode("utf-8", errors="strict"), object_pairs_hook=stable._unique_json_object)
    except (UnicodeDecodeError, json.JSONDecodeError) as exc:
        raise stable.PromotionError(f"existing {name} is not valid UTF-8 JSON") from exc
    require(isinstance(record, dict), f"existing {name} must be a JSON object")
    return record


def _validate_metadata_record(
    name: str,
    record: dict[str, Any],
    candidate: dict[str, Any],
    notes_sha: str,
    publisher_run_id: str,
) -> None:
    record_model = candidate["record"]
    common = {
        "repository": profile.REPOSITORY,
        "releaseTag": profile.BETA_TAG,
        "releaseChannel": "beta",
        "prerelease": True,
        "makeLatest": False,
        "releaseSourceSha": candidate["source_sha"],
        "publisherSourceSha": candidate["publisher_source_sha"],
        "candidateRunId": record_model.run_id,
        "candidateArtifactId": record_model.artifact_id,
        "candidateArtifactSha256": record_model.artifact_digest,
        "apk": candidate["apk_name"],
        "apkSha256": record_model.apk_sha256,
        "signerSha256": record_model.signer_sha256,
        "versionName": profile.BETA_VERSION_NAME,
        "versionCode": profile.BETA_VERSION_CODE,
        "publisherWorkflowRunId": publisher_run_id,
    }
    expected = dict(common)
    if name == "release-provenance.json":
        manifest_bytes = publication_assets(candidate, notes_sha, publisher_run_id)["beta-release-manifest.json"]
        expected.update({
            "betaReleaseManifestSha256": sha256_hex(manifest_bytes),
            "releaseNotesSha256": notes_sha,
            "publisherWorkflow": PUBLISHER_WORKFLOW_PATH,
        })
    require(record == expected, f"existing {name} conflicts with the qualified candidate or publisher identity")


def _metadata_publisher_run_id(
    name: str,
    record: dict[str, Any],
    candidate: dict[str, Any],
    notes_sha: str,
) -> str:
    run_id = record.get("publisherWorkflowRunId")
    require(isinstance(run_id, str) and re.fullmatch(r"[1-9][0-9]*", run_id) is not None,
            f"existing {name} has an invalid publisher workflow run ID")
    _validate_metadata_record(name, record, candidate, notes_sha, run_id)
    return run_id


def check_release(release: dict[str, Any], notes: str, *, published: bool) -> None:
    require(release.get("tag_name") == profile.BETA_TAG and
            release.get("name") == "EQ Library v0.9.0-beta.1" and
            stable.normalize_release_body(release.get("body", "")) == stable.normalize_release_body(notes),
            "v0.9 beta release title, tag, or notes do not match")
    require(release.get("draft") is (not published) and release.get("prerelease") is True,
            "v0.9 beta release must remain a prerelease")
    if published:
        require(release.get("immutable") is True, "published v0.9 beta release is not immutable")


def _download_release_asset(api: stable.GitHubApi, asset: dict[str, Any]) -> bytes:
    asset_id = asset.get("id")
    require(isinstance(asset_id, int) and asset_id > 0, "release asset has no valid ID")
    _, data, _ = api.request_bytes(
        api.api_url + api.repo_path + f"/releases/assets/{asset_id}",
        accept="application/octet-stream",
        max_bytes=2 * 1024 * 1024,
    )
    return data


def _preserve_existing_metadata(
    api: stable.GitHubApi,
    release: dict[str, Any],
    expected: dict[str, bytes],
    candidate: dict[str, Any],
    notes_sha: str,
) -> dict[str, bytes]:
    by_name = {
        item.get("name"): item
        for item in release.get("assets", [])
        if isinstance(item, dict) and isinstance(item.get("name"), str)
    }
    manifest_bytes = _download_release_asset(api, by_name["beta-release-manifest.json"]) \
        if "beta-release-manifest.json" in by_name else None
    provenance_bytes = _download_release_asset(api, by_name["release-provenance.json"]) \
        if "release-provenance.json" in by_name else None
    manifest = _parse_metadata("beta-release-manifest.json", manifest_bytes) if manifest_bytes else None
    provenance = _parse_metadata("release-provenance.json", provenance_bytes) if provenance_bytes else None

    manifest_run_id = _metadata_publisher_run_id(
        "beta-release-manifest.json", manifest, candidate, notes_sha
    ) if manifest is not None else None
    provenance_run_id = _metadata_publisher_run_id(
        "release-provenance.json", provenance, candidate, notes_sha
    ) if provenance is not None else None
    if manifest_run_id is not None and provenance_run_id is not None:
        require(manifest_run_id == provenance_run_id,
                "existing beta metadata records disagree on publisher identity")
        require(provenance.get("betaReleaseManifestSha256") == sha256_hex(manifest_bytes),
                "existing release provenance does not bind the existing beta manifest bytes")
        expected["beta-release-manifest.json"] = manifest_bytes
        expected["release-provenance.json"] = provenance_bytes
        return expected

    selected_run_id = manifest_run_id or provenance_run_id
    if selected_run_id is not None:
        regenerated = publication_assets(candidate, notes_sha, selected_run_id)
        if manifest_bytes is not None:
            require(regenerated["beta-release-manifest.json"] == manifest_bytes,
                    "existing beta manifest conflicts with the qualified candidate")
            expected["beta-release-manifest.json"] = manifest_bytes
        if provenance_bytes is not None:
            require(regenerated["release-provenance.json"] == provenance_bytes,
                    "existing release provenance conflicts with the qualified candidate")
            expected["release-provenance.json"] = provenance_bytes
        if manifest_bytes is None:
            expected["beta-release-manifest.json"] = regenerated["beta-release-manifest.json"]
        if provenance_bytes is None:
            expected["release-provenance.json"] = regenerated["release-provenance.json"]
    return expected


def publish(
    api: stable.GitHubApi,
    *,
    project_root: Path,
    build_tools: Path,
    run_id: int,
    artifact_id: int,
    artifact_digest: str,
    candidate_source_sha: str,
) -> dict[str, Any]:
    candidate = verified_candidate(
        api, project_root=project_root, build_tools=build_tools,
        run_id=run_id, artifact_id=artifact_id, artifact_digest=artifact_digest,
        candidate_source_sha=candidate_source_sha,
    )
    notes_bytes = candidate["notes_bytes"]
    notes = notes_bytes.decode("utf-8")
    notes_sha = candidate["notes_sha"]
    publisher_run_id = os.environ.get("GITHUB_RUN_ID", "")
    require(re.fullmatch(r"[1-9][0-9]*", publisher_run_id) is not None,
            "publisher workflow run ID is invalid")
    ensure_tag(api, candidate, notes_sha)
    release = api.optional_json(api.repo_path + "/releases/tags/" + urllib.parse.quote(profile.BETA_TAG, safe=""))
    assets = publication_assets(candidate, notes_sha, publisher_run_id)
    if release is not None:
        assets = _preserve_existing_metadata(api, release, assets, candidate, notes_sha)
    if release is None:
        validate_stable_latest(api)
        release = api.json("POST", api.repo_path + "/releases", {
            "tag_name": profile.BETA_TAG,
            "name": "EQ Library v0.9.0-beta.1",
            "body": notes,
            "draft": True,
            "prerelease": True,
            "make_latest": "false",
            "generate_release_notes": False,
        })
        check_release(release, notes, published=False)
    else:
        check_release(release, notes, published=release.get("draft") is False)
        if release.get("draft") is False:
            _verify_final(api, release, candidate, notes, assets, build_tools)
            return release

    existing = stable._verify_release_assets(api, release, {
        name: data for name, data in assets.items()
        if name in {item.get("name") for item in release.get("assets", [])}
    }) if release.get("assets") else {}
    for name, data in assets.items():
        if name not in existing:
            stable._upload_asset(api, release, name, data)
    by_name = stable._verify_release_assets(api, release, assets)
    require(set(by_name) == release_asset_names(), "v0.9 beta draft asset set is incomplete")
    stable._verify_public_asset_downloads(api, by_name, assets)
    validate_stable_latest(api)
    published = api.json("PATCH", api.repo_path + f"/releases/{release['id']}", {
        "draft": False,
        "prerelease": True,
        "make_latest": "false",
    })
    check_release(published, notes, published=True)
    final = api.json("GET", api.repo_path + "/releases/tags/" + urllib.parse.quote(profile.BETA_TAG, safe=""))
    _verify_final(api, final, candidate, notes, assets, build_tools)
    return final


def _verify_final(api: stable.GitHubApi, release: dict[str, Any], candidate: dict[str, Any],
                  notes: str, assets: dict[str, bytes], build_tools: Path) -> None:
    check_release(release, notes, published=True)
    require(resolve_tag(api, tag_message(candidate["notes_sha"], candidate)) == candidate["source_sha"],
            "published beta tag no longer resolves to the candidate source")
    by_name = stable._verify_release_assets(api, release, assets)
    stable._verify_public_asset_downloads(api, by_name, assets)
    profile.validate_latest_snapshot(api.json("GET", api.repo_path + "/releases/latest"))


def check_contract(project_root: Path) -> None:
    profile.check_contract(project_root)
    workflow = (project_root / PUBLISHER_WORKFLOW_PATH).read_text(encoding="utf-8")
    for marker in (
        "tools/publish_beta_prerelease.py verify",
        "tools/publish_beta_prerelease.py publish",
        "tools/publish_v090_beta_release.py verify",
        "tools/publish_v090_beta_release.py publish",
        "candidate-source-sha",
        "refs/heads/main",
        "beta-release-approval",
        "PUBLISH_V0.9.0-BETA.1",
    ):
        require(marker in workflow, f"publisher workflow is missing v0.9 contract marker {marker!r}")
    for line in workflow.splitlines():
        if re.match(r"^\s+uses:\s+", line):
            require(re.search(r"@[0-9a-f]{40}(?:\s|$)", line) is not None,
                    "publisher workflow actions must be pinned to full commit SHAs")
    print("V090_BETA_PUBLISHER_CONTRACT_PASSED")


def build_parser() -> argparse.ArgumentParser:
    parser = argparse.ArgumentParser(description=__doc__)
    sub = parser.add_subparsers(dest="command", required=True)
    for command in ("verify", "publish"):
        item = sub.add_parser(command)
        item.add_argument("--project-root", type=Path, default=Path("."))
        item.add_argument("--build-tools", type=Path, required=True)
        item.add_argument("--candidate-run-id", required=True)
        item.add_argument("--candidate-artifact-id", required=True)
        item.add_argument("--candidate-artifact-digest", required=True)
        item.add_argument("--candidate-source-sha", required=True)
    contract = sub.add_parser("check-contract")
    contract.add_argument("--project-root", type=Path, default=Path("."))
    return parser


def main(argv: list[str] | None = None) -> int:
    args = build_parser().parse_args(argv)
    root = args.project_root.resolve()
    if args.command == "check-contract":
        check_contract(root)
        return 0
    api = github_api_from_environment()
    run_id = parse_positive(args.candidate_run_id)
    artifact_id = parse_positive(args.candidate_artifact_id)
    digest = normalize_digest(args.candidate_artifact_digest)
    if args.command == "verify":
        verified_candidate(
            api, project_root=root, build_tools=args.build_tools.resolve(),
            run_id=run_id, artifact_id=artifact_id, artifact_digest=digest,
            candidate_source_sha=args.candidate_source_sha,
        )
        print("V090_BETA_CANDIDATE_VERIFY_PASSED")
        return 0
    if args.command == "publish":
        release = publish(
            api, project_root=root, build_tools=args.build_tools.resolve(),
            run_id=run_id, artifact_id=artifact_id, artifact_digest=digest,
            candidate_source_sha=args.candidate_source_sha,
        )
        print(f"V090_BETA_PRERELEASE_PUBLISHED tag={profile.BETA_TAG} url={release.get('html_url', '')}")
        return 0
    raise AssertionError("argparse returned an unknown command")


if __name__ == "__main__":
    try:
        raise SystemExit(main())
    except stable.PromotionError as exc:
        print(f"V090_BETA_PUBLISHER_FAILED: {exc}", file=sys.stderr)
        raise SystemExit(1)
