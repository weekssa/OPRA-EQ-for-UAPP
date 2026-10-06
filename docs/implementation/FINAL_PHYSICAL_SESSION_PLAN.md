# v0.8.0 Beta — Final Physical Session Plan

**Disposition: FINAL PHYSICAL SESSION CANNOT BE SAFELY DEFINED.** Do not request/use the Pixel for C05 under the current maintained procedure. This file is the single definitive plan/disposition; it is not permission to begin a hardware operation.

## Candidate

- Application/test branch: `codex/v0.8.0-beta-ux`
- Frozen app/test HEAD: `acd95df543632e57ae46cd68e714a35f94781f43`
- Frozen tree: `0594c8887a5a066407700ad8e4906a0b3a93033d`
- App-source commit: `f24b1582e22b19fc75757a61186361b71eab3974`
- App tree: `337889667a9974967ab0d80ba880268b470eeab2`
- Candidate APK available in the frozen checkout: `app/build/outputs/apk/debug/app-debug.apk`
- APK SHA-256: `f33818309d55571166d91e501065706ddbb8faf180833b03bb6eb948e6bffed1`
- Package/version: `com.weekssa.opraeqforuapp`, `0.8.0-beta` / code `10`; debug-signed, not the official beta artifact.
- PR #70: open/draft, exact head matches frozen HEAD; base `main` `c702ef0149b4c647446639cfcb0ab25c09159db3`; 8/8 checks successful at 2026-10-06 05:45 UTC.
- Classification: **Class B**, unchanged.

## Required and optional equipment

If a future maintained procedure makes G3 pass, required equipment is the M4 Mac mini, owner's Pixel 9, TRN Black Pearl, and one shared Wi-Fi network for wireless ADB. The Pixel USB-C port must remain connected only to the active DAC. Headphones/listening device are not required under the current wording/evidence because no additional human-heard TalkBack check is required. If that gate changes, headphones and a separate owner `READY` response become required.

Optional only: FiiO JA11 and SIMGOT EW300 DSP. Both optional Pixel smokes are **NO**; they cannot add a release gate and are not needed to establish identity. The Mac-only identities are recorded in [`v0.8.0-beta-mac-readonly-enumeration.md`](v0.8.0-beta-mac-readonly-enumeration.md).

## Owner actions

**Required now: none.** Do not connect the Pixel for this gate.

If and only if an updated maintained safe procedure makes G3 pass, the single consolidated session would require the owner to: be present for the phone/DAC session; keep Pixel and Mac on the same Wi-Fi; enable wireless debugging; provide the current pairing endpoint/code privately in the active session if pairing is needed (never store the code); connect the Black Pearl to the Pixel USB-C port when asked; and explicitly confirm each required cable/device transition. No current ADB address or pairing code is treated as durable.

Human-heard TalkBack: **no additional check currently required**. The owner previously reported that direct touch made the recovery warning audible, and the relevant wording is unchanged. A changed wording or newly applicable requirement would need a warning and a separate explicit `READY` response before any heard check.

## Codex actions and session ordering

No Android physical action is authorized by this plan. If G3 is changed by new maintained evidence, replace this blocked disposition with an exact procedure before requesting the Pixel. The minimum sequence to prepare then is:

1. Reverify frozen HEAD/tree/app tree, APK SHA, PR head, current checks, and the maintained procedure; stop if any identity differs.
2. Verify wireless ADB pairing/connection, shell, logcat, screenshot, package/version, launch, and process/activity inspection before the DAC is attached. Never use wired ADB.
3. Capture the exact Black Pearl identity and fresh full EQ/DEVICE state, including all ten bands, active slot, tracked playback gain, volume, filter, gain mode, topology, balance, microphone gain, and UAC state. Save original-state evidence before any write.
4. Confirm the active EQ is known and non-flat. The maintained restore-defaults checklist permits an existing non-flat state or flashing a known non-flat saved EQ first. The previous session's Flat baseline does not satisfy this precondition.
5. Proceed with one authorized Restore defaults/reset-recovery sequence only if the replacement maintained procedure identifies the objectively safe transition and observable stage. That missing step is why this sequence cannot currently be executed or estimated.
6. On uncertainty, stop and observe. Never replay Reset, disconnect USB, kill the process, or infer safety from elapsed time.
7. Obtain explicit current EQ and DEVICE reads without replaying a mutation; restore every captured original value and verify complete final readback.
8. Capture UI/screenshots, focused PID logcat, process/activity state, test identity, readbacks, stop decisions, and exact restoration evidence; release the Pixel immediately.

The required C05 step is not supplied by the maintained checklist or mission §48. The last physical recovery observation began from Flat and was initiated by navigating Back approximately 120 ms after Reset while a generic `Resetting EQ to flat…` state was visible. The HID stage and lower-level write count were unknown. Repeating this would recreate the known unsafe ambiguity.

