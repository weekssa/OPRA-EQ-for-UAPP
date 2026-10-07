#!/usr/bin/env python3
"""Focused parser/identity tests for pixel_adb_binding.py."""

import hashlib
import unittest
import sys
from pathlib import Path
from unittest.mock import patch

sys.path.insert(0, str(Path(__file__).resolve().parent))

from pixel_adb_binding import (
    EXPECTED_MODEL,
    EXPECTED_PRODUCT,
    Identity,
    ResolutionError,
    canonical_target,
    is_pixel_service_alias,
    parse_devices,
    parse_mdns_services,
    valid_boot_id,
    valid_endpoint,
)


class PixelAdbBindingTest(unittest.TestCase):
    SERIAL = "PIXELTESTSERIAL001"
    SERIAL_SHA256 = hashlib.sha256(SERIAL.encode("utf-8")).hexdigest()
    PIXEL_FINGERPRINT = "google/tokay/tokay:17/build:keys"

    def setUp(self):
        self.serial_patch = patch("pixel_adb_binding.EXPECTED_SERIAL_SHA256", self.SERIAL_SHA256)
        self.serial_patch.start()

    def tearDown(self):
        self.serial_patch.stop()

    def test_mdns_ignores_pairing_and_keeps_multiple_current_connect_services(self):
        text = """List of discovered mdns services
adb-PIXELTESTSERIAL001-a _adb-tls-pairing._tcp 192.168.50.24:39111
adb-PIXELTESTSERIAL001-a _adb-tls-connect._tcp 192.168.50.24:33185
other-device _adb-tls-connect._tcp 192.168.50.30:44771
"""
        services, pairing, other = parse_mdns_services(text)
        self.assertEqual(pairing, 1)
        self.assertEqual(other, 0)
        self.assertEqual([s.endpoint for s in services], ["192.168.50.24:33185", "192.168.50.30:44771"])
        self.assertEqual(services[0].instance, "adb-PIXELTESTSERIAL001-a")
        self.assertEqual(services[0].adb_transport_alias, "adb-PIXELTESTSERIAL001-a._adb-tls-connect._tcp")

    def test_pairing_only_advertisement_without_endpoint_is_ignored(self):
        services, pairing, other = parse_mdns_services(
            "List of discovered mdns services\nadb-PIXELTESTSERIAL001-a _adb-tls-pairing._tcp\n"
        )
        self.assertEqual(services, [])
        self.assertEqual(pairing, 1)
        self.assertEqual(other, 0)

    def test_invalid_numeric_ipv4_and_port_are_rejected(self):
        for endpoint in ("999.1.1.1:1234", "192.168.1.2:0", "192.168.1.2:65536", "host.local:1234"):
            with self.subTest(endpoint=endpoint):
                self.assertFalse(valid_endpoint(endpoint))
        self.assertTrue(valid_endpoint("[2001:db8::24]:5555"))
        self.assertFalse(valid_endpoint("[2001:db8::24]:65536"))
        self.assertFalse(valid_endpoint("2001:db8::24:5555"))

    def test_service_alias_requires_nonempty_suffix(self):
        self.assertTrue(is_pixel_service_alias(f"adb-{self.SERIAL}-abc._adb-tls-connect._tcp"))
        self.assertFalse(is_pixel_service_alias(f"adb-{self.SERIAL}-._adb-tls-connect._tcp"))
        self.assertFalse(is_pixel_service_alias(f"adb-{self.SERIAL}-abc._adb-tls-pairing._tcp"))

    def test_device_rows_keep_wireless_candidates_and_emulator_for_explicit_ignore(self):
        rows = parse_devices("""List of devices attached
adb-PIXELTESTSERIAL001-a._adb-tls-connect._tcp device product:tokay model:Pixel_9 transport_id:4
192.168.50.24:33185 device product:tokay model:Pixel_9 transport_id:5
emulator-5556 device product:sdk_gphone64_arm64 model:sdk_gphone64_arm64
PIXELTESTSERIAL001 device usb:1-1 product:tokay model:Pixel_9
""")
        self.assertEqual(len(rows), 4)
        self.assertEqual(rows[0].state, "device")
        self.assertEqual(rows[2].serial, "emulator-5556")
        self.assertEqual(rows[3].details.split()[0], "usb:1-1")

    def test_no_permissions_is_parsed_as_a_blocked_transport_state(self):
        rows = parse_devices("List of devices attached\nadb-PIXELTESTSERIAL001-a._adb-tls-connect._tcp no permissions product:tokay model:Pixel_9\n")
        self.assertEqual(len(rows), 1)
        self.assertEqual(rows[0].state, "no permissions")
        self.assertEqual(rows[0].details, "product:tokay model:Pixel_9")

    def test_service_alias_and_endpoint_alias_collapse_only_when_same_endpoint(self):
        service_alias = f"adb-{self.SERIAL}-abc._adb-tls-connect._tcp"
        identities = [
            Identity(service_alias, "service", "192.168.50.24:33185", service_alias, self.SERIAL_SHA256, EXPECTED_MODEL, EXPECTED_PRODUCT, "37", self.PIXEL_FINGERPRINT, ""),
            Identity("192.168.50.24:33185", "endpoint", "192.168.50.24:33185", service_alias, self.SERIAL_SHA256, EXPECTED_MODEL, EXPECTED_PRODUCT, "37", self.PIXEL_FINGERPRINT, ""),
        ]
        self.assertEqual(canonical_target(identities).target, service_alias)

    def test_independent_matching_routes_are_ambiguous(self):
        identities = [
            Identity("192.168.50.24:33185", "endpoint", "192.168.50.24:33185", None, self.SERIAL_SHA256, EXPECTED_MODEL, EXPECTED_PRODUCT, "37", self.PIXEL_FINGERPRINT, ""),
            Identity("192.168.50.24:33186", "endpoint", "192.168.50.24:33186", None, self.SERIAL_SHA256, EXPECTED_MODEL, EXPECTED_PRODUCT, "37", self.PIXEL_FINGERPRINT, ""),
        ]
        with self.assertRaises(ResolutionError):
            canonical_target(identities)

    def test_independent_routes_collapse_only_when_same_boot_identity_is_proven(self):
        identities = [
            Identity("192.168.50.24:33185", "endpoint", "192.168.50.24:33185", None, self.SERIAL_SHA256, EXPECTED_MODEL, EXPECTED_PRODUCT, "37", self.PIXEL_FINGERPRINT, "11111111-1111-1111-1111-111111111111"),
            Identity("192.168.50.25:33186", "endpoint", "192.168.50.25:33186", None, self.SERIAL_SHA256, EXPECTED_MODEL, EXPECTED_PRODUCT, "37", self.PIXEL_FINGERPRINT, "11111111-1111-1111-1111-111111111111"),
        ]
        self.assertEqual(canonical_target(identities).target, "192.168.50.24:33185")

    def test_boot_id_requires_uuid_shape(self):
        self.assertTrue(valid_boot_id("11111111-1111-4111-8111-111111111111"))
        for value in ("--------------------", "11111111111111111111111111111111", "11111111-1111-1111-1111-11111111111x"):
            with self.subTest(value=value):
                self.assertFalse(valid_boot_id(value))
        malformed = [
            Identity("192.168.50.24:33185", "endpoint", "192.168.50.24:33185", None, self.SERIAL_SHA256, EXPECTED_MODEL, EXPECTED_PRODUCT, "37", self.PIXEL_FINGERPRINT, "--------------------"),
            Identity("192.168.50.25:33186", "endpoint", "192.168.50.25:33186", None, self.SERIAL_SHA256, EXPECTED_MODEL, EXPECTED_PRODUCT, "37", self.PIXEL_FINGERPRINT, "--------------------"),
        ]
        with self.assertRaises(ResolutionError):
            canonical_target(malformed)

    def test_wrong_pixel_profile_fingerprint_is_rejected(self):
        identity = Identity(
            "192.168.50.24:33185", "endpoint", "192.168.50.24:33185", None,
            self.SERIAL_SHA256, EXPECTED_MODEL, EXPECTED_PRODUCT, "37", "other/tokay/tokay:17/build", "",
        )
        with self.assertRaisesRegex(ResolutionError, "fingerprint"):
            canonical_target([identity])


if __name__ == "__main__":
    unittest.main()
