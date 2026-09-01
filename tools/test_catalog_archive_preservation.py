#!/usr/bin/env python3
import unittest

from catalog_pipeline import archive_regression_errors


class ArchivePreservationTests(unittest.TestCase):
    def snapshot(self, *, include_profile=True, include_old_revision=True, old_fingerprint="fp-old"):
        revisions = []
        if include_old_revision:
            revisions.append({"revision_id": "r1", "acoustic_fingerprint": old_fingerprint})
        revisions.append({"revision_id": "r2", "acoustic_fingerprint": "fp-new"})
        profiles = []
        if include_profile:
            profiles.append({"canonical_profile_id": "profile-one", "revisions": revisions})
        return {"profiles": profiles}

    def test_missing_published_profile_is_rejected(self):
        errors = archive_regression_errors(self.snapshot(), self.snapshot(include_profile=False))
        self.assertIn("archived canonical profile disappeared: profile-one", errors)

    def test_missing_published_revision_is_rejected(self):
        errors = archive_regression_errors(self.snapshot(), self.snapshot(include_old_revision=False))
        self.assertIn("archived revision disappeared: profile-one/r1", errors)

    def test_published_revision_acoustics_cannot_change_in_place(self):
        errors = archive_regression_errors(self.snapshot(), self.snapshot(old_fingerprint="changed"))
        self.assertTrue(any("acoustic fingerprint changed in place" in error for error in errors))

    def test_metadata_only_candidate_preserves_archive(self):
        baseline = self.snapshot()
        candidate = self.snapshot()
        candidate["profiles"][0]["creator"] = "Updated attribution"
        self.assertEqual([], archive_regression_errors(baseline, candidate))


if __name__ == "__main__":
    unittest.main()
