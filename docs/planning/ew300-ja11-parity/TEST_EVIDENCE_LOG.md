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

### T-006 — exact EW300 UAC descriptor, microphone mute, UAC mode

- Date: 2026-11-05
- Source SHA: working tree from 5832bc5e (five-goal candidate)
- Artifact / APK: ew300Parity in progress
- Device: exact EW300 on the Mac
- Environment: macOS IOUSBDeviceInterface, 31B2:0111, LE XIAN
- Command: /tmp/ew300_uac_probe (compiled from tools/ew300_uac_probe.c)
- Operation: GetConfigurationDescriptorPtr; UAC1 GET_CUR sample rate; GET_CUR/SET_CUR/GET_CUR/restore microphone mute
- Exact bytes sent: descriptor read-only; sample-rate GET_CUR 0xA2/0x81/0x0100/0x01/3; mute GET 0xA1/0x81/0x0100/0x0200/1, SET 0x21/0x01/0x0100/0x0200/1
- Exact bytes received: 303-byte config descriptor; sample-rate 48000 (80BB00); mute 0, then 1, restored 0
- Observed result: UAC1 only; one configuration; microphone feature unit 2 with master mute; playback stream interface 2 endpoint 0x01 with 44.1/48/96 kHz; capture interface 1 endpoint 0x81; no headset feature unit; no UAC2 alternate
- Result: PASS for descriptor/mic mute; UAC mode switch BLOCKED
- What this proves: a real microphone mute control exists; the exact descriptor has no UAC2/UAC-mode alternate; headset-disable is not established.
- What this does not prove: acoustic mic behavior on the Pixel; a vendor-only UAC mode command outside the descriptor.
- Restoration: mute restored to 0 and verified; no Save
- Artifacts: ~/.codex/evidence/ew300/2026-11-05/ew300-uac-descriptor.json (file sha a65245351b68e4a5d750e623aa771cb9c385c85baa7283477e420cbcc034566b; descriptor sha e955ae8c6c23766224c94e442e671b0ea8230052a70c82df1cf4bff499b36907)
- Functional mapping rows updated: M-21..M-24
- Maintained docs updated: protocol notes, capability matrix
- Reviewer: pending final gpt-6-luna

### T-007 — reversible 0–60 volume bytes on 0x66

- Date: 2026-11-05
- Device: exact EW300 on the Mac
- Command: macOS hidapi writes/readbacks to 0x66
- Operation: baseline → level 0 (−60 dB, 88880000) → level 30 (−30 dB, C4C40000) → level 60 (0 dB, 00000000) → baseline
- Observed result: all readbacks exact; restored 00000000; zero Save
- Result: PASS
- What this proves: the exact 0–60 mapping writes both stereo channel bytes and restores.
- What this does not prove: audible level calibration or Pixel UI behavior.
- Artifacts: ~/.codex/evidence/ew300/2026-11-05/ew300-reversible-volume.json (file sha256 6fd8e3682f5e6caf39fe71a5c4fa41f4816f804bbf9039f9520634abf2e5cebd; internal JSON sha256 9671c2d14737eed4e4d006e122e9f804824e9ebfa62469026fc03be461ee928c)

### T-008 — reversible native Low/High Shelf bytes

- Date: 2026-11-05
- Device: exact EW300 on the Mac
- Command: tools/ew300_reversible_shelf_probe.py --live
- Operation: baseline 0x26/0x27 → LS C4FFFA00/C3020300 → HS C4FFA00F/C3020400 → baseline
- Observed result: all readbacks exact; restored 0000E803/E8030000; zero Save; only 0x52/0x57 to 0x26/0x27
- Result: PASS
- What this proves: type 3/4 A/B byte mapping and exact restoration on the exact device.
- What this does not prove: shelf acoustics, Q semantics, or persistence.
- Artifacts: ~/.codex/evidence/ew300/2026-11-05/ew300-reversible-shelf.json (file sha cdba8d041947132fd175a01668034977153a72d22994ec58a117939f4e296f2d; internal sha 37c4db310c85e7ed43758e42bd35d5e4950e8234bdeb6a30511ef030a50ecb89)

### T-009 — five-goal JVM and emulator flows

- Date: 2026-11-05
- Source SHA: working tree from 5832bc5e
- Commands: :app:testDebugUnitTest; :app:testReleaseUnitTest --tests '*Ew300*'; explicit-emulator Ew300FullParityFlowTest; :app:lintDebug
- Observed result: 866 debug tests (123 EW300), 127 release EW300, 3/3 full-parity Compose tests, lint 0 errors
- Result: PASS
- What this proves: five-goal software surfaces, 0–60 mapping, UAC parser/mic row, shelf chips/flash/capture are locally covered.
- What this does not prove: Pixel/phone behavior or owner listening.

### T-010 — five-goal parity APK

- Date: 2026-11-05
- Artifact: app/build/outputs/apk/ew300Parity/app-ew300Parity.apk
- Package/version: com.weekssa.opraeqforuapp.ew300parity / 0.10.0-ew300parity / 22
- SHA-256: 20993f8bc96066572cee9007d8632f07c6bbd136ecf8acf0644372a7ec24ebb3
- Result: PASS (build/identity); signer readback recorded in READINESS.md

### T-011 — chip map, UAC/HID and vendor static analysis

