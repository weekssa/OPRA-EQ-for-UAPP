# Gates: v0.8.3 JA11 defaults beta

OWNS: app/**, docs/implementation/v0.8.3-defaults-beta/**, docs/FIIO_JA11_CAPABILITY_MATRIX.md, docs/FIIO_JA11_VALIDATION_LEDGER.md, docs/FIIO_JA11_PROTOCOL_NOTES.md, docs/V0.6_MY_DAC_STATUS.md, docs/CHATGPT_PROJECT_RUNBOOK.md, docs/PUBLIC_RELEASE_CHECKLIST.md, CHANGELOG.md, README.md, .github/workflows/**, tools/**

Scope: Implement and qualify the separate production-quality v0.8.3 JA11 beta for runtime-only Apply and explicitly confirmed default saving, retaining exact candidate evidence and the installed beta for owner review.

- [x] G1: Current-source contract and bounded implementation plan are recorded before production edits.
  EVIDENCE: `INTAKE.md`, `EXECUTION_HANDOFF.md`, `PLAN.md`; base `575dab59fe774d3683a3714ca490a334f1b20764`.

- [x] G2: Runtime volume and staged program dialogs support Cancel, runtime-only Apply, and Set as Default; Cancel and selection staging do not write.
  EVIDENCE: `TEST_RESULTS.md`; current-source Beta instrumentation 72/72 API 35 and focused JA11 9/9 API 26.

- [x] G3: Confirmed Set as Default transaction writes the target at most once, performs exactly two mic writes on success, restores original mic, verifies the fresh session and companion state, compares applicable User 1 EQ snapshots with read-only reads, and emits no forbidden commands.
  EVIDENCE: `FiioJa11ControlRepositoryTest`; transaction/report source counts; independent review. App-level counters do not claim USB bus capture.

- [x] G4: Failure injection proves fail-closed reconnect denial/timeout, detach/replacement identity, stale session, mismatch, companion change and serialized rapid mutations with no replay.
  EVIDENCE: `FiioJa11ControlRepositoryTest` and `TEST_RESULTS.md`. Android framework permission dialogs are not simulated; physical permission handling remains part of the owner session.

- [x] G5: Focused ViewModel/repository/UI tests and all applicable JVM/security/repository suites pass on coherent source.
  CHECK: ./tools/codex-android :app:testDebugUnitTest
  EXPECT: BUILD SUCCESSFUL
  EVIDENCE: current-source `:app:testDebugUnitTest` passed.

- [x] G6: Lint, debug and release builds, beta build, R8 mapping, and Android-test compilation pass; public behavior is production-equivalent to beta feature behavior.
  CHECK: ./tools/codex-android :app:lintDebug :app:lintBeta :app:assembleDebug :app:assembleRelease :app:assembleBeta :app:compileBetaAndroidTestKotlin
  EXPECT: BUILD SUCCESSFUL
  EVIDENCE: current-source command completed successfully; see `TEST_RESULTS.md`. Candidate SHA build follows commit.

- [ ] G7: Minimum-API and modern emulator install/launch/coexistence/UI instrumentation pass on disposable emulators where available.
  CHECK: API 26 focused JA11 class and API 35 full `:app:connectedBetaAndroidTest`.
  EXPECT: Focused JA11 pass on API 26; full suite pass on API 35. Broad API 26 full-suite failures require the owner's explicit exact-candidate substitute before phone testing.
  EVIDENCE: 9/9 API 26 JA11 and 72/72 API 35 pass; 69/72 broad API 26 with three unrelated failures recorded in `TEST_RESULTS.md`.

- [ ] G8: Independent source/scope review has no unresolved safety finding; live exact-head checks are tied to the frozen candidate.
  EVIDENCE: Independent architecture review found no code blocker after gate-bound EQ readback update; exact-head remote checks pending candidate commit/PR.

- [ ] G9: Private beta app/test APK provenance is frozen: source SHA, package/version/code, signer, digests, build command and test plan.
  EVIDENCE: pending

- [ ] G10: Owner wireless-ADB session completed or stopped at the first failure, with exact beta and app data left installed and actual final settings reported.
  EVIDENCE: pending

- [ ] G11: Owner identifies the installed candidate as passing and directs publication; otherwise remain at the owner checkpoint with the beta installed.
  EVIDENCE: pending

- [ ] G12: After that instruction, the exact implementation is integrated, officially signed, upgrade/clean-install verified, promoted without rebuild, publicly read back, and documentation is closed.
  EVIDENCE: pending
