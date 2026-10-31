from __future__ import annotations

import copy
import hashlib
import io
import json
import os
import shutil
import subprocess
import tempfile
import unittest
import urllib.parse
import zipfile
from pathlib import Path
from unittest import mock

import publish_v090_beta_release as publisher
import v090_release as profile


SOURCE = "a" * 40
RUN_ID = 123
ARTIFACT_ID = 456
SIGNER = "65c1c1256dae3c49e3548f334c91f0ba991969e9be9e0b223ba4e253d2114747"
REPOSITORY_ROOT = Path(__file__).resolve().parents[1]
APK = b"fixture v0.9 beta APK bytes"
APK_SHA = hashlib.sha256(APK).hexdigest()
ARTIFACT_SHA = "b" * 64
R8 = b"com.weekssa.opraeqforuapp.MainActivity -> a.b:\n"
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
    api_url = "https://api.github.com"

    def __init__(self, responses, archive=b""):
        self.responses = responses
        self.archive = archive

    def json(self, method, path, payload=None):
        value = copy.deepcopy(self.responses[(method, path)])
        if isinstance(value, Exception):
            raise value
        return value

    def optional_json(self, path):
        value = self.responses.get(("GET", path))
        if isinstance(value, Exception):
            raise value
        return copy.deepcopy(value)

    def request_bytes(self, url, **kwargs):
        path = url.removeprefix(self.api_url)
        if path == f"{self.repo_path}/actions/artifacts/{ARTIFACT_ID}/zip":
            return 200, self.archive, {}
        raise AssertionError(f"unexpected byte request {url}")