- Date: 2026-11-05
- Device: exact EW300 on the Mac
- Source SHA: 6c276b09 working tree
- Commands: tools/ew300_uac_probe.c (rebuilt with macOS 15.4 SDK); /tmp/jadx/bin/jadx on SIMGOT Control 1.1.2; strings/hashes on assets
- UAC: exact 303-byte descriptor; mic unit 2 mute/volume; unit 7 output volume; unit 4 returned no payload; playback endpoint 0x01/interface 2 returned 48000 (80BB00); no headset feature unit; no UAC2 alternate
- HID: 0x01 GET_REPORT failed (-536870168); 0x4B and 0x54 GET_REPORT returned the same ambiguous 10-byte packet with len=0; no distinct 0x54 semantics
- Vendor APK: package com.simgotcontrol.audio 1.1.2 (21), sha256 eec1e6b973f53e588756e065ec550e306bfa7d874395b9512a470d9bc323abee; products EG280/DEW0S/DEW4U/DEW1U; UsbHidManager 64-byte report 0x4B; no EW300/UAC-mode/headset USB handler
- Firmware assets: firmware.bin f5230b2174cfd6e6ebe06666e6bd7e62f5b5ce268bedab1d035b6e680f320f12 (75,683 bytes); update1.fot 2fd471a28d58da8599d1e829eee2f28a28ffd235d1b57dcb7308c3985df3759b; update(19)(1).ufw f1020ec9472f4c222141e1cc3ded3141902bb67f0fb5e41e1082135c2acc8287; family-level only, no EW300 version/control table
- Result: 256/256 registers read/classified; 96% chip matrix coverage; G2-headset and G3 BLOCKED with exact evidence; G4 48000 Hz exact; G1/G5 physical reversible bytes
- What this proves: the chip has a real mic mute/volume, no descriptor-level headset or UAC2 mode, and the vendor APK is not an EW300 control source.
- What this does not prove: a vendor-only headset/UAC mode command outside the descriptors; exact EW300 firmware is unavailable.
- Restoration: no new write; previous volume/shelf/mute restorations remain verified
- Artifacts: CHIP_MAP.md; vendor APK hash; descriptor hash; evidence JSONs

### T-012 — parity APK emulator install/launch

- Date: 2026-11-05
- Device: emulator-5554, ew300_parity_api36
- Command: ./tools/codex-android adb -s emulator-5554 install -r app/build/outputs/apk/ew300Parity/app-ew300Parity.apk; am start MainActivity
- Observed result: install EXIT 0; MainActivity topResumedActivity; screenshot captured
- Result: PASS (emulator only)
- Artifact: ~/.codex/evidence/ew300/2026-11-05/emulator/parity-launch.png (sha256 a9549528fe4b03644eef89013a21c4d3508543048c5316220791af350b6446d6)
- What this proves: the parity APK installs and launches on the emulator.
- What this does not prove: Pixel install or physical EW300 behavior.

### T-013 — read-only CHP die confirmation

- Date: 2026-11-05
- Device: exact EW300, normal mode 31B2:0111
- Pre-baseline: 0x00 5245473A; 0x26 0000E803; 0x66 00000000
- Entry: HID report 0x54 31 32 33 34 35 36 37 38 00 00; bootloader CDC 8888:CDC0 at /dev/cu.usbmodem2111201
- Commands: KTM handshake 1e4b544d; CHP d2434850; RESET 5a525354
- CHP reply: 12 40 85 B2 30 32 31 31 4C 43 30 32 3E
- Chip ID: 0211LC02 -> KT0211L
- Post: software reset did not return normal mode; owner unplug/replug restored exact 31B2:0111, 0x00 5245473A, 0x26 0000E803, 0x66 00000000
- Result: PASS (identity); KT0211L confirmed
- What this proves: the exact die is KT0211L, not KT02H20; UAC1-only is a silicon limitation.
- What this does not prove: KT0211L GPIO/key/ADC headset behavior; exact EW300 firmware.
- Restoration: normal mode and flat baseline verified
- Artifacts: ~/.codex/evidence/ew300/2026-11-05/ew300-chip-id-chp.json (sha256 d037f7b57a6549d53224178cf33a08ba3aef5c0257bf1966eb8555bf3dbc69bb)

### T-014 — KT0211L headset/GPIO firmware boundary

- Date: 2026-11-05
- Source: SIMGOT Control 1.1.2 assets/firmware.bin (sha256 f5230b2174cfd6e6ebe06666e6bd7e62f5b5ce268bedab1d035b6e680f320f12)
- Static finding: strings contain OMTP at offset 0x74b0, CTIA lradc_data:%d at 0x778f, id %d click/double/long press, host type:computer; family firmware therefore implements headset-type/key detection.
- Exact EW300: no headset feature unit; HID 0x01 GET_REPORT failed; HID 0x54 ambiguous; no USB/HID disable control identified.
- Result: G2-headset **CHIP-LIMITED on the USB surface**; internal detection is present in family firmware but not exposed as a switch.
- Next experiment only if exact EW300 firmware/config is obtained: locate the GPIO/key matrix field for headset disable; do not perform a blind register sweep.

### T-015 — Pixel Plan A read-only

- Date: 2026-11-05
- Device: Pixel 9 tokay, Android 17, wireless ADB adb-46141FDAQ003KZ-3AwgSo
- APK: parity 0.10.0-ew300parity/22; installed and on-device hash matched (initial 20993f8b; after UAC claim fix dda0c90c)
- EW300: exact 31B2:0111 / LE XIAN / SIMGOT EW300 DSP / 1.01; USB permission granted
- Read-only: EQ flat; five bands 1 kHz/0/Q1; output gain 0 dB; UAC1 current rate 96000 Hz; microphone mute Live; UAC mode UAC 1.0; About model.
- Actual stream check: AudioFlinger mixer reported 48000 Hz while the UAC endpoint reported 96000; both are recorded, no descriptor-supported-rate substitution.
- Result: Plan A PASS for UI/permission/identity/readback; Pixel exposes a real 96 kHz UAC endpoint value.

### T-016 — Pixel Plan B runtime writes (zero Save)

- Date: 2026-11-05
- APK: 487445cb6779aa1da23eb5bdfacf126ec85b9f44e9871520ce2f93e2563ebf25
- Runtime target: Low Shelf 250 Hz, -6.0 dB, Q 0.707, type 3; output gain 58/60 = -2.0 dB
- UI readback: 1 active shelf, graph -6.0..0.0 dB, "Experimental native shelf response · predicted · unmeasured"; output gain -2.0 dB
- Zero Save: parity Apply is runtime-only; no Save action used
- Pending: physical unplug/replug to prove the runtime state did not persist; restore/verify original flat/0 dB state

### T-017 — Pixel Plan C one Save

- Date: 2026-11-05
- APK: 487445cb6779aa1da23eb5bdfacf126ec85b9f44e9871520ce2f93e2563ebf25
- Target: Low Shelf 250 Hz, -6.0 dB, Q 0.707, type 3; output gain 0 dB
- Runtime readback: 1 active shelf, graph -6.0..0.0 dB, predicted/unmeasured
- Save: exactly one "Save EQ and output gain"; UI reported "Saved in this session · final readback matched"; no second Save
- Pending: genuine power removal and persistence readback; then one restoration Save to flat/0 dB and exact verification

### T-018 — Pixel Plan C persistence after power removal

