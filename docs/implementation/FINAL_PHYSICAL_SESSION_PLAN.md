# v0.8.0 Beta — Final Physical Session Plan

**Disposition: PLAN PREPARED; exact-head API35 run 3 failed and the current consecutive-pass streak is 0.** This is a read-only physical qualification plan, not permission to begin. Start only after every required check and three consecutive full-suite API35 runs for the exact candidate below are green and the owner confirms the consolidated session.

## Exact candidate

- Repository: `weekssa/OPRA-EQ-for-UAPP`
- App/test branch: `codex/v0.8.0-beta-ux`
- Candidate HEAD: `5c7d19cf2f77e448a97a1ee9d520ad4d408f120f`
- Candidate tree: `e11669f0d67f9fb3c3762ea814f16fb7c4bdab8d`
- App production-source commit: `f24b1582e22b19fc75757a61186361b71eab3974`
- `app/src/main` tree: `857d02a53d0df44fb0bd46e5ddad3b319dc48dab` (unchanged by the test/evidence-only commits)
- Candidate-matched local debug APK: `app/build/outputs/apk/debug/app-debug.apk`
- APK SHA-256: `f33818309d55571166d91e501065706ddbb8faf180833b03bb6eb948e6bffed1`
- APK package/version: `com.weekssa.opraeqforuapp`, `0.8.0-beta` / version code `10`; debug signer SHA-256 `cbd57d13316c2ca9e59fb810135ea71567fd22132b0d7f4640e92dc4434f0e14`. This is not the official signed beta artifact.
- PR #70 is open/draft, base `main` at `c702ef0149b4c647446639cfcb0ab25c09159db3`, head matches this candidate. At 2026-10-06 07:57 UTC, Catalog (`37427641890`), Priority (`37427641910`), and CodeQL (`37427641849`) passed; Android CI (`37427641822`) failed because API35 UI job `112159666615` had 2 failures in 64 tests. API35 runs 1 (`112150826255`) and 2 (`112155299351`) passed 64/64 each; run 3 failed `interruptedEqResetIsNotReplayedAndLeavesAnActionableRecoveryState` at a no-window-focus check and `recoverySurvivesProductionMyDacTabAndRootNavigationWithoutReplayingCallbacks` while waiting for an explicit fake EQ-read callback after DPAD. The failed-run artifact records an EGL stall, Launcher SIGKILL/ANR, and Activity pause overlapping the failures; causality is not proven and no app fatal/OOM was recorded. Current consecutive streak is 0. As of 2026-10-06 08:09 UTC, exact-head API35 UI rerun job `112171539825` is in progress; its build job `112171541098` passed. Incidental API26 cold-install job `112171541652` passed without Library/heap diagnostics and does not reopen the closed API26 gate. Do not duplicate the running suite; inspect its full report/log when complete, diagnose before any further retry, and recheck all required checks on the same exact HEAD immediately before the physical session.
- Classification remains **Class B**. The candidate changes recovery-state lifetime, navigation retention, and action eligibility. No hardware protocol implementation changed.

## C05 qualification split

- **C05-A — PASS:** focused API35 test `recoverySurvivesProductionMyDacTabAndRootNavigationWithoutReplayingCallbacks()` passed 1/1 on AVD serial `emulator-5556`. It restores saved state in production `EqLibraryApp`/`MyDacScreen`, retains recovery through root/tab transitions, leaves Restore defaults disabled, counts one suspended fake reset, observes only explicit EQ/DEVICE reads, and observes zero DEVICE writes. E20 separately records fake-backed Android OS process reconstruction/no-replay. Neither test uses a DAC.
- **C05-B — PASS:** exact diff from mission base `9b1a83b4a0bc8d7cad4a1f10ab99ff92c3259e3c` to candidate `5c7d19cf2f77e448a97a1ee9d520ad4d408f120f` returned no changed files under `app/src/main/java/com/weekssa/opraeqforuapp/domain/blackpearl/`, `app/src/main/java/com/weekssa/opraeqforuapp/data/blackpearl/`, `app/src/main/java/com/weekssa/opraeqforuapp/data/hardware/`, `app/src/main/java/com/weekssa/opraeqforuapp/data/dac/`, `app/src/main/java/com/weekssa/opraeqforuapp/domain/dac/`, `app/src/main/java/com/weekssa/opraeqforuapp/domain/kt02h20/`, or `app/src/main/java/com/weekssa/opraeqforuapp/domain/eq/`. These directories cover the protected protocol/codec/defaults, identity, USB transport, DAC session/transaction, mutation ordering, quantization, persistence, readback, DSP, and capability implementation. The Class B recovery UI remains changed outside those directories.
- **C05-D — PASS:** the independent qualification review on 2026-10-06 accepted the split evidence model, verified the fake-backed no-replay and protected-path audit, confirmed that physical identity/session/read/navigation remains required, and found no unsupported physical claim or missing C05 coverage.
- **C05-C — pending this physical session:** qualify exact Black Pearl recognition and Android session, current-state reads, attached-device navigation stability, truthful post-navigation state, explicit final reads, and no unexpected physical mutation. This physical session does not create the synthetic recovery fault.

