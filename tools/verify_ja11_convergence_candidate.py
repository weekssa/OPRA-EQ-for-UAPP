#!/usr/bin/env python3
"""Create/verify a diagnostic Gradle-debug artifact; never sign, install, tag or publish."""
import argparse
import hashlib
import json
import re
import subprocess
import zipfile
from pathlib import Path

PACKAGE = "com.weekssa.opraeqforuapp.ja11trace"


def digest(path):
    return hashlib.sha256(path.read_bytes()).hexdigest()


def apk_identity(path, sdk):
    tools = sdk / "build-tools" / "36.0.0"
    signed = subprocess.check_output(
        [str(tools / "apksigner"), "verify", "--verbose", "--print-certs", str(path)], text=True
    )
    signers = re.findall(r"Signer #\d+ certificate SHA-256 digest: ([0-9a-f]{64})", signed)
    if len(signers) != 1 or not re.search(r"Verified using v2 scheme.*: true", signed):
        raise ValueError("Expected one signer and an APK v2 signature")
    metadata = subprocess.check_output([str(tools / "aapt"), "dump", "badging", str(path)], text=True)
    # AGP instrumentation APKs intentionally omit version fields; the target APK must have them.
    package = re.search(r"package: name='([^']+)' versionCode='(\d*)' versionName='([^']*)'", metadata)
    if not package:
        raise ValueError("APK package metadata missing")
    return dict(packageId=package[1], versionCode=int(package[2]) if package[2] else None,
                versionName=package[3], signerSha256=signers[0])


def verify(directory, source, sdk):
    manifest = json.loads((directory / "candidate-manifest.json").read_text())
    expected = {"candidate-manifest.json", "app-ja11Diagnostic.apk", "app-ja11Diagnostic-androidTest.apk"}
    if {p.name for p in directory.iterdir()} != expected or any(p.is_symlink() for p in directory.iterdir()):
        raise ValueError("Unexpected artifact files or links")
    if manifest["workflow"] != ".github/workflows/android-ci-exact-head.yml":
        raise ValueError("Unexpected workflow")
    if manifest["sourceSha"] != source or manifest["kind"] != "JA11 diagnostic convergence trace":
        raise ValueError("Source or artifact kind mismatch")
    if manifest["signerKind"] != "Gradle debug key" or manifest["packageId"] != PACKAGE:
        raise ValueError("Not an isolated diagnostic candidate")
    for key, hash_key in [("apk", "apkSha256"), ("testApk", "testApkSha256")]:
        name = manifest[key]
        if Path(name).name != name or not name.endswith(".apk"):
            raise ValueError("Invalid artifact filename")
        path = directory / name
        if digest(path) != manifest[hash_key]:
            raise ValueError("APK digest mismatch")
        actual = apk_identity(path, sdk)
        expected_package = PACKAGE if key == "apk" else PACKAGE + ".test"
        if actual["packageId"] != expected_package or actual["signerSha256"] != manifest["signerSha256"]:
            raise ValueError("APK package or signer mismatch")
        if key == "apk":
            if not actual["versionCode"] or not actual["versionName"].endswith("-ja11trace"):
                raise ValueError("Diagnostic target APK version missing or invalid")
            if actual["versionName"] != manifest["versionName"] or actual["versionCode"] != manifest["versionCode"]:
                raise ValueError("APK version mismatch")
            with zipfile.ZipFile(path) as archive:
                if not any(source.encode() in archive.read(n) for n in archive.namelist() if re.fullmatch(r"classes\d*\.dex", n)):
                    raise ValueError("Embedded source SHA missing")
    return manifest


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("mode", choices=["create", "verify"])
    parser.add_argument("--directory", type=Path, required=True)
    parser.add_argument("--source-sha", required=True)
    parser.add_argument("--sdk", type=Path, required=True)
    parser.add_argument("--run-id", default="local")
    args = parser.parse_args()
    if not re.fullmatch(r"[0-9a-f]{40}", args.source_sha):
        raise ValueError("Exact source SHA required")
    if args.mode == "create":
        apk = args.directory / "app-ja11Diagnostic.apk"
        test_apk = args.directory / "app-ja11Diagnostic-androidTest.apk"
        identity = apk_identity(apk, args.sdk)
        manifest = dict(
            kind="JA11 diagnostic convergence trace", sourceSha=args.source_sha,
            workflow=".github/workflows/android-ci-exact-head.yml", workflowRunId=args.run_id,
            signerKind="Gradle debug key", apk=apk.name, apkSha256=digest(apk),
            testApk=test_apk.name, testApkSha256=digest(test_apk), **identity,
        )
        (args.directory / "candidate-manifest.json").write_text(json.dumps(manifest, indent=2) + "\n")
    manifest = verify(args.directory, args.source_sha, args.sdk)
    print(json.dumps(manifest, indent=2))


if __name__ == "__main__":
    main()
