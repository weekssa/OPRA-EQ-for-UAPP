#!/usr/bin/env python3
"""Bounded read-only probe for the exact SIMGOT EW300 DSP.

Only command 0x52 (register read) can be emitted. The safety guard rejects every other
command before a frame reaches the transport. Writes, Save, handshake, firmware and
bootloader commands are outside this tool by construction.
"""

from __future__ import annotations

import argparse
import hashlib
import json
import os
import sys
import time
from dataclasses import dataclass
from typing import Iterable

VENDOR_ID = 0x31B2
PRODUCT_ID = 0x0111
MANUFACTURER = "LE XIAN"
PRODUCT = "SIMGOT EW300 DSP"
REPORT_ID = 0x4B
REPORT_SIZE = 11
READ_COMMAND = 0x52
REGISTER_COUNT = 256
READ_ATTEMPTS = 3
READ_GAP_SECONDS = 0.1
RESPONSE_TIMEOUT_MS = 700
FORBIDDEN_COMMANDS = {
    0x57: "register write",
    0x53: "persist/save",
    0x43: "family handshake/clear",
    0x08: "extended memory read",
    0x54: "vendor report 0x54",
}


class ProbeError(RuntimeError):
    pass


class ProbeSafetyError(ProbeError):
    pass


class IdentityError(ProbeError):
    pass


class ResponseError(ProbeError):
    pass


def build_read_frame(register: int) -> bytes:
    if not 0 <= register < REGISTER_COUNT:
        raise ValueError("register must be 0x00..0xFF")
    return bytes(
        [
            REPORT_ID,
            register & 0xFF,
            0,
            0,
            0,
            READ_COMMAND,
            0,
            0,
            0,
            0,
            0,
        ]
    )


def normalize_response(response: bytes) -> bytes:
    """HIDAPI may include the report ID or return only the 10-byte payload."""
    data = bytes(response)
    if len(data) == 10:
        return bytes([REPORT_ID]) + data
    return data


def parse_read_response(register: int, response: bytes) -> bytes:
    frame = normalize_response(response)
    if len(frame) != REPORT_SIZE:
        raise ResponseError(f"expected {REPORT_SIZE} response bytes, got {len(frame)}")
    if frame[0] != REPORT_ID:
        raise ResponseError("response report ID did not match 0x4B")
    if frame[1] != register:
        raise ResponseError("response did not echo the requested register")
    if frame[5] != READ_COMMAND:
        raise ResponseError("response did not echo read command 0x52")
    return frame[7:11]


def require_read_command(frame: bytes) -> None:
    if len(frame) != REPORT_SIZE:
        raise ProbeSafetyError("every emitted frame must be exactly 11 bytes")
    if frame[0] != REPORT_ID:
        raise ProbeSafetyError("every emitted frame must use report ID 0x4B")
    command = frame[5]
    if command != READ_COMMAND:
        label = FORBIDDEN_COMMANDS.get(command, "non-read command")
        raise ProbeSafetyError(f"refusing to emit command 0x{command:02X} ({label})")


@dataclass(frozen=True)
class HidIdentity:
    vendor_id: int
    product_id: int
    manufacturer: str
    product: str
    path: bytes
    serial_present: bool

    def shareable(self) -> dict:
        return {
            "vendor_id": f"{self.vendor_id:04x}",
            "product_id": f"{self.product_id:04x}",
            "manufacturer": self.manufacturer,
            "product": self.product,
            "interface": 3,
            "serial_present": self.serial_present,
        }


class HidTransport:
    """Minimal HIDAPI transport. The safety guard runs before every write."""

    def __init__(self, device, identity: HidIdentity):
        self._device = device
        self.identity = identity
        self.emitted_frames: list[bytes] = []
        self.received_frames: list[bytes] = []

    def exchange(self, frame: bytes) -> bytes:
        require_read_command(frame)
        self.emitted_frames.append(bytes(frame))
        written = self._device.write(bytes(frame))
        if written is not None and written < 0:
            raise ProbeError("HID write failed")
        deadline = time.monotonic() + RESPONSE_TIMEOUT_MS / 1000.0
        while time.monotonic() < deadline:
            data = self._device.read(64, int((deadline - time.monotonic()) * 1000) or 1)
            if data:
                response = normalize_response(bytes(data))
                self.received_frames.append(response)
                return response
        return b""

    def close(self) -> None:
        try:
            self._device.close()
        except Exception:
            pass


