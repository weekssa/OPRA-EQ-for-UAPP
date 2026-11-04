# FiiO JA11 tuning model — private My DAC prototype

Status: **OWNER-APPROVED 2026-11-03.** The owner accepted this plan in the
planning handoff ("PLEASE IMPLEMENT THIS PLAN") and asked for autonomous
progress until physical participation is genuinely required. This file is the
durable record of that approved plan and the input to `tools/seed-task`.

Base: `30054466fa110adfaa9f9b2058eb8af700fbc56d` (verified local and remote
`main`, v0.9.0 stable).

## Outcome

Deliver one of two honest results for the JA11 community "DCR / Noise Gate"
tuning interest: a firmware-qualified runtime tuning capability, or a tested,
installable prototype My DAC tuning model with precise limits. Only the second
is reachable with current evidence.

Evidence inspected before this plan:

- The maintained JA11 protocol (VID/PID `2972:0101`/`2972:0102`, HID report
  `0x02`, commands `0x15` bands, `0x17` global gain, `0x18` Apply, `0x19`
  preset-valued Save, `0x04` vendor reconstruction filter) contains **no**
  DCR, DRC-compander, expander, AGC, or noise-gate command.
- `gxcreator/ktmicro-tools@39e620d8cd68c4afb711b91153b4ae91042adc32`
  documents live NoiseGate registers 71-73 and limiter 78-79 on an 11-byte
  report `0x4B` protocol at `31B2:0111`. That is a different transport and
  different device. Its DRC compander/expander/AGC handlers are documented
  stubs. Nothing in it establishes JA11 firmware behavior.
- Public community sources describe editing DCR/Noise Gate in KT tools and
  overwriting the FiiO 2.2 image, then flashing firmware. They describe an
  audible character ("warmer/smoother", tick fixes) and publish no raw values.
  They document a firmware workflow, not a runtime API.
- Firmware label equivalence between FiiO 2.20, community "V2.2" and the
  Ellyn/Ellyn_GP variants is not proven, and the meaning of "DCR" versus "DRC"
  remains unresolved.

Therefore hardware write enablement is not authorized by this task and no
firmware action is permitted. The software model, UI, tests and documentation
are in scope.

## Design decisions

1. **Capability truth first.** The JA11 runtime read of the installed tuning is
   unknown: the app has no read path. Unknown is presented as unknown, never as
   "Off". Named tuning records are provenance only and are never claimed as the
   installed state.
2. **Simulated transport behind a private flag.** The prototype lives in a
   new `Ja11TuningPrototype` surface gated by
   `BuildConfig.JA11_TUNING_PROTOTYPE_ENABLED`, enabled only for `debug` and
   `privateBeta`. Production `release`/`beta` builds keep it `false`, so no
   production UI or hardware path appears. The prototype installs side by side
   as `com.weekssa.opraeqforuapp.v090beta`.
3. **Same transaction discipline as real hardware.** The prototype reuses the
   repository's exclusive operation gate shape (serialized mutation), a
   session-generation fence, a verified readback before success, and no
   replay after an uncertain completion. It sends nothing to USB.
4. **Persistence is simulated and labelled.** The model distinguishes
   session-only retention, saved retention, reset by simulated power cycle, and
   unknown. It never equates EQ Save (`0x19`) with tuning persistence.
5. **Placement.** One row inside the existing My DAC DEVICE "Audio" group,
   next to device volume / EQ program. No new root, tab or navigation context.
   The dialog follows existing row/dialog patterns, 48dp targets, one coherent
   TalkBack announcement per row, and M3 switch only for the genuinely binary
   noise-gate enable in the simulated model.

## Acceptance shape

Gates are in `docs/implementation/ja11-tuning-prototype-20261103/GATES.md`
after sealing. The final independent review is exactly one fresh
`gpt-6-luna` review of the exact candidate head, recorded before it is
spawned and reported in `docs/implementation/ja11-tuning-prototype-20261103/GATES.md`.

## Task contract

