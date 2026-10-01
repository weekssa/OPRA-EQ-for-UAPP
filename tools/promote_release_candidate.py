#!/usr/bin/env python3
"""Verify and promote an immutable signed release-candidate artifact.

This script runs only from the main-only promotion workflow. It never builds,
re-signs, or modifies the APK. The candidate artifact archive, APK, checksum,
manifest, signer, source commit, and uploaded public asset bytes are checked at
both the read-only verification stage and the publish stage.
"""

from __future__ import annotations

import argparse
import hashlib
import io
import json
import os
import re
import stat
import subprocess
import sys
import tempfile
import urllib.error
import urllib.parse
import urllib.request
import zipfile
from pathlib import Path
from typing import Any


REPOSITORY = "weekssa/OPRA-EQ-for-UAPP"
PACKAGE_ID = "com.weekssa.opraeqforuapp"
CANDIDATE_WORKFLOW_PATH = ".github/workflows/github-release.yml"
PROMOTION_WORKFLOW_PATH = ".github/workflows/promote-signed-release.yml"
MAX_ARCHIVE_BYTES = 64 * 1024 * 1024
MAX_MEMBER_BYTES = 96 * 1024 * 1024
MAX_TOTAL_UNCOMPRESSED_BYTES = 160 * 1024 * 1024
SHA256_RE = re.compile(r"^[0-9a-f]{64}$")
SOURCE_SHA_RE = re.compile(r"^[0-9a-f]{40}$")
TAG_RE = re.compile(r"^v(0|[1-9][0-9]*)\.(0|[1-9][0-9]*)\.(0|[1-9][0-9]*)$")


class PromotionError(RuntimeError):
    """A release candidate failed a required promotion assertion."""


def fail(message: str) -> None:
    raise PromotionError(message)


def require(condition: bool, message: str) -> None:
    if not condition:
        fail(message)


def parse_tag(tag: str) -> tuple[int, int, int]:
    match = TAG_RE.fullmatch(tag)
    require(match is not None, "release tag must use exact vMAJOR.MINOR.PATCH syntax")
    assert match is not None
    return tuple(int(part) for part in match.groups())


def normalize_sha256(value: str) -> str:
    normalized = value.strip().lower()
    if normalized.startswith("sha256:"):
        normalized = normalized[len("sha256:"):]
    return re.sub(r"[\s:]", "", normalized)


def sha256_hex(data: bytes) -> str:
    return hashlib.sha256(data).hexdigest()


def _unique_json_object(pairs: list[tuple[str, Any]]) -> dict[str, Any]:
    result: dict[str, Any] = {}
    for key, value in pairs:
        require(key not in result, "candidate manifest contains a duplicate JSON field")
        result[key] = value
    return result


def expected_asset_names(tag: str) -> set[str]:
    apk_name = f"EQ-Library-{tag}.apk"
    return {
        apk_name,
        f"{apk_name}.sha256",
        "candidate-manifest.json",
        "apksigner-verification.txt",
        "zipalign-verification.txt",
    }


def optional_candidate_sidecar_names(tag: str) -> set[str]:
    return {f"EQ-Library-{tag}.apk.idsig"}


def parse_app_version(project_root: Path) -> tuple[str, int]:
    gradle = (project_root / "app/build.gradle.kts").read_text(encoding="utf-8")
    name_match = re.search(r'^\s*versionName\s*=\s*"([^"\r\n]+)"', gradle, re.MULTILINE)
    code_match = re.search(r"^\s*versionCode\s*=\s*([0-9]+)\b", gradle, re.MULTILINE)
    require(name_match is not None and code_match is not None, "Android versionName/versionCode are missing")
    assert name_match is not None and code_match is not None
    return name_match.group(1), int(code_match.group(1))


def read_pinned_signer(project_root: Path) -> str:
    raw = (project_root / "release-signing-cert.sha256").read_text(encoding="utf-8")
    signer = normalize_sha256(raw)
    require(SHA256_RE.fullmatch(signer) is not None, "pinned release signer fingerprint is malformed")
    return signer


def _bounded_member(archive: zipfile.ZipFile, info: zipfile.ZipInfo) -> bytes:
    require(not info.is_dir(), "candidate archive must contain only top-level files")
    require(info.filename == Path(info.filename).name, "candidate archive contains a nested or unsafe path")
    require("/" not in info.filename and "\\" not in info.filename, "candidate archive contains a path separator")
    require(info.flag_bits & 0x1 == 0, "encrypted candidate archives are unsupported")
    mode = (info.external_attr >> 16) & 0o170000
    require(mode != stat.S_IFLNK, "candidate archive may not contain symbolic links")
    require(info.file_size <= MAX_MEMBER_BYTES, "candidate archive member exceeds the size limit")
    data = archive.read(info)
    require(len(data) == info.file_size, "candidate archive member size does not match its directory entry")
    return data


def validate_saved_signer_report(contents: bytes, pinned_signer: str) -> None:
    text = contents.decode("utf-8", errors="strict")
    signer_lines = re.findall(
        r"^Signer #1 certificate SHA-256 digest:\s*([0-9a-fA-F:]+)\s*$",
        text,
        flags=re.MULTILINE,
    )
    require(len(signer_lines) == 1, "candidate signer report must contain exactly one signer fingerprint")
    require(normalize_sha256(signer_lines[0]) == pinned_signer, "candidate signer report differs from the pinned signer")
    require(re.search(r"^Number of signers:\s*1\s*$", text, flags=re.MULTILINE) is not None,
            "candidate signer report does not prove a single signer")
    require(re.search(r"^Verified using v2 scheme \(APK Signature Scheme v2\): true\s*$", text,
                      flags=re.MULTILINE) is not None,
            "candidate signer report does not prove APK Signature Scheme v2")
    require(re.search(r"^Verified using v3 scheme \(APK Signature Scheme v3\): true\s*$", text,
                      flags=re.MULTILINE) is not None,
            "candidate signer report does not prove APK Signature Scheme v3")