- Date: 2026-11-05
- After one target Save, owner unplugged/replugged the EW300; app reconnected exact 31B2:0111.
- Readback: Low Shelf 250 Hz, -6.0 dB, Q 0.707 persisted; output gain 0 dB.
- Result: PASS for the tested Save/power-cycle/readback transaction.

### T-019 — Pixel restoration Save

- Date: 2026-11-05
- Restoration: editor changed band 1 back to Peak 1000 Hz/0 dB/Q1; one separate Save sent.
- Final readback: Flat, 0 active bands, all five Peak 1 kHz/0 dB/Q1; "Saved in this session · final readback matched."
- Result: PASS for the tested restoration transaction. Reset-button confirmation did not execute reliably in this build, so the restoration used editor + one Save.

### T-020 — Pixel UAC interface claim fix

- Date: 2026-11-05
- Finding: initial Android UAC GET_CUR failed because the audio driver owned the AS/AC interfaces; the app did not select the alt setting.
- Fix: force-claim the exact interface, select the UsbInterface alt object, GET_CUR, restore alt 0, release.
- Pixel readback after fix: current rate 96000 Hz (UAC endpoint); microphone mute Live; UAC 1.0; no writes.
- APK: 487445cb6779aa1da23eb5bdfacf126ec85b9f44e9871520ce2f93e2563ebf25; on-device hash matched.

### T-021 — Plan D listening pending

- Fixed targets: Low Shelf 250 Hz/-6 dB/Q0.707/type 3; High Shelf 4000 Hz/-6 dB/Q0.707/type 4.
- Randomized flat-vs-filter trials, 12 per shelf, >=10 correct plus consistent direction/region.
- Status: owner listening pending; no acoustic claim yet.

### T-022 — strict UAC claim/restore release

- Date: 2026-11-05
- APK: 487445cb6779aa1da23eb5bdfacf126ec85b9f44e9871520ce2f93e2563ebf25
- Fix: missing alt, failed setInterface(target), failed restore to alt0, or failed releaseInterface now fail the operation closed; release is attempted even if restore throws.
- Pixel Plan A after rebuild: 96000 Hz, microphone mute Live, UAC 1.0; no writes.
- Reviewer findings-only confirmation: PASS.

### T-023 — Pixel shelf engineering controls: exact readback, harness withdrawal

- Date: 2026-11-05
- APK: 69af25b33ec6749f152f743abfe53b76f684e476ba1d028cf2cfdf4e54799524 (parity 0.10.0-ew300parity/22); on-device base.apk hash matched after install.
- Low Shelf control: applied with the app connected to the exact EW300; readback "1 Low Shelf 250 Hz -6.0 dB Q 0.707", bands 2-5 Peak 1 kHz/0 dB/Q1; app message "Shelf A/B target low shelf applied and verified for this session."
- High Shelf control: applied; readback "1 High Shelf 4,000 Hz -6.0 dB Q 0.707".
- Flat control: applied; readback "Flat", 0 active bands, all five bands Peak 1 kHz/0 dB/Q1.
- Zero Save: no Save was sent in this sequence; saved-state text stayed at its unknown/unsaved wording.
- Harness withdrawal: the 24-state blind runner, its balanced shuffle helper, and its logcat order logging were removed from the app after this session. The three labeled state controls remain, reachable only in the parity package, relabelled "Native shelf engineering controls" with an explicit experimental/unmeasured notice.
- Measurement path: macOS enumerates three audio output devices and no input device (system_profiler SPAudioDataType), and no measurement rig is attached. No objective acoustic capture was possible on this machine.

### T-024 — Shelf acoustics: UNMEASURED disposition

- The protocol layer is demonstrated byte for byte: type 3 A/B words C4FFFA00/C3020300, type 4 C4FFA00F/C3020400, decode, readback, zero-Save runtime apply, one-Save persistence across power removal (T-017/T-018), and exact restoration (T-019).
- The acoustic result of those bytes is not measured. No capture device exists here, no published EW300 measurement uses these exact shelf writes, and the app curve is a prediction computed from the same parameters, so it cannot establish device DSP semantics.
- Disposition: native shelves remain experimental and off by default in production; the app makes no acoustic claim and claims no full JA11 parity. Only a measurement rig, or at minimum a two-comparison region discrimination, could change this label.

### T-025 — Library → EW300 Flash: why it was unavailable (parity package)

- Date: 2026-11-05
- Package: com.weekssa.opraeqforuapp.ew300parity, 0.10.0-ew300parity/22, APK eccd954e0dbc366a4d28d28a23158542e94933604c262e8bd42d23bfb22e29be
- Route inspected: My EQs / EQ Library -> flashManagedProfile / flashSavedEq / flashGeneralEq -> flashHardwareProfile -> flashEw300Profile -> HardwareEqRepository.flashEw300 -> Ew300Flasher.flash. One route; no second flasher was added.
- Active output at diagnosis: SIMGOT EW300 DSP (automatic, because the DAC was connected).
- directEw300FlashEnabled at diagnosis: **false**. The parity package had never written a preferences file (files/datastore absent), so every Direct Flash flag was at its default.
- EW300 connection state at diagnosis: Connected (app session), device present as 31B2:0111.
- Parity database: 0 rows in saved_eqs, 0 in output_saved_eqs, 0 in managed_profiles, 0 in output_general_eqs. The parity package therefore had no library entry to flash at all.
- Profile shape: not applicable until a profile existed; the catalog contains both Peak-only and shelf-bearing profiles, so the shelf fallback mattered.
- Envelope: an out-of-envelope shelf profile previously returned NotSuitable ("outside the cut-only, Q 0.707 envelope"), which is the third blocker.
- Production data: com.weekssa.opraeqforuapp is not debuggable, so its database cannot be read or copied by adb. Library entries can only move by export/import through the apps.
- Conclusion: Flash was unavailable for three independent reasons — Direct Flash off, an empty parity library, and no shelf fallback — plus no single surface stating the prerequisites.

### T-026 — Library → EW300 Flash on the Pixel, one Save, one restoration Save

