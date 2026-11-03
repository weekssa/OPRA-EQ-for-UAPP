#!/usr/bin/env python3
"""Prepare or run the explicitly approved JA11 B1 read-only session.

The default `plan` mode is offline. Hardware mode is reachable only with
--execute and --owner-approved-session, and it sends exactly four read requests:
one volume control and three 0x11 reads with the owner-approved CRC/sequence
matrix. It never sends a filter-set, Apply, Save, retry, or firmware command.
"""

from __future__ import annotations

import argparse
import hashlib
import json
import sys
import time
from dataclasses import dataclass
from pathlib import Path
from typing import Iterable, Sequence


REPORT_ID = 0x02
READ_1 = 0xBB
READ_2 = 0x0B
TERMINATOR = 0xEE
WINDOW_MS = 700


@dataclass(frozen=True)
class Request:
    name: str
    sequence: int
    command: int
    crc_mode: str
    frame: bytes


@dataclass(frozen=True)
class ParsedFrame:
    sequence: int
    command: int
    payload: bytes
    crc: int
    raw_meaningful: bytes


def crc8_maxim(data: bytes) -> int:
    """CRC-8/MAXIM using init 0, reflected polynomial 0x8C, xorout 0."""
    value = 0
    for byte in data:
        value ^= byte
        for _ in range(8):
            value = (value >> 1) ^ 0x8C if value & 1 else value >> 1
    return value


def encode_read(
    *,
    sequence: int,
    command: int,
    payload: bytes = b"",
    crc_mode: str = "computed",
) -> bytes:
    if not 0 <= sequence <= 0xFFFF:
        raise ValueError("sequence must fit in an unsigned 16-bit field")
    if not 0 <= command <= 0xFF:
        raise ValueError("command must fit in one byte")
    if len(payload) > 0xFF:
        raise ValueError("payload must fit in the one-byte length field")
    if crc_mode not in {"computed", "literal-zero"}:
        raise ValueError("unsupported CRC mode")

    prefix = bytes(
        [
            REPORT_ID,
            READ_1,
            READ_2,
            (sequence >> 8) & 0xFF,
            sequence & 0xFF,
            command,
            len(payload),
        ]
    ) + payload
    crc = crc8_maxim(prefix[3:]) if crc_mode == "computed" else 0
    return prefix + bytes([crc, TERMINATOR])


def b1_requests() -> tuple[Request, ...]:
    return (
        Request(
            "volume-control",
            0x0001,
            0x02,
            "computed",
            encode_read(sequence=0x0001, command=0x02),
        ),
        Request(
            "j034-replicate-zero-crc-seq-0000",
            0x0000,
            0x11,
            "literal-zero",
            encode_read(sequence=0x0000, command=0x11, crc_mode="literal-zero"),
        ),
        Request(
            "computed-crc-seq-0000",
            0x0000,
            0x11,
            "computed",
            encode_read(sequence=0x0000, command=0x11),
        ),
        Request(
            "computed-crc-seq-0002",
            0x0002,
            0x11,
            "computed",
            encode_read(sequence=0x0002, command=0x11),
        ),
    )


def hex_bytes(data: bytes) -> str:
    return " ".join(f"{byte:02X}" for byte in data)


def plan_document() -> dict:
    return {
        "schema": 1,
        "session": "JA11 B1 read-only; separate owner approval required",
        "window_ms": WINDOW_MS,
        "send_policy": "one send per request; no retry",
        "filter_writes": False,
        "requests": [
            {
                "order": index,
                "name": request.name,
                "sequence": f"{request.sequence:04X}",
                "command": f"{request.command:02X}",
                "crc_mode": request.crc_mode,
                "frame_hex": hex_bytes(request.frame),
                "length": len(request.frame),
            }
            for index, request in enumerate(b1_requests(), start=1)
        ],
    }


def _meaningful_length(raw: bytes) -> int | None:
    if len(raw) < 9:
        return None
    declared = raw[6]
    total = 9 + declared
    if len(raw) < total:
        return None
    return total


def parse_response(
    raw: bytes,
    *,
    expected_sequence: int,
    expected_command: int,
) -> ParsedFrame:
    """Parse one report-ID-prefixed reply and validate its complete frame."""
    total = _meaningful_length(raw)
    if total is None:
        raise ValueError("incomplete response")
    trailing = raw[total:]
    if any(trailing):
        raise ValueError("non-zero trailing bytes")
    frame = raw[:total]
    if frame[0] != REPORT_ID or frame[1] != READ_1 or frame[2] != READ_2:
        raise ValueError("unexpected response header")
    sequence = (frame[3] << 8) | frame[4]
    command = frame[5]
    length = frame[6]
    if sequence != expected_sequence:
        raise ValueError("response sequence mismatch")
    if command != expected_command:
        raise ValueError("response command mismatch")
    payload = frame[7 : 7 + length]
    carried_crc = frame[7 + length]
    computed_crc = crc8_maxim(frame[3 : 7 + length])
    if carried_crc != computed_crc:
        raise ValueError("response CRC mismatch")
    if frame[8 + length] != TERMINATOR:
        raise ValueError("unexpected response terminator")
    return ParsedFrame(
        sequence=sequence,
        command=command,
        payload=payload,
        crc=carried_crc,
        raw_meaningful=frame,
    )


