#!/usr/bin/env python3
from __future__ import annotations

import copy
import json
import unittest
from pathlib import Path

from catalog_history_repair import repair_history

ROOT = Path(__file__).resolve().parents[1]
SEEDS = json.loads((ROOT / "config" / "immutable_revision_seeds.json").read_text(encoding="utf-8"))
PROFILE_ID = "autoeq-09f4d8d5d5288ccfdf6ddeeb"
CURRENT_FP = "5af24a7cd10bfbc5d5d7eb1cc29cc86712a8a2d72e40c7fec616bdd4199d074a"
HISTORICAL_VERSION = "AutoEq commit 853360a1626b387e1d3d87f3f7ad8c7514d30839"


def stripped_snapshot() -> dict:
    seeded_current = copy.deepcopy(SEEDS["profiles"][0]["revisions"][0])
    seeded_current["is_latest"] = True
    seeded_current["source_references"] = [
        source for source in seeded_current["source_references"] if source["source_id"] == "autoeq"
    ]
    return {
        "schema_version": 1,
        "generated_at": "2026-08-30T00:00:00Z",
        "source_registry_version": "test",
        "headphone_aliases": [],
        "profiles": [
            {
                "canonical_profile_id": PROFILE_ID,
                "creator": "AutoEq",
                "headphone": {
                    "manufacturer": "HIFIMAN",
                    "model": "Edition XS",
                    "pads_or_mode": None,
                    "variant": None,
                },
                "revisions": [seeded_current],
                "target": {"kind": "unknown", "name": None},
                "tuning_label": "AutoEq (oratory1990 measurement)",
            }
        ],
    }


class CatalogHistoryRepairTest(unittest.TestCase):
    def test_restores_revision_and_matching_mirror_without_changing_latest(self):
        repaired, report = repair_history(stripped_snapshot(), SEEDS)
        profile = repaired["profiles"][0]
        self.assertEqual(2, len(profile["revisions"]))
        latest = next(revision for revision in profile["revisions"] if revision["is_latest"])
        self.assertEqual(CURRENT_FP, latest["acoustic_fingerprint"])
        self.assertTrue(any(ref["provenance_tier"] == "mirror" for ref in latest["source_references"]))
        historical = next(
            revision for revision in profile["revisions"] if revision["source_version_label"] == HISTORICAL_VERSION
        )
        self.assertFalse(historical["is_latest"])
        self.assertEqual(1698572942, historical["source_updated_at_epoch_seconds"])
        self.assertEqual({"restored_revisions": 1, "provenance_updates": 1}, report)

    def test_repair_is_idempotent(self):
        once, _ = repair_history(stripped_snapshot(), SEEDS)
        twice, report = repair_history(once, SEEDS)
        self.assertEqual(once, twice)
        self.assertEqual({"restored_revisions": 0, "provenance_updates": 0}, report)


if __name__ == "__main__":
    unittest.main()
