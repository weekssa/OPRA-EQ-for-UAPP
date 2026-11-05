# EW300 / JA11 five-goal parity — readiness package

Status: **five-goal test candidate built and locally tested; exact die KT0211L confirmed; microphone mute implemented; UAC mode CHIP-LIMITED; headset-disable open pending KT0211L GPIO/key/ADC research; physical qualification pending.** No push, PR, merge, publication, signing change, or public hardware claim has occurred.

## Candidate identity

- Base: 5832bc5e9194cbbff7d9321e8a2bf10395d0c9c6
- Branch: codex/ew300-ja11-parity
- Worktree: /Users/stephenweeks/.codex/worktrees/ew300-ja11-parity/OPRA-EQ-development
- Goal ledger: docs/planning/ew300-ja11-parity/GOALS.md
- Code candidate head: 6c276b098a92e4bf169f06abe410e6ea99821eac
- Gate seal: 5ef34b797a53e153 (contrast: current gate was resealed for the five-goal OWNS)

## APK

- Path: app/build/outputs/apk/ew300Parity/app-ew300Parity.apk
- File: app-ew300Parity.apk
- SHA-256: 20993f8bc96066572cee9007d8632f07c6bbd136ecf8acf0644372a7ec24ebb3
- Package: com.weekssa.opraeqforuapp.ew300parity
- versionName: 0.10.0-ew300parity
- versionCode: 22
- Signer SHA-256: 945614206b73258162a2e0722ddcf4a53ae3018652e0fb65d2b9bcc3e38521a5 (Android debug certificate, local test signer)
- Updates: disabled in the parity variant

## Five-goal status

| Goal | Status | Evidence |
|---|---|---|
| G1 0–60 volume | DONE | Device row/slider; 60 = 0 dB, 0 = −60 dB, 1 dB/step; Mac 0x66 bytes 88880000 / C4C40000 / 00000000 and exact restore; zero Save; JVM/emulator tests. |
| G2 mic/headset switches | PARTIAL / OPEN (headset) | Exact die KT0211L (CHP 0211LC02). UAC1 feature unit 2, source 0x0201, master mute; Mac GET 0 → SET 1 → verify 1 → restore 0; mic volume 0xCE00..0x0000/0x8000. No headset feature unit; HID 0x01 failed; 0x54 ambiguous. Next: KT0211L GPIO/key/ADC/bias. |
| G3 UAC mode switching | CHIP-LIMITED | KT0211L is UAC1.0 / 24-bit / 96 kHz with a mono ADC; exact EW300 descriptor is one UAC1 config, 44.1/48/96 kHz, 16/24-bit. No UAC2 silicon path. Descriptor sha256 e955ae8c…. |
| G4 current sample rate | DONE (implementation/tests; active-stream validation pending) | UAC1 GET_CUR endpoint 0x01 (wIndex 0x0001); Mac returned 48000 Hz (80BB00). A zero result is treated as not read, not 0 Hz. |
| G5 native shelves | DONE (implementation/tests; owner listening pending) | LS/HS editor/capture/Flash; type 3/4 bytes C4FFFA00/C3020300 and C4FFA00F/C3020400; Mac reversible and exact restore; graph says predicted/unmeasured. |

## Exact-device evidence

