#!/usr/bin/env python3
"""Offline tests for the fail-closed Phase C Pixel entry guard."""

from __future__ import annotations

import hashlib
import json
import subprocess
import sys
import tempfile
import unittest
from pathlib import Path
from unittest.mock import patch

sys.path.insert(0, str(Path(__file__).resolve().parent))

import pixel_adb_binding as adb_binding
import verify_pixel_binding


TARGET = f"adb-{adb_binding.EXPECTED_SERIAL}-route.{adb_binding.TLS_CONNECT}"
FINGERPRINT = "google/tokay/tokay:17/CP3A.261005.005/16271449:user/release-keys"


def sealed_binding() -> dict[str, object]:
    return {
        "target": TARGET,
        "transport": "wireless-adb-only",
        "pairing_performed": False,
        "installed_apk": {"package": adb_binding.PACKAGE, "sha256": adb_binding.EXPECTED_APK_SHA256},
        "identity": {
            "serial": adb_binding.EXPECTED_SERIAL,
            "model": adb_binding.EXPECTED_MODEL,
            "product": adb_binding.EXPECTED_PRODUCT,
            "sdk": "37",
            "fingerprint": FINGERPRINT,
            "route_boot_id": "not-persisted",
        },
    }


def write_binding(path: Path, binding: dict[str, object]) -> str:
    path.write_text(json.dumps(binding), encoding="utf-8")
    return hashlib.sha256(path.read_bytes()).hexdigest()


class FakeAdb:
    def __init__(self, *, changed: str | None = None, process: bool = False):
        self.changed = changed
        self.process = process
        self.calls: list[list[str]] = []

    def run(self, argv: list[str]) -> subprocess.CompletedProcess[bytes]:
        args = argv[1:]
        self.calls.append(args)
        if args[:3] == ["-s", TARGET, "get-state"]:
            return subprocess.CompletedProcess(argv, 0, b"device\n", b"")
        if args[:3] == ["-s", TARGET, "shell"]:
            op = args[3:]
            if op[:1] == ["getprop"]:
                values = {
                    "ro.serialno": adb_binding.EXPECTED_SERIAL,
                    "ro.product.model": adb_binding.EXPECTED_MODEL,
                    "ro.product.device": adb_binding.EXPECTED_PRODUCT,
                    "ro.build.version.sdk": "37",
                    "ro.build.fingerprint": FINGERPRINT,
                }
                key = op[1]
                value = values[key]
                if self.changed == key:
                    value += "-changed"
                return subprocess.CompletedProcess(argv, 0, (value + "\n").encode(), b"")
            if op == ["cat", "/proc/sys/kernel/random/boot_id"]:
                return subprocess.CompletedProcess(argv, 0, b"12345678-1234-1234-1234-123456789abc\n", b"")
            if op == ["pidof", adb_binding.PACKAGE]:
                return subprocess.CompletedProcess(argv, 0, b"7331\n", b"") if self.process else subprocess.CompletedProcess(argv, 1, b"", b"")
            if op == ["dumpsys", "activity", "activities"]:
                text = "topResumedActivity=ActivityRecord{a u0 com.android.launcher/.Launcher t1}\n"
                return subprocess.CompletedProcess(argv, 0, text.encode(), b"")
        return subprocess.CompletedProcess(argv, 1, b"", b"unexpected fake ADB command")


class VerifyPixelBindingTest(unittest.TestCase):
    def test_passes_when_same_online_pixel_and_app_is_absent(self):
        fake = FakeAdb()
        with tempfile.TemporaryDirectory() as temp:
            root = Path(temp)
            binding_path = root / "binding.json"
            expected_sha256 = write_binding(binding_path, sealed_binding())
            log = root / "entry.log"
            with patch.object(adb_binding, "run_command", side_effect=fake.run):
                result = verify_pixel_binding.verify_existing_binding(
                    "/fake/adb", binding_path, TARGET, expected_sha256, log
                )

        self.assertEqual(result["result"], "PASS")
        self.assertEqual(result["stable_identity_match"], "PASS")
        self.assertEqual(result["app_process"], "ABSENT")
        self.assertIn('"dac_query": "NOT_PERFORMED"', json.dumps(result))
        self.assertFalse(any("mdns" in call or call[:1] == ["connect"] for call in fake.calls))

    def test_rejects_a_different_bound_target_before_adb(self):
        fake = FakeAdb()
        with tempfile.TemporaryDirectory() as temp:
            binding_path = Path(temp) / "binding.json"
            expected_sha256 = write_binding(binding_path, sealed_binding())
            with patch.object(adb_binding, "run_command", side_effect=fake.run):
                with self.assertRaises(adb_binding.ResolutionError):
                    verify_pixel_binding.verify_existing_binding(
                        "/fake/adb", binding_path, "other-target", expected_sha256, Path(temp) / "entry.log"
                    )
        self.assertEqual(fake.calls, [])

    def test_rejects_non_wireless_target_even_when_binding_hash_matches(self):
        fake = FakeAdb()
        with tempfile.TemporaryDirectory() as temp:
            binding_path = Path(temp) / "binding.json"
            binding = sealed_binding()
            binding["target"] = adb_binding.EXPECTED_SERIAL
            expected_sha256 = write_binding(binding_path, binding)
            with patch.object(adb_binding, "run_command", side_effect=fake.run):
                with self.assertRaises(adb_binding.ResolutionError):
                    verify_pixel_binding.verify_existing_binding(
                        "/fake/adb", binding_path, adb_binding.EXPECTED_SERIAL, expected_sha256, Path(temp) / "entry.log"
                    )
        self.assertEqual(fake.calls, [])

    def test_rejects_substituted_binding_before_adb(self):
        fake = FakeAdb()
        with tempfile.TemporaryDirectory() as temp:
            binding_path = Path(temp) / "binding.json"
            write_binding(binding_path, sealed_binding())
            with patch.object(adb_binding, "run_command", side_effect=fake.run):
                with self.assertRaises(adb_binding.ResolutionError):
                    verify_pixel_binding.verify_existing_binding(
                        "/fake/adb", binding_path, TARGET, "0" * 64, Path(temp) / "entry.log"
                    )
        self.assertEqual(fake.calls, [])

    def test_rejects_changed_android_identity_before_app_check(self):
        fake = FakeAdb(changed="ro.build.fingerprint")
        with tempfile.TemporaryDirectory() as temp:
            binding_path = Path(temp) / "binding.json"
            expected_sha256 = write_binding(binding_path, sealed_binding())
            with patch.object(adb_binding, "run_command", side_effect=fake.run):
                with self.assertRaises(adb_binding.ResolutionError):
                    verify_pixel_binding.verify_existing_binding(
                        "/fake/adb", binding_path, TARGET, expected_sha256, Path(temp) / "entry.log"
                    )
        self.assertFalse(any(call[-2:] == ["pidof", adb_binding.PACKAGE] for call in fake.calls))

    def test_rejects_active_app_process(self):
        fake = FakeAdb(process=True)
        with tempfile.TemporaryDirectory() as temp:
            binding_path = Path(temp) / "binding.json"
            expected_sha256 = write_binding(binding_path, sealed_binding())
            with patch.object(adb_binding, "run_command", side_effect=fake.run):
                with self.assertRaises(adb_binding.ResolutionError):
                    verify_pixel_binding.verify_existing_binding(
                        "/fake/adb", binding_path, TARGET, expected_sha256, Path(temp) / "entry.log"
                    )
        self.assertFalse(any(call[-3:] == ["dumpsys", "activity", "activities"] for call in fake.calls))


if __name__ == "__main__":
    unittest.main()
