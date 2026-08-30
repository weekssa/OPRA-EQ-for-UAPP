import unittest

from curated_community_publish import (
    attach_mirror_reference,
    build_curated_candidate,
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

    def test_missing_source_preamp_stays_null_and_safety_headroom_is_separate(self):
        filters = [
            {"type": "PK", "frequency_hz": 100.0 + index * 100.0, "gain_db": 2.0, "q": 1.0}
            for index in range(15)
        ]
        candidate, diagnostics = build_curated_candidate(
            {
                "id": "hd650-no-preamp",
                "creator": "example-user",
                "source_url": "https://example.invalid/no-preamp",
                "filters": filters,
                "preamp_db": None,
            },
            manufacturer="Sennheiser",
            model="HD 650",
            variant=None,
            source_id="reddit-audio",
        )

        revision = candidate["revisions"][0]
        self.assertIsNone(revision["preamp_gain_db"])
        self.assertEqual(15, len(revision["filters"]))
        self.assertIsNotNone(revision["eq_library_safety_headroom_db"])
        self.assertLessEqual(revision["eq_library_safety_headroom_db"], 0.0)
        self.assertEqual("eq-library-safe-headroom", diagnostics["preamp_origin"])
        self.assertIsNone(diagnostics["source_preamp_db"])
        self.assertIn("Source omitted preamp", revision["sound_impact_summary"])
        self.assertIn("safety headroom separately", revision["sound_impact_summary"])

    def test_explicit_source_preamp_is_preserved_without_generated_headroom(self):
        candidate, diagnostics = build_curated_candidate(
            {
                "id": "hd650-source-preamp",
                "creator": "example-user",
                "source_url": "https://example.invalid/source-preamp",
                "filters": [
                    {"type": "PK", "frequency_hz": 1000.0, "gain_db": 2.5, "q": 1.0}
                ],
                "preamp_db": -4.25,
            },
            manufacturer="Sennheiser",
            model="HD 650",
            variant=None,
            source_id="reddit-audio",
        )

        revision = candidate["revisions"][0]
        self.assertEqual(-4.25, revision["preamp_gain_db"])
        self.assertIsNone(revision["eq_library_safety_headroom_db"])
        self.assertEqual("source", diagnostics["preamp_origin"])
        self.assertEqual(-4.25, diagnostics["source_preamp_db"])
        self.assertNotIn("Source omitted preamp", revision["sound_impact_summary"])

    def test_named_tuning_and_explicit_target_keep_same_creator_alternatives_distinct(self):
        base = {
            "creator": "Bop",
            "source_url": "https://example.invalid/head-fi-post",
            "source_date": "2022-06-02",
            "preamp_db": -3.8,
            "filters": [
                {"type": "PK", "frequency_hz": 3300.0, "gain_db": 4.0, "q": 1.8}
            ],
        }
        neutral, neutral_diagnostics = build_curated_candidate(
            {
                **base,
                "id": "p1max-neutral",
                "tuning_label": "Crinacle Neutral Target",
                "target": "Crinacle Neutral Target",
            },
            manufacturer="TINHIFI",
            model="P1 MAX",
            variant=None,
            source_id="head-fi",
        )
        adjusted, adjusted_diagnostics = build_curated_candidate(
            {
                **base,
                "id": "p1max-adjusted",
                "tuning_label": "Crinacle Adjusted Target",
                "target": "Crinacle Adjusted Target",
            },
            manufacturer="TINHIFI",
            model="P1 MAX",
            variant=None,
            source_id="head-fi",
        )

        self.assertNotEqual(neutral["canonical_profile_id"], adjusted["canonical_profile_id"])
        self.assertEqual("Crinacle Neutral Target", neutral["tuning_label"])
        self.assertEqual("Crinacle Neutral Target", neutral["target"]["name"])
        self.assertEqual("explicit_target", neutral["target"]["kind"])
        self.assertEqual("Crinacle Neutral Target", neutral_diagnostics["tuning_label"])
        self.assertEqual("Crinacle Adjusted Target", adjusted_diagnostics["target"])

    def test_default_tuning_label_remains_creator_based_when_source_has_no_named_alternative(self):
        candidate, diagnostics = build_curated_candidate(
            {
                "id": "unnamed",
                "creator": "example-user",
                "source_url": "https://example.invalid/unnamed",
                "preamp_db": -1.0,
                "filters": [
                    {"type": "PK", "frequency_hz": 1000.0, "gain_db": 1.0, "q": 1.0}
                ],
            },
            manufacturer="Maker",
            model="Model",
            variant=None,
            source_id="reddit-audio",
        )

        self.assertEqual("example-user community tuning", candidate["tuning_label"])
        self.assertIsNone(candidate["target"]["name"])
        self.assertEqual("example-user community tuning", diagnostics["tuning_label"])
        self.assertIsNone(diagnostics["target"])

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