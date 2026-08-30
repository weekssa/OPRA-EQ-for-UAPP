import unittest

from eq_submission_issue import normalize_submission_event, parse_issue_form_fields


def event(body, *, number=42):
    return {
        "issue": {
            "number": number,
            "html_url": f"https://github.com/weekssa/OPRA-EQ-for-UAPP/issues/{number}",
            "title": "[EQ source] Example",
            "body": body,
            "user": {"login": "submitter"},
            "created_at": "2026-08-30T12:00:00Z",
            "updated_at": "2026-08-30T12:30:00Z",
        }
    }


def form_body(peq_text, *, preamble="", source_url="https://www.head-fi.org/threads/example.123/post-456", platform="Head-Fi"):
    return f"""{preamble}
### Manufacturer
Example Audio

### Exact model
Model One

### Variant / revision / pads / mode
_No response_

### EQ creator / username
CreatorName

### Original source URL
{source_url}

### Source platform
{platform}

### Target / curve (only if explicitly stated)
Neutral Target

### Parametric EQ / preset data
```text
{peq_text}
```

### Source published / updated date
2026-08-29

### Authorship
- [X] I am the creator of this EQ.

### Notes
Traceable source context only.
"""


class EqSubmissionIssueTest(unittest.TestCase):
    def test_parses_new_form_and_marks_structured_submission_ready_for_source_policy(self):
        filters = "\n".join(
            f"Filter {index}: ON PK Fc {100 + index} Hz Gain {index / 10:.1f} dB Q 1.0"
            for index in range(1, 16)
        )
        submission = normalize_submission_event(event(form_body(filters)))

        self.assertEqual("ready_for_source_policy", submission["candidate_state"])
        self.assertTrue(submission["mechanically_valid"])
        self.assertEqual("unverified", submission["verification_status"])
        self.assertFalse(submission["publication_eligible"])
        self.assertEqual("Example Audio", submission["headphone"]["manufacturer"])
        self.assertEqual("Model One", submission["headphone"]["model"])
        self.assertIsNone(submission["headphone"]["variant"])
        self.assertEqual("parsed", submission["parsed_peq"]["status"])
        self.assertIsNone(submission["parsed_peq"]["preamp_db"])
        self.assertEqual(15, len(submission["parsed_peq"]["filters"]))
        self.assertTrue(submission["submitter_is_creator"])
        self.assertEqual([], submission["validation_errors"])

    def test_preserves_explicit_source_preamp(self):
        body = form_body(
            "Preamp: -6.4 dB\n"
            "Filter 1: ON PK Fc 100 Hz Gain 3.2 dB Q 0.8"
        )

        submission = normalize_submission_event(event(body))

        self.assertEqual(-6.4, submission["parsed_peq"]["preamp_db"])
        self.assertEqual(1, len(submission["parsed_peq"]["filters"]))
        self.assertTrue(submission["mechanically_valid"])

    def test_invalid_peq_is_staged_for_review_instead_of_published_or_dropped(self):
        submission = normalize_submission_event(event(form_body("Gain 3 dB at about 100 Hz")))

        self.assertEqual("needs_review", submission["candidate_state"])
        self.assertFalse(submission["mechanically_valid"])
        self.assertEqual("invalid_needs_review", submission["parsed_peq"]["status"])
        self.assertFalse(submission["publication_eligible"])
        self.assertTrue(submission["review_warnings"])

    def test_preset_link_is_not_fetched_or_interpreted_as_filters(self):
        submission = normalize_submission_event(
            event(form_body("https://example.com/presets/model-one.txt"))
        )

        self.assertEqual("needs_review", submission["candidate_state"])
        self.assertFalse(submission["mechanically_valid"])
        self.assertEqual("preset_link_needs_fetch", submission["parsed_peq"]["status"])
        self.assertEqual([], submission["parsed_peq"]["filters"])

    def test_legacy_combined_headphone_field_is_never_guessed(self):
        body = """
### Headphone / IEM
HIFIMAN Edition XS

### EQ creator / username
Someone

### Original source URL
https://www.reddit.com/r/headphones/comments/example

### Source platform
Reddit

### Parametric EQ / preset data
```text
Filter 1: ON PK Fc 100 Hz Gain 1.0 dB Q 1.0
```
"""
        submission = normalize_submission_event(event(body))

        self.assertIsNone(submission["headphone"]["manufacturer"])
        self.assertIsNone(submission["headphone"]["model"])
        self.assertEqual("HIFIMAN Edition XS", submission["headphone"]["legacy_combined_label"])
        self.assertIn("identity review", submission["validation_errors"][0])
        self.assertFalse(submission["mechanically_valid"])

    def test_missing_required_provenance_stays_review_only(self):
        body = form_body("Filter 1: ON PK Fc 100 Hz Gain 1.0 dB Q 1.0")
        body = body.replace("### EQ creator / username\nCreatorName", "### EQ creator / username\n_No response_")

        submission = normalize_submission_event(event(body))

        self.assertEqual("needs_review", submission["candidate_state"])
        self.assertFalse(submission["mechanically_valid"])
        self.assertTrue(submission["validation_errors"])

    def test_issue_form_parser_ignores_markdown_before_first_heading(self):
        fields = parse_issue_form_fields(
            "Intro text\n\n### Manufacturer\nMaker\n\n### Exact model\nModel"
        )
        self.assertEqual({"Manufacturer": "Maker", "Exact model": "Model"}, fields)


if __name__ == "__main__":
    unittest.main()
