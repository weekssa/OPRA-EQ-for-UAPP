from __future__ import annotations

import copy
import hashlib
import io
import json
import os
import tempfile
import urllib.parse
import unittest
import zipfile
from contextlib import ExitStack
from dataclasses import replace
from pathlib import Path
from unittest.mock import patch

import publish_beta_prerelease as beta

SIGNER = beta.SIGNER_SHA256
APK = b"fixture signed beta APK bytes"
APK_SHA = hashlib.sha256(APK).hexdigest()
R8 = b"com.weekssa.opraeqforuapp.MainActivity -> a.b:\ncom.weekssa.opraeqforuapp.domain.catalog.Profile -> a.c:\n"
SIGNER_REPORT = (
    "Verifies\n"
    "Verified using v2 scheme (APK Signature Scheme v2): true\n"
    "Verified using v3 scheme (APK Signature Scheme v3): true\n"
    "Number of signers: 1\n"
    f"Signer #1 certificate SHA-256 digest: {SIGNER}\n"
).encode("utf-8")
ALIGNMENT = b"Verifying alignment of beta.apk (4)...\nVerification successful\n"


class FakeApi:
    repo_path = "/repos/weekssa/OPRA-EQ-for-UAPP"

    def __init__(self, responses):
        self.responses = responses

    def json(self, method, path, payload=None):
        key = (method, path)
        value = self.responses[key]
        if isinstance(value, Exception):
            raise value
        return copy.deepcopy(value)

    def optional_json(self, path):
        value = self.responses.get(("GET", path))
        return copy.deepcopy(value)


