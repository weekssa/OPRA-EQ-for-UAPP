# EW300 ↔ JA11 My DAC parity brief

Status: planning input, 2026-11-04. Not a capability claim and not maintained product state.

Base at research time: current **origin/main** **180f4a96** (v0.10.0 closeout plus catalog refresh). The local checkout's **main** was **30054466** and 67 commits behind. Re-fetch and record the exact base before planning.

## 0. Canonical review packet

Project root:

/Users/stephenweeks/Documents/Codex/OPRA-EQ-development

Packet root:

/Users/stephenweeks/Documents/Codex/OPRA-EQ-development/docs/planning/ew300-ja11-parity

Review order:

1. REVIEW_PACKET.md
2. README.md
3. BRIEF.md
4. PLANNING_PROMPT.md
5. EXECUTION_PROMPT.md
6. PROGRESS_TEMPLATE.md

## 1. Executive summary

The SIMGOT EW300 DSP's current five-band Peak EQ and persistent Flash path works on the attached device. The owner confirms it. The repository has accepted physical evidence for Save/persistence/restoration (E001) and for Apply/Flash/Restore/Reset (E037-E046). This is the brownfield baseline to preserve.

The EW300 My DAC page is nevertheless **bare bones** compared with the JA11. The EW300 DEVICE tab currently exposes only:

- Output gain
- Equalizer (active Peak bands)
- Connection
- Refresh

The JA11 DEVICE tab exposes:

- Device volume
- EQ program
- Tuning (Ellyn)
- Startup settings
- Microphone / headset remote
- USB Audio Class
- Current sample rate
- About (model, firmware, USB identity, validation)
- Refresh / read controls

The goal is not to clone JA11 blindly. The goal is to inventory every JA11 row, preserve the working EW300 path, add every EW300 capability that can be proven safely, and show a truthful "not available on this EW300" outcome for the rest.

The most likely experimental scope is:

1. Preserve current five-band Peak read/edit/Apply/Flash/Reset and add regression gates.
2. Add native Low Shelf and High Shelf support only after exact-device acoustic evidence; keep Peak as the safe default.
3. Decide whether Apply should become session-only and Flash/Save persistent, matching JA11's runtime-versus-startup model.
4. Add the JA11-style DEVICE rows that the EW300 can support with evidence, especially About and a clear saved/runtime status.
5. Add a filter-type control to the shared editor only for proven types.
6. Keep Ellyn, firmware cross-flash, microphone/UAC control, and invented EQ programs out of scope.

If native shelf support cannot be proven, the honest fallback is Peak-only EQ plus layout, Apply/Flash, About, and status parity.

## 2. Device and silicon

Observed on the attached Mac:

| Field | Value |
| --- | --- |
| USB product | SIMGOT EW300 DSP |
| VID:PID | 31B2:0111 |
| Manufacturer | LE XIAN |
| Serial | 2024-07-03-0000-0000-0000 |
| bcdDevice | 1.01 (0x0101) |
| HID interface | 3 |
| HID usage page / usage | 0x000C / 0x0001 |
| Endpoints | 16-byte interrupt IN/OUT |

Chip-family conclusion:

- The transport and register map match the **KT02H20 family**.
- The official KT Micro KT02H20 page describes a single-chip USB audio codec/SoC with a 5-band DSP EQ, DRC/noise gate, UAC 1.0/2.0, 384 kHz/32-bit DAC, DSD64/128, 2 Mbit internal FLASH, and customer secondary development.
- The exact die marking/chip-ID string is **not exposed by this unit's run-mode identity block**. Register 0x00 reads "REG:", not "KT02H20B" or "0211LC02".
- Do not enter the bootloader solely to read the CHP response. That is a reboot/mutation and outside planning.
- The community KTMicro field guide treats KT02H20 and KT0211L as sharing VID:PID 31B2:0111 and the same 5-band register layout; exact die identification can require a bootloader CHP response or hardware inspection. Do not overclaim.

Official product page:

https://www.ktmicro.com/?usb_104/394.html

## 3. Read-only snapshot of the attached unit

A 2026-11-04 macOS hidapi probe read all 256 run-mode registers using command 0x52 only. No 0x57 write, 0x53 save, 0x43 handshake, reset, firmware, or bootloader command was sent.

