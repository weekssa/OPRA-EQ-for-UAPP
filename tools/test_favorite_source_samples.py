"""Regression checks for the Favorite source sample coverage gate."""

from __future__ import annotations

import copy
import unittest
from pathlib import Path

from tools.verify_favorite_source_samples import (
    CoverageError,
    _read_json,
    build_expected_fixture,
    validate_fixture,
)


ROOT = Path(__file__).resolve().parents[1]


class FavoriteSourceSampleCoverageTest(unittest.TestCase):
    @classmethod
    def setUpClass(cls) -> None:
        cls.catalog = _read_json(ROOT / "catalog" / "catalog.json")
        cls.registry = _read_json(ROOT / "config" / "source_registry.json")
        cls.expected = build_expected_fixture(cls.catalog, cls.registry)

    def test_checked_in_fixture_matches_current_catalog(self) -> None:
        actual = _read_json(
            ROOT / "app" / "src" / "test" / "resources" / "catalog" / "favorite-source-samples.json"
        )
        validate_fixture(actual, self.expected)

    def test_missing_current_source_sample_fails(self) -> None:
        incomplete = copy.deepcopy(self.expected)
        incomplete["samples"].pop()

        with self.assertRaisesRegex(CoverageError, "missing samples"):
            validate_fixture(incomplete, self.expected)

    def test_changed_sample_profile_data_fails(self) -> None:
        stale = copy.deepcopy(self.expected)
        stale["snapshot"]["profiles"][0]["revisions"][0]["filters"][0]["gain_db"] += 0.1

        with self.assertRaisesRegex(CoverageError, "profile data is stale"):
            validate_fixture(stale, self.expected)

    def test_unregistered_catalog_source_id_fails_closed(self) -> None:
        candidate = copy.deepcopy(self.catalog)
        reference = copy.deepcopy(candidate["profiles"][0]["revisions"][0]["source_references"][0])
        reference["source_id"] = "future-unregistered-source"
        candidate["profiles"][0]["revisions"][0]["source_references"].append(reference)

        with self.assertRaisesRegex(CoverageError, "missing from the registry"):
            build_expected_fixture(candidate, self.registry)

    def test_new_registered_source_without_sample_needs_reviewed_exclusion(self) -> None:
        candidate_registry = copy.deepcopy(self.registry)
        candidate_registry["sources"].append(
            {"id": "future-registered-source", "kind": "community", "name": "Future source"}
        )

        with self.assertRaisesRegex(CoverageError, "reviewed exclusion"):
            build_expected_fixture(self.catalog, candidate_registry)


if __name__ == "__main__":
    unittest.main()
