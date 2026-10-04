# F4: Promotion final Release readback audit and test design

Audit date: 2026-10-03
Audited repository: `weekssa/OPRA-EQ-for-UAPP`
Committed baseline: `daef0560bc120aebfe9d8910a1b867a9f9390c15`
Scope: read-only inspection of the publisher workflow, promotion script, tests, versioned release notes, and live v0.7.2 Release metadata. No tests were run. This file is the only artifact written by this audit.

## Finding

At the committed baseline, `publish_release()` checked that the publication PATCH returned a non-draft Release with the expected tag, then fetched the Release again and checked only `draft`, `prerelease`, and `tag_name` (`tools/promote_release_candidate.py:855-865`). It never compared the final API object's `name` or `body` with the values intended for publication. A title or notes change in GitHub's response could therefore pass the final readback and be reported as success.

The publisher already has version-derived expected values:

- The tag is validated against the candidate's Android `versionName` (`tools/promote_release_candidate.py:238-241`).
- The intended title is `f"EQ Library {tag}"`, and the release request body is `release_notes` (`tools/promote_release_candidate.py:794-801`).
- `command_publish()` reads the curated body from `docs/releases/{tag}.md` and passes it to `publish_release()` (`tools/promote_release_candidate.py:1016-1018`). The main-only workflow invokes this publisher with the verified release tag (`.github/workflows/promote-signed-release.yml:237-259`).
- Resuming an existing draft already compares its title and body to those expected values (`tools/promote_release_candidate.py:781-786`). Creating a new draft only checked its tag and draft flag (`tools/promote_release_candidate.py:803-804`), so the same metadata assertion should also run on the creation response before assets are uploaded or publication proceeds.

The existing publication happy-path test's final Release fixture omitted `name` and `body` and still passed (`tools/test_promote_release_candidate.py:729-781`). It did not exercise mismatching final metadata.

## Read-only Release body comparison

On 2026-10-03, the public API endpoint [`GET /repos/weekssa/OPRA-EQ-for-UAPP/releases/tags/v0.7.2`](https://api.github.com/repos/weekssa/OPRA-EQ-for-UAPP/releases/tags/v0.7.2) returned the name `EQ Library v0.7.2`. Its body was byte-for-byte equal to `docs/releases/v0.7.2.md` at the release source commit `b8e90b9b53fc63ea00fefa499d7d4bd6ce4d55ea`: both were 1,707 bytes, SHA-256 `a7d595da1a95812ba2762f656b01b7ec1ecebbe4bcd348a0c20bd26c0d62ac43`, and both ended in LF. The observed API round-trip provides no evidence for line-ending or terminal-newline normalization.

The current branch's `docs/releases/v0.7.2.md` is 3,307 bytes because a post-publication `Verified release` section was appended. It consequently differs from the still-published body's substantive content, not merely its newline. The F3 public correction must update the actual Release body; F4 comparison must not make substantive differences pass through normalization.

## Narrow version-driven implementation

Use the already validated `tag` and the `release_notes` string loaded from its versioned file. Derive the expected title as `f"EQ Library {tag}"`; do not hardcode `v0.7.2`. A small metadata assertion should require the returned Release object's `name` and `body` to match those expected values.

Apply the assertion to a newly created draft response before upload/publication, and to the final GET response after publication. Keep the existing resumed-draft equality check. Checking the POST response ensures malformed draft metadata stops before the publisher uploads assets or makes the Release public; checking the final GET proves the persisted public Release still has the expected metadata.

The source notes and public v0.7.2 API response currently match exactly, including the terminal LF. Exact body equality is sufficient for the observed API behavior. If the remediation contract deliberately allows the API to add or omit one terminal newline, limit that exception to one optional final ASCII LF and preserve every other character. Do not rewrite internal CRLF/CR sequences or use broad `.strip()`/`.rstrip()` normalization. Current evidence does not justify those transformations.

## Regression coverage

Add focused cases around the assertion and publication path:

1. A version-driven expected title and exact body pass. Use the supplied tag in the expected title, and make at least one assertion with a tag other than `v0.7.2` so the contract cannot regress to a release-specific literal.
2. A wrong Release name fails when the body is correct.
3. A wrong Release body fails when the name is correct.
4. The publication-flow fake returns `name` and `body` on the final GET. Mutate each field independently and assert `PromotionError`; retain a matching final response as the positive case.
5. If accepting one optional terminal LF, test both directions (`notes` versus `notes\n`) as the only allowed difference. Also test that a changed sentence, altered internal line ending, or an extra blank line fails.
6. A newly created draft with a wrong name or body fails before any upload or PATCH. This covers the create path separately from the final public readback.

At audit time, a shared uncommitted F4 draft added a metadata helper and final GET assertion, but its newline helper rewrote CRLF and lone CR throughout the body before dropping a final LF, and the helper was not yet applied to the new-draft POST response. Keep those cases constrained as above. This audit made no changes to production code or tests.
