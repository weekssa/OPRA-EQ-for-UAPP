import tempfile
import unittest
from pathlib import Path
from unittest.mock import patch

from qualified_github_ingest import extract_fenced_peq, refresh


README = """
# Target

**7Hz Zero:2 to ISO 226:2023 EQ Profile**
```text
Preamp: -7.2 dB
Filter 1: ON PK Fc 31.82 Hz Gain -5.27 dB Q 0.423
Filter 2: ON PK Fc 89.79 Hz Gain -9.39 dB Q 0.272
Filter 3: ON PK Fc 935.12 Hz Gain 7.40 dB Q 0.960
Filter 4: ON PK Fc 991.83 Hz Gain -6.31 dB Q 0.625
Filter 5: ON PK Fc 999.09 Hz Gain 1.89 dB Q 5.000
Filter 6: ON PK Fc 1487.42 Hz Gain -7.54 dB Q 1.081
Filter 7: ON PK Fc 2815.58 Hz Gain 3.55 dB Q 0.260
Filter 8: ON PK Fc 8035.74 Hz Gain -15.80 dB Q 1.825
Filter 9: ON PK Fc 11109.70 Hz Gain 5.09 dB Q 2.960
Filter 10: ON PK Fc 16000.00 Hz Gain 7.40 dB Q 1.398
```
"""


class QualifiedGithubIngestTest(unittest.TestCase):
    def _registry(self):
        return {
            "schema_version": 1,
            "registry_version": "test-registry",
            "sources": [
                {
                    "id": "mrchillstorm-headphone-target",
                    "kind": "community_repository",
                    "name": "Headphone Target",
                    "scope": "test",
                    "lifecycle": "active",
                    "cadence": "weekly",
                    "parser": "github-qualified-fenced-peq",
                    "parser_version": "1",
                    "cursor_strategy": "blob sha",
                    "redistribution": "structured-data-only",
                    "attribution_required": True,
                }
            ],
        }

    def _manifest(self):
        return {
            "schema_version": 1,
            "sources": [
                {
                    "id": "mrchillstorm-headphone-target",
                    "repository": "MrChillStorm/Headphone_Target",
                    "branch": "main",
                    "license_spdx": "MIT",
                    "creator": "MrChillStorm",
                    "profiles": [
                        {
                            "source_path": "README.md",
                            "marker": "7Hz Zero:2 to ISO 226:2023 EQ Profile",
                            "manufacturer": "7Hz",
                            "model": "Zero:2",
                            "variant": None,
                            "tuning_label": "ISO 226:2023 85 phon",
                            "target": "ISO 226:2023 85 phon (author-defined)",
                            "source_url": "https://github.com/MrChillStorm/Headphone_Target#iem-compatibility",
                            "source_record_id": "repo:zero2",
                        }
                    ],
                }
            ],
        }

    def _catalog(self):
        return {
            "schema_version": 1,
            "generated_at": "2026-08-29T00:00:00Z",
            "source_registry_version": "old",
            "profiles": [],
            "sources": [],
        }

    def test_extracts_only_structured_block(self):
        block = extract_fenced_peq(README, "7Hz Zero:2 to ISO 226:2023 EQ Profile")
        self.assertTrue(block.startswith("Preamp: -7.2 dB"))
        self.assertIn("Filter 10:", block)
        self.assertNotIn("# Target", block)

    @patch("qualified_github_ingest.github_contents", return_value=(README, "abc123"))
    def test_qualified_source_adds_publishable_profile(self, _fetch):
        with tempfile.TemporaryDirectory() as tmp:
            health = Path(tmp) / "health.json"
            catalog, health_payload, outcomes = refresh(
                self._catalog(),
                self._registry(),
                health,
                self._manifest(),
                now_epoch=1770000000,
            )
        self.assertEqual("ok", outcomes[0]["status"])
        self.assertEqual("new_profile", outcomes[0]["outcomes"][0])
        self.assertEqual(1, len(catalog["profiles"]))
        profile = catalog["profiles"][0]
        self.assertEqual("7Hz", profile["headphone"]["manufacturer"])
        self.assertEqual("Zero:2", profile["headphone"]["model"])
        self.assertEqual("MrChillStorm", profile["creator"])
        self.assertEqual(10, len(profile["revisions"][0]["filters"]))
        ref = profile["revisions"][0]["source_references"][0]
        self.assertEqual("mrchillstorm-headphone-target", ref["source_id"])
        self.assertEqual("repository", ref["source_kind"])
        self.assertEqual("structured-data-only", ref["redistribution_policy"])
        self.assertEqual("active", next(item for item in health_payload["sources"] if item["source_id"] == ref["source_id"])["lifecycle"])

    @patch("qualified_github_ingest.github_contents", return_value=("# no matching block", "badsha"))
    def test_source_failure_preserves_catalog_and_degrades_health(self, _fetch):
        original = self._catalog()
        with tempfile.TemporaryDirectory() as tmp:
            health = Path(tmp) / "health.json"
            catalog, health_payload, outcomes = refresh(
                original,
                self._registry(),
                health,
                self._manifest(),
                now_epoch=1770000000,
            )
        self.assertEqual(original["profiles"], catalog["profiles"])
        self.assertEqual("degraded", outcomes[0]["status"])
        state = next(item for item in health_payload["sources"] if item["source_id"] == "mrchillstorm-headphone-target")
        self.assertEqual(1, state["consecutive_failures"])


if __name__ == "__main__":
    unittest.main()
