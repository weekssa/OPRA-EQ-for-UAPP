# Gates — ew300-ja11-parity-20261105

outcome: Five-goal experimental SIMGOT EW300 test build: 0–60 mapped device volume, microphone/headset controls only with exact evidence, UAC mode only if exact evidence exists, read-only current sample rate, and native Low/High Shelf editor/capture/Flash with byte-level reversible Mac evidence; production remains isolated; physical/owner qualification remains separately recorded.
base: 5832bc5e9194cbbff7d9321e8a2bf10395d0c9c6
turns: 12

OWNS: GATES.md,
  docs/implementation/ew300-ja11-parity-20261105/PLAN.md,
  docs/implementation/ew300-ja11-parity-20261105/CONTRACT.md,
  docs/implementation/ew300-ja11-parity-20261105/GATES.md,
  docs/implementation/ew300-ja11-parity-20261105/GATES.md.sha,
  docs/implementation/ew300-ja11-parity-20261105/PROGRESS.md,
  docs/implementation/ew300-ja11-parity-20261105/READINESS.md,
  docs/implementation/ew300-ja11-parity-20261105/PIXEL_SESSION.md,
  docs/planning/ew300-ja11-parity/GOALS.md,
  docs/planning/ew300-ja11-parity/CHIP_MAP.md,
  docs/planning/ew300-ja11-parity/CHIP_CONFIDENCE.md,
  docs/planning/ew300-ja11-parity/README.md,
  docs/planning/ew300-ja11-parity/BRIEF.md,
  docs/planning/ew300-ja11-parity/EXECUTION_PROMPT.md,
  docs/planning/ew300-ja11-parity/FUNCTIONAL_MAPPING.md,
  docs/planning/ew300-ja11-parity/PLANNING_PROMPT.md,
  docs/planning/ew300-ja11-parity/PROGRESS_TEMPLATE.md,
  docs/planning/ew300-ja11-parity/REVIEW_PACKET.md,
  docs/planning/ew300-ja11-parity/RESEARCH_SOURCES.md,
  docs/planning/ew300-ja11-parity/TEST_EVIDENCE_LOG.md,
  app/build.gradle.kts,
  app/src/main/java/com/weekssa/opraeqforuapp/MainActivity.kt,
  app/src/main/java/com/weekssa/opraeqforuapp/ui/EqLibraryActions.kt,
  app/src/main/java/com/weekssa/opraeqforuapp/ui/EqLibraryApp.kt,
  app/src/main/java/com/weekssa/opraeqforuapp/ui/screens/MyDacRootScreen.kt,
  app/src/main/java/com/weekssa/opraeqforuapp/ui/screens/SettingsScreen.kt,
  app/src/main/java/com/weekssa/opraeqforuapp/ui/screens/MyEqsHomeScreen.kt,
  app/src/main/java/com/weekssa/opraeqforuapp/ui/screens/ManagedHeadphoneDetailScreen.kt,
  app/src/main/res/values/strings.xml,
  app/src/main/java/com/weekssa/opraeqforuapp/domain/ew300/Ew300Protocol.kt,
  app/src/main/java/com/weekssa/opraeqforuapp/domain/ew300/Ew300CapabilityProfile.kt,
  app/src/main/java/com/weekssa/opraeqforuapp/domain/ew300/Ew300EditorApply.kt,
  app/src/main/java/com/weekssa/opraeqforuapp/domain/ew300/Ew300Flasher.kt,
  app/src/main/java/com/weekssa/opraeqforuapp/domain/ew300/Ew300TransactionCoordinator.kt,
  app/src/main/java/com/weekssa/opraeqforuapp/domain/ew300/Ew300OperationTrace.kt,
  app/src/main/java/com/weekssa/opraeqforuapp/domain/ew300/Ew300GainQualification.kt,
  app/src/main/java/com/weekssa/opraeqforuapp/domain/ew300/Ew300DeviceControls.kt,
  app/src/main/java/com/weekssa/opraeqforuapp/domain/ew300/Ew300VolumeControl.kt,
  app/src/main/java/com/weekssa/opraeqforuapp/domain/ew300/Ew300UacDescriptor.kt,
  app/src/main/java/com/weekssa/opraeqforuapp/domain/ew300/Ew300UacStatus.kt,
  app/src/main/java/com/weekssa/opraeqforuapp/domain/ew300/Ew300ShelfEnvelope.kt,
  app/src/main/java/com/weekssa/opraeqforuapp/domain/ew300/Ew300CapturedEq.kt,
  app/src/main/java/com/weekssa/opraeqforuapp/domain/ew300/Ew300HardwareEqMatch.kt,
  app/src/main/java/com/weekssa/opraeqforuapp/domain/hardware/HardwareEqDeviceSpecs.kt,
  app/src/main/java/com/weekssa/opraeqforuapp/domain/dac/HardwareEqEditor.kt,
  app/src/main/java/com/weekssa/opraeqforuapp/domain/dac/HardwareEqSnapshotFactory.kt,
  app/src/main/java/com/weekssa/opraeqforuapp/domain/dac/DacCapabilityCatalog.kt,
  app/src/main/java/com/weekssa/opraeqforuapp/data/dac/Ew300ReconnectGate.kt,
  app/src/main/java/com/weekssa/opraeqforuapp/data/dac/Ew300MutationExecutor.kt,
  app/src/main/java/com/weekssa/opraeqforuapp/data/dac/DacSessionRepository.kt,
  app/src/main/java/com/weekssa/opraeqforuapp/data/hardware/HardwareEqRepository.kt,
  app/src/main/java/com/weekssa/opraeqforuapp/data/library/SavedEqRepository.kt,
  app/src/main/java/com/weekssa/opraeqforuapp/data/kt02h20/Ew300GainStatePreferences.kt,
  app/src/main/java/com/weekssa/opraeqforuapp/data/kt02h20/AndroidEw300UsbTransport.kt,
  app/src/main/java/com/weekssa/opraeqforuapp/data/kt02h20/AndroidKt02h20HidSession.kt,
  app/src/main/java/com/weekssa/opraeqforuapp/ui/EqLibraryViewModel.kt,
  app/src/main/java/com/weekssa/opraeqforuapp/ui/MyDacEditorUiState.kt,
  app/src/main/java/com/weekssa/opraeqforuapp/ui/Ew300PlaybackGainUiState.kt,
  app/src/main/java/com/weekssa/opraeqforuapp/ui/screens/Ew300MyDacContent.kt,
  app/src/main/java/com/weekssa/opraeqforuapp/ui/screens/MyDacEqEditorScreen.kt,
  app/src/main/java/com/weekssa/opraeqforuapp/ui/screens/DacDeviceStatus.kt,
  app/src/main/java/com/weekssa/opraeqforuapp/EqLibraryDependencyFactory.kt,
  app/src/test/java/com/weekssa/opraeqforuapp/domain/ew300/Ew300ProtocolTest.kt,
  app/src/test/java/com/weekssa/opraeqforuapp/domain/ew300/Ew300FlasherTest.kt,
  app/src/test/java/com/weekssa/opraeqforuapp/ui/Ew300LibraryFlashRoutingTest.kt,
  app/src/androidTest/java/com/weekssa/opraeqforuapp/ui/screens/Ew300DeviceStatusTest.kt,
  app/src/test/java/com/weekssa/opraeqforuapp/domain/ew300/Ew300EditorApplyTest.kt,
  app/src/test/java/com/weekssa/opraeqforuapp/domain/ew300/Ew300CapabilityProfileTest.kt,
  app/src/test/java/com/weekssa/opraeqforuapp/domain/ew300/Ew300CapabilityBatchTest.kt,
  app/src/test/java/com/weekssa/opraeqforuapp/domain/ew300/Ew300DeviceControlsTest.kt,
  app/src/test/java/com/weekssa/opraeqforuapp/domain/ew300/Ew300OperationTraceTest.kt,
  app/src/test/java/com/weekssa/opraeqforuapp/domain/ew300/Ew300QualificationExportTest.kt,
  app/src/test/java/com/weekssa/opraeqforuapp/domain/ew300/Ew300PersistenceQualificationTest.kt,
  app/src/test/java/com/weekssa/opraeqforuapp/domain/ew300/Ew300CapturedEqTest.kt,
  app/src/test/java/com/weekssa/opraeqforuapp/domain/ew300/Ew300HardwareEqMatchTest.kt,
  app/src/test/java/com/weekssa/opraeqforuapp/domain/ew300/Ew300VolumeControlTest.kt,
  app/src/test/java/com/weekssa/opraeqforuapp/domain/ew300/Ew300UacDescriptorTest.kt,
  app/src/test/java/com/weekssa/opraeqforuapp/domain/ew300/Ew300ShelfMappingTest.kt,
  app/src/test/java/com/weekssa/opraeqforuapp/domain/kt02h20/Ew300ShelfCorpusRegressionTest.kt,
  app/src/test/java/com/weekssa/opraeqforuapp/data/dac/Ew300MutationExecutorTest.kt,
  app/src/test/java/com/weekssa/opraeqforuapp/data/dac/Ew300ReconnectGateTest.kt,
  app/src/test/java/com/weekssa/opraeqforuapp/ui/screens/Ew300OperationStatusTest.kt,
  app/src/test/java/com/weekssa/opraeqforuapp/domain/dac/HardwareEqEditorTest.kt,
  app/src/test/java/com/weekssa/opraeqforuapp/domain/hardware/HardwareEqDeviceSpecsTest.kt,
  app/src/androidTest/java/com/weekssa/opraeqforuapp/ui/screens/Ew300DeviceStatusTest.kt,
  app/src/androidTest/java/com/weekssa/opraeqforuapp/ui/screens/Ew300ParityFlowTest.kt,
  app/src/androidTest/java/com/weekssa/opraeqforuapp/ui/screens/Ew300FullParityFlowTest.kt,
  tools/ew300_readonly_probe.py,
  tools/test_ew300_readonly_probe.py,
  tools/ew300_reversible_shelf_probe.py,
  tools/ew300_uac_probe.c,
  tools/test_ew300_reversible_shelf_probe.py,
  tools/ew300_uac_probe.swift,
  tools/verify_ew300_parity_readiness.py,
  tools/verify_ew300_parity_review.py,
  docs/EW300_DSP_PROTOCOL_NOTES.md,
  docs/EW300_CAPABILITY_MATRIX.md,
  docs/EW300_VALIDATION_LEDGER.md,
  docs/CHATGPT_PROJECT_RUNBOOK.md,
  docs/PROJECT_STATE.md