The owner-approved strategy clarification and automated result are recorded in candidate acceptance evidence E23. Historical E22 remains supplemental and is not relabeled as this read-only run.

## Scope and safety boundary

Required hardware is the owner's Pixel 9 and the exact TRN Black Pearl over USB-C. The M4 Mac mini connects to Pixel over wireless ADB, keeping the Pixel USB-C port available for the DAC. No headphones are required. JA11/EW300 checks are excluded.

This session is **read-only with respect to the DAC**. Do not open Restore defaults or Reset, change DEVICE controls, apply/flash/save an EQ, create a recovery fault, disconnect the DAC, kill/restart the app process during an operation, or retry an uncertain read/write. Do not change EQ to meet the superseded non-flat reset precondition. No state is intentionally changed, so no restoration write is planned. If anything changes unexpectedly, capture it and stop without trying to repair or replay it.

The prior direct-touch owner-reported TalkBack audibility remains sufficient because the recovery warning wording and semantics are unchanged. Do not repeat an audible check. If that condition changes, request a separate `READY FOR TALKBACK CHECK` and wait for explicit `READY` before audio output.

## Owner action and start condition

After exact-head checks pass, the owner should confirm one final session, keep Pixel and Mac on the same Wi-Fi, unlock the Pixel, and attach the Black Pearl to the Pixel's USB-C port. The owner need not tap any mutation control. If the current ADB pairing is lost, enable Wireless debugging and provide the current ephemeral pairing endpoint/code only for the active session; never save a pairing code in evidence.

Before any ADB/device action, verify PR #70 still has the exact HEAD/tree above, the app APK hash still matches, required CI is green, and the owner has confirmed the session. Stop if source, artifact, PR base/head, or check identity differs.

## Wireless ADB and artifact commands

Run from the repository on the Mac. Prefer an existing paired mDNS service; old chat IPs, ports, and pairing codes are expired and must not be reused.

```sh
./tools/codex-android adb mdns services
./tools/codex-android adb connect <current-adb-tls-connect-host:port>
./tools/codex-android adb devices -l
./tools/codex-android adb -s <wireless-serial> shell getprop ro.product.model
./tools/codex-android adb -s <wireless-serial> shell getprop ro.product.device
./tools/codex-android adb -s <wireless-serial> shell getprop ro.build.version.sdk
shasum -a 256 app/build/outputs/apk/debug/app-debug.apk
```

Only if pairing is actually necessary, enter the current code silently so it is not added to shell history; the code is still passed transiently to `adb pair` and must not be copied into logs or evidence:

```zsh
read -r -s 'opraPairCode?Current Wireless debugging pairing code: '
./tools/codex-android adb pair <current-pairing-host:port> "$opraPairCode"
unset opraPairCode
./tools/codex-android adb connect <current-adb-tls-connect-host:port>
```

Use only the wireless serial shown by `adb devices -l` for every later command. Confirm the Pixel model/device/API. If the exact debug APK is not already installed, run:

```sh
./tools/codex-android adb -s <wireless-serial> install -r app/build/outputs/apk/debug/app-debug.apk
```

If install fails due to signer/version conflict, do not uninstall or replace the existing package; stop and report the blocker. Launch the app with:

```sh
./tools/codex-android adb -s <wireless-serial> shell am start -W -n com.weekssa.opraeqforuapp/.MainActivity
```

## Single-session procedure

Create a new, unique local evidence directory under `.unlazy/v080-beta/evidence/`; do not overwrite an earlier record. Save the exact candidate/PR/check snapshot and local APK digest there.