def validate_saved_alignment_report(contents: bytes) -> None:
    text = contents.decode("utf-8", errors="strict")
    require("Verification successful" in text, "candidate alignment report does not prove zipalign success")


def validate_candidate_archive(
    archive_bytes: bytes,
    *,
    artifact_digest: str,
    tag: str,
    source_sha: str,
    project_root: Path,
) -> dict[str, Any]:
    require(len(archive_bytes) <= MAX_ARCHIVE_BYTES, "candidate artifact archive exceeds the size limit")
    require(SOURCE_SHA_RE.fullmatch(source_sha) is not None, "expected source commit SHA is malformed")
    version = parse_tag(tag)
    require(sha256_hex(archive_bytes) == normalize_sha256(artifact_digest),
            "downloaded candidate archive does not match the Actions artifact digest")

    try:
        archive = zipfile.ZipFile(io.BytesIO(archive_bytes), "r")
    except (zipfile.BadZipFile, OSError) as exc:
        raise PromotionError("candidate artifact is not a valid ZIP archive") from exc

    with archive:
        infos = archive.infolist()
        names = [info.filename for info in infos]
        require(len(names) == len(set(names)), "candidate archive contains duplicate member names")
        archive_names = set(names)
        required_names = expected_asset_names(tag)
        allowed_names = required_names | optional_candidate_sidecar_names(tag)
        require(archive_names in (required_names, allowed_names),
                "candidate archive file set does not match the signed-candidate contract")
        require(sum(info.file_size for info in infos) <= MAX_TOTAL_UNCOMPRESSED_BYTES,
                "candidate archive uncompressed size exceeds the limit")
        files = {info.filename: _bounded_member(archive, info) for info in infos}

    idsig_name = next(iter(optional_candidate_sidecar_names(tag)))
    if idsig_name in files:
        require(files[idsig_name], "candidate v4 signature sidecar is empty")

    manifest_bytes = files["candidate-manifest.json"]
    try:
        manifest = json.loads(manifest_bytes.decode("utf-8", errors="strict"), object_pairs_hook=_unique_json_object)
    except (UnicodeDecodeError, json.JSONDecodeError) as exc:
        raise PromotionError("candidate manifest is not valid UTF-8 JSON") from exc
    require(isinstance(manifest, dict), "candidate manifest must be a JSON object")

    version_name, version_code = parse_app_version(project_root)
    require(version_name == f"{version[0]}.{version[1]}.{version[2]}",
            "requested tag does not match the Android versionName")
    apk_name = f"EQ-Library-{tag}.apk"
    actual_apk_sha = sha256_hex(files[apk_name])
    pinned_signer = read_pinned_signer(project_root)

    required_manifest = {
        "sourceSha": source_sha,
        "releaseTag": tag,
        "apk": apk_name,
        "apkSha256": actual_apk_sha,
        "packageId": PACKAGE_ID,
        "versionName": version_name,
        "versionCode": version_code,
        "signerSha256": pinned_signer,
        "r8MinificationEnabled": True,
    }
    for field, expected in required_manifest.items():
        require(manifest.get(field) == expected, f"candidate manifest field {field} does not match the finalized source")
    mapping_digest = manifest.get("r8MappingSha256")
    require(isinstance(mapping_digest, str) and SHA256_RE.fullmatch(mapping_digest) is not None,
            "candidate manifest is missing a valid R8 mapping digest")

    checksum_text = files[f"{apk_name}.sha256"].decode("ascii", errors="strict").strip()
    checksum_match = re.fullmatch(r"([0-9a-f]{64})\s+\*?(.+)", checksum_text)
    require(checksum_match is not None, "candidate APK checksum file is malformed")
    assert checksum_match is not None
    require(checksum_match.group(1) == actual_apk_sha and
            Path(checksum_match.group(2)).name == apk_name and
            checksum_match.group(2) in {apk_name, f"dist/{apk_name}"},
            "candidate APK checksum file does not match the APK bytes")

    validate_saved_signer_report(files["apksigner-verification.txt"], pinned_signer)
    validate_saved_alignment_report(files["zipalign-verification.txt"])
    notes = project_root / "docs/releases" / f"{tag}.md"
    require(notes.is_file() and notes.read_text(encoding="utf-8").strip(),
            "curated release notes are missing or empty at the candidate source")

    return {
        "files": files,
        "manifest": manifest,
        "apk_name": apk_name,
        "apk_sha256": actual_apk_sha,
        "source_sha": source_sha,
        "release_tag": tag,
        "signer_sha256": pinned_signer,
        "version_code": version_code,
        "artifact_digest": normalize_sha256(artifact_digest),
    }


def verify_android_apk(apk_bytes: bytes, *, build_tools: Path, signer_sha256: str,
                       expected_version_name: str, expected_version_code: int | None = None) -> tuple[dict[str, bytes], int]:
    """Verify a signed APK's certificate, alignment, package, and version."""
    with tempfile.TemporaryDirectory(prefix="opra-release-promotion-") as temporary:
        apk_path = Path(temporary) / "candidate.apk"
        apk_path.write_bytes(apk_bytes)

        def run_tool(name: str, *args: str) -> str:
            executable = build_tools / name
            require(executable.is_file(), f"Android build tool is missing: {name}")
            result = subprocess.run(
                [str(executable), *args, str(apk_path)],
                check=False,
                text=True,
                stdout=subprocess.PIPE,
                stderr=subprocess.STDOUT,
                timeout=120,
            )
            require(result.returncode == 0, f"{name} rejected the candidate APK")
            return result.stdout

        signer_report = run_tool("apksigner", "verify", "--verbose", "--print-certs")
        validate_saved_signer_report(signer_report.encode("utf-8"), signer_sha256)

        alignment_report = run_tool("zipalign", "-c", "-v", "4")
        validate_saved_alignment_report(alignment_report.encode("utf-8"))

        badging = run_tool("aapt", "dump", "badging")
        package = re.search(
            r"^package: name='([^']+)' versionCode='([0-9]+)' versionName='([^']+)'",
            badging,
            flags=re.MULTILINE,
        )
        require(package is not None, "aapt did not report APK package and version")
        assert package is not None
        require(package.group(1) == PACKAGE_ID, "APK package does not match the public application ID")
        actual_version_code = int(package.group(2))
        require(actual_version_code > 0, "APK versionCode must be positive")
        if expected_version_code is not None:
            require(actual_version_code == expected_version_code,
                    "APK versionCode does not match the candidate manifest")
        require(package.group(3) == expected_version_name,
                "APK versionName does not match the expected release tag")

        return ({
            "apksigner-verification.txt": signer_report.encode("utf-8"),
            "zipalign-verification.txt": alignment_report.encode("utf-8"),
        }, actual_version_code)


