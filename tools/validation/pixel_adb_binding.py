#!/usr/bin/env python3
"""Resolve and bind the owner's Pixel 9 over current wireless ADB discovery.

This utility never pairs, installs, launches, changes settings, or addresses a
USB ADB transport. It connects only to current ADB TLS-connect mDNS endpoints
when no already-online candidate uniquely identifies the Pixel. Candidate
identity is confirmed from read-only Android properties before package access.
"""

from __future__ import annotations

import argparse
import hashlib
import ipaddress
import json
import re
import subprocess
import sys
import uuid
import zlib
from dataclasses import asdict, dataclass
from datetime import datetime, timezone
from pathlib import Path
from typing import Iterable

from app_activity_checkpoint import ActivityCheckpointError, extract_activity


EXPECTED_SERIAL_SHA256 = "c921897d26153147627f65dd81c921fdddcd94c9757ac12f5b1b33ab4873fef6"
EXPECTED_MODEL = "Pixel 9"
EXPECTED_PRODUCT = "tokay"
PACKAGE = "com.weekssa.opraeqforuapp"
EXPECTED_APK_SHA256 = "f33818309d55571166d91e501065706ddbb8faf180833b03bb6eb948e6bffed1"
TLS_CONNECT = "_adb-tls-connect._tcp"
TLS_PAIRING = "_adb-tls-pairing._tcp"


def sha256_text(value: str) -> str:
    return hashlib.sha256(value.encode("utf-8")).hexdigest()


def target_reference(value: str) -> str:
    return f"target-sha256={sha256_text(value)}"


class ResolutionError(RuntimeError):
    pass


@dataclass(frozen=True)
class Service:
    instance: str
    service_type: str
    endpoint: str

    @property
    def adb_transport_alias(self) -> str:
        return f"{self.instance}.{self.service_type}"


@dataclass(frozen=True)
class DeviceRow:
    serial: str
    state: str
    details: str


@dataclass(frozen=True)
class Identity:
    target: str
    source: str
    endpoint: str | None
    service_instance: str | None
    serial_sha256: str
    model: str
    product: str
    sdk: str
    fingerprint: str
    route_boot_id: str


def valid_endpoint(value: str) -> bool:
    bracketed = re.fullmatch(r"\[([^]]+)\]:([0-9]+)", value)
    ipv4 = re.fullmatch(r"([^:]+):([0-9]+)", value)
    if bracketed:
        try:
            ipaddress.IPv6Address(bracketed.group(1))
            port = int(bracketed.group(2))
        except (ipaddress.AddressValueError, ValueError):
            return False
    elif ipv4:
        try:
            ipaddress.IPv4Address(ipv4.group(1))
            port = int(ipv4.group(2))
        except (ipaddress.AddressValueError, ValueError):
            return False
    else:
        return False
    return 1 <= port <= 65535


def valid_boot_id(value: str) -> bool:
    """Linux boot_id is a UUID-shaped per-boot identifier, not arbitrary hex."""
    if not re.fullmatch(r"[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}", value):
        return False
    try:
        uuid.UUID(value)
        return True
    except ValueError:
        return False


def parse_mdns_services(text: str) -> tuple[list[Service], int, int]:
    """Return valid TLS-connect services plus counts of pair/other services."""
    services: list[Service] = []
    pairing_count = 0
    other_count = 0
    for raw in text.splitlines():
        fields = raw.split()
        if not fields:
            continue
        if fields[0].lower().startswith("list"):
            continue
        if len(fields) >= 2 and fields[1] == TLS_PAIRING:
            pairing_count += 1
            continue
        if len(fields) < 3:
            if any(TLS_CONNECT in field or TLS_PAIRING in field for field in fields):
                if TLS_PAIRING in fields:
                    pairing_count += 1
                    continue
                raise ResolutionError("malformed TLS-connect mDNS service row")
            other_count += 1
            continue
        instance, service_type, endpoint = fields[:3]
        if service_type != TLS_CONNECT:
            # The official `adb mdns services` output is instance, service
            # type, endpoint. Also tolerate a fully qualified instance in the
            # first column, but normalize it before building ADB row aliases.
            if TLS_CONNECT in service_type or TLS_PAIRING in service_type:
                raise ResolutionError("unexpected mDNS service type column")
            other_count += 1
            continue
        if len(fields) != 3:
            raise ResolutionError("malformed TLS-connect mDNS row")
        full_suffix = f".{TLS_CONNECT}"
        if instance.endswith(full_suffix):
            instance = instance[: -len(full_suffix)]
        if not instance or any(ch.isspace() for ch in instance):
            raise ResolutionError("unexpected TLS-connect service instance")
        if not valid_endpoint(endpoint):
            raise ResolutionError("invalid current IP/port endpoint")
        services.append(Service(instance, TLS_CONNECT, endpoint))
    # Exact duplicate rows carry no additional target information. Collapse
    # them while retaining distinct services, including services that share an
    # endpoint; endpoint identity is verified from Android properties below.
    return list(dict.fromkeys(services)), pairing_count, other_count


