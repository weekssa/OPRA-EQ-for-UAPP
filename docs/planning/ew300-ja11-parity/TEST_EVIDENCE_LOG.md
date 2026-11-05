# Test evidence log — EW300 / JA11 parity

Status: append-only implementation/test record, 2026-11-04. Add an entry after every test run that informs the design, not only after physical tests.

## Rules

- Never delete an entry. Mark it superseded and link the replacement.
- Record the exact source SHA and artifact.
- Separate what the test proves from what it does not prove.
- Record the environment: Mac, Android emulator, or Pixel.
- Record the EW300 location and whether any write was sent.
- Link raw logs, JSON, screenshots, or captures.
- Update FUNCTIONAL_MAPPING.md in the same slice.
- Update maintained docs when the fact is durable.

## Entry template

### T-000 — short title

- Date:
- Source SHA:
- Artifact / APK:
- Device:
- Environment:
- EW300 location:
- Preconditions / baseline:
- Command:
- Operation:
- Exact bytes sent (if any):
- Exact bytes received:
- Observed result:
- Result: PASS / FAIL / INCONCLUSIVE
- What this proves:
- What this does not prove:
- Restoration:
- Artifacts:
- Functional mapping rows updated:
- Maintained docs updated:
- Reviewer:

## Entries

Add entries below in chronological order.

### T-000 — pre-change EW300 baseline suite

- Date: 2026-11-05
- Source SHA: 180f4a962a53d22d294311121dd49d92a570d89a (clean worktree before source edits)
- Artifact / APK: none
- Device: Mac host only
- Environment: Codex macOS, ./tools/codex-android, debug unit test
- EW300 location: Mac, read-only scope
- Preconditions / baseline: fresh worktree on branch codex/ew300-ja11-parity
- Command: ./tools/codex-android :app:testDebugUnitTest --tests '*Ew300*' --console=plain
- Operation: existing EW300-focused JVM regression suite
- Exact bytes sent (if any): none
- Exact bytes received: none
- Observed result: BUILD SUCCESSFUL; 15 classes, 97 tests, 0 failures, 0 errors, 0 skipped
- Result: PASS
- What this proves: the unmodified EW300 protocol/session/editor/gain/capture/persistence tests pass at the pinned base.
- What this does not prove: current hardware behavior or the new experimental runtime/save split.
- Restoration: not applicable
- Artifacts: app/build/test-results/testDebugUnitTest/TEST-*Ew300*.xml
- Functional mapping rows updated: none (baseline only)
- Maintained docs updated: none
- Reviewer: none

### T-001 — protocol, capability and runtime/save JVM slice

- Date: 2026-11-05
- Source SHA: 180f4a962a53d22d294311121dd49d92a570d89a + dirty working tree (candidate head recorded after the local commit)
- Artifact / APK: none
- Device: Mac host only
- Environment: Codex macOS, ./tools/codex-android, debug unit test
- EW300 location: Mac, read-only
- Preconditions / baseline: T-000 passed
- Command: ./tools/codex-android :app:testDebugUnitTest --tests '*Ew300*' --console=plain
- Operation: positive RX golden bytes, signed boundaries, experimental shelf gate, stereo gain, runtime Apply, runtime gain, explicit Save, stale/external change, no replay, reconnect completion
- Exact bytes sent (if any): none
- Exact bytes received: none
- Observed result: BUILD SUCCESSFUL; 15 classes, 109 tests, 0 failures, 0 errors, 0 skipped
- Result: PASS
- What this proves: the JVM contract for runtime zero-Save Apply/gain, one-Save persistence, capability flags and positive readback is implemented and covered.
- What this does not prove: physical EW300 runtime behavior, native shelf acoustics, or Pixel instrumentation.
- Restoration: not applicable
- Artifacts: app/build/test-results/testDebugUnitTest/TEST-*Ew300*.xml
- Functional mapping rows updated: M-03, M-04, M-07, M-08, M-15, M-16, M-17
- Maintained docs updated: this log and FUNCTIONAL_MAPPING.md
- Reviewer: pending final gpt-6-luna review

### T-002 — live read-only Mac capture (0x52 only)

- Date: 2026-11-05
- Source SHA: working tree from 180f4a96
- Artifact / APK: none
- Device: SIMGOT EW300 DSP on the Mac, 31B2:0111, LE XIAN
- Environment: macOS hidapi, tools/ew300_readonly_probe.py
- EW300 location: Mac
- Preconditions / baseline: exact identity strings matched; probe self-test passed
- Command: python3 tools/ew300_readonly_probe.py --capture --output ~/.codex/evidence/ew300/2026-11-05
- Operation: 256 register reads, command 0x52 only
- Exact bytes sent (if any): 256 frames of 4B <reg> 00 00 00 52 00 00 00 00 00
- Exact bytes received: 256 exact 11-byte read echoes; all frames recorded in the capture
- Observed result: 256/256 registers captured; all emitted commands were 0x52; firmware bytes 0x04/0x05 decode 1.0.0; bands 0x26-0x2F are flat 1 kHz/0 dB/Q 1; 0x66 is 0
- Result: PASS
- What this proves: the exact unit answers the read-only probe on this Mac, the tool emits no write/Save/handshake command, and the current live EQ/gain are flat.
- What this does not prove: native shelf acoustics, persistence, or any behavior of the Android app.
- Restoration: no mutation was sent; no restoration needed
- Artifacts: /Users/stephenweeks/.codex/evidence/ew300/2026-11-05/ew300-readonly-capture.json (file sha256 030a38cd6cfd75661c9c935e509f7cbcd358747c131d91aa80314838bb672d71; internal capture sha256 f54de03cd8ff0a6a104b54936f130346dd0d086cf7ecebe5178d1bfe094bf17a)
- Functional mapping rows updated: M-13, M-19
- Maintained docs updated: EW300_DSP_PROTOCOL_NOTES.md
- Reviewer: pending final gpt-6-luna review