def validate_android_tools(candidate: dict[str, Any], build_tools: Path) -> dict[str, bytes]:
    """Independently verify the archived APK using pinned Android build tools."""
    logs, _ = verify_android_apk(
        candidate["files"][candidate["apk_name"]],
        build_tools=build_tools,
        signer_sha256=candidate["signer_sha256"],
        expected_version_name=candidate["manifest"]["versionName"],
        expected_version_code=candidate["version_code"],
    )
    return logs


def download_and_verify_upgrade_baseline(api: "GitHubApi", candidate: dict[str, Any], build_tools: Path,
                                         output_path: Path) -> dict[str, str]:
    latest = api.json("GET", api.repo_path + "/releases/latest")
    baseline_tag = latest.get("tag_name", "")
    require(latest.get("draft") is False and latest.get("prerelease") is False,
            "latest public release is not a stable published release")
    require(TAG_RE.fullmatch(baseline_tag) is not None, "latest public release tag is not strict SemVer")
    require(parse_tag(baseline_tag) < parse_tag(candidate["release_tag"]),
            "candidate version does not advance the latest public release")

    apk_name = f"EQ-Library-{baseline_tag}.apk"
    assets = latest.get("assets")
    require(isinstance(assets, list), "latest public release asset list is malformed")
    matching = [asset for asset in assets if isinstance(asset, dict) and asset.get("name") == apk_name]
    require(len(matching) == 1, "latest public release does not contain exactly one expected APK asset")
    asset = matching[0]
    require(asset.get("state") == "uploaded", "latest public APK is not fully uploaded")
    digest = asset.get("digest", "")
    require(re.fullmatch(r"sha256:[0-9a-f]{64}", digest) is not None,
            "GitHub did not provide the latest public APK asset digest")
    asset_id = asset.get("id")
    require(isinstance(asset_id, int) and asset_id > 0, "latest public APK asset ID is invalid")
    url = api.api_url + api.repo_path + f"/releases/assets/{asset_id}"
    _, apk_bytes, _ = api.request_bytes(url, accept="application/octet-stream", max_bytes=MAX_MEMBER_BYTES)
    require(sha256_hex(apk_bytes) == normalize_sha256(digest),
            "downloaded previous-release APK does not match its GitHub asset digest")
    baseline_logs, baseline_version_code = verify_android_apk(
        apk_bytes,
        build_tools=build_tools,
        signer_sha256=candidate["signer_sha256"],
        expected_version_name=baseline_tag[1:],
    )
    require(baseline_version_code < candidate["version_code"],
            "latest public APK versionCode does not precede the release candidate")
    require("Verification successful" in baseline_logs["zipalign-verification.txt"].decode("utf-8"),
            "latest public APK did not pass alignment verification")
    output_path.parent.mkdir(parents=True, exist_ok=True)
    output_path.write_bytes(apk_bytes)
    return {"baseline_tag": baseline_tag, "baseline_version_code": str(baseline_version_code),
            "baseline_apk_sha256": sha256_hex(apk_bytes)}


class _NoCredentialRedirect(urllib.request.HTTPRedirectHandler):
    def redirect_request(self, req, fp, code, msg, headers, newurl):  # type: ignore[no-untyped-def]
        redirected = super().redirect_request(req, fp, code, msg, headers, newurl)
        if redirected is not None:
            old_host = urllib.parse.urlsplit(req.full_url).hostname
            new_host = urllib.parse.urlsplit(newurl).hostname
            if old_host != new_host:
                redirected.remove_header("Authorization")
                redirected.unredirected_hdrs.pop("Authorization", None)
                redirected.remove_header("Cookie")
                redirected.unredirected_hdrs.pop("Cookie", None)
        return redirected


