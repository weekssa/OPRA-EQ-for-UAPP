import argparse
import copy
import hashlib
import io
import json
import tempfile
import unittest
import zipfile
from pathlib import Path
from unittest import mock

import promote_release_candidate as promotion


SOURCE_SHA = "a" * 40
ARTIFACT_ID = 12345
RUN_ID = 23456
TAG = "v0.7.2"
SIGNER = "65c1c1256dae3c49e3548f334c91f0ba991969e9be9e0b223ba4e253d2114747"
SIGNER_COLON = ":".join(SIGNER[index:index + 2] for index in range(0, 64, 2))
APK = b"signed apk fixture bytes"
SIGNER_REPORT = f"""Verifies
Verified using v1 scheme (JAR signing): false
Verified using v2 scheme (APK Signature Scheme v2): true
Verified using v3 scheme (APK Signature Scheme v3): true
Number of signers: 1
Signer #1 certificate SHA-256 digest: {SIGNER_COLON}
""".encode("utf-8")
ALIGNMENT_REPORT = b"Verifying alignment of EQ-Library-v0.7.2.apk (4)...\nVerification successful\n"
R8_MAPPING = (
    b"com.weekssa.opraeqforuapp.MainActivity -> a.b:\n"
    b"com.weekssa.opraeqforuapp.domain.catalog.OpraEqProfile -> a.c:\n"
)


class FakeApi:
    repo_path = "/repos/weekssa/OPRA-EQ-for-UAPP"

    def __init__(self, responses):
        self.responses = responses

    def json(self, method, path, payload=None):
        key = (method, path)
        response = self.responses[key]
        if isinstance(response, Exception):
            raise response
        return copy.deepcopy(response)


