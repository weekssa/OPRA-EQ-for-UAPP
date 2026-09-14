# TRN Black Pearl — v0.6 remaining DEVICE controls consolidated qualification

Status: **READY FOR RETEST — REPLACEMENT EXACT SIGNED CANDIDATE PINNED**

This checklist is the single hands-on qualification round approved by the project owner for the remaining normal Black Pearl DEVICE controls after the already-qualified DAC reconstruction-filter write.

It covers, independently within one exact signed candidate:

1. balance;
2. microphone gain;
3. amplifier topology;
4. gain mode;
5. playback/global level.

The DAC reconstruction-filter write already passed its own maintained qualification and is not re-tested here. Firmware remains read-only. A failure of one control does not erase valid evidence for controls that completed their own restore-and-verify sequence; each result is recorded independently.

## Replacement exact candidate provenance

The replacement behavior candidate incorporates the fixes prompted by the first interrupted consolidated hardware round.

- Repository: `weekssa/opra-eq-for-uapp`
- Branch: `v0.6-my-dac`
- Candidate commit SHA: `001b5ea5b2199784cfc8cf34941b018bf927caa1`
- App version: `0.6.0`
- Immutable exact-candidate APK: `https://raw.githubusercontent.com/weekssa/OPRA-EQ-for-UAPP/mobile-test-apk/candidates/EQ-Library-v0.6.0-beta-001b5ea.apk`
- Signed APK SHA-256: `90214ff585cb9e413264b9c776c14b8a642d124c18984ceefa7cbe0d90ea0de8`
- Signer certificate SHA-256: `65c1c1256dae3c49e3548f334c91f0ba991969e9be9e0b223ba4e253d2114747`
- Android CI #1374 — **PASS**
- CodeQL #1255 — **PASS**
- Catalog currentness CI #1564 — **PASS**
- Priority community coverage CI #1050 — **PASS**
- Signed EQ Library Beta Candidate #1048 / run `34791576746` — **PASS**
- Signed-beta artifact ID: `10328123788`
- Signed-beta artifact ZIP SHA-256: `8441cbfb793fa93e73ac3a8453fe642523339ab258873e1fa843d368bd1a17e6`
- Signing identity: established EQ Library release identity; APK Signature Scheme v2/v3
- Test device: Pixel 9
- DAC: TRN Black Pearl
- Replacement test date: `TBD`
- Tester: Project owner

Moving mobile-test filenames are not qualification provenance. Use only the immutable exact-candidate APK above for this retest.

## First candidate — retained failed/incomplete evidence

The first consolidated candidate was source `a3837779b5b732f3b388f80a86c0b14fc00e34c5`, immutable APK `EQ-Library-v0.6.0-beta-a383777.apk`.

On 2026-09-13 the project owner stopped that round after observing:

- EQ Library correctly displayed **“The Black Pearl readback did not match the requested setting. The change was not reported as successful.”** rather than claiming success;
- amplifier topology eventually changed only after several user refresh/retry attempts, which does not satisfy the one-Apply -> verified-readback contract;
- normal playback/global-level `0.5 dB` changes did not round-trip reliably; a whole-dB state `+3.00 dB` / raw `768` was observed during the mismatch sequence;
- the device was successfully restored to the reference state: firmware `0.6`, `Fast-PC`, `HIGH`, `CLASS AB`, mic `0 dB`, balance `Centered`, playback raw `512` / `+2.00 dB` / approximately `63%`.

No PASS is inferred for balance, microphone gain, or gain mode from the first candidate's screenshot chronology alone. Amp topology and playback/global level failed that candidate and required a new behavior build.

## Replacement-candidate changes under test

The replacement candidate deliberately narrows and hardens behavior rather than weakening verification:

