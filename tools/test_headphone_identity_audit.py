import io
import json
import unittest

from headphone_identity_audit import audit_products, parse_opra


def jsonl(*records):
    return io.StringIO("\n".join(json.dumps(record) for record in records) + "\n")


class HeadphoneIdentityAuditTest(unittest.TestCase):
    def test_exact_normalized_duplicates_are_auto_safe(self):
        stream = jsonl(
            {"type": "vendor", "id": "sen", "data": {"name": "Sennheiser"}},
            {"type": "product", "id": "a", "data": {"vendor_id": "sen", "name": "HD 650", "subtype": "over-ear"}},
            {"type": "product", "id": "b", "data": {"vendor_id": "sen", "name": "HD650", "subtype": "over-ear"}},
        )
        _, products = parse_opra(stream)
        report = audit_products(products, [])
        self.assertEqual(report["auto_safe_group_count"], 1)
        self.assertEqual(report["review_candidate_count"], 0)

    def test_repeated_manufacturer_in_model_is_auto_safe(self):
        stream = jsonl(
            {"type": "vendor", "id": "kz", "data": {"name": "KZ"}},
            {"type": "product", "id": "a", "data": {"vendor_id": "kz", "name": "KZ x Crinacle CRN", "subtype": "in-ear"}},
            {"type": "product", "id": "b", "data": {"vendor_id": "kz", "name": "x Crinacle CRN", "subtype": "in-ear"}},
        )
        _, products = parse_opra(stream)
        report = audit_products(products, [])
        self.assertEqual(report["auto_safe_group_count"], 1)
        self.assertEqual(report["auto_safe_groups"][0]["reason"], "manufacturer token repeated in one model label")

    def test_same_name_with_different_subtypes_is_not_auto_merged(self):
        stream = jsonl(
            {"type": "vendor", "id": "maker", "data": {"name": "Maker"}},
            {"type": "product", "id": "a", "data": {"vendor_id": "maker", "name": "Model 1", "subtype": "over-ear"}},
            {"type": "product", "id": "b", "data": {"vendor_id": "maker", "name": "Model1", "subtype": "in-ear"}},
        )
        _, products = parse_opra(stream)
        report = audit_products(products, [])
        self.assertEqual(report["auto_safe_group_count"], 0)

    def test_broader_alias_is_review_only_until_explicitly_covered(self):
        stream = jsonl(
            {"type": "vendor", "id": "7hz", "data": {"name": "7Hz"}},
            {"type": "product", "id": "a", "data": {"vendor_id": "7hz", "name": "Zero 2", "subtype": "in-ear"}},
            {"type": "product", "id": "b", "data": {"vendor_id": "7hz", "name": "x Crinacle Zero 2", "subtype": "in-ear"}},
        )
        _, products = parse_opra(stream)
        report = audit_products(products, [])
        self.assertEqual(report["review_candidate_count"], 1)

        aliases = [{
            "manufacturer": "7Hz",
            "canonical_model": "Zero:2",
            "aliases": ["Zero 2", "x Crinacle Zero 2"],
            "source": "test",
        }]
        covered = audit_products(products, aliases)
        self.assertEqual(covered["review_candidate_count"], 0)
        self.assertEqual(covered["covered_explicit_alias_pair_count"], 1)

    def test_trailing_variant_is_not_treated_as_alias_candidate(self):
        stream = jsonl(
            {"type": "vendor", "id": "hifiman", "data": {"name": "HIFIMAN"}},
            {"type": "product", "id": "a", "data": {"vendor_id": "hifiman", "name": "HE1000"}},
            {"type": "product", "id": "b", "data": {"vendor_id": "hifiman", "name": "HE1000 Stealth"}},
        )
        _, products = parse_opra(stream)
        report = audit_products(products, [])
        self.assertEqual(report["review_candidate_count"], 0)

    def test_different_model_numbers_are_not_near_duplicate_candidates(self):
        stream = jsonl(
            {"type": "vendor", "id": "sen", "data": {"name": "Sennheiser"}},
            {"type": "product", "id": "a", "data": {"vendor_id": "sen", "name": "HD 600"}},
            {"type": "product", "id": "b", "data": {"vendor_id": "sen", "name": "HD 650"}},
        )
        _, products = parse_opra(stream)
        report = audit_products(products, [])
        self.assertEqual(report["review_candidate_count"], 0)


if __name__ == "__main__":
    unittest.main()
