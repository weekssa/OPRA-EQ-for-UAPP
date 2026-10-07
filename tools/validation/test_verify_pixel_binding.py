#!/usr/bin/env python3
"""Offline tests for the fail-closed Phase C Pixel entry guard."""

from __future__ import annotations

import hashlib
import io
import json
import subprocess
import sys
import tempfile
import unittest
from pathlib import Path
from contextlib import redirect_stderr, redirect_stdout
from unittest.mock import patch

sys.path.insert(0, str(Path(__file__).resolve().parent))

import pixel_adb_binding as adb_binding
import verify_pixel_binding


TEST_SERIAL = "PIXELTESTSERIAL001"
TEST_SERIAL_SHA256 = hashlib.sha256(TEST_SERIAL.encode("utf-8")).hexdigest()
RESOLVER_SHA256 = hashlib.sha256(Path(adb_binding.__file__).read_bytes()).hexdigest()
TARGET = f"adb-{TEST_SERIAL}-route.{adb_binding.TLS_CONNECT}"
GENERIC_TARGET = f"adb-currentroute.{adb_binding.TLS_CONNECT}"
ENDPOINT_TARGET = "192.168.50.24:33185"
FINGERPRINT = "google/tokay/tokay:17/CP3A.261005.005/16271449:user/release-keys"


