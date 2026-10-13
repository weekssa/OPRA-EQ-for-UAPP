import hashlib
import io
import json
import sys
import tempfile
import unittest
import zipfile
from pathlib import Path
from unittest import mock

import verify_signed_candidate_smoke as verifier


sys.path.insert(0, str(Path(__file__).resolve().parent))
import promote_release_candidate as promotion


SOURCE_SHA = "a" * 40
RUN_ID = 23456
ARTIFACT_ID = 12345
TAG = "v0.8.2"
SIGNER = "65c1c1256dae3c49e3548f334c91f0ba991969e9be9e0b223ba4e253d2114747"
SIGNER_COLON = ":".join(SIGNER[index:index + 2] for index in range(0, 64, 2))
SIGNER_REPORT = f"""Verifies
Verified using v2 scheme (APK Signature Scheme v2): true
Verified using v3 scheme (APK Signature Scheme v3): true
Number of signers: 1
Signer #1 certificate SHA-256 digest: {SIGNER_COLON}
""".encode("utf-8")
ALIGNMENT_REPORT = b"Verifying alignment of EQ-Library-v0.8.2.apk (4)...\nVerification successful\n"
R8_MAPPING = b"com.weekssa.opraeqforuapp.MainActivity -> a.b:\n"


def make_apk(source_sha=SOURCE_SHA, fallback=False):
    stream = io.BytesIO()
    with zipfile.ZipFile(stream, "w", compression=zipfile.ZIP_DEFLATED) as apk:
        dex = b"fixture\0Signed candidate source: " + source_sha.encode("ascii")
        if fallback:
            dex += b"\0local-unqualified"
        apk.writestr("classes.dex", dex)
    return stream.getvalue()