## Gates

- [ ] 1. Goal lock and baseline
  CHECK: ~/Documents/Codex/Codex-Usage-Policies/tools/selftest >/dev/null && /Users/stephenweeks/Documents/Codex/Codex-Usage-Policies/tools/scope-guard --root . && test -s docs/planning/ew300-ja11-parity/GOALS.md && rg -q 'G5' docs/planning/ew300-ja11-parity/GOALS.md && ./tools/codex-android :app:testDebugUnitTest --tests '*Ew300*'
  EXPECT: Workflow selftest passes; the sealed contract matches every changed path; GOALS.md contains all five goals; the pre-change and current EW300 baseline is green.

- [ ] 2. Volume, sample rate, shelves
  CHECK: ./tools/codex-android :app:testDebugUnitTest && ./tools/codex-android :app:testReleaseUnitTest --tests '*Ew300*'
  EXPECT: 0–60 volume boundaries and stereo bytes pass; UAC1 descriptor/GET_CUR builder tests pass; shelf type 3/4 golden, capture, readback and Save/restore tests pass; runtime zero-Save and explicit one-Save invariants remain green.

- [ ] 3. Mic/UAC evidence and UI
  CHECK: python3 -m unittest tools.test_ew300_readonly_probe >/dev/null && python3 -m unittest tools.test_ew300_reversible_shelf_probe >/dev/null && ANDROID_SERIAL=emulator-5554 ./tools/codex-android :app:connectedEw300ParityAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.weekssa.opraeqforuapp.ui.screens.Ew300FullParityFlowTest && ./tools/codex-android :app:lintDebug :app:assembleDebug :app:assembleEw300Parity :app:assembleEw300ParityAndroidTest
  EXPECT: Mic/UAC rows show either a real control or an exact evidence-backed blocked state; the five-goal Compose flow passes on the explicit emulator; lint and all builds succeed.

