import unittest

from headphone_identity_decisions import apply_decisions, validate_decisions


class HeadphoneIdentityDecisionsTest(unittest.TestCase):
    def test_valid_decisions_apply_catalog_aliases(self):
        decisions = {
            "schema_version": 1,
            "aliases": [{
                "manufacturer": "Maker",
                "canonical_model": "Model One",
                "aliases": ["Model 1"],
                "evidence": ["https://example.test/model"],
            }],
            "distinct_pairs": [{
                "manufacturer": "Maker",
                "left_model": "Model Pro",
                "right_model": "Studio Pro",
                "evidence": ["https://example.test/distinct"],
            }],
        }
        snapshot = {
            "schema_version": 1,
            "generated_at": "2026-08-30T00:00:00Z",
            "source_registry_version": "test",
            "profiles": [{
                "canonical_profile_id": "profile-1",
                "headphone": {"manufacturer": "Maker", "model": "Model One"},
                "creator": "Creator",
                "target": {"kind": "unknown", "name": None},
                "tuning_label": "Test",
                "revisions": [{
                    "revision_id": "rev-1",
                    "acoustic_fingerprint": "abc",
                    "filters": [{"type": "peak", "frequency_hz": 1000.0, "gain_db": 1.0, "q": 1.0, "slope": None}],
                    "is_latest": True,
                    "source_references": [{"source_id": "test"}],
                }],
            }],
        }
        self.assertEqual(validate_decisions(decisions), [])
        result = apply_decisions(snapshot, decisions)
        self.assertEqual(len(result["headphone_aliases"]), 1)
        self.assertEqual(result["headphone_aliases"][0]["canonical_model"], "Model One")
        self.assertEqual(result["headphone_aliases"][0]["aliases"], ["Model 1"])

    def test_alias_requires_evidence(self):
        decisions = {
            "schema_version": 1,
            "aliases": [{
                "manufacturer": "Maker",
                "canonical_model": "Model One",
                "aliases": ["Model 1"],
                "evidence": [],
            }],
            "distinct_pairs": [],
        }
        errors = validate_decisions(decisions)
        self.assertTrue(any("evidence" in error for error in errors))

    def test_distinct_pair_cannot_normalize_to_same_model(self):
        decisions = {
            "schema_version": 1,
            "aliases": [],
            "distinct_pairs": [{
                "manufacturer": "Maker",
                "left_model": "HD 650",
                "right_model": "HD650",
                "evidence": ["https://example.test"],
            }],
        }
        errors = validate_decisions(decisions)
        self.assertTrue(any("same normalized model" in error for error in errors))


if __name__ == "__main__":
    unittest.main()