```json
{
  "slug": "ja11-tuning-prototype-20261103",
  "outcome": "Private JA11 My DAC tuning prototype: truthful capability model, simulated transport through the existing session gate, verified readback and no-replay behaviour, production flag left off.",
  "base": "30054466fa110adfaa9f9b2058eb8af700fbc56d",
  "turns": 6,
  "owns": [
    "GATES.md",
    "docs/implementation/ja11-tuning-prototype-20261103/PLAN.md",
    "docs/implementation/ja11-tuning-prototype-20261103/GATES.md",
    "docs/implementation/ja11-tuning-prototype-20261103/GATES.md.sha",
    "app/build.gradle.kts",
    "app/src/main/java/com/weekssa/opraeqforuapp/domain/fiio/Ja11TuningModel.kt",
    "app/src/main/java/com/weekssa/opraeqforuapp/domain/fiio/Ja11TuningController.kt",
    "app/src/main/java/com/weekssa/opraeqforuapp/domain/fiio/SimulatedJa11TuningTransport.kt",
    "app/src/main/java/com/weekssa/opraeqforuapp/ui/Ja11TuningUiState.kt",
    "app/src/main/java/com/weekssa/opraeqforuapp/ui/EqLibraryViewModel.kt",
    "app/src/main/java/com/weekssa/opraeqforuapp/ui/screens/DacDeviceStatus.kt",
    "app/src/main/java/com/weekssa/opraeqforuapp/ui/screens/FiioJa11MyDacContent.kt",
    "app/src/main/java/com/weekssa/opraeqforuapp/ui/screens/Ja11TuningPrototype.kt",
    "app/src/test/java/com/weekssa/opraeqforuapp/domain/fiio/Ja11TuningControllerTest.kt",
    "app/src/test/java/com/weekssa/opraeqforuapp/domain/fiio/SimulatedJa11TuningTransportTest.kt",
    "app/src/test/java/com/weekssa/opraeqforuapp/ui/Ja11TuningUiStateTest.kt",
    "app/src/androidTest/java/com/weekssa/opraeqforuapp/ui/screens/Ja11TuningPrototypeTest.kt",
    "docs/FIIO_JA11_CAPABILITY_MATRIX.md",
    "docs/CHATGPT_PROJECT_RUNBOOK.md",
    "docs/PROJECT_STATE.md",
    "docs/releases/0.9.0-beta.4.md",
    "CHANGELOG.md",
    "app/src/main/java/com/weekssa/opraeqforuapp/MainActivity.kt",
    "app/src/main/java/com/weekssa/opraeqforuapp/ui/EqLibraryActions.kt",
    "app/src/main/java/com/weekssa/opraeqforuapp/ui/EqLibraryApp.kt",
    "app/src/main/java/com/weekssa/opraeqforuapp/ui/screens/MyDacRootScreen.kt",
    "app/src/main/java/com/weekssa/opraeqforuapp/data/kt02h20/AndroidFiioJa11UsbTransport.kt",
    "app/src/main/java/com/weekssa/opraeqforuapp/data/kt02h20/AndroidKt02h20HidSession.kt",
    "docs/FIIO_JA11_PROTOCOL_NOTES.md",
    "app/src/main/java/com/weekssa/opraeqforuapp/data/dac/DacSessionRepository.kt",
    "app/src/main/java/com/weekssa/opraeqforuapp/data/hardware/HardwareEqRepository.kt",
    "app/src/main/java/com/weekssa/opraeqforuapp/domain/kt02h20/Ja11DcrTuning.kt",
    "app/src/main/java/com/weekssa/opraeqforuapp/ui/Ja11DcrTuningUiState.kt",
    "app/src/test/java/com/weekssa/opraeqforuapp/ui/Ja11DcrTuningUiStateTest.kt",
    "app/src/androidTest/java/com/weekssa/opraeqforuapp/ui/screens/BlackPearlDeviceResetLifecycleTest.kt",
    "app/src/androidTest/java/com/weekssa/opraeqforuapp/ui/screens/FiioJa11MyDacContentTest.kt",
    "app/src/androidTest/java/com/weekssa/opraeqforuapp/ui/screens/UiModernizationFlowsTest.kt",
    "app/src/main/java/com/weekssa/opraeqforuapp/ui/screens/DeviceOperationStatus.kt",
    "app/src/test/java/com/weekssa/opraeqforuapp/ui/screens/DeviceOperationStatusTest.kt"
  ],
  "excluded": [
    "JA11 hardware writes, writes of any tuning register, Apply or Save on physical hardware",
    "Firmware flashing, KT_USB_APP/KT_BOOT_TOOL integration, firmware image handling or cross-flash",
    "A generic or arbitrary register/command console",
    "Transplanting ktmicro-tools report 0x4B frames or 31B2:0111 register numbers onto JA11",
    "Inventing DCR/DRC/noise-gate register ranges, units or wire fixtures",
    "Canonical EQ, output registry, device adapter or My EQs ownership changes",
    "New root destinations or changes to My DAC navigation",
    "New third-party dependencies, signing changes, merge, PR creation or release publication"
  ],
  "gates": [
    {
      "name": "Approval and scope",
      "check": "test -s docs/implementation/ja11-tuning-prototype-20261103/GATES.md.sha && /Users/stephenweeks/Documents/Codex/Codex-Usage-Policies/tools/scope-guard --root .",
      "expect": "The seal matches and every changed path is inside OWNS."
    },
    {
      "name": "Capability and transaction truth",
      "check": "./tools/codex-android :app:testDebugUnitTest --tests 'com.weekssa.opraeqforuapp.domain.fiio.Ja11TuningControllerTest' --tests 'com.weekssa.opraeqforuapp.domain.fiio.SimulatedJa11TuningTransportTest' --tests 'com.weekssa.opraeqforuapp.ui.Ja11TuningUiStateTest'",
      "expect": "Unknown never renders as Off; unsupported production capability stays unsupported; stale generation, denied permission, mismatch, timeout and disconnect before/during/after write fail closed; one write and no replay; persistence states stay distinct from EQ Save."
    },
    {
      "name": "Prototype UI semantics",
      "check": "./tools/codex-android :app:connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.weekssa.opraeqforuapp.ui.screens.Ja11TuningPrototypeTest",
      "expect": "Row opens the prototype dialog, simulated apply reports verifying then verified, no USB write action exists, and accessibility/large-text assertions pass on an emulator."
    },
    {
      "name": "Coherent build gates",
      "check": "./tools/codex-android :app:testDebugUnitTest :app:lintDebug :app:assembleDebug :app:assemblePrivateBeta :app:assembleRelease --console=plain && bash tools/verify-r8-mapping.sh",
      "expect": "All tasks succeed, and the release variant keeps BuildConfig.JA11_TUNING_PROTOTYPE_ENABLED false with no tuning row reachable."
    },
    {
      "name": "Documentation and candidate identity",
      "check": "git diff --check && rg -n 'JA11_TUNING_PROTOTYPE_ENABLED|ja11-tuning-prototype-20261103' app/build.gradle.kts docs/FIIO_JA11_CAPABILITY_MATRIX.md docs/CHATGPT_PROJECT_RUNBOOK.md docs/PROJECT_STATE.md",
      "expect": "Capability matrix, runbook and project state record the exact limits, prototype scope and next owner action; no production support claim is added."
    }
  ]
}
```

