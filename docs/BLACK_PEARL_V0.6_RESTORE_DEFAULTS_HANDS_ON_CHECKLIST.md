# TRN Black Pearl v0.6 — Restore defaults hands-on checklist

## 2026-09-15 corrective candidate — PASS

The project owner completed the focused Pixel 9 / TRN Black Pearl retest on exact signed source `eb1980076009001b5216ffbb531de8a28a4780eb` and reported **SUCCESS**.

The retest confirmed:

- Restore defaults completed normally on the first attempt and reached **50% / FAST-LL / HIGH / CLASS AB / Centered / microphone 0 dB**.
- The optional EQ-flat behavior and saved My EQs remained protected as specified.
- Reconnect persistence remained correct.
- The revised My EQs managed-headphone detail and General EQs pages were materially less cramped, with the revised action/help hierarchy.

Evidence category: **OWNER-REPORTED**.

Exact signed APK: https://raw.githubusercontent.com/weekssa/OPRA-EQ-for-UAPP/mobile-test-apk/candidates/EQ-Library-v0.6.0-beta-eb19800.apk

APK SHA-256: `dabf4bcdddf69853b09793f5a94bec0a3af7efb430f1cdfe26ffc35a93b783ad`

The exact candidate passed Android CI #1538, CodeQL #1420, Catalog currentness CI #1826, Priority community coverage CI #1311, and Signed EQ Library Beta Candidate #1213. The signing workflow verified the pinned certificate `65c1c1256dae3c49e3548f334c91f0ba991969e9be9e0b223ba4e253d2114747`.

This PASS closes the corrective Black Pearl physical gate. It does not establish TRN factory-default semantics and does not qualify FiiO JA11 hardware behavior. PR #16 remains open and draft; merge, release, and publication remain explicit owner-authorization gates.


Status: **CORRECTIVE BETA — corrective physical retest PASS; final provenance and release authorization remain.**

## 2026-09-15 follow-up — failed candidate and corrective beta

Exact owner-tested source `431cbfa58eebe8c29fe8624a506106f728b613cc` failed the latest physical review.
APK SHA-256: `fbf7a30731c3159e592104d283953f7427357370158e7c6cc772c1a362bbf375`.
Evidence category: **OWNER-REPORTED**, with screenshot details transcribed in the owner's handoff (original images are not in this checkout).

- Restore defaults stopped with a verification error and left Volume at 0%; a later manual attempt succeeded.
- My EQs managed-headphone detail and General EQs used too much screen space before actual EQ content.
- This is **FAIL**, not a pass for premium items 4–7. The older exact `b0340842` restore and `b952aed8` premium items 1–3 observations remain historical evidence, not qualification of this correction.

Code inspection established a screen-state race: a newly issued step could be judged against the previous write's snapshot before the combined StateFlow delivered the new write state. The safety-volume write and final-volume write also share a control ID. Per-write generation tracking now requires completion of the exact newly issued cycle, including verified no-ops; stale emissions wait without sending another command. The restore is pinned to its starting USB session and final success checks every requested DEVICE value.

The correction changes restore orchestration and requires a focused signed-candidate physical retest. It does not add protocol commands, delays, write retries, or weaken readback/persistence checks.

**Genuine failure policy:** stop on an actual failed/unverified operation. Do not automatically raise volume from the safety floor while gain/topology, persistence, or the USB session is uncertain. Compensation is not justified by a verified volume opcode alone: that would raise listening level in an unverified mixed state. The error explains that volume may remain at 0% and directs the user to refresh DEVICE, then deliberately adjust Volume. The false-abort race is corrected so a healthy restore can reach its requested 50% normally.

Presentation: compact title/back row; neutral textual Connected state; notification preference and headphone removal in Headphone options; existing Manage presets and profile callbacks retained; profile actions wrap at large fonts; General EQ help is on demand; Save/Hide only appear with a selection; Select all/none stay discoverable; headers and content share scrolling so enlarged text cannot consume a fixed content viewport. Material touch targets remain at least 48dp.

Software regressions include delayed pre-write screen emissions across all seven steps, verified no-ops, distinct safety/final playback cycles, refusal of a different completed cycle, and a real transfer failure after the safety-volume write with no automatic retry or compensating volume increase.

Current gate: new exact-source Android CI, CodeQL, catalog currentness, priority community coverage, and signed-beta provenance, then focused Pixel 9 testing. The corrective physical retest PASS is recorded above. PR #16 stays open/draft/unmerged; public release remains v0.5.0.

## Historical record (exact evidence pins retained)

Status: **PASS — OWNER-REPORTED on exact signed candidate `b0340842dd88dc85613d9441fcc72ceadf877b20`**

This checklist validates the project-owner selected **EQ Library Restore defaults** behavior for TRN Black Pearl. It does **not** claim that the selected values are TRN factory defaults.

Primary test device: Pixel 9.

Exact signed candidate:

`https://raw.githubusercontent.com/weekssa/OPRA-EQ-for-UAPP/mobile-test-apk/candidates/EQ-Library-v0.6.0-beta-b034084.apk`

APK SHA-256:

`dcae4f54881976af70e93c2c796b6993697d1a0a8f333698e9d925933c61ff37`

