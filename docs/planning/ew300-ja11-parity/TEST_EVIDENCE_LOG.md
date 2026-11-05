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
- APK: dda0c90c13399467c06c328da2b3d1405fb00ad4cbf69392ccf4d0cfdefb5825
- Runtime target: Low Shelf 250 Hz, -6.0 dB, Q 0.707, type 3; output gain 58/60 = -2.0 dB
- UI readback: 1 active shelf, graph -6.0..0.0 dB, "Experimental native shelf response · predicted · unmeasured"; output gain -2.0 dB
- Zero Save: parity Apply is runtime-only; no Save action used
- Pending: physical unplug/replug to prove the runtime state did not persist; restore/verify original flat/0 dB state

### T-017 — Pixel Plan C one Save

- Date: 2026-11-05
- APK: dda0c90c13399467c06c328da2b3d1405fb00ad4cbf69392ccf4d0cfdefb5825
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
- APK: dda0c90c13399467c06c328da2b3d1405fb00ad4cbf69392ccf4d0cfdefb5825; on-device hash matched.

### T-021 — Plan D listening pending

- Fixed targets: Low Shelf 250 Hz/-6 dB/Q0.707/type 3; High Shelf 4000 Hz/-6 dB/Q0.707/type 4.
- Randomized flat-vs-filter trials, 12 per shelf, >=10 correct plus consistent direction/region.
- Status: owner listening pending; no acoustic claim yet.

