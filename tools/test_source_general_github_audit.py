import unittest

import general_github_candidate_audit


class GeneralGithubCandidateAuditTest(unittest.TestCase):
    def test_graphic_eq_without_q_is_processed_but_not_invented_as_peq(self):
        discovery = {
            "source_id": "github-community",
            "candidates": [
                {
                    "candidate_id": "general-1",
                    "repository": "example/eq",
                    "path": "presets.ts",
                    "url": "https://example.test/presets.ts",
                }
            ],
        }
        source = "const bands=[31,62,125]; const preset={name:'Bass Boost',gains:[6,4,2]};"
        report = general_github_candidate_audit.audit(
            discovery,
            github_token=None,
            fetcher=lambda item, token: source,
        )
        self.assertEqual(1, report["processed"])
        self.assertEqual(1, report["decision_counts"]["no_exact_parametric_structure"])

    def test_exact_peq_is_not_published_without_explicit_general_category(self):
        discovery = {
            "source_id": "github-community",
            "candidates": [{"candidate_id": "general-2", "path": "preset.txt"}],
        }
        source = "Preamp: -2 dB\nFilter 1: ON PK Fc 1000 Hz Gain 2 dB Q 1\n"
        report = general_github_candidate_audit.audit(
            discovery,
            github_token=None,
            fetcher=lambda item, token: source,
        )
        self.assertEqual(
            1,
            report["decision_counts"]["structured_peq_needs_explicit_general_category"],
        )


if __name__ == "__main__":
    unittest.main()
