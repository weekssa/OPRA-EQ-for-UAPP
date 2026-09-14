# TRN Black Pearl — v0.6 remaining DEVICE controls consolidated qualification

Status: **READY — EXACT SIGNED CANDIDATE PINNED; PHYSICAL ROUND NEXT**

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

`001b5ea...` is superseded by the UAC-readout and read-transport reliability work below. Its immutable APK remains provenance only and must not be used for the current hardware round.

## Exact current candidate provenance

The current behavior candidate is frozen at:

- Repository: `weekssa/opra-eq-for-uapp`
- Branch: `v0.6-my-dac`
- Source commit: `7b693c838b41126b3b2e82f2f387122962a4baaa`
- Commit: `Fix Black Pearl UAC unit test compatibility`
- App version: `0.6.0`
- Immutable signed APK: `https://raw.githubusercontent.com/weekssa/OPRA-EQ-for-UAPP/mobile-test-apk/candidates/EQ-Library-v0.6.0-beta-7b693c8.apk`
- APK SHA-256: `891cc8cae06070399d5cd4079d3dc981ed6d4b661ae1d534b393bc35164e0bab`
- Android CI #1388 / run `34806374443` — **PASS**
- CodeQL #1269 / run `34806374522` — **PASS**
- Catalog currentness CI #1589 / run `34806374455` — **PASS**
- Priority community coverage CI #1075 / run `34806374471` — **PASS**
- Signed EQ Library Beta Candidate #1062 / run `34806372508` — **PASS**
- Signed-beta artifact: `EQ-Library-signed-beta-7b693c838b41126b3b2e82f2f387122962a4baaa`
- Artifact ID: `10333555957`
- Artifact ZIP digest: `sha256:8e47f07d1f6ab373d025360c74ba01605d4884d1e3c2374e0ed1ed2b05443e0b`
- Base `main` at candidate pin: `f0e6c16d37cbf23ad8e339d49e31251ee4e3f828`
- Candidate branch was 0 commits behind `main` at the pin.

The signed-beta workflow verifies the APK certificate against the repository-pinned release-signing fingerprint before publication. The immutable candidate checksum above is independently published beside the APK.

Later documentation-only commits may update this checklist without replacing the exact `7b693c8` behavior candidate. Any behavior-affecting replacement requires a new exact candidate, new green gates and an explicit new pin here before hardware testing continues.

## Candidate behavior under test

The pinned candidate satisfies the pre-hardware requirements:

- Black Pearl USB operations are serialized;
- a failed nondestructive READ may be reissued at most once in the same USB session;
- session replacement stops retry immediately;
- setting writes are never automatically retried or resent;
- normal playback DEVICE steps are whole-dB while the underlying protocol remains 1/256 dB;
- post-write settling reads are bounded and same-session only;
- balance left/right writes retain conservative spacing;
- UAC 1.0/2.0 current-mode detection is descriptor-based and read-only;
- malformed, conflicting or unsupported UAC descriptor evidence produces Unknown rather than a guess;
- no generic Save-to-Flash or persistence claim is used by these DEVICE transactions.

## Consolidated hands-on scope

Run one continuous Pixel 9 / TRN Black Pearl session with playback stopped for writes and small reversible changes. Advance one section at a time.

### 1. Baseline + UAC readout

Require a fresh **Current session read** and record:

- firmware;
- reconstruction filter;
- gain mode;
- amplifier topology;
- microphone gain;
- balance;
- playback/global level;
- UAC mode as `UAC 1.0`, `UAC 2.0`, or `Unknown` only from actual descriptor evidence.

This section is read-only. Do not change a setting.

### 2. Balance

`Center -> -1 dB -> Center`

Each change must be staged locally, reviewed, Applied once, then verified by complete readback with unrelated state unchanged.

### 3. Microphone gain

`0 dB -> -1 dB -> 0 dB`

Use the same one-Apply -> complete verified-readback rule.

### 4. Amplifier topology

`CLASS AB -> CLASS H -> CLASS AB`

Do not manually refresh/retry a failed write to make it appear successful. If one Apply does not verify, stop the qualification at this section.

### 5. Gain mode

`HIGH -> LOW -> HIGH`

Keep playback stopped and listening level conservative before any later listening.

### 6. Playback/global level

`raw 512 / +2.00 dB -> raw 256 / +1.00 dB -> raw 512 / +2.00 dB`

This is level-sensitive. Use only the approved one-whole-dB decrease and restoration. Do not test a `0.5 dB` step in this qualification round.

### 7. Independent-controller final comparison

After EQ Library releases the USB session, use the trusted independent controller read-only to compare the final restored normal DEVICE state. Never invoke its Save-to-Flash action for this qualification.

### 8. Disconnect/reconnect freshness

Physically disconnect/reconnect, verify retained values are stale until a new hardware read succeeds, then perform one final complete current-session read.

After each Apply, wait for EQ Library verification. If any step reports mismatch, error, session loss, or unexpected unrelated-state change, stop immediately and do not manually retry the failed write or continue to the next control.

## Transaction contract

Every tested write remains:

`fresh complete read -> local staged choice -> explicit Current/New review -> Apply once -> fresh complete baseline -> target-only write once -> bounded same-session read-only verification -> exact target verification -> unrelated-state verification`

The app must never present a staged value as current before verification, replay cached values after reconnect, cross a session replacement, silently resend a setting write, or claim persistence without separate evidence.

## Current per-control state

- DAC reconstruction filter — **PHYSICALLY QUALIFIED / PASS**
- Balance — **PHYSICAL PENDING ON EXACT `7b693c8` CANDIDATE**
- Microphone gain — **PHYSICAL PENDING ON EXACT `7b693c8` CANDIDATE**
- Amp topology — **FIRST BATCH FAILED; REPLACEMENT BEHAVIOR GREEN; PHYSICAL RETEST PENDING ON `7b693c8`**
- Gain mode — **PHYSICAL PENDING ON EXACT `7b693c8` CANDIDATE**
- Playback/global level — **FIRST BATCH FAILED; WHOLE-DB FIX GREEN; PHYSICAL RETEST PENDING ON `7b693c8`**
- UAC current-mode readout — **SOFTWARE IMPLEMENTED / AUTOMATED GREEN / PHYSICAL READBACK PENDING / READ-ONLY**

Overall batch: **READY FOR PHYSICAL QUALIFICATION ON EXACT SIGNED `7b693c8` CANDIDATE**.

No merge or release is authorized by this record.