class GitHubApi:
    def __init__(self, repository: str, token: str, api_url: str) -> None:
        require(repository == REPOSITORY, "promotion is restricted to the approved public repository")
        require(bool(token), "GitHub Actions token is unavailable")
        parsed = urllib.parse.urlsplit(api_url)
        require(parsed.scheme == "https" and parsed.hostname == "api.github.com",
                "only the configured public GitHub API host is supported")
        self.repository = repository
        self.token = token
        self.api_url = api_url.rstrip("/")
        self.opener = urllib.request.build_opener(_NoCredentialRedirect())

    @property
    def repo_path(self) -> str:
        owner, name = self.repository.split("/", 1)
        return f"/repos/{urllib.parse.quote(owner)}/{urllib.parse.quote(name)}"

    def request_bytes(self, url: str, *, method: str = "GET", body: bytes | None = None,
                      content_type: str | None = None, accept: str = "application/vnd.github+json",
                      max_bytes: int = MAX_ARCHIVE_BYTES) -> tuple[int, bytes, dict[str, str]]:
        parsed = urllib.parse.urlsplit(url)
        require(parsed.scheme == "https" and parsed.hostname in {"api.github.com", "uploads.github.com"},
                "GitHub API returned an unsupported request host")
        request = urllib.request.Request(url, data=body, method=method)
        request.add_unredirected_header("Authorization", f"Bearer {self.token}")
        request.add_header("Accept", accept)
        request.add_header("X-GitHub-Api-Version", "2022-11-28")
        request.add_header("User-Agent", "OPRA-EQ-for-UAPP-exact-artifact-publisher")
        if content_type:
            request.add_header("Content-Type", content_type)
        try:
            with self.opener.open(request, timeout=90) as response:
                data = response.read(max_bytes + 1)
                require(len(data) <= max_bytes, "GitHub API response exceeds the configured size limit")
                return response.status, data, dict(response.headers.items())
        except urllib.error.HTTPError as exc:
            body_preview = exc.read(512).decode("utf-8", errors="replace")
            raise PromotionError(f"GitHub API returned HTTP {exc.code}: {body_preview}") from exc
        except (urllib.error.URLError, TimeoutError) as exc:
            raise PromotionError("GitHub API request failed before a release mutation completed") from exc

    def json(self, method: str, path: str, payload: dict[str, Any] | None = None) -> dict[str, Any]:
        decoded = self.json_value(method, path, payload)
        require(isinstance(decoded, dict), "GitHub API returned an unexpected JSON response")
        return decoded

    def json_value(self, method: str, path: str, payload: dict[str, Any] | None = None) -> Any:
        body = None if payload is None else json.dumps(payload, separators=(",", ":")).encode("utf-8")
        url = self.api_url + path
        _, data, _ = self.request_bytes(url, method=method, body=body, content_type="application/json",
                                        max_bytes=2 * 1024 * 1024)
        try:
            decoded = json.loads(data.decode("utf-8", errors="strict"))
        except (UnicodeDecodeError, json.JSONDecodeError) as exc:
            raise PromotionError("GitHub API returned invalid JSON") from exc
        return decoded

    def optional_json(self, path: str) -> dict[str, Any] | None:
        try:
            return self.json("GET", path)
        except PromotionError as exc:
            if "HTTP 404" in str(exc):
                return None
            raise


def validate_github_candidate(api: GitHubApi, *, tag: str, run_id: int, artifact_id: int,
                              source_sha: str) -> tuple[dict[str, Any], dict[str, Any], str]:
    parse_tag(tag)
    require(run_id > 0 and artifact_id > 0, "candidate run and artifact IDs must be positive")
    require(SOURCE_SHA_RE.fullmatch(source_sha) is not None, "publisher source SHA is malformed")

    workflow = api.json("GET", api.repo_path + "/actions/workflows/github-release.yml")
    require(workflow.get("path") == CANDIDATE_WORKFLOW_PATH and workflow.get("state") == "active",
            "signed release candidate workflow path/state is not trusted")

    run = api.json("GET", api.repo_path + f"/actions/runs/{run_id}")
    require(run.get("id") == run_id, "candidate workflow run ID does not match the requested run")
    require(run.get("workflow_id") == workflow.get("id"), "candidate run was not produced by the signed release candidate workflow")
    require(run.get("status") == "completed" and run.get("conclusion") == "success",
            "candidate workflow run did not complete successfully")
    require(run.get("event") == "workflow_dispatch" and run.get("head_branch") == "main",
            "candidate run was not manually dispatched from main")
    require(run.get("head_sha") == source_sha, "candidate run source does not match the finalized main SHA")
    head_repository = run.get("head_repository") or {}
    require(head_repository.get("full_name", "").casefold() == REPOSITORY.casefold(),
            "candidate run did not execute in the canonical repository")

    branch = api.json("GET", api.repo_path + "/branches/main")
    commit = branch.get("commit") or {}
    require(commit.get("sha") == source_sha, "main moved after the signed candidate was built")

    artifact = api.json("GET", api.repo_path + f"/actions/artifacts/{artifact_id}")
    require(artifact.get("id") == artifact_id, "candidate artifact ID does not match the requested artifact")
    require(artifact.get("expired") is False, "candidate artifact has expired")
    require(int(artifact.get("size_in_bytes", 0)) > 0, "candidate artifact is empty")
    expected_name = f"EQ-Library-{tag}-signed-{source_sha}"
    require(artifact.get("name") == expected_name, "candidate artifact name does not match the signed-release workflow contract")
    artifact_run = artifact.get("workflow_run") or {}
    require(artifact_run.get("id") == run_id and artifact_run.get("head_sha") == source_sha and
            artifact_run.get("head_branch") == "main",
            "candidate artifact is not attached to the requested successful main run")
    digest = artifact.get("digest", "")
    require(re.fullmatch(r"sha256:[0-9a-f]{64}", digest) is not None,
            "GitHub did not provide the candidate artifact SHA-256 digest")
    return run, artifact, digest


def read_candidate_archive(api: GitHubApi, artifact_id: int, expected_digest: str) -> bytes:
    path = api.repo_path + f"/actions/artifacts/{artifact_id}/zip"
    _, data, _ = api.request_bytes(api.api_url + path, max_bytes=MAX_ARCHIVE_BYTES)
    require(sha256_hex(data) == normalize_sha256(expected_digest),
            "downloaded candidate ZIP bytes do not match GitHub's recorded artifact digest")
    return data


def current_main_sha(api: GitHubApi) -> str:
    branch = api.json("GET", api.repo_path + "/branches/main")
    sha = (branch.get("commit") or {}).get("sha", "")
    require(SOURCE_SHA_RE.fullmatch(sha) is not None, "GitHub main branch response has no valid commit SHA")
    return sha


def resolve_tag_commit(api: GitHubApi, tag: str) -> str | None:
    ref = api.optional_json(api.repo_path + "/git/ref/tags/" + urllib.parse.quote(tag, safe=""))
    if ref is None:
        return None
    obj = ref.get("object") or {}
    for _ in range(3):
        object_type = obj.get("type")
        object_sha = obj.get("sha", "")
        require(SOURCE_SHA_RE.fullmatch(object_sha) is not None, "release tag points to an invalid Git object SHA")
        if object_type == "commit":
            return object_sha
        require(object_type == "tag", "release tag does not resolve to a commit")
        annotated = api.json("GET", api.repo_path + f"/git/tags/{object_sha}")
        obj = annotated.get("object") or {}
    fail("release tag is nested too deeply to resolve safely")
    return None


