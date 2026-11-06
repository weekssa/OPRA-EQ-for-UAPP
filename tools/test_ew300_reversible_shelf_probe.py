#!/usr/bin/env python3
import importlib.util
import pathlib
import sys
import unittest

MODULE_PATH = pathlib.Path(__file__).with_name("ew300_reversible_shelf_probe.py")
SPEC = importlib.util.spec_from_file_location("ew300_reversible_shelf_probe", MODULE_PATH)
probe = importlib.util.module_from_spec(SPEC)
assert SPEC.loader is not None
sys.modules[SPEC.name] = probe
SPEC.loader.exec_module(probe)


class Ew300ReversibleShelfProbeTest(unittest.TestCase):
    def test_exact_low_and_high_frames(self):
        self.assertEqual(
            probe.build_write_frame(0x26, probe.LOW_SHELF_A).hex().upper(),
            "4B260000005700C4FFFA00",
        )
        self.assertEqual(
            probe.build_write_frame(0x27, probe.HIGH_SHELF_B).hex().upper(),
            "4B270000005700C3020400",
        )

    def test_guard_rejects_forbidden_commands_registers_and_payloads(self):
        guard = probe.Guard({probe.LOW_SHELF_A, probe.LOW_SHELF_B})
        for command in (0x53, 0x43, 0x08, 0x54):
            frame = bytearray(probe.build_read_frame(0x26))
            frame[5] = command
            with self.assertRaises(probe.ProbeSafetyError):
                guard.check(bytes(frame))
        with self.assertRaises(probe.ProbeSafetyError):
            probe.build_write_frame(0x66, b"\x00\x00\x00\x00")
        with self.assertRaises(probe.ProbeSafetyError):
            guard.check(probe.build_write_frame(0x26, b"\x00\x00\x00\x00"))

    def test_self_test_sequence_restores_baseline(self):
        self.assertEqual(probe.self_test(), 0)

    def test_write_failure_still_restores_baseline(self):
        fake = probe.FakeHid(fail_after_writes=2)
        guard = probe.Guard({probe.LOW_SHELF_A, probe.LOW_SHELF_B,
                             probe.HIGH_SHELF_A, probe.HIGH_SHELF_B,
                             fake.baseline[probe.BAND_A], fake.baseline[probe.BAND_B]})
        transport = probe.Transport(fake, {"vendor_id": "31b2"}, guard)
        with self.assertRaises(probe.ProbeError):
            probe.run_reversible(transport)
        self.assertGreaterEqual(fake.write_count, 2)
        self.assertEqual(fake.state[probe.BAND_A], fake.baseline[probe.BAND_A])
        self.assertEqual(fake.state[probe.BAND_B], fake.baseline[probe.BAND_B])


if __name__ == "__main__":
    unittest.main()

