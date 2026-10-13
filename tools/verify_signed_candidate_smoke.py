#!/usr/bin/env python3
"""Verify a main-only signed candidate and prepare its API 35 smoke inputs.

This read-only verifier intentionally does not require that current main still
equals the signed candidate source. Promotion keeps its stricter current-main
guard. The caller checks out the exact candidate source so the trusted verifier
can compare the archive with that commit's version, signer, and release notes.
"""

from __future__ import annotations

import argparse
import importlib.util
import io
import json
import re
import sys
import zipfile
from pathlib import Path
from typing import Any


REPOSITORY = "weekssa/OPRA-EQ-for-UAPP"
CANDIDATE_WORKFLOW_PATH = ".github/workflows/github-release.yml"
PACKAGE_ID = "com.weekssa.opraeqforuapp"
BASELINE_TAG = "v0.8.1"
BASELINE_VERSION_CODE = 12
BASELINE_RELEASE_ID = 411100198
BASELINE_APK_ASSET_ID = 635375149
BASELINE_APK_SHA256 = "36b54063f6193eeb7e7b1ac0486b5094bf088a02442d8ea6d0a67d8b6acc8303"
SOURCE_MARKER = b"Signed candidate source: "


def require(condition: bool, message: str, error_type: type[Exception]) -> None:
    if not condition:
        raise error_type(message)


def validate_run_metadata(
    workflow: dict[str, Any],
    run: dict[str, Any],
    *,
    source_sha: str,
    run_id: int,
    error_type: type[Exception] = ValueError,
) -> None:
    require(workflow.get("path") == CANDIDATE_WORKFLOW_PATH and workflow.get("state") == "active",
            "official signed-candidate workflow path/state is not trusted", error_type)
    workflow_id = workflow.get("id")
    require(isinstance(workflow_id, int) and workflow_id > 0,
            "official signed-candidate workflow ID is missing or invalid", error_type)
    require(run.get("id") == run_id, "signed-candidate run ID mismatch", error_type)
    require(run.get("workflow_id") == workflow_id,
            "run was not produced by the official signed-candidate workflow", error_type)
    require(run.get("status") == "completed" and run.get("conclusion") == "success",
            "official signed-candidate run did not complete successfully", error_type)
    require(run.get("event") == "workflow_dispatch" and run.get("head_branch") == "main",
            "signed-candidate run was not manually dispatched from main", error_type)
    require(run.get("head_sha") == source_sha, "signed-candidate run source SHA mismatch", error_type)
    head_repository = run.get("head_repository") or {}
    require(isinstance(head_repository, dict) and
            head_repository.get("full_name", "").casefold() == REPOSITORY.casefold(),
            "signed-candidate run did not execute in the canonical repository", error_type)


def validate_artifact_metadata(
    artifact: dict[str, Any],
    *,
    tag: str,
    source_sha: str,
    run_id: int,
    artifact_id: int,
    error_type: type[Exception] = ValueError,
) -> str:
    require(artifact.get("id") == artifact_id, "candidate artifact ID mismatch", error_type)
    require(artifact.get("expired") is False and int(artifact.get("size_in_bytes", 0)) > 0,
            "candidate artifact is expired or empty", error_type)
    require(artifact.get("name") == f"EQ-Library-{tag}-signed-{source_sha}",
            "candidate artifact name does not match the signer contract", error_type)
    artifact_run = artifact.get("workflow_run") or {}
    require(isinstance(artifact_run, dict) and artifact_run.get("id") == run_id and
            artifact_run.get("head_sha") == source_sha and artifact_run.get("head_branch") == "main",
            "candidate artifact is not attached to the requested main run/source", error_type)
    digest = artifact.get("digest", "")
    require(isinstance(digest, str) and re.fullmatch(r"sha256:[0-9a-f]{64}", digest) is not None,
            "GitHub candidate artifact digest is missing or malformed", error_type)
    return digest