class SignedCandidateSmokeVerifierTest(unittest.TestCase):
    def setUp(self):
        self.temp = tempfile.TemporaryDirectory(prefix="signed-candidate-smoke-test-")
        self.root = Path(self.temp.name)
        (self.root / "app").mkdir()
        (self.root / "docs/releases").mkdir(parents=True)
        (self.root / "app/build.gradle.kts").write_text(
            'android {\n    defaultConfig {\n        versionCode = 13\n        versionName = "0.8.2"\n    }\n}\n',
            encoding="utf-8",
        )
        (self.root / "release-signing-cert.sha256").write_text(SIGNER + "\n", encoding="utf-8")
        (self.root / "docs/releases/v0.8.2.md").write_text("# EQ Library v0.8.2\n\nCandidate.\n", encoding="utf-8")
        apk_name = f"EQ-Library-{TAG}.apk"
        apk_bytes = make_apk()
        apk_sha = hashlib.sha256(apk_bytes).hexdigest()
        manifest = {
            "sourceSha": SOURCE_SHA,
            "releaseTag": TAG,
            "apk": apk_name,
            "apkSha256": apk_sha,
            "packageId": promotion.PACKAGE_ID,
            "versionName": "0.8.2",
            "versionCode": 13,
            "signerSha256": SIGNER,
            "r8MinificationEnabled": True,
            "r8MappingSha256": hashlib.sha256(R8_MAPPING).hexdigest(),
        }
        files = {
            apk_name: apk_bytes,
            f"{apk_name}.sha256": f"{apk_sha}  dist/{apk_name}\n".encode("ascii"),
            "candidate-manifest.json": (json.dumps(manifest) + "\n").encode("utf-8"),
            "apksigner-verification.txt": SIGNER_REPORT,
            "zipalign-verification.txt": ALIGNMENT_REPORT,
            "r8-mapping.txt": R8_MAPPING,
        }
        stream = io.BytesIO()
        with zipfile.ZipFile(stream, "w", compression=zipfile.ZIP_DEFLATED) as archive:
            for name, contents in files.items():
                archive.writestr(name, contents)
        self.archive = stream.getvalue()
        self.digest = hashlib.sha256(self.archive).hexdigest()

    def tearDown(self):
        self.temp.cleanup()

    @staticmethod
    def official_workflow():
        return {"id": 77, "path": verifier.CANDIDATE_WORKFLOW_PATH, "state": "active"}

    @staticmethod
    def official_run():
        return {
            "id": RUN_ID,
            "workflow_id": 77,
            "status": "completed",
            "conclusion": "success",
            "event": "workflow_dispatch",
            "head_branch": "main",
            "head_sha": SOURCE_SHA,
            "head_repository": {"full_name": verifier.REPOSITORY},
        }

    def official_artifact(self):
        return {
            "id": ARTIFACT_ID,
            "expired": False,
            "size_in_bytes": len(self.archive),
            "name": f"EQ-Library-{TAG}-signed-{SOURCE_SHA}",
            "workflow_run": {"id": RUN_ID, "head_sha": SOURCE_SHA, "head_branch": "main"},
            "digest": f"sha256:{self.digest}",
        }

    def test_accepts_only_successful_official_main_dispatch_for_exact_source(self):
        verifier.validate_run_metadata(
            self.official_workflow(), self.official_run(), source_sha=SOURCE_SHA, run_id=RUN_ID,
        )
        for field, value in (
            ("path", ".github/workflows/promote-signed-release.yml"),
            ("state", "disabled_manually"),
        ):
            workflow = self.official_workflow()
            workflow[field] = value
            with self.subTest(workflow_field=field), self.assertRaises(ValueError):
                verifier.validate_run_metadata(workflow, self.official_run(), source_sha=SOURCE_SHA, run_id=RUN_ID)
        for field, value in (
            ("conclusion", "failure"),
            ("event", "push"),
            ("head_branch", "codex/untrusted"),
            ("head_sha", "b" * 40),
            ("head_repository", {"full_name": "attacker/OPRA-EQ-for-UAPP"}),
        ):
            run = self.official_run()
            run[field] = value
            with self.subTest(run_field=field), self.assertRaises(ValueError):
                verifier.validate_run_metadata(self.official_workflow(), run, source_sha=SOURCE_SHA, run_id=RUN_ID)

    def test_artifact_must_match_run_source_name_identity_and_digest(self):
        artifact = self.official_artifact()
        self.assertEqual(artifact["digest"], verifier.validate_artifact_metadata(
            artifact, tag=TAG, source_sha=SOURCE_SHA, run_id=RUN_ID, artifact_id=ARTIFACT_ID,
        ))
        bad_cases = (
            {"id": ARTIFACT_ID + 1},
            {"expired": True},
            {"name": "arbitrary.zip"},
            {"workflow_run": {"id": RUN_ID + 1, "head_sha": SOURCE_SHA, "head_branch": "main"}},
            {"digest": "sha256:not-a-digest"},
        )
        for change in bad_cases:
            modified = self.official_artifact()
            modified.update(change)
            with self.subTest(change=change), self.assertRaises(ValueError):
                verifier.validate_artifact_metadata(
                    modified, tag=TAG, source_sha=SOURCE_SHA, run_id=RUN_ID, artifact_id=ARTIFACT_ID,
                )

    def official_baseline(self):
        return {
            "id": verifier.BASELINE_RELEASE_ID,
            "tag_name": verifier.BASELINE_TAG,
            "immutable": True,
            "draft": False,
            "prerelease": False,
            "assets": [{
                "id": verifier.BASELINE_APK_ASSET_ID,
                "name": f"EQ-Library-{verifier.BASELINE_TAG}.apk",
                "state": "uploaded",
                "digest": f"sha256:{verifier.BASELINE_APK_SHA256}",
            }],
        }

    def test_stable_baseline_requires_pinned_immutable_release_and_apk(self):
        verifier.validate_stable_baseline_release(self.official_baseline())
        for change in (
            {"immutable": False},
            {"draft": True},
            {"prerelease": True},
            {"tag_name": "v0.8.2"},
            {"id": verifier.BASELINE_RELEASE_ID + 1},
            {"assets": [{
                "id": verifier.BASELINE_APK_ASSET_ID,
                "name": f"EQ-Library-{verifier.BASELINE_TAG}.apk",
                "state": "uploaded",
                "digest": "sha256:" + "0" * 64,
            }]},
        ):
            release = self.official_baseline()
            release.update(change)
            with self.subTest(change=change), self.assertRaises(ValueError):
                verifier.validate_stable_baseline_release(release)

    def test_bad_signer_run_cannot_execute_candidate_python(self):
        candidate_tools = self.root / "untrusted-candidate/tools"
        candidate_tools.mkdir(parents=True)
        sentinel = self.root / "candidate-module-executed"
        (candidate_tools / "promote_release_candidate.py").write_text(
            "from pathlib import Path\n"
            f"Path({str(sentinel)!r}).write_text('executed')\n"
            "import os\n"
            "os.environ.get('GH_TOKEN')\n",
            encoding="utf-8",
        )

        api_instances = []

        class BadRunApi:
            repo_path = "/repos/weekssa/OPRA-EQ-for-UAPP"

            def __init__(self, repository, token, api_url):
                self.calls = []
                api_instances.append(self)

            def json(self, method, path, payload=None):
                self.calls.append(path)
                if path.endswith("/actions/workflows/github-release.yml"):
                    return {"id": 77, "path": verifier.CANDIDATE_WORKFLOW_PATH, "state": "active"}
                if path.endswith(f"/actions/runs/{RUN_ID}"):
                    return {
                        "id": RUN_ID,
                        "workflow_id": 77,
                        "status": "completed",
                        "conclusion": "success",
                        "event": "workflow_dispatch",
                        "head_branch": "untrusted",
                        "head_sha": SOURCE_SHA,
                        "head_repository": {"full_name": verifier.REPOSITORY},
                    }
                raise AssertionError("Artifact lookup must not happen for an untrusted run")

        args = type("Args", (), {
            "candidate_root": str(self.root / "untrusted-candidate"),
            "candidate_source_sha": SOURCE_SHA,
            "tag": TAG,
            "candidate_run_id": RUN_ID,
            "candidate_artifact_id": ARTIFACT_ID,
            "repository": verifier.REPOSITORY,
            "token": "fake-token",
            "api_url": "https://api.github.com",
        })()
        real_loader = verifier.load_promotion_helpers
        loaded_modules = []

        def load_trusted_helpers():
            module = real_loader()
            module.GitHubApi = BadRunApi
            loaded_modules.append(module)
            return module

        with mock.patch.object(verifier, "load_promotion_helpers", side_effect=load_trusted_helpers) as loader:
            with self.assertRaisesRegex(RuntimeError, "main"):
                verifier.verify_and_prepare(args)
            loader.assert_called_once_with()

        self.assertEqual(1, len(loaded_modules))
        trusted_helper_path = Path(verifier.__file__).resolve().with_name("promote_release_candidate.py")
        self.assertEqual(trusted_helper_path, Path(loaded_modules[0].__file__).resolve())
        self.assertEqual([
            "/repos/weekssa/OPRA-EQ-for-UAPP/actions/workflows/github-release.yml",
            f"/repos/weekssa/OPRA-EQ-for-UAPP/actions/runs/{RUN_ID}",
        ], api_instances[0].calls)
        self.assertFalse(sentinel.exists())

    def test_archive_validation_checks_archive_digest_manifest_and_embedded_source(self):
        candidate = verifier.verify_candidate_archive(
            promotion, self.archive, artifact_digest=f"sha256:{self.digest}", tag=TAG,
            source_sha=SOURCE_SHA, project_root=self.root,
        )
        self.assertEqual(SOURCE_SHA, candidate["source_sha"])
        self.assertEqual(hashlib.sha256(candidate["files"][candidate["apk_name"]]).hexdigest(), candidate["apk_sha256"])

        with self.assertRaisesRegex(promotion.PromotionError, "artifact digest"):
            verifier.verify_candidate_archive(
                promotion, self.archive, artifact_digest="0" * 64, tag=TAG,
                source_sha=SOURCE_SHA, project_root=self.root,
            )
        with self.assertRaisesRegex(promotion.PromotionError, "manifest field sourceSha"):
            verifier.verify_candidate_archive(
                promotion, self.archive, artifact_digest=self.digest, tag=TAG,
                source_sha="b" * 40, project_root=self.root,
            )

    def test_embedded_source_rejects_fallback_or_malformed_apk(self):
        with self.assertRaisesRegex(ValueError, "exact candidate source"):
            verifier.verify_embedded_source(make_apk("b" * 40), SOURCE_SHA, ValueError)
        with self.assertRaisesRegex(ValueError, "fallback"):
            verifier.verify_embedded_source(make_apk(fallback=True), SOURCE_SHA, ValueError)
        with self.assertRaisesRegex(ValueError, "classes.dex"):
            verifier.verify_embedded_source(b"not an apk", SOURCE_SHA, ValueError)


if __name__ == "__main__":
    unittest.main()
