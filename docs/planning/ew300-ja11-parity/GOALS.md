# EW300 / JA11 — five-goal closure ledger

Status: controlling goal contract for the experimental `com.weekssa.opraeqforuapp.ew300parity` test build, 2026-11-05. This supersedes the earlier “omit / dormant / not permitted” dispositions for that package. Production remains isolated and unchanged.

| Goal | Required behavior | Implementation path | Exact control / evidence | Unit tests | UI/emulator tests | Mac test | Pixel test | Owner test | Status | Evidence | Blocker | Next experiment |
|---|---|---|---|---|---|---|---|---|---|---|---|---|
| G1 0–60 volume | JA11-style 0–60 EW300 volume, 60 = 0 dB, 0 = −60 dB, 1 dB/step, both stereo bytes, readback after every change, runtime and Save semantics | `Ew300VolumeControl` maps level to dB; existing 0x66 runtime/persistent gain transaction writes and verifies | EW300 register 0x66, signed half-dB steps; 1 dB = 2 steps; label as EW300 mapping, not JA11 0x02 | boundaries 0/1/30/59/60; both channels; runtime zero Save; explicit Save one | DEVICE 0–60 row/slider; value and mapping labels; emulator flow | read current 0x66; runtime set/readback; restore | same, via parity APK | listen for expected level changes | IN PROGRESS | pending | none | implement mapping and UI |
| G2 mic/headset switches | Real switch(es) if the exact EW300 exposes them; no fake read-only substitution | Parse UAC1 feature units/terminals/alternate settings; inspect HID 0x01 and vendor 0x54; inspect 0x4B mic/PGA/noise-gate/limiter registers; GET_CUR/MIN/MAX before write | UAC1 feature unit mute/volume if present; HID report 0x01; vendor report 0x54; 0x4B registers | descriptor parser; mute read/write transaction; negative/failure cases | mic mute switch and headset status in DEVICE; emulator | descriptor dump; controlled UAC request; baseline/readback/restore | same via APK + phone descriptors | confirm mic mute/remote effect | IN PROGRESS | pending | exact control not yet established | parse exact descriptors and test feature units |
| G3 UAC mode switching | UAC switch if the exact EW300 supports it; otherwise exact descriptor evidence and a concrete next experiment | Parse configuration/interface/alternate/endpoint descriptors; test standard UAC requests; look for dual-mode vendor control/re-enumeration | current descriptor shows one configuration/UAC1; JA11 0x20 is not transferable | descriptor parser; mode read-only; negative switch | DEVICE UAC row: current mode; switch only if verified | enumerate descriptors and PIDs | same via APK | none if read-only | IN PROGRESS | pending | no exact switch control located yet | inspect descriptors and vendor/HID hypotheses |
| G4 current sample rate | Read-only current rate, not descriptor-supported rates | UAC1 GET_CUR SAMPLING_FREQ_CONTROL on the streaming endpoint/interface; fallback vendor read only with byte evidence | UAC1 class request 0x81, value 0x0100, endpoint/interface index, 3-byte result | synthetic descriptor parser; request builder; no-rate/failure | DEVICE/About current rate row; emulator | read actual stream rate and compare | same via APK | none | IN PROGRESS | pending | exact endpoint/entity path to confirm on device | parse exact config descriptor and test GET_CUR |
| G5 native shelves | LS/HS editor, type 3/4 A/B read/write/readback, capture, Flash, cut-only −6..0 dB, Q 0.707, 250 Hz/4 kHz, experimental labels | Extend EW300 experimental spec/editor; allow type 3/4 encoding only in parity; capture decode; Flash native shelf target | type 3 = Low Shelf, type 4 = High Shelf; B word type bits; cut-only envelope | golden A/B bytes; read/write/readback; capture; Save/restore; boundary rejects | shelf chips, values, graph “predicted · acoustics unmeasured”, shelf Flash | reversible write/readback/restore on band 1 | parity APK shelf apply/capture/Flash | none; human listening is not an acceptance gate | IN PROGRESS | type-byte transport historical; acoustic unmeasured | no measurement device available here | measurement rig, if one is ever attached |

## Acceptance rules

- G1 and G4 are read/write or read-only goals; G5 is the experimental native-shelf path.
- G2 and G3 may be **BLOCKED** only after exact descriptor/control evidence and a concrete next experiment. “No public docs” and “not in the family map” are not blockers.
- Production flags remain false. Experimental controls are reachable only in `com.weekssa.opraeqforuapp.ew300parity`.
- No firmware/bootloader/erase/calibration/0x08/0x43/0x54 identity writes. Static analysis is authorized; firmware flashing is not.
- Every probe preserves baseline, sends one justified change, reads back, restores, and verifies.

## Current status — 2026-11-05 execution

This status table supersedes the initial “IN PROGRESS” column above.

