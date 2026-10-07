#!/usr/bin/env python3
"""Recheck one sealed wireless Pixel binding and clean app entry state.

This Phase C entry guard never discovers services, pairs, connects,
disconnects, queries the DAC, launches the app, or changes device state.
"""

from __future__ import annotations

import argparse
import hashlib
import json
import sys
from datetime import datetime, timezone
from pathlib import Path

import pixel_adb_binding as adb_binding


def verify_existing_binding(
    adb: str,
    binding_path: Path,
    target: str,
    expected_binding_sha256: str,
    log: Path,
) -> dict[str, str]:
    raw = binding_path.read_bytes()
    if len(expected_binding_sha256) != 64 or any(
        character not in "0123456789abcdef" for character in expected_binding_sha256
    ):
        raise adb_binding.ResolutionError("expected binding SHA-256 is malformed")
    if hashlib.sha256(raw).hexdigest() != expected_binding_sha256:
        raise adb_binding.ResolutionError("binding record differs from the sealed Phase B SHA-256")
    binding = json.loads(raw)
    if not isinstance(binding, dict):
        raise adb_binding.ResolutionError("binding record is not an object")
    if binding.get("transport") != "wireless-adb-only" or binding.get("pairing_performed") is not False:
        raise adb_binding.ResolutionError("binding transport is not the sealed wireless-only, already-paired path")
    if not target or target != binding.get("target"):
        raise adb_binding.ResolutionError("selected target does not exactly match the sealed binding")
    if not adb_binding.is_pixel_service_alias(target):
        raise adb_binding.ResolutionError("selected target is not the bound Pixel TLS-connect wireless alias")

    installed = binding.get("installed_apk")
    if not isinstance(installed, dict) or installed.get("package") != adb_binding.PACKAGE:
        raise adb_binding.ResolutionError("binding package identity is incomplete")
    if installed.get("sha256") != adb_binding.EXPECTED_APK_SHA256:
        raise adb_binding.ResolutionError("binding APK hash does not match the frozen candidate")

    expected = binding.get("identity")
    if not isinstance(expected, dict):
        raise adb_binding.ResolutionError("binding Pixel identity is incomplete")
    stable_fields = ("serial", "model", "product", "sdk", "fingerprint")
    if any(not isinstance(expected.get(key), str) or not expected[key] for key in stable_fields):
        raise adb_binding.ResolutionError("binding Pixel identity has an empty stable field")
    if (
        expected["serial"] != adb_binding.EXPECTED_SERIAL
        or adb_binding.normalize_model(expected["model"]) != adb_binding.EXPECTED_MODEL
        or expected["product"] != adb_binding.EXPECTED_PRODUCT
        or not expected["sdk"].isdigit()
        or not expected["fingerprint"].startswith("google/tokay/tokay:")
    ):
        raise adb_binding.ResolutionError("binding identity is not the previously qualified Pixel 9")

    observed = adb_binding.identity_for(adb, target, "phase-c-entry-recheck", None, None)
    actual = (observed.serial, observed.model, observed.product, observed.sdk, observed.fingerprint)
    sealed = tuple(expected[key] for key in stable_fields)
    if actual != sealed:
        raise adb_binding.ResolutionError("current target stable identity differs from the sealed Pixel binding")

    app_entry = adb_binding.verify_app_entry_ready(adb, target, log)
    result = {
        "checked_at_utc": datetime.now(timezone.utc).isoformat(timespec="seconds"),
        "result": "PASS",
        "transport": "wireless-adb-only",
        "target_sha256": hashlib.sha256(target.encode("utf-8")).hexdigest(),
        "binding_sha256": hashlib.sha256(raw).hexdigest(),
        "android_serial_sha256": hashlib.sha256(observed.serial.encode("utf-8")).hexdigest(),
        "stable_identity_match": "PASS",
        "app_process": app_entry["process"],
        "resumed_activity": app_entry["resumed_activity"],
        "dac_query": "NOT_PERFORMED",
        "app_launch": "NOT_PERFORMED",
        "endpoint_discovery_or_reconnect": "NOT_PERFORMED",
    }
    return result


def main(argv: list[str] | None = None) -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--adb", required=True, help="absolute path to Android SDK platform-tools/adb")
    parser.add_argument("--binding", required=True, type=Path, help="ignored Phase B binding.json")
    parser.add_argument("--expected-binding-sha256", required=True, help="exact sealed Phase B binding file SHA-256")
    parser.add_argument("--target", required=True, help="exact target string already stored in binding.json")
    parser.add_argument("--log", required=True, type=Path, help="ignored local log used by the app-absence checkpoint")
    parser.add_argument("--result", required=True, type=Path, help="ignored path for sanitized JSON result")
    args = parser.parse_args(argv)
    try:
        result = verify_existing_binding(
            str(args.adb), args.binding, args.target, args.expected_binding_sha256, args.log
        )
        args.result.write_text(json.dumps(result, indent=2) + "\n", encoding="utf-8")
    except (adb_binding.ResolutionError, OSError, ValueError, TypeError, KeyError, json.JSONDecodeError):
        try:
            adb_binding.append_log(args.log, f"{datetime.now(timezone.utc).isoformat(timespec='seconds')} phase-c-entry=NOT_PASS")
        except OSError:
            pass
        print("PIXEL_ENTRY_NOT_PASS: sealed wireless identity or clean app-entry state was not confirmed", file=sys.stderr)
        return 2
    print("PIXEL_ENTRY_PASS: stable Pixel identity matches; app process and resumed activity are absent")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
