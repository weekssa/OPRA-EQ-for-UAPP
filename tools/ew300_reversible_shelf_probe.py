#!/usr/bin/env python3
"""Bounded reversible native-shelf probe for the exact SIMGOT EW300 DSP.

Only 0x52 reads and 0x57 writes to registers 0x26/0x27 with this task's exact
Low Shelf / High Shelf / captured-baseline payloads are permitted. No 0x53 Save,
0x43, 0x08, 0x54, identity, or arbitrary register access exists in this tool.
"""

from __future__ import annotations

import argparse
import hashlib
import json
import os
import sys
import time
from typing import Iterable

VENDOR_ID = 0x31B2
PRODUCT_ID = 0x0111
MANUFACTURER = "LE XIAN"
PRODUCT = "SIMGOT EW300 DSP"
REPORT_ID = 0x4B
READ_COMMAND = 0x52
WRITE_COMMAND = 0x57
REGISTER_COUNT = 256
READ_ATTEMPTS = 3
RESPONSE_TIMEOUT_MS = 700

BAND_A = 0x26
BAND_B = 0x27
LOW_SHELF_A = bytes.fromhex("C4FFFA00")  # 250 Hz, -6.0 dB
LOW_SHELF_B = bytes.fromhex("C3020300")  # Q 0.707, type 3
HIGH_SHELF_A = bytes.fromhex("C4FFA00F")  # 4,000 Hz, -6.0 dB
HIGH_SHELF_B = bytes.fromhex("C3020400")  # Q 0.707, type 4

ALLOWED_WRITE_REGISTERS = {BAND_A, BAND_B}
ALLOWED_COMMANDS = {READ_COMMAND, WRITE_COMMAND}


class ProbeError(RuntimeError):
    pass


class ProbeSafetyError(ProbeError):
    pass


class IdentityError(ProbeError):
    pass


class ResponseError(ProbeError):
    pass


class RestorationError(ProbeError):
    pass


def build_write_frame(register: int, data: bytes) -> bytes:
    if register not in ALLOWED_WRITE_REGISTERS:
        raise ProbeSafetyError(f"register 0x{register:02X} is not in the reversible shelf allowlist")
    if len(data) != 4:
        raise ProbeSafetyError("every reversible write payload must be exactly four bytes")
    return bytes([REPORT_ID, register, 0, 0, 0, WRITE_COMMAND, 0]) + data


def build_read_frame(register: int) -> bytes:
    if not 0 <= register < REGISTER_COUNT:
        raise ValueError("register out of range")
    return bytes([REPORT_ID, register, 0, 0, 0, READ_COMMAND, 0, 0, 0, 0, 0])


def parse_read(register: int, response: bytes) -> bytes:
    data = bytes(response)
    if len(data) == 10:
        data = bytes([REPORT_ID]) + data
    if len(data) != 11 or data[0] != REPORT_ID or data[1] != register or data[5] != READ_COMMAND:
        raise ResponseError(f"register 0x{register:02X} did not return its exact read echo")
    return data[7:11]


class Guard:
    def __init__(self, allowed_payloads: Iterable[bytes]):
        self.allowed_payloads = {bytes(payload) for payload in allowed_payloads}

    def check(self, frame: bytes) -> None:
        if len(frame) != 11 or frame[0] != REPORT_ID:
            raise ProbeSafetyError("frame must be one 11-byte report 0x4B")
        command = frame[5]
        if command not in ALLOWED_COMMANDS:
            raise ProbeSafetyError(f"command 0x{command:02X} is outside the reversible probe")
        if command == WRITE_COMMAND:
            if frame[1] not in ALLOWED_WRITE_REGISTERS:
                raise ProbeSafetyError("write register is outside 0x26/0x27")
            if bytes(frame[7:11]) not in self.allowed_payloads:
                raise ProbeSafetyError("write payload is not a declared shelf or baseline payload")