def classify_outcomes(outcomes: Sequence[str]) -> str:
    """Interpret volume plus request-2/3/4 outcomes under the pre-registered rules."""
    if len(outcomes) != 4 or any(value not in {"R", "S"} for value in outcomes):
        raise ValueError("expected four R/S outcomes")
    volume, request_2, request_3, request_4 = outcomes
    if volume != "R":
        return "INCONCLUSIVE: volume control failed; filter results carry no weight"
    if (request_2, request_3, request_4) == ("S", "S", "S"):
        return "BOUNDED_NEGATIVE: all three exact filter reads were silent"
    return "REOPEN: at least one exact filter read returned a qualifying value"


def _write_json(path: Path, document: object) -> None:
    path.write_text(json.dumps(document, indent=2, sort_keys=True) + "\n", encoding="utf-8")


def _append_event(handle, event: dict) -> None:
    event = {"monotonic_ns": time.monotonic_ns(), **event}
    handle.write(json.dumps(event, sort_keys=True) + "\n")
    handle.flush()


def _open_exact_hid(path_value: str, vendor_id: int, product_id: int):
    try:
        import hid  # type: ignore
    except ImportError as exc:
        raise RuntimeError(
            "Python HIDAPI binding is unavailable; use an existing approved setup"
        ) from exc

    identity = select_exact_identity(
        hid.enumerate(vendor_id, product_id),
        path_value=path_value,
        vendor_id=vendor_id,
        product_id=product_id,
    )
    device = hid.device()
    device.open_path(identity["path"])
    return hid, device, identity, identity_signature(identity)


def select_exact_identity(
    items: Iterable[dict],
    *,
    path_value: str,
    vendor_id: int,
    product_id: int,
) -> dict:
    matches = [
        item
        for item in items
        if bytes(item.get("path", b"")) == path_value.encode()
        and int(item.get("vendor_id", -1)) == vendor_id
        and int(item.get("product_id", -1)) == product_id
    ]
    if len(matches) != 1:
        raise RuntimeError("the exact JA11 HID path was not found uniquely")
    return matches[0]


def identity_signature(identity: dict) -> tuple:
    return (
        int(identity.get("vendor_id", -1)),
        int(identity.get("product_id", -1)),
        bytes(identity.get("path", b"")),
        str(identity.get("serial_number", "")),
        str(identity.get("product_string", "")),
    )