- Date: 2026-11-05
- Package/APK: com.weekssa.opraeqforuapp.ew300parity, 0.10.0-ew300parity/22, eccd954e0dbc366a4d28d28a23158542e94933604c262e8bd42d23bfb22e29be; device Pixel 9 (tokay), Android 17, wireless ADB.
- Setup: Settings -> Parity setup -> 1 Active output (SIMGOT EW300 DSP, Done) -> 2 Enable Direct Flash (Done) -> 3 Connect EW300 (Connected, Done); panel reported "Ready · Flash actions are enabled for saved EQs and managed profiles."
- Library entry: EQ Library -> Simgot Audio -> EW300 (silver) -> AutoEq profile (Database/Creator Fahryst), "Optimized · 10 → 5 bands · full-response fit", tracked headroom -5.00 dB. Added to My EQs (1 selected).
- Flash: My EQs -> EW300 (silver) -> Flash -> confirm. Result "Flash complete · EQ saved and verified. The DAC reconnected and the replacement session was verified." Exactly one Save.
- Final readback: "Exact saved match", five Peak bands — 1: 6,474 Hz +5.5 dB Q 1.400; 2: 20,000 Hz -3.5 dB Q 0.375; 3: 176 Hz -1.5 dB Q 1.000; 4: 1,839 Hz -2.0 dB Q 1.400; 5: 26 Hz -1.5 dB Q 0.700; playback/global gain -5.0 dB (0x66, 55/60).
- Shape: Peak-optimized, not native shelf — the source is a 10-band parametric profile fitted to the five-band bank.
- Restoration: "Set Flat" (runtime, zero Save) plus output gain 0.0 dB (runtime, zero Save), then exactly one "Save EQ and output gain". Result "Saved in this session · final readback matched."; readback Flat, 0 active bands, five Peak 1 kHz/0 dB/Q1, gain 0.0 dB (60/60).
- Save count for the session: 1 target Save + 1 restoration Save. No listening test was performed.
- Follow-up gate: the EQ Library profile rows now hide their inline Flash action unless Direct Flash is enabled for the EW300 (previously they gated on connection alone). The managed-profile screen and My EQs keep showing the exact reason.

### T-027 — Production library inventory and the migration method actually used

- Date: 2026-11-05
- Production package: com.weekssa.opraeqforuapp (release, not debuggable). No adb or run-as database access was used.
- Production My EQs contained: **AFUL Explorer → LoboNautics** (community submission, Source Reddit, 8 filters, preamp -3.7 dB, canonical id community-96571b708868cdf52c109d5f) and **TANGZU Wan'er 2 → AutoEq / Jaytiss** (Source AutoEQ, 10 filters). Personal EQs: none. Saved General EQs: none.
- Export capability: the active production output for these entries is SIMGOT EW300 DSP, which is hardware-only (supportsFileExport = false). Its profile rows offer Source, Flash, and Remove — no Export action — so these entries cannot be written to a text file from production today.
- Migration used: re-select the same canonical catalog profiles in the parity package. The parity app shares the EQ Library catalog, so the profile id, creator, source, filter values, and EW300 output association are identical; the parity entry reproduces "AFUL · 1 selected · 5 available", LoboNautics, "Optimized · 8 → 5 bands · full-response fit" exactly as production shows it.
- What is lost: production's own record timestamps and its "latest revision seen" bookkeeping. Nothing about the curve, name, or provenance. A profile the owner had edited away from the catalog original would not transfer this way.
- What could not be exercised: a file export/import of a Personal EQ or a General EQ, because production contains none. The parity package's user-facing "Import Personal EQ" flow remains the path for those, and it was left untouched.

### T-028 — Final-artifact hardware Flash (owner library EQ), one Save, one restoration Save

- Date: 2026-11-05
- APK: f43f9ac1f814bcac20b3a1dd4282b9d84e7c8cc9efe6a042242a97e49d713b57; package com.weekssa.opraeqforuapp.ew300parity, 0.10.0-ew300parity/22; on-device base.apk hash matched after install.
- Device: Pixel 9 (tokay), Android 17, wireless ADB.
- Prerequisite state: DAC Flat, 0.0 dB, before the Flash.
- Profile: AFUL Explorer — LoboNautics (owner library entry, community/Reddit, 8 filters), EW300 output, tracked headroom -3.50 dB.
- Shape: **five-Peak fallback**, not native shelf — the source's two low shelves use Q 0.3, outside the -6 dB / Q 0.707 envelope. The confirmation read "Optimized · 8 → 5 bands · full-response fit".
- Result: one Save; expected re-enumeration; readback reported "Exact saved match" for Explorer · LoboNautics.
- Final readback: 1: 20 Hz +3.5 dB Q 0.300; 2: 3,500 Hz +3.0 dB Q 0.825; 3: 11,892 Hz +1.0 dB Q 0.500; 4: 1,784 Hz -0.5 dB Q 1.000; 5: 2,700 Hz +1.0 dB Q 3.000; playback/global gain -3.5 dB.
- Restoration: "Set Flat" and output gain 0.0 dB as runtime writes (zero Save each), then exactly one "Save EQ and output gain" -> "Saved in this session · final readback matched." Final readback: Flat, 0 active bands, five Peak 1 kHz/0 dB/Q1, gain 0.0 dB.
- Save count: 1 target Save + 1 restoration Save. No listening test.

### T-029 — Final-artifact UI verification of the Flash prerequisites

- Parity setup panel: 1 Active output = SIMGOT EW300 DSP · Done; 2 Direct Flash = Enabled · Done; 3 Connection = Connected · Done; "Ready · Flash actions are enabled for saved EQs and managed profiles."
- Direct Flash turned off (preference only, no write): panel showed "Off · Flash stays disabled"; My EQs showed "Direct Flash unavailable · Direct Flash is off. Enable it in Settings → SIMGOT EW300 DSP."; the managed-profile screen showed "Enable direct Flash in Settings → SIMGOT EW300 DSP before connecting to the DAC." with Flash disabled; EQ Library profile rows contained no Flash action at all.
- Direct Flash re-enabled: the EQ Library Flash action returned, and the panel returned to Ready.
- Non-hardware target check: selecting USB Audio Player PRO as the active target removed the hardware Flash surface entirely and showed the export state instead.
- No Save or register write occurred during these checks; only preference and navigation state changed.


### T-030 — v0.11 productized prototype, phone-free validation