class Transport:
    def __init__(self, device, identity: dict, guard: Guard):
        self.device = device
        self.identity = identity
        self.guard = guard
        self.frames: list[dict] = []

    def exchange(self, frame: bytes) -> bytes:
        self.guard.check(frame)
        self.frames.append({"request": frame.hex().upper(), "response": ""})
        written = self.device.write(bytes(frame))
        if written is not None and written < 0:
            raise ProbeError("HID write failed")
        deadline = time.monotonic() + RESPONSE_TIMEOUT_MS / 1000.0
        while time.monotonic() < deadline:
            data = self.device.read(64, max(1, int((deadline - time.monotonic()) * 1000)))
            if data:
                response = bytes(data)
                if len(response) == 10:
                    response = bytes([REPORT_ID]) + response
                self.frames[-1]["response"] = response.hex().upper()
                # A write ACK or unsolicited status can arrive before the READ echo. Keep
                # listening for the exact 0x52 response instead of returning the first packet.
                if len(response) == 11 and response[0] == REPORT_ID and response[5] == READ_COMMAND:
                    return response
        return b""

    def read(self, register: int) -> bytes:
        last: Exception | None = None
        for attempt in range(READ_ATTEMPTS):
            if attempt:
                time.sleep(0.1)
            try:
                return parse_read(register, self.exchange(build_read_frame(register)))
            except ResponseError as error:
                last = error
        raise ResponseError(f"read 0x{register:02X} failed: {last}")

    def write(self, register: int, data: bytes) -> None:
        frame = build_write_frame(register, data)
        self.guard.check(frame)
        self.frames.append({"request": frame.hex().upper(), "response": ""})
        written = self.device.write(bytes(frame))
        if written != len(frame):
            raise ProbeError(f"write 0x{register:02X} was not accepted by the HID transport")
        time.sleep(0.2)
        # Consume and preserve the acknowledged 0x57 response so it cannot satisfy a later read.
        ack = self.device.read(64, 80)
        if ack:
            ack_bytes = bytes(ack)
            if len(ack_bytes) == 10:
                ack_bytes = bytes([REPORT_ID]) + ack_bytes
            self.frames[-1]["response"] = ack_bytes.hex().upper()

    def close(self) -> None:
        try:
            self.device.close()
        except Exception:
            pass


class FakeHid:
    def __init__(self, fail_after_writes: int | None = None):
        self.state = {BAND_A: bytes.fromhex("0000E803"), BAND_B: bytes.fromhex("E8030000")}
        self.baseline = dict(self.state)
        self.frames: list[bytes] = []
        self.fail_after_writes = fail_after_writes

    def write(self, frame: bytes) -> int:
        Guard({LOW_SHELF_A, LOW_SHELF_B, HIGH_SHELF_A, HIGH_SHELF_B,
               self.baseline[BAND_A], self.baseline[BAND_B],
               self.state[BAND_A], self.state[BAND_B]}).check(frame)
        self.frames.append(bytes(frame))
        if self.fail_after_writes is not None and len(self.frames) >= self.fail_after_writes:
            self.fail_after_writes = None
            raise ProbeError("injected write failure")
        if frame[5] == WRITE_COMMAND:
            self.state[frame[1]] = bytes(frame[7:11])
        return len(frame)

    def read(self, size: int, timeout_ms: int) -> bytes:
        request = self.frames[-1]
        return bytes([REPORT_ID, request[1], 0, 0, 0, READ_COMMAND, 0]) + self.state[request[1]]

    def close(self) -> None:
        pass


def find_exact_device(devices: Iterable[dict]) -> dict:
    matches = [
        d for d in devices
        if d.get("vendor_id") == VENDOR_ID and d.get("product_id") == PRODUCT_ID
        and (d.get("manufacturer_string") or "").strip() == MANUFACTURER
        and (d.get("product_string") or "").strip() == PRODUCT
    ]
    if len(matches) != 1:
        raise IdentityError(f"expected exactly one exact EW300 HID interface, found {len(matches)}")
    return matches[0]


