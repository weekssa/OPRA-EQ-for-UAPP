# TRN Black Pearl — v0.6 DAC reconstruction-filter write qualification

Status: **READY FOR PHYSICAL TEST — NOT YET QUALIFIED**

This is the first write-capable Black Pearl DEVICE qualification gate for v0.6. It qualifies **only the DAC reconstruction-filter write** implemented in the exact candidate below. It does not qualify balance, microphone gain, amplifier topology, gain mode, playback/global gain, firmware, persistence, or any generic Save-to-Flash behavior.

The already completed read-only DEVICE qualification remains authoritative for Black Pearl read semantics and does not need to be repeated.

## Exact candidate provenance

- Repository: `weekssa/opra-eq-for-uapp`
- Branch: `v0.6-my-dac`
- Candidate commit SHA: `f7a3179d8124c70175e377c8673958e2c83d28a3`
- App version: `0.6.0`
- Immutable exact-candidate APK: `https://raw.githubusercontent.com/weekssa/OPRA-EQ-for-UAPP/mobile-test-apk/candidates/EQ-Library-v0.6.0-beta-f7a3179.apk`
- Immutable checksum: `https://raw.githubusercontent.com/weekssa/OPRA-EQ-for-UAPP/mobile-test-apk/candidates/EQ-Library-v0.6.0-beta-f7a3179.apk.sha256`
- Signed APK SHA-256: `7adc9e310794a74f5838e231195f167d76f690fa997b47cd5a7c4fff68517a2e`
- Signer certificate SHA-256: `65c1c1256dae3c49e3548f334c91f0ba991969e9be9e0b223ba4e253d2114747`
- Android CI: run #1342 — **PASS**
- CodeQL: run #1223 — **PASS**
- Catalog currentness CI: run #1512 — **PASS**
- Priority community coverage CI: run #999 — **PASS**
- Signed EQ Library Beta Candidate: run #1016 / ID `34767883804` — **PASS**
- Signed-beta artifact ID: `10320827978`
- Signed-beta artifact ZIP SHA-256: `7d09ae7ac3a7d92c1be2a436ddee8120bb8ca002221e6d34c01fa9d45ca29aee`
- Test device: Pixel 9
- DAC: TRN Black Pearl
- Test date: `TBD`
- Tester: Project owner

Moving `EQ-Library-mobile-test.apk` or `EQ-Library-v0.6.0-mobile-test.apk` filenames are not qualification provenance. Use the immutable exact-candidate APK above.

## Candidate behavior under test

The product flow is intentionally narrow:

`fresh complete read -> choose filter -> explicit Current/New review -> Apply -> fresh complete baseline -> filter-only write -> complete readback -> requested filter verification -> unrelated-state verification`

The candidate must not:

- invoke the Black Pearl generic Save-to-Flash command;
- claim power-cycle persistence;
- expose balance, mic-gain, topology, gain-mode, or playback/global-gain writes;
- silently retry after USB-session replacement;
- present requested local state as current before readback verification;
- report success if any unrelated qualified read field changes unexpectedly.

## Safety setup

1. Stop playback before every write step.
2. Keep listening volume conservative before reconnecting or resuming playback.
3. Do not change gain mode, amplifier topology, microphone gain, balance, playback/global gain, EQ bands, or firmware during this qualification.
4. Use the independent Black Pearl controller only after EQ Library has released the USB session; do not leave two controllers actively connected at once.
5. **Do not use Save to Flash** in the independent controller during this checklist.
6. Choose only a normal reconstruction-filter option presented by EQ Library. Do not infer or enter raw protocol values.

## 1. Exact-candidate recognition and fresh baseline

1. Install the immutable candidate recorded above.
2. Stop playback and connect the Black Pearl to the Pixel 9 through the normal Android USB flow.
3. Open **My DAC -> DEVICE**.
4. Tap **Read candidate controls** once.
5. Verify the panel reports **Current session read** and provides one complete current snapshot.
6. Record the current reconstruction filter and all unrelated values used for transaction verification:
   - firmware/version;
   - gain mode;
   - amp topology;
   - mic gain;
   - balance;
   - playback/global gain.
7. Verify only the DAC reconstruction-filter control is write-interactive; later Black Pearl DEVICE writes remain hidden/read-only.