- Date: 2026-11-05
- Base: origin/main 97e72cc065edb948c121431aeaeb0e5e73576456; branch codex/v0.11-ew300-prototype; isolated worktree.
- Variant: v011beta (release-derived, unminified, local test signer), package com.weekssa.opraeqforuapp.v011beta, 0.11.0-beta.1 / 23, updates disabled.
- Product variant flags: EW300_PARITY_EXPERIMENT_ENABLED=true (proven product surfaces), EW300_SHELF_EDITOR_ENABLED=false, EW300_ENGINEERING_CONTROLS=false, EW300_SHELF_QUALIFICATION_ENABLED=false.
- Engineering variant: ew300Engineering (debug-derived) is the only build with shelf A/B controls, shelf editor chips, and the research setup panel; never distributed.
- JVM: full debug unit suite 878 tests, 0 failures.
- Lint: 0 errors, 121 warnings, 3 hints.
- Emulator: connected v011beta instrumented suite 78/78 on emulator-5554 (API 36), including the product-surface test that asserts Device volume (0–60), Microphone mute, and Current sample rate are present while the shelf engineering controls are absent.
- Product decision recorded in test: experimentalEditorExposesNativeShelfChips now asserts the shelf chips are absent when the editor flag is off, and present when it is on.
- APK: built locally; identity frozen in the session checkpoint (no Pixel install).
- Not claimed: native-shelf acoustics, headset-remote control, UAC mode switching, full JA11 parity.


### T-031 — v0.11 prototype Pixel qualification (this exact APK)

- Date: 2026-11-05
- APK: 9c2d78d018909aed778c79f496f58822beaa296b6f2a3ecb1bf1fc98eb1d366d; package com.weekssa.opraeqforuapp.v011beta, 0.11.0-beta.1/23; on-device base.apk hash matched.
- Device: Pixel 9 (tokay), Android 17, wireless ADB.
- Setup path: Settings → SIMGOT EW300 DSP output selected, Direct Flash switch on. Before connection the app showed "Direct Flash unavailable · SIMGOT EW300 DSP is not connected" and after connection the line cleared, proving the reason follows the real prerequisite.
- Device checks: 31B2:0111 · revision 1.01 · HID interface 3; current sample rate 96000 Hz; UAC 1.0 read-only; microphone mute Live; headset remote "No disable control"; 0–60 volume 60/60 at 0.0 dB.
- Flash: AFUL Explorer — LoboNautics (community, Reddit, 8 filters) via My EQs → Flash; one Save; "Optimized · 8 → 5 bands · full-response fit"; -3.50 dB tracked playback gain; re-enumeration handled by the app.
- Readback: "Exact saved match": 20 Hz +3.5 dB Q 0.300; 3,500 Hz +3.0 dB Q 0.825; 11,892 Hz +1.0 dB Q 0.500; 1,784 Hz -0.5 dB Q 1.000; 2,700 Hz +1.0 dB Q 3.000; global gain -3.5 dB.
- Shape: five-Peak fallback, not native shelf.
- Restoration: "Reset to flat" → one Save → Flat, 0 active bands, five Peak 1 kHz/0 dB/Q1, gain 0.0 dB.
- Persistence: owner power-cycled the EW300 (unplug, five seconds, replug); readback after reconnection remained Flat at 0.0 dB, so the restoration Save persisted.
- Save count: one target Save plus one restoration Save. No listening test.
- Engineering surfaces: no shelf A/B controls or engineering panel appeared in either tab.


### T-032 — v0.11 DEVICE surface UX correction and microphone disposition

- Date: 2026-11-05
- Candidate: package com.weekssa.opraeqforuapp.v011beta, 0.11.0-beta.1/23, APK 12974c73297479df9c53a360090d6a09b400ff2aefb85d25839cc5fb1ecba47e, built 2026-11-06T04:18:41Z, local debug signer.
- Section map: Device settings header + Refresh → Audio (Device volume 0–60; Startup settings only when Saved or Not saved) → USB & System (Current sample rate; USB Audio Class) → About (Model, USB identity, Filters, Device limitations; Firmware only when read).
- Removed rows: Output gain, Equalizer, Connection, Headset remote, Read USB audio, Experimental features, native-shelf engineering section, and all raw descriptor/GET_CUR prose.
- Microphone: the device accepts a UAC1 mute request and the value verifies, but the Pixel audio policy exposes no USB input device (only built-in microphones), no capture path reaches the EW300, and the owner heard no difference in either position. The toggle and the Microphone section are removed; the About limitations line records that no effect on captured audio has been measured. UI plumbing for the mute was deleted; the transport capability remains unused.
- Validation on this candidate: JVM 878 tests, 0 failures (completed 04:20:40Z); lint 0 errors / 121 warnings / 2 hints (report 22:20:34 CST); connected v011beta instrumented suite 83/83 on emulator-5554 (BUILD SUCCESSFUL 04:23:33Z).
- Screenshots captured from this candidate on emulator-5554: device-top, about-expanded, device-bottom (360×360 scrolled end), limitations.
- No Pixel interaction, no Flash, no Save, no Connect session for this candidate.


### T-033 — Pixel qualification of the corrected DEVICE surface (candidate 12974c73)

- Date: 2026-11-05
- APK: 12974c73297479df9c53a360090d6a09b400ff2aefb85d25839cc5fb1ecba47e; on-device base.apk hash matched; package com.weekssa.opraeqforuapp.v011beta, 0.11.0-beta.1/23; Pixel 9 (tokay), Android 17, wireless ADB.
- DEVICE page observed live: "Device settings" + Refresh; Audio with a single "Device volume 60 / 60" row (no Startup settings row because the saved state was unknown); USB & System with "Current sample rate 96,000 Hz" and "USB Audio Class UAC 1.0"; About expander showing Model SIMGOT EW300 DSP, USB identity 31B2:0111, Filters "Five-band Peak, verified", and the Device limitations sentence. Firmware omitted because it has not been read. No Microphone heading, no Output gain/Equalizer/Connection rows, no engineering controls.
- Eligibility copy: before connecting, My EQs showed "Direct Flash unavailable · SIMGOT EW300 DSP is not connected. Connect it in My DAC." and cleared after connecting.
- Flash: AFUL Explorer — LoboNautics via My EQs, one Save, "Optimized · 8 → 5 bands", −3.50 dB tracked gain, re-enumeration handled. Readback "Exact saved match": 20 Hz +3.5 dB Q 0.300; 3,500 Hz +3.0 dB Q 0.825; 11,892 Hz +1.0 dB Q 0.500; 1,784 Hz -0.5 dB Q 1.000; 2,700 Hz +1.0 dB Q 3.000; global gain -3.5 dB. The DEVICE volume row then read 57 / 60.
- Restoration: "Reset to flat" with one Save returned Flat, five Peak 1 kHz/0 dB/Q1, gain 0.0 dB.
- Save count: one target Save plus one restoration Save. No listening test. DAC left flat at 0.0 dB.