## Execution record (2026-11-03)

Candidate commit: `2432fd74cacff843a90cb8eed9c5fe322f35d31b` plus the review corrections recorded
in the commit that adds this section. Branch `codex/ja11-tuning-prototype`, worktree
`/Users/stephenweeks/.codex/worktrees/9af0/OPRA-EQ-development`, base
`30054466fa110adfaa9f9b2058eb8af700fbc56d`.

Commands and results, all with `./tools/codex-android`:

- focused new JVM tests (controller, simulated transport, UI state): 29 tests, 0 failures.
- `:app:testDebugUnitTest`: 863 tests, 0 failures, 0 skipped.
- `:app:lintDebug :app:assembleDebug :app:assemblePrivateBeta :app:assembleRelease`: BUILD SUCCESSFUL.
- `bash tools/verify-r8-mapping.sh`: R8 mapping verified.
- generated `BuildConfig`: `JA11_TUNING_PROTOTYPE_ENABLED=false` for release, `true` for debug and
  privateBeta.
- `:app:connectedDebugAndroidTest`-equivalent by explicit serial (`adb -s emulator-5602`):
  `com.weekssa.opraeqforuapp.ui.screens.Ja11TuningPrototypeTest` 8/8 passed on an isolated API 36
  AVD (`opra_api36`, port 5602).
  The contract's gate 3 command would target every attached device, including the physical Pixel 9
  visible on wireless ADB, so it was executed as an explicit-serial instrumentation run instead.
- privateBeta APK installed and launched on isolated API 36 (port 5602), API 35 (port 5606) and
  API 26 (port 5604) AVDs; process alive, no crash-buffer entry. The pre-existing emulator-only
  `com.weekssa.opraeqforuapp.v090beta` installs had a different signer and were uninstalled first.
