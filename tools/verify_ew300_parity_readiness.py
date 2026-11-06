#!/usr/bin/env python3
"""Fail-closed readiness check for the local EW300 parity APK."""

from __future__ import annotations

import argparse
import glob
import hashlib
import os
import re
import subprocess
import sys


def sha256(path: str) -> str:
    digest = hashlib.sha256()
    with open(path, "rb") as handle:
        for chunk in iter(lambda: handle.read(1024 * 1024), b""):
            digest.update(chunk)
    return digest.hexdigest()


def find_sdk_tool(name: str) -> str | None:
    sdk = os.environ.get("ANDROID_HOME") or os.environ.get("ANDROID_SDK_ROOT") or os.path.expanduser(
        "~/Library/Android/sdk"
    )
    candidates = sorted(glob.glob(os.path.join(sdk, "build-tools", "*", name)), reverse=True)
    return candidates[0] if candidates else None


def run(command: list[str]) -> str:
    result = subprocess.run(command, capture_output=True, text=True)
    if result.returncode != 0:
        raise RuntimeError(result.stderr.strip() or f"{command[0]} failed")
    return result.stdout + result.stderr


def main(argv: list[str]) -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--apk-dir", required=True)
    parser.add_argument("--manifest", required=True)
    args = parser.parse_args(argv[1:])

    apks = sorted(glob.glob(os.path.join(args.apk_dir, "*.apk")))
    if len(apks) != 1:
        print(f"expected exactly one APK in {args.apk_dir}, found {len(apks)}", file=sys.stderr)
        return 1
    apk = apks[0]
    digest = sha256(apk)

    aapt2 = find_sdk_tool("aapt2")
    apksigner = find_sdk_tool("apksigner")
    if not aapt2 or not apksigner:
        print("Android build-tools aapt2/apksigner not found", file=sys.stderr)
        return 1

    badging = run([aapt2, "dump", "badging", apk])
    package = re.search(r"package: name='([^']+)' versionCode='([^']+)' versionName='([^']+)'", badging)
    if not package:
        print("could not parse APK package metadata", file=sys.stderr)
        return 1
    name, code, version = package.groups()

    signer_output = run([apksigner, "verify", "--print-certs", apk])
    signer = re.search(r"Signer #1 certificate SHA-256 digest: ([0-9a-f]+)", signer_output, re.I)

    manifest = open(args.manifest, encoding="utf-8").read()
    problems: list[str] = []
    if name != "com.weekssa.opraeqforuapp.ew300parity":
        problems.append(f"unexpected package {name}")
    if version != "0.10.0-ew300parity":
        problems.append(f"unexpected versionName {version}")
    if code != "22":
        problems.append(f"unexpected versionCode {code}")
    if digest not in manifest.lower():
        problems.append("APK SHA-256 is not recorded in the readiness manifest")
    if os.path.basename(apk) not in manifest:
        problems.append("APK filename is not recorded in the readiness manifest")
    if not signer or signer.group(1).lower() not in manifest.lower():
        problems.append("signer SHA-256 is not recorded in the readiness manifest")
    for required in (
        "physical qualification pending",
        "docs/planning/ew300-ja11-parity/FUNCTIONAL_MAPPING.md",
        "docs/planning/ew300-ja11-parity/TEST_EVIDENCE_LOG.md",
        "No push, PR, merge, publication, signing change, or public hardware claim",
    ):
        if required not in manifest:
            problems.append(f"readiness manifest is missing: {required}")

    print(f"apk      {apk}")
    print(f"package  {name}")
    print(f"version  {version} / {code}")
    print(f"sha256   {digest}")
    print(f"signer   {signer.group(1) if signer else 'UNKNOWN'}")
    if problems:
        for problem in problems:
            print(f"FAIL {problem}", file=sys.stderr)
        return 1
    print("readiness manifest PASS")
    return 0


if __name__ == "__main__":
    sys.exit(main(sys.argv))