### T-034 — DEVICE polish: startup Save, fixed USB class moved to About, tighter copy

- Date: 2026-11-06
- Candidate: com.weekssa.opraeqforuapp.v011beta 0.11.0-beta.1/23, APK 1995927d3adfe70928a44488c5ba876e97e60e67636fb321b8561e50bb199ea1, built 2026-11-06T04:52:07Z, local debug signer.
- Startup settings now mirrors JA11: when the state is "Not saved" the row carries a Save action that calls the same Save transaction as the EQ tab; "Saved" shows without an action, and an unknown state still renders no row. A Compose test asserts the Save button is present, clickable, and invokes the callback.
- USB Audio Class moved from USB & System to About as "USB audio class", because the class is fixed for this device and was presented like a settable row. USB & System now holds only "Current sample rate".
- Copy tightened: the volume dialog reads "60 is the loudest, 0 is silent, one step per dB. Applies until you save."; the Settings Direct Flash help reads "Direct Flash writes only the EQ you request. The DAC must be the active target, have Direct Flash on, and be connected."; the EW300 output subtitle is "Five Peak bands · shelf bands fitted and reported as Optimized".
- Consistency: rows use the shared JA11 components (DeviceOperationStatusHeader, DeviceSectionTitle, SimpleDeviceSettingRow, ReadOnlyValueRow, ExpandableAboutHeader), sentence-case labels, one action per row, 48 dp touch targets.
- Validation: JVM 878 tests, 0 failures (04:52:33Z); lint 0 errors / 121 warnings / 2 hints (report 22:57:35 CST); connected v011beta instrumented suite 84/84 on emulator-5554 (BUILD SUCCESSFUL 04:55:17Z). Screenshots recaptured from this APK.
- No Pixel interaction for this candidate; no Flash or Save was repeated.


### T-035 — Pixel verification of the polished DEVICE page, Startup Save, and a reset-persistence finding

- Date: 2026-11-06
- APK: 1995927d3adfe70928a44488c5ba876e97e60e67636fb321b8561e50bb199ea1 (0.11.0-beta.1/23); on-device hash matched; Pixel 9, wireless ADB.
- Startup settings on device: after applying device volume 60, the row showed "Not saved" with a Save action. Tapping Save produced "Saved". A full unplug/replug then showed "Saved" and the EQ readback stayed Flat at 0.0 dB, so the Startup Save persisted.
- USB & System now contains only "Current sample rate · 96,000 Hz". "USB audio class · UAC 1.0" appears in About, alongside Model, USB identity, Filters, and Device limitations.
- Volume dialog copy observed on device: "60 is the loudest, 0 is silent, one step per dB. Applies until you save."
- **Finding:** at the start of this session the DAC still held the previously flashed AFUL Explorer EQ at -3.5 dB even though the earlier "Reset to flat" had read back Flat/0 dB and the app had been left in that state. A reset alone therefore did not survive the disconnect, and the app reported the saved state as unknown after a reset. In this session, reset followed by an explicit Startup Save did persist across a power cycle. The reset path is not claimed as persisted evidence until it is reproduced with a power cycle.
- Saves on this session: one reset Save plus one Startup Save. DAC left Flat at 0.0 dB, "Saved", connected.


### T-036 — My DAC stuck on an unplugged DAC and lost its Connect action

- Reported: on the phone, after switching DACs, My DAC showed a previously selected DAC that was no longer attached ("Disconnected") with no Connect button, while the attached EW300 was unreachable. The My EQs banner read "Supported DACs · 2 devices recognized · Choose in My DAC" but tapping it opened the stale device.
- Cause: MyDacRootScreen kept the remembered selection whenever that device had ever been recognized in the session. The "Choose connected DAC" list only appears when nothing is selected, so a stale selection pinned the screen to an absent DAC, and shouldOfferMyDacConnect() is false for an absent device, which removed the Connect action.
- Fix: selection now follows the attached DAC. nextMyDacSelection(current, present, recognized) keeps the current device only while it is attached, otherwise takes the single attached device, otherwise keeps the single recognized device, otherwise falls back to the chooser. Covered by MyDacSelectionTest (5 cases).
- Device check on APK 43534eb3a70efde2bb9ef9dc35f205a984e1ce770d03ffd27383ad75859d87aa: after reinstall the My DAC entry opened on SIMGOT EW300 DSP with the Connect action, and Connect established the session (readback Flat, 0.0 dB).
- Validation: JVM 883 tests, 0 failures (05:17Z); lint 0 errors / 121 warnings / 2 hints; connected v011beta suite 84/84 on emulator-5554 (05:20:05Z).


### T-037 — Accurate copy when the EW300 is attached but not connected

- Candidate: APK 5e073d9db2311d552e908936cf9fef17e8f7aba75392f4b83e1f2622e32635c9, built 2026-11-05 23:23:59 CST.
- Change: Ew300MyDacContent takes deviceAttached from recognitionState.presentDeviceIds. The disconnected page now says "EW300 is attached. Connect to read its settings." when the cable is present and keeps "Connect the EW300 USB cable to manage its EQ." when it is not.
- Device check: after reinstalling and restarting the app, My DAC showed "EW300 is attached. Connect to read its settings." with the Connect action.
- Validation: JVM 883 tests, 0 failures; lint 0 errors / 121 warnings / 2 hints; connected v011beta suite 84/84 (05:28:44Z).


### T-038 — Settings v2: five groups, one control per row, 29% fewer words