def run_hardware(args: argparse.Namespace) -> int:
    if not args.execute or not args.owner_approved_session.strip():
        raise RuntimeError(
            "hardware mode requires --execute and --owner-approved-session"
        )
    evidence_dir = Path(args.evidence_dir)
    if evidence_dir.exists() and any(evidence_dir.iterdir()):
        raise RuntimeError("evidence directory must be new or empty")
    evidence_dir.mkdir(parents=True, exist_ok=True)

    hid, device, identity, opening_signature = _open_exact_hid(
        args.device_path, args.vendor_id, args.product_id
    )
    safe_identity = {
        "vendor_id": identity.get("vendor_id"),
        "product_id": identity.get("product_id"),
        "product_string": identity.get("product_string"),
        "path_sha256": hashlib.sha256(bytes(identity.get("path", b""))).hexdigest(),
        "serial_sha256": hashlib.sha256(
            str(identity.get("serial_number", "")).encode("utf-8")
        ).hexdigest(),
    }
    requests = b1_requests()
    outcomes: list[str] = []
    result_requests: list[dict] = []
    manifest = {
        "schema": 1,
        "mode": "JA11 B1 read-only",
        "session_approval": args.owner_approved_session,
        "operator": args.operator,
        "host_label": args.host_label,
        "device_provenance": args.device_provenance,
        "identity": safe_identity,
        "plan": plan_document(),
        "outcomes": [],
        "interpretation": "INCONCLUSIVE: session did not complete",
    }
    _write_json(evidence_dir / "manifest.json", manifest)

    with (evidence_dir / "session.jsonl").open("x", encoding="utf-8") as journal:
        _append_event(journal, {"event": "session_start", "manifest": manifest})
        try:
            for index, request in enumerate(requests):
                if outcomes and outcomes[0] != "R":
                    raise RuntimeError("volume control failed; no filter request sent")
                if index > 0:
                    current = select_exact_identity(
                        hid.enumerate(args.vendor_id, args.product_id),
                        path_value=args.device_path,
                        vendor_id=args.vendor_id,
                        product_id=args.product_id,
                    )
                    if identity_signature(current) != opening_signature:
                        _append_event(
                            journal,
                            {"event": "identity_changed", "request": request.name},
                        )
                        raise RuntimeError("device identity changed during the session")
                    late = bytes(device.read(64, timeout_ms=0) or [])
                    if late:
                        _append_event(
                            journal,
                            {"event": "late_or_queued_report", "request": request.name, "raw_hex": hex_bytes(late)},
                        )
                        raise RuntimeError("queued/late report makes attribution ambiguous")

                sent_at = time.monotonic_ns()
                written = device.write(list(request.frame))
                if written != len(request.frame):
                    raise RuntimeError(f"partial HID write for {request.name}: {written}")
                _append_event(
                    journal,
                    {"event": "send", "request": request.name, "frame_hex": hex_bytes(request.frame)},
                )
                deadline = time.monotonic() + WINDOW_MS / 1000
                raw = b""
                while time.monotonic() < deadline:
                    chunk = bytes(device.read(64, timeout_ms=50) or [])
                    if chunk:
                        raw = chunk
                        break
                elapsed_ms = (time.monotonic_ns() - sent_at) / 1_000_000
                if not raw:
                    outcomes.append("S")
                    result_requests.append(
                        {"name": request.name, "outcome": "S", "elapsed_ms": round(elapsed_ms, 3)}
                    )
                    _append_event(journal, {"event": "silence", "request": request.name, "elapsed_ms": elapsed_ms})
                    continue

                try:
                    parsed = parse_response(
                        raw,
                        expected_sequence=request.sequence,
                        expected_command=request.command,
                    )
                    value = parsed.payload[0] if parsed.payload else None
                    if request.command == 0x02 and (value is None or not 0 <= value <= 60):
                        raise ValueError("volume response value is outside 0..60")
                    if request.command == 0x11 and (value is None or not 1 <= value <= 5):
                        raise ValueError("filter response value is outside 1..5")
                except ValueError as exc:
                    _append_event(
                        journal,
                        {"event": "malformed", "request": request.name, "raw_hex": hex_bytes(raw), "error": str(exc)},
                    )
                    raise RuntimeError(f"malformed response for {request.name}: {exc}") from exc

                outcomes.append("R")
                result_requests.append(
                    {
                        "name": request.name,
                        "outcome": "R",
                        "elapsed_ms": round(elapsed_ms, 3),
                        "response_hex": hex_bytes(raw),
                        "value": value,
                    }
                )
                _append_event(
                    journal,
                    {
                        "event": "response",
                        "request": request.name,
                        "elapsed_ms": elapsed_ms,
                        "raw_hex": hex_bytes(raw),
                        "value": value,
                    },
                )

            interpretation = classify_outcomes(outcomes)
            manifest["outcomes"] = outcomes
            manifest["requests"] = result_requests
            manifest["interpretation"] = interpretation
            _write_json(evidence_dir / "manifest.json", manifest)
            _append_event(journal, {"event": "session_complete", "outcomes": outcomes, "interpretation": interpretation})
            print(interpretation)
            return 0
        except Exception as exc:
            manifest["outcomes"] = outcomes
            manifest["requests"] = result_requests
            manifest["interpretation"] = f"INCONCLUSIVE: stopped: {exc}"
            _write_json(evidence_dir / "manifest.json", manifest)
            _append_event(journal, {"event": "session_stopped", "error": str(exc)})
            raise
        finally:
            try:
                device.close()
            except Exception:
                pass


def build_parser() -> argparse.ArgumentParser:
    parser = argparse.ArgumentParser(description=__doc__)
    sub = parser.add_subparsers(dest="mode", required=True)
    sub.add_parser("plan", help="print the fixed offline request plan")

    run = sub.add_parser("run", help="run one separately approved read-only B1 session")
    run.add_argument("--execute", action="store_true")
    run.add_argument(
        "--owner-approved-session",
        required=True,
        metavar="APPROVAL_REFERENCE",
        help="non-empty owner approval reference for this exact session",
    )
    run.add_argument("--device-path", required=True)
    run.add_argument("--vendor-id", type=lambda value: int(value, 0), default=0x2972)
    run.add_argument("--product-id", type=lambda value: int(value, 0), default=0x0102)
    run.add_argument("--evidence-dir", required=True)
    run.add_argument("--operator", required=True)
    run.add_argument("--host-label", required=True)
    run.add_argument("--device-provenance", required=True)
    return parser


def main(argv: Iterable[str] | None = None) -> int:
    args = build_parser().parse_args(argv)
    if args.mode == "plan":
        print(json.dumps(plan_document(), indent=2, sort_keys=True))
        return 0
    if args.mode == "run":
        return run_hardware(args)
    raise AssertionError(args.mode)


if __name__ == "__main__":
    try:
        raise SystemExit(main())
    except (RuntimeError, ValueError) as exc:
        print(f"error: {exc}", file=sys.stderr)
        raise SystemExit(1)