class FakePublishingApi:
    repo_path = "/repos/weekssa/OPRA-EQ-for-UAPP"
    api_url = "https://api.github.com"
    tag_object_sha = "d" * 40

    def __init__(self, pin, archive):
        self.pin = pin
        self.archive = archive
        self.calls = []
        self.tag_object = None
        self.tag_ref = None
        self.release = None
        self.latest_override = None
        self.assets_by_id = {}
        self.asset_bytes = {}
        self.next_asset_id = 800
        self.downloaded_assets = set()
        self.publisher_run = "111"
        self.main_sha = pin.source_sha
        self.main_checks = 0
        self.main_drift_at = None
        self.fail_before_upload_at = None
        self.fail_after_upload_at = None
        self.failed_upload = False
        self.upload_count = 0
        self.corrupt_download_name = None
        self.latest_beta_on_publish = False

    def _stable_latest(self):
        return {
            "tag_name": beta.LATEST_STABLE_TAG,
            "draft": False,
            "prerelease": False,
            "immutable": True,
            "assets": [{
                "name": "EQ-Library-v0.7.2.apk",
                "digest": f"sha256:{beta.LATEST_STABLE_APK_SHA256}",
            }],
        }

    def _release_response(self):
        return copy.deepcopy(self.release)

    def json(self, method, path, payload=None):
        self.calls.append((method, path, copy.deepcopy(payload)))
        if method == "GET":
            if path == self.repo_path + "/actions/workflows/signed-beta.yml":
                return {"id": beta.CANDIDATE_WORKFLOW_ID, "path": beta.CANDIDATE_WORKFLOW, "state": "active"}
            if path == self.repo_path + f"/actions/runs/{self.pin.run_id}":
                return {
                    "id": self.pin.run_id, "run_number": self.pin.run_number,
                    "workflow_id": beta.CANDIDATE_WORKFLOW_ID, "path": beta.CANDIDATE_WORKFLOW,
                    "status": "completed", "conclusion": "success", "run_attempt": 1,
                    "event": "workflow_dispatch", "head_branch": "main", "head_sha": self.pin.source_sha,
                    "head_repository": {"full_name": beta.REPOSITORY},
                }
            if path == self.repo_path + f"/actions/artifacts/{self.pin.artifact_id}":
                return {
                    "id": self.pin.artifact_id, "name": self.pin.artifact_name, "expired": False,
                    "size_in_bytes": len(self.archive), "digest": f"sha256:{self.pin.artifact_digest}",
                    "workflow_run": {"id": self.pin.run_id, "head_branch": "main", "head_sha": self.pin.source_sha},
                }
            if path == self.repo_path + "/branches/main":
                self.main_checks += 1
                sha = self.main_sha
                if self.main_drift_at is not None and self.main_checks >= self.main_drift_at:
                    sha = "e" * 40
                return {"commit": {"sha": sha}}
            if path == self.repo_path + "/releases/latest":
                return copy.deepcopy(self.latest_override or self._stable_latest())
            if path == self.repo_path + "/git/ref/tags/" + urllib.parse.quote(self.pin.tag, safe=""):
                if self.tag_ref is None:
                    raise beta.stable.PromotionError("GitHub API returned HTTP 404: ref missing")
                return copy.deepcopy(self.tag_ref)
            if path == self.repo_path + f"/git/tags/{self.tag_object_sha}":
                if self.tag_object is None:
                    raise beta.stable.PromotionError("GitHub API returned HTTP 404: tag object missing")
                return copy.deepcopy(self.tag_object)
            if path == self.repo_path + "/releases/tags/" + urllib.parse.quote(self.pin.tag, safe=""):
                if self.release is None:
                    raise beta.stable.PromotionError("GitHub API returned HTTP 404: release missing")
                return self._release_response()
            raise AssertionError(f"unexpected fake GET {path}")

        if method == "POST" and path == self.repo_path + "/git/tags":
            if self.tag_object is not None:
                raise AssertionError("publisher attempted to create a duplicate annotated tag")
            self.tag_object = {
                **copy.deepcopy(payload),
                "object": {"type": payload["type"], "sha": payload["object"]},
                "sha": self.tag_object_sha,
            }
            return copy.deepcopy(self.tag_object)
        if method == "POST" and path == self.repo_path + "/git/refs":
            if self.tag_ref is not None:
                raise AssertionError("publisher attempted to create a duplicate tag ref")
            self.tag_ref = {"ref": f"refs/tags/{self.pin.tag}", "object": {"type": "tag", "sha": payload["sha"]}}
            return copy.deepcopy(self.tag_ref)
        if method == "POST" and path == self.repo_path + "/releases":
            if self.release is not None:
                raise AssertionError("publisher attempted to create a duplicate release")
            self.release = {
                "id": 700,
                "tag_name": payload["tag_name"],
                "name": payload["name"],
                "body": payload["body"],
                "draft": payload["draft"],
                "prerelease": payload["prerelease"],
                "immutable": False,
                "upload_url": f"https://uploads.github.com{self.repo_path}/releases/700/assets{{?name,label}}",
                "assets_url": f"{self.api_url}{self.repo_path}/releases/700/assets",
                "html_url": f"https://github.com/{beta.REPOSITORY}/releases/tag/{self.pin.tag}",
                "assets": [],
            }
            return self._release_response()
        if method == "PATCH" and path == self.repo_path + "/releases/700":
            if self.release is None:
                raise AssertionError("publisher attempted to publish a missing release")
            self.release.update(copy.deepcopy(payload))
            if payload.get("draft") is False:
                self.release["immutable"] = True
                if self.latest_beta_on_publish:
                    self.latest_override = {
                        "tag_name": self.pin.tag, "draft": False, "prerelease": True,
                        "immutable": True, "assets": [],
                    }
            return self._release_response()
        raise AssertionError(f"unexpected fake mutation {method} {path}")

    def optional_json(self, path):
        try:
            return self.json("GET", path)
        except beta.stable.PromotionError as exc:
            if "HTTP 404" in str(exc):
                return None
            raise

    def json_value(self, method, path, payload=None):
        if method == "GET" and path.startswith(self.repo_path + "/releases/700/assets"):
            self.calls.append((method, path, copy.deepcopy(payload)))
            return copy.deepcopy(self.release["assets"] if self.release else [])
        return self.json(method, path, payload)

    def request_bytes(self, url, *, method="GET", body=None, content_type=None,
                      accept="application/vnd.github+json", max_bytes=64 * 1024 * 1024):
        parsed = urllib.parse.urlsplit(url)
        self.calls.append((method, parsed.path + ("?" + parsed.query if parsed.query else ""), None))
        if method == "GET" and parsed.path == self.repo_path + f"/actions/artifacts/{self.pin.artifact_id}/zip":
            return 200, self.archive, {}
        if method == "POST" and parsed.hostname == "uploads.github.com":
            name = urllib.parse.parse_qs(parsed.query).get("name", [None])[0]
            if self.release is None or not isinstance(name, str) or body is None:
                raise AssertionError("invalid fake release asset upload")
            self.upload_count += 1
            if self.fail_before_upload_at == self.upload_count and not self.failed_upload:
                self.failed_upload = True
                raise beta.stable.PromotionError("simulated upload failure before storing bytes")
            if name in self.asset_bytes:
                raise AssertionError(f"publisher attempted to upload duplicate asset {name}")
            asset_id = self.next_asset_id
            self.next_asset_id += 1
            self.asset_bytes[name] = body
            asset = {
                "id": asset_id, "name": name, "state": "uploaded", "size": len(body),
                "digest": beta.stable.asset_digest(body),
            }
            self.assets_by_id[asset_id] = name
            self.release["assets"].append(asset)
            if self.fail_after_upload_at == self.upload_count and not self.failed_upload:
                self.failed_upload = True
                raise beta.stable.PromotionError("simulated lost response after storing uploaded bytes")
            return 201, json.dumps(asset).encode("utf-8"), {}
        if method == "GET" and parsed.path.startswith(self.repo_path + "/releases/assets/"):
            asset_id = int(parsed.path.rsplit("/", 1)[1])
            name = self.assets_by_id[asset_id]
            self.downloaded_assets.add(name)
            data = self.asset_bytes[name]
            if self.corrupt_download_name == name:
                data = data + b"corruption"
            return 200, data, {}
        raise AssertionError(f"unexpected fake byte request {method} {url}")