class ReadOnlyProbe:
    def __init__(self, transport: HidTransport):
        self.transport = transport

    def read_register(self, register: int) -> bytes:
        last_error: Exception | None = None
        for attempt in range(READ_ATTEMPTS):
            if attempt:
                time.sleep(READ_GAP_SECONDS)
            response = self.transport.exchange(build_read_frame(register))
            try:
                return parse_read_response(register, response)
            except ResponseError as error:
                last_error = error
        raise ResponseError(f"register 0x{register:02X} did not return a valid read echo: {last_error}")

    def capture(self) -> dict:
        registers: dict[str, dict] = {}
        for register in range(REGISTER_COUNT):
            value = self.read_register(register)
            registers[f"0x{register:02X}"] = {
                "raw": value.hex().upper(),
                "value_le32": int.from_bytes(value, "little"),
            }
        payload = {
            "schema": "ew300-readonly-probe-v1",
            "identity": self.transport.identity.shareable(),
            "command_allowlist": ["0x52"],
            "registers": registers,
            "frames": [
                {"request": request.hex().upper(), "response": response.hex().upper()}
                for request, response in zip(self.transport.emitted_frames, self.transport.received_frames)
            ],
        }
        canonical = json.dumps(payload, sort_keys=True, separators=(",", ":")).encode("utf-8")
        payload["sha256"] = hashlib.sha256(canonical).hexdigest()
        payload["captured_at"] = time.strftime("%Y-%m-%dT%H:%M:%SZ", time.gmtime())
        payload["notes"] = [
            "Read-only capture using command 0x52 only.",
            "No 0x57 write, 0x53 Save, 0x43 handshake, 0x08 extended read, or bootloader command was sent.",
            "Serial value is intentionally omitted from this shareable record.",
        ]
        return payload


def find_exact_device(devices: Iterable[dict]) -> dict:
    matches = [
        device
        for device in devices
        if device.get("vendor_id") == VENDOR_ID
        and device.get("product_id") == PRODUCT_ID
        and (device.get("manufacturer_string") or "").strip() == MANUFACTURER
        and (device.get("product_string") or "").strip() == PRODUCT
    ]
    if not matches:
        raise IdentityError("no HID device matched 31B2:0111 with the exact EW300 strings")
    if len(matches) > 1:
        raise IdentityError("more than one HID interface matched the exact EW300 identity")
    return matches[0]


def open_live_probe() -> ReadOnlyProbe:
    try:
        import hid  # type: ignore
    except ImportError as error:  # pragma: no cover - environment dependent
        raise ProbeError("Python hidapi module is not installed; use --self-test or install hidapi") from error
    device_info = find_exact_device(hid.enumerate(VENDOR_ID, PRODUCT_ID))
    device = hid.device()
    device.open_path(device_info["path"])
    identity = HidIdentity(
        vendor_id=device_info["vendor_id"],
        product_id=device_info["product_id"],
        manufacturer=device_info.get("manufacturer_string") or "",
        product=device_info.get("product_string") or "",
        path=device_info["path"],
        serial_present=bool(device_info.get("serial_number")),
    )
    return ReadOnlyProbe(HidTransport(device, identity))


class FakeHid:
    """Deterministic fake used by --self-test. It records every emitted frame."""

    def __init__(self, fail_register: int | None = None):
        self.fail_register = fail_register
        self.frames: list[bytes] = []
        self.closed = False
        self._state = {
            register: bytes([0xF5, 0xFF, 0x64, 0x00]) if register == 0x26 else bytes([0, 0, 0, 0])
            for register in range(REGISTER_COUNT)
        }

    def write(self, frame: bytes) -> int:
        self.frames.append(bytes(frame))
        require_read_command(frame)
        register = frame[1]
        value = b"" if register == self.fail_register else self._state[register]
        self._pending = bytes([REPORT_ID, register, 0, 0, 0, READ_COMMAND, 0]) + value
        return len(frame)

    def read(self, size: int, timeout_ms: int) -> bytes:
        pending = getattr(self, "_pending", b"")
        self._pending = b""
        return pending

    def close(self) -> None:
        self.closed = True