def parse_devices(text: str) -> list[DeviceRow]:
    rows: list[DeviceRow] = []
    for raw in text.splitlines():
        fields = raw.split()
        if len(fields) < 2 or fields[0].lower() == "list" or fields[0] == "*":
            continue
        if fields[1] == "no" and len(fields) >= 3 and fields[2] == "permissions":
            state = "no permissions"
            details = " ".join(fields[3:])
        else:
            state = fields[1]
            details = " ".join(fields[2:])
        if state not in {"device", "offline", "unauthorized", "no permissions", "recovery", "sideload", "bootloader"}:
            continue
        rows.append(DeviceRow(fields[0], state, details))
    return rows


def service_serial_token(value: str) -> str | None:
    """Return a serial token only from an ADB TLS-connect service alias."""
    suffix = f".{TLS_CONNECT}"
    if not value.endswith(suffix):
        return None
    instance = value[: -len(suffix)]
    if not instance.startswith("adb-") or any(ch.isspace() for ch in instance):
        return None
    remainder = instance[4:]
    token, separator, route = remainder.partition("-")
    if not separator or not token or not route:
        return None
    return token


def is_tls_connect_service_alias(value: object) -> bool:
    suffix = f".{TLS_CONNECT}"
    return isinstance(value, str) and value.endswith(suffix) and not any(ch.isspace() for ch in value)


def is_pixel_service_alias(value: str) -> bool:
    token = service_serial_token(value)
    return token is not None and hashlib.sha256(token.encode("utf-8")).hexdigest() == EXPECTED_SERIAL_SHA256


def normalize_model(value: str) -> str:
    """Normalize the spaces ADB replaces with underscores in model labels."""
    return " ".join(value.replace("_", " ").split())


def endpoint_map(services: Iterable[Service]) -> dict[str, list[Service]]:
    result: dict[str, list[Service]] = {}
    for service in services:
        result.setdefault(service.endpoint, []).append(service)
    return result


def run_command(argv: list[str]) -> subprocess.CompletedProcess[bytes]:
    try:
        return subprocess.run(argv, stdout=subprocess.PIPE, stderr=subprocess.PIPE, check=False, timeout=60)
    except subprocess.TimeoutExpired as exc:
        raise ResolutionError("ADB command did not complete within the per-command 60-second transport bound") from exc


def text_of(result: subprocess.CompletedProcess[bytes]) -> str:
    return result.stdout.decode("utf-8", errors="replace").replace("\r", "").strip()


def valid_png_stream(data: bytes) -> bool:
    """Validate one complete, CRC-correct, non-interlaced PNG image stream."""
    signature = b"\x89PNG\r\n\x1a\n"
    if not data.startswith(signature):
        return False
    offset = len(signature)
    width = height = bit_depth = color_type = interlace = None
    seen_ihdr = seen_plte = seen_idat = seen_iend = False
    idat_ended = False
    idat_payload = bytearray()
    max_raw_bytes = 100_000_000

    while offset < len(data):
        if len(data) - offset < 12:
            return False
        length = int.from_bytes(data[offset : offset + 4], "big")
        chunk_type = data[offset + 4 : offset + 8]
        end = offset + 12 + length
        if end > len(data) or len(chunk_type) != 4 or not all(
            65 <= value <= 90 or 97 <= value <= 122 for value in chunk_type
        ):
            return False
        payload = data[offset + 8 : offset + 8 + length]
        expected_crc = int.from_bytes(data[offset + 8 + length : end], "big")
        if zlib.crc32(chunk_type + payload) & 0xFFFFFFFF != expected_crc:
            return False

        if not seen_ihdr:
            if chunk_type != b"IHDR" or length != 13:
                return False
            width = int.from_bytes(payload[0:4], "big")
            height = int.from_bytes(payload[4:8], "big")
            bit_depth, color_type, compression, filter_method, interlace = payload[8:13]
            if (
                width < 1
                or height < 1
                or width > 0x7FFFFFFF
                or height > 0x7FFFFFFF
                or compression != 0
                or filter_method != 0
                or interlace != 0
            ):
                return False
            allowed_depths = {
                0: {1, 2, 4, 8, 16},
                2: {8, 16},
                3: {1, 2, 4, 8},
                4: {8, 16},
                6: {8, 16},
            }
            if bit_depth not in allowed_depths.get(color_type, set()):
                return False
            seen_ihdr = True
        elif chunk_type == b"IHDR":
            return False
        elif chunk_type == b"PLTE":
            if seen_plte or seen_idat or not length or length % 3 or length > 768 or color_type in {0, 4}:
                return False
            palette_entries = length // 3
            if color_type == 3 and palette_entries > (1 << bit_depth):
                return False
            seen_plte = True
        elif chunk_type == b"IDAT":
            if idat_ended:
                return False
            seen_idat = True
            idat_payload.extend(payload)
        elif chunk_type == b"IEND":
            if length != 0 or not seen_idat or (color_type == 3 and not seen_plte):
                return False
            seen_iend = True
            offset = end
            if offset != len(data):
                return False
            break
        else:
            if seen_idat:
                idat_ended = True
            # Unknown critical chunks cannot be safely interpreted.
            if 65 <= chunk_type[0] <= 90:
                return False

        if seen_idat and chunk_type != b"IDAT":
            idat_ended = True
        offset = end

    if not (seen_ihdr and seen_idat and seen_iend) or width is None or height is None:
        return False
    channels = {0: 1, 2: 3, 3: 1, 4: 2, 6: 4}[color_type]
    row_bytes = (width * channels * bit_depth + 7) // 8
    expected_raw_bytes = height * (row_bytes + 1)
    if expected_raw_bytes > max_raw_bytes:
        return False
    try:
        decoder = zlib.decompressobj()
        decoded = decoder.decompress(bytes(idat_payload), expected_raw_bytes + 1)
    except zlib.error:
        return False
    if (
        len(decoded) != expected_raw_bytes
        or not decoder.eof
        or decoder.unused_data
        or decoder.unconsumed_tail
        or decoder.flush()
    ):
        return False
    return all(decoded[row * (row_bytes + 1)] <= 4 for row in range(height))