def test_pin(root: Path):
    apk_name = "EQ-Library-v0.8.0-beta-test.apk"
    manifest = {
        "sourceSha": "a" * 40,
        "candidateTarget": "black-pearl-ja11",
        "apk": apk_name,
        "apkSha256": APK_SHA,
        "packageId": beta.PACKAGE_ID,
        "versionName": beta.VERSION_NAME,
        "versionCode": beta.VERSION_CODE,
        "signerSha256": SIGNER,
        "r8MinificationEnabled": True,
        "r8MappingSha256": hashlib.sha256(R8).hexdigest(),
        "capabilityProfile": "TRN Black Pearl final-native-readback-gated Direct Flash; FiiO JA11 exact model; five-band User 1 editor; global EQ gain",
        "testPlan": "docs/UX_REVIEW_WORKFLOW/context/black-pearl-ja11-remediation-20260927/05-release-handoff.md",
        "evidenceIds": ["BLACK-PEARL-SOFTWARE", "JA11-SOFTWARE"],
        "outstanding": "owner Pixel 9 TRN Black Pearl and FiiO JA11 physical validation; final public release/public-support approval",
    }
    manifest_bytes = (json.dumps(manifest, separators=(",", ":")) + "\n").encode("utf-8")
    pin = replace(beta.BETA, source_sha="a" * 40, artifact_digest="0" * 64,
                  artifact_name="test-artifact", artifact_id=100, run_id=200, run_number=1,
                  apk_name=apk_name, apk_sha256=APK_SHA,
                  manifest_sha256=hashlib.sha256(manifest_bytes).hexdigest(),
                  r8_mapping_sha256=hashlib.sha256(R8).hexdigest())
    files = {
        apk_name: APK,
        f"{apk_name}.sha256": f"{APK_SHA}  dist/{apk_name}\n".encode("ascii"),
        f"{apk_name}.idsig": b"signature sidecar",
        "candidate-manifest.json": manifest_bytes,
        "apksigner-verification.txt": SIGNER_REPORT,
        "zipalign-verification.txt": ALIGNMENT,
        "r8-mapping.txt": R8,
    }
    (root / "app").mkdir(parents=True)
    (root / "app/build.gradle.kts").write_text(
        'android { defaultConfig { versionCode = 10; versionName = "0.8.0-beta" } }\n', encoding="utf-8"
    )
    (root / "release-signing-cert.sha256").write_text(SIGNER + "\n", encoding="utf-8")
    notes = root / "docs/releases/v0.8.0-beta.md"
    notes.parent.mkdir(parents=True)
    notes.write_text(
        "# EQ Library v0.8.0 beta\n\n"
        f"Source {beta.SOURCE_SHA}; artifact {beta.CANDIDATE_ARTIFACT_DIGEST}; "
        f"APK {beta.APK_SHA256}; signer {beta.SIGNER_SHA256}.\n"
        "The signed v0.7.2 in-place upgrade and separate signed clean-install/core smoke both passed.\n"
        "The Black Pearl C05-C qualification was a read-only check. Class B remains. "
        "Stable latest remains v0.7.2.\n",
        encoding="utf-8",
    )
    return pin, files


