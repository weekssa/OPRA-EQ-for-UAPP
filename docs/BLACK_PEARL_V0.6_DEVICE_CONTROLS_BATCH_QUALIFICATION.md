# TRN Black Pearl — v0.6 remaining DEVICE controls consolidated qualification

Status: **PAUSED — PREVIOUS REPLACEMENT CANDIDATE SUPERSEDED BEFORE RETEST**

This is the maintained record for the one-round hardware qualification of the remaining normal Black Pearl DEVICE controls after the already-qualified DAC reconstruction-filter write.

It covers independently within one exact signed candidate:

1. balance;
2. microphone gain;
3. amplifier topology;
4. gain mode;
5. playback/global level;
6. current USB audio mode readout as a read-only observation.

The DAC reconstruction-filter write already passed its own qualification and is not re-tested. Firmware remains read-only. UAC switching is not part of this checklist because no exact software UAC command is established.

## Candidate history

### First consolidated candidate — FAILED / STOPPED

Source: `a3837779b5b732f3b388f80a86c0b14fc00e34c5`

The 2026-09-13 Pixel 9 / Black Pearl round was stopped after:

- amp topology did not satisfy the required one-Apply -> verified-readback flow and became visible only after several manual refresh/retry attempts;
- normal playback/global-level `0.5 dB` changes did not round-trip reliably;
- EQ Library correctly displayed readback mismatch rather than reporting false success;
- the project owner restored the reference state: firmware `0.6`, `Fast-PC`, `HIGH`, `CLASS AB`, mic `0 dB`, balance `Centered`, playback raw `512` / `+2.00 dB` / about `63%`.

No PASS is inferred for balance, mic gain or gain mode from that incomplete round.

### First replacement candidate — AUTOMATED PASS, PHYSICAL RETEST NOT RUN

Source: `001b5ea5b2199784cfc8cf34941b018bf927caa1`

It added whole-dB normal playback steps and bounded same-session post-write settling reads without resending writes. Its automated/security/catalog/signing gates passed, but the owner correctly paused physical retest when the missing UAC-mode surface was identified.

`001b5ea...` is now **superseded** by subsequent UAC-readout and read-transport reliability work. Its immutable APK remains provenance only and must not be used for the next hardware round.

## Current candidate requirements

Before asking the owner to test again, freeze one newer exact behavior source and require:

- Android unit tests/lint/debug/release assembly PASS;
- CodeQL PASS;
- catalog currentness PASS;
- priority community coverage PASS;
- signed-beta PASS with immutable APK, APK SHA-256 and established signer pin;
- UAC current-mode descriptor readout present but non-writable;
- normal playback DEVICE steps remain whole-dB;
- Black Pearl transport serializes I/O and permits at most one same-session retry of a failed nondestructive READ;
- no setting write retry/resend;
- candidate post-write settling remains bounded and same-session only;
- balance left/right writes retain safe spacing;
- no generic Save-to-Flash or persistence claim.

## Next consolidated hands-on scope

When the next exact signed candidate is pinned, run one continuous session with playback stopped for writes and small reversible changes:

1. baseline: require Current session read and record firmware/filter/gain/topology/mic/balance/playback plus UAC 1.0/2.0 readout;
2. balance: Center -> `-1 dB` -> Center;
3. microphone gain: `0 dB` -> `-1 dB` -> `0 dB`;
4. amplifier topology: `CLASS AB` -> `CLASS H` -> `CLASS AB`;
5. gain mode: `HIGH` -> `LOW` -> `HIGH`;
6. playback/global: raw `512` / `+2.00 dB` -> raw `256` / `+1.00 dB` -> raw `512` / `+2.00 dB`;
7. independent-controller read-only final comparison; never Save to Flash;
8. disconnect/reconnect freshness and final complete read.

After each Apply, wait for EQ Library verification. If any step reports mismatch/error/session loss/unexpected unrelated state, stop immediately and do not manually retry the failed write.

## Transaction contract

Every tested write remains:

`fresh complete read -> local staged choice -> explicit Current/New review -> Apply once -> fresh complete baseline -> target-only write once -> bounded same-session read-only verification -> exact target verification -> unrelated-state verification`

The app must never present a staged value as current before verification, replay cached values after reconnect, cross a session replacement, silently resend a setting write, or claim persistence without separate evidence.

## Current per-control state

- DAC reconstruction filter — **PHYSICALLY QUALIFIED / PASS**
- Balance — **PHYSICAL PENDING ON NEXT EXACT CANDIDATE**
- Microphone gain — **PHYSICAL PENDING ON NEXT EXACT CANDIDATE**
- Amp topology — **FIRST BATCH FAILED; FIX IN VALIDATION; PHYSICAL RETEST PENDING**
- Gain mode — **PHYSICAL PENDING ON NEXT EXACT CANDIDATE**
- Playback/global level — **FIRST BATCH FAILED; WHOLE-DB FIX IN VALIDATION; PHYSICAL RETEST PENDING**
- UAC current-mode readout — **SOFTWARE IMPLEMENTED; PHYSICAL READBACK PENDING; READ-ONLY**

Overall batch: **PAUSED UNTIL NEXT EXACT SIGNED CANDIDATE IS PINNED**.

No merge or release is authorized by this record.