def require_success(label: str, result: subprocess.CompletedProcess[bytes]) -> str:
    output = text_of(result)
    if result.returncode != 0:
        err = result.stderr.decode("utf-8", errors="replace").replace("\r", "").strip()
        raise ResolutionError(
            f"{label} failed with exit {result.returncode}; stdout_present={bool(output)}; stderr_present={bool(err)}"
        )
    return output


def adb_call(adb: str, *args: str) -> subprocess.CompletedProcess[bytes]:
    return run_command([adb, *args])


def identity_for(adb: str, target: str, source: str, endpoint: str | None, instance: str | None) -> Identity:
    target_ref = target_reference(target)
    state = require_success(f"get-state {target_ref}", adb_call(adb, "-s", target, "get-state"))
    if state != "device":
        raise ResolutionError(f"candidate {target_ref} is not online (get-state={state!r})")

    def prop(key: str) -> str:
        value = require_success(f"{key} {target_ref}", adb_call(adb, "-s", target, "shell", "getprop", key))
        if not value or "\n" in value:
            raise ResolutionError(f"{key} was empty or non-scalar for {target_ref}")
        return value

    boot_result = adb_call(adb, "-s", target, "shell", "cat", "/proc/sys/kernel/random/boot_id")
    boot_id = text_of(boot_result) if boot_result.returncode == 0 else ""
    if boot_id and not valid_boot_id(boot_id):
        boot_id = ""
    return Identity(
        target=target,
        source=source,
        endpoint=endpoint,
        service_instance=instance,
        serial_sha256=hashlib.sha256(prop("ro.serialno").encode("utf-8")).hexdigest(),
        model=prop("ro.product.model"),
        product=prop("ro.product.device"),
        sdk=prop("ro.build.version.sdk"),
        fingerprint=prop("ro.build.fingerprint"),
        route_boot_id=boot_id,
    )


def canonical_target(identities: list[Identity]) -> Identity | None:
    matches = [i for i in identities if i.serial_sha256 == EXPECTED_SERIAL_SHA256]
    for identity in matches:
        if normalize_model(identity.model) != EXPECTED_MODEL or identity.product != EXPECTED_PRODUCT:
            raise ResolutionError(
                f"stable serial matched but model/product did not: {identity.model!r}/{identity.product!r}"
            )
        if not identity.sdk.isdigit() or int(identity.sdk) < 35:
            raise ResolutionError(f"unexpected Android SDK for Pixel 9: {identity.sdk!r}")
        if not identity.fingerprint.startswith("google/tokay/tokay:"):
            raise ResolutionError("stable serial matched but the Android fingerprint is outside the recorded Pixel 9 profile")
        if identity.route_boot_id and not valid_boot_id(identity.route_boot_id):
            raise ResolutionError("a current Pixel route returned a malformed Android boot ID")
    if not matches:
        return None

    # Multiple current routes can still identify one Pixel. They are safe to
    # collapse only when every stable Android identity field agrees exactly.
    stable_profiles = {(i.serial_sha256, i.model, i.product, i.sdk, i.fingerprint) for i in matches}
    if len(stable_profiles) > 1:
        raise ResolutionError(
            "current wireless routes with the expected serial disagree on stable Android identity"
        )
    distinct_endpoints = {i.endpoint for i in matches if i.endpoint}
    if len(distinct_endpoints) > 1:
        boot_ids = {i.route_boot_id for i in matches}
        if "" in boot_ids or len(boot_ids) != 1:
            raise ResolutionError(
                "multiple matching Android routes cannot be proven to belong to one current boot session"
            )

    # Prefer the current service-instance alias. It carries the identity prefix
    # and avoids relying on an ephemeral host:port when both aliases exist.
    matches.sort(key=lambda i: (not is_pixel_service_alias(i.target), i.target))
    return matches[0]


def append_log(path: Path, line: str) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    with path.open("a", encoding="utf-8") as stream:
        stream.write(line.rstrip("\n") + "\n")


def disconnect_new_routes(adb: str, endpoints: Iterable[str], log: Path) -> list[str]:
    failures: list[str] = []
    for endpoint in sorted(set(endpoints)):
        result = adb_call(adb, "disconnect", endpoint)
        output = text_of(result)
        stderr = result.stderr.decode("utf-8", errors="replace").replace("\r", "").strip()
        ok = result.returncode == 0 and output.startswith("disconnected ")
        append_log(
            log,
            f"{datetime.now(timezone.utc).isoformat(timespec='seconds')} disconnect-new-adb-route endpoint_sha256={sha256_text(endpoint)} exit={result.returncode} success={ok} stderr_present={bool(stderr)}",
        )
        if not ok:
            failures.append(endpoint)
    return failures