def provenance_bytes(candidate: dict[str, Any], run_id: int, artifact_id: int, publisher_run_id: str) -> bytes:
    data = {
        "repository": REPOSITORY,
        "releaseTag": candidate["release_tag"],
        "releaseSourceSha": candidate["source_sha"],
        "candidateWorkflow": CANDIDATE_WORKFLOW_PATH,
        "candidateRunId": run_id,
        "candidateArtifactId": artifact_id,
        "candidateArtifactSha256": candidate["artifact_digest"],
        "apk": candidate["apk_name"],
        "apkSha256": candidate["apk_sha256"],
        "signerSha256": candidate["signer_sha256"],
        "promotionWorkflow": PROMOTION_WORKFLOW_PATH,
        "promotionWorkflowRunId": publisher_run_id,
    }
    return (json.dumps(data, indent=2, sort_keys=True) + "\n").encode("utf-8")


def release_assets(candidate: dict[str, Any], android_logs: dict[str, bytes], run_id: int,
                   artifact_id: int, publisher_run_id: str) -> dict[str, bytes]:
    files = candidate["files"]
    names = expected_asset_names(candidate["release_tag"])
    assets = {name: files[name] for name in names}
    assets[f"{candidate['apk_name']}.sha256"] = (
        f"{candidate['apk_sha256']}  {candidate['apk_name']}\n"
    ).encode("ascii")
    assets["apksigner-verification.txt"] = android_logs["apksigner-verification.txt"]
    assets["zipalign-verification.txt"] = android_logs["zipalign-verification.txt"]
    assets["release-provenance.json"] = provenance_bytes(candidate, run_id, artifact_id, publisher_run_id)
    return assets


def asset_digest(data: bytes) -> str:
    return "sha256:" + sha256_hex(data)


def _asset_path(api: GitHubApi, release: dict[str, Any]) -> str:
    url = release.get("assets_url", "")
    parsed = urllib.parse.urlsplit(url)
    require(parsed.scheme == "https" and parsed.hostname == "api.github.com" and
            parsed.path.startswith(api.repo_path + "/releases/"),
            "GitHub release returned an unsupported asset-list URL")
    return parsed.path + "?per_page=100"


def _upload_asset(api: GitHubApi, release: dict[str, Any], name: str, data: bytes) -> dict[str, Any]:
    upload_url = release.get("upload_url", "").split("{", 1)[0]
    parsed = urllib.parse.urlsplit(upload_url)
    require(parsed.scheme == "https" and parsed.hostname == "uploads.github.com" and
            parsed.path.startswith(api.repo_path + "/releases/"),
            "GitHub release returned an unsupported asset upload URL")
    content_type = "application/vnd.android.package-archive" if name.endswith(".apk") else (
        "application/json" if name.endswith(".json") else "text/plain; charset=utf-8")
    url = upload_url + "?" + urllib.parse.urlencode({"name": name})
    _, response, _ = api.request_bytes(url, method="POST", body=data, content_type=content_type,
                                       max_bytes=2 * 1024 * 1024)
    try:
        asset = json.loads(response.decode("utf-8", errors="strict"))
    except (UnicodeDecodeError, json.JSONDecodeError) as exc:
        raise PromotionError("GitHub returned invalid JSON after uploading a release asset") from exc
    require(isinstance(asset, dict), "GitHub returned an unexpected release asset response")
    return asset


def _verify_release_assets(api: GitHubApi, release: dict[str, Any], expected: dict[str, bytes]) -> dict[str, dict[str, Any]]:
    assets = api.json_value("GET", _asset_path(api, release))
    require(isinstance(assets, list), "GitHub release asset list is malformed")
    by_name: dict[str, dict[str, Any]] = {}
    for asset in assets:
        require(isinstance(asset, dict), "GitHub release asset entry is malformed")
        name = asset.get("name")
        require(isinstance(name, str) and name not in by_name, "GitHub release has duplicate or invalid asset names")
        require(name in expected, f"unexpected public release asset: {name}")
        require(asset.get("state") == "uploaded", f"release asset {name} is not fully uploaded")
        require(asset.get("size") == len(expected[name]), f"release asset {name} has the wrong byte size")
        require(asset.get("digest") == asset_digest(expected[name]), f"release asset {name} digest does not match the verified bytes")
        by_name[name] = asset
    return by_name


def _verify_public_asset_downloads(api: GitHubApi, by_name: dict[str, dict[str, Any]], expected: dict[str, bytes]) -> None:
    for name, asset in by_name.items():
        asset_id = asset.get("id")
        require(isinstance(asset_id, int) and asset_id > 0, f"release asset {name} has no valid ID")
        url = api.api_url + api.repo_path + f"/releases/assets/{asset_id}"
        _, data, _ = api.request_bytes(url, accept="application/octet-stream", max_bytes=MAX_MEMBER_BYTES)
        require(data == expected[name], f"public download bytes for {name} differ from the verified release asset")


