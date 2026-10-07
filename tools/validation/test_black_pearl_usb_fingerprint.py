#!/usr/bin/env python3
"""Fixture tests for the sanitized Black Pearl USB descriptor parser."""

import sys
import unittest
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))

from black_pearl_usb_fingerprint import DescriptorError, extract_black_pearl


def device(name: str, vendor: str, product: str, manufacturer: str, model: str, serial: str) -> str:
    return f"""      name={name}
      vendor_id={vendor}
      product_id={product}
      manufacturer_name={manufacturer}
      product_name={model}
      serial_number={serial}
      configurations={{
        id=1
        interfaces=[
        ]
      }}
"""


def dump(*devices: str) -> str:
    return "USB Manager State:\n  host_manager={\n    devices={\n" + "".join(devices) + "    }\n  }\n"


class BlackPearlUsbFingerprintTest(unittest.TestCase):
    def setUp(self):
        self.black_pearl = device(
            "/dev/bus/usb/001/004", "13058", "17384", "TTGK Technology", "TE-C", "330243E8260129"
        )

    def test_extracts_exact_record_and_ignores_other_devices(self):
        unrelated = device("/dev/bus/usb/001/003", "12722", "273", "LE XIAN", "SIMGOT", "other")
        self.assertEqual(
            extract_black_pearl(dump(unrelated, self.black_pearl)),
            [
                "name=/dev/bus/usb/001/004",
                "vendor_id=13058",
                "product_id=17384",
                "manufacturer_name=TTGK Technology",
                "product_name=TE-C",
                "serial_number=330243E8260129",
            ],
        )

    def test_absent_or_duplicate_identity_fails_closed(self):
        with self.assertRaises(DescriptorError):
            extract_black_pearl(dump())
        with self.assertRaises(DescriptorError):
            extract_black_pearl(dump(self.black_pearl, self.black_pearl))

    def test_conflicting_signature_record_fails_closed(self):
        conflict = device("/dev/bus/usb/001/004", "13058", "17384", "TTGK Technology", "OTHER", "wrong")
        with self.assertRaises(DescriptorError):
            extract_black_pearl(dump(conflict))

    def test_signature_with_invalid_usb_device_path_fails_closed(self):
        invalid_path = device("usb:001/004", "13058", "17384", "TTGK Technology", "TE-C", "330243E8260129")
        with self.assertRaises(DescriptorError):
            extract_black_pearl(dump(invalid_path))


if __name__ == "__main__":
    unittest.main()
