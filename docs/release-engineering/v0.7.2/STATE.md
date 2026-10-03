# v0.7.2 active execution state

## Identity

- Repository: `weekssa/OPRA-EQ-for-UAPP`
- Worktree: `/Users/stephenweeks/.codex/worktrees/a79b/OPRA-EQ-for-UAPP`
- Branch: `codex/v0.7.2-stabilization`
- Verified release base: `v0.7.1` at `c48f6a5daa08a5e03475b2e415fe80b41d3357db`, versionName `0.7.1`, versionCode `8`
- Latest pushed production source commit: `e63fc4bf5be629135b0fd56449bad4c1b0bfd1b4`
- Recovery fix commits in the pushed branch: `40b4f5d8c7b255ebed7cc886158d6e6621a911c4`, `e63fc4bf5be629135b0fd56449bad4c1b0bfd1b4`
- Product candidate already validated and in the PR: `1c36349ca3edb69061a34b44d385670380f60512`
- Both recovery fixes are pushed. The current PR commit may be a docs-only descendant; resolve the live checkout and PR SHA before relying on CI or review.
- PR #65 is open from `codex/v0.7.2-stabilization` into `main`.
- Release target: versionName `0.7.2`, versionCode `9`.
- No physical DAC was connected or mutated.

## Completed before the current recovery fix

- The v0.7.2 candidate addresses the reproduced API 26 startup OOM, includes an independent dense DSP response oracle and bounded safety correction, version-driven release workflows, and synchronized release docs. The DSP evidence is recorded in D005 and the test matrix.
- Product commit `1c36349ca3edb69061a34b44d385670380f60512` passed 727 JVM tests, lint, debug/release assembly, R8 mapping, and API 35 instrumentation (25/25). The Python suite passed 235 tests; catalog/release contracts, actionlint, ShellCheck, and API 26 ARM64 cold smoke passed. These results apply to that source and are not transferred to later commits.
- Prior PR head `af3f4c596e99e0cf8dce408b982c641c6898de6a` passed all eight CI/security checks. API 26 artifact `11259384390` and UI artifact `11258319894` were downloaded and inspected. These checks predate the current EW300 recovery fix.
- PR source head `e63fc4bf5be629135b0fd56449bad4c1b0bfd1b4`: all eight checks passed, including Android build/UI/API26 (`37087067275`), Analyze Kotlin (`37087067259`), CodeQL (`111100633597`), Catalog (`37087067266`), Priority community (`37087067347`), and dependency submission (`37087067174`). The ledger commit will create a docs-only descendant requiring fresh exact-head CI and review.

## Current exact-head finding and recovery

- Independent reviews of PR heads `af3f4c59` and `13bf1f20` returned FAIL with separate P2 replay windows in two baseline-restoration paths. Both were caused by sending restoration writes before persisting a terminal attempt marker.
- Commits `40b4f5d8` and `e63fc4bf` now persist durable `RESTORATION_ATTEMPTED` before the first baseline write in both paths. Failure to save the marker prevents that path's restoration write; the stage is terminal if later uncertainty persistence fails.
- Simulated transport/store regressions cover each path, transient temporary readback, failed pre-write checkpoints, failed `UNCERTAIN` checkpoints, and qualifier recreation. Focused persistence class passes 17/17.
- Fresh full Gradle gate on exact source commit `e63fc4bf5be629135b0fd56449bad4c1b0bfd1b4`: 731 tests, 0 failures/errors/skips; lint 0 errors, 111 warnings, 2 hints; debug and release assemblies pass. `bash tools/verify-r8-mapping.sh` passes.
- No protocol bytes, hardware identity rules, authorization, or real DAC state changed.

## Current changes

Three Kotlin source/test files cover both fixes in commits `40b4f5d8` and `e63fc4bf`; the latest production source commit is pushed. `.unlazy/v0.7.2-autonomous-release/` is a local untracked orchestration workspace and must not be staged. Resolve the live PR head before relying on CI/review status; exact-head CI and independent review remain incomplete.

## Next exact actions

1. Complete all exact-head CI checks and independent review for PR source head `e63fc4bf`.
2. Repair any owned finding and repeat exact-head checks/review.
3. Merge normally under the active `Protect main` ruleset only after exact-head CI and review pass.
4. Reverify merged main, create and independently verify the main-only signed candidate, then tag and publish only after every artifact gate passes.

## Current failure and release state

Both P2 findings are fixed in pushed source head `e63fc4bf`; current-head CI and independent review remain pending. Merge, signed candidate, immutable v0.7.2 tag, and public GitHub Release remain pending. No owner action is required.