def run_reversible(transport: Transport) -> dict:
    baseline_a = transport.read(BAND_A)
    baseline_b = transport.read(BAND_B)
    allowed = {LOW_SHELF_A, LOW_SHELF_B, HIGH_SHELF_A, HIGH_SHELF_B, baseline_a, baseline_b}
    transport.guard.allowed_payloads = allowed
    low_read_a = low_read_b = high_read_a = high_read_b = None
    failure: Exception | None = None
    try:
        transport.write(BAND_A, LOW_SHELF_A)
        transport.write(BAND_B, LOW_SHELF_B)
        low_read_a = transport.read(BAND_A)
        low_read_b = transport.read(BAND_B)
        if low_read_a != LOW_SHELF_A or low_read_b != LOW_SHELF_B:
            raise ResponseError("Low Shelf readback did not match the exact written bytes")
        transport.write(BAND_A, HIGH_SHELF_A)
        transport.write(BAND_B, HIGH_SHELF_B)
        high_read_a = transport.read(BAND_A)
        high_read_b = transport.read(BAND_B)
        if high_read_a != HIGH_SHELF_A or high_read_b != HIGH_SHELF_B:
            raise ResponseError("High Shelf readback did not match the exact written bytes")
    except Exception as error:
        failure = error
    finally:
        try:
            transport.write(BAND_A, baseline_a)
            transport.write(BAND_B, baseline_b)
            restored_a = transport.read(BAND_A)
            restored_b = transport.read(BAND_B)
            if restored_a != baseline_a or restored_b != baseline_b:
                raise ResponseError("baseline restoration did not verify byte-for-byte")
        except Exception as restore_error:
            raise RestorationError(f"baseline restoration failed: {restore_error}") from restore_error
    if failure is not None:
        raise failure
    result = {
        "schema": "ew300-reversible-shelf-v1",
        "identity": transport.identity,
        "baseline": {"0x26": baseline_a.hex().upper(), "0x27": baseline_b.hex().upper()},
        "low_shelf": {"0x26": low_read_a.hex().upper(), "0x27": low_read_b.hex().upper()},
        "high_shelf": {"0x26": high_read_a.hex().upper(), "0x27": high_read_b.hex().upper()},
        "restored": {"0x26": restored_a.hex().upper(), "0x27": restored_b.hex().upper()},
        "frames": transport.frames,
        "save_command_count": 0,
        "notes": [
            "Only 0x52 reads and 0x57 writes to 0x26/0x27 were permitted.",
            "Low Shelf: 250 Hz, -6.0 dB, Q 0.707, type 3.",
            "High Shelf: 4,000 Hz, -6.0 dB, Q 0.707, type 4.",
            "No 0x53 Save, 0x43, 0x08, 0x54, identity, or arbitrary register write was sent.",
        ],
    }
    canonical = json.dumps(result, sort_keys=True, separators=(",", ":")).encode()
    result["sha256"] = hashlib.sha256(canonical).hexdigest()
    result["captured_at"] = time.strftime("%Y-%m-%dT%H:%M:%SZ", time.gmtime())
    return result


def self_test() -> int:
    fake = FakeHid()
    guard = Guard({LOW_SHELF_A, LOW_SHELF_B, HIGH_SHELF_A, HIGH_SHELF_B,
                   fake.state[BAND_A], fake.state[BAND_B]})
    transport = Transport(fake, {"vendor_id": "31b2", "product_id": "0111"}, guard)
    result = run_reversible(transport)
    assert result["restored"] == result["baseline"]
    assert all(frame["request"][10:12] in ("52", "57") for frame in result["frames"])
    assert not any(frame["request"][10:12] == "53" for frame in result["frames"])
    for command in (0x53, 0x43, 0x08, 0x54):
        bad = bytearray(build_read_frame(BAND_A))
        bad[5] = command
        try:
            guard.check(bytes(bad))
        except ProbeSafetyError:
            pass
        else:
            raise AssertionError(f"command 0x{command:02X} was not rejected")
    try:
        build_write_frame(0x66, b"\x00\x00\x00\x00")
    except ProbeSafetyError:
        pass
    else:
        raise AssertionError("register 0x66 write was not rejected")
    print("self-test PASS: baseline -> LS -> HS -> restore, exact bytes, no Save")
    print("sha256", result["sha256"])
    return 0


def live(output: str | None) -> int:
    try:
        import hid  # type: ignore
    except ImportError as error:
        print(error, file=sys.stderr)
        return 2
    try:
        info = find_exact_device(hid.enumerate(VENDOR_ID, PRODUCT_ID))
    except IdentityError as error:
        print(error, file=sys.stderr)
        return 2
    device = hid.device()
    device.open_path(info["path"])
    guard = Guard({LOW_SHELF_A, LOW_SHELF_B, HIGH_SHELF_A, HIGH_SHELF_B})
    transport = Transport(
        device,
        {
            "vendor_id": f"{VENDOR_ID:04x}",
            "product_id": f"{PRODUCT_ID:04x}",
            "manufacturer": MANUFACTURER,
            "product": PRODUCT,
            "interface": 3,
        },
        guard,
    )
    try:
        result = run_reversible(transport)
        if output:
            os.makedirs(output, exist_ok=True)
            path = os.path.join(output, "ew300-reversible-shelf.json")
            with open(path, "w", encoding="utf-8") as handle:
                json.dump(result, handle, indent=2, sort_keys=True)
                handle.write("\n")
            print(path)
            print("file sha256", hashlib.sha256(open(path, "rb").read()).hexdigest())
        else:
            print(json.dumps(result, indent=2, sort_keys=True))
        print("internal sha256", result["sha256"])
        return 0
    except (ProbeError, ResponseError) as error:
        print(f"reversible probe stopped: {error}", file=sys.stderr)
        return 1
    finally:
        transport.close()


def main(argv: list[str]) -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--self-test", action="store_true")
    parser.add_argument("--live", action="store_true")
    parser.add_argument("--output")
    args = parser.parse_args(argv[1:])
    if args.self_test:
        return self_test()
    if args.live:
        return live(args.output)
    parser.print_help()
    return 2


if __name__ == "__main__":
    sys.exit(main(sys.argv))

