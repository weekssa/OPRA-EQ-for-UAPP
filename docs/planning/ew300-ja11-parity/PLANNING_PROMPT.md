# PLANNING PROMPT — EW300 / JA11 My DAC parity

You are in **planning mode**. Do not edit source, commit, push, install an APK, enter a bootloader, or send any write/save command to the DAC. Produce a plan, a design concept, and an evidence packet. Execution will happen later in a fresh DeepSeek execution thread in an isolated worktree.

## Canonical review packet

Project root:

/Users/stephenweeks/Documents/Codex/OPRA-EQ-development

Packet root:

/Users/stephenweeks/Documents/Codex/OPRA-EQ-development/docs/planning/ew300-ja11-parity

Review order: **REVIEW_PACKET.md**, then **README.md**, **BRIEF.md**, this file, **EXECUTION_PROMPT.md**, and **PROGRESS_TEMPLATE.md**.

## Read first

Read these in order:

1. **AGENTS.md** in the repository root.
2. **/Users/stephenweeks/Documents/Codex/Codex-Usage-Policies/WORKFLOW.md**.
3. **docs/planning/ew300-ja11-parity/BRIEF.md** completely.
4. **docs/CHATGPT_PROJECT_RUNBOOK.md**, **docs/PROJECT_STATE.md**, **docs/OPERATING_RUNBOOK.md**, and **docs/GOALS_AND_SCOPE.md**.
5. **docs/EW300_DSP_PROTOCOL_NOTES.md**, **docs/EW300_CAPABILITY_MATRIX.md**, **docs/EW300_VALIDATION_LEDGER.md**, and **docs/V0.7_EW300_DSP_STATUS.md**.
6. **docs/FIIO_JA11_PROTOCOL_NOTES.md**, **docs/FIIO_JA11_CAPABILITY_MATRIX.md**, **docs/research/FIIO_JA11_DIGITAL_FILTER_RESEARCH_HANDOFF.md**, and **docs/implementation/ja11-tuning-prototype-20261103/PLAN.md**.
7. The current Compose and domain code for EW300 and JA11.
8. **FUNCTIONAL_MAPPING.md** and **TEST_EVIDENCE_LOG.md** in this packet.
9. **RESEARCH_SOURCES.md** in this packet.

Use current **origin/main**, not the stale local checkout. The local main was 67 commits behind at research time. Fetch, verify the base SHA, and record it.

## Objective

Design a local-only experimental build that makes the SIMGOT EW300 DSP's five-band DAC PEQ and My DAC surface match the FiiO JA11's five-band User 1 experience as closely as exact evidence allows.

This is a **brownfield extension**. The current EW300 five-band Peak read/edit/Apply/Flash/Reset/reconnect path works on the attached device. Preserve it. Add capability and layout parity around it. Do not rebuild the DAC framework or regress the working path.

The hardware order is part of the design: keep the EW300 on the Mac for as much testing as possible, use Android emulators for app UI and fake-transport flows, and make the wireless-ADB Pixel session the final phase. Do not request the phone until the Mac probe/golden tests, emulator tests, local build, and debug APK are ready.

The deliverable must be concrete enough that a fresh execution agent can implement it without redoing the chip research.

## Verified starting point

- Device: SIMGOT EW300 DSP, USB **31B2:0111**, interface 3, device revision 1.01.
- Chip family: **KT02H20-family** by VID/PID, 0x4B report framing, 5-band register layout, 0x53 save, and 0xE1 magic.
- Exact die/chip-ID string is not exposed in this unit's run mode; 0x00 reads "REG:", not "KT02H20B". Do not enter the bootloader solely to read it.
- Official KT Micro page: 5-band DSP EQ, DRC/noise gate, UAC 1.0/2.0, 384 kHz/32-bit, 2 Mbit internal FLASH, customer secondary development.
- Current read-only snapshot: firmware 1.0.0, build date Jul 1 2024 15:24:49, EQ enabled at 0x24, five DAC bands flat at 1000 Hz / 0 dB / Q 1.0, 0x66 = 0, 0x3A = 5, 0x3B = 13.
- Raw dump: **/Users/stephenweeks/.codex/evidence/ew300/2026-11-04/register-dump.json**.
- Current app: five-band Peak read/write, guarded Apply/Flash/Reset, reconnect, operation trace, Personal EQ capture, EQ/DEVICE tabs.
- Current EW300 DEVICE surface is only: Output gain, Equalizer active Peak bands, Connection.
- JA11 DEVICE surface includes: Device volume, EQ program, Tuning (Ellyn), Startup settings, Microphone, USB & System, and About.