## Wireless ADB and prepared diagnostics

Use current on-screen Developer options values only; earlier IP addresses/ports and six-digit pairing codes expire. Pair only if necessary, and do not write a pairing code to logs or evidence:

```sh
./tools/codex-android adb pair <current-pairing-host>:<pairing-port>
./tools/codex-android adb connect <current-debugging-host>:<debugging-port>
./tools/codex-android adb devices -l
```

Before any DAC-sensitive step, verify connected wireless device, shell, package, launch, activity/process and screenshot. Prepared command patterns (not run now):

```sh
./tools/codex-android adb -s <wireless-serial> shell getprop ro.product.model
./tools/codex-android adb -s <wireless-serial> install -r <exact-candidate-apk>
./tools/codex-android adb -s <wireless-serial> shell dumpsys package com.weekssa.opraeqforuapp
./tools/codex-android adb -s <wireless-serial> shell monkey -p com.weekssa.opraeqforuapp 1
./tools/codex-android adb -s <wireless-serial> shell pidof com.weekssa.opraeqforuapp
./tools/codex-android adb -s <wireless-serial> shell dumpsys activity activities
./tools/codex-android adb -s <wireless-serial> exec-out screencap -p > <evidence-dir>/screen.png
./tools/codex-android adb -s <wireless-serial> logcat -v threadtime --pid=<app-pid>
```

Do not clear the Android log buffer. Keep original binary/raw outputs and a short index; inspect logs after the phone session. The exact debug APK above was already built from this frozen app-source tree and hashed; do not rebuild it merely for this watch update.

## Starting state, non-flat requirement, and C05 procedure

- Original state: must be captured fresh at the session start. Historical-only reference from the prior Pixel session: EQ Flat / 10 filters / slot 1 / +13.00 dB; DEVICE Volume 80% / FAST-LL / LOW / CLASS AB / Centered / microphone 0 dB / UAC 2.0. It is not current state and cannot substitute for a fresh read.
- Required non-flat state: the maintained checklist §E requires a known non-flat EQ before checkbox-on Restore defaults. An existing non-flat current slot is acceptable; otherwise the checklist allows flashing a known non-flat saved EQ first. No state change should be staged until an exact safe procedure and original-state restoration have been planned.
- Safe C05 procedure: **NONE FOUND**. One independent reviewer concluded `NO — SAFE PROCEDURE NOT DEFINED BY MAINTAINED EVIDENCE`. The bounded Class B redesign review also concluded no source-supported minimal redesign safely eliminates the physical gate. Keep Class B.
- Observable transition / expected stage: **undefined** for cancellation during the multi-report reset. Do not substitute timing, a generic progress string, or an undocumented HID stage.

## Pass, abort, no-replay, readback, and restoration criteria

No physical pass can be claimed until an updated procedure defines a safe transition and is approved in the maintained plan. The required outcome, if such a plan exists, must include: exact candidate/device identity; required known non-flat starting EQ; recovery state retained across the defined navigation/lifecycle transition; Restore defaults/action eligibility remains blocked during recovery; zero automatic second mutation callbacks; explicit non-mutating EQ/DEVICE observation; complete final native readback; exact restoration of the captured original EQ and DEVICE state; and final verification that all original values match.

Abort before mutation if candidate/APK/PR identity differs, wireless ADB fails, identity is ambiguous, baseline cannot be captured, the EQ is flat when the precondition requires non-flat, playback/volume is unsafe, the device/session changes, or the maintained observable safe stage is absent. Once a hardware action may have begun, do not cancel at an unknown stage or retry; use only the maintained explicit read/observe/recovery path, then restore and verify. If any result is uncertain, stop without replay and preserve evidence.

Final readback must verify all ten native EQ bands, active slot, playback gain and tracked delta, plus every captured DEVICE value. Restoration uses the already-qualified app flows only, with explicit user confirmation and one mutation per state transition; each write is followed by readback. If original-state readback fails, stop and preserve device/evidence for safe owner handling; never claim restored.

## Expected occupancy and final disposition

Optional JA11 smoke: **NO**. Optional EW300 smoke: **NO**.

Total expected Pixel occupancy: **not responsibly estimable**, because the required C05 procedure has no safe observable transition. No session may be booked or started from this plan. The owner should continue normal phone use.

Maintained sources: `docs/BLACK_PEARL_V0.6_RESTORE_DEFAULTS_HANDS_ON_CHECKLIST.md` §§A/E/F, `docs/BLACK_PEARL_FLAT_RESET_HANDS_ON_CHECKLIST.md` §§3/8, `docs/implementation/v0.8.0-beta-mission.md` §48, current Black Pearl transaction source, and [watchdog issue #71](https://github.com/weekssa/OPRA-EQ-for-UAPP/issues/71). Detailed C05 disposition is also summarized in [`v0.8.0-beta-autonomy-status.md`](v0.8.0-beta-autonomy-status.md).
