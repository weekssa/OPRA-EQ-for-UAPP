# TRN Black Pearl — v0.6 remaining DEVICE controls consolidated qualification

Status: **SOFTWARE BATCH IN VALIDATION — DO NOT TEST UNTIL EXACT SIGNED CANDIDATE IS PINNED**

This checklist is the single hands-on qualification round approved by the project owner for the remaining normal Black Pearl DEVICE controls after the already-qualified DAC reconstruction-filter write.

It covers, independently within one exact signed candidate:

1. balance;
2. microphone gain;
3. amplifier topology;
4. gain mode;
5. playback/global level.

The DAC reconstruction-filter write already passed its own maintained qualification and is not re-tested here. Firmware remains read-only. A failure of one control does not erase valid evidence for controls that completed their own restore-and-verify sequence; each result is recorded independently.

## Exact candidate provenance

Populate only after the final batch software head passes all required automated and signing gates.

- Repository: `weekssa/opra-eq-for-uapp`
- Branch: `v0.6-my-dac`
- Candidate commit SHA: `TBD`
- App version: `0.6.0`
- Immutable exact-candidate APK: `TBD`
- Signed APK SHA-256: `TBD`
- Signer certificate SHA-256: `65c1c1256dae3c49e3548f334c91f0ba991969e9be9e0b223ba4e253d2114747`
- Android CI: `TBD`
- CodeQL: `TBD`
- Catalog currentness CI: `TBD`
- Priority community coverage CI: `TBD`
- Signed EQ Library Beta Candidate: `TBD`
- Signed-beta artifact ID: `TBD`
- Signed-beta artifact ZIP SHA-256: `TBD`
- Test device: Pixel 9
- DAC: TRN Black Pearl
- Test date: `TBD`
- Tester: Project owner

Moving mobile-test filenames are not qualification provenance. Use only the immutable exact-candidate APK recorded above.

## Shared transaction contract

Every tested control follows:

`fresh complete read -> local staged choice -> explicit Current/New review -> Apply once -> fresh complete baseline -> target-only write -> complete readback -> exact requested-value verification -> unrelated-state verification`

The batch candidate must never:

- write while a slider, stepper, text field, or choice sheet is merely being adjusted;
- replay a cached setting after USB reconnect;
- silently retry after USB-session replacement;
- report the staged/requested value as current before readback verification;
- report success when any unrelated DEVICE field changes unexpectedly;
- send the Black Pearl generic Save-to-Flash command for these DEVICE changes;
- claim that these transient DEVICE writes survive a power cycle;
- make firmware write-interactive.

## Safety setup

1. Use only the exact immutable signed candidate recorded above.
2. Start with playback stopped and ordinary listening/downstream volume conservative.
3. Do not change EQ bands or use Direct Flash/Reset during this checklist.
4. Do not change the already-qualified DAC reconstruction filter during this checklist; it should remain `Fast-PC` unless the actual baseline differs, in which case record the actual state and stop before writes.
5. Use an independent Black Pearl controller only after EQ Library has released the USB session.
6. Never tap **Save to Flash** in the independent controller.
7. Make only the small reversible changes specified below.
8. If any step reports an error, mismatch, stale session, disconnect, transfer failure, or unexpected unrelated value change, stop the batch at that point and record the exact screen/result. Do not retry blindly.

## 1. Exact-candidate baseline and exposure check

1. Install the exact immutable candidate.
2. Connect the Black Pearl normally on Pixel 9.
3. Open **My DAC -> DEVICE** and perform one fresh **Refresh device state**.
4. Require **Current session read**.
5. Record the complete baseline. Expected restored reference state from earlier qualification is:
   - firmware `0.6`;
   - DAC filter `Fast-PC`;
   - gain mode `HIGH`;
   - amp topology `CLASS AB`;
   - microphone gain `0 dB`;
   - balance `Centered`;
   - playback/global level raw `512` = `+2.00 dB`, approximately `63%` in the independent controller.
6. Confirm DAC filter is still changeable as an already-qualified control.
7. Confirm the same DEVICE surface now offers reviewed changes for balance, microphone gain, amp topology, gain mode, and playback level.
8. Confirm firmware is read-only.

PASS / FAIL: **TBD**

Observed baseline: **TBD**

Notes: **TBD**

## 2. Balance — one small step and restore

1. Keep playback stopped.
2. Open **Balance**.
3. Stage `-1 dB` from Center. Moving the slider/stepper must not claim a device change.
4. Review must show `Current: Centered` and `New: -1 dB` (left attenuated).
5. Tap **Apply** once.
6. Require verified complete readback showing the requested balance and every unrelated field unchanged.
7. Open Balance again, stage **Center**, Review, and Apply once.
8. Require verified complete readback showing Center restored and every unrelated field unchanged.