def validate_stable_baseline_release(release: dict[str, Any], error_type: type[Exception] = ValueError) -> None:
    require(release.get("id") == BASELINE_RELEASE_ID and release.get("tag_name") == BASELINE_TAG,
            "latest stable release is not the pinned v0.8.1 baseline", error_type)
    require(release.get("immutable") is True and release.get("draft") is False and
            release.get("prerelease") is False,
            "v0.8.1 baseline must be an immutable published stable release", error_type)
    assets = release.get("assets")
    require(isinstance(assets, list), "v0.8.1 baseline asset list is malformed", error_type)
    matching = [asset for asset in assets if isinstance(asset, dict) and
                asset.get("name") == f"EQ-Library-{BASELINE_TAG}.apk"]
    require(len(matching) == 1, "v0.8.1 baseline must contain exactly one expected APK", error_type)
    asset = matching[0]
    require(asset.get("id") == BASELINE_APK_ASSET_ID and asset.get("state") == "uploaded" and
            asset.get("digest") == f"sha256:{BASELINE_APK_SHA256}",
            "v0.8.1 baseline APK asset identity or digest differs from the pinned release", error_type)


def verify_embedded_source(apk_bytes: bytes, source_sha: str, error_type: type[Exception]) -> None:
    try:
        with zipfile.ZipFile(io.BytesIO(apk_bytes), "r") as apk:
            dex = apk.read("classes.dex")
    except (KeyError, OSError, zipfile.BadZipFile) as exc:
        raise error_type("signed candidate APK has no readable classes.dex") from exc
    require(SOURCE_MARKER + source_sha.encode("ascii") in dex,
            "signed APK does not embed the exact candidate source SHA", error_type)
    require(b"local-unqualified" not in dex,
            "signed APK contains the unqualified source fallback", error_type)


def verify_candidate_archive(
    promotion: Any,
    archive_bytes: bytes,
    *,
    artifact_digest: str,
    tag: str,
    source_sha: str,
    project_root: Path,
) -> dict[str, Any]:
    candidate = promotion.validate_candidate_archive(
        archive_bytes,
        artifact_digest=artifact_digest,
        tag=tag,
        source_sha=source_sha,
        project_root=project_root,
    )
    verify_embedded_source(candidate["files"][candidate["apk_name"]], source_sha, promotion.PromotionError)
    return candidate


def load_promotion_helpers():
    """Load the reviewed helper shipped with the validation workflow source."""
    module_path = Path(__file__).resolve().with_name("promote_release_candidate.py")
    spec = importlib.util.spec_from_file_location("candidate_promote_release_candidate", module_path)
    if spec is None or spec.loader is None:
        raise RuntimeError("could not load release verifier from exact candidate source")
    module = importlib.util.module_from_spec(spec)
    sys.modules[spec.name] = module
    spec.loader.exec_module(module)
    return module