- [ ] 4. Reversible Mac and readiness
  CHECK: python3 tools/ew300_reversible_shelf_probe.py --self-test && ./tools/codex-android :app:assembleEw300Parity && python3 tools/verify_ew300_parity_readiness.py --apk-dir app/build/outputs/apk/ew300Parity --manifest docs/implementation/ew300-ja11-parity-20261105/READINESS.md
  EXPECT: The reversible probe self-test proves baseline/write/readback/restore-only framing; the live Mac reversible shelf result is recorded in TEST_EVIDENCE_LOG.md; the APK package/version/hash/signer and five-goal readiness manifest match.

- [ ] 5. Independent review
  CHECK: python3 tools/verify_ew300_parity_review.py docs/implementation/ew300-ja11-parity-20261105/GATES.md
  EXPECT: Exactly one fresh gpt-6-luna reviewer from the other account reviews the five-goal candidate; findings are closed and one findings-only confirmation pass is recorded.

## Not permitted

- Git push, pull request, merge, tag, release, publication, signing-policy change, or any public hardware-support claim
- Firmware flashing, bootloader writes, erase, calibration, recovery, 0x08, 0x43, 0x54 identity writes, arbitrary register console, or USB identity writes
- Blind register sweeps or writes without a stated hypothesis and a baseline/restore path
- A new navigation root or a duplicate editor, session, protocol stack, or device-specific My DAC shell
- Canonical EQ schema changes, Room migrations, output-registry ownership changes, or My EQs ownership changes
- Weakening tests or validation, silencing failures, retrying an uncertain Save, or treating emulator/Mac-read evidence as Pixel qualification
- Editing the saved source checkout or any existing worktree

## Review

verdict: PASS
reviewer: gpt-6-luna
account: other
reviewed_head: 66f9b50ff92d8b48d19f5b340176c52f5e739e3a
reviewed_code_head: 6c276b098a92e4bf169f06abe410e6ea99821eac
review_scope: Pixel-validated APK 487445cb; H14 includes the strict UAC claim/restore fix
ran: full review at 0800e712; fixes at 4a87ce9b/66af7a03; findings-only confirmations at b0c8ded9 and 6c276b09
confirmation: PASS
findings:
- All prior findings closed. UAC1 sample-rate wIndex uses endpoint low byte 0x01; exact-device GET_CUR returned 48000/80BB00. Mac UAC probe is read-only. Shelf probe restores in finally with fault injection that reaches the restoration path. Microphone mute is a real switch; unknown readback is Not read. Out-of-scale volume is Unmapped. Volume evidence records file/internal hashes. Same reviewer confirmed PASS.
