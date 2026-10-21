# Final Report — Repository Foundation and v0.8.3 Consolidation

Status: PR #100 MERGED; POST-MERGE VERIFIED; FOLLOW-UPS OPEN

## Executive summary

The public stable v0.8.3 release is verified from source `899755507f812c940f3ab9381a856c80c6030885` with the exact signed APK, checksum and signer recorded in `docs/release-engineering/v0.8.3-stable/ARTIFACTS.md`. The installed physical-device package/source remains **UNKNOWN / NOT VERIFIED** because no ADB device was available. Historic records say a separate Beta v1 package remained installed; that is not current device evidence.

The protected full-copy backup passed inventory/hash/metadata verification, Git integrity checks, and a separate non-destructive restore test. The supplementary bundle cannot independently recover beyond the source repository's shallow-history boundary; the full copy is the recovery method. No external backup drive was available.

The owner approved and PR #100 merged normally into GitHub `main` at `1599ab451f711d8a27adfefbcba10293cd53dd84`. Its tree matches the validated PR head `ccec0184742d7798778bcb91d173e4a7ea44531e`. No application, dependency, signing, package identity, release, or historical-branch changes were included. The cleanup branch is retained.

## Review and validation

Exact-head Android CI [run 39513136561](https://github.com/weekssa/OPRA-EQ-for-UAPP/actions/runs/39513136561) passed all four jobs on the reviewed source SHA: build, emulator-ui-test, CodeQL exact head, and min-api-smoke. Failed, skipped, and pending jobs: 0. Live PR checks Analyze Kotlin, CodeQL, and submit-gradle passed. Final diff review found no unintended application/dependency/signing/release changes.

In the clean post-merge worktree, `python3 tools/validate-repository-foundation.py` passed and the documented `./tools/codex-android :app:testDebugUnitTest :app:lintDebug :app:assembleDebug` completed successfully in 55 seconds. The output included existing deprecation/lint warnings; no failures.

On merge commit `1599ab451f711d8a27adfefbcba10293cd53dd84`, Android push run [39514881784](https://github.com/weekssa/OPRA-EQ-for-UAPP/actions/runs/39514881784) failed before creating jobs (zero jobs; no logs). Dependabot security update run [39514901376](https://github.com/weekssa/OPRA-EQ-for-UAPP/actions/runs/39514901376) failed with `dependency_not_found`, because requested dependencies were absent from the dependency snapshot. Both are separate tracked follow-ups. Main CodeQL run [39514883158](https://github.com/weekssa/OPRA-EQ-for-UAPP/actions/runs/39514883158) completed successfully on the merge commit.

## Post-merge workspace

The new clean local development baseline is the `codex/development-main` worktree tracking `origin/main` at the merge commit. It contains root `AGENTS.md`, project state, operating runbook, release guidance, Android skill routing, and the documented Gradle helper. The original checkout's HEAD and status still match preflight. The verified backup remains untouched. No historical branch, original file, or backup was deleted.

## Remaining follow-ups

GitHub issue [#99](https://github.com/weekssa/OPRA-EQ-for-UAPP/issues/99) remains the parent record. Open follow-ups are [#101](https://github.com/weekssa/OPRA-EQ-for-UAPP/issues/101) P1 security assessment of all 56 Dependabot alerts (3 critical first) and updater snapshot failure; [#102](https://github.com/weekssa/OPRA-EQ-for-UAPP/issues/102) P2 zero-job push workflow; [#103](https://github.com/weekssa/OPRA-EQ-for-UAPP/issues/103) P2 emulator Activity-focus flakiness; [#104](https://github.com/weekssa/OPRA-EQ-for-UAPP/issues/104) P2 installed-device provenance when available and authorized; [#105](https://github.com/weekssa/OPRA-EQ-for-UAPP/issues/105) P3 external backup availability/retention; [#106](https://github.com/weekssa/OPRA-EQ-for-UAPP/issues/106) P3 obsolete Codex/ChatGPT instructions; and [#107](https://github.com/weekssa/OPRA-EQ-for-UAPP/issues/107) P2 historical branch disposition. No dependency remediation or application changes were made.
