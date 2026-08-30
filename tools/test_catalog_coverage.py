import json
import unittest
from pathlib import Path

from catalog_coverage import build_report, validate_report


ROOT = Path(__file__).resolve().parents[1]


class CatalogCoverageTest(unittest.TestCase):
    def test_complete_catalog_requires_every_active_publishable_source_and_qualified_record(self):
        registry = {
            "sources": [
                {"id": "opra", "lifecycle": "active", "redistribution": "structured-data-only"},
                {"id": "autoeq", "lifecycle": "active", "redistribution": "structured-data-only"},
                {"id": "creator-links", "lifecycle": "link-only", "redistribution": "link-only"},
            ]
        }
        manifest = {
            "sources": [
                {"id": "opra", "profiles": [{"source_record_id": "opra:one"}]},
            ]
        }
        catalog = {
            "profiles": [
                {
                    "canonical_profile_id": "one",
                    "revisions": [
                        {
                            "revision_id": "r1",
                            "source_references": [
                                {"source_id": "opra", "source_record_id": "opra:one"},
                                {
                                    "source_id": "autoeq",
                                    "source_record_id": "results/oratory1990/over-ear/Test/Test ParametricEQ.txt",
                                },
                            ],
                        }
                    ],
                }
            ]
        }

        report = build_report(catalog, registry, manifest)
        self.assertTrue(report["complete"])
        self.assertEqual([], validate_report(report))
        self.assertEqual(1, report["autoeq_measurement_source_count"])
        self.assertEqual(1, report["autoeq_measurement_sources"]["oratory1990"])

    def test_missing_active_source_fails(self):
        registry = {
            "sources": [
                {"id": "opra", "lifecycle": "active", "redistribution": "structured-data-only"},
                {"id": "autoeq", "lifecycle": "active", "redistribution": "structured-data-only"},
            ]
        }
        catalog = {
            "profiles": [
                {
                    "canonical_profile_id": "one",
                    "revisions": [
                        {"revision_id": "r1", "source_references": [{"source_id": "opra", "source_record_id": "one"}]}
                    ],
                }
            ]
        }
        report = build_report(catalog, registry)
        errors = validate_report(report)
        self.assertFalse(report["complete"])
        self.assertTrue(any("autoeq" in error for error in errors))

    def test_missing_qualified_record_fails_even_when_source_is_present(self):
        registry = {
            "sources": [
                {"id": "community", "lifecycle": "active", "redistribution": "allowed"},
            ]
        }
        manifest = {
            "sources": [
                {
                    "id": "community",
                    "profiles": [
                        {"source_record_id": "repo:first"},
                        {"source_record_id": "repo:second"},
                    ],
                }
            ]
        }
        catalog = {
            "profiles": [
                {
                    "canonical_profile_id": "one",
                    "revisions": [
                        {"revision_id": "r1", "source_references": [{"source_id": "community", "source_record_id": "repo:first"}]}
                    ],
                }
            ]
        }
        report = build_report(catalog, registry, manifest)
        errors = validate_report(report)
        self.assertFalse(report["complete"])
        self.assertTrue(any("repo:second" in error for error in errors))

    def test_published_catalog_meets_current_qualified_coverage_contract(self):
        catalog = json.loads((ROOT / "catalog/catalog.json").read_text(encoding="utf-8"))
        registry = json.loads((ROOT / "config/source_registry.json").read_text(encoding="utf-8"))
        manifest = json.loads((ROOT / "config/qualified_github_sources.json").read_text(encoding="utf-8"))
        report = build_report(catalog, registry, manifest)
        self.assertEqual([], validate_report(report), json.dumps(report, sort_keys=True))
        self.assertTrue(report["complete"])


if __name__ == "__main__":
    unittest.main()
