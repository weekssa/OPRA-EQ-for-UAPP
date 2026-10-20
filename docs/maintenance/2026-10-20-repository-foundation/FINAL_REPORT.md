# Final Report — Repository Foundation and v0.8.3 Consolidation

Status: PRE-MERGE REVIEW; NOT INTEGRATED

## Executive summary

The public stable v0.8.3 release is verified from source `899755507f812c940f3ab9381a856c80c6030885` with exact signed APK, checksum, and signer in `docs/release-engineering/v0.8.3-stable/ARTIFACTS.md`. At the audit snapshot, live `main` was `35e91c2170786ae4afc7a6679ba9ba34a01511a8`. The exact app currently installed on the owner's Pixel could not be read; no device was connected. Historic release records say a separate Beta v1 package remained installed. Do not equate installed state with public release provenance.

A protected private full-copy backup passed inventory/hash/metadata verification, Git integrity checks, and a separate non-destructive restore test. The source Git repository is shallow; the supplementary bundle verifies against the source but cannot independently restore past the shallow boundary. The full copy is the recovery method. No suitable external drive was available.

The original Mac checkout and its user-owned files remain preserved. No existing branch was deleted or merged. No application, dependency, signing, package identity, or release changes are included in this documentation/workflow PR.

## Work completed

Canonical state, goals, known issues, operating workflow, active instructions, Android skills routing, and CI documentation validation were consolidated. The validator and PR template are included in draft PR #100. Local documentation validation, diff/scope checks, and Android unit-test/lint/debug assembly passed.

On prior PR head `e955f62db0bb7ab96182ee3964aa579a6cb9d186`, exact-head run [39507302003](https://github.com/weekssa/OPRA-EQ-for-UAPP/actions/runs/39507302003) attempt 2 passed all four jobs: `build`, `CodeQL exact head`, `min-api-smoke`, and `emulator-ui-test`; no jobs were skipped. Attempt 1 on that same SHA failed 8/72 emulator UI tests because Activity focus was false (0 skipped); attempt 2 passed all 72 without source changes. PR check-runs `Analyze Kotlin`, `CodeQL`, and `submit-gradle` passed. Ordinary push run [39507297134](https://github.com/weekssa/OPRA-EQ-for-UAPP/actions/runs/39507297134) failed with zero jobs and no downloadable logs (API 404); its cause is unknown. A correction to these stale records advances the PR SHA; exact-head validation must be checked on that final head. Issue #99 and PR #100 hold the live SHA and run readback.

GitHub reports 56 open Maven Dependabot alerts on `main` (3 critical, 22 high, 28 moderate, 3 low), attributed to `settings.gradle.kts`. Runtime inclusion/reachability is unclassified. Separate follow-ups #101-#107 track security, CI, emulator flakiness, physical provenance, backup retention, external instruction reconciliation, and historical branch disposition. No dependency remediation is in this PR.

## Before merge

- Finish the exact remote PR review and verify CI/checks against the final SHA.
- Confirm no sensitive/private material and no application changes in the final diff.
- Report a GO/NO-GO recommendation and obtain explicit owner approval. The PR remains draft and unmerged until approved.

The active `Protect main` ruleset prevents deletion and non-fast-forward updates but enforces no required review count or check contexts. The task's owner-approval gate applies regardless.

## Immediately after approved merge

Verify the merge into actual GitHub `main`; create a clean local development worktree based on that merged tip; verify root `AGENTS.md`, canonical state/runbooks, Android skills routing, and release guidance from the new workspace; confirm original checkout and backup integrity. Do not reset, overwrite, clean, or forcibly synchronize the original checkout.

## Delivery and decision

Tracking issue: [#99](https://github.com/weekssa/OPRA-EQ-for-UAPP/issues/99).

Draft PR: [#100](https://github.com/weekssa/OPRA-EQ-for-UAPP/pull/100).
Separate follow-ups: [#101](https://github.com/weekssa/OPRA-EQ-for-UAPP/issues/101), [#102](https://github.com/weekssa/OPRA-EQ-for-UAPP/issues/102), [#103](https://github.com/weekssa/OPRA-EQ-for-UAPP/issues/103), [#104](https://github.com/weekssa/OPRA-EQ-for-UAPP/issues/104), [#105](https://github.com/weekssa/OPRA-EQ-for-UAPP/issues/105), [#106](https://github.com/weekssa/OPRA-EQ-for-UAPP/issues/106), [#107](https://github.com/weekssa/OPRA-EQ-for-UAPP/issues/107).

Owner decision still required: approve or decline merging PR #100 after its final-head review. Post-merge synchronization has not started.
