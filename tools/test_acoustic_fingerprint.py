import unittest

from acoustic_fingerprint import acoustic_fingerprint


class AcousticFingerprintTest(unittest.TestCase):
    def test_matches_android_reference_vector(self):
        filters = [
            {"type": "low_shelf", "frequency_hz": 105, "gain_db": 5.8, "q": 0.70, "slope": None},
            {"type": "peak", "frequency_hz": 87, "gain_db": -2.8, "q": 0.33, "slope": None},
            {"type": "peak", "frequency_hz": 1896, "gain_db": 4.0, "q": 1.92, "slope": None},
            {"type": "peak", "frequency_hz": 2896, "gain_db": -3.7, "q": 3.11, "slope": None},
            {"type": "peak", "frequency_hz": 1420, "gain_db": 1.3, "q": 3.89, "slope": None},
            {"type": "high_shelf", "frequency_hz": 10000, "gain_db": -5.8, "q": 0.70, "slope": None},
            {"type": "peak", "frequency_hz": 5585, "gain_db": 1.7, "q": 6.00, "slope": None},
            {"type": "peak", "frequency_hz": 70, "gain_db": -0.4, "q": 2.12, "slope": None},
            {"type": "peak", "frequency_hz": 111, "gain_db": 0.4, "q": 1.64, "slope": None},
            {"type": "peak", "frequency_hz": 927, "gain_db": -0.9, "q": 6.00, "slope": None},
        ]
        self.assertEqual(
            "5af24a7cd10bfbc5d5d7eb1cc29cc86712a8a2d72e40c7fec616bdd4199d074a",
            acoustic_fingerprint(-4.8, filters),
        )

    def test_ignores_filter_order(self):
        filters = [
            {"type": "peak", "frequency_hz": 1000, "gain_db": 2, "q": 1.0, "slope": None},
            {"type": "low_shelf", "frequency_hz": 100, "gain_db": 3, "q": 0.7, "slope": None},
        ]
        self.assertEqual(
            acoustic_fingerprint(-3.0, filters),
            acoustic_fingerprint(-3.0, list(reversed(filters))),
        )


if __name__ == "__main__":
    unittest.main()
