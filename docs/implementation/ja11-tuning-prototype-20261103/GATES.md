# Gates — ja11-tuning-prototype-20261103

outcome: Private JA11 My DAC tuning prototype: truthful capability model, simulated transport through the existing session gate, verified readback and no-replay behaviour, production flag left off.
base: 30054466fa110adfaa9f9b2058eb8af700fbc56d
turns: 6

OWNS: GATES.md,
  docs/implementation/ja11-tuning-prototype-20261103/PLAN.md,
  docs/implementation/ja11-tuning-prototype-20261103/GATES.md,
  docs/implementation/ja11-tuning-prototype-20261103/GATES.md.sha,
  app/build.gradle.kts,
  app/src/main/java/com/weekssa/opraeqforuapp/domain/fiio/Ja11TuningModel.kt,
  app/src/main/java/com/weekssa/opraeqforuapp/domain/fiio/Ja11TuningController.kt,
  app/src/main/java/com/weekssa/opraeqforuapp/domain/fiio/SimulatedJa11TuningTransport.kt,
  app/src/main/java/com/weekssa/opraeqforuapp/ui/Ja11TuningUiState.kt,
  app/src/main/java/com/weekssa/opraeqforuapp/ui/EqLibraryViewModel.kt,
  app/src/main/java/com/weekssa/opraeqforuapp/ui/screens/DacDeviceStatus.kt,
  app/src/main/java/com/weekssa/opraeqforuapp/ui/screens/FiioJa11MyDacContent.kt,
  app/src/main/java/com/weekssa/opraeqforuapp/ui/screens/Ja11TuningPrototype.kt,
  app/src/test/java/com/weekssa/opraeqforuapp/domain/fiio/Ja11TuningControllerTest.kt,
  app/src/test/java/com/weekssa/opraeqforuapp/domain/fiio/SimulatedJa11TuningTransportTest.kt,
  app/src/test/java/com/weekssa/opraeqforuapp/ui/Ja11TuningUiStateTest.kt,
  app/src/androidTest/java/com/weekssa/opraeqforuapp/ui/screens/Ja11TuningPrototypeTest.kt,
  docs/FIIO_JA11_CAPABILITY_MATRIX.md,
  docs/CHATGPT_PROJECT_RUNBOOK.md,
  docs/PROJECT_STATE.md

## Gates

- [ ] Approval and scope
  CHECK: test -s docs/implementation/ja11-tuning-prototype-20261103/GATES.md.sha && /Users/stephenweeks/Documents/Codex/Codex-Usage-Policies/tools/scope-guard --root .
  EXPECT: The seal matches and every changed path is inside OWNS.

- [ ] Capability and transaction truth
  CHECK: ./tools/codex-android :app:testDebugUnitTest --tests 'com.weekssa.opraeqforuapp.domain.fiio.Ja11TuningControllerTest' --tests 'com.weekssa.opraeqforuapp.domain.fiio.SimulatedJa11TuningTransportTest' --tests 'com.weekssa.opraeqforuapp.ui.Ja11TuningUiStateTest'
  EXPECT: Unknown never renders as Off; unsupported production capability stays unsupported; stale generation, denied permission, mismatch, timeout and disconnect before/during/after write fail closed; one write and no replay; persistence states stay distinct from EQ Save.

- [ ] Prototype UI semantics
  CHECK: ./tools/codex-android :app:connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.weekssa.opraeqforuapp.ui.screens.Ja11TuningPrototypeTest
  EXPECT: Row opens the prototype dialog, simulated apply reports verifying then verified, no USB write action exists, and accessibility/large-text assertions pass on an emulator.

- [ ] Coherent build gates
  CHECK: ./tools/codex-android :app:testDebugUnitTest :app:lintDebug :app:assembleDebug :app:assemblePrivateBeta :app:assembleRelease --console=plain && bash tools/verify-r8-mapping.sh
  EXPECT: All tasks succeed, and the release variant keeps BuildConfig.JA11_TUNING_PROTOTYPE_ENABLED false with no tuning row reachable.

- [ ] Documentation and candidate identity
  CHECK: git diff --check && rg -n 'JA11_TUNING_PROTOTYPE_ENABLED|ja11-tuning-prototype-20261103' app/build.gradle.kts docs/FIIO_JA11_CAPABILITY_MATRIX.md docs/CHATGPT_PROJECT_RUNBOOK.md docs/PROJECT_STATE.md
  EXPECT: Capability matrix, runbook and project state record the exact limits, prototype scope and next owner action; no production support claim is added.

## Not permitted

- JA11 hardware writes, writes of any tuning register, Apply or Save on physical hardware
- Firmware flashing, KT_USB_APP/KT_BOOT_TOOL integration, firmware image handling or cross-flash
- A generic or arbitrary register/command console
- Transplanting ktmicro-tools report 0x4B frames or 31B2:0111 register numbers onto JA11
- Inventing DCR/DRC/noise-gate register ranges, units or wire fixtures
- Canonical EQ, output registry, device adapter or My EQs ownership changes
- New root destinations or changes to My DAC navigation
- New third-party dependencies, signing changes, merge, PR creation or release publication

## Review

verdict: pending
ran: none
findings:
- none yet