1. Capture pre-session Android USB/session and app identity evidence without issuing a USB write:

   ```sh
   ./tools/codex-android adb -s <wireless-serial> shell dumpsys usb > <evidence-dir>/usb-before.txt
   ./tools/codex-android adb -s <wireless-serial> shell pidof com.weekssa.opraeqforuapp > <evidence-dir>/pid-before.txt
   ./tools/codex-android adb -s <wireless-serial> shell dumpsys activity activities > <evidence-dir>/activities-before.txt
   ./tools/codex-android adb -s <wireless-serial> exec-out screencap -p > <evidence-dir>/screen-before.png
   ```

2. In the production app, open My DAC and verify it identifies the connected device as TRN Black Pearl and presents the active session truthfully. Capture any currently available USB identity fields; compare the serial to the previously recorded `330243E8260129` only if Android exposes it. The app's authoritative recognition must at least identify the supported Black Pearl; do not infer a serial from VID/PID alone.

3. Use only explicit read actions: `Read current EQ` and `Refresh DEVICE`. Record the fresh EQ snapshot (all values the production read exposes, active slot, and playback gain) and DEVICE values (volume, filter, gain mode, topology, balance, microphone gain, and UAC mode). Capture screenshots and UI hierarchy for every page needed to make the state legible. A read failure or incomplete/ambiguous identity is an abort, not permission to reset or repair.

4. With Black Pearl still physically attached, navigate from My DAC to EQ Library and back using the visible production navigation. Do not invoke Restore defaults, Reset, Flash, Save, or DEVICE setters. Record whether recognition/session state stays truthful and note the app process ID; do not kill the process or unplug the DAC.

5. After returning, invoke only `Read current EQ` and `Refresh DEVICE` again. Compare all captured values and session presentation with the initial fresh reads. Capture final screenshots and:

   ```sh
   ./tools/codex-android adb -s <wireless-serial> shell dumpsys usb > <evidence-dir>/usb-after.txt
   ./tools/codex-android adb -s <wireless-serial> shell pidof com.weekssa.opraeqforuapp > <evidence-dir>/pid-after.txt
   ./tools/codex-android adb -s <wireless-serial> shell dumpsys activity activities > <evidence-dir>/activities-after.txt
   ./tools/codex-android adb -s <wireless-serial> exec-out screencap -p > <evidence-dir>/screen-after.png
   ```

6. Record exact before/after values, identity/session observations, navigation results, commands, timestamps, candidate/APK/check identity, and artifact hashes. Preserve logs without clearing logcat; if needed, capture app-PID logcat after the session with `adb logcat -d -v threadtime --pid=<app-pid>`. Do not claim a lower-level USB write count from unchanged UI values.

## Pass and abort criteria

**PASS C05-C only if all are observed:**

- Exact candidate, app-source APK hash, PR #70 head/base, and required checks still match and pass.
- Android identifies the attached unit as the supported TRN Black Pearl and the production app reports the session accurately.
- Fresh production EQ and DEVICE reads succeed and provide a known baseline.
- My DAC → EQ Library → My DAC navigation while attached does not silently switch identity, claim a false session, or change read values.
- Explicit final EQ and DEVICE reads succeed and match the initial values; no DEVICE write, reset, Flash, save, or unexplained state change occurred.
- Evidence is saved and hashes are recorded. Because the physical session intentionally makes no state changes, no restoration mutation is required.

**ABORT without retry** for a candidate/check mismatch, non-Pixel 9 target, ambiguous Black Pearl identity, absent/unstable Android session, incomplete/failed read, process/session change, unexpected write or value change, or any unclear hardware state. Do not attempt reset, cancellation, disconnection, process kill, or restorative write to make the result green. Preserve evidence and report the exact observation.

## Occupancy

Expected Pixel occupancy is **8–10 minutes**, with a **15-minute hard limit**. Pairing/setup should be completed before starting the timed session. At the limit, stop cleanly after capturing current evidence; do not omit final readback or extend into optional DAC work. Optional JA11/EW300 checks are NO.

Maintained references: `docs/BLACK_PEARL_V0.6_RESTORE_DEFAULTS_HANDS_ON_CHECKLIST.md` §§A/E/F (existing production identity/read/write behavior and stop rules; its mutation checklist is not invoked), `docs/implementation/v0.8.0-beta-mission.md` §48, candidate acceptance evidence E23, and [watchdog issue #71](https://github.com/weekssa/OPRA-EQ-for-UAPP/issues/71).
