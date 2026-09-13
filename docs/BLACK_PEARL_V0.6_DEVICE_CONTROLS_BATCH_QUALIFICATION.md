# TRN Black Pearl — v0.6 remaining DEVICE controls consolidated qualification

Status: **PHYSICAL TEST STOPPED — CANDIDATE REQUIRES FIXES / RETEST**

This checklist is the single hands-on qualification round approved by the project owner for the remaining normal Black Pearl DEVICE controls after the already-qualified DAC reconstruction-filter write.

It covers, independently within one exact signed candidate:

1. balance;
2. microphone gain;
3. amplifier topology;
4. gain mode;
5. playback/global level.

The DAC reconstruction-filter write already passed its own maintained qualification and is not re-tested here. Firmware remains read-only. A failure of one control does not erase valid evidence for controls that completed their own restore-and-verify sequence; each result is recorded independently.

## Exact candidate provenance

The first consolidated physical candidate was:

- Repository: `weekssa/opra-eq-for-uapp`
- Branch: `v0.6-my-dac`
- Candidate commit SHA: `a3837779b5b732f3b388f80a86c0b14fc00e34c5`
- App version: `0.6.0`
- Immutable exact-candidate APK: `https://raw.githubusercontent.com/weekssa/OPRA-EQ-for-UAPP/mobile-test-apk/candidates/EQ-Library-v0.6.0-beta-a383777.apk`
- Signed APK SHA-256: `cdf1d08551eb3aa336d2ade8d7d15ee32b608366ca11c2e0c20eefa86b013b89`
- Signer certificate SHA-256: `65c1c1256dae3c49e3548f334c91f0ba991969e9be9e0b223ba4e253d2114747`
- Android CI #1366 — **PASS**
- CodeQL #1247 — **PASS**
- Catalog currentness CI #1552 — **PASS**
- Priority community coverage CI #1038 — **PASS**
- Signed EQ Library Beta Candidate #1040 / run `34789342972` — **PASS**
- Signed-beta artifact ID: `10327790834`
- Signed-beta artifact ZIP SHA-256: `34027f387e9f0862f38c6e95a6a39f109a06c6b068f280b4b8353c321f3ae98c`
- Signing verification: APK Signature Scheme v2/v3; signer `CN=OPRA EQ for UAPP, O=weekssa`; RSA 4096
- Test device: Pixel 9
- DAC: TRN Black Pearl
- Test date: `2026-09-13`
- Tester: Project owner

Moving mobile-test filenames are not qualification provenance. The candidate above is retained as failed/incomplete physical evidence and must not be reused as the next qualification candidate after behavior changes.

## Shared transaction contract

Every tested control follows:

`fresh complete read -> local staged choice -> explicit Current/New review -> Apply once -> fresh complete baseline -> target-only write -> complete readback -> exact requested-value verification -> unrelated-state verification`

The batch candidate must never:

- write while a slider, stepper, text field, or choice sheet is merely being adjusted;
- replay a cached setting after USB reconnect;
- silently retry a write after USB-session replacement;
- report the staged/requested value as current before readback verification;
- report success when any unrelated DEVICE field changes unexpectedly;
- send the Black Pearl generic Save-to-Flash command for these DEVICE changes;
- claim that these transient DEVICE writes survive a power cycle;
- make firmware write-interactive.

A bounded same-session **read-only** settling/reverification sequence after one write is permitted when physical evidence shows a control applies asynchronously. Such verification must never resend the write, cross a USB-session replacement, or hide a final mismatch.

## Safety setup

1. Use only the exact immutable signed candidate recorded for the current test round.
2. Start with playback stopped and ordinary listening/downstream volume conservative.
3. Do not change EQ bands or use Direct Flash/Reset during this checklist.
4. Do not change the already-qualified DAC reconstruction filter during this checklist; it should remain `Fast-PC` unless the actual baseline differs, in which case record the actual state and stop before writes.
5. Use an independent Black Pearl controller only after EQ Library has released the USB session.
6. Never tap **Save to Flash** in the independent controller.
7. Make only the small reversible changes specified below.
8. If any step reports an error, mismatch, stale session, disconnect, transfer failure, or unexpected unrelated value change, stop the batch at that point and record the exact screen/result. Do not retry blindly.

## First consolidated candidate physical observation — 2026-09-13

The project owner exercised the `a383777...` candidate and encountered reproducible/visible verification problems before the consolidated batch could be qualified.

Observed/reportable evidence:

- the initial/restored reference state was visible as firmware `0.6`, `Fast-PC`, `HIGH`, `CLASS AB`, microphone `0 dB`, balance `Centered`, and playback raw `512` / `+2.00 dB` / approximately `63%`;
- EQ Library displayed **“The Black Pearl readback did not match the requested setting. The change was not reported as successful.”** during the session;
- the owner reported amplifier-topology changes required several refresh/retry attempts before the requested topology took effect reliably enough to continue;
- a screenshot captured `CLASS H` during the session while the mismatch banner was present, showing that the hardware/app verification timing or transaction semantics need further investigation rather than being treated as a clean first-pass success;
- playback/global level did not behave reliably with the candidate's `0.5 dB` test increments: the owner reported that a downward `0.5 dB` step was not accepted reliably, and after an upward `0.5 dB` attempt subsequent observed changes behaved in whole-dB increments;
- a screenshot captured playback at raw `768` / `+3.00 dB` / approximately `64%` while the mismatch banner was present, despite the checklist being designed around a `0.5 dB` step;
- the owner successfully restored the Black Pearl to the reference/default state at the end of the session: `Fast-PC`, `HIGH`, `CLASS AB`, mic `0 dB`, centered balance, raw `512` / `+2.00 dB` / approximately `63%`.

The attached screenshots also include a microphone-gain review screen, but the owner did not claim a completed microphone-gain qualification result from that image alone. Do not infer a PASS or FAIL for microphone gain solely from screenshot chronology.

**Decision:** stop qualification on this candidate. Do not promote any newly tested control solely from this session. Investigate/fix readback settling and playback-step assumptions, then produce a new exact signed candidate and repeat one consolidated small-change session.

## 1. Exact-candidate baseline and exposure check

First candidate `a383777...`: **PASS for baseline/exposure observation.**

Observed restored reference state:
- firmware `0.6`;
- DAC filter `Fast-PC`;
- gain mode `HIGH`;
- amp topology `CLASS AB`;
- microphone gain `0 dB`;
- balance `Centered`;
- playback/global level raw `512` = `+2.00 dB`, approximately `63%`.

The remaining write controls were exposed through reviewed Change flows as intended, and firmware remained read-only.

## 2. Balance — one small step and restore

First candidate result: **PENDING / NOT QUALIFIED FROM THIS SESSION.**

No independent PASS is recorded here from the evidence supplied so far.

## 3. Microphone gain — one small step and restore

First candidate result: **PENDING / NOT QUALIFIED FROM THIS SESSION.**

A review screen was captured, but no independent completed PASS/FAIL is inferred from the screenshot alone.

## 4. Amplifier topology — alternate mode and restore

First candidate result: **FAIL / REQUIRES SOFTWARE INVESTIGATION.**

The owner reported the topology change eventually worked only after several refresh/retry attempts. That does not satisfy the one-Apply -> verified-readback qualification contract. A future candidate must verify the requested topology from the single deliberate write without requiring the user to resend it. If the hardware applies this control asynchronously, EQ Library may perform bounded same-session read-only settling/reverification before declaring mismatch.

## 5. Gain mode — lower mode first and restore

First candidate result: **PENDING / NOT QUALIFIED FROM THIS SESSION.**

Do not infer gain-mode qualification from the playback/global-gain behavior described below.

## 6. Playback/global level — small decrease first and exact restore

First candidate result: **FAIL / CURRENT STEP ASSUMPTION INVALIDATED.**

The `0.5 dB` qualification assumption did not round-trip reliably on the physical Black Pearl. The owner observed mismatch behavior and a whole-dB state (`+3.00 dB`, raw `768`) during the attempted small-step testing, then restored the reference raw `512` / `+2.00 dB` state.

Until a finer settled increment is independently proven, the normal My DAC playback control must use the conservative physically observed whole-dB product step rather than advertising `0.5 dB` as a reliable settled setting. This does not rewrite the lower-level global-gain protocol representation used by already-qualified EQ/Flash behavior; it narrows only the normal DEVICE-control UX/qualification contract.

The replacement test should therefore use a one-dB **decrease first**, for example `+2.00 dB / raw 512 -> +1.00 dB / raw 256 -> +2.00 dB / raw 512`, with playback stopped and exact readback verification.

## 7. Independent semantic comparison

First candidate result: **NOT REACHED AS A QUALIFICATION STEP.**

The owner nevertheless restored the device to the reference/default state before ending the interrupted session.

## 8. Disconnect/reconnect freshness and final read

First candidate result: **NOT REACHED AS A CONSOLIDATED QUALIFICATION STEP.**

Previously qualified read-only/DAC-filter reconnect-freshness behavior remains authoritative and is not invalidated by these new control-write bugs.

## 9. Failure/session-interruption behavior

The candidate surfaced real readback-mismatch handling without a deliberate USB interruption. The app correctly did **not** report those mismatches as successful. Deliberate mid-write USB disconnection remains unnecessary.

## Per-control qualification decisions after first consolidated candidate

- Balance write: **PENDING**
- Microphone gain write: **PENDING**
- Amp topology write: **FAIL ON `a383777...`; fix/retest required**
- Gain mode write: **PENDING**
- Playback/global level write: **FAIL ON `a383777...`; step/readback behavior fix/retest required**

Overall consolidated batch on `a383777...`: **FAIL / STOPPED; DEVICE RESTORED TO REFERENCE STATE**

A new behavior-affecting candidate must pass automated/security/signing gates before the next physical round. The already-qualified DAC reconstruction-filter write remains qualified unless a later code change touches its behavior.