Signer:

`CN=OPRA EQ for UAPP, O=weekssa`

Signer certificate SHA-256:

`65c1c1256dae3c49e3548f334c91f0ba991969e9be9e0b223ba4e253d2114747`

Evidence date: **2026-09-14**. Evidence category: **OWNER-REPORTED**.

## Expected restore contract

With **Also reset EQ to flat** OFF, Restore defaults must establish:

- Volume: **50% presentation**;
- DAC filter: **FAST-LL**;
- Gain: **HIGH**;
- Amplifier: **CLASS AB**;
- Balance: **Centered**;
- Microphone gain: **0 dB**;
- current hardware EQ: **unchanged**.

With **Also reset EQ to flat** ON, the same DEVICE targets must be established and the current hardware EQ must additionally become flat using the already-qualified Reset EQ to flat transaction.

Saved EQs in EQ Library must never be deleted or modified by either path.

## Safety contract

The implementation reuses the qualified individual Black Pearl DEVICE write/persist/readback path. It first lowers playback to a conservative qualified minimum, submits each selected restore target through that verified path, and establishes the final displayed 50% volume last. A target that already matches may be treated as a verified no-op by the normal repository path; changed targets are written once and persisted without automatic write retry.

Any disconnect, stale session, transfer failure, readback mismatch, or unrelated-state change must stop the sequence and must not be reported as success.

## A. Baseline

- [x] Install/update the exact signed candidate without clearing app data.
- [x] Connect TRN Black Pearl normally and wait for fresh My DAC -> DEVICE state.
- [x] Establish a clearly non-default DEVICE state for at least one target, if necessary, so the restore action has something observable to change.
- [x] Set/confirm microphone gain so the **0 dB** restore target is observable.
- [x] Confirm the current hardware EQ is non-flat before the checkbox-OFF test, or flash a known non-flat saved EQ first.

## B. Cancel — no write

- [x] Open My DAC -> DEVICE -> **Reset device** -> **Restore defaults**.
- [x] Confirm the dialog lists 50% / FAST-LL / HIGH / CLASS AB / Centered / **0 dB microphone gain**.
- [x] Confirm **Also reset EQ to flat** starts OFF.
- [x] Tap **Cancel**.
- [x] Verify DEVICE values did not change.
- [x] Verify hardware EQ did not change.

## C. Restore defaults with EQ checkbox OFF

- [x] Reopen Restore defaults and leave **Also reset EQ to flat** OFF.
- [x] Tap **Reset** once.
- [x] Do not manually Refresh or resend settings while the restore is running.
- [x] Verify final Volume displays **50%**.
- [x] Verify DAC filter = **FAST-LL**.
- [x] Verify Gain = **HIGH**.
- [x] Verify Amplifier = **CLASS AB**.
- [x] Verify Balance = **Centered**.
- [x] Verify Microphone gain = **0 dB**.
- [x] Verify the previously non-flat hardware EQ is still unchanged/non-flat.
- [x] Verify the app reports success only after the requested DEVICE state is visible as verified current state.

## D. Persistence after reconnect

- [x] Unplug the Black Pearl normally.
- [x] Reconnect it without closing the app and without tapping app Connect/Refresh after physical reattach.
- [x] Wait for the normal automatic replacement-session read.
- [x] Verify Volume still displays **50%**.
- [x] Verify FAST-LL / HIGH / CLASS AB / Centered remain present.
- [x] Verify Microphone gain remains **0 dB**.
- [x] Verify no cached-state push or spurious Applying/Flashing state appears during reconnect.

## E. Restore defaults with EQ checkbox ON

- [x] Ensure the hardware EQ is non-flat again before this test.
- [x] Open Restore defaults and enable **Also reset EQ to flat**.
- [x] Tap **Reset** once.
- [x] Verify the same DEVICE targets: 50% / FAST-LL / HIGH / CLASS AB / Centered / **0 dB microphone gain**.
- [x] Verify the hardware EQ is now **Flat**.
- [x] Verify saved EQs in My EQs / EQ Library are still present and unchanged.
- [x] Disconnect/reconnect once more and verify the restored DEVICE state and Flat hardware EQ persist.

## F. Failure truthfulness

Automated tests cover stale-session/readback/transaction failure handling. Do not deliberately create a risky mid-write USB failure for routine owner testing unless a later protocol investigation specifically requires it.

During ordinary testing, if any disconnect or verification error happens naturally:

- [ ] the app must stop rather than automatically resend the failed setting write;
- [ ] the app must not report **defaults restored** unless all requested targets were verified;
- [ ] after reconnect, fresh hardware readback—not cached requested state—must be shown.

These failure-injection items were not intentionally induced during the owner test; their software behavior remains covered by automated transaction regressions.

## Result

- [x] **PASS** — Sections A-E complete with expected values and no unexpected state changes.
- [ ] **FAIL / STOPPED** — record the exact step, visible state, and whether the DAC remained connected.

**Result: PASS, OWNER-REPORTED, on exact signed source `b0340842dd88dc85613d9441fcc72ceadf877b20`.**

This PASS qualifies the EQ Library **Restore defaults** behavior on the exact signed candidate tested. It does not establish the selected values as TRN factory defaults.
