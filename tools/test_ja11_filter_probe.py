import contextlib
import io
import unittest

import ja11_filter_probe as probe


class Ja11FilterProbeTest(unittest.TestCase):
    def test_exact_requests(self):
        self.assertEqual(
            [request.frame.hex(" ").upper() for request in probe.b1_requests()],
            [
                "02 BB 0B 00 01 02 00 3A EE",
                "02 BB 0B 00 00 11 00 00 EE",
                "02 BB 0B 00 00 11 00 28 EE",
                "02 BB 0B 00 02 11 00 67 EE",
            ],
        )

    def test_crc_values_and_check_vector(self):
        self.assertEqual(probe.crc8_maxim(b"123456789"), 0xA1)
        self.assertEqual(probe.crc8_maxim(bytes.fromhex("00 01 02 00")), 0x3A)
        self.assertEqual(probe.crc8_maxim(bytes.fromhex("00 00 11 00")), 0x28)
        self.assertEqual(probe.crc8_maxim(bytes.fromhex("00 02 11 00")), 0x67)

    def test_parse_volume_and_padding(self):
        raw = self._response(sequence=1, command=2, payload=bytes([60])) + bytes(50)
        parsed = probe.parse_response(raw, expected_sequence=1, expected_command=2)
        self.assertEqual(parsed.payload, bytes([60]))
        self.assertEqual(parsed.crc, probe.crc8_maxim(bytes.fromhex("00 01 02 01 3C")))

    def test_parse_filter(self):
        raw = self._response(sequence=2, command=0x11, payload=bytes([3]))
        parsed = probe.parse_response(raw, expected_sequence=2, expected_command=0x11)
        self.assertEqual(parsed.payload, bytes([3]))

    def test_rejects_wrong_sequence(self):
        raw = self._response(sequence=1, command=2, payload=bytes([60]))
        with self.assertRaisesRegex(ValueError, "sequence mismatch"):
            probe.parse_response(raw, expected_sequence=2, expected_command=2)

    def test_rejects_wrong_crc(self):
        raw = bytearray(self._response(sequence=1, command=2, payload=bytes([60])))
        raw[-2] ^= 1
        with self.assertRaisesRegex(ValueError, "CRC mismatch"):
            probe.parse_response(bytes(raw), expected_sequence=1, expected_command=2)

    def test_rejects_bad_terminator_and_nonzero_padding(self):
        raw = bytearray(self._response(sequence=1, command=2, payload=bytes([60])))
        raw[-1] = 0
        with self.assertRaisesRegex(ValueError, "terminator"):
            probe.parse_response(bytes(raw), expected_sequence=1, expected_command=2)
        raw = self._response(sequence=1, command=2, payload=bytes([60])) + b"\x01"
        with self.assertRaisesRegex(ValueError, "trailing"):
            probe.parse_response(raw, expected_sequence=1, expected_command=2)

    def test_classification_matrix(self):
        self.assertEqual(probe.classify_outcomes("RRRR").split(":", 1)[0], "REOPEN")
        self.assertEqual(probe.classify_outcomes("RSSS").split(":", 1)[0], "BOUNDED_NEGATIVE")
        self.assertEqual(probe.classify_outcomes("RSSS"), "BOUNDED_NEGATIVE: all three exact filter reads were silent")
        self.assertEqual(probe.classify_outcomes("RSRS").split(":", 1)[0], "REOPEN")
        self.assertIn("volume control failed", probe.classify_outcomes("SRRR"))
        self.assertIn("volume control failed", probe.classify_outcomes("SSRR"))

    def test_all_interpretive_patterns_are_covered(self):
        patterns = {
            "RRR": "REOPEN",
            "SRR": "REOPEN",
            "SSR": "REOPEN",
            "SSS": "BOUNDED_NEGATIVE",
            "RSR": "REOPEN",
            "RRS": "REOPEN",
            "RSS": "REOPEN",
            "SRS": "REOPEN",
        }
        for pattern, expected in patterns.items():
            with self.subTest(pattern=pattern):
                self.assertEqual(probe.classify_outcomes("R" + pattern).split(":", 1)[0], expected)

    def test_plan_has_no_filter_set(self):
        document = probe.plan_document()
        self.assertFalse(document["filter_writes"])
        frames = [entry["frame_hex"] for entry in document["requests"]]
        self.assertTrue(all(frame.startswith("02 BB 0B") for frame in frames))
        self.assertFalse(any("02 AA 0A" in frame for frame in frames))

    def test_hardware_parser_requires_approval_reference(self):
        parser = probe.build_parser()
        with contextlib.redirect_stderr(io.StringIO()):
            with self.assertRaises(SystemExit):
                parser.parse_args(
                    [
                        "run",
                        "--execute",
                        "--device-path",
                        "/dev/example",
                        "--evidence-dir",
                        "/tmp/example",
                        "--operator",
                        "owner",
                        "--host-label",
                        "host",
                        "--device-provenance",
                        "unit",
                    ]
                )

    def test_exact_identity_rejects_substitute(self):
        item = {
            "vendor_id": 0x2972,
            "product_id": 0x0102,
            "path": b"path-a",
            "serial_number": "serial-a",
            "product_string": "FiiO JA11",
        }
        selected = probe.select_exact_identity(
            [item, {**item, "path": b"path-b"}],
            path_value="path-a",
            vendor_id=0x2972,
            product_id=0x0102,
        )
        self.assertEqual(selected, item)
        self.assertNotEqual(
            probe.identity_signature(item),
            probe.identity_signature({**item, "serial_number": "serial-b"}),
        )
        with self.assertRaisesRegex(RuntimeError, "not found uniquely"):
            probe.select_exact_identity(
                [{**item, "path": b"path-b"}],
                path_value="path-a",
                vendor_id=0x2972,
                product_id=0x0102,
            )

    @staticmethod
    def _response(*, sequence: int, command: int, payload: bytes) -> bytes:
        prefix = bytes(
            [
                probe.REPORT_ID,
                probe.READ_1,
                probe.READ_2,
                (sequence >> 8) & 0xFF,
                sequence & 0xFF,
                command,
                len(payload),
            ]
        ) + payload
        return prefix + bytes([probe.crc8_maxim(prefix[3:]), probe.TERMINATOR])


if __name__ == "__main__":
    unittest.main()