class ReleasePromotionTest(unittest.TestCase):
    def setUp(self):
        self.temp = tempfile.TemporaryDirectory(prefix="release-promotion-test-")
        self.root = Path(self.temp.name)
        (self.root / "app").mkdir()
        (self.root / "docs/releases").mkdir(parents=True)
        (self.root / "app/build.gradle.kts").write_text(
            'android {\n    defaultConfig {\n        versionCode = 9\n        versionName = "0.7.2"\n    }\n}\n',
            encoding="utf-8",
        )
        (self.root / "release-signing-cert.sha256").write_text(SIGNER + "\n", encoding="utf-8")
        (self.root / "docs/releases/v0.7.2.md").write_text("# EQ Library v0.7.2\n\nRelease fixture.\n", encoding="utf-8")
        self.files = self.make_files()
        self.archive = self.make_archive(self.files)
        self.digest = hashlib.sha256(self.archive).hexdigest()

    def tearDown(self):
        self.temp.cleanup()

    def make_files(self):
        apk_name = f"EQ-Library-{TAG}.apk"
        apk_sha = hashlib.sha256(APK).hexdigest()
        manifest = {
            "sourceSha": SOURCE_SHA,
            "releaseTag": TAG,
            "apk": apk_name,
            "apkSha256": apk_sha,
            "packageId": promotion.PACKAGE_ID,
            "versionName": "0.7.2",
            "versionCode": 9,
            "signerSha256": SIGNER,
            "r8MinificationEnabled": True,
            "r8MappingSha256": hashlib.sha256(R8_MAPPING).hexdigest(),
        }
        return {
            apk_name: APK,
            f"{apk_name}.sha256": f"{apk_sha}  dist/{apk_name}\n".encode("ascii"),
            "candidate-manifest.json": (json.dumps(manifest) + "\n").encode("utf-8"),
            "apksigner-verification.txt": SIGNER_REPORT,
            "zipalign-verification.txt": ALIGNMENT_REPORT,
            "r8-mapping.txt": R8_MAPPING,
        }

    @staticmethod
    def make_archive(files):
        stream = io.BytesIO()
        with zipfile.ZipFile(stream, "w", compression=zipfile.ZIP_DEFLATED) as archive:
            for name, data in files.items():
                archive.writestr(name, data)
        return stream.getvalue()

    def validate(self, archive=None, digest=None, tag=TAG, source_sha=SOURCE_SHA):
        candidate = promotion.validate_candidate_archive(
            self.archive if archive is None else archive,
            artifact_digest=self.digest if digest is None else digest,
            tag=tag,
            source_sha=source_sha,
            project_root=self.root,
        )
        candidate.update(candidate_run_id=RUN_ID, candidate_artifact_id=ARTIFACT_ID)
        return candidate

    def test_accepts_exact_candidate_manifest_checksum_signer_alignment_and_source(self):
        candidate = self.validate(digest=f"sha256:{self.digest}")
        self.assertEqual(SOURCE_SHA, candidate["source_sha"])
        self.assertEqual(hashlib.sha256(APK).hexdigest(), candidate["apk_sha256"])
        self.assertEqual(SIGNER, candidate["signer_sha256"])

    def test_rejects_downloaded_archive_digest_mismatch(self):
        with self.assertRaisesRegex(promotion.PromotionError, "artifact digest"):
            self.validate(digest="c" * 64)

    def test_artifact_digest_comparison_accepts_github_prefix_on_either_value(self):
        promotion.require_matching_sha256_digest(
            f"sha256:{self.digest}", self.digest, "artifact digest mismatch"
        )
        promotion.require_matching_sha256_digest(
            self.digest, f"sha256:{self.digest}", "artifact digest mismatch"
        )

    def test_artifact_digest_comparison_rejects_mismatch_and_malformed_values(self):
        with self.assertRaisesRegex(promotion.PromotionError, "artifact digest mismatch"):
            promotion.require_matching_sha256_digest(
                f"sha256:{self.digest}", "c" * 64, "artifact digest mismatch"
            )
        with self.assertRaisesRegex(promotion.PromotionError, "SHA-256 digest input is malformed"):
            promotion.require_matching_sha256_digest(
                f"sha256:{self.digest}", "not-a-digest", "artifact digest mismatch"
            )

    def test_published_release_name_and_body_match(self):
        promotion.require_release_name_and_body(
            {"name": "EQ Library v1.2.3", "body": "# Notes\n"},
            tag="v1.2.3",
            release_notes="# Notes\n",
        )

    def test_published_release_name_mismatch_fails(self):
        with self.assertRaisesRegex(promotion.PromotionError, "expected release name"):
            promotion.require_release_name_and_body(
                {"name": "Wrong title", "body": "# Notes\n"},
                tag=TAG,
                release_notes="# Notes\n",
            )

    def test_published_release_body_mismatch_fails(self):
        with self.assertRaisesRegex(promotion.PromotionError, "expected release body"):
            promotion.require_release_name_and_body(
                {"name": "EQ Library v0.7.2", "body": "# Different notes\n"},
                tag=TAG,
                release_notes="# Notes\n",
            )

    def test_published_release_body_normalizes_one_trailing_newline(self):
        promotion.require_release_name_and_body(
            {"name": "EQ Library v0.7.2", "body": "# Notes"},
            tag=TAG,
            release_notes="# Notes\n",
        )

    def test_published_release_body_rejects_extra_trailing_blank_line(self):
        with self.assertRaisesRegex(promotion.PromotionError, "expected release body"):
            promotion.require_release_name_and_body(
                {"name": "EQ Library v0.7.2", "body": "# Notes\n\n"},
                tag=TAG,
                release_notes="# Notes\n",
            )

    def test_published_release_body_does_not_rewrite_internal_line_endings(self):
        with self.assertRaisesRegex(promotion.PromotionError, "expected release body"):
            promotion.require_release_name_and_body(
                {"name": "EQ Library v0.7.2", "body": "# Notes\r\nSection\r\n"},
                tag=TAG,
                release_notes="# Notes\nSection\n",
            )

    def test_new_draft_release_metadata_is_checked_before_return(self):
        candidate = self.validate()
        candidate.update(candidate_run_id=RUN_ID, candidate_artifact_id=ARTIFACT_ID)

        class DraftApi:
            repo_path = "/repos/weekssa/OPRA-EQ-for-UAPP"

            def __init__(self, created):
                self.created = created
                self.calls = []

            def optional_json(self, path):
                self.calls.append(("OPTIONAL", path))
                return None

            def json(self, method, path, payload=None):
                self.calls.append((method, path, payload))
                return copy.deepcopy(self.created)

        created = {
            "id": 12,
            "tag_name": TAG,
            "name": f"EQ Library {TAG}",
            "body": "# Notes\n",
            "draft": True,
            "prerelease": False,
            "assets": [],
        }
        api = DraftApi(created)
        with (
            mock.patch.object(promotion, "resolve_candidate_annotated_tag", return_value=SOURCE_SHA),
            mock.patch.object(promotion, "current_main_sha", return_value=SOURCE_SHA),
            mock.patch.object(promotion, "require_release_version_advances"),
        ):
            result = promotion._verify_or_create_draft(api, candidate, "# Notes\n")
        self.assertEqual(created, result)
        self.assertEqual(1, sum(call[0] == "POST" for call in api.calls))

    def test_new_draft_release_metadata_mismatch_fails_before_return(self):
        candidate = self.validate()
        candidate.update(candidate_run_id=RUN_ID, candidate_artifact_id=ARTIFACT_ID)

        class DraftApi:
            repo_path = "/repos/weekssa/OPRA-EQ-for-UAPP"

            def __init__(self, created):
                self.created = created
                self.calls = []

            def optional_json(self, path):
                self.calls.append(("OPTIONAL", path))
                return None

            def json(self, method, path, payload=None):
                self.calls.append((method, path, payload))
                return copy.deepcopy(self.created)

        for field, value, expected in (
            ("name", "Wrong title", "expected release name"),
            ("body", "# Altered notes\n", "expected release body"),
        ):
            with self.subTest(field=field):
                created = {
                    "id": 12,
                    "tag_name": TAG,
                    "name": f"EQ Library {TAG}",
                    "body": "# Notes\n",
                    "draft": True,
                    "prerelease": False,
                    "assets": [],
                }
                created[field] = value
                api = DraftApi(created)
                with (
                    mock.patch.object(promotion, "resolve_candidate_annotated_tag", return_value=SOURCE_SHA),
                    mock.patch.object(promotion, "current_main_sha", return_value=SOURCE_SHA),
                    mock.patch.object(promotion, "require_release_version_advances"),
                ):
                    with self.assertRaisesRegex(promotion.PromotionError, expected):
                        promotion._verify_or_create_draft(api, candidate, "# Notes\n")
                self.assertEqual(1, sum(call[0] == "POST" for call in api.calls))

    def test_tag_command_accepts_prefixed_github_digest_from_verifier(self):
        github_digest = f"sha256:{self.digest}"
        args = argparse.Namespace(
            project_root=self.root,
            tag=TAG,
            candidate_run_id=RUN_ID,
            candidate_artifact_id=ARTIFACT_ID,
            source_sha=SOURCE_SHA,
            artifact_digest=github_digest,
        )
        candidate = {"source_sha": SOURCE_SHA}
        with (
            mock.patch.object(promotion, "check_contract"),
            mock.patch.object(promotion, "_github_api_from_environment", return_value=object()),
            mock.patch.object(
                promotion,
                "validate_github_candidate",
                return_value=(None, None, github_digest),
            ),
            mock.patch.object(promotion, "read_candidate_archive", return_value=self.archive) as read_archive,
            mock.patch.object(promotion, "validate_candidate_archive", return_value=candidate) as validate_archive,
            mock.patch.object(promotion, "create_candidate_tag") as create_tag,
        ):
            promotion.command_tag(args)

        read_archive.assert_called_once_with(mock.ANY, ARTIFACT_ID, github_digest)
        validate_archive.assert_called_once_with(
            self.archive,
            artifact_digest=github_digest,
            tag=TAG,
            source_sha=SOURCE_SHA,
            project_root=self.root,
        )
        create_tag.assert_called_once()

    def test_rejects_candidate_from_another_source_sha(self):
        with self.assertRaisesRegex(promotion.PromotionError, "sourceSha"):
            self.validate(source_sha="d" * 40)

    def test_rejects_checksum_sidecar_for_different_apk_bytes(self):
        files = self.make_files()
        files[f"EQ-Library-{TAG}.apk.sha256"] = ("0" * 64 + f"  dist/EQ-Library-{TAG}.apk\n").encode("ascii")
        archive = self.make_archive(files)
        with self.assertRaisesRegex(promotion.PromotionError, "checksum file"):
            self.validate(archive=archive, digest=hashlib.sha256(archive).hexdigest())

    def test_rejects_unexpected_archive_members(self):
        files = self.make_files()
        files["unexpected.txt"] = b"not part of a release candidate"
        archive = self.make_archive(files)
        with self.assertRaisesRegex(promotion.PromotionError, "file set"):
            self.validate(archive=archive, digest=hashlib.sha256(archive).hexdigest())

    def test_accepts_apksigner_v4_sidecar_but_does_not_publish_it(self):
        files = self.make_files()
        idsig_name = f"EQ-Library-{TAG}.apk.idsig"
        files[idsig_name] = b"v4 signature sidecar fixture"
        archive = self.make_archive(files)
        candidate = self.validate(archive=archive, digest=hashlib.sha256(archive).hexdigest())
        self.assertEqual(files[idsig_name], candidate["files"][idsig_name])

        assets = promotion.release_assets(
            candidate,
            {"apksigner-verification.txt": SIGNER_REPORT, "zipalign-verification.txt": ALIGNMENT_REPORT},
            RUN_ID,
            ARTIFACT_ID,
            "34567",
        )
        self.assertNotIn(idsig_name, assets)
        self.assertEqual(APK, assets[candidate["apk_name"]])

    def test_rejects_empty_apksigner_v4_sidecar(self):
        files = self.make_files()
        files[f"EQ-Library-{TAG}.apk.idsig"] = b""
        archive = self.make_archive(files)
        with self.assertRaisesRegex(promotion.PromotionError, "sidecar is empty"):
            self.validate(archive=archive, digest=hashlib.sha256(archive).hexdigest())

    def test_rejects_ambiguous_duplicate_manifest_fields(self):
        files = self.make_files()
        manifest = files["candidate-manifest.json"].decode("utf-8")
        manifest = manifest.replace("{", '{"sourceSha":"' + SOURCE_SHA + '",', 1)
        files["candidate-manifest.json"] = manifest.encode("utf-8")
        archive = self.make_archive(files)
        with self.assertRaisesRegex(promotion.PromotionError, "duplicate JSON field"):
            self.validate(archive=archive, digest=hashlib.sha256(archive).hexdigest())

    def test_rejects_nested_archive_paths(self):
        files = self.make_files()
        files[f"dist/EQ-Library-{TAG}.apk"] = files.pop(f"EQ-Library-{TAG}.apk")
        archive = self.make_archive(files)
        with self.assertRaisesRegex(promotion.PromotionError, "file set"):
            self.validate(archive=archive, digest=hashlib.sha256(archive).hexdigest())

    def test_rejects_wrong_pinned_signer_in_candidate_report(self):
        files = self.make_files()
        files["apksigner-verification.txt"] = SIGNER_REPORT.replace(SIGNER_COLON.encode("ascii"), b"00:" * 31 + b"00")
        archive = self.make_archive(files)
        with self.assertRaisesRegex(promotion.PromotionError, "signer report"):
            self.validate(archive=archive, digest=hashlib.sha256(archive).hexdigest())

    def test_rejects_missing_v3_signature_evidence(self):
        files = self.make_files()
        files["apksigner-verification.txt"] = SIGNER_REPORT.replace(
            b"Verified using v3 scheme (APK Signature Scheme v3): true\n", b""
        )
        archive = self.make_archive(files)
        with self.assertRaisesRegex(promotion.PromotionError, "v3"):
            self.validate(archive=archive, digest=hashlib.sha256(archive).hexdigest())

    def test_rejects_bad_alignment_evidence(self):
        files = self.make_files()
        files["zipalign-verification.txt"] = b"Verification failed"
        archive = self.make_archive(files)
        with self.assertRaisesRegex(promotion.PromotionError, "alignment report"):
            self.validate(archive=archive, digest=hashlib.sha256(archive).hexdigest())

    def test_rejects_r8_mapping_digest_that_does_not_match_private_mapping_bytes(self):
        files = self.make_files()
        manifest = json.loads(files["candidate-manifest.json"])
        manifest["r8MappingSha256"] = "c" * 64
        files["candidate-manifest.json"] = (json.dumps(manifest) + "\n").encode("utf-8")
        archive = self.make_archive(files)
        with self.assertRaisesRegex(promotion.PromotionError, "mapping bytes"):
            self.validate(archive=archive, digest=hashlib.sha256(archive).hexdigest())

    def test_rejects_r8_mapping_without_a_renamed_application_class(self):
        files = self.make_files()
        files["r8-mapping.txt"] = b"com.weekssa.opraeqforuapp.MainActivity -> com.weekssa.opraeqforuapp.MainActivity:\n"
        manifest = json.loads(files["candidate-manifest.json"])
        manifest["r8MappingSha256"] = hashlib.sha256(files["r8-mapping.txt"]).hexdigest()
        files["candidate-manifest.json"] = (json.dumps(manifest) + "\n").encode("utf-8")
        archive = self.make_archive(files)
        with self.assertRaisesRegex(promotion.PromotionError, "application class was renamed"):
            self.validate(archive=archive, digest=hashlib.sha256(archive).hexdigest())

    def test_public_release_assets_do_not_include_private_r8_mapping(self):
        candidate = self.validate()
        assets = promotion.release_assets(
            candidate,
            {"apksigner-verification.txt": SIGNER_REPORT, "zipalign-verification.txt": ALIGNMENT_REPORT},
            RUN_ID,
            ARTIFACT_ID,
            "34567",
        )
        self.assertNotIn("r8-mapping.txt", assets)

    def test_rejects_tag_that_disagrees_with_app_version(self):
        (self.root / "app/build.gradle.kts").write_text(
            'android {\n    defaultConfig {\n        versionCode = 10\n        versionName = "0.8.0"\n    }\n}\n',
            encoding="utf-8",
        )
        with self.assertRaisesRegex(promotion.PromotionError, "versionName"):
            self.validate()

    def test_candidate_validation_accepts_a_future_gradle_version_without_workflow_edits(self):
        tag = "v4.12.0"
        version_name = tag[1:]
        version_code = 412
        (self.root / "app/build.gradle.kts").write_text(
            f'android {{\n    defaultConfig {{\n        versionCode = {version_code}\n        versionName = "{version_name}"\n    }}\n}}\n',
            encoding="utf-8",
        )
        (self.root / "docs/releases" / f"{tag}.md").write_text(
            f"# EQ Library {tag}\n\nSynthetic future version fixture.\n", encoding="utf-8"
        )
        files = self.make_files()
        old_apk_name = f"EQ-Library-{TAG}.apk"
        new_apk_name = f"EQ-Library-{tag}.apk"
        files[new_apk_name] = files.pop(old_apk_name)
        old_checksum_name = f"{old_apk_name}.sha256"
        files.pop(old_checksum_name)
        manifest = json.loads(files["candidate-manifest.json"])
        manifest.update({"releaseTag": tag, "apk": new_apk_name, "versionName": version_name,
                         "versionCode": version_code})
        files["candidate-manifest.json"] = (json.dumps(manifest) + "\n").encode("utf-8")
        apk_sha = hashlib.sha256(APK).hexdigest()
        files[f"{new_apk_name}.sha256"] = f"{apk_sha}  {new_apk_name}\n".encode("ascii")
        archive = self.make_archive(files)
        candidate = promotion.validate_candidate_archive(
            archive,
            artifact_digest=hashlib.sha256(archive).hexdigest(),
            tag=tag,
            source_sha=SOURCE_SHA,
            project_root=self.root,
        )
        self.assertEqual(version_name, candidate["manifest"]["versionName"])
        self.assertEqual(version_code, candidate["version_code"])

    def test_strict_semver_parser_rejects_ambiguous_tags(self):
        for tag in ("0.7.2", "v00.7.2", "v0.7", "v0.7.2-beta.1", "v0.7.2/other"):
            with self.subTest(tag=tag), self.assertRaises(promotion.PromotionError):
                promotion.parse_tag(tag)

    def test_release_assets_keep_apk_bytes_and_make_downloadable_checksum_filename(self):
        candidate = self.validate()
        logs = {
            "apksigner-verification.txt": SIGNER_REPORT,
            "zipalign-verification.txt": ALIGNMENT_REPORT,
        }
        assets = promotion.release_assets(candidate, logs, RUN_ID, ARTIFACT_ID, "34567")
        apk_name = candidate["apk_name"]
        self.assertEqual(APK, assets[apk_name])
        self.assertEqual(
            f"{candidate['apk_sha256']}  {apk_name}\n".encode("ascii"),
            assets[f"{apk_name}.sha256"],
        )
        provenance = json.loads(assets["release-provenance.json"].decode("utf-8"))
        self.assertEqual(SOURCE_SHA, provenance["releaseSourceSha"])
        self.assertEqual(ARTIFACT_ID, provenance["candidateArtifactId"])
        self.assertEqual(candidate["artifact_digest"], provenance["candidateArtifactSha256"])

    def test_candidate_run_must_be_successful_main_workflow_run_and_current_head(self):
        workflow = {"id": 91, "path": promotion.CANDIDATE_WORKFLOW_PATH, "state": "active"}
        run = {
            "id": RUN_ID,
            "workflow_id": 91,
            "status": "completed",
            "conclusion": "success",
            "event": "workflow_dispatch",
            "head_branch": "main",
            "head_sha": SOURCE_SHA,
            "head_repository": {"full_name": promotion.REPOSITORY},
        }
        artifact = {
            "id": ARTIFACT_ID,
            "expired": False,
            "size_in_bytes": 123,
            "name": f"EQ-Library-{TAG}-signed-{SOURCE_SHA}",
            "digest": f"sha256:{self.digest}",
            "workflow_run": {"id": RUN_ID, "head_sha": SOURCE_SHA, "head_branch": "main"},
        }
        responses = {
            ("GET", "/repos/weekssa/OPRA-EQ-for-UAPP/actions/workflows/github-release.yml"): workflow,
            ("GET", f"/repos/weekssa/OPRA-EQ-for-UAPP/actions/runs/{RUN_ID}"): run,
            ("GET", "/repos/weekssa/OPRA-EQ-for-UAPP/branches/main"): {"commit": {"sha": SOURCE_SHA}},
            ("GET", f"/repos/weekssa/OPRA-EQ-for-UAPP/actions/artifacts/{ARTIFACT_ID}"): artifact,
        }
        api = FakeApi(responses)
        actual_run, actual_artifact, digest = promotion.validate_github_candidate(
            api, tag=TAG, run_id=RUN_ID, artifact_id=ARTIFACT_ID, source_sha=SOURCE_SHA
        )
        self.assertEqual(run, actual_run)
        self.assertEqual(artifact, actual_artifact)
        self.assertEqual(f"sha256:{self.digest}", digest)

    def test_candidate_metadata_rejects_wrong_workflow_branch_run_state_and_artifact_owner(self):
        workflow = {"id": 91, "path": promotion.CANDIDATE_WORKFLOW_PATH, "state": "active"}
        base_run = {
            "id": RUN_ID,
            "workflow_id": 91,
            "status": "completed",
            "conclusion": "success",
            "event": "workflow_dispatch",
            "head_branch": "main",
            "head_sha": SOURCE_SHA,
            "head_repository": {"full_name": promotion.REPOSITORY},
        }
        base_artifact = {
            "id": ARTIFACT_ID,
            "expired": False,
            "size_in_bytes": 123,
            "name": f"EQ-Library-{TAG}-signed-{SOURCE_SHA}",
            "digest": f"sha256:{self.digest}",
            "workflow_run": {"id": RUN_ID, "head_sha": SOURCE_SHA, "head_branch": "main"},
        }
        cases = [
            ("wrong branch", lambda run, artifact: run.update(head_branch="feature"), "manually dispatched from main"),
            ("wrong status", lambda run, artifact: run.update(conclusion="failure"), "complete successfully"),
            ("wrong workflow", lambda run, artifact: run.update(workflow_id=999), "signed release candidate workflow"),
            ("wrong artifact run", lambda run, artifact: artifact["workflow_run"].update(id=999), "not attached"),
            ("expired artifact", lambda run, artifact: artifact.update(expired=True), "expired"),
            ("wrong artifact name", lambda run, artifact: artifact.update(name="other"), "artifact name"),
        ]
        for label, mutate, error in cases:
            with self.subTest(label=label):
                run = copy.deepcopy(base_run)
                artifact = copy.deepcopy(base_artifact)
                mutate(run, artifact)
                responses = {
                    ("GET", "/repos/weekssa/OPRA-EQ-for-UAPP/actions/workflows/github-release.yml"): workflow,
                    ("GET", f"/repos/weekssa/OPRA-EQ-for-UAPP/actions/runs/{RUN_ID}"): run,
                    ("GET", "/repos/weekssa/OPRA-EQ-for-UAPP/branches/main"): {"commit": {"sha": SOURCE_SHA}},
                    ("GET", f"/repos/weekssa/OPRA-EQ-for-UAPP/actions/artifacts/{ARTIFACT_ID}"): artifact,
                }
                with self.assertRaisesRegex(promotion.PromotionError, error):
                    promotion.validate_github_candidate(
                        FakeApi(responses), tag=TAG, run_id=RUN_ID, artifact_id=ARTIFACT_ID, source_sha=SOURCE_SHA
                    )

    def test_repository_workflow_and_documentation_contract(self):
        root = Path(__file__).resolve().parents[1]
        promotion.check_contract(root)

    def test_candidate_tag_message_binds_source_run_artifact_and_digest(self):
        message = promotion.candidate_tag_message(TAG, SOURCE_SHA, RUN_ID, ARTIFACT_ID, self.digest)
        self.assertIn(f"Release-Tag: {TAG}\n", message)
        self.assertIn(f"Source-SHA: {SOURCE_SHA}\n", message)
        self.assertIn(f"Candidate-Run-ID: {RUN_ID}\n", message)
        self.assertIn(f"Candidate-Artifact-ID: {ARTIFACT_ID}\n", message)
        self.assertIn(f"Candidate-Artifact-SHA256: {self.digest}\n", message)

    def test_creates_exact_annotated_candidate_tag_after_version_gate(self):
        class TagApi:
            repo_path = "/repos/weekssa/OPRA-EQ-for-UAPP"

            def __init__(self):
                self.ref = None
                self.tag_object = None
                self.calls = []

            def optional_json(self, path):
                if path.endswith(f"/git/ref/tags/{TAG}"):
                    return copy.deepcopy(self.ref)
                if path.endswith(f"/releases/tags/{TAG}"):
                    return None
                if path.endswith("/releases/latest"):
                    return {"tag_name": "v0.7.1"}
                raise AssertionError(path)

            def json(self, method, path, payload=None):
                self.calls.append((method, path, payload))
                if method == "GET" and path.endswith("/branches/main"):
                    return {"commit": {"sha": SOURCE_SHA}}
                if method == "POST" and path.endswith("/git/tags"):
                    self.tag_object = {
                        "sha": "f" * 40,
                        "tag": payload["tag"],
                        "message": payload["message"],
                        "object": {"type": "commit", "sha": payload["object"]},
                    }
                    self.assert_tag_payload = copy.deepcopy(payload)
                    return copy.deepcopy(self.tag_object)
                if method == "POST" and path.endswith("/git/refs"):
                    self.ref = {"object": {"type": "tag", "sha": payload["sha"]}}
                    return copy.deepcopy(self.ref)
                if method == "GET" and path.endswith(f"/git/tags/{'f' * 40}"):
                    return copy.deepcopy(self.tag_object)
                raise AssertionError((method, path, payload))

        api = TagApi()
        with mock.patch.object(promotion, "current_main_sha", return_value=SOURCE_SHA):
            promotion.create_candidate_tag(
                api,
                tag=TAG,
                source_sha=SOURCE_SHA,
                run_id=RUN_ID,
                artifact_id=ARTIFACT_ID,
                artifact_digest=self.digest,
                release_notes="# Release notes\n",
            )
        self.assertEqual("commit", api.assert_tag_payload["type"])
        self.assertEqual(SOURCE_SHA, api.assert_tag_payload["object"])
        self.assertEqual("github-actions[bot]", api.assert_tag_payload["tagger"]["name"])
        self.assertEqual("tag", api.ref["object"]["type"])
        self.assertEqual(1, sum(call[0] == "POST" and call[1].endswith("/git/tags") for call in api.calls))
        self.assertEqual(1, sum(call[0] == "POST" and call[1].endswith("/git/refs") for call in api.calls))
        self.assertFalse(any("force" in str(call[2]).lower() for call in api.calls if call[2]))

    def test_exact_annotated_candidate_tag_retry_is_idempotent(self):
        message = promotion.candidate_tag_message(TAG, SOURCE_SHA, RUN_ID, ARTIFACT_ID, self.digest)
        tag_object = {
            "sha": "f" * 40,
            "tag": TAG,
            "message": message,
            "object": {"type": "commit", "sha": SOURCE_SHA},
        }

        class TagApi:
            repo_path = "/repos/weekssa/OPRA-EQ-for-UAPP"

            def __init__(self):
                self.calls = []

            def optional_json(self, path):
                if path.endswith(f"/git/ref/tags/{TAG}"):
                    return {"object": {"type": "tag", "sha": "f" * 40}}
                if path.endswith(f"/releases/tags/{TAG}"):
                    return None
                raise AssertionError(path)

            def json(self, method, path, payload=None):
                self.calls.append((method, path, payload))
                if method == "GET" and path.endswith("/branches/main"):
                    return {"commit": {"sha": SOURCE_SHA}}
                if method == "GET" and path.endswith(f"/git/tags/{'f' * 40}"):
                    return copy.deepcopy(tag_object)
                raise AssertionError((method, path, payload))

        api = TagApi()
        with mock.patch.object(promotion, "current_main_sha", return_value=SOURCE_SHA):
            promotion.create_candidate_tag(
                api,
                tag=TAG,
                source_sha=SOURCE_SHA,
                run_id=RUN_ID,
                artifact_id=ARTIFACT_ID,
                artifact_digest=self.digest,
                release_notes="# Release notes\n",
            )
        self.assertFalse(any(call[0] == "POST" for call in api.calls))

    def test_candidate_tag_rejects_lightweight_and_mismatched_annotated_tags(self):
        message = promotion.candidate_tag_message(TAG, SOURCE_SHA, RUN_ID, ARTIFACT_ID, self.digest)
        cases = [
            ("lightweight", {"type": "commit", "sha": SOURCE_SHA}, None,
             "lightweight tag"),
            ("wrong candidate tuple", {"type": "tag", "sha": "f" * 40},
             {"tag": TAG, "message": message.replace(f"Candidate-Artifact-ID: {ARTIFACT_ID}", "Candidate-Artifact-ID: 7"),
              "object": {"type": "commit", "sha": SOURCE_SHA}}, "does not bind"),
            ("wrong source", {"type": "tag", "sha": "f" * 40},
             {"tag": TAG, "message": message, "object": {"type": "commit", "sha": "b" * 40}}, "does not bind"),
        ]
        for label, ref_object, tag_object, expected_error in cases:
            with self.subTest(label=label):
                class TagApi:
                    repo_path = "/repos/weekssa/OPRA-EQ-for-UAPP"

                    def optional_json(self, _path):
                        return {"object": ref_object}

                    def json(self, _method, _path, _payload=None):
                        return tag_object

                with self.assertRaisesRegex(promotion.PromotionError, expected_error):
                    promotion.resolve_candidate_annotated_tag(
                        TagApi(), tag=TAG, source_sha=SOURCE_SHA, run_id=RUN_ID,
                        artifact_id=ARTIFACT_ID, artifact_digest=self.digest,
                    )

    def test_draft_version_cannot_be_tagged_before_candidate_tag_exists(self):
        class TagApi:
            repo_path = "/repos/weekssa/OPRA-EQ-for-UAPP"

            def optional_json(self, path):
                if path.endswith(f"/git/ref/tags/{TAG}"):
                    return None
                if path.endswith(f"/releases/tags/{TAG}"):
                    return {"draft": True, "tag_name": TAG, "assets": []}
                raise AssertionError(path)

            def json(self, method, path, payload=None):
                if method == "GET" and path.endswith("/branches/main"):
                    return {"commit": {"sha": SOURCE_SHA}}
                raise AssertionError((method, path, payload))

        with mock.patch.object(promotion, "current_main_sha", return_value=SOURCE_SHA):
            with self.assertRaisesRegex(promotion.PromotionError, "release already uses this version"):
                promotion.create_candidate_tag(
                    TagApi(), tag=TAG, source_sha=SOURCE_SHA, run_id=RUN_ID,
                    artifact_id=ARTIFACT_ID, artifact_digest=self.digest,
                    release_notes="# Release notes\n",
                )

    def test_candidate_tag_creation_requires_version_to_advance_latest_public_release(self):
        class TagApi:
            repo_path = "/repos/weekssa/OPRA-EQ-for-UAPP"
            calls = []

            def optional_json(self, path):
                if path.endswith(f"/git/ref/tags/{TAG}") or path.endswith(f"/releases/tags/{TAG}"):
                    return None
                if path.endswith("/releases/latest"):
                    return {"tag_name": "v0.8.0"}
                raise AssertionError(path)

            def json(self, method, path, payload=None):
                self.calls.append((method, path, payload))
                raise AssertionError((method, path, payload))

        api = TagApi()
        with mock.patch.object(promotion, "current_main_sha", return_value=SOURCE_SHA):
            with self.assertRaisesRegex(promotion.PromotionError, "does not advance"):
                promotion.create_candidate_tag(
                    api, tag=TAG, source_sha=SOURCE_SHA, run_id=RUN_ID,
                    artifact_id=ARTIFACT_ID, artifact_digest=self.digest,
                    release_notes="# Release notes\n",
                )
        self.assertEqual([], api.calls)

    def test_existing_private_draft_resumes_only_for_exact_annotated_candidate_tag(self):
        candidate = self.validate()
        candidate.update(candidate_run_id=RUN_ID, candidate_artifact_id=ARTIFACT_ID)
        draft = {
            "id": 12,
            "tag_name": TAG,
            "name": f"EQ Library {TAG}",
            "body": "# Release notes\n",
            "target_commitish": SOURCE_SHA,
            "draft": True,
            "prerelease": False,
            "assets": [],
        }

        class DraftApi:
            repo_path = "/repos/weekssa/OPRA-EQ-for-UAPP"

            def optional_json(self, _path):
                return draft

        with (
            mock.patch.object(promotion, "resolve_candidate_annotated_tag", return_value=SOURCE_SHA),
            mock.patch.object(promotion, "current_main_sha", return_value=SOURCE_SHA),
        ):
            result = promotion._verify_or_create_draft(DraftApi(), candidate, "# Release notes\n")
        self.assertIs(result, draft)

    def test_existing_tag_source_is_authoritative_over_unused_release_target_commitish(self):
        candidate = self.validate()
        candidate.update(candidate_run_id=RUN_ID, candidate_artifact_id=ARTIFACT_ID)
        draft = {
            "id": 12,
            "tag_name": TAG,
            "name": f"EQ Library {TAG}",
            "body": "# Release notes\n",
            "target_commitish": "b" * 40,
            "draft": True,
            "prerelease": False,
            "assets": [],
        }

        class DraftApi:
            repo_path = "/repos/weekssa/OPRA-EQ-for-UAPP"

            def optional_json(self, _path):
                return draft

        with (
            mock.patch.object(promotion, "resolve_candidate_annotated_tag", return_value=SOURCE_SHA),
            mock.patch.object(promotion, "current_main_sha", return_value=SOURCE_SHA),
        ):
            result = promotion._verify_or_create_draft(DraftApi(), candidate, "# Release notes\n")
        self.assertIs(result, draft)

    def test_emulator_upgrade_uses_outputs_from_same_job_verification_step(self):
        workflow = (Path(__file__).resolve().parents[1] / ".github/workflows/promote-signed-release.yml").read_text(
            encoding="utf-8"
        )
        upgrade_step = workflow.split(
            "- name: Install exact candidate over the latest public release on API 35\n", 1
        )[1].split("\n      - name: Upload promotion emulator diagnostics", 1)[0]
        self.assertNotIn("${{ needs.verify-candidate.outputs.", upgrade_step)
        for output in ("baseline_version_code", "candidate_version_code", "candidate_version_name"):
            with self.subTest(output=output):
                self.assertIn(f"${{{{ steps.verify.outputs.{output} }}}}", upgrade_step)

    def test_latest_release_baseline_is_verified_before_emulator_upgrade(self):
        candidate = self.validate()
        apk_sha = hashlib.sha256(APK).hexdigest()
        test_case = self

        class BaselineApi:
            api_url = "https://api.github.com"
            repo_path = "/repos/weekssa/OPRA-EQ-for-UAPP"

            def json(self, method, path, payload=None):
                test_case.assertEqual("GET", method)
                test_case.assertEqual(self.repo_path + "/releases/latest", path)
                return {
                    "tag_name": "v0.7.1",
                    "draft": False,
                    "prerelease": False,
                    "assets": [{
                        "id": 98,
                        "name": "EQ-Library-v0.7.1.apk",
                        "state": "uploaded",
                        "digest": f"sha256:{apk_sha}",
                    }],
                }

            def request_bytes(self, url, **kwargs):
                test_case.assertTrue(url.endswith("/releases/assets/98"))
                test_case.assertEqual("application/octet-stream", kwargs["accept"])
                return 200, APK, {}

        api = BaselineApi()
        with tempfile.TemporaryDirectory(prefix="upgrade-baseline-test-") as temporary:
            output = Path(temporary) / "baseline.apk"
            with mock.patch.object(
                promotion,
                "verify_android_apk",
                return_value=({"zipalign-verification.txt": ALIGNMENT_REPORT}, 8),
            ) as verify:
                summary = promotion.download_and_verify_upgrade_baseline(api, candidate, Path("unused"), output)
            verify.assert_called_once()
            self.assertEqual(APK, output.read_bytes())
            self.assertEqual("v0.7.1", summary["baseline_tag"])
            self.assertEqual(apk_sha, summary["baseline_apk_sha256"])

    def test_publish_does_not_publish_until_every_asset_readback_passes(self):
        candidate = self.validate()
        logs = {
            "apksigner-verification.txt": SIGNER_REPORT,
            "zipalign-verification.txt": ALIGNMENT_REPORT,
        }
        assets = promotion.release_assets(candidate, logs, RUN_ID, ARTIFACT_ID, "34567")
        source_sha = candidate["source_sha"]
        draft = {"id": 12, "tag_name": TAG, "target_commitish": source_sha,
                 "draft": True, "prerelease": False, "assets": []}
        final = {
            "id": 12,
            "name": f"EQ Library {TAG}",
            "body": "# Notes\n",
            "tag_name": TAG,
            "draft": False,
            "prerelease": False,
            "html_url": f"https://github.com/{promotion.REPOSITORY}/releases/tag/{TAG}",
            "assets_url": f"https://api.github.com/repos/{promotion.REPOSITORY}/releases/12/assets",
        }

        class PublishApi:
            repo_path = "/repos/weekssa/OPRA-EQ-for-UAPP"

            def __init__(self):
                self.calls = []

            def json(self, method, path, payload=None):
                self.calls.append((method, path, payload))
                if method == "PATCH":
                    return {"id": 12, "tag_name": TAG, "draft": False}
                if path.endswith("/releases/latest"):
                    return {"tag_name": TAG}
                return final

        api = PublishApi()
        verified_assets = {name: {"name": name} for name in assets}
        with (
            mock.patch.object(promotion, "_verify_or_create_draft", return_value=draft),
            mock.patch.object(promotion, "_upload_asset"),
            mock.patch.object(promotion, "_verify_release_assets", side_effect=[verified_assets, verified_assets]),
            mock.patch.object(promotion, "_verify_public_asset_downloads"),
            mock.patch.object(promotion, "resolve_candidate_annotated_tag", side_effect=[source_sha, source_sha]),
            mock.patch.object(promotion, "current_main_sha", return_value=source_sha),
            mock.patch.object(promotion, "require_release_version_advances"),
        ):
            result = promotion.publish_release(api, candidate=candidate, assets=assets, release_notes="# Notes\n")
        self.assertFalse(result["draft"])
        mutations = [call for call in api.calls if call[0] == "PATCH"]
        self.assertEqual(1, len(mutations))
        self.assertIs(mutations[0][2]["draft"], False)
        self.assertNotIn("target_commitish", mutations[0][2])
        self.assertNotIn("tag_name", mutations[0][2])
        self.assertEqual("true", mutations[0][2]["make_latest"])
        self.assertIn(("GET", api.repo_path + "/releases/latest", None), api.calls)

    def test_publish_rejects_published_release_metadata_mismatch(self):
        candidate = self.validate()
        logs = {
            "apksigner-verification.txt": SIGNER_REPORT,
            "zipalign-verification.txt": ALIGNMENT_REPORT,
        }
        assets = promotion.release_assets(candidate, logs, RUN_ID, ARTIFACT_ID, "34567")
        source_sha = candidate["source_sha"]
        draft = {"id": 12, "tag_name": TAG, "target_commitish": source_sha,
                 "draft": True, "prerelease": False, "assets": []}

        for field, value, expected in (
            ("name", "Wrong title", "expected release name"),
            ("body", "# Altered notes\n", "expected release body"),
        ):
            with self.subTest(field=field):
                final = {
                    "id": 12,
                    "name": f"EQ Library {TAG}",
                    "body": "# Notes\n",
                    "tag_name": TAG,
                    "draft": False,
                    "prerelease": False,
                }
                final[field] = value

                class PublishApi:
                    repo_path = "/repos/weekssa/OPRA-EQ-for-UAPP"

                    def __init__(self):
                        self.calls = []

                    def json(self, method, path, payload=None):
                        self.calls.append((method, path, payload))
                        if method == "PATCH":
                            return {"id": 12, "tag_name": TAG, "draft": False}
                        return final

                api = PublishApi()
                verified_assets = {name: {"name": name} for name in assets}
                with (
                    mock.patch.object(promotion, "_verify_or_create_draft", return_value=draft),
                    mock.patch.object(promotion, "_upload_asset"),
                    mock.patch.object(promotion, "_verify_release_assets", return_value=verified_assets),
                    mock.patch.object(promotion, "_verify_public_asset_downloads"),
                    mock.patch.object(promotion, "resolve_candidate_annotated_tag",
                                      side_effect=[source_sha, source_sha]),
                    mock.patch.object(promotion, "current_main_sha", return_value=source_sha),
                    mock.patch.object(promotion, "require_release_version_advances"),
                ):
                    with self.assertRaisesRegex(promotion.PromotionError, expected):
                        promotion.publish_release(api, candidate=candidate, assets=assets,
                                                  release_notes="# Notes\n")

                self.assertEqual(1, sum(call[0] == "PATCH" for call in api.calls))
                self.assertFalse(any(call[1].endswith("/releases/latest") for call in api.calls))

    def test_publish_stops_before_mutation_when_asset_set_is_incomplete(self):
        candidate = self.validate()
        assets = promotion.release_assets(
            candidate,
            {"apksigner-verification.txt": SIGNER_REPORT, "zipalign-verification.txt": ALIGNMENT_REPORT},
            RUN_ID,
            ARTIFACT_ID,
            "34567",
        )
        source_sha = candidate["source_sha"]
        draft = {"id": 12, "tag_name": TAG, "target_commitish": source_sha,
                 "draft": True, "prerelease": False, "assets": []}

        class PublishApi:
            calls = []

            def json(self, method, path, payload=None):
                self.calls.append((method, path, payload))
                return {}

        api = PublishApi()
        incomplete = {name: {"name": name} for name in assets if name != "zipalign-verification.txt"}
        with (
            mock.patch.object(promotion, "_verify_or_create_draft", return_value=draft),
            mock.patch.object(promotion, "_upload_asset"),
            mock.patch.object(promotion, "_verify_release_assets", return_value=incomplete),
            mock.patch.object(promotion, "resolve_candidate_annotated_tag", return_value=source_sha),
            mock.patch.object(promotion, "current_main_sha", return_value=source_sha),
            mock.patch.object(promotion, "require_release_version_advances"),
        ):
            with self.assertRaisesRegex(promotion.PromotionError, "asset set is incomplete"):
                promotion.publish_release(api, candidate=candidate, assets=assets, release_notes="# Notes\n")
        self.assertFalse(any(call[0] == "PATCH" for call in api.calls))

    def test_publish_retry_preserves_verified_provenance_from_partial_draft(self):
        candidate = self.validate()
        logs = {
            "apksigner-verification.txt": SIGNER_REPORT,
            "zipalign-verification.txt": ALIGNMENT_REPORT,
        }
        first_assets = promotion.release_assets(candidate, logs, RUN_ID, ARTIFACT_ID, "34567")
        retry_assets = promotion.release_assets(candidate, logs, RUN_ID, ARTIFACT_ID, "45678")
        prior_provenance = first_assets["release-provenance.json"]
        draft = {
            "id": 12,
            "tag_name": TAG,
            "target_commitish": candidate["source_sha"],
            "draft": True,
            "prerelease": False,
            "assets": [{
                "id": 99,
                "name": "release-provenance.json",
                "state": "uploaded",
                "size": len(prior_provenance),
                "digest": promotion.asset_digest(prior_provenance),
            }],
        }
        final = {
            "id": 12,
            "name": f"EQ Library {TAG}",
            "body": "# Notes\n",
            "tag_name": TAG,
            "target_commitish": candidate["source_sha"],
            "draft": False,
            "prerelease": False,
            "html_url": f"https://github.com/{promotion.REPOSITORY}/releases/tag/{TAG}",
            "assets_url": f"https://api.github.com/repos/{promotion.REPOSITORY}/releases/12/assets",
        }

        class RetryApi:
            api_url = "https://api.github.com"
            repo_path = "/repos/weekssa/OPRA-EQ-for-UAPP"
            test_case = self

            def __init__(self):
                self.calls = []

            def json(self, method, path, payload=None):
                self.calls.append((method, path, payload))
                if method == "PATCH":
                    return {"id": 12, "tag_name": TAG, "draft": False}
                if path.endswith("/releases/latest"):
                    return {"tag_name": TAG}
                return final

            def request_bytes(self, url, **kwargs):
                self.test_case.assertTrue(url.endswith("/releases/assets/99"))
                self.test_case.assertEqual("application/octet-stream", kwargs["accept"])
                return 200, prior_provenance, {}

        api = RetryApi()
        verified = []

        def verify_assets(_api, _release, expected):
            verified.append(dict(expected))
            return {name: {"id": 100 + index, "name": name}
                    for index, name in enumerate(expected)}

        with (
            mock.patch.object(promotion, "_verify_or_create_draft", return_value=draft),
            mock.patch.object(promotion, "_upload_asset") as upload,
            mock.patch.object(promotion, "_verify_release_assets", side_effect=verify_assets),
            mock.patch.object(promotion, "_verify_public_asset_downloads"),
            mock.patch.object(promotion, "resolve_candidate_annotated_tag", return_value=candidate["source_sha"]),
            mock.patch.object(promotion, "current_main_sha", return_value=candidate["source_sha"]),
            mock.patch.object(promotion, "require_release_version_advances"),
        ):
            result = promotion.publish_release(api, candidate=candidate, assets=retry_assets,
                                               release_notes="# Notes\n")

        self.assertFalse(result["draft"])
        self.assertEqual(prior_provenance, retry_assets["release-provenance.json"])
        self.assertNotIn("release-provenance.json", [call.args[2] for call in upload.call_args_list])
        self.assertEqual(set(first_assets) - {"release-provenance.json"},
                         {call.args[2] for call in upload.call_args_list})
        self.assertEqual(3, len(verified))
        self.assertEqual(prior_provenance, verified[0]["release-provenance.json"])
        self.assertEqual(1, sum(call[0] == "PATCH" for call in api.calls))

    def test_publish_retry_rejects_partial_draft_provenance_for_another_source(self):
        candidate = self.validate()
        provenance = json.loads(
            promotion.provenance_bytes(candidate, RUN_ID, ARTIFACT_ID, "34567").decode("utf-8")
        )
        provenance["releaseSourceSha"] = "c" * 40
        contents = (json.dumps(provenance, sort_keys=True) + "\n").encode("utf-8")
        draft = {
            "id": 12,
            "tag_name": TAG,
            "draft": True,
            "prerelease": False,
            "assets": [{
                "id": 99,
                "name": "release-provenance.json",
                "state": "uploaded",
                "size": len(contents),
                "digest": promotion.asset_digest(contents),
            }],
        }

        class RetryApi:
            api_url = "https://api.github.com"
            repo_path = "/repos/weekssa/OPRA-EQ-for-UAPP"

            def request_bytes(self, _url, **_kwargs):
                return 200, contents, {}

        assets = promotion.release_assets(
            candidate,
            {"apksigner-verification.txt": SIGNER_REPORT, "zipalign-verification.txt": ALIGNMENT_REPORT},
            RUN_ID,
            ARTIFACT_ID,
            "45678",
        )
        with self.assertRaisesRegex(promotion.PromotionError, "does not match this exact signed candidate"):
            promotion._preserve_matching_provenance(RetryApi(), draft, candidate, assets)


if __name__ == "__main__":
    unittest.main()
