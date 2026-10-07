#!/usr/bin/env python3
"""Fixture tests for fail-closed resumed-activity evidence."""

import sys
import unittest
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))

from app_activity_checkpoint import ActivityCheckpointError, extract_activity


PACKAGE = "com.weekssa.opraeqforuapp"


class AppActivityCheckpointTest(unittest.TestCase):
    def test_prefers_single_top_resumed_record_and_checks_app(self):
        text = """mResumedActivity: ActivityRecord{a u0 com.android.launcher/.Launcher t1}
topResumedActivity=ActivityRecord{b u0 com.weekssa.opraeqforuapp/.MainActivity t2}
"""
        self.assertEqual(
            extract_activity(text, PACKAGE, expect_present=True),
            "topResumedActivity=ActivityRecord{b u0 com.weekssa.opraeqforuapp/.MainActivity t2}",
        )

    def test_falls_back_to_single_m_resumed_record(self):
        text = "mResumedActivity: ActivityRecord{b u0 com.weekssa.opraeqforuapp/.MainActivity t2}\n"
        self.assertIn("mResumedActivity=", extract_activity(text, PACKAGE, expect_present=True))

    def test_accepts_android_17_resumed_activity_record(self):
        text = """Resumed activities in task display areas from top to bottom:
 ResumedActivity: ActivityRecord{b u0 com.android.settings/.Settings t2}
"""
        self.assertEqual(
            extract_activity(text, PACKAGE, expect_present=False),
            "ResumedActivity=ActivityRecord{b u0 com.android.settings/.Settings t2}",
        )

    def test_top_resumed_record_takes_precedence_over_platform_and_legacy_records(self):
        text = """mResumedActivity: ActivityRecord{a u0 com.android.launcher/.Launcher t1}
ResumedActivity: ActivityRecord{b u0 com.android.launcher/.Launcher t2}
topResumedActivity=ActivityRecord{c u0 com.android.launcher/.Launcher t3}
"""
        self.assertEqual(
            extract_activity(text, PACKAGE, expect_present=False),
            "topResumedActivity=ActivityRecord{c u0 com.android.launcher/.Launcher t3}",
        )

    def test_unrelated_activity_proves_app_absent(self):
        text = "topResumedActivity=ActivityRecord{a u0 com.android.launcher/.Launcher t1}\n"
        self.assertIn("com.android.launcher", extract_activity(text, PACKAGE, expect_present=False))

    def test_missing_or_ambiguous_top_record_fails_closed(self):
        with self.assertRaises(ActivityCheckpointError):
            extract_activity("", PACKAGE, expect_present=True)
        with self.assertRaises(ActivityCheckpointError):
            extract_activity(
                "topResumedActivity=ActivityRecord{a u0 com.foo/.A t1}\n"
                "topResumedActivity=ActivityRecord{b u0 com.foo/.B t2}\n",
                PACKAGE,
                expect_present=False,
            )

    def test_expectation_mismatch_fails_closed(self):
        with self.assertRaises(ActivityCheckpointError):
            extract_activity("topResumedActivity=ActivityRecord{a u0 com.foo/.A t1}\n", PACKAGE, True)
        with self.assertRaises(ActivityCheckpointError):
            extract_activity(
                "topResumedActivity=ActivityRecord{a u0 com.weekssa.opraeqforuapp.child/.MainActivity t1}\n",
                PACKAGE,
                True,
            )
        with self.assertRaises(ActivityCheckpointError):
            extract_activity(
                "topResumedActivity=ActivityRecord{a u0 com.weekssa.opraeqforuapp/.MainActivity t1}\n",
                PACKAGE,
                False,
            )

    def test_malformed_nonempty_activity_record_fails_closed(self):
        for value in (
            "ResumedActivity: unavailable\n",
            "ResumedActivity: ActivityRecord{a u0 com.android.launcher/.Launcher t1\n",
            "ResumedActivity: ActivityRecord{}\n",
            "ResumedActivity: ActivityRecord{garbage}\n",
            "ResumedActivity: ActivityRecord{a u0 invalid-package/.Launcher t1}\n",
            "ResumedActivity: ActivityRecord{a u0 com.android.launcher/.Launcher t1}}\n",
        ):
            with self.subTest(value=value), self.assertRaises(ActivityCheckpointError):
                extract_activity(value, PACKAGE, expect_present=False)


if __name__ == "__main__":
    unittest.main()