| Register | Observed | Meaning |
| --- | --- | --- |
| 0x00 | 0x3A474552 ("REG:") | run-mode identity marker |
| 0x01 | 0x00000001 | protocol flags; bit 9 clear means stereo |
| 0x04-0x05 | "1.0.0" | firmware version |
| 0x08-0x0D | "Jul  1 2024 15:24:49" | build date/time |
| 0x10-0x11 | "5b66896" | build identifier |
| 0x18 | 1 | ADC EQ enable |
| 0x1A-0x23 | 1000/2000/5000/8000/10000 Hz, 0 dB, Q 0.707 | ADC five-band EQ |
| 0x24 | 1 | DAC EQ enable |
| 0x26-0x2F | five Peak bands, 1000 Hz, 0 dB, Q 1.000 | DAC five-band EQ; current flat |
| 0x3A | 5 | A_ADC analog PGA index |
| 0x3B | 13 | A_DAC analog PGA index |
| 0x40-0x56 | LE XIAN / SIMGOT EW300 DSP / serial | USB strings |
| 0x5B | 0x011131B2 | packed PID/VID |
| 0x65 | 0 | ADC digital gain |
| 0x66 | 0 | DAC digital gain, stereo L/R |
| 0x71-0x73 | 3CE2DAD8 / 0014000A / 01F4000A | noise gate |
| 0x78-0x79 | 000000FD / 012C000A | limiter |
| 0xE1 | 0x12345678 | magic |

Raw dump: **/Users/stephenweeks/.codex/evidence/ew300/2026-11-04/register-dump.json**

SHA-256: **40a40c97ff3f4b3400a33e7877837048121b54bdd739e658b61248f3466256b5**

Do not assume the older non-flat stock capture still describes this unit. Take a fresh baseline before any future physical test.

## 4. What already works on EW300

The current app is more capable than the DEVICE tab suggests. Preserve these paths:

- exact USB identity and session recognition;
- five-band DAC EQ readback;
- direct-Hz frequency and 0.1 dB gain decoding;
- Peak-only editor and Review;
- guarded Apply (currently persistent);
- Flash from My EQs / EQ Library;
- Reset EQ to flat;
- final readback and replacement-session verification;
- no automatic retry after an uncertain Save;
- Personal EQ capture with EW300 provenance;
- playback/global gain at 0x66, currently -64.0 to 0.0 dB in 0.5 dB steps;
- capability report and operation trace;
- EQ/DEVICE tabs and shared editor;
- stale, disconnected, permission, and error states.

Accepted physical evidence:

- E001: one-time Save, Peak persistence, playback-gain persistence, exact baseline restoration, two complete power removals.
- E037-E040: physical Flash, exact-baseline Restore, Apply, Reset on the source-381 candidate.
- E043-E046: read-only PASS plus Flash, exact-baseline Restore, Reset on exact signed executable source 7599.
- Do not repeat these tests because a later source SHA exists.

Current EW300-focused tests that must be preserved and extended:

| Area | Test |
| --- | --- |
| Protocol frames and band codec | Ew300ProtocolTest |
| Flasher, one Save, reconnect, no replay | Ew300FlasherTest |
| Editor Apply path | Ew300EditorApplyTest |
| Mutation executor | Ew300MutationExecutorTest |
| Reconnect gate | Ew300ReconnectGateTest |
| Capability profile and batch | Ew300CapabilityProfileTest, Ew300CapabilityBatchTest |
| Device controls and output gain | Ew300DeviceControlsTest |
| Operation trace and export | Ew300OperationTraceTest, Ew300QualificationExportTest |
| Persistence qualification | Ew300PersistenceQualificationTest |
| Shelf-source corpus | Ew300ShelfCorpusRegressionTest |
| Hardware EQ matching and capture | Ew300HardwareEqMatchTest, Ew300CapturedEqTest |
| DEVICE UI | Ew300DeviceStatusTest (androidTest) |
| Operation status UI | Ew300OperationStatusTest |

The execution phase must run the current EW300 tests before changing the protocol or flasher, and again after each slice.

The current DEVICE tab is intentionally narrow. That is the gap.

## 5. JA11 My DAC feature inventory

The JA11 DEVICE page currently contains:

