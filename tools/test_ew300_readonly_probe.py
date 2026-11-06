#!/usr/bin/env python3
import importlib.util
import pathlib
import sys
import unittest

MODULE_PATH = pathlib.Path(__file__).with_name("ew300_readonly_probe.py")
SPEC = importlib.util.spec_from_file_location("ew300_readonly_probe", MODULE_PATH)
probe = importlib.util.module_from_spec(SPEC)
assert SPEC.loader is not None
sys.modules[SPEC.name] = probe
SPEC.loader.exec_module(probe)


class Ew300ReadonlyProbeTest(unittest.TestCase):
    def test_read_frame_is_exact(self):
        self.assertEqual(
            probe.build_read_frame(0x26).hex().upper(),
            "4B26000000520000000000",
        )

    def test_response_decodes_with_or_without_report_id(self):
        self.assertEqual(
            probe.parse_read_response(0x26, bytes.fromhex("4B260000005200F5FF6400")).hex().upper(),
            "F5FF6400",
        )
        self.assertEqual(
            probe.parse_read_response(0x26, bytes.fromhex("260000005200F5FF6400")).hex().upper(),
            "F5FF6400",
        )

    def test_foreign_or_malformed_replies_are_rejected(self):
        for frame in (
            "54260000005200F5FF6400",
            "4B270000005200F5FF6400",
            "4B260000005700F5FF6400",
            "4B260000005200",
        ):
            with self.assertRaises(probe.ResponseError):
                probe.parse_read_response(0x26, bytes.fromhex(frame))

    def test_every_forbidden_command_is_rejected_before_transport(self):
        for command in probe.FORBIDDEN_COMMANDS:
            frame = bytearray(probe.build_read_frame(0x26))
            frame[5] = command
            with self.assertRaises(probe.ProbeSafetyError):
                probe.require_read_command(bytes(frame))

    def test_fake_capture_emits_only_reads_and_covers_every_register(self):
        fake = probe.FakeHid()
        identity = probe.HidIdentity(
            probe.VENDOR_ID,
            probe.PRODUCT_ID,
            probe.MANUFACTURER,
            probe.PRODUCT,
            b"fake",
            False,
        )
        capture = probe.ReadOnlyProbe(probe.HidTransport(fake, identity)).capture()

        self.assertEqual(len(capture["registers"]), probe.REGISTER_COUNT)
        self.assertTrue(all(frame[5] == probe.READ_COMMAND for frame in fake.frames))
        self.assertEqual(capture["registers"]["0x26"]["raw"], "F5FF6400")

    def test_missing_register_stops_the_capture(self):
        fake = probe.FakeHid(fail_register=0x40)
        identity = probe.HidIdentity(
            probe.VENDOR_ID,
            probe.PRODUCT_ID,
            probe.MANUFACTURER,
            probe.PRODUCT,
            b"fake",
            False,
        )
        with self.assertRaises(probe.ResponseError):
            probe.ReadOnlyProbe(probe.HidTransport(fake, identity)).capture()

    def test_non_exact_identity_is_rejected(self):
        with self.assertRaises(probe.IdentityError):
            probe.find_exact_device(
                [
                    {
                        "vendor_id": probe.VENDOR_ID,
                        "product_id": probe.PRODUCT_ID,
                        "manufacturer_string": "OTHER",
                        "product_string": probe.PRODUCT,
                        "path": b"x",
                    }
                ]
            )


if __name__ == "__main__":
    unittest.main()