class FakePublishingApi:
    repo_path = "/repos/weekssa/OPRA-EQ-for-UAPP"
    api_url = "https://api.github.com"
    tag_object_sha = "d" * 40

    def __init__(self, archive, candidate_source):
        self.archive = archive
        self.candidate_source = candidate_source
        self.calls = []
        self.release = None
        self.tag_object = None
        self.tag_ref = None
        self.assets = {}
        self.asset_bytes = {}
        self.next_asset_id = 900
        self.upload_count = 0
        self.fail_upload_at = None
        self.published_once = False

    def _latest(self):
        return {
            "tag_name": profile.STABLE_LATEST_TAG,
            "draft": False,
            "prerelease": False,
            "immutable": True,
            "assets": [{
                "name": "EQ-Library-v0.8.3.apk",
                "digest": f"sha256:{profile.STABLE_LATEST_APK_SHA256}",
            }],
        }

    def json(self, method, path, payload=None):
        self.calls.append((method, path, copy.deepcopy(payload)))
        if method == "GET":
            if path == self.repo_path + "/actions/workflows/github-release.yml":
                return {"id": 91, "path": profile.CANDIDATE_WORKFLOW_PATH, "state": "active"}
            if path == self.repo_path + f"/actions/runs/{RUN_ID}":
                return {
                    "id": RUN_ID, "workflow_id": 91, "status": "completed", "conclusion": "success",
                    "run_attempt": 1, "event": "workflow_dispatch", "head_branch": "main",
                    "head_sha": self.candidate_source, "head_repository": {"full_name": profile.REPOSITORY},
                }
            if path == self.repo_path + f"/actions/artifacts/{ARTIFACT_ID}":
                return {
                    "id": ARTIFACT_ID, "expired": False, "size_in_bytes": len(self.archive),
                    "name": profile.expected_candidate_artifact_name(self.candidate_source),
                    "digest": f"sha256:{hashlib.sha256(self.archive).hexdigest()}",
                    "workflow_run": {"id": RUN_ID, "head_branch": "main", "head_sha": self.candidate_source},
                }
            if path == self.repo_path + "/branches/main":
                return {"commit": {"sha": self.candidate_source}}
            if path == self.repo_path + "/releases/latest":
                return copy.deepcopy(self._latest())
            if path == self.repo_path + "/releases/tags/" + urllib.parse.quote(profile.BETA_TAG, safe=""):
                if self.release is None:
                    raise publisher.stable.PromotionError("GitHub API returned HTTP 404: release missing")
                return copy.deepcopy(self.release)
            if path == self.repo_path + "/git/ref/tags/" + urllib.parse.quote(profile.BETA_TAG, safe=""):
                if self.tag_ref is None:
                    raise publisher.stable.PromotionError("GitHub API returned HTTP 404: tag ref missing")
                return copy.deepcopy(self.tag_ref)
            if path == self.repo_path + f"/git/tags/{self.tag_object_sha}":
                if self.tag_object is None:
                    raise publisher.stable.PromotionError("GitHub API returned HTTP 404: tag object missing")
                return copy.deepcopy(self.tag_object)
            raise AssertionError(f"unexpected fake GET {path}")
        if method == "POST" and path == self.repo_path + "/git/tags":
            self.tag_object = {
                **copy.deepcopy(payload), "sha": self.tag_object_sha,
                "object": {"type": "commit", "sha": payload["object"]},
            }
            return copy.deepcopy(self.tag_object)
        if method == "POST" and path == self.repo_path + "/git/refs":
            self.tag_ref = {"ref": payload["ref"], "object": {"type": "tag", "sha": payload["sha"]}}
            return copy.deepcopy(self.tag_ref)
        if method == "POST" and path == self.repo_path + "/releases":
            self.release = {
                "id": 700, "tag_name": payload["tag_name"], "name": payload["name"],
                "body": payload["body"], "draft": True, "prerelease": True, "immutable": False,
                "upload_url": f"https://uploads.github.com{self.repo_path}/releases/700/assets{{?name,label}}",
                "assets_url": f"{self.api_url}{self.repo_path}/releases/700/assets",
                "html_url": f"https://github.com/{profile.REPOSITORY}/releases/tag/{profile.BETA_TAG}",
                "assets": [],
            }
            return copy.deepcopy(self.release)
        if method == "PATCH" and path == self.repo_path + "/releases/700":
            self.release.update(copy.deepcopy(payload))
            if payload.get("draft") is False:
                self.release["immutable"] = True
                self.published_once = True
            return copy.deepcopy(self.release)
        raise AssertionError(f"unexpected fake mutation {method} {path}")

    def optional_json(self, path):
        try:
            return self.json("GET", path)
        except publisher.stable.PromotionError as exc:
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
        if method == "GET" and parsed.path == self.repo_path + f"/actions/artifacts/{ARTIFACT_ID}/zip":
            return 200, self.archive, {}
        if method == "POST" and parsed.hostname == "uploads.github.com":
            name = urllib.parse.parse_qs(parsed.query)["name"][0]
            self.upload_count += 1
            if self.fail_upload_at == self.upload_count:
                raise publisher.stable.PromotionError("simulated upload interruption")
            asset_id = self.next_asset_id
            self.next_asset_id += 1
            self.asset_bytes[name] = body
            asset = {"id": asset_id, "name": name, "state": "uploaded", "size": len(body),
                     "digest": publisher.stable.asset_digest(body)}
            self.assets[name] = asset
            self.release["assets"].append(asset)
            return 201, json.dumps(asset).encode(), {}
        if method == "GET" and parsed.path.startswith(self.repo_path + "/releases/assets/"):
            asset_id = int(parsed.path.rsplit("/", 1)[1])
            by_id = {asset["id"]: name for name, asset in self.assets.items()}
            return 200, self.asset_bytes[by_id[asset_id]], {}
        raise AssertionError(f"unexpected byte request {url}")


