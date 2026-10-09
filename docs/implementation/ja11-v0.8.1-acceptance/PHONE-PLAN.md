# JA11 v0.8.1 physical acceptance plan

J020 ended with Mic Off as the last verified state; Mic On restoration remains outstanding. Run
this procedure only after G2-G5 are complete, all required checks are green
on the latest live PR #80 head, and the corrected exact candidate is still valid. One bundled session
only. Do not perform research/build/documentation
work while the Pixel is occupied.
Budget about 25–35 minutes if the existing wireless ADB connection is reusable. If ADB recovery
requires prolonged troubleshooting or pairing setup, stop before attaching the JA11 or mutating it,
release the phone, and resume host preparation off-phone.

## Frozen off-phone candidate

- App source: `616958037349e2f0e0784a556c6430b0de6ceb18` (tree
  `fdb6c8d10c8aa865da1a4818d22a7c14b2559b21`).
- Diagnostic APK: `opra-eq-ja11diag-0.8.0-source-61695803.apk`.
- APK SHA-256: `ee0fe4fbfaae7b3f959d4122f0c21c128dffdf21d47586376ee383e2534ceb0d`.
- Package/version/code: `com.weekssa.opraeqforuapp.ja11diag`, `0.8.0-ja11diag` / `11`.
- Debug signer certificate SHA-256:
  `73aa7581c8dc7dcc8ccea7586771119a98f9a74d7d8cf23716e09c557c9f6b41`.
- Private APK: `/private/tmp/ja11-v0.8.1-acceptance-61695803/opra-eq-ja11diag-0.8.0-source-61695803.apk`.
- A wiped API 35 emulator cold-launched the exact source-bound APK and reported the exact source
  SHA in `APP_BUILD_INFO`; API 35 instrumentation passed 64/64. Exact latest PR-head CI is pending.
- Exact-head PR CI must pass before any phone command. The emulator result is not hardware evidence.

This candidate includes the JA11-only opened-connection serial fallback, which reads the serial from
the claimed connection only when `UsbDevice.serialNumber` returns null. The unique nonblank serial
identity gate remains fail-closed. J021 used the prior `da1f8e2` candidate for read-only testing and
returned null from the device getter; J021 is the exact rollback build if a safe software rollback
is needed.

## Install and rollback

Before updating, the helper pulls the installed diagnostic package's `base.apk` into the private
evidence directory and verifies its exact J021 checksum, package/version, and debug signer. It prints
the timestamped private backup path. The corrected candidate is installed in place with
`adb install -r`, preserving app data. Never uninstall the old package as an installation step. If
install fails, stop, preserve the captured APK and logs, and do not retry or start a JA11 operation.
If software rollback is needed after no JA11 command was sent, or after the complete original JA11
state has been freshly verified, run `phone-session.sh "$SERIAL" rollback`. This action verifies the
installed corrected APK and the exact rollback APK, uses `adb install -r`, then verifies the restored
package version, checksum, and signer while saving a private APK copy. The action uses the exact
J021 APK at `/private/tmp/ja11-v0.8.1-acceptance-da1f8e25/opra-eq-ja11diag-0.8.0-source-da1f8e25.apk`
unless `JA11_ROLLBACK_APK` names that same verified artifact. Do not use the superseded J021 build
for further JA11 interaction. If a hardware result or write is uncertain, do not change app
versions; preserve logs and resolve the device state first.

## Before any mutation