PASS / FAIL: **TBD**

Notes: **TBD**

## 3. Microphone gain — one small step and restore

1. Keep playback stopped.
2. Open **Microphone gain**.
3. Stage `-1 dB` from the expected `0 dB` baseline.
4. Review Current/New and tap **Apply** once.
5. Require verified complete readback showing `-1 dB` mic gain and no unrelated changes.
6. Stage `0 dB`, Review, Apply once, and require verified restoration.

PASS / FAIL: **TBD**

Notes: **TBD**

## 4. Amplifier topology — alternate mode and restore

1. Keep playback stopped and downstream/listening volume conservative.
2. Open **Amp topology**.
3. If baseline is `CLASS AB`, stage `CLASS H`.
4. Review the explicit Current/New values and the level-sensitive warning.
5. Tap **Apply** once.
6. Require verified complete readback showing `CLASS H` and every unrelated field unchanged.
7. Stage `CLASS AB`, Review, Apply once, and require verified restoration.

PASS / FAIL: **TBD**

Notes: **TBD**

## 5. Gain mode — lower mode first and restore

1. Keep playback stopped and downstream/listening volume conservative.
2. Open **Gain mode**.
3. From expected `HIGH`, stage `LOW` first. Do not make an upward gain jump as the first test.
4. Review Current/New and the level-sensitive warning.
5. Tap **Apply** once.
6. Require verified complete readback showing `LOW` and every unrelated field unchanged.
7. With playback still stopped, stage `HIGH`, Review, Apply once, and require verified restoration.

PASS / FAIL: **TBD**

Notes: **TBD**

## 6. Playback/global level — small decrease first and exact restore

This is deliberately last because it is level-sensitive. The controller percentage is presentation only; qualification is anchored to the exact raw/dB device value.

1. Keep playback stopped and downstream/listening volume conservative.
2. Open **Playback level**.
3. From expected raw `512` / `+2.00 dB`, use the small-decrease control to stage `+1.50 dB`, which is exact raw `384`.
4. Review must show the exact Current/New dB and raw values; the approximate controller percentage may also be shown as presentation.
5. Tap **Apply** once.
6. Require verified complete readback showing raw `384` / `+1.50 dB` and every unrelated field unchanged.
7. Re-open Playback level and stage the exact baseline `+2.00 dB` / raw `512`.
8. Review, Apply once, and require verified complete readback restoring raw `512` / `+2.00 dB`.
9. Do not test a larger upward jump.

PASS / FAIL: **TBD**

Notes: **TBD**

## 7. Independent semantic comparison

After all controls have been restored through EQ Library:

1. Release EQ Library's Black Pearl USB session as needed.
2. Open the trusted independent controller.
3. Observe only; do not change settings and do not tap Save to Flash.
4. Confirm the final restored reference state agrees semantically:
   - `FAST-PC` / Fast-PC;
   - `High` / HIGH;
   - `Class-AB` / CLASS AB;
   - microphone `0 dB`;
   - balance `Center`;
   - Volume approximately `63%`, corresponding to raw `512` / `+2.00 dB`.

PASS / FAIL: **TBD**

Notes: **TBD**

## 8. Disconnect/reconnect freshness and final read

1. Release the independent controller and reconnect EQ Library if needed.
2. Physically disconnect/reconnect the Black Pearl once.
3. Before a new read, retained old-session state must not be presented as current.
4. Perform one fresh **Refresh device state**.
5. Require **Current session read** and the restored final baseline.
6. Confirm no UI wording says these DEVICE changes were saved to flash, made permanent, or power-cycle persistent.

PASS / FAIL: **TBD**

Notes: **TBD**

## 9. Failure/session-interruption behavior

Deliberate mid-write USB disconnection is **not required** merely to create risk. Automated regression coverage must be green for stale generation, session replacement, failed transfer, readback mismatch, invalid/out-of-range/non-native-grid values, inconsistent balance, unrelated-state-change rejection, and busy/current-session projection.

PASS / FAIL / NOT EXERCISED: **TBD**

Notes: **TBD**

## Per-control qualification decisions

Record each independently after the single hands-on session:

- Balance write: **PENDING**
- Microphone gain write: **PENDING**
- Amp topology write: **PENDING**
- Gain mode write: **PENDING**
- Playback/global level write: **PENDING**

Overall consolidated batch: **PENDING**

A control may be promoted to production-qualified only if its own change, complete readback, unrelated-state preservation, and restoration steps pass on the exact signed candidate. A failure in a later control stops further hands-on writes until investigated, but does not silently convert earlier passed controls into failures.