def _preserve_matching_provenance(api: GitHubApi, release: dict[str, Any], candidate: dict[str, Any],
                                  assets: dict[str, bytes]) -> None:
    """Keep the first verified promotion-run identity when resuming an uploaded draft."""
    existing = [item for item in release.get("assets", [])
                if isinstance(item, dict) and item.get("name") == "release-provenance.json"]
    require(len(existing) <= 1, "draft release has duplicate provenance assets")
    if not existing:
        return

    asset = existing[0]
    asset_id = asset.get("id")
    digest = asset.get("digest", "")
    require(asset.get("state") == "uploaded" and isinstance(asset_id, int) and asset_id > 0,
            "existing draft provenance asset is not fully uploaded")
    require(re.fullmatch(r"sha256:[0-9a-f]{64}", digest) is not None,
            "existing draft provenance asset has no verified digest")
    url = api.api_url + api.repo_path + f"/releases/assets/{asset_id}"
    _, data, _ = api.request_bytes(url, accept="application/octet-stream", max_bytes=2 * 1024 * 1024)
    require(sha256_hex(data) == normalize_sha256(digest),
            "existing draft provenance bytes do not match their GitHub asset digest")
    try:
        record = json.loads(data.decode("utf-8", errors="strict"), object_pairs_hook=_unique_json_object)
    except (UnicodeDecodeError, json.JSONDecodeError) as exc:
        raise PromotionError("existing draft provenance is not valid UTF-8 JSON") from exc

    try:
        current_record = json.loads(
            assets["release-provenance.json"].decode("utf-8", errors="strict"),
            object_pairs_hook=_unique_json_object,
        )
    except (UnicodeDecodeError, json.JSONDecodeError, KeyError) as exc:
        raise PromotionError("current draft provenance is not valid UTF-8 JSON") from exc
    require(isinstance(record, dict) and isinstance(current_record, dict),
            "draft provenance must use JSON objects")

    required_fields = {
        "repository": REPOSITORY,
        "releaseTag": candidate["release_tag"],
        "releaseSourceSha": candidate["source_sha"],
        "candidateWorkflow": CANDIDATE_WORKFLOW_PATH,
        "candidateRunId": current_record.get("candidateRunId"),
        "candidateArtifactId": current_record.get("candidateArtifactId"),
        "candidateArtifactSha256": candidate["artifact_digest"],
        "apk": candidate["apk_name"],
        "apkSha256": candidate["apk_sha256"],
        "signerSha256": candidate["signer_sha256"],
        "promotionWorkflow": PROMOTION_WORKFLOW_PATH,
    }
    schema = set(required_fields) | {"promotionWorkflowRunId"}
    require(set(record) == schema and set(current_record) == schema,
            "existing draft provenance has an unexpected schema")
    require(all(record.get(field) == value for field, value in required_fields.items()) and
            all(current_record.get(field) == value for field, value in required_fields.items()),
            "existing draft provenance does not match this exact signed candidate")
    require(isinstance(record.get("candidateRunId"), int) and record["candidateRunId"] > 0 and
            isinstance(record.get("candidateArtifactId"), int) and record["candidateArtifactId"] > 0 and
            isinstance(current_record.get("candidateRunId"), int) and current_record["candidateRunId"] > 0 and
            isinstance(current_record.get("candidateArtifactId"), int) and current_record["candidateArtifactId"] > 0 and
            isinstance(record.get("promotionWorkflowRunId"), str) and
            re.fullmatch(r"[1-9][0-9]*", record["promotionWorkflowRunId"]) is not None and
            isinstance(current_record.get("promotionWorkflowRunId"), str) and
            re.fullmatch(r"[1-9][0-9]*", current_record["promotionWorkflowRunId"]) is not None,
            "existing draft provenance contains invalid workflow or artifact IDs")
    assets["release-provenance.json"] = data


def _verify_or_create_draft(api: GitHubApi, candidate: dict[str, Any], release_notes: str) -> dict[str, Any]:
    tag = candidate["release_tag"]
    source_sha = candidate["source_sha"]
    tag_commit = resolve_tag_commit(api, tag)
    existing = api.optional_json(api.repo_path + "/releases/tags/" + urllib.parse.quote(tag, safe=""))
    require(tag_commit is None or tag_commit == source_sha,
            "release tag already exists at a different commit")
    if existing is not None:
        require(existing.get("draft") is True and existing.get("prerelease") is False and
                existing.get("tag_name") == tag and existing.get("body") == release_notes,
                "an existing public or mismatched release already uses this tag")
        require(tag_commit == source_sha, "existing draft release tag does not point to the exact candidate source")
        return existing

    require(tag_commit is None, "tag exists without a resumable draft release")
    require_release_version_advances(api, tag)

    require(current_main_sha(api) == source_sha, "main moved before public draft creation")
    created = api.json("POST", api.repo_path + "/releases", {
        "tag_name": tag,
        "target_commitish": source_sha,
        "name": f"EQ Library {tag}",
        "body": release_notes,
        "draft": True,
        "prerelease": False,
        "make_latest": "false",
        "generate_release_notes": False,
    })
    require(created.get("tag_name") == tag and created.get("draft") is True,
            "GitHub did not create the expected private draft release")
    require(resolve_tag_commit(api, tag) == source_sha, "created release tag does not point to the exact candidate source")
    return created


def require_release_version_advances(api: GitHubApi, tag: str) -> None:
    latest = api.optional_json(api.repo_path + "/releases/latest")
    if latest is not None:
        latest_tag = latest.get("tag_name", "")
        require(TAG_RE.fullmatch(latest_tag) is not None, "latest public release tag is not strict SemVer")
        require(parse_tag(tag) > parse_tag(latest_tag), "new release version does not advance the latest public release")


