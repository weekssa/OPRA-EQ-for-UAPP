# Gates: Black Pearl Flash verification fix

OWNS: app/src/main/java/com/weekssa/opraeqforuapp/domain/blackpearl/**, app/src/main/java/com/weekssa/opraeqforuapp/data/blackpearl/**, app/src/main/java/com/weekssa/opraeqforuapp/data/dac/DacSessionRepository.kt, app/src/main/java/com/weekssa/opraeqforuapp/data/hardware/HardwareEqRepository.kt, app/src/main/java/com/weekssa/opraeqforuapp/ui/EqLibraryViewModel.kt, app/src/main/java/com/weekssa/opraeqforuapp/ui/EqLibraryApp.kt, app/src/main/java/com/weekssa/opraeqforuapp/ui/screens/MyEqsHomeScreen.kt, app/src/main/java/com/weekssa/opraeqforuapp/ui/screens/FiioJa11OperationPresentation.kt, app/src/main/java/com/weekssa/opraeqforuapp/ui/screens/Ew300MyDacContent.kt, app/src/test/**, docs/UX_REVIEW_WORKFLOW/context/black-pearl-flash-verification-20260926/03-implementation-report.md, docs/UX_REVIEW_WORKFLOW/context/black-pearl-flash-verification-20260926/03-pixel-9-handoff.md

Scope: verify TRN Black Pearl Flash through final native EQ and gain readback, present compact truthful outcomes, and complete software-side validation without hardware mutation.

- [x] G1: Black Pearl domain verification tests cover matching readback, missing/mismatched bands or gain, stale session, and fail-closed write behavior
  CHECK: ./gradlew :app:testDebugUnitTest --tests '*BlackPearlFlasherTest*'
  EXPECT: BUILD SUCCESSFUL
  EVIDENCE: PASS on 2026-09-27; focused command ran with Android SDK and completed successfully. The README-recommended `./tools/codex-android` self-test also passed the full unit suite.

- [x] G2: Black Pearl and cross-DAC presentation tests prove verified wording, not-verified wording, compact feedback, and no duplicate terminal success
  CHECK: ./gradlew :app:testDebugUnitTest --tests '*HardwareFlashMessagingTest*' --tests '*FiioJa11OperationPresentationTest*' --tests '*Ew300OperationStatusTest*'
  EXPECT: BUILD SUCCESSFUL
  EVIDENCE: PASS on 2026-09-27; presentation coverage completed successfully within the full unit suite and the focused validation run.

- [x] G3: affected app unit, lint, and debug/release assembly checks pass on the final implementation head
  CHECK: ./gradlew :app:testDebugUnitTest :app:lintDebug :app:assembleDebug :app:assembleRelease
  EXPECT: BUILD SUCCESSFUL
  EVIDENCE: PASS on 2026-09-27; `./tools/codex-android :app:check :app:assembleDebug :app:assembleRelease` completed successfully, including unit tests, debug lint, release R8/lint-vital, and both APK assemblies.

- [x] G4: final diff review confirms protocol and scope guardrails, report provenance, and Pixel 9 handoff are complete
  EVIDENCE: PASS; source/diff checks are clean, protocol/transport/session-owner/JA11/EW300 production files remain unchanged, and the implementation report plus Pixel 9 handoff are updated.

- [ ] G5: exact signed APK candidate provenance is available for the owner-controlled Pixel 9 gate
  EVIDENCE: pending until a signed candidate is produced by the authorized release workflow
