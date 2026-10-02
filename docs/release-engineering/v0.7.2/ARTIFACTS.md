# v0.7.2 artifact and remote state

## Source

- Repository: weekssa/OPRA-EQ-for-UAPP
- Worktree: /Users/stephenweeks/.codex/worktrees/a79b/OPRA-EQ-for-UAPP
- Branch: codex/v0.7.2-stabilization
- Base/tag: v0.7.1
- Base commit: c48f6a5daa08a5e03475b2e415fe80b41d3357db
- Starting v0.7.1 metadata: versionName 0.7.1, versionCode 8
- Target metadata: versionName 0.7.2, versionCode 9
- v0.7.2 tag at initial remote check: absent
- Latest fetched `origin/main`: `afed3dc90b5873218d5e333882528f8c5ddd54a2` (2026-10-02); changes since v0.7.1 are README/changelog/runbook/checklist/signing docs and live catalog data, with no production Kotlin changes
- Latest main merge: `1530d02f` on this branch, with `afed3dc90b5873218d5e333882528f8c5ddd54a2` as the merged parent; v0.7.1 `c48f6a5daa08a5e03475b2e415fe80b41d3357db` remains an ancestor
- Current remote workflows: latest observed main dependency-submission run `37035754432` succeeded; catalog/currentness runs `37035683308` and `37033343784` succeeded; older Dependabot failure `37029780198` was on superseded main `4a3cc20f24dd6308bacd36e3d52e98d09c3e8e37`
- Recovery commit: `8acb4b976f1f9c25cd364f517ab0e6af33a253f1` (`fix(catalog): stream large canonical snapshot loads`), parent is exact v0.7.1 commit
- Pristine v0.7.1 API 35 instrumentation: 25 tests passed, 0 failures/errors/skips on `opra-v072-api35` (API 35, Google APIs ARM64, emulator 37.1.11)
- Pristine v0.7.1 API 26 cold-install smoke: failed on `opra-v072-api26` with confirmed catalog-load OOM; failure evidence was captured from logcat before applying the source fix
- Corrected API 26 smoke: current worktree release APK, versionName 0.7.1/code 8, locally signed with a temporary smoke-only key; install/start/resumed-activity/process checks passed and AndroidRuntime error log was empty
- Temporary API 26 smoke keystore and APK were deleted after verification; this is not a signed candidate and its temporary certificate is not the release signer
- Original `medium_phone` API 36 AVD was restarted without wiping and still has the previously installed app, versionName 0.7.0/code 7

## Promotion records

- Branch commits beyond v0.7.1: recovery source `8acb4b976f1f9c25cd364f517ab0e6af33a253f1`, recovery ledger `bd615efd`, and latest-main merge `1530d02f`; push of the merge checkpoint pending
- Pull request: NONE
- CI runs: NONE for this branch
- Merge SHA: NONE
- Signed candidate workflow/run/artifact: NONE
- APK SHA-256: NONE
- Signer certificate SHA-256: expected pinned project identity, to be reverified on candidate
- Candidate manifest: NONE
- v0.7.2 tag: absent
- GitHub Release URL: NONE

Update this file after every branch push, PR/CI change, merge, candidate, tag, publication, and public asset verification. Never copy a previous candidate checksum or signer verification forward as evidence for a changed source.