class V090PublisherTest(unittest.TestCase):
    def setUp(self):
        self.temp = tempfile.TemporaryDirectory(prefix="v090-publisher-test-")
        self.root = Path(self.temp.name)
        (self.root / "app").mkdir()
        (self.root / "docs/releases").mkdir(parents=True)
        (self.root / "app/build.gradle.kts").write_text(
            'android {\n    defaultConfig {\n        versionCode = 15\n        versionName = "0.9.0-beta.1"\n    }\n}\n',
            encoding="utf-8",
        )
        (self.root / "release-signing-cert.sha256").write_text(SIGNER + "\n", encoding="utf-8")
        (self.root / "docs/releases/v0.9.0-beta.1.md").write_text(
            "# EQ Library v0.9.0-beta.1\n\nPublic stable/latest remains v0.8.3. "
            "Publish only as a prerelease.\n",
            encoding="utf-8",
        )
        self.files = self.make_files()
        self.archive = self.archive_bytes(self.files)
        self.archive_sha = hashlib.sha256(self.archive).hexdigest()
        (self.root / "docs/releases/v0.9.0").mkdir(parents=True, exist_ok=True)
        shutil.copyfile(
            REPOSITORY_ROOT / "docs/releases/v0.9.0/PLAN.md",
            self.root / "docs/releases/v0.9.0/PLAN.md",
        )
        shutil.copyfile(
            REPOSITORY_ROOT / "docs/releases/v0.9.0/SEAL.sha256",
            self.root / "docs/releases/v0.9.0/SEAL.sha256",
        )
        self.write_qualification()

    def tearDown(self):
        self.temp.cleanup()

    def make_files(self):
        apk_name = f"EQ-Library-{profile.BETA_TAG}.apk"
        manifest = profile.candidate_metadata(SOURCE) | {
            "apk": apk_name,
            "apkSha256": APK_SHA,
            "signerSha256": SIGNER,
            "r8MinificationEnabled": True,
            "r8MappingSha256": hashlib.sha256(R8).hexdigest(),
        }
        return {
            apk_name: APK,
            f"{apk_name}.sha256": f"{APK_SHA}  {apk_name}\n".encode("ascii"),
            "candidate-manifest.json": (json.dumps(manifest) + "\n").encode("utf-8"),
            "apksigner-verification.txt": SIGNER_REPORT,
            "zipalign-verification.txt": ALIGNMENT,
            "r8-mapping.txt": R8,
        }

    def write_qualification(self, *, status: str = "PASS", readiness: str = "READY FOR BETA",
                            source: str = SOURCE, archive_sha: str | None = None):
        record = {
            "schemaVersion": 1,
            "releaseTag": profile.BETA_TAG,
            "qualificationStatus": status,
            "readiness": readiness,
            "planSeal": {
                "planSha256": hashlib.sha256((self.root / "docs/releases/v0.9.0/PLAN.md").read_bytes()).hexdigest(),
                "sealManifestSha256": hashlib.sha256(
                    (self.root / "docs/releases/v0.9.0/SEAL.sha256").read_bytes()
                ).hexdigest(),
                "baselineCommit": "35f0c90a0e55e701d4343ed978a440d34aae13aa",
            },
            "candidate": {
                "sourceSha": source,
                "runId": RUN_ID,
                "artifactId": ARTIFACT_ID,
                "artifactArchiveSha256": archive_sha or self.archive_sha,
                "apkSha256": APK_SHA,
                "signerSha256": SIGNER,
            },
            "computerQualification": {"status": "PASS", "evidenceRefs": ["docs/releases/v0.9.0/EXECUTION_REPORT.md"]},
            "hardwareQualification": {"status": "PASS", "evidenceRefs": ["#J-verification"]},
            "restoration": {"status": "PASS", "evidenceRefs": ["#restoration"]},
        }
        (self.root / publisher.QUALIFICATION_PATH).write_text(json.dumps(record, indent=2) + "\n", encoding="utf-8")

    @staticmethod
    def archive_bytes(files):
        stream = io.BytesIO()
        with zipfile.ZipFile(stream, "w", compression=zipfile.ZIP_DEFLATED) as archive:
            for name, data in files.items():
                archive.writestr(name, data)
        return stream.getvalue()

    def candidate_api(self, *, run_source=SOURCE, run_id=RUN_ID, artifact_id=ARTIFACT_ID,
                      artifact_digest=None, artifact_name=None, workflow_path=None):
        workflow_path = workflow_path or profile.CANDIDATE_WORKFLOW_PATH
        digest = artifact_digest or self.archive_sha
        artifact_name = artifact_name or profile.expected_candidate_artifact_name(SOURCE)
        return FakeApi({
            ("GET", "/repos/weekssa/OPRA-EQ-for-UAPP/actions/workflows/github-release.yml"): {
                "id": 91, "path": workflow_path, "state": "active",
            },
            ("GET", f"/repos/weekssa/OPRA-EQ-for-UAPP/actions/runs/{RUN_ID}"): {
                "id": RUN_ID, "workflow_id": 91, "status": "completed", "conclusion": "success",
                "run_attempt": 1, "event": "workflow_dispatch", "head_branch": "main",
                "head_sha": run_source,
                "head_repository": {"full_name": profile.REPOSITORY},
            },
            ("GET", "/repos/weekssa/OPRA-EQ-for-UAPP/branches/main"): {"commit": {"sha": SOURCE}},
            ("GET", f"/repos/weekssa/OPRA-EQ-for-UAPP/actions/artifacts/{ARTIFACT_ID}"): {
                "id": artifact_id, "expired": False, "size_in_bytes": len(self.archive),
                "name": artifact_name, "digest": f"sha256:{digest}",
                "workflow_run": {"id": run_id, "head_branch": "main", "head_sha": run_source},
            },
        }, archive=self.archive)

    def validate_archive(self, files=None, digest=None):
        archive = self.archive if files is None else self.archive_bytes(files)
        return publisher.validate_candidate_archive(
            archive,
            project_root=self.root,
            source_sha=SOURCE,
            run_id=RUN_ID,
            artifact_id=ARTIFACT_ID,
            artifact_digest=digest or hashlib.sha256(archive).hexdigest(),
        )

    def publish_with_fake(self, api, run_id: str = "789"):
        with mock.patch.dict(os.environ, {
            "GITHUB_REPOSITORY": profile.REPOSITORY,
            "GITHUB_REF": "refs/heads/main",
            "GITHUB_SHA": SOURCE,
            "GITHUB_RUN_ID": run_id,
        }), mock.patch.object(publisher, "validate_checkout"), mock.patch.object(
            publisher, "verify_android", return_value={
                "apksigner-verification.txt": SIGNER_REPORT,
                "zipalign-verification.txt": ALIGNMENT,
            },
        ):
            return publisher.publish(
                api, project_root=self.root, build_tools=Path("/build-tools"),
                run_id=RUN_ID, artifact_id=ARTIFACT_ID, artifact_digest=self.archive_sha,
                candidate_source_sha=SOURCE,
            )

    def test_exact_candidate_run_and_artifact_pass(self):
        api = self.candidate_api()
        publisher.validate_candidate_run_and_artifact(
            api, run_id=RUN_ID, artifact_id=ARTIFACT_ID,
            artifact_digest=self.archive_sha, source_sha=SOURCE, publisher_sha=SOURCE,
        )
        candidate = self.validate_archive()
        self.assertEqual(APK_SHA, candidate["apk_sha256"])
        self.assertEqual(SIGNER, candidate["signer_sha256"])

    def test_wrong_source_run_artifact_and_workflow_are_rejected(self):
        cases = [
            ("source", self.candidate_api(run_source="c" * 40), "source"),
            ("run", self.candidate_api(run_id=999), "run"),
            ("artifact id", self.candidate_api(artifact_id=999), "artifact ID"),
            ("artifact name", self.candidate_api(artifact_name="wrong.zip"), "artifact name"),
            ("workflow", self.candidate_api(workflow_path="wrong.yml"), "workflow"),
        ]
        for label, api, error in cases:
            with self.subTest(label=label), self.assertRaisesRegex(publisher.stable.PromotionError, error):
                publisher.validate_candidate_run_and_artifact(
                    api, run_id=RUN_ID, artifact_id=ARTIFACT_ID,
                    artifact_digest=self.archive_sha, source_sha=SOURCE, publisher_sha=SOURCE,
                )

    def test_wrong_artifact_hash_apk_hash_or_signer_is_rejected(self):
        api = self.candidate_api(artifact_digest="d" * 64)
        with self.assertRaisesRegex(publisher.stable.PromotionError, "digest"):
            publisher.validate_candidate_run_and_artifact(
                api, run_id=RUN_ID, artifact_id=ARTIFACT_ID,
                artifact_digest=self.archive_sha, source_sha=SOURCE, publisher_sha=SOURCE,
            )

        files = self.make_files()
        manifest = json.loads(files["candidate-manifest.json"])
        manifest["apkSha256"] = "e" * 64
        files["candidate-manifest.json"] = (json.dumps(manifest) + "\n").encode("utf-8")
        with self.assertRaisesRegex(publisher.stable.PromotionError, "APK hash"):
            self.validate_archive(files)

        files = self.make_files()
        files["apksigner-verification.txt"] = SIGNER_REPORT.replace(SIGNER.encode(), b"f" * 64)
        with self.assertRaisesRegex(publisher.stable.PromotionError, "signer"):
            self.validate_archive(files)

    def test_published_release_requires_prerelease_and_immutability(self):
        release = {
            "tag_name": profile.BETA_TAG,
            "name": "EQ Library v0.9.0-beta.1",
            "body": publisher.release_notes(self.root).decode("utf-8"),
            "draft": False,
            "prerelease": True,
            "immutable": True,
        }
        publisher.check_release(release, release["body"], published=True)
        for field, value, error in (("prerelease", False, "prerelease"), ("immutable", False, "immutable")):
            unsafe = dict(release)
            unsafe[field] = value
            with self.subTest(field=field), self.assertRaisesRegex(publisher.stable.PromotionError, error):
                publisher.check_release(unsafe, unsafe["body"], published=True)

    def test_publication_manifest_explicitly_preserves_nonlatest_prerelease(self):
        candidate = self.validate_archive()
        candidate.update({"source_sha": SOURCE, "publisher_source_sha": SOURCE,
                          "artifact_digest": self.archive_sha,
                          "android_logs": {"apksigner-verification.txt": SIGNER_REPORT,
                                           "zipalign-verification.txt": ALIGNMENT}})
        assets = publisher.publication_assets(candidate, "d" * 64, "789")
        manifest = json.loads(assets["beta-release-manifest.json"])
        self.assertIs(True, manifest["prerelease"])
        self.assertIs(False, manifest["makeLatest"])
        self.assertEqual(SOURCE, manifest["releaseSourceSha"])
        self.assertEqual(self.archive_sha, manifest["candidateArtifactSha256"])

    def test_workflow_contract_preserves_legacy_and_requires_explicit_approval(self):
        publisher.check_contract(Path(__file__).resolve().parents[1])

    def assert_metadata_pair(self, api, publisher_run_id: str) -> None:
        manifest_bytes = api.asset_bytes["beta-release-manifest.json"]
        provenance_bytes = api.asset_bytes["release-provenance.json"]
        manifest = json.loads(manifest_bytes)
        provenance = json.loads(provenance_bytes)
        self.assertEqual(publisher_run_id, manifest["publisherWorkflowRunId"])
        self.assertEqual(publisher_run_id, provenance["publisherWorkflowRunId"])
        self.assertEqual(hashlib.sha256(manifest_bytes).hexdigest(),
                         provenance["betaReleaseManifestSha256"])
        self.assertEqual(SOURCE, manifest["releaseSourceSha"])
        self.assertEqual(SOURCE, provenance["releaseSourceSha"])
        self.assertEqual(manifest["releaseSourceSha"], provenance["releaseSourceSha"])
        self.assertEqual(manifest["publisherSourceSha"], provenance["publisherSourceSha"])

    def test_fresh_publish_has_consistent_metadata_and_idempotent_retry(self):
        api = FakePublishingApi(self.archive, SOURCE)
        release = self.publish_with_fake(api, "789")
        self.assertFalse(release["draft"])
        self.assertTrue(release["prerelease"])
        self.assertTrue(release["immutable"])
        self.assertEqual(profile.STABLE_LATEST_TAG, api._latest()["tag_name"])
        self.assertEqual(SOURCE, api.tag_object["object"]["sha"])
        self.assert_metadata_pair(api, "789")

        mutations = len([call for call in api.calls if call[0] in {"POST", "PATCH"}])
        uploads = api.upload_count
        self.assertEqual(release, self.publish_with_fake(api, "790"))
        self.assertEqual(mutations, len([call for call in api.calls if call[0] in {"POST", "PATCH"}]))
        self.assertEqual(uploads, api.upload_count)

    def test_manifest_only_resume_regenerates_provenance_with_original_publisher_identity(self):
        api = FakePublishingApi(self.archive, SOURCE)
        api.fail_upload_at = 4  # APK, checksum, and manifest succeeded; provenance failed.
        with self.assertRaisesRegex(publisher.stable.PromotionError, "upload interruption"):
            self.publish_with_fake(api, "789")
        self.assertIn("beta-release-manifest.json", api.asset_bytes)
        self.assertNotIn("release-provenance.json", api.asset_bytes)

        api.fail_upload_at = None
        release = self.publish_with_fake(api, "790")
        self.assertFalse(release["draft"])
        self.assert_metadata_pair(api, "789")

    def test_both_existing_metadata_resume_preserves_exact_bytes_and_identity(self):
        api = FakePublishingApi(self.archive, SOURCE)
        api.fail_upload_at = 5  # Both metadata records exist; signer upload was interrupted.
        with self.assertRaisesRegex(publisher.stable.PromotionError, "upload interruption"):
            self.publish_with_fake(api, "789")
        manifest = api.asset_bytes["beta-release-manifest.json"]
        provenance = api.asset_bytes["release-provenance.json"]

        api.fail_upload_at = None
        release = self.publish_with_fake(api, "790")
        self.assertFalse(release["draft"])
        self.assertEqual(manifest, api.asset_bytes["beta-release-manifest.json"])
        self.assertEqual(provenance, api.asset_bytes["release-provenance.json"])
        self.assert_metadata_pair(api, "789")

    def test_conflicting_existing_metadata_blocks_before_publication(self):
        api = FakePublishingApi(self.archive, SOURCE)
        api.fail_upload_at = 5
        with self.assertRaisesRegex(publisher.stable.PromotionError, "upload interruption"):
            self.publish_with_fake(api, "789")
        provenance = json.loads(api.asset_bytes["release-provenance.json"])
        provenance["publisherWorkflowRunId"] = "790"
        api.asset_bytes["release-provenance.json"] = (json.dumps(provenance, indent=2, sort_keys=True) + "\n").encode()
        api.fail_upload_at = None

        with self.assertRaisesRegex(publisher.stable.PromotionError, "publisher identity|conflicts"):
            self.publish_with_fake(api, "790")
        self.assertTrue(api.release["draft"])
        self.assertFalse(any(call[0] == "PATCH" for call in api.calls))

    def test_missing_or_nonpass_qualification_blocks_before_mutation(self):
        api = FakePublishingApi(self.archive, SOURCE)
        with mock.patch.dict(os.environ, {
            "GITHUB_REPOSITORY": profile.REPOSITORY,
            "GITHUB_REF": "refs/heads/main",
            "GITHUB_SHA": SOURCE,
            "GITHUB_RUN_ID": "789",
        }), mock.patch.object(publisher, "validate_checkout"), mock.patch.object(
            publisher, "verify_android", return_value={
                "apksigner-verification.txt": SIGNER_REPORT,
                "zipalign-verification.txt": ALIGNMENT,
            },
        ):
            (self.root / publisher.QUALIFICATION_PATH).unlink()
            with self.assertRaisesRegex(publisher.stable.PromotionError, "qualification record is missing"):
                publisher.publish(
                    api, project_root=self.root, build_tools=Path("/build-tools"),
                    run_id=RUN_ID, artifact_id=ARTIFACT_ID, artifact_digest=self.archive_sha,
                    candidate_source_sha=SOURCE,
                )
            self.assertFalse(any(call[0] in {"POST", "PATCH"} for call in api.calls))

            self.write_qualification()
            (self.root / "docs/releases/v0.9.0-beta.1.md").write_text(
                "# Wrong release title\n\nPublic stable/latest remains v0.8.3. prerelease.\n",
                encoding="utf-8",
            )
            with self.assertRaisesRegex(publisher.stable.PromotionError, "release notes"):
                publisher.publish(
                    api, project_root=self.root, build_tools=Path("/build-tools"),
                    run_id=RUN_ID, artifact_id=ARTIFACT_ID, artifact_digest=self.archive_sha,
                    candidate_source_sha=SOURCE,
                )
            self.assertFalse(any(call[0] in {"POST", "PATCH"} for call in api.calls))

            (self.root / "docs/releases/v0.9.0-beta.1.md").write_text(
                "# EQ Library v0.9.0-beta.1\n\nPublic stable/latest remains v0.8.3. "
                "Publish only as a prerelease.\n",
                encoding="utf-8",
            )
            self.write_qualification(status="FAIL")
            with self.assertRaisesRegex(publisher.stable.PromotionError, "not PASS"):
                publisher.publish(
                    api, project_root=self.root, build_tools=Path("/build-tools"),
                    run_id=RUN_ID, artifact_id=ARTIFACT_ID, artifact_digest=self.archive_sha,
                    candidate_source_sha=SOURCE,
                )
            self.assertFalse(any(call[0] in {"POST", "PATCH"} for call in api.calls))

            self.write_qualification(source="e" * 40)
            with self.assertRaisesRegex(publisher.stable.PromotionError, "candidate tuple"):
                publisher.publish(
                    api, project_root=self.root, build_tools=Path("/build-tools"),
                    run_id=RUN_ID, artifact_id=ARTIFACT_ID, artifact_digest=self.archive_sha,
                    candidate_source_sha=SOURCE,
                )
            self.assertFalse(any(call[0] in {"POST", "PATCH"} for call in api.calls))

    def test_changed_plan_or_seal_cannot_replace_the_candidate_contract(self):
        candidate = self.validate_archive()
        candidate.update({"source_sha": SOURCE, "artifact_digest": self.archive_sha})
        publisher.load_qualification(self.root, candidate)

        plan = self.root / "docs/releases/v0.9.0/PLAN.md"
        plan.write_bytes(plan.read_bytes() + b"replacement\n")
        with self.assertRaisesRegex(publisher.stable.PromotionError, "pinned v0.9 plan"):
            publisher.load_qualification(self.root, candidate)

        shutil.copyfile(REPOSITORY_ROOT / "docs/releases/v0.9.0/PLAN.md", plan)
        seal = self.root / "docs/releases/v0.9.0/SEAL.sha256"
        seal.write_bytes(seal.read_bytes() + b"replacement\n")
        with self.assertRaisesRegex(publisher.stable.PromotionError, "pinned v0.9 seal"):
            publisher.load_qualification(self.root, candidate)

    def test_publisher_requires_docs_only_advance_from_candidate_source(self):
        repo = self.root
        subprocess.run(["git", "init", "-q"], cwd=repo, check=True)
        subprocess.run(["git", "add", "."], cwd=repo, check=True)
        subprocess.run(
            ["git", "-c", "user.name=Test", "-c", "user.email=test@example.com", "commit", "-qm", "candidate"],
            cwd=repo,
            check=True,
        )
        candidate = subprocess.run(["git", "rev-parse", "HEAD"], cwd=repo, check=True,
                                   text=True, stdout=subprocess.PIPE).stdout.strip()
        (repo / "README.md").write_text("candidate\n", encoding="utf-8")
        subprocess.run(["git", "add", "README.md"], cwd=repo, check=True)
        subprocess.run(
            ["git", "-c", "user.name=Test", "-c", "user.email=test@example.com", "commit", "-qm", "docs"],
            cwd=repo,
            check=True,
        )
        docs_head = subprocess.run(["git", "rev-parse", "HEAD"], cwd=repo, check=True,
                                   text=True, stdout=subprocess.PIPE).stdout.strip()
        with mock.patch.dict(os.environ, {"GITHUB_REPOSITORY": profile.REPOSITORY,
                                          "GITHUB_REF": "refs/heads/main"}):
            publisher.validate_checkout(repo, docs_head, candidate)
            disallowed = (
                "app/build.gradle.kts",
                "app/src/product.txt",
                ".github/workflows/github-release.yml",
                "tools/product_tool.py",
                "release-signing-cert.sha256",
                "README.md.evil",
            )
            for index, relative in enumerate(disallowed):
                with self.subTest(relative=relative):
                    subprocess.run(["git", "checkout", "-q", "-B", f"case-{index}", docs_head], cwd=repo, check=True)
                    path = repo / relative
                    path.parent.mkdir(parents=True, exist_ok=True)
                    prior = path.read_text(encoding="utf-8") if path.exists() else ""
                    path.write_text(prior + "\n# disallowed change\n", encoding="utf-8")
                    subprocess.run(["git", "add", relative], cwd=repo, check=True)
                    subprocess.run(
                        ["git", "-c", "user.name=Test", "-c", "user.email=test@example.com",
                         "commit", "-qm", relative],
                        cwd=repo,
                        check=True,
                    )
                    changed_head = subprocess.run(
                        ["git", "rev-parse", "HEAD"], cwd=repo, check=True, text=True, stdout=subprocess.PIPE
                    ).stdout.strip()
                    with self.assertRaisesRegex(publisher.stable.PromotionError, "executable"):
                        publisher.validate_checkout(repo, changed_head, docs_head)


if __name__ == "__main__":
    unittest.main()