def invoke_logged(adb: str, args: list[str], label: str, log: Path, *, redact_output: bool = False) -> str:
    started = datetime.now(timezone.utc).isoformat(timespec="seconds")
    result = adb_call(adb, *args)
    output = text_of(result)
    stderr = result.stderr.decode("utf-8", errors="replace").replace("\r", "").strip()
    target = args[args.index("-s") + 1] if "-s" in args and args.index("-s") + 1 < len(args) else None
    target_log = f" {target_reference(target)}" if target else ""
    append_log(
        log,
        f"{started} {label}{target_log} exit={result.returncode} stdout_bytes={len(result.stdout)} stderr_present={bool(stderr)}",
    )
    if result.returncode != 0:
        raise ResolutionError(
            f"{label} failed with exit {result.returncode}; stdout_present={bool(output)}; stderr_present={bool(stderr)}"
        )
    return output


def record_service_snapshot(adb: str, log: Path) -> list[Service]:
    result = adb_call(adb, "mdns", "services")
    output = require_success("mdns services", result)
    services, pairing_count, other_count = parse_mdns_services(output)
    pixel_services = [service for service in services if is_pixel_service_alias(service.adb_transport_alias)]
    other_connect_count = len(services) - len(pixel_services)
    append_log(log, f"{datetime.now(timezone.utc).isoformat(timespec='seconds')} mdns-summary connect_count={len(services)} pairing_ignored={pairing_count} other_ignored={other_count} nonpixel_connect_count={other_connect_count}")
    if pixel_services:
        append_log(log, f"current-connect-service pixel_alias_count={len(pixel_services)} route_metadata=omitted")
    return services


def devices_snapshot(adb: str, label: str, services: list[Service], log: Path) -> list[DeviceRow]:
    result = adb_call(adb, "devices", "-l")
    output = require_success(label, result)
    rows = parse_devices(output)
    pixel_aliases = [row for row in rows if is_pixel_service_alias(row.serial)]
    current_endpoints = {service.endpoint for service in services}
    pixel_endpoint_rows = [row for row in rows if row.serial in current_endpoints and any(s.endpoint == row.serial and is_pixel_service_alias(s.adb_transport_alias) for s in services)]
    usb_pixel_present = any(hashlib.sha256(row.serial.encode("utf-8")).hexdigest() == EXPECTED_SERIAL_SHA256 for row in rows)
    append_log(log, f"{datetime.now(timezone.utc).isoformat(timespec='seconds')} {label} summary row_count={len(rows)} online_count={sum(row.state == 'device' for row in rows)} pixel_service_aliases={len(pixel_aliases)} pixel_endpoint_rows={len(pixel_endpoint_rows)} expected_serial_usb_row={'present' if usb_pixel_present else 'absent'} emulator_rows={sum(row.serial.startswith('emulator-') for row in rows)}")
    for row in pixel_aliases:
        alias_hash = hashlib.sha256(row.serial.encode("utf-8")).hexdigest()
        append_log(log, f"pixel-service-alias state={row.state} alias_sha256={alias_hash}")
    for row in pixel_endpoint_rows:
        target_hash = hashlib.sha256(row.serial.encode("utf-8")).hexdigest()
        append_log(log, f"pixel-current-endpoint state={row.state} target_sha256={target_hash}")
    return rows