- normal My DAC playback changes use conservative **whole-dB** product steps; the lower-level 1/256 dB raw protocol and already-qualified EQ/Flash gain behavior are unchanged;
- the playback `-1 dB` / `+1 dB` buttons advance from the **staged draft**, not repeatedly from the original current value;
- fractional normal DEVICE playback targets such as `+1.5 dB` are rejected before USB traffic;
- after one successful write to an unqualified normal DEVICE control, EQ Library allows a bounded same-session **read-only settling/reverification** window before declaring final mismatch;
- settling never resends the hardware write, never crosses USB-session replacement, and never hides a read failure, unrelated-state change, or final mismatch;
- the already-qualified DAC-filter transaction keeps its original immediate single complete readback behavior;
- automated tests cover delayed visibility eventually verifying with exactly one write, never-settled final mismatch with exactly one write, session replacement during settling, and half-dB playback rejection before any read/write.

## Shared transaction contract

Every tested control follows:

`fresh complete read -> local staged choice -> explicit Current/New review -> Apply once -> fresh complete baseline -> target-only write once -> bounded same-session complete readback verification -> exact requested-value verification -> unrelated-state verification`

The candidate must never:

- write while a slider, stepper, text field, or choice sheet is merely being adjusted;
- replay a cached setting after USB reconnect;
- silently retry or resend a write after readback mismatch or USB-session replacement;
- report the staged/requested value as current before readback verification;
- report success when any unrelated DEVICE field changes unexpectedly;
- send the Black Pearl generic Save-to-Flash command for these DEVICE changes;
- claim that these transient DEVICE writes survive a power cycle;
- make firmware write-interactive.

## Safety setup

1. Use only the replacement exact immutable signed candidate recorded above.
2. Start with playback stopped and ordinary listening/downstream volume conservative.
3. Do not change EQ bands or use Direct Flash/Reset during this checklist.
4. Do not change the already-qualified DAC reconstruction filter during this checklist; it should remain `Fast-PC` unless the actual baseline differs, in which case record the actual state and stop before writes.
5. Use an independent Black Pearl controller only after EQ Library has released the USB session.
6. Never tap **Save to Flash** in the independent controller.
7. Make only the small reversible changes specified below.
8. After each Apply, wait for EQ Library to finish verification. **Do not manually Refresh or retry a failed write.**
9. If any step reports an error, mismatch, stale session, disconnect, transfer failure, or unexpected unrelated value change, stop the batch at that point and record the exact screen/result.

## 1. Replacement-candidate baseline and exposure check

1. Install the exact immutable replacement candidate.
2. Connect the Black Pearl normally on Pixel 9.
3. Open **My DAC -> DEVICE** and perform one fresh **Refresh device state**.
4. Require **Current session read**.
5. Record the complete baseline. Expected restored reference state is:
   - firmware `0.6`;
   - DAC filter `Fast-PC`;
   - gain mode `HIGH`;
   - amp topology `CLASS AB`;
   - microphone gain `0 dB`;
   - balance `Centered`;
   - playback/global level raw `512` = `+2.00 dB`, approximately `63%` in the independent controller.
6. Confirm DAC filter is still changeable as an already-qualified control.
7. Confirm the same DEVICE surface offers reviewed changes for balance, microphone gain, amp topology, gain mode, and playback level.
8. Confirm firmware is read-only.

PASS / FAIL: **TBD**

Observed baseline: **TBD**

Notes: **TBD**

## 2. Balance — one small step and restore

1. Keep playback stopped.
2. Open **Balance**.
3. Stage `-1 dB` from Center. Moving the slider/stepper must not claim a device change.
4. Review must show `Current: Centered` and `New: -1 dB` (left attenuated).
5. Tap **Apply once** and then wait; do not Refresh or retry while verification is running.
6. Require verified complete readback showing the requested balance and every unrelated field unchanged.
7. Open Balance again, stage **Center**, Review, and Apply once.
8. Require verified complete readback showing Center restored and every unrelated field unchanged.

PASS / FAIL: **TBD**

Notes: **TBD**

## 3. Microphone gain — one small step and restore