def self_test() -> int:
    fake = FakeHid()
    identity = HidIdentity(VENDOR_ID, PRODUCT_ID, MANUFACTURER, PRODUCT, b"fake", False)
    probe = ReadOnlyProbe(HidTransport(fake, identity))

    assert build_read_frame(0x26) == bytes.fromhex("4B26000000520000000000")
    assert parse_read_response(0x26, bytes.fromhex("4B260000005200F5FF6400")) == bytes.fromhex("F5FF6400")

    capture = probe.capture()
    assert len(capture["registers"]) == REGISTER_COUNT
    assert capture["registers"]["0x26"]["raw"] == "F5FF6400"
    assert all(frame[5] == READ_COMMAND for frame in fake.frames)
    assert all(frame[:1] == bytes([REPORT_ID]) for frame in fake.frames)
    assert len(capture["sha256"]) == 64

    for command in FORBIDDEN_COMMANDS:
        forbidden = bytearray(build_read_frame(0x26))
        forbidden[5] = command
        try:
            require_read_command(bytes(forbidden))
        except ProbeSafetyError:
            pass
        else:
            raise AssertionError(f"command 0x{command:02X} was not rejected")

    bad = FakeHid(fail_register=0x40)
    bad_probe = ReadOnlyProbe(HidTransport(bad, identity))
    try:
        bad_probe.capture()
    except ResponseError:
        pass
    else:
        raise AssertionError("missing register did not stop the capture")

    try:
        find_exact_device(
            [
                {
                    "vendor_id": VENDOR_ID,
                    "product_id": PRODUCT_ID,
                    "manufacturer_string": "OTHER",
                    "product_string": PRODUCT,
                    "path": b"x",
                }
            ]
        )
    except IdentityError:
        pass
    else:
        raise AssertionError("non-exact identity was accepted")

    print("self-test PASS: only 0x52 frames can be emitted; missing register and foreign identity fail closed")
    print(f"fake capture sha256 {capture['sha256']}")
    return 0


def main(argv: list[str]) -> int:
    parser = argparse.ArgumentParser(description="Read-only SIMGOT EW300 register probe")
    parser.add_argument("--self-test", action="store_true")
    parser.add_argument("--list", action="store_true")
    parser.add_argument("--capture", action="store_true")
    parser.add_argument("--register", type=lambda value: int(value, 0))
    parser.add_argument("--output")
    args = parser.parse_args(argv[1:])

    if args.self_test:
        return self_test()

    if args.list:
        try:
            import hid  # type: ignore
        except ImportError as error:
            print(error, file=sys.stderr)
            return 2
        devices = [
            device
            for device in hid.enumerate(VENDOR_ID, PRODUCT_ID)
            if device.get("manufacturer_string") == MANUFACTURER
            and device.get("product_string") == PRODUCT
        ]
        for device in devices:
            print(f"{device['vendor_id']:04x}:{device['product_id']:04x} {device.get('product_string')}")
        return 0

    if not args.capture and args.register is None:
        parser.print_help()
        return 2

    try:
        probe = open_live_probe()
    except (ProbeError, IdentityError) as error:
        print(f"probe refused: {error}", file=sys.stderr)
        return 2

    try:
        if args.register is not None:
            value = probe.read_register(args.register)
            print(f"0x{args.register:02X} {value.hex().upper()}")
            return 0

        capture = probe.capture()
        if not args.output:
            print(json.dumps(capture, indent=2, sort_keys=True))
            return 0
        os.makedirs(args.output, exist_ok=True)
        path = os.path.join(args.output, "ew300-readonly-capture.json")
        with open(path, "w", encoding="utf-8") as handle:
            json.dump(capture, handle, indent=2, sort_keys=True)
            handle.write("\n")
        digest = hashlib.sha256(open(path, "rb").read()).hexdigest()
        print(path)
        print(f"sha256 {digest}")
        return 0
    except (ProbeError, ResponseError) as error:
        print(f"capture stopped: {error}", file=sys.stderr)
        return 1
    finally:
        probe.transport.close()


if __name__ == "__main__":
    sys.exit(main(sys.argv))