def verify_and_prepare(args: argparse.Namespace) -> dict[str, Any]:
    candidate_root = Path(args.candidate_root).resolve()
    # This verifier comes from the separately pinned workflow checkout. Never
    # import or execute Python from candidate_root; it is not trusted until its
    # signer-run, artifact, source, and archive provenance has been checked.
    promotion = load_promotion_helpers()
    source_sha = args.candidate_source_sha
    tag = args.tag
    require(promotion.SOURCE_SHA_RE.fullmatch(source_sha) is not None,
            "candidate source SHA must be a full 40-character commit ID", promotion.PromotionError)
    promotion.parse_tag(tag)
    require(args.candidate_run_id > 0 and args.candidate_artifact_id > 0,
            "candidate run and artifact IDs must be positive", promotion.PromotionError)

    api = promotion.GitHubApi(args.repository, args.token, args.api_url)
    workflow = api.json("GET", api.repo_path + "/actions/workflows/github-release.yml")
    run = api.json("GET", api.repo_path + f"/actions/runs/{args.candidate_run_id}")
    validate_run_metadata(
        workflow, run, source_sha=source_sha, run_id=args.candidate_run_id,
        error_type=promotion.PromotionError,
    )
    artifact = api.json("GET", api.repo_path + f"/actions/artifacts/{args.candidate_artifact_id}")
    digest = validate_artifact_metadata(
        artifact, tag=tag, source_sha=source_sha, run_id=args.candidate_run_id,
        artifact_id=args.candidate_artifact_id, error_type=promotion.PromotionError,
    )
    archive = promotion.read_candidate_archive(api, args.candidate_artifact_id, digest)
    candidate = verify_candidate_archive(
        promotion,
        archive,
        artifact_digest=digest,
        tag=tag,
        source_sha=source_sha,
        project_root=candidate_root,
    )
    promotion.validate_android_tools(candidate, Path(args.build_tools))

    baseline_release = api.json("GET", api.repo_path + "/releases/latest")
    validate_stable_baseline_release(baseline_release, promotion.PromotionError)

    class PinnedBaselineApi:
        """Keep the existing APK verifier bound to the immutable release just checked."""

        repo_path = api.repo_path
        api_url = api.api_url

        def json(self, method, path, payload=None):
            if method == "GET" and path == self.repo_path + "/releases/latest":
                return baseline_release
            return api.json(method, path, payload)

        def request_bytes(self, url, **kwargs):
            return api.request_bytes(url, **kwargs)

    output_dir = Path(args.transfer_dir)
    output_dir.mkdir(parents=True, exist_ok=True)
    candidate_apk = output_dir / "signed-candidate.apk"
    candidate_apk.write_bytes(candidate["files"][candidate["apk_name"]])

    baseline_apk = output_dir / "stable-baseline.apk"
    baseline = promotion.download_and_verify_upgrade_baseline(
        PinnedBaselineApi(), candidate, Path(args.build_tools), baseline_apk,
    )
    require(baseline.get("baseline_tag") == BASELINE_TAG,
            f"latest stable baseline must be {BASELINE_TAG}", promotion.PromotionError)
    require(baseline.get("baseline_version_code") == str(BASELINE_VERSION_CODE),
            f"{BASELINE_TAG} baseline versionCode must be {BASELINE_VERSION_CODE}", promotion.PromotionError)
    require(baseline.get("baseline_apk_sha256") == BASELINE_APK_SHA256,
            "downloaded v0.8.1 APK differs from the pinned immutable baseline bytes", promotion.PromotionError)

    result = {
        "repository": args.repository,
        "candidateWorkflowPath": CANDIDATE_WORKFLOW_PATH,
        "candidateWorkflowId": workflow.get("id"),
        "candidateRunId": args.candidate_run_id,
        "candidateSourceSha": source_sha,
        "candidateArtifactId": args.candidate_artifact_id,
        "candidateArtifactDigest": digest.removeprefix("sha256:"),
        "candidateArtifactName": artifact["name"],
        "candidateApk": candidate["apk_name"],
        "candidateApkSha256": candidate["apk_sha256"],
        "candidateSignerSha256": candidate["signer_sha256"],
        "packageId": candidate["manifest"]["packageId"],
        "candidateVersionName": candidate["manifest"]["versionName"],
        "candidateVersionCode": candidate["version_code"],
        "baselineTag": baseline["baseline_tag"],
        "baselineApkSha256": baseline["baseline_apk_sha256"],
        "baselineVersionCode": int(baseline["baseline_version_code"]),
        "candidateApkPath": str(candidate_apk),
        "baselineApkPath": str(baseline_apk),
    }
    evidence_path = Path(args.evidence_dir) / "candidate-verification.json"
    evidence_path.parent.mkdir(parents=True, exist_ok=True)
    evidence_path.write_text(json.dumps(result, indent=2, sort_keys=True) + "\n", encoding="utf-8")
    print(json.dumps(result, sort_keys=True))
    return result


def build_parser() -> argparse.ArgumentParser:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--repository", default=REPOSITORY)
    parser.add_argument("--candidate-source-sha", required=True)
    parser.add_argument("--candidate-run-id", required=True, type=int)
    parser.add_argument("--candidate-artifact-id", required=True, type=int)
    parser.add_argument("--tag", required=True)
    parser.add_argument("--candidate-root", required=True)
    parser.add_argument("--build-tools", required=True)
    parser.add_argument("--transfer-dir", required=True)
    parser.add_argument("--evidence-dir", required=True)
    parser.add_argument("--token", default=None)
    parser.add_argument("--api-url", default="https://api.github.com")
    return parser


def main(argv: list[str] | None = None) -> int:
    parser = build_parser()
    args = parser.parse_args(argv)
    import os

    args.token = args.token or os.environ.get("GH_TOKEN", "")
    args.repository = os.environ.get("GITHUB_REPOSITORY", args.repository)
    args.api_url = os.environ.get("GITHUB_API_URL", args.api_url)
    try:
        verify_and_prepare(args)
    except Exception as exc:
        print(f"SIGNED_CANDIDATE_VERIFICATION_FAILED: {exc}", file=sys.stderr)
        return 1
    print("SIGNED_CANDIDATE_VERIFICATION_PASSED")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