PASS / FAIL: **TBD**

Observed baseline: **TBD**

Notes: **TBD**

## 2. Review and Cancel cause no write

1. With playback stopped, choose a different normal DAC reconstruction filter shown by EQ Library.
2. Verify an explicit review shows the actual **Current** filter and the intended **New** filter before Apply.
3. Tap **Cancel**.
4. Perform a fresh current read.
5. Verify the reconstruction filter is still the baseline value and every unrelated field remains unchanged.

PASS / FAIL: **TBD**

Reviewed alternate filter: **TBD**

Notes: **TBD**

## 3. Apply one targeted filter change and verify readback

1. Stop playback.
2. Choose the same known alternate filter used in Section 2.
3. Confirm the review again shows the correct Current/New values.
4. Tap **Apply** once.
5. Verify EQ Library does not report success until the transaction completes its fresh baseline, write, complete readback, and verification.
6. Verify the resulting current snapshot reports the requested reconstruction filter.
7. Verify firmware/version, gain mode, topology, mic gain, balance, and playback/global gain still match the Section 1 baseline.
8. If the app reports a mismatch, stale session, disconnect, transfer failure, or unrelated-state change, record the exact result and stop; do not retry blindly.

PASS / FAIL: **TBD**

Requested filter: **TBD**

Verified readback: **TBD**

Notes: **TBD**

## 4. Independent semantic comparison

1. Close/release EQ Library's Black Pearl USB session as needed.
2. Connect with the trusted independent Black Pearl controller.
3. Verify its current reconstruction filter agrees with the filter EQ Library just verified.
4. Verify the conveniently observable unrelated settings still match the Section 1 baseline.
5. Do not tap **Save to Flash** and do not change any other setting.

PASS / FAIL: **TBD**

Notes: **TBD**

## 5. Restore the original filter through EQ Library

1. Release the independent controller and reconnect the exact EQ Library candidate.
2. Perform a fresh current read before writing.
3. Stop playback.
4. Choose the original Section 1 reconstruction filter.
5. Review Current/New and tap **Apply** once.
6. Verify success only after complete readback.
7. Verify the original filter is current again and all unrelated values remain unchanged.
8. If practical, perform one final independent observation of the restored filter without using Save to Flash.

PASS / FAIL: **TBD**

Restored filter: **TBD**

Notes: **TBD**

## 6. Reconnect freshness and persistence wording

This qualification does **not** require or claim power-cycle persistence because the candidate intentionally does not issue a generic Save-to-Flash command.

1. After restoring the baseline filter, disconnect and reconnect the Black Pearl normally.
2. Verify retained old-session values are not silently treated as current before a fresh read.
3. Perform a fresh read and verify the app reports actual current hardware state.
4. Verify the UI does not describe the filter write as saved to flash, permanent, or power-cycle persistent.

PASS / FAIL: **TBD**

Notes: **TBD**

A future persistence claim, if desired, requires independent evidence and its own product/qualification decision. Lack of a persistence claim is not a failure for this transient verified control.

## 7. Failure/session-interruption behavior

Deliberate mid-write USB disconnection is optional and should not be performed merely to create risk. Automated regression coverage already exercises stale generation, session replacement, transfer failure, readback mismatch, invalid values, and unrelated-state-change rejection.

PASS / FAIL / NOT EXERCISED: **TBD**

Notes: If no safe physical failure injection is useful, record **NOT EXERCISED** and rely on the exact-candidate automated gates above.

## Qualification decision

A DAC reconstruction-filter write **PASS** requires:

- Sections 1–6 pass;
- Section 7 either passes or is explicitly Not exercised with automated failure/session coverage green;
- Cancel performs no write;
- Apply changes only the requested reconstruction filter;
- EQ Library reports success only after complete verified readback;
- unrelated current DEVICE values remain unchanged;
- independent controller observation agrees semantically with the requested/verified filter;
- the original filter is restored and verified at the end;
- no Save-to-Flash or persistence claim is used to obtain the result;
- the exact tested signed candidate is the one recorded above.

Overall DAC reconstruction-filter write qualification: **PENDING**

Passing this checklist qualifies only this one Black Pearl DEVICE write. The next write candidate remains hidden until separately admitted and physically qualified.