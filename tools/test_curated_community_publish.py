import unittest

from curated_community_publish import (
    attach_mirror_reference,
    curated_headphone_identity,
    headphone_revisions,
)


class CuratedCommunityPublishTest(unittest.TestCase):
    def test_curated_headphone_identity_is_data_driven(self):
        self.assertEqual(
            ("Sennheiser", "HD 650", "2024 pads"),
            curated_headphone_identity(
                {
                    "headphone": {
                        "manufacturer": " Sennheiser ",
                        "model": " HD 650 ",
                        "variant": " 2024 pads ",
                    }
                }
            ),
        )

    def test_curated_headphone_identity_requires_manufacturer_and_model(self):
        with self.assertRaises(ValueError):
            curated_headphone_identity({"headphone": {"manufacturer": "Sennheiser"}})

    def test_existing_revision_lookup_is_not_tied_to_edition_xs(self):
        hd650_filters = [
            {"type": "peak", "frequency_hz": 1000.0, "gain_db": -2.0, "q": 1.0}
        ]
        snapshot = {
            "profiles": [
                {
                    "canonical_profile_id": "hd650-community",
                    "headphone": {"manufacturer": "Sennheiser", "model": "HD 650"},
                    "revisions": [{"filters": hd650_filters}],
                },
                {
                    "canonical_profile_id": "edition-xs-community",
                    "headphone": {"manufacturer": "HIFIMAN", "model": "Edition XS"},
                    "revisions": [
                        {
                            "filters": [
                                {"type": "peak", "frequency_hz": 2000.0, "gain_db": 3.0, "q": 1.0}
                            ]
                        }
                    ],
                },
            ]
        }

        self.assertEqual(
            [("hd650-community", hd650_filters)],
            headphone_revisions(snapshot, "Sennheiser", "HD 650"),
        )

    def test_mirror_provenance_uses_curated_headphone_identity(self):
        snapshot = {
            "profiles": [
                {
                    "canonical_profile_id": "hd650-oratory",
                    "headphone": {"manufacturer": "Sennheiser", "model": "HD 650"},
                    "revisions": [
                        {
                            "filters": [
                                {"type": "peak", "frequency_hz": 1000.0, "gain_db": -2.0, "q": 1.0}
                            ],
                            "source_references": [
                                {
                                    "source_id": "oratory1990",
                                    "source_record_id": "original",
                                    "url": "https://example.invalid/original",
                                }
                            ],
                        }
                    ],
                }
            ]
        }
        record = {
            "id": "mirror-post",
            "lineage": "Forum mirror of oratory1990 preset",
            "source_url": "https://example.invalid/mirror",
            "creator": "forum-user",
        }

        profile_id = attach_mirror_reference(
            snapshot,
            record,
            "audio-science-review",
            "Sennheiser",
            "HD 650",
        )

        self.assertEqual("hd650-oratory", profile_id)
        refs = snapshot["profiles"][0]["revisions"][0]["source_references"]
        mirror = next(ref for ref in refs if ref["source_id"] == "audio-science-review")
        self.assertEqual("Sennheiser", mirror["source_vendor_id"])
        self.assertEqual("HD 650", mirror["source_product_id"])
        self.assertEqual("mirror", mirror["provenance_tier"])
        self.assertFalse(mirror["is_primary"])


if __name__ == "__main__":
    unittest.main()
