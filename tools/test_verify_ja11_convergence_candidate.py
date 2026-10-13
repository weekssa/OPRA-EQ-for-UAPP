import json
import tempfile
import unittest
import zipfile
from pathlib import Path
from unittest.mock import patch
from tools import verify_ja11_convergence_candidate as candidate


class CandidateTest(unittest.TestCase):
    def setUp(self):
        self.temp = tempfile.TemporaryDirectory()
        self.directory = Path(self.temp.name)
        self.source = "a" * 40
        for name in ["app-ja11Diagnostic.apk", "app-ja11Diagnostic-androidTest.apk"]:
            with zipfile.ZipFile(self.directory / name, "w") as archive:
                archive.writestr("classes.dex", self.source)
        self.manifest = dict(
            kind="JA11 diagnostic convergence trace", sourceSha=self.source,
            workflow=".github/workflows/android-ci-exact-head.yml", workflowRunId="123",
            signerKind="Gradle debug key", apk="app-ja11Diagnostic.apk",
            testApk="app-ja11Diagnostic-androidTest.apk", packageId=candidate.PACKAGE,
            versionName="0.8.1-ja11trace", versionCode=12, signerSha256="b" * 64,
        )
        for name, key in [(self.manifest["apk"], "apkSha256"), (self.manifest["testApk"], "testApkSha256")]:
            self.manifest[key] = candidate.digest(self.directory / name)
        self.write()
        self.metadata = patch.object(candidate, "apk_identity", side_effect=self.identity)
        self.metadata.start()

    def tearDown(self):
        self.metadata.stop()
        self.temp.cleanup()

    def identity(self, path, sdk):
        return dict(packageId=candidate.PACKAGE + (".test" if "androidTest" in path.name else ""),
                    versionName=self.manifest["versionName"], versionCode=12, signerSha256="b" * 64)

    def write(self):
        (self.directory / "candidate-manifest.json").write_text(json.dumps(self.manifest))

    def verify(self):
        return candidate.verify(self.directory, self.source, Path("unused-mocked-sdk"))

    def test_valid_identity_is_positive_control(self):
        self.assertEqual(self.verify()["sourceSha"], self.source)

    def test_changed_apk_fails_digest_gate(self):
        with (self.directory / self.manifest["apk"]).open("ab") as stream:
            stream.write(b"changed")
        with self.assertRaisesRegex(ValueError, "digest mismatch"):
            self.verify()

    def test_wrong_source_is_rejected(self):
        self.manifest["sourceSha"] = "c" * 40
        self.write()
        with self.assertRaisesRegex(ValueError, "Source"):
            self.verify()

    def test_wrong_signer_is_rejected(self):
        self.manifest["signerSha256"] = "c" * 64
        self.write()
        with self.assertRaisesRegex(ValueError, "signer mismatch"):
            self.verify()

    def test_release_package_cannot_be_used(self):
        self.manifest["packageId"] = "com.weekssa.opraeqforuapp"
        self.write()
        with self.assertRaisesRegex(ValueError, "isolated diagnostic"):
            self.verify()

    def test_target_version_cannot_be_missing(self):
        self.manifest["versionName"] = ""
        self.write()
        with self.assertRaisesRegex(ValueError, "version missing"):
            self.verify()

    def test_paths_and_extra_files_are_rejected(self):
        self.manifest["apk"] = "../private.apk"
        self.write()
        with self.assertRaisesRegex(ValueError, "filename"):
            self.verify()
        self.manifest["apk"] = "app-ja11Diagnostic.apk"
        self.write()
        (self.directory / "unrelated-report.txt").write_text("not an artifact")
        with self.assertRaisesRegex(ValueError, "Unexpected artifact"):
            self.verify()


class ApkIdentityParserTest(unittest.TestCase):
    def identity(self, badging):
        signature = "Signer #1 certificate SHA-256 digest: " + "b" * 64 + "\nVerified using v2 scheme (APK Signature Scheme v2): true\n"
        with patch.object(candidate.subprocess, "check_output", side_effect=[signature, badging]):
            return candidate.apk_identity(Path("fixture.apk"), Path("fixture-sdk"))

    def test_instrumentation_apk_without_version_is_parsed(self):
        identity = self.identity("package: name='" + candidate.PACKAGE + ".test' versionCode='' versionName='' platformBuildVersionName='16'")
        self.assertIsNone(identity["versionCode"])
        self.assertEqual(identity["packageId"], candidate.PACKAGE + ".test")

    def test_target_apk_keeps_exact_version_fields(self):
        identity = self.identity("package: name='" + candidate.PACKAGE + "' versionCode='12' versionName='0.8.1-ja11trace'")
        self.assertEqual(identity["versionCode"], 12)
        self.assertEqual(identity["versionName"], "0.8.1-ja11trace")


if __name__ == "__main__":
    unittest.main()