def archive_bytes(files):
    stream = io.BytesIO()
    with zipfile.ZipFile(stream, "w", compression=zipfile.ZIP_DEFLATED) as archive:
        for name, data in files.items():
            archive.writestr(name, data)
    return stream.getvalue()


class BetaPublisherTest(unittest.TestCase):
    def setUp(self):
        self.temp = tempfile.TemporaryDirectory(prefix="beta-publisher-test-")
        self.root = Path(self.temp.name)
        self.pin, self.files = test_pin(self.root)
        self.archive = archive_bytes(self.files)
        self.digest = hashlib.sha256(self.archive).hexdigest()
        self.pin = replace(self.pin, artifact_digest=self.digest)

    def tearDown(self):
        self.temp.cleanup()

    def validate(self, files=None, pin=None, archive=None, digest=None):
        actual_pin = self.pin if pin is None else pin
        data = self.archive if archive is None else archive
        expected_digest = hashlib.sha256(data).hexdigest() if digest is None else digest
        return beta.validate_candidate_archive(
            data,
            artifact_digest=expected_digest,
            project_root=self.root,
            pin=actual_pin,
        )

    def publish_with_fake_api(self, api, *, run_id="111"):
        publisher_sha = self.pin.source_sha
        release_notes = (
            b"# EQ Library v0.8.0 beta\n\n"
            b"The signed v0.7.2 in-place upgrade and separate signed clean-install/core smoke both passed.\n"
            b"The Black Pearl C05-C qualification was a read-only check. Class B remains. "
            b"Stable latest remains v0.7.2.\n"
        )
        with ExitStack() as stack:
            stack.enter_context(patch.dict(os.environ, {
                "GITHUB_REPOSITORY": beta.REPOSITORY,
                "GITHUB_REF": "refs/heads/main",
                "GITHUB_SHA": publisher_sha,
                "GITHUB_RUN_ID": run_id,
                "GH_TOKEN": "test-token",
                "GITHUB_API_URL": "https://api.github.com",
            }))
            stack.enter_context(patch.object(beta, "validate_checkout"))
            stack.enter_context(patch.object(beta, "verify_android", return_value={}))
            stack.enter_context(patch.object(beta, "release_notes", return_value=release_notes))
            stack.enter_context(patch.object(beta.stable, "verify_android_apk",
                                             return_value=({}, self.pin.version_code)))
            return beta.publish(api, project_root=self.root, build_tools=Path("/fake/build-tools"), pin=self.pin)

    def test_accepts_exact_candidate_zip_and_returns_pinned_identity(self):
        candidate = self.validate()
        self.assertEqual(candidate["source_sha"], "a" * 40)
        self.assertEqual(candidate["apk_sha256"], APK_SHA)
        self.assertEqual(candidate["signer_sha256"], SIGNER)

    def test_rejects_artifact_zip_digest_mismatch(self):
        with self.assertRaisesRegex(beta.stable.PromotionError, "immutable Actions digest"):
            self.validate(digest="f" * 64)

    def test_rejects_unexpected_archive_member(self):
        files = dict(self.files)
        files["unexpected.txt"] = b"not approved"
        with self.assertRaisesRegex(beta.stable.PromotionError, "file set"):
            self.validate(archive=archive_bytes(files))

    def test_rejects_missing_apk_sidecar(self):
        files = dict(self.files)
        files.pop(f"{self.pin.apk_name}.idsig")
        with self.assertRaisesRegex(beta.stable.PromotionError, "file set"):
            self.validate(archive=archive_bytes(files))

    def test_rejects_candidate_manifest_digest_mismatch(self):
        files = dict(self.files)
        files["candidate-manifest.json"] += b" "
        with self.assertRaisesRegex(beta.stable.PromotionError, "manifest bytes"):
            self.validate(archive=archive_bytes(files))

    def test_rejects_apk_checksum_mismatch(self):
        files = dict(self.files)
        files[f"{self.pin.apk_name}.sha256"] = b"0" * 64 + b"  dist/file.apk\n"
        pin = replace(self.pin, manifest_sha256=hashlib.sha256(files["candidate-manifest.json"]).hexdigest())
        with self.assertRaisesRegex(beta.stable.PromotionError, "checksum file"):
            self.validate(pin=pin, archive=archive_bytes(files))

    def test_rejects_wrong_signer_report(self):
        files = dict(self.files)
        files["apksigner-verification.txt"] = SIGNER_REPORT.replace(SIGNER.encode(), b"0" * 64)
        with self.assertRaisesRegex(beta.stable.PromotionError, "signer report"):
            self.validate(archive=archive_bytes(files))

    def test_rejects_wrong_r8_mapping(self):
        files = dict(self.files)
        files["r8-mapping.txt"] = b"com.weekssa.opraeqforuapp.MainActivity -> MainActivity:\n"
        with self.assertRaisesRegex(beta.stable.PromotionError, "R8 mapping digest"):
            self.validate(archive=archive_bytes(files))

    def test_rejects_duplicate_manifest_json_key(self):
        files = dict(self.files)
        original = json.loads(files["candidate-manifest.json"])
        files["candidate-manifest.json"] = (
            json.dumps(original, separators=(",", ":"))[:-1] + ',"sourceSha":"b' + 'b' * 39 + '"}\n'
        ).encode()
        pin = replace(self.pin, manifest_sha256=hashlib.sha256(files["candidate-manifest.json"]).hexdigest())
        with self.assertRaisesRegex(beta.stable.PromotionError, "duplicate JSON field"):
            self.validate(pin=pin, archive=archive_bytes(files))

    def test_run_and_artifact_accept_exact_successful_official_tuple(self):
        api = FakeApi({
            ("GET", self._p("/actions/workflows/signed-beta.yml")): {
                "id": beta.CANDIDATE_WORKFLOW_ID, "path": beta.CANDIDATE_WORKFLOW, "state": "active"
            },
            ("GET", self._p(f"/actions/runs/{self.pin.run_id}")): self._run(),
            ("GET", self._p(f"/actions/artifacts/{self.pin.artifact_id}")): self._artifact(),
        })
        result = beta.validate_official_run_and_artifact(api, self.pin)
        self.assertEqual(result["artifact"]["id"], self.pin.artifact_id)

    def test_run_rejects_wrong_source_sha(self):
        run = self._run()
        run["head_sha"] = "c" * 40
        api = self._api(run=run)
        with self.assertRaisesRegex(beta.stable.PromotionError, "exact merged main source"):
            beta.validate_official_run_and_artifact(api, self.pin)

    def test_artifact_rejects_expired_or_wrong_run(self):
        artifact = self._artifact()
        artifact["expired"] = True
        api = self._api(artifact=artifact)
        with self.assertRaisesRegex(beta.stable.PromotionError, "expired"):
            beta.validate_official_run_and_artifact(api, self.pin)
        artifact = self._artifact()
        artifact["workflow_run"]["id"] = 999
        api = self._api(artifact=artifact)
        with self.assertRaisesRegex(beta.stable.PromotionError, "not attached"):
            beta.validate_official_run_and_artifact(api, self.pin)

    def test_candidate_tag_annotation_binds_exact_release_tuple(self):
        message = beta.candidate_tag_message("e" * 64, self.pin)
        self.assertIn(f"Source-SHA: {self.pin.source_sha}", message)
        self.assertIn(f"Candidate-Artifact-SHA256: {self.pin.artifact_digest}", message)
        self.assertIn(f"Release-Notes-SHA256: {'e' * 64}", message)

    def test_release_notes_require_the_completed_gate_facts(self):
        self.assertIn(b"Class B remains", beta.release_notes(self.root))
        notes = self.root / "docs/releases/v0.8.0-beta.md"
        original = notes.read_bytes()
        notes.write_bytes(original.replace(b"Class B remains", b"Class A now"))
        with self.assertRaisesRegex(beta.stable.PromotionError, "Class B remains"):
            beta.release_notes(self.root)

    def test_tag_resolver_rejects_lightweight_tag(self):
        api = FakeApi({("GET", self._p("/git/ref/tags/v0.8.0-beta")): {
            "object": {"type": "commit", "sha": self.pin.source_sha}
        }})
        with self.assertRaisesRegex(beta.stable.PromotionError, "lightweight tag"):
            beta.resolve_annotated_tag(api, "e" * 64, self.pin)

    def test_latest_stable_requires_v072_nonprerelease(self):
        latest = {"tag_name": "v0.7.2", "draft": False, "prerelease": False, "immutable": True,
                  "assets": [{"name": "EQ-Library-v0.7.2.apk", "digest": f"sha256:{beta.LATEST_STABLE_APK_SHA256}"}]}
        api = FakeApi({("GET", self._p("/releases/latest")): latest})
        self.assertEqual(beta.latest_stable(api)["tag_name"], "v0.7.2")
        latest["tag_name"] = "v0.8.0-beta"
        api = FakeApi({("GET", self._p("/releases/latest")): latest})
        with self.assertRaisesRegex(beta.stable.PromotionError, "not the expected v0.7.2"):
            beta.latest_stable(api)

    def test_public_assets_bind_beta_and_explicit_nonlatest_flags(self):
        candidate = self.validate()
        candidate.update(publisher_sha="b" * 40, publisher_run_id="1234")
        notes_sha = "c" * 64
        assets = beta.public_assets(candidate, self.pin, notes_sha, "b" * 40, "1234")
        self.assertEqual(set(assets), beta.release_asset_names())
        manifest = json.loads(assets["beta-release-manifest.json"])
        provenance = json.loads(assets["release-provenance.json"])
        self.assertIs(manifest["prerelease"], True)
        self.assertIs(manifest["makeLatest"], False)
        self.assertIs(provenance["releasePrerelease"], True)
        self.assertIs(provenance["releaseMakeLatest"], False)
        self.assertEqual(hashlib.sha256(assets[beta.PUBLIC_APK_NAME]).hexdigest(), APK_SHA)

    def test_publish_sends_prerelease_false_latest_payloads_after_exact_asset_readback(self):
        api = FakePublishingApi(self.pin, self.archive)
        result = self.publish_with_fake_api(api)
        create = next(payload for method, path, payload in api.calls
                      if method == "POST" and path == api.repo_path + "/releases")
        update = next(payload for method, path, payload in api.calls
                      if method == "PATCH" and path == api.repo_path + "/releases/700")
        tag = next(payload for method, path, payload in api.calls
                   if method == "POST" and path == api.repo_path + "/git/tags")
        tag_ref = next(payload for method, path, payload in api.calls
                       if method == "POST" and path == api.repo_path + "/git/refs")
        notes_sha = hashlib.sha256(create["body"].encode("utf-8")).hexdigest()
        self.assertEqual(tag["tag"], self.pin.tag)
        self.assertEqual(tag["object"], self.pin.source_sha)
        self.assertIn(f"Candidate-Artifact-SHA256: {self.pin.artifact_digest}", tag["message"])
        self.assertIn(f"Release-Notes-SHA256: {notes_sha}", tag["message"])
        self.assertEqual(tag_ref, {"ref": f"refs/tags/{self.pin.tag}",
                                   "sha": api.tag_object_sha})
        self.assertEqual(create["tag_name"], self.pin.tag)
        self.assertIs(create["draft"], True)
        self.assertIs(create["prerelease"], True)
        self.assertEqual(create["make_latest"], "false")
        self.assertIs(update["draft"], False)
        self.assertIs(update["prerelease"], True)
        self.assertEqual(update["make_latest"], "false")
        self.assertEqual(set(api.asset_bytes), beta.release_asset_names())
        self.assertEqual(api.downloaded_assets, beta.release_asset_names())
        self.assertIs(result["prerelease"], True)
        self.assertIs(result["immutable"], True)
        self.assertEqual(api.latest_override["tag_name"] if api.latest_override else beta.LATEST_STABLE_TAG,
                         beta.LATEST_STABLE_TAG)

    def test_publish_never_publishes_before_exact_download_readback(self):
        api = FakePublishingApi(self.pin, self.archive)
        api.corrupt_download_name = beta.PUBLIC_APK_NAME
        with self.assertRaisesRegex(beta.stable.PromotionError, "public download bytes"):
            self.publish_with_fake_api(api)
        self.assertTrue(api.release["draft"])
        self.assertFalse(any(method == "PATCH" for method, _, _ in api.calls))

    def test_partial_draft_retry_preserves_first_publisher_run_and_publishes_once(self):
        api = FakePublishingApi(self.pin, self.archive)
        api.fail_after_upload_at = 3
        with self.assertRaisesRegex(beta.stable.PromotionError, "lost response after storing"):
            self.publish_with_fake_api(api, run_id="111")
        self.assertTrue(api.release["draft"])
        self.assertEqual(len(api.asset_bytes), 3)

        self.publish_with_fake_api(api, run_id="222")
        manifest = json.loads(api.asset_bytes["beta-release-manifest.json"])
        self.assertEqual(manifest["publisherWorkflowRunId"], "111")
        self.assertEqual(len([1 for method, path, _ in api.calls
                              if method == "POST" and path == api.repo_path + "/git/tags"]), 1)
        self.assertEqual(len([1 for method, path, _ in api.calls
                              if method == "POST" and path == api.repo_path + "/releases"]), 1)
        self.assertEqual(len([1 for method, path, _ in api.calls
                              if method == "PATCH" and path == api.repo_path + "/releases/700"]), 1)

    def test_unapproved_partial_draft_asset_blocks_publication(self):
        api = FakePublishingApi(self.pin, self.archive)
        api.fail_before_upload_at = 1
        with self.assertRaisesRegex(beta.stable.PromotionError, "simulated upload failure"):
            self.publish_with_fake_api(api)
        bad_name = "candidate-manifest.json"
        bad_bytes = b"must remain private"
        asset_id = api.next_asset_id
        api.next_asset_id += 1
        api.asset_bytes[bad_name] = bad_bytes
        api.assets_by_id[asset_id] = bad_name
        api.release["assets"].append({
            "id": asset_id, "name": bad_name, "state": "uploaded", "size": len(bad_bytes),
            "digest": beta.stable.asset_digest(bad_bytes),
        })
        with self.assertRaisesRegex(beta.stable.PromotionError, "unapproved"):
            self.publish_with_fake_api(api, run_id="222")
        self.assertTrue(api.release["draft"])
        self.assertFalse(any(method == "PATCH" for method, _, _ in api.calls))

    def test_main_drift_after_uploads_leaves_release_as_draft(self):
        api = FakePublishingApi(self.pin, self.archive)
        api.main_drift_at = 4
        with self.assertRaisesRegex(beta.stable.PromotionError, "main moved during beta publication"):
            self.publish_with_fake_api(api)
        self.assertEqual(api.main_checks, 4)
        self.assertTrue(api.release["draft"])
        self.assertEqual(set(api.asset_bytes), beta.release_asset_names())
        self.assertFalse(any(method == "PATCH" for method, _, _ in api.calls))

    def test_published_release_retry_is_read_only_and_stable_latest_remains_v072(self):
        api = FakePublishingApi(self.pin, self.archive)
        self.publish_with_fake_api(api, run_id="111")
        mutation_count = sum(method in {"POST", "PATCH"} for method, _, _ in api.calls)
        api.downloaded_assets.clear()
        result = self.publish_with_fake_api(api, run_id="222")
        self.assertEqual(sum(method in {"POST", "PATCH"} for method, _, _ in api.calls), mutation_count)
        self.assertEqual(api.downloaded_assets, beta.release_asset_names())
        self.assertEqual(api.latest_override, None)
        self.assertEqual(result["tag_name"], self.pin.tag)

    def test_publish_readback_fails_if_github_moves_latest_to_the_beta(self):
        api = FakePublishingApi(self.pin, self.archive)
        api.latest_beta_on_publish = True
        with self.assertRaisesRegex(beta.stable.PromotionError, "not the expected v0.7.2"):
            self.publish_with_fake_api(api)
        self.assertIs(api.release["draft"], False)
        self.assertEqual(api.latest_override["tag_name"], self.pin.tag)
        self.assertEqual(len([1 for method, path, _ in api.calls
                              if method == "PATCH" and path == api.repo_path + "/releases/700"]), 1)

    def _p(self, suffix):
        return "/repos/weekssa/OPRA-EQ-for-UAPP" + suffix

    def _run(self):
        return {"id": self.pin.run_id, "run_number": self.pin.run_number,
                "workflow_id": beta.CANDIDATE_WORKFLOW_ID, "path": beta.CANDIDATE_WORKFLOW,
                "status": "completed", "conclusion": "success", "run_attempt": 1,
                "event": "workflow_dispatch", "head_branch": "main", "head_sha": self.pin.source_sha,
                "head_repository": {"full_name": beta.REPOSITORY}}

    def _artifact(self):
        return {"id": self.pin.artifact_id, "name": self.pin.artifact_name, "expired": False,
                "size_in_bytes": len(self.archive), "digest": f"sha256:{self.pin.artifact_digest}",
                "workflow_run": {"id": self.pin.run_id, "head_branch": "main", "head_sha": self.pin.source_sha}}

    def _api(self, run=None, artifact=None):
        return FakeApi({
            ("GET", self._p("/actions/workflows/signed-beta.yml")): {
                "id": beta.CANDIDATE_WORKFLOW_ID, "path": beta.CANDIDATE_WORKFLOW, "state": "active"
            },
            ("GET", self._p(f"/actions/runs/{self.pin.run_id}")): self._run() if run is None else run,
            ("GET", self._p(f"/actions/artifacts/{self.pin.artifact_id}")): self._artifact() if artifact is None else artifact,
        })


if __name__ == "__main__":
    suite = unittest.defaultTestLoader.loadTestsFromTestCase(BetaPublisherTest)
    result = unittest.TextTestRunner(verbosity=2).run(suite)
    if result.wasSuccessful():
        print("BETA_PUBLISHER_TESTS_PASSED")
    raise SystemExit(0 if result.wasSuccessful() else 1)