- the physical Pixel 9 visible on wireless ADB was never targeted; every device command named an
  emulator serial.

Private prototype APK: `EQ-Library-ja11-tuning-prototype-phone.apk`, build type `ja11Tuning`,
package `com.weekssa.opraeqforuapp.ja11tuning`, versionName `0.9.0-tuningproto`, versionCode `20`,
SHA-256 `db9201693528704d8f6578ad0af0f8ec30f5a1b6bce807e5748e75edf2d6f08e`, local test signer
SHA-256 `945614206b73258162a2e0722ddcf4a53ae3018652e0fb65d2b9bcc3e38521a5` (not the pinned release
signer). The earlier `privateBeta` build (`.v090beta`) could not be installed over the phone's
beta.3 without uninstalling it, so the dedicated build type was added.
Known-good rollback APK: public v0.9.0, SHA-256
`8930dfd4dac7bf97b68bbedb14e89f2cf88da0bcf927be10be8333038c0f8029`.

Screenshots (light, dark, verified and 1.5x system font scale) were captured from the API 36 AVD
into the local owner-package directory outside the repository. The pre-review APK digest
`588af702…` is superseded by the corrected build above.

Implementation note: the simulated controller is owned by the prototype composable rather than the
ViewModel, because the surface owns no USB session and must not touch the production dependency
graph. Serialization, the session fence, readback and no-replay semantics live in the tested
controller; the UI only stages and renders. State does not survive process death, which is
acceptable for a simulation and is disclosed here.

## Review

Exactly one fresh independent `gpt-6-luna` review (high effort) inspected candidate `2432fd7` and
returned **FAIL** with three P2 findings and one inconclusive note. No second review was spawned.
The findings and dispositions:

1. **Attempted values shown as last read.** `beginApply()` replaced the displayed level and gate
   before verification, so an uncertain disconnect could render the requested value as "Last read".
   Fixed: `beginApply` no longer touches verified values; a regression test asserts the row keeps the
   last verified value during apply and after an uncertain disconnect.
2. **No build-level write guard in the controller.** Fixed: `Ja11TuningController` now takes
   `prototypeEnabled = BuildConfig.JA11_TUNING_PROTOTYPE_ENABLED` and returns
   `Unsupported` from `apply` and `saveStartupDefaults` when false. A test proves a production
   build cannot write even through a simulated transport.
3. **Evidence record inconsistent with the sealed gate file.** The gate file is sealed by
   `seed-task`; editing it would invalidate the approval seal, so the execution record and verdict
   live here and this section is the authoritative evidence pointer. The runbook entry cites the
   same candidate.
4. **Inconclusive: rendered large text.** Resolved by capturing a dialog screenshot at a real 1.5x
   system font scale on the API 36 AVD; text reflows without clipping. The Compose `LocalDensity`
   override in the test does not affect dialog windows, so the system-scale capture is the evidence.

Residual limits: simulated behavior only; no JA11 read, write, firmware, acoustic or persistence
evidence; the physical Pixel 9 and every DAC remain untouched.

## Owner phone session (2026-11-03)

With the owner's "the pixel is ready for testing", the following was run on the physical Pixel 9
(Android 17) with the JA11 attached:

- read-only baseline: `com.weekssa.opraeqforuapp` is v0.9.0 / 20 and its on-device `base.apk`
  SHA-256 is `8930dfd4dac7bf97b68bbedb14e89f2cf88da0bcf927be10be8333038c0f8029`, the published APK;
  `com.weekssa.opraeqforuapp.v090beta` is 0.9.0-beta.3 / 17 and was left untouched.
- installed the separate `com.weekssa.opraeqforuapp.ja11tuning` package, opened My DAC, connected
  the existing USB session, and opened DEVICE. The Tuning row appears between EQ program and
  Startup defaults with "Prototype · simulated. No command is sent to JA11."
- selected Warmer and applied: the dialog reported "Simulated apply verified by readback" and
  session-only persistence; the row then showed "Warmer · gate off".
- rotated the device to landscape and back: the verified simulated value and open dialog survived.
- hardware state before and after: volume 60/60, EQ program Off; no crash-buffer entry. No DAC
  write, Apply, Save, firmware, microphone or UAC operation occurred.

Screenshots from the phone are in the local owner package (`phone/01-launch.png` …
`phone/10-final-device.png`). Orientation settings changed for the rotation check were restored.