| Section | Row | JA11 behavior |
| --- | --- | --- |
| Top | Refresh / Read | Reads current controls |
| Audio | Device volume | 0-60, slider, Apply |
| Audio | EQ program | Off / Vocal / Classic / Bass / User 1 |
| Audio | Tuning (Ellyn) | Two-register DCR/gate toggle; persists on write |
| Audio | Startup settings | Saved / Not saved; Save action |
| Microphone | Headset / mic remote | On / Off |
| USB & System | USB Audio Class | UAC 1.0 / 2.0 |
| USB & System | Current sample rate | Read-only |
| About | Model, firmware, USB identity, validation | Read-only |

The JA11 EQ tab contains:

- program state and current/last-read status;
- response graph;
- five User 1 band rows;
- Edit;
- Reset to flat;
- operation status and report access.

The shared editor supports Peak, Low Shelf, and High Shelf for JA11. The EW300 editor currently supports Peak only.

## 6. EW300 My DAC current inventory

The EW300 DEVICE tab currently contains:

| Section | Row | EW300 behavior |
| --- | --- | --- |
| Top | Refresh | Rereads the shared session |
| Device state | Output gain | -64.0 to 0.0 dB, persisted and verified |
| Device state | Equalizer | Active Peak band count |
| Device state | Connection | Current session / last read / awaiting read |

The EW300 EQ tab contains:

- response graph;
- active Peak band count;
- playback/global gain;
- Edit current EQ;
- Reset EQ to flat;
- Save readback as Personal EQ;
- operation status.

The EW300 editor is the shared editor but its spec exposes only Peak. The current Apply path is persistent and sends 0x53. There is no separate runtime-only Apply and no explicit "Saved / Not saved" startup status.

## 7. Gap analysis

| JA11 element | EW300 current | Recommended disposition | Evidence status |
| --- | --- | --- | --- |
| Device volume | Output gain at 0x66 | Keep EW300-native output gain; do not relabel as 0-60 device volume | Different control; 0x66 is digital DAC gain |
| EQ program | One DAC bank, 0x24 enable | Do not invent five programs; investigate a simple EQ on/off only if 0x24 semantics are proven | 0x24 reads 1; family docs mention 3/0x03/0x02 |
| Tuning (Ellyn) | None | Exclude; 0x3A/0x3B are PGA on EW300 | Wrong register function; unsafe to transplant |
| Startup settings | Flash persists, but no status row | Add "Saved to DAC" / "Runtime only" if Apply/Flash split is implemented | 0x53 Save qualified; volatile writes demonstrated |
| Microphone | None | Exclude unless exact EW300 mic-control evidence appears | Insufficient evidence |
| USB Audio Class | None | Exclude unless exact EW300 mode control appears | UAC1 descriptor; no control evidence |
| Current sample rate | None | Optional read-only if a safe read path is proven | Not established |
| About | None | Add model, firmware, USB identity, validation from safe reads | Identity block is readable |
| Refresh | Present | Keep | Existing |
| EQ graph/bands | Peak only | Add type labels/selection for proven types | Peak qualified; shelves need acoustic proof |
| Editor | Peak only | Add Peak/LS/HS if proven | Raw types 3/4 transport-qualified, not acoustic |
| Apply vs Flash | Apply is persistent | Consider session-only Apply + persistent Flash | Strong candidate for parity |
| Reset to flat | Present | Keep | Qualified |
| Readback/reconnect | Present | Keep | Qualified |
| Personal EQ capture | Peak-only | Keep Peak-only until shelf capture is proven | Evidence-bound |
| Reports/accessibility | Present | Extend for new rows/types | Existing |

The key product decision is not "can we add more rows?" It is "which rows are truthful?" A visible "Not available on this EW300" is better than a disabled fake control.

## 8. Byte-level protocol findings

### Transport

- HID interface 3.
- 11-byte report, report ID 0x4B.
- Payload is 10 bytes: address LE32, command, status/reserved, value LE32.
- The device also exposes a Consumer Control report ID 0x01 and vendor report IDs 0x4B/0x54 in its HID descriptor.

### Commands

| Command | Meaning | Notes |
| --- | --- | --- |
| 0x52 | Read register | Response echoes address and command; value at bytes 7-10 |
| 0x57 | Write register | Response status 0x03 on KT02H20-era devices |
| 0x53 | Save live DSP RAM to flash | Device typically reboots/re-enumerates; status 0x03 or 0x4F |
| 0x43 | Handshake | Do not send blindly on an unknown device |
| 0x08 | Extended memory read | Not a firmware backup path; often zeros in unmapped space |