## Critical corrections

- **Ellyn is not an EQ filter band.** It is JA11 Tuning (DCR/gate) at 0x3A/0x3B. On EW300, 0x3A/0x3B are A_ADC/A_DAC analog PGA. Writing JA11's 4/12 values would change analog gain. Exclude Ellyn and document the collision.
- **"Flash" has two meanings.** The product Flash is the 0x53 Save of live DSP RAM to the chip's internal flash. Firmware cross-flashing is a different, unsafe path; exclude it. There is no software readback/backup for the full firmware image.
- **0x5B is USB identity.** Never expose arbitrary register writes. Keep a strict product allowlist.

## Questions the plan must answer

1. Which JA11 My DAC rows can the EW300 support with exact evidence, and which must be truthfully omitted or marked unavailable?
2. Which native filter types can be exposed? The family defines 0 = Peak, 1 = LPF, 2 = HPF, 3 = Low Shelf, 4 = High Shelf. EW300 raw types 1-4 were previously written/read/restored, but acoustics were not proven.
3. Do Low Shelf and High Shelf match the JA11 editor semantics? Do they require NATIVE shelf readback/editor support, or only source-response adaptation? Do not assume.
4. What are the exact frequency, gain, Q, and type semantics for each proposed type? What are the safe ranges?
5. What does 0x24 mean? The attached unit reads 1; some family documentation describes 3 or 0x03/0x02. Resolve before adding EQ on/off.
6. Should the experimental build separate **session-only Apply** from **persistent Flash/Save**? JA11 does; the current EW300 Apply is persistent and sends 0x53. The transport supports volatile writes. This is likely the largest behavioral gap.
7. Does the EW300 have any real EQ program/slot capability? Do not invent a five-program selector because JA11 has one.
8. What exactly is the JA11 layout, row order, labels, states, and accessibility model? What is already shared through the common editor? What must differ?
9. Which existing classes and tests should be extended? No duplicate protocol, session, editor, or optimizer.
10. What can be proven with golden bytes, simulated transport, unit tests, lint, and emulator? What genuinely needs the Pixel and attached EW300?
11. If native shelf support cannot be proven, what is the honest fallback? Peak-only plus layout, Apply/Flash, and About parity is acceptable.

## Required deliverables

Produce one Markdown planning document with these sections.

### 1. Executive decision

State the recommended experimental scope in plain language: what is built, what is matched, what is excluded, what evidence exists, what needs a physical session, and why it is safe enough to prototype locally.

### 2. Current-state and regression baseline

Document the current working EW300 behavior that must not regress:

- five-band Peak readback;
- local edit and Review;
- Apply (currently persistent);
- Flash from My EQs / EQ Library;
- Reset to flat;
- reconnect and final readback;
- Personal EQ capture;
- output gain;
- capability and operation reports;
- EQ/DEVICE tab behavior and stale/error states.

List the exact tests that currently cover these. Add a preservation gate.

### 3. JA11 My DAC feature inventory

One row for every JA11 row/section and interaction:

- Device volume
- EQ program
- Tuning (Ellyn)
- Startup settings
- Microphone / headset remote
- USB Audio Class
- Current sample rate
- About: model, firmware, USB identity, validation
- Refresh / Read controls
- EQ status, graph, per-band rows, Edit, Reset
- Editor: type, frequency, gain, Q, safe gain, Review, Apply
- Any operation/report actions

For each, give the JA11 behavior, the EW300 current behavior, the proposed experimental behavior, the evidence status, and whether it is in or out.

### 4. Byte-level protocol map

Include:

- USB identity and HID interface/endpoints.
- 11-byte report 0x4B envelope.
- Read 0x52, write 0x57, save 0x53, handshake 0x43, extended read 0x08.
- Exact request/response layouts.
- Status-byte behavior and what is actually verified on EW300.
- Timing, settle, and reconnect behavior.
- A register table for 0x00-0xFF with observed values, meaning, confidence, and safe read/write status.
- Band A/B encoding formulas, signed gain, and endianness.
- Type code table and which codes are proposed for product exposure.
- A fixture table: operation, exact TX bytes, expected RX bytes, meaning, source.

