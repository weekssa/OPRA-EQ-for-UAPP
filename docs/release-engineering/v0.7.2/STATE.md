# v0.7.2 active execution state

## Identity

- Repository: `weekssa/OPRA-EQ-for-UAPP`
- Worktree: `/Users/stephenweeks/.codex/worktrees/a79b/OPRA-EQ-for-UAPP`
- Branch: `codex/v0.7.2-stabilization`
- Verified release base: `v0.7.1` at `c48f6a5daa08a5e03475b2e415fe80b41d3357db`, versionName `0.7.1`, versionCode `8`
- Latest pushed production source commit: `e63fc4bf5be629135b0fd56449bad4c1b0bfd1b4`
- Recovery fix commits in the pushed branch: `40b4f5d8c7b255ebed7cc886158d6e6621a911c4`, `e63fc4bf5be629135b0fd56449bad4c1b0bfd1b4`
- Product candidate already validated and in the PR: `1c36349ca3edb69061a34b44d385670380f60512`
- PR #65 was merged normally at `a60411bebfdbd1cea4218d3bde45013bb7ed26a9` from exact reviewed head `e7f2fc937e9b265770296dbdc4cbb40a4e5e13c9`.
- All eight checks on `e7f2fc93` passed and independent review returned PASS with no P0-P2 finding. The merged main tree exactly equals that tested tree.
- `main` is verified at `a60411bebfdbd1cea4218d3bde45013bb7ed26a9`; app metadata is 0.7.2/code 9, package ID unchanged, minSdk 26, targetSdk 36.
- The recovery branch last pushed head remains `e7f2fc93` until this post-merge ledger checkpoint is committed and pushed.
- Signed beta run `37093821378` succeeded from exact merged SHA and published a signed APK to temporary branch `mobile-test-apk` at commit `19ded401683b87ae56528ba5fc3af783cacec4bb`. The artifact is testing evidence only; the promotion verifier correctly rejected it because it was not produced by the required release workflow.
- Official Signed Release Candidate run `37095180116` succeeded from exact merged SHA `a60411bebfdbd1cea4218d3bde45013bb7ed26a9` with tag `v0.7.2`; artifact `11264251526` passed independent verification. Candidate APK SHA-256 `f3afaa102a31491286828faa37cfe1454853721d1e4aa736da57bf4919e89ded`; artifact ZIP digest `sha256:1da409dcf47368ec254a5432e7c1d316473920c28e27c19416b10cc8e29969d9`.
- Promotion run `37096259477` passed candidate verification and API 35 v0.7.1 clean install / exact candidate in-place upgrade / cold launch, but failed in the tag job due to asymmetric SHA-256 prefix normalization. Publish was skipped; no tag or release mutation occurred.
- Release target: versionName `0.7.2`, versionCode `9`.
- No physical DAC was connected or mutated.

## Completed before the current recovery fix

- The v0.7.2 candidate addresses the reproduced API 26 startup OOM, includes an independent dense DSP response oracle and bounded safety correction, version-driven release workflows, and synchronized release docs. The DSP evidence is recorded in D005 and the test matrix.
- Product commit `1c36349ca3edb69061a34b44d385670380f60512` passed 727 JVM tests, lint, debug/release assembly, R8 mapping, and API 35 instrumentation (25/25). The Python suite passed 235 tests; catalog/release contracts, actionlint, ShellCheck, and API 26 ARM64 cold smoke passed. These results apply to that source and are not transferred to later commits.
- Prior PR head `af3f4c596e99e0cf8dce408b982c641c6898de6a` passed all eight CI/security checks. API 26 artifact `11259384390` and UI artifact `11258319894` were downloaded and inspected. These checks predate the current EW300 recovery fix.
- PR head `8db499af0212795d05b97d0439a0c82f462722b9`: all eight checks passed. Android build, emulator UI, and API26 jobs are in run `37088331227`; Analyze Kotlin `37088331231`; CodeQL check `111104328833`; Catalog `37088331236`; Priority community `37088331239`; dependency submission `37088326577`.
- Exact-head API35 emulator UI instrumentation passed 25/25 (artifact `11261079560`). Fresh-wipe API26 x86_64 smoke installed the minified release APK, launched MainActivity, reached the Manufacturers list, and remained alive/resumed for 60 seconds without an app AndroidRuntime error (artifact `11261199392`).
- The downloaded debug APK is package `com.weekssa.opraeqforuapp`, versionName `0.7.2`, versionCode `9`, SHA-256 `bf18e80806756814bd30bd4a7d35e3c0615cb211689accc44a69995a8e367b9c`; it is test evidence, not a release candidate.

