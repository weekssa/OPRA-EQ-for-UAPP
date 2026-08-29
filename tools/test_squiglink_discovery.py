import unittest

from squiglink_discovery import discover_phone_book, stable_candidate_id


class SquiglinkDiscoveryTest(unittest.TestCase):
    def test_discovers_valid_phone_book_records_as_review_only_candidates(self):
        payload = [
            {
                "name": "HIFIMAN",
                "phones": [
                    {"name": "Edition XS", "file": "HIFIMAN Edition XS"},
                    {"name": "Ananda", "file": "HIFIMAN Ananda"},
                ],
            }
        ]

        candidates = discover_phone_book(
            payload,
            source_id="squig-example",
            source_name="Example Squiglink",
            base_url="https://example.squig.link",
            creator="Example Measurer",
            source_revision="rev-1",
        )

        self.assertEqual(2, len(candidates))
        edition = next(item for item in candidates if item["model"] == "Edition XS")
        self.assertEqual("structured_measurement", edition["source_kind"])
        self.assertEqual("Example Measurer", edition["creator"])
        self.assertEqual("review-required", edition["redistribution"])
        self.assertFalse(edition["publication_eligible"])
        self.assertTrue(edition["license_review_required"])
        self.assertIn("database_or_creator_terms", edition["qualification_required"])
        self.assertEqual("https://example.squig.link/data/HIFIMAN%20Edition%20XS.txt", edition["url"])

    def test_skips_malformed_brand_and_phone_records(self):
        payload = [
            None,
            {"name": "", "phones": [{"name": "Ignored", "file": "Ignored"}]},
            {
                "name": "Valid",
                "phones": [
                    None,
                    {"name": "Missing file"},
                    {"file": "Missing model"},
                    {"name": "Good", "file": "Valid Good"},
                ],
            },
        ]

        candidates = discover_phone_book(
            payload,
            source_id="source",
            source_name="Source",
            base_url="https://example.com",
        )

        self.assertEqual(1, len(candidates))
        self.assertEqual("Good", candidates[0]["model"])

    def test_ids_are_deterministic_and_duplicate_records_collapse(self):
        phone = {"name": "Edition XS", "file": "HIFIMAN Edition XS"}
        payload = [{"name": "HIFIMAN", "phones": [phone, phone]}]

        first = discover_phone_book(
            payload,
            source_id="source",
            source_name="Source",
            base_url="https://example.com",
        )
        second = discover_phone_book(
            payload,
            source_id="source",
            source_name="Source",
            base_url="https://example.com",
        )

        self.assertEqual(1, len(first))
        self.assertEqual(first, second)
        self.assertEqual(
            stable_candidate_id("source", "HIFIMAN", "Edition XS", "HIFIMAN Edition XS"),
            first[0]["candidate_id"],
        )

    def test_rejects_non_http_source_base(self):
        with self.assertRaisesRegex(ValueError, "http"):
            discover_phone_book(
                [],
                source_id="source",
                source_name="Source",
                base_url="file:///tmp/data",
            )


if __name__ == "__main__":
    unittest.main()