def collect_identity_candidates(adb: str, rows: list[DeviceRow], services: list[Service], log: Path) -> tuple[list[Identity], list[DeviceRow]]:
    by_endpoint = endpoint_map(services)
    aliases: dict[str, list[Service]] = {}
    for service in services:
        aliases.setdefault(service.adb_transport_alias, []).append(service)
    identities: list[Identity] = []
    target_states: list[DeviceRow] = []
    seen: set[tuple[str, str | None]] = set()
    for row in rows:
        if hashlib.sha256(row.serial.encode("utf-8")).hexdigest() == EXPECTED_SERIAL_SHA256:
            raise ResolutionError("the Pixel serial is listed as a non-Wi-Fi ADB serial; USB ADB is not allowed")
        endpoint = row.serial if valid_endpoint(row.serial) else None
        mapped = by_endpoint.get(endpoint, []) if endpoint else []
        if endpoint:
            if not mapped:
                # A host:port row without a current TLS-connect service mapping
                # could be a remembered endpoint; never query or bind it.
                continue
            # A currently advertised TLS-connect endpoint is a valid read-only
            # identity candidate even when its instance name is ambiguous. If
            # several current services share the endpoint, query that transport
            # once and use the returned Android identity as authority.
            service = sorted(mapped, key=lambda item: (not is_pixel_service_alias(item.adb_transport_alias), item.instance))[0]
            source = "current-mdns-endpoint-map"
            instance = service.adb_transport_alias
        elif is_tls_connect_service_alias(row.serial):
            # An already-online TLS-connect alias is itself a
            # current ADB transport, not a remembered host:port endpoint. Its
            # service name is only a candidate; stable Android identity below
            # remains authoritative even when mDNS currently has no row.
            matching_services = [service for service in services if service.adb_transport_alias == row.serial]
            distinct_endpoints = {service.endpoint for service in matching_services}
            if not matching_services or len(distinct_endpoints) > 1:
                # Query the exact already-online alias and retain no guessed
                # endpoint when service discovery duplicates its name.
                endpoint = None
                source = "current-online-tls-connect-alias-unmapped-or-ambiguous-endpoint"
            else:
                source = "current-online-tls-connect-alias"
            instance = row.serial
            if len(distinct_endpoints) == 1:
                endpoint = next(iter(distinct_endpoints))
        elif row.serial in aliases:
            # An online current connect-service alias can be safely queried
            # even when the alias itself does not expose a stable serial. A
            # duplicated alias that resolves to multiple endpoints must not
            # select an arbitrary endpoint from service-discovery order; query
            # the already-online alias directly and leave endpoint unbound.
            matching_services = aliases[row.serial]
            distinct_endpoints = {service.endpoint for service in matching_services}
            if len(distinct_endpoints) == 1:
                endpoint = next(iter(distinct_endpoints))
                source = "current-mdns-service-alias"
            else:
                endpoint = None
                source = "current-mdns-service-alias-ambiguous-endpoint"
            instance = row.serial
        else:
            continue  # emulator and unrelated USB/TCP serials are ignored
        if row.state != "device":
            if is_pixel_service_alias(row.serial) or (endpoint and any(is_pixel_service_alias(s.adb_transport_alias) for s in by_endpoint.get(endpoint, []))):
                target_states.append(row)
            continue
        key = (row.serial, endpoint)
        if key in seen:
            continue
        seen.add(key)
        identities.append(identity_for(adb, row.serial, source, endpoint, instance))
    return identities, target_states


def online_represents_endpoint(endpoint: str, rows: list[DeviceRow], services: list[Service]) -> bool:
    aliases = {service.adb_transport_alias for service in services if service.endpoint == endpoint}
    return any(row.state == "device" and row.serial in aliases | {endpoint} for row in rows)


def pixel_endpoints_to_probe(services: list[Service], rows: list[DeviceRow]) -> list[str]:
    """Return every unrepresented current TLS-connect endpoint in stable order.

    A matching-looking mDNS name is only a probe-order hint. It never excludes
    generic names or substitutes for a read-only Android identity query.
    """
    by_endpoint: dict[str, list[Service]] = {}
    for service in services:
        by_endpoint.setdefault(service.endpoint, []).append(service)
    endpoints = [endpoint for endpoint in by_endpoint if not online_represents_endpoint(endpoint, rows, services)]
    return sorted(
        endpoints,
        key=lambda endpoint: (
            not any(is_pixel_service_alias(service.adb_transport_alias) for service in by_endpoint[endpoint]),
            endpoint,
        ),
    )


def capture_installed_apk(
    adb: str,
    target: str,
    output: Path,
    apk_path: Path,
    log: Path,
    *,
    filename: str = "installed-base.apk",
) -> dict[str, str]:
    paths = invoke_logged(adb, ["-s", target, "shell", "pm", "path", PACKAGE], "package-path", log)
    path_rows = [line.removeprefix("package:") for line in paths.splitlines() if line.startswith("package:")]
    if len(path_rows) != 1 or not path_rows[0].endswith("/base.apk"):
        raise ResolutionError(f"expected exactly one installed base.apk path for {PACKAGE}; got {len(path_rows)}")
    pulled = output / filename
    invoke_logged(adb, ["-s", target, "pull", path_rows[0], str(pulled)], "installed-base-apk-pull", log)
    digest = hashlib.sha256(pulled.read_bytes()).hexdigest()
    local_digest = hashlib.sha256(apk_path.read_bytes()).hexdigest()
    append_log(log, f"installed-apk-sha256={digest} local-apk-sha256={local_digest}")
    if digest != EXPECTED_APK_SHA256 or local_digest != EXPECTED_APK_SHA256 or digest != local_digest:
        raise ResolutionError("installed base.apk bytes do not match the frozen local candidate APK SHA-256")
    return {"package": PACKAGE, "installed_path": path_rows[0], "sha256": digest}


def verify_capture_paths(adb: str, target: str, log: Path) -> dict[str, str]:
    shell = invoke_logged(adb, ["-s", target, "shell", "true"], "shell-ready", log)
    if shell:
        raise ResolutionError("read-only shell true returned unexpected output")

    screenshot = adb_call(adb, "-s", target, "exec-out", "screencap", "-p")
    if screenshot.returncode != 0 or not valid_png_stream(screenshot.stdout):
        err = screenshot.stderr.decode("utf-8", errors="replace").strip()
        raise ResolutionError(
            f"screenshot stream did not return a PNG (exit {screenshot.returncode}; stderr_present={bool(err)})"
        )
    append_log(log, f"{datetime.now(timezone.utc).isoformat(timespec='seconds')} screenshot-stream=PASS bytes={len(screenshot.stdout)} stored=no")

    # Read only one log line and discard it so unrelated app/personal logs are
    # not copied into the durable evidence record.
    logs = adb_call(adb, "-s", target, "logcat", "-d", "-t", "1")
    if logs.returncode != 0:
        err = logs.stderr.decode("utf-8", errors="replace").strip()
        raise ResolutionError(f"bounded logcat probe failed (exit {logs.returncode}; stderr_present={bool(err)})")
    append_log(log, f"{datetime.now(timezone.utc).isoformat(timespec='seconds')} logcat-probe=PASS lines=discarded bytes={len(logs.stdout)}")
    return {"shell": "PASS", "screenshot_stream": "PASS_NOT_STORED", "bounded_logcat_probe": "PASS_OUTPUT_DISCARDED"}