- Candidate: APK 1e97db8e076b78a36eb55acacd905c879b8ba41252b09ca8ea0ad91bcff53a02, built 2026-11-05 23:40:20 CST.
- Structure: EQ target (Automatic / Manual with one-line descriptions, Current target, Default target with a picker dialog) → Outputs (Hardware DACs / Apps / Formats, short per-device subtitles, Direct Flash as a single indented switch row) → Library (Folder, Hidden EQs, Catalog with status and Refresh) → Appearance (System / Light / Dark, no descriptions) → About (Version, optional Update row, Help, About EQ Library, Data sources, Feedback).
- Removed: the five stacked paragraphs around Output behaviour, both "Available outputs" intros, the duplicated default-target device list, the four-line folder explanation, the catalog explanation paragraph, the Updates section's release-channel paragraph, and all per-link descriptions.
- Measured on the Settings root body: 93 → 81 user-facing strings and 867 → 616 words (-29%); the root now has five groups and one trailing control per row, with Direct Flash on its own row beneath the DAC.
- Validation: JVM 883 tests, 0 failures; lint 0 errors / 121 warnings / 2 hints; connected v011beta suite 84/84 (05:48:46Z). Device check: the phone shows the new groups, the Default target picker opens from Change, and the Catalog row reports its last refresh with a Refresh action.
- Screenshots: docs/screenshots/v011/settings-before-top-412dp.png and settings-after-top-412dp.png.


### T-039 — Settings v2 density and large-text check

- Captured Settings at 360 dp on emulator-5554 with font scale 1.0 / 1.3 / 2.0 (docs/screenshots/v011/settings-360dp-font*.png).
- 2.0: labels wrap cleanly, the Default target value takes three lines, Change keeps its touch target; no clipping or horizontal scroll.
- DAC status card: ten instrumented tests reach My DAC through its "Open My DAC" label, so removing the card requires migrating those navigation paths; the card was left in place this pass.
- Re-validated after the decision: JVM 883 tests, 0 failures; lint 0 errors / 121 warnings / 2 hints; connected v011beta suite 84/84 (05:57:22Z). APK unchanged at 1e97db8e.


### T-040 — My DAC reachable from the navigation bar

- Cause of the outage: an experimental build with the device status card removed was installed on the phone and reached My DAC only through that card, so the workspace became unreachable. My DAC had been documented as "never a persistent root tab", so nothing else offered a path.
- Fix: My DAC is now a navigation destination whenever a supported production DAC is recognized (JCALLY identities still create no entry), selected while the workspace is open. The status card stays as a shortcut.
- Contract test updated: recognizedDacStaysContextualAndRemainsSeparateFromTheExportTarget now asserts the My DAC destination exists and that the export target stays independent; unsupportedRecognizedDeviceDoesNotCreateAProductionMyDacEntry still passes.
- Candidate: APK 45a00243b060f10958c2108ec0261f85f7aca961bc29cb8bbb17b169e4cd0c03; validated JVM 883 tests, 0 failures; lint 0 errors; connected v011beta suite 84/84 (06:24:10Z). Installed on the Pixel; the bar shows My EQs, EQ Library, Settings and My DAC.


### T-041 — Session pause: handoff recorded, no work left only in chat

- Date: 2026-11-06. Branch codex/v0.11-ew300-prototype, code head 51375505, docs head 2d891200, working tree clean.
- SESSION_HANDOFF.md records the resume prompt, repository and artifact identity, the commit list,
  every decision and its reason, the Pixel evidence, the open reset-persistence finding, the open
  work list, and the exact build/test/screenshot commands.
- Artifact at pause: APK 45a00243b060f10958c2108ec0261f85f7aca961bc29cb8bbb17b169e4cd0c03,
  package com.weekssa.opraeqforuapp.v011beta 0.11.0-beta.1/23, built 2026-11-06 00:13:15 CST.
- Validated before pause: JVM 883 tests / 0 failures; lint 0 errors / 121 warnings / 2 hints;
  connected v011beta suite 84/84 on emulator-5554 (06:24:10Z).
- Device at pause: EW300 left Flat at 0.0 dB with the prototype installed; owner is disconnecting
  wireless debugging. No further device work is possible until it is re-enabled.
- Nothing was pushed, merged, released, or re-signed.


### T-043 — Reset persistence: modelling the transaction

- Added two fake-transport tests in Ew300FlasherTest with a transport that separates volatile state from
  persisted state (commit copies volatile to persisted; powerCycle reloads persisted).
- resetPersistsFlatAcrossAPowerCycle: reset writes five flat bands, sends exactly one Save, and after a
  simulated power cycle the persisted state is flat with the gain unchanged (D8D80000, -20 dB). Test
  passes.
- resetDoesNotClaimPersistenceWhenTheSaveIsRejected: with commit rejecting, the result is
  TransferFailed and the persisted state keeps the pre-reset gain. Test passes.
- Conclusion: the app-side reset transaction (write, single Save, readback) behaves correctly in a model
  that separates volatile from persisted state. The field observation (reset read back flat, then a
  later session held the previous EQ) is not reproducible from our sequencing; it needs a hardware
  reproduction with one replug before any behaviour change is justified. No production code changed.
- JVM 885 tests / 0 failures at 19:40Z; lint 0 errors / 121 warnings / 2 hints.

### T-044 — Reset persistence on the Pixel: passed on this run

- Date: 2026-11-06, APK 7516516b, Pixel 9, wireless ADB.
- Sequence: Connect -> Flash AFUL Explorer - LoboNautics (one Save) -> Reset to flat (one Save) -> app read back Flat at 0.0 dB -> owner unplugged five seconds and replugged.
- After the power cycle the EW300 read Flat, 0 active Peak bands, playback/global gain 0.0 dB. The reset persisted; the earlier field failure did not reproduce.
- Save count: two (one Flash, one Reset). No other writes.
- The earlier observation (reset appeared flat, later session held the previous EQ) is therefore not reproducible on this build; it remains unexplained rather than fixed.

### T-045 — One vocabulary for target, output, Direct Flash, Save
- The bar's selector now reads "EQ target: <name> ▾", matching the Settings "EQ target" group; the duplicate disclaimer line under it ("Prepares EQ files or actions…") is removed.
- Fixed vocabulary in use: **EQ target** is the single active destination; **Outputs** is the list you enable in Settings; **Direct Flash** is the per-DAC write switch; **Save** is the persistence action; **Flash** is the verb on the button that writes a preset.
- JVM 885 / 0 failures, lint 0 errors, instrumented 92/92 on emulator-5580 (19:53:57Z).


### T-046 — Lint and dead-code cleanup: attempted, reverted, no change shipped