def sealed_binding(
    *,
    target: str = TARGET,
    target_source: str = "current-online-tls-connect-alias",
    endpoint_last_resolved: str | None = "192.168.50.24:33185",
    service_instance: str | None = TARGET,
) -> dict[str, object]:
    return {
        "schema_version": 1,
        "target": target,
        "target_source": target_source,
        "endpoint_last_resolved": endpoint_last_resolved,
        "service_instance": service_instance,
        "transport": "wireless-adb-only",
        "pairing_performed": False,
        "resolver_sha256": RESOLVER_SHA256,
        "installed_apk": {"package": adb_binding.PACKAGE, "sha256": adb_binding.EXPECTED_APK_SHA256},
        "identity": {
            "serial_sha256": adb_binding.EXPECTED_SERIAL_SHA256,
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
    def __init__(self, *, target: str = TARGET, changed: str | None = None, process: bool = False):
        self.target = target
        self.changed = changed
        self.process = process
        self.calls: list[list[str]] = []

    def run(self, argv: list[str]) -> subprocess.CompletedProcess[bytes]:
        args = argv[1:]
        self.calls.append(args)
        if args[:3] == ["-s", self.target, "get-state"]:
            return subprocess.CompletedProcess(argv, 0, b"device\n", b"")
        if args[:3] == ["-s", self.target, "shell"]:
            op = args[3:]
            if op[:1] == ["getprop"]:
                values = {
                    "ro.serialno": TEST_SERIAL,
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
    def setUp(self):
        self.serial_patch = patch.object(adb_binding, "EXPECTED_SERIAL_SHA256", TEST_SERIAL_SHA256)
        self.serial_patch.start()

    def tearDown(self):
        self.serial_patch.stop()

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

    def test_passes_for_generic_current_mdns_tls_connect_alias(self):
        fake = FakeAdb(target=GENERIC_TARGET)
        binding = sealed_binding(
            target=GENERIC_TARGET,
            target_source="current-mdns-service-alias",
            endpoint_last_resolved="192.168.50.24:33185",
            service_instance=GENERIC_TARGET,
        )
        with tempfile.TemporaryDirectory() as temp:
            root = Path(temp)
            binding_path = root / "binding.json"
            expected_sha256 = write_binding(binding_path, binding)
            with patch.object(adb_binding, "run_command", side_effect=fake.run):
                result = verify_pixel_binding.verify_existing_binding(
                    "/fake/adb", binding_path, GENERIC_TARGET, expected_sha256, root / "entry.log"
                )

        self.assertEqual(result["result"], "PASS")
        self.assertTrue(all(call[:2] == ["-s", GENERIC_TARGET] for call in fake.calls))

    def test_passes_for_current_mdns_mapped_endpoint_target(self):
        fake = FakeAdb(target=ENDPOINT_TARGET)
        binding = sealed_binding(
            target=ENDPOINT_TARGET,
            target_source="current-mdns-endpoint-map",
            endpoint_last_resolved=ENDPOINT_TARGET,
            service_instance=GENERIC_TARGET,
        )
        with tempfile.TemporaryDirectory() as temp:
            root = Path(temp)
            binding_path = root / "binding.json"
            expected_sha256 = write_binding(binding_path, binding)
            with patch.object(adb_binding, "run_command", side_effect=fake.run):
                result = verify_pixel_binding.verify_existing_binding(
                    "/fake/adb", binding_path, ENDPOINT_TARGET, expected_sha256, root / "entry.log"
                )

        self.assertEqual(result["result"], "PASS")
        self.assertTrue(all(call[:2] == ["-s", ENDPOINT_TARGET] for call in fake.calls))

    def test_rejects_endpoint_target_without_exact_current_mdns_binding(self):
        fake = FakeAdb(target=ENDPOINT_TARGET)
        binding = sealed_binding(
            target=ENDPOINT_TARGET,
            target_source="current-mdns-endpoint-map",
            endpoint_last_resolved="192.168.50.99:5555",
            service_instance=GENERIC_TARGET,
        )
        with tempfile.TemporaryDirectory() as temp:
            binding_path = Path(temp) / "binding.json"
            expected_sha256 = write_binding(binding_path, binding)
            with patch.object(adb_binding, "run_command", side_effect=fake.run):
                with self.assertRaises(adb_binding.ResolutionError):
                    verify_pixel_binding.verify_existing_binding(
                        "/fake/adb", binding_path, ENDPOINT_TARGET, expected_sha256, Path(temp) / "entry.log"
                    )
        self.assertEqual(fake.calls, [])

    def test_rejects_arbitrary_non_transport_target_before_adb(self):
        fake = FakeAdb(target="phone.local:5555")
        binding = sealed_binding(
            target="phone.local:5555",
            target_source="current-mdns-endpoint-map",
            endpoint_last_resolved="phone.local:5555",
            service_instance=GENERIC_TARGET,
        )
        with tempfile.TemporaryDirectory() as temp:
            binding_path = Path(temp) / "binding.json"
            expected_sha256 = write_binding(binding_path, binding)
            with patch.object(adb_binding, "run_command", side_effect=fake.run):
                with self.assertRaises(adb_binding.ResolutionError):
                    verify_pixel_binding.verify_existing_binding(
                        "/fake/adb", binding_path, "phone.local:5555", expected_sha256, Path(temp) / "entry.log"
                    )
        self.assertEqual(fake.calls, [])

    def test_rejects_non_string_service_instance_before_adb(self):
        for malformed in (42, [GENERIC_TARGET]):
            with self.subTest(malformed=type(malformed).__name__), tempfile.TemporaryDirectory() as temp:
                fake = FakeAdb(target=ENDPOINT_TARGET)
                binding = sealed_binding(
                    target=ENDPOINT_TARGET,
                    target_source="current-mdns-endpoint-map",
                    endpoint_last_resolved=ENDPOINT_TARGET,
                    service_instance=malformed,
                )
                binding_path = Path(temp) / "binding.json"
                expected_sha256 = write_binding(binding_path, binding)
                with patch.object(adb_binding, "run_command", side_effect=fake.run):
                    with self.assertRaisesRegex(adb_binding.ResolutionError, "service instance"):
                        verify_pixel_binding.verify_existing_binding(
                            "/fake/adb", binding_path, ENDPOINT_TARGET, expected_sha256, Path(temp) / "entry.log"
                        )
                self.assertEqual(fake.calls, [])

    def test_cli_malformed_binding_fails_with_sanitized_diagnostic(self):
        with tempfile.TemporaryDirectory() as temp:
            root = Path(temp)
            binding_path = root / "binding.json"
            malformed = sealed_binding(
                target=ENDPOINT_TARGET,
                target_source="current-mdns-endpoint-map",
                endpoint_last_resolved=ENDPOINT_TARGET,
                service_instance=[GENERIC_TARGET],
            )
            expected_sha256 = write_binding(binding_path, malformed)
            stdout = io.StringIO()
            stderr = io.StringIO()
            with patch.object(adb_binding, "run_command") as runner:
                with redirect_stdout(stdout), redirect_stderr(stderr):
                    result = verify_pixel_binding.main([
                        "--adb", "/fake/adb",
                        "--binding", str(binding_path),
                        "--expected-binding-sha256", expected_sha256,
                        "--target", ENDPOINT_TARGET,
                        "--log", str(root / "entry.log"),
                        "--result", str(root / "result.json"),
                    ])
                runner.assert_not_called()

            self.assertEqual(result, 2)
            combined = stdout.getvalue() + stderr.getvalue()
            self.assertNotIn(GENERIC_TARGET, combined)
            self.assertNotIn(ENDPOINT_TARGET, combined)
            self.assertNotIn("Traceback", combined)
            self.assertIn("PIXEL_ENTRY_NOT_PASS", combined)

    def test_rejects_non_pixel_fingerprint_profile_before_app_check(self):
        fake = FakeAdb()
        with tempfile.TemporaryDirectory() as temp:
            root = Path(temp)
            binding_path = root / "binding.json"
            binding = sealed_binding()
            binding["identity"]["fingerprint"] = "vendor/other/other:17/build:keys"
            expected_sha256 = write_binding(binding_path, binding)
            with patch.object(adb_binding, "run_command", side_effect=fake.run):
                with self.assertRaises(adb_binding.ResolutionError):
                    verify_pixel_binding.verify_existing_binding(
                        "/fake/adb", binding_path, TARGET, expected_sha256, root / "entry.log"
                    )
        self.assertEqual(fake.calls, [])

    def test_rejects_binding_from_different_resolver_version_before_adb(self):
        fake = FakeAdb()
        with tempfile.TemporaryDirectory() as temp:
            root = Path(temp)
            binding_path = root / "binding.json"
            binding = sealed_binding()
            binding["resolver_sha256"] = "0" * 64
            expected_sha256 = write_binding(binding_path, binding)
            with patch.object(adb_binding, "run_command", side_effect=fake.run):
                with self.assertRaisesRegex(adb_binding.ResolutionError, "different Pixel resolver"):
                    verify_pixel_binding.verify_existing_binding(
                        "/fake/adb", binding_path, TARGET, expected_sha256, root / "entry.log"
                    )
        self.assertEqual(fake.calls, [])

    def test_rejects_boolean_schema_version_before_adb(self):
        fake = FakeAdb()
        with tempfile.TemporaryDirectory() as temp:
            root = Path(temp)
            binding_path = root / "binding.json"
            binding = sealed_binding()
            binding["schema_version"] = True
            expected_sha256 = write_binding(binding_path, binding)
            with patch.object(adb_binding, "run_command", side_effect=fake.run):
                with self.assertRaisesRegex(adb_binding.ResolutionError, "schema version"):
                    verify_pixel_binding.verify_existing_binding(
                        "/fake/adb", binding_path, TARGET, expected_sha256, root / "entry.log"
                    )
        self.assertEqual(fake.calls, [])

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
            binding["target"] = TEST_SERIAL
            expected_sha256 = write_binding(binding_path, binding)
            with patch.object(adb_binding, "run_command", side_effect=fake.run):
                with self.assertRaises(adb_binding.ResolutionError):
                    verify_pixel_binding.verify_existing_binding(
                        "/fake/adb", binding_path, TEST_SERIAL, expected_sha256, Path(temp) / "entry.log"
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
