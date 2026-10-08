#!/usr/bin/env python3
"""Exercise signed stable upgrade and clean-install paths on an API 35 emulator.

The workflow verifies APK signatures and provenance before invoking this helper.
This script uses only the product UI to seed/check user state and never operates
hardware. Each upgrade invocation expects a freshly wiped/uninstalled app state.
"""

from __future__ import annotations

import argparse
import os
import re
import subprocess
import sys
import time
import xml.etree.ElementTree as ET
from pathlib import Path
from typing import Sequence


PACKAGE = "com.weekssa.opraeqforuapp"
ACTIVITY = f"{PACKAGE}/.MainActivity"
FIXTURE_MANUFACTURER = "Release Test"
FIXTURE_MODEL = "Upgrade Fixture"
FIXTURE_EQ_NAME = "Stable Upgrade Fixture"
FIXTURE_PEQ = "Filter 1: ON PK Fc 100 Hz Gain 1 dB Q 1.0"
BOUNDS_RE = re.compile(r"\[(\d+),(\d+)\]\[(\d+),(\d+)\]")


class SmokeError(RuntimeError):
    pass


class Device:
    def __init__(self, adb: str, output: Path, timeout: int = 60) -> None:
        self.adb = adb
        self.output = output
        self.timeout = timeout
        self.output.mkdir(parents=True, exist_ok=True)
        self.width, self.height = self._screen_size()

    def run(self, *args: str, timeout: int | None = None, check: bool = True) -> str:
        result = subprocess.run(
            [self.adb, *args],
            check=False,
            text=True,
            stdout=subprocess.PIPE,
            stderr=subprocess.STDOUT,
            timeout=timeout or self.timeout,
        )
        if check and result.returncode != 0:
            raise SmokeError(f"adb {' '.join(args)} failed ({result.returncode}):\n{result.stdout}")
        return result.stdout

    def shell(self, *args: str, timeout: int | None = None, check: bool = True) -> str:
        return self.run("shell", *args, timeout=timeout, check=check)

    def _screen_size(self) -> tuple[int, int]:
        raw = self.shell("wm", "size")
        match = re.search(r"Physical size:\s*(\d+)x(\d+)", raw)
        if match is None:
            raise SmokeError(f"Could not read emulator screen size: {raw}")
        return int(match.group(1)), int(match.group(2))

    def dump(self, name: str) -> ET.Element:
        path = f"/sdcard/{name}.xml"
        self.shell("uiautomator", "dump", path, timeout=30, check=False)
        xml = self.shell("cat", path, timeout=30)
        start = xml.find("<hierarchy")
        end = xml.rfind("</hierarchy>")
        if start < 0 or end < start:
            raise SmokeError(f"UI hierarchy {name} was not valid XML:\n{xml[:1000]}")
        content = xml[start:end + len("</hierarchy>")]
        (self.output / f"{name}.xml").write_text(content + "\n", encoding="utf-8")
        try:
            return ET.fromstring(content)
        except ET.ParseError as error:
            raise SmokeError(f"UI hierarchy {name} could not be parsed: {error}") from error

    @staticmethod
    def _node_value(node: ET.Element) -> str:
        return " ".join(
            node.attrib.get(key, "")
            for key in ("text", "content-desc", "contentDescription", "hint")
        ).strip()

    def find_node(self, labels: Sequence[str], *, contains: bool = False) -> ET.Element | None:
        root = self.dump("current")
        wanted = tuple(label.casefold() for label in labels)
        for node in root.iter("node"):
            values = tuple(
                node.attrib.get(key, "").casefold()
                for key in ("text", "content-desc", "contentDescription", "hint")
            )
            if any(
                expected in value if contains else expected == value
                for expected in wanted
                for value in values
                if value
            ):
                if BOUNDS_RE.fullmatch(node.attrib.get("bounds", "")):
                    return node
        return None

    def has_text(self, label: str, *, contains: bool = False) -> bool:
        return self.find_node((label,), contains=contains) is not None

    def wait_text(self, labels: Sequence[str], *, contains: bool = False, timeout: int = 60) -> ET.Element:
        deadline = time.monotonic() + timeout
        while time.monotonic() < deadline:
            node = self.find_node(labels, contains=contains)
            if node is not None:
                return node
            time.sleep(2)
        raise SmokeError(f"Timed out waiting for visible text among {tuple(labels)!r}")

    def tap_node(self, node: ET.Element) -> None:
        match = BOUNDS_RE.fullmatch(node.attrib.get("bounds", ""))
        if match is None:
            raise SmokeError(f"Visible UI node has no bounds: {node.attrib}")
        left, top, right, bottom = map(int, match.groups())
        self.shell("input", "tap", str((left + right) // 2), str((top + bottom) // 2))

    def tap_text(self, labels: Sequence[str], *, contains: bool = False, timeout: int = 45) -> None:
        deadline = time.monotonic() + timeout
        swipe_count = 0
        while time.monotonic() < deadline:
            node = self.find_node(labels, contains=contains)
            if node is not None:
                self.tap_node(node)
                time.sleep(0.35)
                return
            if swipe_count < 4:
                center_x = self.width // 2
                self.shell(
                    "input", "swipe", str(center_x), str(self.height * 4 // 5),
                    str(center_x), str(self.height // 3), "350",
                )
                swipe_count += 1
            time.sleep(1)
        raise SmokeError(f"Could not tap visible UI text among {tuple(labels)!r}")

    def type_text(self, label: str, value: str, *, contains: bool = False) -> None:
        self.tap_text((label,), contains=contains)
        encoded = value.replace("%", "\\%").replace(" ", "%s")
        self.shell("input", "text", encoded)
        self.shell("input", "keyevent", "4")

    def screenshot(self, name: str) -> None:
        data = subprocess.run(
            [self.adb, "exec-out", "screencap", "-p"],
            check=False,
            stdout=subprocess.PIPE,
            stderr=subprocess.PIPE,
            timeout=30,
        )
        if data.returncode != 0 or not data.stdout.startswith(b"\x89PNG\r\n\x1a\n"):
            raise SmokeError(f"Could not capture emulator screenshot {name}: {data.stderr.decode(errors='replace')}")
        (self.output / f"{name}.png").write_bytes(data.stdout)

    def installed_package_dump(self) -> str:
        value = self.shell("dumpsys", "package", PACKAGE, timeout=30)
        (self.output / "package-dump.txt").write_text(value, encoding="utf-8")
        return value

    def assert_version(self, name: str, code: str) -> None:
        package_dump = self.installed_package_dump()
        if not re.search(rf"\bversionCode={re.escape(code)}(?:\s|$)", package_dump):
            raise SmokeError(f"Installed package did not report versionCode {code}")
        if not re.search(rf"\bversionName={re.escape(name)}(?:\s|$)", package_dump):
            raise SmokeError(f"Installed package did not report versionName {name}")
        paths = self.shell("pm", "path", PACKAGE)
        if not paths.strip().startswith("package:"):
            raise SmokeError("Package Manager cannot locate the installed application")

    def launch(self) -> None:
        self.shell("am", "force-stop", PACKAGE)
        result = self.shell("am", "start", "-W", "-n", ACTIVITY, timeout=60)
        (self.output / "launch.txt").write_text(result, encoding="utf-8")
        if "Status: ok" not in result:
            raise SmokeError(f"Cold launch did not report Status: ok:\n{result}")

    def assert_no_fatal_crash(self) -> None:
        crash = self.shell("logcat", "-d", "-b", "crash", "-v", "threadtime", timeout=45)
        (self.output / "crash-logcat.txt").write_text(crash, encoding="utf-8")
        if PACKAGE in crash and ("FATAL EXCEPTION" in crash or "am_crash" in crash):
            raise SmokeError(f"Crash buffer contains a fatal app failure:\n{crash}")


def seed_persisted_state(device: Device) -> None:
    device.tap_text(("My EQs",))
    device.tap_text(("Import", "Import PEQ", "Import Personal EQ"))
    device.wait_text(("Import Personal EQ", "Import personal PEQ"), timeout=30)
    time.sleep(0.4)
    hierarchy = device.dump("import-open")
    visible = " ".join(Device._node_value(node) for node in hierarchy.iter("node"))

    if "Step 1 of 5" in visible or "Step 1 of 5 · Input" in visible:
        device.type_text("Equalizer APO / AutoEq text", FIXTURE_PEQ)
        device.tap_text(("Parse EQ text",))
        device.wait_text(("Step 2 of 5 · Parse",), timeout=30)
        device.tap_text(("Continue to description",))
        device.wait_text(("Describe this EQ",), timeout=30)
        device.type_text("Manufacturer", FIXTURE_MANUFACTURER)
        device.type_text("Headphone model", FIXTURE_MODEL)
        device.type_text("EQ name", FIXTURE_EQ_NAME)
        device.tap_text(("Review EQ",))
        device.wait_text(("Step 4 of 5 · Review",), timeout=30)
        device.tap_text(("Continue to save",))
        device.wait_text(("Step 5 of 5 · Save",), timeout=30)
        device.tap_text(("Save to My EQs",))
    elif is_legacy_personal_eq_import_form(visible):
        device.type_text("Manufacturer", FIXTURE_MANUFACTURER)
        device.type_text("Headphone model", FIXTURE_MODEL)
        device.type_text("EQ name", FIXTURE_EQ_NAME)
        device.type_text("Equalizer APO / AutoEq text", FIXTURE_PEQ)
        device.tap_text(("Save & export", "Save"))
    else:
        raise SmokeError("The expected Personal EQ import form did not open")

    device.wait_text((FIXTURE_EQ_NAME,), timeout=45)
    device.screenshot("seeded-my-eqs")

    device.tap_text(("Settings",))
    device.wait_text(("Output behavior",), timeout=30)
    device.tap_text(("Manual",))
    assert_checked(device, "Manual")
    device.screenshot("seeded-settings-manual")


def is_legacy_personal_eq_import_form(visible: str) -> bool:
    normalized = " ".join(visible.casefold().split())
    has_import_heading = any(
        heading in normalized
        for heading in ("import personal eq", "import personal peq")
    )
    return has_import_heading and all(
        label in normalized
        for label in (
            "manufacturer",
            "headphone model",
            "eq name",
            "paste peq text",
            "equalizer apo / autoeq text",
        )
    )


def assert_checked(device: Device, label: str) -> None:
    node = device.wait_text((label,), timeout=30)
    # Compose's RadioButton semantics are exposed to UiAutomator as the node's
    # checked state. Do not infer a selected preference from its label alone.
    if node.attrib.get("checked", "").lower() != "true":
        raise SmokeError(f"Preference {label!r} was not exposed as checked in accessibility state: {node.attrib}")


def wait_for_catalog(device: Device, prefix: str) -> None:
    device.tap_text(("EQ Library",))
    device.wait_text(("Manufacturers",), timeout=60)
    device.wait_text(("1Custom",), timeout=300)
    device.screenshot(f"{prefix}-library-ready")


def assert_seeded_state(device: Device, prefix: str) -> None:
    device.tap_text(("My EQs",))
    device.wait_text((FIXTURE_EQ_NAME,), timeout=45)
    device.screenshot(f"{prefix}-my-eqs-preserved")
    device.tap_text(("Settings",))
    device.wait_text(("Output behavior",), timeout=30)
    assert_checked(device, "Manual")
    device.screenshot(f"{prefix}-manual-preserved")
    device.tap_text(("EQ Library",))
    device.wait_text(("Manufacturers",), timeout=60)
    device.wait_text(("1Custom",), timeout=300)
    device.screenshot(f"{prefix}-catalog-ready")


def verify_upgrade(args: argparse.Namespace) -> None:
    device = Device(args.adb, Path(args.evidence_dir))
    device.run("logcat", "-c", check=False)
    if device.shell("pm", "path", PACKAGE, check=False).strip().startswith("package:"):
        raise SmokeError("Upgrade lane was not clean: the baseline package was already installed")
    baseline_install = device.run("install", args.baseline_apk, timeout=120)
    (device.output / "baseline-install.txt").write_text(baseline_install, encoding="utf-8")
    device.assert_version(args.baseline_version_name, args.baseline_version_code)
    device.launch()
    seed_persisted_state(device)
    wait_for_catalog(device, "before-upgrade")
    device.assert_no_fatal_crash()

    update = device.run("install", "-r", args.candidate_apk, timeout=120)
    (device.output / "candidate-update.txt").write_text(update, encoding="utf-8")
    if "Success" not in update:
        raise SmokeError(f"Android did not accept the stable in-place update:\n{update}")
    device.assert_version(args.candidate_version_name, args.candidate_version_code)
    device.launch()
    assert_seeded_state(device, "after-upgrade")
    device.assert_no_fatal_crash()
    (device.output / "result.txt").write_text(
        f"PASS {args.baseline_version_name}/{args.baseline_version_code} -> "
        f"{args.candidate_version_name}/{args.candidate_version_code}; "
        "Personal EQ Room row, Manual DataStore preference, and populated Library checked.\n",
        encoding="utf-8",
    )


def verify_clean_install(args: argparse.Namespace) -> None:
    device = Device(args.adb, Path(args.evidence_dir))
    device.run("logcat", "-c", check=False)
    if device.shell("pm", "path", PACKAGE, check=False).strip().startswith("package:"):
        raise SmokeError("Clean-install lane requires the package to be absent before install")
    install = device.run("install", args.candidate_apk, timeout=120)
    (device.output / "candidate-clean-install.txt").write_text(install, encoding="utf-8")
    if "Success" not in install:
        raise SmokeError(f"Android did not accept the clean installation:\n{install}")
    device.assert_version(args.candidate_version_name, args.candidate_version_code)
    device.launch()
    device.wait_text(("Build your EQ library",), timeout=45)
    device.screenshot("clean-first-run-my-eqs")
    if device.has_text(FIXTURE_EQ_NAME):
        raise SmokeError("A clean stable install unexpectedly contains the upgrade fixture EQ")
    wait_for_catalog(device, "clean-install")
    device.tap_text(("General EQs",))
    device.wait_text(("Sound",), timeout=45)
    device.screenshot("clean-general-eqs")
    device.tap_text(("Settings",))
    device.wait_text(("Output behavior",), timeout=45)
    device.screenshot("clean-settings")
    device.assert_no_fatal_crash()
    (device.output / "result.txt").write_text(
        f"PASS clean install {args.candidate_version_name}/{args.candidate_version_code}; "
        "first-run My EQs, populated Library, General EQs, Settings, and crash buffer checked.\n",
        encoding="utf-8",
    )


def build_parser() -> argparse.ArgumentParser:
    parser = argparse.ArgumentParser(description=__doc__)
    subparsers = parser.add_subparsers(dest="mode", required=True)
    upgrade = subparsers.add_parser("upgrade")
    upgrade.add_argument("--baseline-apk", required=True)
    upgrade.add_argument("--baseline-version-name", required=True)
    upgrade.add_argument("--baseline-version-code", required=True)
    upgrade.add_argument("--candidate-apk", required=True)
    upgrade.add_argument("--candidate-version-name", required=True)
    upgrade.add_argument("--candidate-version-code", required=True)
    upgrade.add_argument("--evidence-dir", required=True)
    clean = subparsers.add_parser("clean-install")
    clean.add_argument("--candidate-apk", required=True)
    clean.add_argument("--candidate-version-name", required=True)
    clean.add_argument("--candidate-version-code", required=True)
    clean.add_argument("--evidence-dir", required=True)
    parser.add_argument("--adb", default=os.environ.get("ADB", "adb"))
    return parser


def main(argv: list[str] | None = None) -> int:
    args = build_parser().parse_args(argv)
    try:
        if args.mode == "upgrade":
            verify_upgrade(args)
        else:
            verify_clean_install(args)
    except (OSError, subprocess.SubprocessError, SmokeError, ET.ParseError) as error:
        print(f"STABLE_RELEASE_SMOKE_FAILED: {error}", file=sys.stderr)
        return 1
    print("STABLE_RELEASE_SMOKE_PASSED")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