### 5. Capability and parity matrix

A row-by-row matrix covering at least:

- five bands;
- per-band type;
- frequency/gain/Q ranges;
- global gain;
- EQ enable/program;
- runtime Apply vs persistent Save;
- Reset;
- readback/reconnect;
- editor layout and Review;
- DEVICE rows;
- About;
- Personal EQ capture;
- Ellyn;
- reconstruction filter;
- microphone/UAC/balance/volume;
- reports and accessibility.

Do not force a row to exist because JA11 has it. "Not available on this EW300" is a valid outcome.

### 6. Flash and transaction state machine

Specify baseline capture, volatile Apply, persistent Flash, final readback, expected re-enumeration, permission handling, stale/uncertain result, restore, and Reset.

Include one-Save maximum, no automatic retry, post-Save reconciliation without a second Save, replacement-session identity, unknown-state display, and how the UI distinguishes "runtime only" from "saved to DAC."

Separate product Save from firmware flashing explicitly.

### 7. UI/UX design concept

Compare the actual Compose implementations of:

- FiioJa11MyDacContent.kt
- Ew300MyDacContent.kt
- DacEqEditorScreen / MyDacEqEditorScreen.kt
- DacDeviceStatus.kt

Produce a component tree or simple wireframe for the proposed EW300 EQ tab and DEVICE tab. Include row order, labels, helper text, value formatting, empty/loading/offline/disconnected/permission/stale/failure/recovery states, and accessibility.

Reuse the shared My DAC shell, EQ/DEVICE tabs, editor, review, graph, and operation status. Do not add a new root destination, second editor, or JA11 clone screen. Do not display a control the exact EW300 does not support.

### 8. Implementation plan

Give a file-level plan:

- exact files to change;
- exact classes/interfaces to extend;
- new tests;
- debug/private build flag if appropriate;
- migration/rollback for the experimental branch;
- explicit statement that no production release, signing change, PR, or push occurs.

Cover at least Ew300Protocol, Ew300Flasher, Ew300EditorApply, Ew300TransactionCoordinator, Ew300CapabilityProfile, HardwareEqDeviceSpecs, HardwareEqEditor, Ew300MyDacContent, and the relevant tests.

### 9. Test and qualification plan

Organize by evidence level:

1. Mac-attached EW300 read-only identity/state probes and golden-frame checks;
2. pure protocol/golden-byte tests;
3. simulated transport;
4. domain/flasher/session tests;
5. AutoEq/squig measurement cross-check for proposed shelf types, with rig limits recorded;
6. Compose UI and accessibility tests;
7. local Gradle build and lint;
8. Android emulator install/launch and fake-transport UI tests;
9. optional Android emulator USB passthrough, if the emulator can claim the EW300;
10. local debug APK;
11. final wireless-ADB Pixel session (USB fallback), only after the readiness gate;
12. bounded physical EW300 test, only after plan approval.

For each, state the exact command or test class, expected result, what it proves, and what it does not prove.

Include a smallest safe physical test only if native shelf types or the Apply/Flash split need it. It must use the exact fingerprint, capture a fresh full baseline, make one narrowly scoped change, verify readback, use at most one Save per phase, tolerate re-enumeration, prove power-cycle persistence, restore the exact baseline, and stop on any permission/detach/mismatch/uncertainty. Never retry an uncertain Save.

Define the final-session readiness gate explicitly: Mac probe harness works; golden/protocol tests pass; emulator UI and fake-transport tests pass; debug APK is built and hashed; physical test plan is approved or the session is read-only; EW300 can be moved safely; rollback/restoration is documented.

Define a persistent execution loop. Local test, build, and emulator iteration is pre-authorized. A failed test, a passed test, a rebuild, an emulator restart, a new test case, or “this will take more work” is not a reason to stop. Execution continues until the accepted gates pass or a real stop condition is reached. Real stop conditions are limited to a material product choice not covered by the accepted plan, a required physical action or unauthorized hardware write, a destructive/irreversible action, a gate turn/stop limit, a hard blocker, or evidence that contradicts the plan.

Documentation is part of the loop. Every code slice and test run must update FUNCTIONAL_MAPPING.md and TEST_EVIDENCE_LOG.md. A test is not complete until its mapping row and evidence entry are updated.

### 10. Risks, unknowns, decision log, and gate proposal