def publish_release(api: GitHubApi, *, candidate: dict[str, Any], assets: dict[str, bytes],
                    release_notes: str) -> dict[str, Any]:
    tag = candidate["release_tag"]
    release = _verify_or_create_draft(api, candidate, release_notes)
    require(release.get("tag_name") == tag and release.get("draft") is True,
            "release is not in the expected unpublished draft state")
    _preserve_matching_provenance(api, release, candidate, assets)

    existing_names = [item.get("name") for item in release.get("assets", [])]
    require(len(existing_names) == len(set(existing_names)), "draft release contains duplicate asset names")
    existing_expected = {name: assets[name] for name in existing_names if name in assets}
    existing_assets = _verify_release_assets(api, release, existing_expected) if existing_names else {}
    for name, data in assets.items():
        if name in existing_assets:
            continue
        _upload_asset(api, release, name, data)

    by_name = _verify_release_assets(api, release, assets)
    require(set(by_name) == set(assets), "release asset set is incomplete")
    require(resolve_tag_commit(api, tag) == candidate["source_sha"],
            "release tag changed before publication")
    require(current_main_sha(api) == candidate["source_sha"],
            "main moved during promotion; release remains an unpublished draft")
    require_release_version_advances(api, tag)

    published = api.json("PATCH", api.repo_path + f"/releases/{release['id']}", {
        "draft": False,
        "prerelease": False,
        "make_latest": "true",
    })
    require(published.get("draft") is False and published.get("tag_name") == tag,
            "GitHub did not publish the expected release")

    final = api.json("GET", api.repo_path + "/releases/tags/" + urllib.parse.quote(tag, safe=""))
    require(final.get("draft") is False and final.get("prerelease") is False and final.get("tag_name") == tag,
            "public release readback did not confirm the expected published state")
    require(resolve_tag_commit(api, tag) == candidate["source_sha"],
            "public release tag no longer resolves to the candidate source")
    final_assets = _verify_release_assets(api, final, assets)
    _verify_public_asset_downloads(api, final_assets, assets)

    latest = api.json("GET", api.repo_path + "/releases/latest")
    require(latest.get("tag_name") == tag, "the app's latest-release metadata endpoint does not resolve to the new release")
    require(isinstance(final.get("html_url"), str) and final["html_url"].startswith("https://github.com/"),
            "public release URL is missing")
    print(f"PUBLIC_RELEASE_PUBLISHED tag={tag} source={candidate['source_sha']} apk_sha256={candidate['apk_sha256']} url={final['html_url']}")
    return final


def write_outputs(path: Path, values: dict[str, str]) -> None:
    with path.open("a", encoding="utf-8") as output:
        for key, value in values.items():
            require("\n" not in value and "\r" not in value, "workflow output contains a line break")
            output.write(f"{key}={value}\n")


def _github_api_from_environment() -> GitHubApi:
    return GitHubApi(
        os.environ.get("GITHUB_REPOSITORY", ""),
        os.environ.get("GH_TOKEN", ""),
        os.environ.get("GITHUB_API_URL", "https://api.github.com"),
    )


def _verify_inputs(api: GitHubApi, args: argparse.Namespace, project_root: Path) -> tuple[dict[str, Any], dict[str, Any], str, bytes, dict[str, Any], dict[str, bytes]]:
    run, artifact, digest = validate_github_candidate(
        api,
        tag=args.tag,
        run_id=args.candidate_run_id,
        artifact_id=args.candidate_artifact_id,
        source_sha=args.source_sha,
    )
    archive_bytes = read_candidate_archive(api, args.candidate_artifact_id, digest)
    candidate = validate_candidate_archive(
        archive_bytes,
        artifact_digest=digest,
        tag=args.tag,
        source_sha=args.source_sha,
        project_root=project_root,
    )
    logs = validate_android_tools(candidate, Path(args.build_tools))
    return run, artifact, digest, archive_bytes, candidate, logs


def command_verify(args: argparse.Namespace) -> None:
    project_root = Path(args.project_root)
    check_contract(project_root)
    api = _github_api_from_environment()
    _, _, digest, archive_bytes, candidate, _ = _verify_inputs(api, args, project_root)
    archive_path = Path(args.archive_out)
    archive_path.parent.mkdir(parents=True, exist_ok=True)
    archive_path.write_bytes(archive_bytes)
    apk_path = Path(args.apk_out)
    apk_path.parent.mkdir(parents=True, exist_ok=True)
    apk_path.write_bytes(candidate["files"][candidate["apk_name"]])
    baseline = download_and_verify_upgrade_baseline(
        api, candidate, Path(args.build_tools), Path(args.baseline_apk_out)
    )
    write_outputs(Path(args.github_output), {
        "release_tag": candidate["release_tag"],
        "source_sha": candidate["source_sha"],
        "artifact_digest": digest,
        "candidate_run_id": str(args.candidate_run_id),
        "candidate_artifact_id": str(args.candidate_artifact_id),
        "candidate_version_name": candidate["manifest"]["versionName"],
        "candidate_version_code": str(candidate["version_code"]),
        **baseline,
    })
    print(f"UPGRADE_BASELINE_VERIFIED tag={baseline['baseline_tag']} version_code={baseline['baseline_version_code']} apk_sha256={baseline['baseline_apk_sha256']}")
    print(f"CANDIDATE_VERIFIED tag={candidate['release_tag']} source={candidate['source_sha']} apk_sha256={candidate['apk_sha256']} artifact_sha256={candidate['artifact_digest']}")


def command_publish(args: argparse.Namespace) -> None:
    project_root = Path(args.project_root)
    check_contract(project_root)
    api = _github_api_from_environment()
    _, artifact, digest = validate_github_candidate(
        api,
        tag=args.tag,
        run_id=args.candidate_run_id,
        artifact_id=args.candidate_artifact_id,
        source_sha=args.source_sha,
    )
    archive_bytes = Path(args.archive).read_bytes()
    require(sha256_hex(archive_bytes) == normalize_sha256(digest),
            "transferred candidate archive differs from the candidate artifact digest")
    require(digest == args.artifact_digest, "verification-job artifact digest differs from publisher input")
    candidate = validate_candidate_archive(
        archive_bytes,
        artifact_digest=digest,
        tag=args.tag,
        source_sha=args.source_sha,
        project_root=project_root,
    )
    logs = validate_android_tools(candidate, Path(args.build_tools))
    run_id_text = os.environ.get("GITHUB_RUN_ID", "")
    require(run_id_text.isdigit() and int(run_id_text) > 0, "promotion workflow run ID is missing")
    assets = release_assets(candidate, logs, args.candidate_run_id, args.candidate_artifact_id, run_id_text)
    notes_path = project_root / "docs/releases" / f"{args.tag}.md"
    release_notes = notes_path.read_text(encoding="utf-8")
    publish_release(api, candidate=candidate, assets=assets, release_notes=release_notes)


