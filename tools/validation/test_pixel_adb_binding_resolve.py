#!/usr/bin/env python3
"""Mocked end-to-end ADB command tests for safe Pixel target binding."""

from __future__ import annotations

import hashlib
import io
import json
import struct
import subprocess
import sys
import tempfile
import unittest
import zlib
from pathlib import Path
from contextlib import redirect_stderr, redirect_stdout
from unittest.mock import patch

sys.path.insert(0, str(Path(__file__).resolve().parent))

import pixel_adb_binding as binding


SERIAL = "PIXELTESTSERIAL001"
SERIAL_SHA256 = hashlib.sha256(SERIAL.encode("utf-8")).hexdigest()
PIXEL_INSTANCE = f"adb-{SERIAL}-route"
PIXEL_ALIAS = f"{PIXEL_INSTANCE}.{binding.TLS_CONNECT}"
PIXEL_ENDPOINT = "192.168.50.24:33185"
OTHER_INSTANCE = "adb-OTHERDEVICE-route"
OTHER_ENDPOINT = "192.168.50.30:44771"
APK_BYTES = b"frozen candidate apk fixture"


def png_chunk(kind: bytes, payload: bytes) -> bytes:
    return (
        struct.pack(">I", len(payload))
        + kind
        + payload
        + struct.pack(">I", zlib.crc32(kind + payload) & 0xFFFFFFFF)
    )


PNG_BYTES = (
    b"\x89PNG\r\n\x1a\n"
    + png_chunk(b"IHDR", struct.pack(">IIBBBBB", 1, 1, 8, 6, 0, 0, 0))
    + png_chunk(b"IDAT", zlib.compress(b"\x00\x00\x00\x00\xff"))
    + png_chunk(b"IEND", b"")
)


def indexed_png(bit_depth: int, palette_entries: int) -> bytes:
    return (
        b"\x89PNG\r\n\x1a\n"
        + png_chunk(b"IHDR", struct.pack(">IIBBBBB", 1, 1, bit_depth, 3, 0, 0, 0))
        + png_chunk(b"PLTE", bytes(palette_entries * 3))
        + png_chunk(b"IDAT", zlib.compress(b"\x00\x00"))
        + png_chunk(b"IEND", b"")
    )


def png_with_palette(color_type: int) -> bytes:
    channels = {2: 3, 6: 4}[color_type]
    return (
        b"\x89PNG\r\n\x1a\n"
        + png_chunk(b"IHDR", struct.pack(">IIBBBBB", 1, 1, 8, color_type, 0, 0, 0))
        + png_chunk(b"PLTE", bytes(6))
        + png_chunk(b"IDAT", zlib.compress(b"\x00" + bytes(channels)))
        + png_chunk(b"IEND", b"")
    )


def mdns(*rows: str) -> str:
    return "List of discovered mdns services\n" + "\n".join(rows) + "\n"


def devices(*rows: str) -> str:
    return "List of devices attached\n" + "\n".join(rows) + "\n"


