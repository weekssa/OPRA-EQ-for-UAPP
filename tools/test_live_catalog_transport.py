import json
import unittest

from live_catalog_transport import render_transport


def valid_snapshot():
    return {
        "schema_version": 1,
        "generated_at": "2026-09-07T00:00:00Z",
        "source_registry_version": "test-1",
        "headphone_aliases": [],
        "profiles": [],
    }


class LiveCatalogTransportTest(unittest.TestCase):
    def test_valid_snapshot_renders_deterministic_compact_json(self):
        snapshot = valid_snapshot()
        rendered = render_transport(snapshot)
        self.assertEqual(
            json.dumps(snapshot, sort_keys=True, separators=(",", ":"), ensure_ascii=False) + "\n",
            rendered,
        )
        self.assertNotIn("\n  ", rendered)

    def test_invalid_snapshot_is_rejected(self):
        snapshot = valid_snapshot()
        snapshot.pop("generated_at")
        with self.assertRaisesRegex(ValueError, "live catalog snapshot is invalid"):
            render_transport(snapshot)


if __name__ == "__main__":
    unittest.main()