def check_contract(project_root: Path) -> None:
    workflow_path = project_root / PROMOTION_WORKFLOW_PATH
    workflow = workflow_path.read_text(encoding="utf-8")
    candidate_workflow = (project_root / CANDIDATE_WORKFLOW_PATH).read_text(encoding="utf-8")
    android_ci = (project_root / ".github/workflows/android-ci.yml").read_text(encoding="utf-8")
    signing = (project_root / "docs/RELEASE_SIGNING.md").read_text(encoding="utf-8")
    checklist = (project_root / "docs/PUBLIC_RELEASE_CHECKLIST.md").read_text(encoding="utf-8")
    publisher = (project_root / "tools/promote_release_candidate.py").read_text(encoding="utf-8")
    required = [
        "workflow_dispatch:",
        "candidate_run_id:",
        "candidate_artifact_id:",
        "refs/heads/main",
        "permissions:\n      actions: read\n      contents: read",
        "permissions:\n      actions: read\n      contents: write",
        "actions/download-artifact@3e5f45b2cfb9172054b4087a40e8e0b5a5461e7c",
        "tools/promote_release_candidate.py verify",
        "tools/promote_release_candidate.py publish",
        "persist-credentials: false",
        "--apk-out",
        "--baseline-apk-out",
        'export ANDROID_AVD_HOME="$avd_home"',
        "Install exact candidate over the latest public release on API 35",
        "adb install -r",
    ]
    for marker in required:
        require(marker in workflow, f"promotion workflow contract is missing {marker!r}")
    upgrade_step = workflow.split(
        "- name: Install exact candidate over the latest public release on API 35\n", 1
    )[1].split("\n      - name: Upload promotion emulator diagnostics", 1)[0]
    require("${{ needs.verify-candidate.outputs." not in upgrade_step,
            "same-job emulator checks must not read verify-candidate through needs")
    for output in ("baseline_version_code", "candidate_version_code", "candidate_version_name"):
        require(f"${{{{ steps.verify.outputs.{output} }}}}" in upgrade_step,
                f"API 35 emulator check does not consume verify-step output {output!r}")
    require('"/releases/latest"' in publisher and '"make_latest": "true"' in publisher and
            '"release-provenance.json"' in publisher,
            "publisher does not preserve its latest metadata and provenance contract")
    for line in workflow.splitlines():
        if re.match(r"^\s+uses:\s+", line):
            require(re.search(r"@[0-9a-f]{40}(?:\s|$)", line) is not None,
                    "promotion workflow must pin every action to a full commit SHA")
    top_level_permissions = workflow.split("jobs:", 1)[0]
    require("contents: write" not in top_level_permissions and "actions: write" not in top_level_permissions,
            "write permissions must remain limited to the job that requires them")
    for marker in (
        "github.repository == 'weekssa/OPRA-EQ-for-UAPP' && github.ref == 'refs/heads/main'",
        "gradle :app:testDebugUnitTest :app:lintDebug :app:assembleRelease",
        "uses: actions/upload-artifact@ea165f8d65b6e75b540449e92b4886f43607fa02",
    ):
        require(marker in candidate_workflow, f"signed-candidate workflow contract is missing {marker!r}")
    for line in candidate_workflow.splitlines():
        if re.match(r"^\s+uses:\s+", line):
            require(re.search(r"@[0-9a-f]{40}(?:\s|$)", line) is not None,
                    "signed-candidate workflow must pin every action to a full commit SHA")
    require("test_promote_release_candidate.py" in android_ci and
            "tools/promote_release_candidate.py check-contract" in android_ci,
            "Android CI does not test the exact-candidate promotion contract")
    require("promote-signed-release.yml" in signing and "exact candidate APK bytes" in signing,
            "signing policy does not identify the exact-byte publisher")
    require("promote-signed-release.yml" in checklist and "owner approval" in checklist.lower(),
            "release checklist does not describe the publisher and existing owner authorization")
    require("exact-artifact promotion is not implemented" not in checklist.lower(),
            "release checklist still says exact-artifact promotion is unavailable")
    print("PROMOTION_WORKFLOW_CONTRACT_PASSED")


def _positive_int(value: str) -> int:
    require(re.fullmatch(r"[1-9][0-9]*", value) is not None, "workflow run/artifact ID must be a positive integer")
    return int(value)


def build_parser() -> argparse.ArgumentParser:
    parser = argparse.ArgumentParser(description=__doc__)
    subparsers = parser.add_subparsers(dest="command", required=True)
    subparsers.add_parser("check-contract", help="validate main-only workflow and release policy invariants")
    for command in ("verify", "publish"):
        sub = subparsers.add_parser(command)
        sub.add_argument("--tag", required=True)
        sub.add_argument("--candidate-run-id", required=True, type=_positive_int)
        sub.add_argument("--candidate-artifact-id", required=True, type=_positive_int)
        sub.add_argument("--source-sha", required=True)
        sub.add_argument("--build-tools", required=True)
        sub.add_argument("--project-root", default=".")
        if command == "verify":
            sub.add_argument("--archive-out", required=True)
            sub.add_argument("--apk-out", required=True)
            sub.add_argument("--baseline-apk-out", required=True)
            sub.add_argument("--github-output", required=True)
        else:
            sub.add_argument("--archive", required=True)
            sub.add_argument("--artifact-digest", required=True)
    return parser


def main(argv: list[str] | None = None) -> int:
    args = build_parser().parse_args(argv)
    try:
        if args.command == "check-contract":
            check_contract(Path("."))
        elif args.command == "verify":
            command_verify(args)
        elif args.command == "publish":
            command_publish(args)
        else:
            fail("unknown command")
    except (PromotionError, OSError, UnicodeError, subprocess.SubprocessError, zipfile.BadZipFile) as exc:
        print(f"RELEASE_PROMOTION_FAILED: {exc}", file=sys.stderr)
        return 1
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
