# TRN Black Pearl v0.6 — Restore defaults hands-on checklist

Status: **FOCUSED PHYSICAL QUALIFICATION PENDING**

This checklist validates the project-owner selected **EQ Library Restore defaults** behavior for TRN Black Pearl. It does **not** claim that the selected values are TRN factory defaults.

Use only an exact signed candidate whose source SHA, APK SHA-256, and signer certificate have been verified. Primary test device: Pixel 9.

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

- [ ] Install/update the exact signed candidate without clearing app data.
- [ ] Connect TRN Black Pearl normally and wait for fresh My DAC -> DEVICE state.
- [ ] Establish a clearly non-default DEVICE state for at least one target, if necessary, so the restore action has something observable to change.
- [ ] If practical, set microphone gain to a non-zero value before the checkbox-OFF test so the 0 dB restore is directly observable.
- [ ] Confirm the current hardware EQ is non-flat before the checkbox-OFF test, or flash a known non-flat saved EQ first.

## B. Cancel — no write

- [ ] Open My DAC -> DEVICE -> **Reset device** -> **Restore defaults**.
- [ ] Confirm the dialog lists 50% / FAST-LL / HIGH / CLASS AB / Centered / **0 dB microphone gain**.
- [ ] Confirm **Also reset EQ to flat** starts OFF.
- [ ] Tap **Cancel**.
- [ ] Verify DEVICE values did not change.
- [ ] Verify hardware EQ did not change.

## C. Restore defaults with EQ checkbox OFF

- [ ] Reopen Restore defaults and leave **Also reset EQ to flat** OFF.
- [ ] Tap **Reset** once.
- [ ] Do not manually Refresh or resend settings while the restore is running.
- [ ] Verify final Volume displays **50%**.
- [ ] Verify DAC filter = **FAST-LL**.
- [ ] Verify Gain = **HIGH**.
- [ ] Verify Amplifier = **CLASS AB**.
- [ ] Verify Balance = **Centered**.
- [ ] Verify Microphone gain = **0 dB**.
- [ ] Verify the previously non-flat hardware EQ is still unchanged/non-flat.
- [ ] Verify the app reports success only after the requested DEVICE state is visible as verified current state.

## D. Persistence after reconnect

- [ ] Unplug the Black Pearl normally.
- [ ] Reconnect it without closing the app and without tapping app Connect/Refresh after physical reattach.
- [ ] Wait for the normal automatic replacement-session read.
- [ ] Verify Volume still displays **50%**.
- [ ] Verify FAST-LL / HIGH / CLASS AB / Centered remain present.
- [ ] Verify Microphone gain remains **0 dB**.
- [ ] Verify no cached-state push or spurious Applying/Flashing state appears during reconnect.

## E. Restore defaults with EQ checkbox ON

- [ ] Ensure the hardware EQ is non-flat again before this test.
- [ ] Open Restore defaults and enable **Also reset EQ to flat**.
- [ ] Tap **Reset** once.
- [ ] Verify the same DEVICE targets: 50% / FAST-LL / HIGH / CLASS AB / Centered / **0 dB microphone gain**.
- [ ] Verify the hardware EQ is now **Flat**.
- [ ] Verify saved EQs in My EQs / EQ Library are still present and unchanged.
- [ ] Disconnect/reconnect once more and verify the restored DEVICE state and Flat hardware EQ persist.

## F. Failure truthfulness

Automated tests cover stale-session/readback/transaction failure handling. Do not deliberately create a risky mid-write USB failure for routine owner testing unless a later protocol investigation specifically requires it.

During ordinary testing, if any disconnect or verification error happens naturally:

- [ ] the app must stop rather than automatically resend the failed setting write;
- [ ] the app must not report **defaults restored** unless all requested targets were verified;
- [ ] after reconnect, fresh hardware readback—not cached requested state—must be shown.

## Result

- [ ] **PASS** — Sections A-E complete with expected values and no unexpected state changes.
- [ ] **FAIL / STOPPED** — record the exact step, visible state, and whether the DAC remained connected.

A PASS qualifies the EQ Library **Restore defaults** behavior on the exact signed candidate tested. It does not establish the selected values as TRN factory defaults.
