# JA11 v0.8.1 physical acceptance plan

Run only after the owner is available and the prepared exact candidate is still valid. One bundled session only. Do not perform research/build/documentation work while the Pixel is occupied.

## Before any mutation

1. Resolve the current wireless runtime endpoint using `adb devices -l`. If the existing Pixel is online, verify its model with the prepared script and select exactly one explicit serial. If discovery is empty, use the mission ADB recovery ladder; pairing details are a last step only.
2. Verify manufacturer `Google` and model `Pixel 9`. Inspect only the diagnostic package `com.weekssa.opraeqforuapp.ja11diag`. If absent, install the exact frozen candidate. If present, pull its sole installed `base.apk` into the private evidence directory and require its SHA-256 to equal the prior J019 APK `85e06ca0db818586a7eb2eab3378a1b21949b3c8593e1318536ec651d8369305`, package/version to be `0.8.0-ja11diag`/11, and debug signer SHA-256 to be `73aa7581c8dc7dcc8ccea7586771119a98f9a74d7d8cf23716e09c557c9f6b41`. Then update only that package using `adb install -r` so its data remains. Any other installed candidate/signature is a stop. Never uninstall or update the stable package.
3. Start full local log capture to the private acceptance directory, launch the replacement diagnostic package, and confirm `APP_BUILD_INFO` has the exact package, replacement source SHA, version and diagnostics enabled.
4. Attach the JA11 and use the app's normal identity gate. Confirm VID `2972`, PID `0101` or `0102`, firmware `2.20` when readable, and one fresh session. If identity is ambiguous or another DAC appears, stop without sending commands.
5. Capture the initial state before any mutation: volume, mic/headset state, program, UAC, sample rate, global gain, all five User 1 bands, firmware, PID and session generation. Obtain bands/global gain from a complete `SNAPSHOT_READ_COMPLETE` event, source SHA and session generation included. Save a sanitized baseline record in the private evidence folder. The current snapshot reader supports this event only while the active program is Off or User 1. If the initial program is Vocal, Classic or Bass, do not change it and do not mutate anything; release the phone and resume off-phone with a revised safe baseline path. No mutation until the complete baseline exists.
6. Before Test A or Test B, require the latest successful `RESTART_IDENTITY_AVAILABILITY` event to show `identityAvailable=true`, `sessionCurrent=true` and the same session generation as the complete snapshot. This event reveals no serial or fingerprint. If identity is unavailable, missing, stale or mismatched, stop before any restart-control write and release the phone; do not use an attempted restart as an identity probe.

## Test A: microphone/headset control — retry after J019 failure

J019 on the prior `3f5e0c3` candidate verified On-to-Off but timed out during automatic Off-to-On verification. A read-only refresh later confirmed Mic On and exact baseline restoration; no write was retried. The replacement candidate must repeat Test A from the newly captured current baseline. This does not establish J019's root cause or transfer its result to the new candidate.

- Choose the opposite of the original mic/headset control value.
- Trigger one change in My DAC → DEVICE. Do not retry if the write result is uncertain.
- Require completed command/report evidence, expected detach/re-enumeration if generated, old session closure, a new session generation, fresh read and requested value.
- If the requested value differs from the original, restore the original once after the first result is fully verified; independently confirm fresh readback.
- On failure: stop all mutation, reacquire exact JA11 read-only, capture current value/session and logs. No repeated write unless a later off-phone decision and new exact candidate authorize another session.

## Test B: UAC mode

- Choose the opposite of the original mode.
- Trigger one change from My DAC → DEVICE. Record starting/ending PID, expected detach, new generation, fresh read and outcome.
- Require the matching re-enumerated PID (`2972:0101` for UAC 1.0, `2972:0102` for UAC 2.0) and requested fresh readback.
- Restore original mode once after verified success if it differs; verify original PID and mode on the replacement session.
- Stop on ambiguous identity, permission failure, uncertain write, missing replacement session or incorrect mode. Do not replay.

## Test C: one Flash from Off

1. Generate the temporary baseline profile from the exact candidate's complete snapshot event. Do not hand transcribe protocol data. Review its six fields against the app's imported preview and require JA11 optimization status **Exact**.
2. Import with My EQs → Import Personal EQ using these temporary test labels: Manufacturer `JA11 Acceptance`, Headphone model `User 1 Baseline`, EQ name `JA11 v0.8.1 baseline a7880844`; then stage the imported item.
3. Read current program. If needed, select Off using the ordinary DEVICE selector; freshly verify Off. Record the state. Do not use Reset or any preliminary reboot.
4. Press **Flash exactly once**. Let the existing transaction run to a terminal state. The expected sequence remains five band writes, global gain, select User 1, Apply, pre-Save verification, exactly one Save, then fresh-session final readback if restart occurs.
5. Pass only if the UI reports verified success, logs show exactly one Save command (`0x19`), no old-session continuation, and final User 1 bands/gain match the baseline values exactly. A restart requires a new session and final authoritative readback. A log gap or uncertain outcome is a failure/stop, not permission to retry.
6. If Flash fails: do not press Flash again. Stop mutation. Reacquire exact JA11 read-only and capture active program, the full User 1 bank/gain, last completed command and whether Save was reached. Release the Pixel when state is safe. Investigate off-phone in the same mission.

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
- Stop log capture, copy logs/screenshots and a sanitized record into the private evidence directory, then remove only the staged test profile from Downloads and uninstall only `com.weekssa.opraeqforuapp.ja11diag` after the app's hardware state is safe and evidence is saved.
- At that point state: **PHONE RELEASED — YOU CAN TAKE THE PIXEL BACK**.

## Stop conditions

Stop mutation immediately for any wrong device identity, stale or ambiguous session, permission failure, missing/malformed baseline, source hash mismatch, failed fresh readback, transport completion unknown, unexpected state, persistent busy state, failed Flash, non-baseline target, or loss of logs. Never auto-replay; never press Flash twice; never send commands to an unqualified or ambiguous device. Capture read-only state and evidence, restore only when the exact safe restoration is known, and return the phone as soon as safe.
