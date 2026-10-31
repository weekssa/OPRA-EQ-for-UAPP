from __future__ import annotations

import json
import tempfile
import unittest
from dataclasses import replace
from pathlib import Path

import v090_release as release


SOURCE = "a" * 40
ARTIFACT_SHA = "b" * 64
APK_SHA = "c" * 64
SIGNER_SHA = "65c1c1256dae3c49e3548f334c91f0ba991969e9be9e0b223ba4e253d2114747"


class V090ReleaseTest(unittest.TestCase):
    def setUp(self):
        self.temp = tempfile.TemporaryDirectory(prefix="v090-release-test-")
        self.root = Path(self.temp.name)
        (self.root / "app").mkdir()
        self.write_version("0.9.0-beta.1", 15)

    def tearDown(self):
        self.temp.cleanup()

    def write_version(self, name: str, code: int) -> None:
        (self.root / "app/build.gradle.kts").write_text(
            f'android {{\n    defaultConfig {{\n        versionCode = {code}\n        versionName = "{name}"\n    }}\n}}\n',
            encoding="utf-8",
        )

    def record(self) -> release.CandidateRecord:
        return release.CandidateRecord(
            tag=release.BETA_TAG,
            source_sha=SOURCE,
            run_id=123,
            artifact_id=456,
            artifact_name=release.expected_candidate_artifact_name(SOURCE),
            artifact_digest=ARTIFACT_SHA,
            apk_sha256=APK_SHA,
            signer_sha256=SIGNER_SHA,
        )

    def test_version_and_exact_beta_tag_are_required(self):
        release.validate_version(self.root)
        release.validate_candidate_tag("v0.9.0-beta.1")
        for tag in ("v0.9.0", "v0.9.0-beta.2", "0.9.0-beta.1"):
            with self.subTest(tag=tag), self.assertRaises(release.V090ReleaseError):
                release.validate_candidate_tag(tag)

    def test_wrong_android_name_or_code_is_rejected(self):
        self.write_version("0.9.0", 15)
        with self.assertRaisesRegex(release.V090ReleaseError, "versionName"):
            release.validate_version(self.root)
        self.write_version("0.9.0-beta.1", 14)
        with self.assertRaisesRegex(release.V090ReleaseError, "versionCode"):
            release.validate_version(self.root)

    def test_candidate_metadata_is_beta_prerelease_and_nonlatest(self):
        metadata = release.candidate_metadata(SOURCE)
        self.assertEqual("beta", metadata["releaseChannel"])
        self.assertIs(True, metadata["prerelease"])
        self.assertIs(False, metadata["makeLatest"])
        self.assertIs(False, metadata["publishOnSign"])

    def test_wrong_source_run_artifact_hash_and_signer_are_rejected(self):
        base = self.record()
        cases = [
            ("source", replace(base, source_sha="d" * 40), "source"),
            ("run", replace(base, run_id=0), "run ID"),
            ("artifact id", replace(base, artifact_id=0), "artifact ID"),
            ("artifact name", replace(base, artifact_name="other.zip"), "artifact name"),
            ("artifact hash", replace(base, artifact_digest="bad"), "artifact digest"),
            ("apk hash", replace(base, apk_sha256="bad"), "APK digest"),
            ("signer", replace(base, signer_sha256="e" * 64), "signer"),
        ]
        for label, record, error in cases:
            with self.subTest(label=label), self.assertRaisesRegex(release.V090ReleaseError, error):
                release.validate_candidate_record(
                    record,
                    expected_source_sha=SOURCE,
                    expected_signer_sha256=SIGNER_SHA,
                )

    def test_manifest_requires_beta_safety_metadata(self):
        manifest = release.candidate_metadata(SOURCE) | {
            "apkSha256": APK_SHA,
            "signerSha256": SIGNER_SHA,
        }
        record = release.candidate_record_from_manifest(
            manifest,
            run_id=123,
            artifact_id=456,
            artifact_name=release.expected_candidate_artifact_name(SOURCE),
            artifact_digest=ARTIFACT_SHA,
            pinned_signer_sha256=SIGNER_SHA,
            expected_source_sha=SOURCE,
        )
        self.assertEqual(SOURCE, record.source_sha)
        for field, value in (("prerelease", False), ("makeLatest", True), ("releaseChannel", "stable")):
            unsafe = dict(manifest)
            unsafe[field] = value
            with self.subTest(field=field), self.assertRaises(release.V090ReleaseError):
                release.candidate_record_from_manifest(
                    unsafe,
                    run_id=123,
                    artifact_id=456,
                    artifact_name=release.expected_candidate_artifact_name(SOURCE),
                    artifact_digest=ARTIFACT_SHA,
                    pinned_signer_sha256=SIGNER_SHA,
                    expected_source_sha=SOURCE,
                )

    def test_stable_latest_safety_rejects_prerelease_or_changed_apk(self):
        latest = {
            "tag_name": release.STABLE_LATEST_TAG,
            "draft": False,
            "prerelease": False,
            "immutable": True,
            "assets": [{
                "name": "EQ-Library-v0.8.3.apk",
                "digest": f"sha256:{release.STABLE_LATEST_APK_SHA256}",
            }],
        }
        release.validate_latest_snapshot(latest)
        for label, mutate in (
            ("prerelease", lambda value: value.update(prerelease=True)),
            ("latest tag", lambda value: value.update(tag_name=release.BETA_TAG)),
            ("apk", lambda value: value["assets"][0].update(digest="sha256:" + "f" * 64)),
        ):
            candidate = json.loads(json.dumps(latest))
            mutate(candidate)
            with self.subTest(label=label), self.assertRaises(release.V090ReleaseError):
                release.validate_latest_snapshot(candidate)

    def test_repository_contract(self):
        release.check_contract(Path(__file__).resolve().parents[1])


if __name__ == "__main__":
    unittest.main()