def verify_app_entry_ready(adb: str, target: str, log: Path) -> dict[str, str]:
    process = adb_call(adb, "-s", target, "shell", "pidof", PACKAGE)
    process_output = text_of(process)
    process_error = process.stderr.decode("utf-8", errors="replace").replace("\r", "").strip()
    append_log(log, f"{datetime.now(timezone.utc).isoformat(timespec='seconds')} app-process-absent exit={process.returncode} output_present={bool(process_output)} stderr_present={bool(process_error)}")
    if process.returncode != 1 or process_output or process_error:
        raise ResolutionError("the production app process is not confirmed absent; Phase C clean entry is not ready and no force-stop is allowed")

    activity = adb_call(adb, "-s", target, "shell", "dumpsys", "activity", "activities")
    activity_text = require_success("app-entry activity checkpoint", activity)
    try:
        activity_record = extract_activity(activity_text, PACKAGE, expect_present=False)
    except ActivityCheckpointError as exc:
        raise ResolutionError(f"the production app is unexpectedly resumed or the activity checkpoint is ambiguous: {exc}") from exc
    activity_field = activity_record.partition("=")[0]
    append_log(
        log,
        f"{datetime.now(timezone.utc).isoformat(timespec='seconds')} app-entry-activity-absent=PASS status=APP_ABSENT record_field={activity_field} app_package_present=False",
    )
    return {"process": "ABSENT", "resumed_activity": "APP_ABSENT"}


def verify_phase_b_final(
    adb: str,
    output: Path,
    apk_path: Path,
    log: Path,
    target: Identity,
    initial_installed_apk: dict[str, str],
) -> dict[str, object]:
    """Recheck the same target and clean app/capture state before sealing binding."""
    current = identity_for(adb, target.target, "phase-b-final-recheck", target.endpoint, target.service_instance)
    stable_before = (target.serial_sha256, target.model, target.product, target.sdk, target.fingerprint)
    stable_now = (current.serial_sha256, current.model, current.product, current.sdk, current.fingerprint)
    if stable_now != stable_before:
        raise ResolutionError("the final Android identity no longer matches the Pixel identity resolved earlier in Phase B")
    if target.route_boot_id:
        if not current.route_boot_id:
            raise ResolutionError("the final Pixel identity recheck lost the previously available boot identity")
        if target.route_boot_id != current.route_boot_id:
            raise ResolutionError("the Pixel boot identity changed during Phase B")

    installed_final = capture_installed_apk(
        adb, target.target, output, apk_path, log, filename="installed-base-final.apk"
    )
    if installed_final["installed_path"] != initial_installed_apk["installed_path"]:
        raise ResolutionError("the installed package path changed during Phase B")

    app_entry = verify_app_entry_ready(adb, target.target, log)
    capture = verify_capture_paths(adb, target.target, log)
    app_entry_after_capture = verify_app_entry_ready(adb, target.target, log)
    return {
        "identity": "SAME_PIXEL_STABLE_IDENTITY",
        "same_boot_session": "PASS" if target.route_boot_id and target.route_boot_id == current.route_boot_id else "NOT_AVAILABLE",
        "installed_apk": installed_final,
        "app_entry_readiness": app_entry,
        "capture_readiness": capture,
        "app_entry_after_capture": app_entry_after_capture,
    }