class FakeAdb:
    def __init__(self, services: str, device_snapshots: list[str], *, pm_paths: str | None = None, fail: str | None = None, fingerprints: dict[str, str] | None = None, identities: dict[str, dict[str, str]] | None = None):
        self.services = services
        self.device_snapshots = device_snapshots
        self.pm_paths = pm_paths if pm_paths is not None else f"package:/data/app/~~candidate/base.apk\n"
        self.fail = fail
        self.fingerprints = fingerprints or {}
        self.identities = identities or {}
        self.calls: list[list[str]] = []
        self.device_call_count = 0
        self.apk_bytes = APK_BYTES

    def run(self, argv: list[str]) -> subprocess.CompletedProcess[bytes]:
        args = argv[1:]
        self.calls.append(args)

        def result(code: int = 0, stdout: bytes = b"", stderr: bytes = b""):
            return subprocess.CompletedProcess(argv, code, stdout, stderr)

        if args == ["mdns", "services"]:
            return result(stdout=self.services.encode())
        if args == ["devices", "-l"]:
            index = min(self.device_call_count, len(self.device_snapshots) - 1)
            self.device_call_count += 1
            return result(stdout=self.device_snapshots[index].encode())
        if args and args[0] == "connect":
            return result(stdout=f"connected to {args[1]}\n".encode())
        if args and args[0] == "disconnect":
            if self.fail == "disconnect":
                return result(code=1, stderr=b"disconnect failed")
            return result(stdout=f"disconnected {args[1]}\n".encode())
        if len(args) >= 3 and args[0] == "-s":
            target = args[1]
            op = args[2:]
            if op == ["get-state"]:
                return result(stdout=b"device\n")
            if op[:3] == ["shell", "getprop", "ro.serialno"]:
                value = self.identities.get(target, {}).get("serial", SERIAL)
                return result(stdout=(value + "\n").encode())
            if op[:3] == ["shell", "getprop", "ro.product.model"]:
                value = self.identities.get(target, {}).get("model", binding.EXPECTED_MODEL)
                return result(stdout=(value + "\n").encode())
            if op[:3] == ["shell", "getprop", "ro.product.device"]:
                value = self.identities.get(target, {}).get("product", binding.EXPECTED_PRODUCT)
                return result(stdout=(value + "\n").encode())
            if op[:3] == ["shell", "getprop", "ro.build.version.sdk"]:
                value = self.identities.get(target, {}).get("sdk", "37")
                return result(stdout=(value + "\n").encode())
            if op[:3] == ["shell", "getprop", "ro.build.fingerprint"]:
                fingerprint = self.fingerprints.get(target, self.identities.get(target, {}).get("fingerprint", "google/tokay/tokay:17/build:keys"))
                return result(stdout=(fingerprint + "\n").encode())
            if op == ["shell", "cat", "/proc/sys/kernel/random/boot_id"]:
                boot_id = self.identities.get(target, {}).get("route_boot_id", "11111111-1111-1111-1111-111111111111")
                return result(stdout=(boot_id + "\n").encode())
            if op == ["shell", "pm", "path", binding.PACKAGE]:
                return result(stdout=self.pm_paths.encode())
            if len(op) == 3 and op[0] == "pull":
                Path(op[2]).write_bytes(self.apk_bytes)
                return result(stdout=b"1 file pulled\n")
            if op == ["shell", "true"]:
                return result(code=1 if self.fail == "shell" else 0, stderr=b"shell failed" if self.fail == "shell" else b"")
            if op == ["shell", "pidof", binding.PACKAGE]:
                if self.fail == "process-error":
                    return result(code=1, stderr=b"pidof unavailable")
                return result(code=0, stdout=b"1234\n") if self.fail == "process" else result(code=1)
            if op == ["shell", "dumpsys", "activity", "activities"]:
                if self.fail == "activity":
                    return result(stdout=b"topResumedActivity=ActivityRecord{a u0 com.weekssa.opraeqforuapp/.MainActivity t1}\n")
                if self.fail == "activity-android17":
                    return result(stdout=b"Resumed activities in task display areas from top to bottom:\n ResumedActivity: ActivityRecord{a u0 com.android.launcher/.Launcher t1}\n")
                return result(stdout=b"topResumedActivity=ActivityRecord{a u0 com.android.launcher/.Launcher t1}\n")
            if op == ["exec-out", "screencap", "-p"]:
                payload = (
                    b"not png"
                    if self.fail == "screenshot"
                    else PNG_BYTES[:-4]
                    if self.fail == "screenshot-truncated"
                    else PNG_BYTES
                )
                return result(code=1 if self.fail == "screenshot" else 0, stdout=payload)
            if op == ["logcat", "-d", "-t", "1"]:
                return result(code=1 if self.fail == "logcat" else 0, stdout=b"private log line" if self.fail != "logcat" else b"", stderr=b"logcat failed" if self.fail == "logcat" else b"")
        return result(code=1, stderr=f"unexpected fake ADB command: {args}".encode())