1. Resolve the current wireless runtime endpoint using `adb devices -l`. If the existing Pixel is online, verify its model with the prepared script and select exactly one explicit serial. If discovery is empty, use the mission ADB recovery ladder; pairing details are a last step only.
2. Verify manufacturer `Google` and model `Pixel 9`. Inspect only the diagnostic package `com.weekssa.opraeqforuapp.ja11diag`. If absent, install the exact corrected candidate. If present, pull its sole installed `base.apk` into the private evidence directory and require its SHA-256 to equal the J021 APK `767b42591adc92f0e1662480112bf9efe87ce15f51060d20a7aa39118b8d8c24`, package/version to be `0.8.0-ja11diag`/11, and debug signer SHA-256 to be `73aa7581c8dc7dcc8ccea7586771119a98f9a74d7d8cf23716e09c557c9f6b41`. Then update only that package using `adb install -r` so its data remains. Any other installed candidate/signature is a stop. Never uninstall or update the stable package.
3. Start full local log capture to the private acceptance directory with `phone-session.sh "$SERIAL" start-logcat` in its own foreground terminal session. Leave that process running during the procedure and stop it with Ctrl-C in the same session so the capture remains attached and flushes to disk. Do not detach it. Launch the replacement diagnostic package and confirm `APP_BUILD_INFO` has the exact package, replacement source SHA, version and diagnostics enabled. Save a local screenshot of the candidate app/build state with `phone-session.sh "$SERIAL" capture-screen`.
4. Attach the JA11 and use the app's normal identity gate. Confirm VID `2972`, PID `0101` or `0102`, firmware `2.20` when readable, and one fresh session. If identity is ambiguous or another DAC appears, stop without sending commands.
5. Capture the initial state before any mutation: volume, mic/headset state, program, UAC, sample rate, global gain, all five User 1 bands, firmware, PID and session generation. Obtain bands/global gain from a complete `SNAPSHOT_READ_COMPLETE` event, source SHA and session generation included. Save a sanitized baseline record and a screenshot of the My DAC state in the private evidence folder. The current snapshot reader supports this event only while the active program is Off or User 1. If the initial program is Vocal, Classic or Bass, do not change it and do not mutate anything; release the phone and resume off-phone with a revised safe baseline path. No mutation until the complete baseline exists.
6. Before any write, run the helper's read-only `verify-identity`. It requires the exact candidate `APP_BUILD_INFO`; a same-process `USB_IDENTITY_DESCRIPTOR_STATUS` with `permissionGranted=true`, `serialStatus=READABLE_NULL`, `connectionSerialStatus=READABLE_NONBLANK`, `serialSource=USB_CONNECTION`, and JA11 product ID `257` or `258`; and the following `USB_SESSION_OPENED` event from that process with the same product ID. The descriptor status must be the latest one for the process. A complete `SNAPSHOT_READ_COMPLETE` event and `RESTART_IDENTITY_AVAILABILITY` must then match that opened session's generation and candidate source SHA, with `identityAvailable=true` and `sessionCurrent=true`. The helper also confirms the emitting package PID is still running and there is no later detach or session-close event for that generation. This proves that the opened-connection fallback supplied a nonblank serial to the fail-closed unique identity key without logging its value. If any field is missing or mismatched, stop before writing, capture evidence, and release the phone. Repeat the current-session identity check after each expected restart; never use a restart as an identity probe.

## Test A: microphone/headset restoration — J020 recovery and delayed-grant regression

J020 on the `a7880844` candidate completed On-to-Off, then permission arrived about 18.5 seconds
after request. A current replacement session read Mic Off and the unchanged EQ baseline but had no
stable identity; no automatic verifier event occurred. No second write or restoration was
attempted. The exact corrected candidate must establish fresh identity. If the complete fresh
baseline reads Mic Off, restore Mic On before other tests; if it already reads On, record restoration
as satisfied and skip all microphone writes.

- If the fresh baseline reads Mic Off, trigger exactly one Off-to-On write in My DAC → DEVICE. This
  is both the required restoration and the corrected candidate's Test A. Do not toggle again after
  terminal success plus fresh On readback and complete baseline verification.
- If the fresh baseline already reads Mic On, record that the original state is restored and skip
  all microphone mutations. Do not turn Mic Off just to repeat Test A; proceed to Test B only after
  the complete baseline has been captured and verified.
- Before each write require current candidate identity to be available. Never retry an uncertain
  write. If permission exceeds the 25-second JA11 bound, treat the attempt as terminal; do not
  replay it. If replacement identity is unavailable, stop all mutations even if read-only data is
  visible.
- For the Off-to-On restoration re-enumeration, if Android shows the USB permission dialog, capture
  it and leave it pending for about 12 seconds before accepting. This crosses the old 10-second fallback
  while staying inside the new 25-second deadline. Accept immediately if the prompt appeared late;
  never wait past 20 seconds from the visible prompt. The helper log records timing. If permission
  is denied or the attempt reaches its deadline, stop without replaying the write.