### Band encoding

Each DAC band uses two 32-bit registers, A and B:

- A = (frequency_hz and 0xFFFF) shifted left 16, OR (signed gain_tenths and 0xFFFF).
- B = ((type and 0x7) shifted left 16), OR (Q_milli and 0xFFFF).
- Type: 0 = Peak, 1 = LPF, 2 = HPF, 3 = Low Shelf, 4 = High Shelf.
- Frequency is direct Hz on the exact EW300; the generic 2x fallback is not correct here.
- Gain is signed tenths of a dB.
- Q is thousandths.

Current EW300 DAC bands:

- 0x26/0x27 = band 1
- 0x28/0x29 = band 2
- 0x2A/0x2B = band 3
- 0x2C/0x2D = band 4
- 0x2E/0x2F = band 5

Current values are all Peak, 1000 Hz, 0 dB, Q 1.000.

### Key registers

| Register | Meaning | Safety |
| --- | --- | --- |
| 0x00-0x11 | identity marker, flags, version, build/date | read-only |
| 0x18 | ADC EQ enable | read-only for this task |
| 0x1A-0x23 | ADC five-band EQ | out of scope |
| 0x24 | DAC EQ enable | investigate before writing |
| 0x26-0x2F | DAC five-band EQ | product writes only |
| 0x3A/0x3B | A_ADC/A_DAC PGA on EW300 | never map Ellyn here |
| 0x40-0x56 | USB strings | read-only |
| 0x5B | USB VID/PID | never write |
| 0x65/0x66 | digital ADC/DAC gain | 0x66 only through guarded product path |
| 0x71-0x73, 0x78-0x79 | noise gate/limiter | out of scope |
| 0xE1 | magic | read-only |

### Save behavior

0x53 commits the live DSP RAM to the chip's internal flash. On the exact EW300, the accepted evidence proves Peak persistence and exact restoration. The current transport sends 0x53 once, tolerates the expected re-enumeration, and verifies a fresh final readback.

The plan must preserve one-Save maximum, no automatic retry, and post-Save uncertainty handling. It should capture the Save response status even if the current code does not require it.

### The Ellyn collision

JA11 Tuning (Ellyn) writes 0x3A = 4 to 0 and 0x3B = 0x0D to 0x0C, and it persists on write. On the EW300, 0x3A and 0x3B are the A_ADC and A_DAC analog PGA indices; the attached unit reads 5 and 13. Copying the JA11 values would change analog gain, not DCR/gate behavior.

This is why Ellyn is excluded, and why the plan must document the register-function collision.

### Identity

0x5B is the USB VID/PID identity. Never expose arbitrary register writes. Block identity and unrelated config registers at the transport or product layer.

### 0x24 ambiguity

The attached EW300 reads 0x24 = 1. Some family documentation describes 0x03/0x02 or "3" for KT02H20-era devices. Do not add an EQ on/off control until the bits and behavior are proven.

### Firmware

The chip has 2 Mbit internal flash and a USB/UART bootloader. The community has vendor tools and cross-flash research. There is no software readback/backup for the full firmware image. Firmware flashing and cross-flash are out of scope. Do not use the firmware path to "enable" features that can be controlled at runtime.

## 9. Source-quality assessment

Strongest:

- Exact-device EW300 evidence in the repository: E001, E037-E046, read-only register captures, transport code.
- The attached unit's live read-only register snapshot.

Primary vendor:

- KT Micro KT02H20 product page: 5-band EQ, DRC/noise gate, UAC 1.0/2.0, 384 kHz/32-bit, 2 Mbit flash, customer secondary development.

Useful community/family references:

- gxcreator/ktmicro-tools and KT_USB_PROTOCOL.md (BSD-3-Clause): protocol and register map.
- jeromeof/devicePEQ, ktmicroUsbHidHandler.js (0BSD): browser handler and 0x53 commit.
- Ircama/ja11-config: JA11 runtime Apply and 0x19 Save semantics; check license before using code.
- eyad-elghareeb/kt0231h: chip-family field guide, protocol, architecture, firmware forensics; community synthesis, not EW300 proof.
- Bukutsu/glacier-eq (GPL-3.0): independent JA11 implementation, 5-band limits, RAM apply vs save, UI patterns; use behavior descriptions only.
- potatosalad775/eqcaps (MIT code / CC0 data): KT02H20 profile, Peak-only JCALLY constraint, frequency-halving quirk; do not transfer to EW300 without exact evidence.
- mandy321/Audiocular-Aura (GPL-3.0): five-band JA11 UI and Peak/Low Shelf/High Shelf mapping; JA11 protocol differs.
- txtsamu/claude-research (no license found): KTMicro dongle comparison, register diff, save and firmware caveats; treat as notes only.
- lokal-music/mobile (GPL-3.0): SIMGOT EW300 UAC2 direct-stream regression; Android USB lifecycle only, not EQ.
- jaakkopasanen/AutoEq (MIT): EW300 and EW300 DSP measurements and EQ fits; useful for acoustic cross-checks.
- ParkWardRR toolkit and txtsamu kt02h20-flasher: firmware/cross-flash context only; do not use their flashing path.

No official SIMGOT EW300 protocol or firmware specification was found. The official SIMGOT Control compatibility page does not list the EW300 DSP. Treat generic VID/PID or family behavior as a hypothesis, not product authority.

See **RESEARCH_SOURCES.md** for the full source map, licenses, search log, and repeatable search checklist. A planning run must perform its own search pass and update that file; do not assume this list is exhaustive.

## 10. Design principles

1. **Brownfield first.** The current EW300 EQ/Flash path is working. Add regression tests before refactoring.
2. **Capability truth.** A row exists only if the exact EW300 can support it. "Not available" is a valid row value.
3. **Reuse the shared shell.** EQ/DEVICE tabs, editor, review, graph, session, reconnect, and operation status stay shared.
4. **No fake JA11 clone.** Do not add Ellyn, microphone, UAC, or five programs without exact evidence.
5. **Runtime versus persistent is a product decision.** Apply and Flash must be clearly distinct if the split is implemented.
6. **No arbitrary register console.** Only product allowlisted reads/writes.
7. **Accessibility and truthful states.** Loading, offline, empty, disconnected, permission, stale, failure, recovery, success.
8. **Local only.** No push, PR, release, or public claim.
9. **Small slices.** Each slice has tests and a gate.
10. **Stop on uncertainty.** Never replay an uncertain mutation.

### Documentation and evidence discipline

Documentation is part of the implementation and test loop, not a final cleanup task.

- Keep **FUNCTIONAL_MAPPING.md** current as a living JA11-to-EW300 feature, register, byte, UI, test, status, and confidence map.
- Keep **TEST_EVIDENCE_LOG.md** append-only. Every test run that informs the design gets an entry with source SHA, environment, exact command/bytes, observed result, what it proves, what it does not prove, and artifacts.
- When a durable fact changes, update the maintained repository documents: EW300 protocol notes, capability matrix, validation ledger, and the project runbook where product behavior changes.
- Do not delete historical evidence. Mark superseded entries and link replacements.
- Do not claim VERIFIED_PHONE from an emulator or an older SHA.
- A test is not complete until its mapping row and evidence entry are updated.

## 11. Planning deliverables

The planning thread must return:

- executive decision and recommended scope;
- current-state regression baseline;
- JA11 feature inventory;
- EW300 current inventory;
- gap/parity matrix;
- byte-level protocol map;
- Flash/transaction state machine;
- UI design concept and component tree;
- file-level implementation plan;
- test/qualification plan;
- risk/unknown/decision log;
- living documentation and evidence plan;
- internet research pass and source-quality update;
- new gate proposal;
- one next action.

## 12. Hardware and execution order

The EW300 stays attached to the Mac until the final phase. Do not request the Pixel early.

| Phase | Hardware | Purpose | Entry condition | Exit evidence |
| --- | --- | --- | --- | --- |
| 1 | EW300 on Mac | Read-only identity/state probes, golden-frame validation, protocol conformance | Plan accepted; no writes | Raw bytes, fixtures, probe notes |
| 2 | Mac + JVM | Codec, flasher, session, and capability tests | Phase 1 fixtures available | Unit-test results |
| 3 | Android emulator | App UI, fake-transport flows, accessibility, stale/error states | App builds; unit tests pass | Emulator screenshots and UI tests |
| 4 | Android emulator with USB passthrough (optional) | Real Android USB path if the emulator can claim the EW300 | Phase 3 stable; passthrough works | Go/no-go note |
| 5 | Local debug APK | Installable candidate for the Pixel | Local gates pass | APK path, package, version, SHA-256 |
| 6 | Pixel over wireless ADB (USB fallback) | Final real-device read-only check, then the approved bounded physical test | Readiness gate below is signed | Install/launch log, readback, restoration |