- Configuration descriptor: 303 bytes, UAC1, AC interface 0, mic feature unit 2 (master mute), playback endpoint 0x01/interface 2 (44.1/48/96 kHz), capture endpoint 0x81/interface 1, HID interface 3.
- UAC evidence JSON: /Users/stephenweeks/.codex/evidence/ew300/2026-11-05/ew300-uac-descriptor.json (file sha256 a65245351b68e4a5d750e623aa771cb9c385c85baa7283477e420cbcc034566b).
- Volume evidence: /Users/stephenweeks/.codex/evidence/ew300/2026-11-05/ew300-reversible-volume.json (file sha256 6fd8e3682f5e6caf39fe71a5c4fa41f4816f804bbf9039f9520634abf2e5cebd; internal JSON sha256 9671c2d14737eed4e4d006e122e9f804824e9ebfa62469026fc03be461ee928c).
- Shelf evidence: /Users/stephenweeks/.codex/evidence/ew300/2026-11-05/ew300-reversible-shelf.json (file sha256 cdba8d041947132fd175a01668034977153a72d22994ec58a117939f4e296f2d; internal sha256 37c4db310c85e7ed43758e42bd35d5e4950e8234bdeb6a30511ef030a50ecb89).
- Live read-only full register capture: /Users/stephenweeks/.codex/evidence/ew300/2026-11-05/ew300-readonly-capture.json (file sha256 030a38cd6cfd75661c9c935e509f7cbcd358747c131d91aa80314838bb672d71).
- Chip map: docs/planning/ew300-ja11-parity/CHIP_MAP.md (256/256 registers; 96% coverage; 4% residual headset/UAC-mode/unit-4/0x54).
- Exact die: KT0211L (CHP 0211LC02), confirmed read-only; CHIP_CONFIDENCE.md records the candidate roster and 96% score.
- Vendor APK: SIMGOT Control 1.1.2, sha256 eec1e6b973f53e588756e065ec550e306bfa7d874395b9512a470d9bc323abee; firmware.bin f5230b21…, update1.fot 2fd471a2…, ufw f1020ec9….

## Tests

- Debug JVM: 866 tests, 123 EW300, 0 failures.
- Release EW300: 127 tests, 0 failures.
- Emulator: Ew300FullParityFlowTest 3/3 on emulator-5554; prior parity flow 4/4.
- Emulator install/launch: parity APK installed and MainActivity resumed on emulator-5554; screenshot sha256 a9549528fe4b03644eef89013a21c4d3508543048c5316220791af350b6446d6.
- Lint: 0 errors, 120 warnings, 3 hints.
- Builds: debug, ew300Parity, ew300ParityAndroidTest.
- Probe self-tests: readonly 7/7; reversible shelf 3/3; live reversible volume/shelf/restore.

## Blockers and next experiments

Chip coverage score: **96%** of the defined matrix in CHIP_MAP.md/CHIP_CONFIDENCE.md. All 256
registers were read and classified; all UAC entities/alternates, HID 0x01/0x4B/0x54, the vendor APK
product/protocol paths, and family firmware assets were inspected or tested. The residual 4% is the
KT0211L headset/GPIO/key configuration, feature-unit-4 semantics, HID-0x54 ambiguity, and Pixel
active-stream validation. UAC mode is CHIP-LIMITED, not blocked.

- Headset-remote disable: no exact descriptor/HID control. Next: static vendor APK/HID 0x54 analysis and one hypothesis-driven vendor request with baseline/restore; no blind sweep.
- UAC mode switching: CHIP-LIMITED by KT0211L (UAC1.0 / 24-bit / 96 kHz); exact descriptor matches. No UAC2 switch exists to chase.
- Sample-rate Pixel validation: play a known-rate stream and compare UAC1 GET_CUR with the actual stream; a zero result must remain “not read.”
- Pixel session: no Pixel is connected in this environment; wireless/USB plans A–E remain ready. This is the one external dependency.
- Owner listening: fixed 250 Hz Low Shelf and 4 kHz High Shelf, −6 dB, Q 0.707; owner performs the scored listening test.
- Physical persistence: one target Save and one restoration Save only after the owner approves the bounded session.

## Safety and rollback

- No firmware, bootloader, erase, calibration, 0x08, 0x43, 0x54 identity, or arbitrary register write.
- The reversible Mac probes wrote only 0x26/0x27 (shelves) and 0x66 (volume), verified readback, and restored the exact baseline. They never sent 0x53.
- Rollback: stop using the experiment package; complete any DAC restoration before uninstalling.
- Production flags remain false; only com.weekssa.opraeqforuapp.ew300parity exposes experimental controls.

## Documentation

- docs/planning/ew300-ja11-parity/GOALS.md
- docs/planning/ew300-ja11-parity/FUNCTIONAL_MAPPING.md
- docs/planning/ew300-ja11-parity/TEST_EVIDENCE_LOG.md
- docs/planning/ew300-ja11-parity/RESEARCH_SOURCES.md
- docs/EW300_DSP_PROTOCOL_NOTES.md, docs/EW300_CAPABILITY_MATRIX.md, docs/EW300_VALIDATION_LEDGER.md
- docs/CHATGPT_PROJECT_RUNBOOK.md, docs/PROJECT_STATE.md

Physical qualification pending.

