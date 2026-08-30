import json
import unittest
from pathlib import Path

from curated_community_publish import (
    ELIGIBLE_STATUSES,
    REFERENCE_STATUSES,
    SOURCE_ID,
    curated_headphone_identity,
    normalized_filters,
)


ROOT = Path(__file__).resolve().parents[1]
DISCOVERY = ROOT / "catalog" / "discovery"


class CuratedCommunityInputsTest(unittest.TestCase):
    def curated_files(self):
        return sorted(DISCOVERY.glob("*_community_curated.json"))

    def test_curated_inputs_cover_multiple_headphones(self):
        identities = set()
        manufacturers = set()
        files = self.curated_files()
        self.assertGreaterEqual(len(files), 2)

        for path in files:
            payload = json.loads(path.read_text(encoding="utf-8"))
            manufacturer, model, variant = curated_headphone_identity(payload)
            identities.add((manufacturer.casefold(), model.casefold(), (variant or "").casefold()))
            manufacturers.add(manufacturer.casefold())

        self.assertGreaterEqual(len(identities), 2)
        self.assertGreaterEqual(len(manufacturers), 2)

    def test_curated_records_are_structurally_reviewable_without_device_band_limit(self):
        saw_more_than_ten_filters = False
        for path in self.curated_files():
            payload = json.loads(path.read_text(encoding="utf-8"))
            record_ids = set()
            for record in payload.get("records") or []:
                record_id = str(record.get("id") or "").strip()
                self.assertTrue(record_id, path.name)
                self.assertNotIn(record_id, record_ids, path.name)
                record_ids.add(record_id)

                source_url = str(record.get("source_url") or "")
                self.assertTrue(source_url.startswith(("https://", "http://")), record_id)

                status = record.get("status")
                surface = str(record.get("surface") or "")
                if status in ELIGIBLE_STATUSES and surface in SOURCE_ID:
                    filters = normalized_filters(record)
                    self.assertEqual(len(record.get("filters") or []), len(filters), record_id)
                    if len(filters) > 10:
                        saw_more_than_ten_filters = True
                elif status in REFERENCE_STATUSES:
                    # Mirrors/reposts may be provenance-only. They intentionally attach a
                    # source reference to an existing canonical tuning without inventing or
                    # duplicating filter data.
                    self.assertIn(surface, SOURCE_ID, record_id)

                preamp = record.get("preamp_db")
                if preamp is not None:
                    self.assertIsInstance(preamp, (int, float), record_id)

        # The canonical community corpus itself proves that device limits such as UAPP's
        # ten-band target are not ingestion/storage limits.
        self.assertTrue(saw_more_than_ten_filters)


if __name__ == "__main__":
    unittest.main()