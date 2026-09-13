# TRN Black Pearl — v0.6 balance write qualification

Status: **SOFTWARE CANDIDATE IN VALIDATION — DO NOT TEST UNTIL EXACT SIGNED CANDIDATE IS PINNED**

This is the second write-capable Black Pearl DEVICE qualification gate for v0.6. It qualifies **only the left/right balance write** after the already-qualified DAC reconstruction-filter write. It does not qualify microphone gain, amplifier topology, gain mode, playback/global gain, firmware, persistence, or any generic Save-to-Flash behavior.

The completed Black Pearl read-only DEVICE qualification and DAC-filter write qualification remain authoritative and do not need to be repeated unless a later behavior change specifically invalidates them.

## Exact candidate provenance

Populate only after the balance software head passes all required gates and the signed immutable APK is published.

- Repository: `weekssa/opra-eq-for-uapp`
- Branch: `v0.6-my-dac`
- Candidate commit SHA: `TBD`
- App version: `0.6.0`
- Immutable exact-candidate APK: `TBD`
- Immutable checksum: `TBD`
- Signed APK SHA-256: `TBD`
- Signer certificate SHA-256: `65c1c1256dae3c49e3548f334c91f0ba991969e9be9e0b223ba4e253d2114747`
- Android CI: `TBD`
- CodeQL: `TBD`
- Catalog currentness CI: `TBD`
- Priority community coverage CI: `TBD`
- Source health currentness: `TBD`
- Signed EQ Library Beta Candidate: `TBD`
- Signed-beta artifact ID: `TBD`
- Signed-beta artifact ZIP SHA-256: `TBD`
- Test device: Pixel 9
- DAC: TRN Black Pearl
- Test date: `TBD`
- Tester: Project owner

Moving mobile-test filenames are not qualification provenance. Use only the immutable exact-candidate APK recorded above.

## Candidate contract

Balance uses the independently corroborated one-sided Black Pearl representation:

- center = neither channel attenuated;
- a negative signed value attenuates the left channel;
- a positive signed value attenuates the right channel;
- range `-15..+15 dB` in exact `1 dB` hardware steps;
- both left and right balance fields are written for every requested value so stale opposite-side attenuation cannot remain active.

Product flow:

`fresh complete read -> local balance adjustment -> explicit Current/New review -> Apply -> fresh complete baseline -> balance-only write -> complete readback -> requested balance verification -> unrelated-state verification`

The candidate must not:

- write merely because the local slider/stepper moves;
- invoke the Black Pearl generic Save-to-Flash command;
- claim power-cycle persistence;
- expose unqualified mic-gain/topology/gain-mode/playback writes;
- silently retry after USB-session replacement;
- present the staged value as current before readback verification;
- report success if any unrelated qualified DEVICE field changes unexpectedly.

## Safety setup

1. Stop playback before every write step. Balance is not a volume control, but qualification should not create avoidable audible surprises.
2. Keep ordinary listening volume conservative before reconnecting or resuming playback.
3. Do not change DAC filter, gain mode, amp topology, microphone gain, playback/global gain, EQ bands, or firmware during this qualification.
4. Use the independent Black Pearl controller only after EQ Library has released the USB session; never leave two controllers actively connected at once.
5. **Do not use Save to Flash** in the independent controller.
6. Use only the app's deliberate balance UI. Do not enter raw protocol values.
7. The first physical balance change should be only **1 dB away from Center**, then restored to Center after semantic verification.

## 1. Exact-candidate recognition and fresh baseline

1. Install the immutable exact candidate above.
2. Stop playback and connect the Black Pearl to the Pixel 9 through the normal Android USB flow.
3. Open **My DAC -> DEVICE** and perform one fresh device-state read.
4. Verify **Current session read** and record the complete baseline:
   - firmware/version;
   - DAC filter;
   - gain mode;
   - amp topology;
   - mic gain;
   - balance;
   - playback/global gain.
5. Expected starting balance for the controlled qualification is **Centered**. If it is not centered, stop and record the actual state rather than silently normalizing it.
6. Verify DAC filter remains available as an already-qualified write and **balance is the only new qualification-candidate write**. Mic gain, topology, gain mode, and playback remain read-only.

