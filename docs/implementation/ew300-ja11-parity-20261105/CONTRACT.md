# EW300 / JA11 parity — machine-readable task contract

Accepted plan: `PLAN.md`. This block is consumed by `tools/seed-task`; it is not part of the approved plan text.

```json
{
  "slug": "ew300-ja11-parity-20261105",
  "outcome": "Local experimental SIMGOT EW300 / JA11 My DAC parity build: capability-correct Peak editor, runtime Apply and output gain with zero Save, explicit one-Save runtime persistence, truthful saved-session state, read-only About, dormant gated shelf research, separate debug-derived package, complete evidence and one independent review; physical qualification pending.",
  "base": "180f4a962a53d22d294311121dd49d92a570d89a",
  "turns": 12,
  "owns": [
    "GATES.md",
    "docs/implementation/ew300-ja11-parity-20261105/PLAN.md",
    "docs/implementation/ew300-ja11-parity-20261105/CONTRACT.md",
    "docs/implementation/ew300-ja11-parity-20261105/GATES.md",
    "docs/implementation/ew300-ja11-parity-20261105/GATES.md.sha",
    "docs/implementation/ew300-ja11-parity-20261105/PROGRESS.md",
    "docs/implementation/ew300-ja11-parity-20261105/READINESS.md",
    "docs/planning/ew300-ja11-parity/README.md",
    "docs/planning/ew300-ja11-parity/BRIEF.md",
    "docs/planning/ew300-ja11-parity/EXECUTION_PROMPT.md",
    "docs/planning/ew300-ja11-parity/FUNCTIONAL_MAPPING.md",
    "docs/planning/ew300-ja11-parity/PLANNING_PROMPT.md",
    "docs/planning/ew300-ja11-parity/PROGRESS_TEMPLATE.md",
    "docs/planning/ew300-ja11-parity/REVIEW_PACKET.md",
    "docs/planning/ew300-ja11-parity/RESEARCH_SOURCES.md",
    "docs/planning/ew300-ja11-parity/TEST_EVIDENCE_LOG.md",
    "app/build.gradle.kts",
    "app/src/main/java/com/weekssa/opraeqforuapp/MainActivity.kt",
    "app/src/main/java/com/weekssa/opraeqforuapp/ui/EqLibraryActions.kt",
    "app/src/main/java/com/weekssa/opraeqforuapp/ui/EqLibraryApp.kt",
    "app/src/main/java/com/weekssa/opraeqforuapp/ui/screens/MyDacRootScreen.kt",
    "app/src/main/res/values/strings.xml",
    "app/src/main/java/com/weekssa/opraeqforuapp/domain/ew300/Ew300Protocol.kt",
    "app/src/main/java/com/weekssa/opraeqforuapp/domain/ew300/Ew300CapabilityProfile.kt",
    "app/src/main/java/com/weekssa/opraeqforuapp/domain/ew300/Ew300EditorApply.kt",
    "app/src/main/java/com/weekssa/opraeqforuapp/domain/ew300/Ew300Flasher.kt",
    "app/src/main/java/com/weekssa/opraeqforuapp/domain/ew300/Ew300TransactionCoordinator.kt",
    "app/src/main/java/com/weekssa/opraeqforuapp/domain/ew300/Ew300OperationTrace.kt",
    "app/src/main/java/com/weekssa/opraeqforuapp/domain/ew300/Ew300GainQualification.kt",
    "app/src/main/java/com/weekssa/opraeqforuapp/domain/ew300/Ew300DeviceControls.kt",
    "app/src/main/java/com/weekssa/opraeqforuapp/domain/hardware/HardwareEqDeviceSpecs.kt",
    "app/src/main/java/com/weekssa/opraeqforuapp/domain/dac/HardwareEqEditor.kt",
    "app/src/main/java/com/weekssa/opraeqforuapp/domain/dac/HardwareEqSnapshotFactory.kt",
    "app/src/main/java/com/weekssa/opraeqforuapp/domain/dac/DacCapabilityCatalog.kt",
    "app/src/main/java/com/weekssa/opraeqforuapp/data/dac/Ew300ReconnectGate.kt",
    "app/src/main/java/com/weekssa/opraeqforuapp/data/dac/Ew300MutationExecutor.kt",
    "app/src/main/java/com/weekssa/opraeqforuapp/data/dac/DacSessionRepository.kt",
    "app/src/main/java/com/weekssa/opraeqforuapp/data/hardware/HardwareEqRepository.kt",
    "app/src/main/java/com/weekssa/opraeqforuapp/data/kt02h20/Ew300GainStatePreferences.kt",
    "app/src/main/java/com/weekssa/opraeqforuapp/data/kt02h20/AndroidEw300UsbTransport.kt",
    "app/src/main/java/com/weekssa/opraeqforuapp/ui/EqLibraryViewModel.kt",
    "app/src/main/java/com/weekssa/opraeqforuapp/ui/MyDacEditorUiState.kt",
    "app/src/main/java/com/weekssa/opraeqforuapp/ui/Ew300PlaybackGainUiState.kt",
    "app/src/main/java/com/weekssa/opraeqforuapp/ui/screens/Ew300MyDacContent.kt",
    "app/src/main/java/com/weekssa/opraeqforuapp/ui/screens/MyDacEqEditorScreen.kt",
    "app/src/main/java/com/weekssa/opraeqforuapp/ui/screens/DacDeviceStatus.kt",
    "app/src/main/java/com/weekssa/opraeqforuapp/EqLibraryDependencyFactory.kt",
    "app/src/test/java/com/weekssa/opraeqforuapp/domain/ew300/Ew300ProtocolTest.kt",
    "app/src/test/java/com/weekssa/opraeqforuapp/domain/ew300/Ew300FlasherTest.kt",
    "app/src/test/java/com/weekssa/opraeqforuapp/domain/ew300/Ew300EditorApplyTest.kt",
    "app/src/test/java/com/weekssa/opraeqforuapp/domain/ew300/Ew300CapabilityProfileTest.kt",
    "app/src/test/java/com/weekssa/opraeqforuapp/domain/ew300/Ew300CapabilityBatchTest.kt",
    "app/src/test/java/com/weekssa/opraeqforuapp/domain/ew300/Ew300DeviceControlsTest.kt",
    "app/src/test/java/com/weekssa/opraeqforuapp/domain/ew300/Ew300OperationTraceTest.kt",
    "app/src/test/java/com/weekssa/opraeqforuapp/domain/ew300/Ew300QualificationExportTest.kt",
    "app/src/test/java/com/weekssa/opraeqforuapp/domain/ew300/Ew300PersistenceQualificationTest.kt",
    "app/src/test/java/com/weekssa/opraeqforuapp/domain/ew300/Ew300CapturedEqTest.kt",
    "app/src/test/java/com/weekssa/opraeqforuapp/domain/ew300/Ew300HardwareEqMatchTest.kt",
    "app/src/test/java/com/weekssa/opraeqforuapp/domain/kt02h20/Ew300ShelfCorpusRegressionTest.kt",
    "app/src/test/java/com/weekssa/opraeqforuapp/data/dac/Ew300MutationExecutorTest.kt",
    "app/src/test/java/com/weekssa/opraeqforuapp/data/dac/Ew300ReconnectGateTest.kt",
    "app/src/test/java/com/weekssa/opraeqforuapp/ui/screens/Ew300OperationStatusTest.kt",
    "app/src/test/java/com/weekssa/opraeqforuapp/domain/dac/HardwareEqEditorTest.kt",
    "app/src/test/java/com/weekssa/opraeqforuapp/domain/hardware/HardwareEqDeviceSpecsTest.kt",
    "app/src/androidTest/java/com/weekssa/opraeqforuapp/ui/screens/Ew300DeviceStatusTest.kt",
    "app/src/androidTest/java/com/weekssa/opraeqforuapp/ui/screens/Ew300ParityFlowTest.kt",
    "tools/ew300_readonly_probe.py",
    "tools/test_ew300_readonly_probe.py",
    "tools/verify_ew300_parity_readiness.py",
    "tools/verify_ew300_parity_review.py",
    "docs/EW300_DSP_PROTOCOL_NOTES.md",
    "docs/EW300_CAPABILITY_MATRIX.md",
    "docs/EW300_VALIDATION_LEDGER.md",
    "docs/CHATGPT_PROJECT_RUNBOOK.md",
    "docs/PROJECT_STATE.md"
  ],
  "excluded": [
    "Git push, pull request, merge, tag, release, publication, signing-policy change, or any public hardware-support claim",
    "Any EW300 hardware write, Save, Reset, Restore, power cycle, or DAC relocation during this local readiness run",
    "Firmware, bootloader, erase, calibration, recovery, 0x08, 0x43, 0x54, or arbitrary register writes; writes outside the product allowlist; USB identity writes",
    "Ordinary native shelf editor exposure before the owner listening qualification; shelf capture; source-derived native shelf Flash",
    "A new navigation root or a duplicate editor, session, protocol stack, or device-specific My DAC shell",
    "Canonical EQ schema changes, Room migrations, output-registry ownership changes, or My EQs ownership changes",
    "Weakening tests or validation, silencing failures, retrying an uncertain hardware mutation, or treating emulator/Mac-read evidence as physical qualification",
    "Editing the saved source checkout or any existing worktree"
  ],
  "gates": [
    {
      "name": "1. Baseline and contract",
      "check": "~/Documents/Codex/Codex-Usage-Policies/tools/selftest >/dev/null && /Users/stephenweeks/Documents/Codex/Codex-Usage-Policies/tools/scope-guard --root . && ./tools/codex-android :app:testDebugUnitTest --tests '*Ew300*' && rg -q 'T-000' docs/planning/ew300-ja11-parity/TEST_EVIDENCE_LOG.md",
      "expect": "Workflow selftest passes; the execution-start model/effort is recorded; base 180f4a96 resolves; the sealed contract matches every changed path; and the pre-change EW300 suite result is preserved in T-000."
    },
    {
      "name": "2. Protocol and transactions",
      "check": "./tools/codex-android :app:testDebugUnitTest && ./tools/codex-android :app:testReleaseUnitTest --tests '*Ew300*'",
      "expect": "Positive RX and signed/type/stereo golden fixtures pass; APPLY_RUNTIME and SET_GAIN_RUNTIME send zero Save; SAVE_RUNTIME sends exactly one Save and verifies final readback; stale baseline, identity mismatch, cancellation, uncertain Save and no-replay negatives fail closed; release policy keeps experimental behavior unreachable."
    },
    {
      "name": "3. UI and build isolation",
      "check": "ANDROID_SERIAL=emulator-5554 ./tools/codex-android :app:connectedEw300ParityAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.weekssa.opraeqforuapp.ui.screens.Ew300ParityFlowTest && ./tools/codex-android :app:lintDebug :app:assembleDebug :app:assembleEw300Parity :app:assembleEw300ParityAndroidTest",
      "expect": "The explicit emulator serial only runs the Compose/fake-transport flow; band rows, capability-driven type chips, runtime-only and saved-session states, Save, About, and error recovery are reachable; variant policy keeps the parity package separate and release flags false; lint and all named builds succeed."
    },
    {
      "name": "4. Readiness and evidence",
    "check": "python3 tools/ew300_readonly_probe.py --self-test && ./tools/codex-android :app:assembleEw300Parity && python3 tools/verify_ew300_parity_readiness.py --apk-dir app/build/outputs/apk/ew300Parity --manifest docs/implementation/ew300-ja11-parity-20261105/READINESS.md",
      "expect": "The fake-HID self-test proves only 0x52 reads can be emitted; the parity APK package, version, SHA-256, signer and manifest match; the readiness record is complete; FUNCTIONAL_MAPPING.md, TEST_EVIDENCE_LOG.md, RESEARCH_SOURCES.md and maintained EW300 docs cross-link the exact candidate and explicitly say physical qualification is pending."
    },
    {
      "name": "5. Independent review",
      "check": "python3 tools/verify_ew300_parity_review.py docs/implementation/ew300-ja11-parity-20261105/GATES.md",
      "expect": "Exactly one fresh gpt-6-luna reviewer from the other account has a recorded PASS on the exact candidate; every finding is closed; a findings-only confirmation pass by the same reviewer confirms the fixes; no research fan-out or second review occurred."
    }
  ]
}
```

