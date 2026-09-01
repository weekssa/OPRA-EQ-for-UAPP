import unittest

from general_preset_publish import publish_manifest


class GeneralPresetPublishTest(unittest.TestCase):
    def registry(self):
        return {
            "schema_version": 1,
            "registry_version": "test-general",
            "sources": [
                {
                    "id": "example-general",
                    "kind": "community_repository",
                    "lifecycle": "active",
                    "redistribution": "structured-data-only",
                }
            ],
        }

    def manifest(self):
        return {
            "schema_version": 1,
            "source_id": "example-general",
            "source_kind": "community_repository",
            "creator": "Example Creator",
            "source_url": "https://example.test/source",
            "source_version": "abc123",
            "verification_status": "verified",
            "discovered_at_epoch_seconds": 1788220800,
            "catalog_generated_at": "2026-09-01T03:00:00Z",
            "presets": [
                {
                    "source_record_id": "sound",
                    "purpose": "effect",
                    "tuning_label": "Bass Boost",
                    "peq_text": "Filter 1: ON LS Fc 80 Hz Gain 4.0 dB Q 0.7\nFilter 2: ON PK Fc 200 Hz Gain 1.0 dB Q 1.0",
                },
                {
                    "source_record_id": "genre",
                    "purpose": "genre",
                    "tuning_label": "Rock",
                    "sound_impact_summary": "Source-authored genre intent.",
                    "peq_text": "Filter 1: ON PK Fc 1000 Hz Gain 2.0 dB Q 1.0",
                },
            ],
        }

    def snapshot(self):
        return {
            "schema_version": 1,
            "generated_at": "2026-08-31T00:00:00Z",
            "source_registry_version": "old",
            "profiles": [],
        }

    def test_publishes_sound_and_genre_without_fake_headphones(self):
        merged, outcomes = publish_manifest(self.snapshot(), self.manifest(), self.registry())
        self.assertEqual(2, outcomes["new_profile"])
        self.assertEqual({"effect", "genre"}, {profile["purpose"] for profile in merged["profiles"]})
        self.assertTrue(all(profile["scope"] == "general" for profile in merged["profiles"]))
        self.assertTrue(all(profile["headphone"] is None for profile in merged["profiles"]))
        sound = next(profile for profile in merged["profiles"] if profile["purpose"] == "effect")
        revision = sound["revisions"][0]
        self.assertIsNone(revision["preamp_gain_db"])
        self.assertLess(revision["eq_library_safety_headroom_db"], 0.0)
        genre = next(profile for profile in merged["profiles"] if profile["purpose"] == "genre")
        self.assertEqual("Source-authored genre intent.", genre["revisions"][0]["sound_impact_summary"])
        self.assertEqual("test-general", merged["source_registry_version"])

    def test_rejects_non_publishable_registry_source(self):
        registry = self.registry()
        registry["sources"][0]["redistribution"] = "review-required"
        with self.assertRaisesRegex(ValueError, "not publication-qualified"):
            publish_manifest(self.snapshot(), self.manifest(), registry)


if __name__ == "__main__":
    unittest.main()
