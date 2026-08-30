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
            {"type": "product", "id": "a", "data": {"vendor_id": "sen", "name": "HD 650"}},
            {"type": "product", "id": "b", "data": {"vendor_id": "sen", "name": "HD650"}},
        )
        _, products = parse_opra(stream)
        report = audit_products(products, [])
        self.assertEqual(report["auto_safe_exact_group_count"], 1)
        self.assertEqual(report["review_candidate_count"], 0)

    def test_broader_alias_is_review_only_until_explicitly_covered(self):
        stream = jsonl(
            {"type": "vendor", "id": "7hz", "data": {"name": "7Hz"}},
            {"type": "product", "id": "a", "data": {"vendor_id": "7hz", "name": "Zero 2"}},
            {"type": "product", "id": "b", "data": {"vendor_id": "7hz", "name": "x Crinacle Zero 2"}},
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