1. Keep playback stopped.
2. Open **Microphone gain**.
3. Stage `-1 dB` from the expected `0 dB` baseline.
4. Review Current/New and tap **Apply once**; wait for verification without manually refreshing.
5. Require verified complete readback showing `-1 dB` mic gain and no unrelated changes.
6. Stage `0 dB`, Review, Apply once, and require verified restoration.

PASS / FAIL: **TBD**

Notes: **TBD**

## 4. Amplifier topology — alternate mode and restore

1. Keep playback stopped and downstream/listening volume conservative.
2. Open **Amp topology**.
3. From `CLASS AB`, stage `CLASS H`.
4. Review the explicit Current/New values and the level-sensitive warning.
5. Tap **Apply exactly once**. Let EQ Library perform its automatic settling/readback sequence; **do not tap Refresh and do not retry the write**.
6. Require verified complete readback showing `CLASS H` and every unrelated field unchanged.
7. Stage `CLASS AB`, Review, Apply once, wait for verification, and require verified restoration.

PASS / FAIL: **TBD**

Notes: **TBD**

## 5. Gain mode — lower mode first and restore

1. Keep playback stopped and downstream/listening volume conservative.
2. Open **Gain mode**.
3. From expected `HIGH`, stage `LOW` first. Do not make an upward gain jump as the first test.
4. Review Current/New and the level-sensitive warning.
5. Tap **Apply once** and wait for automatic verification; do not manually Refresh/retry.
6. Require verified complete readback showing `LOW` and every unrelated field unchanged.
7. With playback still stopped, stage `HIGH`, Review, Apply once, and require verified restoration.

PASS / FAIL: **TBD**

Notes: **TBD**

## 6. Playback/global level — one whole-dB decrease and exact restore

This is deliberately last because it is level-sensitive. Controller percentage is presentation only; qualification is anchored to the exact raw/dB device value.

1. Keep playback stopped and downstream/listening volume conservative.
2. Open **Playback level**.
3. Verify the UI now uses `-1 dB` / `+1 dB` normal DEVICE steps, not `0.5 dB` steps.
4. From expected raw `512` / `+2.00 dB`, tap **-1 dB** once to stage `+1.00 dB`, exact raw `256`.
5. Review must show exact Current/New dB and raw values.
6. Tap **Apply once** and wait for automatic verification; do not manually Refresh/retry.
7. Require verified complete readback showing raw `256` / `+1.00 dB` and every unrelated field unchanged.
8. Re-open Playback level and tap **+1 dB** once to stage the exact baseline `+2.00 dB` / raw `512`.
9. Review, Apply once, and require verified complete readback restoring raw `512` / `+2.00 dB`.
10. Do not test a larger upward jump.

PASS / FAIL: **TBD**

Notes: **TBD**

## 7. Independent semantic comparison

After all controls have been restored through EQ Library:

1. Release EQ Library's Black Pearl USB session as needed.
2. Open the trusted independent Black Pearl controller.
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

Deliberate mid-write USB disconnection is **not required** merely to create risk. Exact replacement-candidate automated coverage is green for stale generation, session replacement, failed transfer, delayed target visibility, final readback mismatch, invalid/out-of-range/non-product-grid values, inconsistent balance, unrelated-state-change rejection, and busy/current-session projection.

PASS / FAIL / NOT EXERCISED: **TBD**

Notes: **TBD**

## Per-control qualification decisions

Record each independently after the replacement hands-on session:

- Balance write: **PENDING**
- Microphone gain write: **PENDING**
- Amp topology write: **PENDING RETEST ON `001b5ea...`**
- Gain mode write: **PENDING**
- Playback/global level write: **PENDING RETEST ON `001b5ea...`**

Overall replacement consolidated batch: **PENDING RETEST**

A control may be promoted to production-qualified only if its own single-write change, complete readback, unrelated-state preservation, and restoration steps pass on the replacement exact signed candidate. Any later failure stops further hands-on writes until investigated but does not silently convert earlier independently completed controls into failures.