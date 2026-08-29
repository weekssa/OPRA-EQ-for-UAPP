import unittest

from autoeq_ingest import acoustic_fingerprint, build_candidate, parse_parametric_eq


SAMPLE = """Preamp: -6.1 dB
Filter 1: ON LSC Fc 105 Hz Gain 6.4 dB Q 0.70
Filter 2: ON PK Fc 8800 Hz Gain 5.1 dB Q 1.42
Filter 3: ON HSC Fc 10000 Hz Gain -2.1 dB Q 0.70
"""


class AutoEqIngestTest(unittest.TestCase):
    def test_parses_parametric_text(self):
        parsed = parse_parametric_eq(SAMPLE)
        self.assertEqual(-6.1, parsed.preamp_db)
        self.assertEqual(3, len(parsed.filters))
        self.assertEqual("low_shelf", parsed.filters[0]["type"])
        self.assertEqual("peak", parsed.filters[1]["type"])
        self.assertEqual("high_shelf", parsed.filters[2]["type"])

    def test_fingerprint_ignores_filter_order(self):
        first = parse_parametric_eq(SAMPLE)
        second = type(first)(preamp_db=first.preamp_db, filters=list(reversed(first.filters)))
        self.assertEqual(acoustic_fingerprint(first), acoustic_fingerprint(second))

    def test_builds_measurement_derived_provenance(self):
        candidate = build_candidate(
            parse_parametric_eq(SAMPLE),
            manufacturer="Sennheiser",
            model="HD 650",
            measurement_source="oratory1990",
            target="AutoEq over-ear target",
            source_url="https://github.com/jaakkopasanen/AutoEq",
            source_record_id="results/oratory1990/over-ear/Sennheiser HD 650",
            source_version="abc123",
            discovered_at_epoch_seconds=1788020000,
        )
        revision = candidate["revisions"][0]
        source = revision["source_references"][0]
        self.assertEqual("AutoEq", candidate["creator"])
        self.assertEqual("measurement_derived", source["source_kind"])
        self.assertEqual("structured-data-only", source["redistribution_policy"])
        self.assertEqual("explicit_target", candidate["target"]["kind"])
        self.assertTrue(revision["is_latest"])
        self.assertEqual(3, len(revision["filters"]))

    def test_preserves_unknown_target_instead_of_guessing(self):
        candidate = build_candidate(
            parse_parametric_eq(SAMPLE),
            manufacturer="HIFIMAN",
            model="Edition XS",
            measurement_source="oratory1990",
            target=None,
            source_url="https://github.com/jaakkopasanen/AutoEq",
            source_record_id="results/oratory1990/over-ear/HIFIMAN Edition XS",
            source_version="abc123",
            discovered_at_epoch_seconds=1788020000,
        )
        self.assertIsNone(candidate["target"]["name"])
        self.assertEqual("unknown", candidate["target"]["kind"])
        self.assertEqual("AutoEq (oratory1990 measurement)", candidate["tuning_label"])

    def test_rejects_unknown_lines(self):
        with self.assertRaises(ValueError):
            parse_parametric_eq("Preamp: -1 dB\nUnexpected: value\n")


if __name__ == "__main__":
    unittest.main()