Include risks, unknowns, and the evidence that would close each one. Propose a new task gate with failing-capable checks; do not reuse the existing root GATES.md pointer. Include base SHA, worktree, one writer, one final gpt-6-luna review, selftest, flow-check, scope-guard, local-only branch, stop conditions, and the single next action.

### 11. Living documentation and evidence plan

Define the update cadence and ownership for:

- FUNCTIONAL_MAPPING.md (feature/register/byte/UI/test/status/confidence);
- TEST_EVIDENCE_LOG.md (append-only test records);
- maintained EW300 protocol notes, capability matrix, validation ledger, and project runbook when facts change.

State that documentation is updated during tests, not deferred to the end. Define what counts as a complete evidence entry and how superseded entries are preserved.

### 12. Internet research and source-quality pass

Run a fresh search pass at planning time. Do not rely only on the provided source map.

Search GitHub repositories and code, vendor pages, manuals, app packages, firmware notes, issue trackers, teardown/measurement sources, and Android USB documentation. Look specifically for:

- SIMGOT EW300 or EW300 DSP protocol, firmware, teardown, or control projects;
- KT02H20/KT0211L/KT02F20 register maps, save commands, filter types, and flash behavior;
- five-band KTMicro EQ implementations and UI/state-machine patterns;
- AutoEq or squig.link EW300 and EW300 DSP measurements for shelf acoustic cross-checks;
- Android UAC2/direct-USB lifecycle and wireless-ADB issues.

Record each query, date, source, claim, confidence, license, and decision in RESEARCH_SOURCES.md. Label sources as vendor, exact-device, community, adjacent, or unknown. Do not copy GPL-3.0 or unlicensed code. Treat family behavior as a hypothesis until verified on the exact EW300.

## Scope and non-goals

In scope:

- local experimental Android build;
- five-band DAC PEQ parity;
- native Peak/Low Shelf/High Shelf only if exact evidence supports it;
- session-only Apply vs persistent Flash if evidence supports it;
- JA11-style editor/device layout where the EW300 capability is real;
- About/read-only identity where available;
- byte-level protocol documentation;
- living functional mapping and test evidence log;
- documented internet research and source-quality log;
- Mac-attached read-only probing;
- local tests, Android emulator, and final wireless ADB.

Out of scope:

- Ellyn / DCR / noise-gate tuning on EW300;
- firmware cross-flash, bootloader, erase, calibration, recovery;
- arbitrary register console;
- USB identity writes (0x5B);
- microphone, UAC, balance, gain-mode, or output-topology controls without exact evidence;
- invented multiple EQ programs/slots;
- JA11 reconstruction-filter emulation;
- production release, signing, PR, push, or public support claim;
- repeating E001, E037-E040, or E043-E046 physical tests.

## Acceptance criteria

The plan is acceptable only if it:

1. Treats Ellyn as JA11 DCR/gate tuning, not an EQ band, and excludes it.
2. Explicitly preserves the current working EW300 EQ/Flash path with regression tests.
3. Inventories every JA11 My DAC row and gives each an honest EW300 disposition.
4. Distinguishes exact-device evidence from family and community claims.
5. Gives a byte-level protocol map, not just a feature list.
6. States which native filter types are safe to expose, or why none can be.
7. Resolves or gates the Apply-versus-Flash question.
8. Reuses the existing EW300/shared My DAC architecture.
9. Includes a concrete UI design with truthful states and accessibility.
10. Includes a Mac-first test ladder, an Android emulator plan, and a final wireless-ADB Pixel plan.
11. Excludes firmware flashing and arbitrary register writes.
12. Defines the readiness gate for moving the EW300 from the Mac to the Pixel.
13. Defines the persistent execution loop and explicitly authorizes repeated local tests, builds, and emulator runs without per-test approval.
14. Requires FUNCTIONAL_MAPPING.md and TEST_EVIDENCE_LOG.md updates after every slice, plus maintained-doc updates when facts change.
15. Documents a fresh internet research pass and source-quality decisions, including licenses and limits.
16. Ends with one recommended scope and one next action.

## Output format

Return a Markdown plan with the sections above. Use tables for the evidence matrix, parity matrix, register map, and test matrix. Start with the executive decision. Do not include generic Android advice or restate this prompt.

Begin by restating the objective in one paragraph, then produce the plan.