- Require completed command/report evidence, expected detach/re-enumeration if generated, old session closure, a new session generation, fresh read and requested value.
- Capture the permission dialog before owner confirmation if it appears, then capture the terminal My DAC readback. Keep screenshots local/private; do not capture serials, fingerprints, or host network details. Use logs and complete snapshots as authoritative evidence.
- Verify fresh Mic On plus the complete original state before any Test B/C/D operation.
- On failure: stop all mutation, capture read-only state and logs, and release the phone. Do not infer restoration from detach or a successful USB read.

## Test B: UAC mode

- Choose the opposite of the original mode.
- Trigger one change from My DAC → DEVICE. Record starting/ending PID, expected detach, new generation, fresh read and outcome.
- Capture the terminal UAC readback after reconnection.
- Require the matching re-enumerated PID (`2972:0101` for UAC 1.0, `2972:0102` for UAC 2.0) and requested fresh readback.
- Restore original mode once after verified success if it differs; verify original PID and mode on the replacement session.
- Stop on ambiguous identity, permission failure, uncertain write, missing replacement session or incorrect mode. Do not replay.

## Test C: one Flash from Off

1. Generate the temporary baseline profile from the exact candidate's complete snapshot event. Do not hand transcribe protocol data. Review its six fields against the app's imported preview and require JA11 optimization status **Exact**.
2. Import with My EQs → Import Personal EQ using these temporary test labels: Manufacturer `JA11 Acceptance`, Headphone model `User 1 Baseline`, EQ name `JA11 v0.8.1 baseline 61695803`; then stage the imported item.
3. Read current program. If needed, select Off using the ordinary DEVICE selector; freshly verify Off. Record the state. Do not use Reset or any preliminary reboot.
4. Press **Flash exactly once**. Let the existing transaction run to a terminal state. The expected sequence remains five band writes, global gain, select User 1, Apply, pre-Save verification, exactly one Save, then fresh-session final readback if restart occurs.
5. Pass only if the UI reports verified success, logs show exactly one Save command (`0x19`), no old-session continuation, and final User 1 bands/gain match the baseline values exactly. A restart requires a new session and final authoritative readback. A log gap or uncertain outcome is a failure/stop, not permission to retry.
6. Capture the terminal Flash result screen and final verified User 1 state.
7. If Flash fails: do not press Flash again. Stop mutation. Reacquire exact JA11 read-only and capture active program, the full User 1 bank/gain, last completed command and whether Save was reached. Release the Pixel when state is safe. Investigate off-phone in the same mission.

## Test D: full-power volume/program truth

Only after Flash passes:

1. Set output volume to 30 (or a similarly distinct safe level), then freshly verify it.
2. Select Off, then freshly verify it.
3. Perform one controlled physical JA11 unplug/reconnect. Wait for the normal app reconnect flow; do not restart or relaunch the app unless evidence requires.
4. Reacquire exact supported JA11 in a new session. Read actual volume and program. Record values; do not assume persistence and do not add setters for startup persistence.
5. Restore volume and program to the original captured values and verify both. Program restore uses the normal selector.

## Restoration and phone release

- Preserve the complete original User 1 bank/global gain. The Flash target is derived from that bank. If any coefficient differs, stop and do not use Reset; determine a safe restoration plan off-phone.
- Restore each changed mic/UAC/volume/program setting to the original value. Restore UAC first if it affects PID/permissions; verify each fresh read after reconnection.
- Capture final full bank/global gain and DEVICE state; require exact equality to the original baseline for all restorable fields.
- Capture one final My DAC screenshot after exact original-state readback. Use the timestamped `capture-screen` helper; it saves mode-0600 PNGs directly to the private local evidence folder.
- Stop log capture, copy logs/screenshots and a sanitized record into the private evidence directory, then remove only the staged test profile from Downloads and uninstall only `com.weekssa.opraeqforuapp.ja11diag` after the app's hardware state is safe and evidence is saved.
- At that point state: **PHONE RELEASED — YOU CAN TAKE THE PIXEL BACK**.

## Stop conditions

Stop mutation immediately for any wrong device identity, stale or ambiguous session, permission failure, missing/malformed baseline, source hash mismatch, failed fresh readback, transport completion unknown, unexpected state, persistent busy state, failed Flash, non-baseline target, or loss of logs. Never auto-replay; never press Flash twice; never send commands to an unqualified or ambiguous device. Capture read-only state and evidence, restore only when the exact safe restoration is known, and return the phone as soon as safe.