def resolve(adb: str, output: Path, apk_path: Path) -> dict[str, object]:
    if output.exists():
        if not output.is_dir() or any(output.iterdir()):
            raise ResolutionError("refusing to write into a nonempty or non-directory binding path; choose a fresh output directory")
    else:
        output.mkdir(parents=True, exist_ok=False)
    log = output / "binding.log"

    mdns_services = record_service_snapshot(adb, log)
    first_rows = devices_snapshot(adb, "devices-initial", mdns_services, log)
    identities, blocked = collect_identity_candidates(adb, first_rows, mdns_services, log)
    if blocked:
        blocked_states = ",".join(sorted({r.state for r in blocked}))
        raise ResolutionError(f"a current Pixel-qualified ADB row is not online; blocked_rows={len(blocked)} states={blocked_states}")

    candidate_pixel = canonical_target(identities)

    # Probe current connect endpoints only while the expected Pixel identity
    # is still unresolved. Service names determine probe order, never identity;
    # once the stable Pixel serial/model/product/build tuple is verified, extra
    # advertised routes cannot improve the target binding and would create
    # unnecessary ADB transports on the owner's phone.
    endpoints_to_probe = (
        [] if candidate_pixel is not None else pixel_endpoints_to_probe(mdns_services, first_rows)
    )

    connect_attempted: list[str] = []
    connected_here: set[str] = set()
    detached_routes: set[str] = set()
    if endpoints_to_probe:
        for endpoint in endpoints_to_probe:
            connect_attempted.append(endpoint)
            connect_error: str | None = None
            try:
                result = adb_call(adb, "connect", endpoint)
            except ResolutionError as exc:
                # The ADB server may have completed a connection after the
                # client timed out. Query the current device list before
                # deciding whether any cleanup is safe.
                connect_error = "command-error"
                append_log(log, f"{datetime.now(timezone.utc).isoformat(timespec='seconds')} connect-current-tls-endpoint endpoint_sha256={sha256_text(endpoint)} outcome=command-error")
            else:
                out = text_of(result)
                err = result.stderr.decode("utf-8", errors="replace").strip()
                response = "connected" if out.startswith("connected to ") else "already-connected" if out.startswith("already connected to ") else "not-connected"
                if result.returncode == 0 and response == "connected":
                    connected_here.add(endpoint)
                append_log(log, f"{datetime.now(timezone.utc).isoformat(timespec='seconds')} connect-current-tls-endpoint endpoint_sha256={sha256_text(endpoint)} exit={result.returncode} response={response} stderr_present={bool(err)}")

            refreshed_rows = devices_snapshot(adb, f"devices-after-current-mdns-connect-{len(connect_attempted)}", mdns_services, log)
            new_identities, blocked = collect_identity_candidates(adb, refreshed_rows, mdns_services, log)
            if blocked:
                blocked_states = ",".join(sorted({r.state for r in blocked}))
                raise ResolutionError(f"a current Pixel-qualified ADB row is not online; blocked_rows={len(blocked)} states={blocked_states}")
            identities.extend(new_identities)
            candidate_pixel = canonical_target(identities)
            if candidate_pixel is not None:
                if endpoint in connected_here:
                    endpoint_identities = [identity for identity in new_identities if identity.endpoint == endpoint]
                    if not endpoint_identities:
                        raise ResolutionError(
                            "newly opened current ADB route returned no read-only Android identity; preserving it and stopping Phase B"
                        )
                    endpoint_profiles = {
                        (identity.serial_sha256, identity.model, identity.product, identity.sdk, identity.fingerprint)
                        for identity in endpoint_identities
                    }
                    if len(endpoint_profiles) != 1:
                        raise ResolutionError(
                            "newly opened current ADB route returned conflicting Android identities; preserving it and stopping Phase B"
                        )
                    if all(identity.serial_sha256 != EXPECTED_SERIAL_SHA256 for identity in endpoint_identities):
                        failures = disconnect_new_routes(adb, [endpoint], log)
                        if failures:
                            raise ResolutionError(
                                "a newly opened endpoint was positively identified as non-Pixel but could not be detached"
                            )
                        connected_here.remove(endpoint)
                        detached_routes.add(endpoint)
                    elif any(identity.serial_sha256 != EXPECTED_SERIAL_SHA256 for identity in endpoint_identities):
                        raise ResolutionError(
                            "newly opened current ADB route returned mixed Pixel and non-Pixel identities; preserving it and stopping Phase B"
                        )
                elif connect_error and not any(identity.endpoint == endpoint for identity in new_identities):
                    raise ResolutionError("ADB connect timed out for a current endpoint; no read-only identity was recovered, so Phase B stops without disconnecting an uncertain route")
                if candidate_pixel is not None:
                    break
                continue

            if endpoint in connected_here:
                endpoint_identities = [identity for identity in new_identities if identity.endpoint == endpoint]
                if not endpoint_identities:
                    raise ResolutionError(
                        "newly opened current ADB route returned no read-only Android identity; preserving it and stopping Phase B"
                    )
                if any(identity.serial_sha256 == EXPECTED_SERIAL_SHA256 for identity in endpoint_identities):
                    raise ResolutionError("a connected route exposed conflicting Pixel identity data; preserving it and stopping Phase B")
                failures = disconnect_new_routes(adb, [endpoint], log)
                if failures:
                    raise ResolutionError(
                        "a newly opened endpoint was positively identified as non-Pixel but could not be detached"
                    )
                connected_here.remove(endpoint)
                detached_routes.add(endpoint)
            elif connect_error:
                # The connect may have completed in the ADB server despite the
                # client timeout. Do not disconnect or continue unless the
                # Pixel identity above resolved; no ownership of this route
                # can be proven after a timed-out connect.
                raise ResolutionError("ADB connect timed out for a current endpoint; identity was not resolved, so Phase B stops without disconnecting an uncertain route")

    if candidate_pixel is None:
        failures = disconnect_new_routes(adb, connected_here, log)
        if failures:
            raise ResolutionError(
                f"Pixel identity did not resolve and {len(failures)} newly opened candidate ADB route(s) could not be detached"
            )
        raise ResolutionError("no currently online wireless ADB target returned the expected Pixel 9 stable identity")

    # Prefer an exact current mDNS mapping when one exists. Android's ADB
    # server can retain a live online TLS-connect transport while mDNS
    # discovery temporarily returns no services, though. In that case accept
    # only the exact already-online TLS-connect service alias that was
    # queried and whose Android serial/model/product/API were verified above;
    # the final get-state and identity recheck must still pass below.
    current_aliases = {service.adb_transport_alias for service in mdns_services}
    current_endpoints = {service.endpoint for service in mdns_services}
    target_is_current_online_pixel_alias = (
        candidate_pixel.source.startswith("current-online-tls-connect-alias")
        and is_tls_connect_service_alias(candidate_pixel.target)
        and any(row.serial == candidate_pixel.target and row.state == "device" for row in first_rows)
    )
    if candidate_pixel.target not in current_aliases | current_endpoints and not target_is_current_online_pixel_alias:
        raise ResolutionError("the selected target no longer maps to a current advertised TLS-connect service")

    selected_endpoints = {candidate_pixel.endpoint} if candidate_pixel.endpoint else set()
    selected_endpoints.update(
        service.endpoint
        for service in mdns_services
        if service.adb_transport_alias == candidate_pixel.target
    )
    redundant_new_routes = connected_here - selected_endpoints
    if redundant_new_routes:
        failures = disconnect_new_routes(adb, redundant_new_routes, log)
        if failures:
            raise ResolutionError("a duplicate newly opened ADB route could not be detached after stable identity comparison")
        connected_here.difference_update(redundant_new_routes)
        detached_routes.update(redundant_new_routes)

    app_sha = capture_installed_apk(adb, candidate_pixel.target, output, apk_path, log)
    app_entry = verify_app_entry_ready(adb, candidate_pixel.target, log)
    capture = verify_capture_paths(adb, candidate_pixel.target, log)
    final_check = verify_phase_b_final(adb, output, apk_path, log, candidate_pixel, app_sha)

    durable_identity = asdict(candidate_pixel) | {"target": None, "source": None, "endpoint": None, "service_instance": None}
    # The Android boot ID is only used transiently to prove that two current
    # wireless routes reach one live system. Do not persist that per-boot value.
    durable_identity["route_boot_id"] = "not-persisted"
    binding: dict[str, object] = {
        "schema_version": 1,
        "resolved_at_utc": datetime.now(timezone.utc).isoformat(timespec="seconds"),
        "target": candidate_pixel.target,
        "target_source": candidate_pixel.source,
        "endpoint_last_resolved": candidate_pixel.endpoint,
        "service_instance": candidate_pixel.service_instance,
        "transport": "wireless-adb-only",
        "pairing_performed": False,
        "resolver_sha256": hashlib.sha256(Path(__file__).read_bytes()).hexdigest(),
        "identity": durable_identity,
        "installed_apk": app_sha,
        "app_entry_readiness": app_entry,
        "capture_readiness": capture,
        "phase_b_final_recheck": final_check,
        "local_apk_sha256": hashlib.sha256(apk_path.read_bytes()).hexdigest(),
        "adb_connect_attempted": bool(connect_attempted),
        "adb_connect_attempted_endpoints": connect_attempted,
        "adb_connect_created_nonpixel_routes_detached": sorted(detached_routes),
        "adb_connect_created_pixel_route_left_bound": sorted(connected_here),
        "note": "Stable Android identity is authoritative; service endpoint is ephemeral and must be rediscovered on later sessions.",
    }
    binding_path = output / "binding.json"
    binding_path.write_text(json.dumps(binding, indent=2) + "\n", encoding="utf-8")
    return binding


