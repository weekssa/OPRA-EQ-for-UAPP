# v0.7.2 active execution state

## Identity

- Repository: `weekssa/OPRA-EQ-for-UAPP`
- Worktree: `/Users/stephenweeks/.codex/worktrees/a79b/OPRA-EQ-for-UAPP`
- Branch: `codex/v0.7.2-stabilization`
- Verified release base: `v0.7.1` at `c48f6a5daa08a5e03475b2e415fe80b41d3357db`, versionName `0.7.1`, versionCode `8`
- Last pushed PR head before this recovery fix: `af3f4c596e99e0cf8dce408b982c641c6898de6a`
- Recovery fix commit, local and not yet pushed: `40b4f5d8c7b255ebed7cc886158d6e6621a911c4`
- Product candidate already validated and in the PR: `1c36349ca3edb69061a34b44d385670380f60512`
- Recovery code and initial ledger sync are committed locally; this follow-up status update remains uncommitted. Neither has been pushed.
- PR #65 is open from `codex/v0.7.2-stabilization` into `main`.
- Release target: versionName `0.7.2`, versionCode `9`.
- No physical DAC was connected or mutated.

## Completed before the current recovery fix

- The v0.7.2 candidate addresses the reproduced API 26 startup OOM, includes an independent dense DSP response oracle and bounded safety correction, version-driven release workflows, and synchronized release docs. The DSP evidence is recorded in D005 and the test matrix.
- Product commit `1c36349ca3edb69061a34b44d385670380f60512` passed 727 JVM tests, lint, debug/release assembly, R8 mapping, and API 35 instrumentation (25/25). The Python suite passed 235 tests; catalog/release contracts, actionlint, ShellCheck, and API 26 ARM64 cold smoke passed. These results apply to that source and are not transferred to later commits.
- PR head `af3f4c596e99e0cf8dce408b982c641c6898de6a` passed all eight CI/security checks. API 26 artifact `11259384390` and UI artifact `11258319894` were downloaded and inspected. These checks predate the current EW300 recovery fix.

## Current exact-head finding and recovery

- Independent review of PR head `af3f4c59` returned FAIL with one P2: if baseline restoration failed and the follow-up `UNCERTAIN` checkpoint also failed, `TEMPORARY_COMMITTED` could survive on disk and restoration could be retried after process recreation.
- The local fix adds durable `RESTORATION_ATTEMPTED` before the first restoration write and treats that stage as terminal. Failure to save the marker prevents the hardware write.
- The simulated transport/store tests include an ambiguous write that partially mutates state, failure of the `UNCERTAIN` checkpoint, and a recreated qualifier using the same persisted record. Both new regressions pass.
- Fresh full Gradle gate on exact source commit `40b4f5d8c7b255ebed7cc886158d6e6621a911c4`: 729 tests, 0 failures/errors/skips; lint 0 errors, 111 warnings, 2 hints; debug and release assemblies pass. `bash tools/verify-r8-mapping.sh` passes. The focused EW300 persistence class passes 15/15.
- No protocol bytes, hardware identity rules, authorization, or real DAC state changed.

## Current changes

Three Kotlin source/test files implement and cover the fix in local commit `40b4f5d8c7b255ebed7cc886158d6e6621a911c4`; release engineering docs record the review finding and current validation. The local branch and ledger sync are not yet pushed. `.unlazy/v0.7.2-autonomous-release/` is a local untracked orchestration workspace and must not be staged. Exact new PR-head CI and independent review are not yet available.

## Next exact actions

1. Run final diff checks and verify intended branch state.
2. Push the local recovery-fix branch normally, leaving `.unlazy/` untracked.
3. Refresh PR #65 and require all exact-head checks.
4. Obtain independent review of the exact pushed head. Repair findings and repeat applicable checks/review.
5. Merge normally under the active `Protect main` ruleset only after exact-head CI and review pass.
6. Reverify merged main, create and independently verify the main-only signed candidate, then tag and publish only after every artifact gate passes.

## Current failure and release state

The P2 finding is fixed locally but remains unverified on a pushed PR head. Merge, signed candidate, immutable v0.7.2 tag, and public GitHub Release remain pending. No owner action is required.
