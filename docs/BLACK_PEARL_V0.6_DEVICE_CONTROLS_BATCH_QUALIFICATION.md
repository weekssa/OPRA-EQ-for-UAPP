# TRN Black Pearl — v0.6 remaining DEVICE controls consolidated qualification

Status: **SUPERSEDED AS THE CURRENT UI CANDIDATE; retained as exact transaction-behavior provenance**

This record preserves the consolidated Black Pearl DEVICE-control qualification work that preceded the finished v0.6 connected-DAC UX refactor.

It covers independently within the original consolidated behavior scope:

1. balance;
2. microphone gain;
3. amplifier topology;
4. gain mode;
5. playback/global level;
6. current USB audio mode readout as a read-only observation.

The DAC reconstruction-filter write already passed its own dedicated qualification and is not reclassified here. Firmware remains read-only. UAC switching is not part of this record because no exact software UAC command is established.

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

It added whole-dB normal playback steps and bounded same-session post-write settling reads without resending writes. Its automated/security/catalog/signing gates passed, but the owner paused physical retest when the missing UAC-mode surface was identified.

`001b5ea...` is superseded by the UAC-readout and read-transport reliability work below.

### Consolidated transaction-behavior candidate

Source: `7b693c838b41126b3b2e82f2f387122962a4baaa`

- Repository: `weekssa/opra-eq-for-uapp`
- Branch: `v0.6-my-dac`
- App version: `0.6.0`
- Immutable signed APK: `https://raw.githubusercontent.com/weekssa/OPRA-EQ-for-UAPP/mobile-test-apk/candidates/EQ-Library-v0.6.0-beta-7b693c8.apk`
- APK SHA-256: `891cc8cae06070399d5cd4079d3dc981ed6d4b661ae1d534b393bc35164e0bab`
- Android CI #1388 / run `34806374443` — **PASS**
- CodeQL #1269 / run `34806374522` — **PASS**
- Catalog currentness CI #1589 / run `34806374455` — **PASS**
- Priority community coverage CI #1075 / run `34806374471` — **PASS**
- Signed EQ Library Beta Candidate #1062 / run `34806372508` — **PASS**
- signed-beta artifact ID: `10333555957`
- artifact ZIP digest: `sha256:8e47f07d1f6ab373d025360c74ba01605d4884d1e3c2374e0ed1ed2b05443e0b`
- base `main` at pin: `f0e6c16d37cbf23ad8e339d49e31251ee4e3f828`

This candidate established the replacement software behavior:

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

## Evidence classification after `7b693c8`

The project owner subsequently reported the remaining controls working in normal use on this exact signed behavior candidate, including correct UAC-mode readout.

Record that evidence as **OWNER-REPORTED**. It supports the maintained transaction behavior but is not silently upgraded into screenshot, independent-controller, or formally completed section-by-section evidence for every individual control.

The earlier failed round remains important evidence because it demonstrated that EQ Library stopped on real readback mismatches instead of reporting success. The replacement candidate addressed those two observed failure modes without adding write retries.

## Original consolidated transaction contract

Every tested write was designed to remain:

`fresh complete read -> local staged choice -> explicit user intent -> target-only write once -> bounded same-session read-only verification -> exact target verification -> unrelated-state verification`

The app must never present a requested value as current before verification, replay cached values after reconnect, cross a session replacement, silently resend a setting write, or claim persistence without separate evidence.

The finished v0.6 UI simplifies the presentation of ordinary controls to row-level selection plus immediate verified apply. That does not weaken this transaction contract.

## Current per-control evidence state

- DAC reconstruction filter — **PHYSICALLY QUALIFIED / PASS** in its dedicated record.
- Balance — software transaction green; later normal-use success is **OWNER-REPORTED**.
- Microphone gain — software transaction green; later normal-use success is **OWNER-REPORTED**.
- Amp topology — first batch exposed a verification failure; replacement behavior green; later normal-use success is **OWNER-REPORTED**.
- Gain mode — software transaction green; later normal-use success is **OWNER-REPORTED**.
- Playback/global level — first batch exposed unreliable 0.5 dB behavior; whole-dB replacement green; later normal-use success is **OWNER-REPORTED**.
- UAC current-mode readout — descriptor-based read-only implementation green; later correct readout is **OWNER-REPORTED**.

## Current finished-UX candidate

The app-wide shared-session / automatic-read / simplified DEVICE UX is now pinned separately to behavior source:

`f32b9c3a81f77a8650f1b6f730e4a4c9171310ad`

Its immutable signed APK is:

`https://raw.githubusercontent.com/weekssa/OPRA-EQ-for-UAPP/mobile-test-apk/candidates/EQ-Library-v0.6.0-beta-f32b9c3.apk`

APK SHA-256:

`21407021a4cfa41960cb45f9f59979f868b50f41b9fc81c718ee0ebf4b12458d`

The exact candidate passed Android CI #1410, CodeQL #1291, Catalog currentness #1625, Priority community coverage #1111, and Signed EQ Library Beta Candidate #1084.

The required next physical work is the focused finished-UX regression maintained in `docs/V0.6_RELEASE_CHECKLIST.md`. It validates the changed session/read orchestration and representative user-visible hardware actions without pretending the older protocol evidence disappeared.

No merge or release is authorized by this record.
