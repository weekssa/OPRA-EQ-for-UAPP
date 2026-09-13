# TRN Black Pearl — v0.6 DAC reconstruction-filter write qualification

Status: **PASS — DAC RECONSTRUCTION-FILTER WRITE QUALIFIED**

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
- Test date: `2026-09-13`
- Tester: Project owner

Moving `EQ-Library-mobile-test.apk` or `EQ-Library-v0.6.0-mobile-test.apk` filenames are not qualification provenance. The exact immutable candidate above is the qualification authority.

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

## 1. Exact-candidate recognition and fresh baseline

PASS / FAIL: **PASS**

Observed baseline:

- Firmware/version: `0.6`
- DAC filter: `Fast-PC`
- Gain mode: `HIGH`
- Amp topology: `CLASS AB`
- Mic gain: `+0.00 dB`
- Balance: `Centered`
- Playback/global gain: `+2.00 dB` / raw `512`

Notes: The exact signed candidate was installed on the Pixel 9 and the Black Pearl was connected through the normal Android USB flow. **My DAC -> DEVICE** reported **Current session read** with the complete baseline above. The DAC reconstruction filter was the only new write-interactive Black Pearl DEVICE control; later DEVICE writes remained hidden.

## 2. Review and Cancel cause no write

PASS / FAIL: **PASS**

Reviewed alternate filter: `FAST-LL`

Notes: The review explicitly showed **Current: Fast-PC -> New: FAST-LL** before Apply and described fresh-read/write/readback verification plus the absence of Save-to-Flash. The project owner tapped **Cancel**, then refreshed device state. The fresh current snapshot remained `Fast-PC` with firmware `0.6`, `HIGH`, `CLASS AB`, mic `+0.00 dB`, `Centered`, and playback/global `+2.00 dB` / raw `512` unchanged. Cancel therefore caused no write.

## 3. Apply one targeted filter change and verify readback

PASS / FAIL: **PASS**

Requested filter: `FAST-LL`

Verified readback: `FAST-LL`

Notes: With playback stopped, the project owner reviewed **Current: Fast-PC -> New: FAST-LL** and tapped **Apply** once. The app completed the transaction successfully and did not report an error, mismatch, stale session, or disconnect. The resulting current state verified `FAST-LL`; the project owner confirmed the unrelated baseline values remained unchanged. The candidate did not report success before its verified transaction completed.

## 4. Independent semantic comparison

PASS / FAIL: **PASS**

Notes: After EQ Library released the USB session, the trusted independent Black Pearl controller reported `FAST-LL` as the current reconstruction filter. The project owner also confirmed the observable unrelated settings remained at the established baseline (`High`, `Class-AB`, mic `0 dB`, `Center`, `Volume 63%` where visible). No Save-to-Flash action was used.

## 5. Restore the original filter through EQ Library

PASS / FAIL: **PASS**

Restored filter: `Fast-PC`

Notes: The independent controller was released, EQ Library reconnected and performed a fresh current read, then the project owner reviewed **Current: FAST-LL -> New: Fast-PC** and applied once. EQ Library verified completion and returned the reconstruction filter to `Fast-PC`; the project owner confirmed firmware `0.6`, `HIGH`, `CLASS AB`, mic `+0.00 dB`, `Centered`, and playback/global `+2.00 dB` / raw `512` remained unchanged.

## 6. Reconnect freshness and persistence wording

PASS / FAIL: **PASS**

Notes: After restoring `Fast-PC`, the project owner physically unplugged and reconnected the Black Pearl. Before a new refresh, retained old-session values were not promoted to a fresh current read merely because the device reappeared. After **Refresh device state**, the page returned to **Current session read**, reported actual current `Fast-PC` plus the unchanged baseline, and did not describe the filter write as saved to flash, permanent, or power-cycle persistent.

This qualification does **not** establish power-cycle persistence because the candidate intentionally does not issue a generic Save-to-Flash command. Any future persistence claim requires separate evidence and approval.

## 7. Failure/session-interruption behavior

PASS / FAIL / NOT EXERCISED: **NOT EXERCISED ON HARDWARE**

Notes: Deliberate mid-write USB disconnection was not performed. The exact candidate's automated regression coverage already exercises stale generation, session replacement, transfer failure, readback mismatch, invalid values, unrelated-state-change rejection, and transaction busy-state/current-session projection. Those gates passed before the physical test.

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

Overall DAC reconstruction-filter write qualification: **PASS — 2026-09-13**

This PASS qualifies only the Black Pearl DAC reconstruction-filter write implemented by the exact `f7a3179...` candidate. Balance, microphone gain, amplifier topology, gain mode, playback/global volume management, firmware writes, and generic persistence remain independently gated.