### T-003 — explicit-emulator parity Compose flow

- Date: 2026-11-05
- Source SHA: working tree from 180f4a96
- Artifact / APK: ew300Parity debug APK and test APK
- Device: emulator-5554, ew300_parity_api36, API 36
- Environment: Android emulator, explicit ANDROID_SERIAL
- EW300 location: Mac; no hardware path used by the test
- Preconditions / baseline: builds passed; only the named test class ran
- Command: ANDROID_SERIAL=emulator-5554 ./tools/codex-android :app:connectedEw300ParityAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.weekssa.opraeqforuapp.ui.screens.Ew300ParityFlowTest
- Operation: five-band rows, runtime-only saved state, explicit Save callback, persistent Reset confirmation, DEVICE About limits, accessible band values
- Exact bytes sent (if any): none
- Exact bytes received: none
- Observed result: BUILD SUCCESSFUL; 4 tests, 0 failures
- Result: PASS
- What this proves: the parity Compose surface renders and invokes the reviewed callbacks on an explicit emulator.
- What this does not prove: physical EW300 behavior or end-to-end Android USB.
- Restoration: emulator only; no app data or hardware state changed
- Artifacts: app/build/outputs/androidTest-results/connected/ew300Parity/
- Functional mapping rows updated: M-04, M-07, M-13, M-15, M-16, M-18
- Maintained docs updated: none beyond this log
- Reviewer: pending final gpt-6-luna review

### T-004 — parity build and APK identity

- Date: 2026-11-05
- Source SHA: working tree from 180f4a96
- Artifact / APK: app/build/outputs/apk/ew300Parity/app-ew300Parity.apk
- Device: Mac host
- Environment: ./tools/codex-android, Android Build Tools 36.0.0
- EW300 location: Mac, read-only
- Preconditions / baseline: T-001 and T-003 passed
- Command: ./tools/codex-android :app:assembleDebug :app:assembleEw300Parity :app:assembleEw300ParityAndroidTest
- Operation: variant isolation and APK identity readback
- Exact bytes sent (if any): none
- Exact bytes received: none
- Observed result: package com.weekssa.opraeqforuapp.ew300parity, versionName 0.10.0-ew300parity, versionCode 22, local debug signer 945614206b73258162a2e0722ddcf4a53ae3018652e0fb65d2b9bcc3e38521a5, APK SHA-256 63c71d85195f01f2f07c7cb341efbc55ba21c6dcc1e61c2a0a2003101c8689a7
- Result: PASS
- What this proves: the separate package and AndroidTest variant build with the experimental flag only in the parity variant.
- What this does not prove: installation or launch on a Pixel; physical qualification.
- Restoration: not applicable
- Artifacts: named APK, output-metadata.json
- Functional mapping rows updated: M-20
- Maintained docs updated: READINESS.md
- Reviewer: pending final gpt-6-luna review

### T-005 — review P1 fix: runtime gain bookkeeping

- Date: 2026-11-05
- Source SHA: working tree after reviewed head 7b80da22 (fix commit follows)
- Artifact / APK: rebuilt ew300Parity APK
- Device: Mac host
- Environment: ./tools/codex-android
- EW300 location: Mac, read-only
- Preconditions / baseline: independent gpt-6-luna review returned FAIL with one P1 in setPlaybackGainRuntime
- Command: ./tools/codex-android :app:testDebugUnitTest --tests '*Ew300*' && :app:assembleEw300Parity
- Operation: rebase tracked EQ delta on a verified no-op absolute gain; invalidate delta and clear pending target after an uncertain write/readback
- Exact bytes sent (if any): none
- Exact bytes received: none
- Observed result: 112 EW300 tests, 0 failures; rebuilt parity APK SHA-256 1599d98e9224264d7a88993a9e328159f63c6f256d12c422e339ddb86936e778
- Follow-up confirmation: the first fix covered the verified no-op and rejected-write paths; the reviewer then required explicit coverage of the accepted-write/readback-mismatch branch, now added.
- Result: PASS (finding fixed; confirmation pass pending)
- What this proves: the reviewed stale-compensation defect now has explicit no-op and uncertain-write coverage.
- What this does not prove: physical gain behavior.
- Restoration: not applicable
- Artifacts: named APK; test XMLs
- Functional mapping rows updated: M-08
- Maintained docs updated: this log
- Reviewer: gpt-6-luna finding; same reviewer confirmation pending

