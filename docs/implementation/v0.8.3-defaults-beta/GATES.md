# Gates: v0.8.3 JA11 defaults beta

OWNS: app/**, docs/implementation/v0.8.3-defaults-beta/**, docs/FIIO_JA11_CAPABILITY_MATRIX.md, docs/FIIO_JA11_VALIDATION_LEDGER.md, docs/FIIO_JA11_PROTOCOL_NOTES.md, docs/V0.6_MY_DAC_STATUS.md, docs/CHATGPT_PROJECT_RUNBOOK.md, docs/PUBLIC_RELEASE_CHECKLIST.md, CHANGELOG.md, README.md, .github/workflows/**, tools/**

Scope: Implement and qualify the separate production-quality v0.8.3 JA11 beta for runtime-only Apply and explicitly confirmed default saving, retaining exact candidate evidence and the installed beta for owner review.

- [x] G1: Current-source contract and bounded implementation plan are recorded before production edits.
  EVIDENCE: `INTAKE.md`, `EXECUTION_HANDOFF.md`, `PLAN.md`; base `575dab59fe774d3683a3714ca490a334f1b20764`.

- [x] G2: Runtime volume and staged program dialogs support Cancel, runtime-only Apply, and Set as Default; Cancel and selection staging do not write.
  EVIDENCE: `TEST_RESULTS.md`; frozen-source unminified Debug instrumentation passes the focused JA11 class 9/9 on API 26 and full suite 72/72 on API 35. Exact minified Beta test APK has a separately recorded startup failure.

- [x] G3: Confirmed Set as Default transaction writes the target at most once, performs exactly two mic writes on success, restores original mic, verifies the fresh session and companion state, compares applicable User 1 EQ snapshots with read-only reads, and emits no forbidden commands.
  EVIDENCE: `FiioJa11ControlRepositoryTest`; transaction/report source counts; independent review. App-level counters do not claim USB bus capture.

- [x] G4: Failure injection proves fail-closed reconnect denial/timeout, detach/replacement identity, stale session, mismatch, companion change and serialized rapid mutations with no replay.
  EVIDENCE: `FiioJa11ControlRepositoryTest` and `TEST_RESULTS.md`. Android framework permission dialogs are not simulated; physical permission handling remains part of the owner session.

- [x] G5: Focused ViewModel/repository/UI tests and all applicable JVM/security/repository suites pass on coherent source.
  CHECK: ./tools/codex-android :app:testDebugUnitTest
  EXPECT: BUILD SUCCESSFUL
  EVIDENCE: exact source `7cea4545dddbee2da0b436e0684a9c489835e3cd` `:app:testDebugUnitTest` passed.

- [x] G6: Lint, debug and release builds, beta build, R8 mapping, and Android-test compilation pass; public behavior is production-equivalent to beta feature behavior.
  CHECK: ./tools/codex-android :app:lintDebug :app:lintBeta :app:assembleDebug :app:assembleRelease :app:assembleBeta :app:compileBetaAndroidTestKotlin
  EXPECT: BUILD SUCCESSFUL
  EVIDENCE: exact-source command completed successfully on frozen `7cea454`; app/test APK provenance and exact source embedding are recorded in `ARTIFACTS.md`.

- [x] G7: Emulator UI gate is satisfied by an owner-approved, bounded substitute; original failures remain recorded and are not relabeled as passes.
  CHECK: Exact Beta APK install/launch/coexistence on API 26 and 35; minified `:app:connectedBetaAndroidTest`; same-SHA Debug UI substitute.
  EXPECT: Exact Beta direct launch succeeds. The minified Beta instrumentation failure remains visible and may be substituted only by explicit owner acceptance; proposed substitute is Debug JA11 9/9 on API 26, full Debug 72/72 on API 35, plus direct minified Beta install/launch/coexistence on both. Full Debug API 26 is 68/72 with four unrelated failures; focused JA11 passes 9/9.
  EVIDENCE: Exact source `7cea454`; Beta launches on both APIs; minified runner fails before test discovery because `androidx.tracing.Trace` is absent; substitute results and all four API 26 failures are in `TEST_RESULTS.md`. Owner explicitly accepted the bounded substitution on 2026-10-14 before phone work.

- [x] G8: Independent source/scope review has no unresolved safety finding; live exact-head checks are tied to the frozen candidate.
  EVIDENCE: Independent review found no product-code blocker. PR #97 head `ecdd230aed8e50f5fd5cb5de0549c77ccca2d0e5` passes `validate`, `validate-priority-community`, `submit-gradle`, and `Analyze Kotlin`; generic CodeQL is neutral/skipped. New owner-acceptance and release-preparation changes are documentation-only and will trigger a fresh exact-head run.

- [x] G9: Private beta app/test APK provenance is frozen: source SHA, package/version/code, signer, digests, build command and test plan.
  EVIDENCE: `ARTIFACTS.md`, frozen source `7cea4545dddbee2da0b436e0684a9c489835e3cd`; exact SHA embedded in the app and test APK BuildConfigs and app DEX.

- [x] G10: Owner wireless-ADB session completed with exact beta and app data left installed; final settings and evidence limits are recorded.
  EVIDENCE: Owner accepted the G7 substitution, confirmed readiness, authorized USB `Always allow`, and later performed a 10-second physical JA11 power removal on 2026-10-14. Exact Beta v1 APK remains installed on Pixel 9 (API 37), package/version verified, with stable and earlier JA11 beta apps preserved. Runtime-only Apply, staged Cancel and three default transactions are recorded in `TEST_RESULTS.md` and J032 in the validation ledger. Mic restoration and fresh post-power-cycle readback passed for pair 19 / Classic. User 1 EQ comparison and unique device continuity remain unverified; see limits.

- [x] G11: Owner identifies the installed candidate as passing and directs publication; otherwise remain at the owner checkpoint with the beta installed.
  EVIDENCE: On 2026-10-14 the owner reported success for the identified Beta v1 manual testing, stated that all other manual tests passed, and authorized final go-live. J033 records the owner's statement and exact evidence limits. No bus capture, unique serial, or independent EQ-bank payload was supplied; no broader claim is made.

- [ ] G12: Integrate the exact implementation, officially sign from main, pass hosted upgrade/clean-install checks, promote without rebuild, verify public readback, and close documentation.
  EVIDENCE: In progress after owner authorization; PR #97 is the normal integration path. Record exact main SHA, candidate run/artifact/digests, promotion run, public assets and metadata before marking complete.