- Lint reports 18 parameters of `CapabilityDrivenDeviceStatus` as unused in its body:
  `fiioJa11DeviceState`, `fiioJa11Connected`, `onReadFiioJa11DeviceControls`,
  `onSetFiioJa11OutputVolume`, `onSetFiioJa11EqProgram`, `onSetFiioJa11Defaults`,
  `onSetFiioJa11HeadsetControl`, `onSetFiioJa11UacMode`, `fiioJa11OperationBusy`,
  `ja11DcrTuningState`, `onSetJa11DcrTuning` and the Black Pearl pairs they mirror.
- They are not dead. Every one is forwarded unchanged into `FiioJa11DeviceStatus(...)` or
  `BlackPearlDeviceStatus(...)` in the same function, so the composable is a pass-through
  dispatcher and the lint finding is a false positive for this shape.
- The removal was attempted and the compiler rejected the result: three androidTest call sites
  (`Ew300DeviceStatusTest`, `Ew300ParityFlowTest`, `Ew300DeviceScreenshotTest`) and two
  composables that pass the JA11 device state down stopped compiling.
- The attempt was reverted with `git checkout -- app/` before any commit. Nothing shipped, no
  warning count changed, no behaviour changed. The 118 `:app:lintV011beta` warnings are accepted
  as-is for this candidate; suppressing them would hide the real shape of the API.
- Gates on the unchanged tree: JVM 885 tests / 0 failures; `:app:lintV011beta` 0 errors / 118
  warnings / 2 hints; instrumented v011beta suite 94/94 on emulator-5580.


### T-047 — Bar tests: connect targeting and the several-DACs picker

- `barConnectTargetsTheSelectedDacWhileAnotherSessionIsOpen`: target FIIO JA11, both DACs present,
  EW300 connected and FIIO disconnected. The bar shows "Not connected" and its Connect action
  dispatches `onConnectFiioJa11` exactly once and `onConnectEw300` never, so a live session on a
  second DAC neither changes the block nor redirects the connect. The first draft asserted
  `onConnectDacForMyDac(deviceId)`; the bar routes through the per-device connect actions, so the
  test was corrected to the shipped wiring rather than the wiring to the test.
- `pickerSwitchesTheBlockBetweenTwoRecognizedDacs`: both DACs recognised and enabled as outputs,
  FIIO active and connected, EW300 disconnected. Picking SIMGOT EW300 DSP from the "EQ target:"
  row moves the block to the EW300 and shows "Not connected" with no "No DAC attached" anchor
  left behind. This is the first test that exercises the picker with two recognised hardware DACs;
  the earlier draft failed because the picker lists only outputs enabled in Settings, which is the
  intended behaviour, not a defect.
- `noOpEqLibraryActions` gained defaulted `onConnectFiioJa11` and `onConnectEw300` so the bar's
  actions can be observed without touching any other test.
- Gates: instrumented v011beta suite 94/94 on emulator-5580 (of which 43 are
  UiModernizationFlowsTest); JVM 885 / 0 failures; `:app:lintV011beta` 0 errors / 118 warnings /
  2 hints. No main-source change, so the APK is unchanged at 7516516b.


### T-048 — Post-vocabulary build installed on the Pixel; bar copy verified live

- The build installed on the phone at 12:46 predated commit 49ba05bf, so it still read
  "Target: SIMGOT EW300 DSP". Rebuilt `:app:assembleV011beta` from head 50a5c0ea and reinstalled.
- Artifact now on the device: APK
  1f1ea2c174a09d84241f480536b7ea066130b49396d0b2bf51e1e77ad3400fb3, package
  com.weekssa.opraeqforuapp.v011beta 0.11.0-beta.1/23. The on-device base.apk checksum matches the
  local build byte for byte.
- My EQs with the session closed: "EQ target: SIMGOT EW300 DSP ▾", interface
  "SIMGOT EW300 DSP · Not connected · Connect", banner "Direct Flash unavailable · SIMGOT EW300 DSP
  is not connected. Connect it in My DAC."
  (docs/screenshots/v011/context-bar-pixel-not-connected.png)
- After one Connect tap on the phone, which only opens the read-only session: interface
  "SIMGOT EW300 DSP · Connected · State current · Open My DAC", banner "Direct Flash ready · Flash is
  enabled for saved EQs and managed profiles."
  (docs/screenshots/v011/context-bar-pixel-connected.png)
- My DAC read back Flat, 0 active Peak bands, playback/global gain 0.0 dB
  (docs/screenshots/v011/pixel-mydac-flat-1f1ea2c1.png). No Flash, no Save, no write of any kind was
  issued in this session; the DAC is left exactly as the previous session left it.
- This closes the copy and state-table question on the real device. It is not an acoustic test and
  says nothing about shelf behaviour.


### T-049 — Direct Flash off on the Pixel: precedence recorded, settings restored

- Artifact 1f1ea2c1, connected session. Settings → Outputs → SIMGOT EW300 DSP → Direct Flash was
  turned off with the DAC attached and live.
- My EQs then showed "SIMGOT EW300 DSP · Connected · State current · Open My DAC" with the banner
  "Direct Flash unavailable · Direct Flash is off. Enable it in Settings → SIMGOT EW300 DSP."
  The action did not become "Open Settings".
- That is the shipped precedence, not a defect: a connected device always offers My DAC, and the
  banner carries the Flash-off reason. The "Direct Flash off → Open Settings" row applies only while
  the device is attached and not connected, which is how the earlier Pixel run reached it.
  CONTEXT_BAR_DESIGN.md now states the ordering explicitly.
- Screenshot: docs/screenshots/v011/context-bar-pixel-direct-flash-off.png.
- The switch was turned back on and the bar verified again: "Connected · State current · Open My DAC"
  with "Direct Flash ready · Flash is enabled for saved EQs and managed profiles." Device state is
  unchanged: flat, 0 bands, 0.0 dB, still connected. No Flash, Save or write was issued.


### T-050 — Bar scope on the Pixel: My EQs and EQ Library yes, Settings no

- My EQs and EQ Library both show "EQ target: SIMGOT EW300 DSP ▾" and the device block.
- Settings shows the "EQ target" group heading and the Default target row, and no instance of the bar
  selector: the dump contains zero nodes matching "EQ target: ". The owner's rule — Settings keeps
  the controls, the bar stays on the audio surfaces — holds on the device, not just in the emulator.
- Read-only: three tab switches and three UI dumps. No setting was changed apart from the Direct Flash
  toggle in T-049, which was restored and re-verified.
