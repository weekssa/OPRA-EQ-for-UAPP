import copy
import json
import unittest
from pathlib import Path

from priority_coverage import REQUIRED_PRIORITY, validate_coverage


COVERAGE_PATH = Path(__file__).resolve().parents[1] / "catalog" / "discovery" / "priority_headphone_coverage.json"


def load_coverage():
    return json.loads(COVERAGE_PATH.read_text(encoding="utf-8"))


class PriorityCoverageTest(unittest.TestCase):
    def test_repository_coverage_is_valid_and_contains_exact_priority_queue(self):
        payload = load_coverage()
        summary = validate_coverage(payload)
        identities = {
            (row["manufacturer"], row["model"])
            for row in payload["priority_headphones"]
        }
        self.assertEqual(REQUIRED_PRIORITY, identities)
        self.assertEqual(8, summary["headphones"])
        self.assertGreater(summary["searched_or_qualifying_rows"], 0)
        self.assertGreater(summary["candidates"], 0)

    def test_not_yet_searched_is_distinct_from_zero_result_search(self):
        payload = load_coverage()
        searched_zero = None
        not_searched = None
        for headphone in payload["priority_headphones"]:
            for source in headphone["sources"]:
                if source["state"] == "searched" and source["candidates_found"] == 0:
                    searched_zero = source
                if source["state"] == "not_yet_searched":
                    not_searched = source
        self.assertIsNotNone(searched_zero)
        self.assertIsNotNone(not_searched)
        self.assertNotEqual(searched_zero["state"], not_searched["state"])

    def test_rejects_candidate_counts_on_unsearched_source(self):
        payload = copy.deepcopy(load_coverage())
        row = payload["priority_headphones"][1]["sources"][2]
        self.assertEqual("not_yet_searched", row["state"])
        row["candidates_found"] = 1
        with self.assertRaisesRegex(ValueError, "not_yet_searched"):
            validate_coverage(payload)

    def test_rejects_missing_approved_priority_headphone(self):
        payload = copy.deepcopy(load_coverage())
        payload["priority_headphones"].pop()
        with self.assertRaisesRegex(ValueError, "missing approved targets"):
            validate_coverage(payload)

    def test_rejects_disposition_counts_above_candidate_count(self):
        payload = copy.deepcopy(load_coverage())
        row = payload["priority_headphones"][0]["sources"][0]
        row["held"] = row["candidates_found"] + 1
        with self.assertRaisesRegex(ValueError, "disposition counts exceed"):
            validate_coverage(payload)


if __name__ == "__main__":
    unittest.main()