Readiness gate for phase 6:

- the Mac read-only probe harness works and is documented;
- golden-frame and protocol fixtures pass;
- emulator UI and fake-transport tests pass;
- the debug APK is built and its exact SHA-256 is recorded;
- the physical test plan is approved, or the session is read-only;
- the EW300 can be moved from the Mac to the Pixel safely;
- rollback and exact-baseline restoration are documented;
- the owner has a one-page test guide.

If the emulator cannot claim the EW300 through USB passthrough, document that and use the fake transport. Do not block the plan on emulator USB hardware access.

The recommended handoff is:

1. **Planning thread** reads this brief and produces the plan. No code edits.
2. Owner reviews and accepts the plan.
3. **New task gate** via seed-task; seal it; record base SHA, files, tests, and approvals.
4. **Fresh DeepSeek execution thread** in an isolated worktree from current origin/main. One writer.
5. Execution implements in slices:
   - preserve current tests;
   - protocol/codec changes;
   - flasher/session changes;
   - UI changes;
   - Mac-side read-only probe/golden checks;
   - emulator UI tests;
   - lint/build;
   - debug APK.
6. Execution uses the Mac-attached EW300 for read-only probing and protocol conformance, and the emulator for UI/fake-transport tests.
7. Execution installs the debug APK on the Pixel only in the final phase, after the readiness gate passes.
8. Physical writes only after the bounded test plan is approved and the exact candidate is identified.
9. Final report: branch, SHA, APK, tests, emulator evidence, phone install state, remaining limits, next action.

Autonomy contract for execution:

- Continue through implementation, tests, build, and emulator without asking for permission at every step.
- Local test, build, and emulator iteration is pre-authorized. Do not pause after each test or ask whether to try another one.
- A failed test, a passed test, a rebuild, an emulator restart, a new test case, or “this will take more work” is not a stop condition.
- Diagnose failures, fix them, rerun the relevant tests, and continue through the next slice.
- Stop only for a material product choice not covered by the accepted plan, a required physical action or unauthorized hardware write, a destructive/irreversible action, a gate turn/stop limit, a hard blocker, or evidence that contradicts the plan.
- Do not ask “should I continue?” while safe in-scope work remains.
- Set a generous gate turn cap and stop time before execution; the cap is a safety boundary, not a per-test checkpoint.
- Keep a progress log in the task folder so a fresh thread can resume after compaction.
- Update FUNCTIONAL_MAPPING.md and TEST_EVIDENCE_LOG.md after every slice; update maintained docs when facts change.
- Do not push to GitHub.
- Do not weaken tests or claim hardware evidence from an emulator.

## 13. Test strategy

Current-behavior preservation:

- five-band Peak readback;
- edit/Review;
- Apply;
- Flash;
- Reset;
- reconnect;
- Personal EQ capture;
- output gain;
- capability/operation report;
- EQ/DEVICE UI states.

New-behavior tests:

- A/B register golden bytes for Peak/LS/HS;
- signed gain, frequency, Q, type bits;
- read/write/readback fixtures;
- AutoEq/squig EW300 versus EW300 DSP measurement cross-check for shelf acoustics, with rig differences recorded;
- 0x53 Save sequence and one-Save invariant;
- re-enumeration and replacement session;
- no replay after uncertainty;
- filter type UI and accessibility;
- Apply vs Flash status;
- About/read-only identity;
- unsupported-row states.
- documentation and mapping updates after each slice.

Hardware/test ladder:

1. Mac-attached EW300: read-only registers, identity/state capture, golden frames, one-command probe report.
2. Mac JVM: protocol, codec, flasher, session, and capability tests.
3. Android emulator: UI, fake transport, accessibility, error/stale states.
4. Android emulator with USB passthrough: optional; document whether it works.
5. Local debug APK: build, install on emulator, launch.
6. Pixel over wireless ADB: final read-only check; then the approved bounded physical test.

The Mac probe harness should be a small read-only tool (Python/hidapi or Swift/IOHIDManager) that opens interface 3, sends only 0x52 reads, records raw request/response bytes, and never sends 0x57, 0x53, or 0x43. The one-off probe above already demonstrates the path; the execution plan should decide whether to check the tool into the repo or keep it as a local test aid.