## Current exact-head finding and recovery

- Independent reviews of PR heads `af3f4c59` and `13bf1f20` returned FAIL with separate P2 replay windows in two baseline-restoration paths. Both were caused by sending restoration writes before persisting a terminal attempt marker.
- Commits `40b4f5d8` and `e63fc4bf` now persist durable `RESTORATION_ATTEMPTED` before the first baseline write in both paths. Failure to save the marker prevents that path's restoration write; the stage is terminal if later uncertainty persistence fails.
- Simulated transport/store regressions cover each path, transient temporary readback, failed pre-write checkpoints, failed `UNCERTAIN` checkpoints, and qualifier recreation. Focused persistence class passes 17/17.
- Fresh full Gradle gate on exact source commit `e63fc4bf5be629135b0fd56449bad4c1b0bfd1b4`: 731 tests, 0 failures/errors/skips; lint 0 errors, 111 warnings, 2 hints; debug and release assemblies pass. `bash tools/verify-r8-mapping.sh` passes.
- Independent exact-head review of `8db499af0212795d05b97d0439a0c82f462722b9` returned PASS with no actionable P0-P2 issue or unsupported release claim.
- Follow-up review of docs-only head `adf871f25c4e60b9b777063b3ca6af4be1c6fb0b` found a P2 stale restart instruction in `RESUME.md`; commit `3ccf6728` corrected the instruction to resolve live PR state and wait for gates. Independent review of `3ccf6728` returned PASS and its eight checks passed. No source-code finding was reported.
- No protocol bytes, hardware identity rules, authorization, or real DAC state changed.

## Current changes

The product recovery fixes in commits `40b4f5d8` and `e63fc4bf` passed exact-head review, all eight PR checks, and post-merge validation at `a60411be`. The official release candidate and API 35 upgrade passed, but promotion run `37096259477` stopped before tag creation on an asymmetric SHA-256 prefix comparison. The current worktree fixes both tag and publish comparisons, with a direct `command_tag()` regression. The focused publisher tests pass 39/39; the full Python suite passes 238/238 under bundled Python 3.12; the release contract passes. `.unlazy/v0.7.2-autonomous-release/` is a local untracked orchestration workspace and must not be staged.

## Next exact actions

1. Complete independent review and publish the tracked fix through normal PR checks and merge.
2. Reverify corrected `main`, generate and verify a fresh candidate bound to its exact SHA, and run promotion again.
3. Verify remote `v0.7.2` annotated tag binding and public release metadata.
4. Download every public asset and confirm digest, APK bytes, signer, provenance, curated notes, and `/releases/latest`; then push final recovery state and sync main docs.

## Current failure and release state

Both EW300 P2 findings are fixed in source commit `e63fc4bf`. PR #65 exact head `e7f2fc93` passed all eight checks and independent review and merged normally as `a60411be`; the merge tree equals that tested head. Post-merge local Gradle and R8 gates pass. Candidate `37095180116` was independently verified and the promotion API 35 install/upgrade passed, but the tag job failed on digest prefix normalization. The fix and regression are present locally and all 238 Python tests pass; independent review and normal PR merge remain. No tag/release exists. No physical DAC writes were performed. No owner action is required.