def main(argv: list[str] | None = None) -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--adb", required=True, help="absolute path to Android SDK platform-tools/adb")
    parser.add_argument("--output", required=True, type=Path, help="new ignored evidence directory")
    parser.add_argument("--apk", required=True, type=Path, help="exact frozen local APK")
    args = parser.parse_args(argv)
    try:
        binding = resolve(str(args.adb), args.output, args.apk)
    except (ResolutionError, OSError) as exc:
        try:
            append_log(
                args.output / "binding.log",
                f"{datetime.now(timezone.utc).isoformat(timespec='seconds')} phase-b=NOT_PASS error_type={type(exc).__name__} detail=redacted",
            )
        except OSError:
            pass
        print("PIXEL_BINDING_NOT_PASS: read-only target resolution failed; inspect the ignored local binding log", file=sys.stderr)
        return 2
    safe_summary = {
        "result": "PASS",
        "resolved_at_utc": binding["resolved_at_utc"],
        "transport": binding["transport"],
        "target_sha256": hashlib.sha256(str(binding["target"]).encode("utf-8")).hexdigest(),
        "stable_identity": {
            "serial_sha256": binding["identity"]["serial_sha256"],
            "model": binding["identity"]["model"],
            "product": binding["identity"]["product"],
            "sdk": binding["identity"]["sdk"],
            "fingerprint": binding["identity"]["fingerprint"],
        },
        "installed_apk_sha256": binding["installed_apk"]["sha256"],
        "app_entry_readiness": binding["app_entry_readiness"],
        "capture_readiness": binding["capture_readiness"],
        "candidate_route_count": len(binding["adb_connect_attempted_endpoints"]),
        "pairing_performed": binding["pairing_performed"],
    }
    print(json.dumps(safe_summary, indent=2))
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
