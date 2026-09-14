# TRN Black Pearl v0.6 — Restore defaults hands-on checklist

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
