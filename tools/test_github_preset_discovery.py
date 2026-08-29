import unittest

from github_preset_discovery import discover_code_search, discover_gists, is_structured_eq_path


class GitHubPresetDiscoveryTest(unittest.TestCase):
    def test_supported_paths_require_structured_eq_signal(self):
        self.assertTrue(is_structured_eq_path("HIFIMAN Edition XS ParametricEQ.txt"))
        self.assertTrue(is_structured_eq_path("presets/hd650_peq.json"))
        self.assertFalse(is_structured_eq_path("README.txt"))
        self.assertFalse(is_structured_eq_path("preset.png"))

    def test_code_search_results_become_review_candidates(self):
        payload = {
            "items": [
                {
                    "name": "HD 650 ParametricEQ.txt",
                    "path": "presets/HD 650 ParametricEQ.txt",
                    "sha": "abc123",
                    "html_url": "https://github.com/example/eq/blob/main/presets/HD%20650%20ParametricEQ.txt",
                    "repository": {
                        "full_name": "example/eq",
                        "owner": {"login": "example"},
                    },
                },
                {
                    "name": "README.md",
                    "path": "README.md",
                    "sha": "ignore",
                    "html_url": "https://github.com/example/eq/blob/main/README.md",
                    "repository": {"full_name": "example/eq", "owner": {"login": "example"}},
                },
            ]
        }

        candidates = discover_code_search(payload)

        self.assertEqual(1, len(candidates))
        candidate = candidates[0]
        self.assertEqual("github-community", candidate["source_id"])
        self.assertEqual("community_repository", candidate["source_kind"])
        self.assertEqual("example/eq", candidate["repository"])
        self.assertEqual("example", candidate["creator"])
        self.assertEqual("review-required", candidate["redistribution"])
        self.assertFalse(candidate["publication_eligible"])
        self.assertTrue(candidate["license_review_required"])
        self.assertIn("canonical_dedupe", candidate["qualification_required"])

    def test_gist_files_are_discovered_without_auto_publication(self):
        payload = [
            {
                "id": "gist123",
                "html_url": "https://gist.github.com/tester/gist123",
                "updated_at": "2026-08-29T12:00:00Z",
                "owner": {"login": "tester"},
                "files": {
                    "Edition XS PEQ.txt": {
                        "raw_url": "https://gist.githubusercontent.com/tester/gist123/raw/edition-xs.txt"
                    },
                    "notes.md": {"raw_url": "https://example.com/notes"},
                },
            }
        ]

        candidates = discover_gists(payload)

        self.assertEqual(1, len(candidates))
        candidate = candidates[0]
        self.assertEqual("github_gist", candidate["platform"])
        self.assertEqual("tester", candidate["creator"])
        self.assertEqual("gist:gist123:Edition XS PEQ.txt", candidate["source_record_id"])
        self.assertEqual("2026-08-29T12:00:00Z", candidate["source_updated_at"])
        self.assertFalse(candidate["publication_eligible"])

    def test_duplicate_search_results_collapse_to_one_candidate(self):
        item = {
            "name": "preset.txt",
            "path": "peq/preset.txt",
            "sha": "abc",
            "html_url": "https://github.com/example/eq/blob/main/peq/preset.txt",
            "repository": {"full_name": "example/eq", "owner": {"login": "example"}},
        }
        candidates = discover_code_search({"items": [item, item]})
        self.assertEqual(1, len(candidates))


if __name__ == "__main__":
    unittest.main()
