# Functional mapping — EW300 / JA11 parity

Status: living planning/implementation document, 2026-11-04. Update after every code slice and every test run. Do not let it drift behind the implementation.

## Purpose

This table maps JA11 behavior to the EW300 implementation, exact register/byte encoding, UI surface, tests, evidence, and confidence. It is the reference for future changes and the record of what was deliberately not matched.

## Status vocabulary

- IMPLEMENTED — code exists, not yet verified.
- PLANNED — accepted for the experiment but not yet implemented.
- VERIFIED_JVM — unit/protocol tests pass.
- VERIFIED_EMULATOR — emulator UI/fake-transport tests pass.
- VERIFIED_PHONE — final Pixel session passes.
- DEFERRED — intentionally not in the current experiment.
- UNSUPPORTED — exact EW300 evidence says the capability is not available or not safe.
- UNKNOWN — insufficient evidence; do not invent.
- OUT_OF_SCOPE — excluded by product/safety boundary.

## Mapping table

| ID | JA11 behavior | EW300 hardware / register | Exact encoding or bytes | UI surface | Tests | Status | Evidence | Confidence | Notes / future-change risk |
| --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
| M-01 | Five-band User 1 PEQ | DAC bands 0x26-0x2F | A = freq16/gain10; B = type/Q1000 | EQ tab, graph, band rows | Ew300ProtocolTest, Ew300FlasherTest | VERIFIED_PHONE | E001, E037-E046 | high | Preserve current path |
| M-02 | Peak filter type | type 0 | B bits 16-18 = 0 | Editor type field | Ew300ProtocolTest | VERIFIED_PHONE | current qualified path | high | Baseline |
| M-03 | Low Shelf / High Shelf | type 3 / 4 | B bits 16-18 = 3 / 4 | Dormant; fixed test targets only | Ew300ProtocolTest | PLANNED / DORMANT | raw type transport only; owner listening not run | low | Needs acoustic + persistence evidence; editor exposure stays off |
| M-04 | Runtime Apply | volatile writes, no 0x53 | 0x57 writes; no Save | Editor Apply in parity build | Ew300EditorApplyTest, Ew300FlasherTest, Ew300ParityFlowTest | VERIFIED_JVM / VERIFIED_EMULATOR | zero Save asserted; no physical session | medium | One Save invariant preserved; production Apply remains persistent |
| M-05 | Persistent Save/Flash | 0x53 | 0x4B 00 00 00 00 53 00 00 00 00 00 | Flash/Save status | Ew300FlasherTest | VERIFIED_PHONE | E001, E037-E046 | high | Preserve one-Save/no-replay |
| M-06 | Reset to flat | five bands + 0x66, 0x53 | exact baseline writes | Reset | Ew300FlasherTest | VERIFIED_PHONE | E037-E046 | high | Preserve |
| M-07 | Startup settings | saved vs runtime state | Runtime only / Saving / Saved in this session / Save not verified | EQ and DEVICE rows | Ew300FlasherTest, Ew300ParityFlowTest | VERIFIED_JVM / VERIFIED_EMULATOR | session-scoped state; no physical session | medium | Saved-state claims reset with session/process; never inferred |
| M-08 | Device volume | EW300 output gain 0x66 | stereo signed 0.5 dB steps | Output gain row | Ew300DeviceControlsTest, Ew300FlasherTest | VERIFIED_JVM | runtime gain in parity; independent physical validation pending | high | Not the same as JA11 0-60 volume |
| M-09 | EQ program | 0x24 enable; one bank | bit semantics pending | DEVICE row | pending | UNKNOWN | 0x24 reads 1 | low | Do not invent five programs |
| M-10 | Tuning (Ellyn) | none | 0x3A/0x3B are PGA | none | n/a | OUT_OF_SCOPE | register collision | high | Never transplant 4/12 |
| M-11 | Microphone / headset | none established | n/a | none or unavailable note | n/a | UNSUPPORTED | no exact evidence | medium | Do not add a fake switch |
| M-12 | USB Audio Class | none established | n/a | About/read-only only | n/a | UNSUPPORTED | UAC1 descriptor only | medium | No mode writes |
| M-13 | About | identity/version/date registers | 0x00-0x11, 0x40-0x56 | About section | Ew300ParityFlowTest | VERIFIED_EMULATOR (read-only) | firmware 1.0.0 read from the 2026-11-05 live capture; serial omitted | high | Read-only; no identity writes |
| M-14 | Personal EQ capture | Peak-only capture | canonical filters | Capture action | Ew300CapturedEqTest | IMPLEMENTED | current path; capture UX physical evidence pending | high | Shelf capture only if proven |
| M-15 | Reports/accessibility | operation trace, semantics | APPLY_RUNTIME / SET_GAIN_RUNTIME / SAVE_RUNTIME | status/report surfaces, band-row semantics | Ew300OperationTraceTest, Ew300ParityFlowTest | VERIFIED_JVM / VERIFIED_EMULATOR | truthful Save counts; serial redacted | high | Keep privacy |
| M-16 | Capability-driven editor types | device spec | Peak only for EW300; JA11 keeps shelves | Editor type chips | Ew300ParityFlowTest (editor spec) | IMPLEMENTED | shared spec lookup; EW300 exposes Peak | high | No hardcoded three-type list |
| M-17 | Explicit Save | 0x53 once | fresh comparison, one Save, reconnect, final readback | Save EQ and output gain | Ew300FlasherTest | VERIFIED_JVM | zero/one Save assertions; no physical session | medium | No replay after uncertainty |
| M-18 | Five-band value rows | DAC band registers | type / Hz / dB / Q | EQ tab | Ew300ParityFlowTest | VERIFIED_EMULATOR | accessible row text and values | high | Values remain readback-derived |
| M-19 | Read-only Mac probe | 0x52 only | 11-byte 0x4B frames | n/a | test_ew300_readonly_probe.py, self-test | VERIFIED_JVM (host) | full 256-register capture; sha256 f54de03c… | high | Never emits 0x57/0x53/0x43/0x08/0x54 |
| M-20 | Separate experiment package | build variant | com.weekssa.opraeqforuapp.ew300parity | install/launch isolation | assembleEw300Parity, assembleEw300ParityAndroidTest | VERIFIED_BUILD | APK 20993f8b…; debug signer | high | Production flags remain false |
| M-21 | 0–60 device volume | 0x66 stereo half-dB | 60 = 0 dB, 0 = −60 dB, 1 dB/step; 0 = 88880000, 30 = C4C40000, 60 = 00000000 | DEVICE 0–60 row/slider | Ew300VolumeControlTest, Ew300FlasherTest, Ew300FullParityFlowTest | VERIFIED_JVM / VERIFIED_EMULATOR / VERIFIED_MAC | Mac reversible bytes + JSON sha 9671c2d1… | high | EW300 mapping, not JA11 0x02 |
| M-22 | Microphone mute | UAC1 feature unit 2, source 0x0201 | GET 0xA1/0x81/0x0100/0x0200/1; SET 0x21/0x01/…/1 | DEVICE microphone mute row | Ew300UacDescriptorTest, Ew300FullParityFlowTest | VERIFIED_MAC / VERIFIED_EMULATOR | Mac GET 0 → SET 1 → verify 1 → restore 0 | high | Real UAC control; Pixel acoustic check pending |
| M-23 | Headset remote switch | HID consumer report 0x01 | passive only; no disable control in descriptor/HID | DEVICE headset row | descriptor/HID evidence | BLOCKED | no headset feature unit; no exact disable control | high | Next: vendor APK/HID 0x54 static analysis |
| M-24 | UAC mode switching | one configuration, UAC1 AC subclass 1 | no UAC2 alternate or mode selector; JA11 0x20 not transferable | DEVICE read-only UAC row | Ew300UacDescriptorTest | BLOCKED | exact 303-byte descriptor sha e955ae8c… | high | Next: vendor-only command research |
| M-25 | Current sample rate | UAC1 GET_CUR playback endpoint 0x01 / interface 2 | 0xA2/0x81/0x0100/0x0102/3; idle 000000 | DEVICE/About current rate | Ew300UacDescriptorTest, Ew300FullParityFlowTest | VERIFIED_MAC (idle) / VERIFIED_EMULATOR | descriptor rates 44.1/48/96 kHz; idle returns 0 | medium | Active-stream/Pixel confirmation pending |
| M-26 | Native shelf editor/capture/Flash | type 3/4 A/B | LS C4FFFA00/C3020300; HS C4FFA00F/C3020400; cut-only −6..0, Q .707 | shelf chips, graph, capture, Flash | Ew300ShelfMappingTest, Ew300FlasherTest, Ew300FullParityFlowTest | VERIFIED_JVM / VERIFIED_EMULATOR / VERIFIED_MAC | Mac reversible bytes; file sha cdba8d04… | high (byte), low (acoustic) | Owner listening pending |

Update or add rows as the implementation evolves.

## Update rule

1. After each code slice, update the affected row's status and tests.
2. After each test run, add a matching entry to TEST_EVIDENCE_LOG.md.
3. When a fact changes, update the maintained repository documents:
   - docs/EW300_DSP_PROTOCOL_NOTES.md
   - docs/EW300_CAPABILITY_MATRIX.md
   - docs/EW300_VALIDATION_LEDGER.md
   - docs/CHATGPT_PROJECT_RUNBOOK.md when product behavior changes
4. Never delete historical evidence. Mark superseded rows and link the replacement.
5. Do not mark VERIFIED_PHONE from an emulator or from a previous SHA.