PASS / FAIL: **TBD**

Observed baseline: **TBD**

Notes: **TBD**

## 2. Local adjustment, Review, and Cancel cause no write

1. With playback stopped, open **Change balance**.
2. Stage exactly one 1 dB step away from Center.
3. Verify the adjustment UI clearly shows the staged local value and that moving the slider/stepper itself does not report a hardware change.
4. Continue to **Review**.
5. Verify the review shows the actual **Current** balance and intended **New** balance before Apply.
6. Tap **Cancel**.
7. Perform a fresh device-state read.
8. Verify balance is still Centered and every unrelated baseline field remains unchanged.

PASS / FAIL: **TBD**

Reviewed 1 dB target: **TBD**

Notes: **TBD**

## 3. Apply one 1 dB balance change and verify complete readback

1. Stop playback.
2. Stage the same 1 dB target used in Section 2.
3. Confirm the review again shows the correct Current/New values.
4. Tap **Apply** once.
5. Verify EQ Library does not report success until the transaction completes its fresh baseline, target-only write, complete readback, and verification.
6. Verify the resulting current snapshot reports the intended balance state.
7. Verify firmware/version, DAC filter, gain mode, topology, mic gain, and playback/global gain still match the Section 1 baseline.
8. If the app reports mismatch, stale session, disconnect, transfer failure, inconsistent two-sided balance, or unrelated-state change, record the exact result and stop; do not retry blindly.

PASS / FAIL: **TBD**

Requested balance: **TBD**

Verified readback: **TBD**

Notes: **TBD**

## 4. Independent semantic comparison

1. Close/release EQ Library's Black Pearl USB session as needed.
2. Open the trusted independent Black Pearl controller.
3. Verify its balance presentation semantically agrees with the 1 dB state EQ Library just verified.
4. Verify the conveniently observable unrelated settings still match the Section 1 baseline.
5. Do not tap **Save to Flash** and do not change any other setting.

PASS / FAIL: **TBD**

Notes: **TBD**

## 5. Restore Center through EQ Library

1. Release the independent controller and reconnect the exact EQ Library candidate.
2. Perform a fresh current read before writing.
3. Stop playback.
4. Choose **Center**.
5. Review Current/New and tap **Apply** once.
6. Verify success only after complete readback.
7. Verify balance is Centered again and all unrelated values remain unchanged.
8. If practical, perform one final independent observation of Center without using Save to Flash.

PASS / FAIL: **TBD**

Notes: **TBD**

## 6. Reconnect freshness and persistence wording

1. After restoring Center, physically disconnect and reconnect the Black Pearl normally.
2. Before a new read, verify retained old-session values are not silently treated as current.
3. Perform a fresh read and verify the app reports actual current hardware state.
4. Verify the UI does not describe the balance write as saved to flash, permanent, or power-cycle persistent.

PASS / FAIL: **TBD**

Notes: **TBD**

A future persistence claim requires independent evidence and its own product/qualification decision. Lack of a persistence claim is not a failure for this transient verified control.

## 7. Failure/session-interruption behavior

Deliberate mid-write USB disconnection is optional and should not be performed merely to create risk. Automated regression coverage must be green for stale generation, session replacement, transfer failure, readback mismatch, invalid/out-of-range balance, inconsistent balance readback, unrelated-state-change rejection, and busy-state/current-session projection.

PASS / FAIL / NOT EXERCISED: **TBD**

Notes: **TBD**

## Qualification decision

A Black Pearl balance write **PASS** requires:

- Sections 1–6 pass;
- Section 7 either passes or is explicitly Not exercised with automated failure/session coverage green;
- local staging and Cancel perform no write;
- Apply changes only the requested balance state;
- EQ Library reports success only after complete verified readback;
- unrelated current DEVICE values remain unchanged;
- independent controller observation agrees semantically with the requested/verified 1 dB balance change;
- Center is restored and verified at the end;
- no Save-to-Flash or persistence claim is used to obtain the result;
- the exact tested signed candidate is the one recorded above.

Overall Black Pearl balance write qualification: **PENDING**

Passing this checklist qualifies only balance. Microphone gain remains the next planned Black Pearl write candidate and must not become write-interactive until this gate passes and the qualification policy advances deliberately.
