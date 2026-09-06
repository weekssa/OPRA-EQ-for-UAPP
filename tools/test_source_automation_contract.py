import json
import unittest
from pathlib import Path


ROOT = Path(__file__).resolve().parents[1]


class SourceAutomationContractTest(unittest.TestCase):
    def test_repository_has_no_recurring_manual_currentness_owner(self):
        registry = json.loads((ROOT / "config/source_registry.json").read_text(encoding="utf-8"))
        manual = sorted(
            source["id"]
            for source in registry.get("sources", [])
            if source.get("currentness_mode") == "manual"
        )
        self.assertEqual([], manual)

    def test_non_automatable_sources_are_explicitly_paused_not_faked(self):
        registry = json.loads((ROOT / "config/source_registry.json").read_text(encoding="utf-8"))
        modes = {source["id"]: source.get("currentness_mode") for source in registry.get("sources", [])}
        self.assertEqual("paused", modes["reddit-audio"])
        self.assertEqual("paused", modes["topping-community"])
        self.assertEqual("paused", modes["oratory1990"])

    def test_newly_automated_sources_have_real_scheduled_owners(self):
        registry = json.loads((ROOT / "config/source_registry.json").read_text(encoding="utf-8"))
        sources = {source["id"]: source for source in registry.get("sources", [])}
        for source_id in ("head-fi", "audio-science-review", "squiglink", "paraeq"):
            with self.subTest(source_id=source_id):
                self.assertEqual("active", sources[source_id]["lifecycle"])
                self.assertEqual("scheduled", sources[source_id]["currentness_mode"])
                self.assertNotEqual("manual", sources[source_id]["cadence"])


if __name__ == "__main__":
    unittest.main()