class PixelAdbBindingResolveTest(unittest.TestCase):
    def run_binding(self, fake: FakeAdb, output: Path) -> dict[str, object]:
        apk = output.parent / "local.apk"
        apk.write_bytes(APK_BYTES)
        digest = hashlib.sha256(APK_BYTES).hexdigest()
        with patch.object(binding, "EXPECTED_APK_SHA256", digest), patch.object(
            binding, "EXPECTED_SERIAL_SHA256", SERIAL_SHA256
        ), patch.object(binding, "run_command", side_effect=fake.run):
            return binding.resolve("/fake/adb", output, apk)

    def test_already_online_pixel_alias_is_resolved_when_mdns_temporarily_has_no_rows(self):
        fake = FakeAdb(
            mdns(),
            [devices(f"{PIXEL_ALIAS} device product:tokay model:Pixel_9 transport_id:4")],
        )
        with tempfile.TemporaryDirectory() as temp:
            result = self.run_binding(fake, Path(temp) / "binding")

        self.assertEqual(result["target"], PIXEL_ALIAS)
        self.assertEqual(result["target_source"], "current-online-tls-connect-alias-unmapped-or-ambiguous-endpoint")
        self.assertIsNone(result["endpoint_last_resolved"])
        self.assertFalse(any(call and call[0] == "connect" for call in fake.calls))
        self.assertFalse(any(call and call[0] == "pair" for call in fake.calls))
        self.assertTrue(any(call[:3] == ["-s", PIXEL_ALIAS, "shell"] for call in fake.calls if len(call) >= 3))

    def test_online_serial_qualified_alias_still_requires_android_identity_match(self):
        fake = FakeAdb(
            mdns(),
            [devices(f"{PIXEL_ALIAS} device product:other model:Other_Phone transport_id:4")],
            identities={PIXEL_ALIAS: {"serial": "OTHER-ANDROID-001", "product": "other", "model": "Other Phone"}},
        )
        with tempfile.TemporaryDirectory() as temp:
            with self.assertRaisesRegex(binding.ResolutionError, "no currently online wireless ADB target"):
                self.run_binding(fake, Path(temp) / "binding")
        self.assertFalse(any(call and call[0] == "connect" for call in fake.calls))
        self.assertFalse(any(call and call[0] == "pair" for call in fake.calls))

    def test_standard_mdns_rows_bind_existing_pixel_alias_without_unrelated_connect(self):
        fake = FakeAdb(
            mdns(
                f"{PIXEL_INSTANCE} {binding.TLS_CONNECT} {PIXEL_ENDPOINT}",
                f"{OTHER_INSTANCE} {binding.TLS_CONNECT} {OTHER_ENDPOINT}",
                f"adb-{SERIAL}-pair {binding.TLS_PAIRING} 192.168.50.24:39111",
            ),
            [devices(
                f"{PIXEL_ALIAS} device product:tokay model:Pixel_9 transport_id:4",
                f"{PIXEL_ENDPOINT} device product:tokay model:Pixel_9 transport_id:5",
                f"{OTHER_ENDPOINT} device product:other model:Other transport_id:6",
                "emulator-5556 device product:sdk_gphone64_arm64 model:sdk_gphone64_arm64",
            )],
        )
        with tempfile.TemporaryDirectory() as temp:
            output = Path(temp) / "binding"
            bound = self.run_binding(fake, output)
            self.assertEqual(bound["target"], PIXEL_ALIAS)
            self.assertEqual(bound["transport"], "wireless-adb-only")
            self.assertEqual(bound["identity"]["serial_sha256"], SERIAL_SHA256)
            self.assertEqual(bound["installed_apk"]["sha256"], hashlib.sha256(APK_BYTES).hexdigest())
            self.assertEqual(bound["phase_b_final_recheck"]["identity"], "SAME_PIXEL_STABLE_IDENTITY")
            self.assertEqual(bound["phase_b_final_recheck"]["installed_apk"]["sha256"], hashlib.sha256(APK_BYTES).hexdigest())
            self.assertFalse(any(call and call[0] in {"connect", "disconnect", "pair"} for call in fake.calls))
            self.assertGreaterEqual(sum(call[-1:] == ["get-state"] for call in fake.calls), 2)
            self.assertTrue((output / "binding.json").is_file())
            log = (output / "binding.log").read_text()
            self.assertNotIn("private log line", log)
            self.assertNotIn("com.android.launcher", log)
            self.assertNotIn(PIXEL_ALIAS, log)
            self.assertNotIn(PIXEL_ENDPOINT, log)
            self.assertIn("target-sha256=", log)
            # The ignored private binding is the scoped record that may carry
            # the ephemeral ADB target needed by the immediately following phase.
            self.assertEqual(bound["target"], PIXEL_ALIAS)

    def test_cli_failure_diagnostics_do_not_echo_raw_target_or_endpoint(self):
        secret_error = f"target={PIXEL_ALIAS} endpoint={PIXEL_ENDPOINT}"
        stdout = io.StringIO()
        stderr = io.StringIO()
        with tempfile.TemporaryDirectory() as temp:
            with patch.object(binding, "resolve", side_effect=binding.ResolutionError(secret_error)):
                with redirect_stdout(stdout), redirect_stderr(stderr):
                    result = binding.main([
                        "--adb", "/fake/adb",
                        "--output", str(Path(temp) / "binding"),
                        "--apk", str(Path(temp) / "app.apk"),
                    ])

        combined = stdout.getvalue() + stderr.getvalue()
        self.assertEqual(result, 2)
        self.assertNotIn(PIXEL_ALIAS, combined)
        self.assertNotIn(PIXEL_ENDPOINT, combined)
        self.assertIn("PIXEL_BINDING_NOT_PASS", combined)

    def test_cli_argument_errors_do_not_echo_raw_target_or_endpoint(self):
        for secret_argument in (PIXEL_ALIAS, PIXEL_ENDPOINT):
            with self.subTest(secret_argument=secret_argument):
                stderr = io.StringIO()
                with redirect_stdout(io.StringIO()), redirect_stderr(stderr):
                    with self.assertRaises(SystemExit) as raised:
                        binding.main([
                            "--adb", "/fake/adb",
                            "--output", "/tmp/private-binding",
                            "--apk", "/tmp/private-app.apk",
                            secret_argument,
                        ])
                self.assertEqual(raised.exception.code, 2)
                self.assertEqual(stderr.getvalue(), "invalid command-line arguments\n")
                self.assertNotIn(PIXEL_ALIAS, stderr.getvalue())
                self.assertNotIn(PIXEL_ENDPOINT, stderr.getvalue())

    def test_cli_success_summary_omits_raw_target_endpoint_and_serial(self):
        fake = FakeAdb(
            mdns(f"{PIXEL_INSTANCE} {binding.TLS_CONNECT} {PIXEL_ENDPOINT}"),
            [devices(f"{PIXEL_ALIAS} device product:tokay model:Pixel_9 transport_id:4")],
        )
        stdout = io.StringIO()
        stderr = io.StringIO()
        with tempfile.TemporaryDirectory() as temp:
            output = Path(temp) / "binding"
            apk = Path(temp) / "local.apk"
            apk.write_bytes(APK_BYTES)
            digest = hashlib.sha256(APK_BYTES).hexdigest()
            with patch.object(binding, "EXPECTED_APK_SHA256", digest), patch.object(
                binding, "EXPECTED_SERIAL_SHA256", SERIAL_SHA256
            ), patch.object(binding, "run_command", side_effect=fake.run), redirect_stdout(stdout), redirect_stderr(stderr):
                result = binding.main([
                    "--adb", "/fake/adb",
                    "--output", str(output),
                    "--apk", str(apk),
                ])

            combined = stdout.getvalue() + stderr.getvalue()
            self.assertEqual(result, 0)
            self.assertNotIn(PIXEL_ALIAS, combined)
            self.assertNotIn(PIXEL_ENDPOINT, combined)
            self.assertNotIn(SERIAL, combined)
            summary = json.loads(stdout.getvalue())
            self.assertEqual(summary["result"], "PASS")
            self.assertEqual(summary["target_sha256"], hashlib.sha256(PIXEL_ALIAS.encode()).hexdigest())
            self.assertEqual(summary["stable_identity"]["serial_sha256"], SERIAL_SHA256)
            log = (output / "binding.log").read_text()
            self.assertNotIn(PIXEL_ALIAS, log)
            self.assertNotIn(PIXEL_ENDPOINT, log)
            self.assertNotIn(SERIAL, log)

    def test_connects_only_current_pixel_tls_service_when_binding_is_needed(self):
        fake = FakeAdb(
            mdns(
                f"{PIXEL_INSTANCE} {binding.TLS_CONNECT} {PIXEL_ENDPOINT}",
                f"{OTHER_INSTANCE} {binding.TLS_CONNECT} {OTHER_ENDPOINT}",
            ),
            [
                devices("emulator-5556 device product:sdk_gphone64_arm64 model:sdk_gphone64_arm64"),
                devices(f"{PIXEL_ALIAS} device product:tokay model:Pixel_9 transport_id:7"),
            ],
        )
        with tempfile.TemporaryDirectory() as temp:
            bound = self.run_binding(fake, Path(temp) / "binding")
            self.assertEqual(bound["target"], PIXEL_ALIAS)
            connects = [call[1] for call in fake.calls if call and call[0] == "connect"]
            self.assertEqual(connects, [PIXEL_ENDPOINT])
            self.assertNotIn(OTHER_ENDPOINT, connects)
            self.assertFalse(any(call and call[0] in {"pair", "disconnect"} for call in fake.calls))

    def test_multiple_pixel_services_are_identity_checked_and_canonicalized(self):
        second_instance = f"adb-{SERIAL}-route-b"
        second_alias = f"{second_instance}.{binding.TLS_CONNECT}"
        second_endpoint = "192.168.50.25:33186"
        fake = FakeAdb(
            mdns(
                f"{PIXEL_INSTANCE} {binding.TLS_CONNECT} {PIXEL_ENDPOINT}",
                f"{second_instance} {binding.TLS_CONNECT} {second_endpoint}",
            ),
            [devices(
                f"{PIXEL_ALIAS} device product:tokay model:Pixel_9 transport_id:7",
                f"{second_alias} device product:tokay model:Pixel_9 transport_id:8",
            )],
        )
        with tempfile.TemporaryDirectory() as temp:
            bound = self.run_binding(fake, Path(temp) / "binding")
            self.assertEqual(bound["target"], min(PIXEL_ALIAS, second_alias))
            self.assertEqual(set(call[1] for call in fake.calls if call and call[0] == "connect"), set())
            self.assertFalse(any(call and call[0] == "disconnect" for call in fake.calls))

    def test_duplicate_service_alias_with_different_endpoints_uses_live_alias_without_guessing(self):
        fake = FakeAdb(
            mdns(
                f"{PIXEL_INSTANCE} {binding.TLS_CONNECT} {PIXEL_ENDPOINT}",
                f"{PIXEL_INSTANCE} {binding.TLS_CONNECT} 192.168.50.25:33186",
            ),
            [devices(f"{PIXEL_ALIAS} device product:tokay model:Pixel_9 transport_id:4")],
        )
        with tempfile.TemporaryDirectory() as temp:
            output = Path(temp) / "binding"
            bound = self.run_binding(fake, output)
            self.assertEqual(bound["target"], PIXEL_ALIAS)
            self.assertIsNone(bound["endpoint_last_resolved"])
            self.assertEqual(bound["target_source"], "current-online-tls-connect-alias-unmapped-or-ambiguous-endpoint")
            self.assertFalse(any(call and call[0] in {"connect", "disconnect", "pair"} for call in fake.calls))

    def test_detaches_new_nonpixel_route_when_pixel_is_seen_in_same_refreshed_snapshot(self):
        second_instance = f"adb-{SERIAL}-route-b"
        second_alias = f"{second_instance}.{binding.TLS_CONNECT}"
        second_endpoint = "192.168.50.25:33186"
        fake = FakeAdb(
            mdns(
                f"{PIXEL_INSTANCE} {binding.TLS_CONNECT} {PIXEL_ENDPOINT}",
                f"{second_instance} {binding.TLS_CONNECT} {second_endpoint}",
            ),
            [
                devices("emulator-5556 device product:sdk_gphone64_arm64 model:sdk_gphone64_arm64"),
                devices(
                    f"{PIXEL_ENDPOINT} device transport_id:7",
                    f"{second_alias} device product:tokay model:Pixel_9 transport_id:8",
                ),
            ],
            identities={PIXEL_ENDPOINT: {
                "serial": "OTHER-ANDROID-001",
                "model": "Other Phone",
                "product": "other",
                "sdk": "36",
                "fingerprint": "vendor/other/other:16/build:keys",
            }},
        )
        with tempfile.TemporaryDirectory() as temp:
            bound = self.run_binding(fake, Path(temp) / "binding")
            disconnects = [call[1] for call in fake.calls if call and call[0] == "disconnect"]
            self.assertEqual(bound["target"], second_alias)
            self.assertEqual(disconnects, [PIXEL_ENDPOINT])
            self.assertEqual(bound["adb_connect_created_nonpixel_routes_detached"], [PIXEL_ENDPOINT])

    def test_final_recheck_requires_boot_id_when_initial_identity_had_one(self):
        target = binding.Identity(
            target=PIXEL_ALIAS,
            source="test",
            endpoint=PIXEL_ENDPOINT,
            service_instance=PIXEL_INSTANCE,
            serial_sha256=SERIAL_SHA256,
            model=binding.EXPECTED_MODEL,
            product=binding.EXPECTED_PRODUCT,
            sdk="37",
            fingerprint="google/tokay/tokay:17/build:keys",
            route_boot_id="11111111-1111-1111-1111-111111111111",
        )
        current = binding.Identity(
            target=target.target,
            source="phase-b-final-recheck",
            endpoint=target.endpoint,
            service_instance=target.service_instance,
            serial_sha256=target.serial_sha256,
            model=target.model,
            product=target.product,
            sdk=target.sdk,
            fingerprint=target.fingerprint,
            route_boot_id="",
        )
        with tempfile.TemporaryDirectory() as temp:
            with patch.object(binding, "identity_for", return_value=current):
                with self.assertRaisesRegex(binding.ResolutionError, "lost the previously available boot identity"):
                    binding.verify_phase_b_final(
                        "/fake/adb", Path(temp), Path(temp) / "local.apk", Path(temp) / "binding.log", target, {}
                    )

    def test_final_recheck_rejects_changed_boot_id(self):
        target = binding.Identity(
            target=PIXEL_ALIAS,
            source="test",
            endpoint=PIXEL_ENDPOINT,
            service_instance=PIXEL_INSTANCE,
            serial_sha256=SERIAL_SHA256,
            model=binding.EXPECTED_MODEL,
            product=binding.EXPECTED_PRODUCT,
            sdk="37",
            fingerprint="google/tokay/tokay:17/build:keys",
            route_boot_id="11111111-1111-1111-1111-111111111111",
        )
        current = binding.Identity(
            target=target.target,
            source="phase-b-final-recheck",
            endpoint=target.endpoint,
            service_instance=target.service_instance,
            serial_sha256=target.serial_sha256,
            model=target.model,
            product=target.product,
            sdk=target.sdk,
            fingerprint=target.fingerprint,
            route_boot_id="22222222-2222-2222-2222-222222222222",
        )
        with tempfile.TemporaryDirectory() as temp:
            with patch.object(binding, "identity_for", return_value=current):
                with self.assertRaisesRegex(binding.ResolutionError, "boot identity changed"):
                    binding.verify_phase_b_final(
                        "/fake/adb", Path(temp), Path(temp) / "local.apk", Path(temp) / "binding.log", target, {}
                    )

    def test_ambiguous_service_names_are_read_only_identity_checked(self):
        pixel_instance = "adb-route-pixel"
        other_instance = "adb-route-other"
        pixel_alias = f"{pixel_instance}.{binding.TLS_CONNECT}"
        other_alias = f"{other_instance}.{binding.TLS_CONNECT}"
        services = mdns(
            f"{pixel_instance} {binding.TLS_CONNECT} {PIXEL_ENDPOINT}",
            f"{other_instance} {binding.TLS_CONNECT} {OTHER_ENDPOINT}",
            f"adb-route-pair {binding.TLS_PAIRING} 192.168.50.24:39111",
        )
        fake = FakeAdb(
            services,
            [
                devices("emulator-5556 device product:sdk_gphone64_arm64 model:sdk_gphone64_arm64"),
                devices(f"{PIXEL_ENDPOINT} device transport_id:7"),
            ],
        )
        with tempfile.TemporaryDirectory() as temp:
            output = Path(temp) / "binding"
            bound = self.run_binding(fake, output)
            self.assertEqual(bound["target"], PIXEL_ENDPOINT)
            self.assertEqual(bound["identity"]["serial_sha256"], SERIAL_SHA256)
            connects = {call[1] for call in fake.calls if call and call[0] == "connect"}
            disconnects = {call[1] for call in fake.calls if call and call[0] == "disconnect"}
            self.assertEqual(connects, {PIXEL_ENDPOINT})
            self.assertEqual(disconnects, set())
            self.assertTrue(bound["adb_connect_attempted"])
            self.assertEqual(bound["adb_connect_created_nonpixel_routes_detached"], [])
            self.assertFalse(any(call and call[0] == "pair" for call in fake.calls))

    def test_already_bound_pixel_does_not_connect_other_ambiguous_services(self):
        pixel_endpoint = "192.168.50.24:33185"
        other_endpoint = "192.168.50.30:44771"
        services = mdns(
            f"adb-route-pixel {binding.TLS_CONNECT} {pixel_endpoint}",
            f"adb-route-other {binding.TLS_CONNECT} {other_endpoint}",
        )
        fake = FakeAdb(
            services,
            [devices(
                f"{pixel_endpoint} device product:tokay model:Pixel_9 transport_id:7",
                f"{other_endpoint} device product:other model:Other transport_id:8",
            )],
            identities={other_endpoint: {
                "serial": "OTHER-ANDROID-001",
                "model": "Other Phone",
                "product": "other",
                "sdk": "36",
                "fingerprint": "vendor/other/other:16/build:keys",
            }},
        )
        with tempfile.TemporaryDirectory() as temp:
            bound = self.run_binding(fake, Path(temp) / "binding")
            self.assertEqual(bound["target"], pixel_endpoint)
            self.assertFalse(bound["adb_connect_attempted"])
            self.assertEqual(bound["adb_connect_attempted_endpoints"], [])
            self.assertFalse(any(call and call[0] in {"connect", "disconnect"} for call in fake.calls))

    def test_ambiguous_services_are_probed_one_at_a_time_and_proven_nonpixel_route_is_detached(self):
        nonpixel_endpoint = "192.168.50.20:44771"
        pixel_endpoint = "192.168.50.24:33185"
        services = mdns(
            f"adb-route-nonpixel {binding.TLS_CONNECT} {nonpixel_endpoint}",
            f"adb-route-pixel {binding.TLS_CONNECT} {pixel_endpoint}",
        )
        fake = FakeAdb(
            services,
            [
                devices("emulator-5556 device product:sdk_gphone64_arm64 model:sdk_gphone64_arm64"),
                devices(f"{nonpixel_endpoint} device transport_id:8"),
                devices(f"{pixel_endpoint} device transport_id:9"),
            ],
            identities={nonpixel_endpoint: {
                "serial": "OTHER-ANDROID-001",
                "model": "Other Phone",
                "product": "other",
                "sdk": "36",
                "fingerprint": "vendor/other/other:16/build:keys",
            }},
        )
        with tempfile.TemporaryDirectory() as temp:
            bound = self.run_binding(fake, Path(temp) / "binding")
            connects = [call[1] for call in fake.calls if call and call[0] == "connect"]
            disconnects = [call[1] for call in fake.calls if call and call[0] == "disconnect"]
            self.assertEqual(connects, [nonpixel_endpoint, pixel_endpoint])
            self.assertEqual(disconnects, [nonpixel_endpoint])
            self.assertEqual(bound["target"], pixel_endpoint)
            self.assertEqual(bound["adb_connect_created_nonpixel_routes_detached"], [nonpixel_endpoint])
            self.assertEqual(bound["adb_connect_created_pixel_route_left_bound"], [pixel_endpoint])

    def test_serial_looking_nonpixel_service_does_not_hide_generic_pixel_service(self):
        prefixed_nonpixel_endpoint = "192.168.50.21:44771"
        generic_pixel_endpoint = "192.168.50.24:33185"
        services = mdns(
            f"{PIXEL_INSTANCE} {binding.TLS_CONNECT} {prefixed_nonpixel_endpoint}",
            f"adb-current-route {binding.TLS_CONNECT} {generic_pixel_endpoint}",
        )
        fake = FakeAdb(
            services,
            [
                devices("emulator-5556 device product:sdk_gphone64_arm64 model:sdk_gphone64_arm64"),
                devices(f"{prefixed_nonpixel_endpoint} device transport_id:8"),
                devices(f"{generic_pixel_endpoint} device transport_id:9"),
            ],
            identities={prefixed_nonpixel_endpoint: {
                "serial": "OTHER-ANDROID-001",
                "model": "Other Phone",
                "product": "other",
                "sdk": "36",
                "fingerprint": "vendor/other/other:16/build:keys",
            }},
        )

        with tempfile.TemporaryDirectory() as temp:
            bound = self.run_binding(fake, Path(temp) / "binding")

        connects = [call[1] for call in fake.calls if call and call[0] == "connect"]
        disconnects = [call[1] for call in fake.calls if call and call[0] == "disconnect"]
        self.assertEqual(connects, [prefixed_nonpixel_endpoint, generic_pixel_endpoint])
        self.assertEqual(disconnects, [prefixed_nonpixel_endpoint])
        self.assertEqual(bound["target"], generic_pixel_endpoint)
        self.assertEqual(bound["identity"]["serial_sha256"], SERIAL_SHA256)
        self.assertEqual(bound["adb_connect_created_nonpixel_routes_detached"], [prefixed_nonpixel_endpoint])
        self.assertEqual(bound["adb_connect_created_pixel_route_left_bound"], [generic_pixel_endpoint])

    def test_boot_id_disagreement_keeps_multiple_matching_pixels_ambiguous(self):
        second_instance = f"adb-{SERIAL}-route-b"
        second_alias = f"{second_instance}.{binding.TLS_CONNECT}"
        second_endpoint = "192.168.50.25:33186"
        fake = FakeAdb(
            mdns(
                f"{PIXEL_INSTANCE} {binding.TLS_CONNECT} {PIXEL_ENDPOINT}",
                f"{second_instance} {binding.TLS_CONNECT} {second_endpoint}",
            ),
            [
                devices("emulator-5556 device product:sdk_gphone64_arm64 model:sdk_gphone64_arm64"),
                devices(
                    f"{PIXEL_ALIAS} device product:tokay model:Pixel_9 transport_id:7",
                    f"{second_alias} device product:tokay model:Pixel_9 transport_id:8",
                ),
            ],
            identities={second_alias: {"route_boot_id": "22222222-2222-2222-2222-222222222222"}},
        )
        with tempfile.TemporaryDirectory() as temp:
            with self.assertRaises(binding.ResolutionError):
                self.run_binding(fake, Path(temp) / "binding")
            self.assertFalse(any(call and call[0] == "pair" for call in fake.calls))

    def test_pairing_only_is_ignored_and_unrelated_current_route_is_detached_after_read_only_probe(self):
        fake = FakeAdb(
            mdns(
                f"adb-{SERIAL}-pair {binding.TLS_PAIRING} 192.168.50.24:39111",
                f"{OTHER_INSTANCE} {binding.TLS_CONNECT} {OTHER_ENDPOINT}",
            ),
            [
                devices("emulator-5556 device product:sdk_gphone64_arm64 model:sdk_gphone64_arm64"),
                devices(f"{OTHER_ENDPOINT} device product:other model:Other transport_id:6"),
            ],
            identities={OTHER_ENDPOINT: {
                "serial": "OTHER-ANDROID-001",
                "model": "Other Phone",
                "product": "other",
                "sdk": "36",
                "fingerprint": "vendor/other/other:16/build:keys",
            }},
        )
        with tempfile.TemporaryDirectory() as temp:
            with self.assertRaises(binding.ResolutionError):
                self.run_binding(fake, Path(temp) / "binding")
            connects = [call[1] for call in fake.calls if call and call[0] == "connect"]
            disconnects = [call[1] for call in fake.calls if call and call[0] == "disconnect"]
            self.assertEqual(connects, [OTHER_ENDPOINT])
            self.assertEqual(disconnects, [OTHER_ENDPOINT])
            self.assertFalse(any(call and call[0] == "pair" for call in fake.calls))

    def test_failed_cleanup_stops_before_trying_another_endpoint(self):
        nonpixel_endpoint = "192.168.50.20:44771"
        pixel_endpoint = "192.168.50.24:33185"
        fake = FakeAdb(
            mdns(
                f"adb-route-nonpixel {binding.TLS_CONNECT} {nonpixel_endpoint}",
                f"adb-route-pixel {binding.TLS_CONNECT} {pixel_endpoint}",
            ),
            [
                devices("emulator-5556 device product:sdk_gphone64_arm64 model:sdk_gphone64_arm64"),
                devices(f"{nonpixel_endpoint} device transport_id:8"),
            ],
            fail="disconnect",
            identities={nonpixel_endpoint: {
                "serial": "OTHER-ANDROID-001",
                "model": "Other Phone",
                "product": "other",
                "sdk": "36",
                "fingerprint": "vendor/other/other:16/build:keys",
            }},
        )
        with tempfile.TemporaryDirectory() as temp:
            with self.assertRaises(binding.ResolutionError):
                self.run_binding(fake, Path(temp) / "binding")
            connects = [call[1] for call in fake.calls if call and call[0] == "connect"]
            self.assertEqual(connects, [nonpixel_endpoint])

    def test_unmapped_tcp_device_row_is_not_queried_as_a_remembered_target(self):
        stale_endpoint = "192.168.50.24:33185"
        fake = FakeAdb("List of discovered mdns services\n", [devices(f"{stale_endpoint} device transport_id:3")])
        with tempfile.TemporaryDirectory() as temp:
            with self.assertRaises(binding.ResolutionError):
                self.run_binding(fake, Path(temp) / "binding")
            self.assertFalse(any(call[:2] == ["-s", stale_endpoint] for call in fake.calls if call))

    def test_conflicting_identity_across_current_pixel_routes_fails_closed(self):
        second_instance = f"adb-{SERIAL}-route-b"
        second_alias = f"{second_instance}.{binding.TLS_CONNECT}"
        second_endpoint = "192.168.50.25:33186"
        fake = FakeAdb(
            mdns(
                f"{PIXEL_INSTANCE} {binding.TLS_CONNECT} {PIXEL_ENDPOINT}",
                f"{second_instance} {binding.TLS_CONNECT} {second_endpoint}",
            ),
            [
                devices("emulator-5556 device product:sdk_gphone64_arm64 model:sdk_gphone64_arm64"),
                devices(
                    f"{PIXEL_ALIAS} device product:tokay model:Pixel_9 transport_id:7",
                    f"{second_alias} device product:tokay model:Pixel_9 transport_id:8",
                ),
            ],
            fingerprints={second_alias: "different/build/fingerprint"},
        )
        with tempfile.TemporaryDirectory() as temp:
            with self.assertRaises(binding.ResolutionError):
                self.run_binding(fake, Path(temp) / "binding")

    def test_nonempty_output_path_is_never_overwritten(self):
        fake = FakeAdb("", [devices("emulator-5556 device")])
        with tempfile.TemporaryDirectory() as temp:
            output = Path(temp) / "binding"
            output.mkdir()
            sentinel = output / "binding.json"
            sentinel.write_text("owner data\n")
            with self.assertRaises(binding.ResolutionError):
                self.run_binding(fake, output)
            self.assertEqual(sentinel.read_text(), "owner data\n")
            self.assertEqual(fake.calls, [])

    def test_multiple_base_paths_fail_before_pull(self):
        fake = FakeAdb(
            mdns(f"{PIXEL_INSTANCE} {binding.TLS_CONNECT} {PIXEL_ENDPOINT}"),
            [devices(f"{PIXEL_ALIAS} device product:tokay model:Pixel_9 transport_id:4")],
            pm_paths="package:/data/app/a/base.apk\npackage:/data/app/b/base.apk\n",
        )
        with tempfile.TemporaryDirectory() as temp:
            with self.assertRaises(binding.ResolutionError):
                self.run_binding(fake, Path(temp) / "binding")
            self.assertFalse(any(len(call) >= 3 and call[2] == "pull" for call in fake.calls))

    def test_capture_path_failure_is_not_recorded_as_ready(self):
        for fail in ("shell", "screenshot", "logcat"):
            with self.subTest(fail=fail):
                fake = FakeAdb(
                    mdns(f"{PIXEL_INSTANCE} {binding.TLS_CONNECT} {PIXEL_ENDPOINT}"),
                    [devices(f"{PIXEL_ALIAS} device product:tokay model:Pixel_9 transport_id:4")],
                    fail=fail,
                )
                with tempfile.TemporaryDirectory() as temp:
                    with self.assertRaises(binding.ResolutionError):
                        self.run_binding(fake, Path(temp) / "binding")

    def test_capture_rejects_truncated_png_stream(self):
        fake = FakeAdb(
            mdns(f"{PIXEL_INSTANCE} {binding.TLS_CONNECT} {PIXEL_ENDPOINT}"),
            [devices(f"{PIXEL_ALIAS} device product:tokay model:Pixel_9 transport_id:4")],
            fail="screenshot-truncated",
        )
        with tempfile.TemporaryDirectory() as temp:
            output = Path(temp) / "binding"
            with self.assertRaisesRegex(binding.ResolutionError, "did not return a PNG"):
                self.run_binding(fake, output)
            self.assertFalse((output / "binding.json").exists())

    def test_valid_png_validator_checks_structure_crc_and_image_stream(self):
        self.assertTrue(binding.valid_png_stream(PNG_BYTES))
        self.assertFalse(binding.valid_png_stream(PNG_BYTES[:-4]))
        corrupted = bytearray(PNG_BYTES)
        corrupted[-5] ^= 0x01
        self.assertFalse(binding.valid_png_stream(bytes(corrupted)))

    def test_png_validator_enforces_indexed_palette_depth_and_allows_truecolor_alpha_palette(self):
        self.assertTrue(binding.valid_png_stream(indexed_png(1, 2)))
        self.assertFalse(binding.valid_png_stream(indexed_png(1, 3)))
        self.assertTrue(binding.valid_png_stream(png_with_palette(2)))
        self.assertTrue(binding.valid_png_stream(png_with_palette(6)))

        for color_type in (0, 4):
            invalid_palette = (
                b"\x89PNG\r\n\x1a\n"
                + png_chunk(b"IHDR", struct.pack(">IIBBBBB", 1, 1, 8, color_type, 0, 0, 0))
                + png_chunk(b"PLTE", bytes(6))
                + png_chunk(b"IDAT", zlib.compress(b"\x00\x00"))
                + png_chunk(b"IEND", b"")
            )
            with self.subTest(color_type=color_type):
                self.assertFalse(binding.valid_png_stream(invalid_palette))

    def test_phase_b_accepts_current_android_17_activity_dump_format(self):
        fake = FakeAdb(
            mdns(),
            [devices(f"{PIXEL_ALIAS} device product:tokay model:Pixel_9 transport_id:4")],
            fail="activity-android17",
        )
        with tempfile.TemporaryDirectory() as temp:
            result = self.run_binding(fake, Path(temp) / "binding")
        self.assertEqual(result["app_entry_readiness"], {"process": "ABSENT", "resumed_activity": "APP_ABSENT"})
        self.assertEqual(result["phase_b_final_recheck"]["app_entry_readiness"], result["app_entry_readiness"])

    def test_running_or_ambiguous_app_entry_state_blocks_readiness(self):
        for fail in ("process", "process-error", "activity"):
            with self.subTest(fail=fail), tempfile.TemporaryDirectory() as temp:
                fake = FakeAdb(
                    mdns(f"{PIXEL_INSTANCE} {binding.TLS_CONNECT} {PIXEL_ENDPOINT}"),
                    [devices(f"{PIXEL_ALIAS} device product:tokay model:Pixel_9 transport_id:4")],
                    fail=fail,
                )
                with self.assertRaises(binding.ResolutionError):
                    self.run_binding(fake, Path(temp) / "binding")
                self.assertFalse((Path(temp) / "binding" / "binding.json").exists())


if __name__ == "__main__":
    unittest.main()