| Goal | Status | Implemented | Tests | Physical evidence | Owner test | Blocker / next experiment |
|---|---|---|---|---|---|---|
| G1 0–60 volume | **DONE** | DEVICE 0–60 row/slider, 60 = 0 dB, 0 = −60 dB, 1 dB/step, both stereo bytes, runtime/Save semantics; out-of-scale gain displays as unmapped | Ew300VolumeControlTest boundaries; Ew300FlasherTest; Ew300FullParityFlowTest | Mac reversible 0x66: baseline 00000000 → level 0 88880000 → level 30 C4C40000 → level 60 00000000 → restore 00000000; no Save; file sha 6fd8e368…, internal JSON sha 9671c2d1… | Listen for expected level changes | none |
| G2 mic/headset switches | **MIC DONE / HEADSET CHIP-LIMITED** | Real UAC1 microphone mute and mic volume; headset row explicit “no USB-exposed disable control” | Ew300UacDescriptorTest; Ew300FullParityFlowTest; live Mac UAC | Exact die KT0211L. Feature unit 2: mic mute + volume 0xCE00..0x0000/0x8000; GET 0 → SET 1 → verify 1 → restore 0. No headset feature unit; HID 0x01 failed; 0x54 ambiguous. KT0211L family firmware contains OMTP/CTIA/lradc and click/double/long-press strings, but no USB/HID disable control. | Mic mute effect | No USB-exposed headset-disable control identified. Next only if exact firmware/config is obtained: locate GPIO/key matrix field; no blind write. |
| G3 UAC mode switching | **CHIP-LIMITED** | Read-only UAC1 status; no switch exposed | Ew300UacDescriptorTest; Ew300FullParityFlowTest | Exact die KT0211L; official profile UAC1.0, 24/96, mono ADC; EW300 descriptor one UAC1 config, 44.1/48/96, 16/24-bit. No UAC2 silicon path. | none | No UAC2 switch exists to chase; do not reuse JA11 0x20. |
| G4 current sample rate | **DONE (Pixel)** | UAC1 GET_CUR on playback endpoint 0x01/interface 2; DEVICE/About row | Ew300UacDescriptorTest; Ew300FullParityFlowTest | Pixel UAC1 GET_CUR 96000 Hz with active stream; AudioFlinger mixer 48000 Hz recorded separately; Mac 48000 Hz. | Confirmed on Pixel | no fake descriptor-supported rate shown. |
| G5 native shelves | **PROTOCOL DONE / ACOUSTICS UNMEASURED** | LS/HS chips, type 3/4 A/B, capture, Flash, cut-only −6..0 dB, Q 0.707, predicted/unmeasured graph; state controls are engineering-only in the parity package | Golden/JVM/Compose/reversible Mac/Pixel | Mac reversible LS/HS; Pixel runtime shelf + -2 dB gain reverted on replug; one Save persisted across power cycle; one restoration Save returned flat/0 dB with final readback; Pixel controls read back exactly (T-023). | none required for acceptance | Acoustic meaning is unmeasured: no capture device on this Mac and no rig available. No listening score is an acceptance criterion. |

## Chip-first coverage — 2026-11-05

## Library → Flash route — 2026-11-05

The EQ Library Flash path is the existing single route: My EQs / EQ Library -> flashManagedProfile / flashSavedEq / flashGeneralEq -> flashHardwareProfile -> flashEw300Profile -> HardwareEqRepository.flashEw300 -> Ew300Flasher.flash. No second flasher or protocol path was added.

| Item | Status | Evidence |
|---|---|---|
| Why Flash was unusable | DIAGNOSED | Direct Flash default off (parity package had never written preferences), empty parity database (0 saved EQs, 0 managed profiles), out-of-envelope shelves rejected with NotSuitable, and no surface naming the prerequisites. T-025 |
| Parity setup path | DONE | Settings -> "Parity setup · Flash to SIMGOT EW300" with three explicit steps (active output, Direct Flash, connection) and a Ready line; nothing is enabled silently |
| Eligibility reasons | DONE | Shared hardwareFlashUnavailableReason() drives My EQs and the managed-profile screen: no active output, Direct Flash off, not connected, or (for saved rows) no profile for this output |
| Library data | DOCUMENTED | The parity database starts empty; production is not debuggable, so entries move by export from production and import into parity. This session used the EQ Library catalog itself (Simgot Audio -> EW300 (silver) -> AutoEq) |
| Shelf fallback | DONE | Native type 3/4 inside the -6 dB/Q0.707 envelope; anything else is fitted to five Peak bands by the shared SIMGOT_EW300 optimizer and reported as Optimized — no silent shelf drop, no acoustic claim |
| Pixel Flash | PASS | One Save wrote the Peak-optimized AutoEq profile; readback matched the saved EQ exactly; one restoration Save returned flat/0 dB. T-026 |

The controlling map is [CHIP_MAP.md](CHIP_MAP.md): all 256 registers read and classified, all UAC entities/alternates enumerated, HID 0x01/0x4B/0x54 inspected, the official SIMGOT Control APK decompiled, and its firmware assets hashed/inspected. Coverage is **96% of the defined chip matrix**; residual 4% is the two vendor-only control gaps plus unresolved unit-4/0x54 semantics.

| Chip path | Exact result | Consequence |
|---|---|---|
| 0x00–0xFF | 256/256 read; known ranges and unknowns classified; only 0x26–0x2F and 0x66 have a tested write path | Register coverage complete |
| UAC1 | One config; AC iface 0; mic feature unit 2 (mute + volume ch1); unit 7 output volume; unit 4 no usable control payload; playback endpoint 0x01/interface 2; capture 0x81/interface 1; no headset feature unit; no UAC2 alternate | Mic mute real; headset open; UAC mode chip-limited by KT0211L |
| HID 0x01 | Consumer report; GET_REPORT failed; passive reads none | no headset-disable switch |
| HID 0x4B | 11-byte address/command/value framing; 0x52/0x57/0x53 tested | EQ/gain/Save path |
| HID 0x54 | 10-byte vendor report; GET_REPORT returned an ambiguous 0x4B-like packet | no write until semantics established |
| Vendor APK | SIMGOT Control 1.1.2 sha eec1e6b9…; products EG280/DEW0S/DEW4U/DEW1U only; UsbHidManager uses 64-byte 0x4B framing; no EW300/UAC-mode/headset USB switch | secondary negative |
| Family firmware assets | firmware.bin f5230b21…, update1.fot 2fd471a2…, ufw f1020ec9…; no EW300 version/control table found | bounded negative; no flashing |