Emulator USB passthrough is a feasibility question, not an assumption. If the emulator cannot claim the EW300, the app tests use the fake transport and the Mac harness covers the real protocol.

Do not use the Pixel as the primary development loop. Do not request the phone until the readiness gate is satisfied.

Local commands should be confirmed from the current repo, typically:

- ./tools/codex-android :app:testDebugUnitTest
- ./tools/codex-android :app:lintDebug
- ./tools/codex-android :app:assembleDebug
- ./tools/codex-android :app:connectedDebugAndroidTest (emulator or Pixel, as configured)

Do not claim emulator or Pixel evidence unless the command actually ran.

## 14. Open questions

- Can run-mode identity distinguish KT02H20 from KT0211L/KT02F20? If not, what is the minimum non-invasive evidence?
- Do type 3/4 produce true Low Shelf/High Shelf acoustics on the exact EW300? What are the Q/gain/frequency semantics?
- Does 0x24 bit 0 enable EQ? What do the other bits mean? Why does this unit read 1 while family docs mention 3?
- Is a session-only Apply safe and useful? Does the EW300 revert on power loss without 0x53? The early volatile tests suggest yes, but product behavior needs a bounded plan.
- Does the EW300 have any multiple-program/slot capability? If not, how should the UI explain the difference from JA11?
- Which About fields are readable safely? Firmware version/date and identity are present; sample rate/UAC mode need evidence.
- Which physical test is truly necessary for shelf types or Apply/Flash, and which parts can be proven offline?
- Can the Android emulator claim the EW300 through USB passthrough? If not, what is the documented fake-transport fallback?
- Is the phase-6 readiness checklist complete before the EW300 leaves the Mac?
- What is the exact local debug artifact and install path for the Pixel?

## 15. Non-goals

- Ellyn / DCR / noise-gate tuning on EW300.
- Firmware cross-flash, bootloader, erase, calibration, recovery.
- Arbitrary register console.
- USB identity writes.
- Microphone, UAC, balance, gain-mode, or output-topology controls without exact evidence.
- Invented multiple EQ programs/slots.
- JA11 reconstruction-filter emulation.
- Production release, signing, PR, push, or public support claim.
- Repeating E001, E037-E040, or E043-E046 physical tests.

## 16. Definition of done for the experimental build

A local debug build is done when:

- current EW300 EQ/Flash behavior still passes its regression tests;
- the accepted parity rows are implemented or truthfully marked unavailable;
- new protocol/UI tests pass;
- lint and build pass;
- the emulator UI test passes;
- the Mac read-only probe and golden-frame checks pass;
- a debug APK is produced and its SHA-256 is recorded;
- the final wireless-ADB Pixel session has run read-only first, and any approved physical test has a recorded outcome and restoration;
- the owner has a one-page test guide;
- the repo/worktree state and exact artifact are recorded;
- no GitHub push, release, or unsupported hardware claim exists.

## 17. Source links

- KT Micro KT02H20: https://www.ktmicro.com/?usb_104/394.html
- KT Micro USB audio series: https://www.ktmicro.com/?usb_104/
- ktmicro-tools: https://github.com/gxcreator/ktmicro-tools
- devicePEQ: https://github.com/jeromeof/devicePEQ
- ja11-config: https://github.com/Ircama/ja11-config
- kt0231h field guide: https://github.com/eyad-elghareeb/kt0231h
- Glacier EQ: https://github.com/Bukutsu/glacier-eq
- eqcaps: https://github.com/potatosalad775/eqcaps
- AuraPEQ: https://github.com/mandy321/Audiocular-Aura
- KTMicro dongle comparison notes: https://github.com/txtsamu/claude-research
- SIMGOT EW300 UAC2 regression note: https://github.com/lokal-music/mobile/blob/main/docs/audio/simgot-ew300-usb-dac-regression.md
- AutoEq: https://github.com/jaakkopasanen/AutoEq
- ParkWardRR cross-flash toolkit: https://github.com/ParkWardRR/fiio-ja11-jcally-jm12-moondropkt02-kt02h20-dac-amp-toolkit
- kt02h20-flasher: https://github.com/txtsamu/kt02h20-flasher
- Android USB host: https://developer.android.com/guide/topics/connectivity/usb/host

