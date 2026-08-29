import copy
import unittest

from oratory_provenance import enrich_catalog


class OratoryProvenanceTest(unittest.TestCase):
    def _snapshot(self):
        return {
            "schema_version": 1,
            "generated_at": "2026-08-29T00:00:00Z",
            "source_registry_version": "test",
            "profiles": [
                {
                    "canonical_profile_id": "oratory-profile",
                    "creator": "oratory1990",
                    "headphone": {"manufacturer": "HIFIMAN", "model": "Edition XS"},
                    "target": {"name": "Harman", "kind": "explicit_target"},
                    "tuning_label": "Harman Target",
                    "revisions": [
                        {
                            "revision_id": "rev-a",
                            "acoustic_fingerprint": "abc",
                            "preamp_gain_db": -5.0,
                            "filters": [{"type": "peak", "frequency_hz": 1000.0, "gain_db": -2.0, "q": 1.0, "slope": None}],
                            "source_references": [
                                {
                                    "source_id": "opra",
                                    "source_kind": "structured_catalog",
                                    "source_record_id": "hifiman:edition-xs:oratory",
                                    "source_vendor_id": "hifiman",
                                    "source_product_id": "edition-xs",
                                    "url": "https://example.com/oratory.pdf",
                                    "creator": "oratory1990",
                                    "provenance_tier": "authoritative",
                                    "redistribution_policy": "structured-data-only",
                                    "is_primary": True,
                                }
                            ],
                            "is_latest": True,
                        }
                    ],
                },
                {
                    "canonical_profile_id": "autoeq-profile",
                    "creator": "AutoEq",
                    "headphone": {"manufacturer": "HIFIMAN", "model": "Edition XS"},
                    "target": {"name": None, "kind": "unknown"},
                    "tuning_label": "AutoEq (oratory1990 measurement)",
                    "revisions": [
                        {
                            "revision_id": "rev-b",
                            "acoustic_fingerprint": "def",
                            "preamp_gain_db": -4.0,
                            "filters": [{"type": "peak", "frequency_hz": 2000.0, "gain_db": 3.0, "q": 1.5, "slope": None}],
                            "source_references": [
                                {
                                    "source_id": "autoeq",
                                    "source_kind": "measurement_derived",
                                    "source_record_id": "results/oratory1990/example",
                                    "url": "https://github.com/jaakkopasanen/AutoEq/example",
                                    "creator": "AutoEq",
                                    "provenance_tier": "measurement_derived",
                                    "redistribution_policy": "structured-data-only",
                                    "is_primary": True,
                                }
                            ],
                            "is_latest": True,
                        }
                    ],
                },
            ],
            "sources": [],
        }

    def test_enriches_authored_profile_without_changing_filters(self):
        original = self._snapshot()
        expected_filters = copy.deepcopy(original["profiles"][0]["revisions"][0]["filters"])
        enriched, report = enrich_catalog(original)

        revision = enriched["profiles"][0]["revisions"][0]
        direct = [ref for ref in revision["source_references"] if ref["source_id"] == "oratory1990"]
        self.assertEqual(1, len(direct))
        self.assertEqual("creator", direct[0]["source_kind"])
        self.assertEqual("authoritative", direct[0]["provenance_tier"])
        self.assertEqual("link-only", direct[0]["redistribution_policy"])
        self.assertFalse(direct[0]["is_primary"])
        self.assertEqual(expected_filters, revision["filters"])
        self.assertEqual({"added_references": 1, "touched_revisions": 1}, report)

    def test_does_not_misattribute_autoeq_measurement_source(self):
        enriched, _ = enrich_catalog(self._snapshot())
        refs = enriched["profiles"][1]["revisions"][0]["source_references"]
        self.assertFalse(any(ref["source_id"] == "oratory1990" for ref in refs))

    def test_is_idempotent(self):
        once, _ = enrich_catalog(self._snapshot())
        twice, report = enrich_catalog(once)
        refs = twice["profiles"][0]["revisions"][0]["source_references"]
        self.assertEqual(1, sum(ref["source_id"] == "oratory1990" for ref in refs))
        self.assertEqual({"added_references": 0, "touched_revisions": 0}, report)


if __name__ == "__main__":
    unittest.main()
