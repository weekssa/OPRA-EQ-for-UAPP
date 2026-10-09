# JA11 v0.8.1 physical acceptance plan

## Current status — User 1 ordering correction

The candidate is source `92c11fb0e41ae11b118b2e7bb105234d6606dbdb` with diagnostic APK SHA-256
`ce3f417f20c275fd4d535cf5e70f658d8430af8fdd3f87ea950705fbfd574637`. G2/G3/G3a, independent
review, artifact provenance, exact APK cold launch on isolated API 35, and all 64 applicable
instrumentation tests pass. Exact-head PR #80 CI remains pending. Do not use the Pixel until those
checks pass. The user has already authorized the prepared testing cycle; do not request another
confirmation unless the phone is unavailable or scope changes. Once CI passes, install and verify
this exact diagnostic candidate, then run the read-only JA11 current-session/cardinality/baseline
gate before any hardware write. Do not manually install public stable v0.8.0; it is not this
source-bound diagnostic candidate.

The prior exact candidate passed its read-only identity gate and completed the allowed Mic
Off-to-On restoration. UAC 2.0-to-1.0 was verified; restoring UAC 2.0 had an expected reset, but
Android permission arrived after the transaction deadline. A later fresh read confirmed UAC 2.0.
Classify that UAC restore as permission-timing inconclusive, not a JA11 failure. One Flash from Off
then failed band 1 volatile readback before persistent Save. The retained command trace shows five
band writes and global gain before selecting User 1. Do not retry that candidate's Flash.

After read-only recovery, the active program was restored to Off and freshly verified. Mic was On;
UAC was 2.0; volume was 30; sample rate was 384 kHz; global gain was -3.7 dB; and the five-band
snapshot was flat. The candidate profile was removed, the previous diagnostic APK was restored, logs
were saved, and the Pixel was released. Evidence is appended to the validation ledger and retained
privately under `/private/tmp/ja11-v0.8.1-acceptance-61695803/owner-phone-session-20261009T161940Z-model-d-resume/`.

The regression indicates that JA11's active program must be User 1 before `0x15`/`0x17` data writes.
The corrected Flash and Reset paths select User 1 only when needed, verify that program in the same
session, and stop before data writes if the selector is not confirmed. Editor Apply also verifies
User 1 immediately before data writes and no longer reselects it afterward. This is strongly
supported by the physical trace but still requires exact-candidate hardware confirmation.

Before the next Flash, capture the real User 1 bank as the restoration baseline. If the initial
program is Off, use the normal program selector to select User 1, verify it, capture the complete
five-band/gain snapshot, then restore the original Off program and verify it. Build the temporary
Flash profile from that captured User 1 snapshot, not the Off/flat presentation. If the complete
User 1 baseline cannot be captured and its original program restored, stop before Flash.

## Historical pre-session Model D candidate — superseded by the current status above

This section records the state before the physical window described above. Its J020-era last-state
and no-physical-result statements are historical; the validation ledger records the later session.

The 2026-10-09 owner-approved Model D policy supersedes the serial-required instructions in the
historical J024 procedure below. J024 remains a read-only result: both Android serial readers
returned null from a permissioned, current JA11 session, and no write occurred. The last mutation
remains J020; Mic Off is the last verified value and restoring the original Mic On state remains
outstanding.

Production source commit `1d19067c9150aae1b09e01fafb8af3647bf65f71` and its exact diagnostic APK have
been built and artifact-verified. APK SHA-256 is
`23adf9f4955b056f110562362717c706767b7e3ecc50225c07439a3ff5c23711`; package/version/code is
`com.weekssa.opraeqforuapp.ja11diag`, `0.8.0-ja11diag` / `11`; signer SHA-256 is
`73aa7581c8dc7dcc8ccea7586771119a98f9a74d7d8cf23716e09c557c9f6b41`. The exact diagnostic APK
cold-launched on the isolated API 35 AVD and reported the exact source SHA. All 64 applicable tests
passed on the same source snapshot's debug test variant because the project does not provide a
diagnostic-variant instrumentation APK. All eight checks passed on app-source SHA
`1d19067c9150aae1b09e01fafb8af3647bf65f71` and docs/helper PR head
`69076094811c13365bafecb9a1e0cf50be8d14f0`. The first UI job attempt on that head failed after a
recorded Nexus Launcher input-dispatch ANR; its targeted retry passed 64/64 with no failures, errors,
or skips. Candidate and rollback artifact pins were
reverified host-only, without enumerating an Android device. This status synchronization creates a
further docs-only head; require all eight checks on the exact live head before freezing the physical
candidate.

This was the pre-session instruction before the owner authorized the physical testing cycle and
before the prior session was completed. The current permission/status is stated at the top of this
file. The new source must still pass exact-head CI before the Pixel is used.

During that window, install and verify the exact candidate and begin with a read-only full baseline.
Require the supported VID/PID allowlist, valid fresh HID session, current permission/generation, and
exactly one supported JA11 candidate. Serial is optional. If both old and replacement sessions
provide a serial, reject mismatch; absent serial is permitted only for one operation-bound returning
candidate with every expected-reset lifecycle gate and fresh readback satisfied. Do not claim the
same physical unit without a matching usable serial. If the baseline reads Mic Off, restore it to On
as the first mutation; if it already reads On, skip the write. Never replay an uncertain write.

This confirmation workflow is historical for the prior phone window and is superseded by the user's
continuing authorization for this prepared acceptance cycle. If availability changes or scope grows,
stop and ask; otherwise proceed after exact-head CI, capture evidence, and release the Pixel as soon
as sufficient evidence is recorded.

The prior full acceptance plan budget was about 25–35 minutes if the existing wireless ADB connection is reusable. If ADB recovery requires prolonged troubleshooting or pairing setup, stop before attaching the JA11 or mutating it, release the phone, and resume host preparation off-phone.

## Historical J024 off-phone candidate — do not reuse for physical acceptance

- App source: `616958037349e2f0e0784a556c6430b0de6ceb18` (tree
  `fdb6c8d10c8aa865da1a4818d22a7c14b2559b21`).
- Diagnostic APK: `opra-eq-ja11diag-0.8.0-source-61695803.apk`.
- APK SHA-256: `ee0fe4fbfaae7b3f959d4122f0c21c128dffdf21d47586376ee383e2534ceb0d`.
- Package/version/code: `com.weekssa.opraeqforuapp.ja11diag`, `0.8.0-ja11diag` / `11`.
- Debug signer certificate SHA-256:
  `73aa7581c8dc7dcc8ccea7586771119a98f9a74d7d8cf23716e09c557c9f6b41`.
- Private APK: `/private/tmp/ja11-v0.8.1-acceptance-61695803/opra-eq-ja11diag-0.8.0-source-61695803.apk`.
- A wiped API 35 emulator cold-launched the exact source-bound APK and reported the exact source
  SHA in `APP_BUILD_INFO`; API 35 instrumentation passed 64/64. All eight required checks passed on
  exact pushed PR #80 head `f2f6c3b2317f22cf4fe23ce9ddd4878cf957d03d`. These results qualify only
  the J024 candidate, not the Model D implementation.
- J024 then failed the read-only identity gate on this candidate. These CI/emulator results do not
  establish hardware acceptance. Do not repeat this identity test on the same APK.

This candidate includes the JA11-only opened-connection serial fallback, which reads the serial from
the claimed connection only when `UsbDevice.serialNumber` returns null. J024 proved that this reader
also returned null on the connected unit. The owner later approved serial-optional Model D behavior;
neither this candidate nor its old physical procedure is the new candidate.

## Candidate-specific install and rollback — refresh after the Model D candidate freezes

Before updating, the helper pulls the installed diagnostic package's `base.apk` into the private
evidence directory and verifies its checksum, package/version, and debug signer against the frozen
candidate sidecar. That prior-installed APK and the rollback APK must be identified and hashed before
the phone window opens; refresh the helper constants for this exact candidate and current installed
artifact. The new candidate is installed in place with `adb install -r`, preserving app data. Never
uninstall the old package as an installation step. If install fails, stop, preserve the captured APK
and logs, and do not retry or start a JA11 operation. If software rollback is needed after no JA11
command was sent, or after the complete original JA11 state has been freshly verified, run
`phone-session.sh "$SERIAL" rollback`. It must use the preverified exact prior diagnostic APK and
verify the restored package version, checksum, and signer. If a hardware result or write is
uncertain, do not change app versions; preserve logs and resolve the device state first.

## Before any mutation

1. Resolve the current wireless runtime endpoint using `adb devices -l`. If the existing Pixel is online, verify its model with the prepared script and select exactly one explicit serial. If discovery is empty, use the mission ADB recovery ladder; pairing details are a last step only.
2. Verify manufacturer `Google` and model `Pixel 9`. Inspect only `com.weekssa.opraeqforuapp.ja11diag`. Use the exact prior-installed APK checksum and the frozen package/version/signer tuple recorded in the candidate sidecar; pull and verify the sole installed `base.apk` before in-place update. Any unknown installed build or signature is a stop. Never uninstall or update the stable package.
3. Start full local log capture to the private acceptance directory with `phone-session.sh "$SERIAL" start-logcat` in its own foreground terminal session. Leave that ADB process running during the procedure and stop it with Ctrl-C in the same session so the capture remains attached and flushes to disk. Do not detach it. Launch the diagnostic package and confirm `APP_BUILD_INFO` has the exact frozen source SHA, package/version, and diagnostics enabled. The `verify` and `verify-ja11-session` helpers read this retained local log and require its matching ADB process to remain active; it must begin before launch and stay running through verification. Save a local screenshot of the candidate app/build state with `phone-session.sh "$SERIAL" capture-screen`.
4. Attach the JA11 and use the app's normal session gate. Confirm VID `2972`, PID `0101` or `0102`, firmware `2.20` when readable, current permission, a fresh session, valid HID endpoints, and exactly one supported JA11 candidate. A serial may be recorded as optional continuity evidence; its absence alone does not stop the read-only baseline.
5. Capture the initial state before any mutation: volume, mic/headset state, active program, UAC, sample rate, the current program's reported bands/gain, firmware, PID and session generation. Obtain bands/gain from a complete `SNAPSHOT_READ_COMPLETE` event with source SHA and session generation. If Off is active, its flat presentation is not a baseline for the stored User 1 bank. Save a sanitized record and My DAC screenshot in the private evidence folder. The reader supports complete snapshots only while Off or User 1 is active. If the initial program is Vocal, Classic or Bass, do not change it; release the phone and resume off-phone with a revised safe baseline path. No mutation until the active-state baseline exists.
6. Before any write, run the helper's read-only `verify-ja11-session`. It requires exact candidate `APP_BUILD_INFO`; a same-process permissioned JA11 descriptor event; one supported candidate in both `USB_SESSION_OPENED` and `RESTART_IDENTITY_AVAILABILITY`; and a complete `SNAPSHOT_READ_COMPLETE` plus current-session event for the same source SHA, process, and generation. It confirms the emitting app process remains active and no later detach, close, or ambiguous attach invalidated the session. Serial status is printed only as a category and is optional. If candidate count, session currency, baseline, or source binding is missing or ambiguous, stop before writing, preserve evidence, and release the phone. Repeat current-session/cardinality/readback checks after every expected restart; never use a restart as an identity probe.

## Test A: microphone/headset restoration — J020 recovery and delayed-grant regression

J020 on the `a7880844` candidate completed On-to-Off, then permission arrived about 18.5 seconds
after request. A current replacement session read Mic Off and the unchanged EQ baseline, but the
old candidate did not run the automatic verifier. No second write or restoration was attempted.
Under Model D, the exact new candidate must establish a sole supported replacement and complete
the operation-bound lifecycle/readback checks; serial remains optional. If the complete fresh
baseline reads Mic Off, restore Mic On before other tests; if it already reads On, record restoration
as satisfied and skip all microphone writes.

- If the fresh baseline reads Mic Off, trigger exactly one Off-to-On write in My DAC → DEVICE. This
  is both the required restoration and the corrected candidate's Test A. Do not toggle again after
  terminal success plus fresh On readback and complete baseline verification.
- If the fresh baseline already reads Mic On, record that the original state is restored and skip
  all microphone mutations. Do not turn Mic Off just to repeat Test A.
- Before each write require one current supported JA11 candidate and a complete fresh baseline.
  Never retry an uncertain write. If permission exceeds the 25-second JA11 bound, treat the attempt
  as terminal; do not replay it. A missing serial alone is allowed only when all operation-bound
  replacement, permission, generation, cardinality, and authoritative-readback checks pass.
- For the Off-to-On restoration re-enumeration, if Android shows the USB permission dialog, capture
  it and leave it pending for about 12 seconds before accepting. This crosses the old 10-second fallback
  while staying inside the new 25-second deadline. Accept immediately if the prompt appeared late;
  never wait past 20 seconds from the visible prompt. The helper log records timing. If permission
  is denied or the attempt reaches its deadline, stop without replaying the write.
- Require completed command/report evidence, expected detach/re-enumeration if generated, old session closure, a new session generation, fresh read and requested value.
- Capture the permission dialog before owner confirmation if it appears, then capture the terminal My DAC readback. Keep screenshots local/private; do not capture serials, fingerprints, or host network details. Use logs and complete snapshots as authoritative evidence.
- Verify fresh Mic On plus the complete original state before any Test B/C/D operation.
- Before Test B, if the original program was Off and the stored User 1 bank was not visible in the
  initial snapshot, select User 1 using the ordinary DEVICE selector, verify it, capture a complete
  User 1 bands/gain snapshot, then restore and verify the original Off program. Do this only after
  Mic restoration is complete. Stop before Test B if the complete User 1 baseline or original
  program restoration is uncertain. If User 1 was active initially, use the already captured
  complete snapshot.
- On failure: stop all mutation, capture read-only state and logs, and release the phone. Do not infer restoration from detach or a successful USB read.

## Test B: UAC mode

- Choose the opposite of the original mode.
- Trigger one change from My DAC → DEVICE. Record starting/ending PID, expected detach, new generation, fresh read and outcome.
- Capture the terminal UAC readback after reconnection.
- Require the matching re-enumerated PID (`2972:0101` for UAC 1.0, `2972:0102` for UAC 2.0) and requested fresh readback.
- Restore original mode once after verified success if it differs; verify original PID and mode on the replacement session.
- Stop on multiple plausible candidates, a serial mismatch when both serials are usable, permission failure, uncertain write, missing replacement session or incorrect mode. Do not replay.

## Test C: one Flash from Off

1. Capture a complete User 1 snapshot for the restoration baseline. If the initial program is Off, select User 1 with the normal DEVICE selector, freshly verify User 1, capture all five bands and global gain, then restore and freshly verify the original Off program. Do not use Reset or Save for this baseline capture.
2. Generate the temporary baseline profile from the exact candidate's complete User 1 snapshot event. Do not hand transcribe protocol data. Review its six fields against the app's imported preview and require JA11 optimization status **Exact**.
3. Import with My EQs → Import Personal EQ using temporary labels that include the frozen source SHA prefix; then stage the imported item.
4. Read current program. If needed, select Off using the ordinary DEVICE selector; freshly verify Off. Record the state.
5. Press **Flash exactly once**. The expected sequence is: select User 1 if needed, read back User 1 in the same current session, write five bands, write global gain, Apply, pre-Save verification, exactly one Save, then fresh-session final readback if restart occurs. No `0x15` or `0x17` write may precede verified User 1 selection.
6. Pass only if the UI reports verified success, logs show exactly one Save command (`0x19`), no old-session continuation, and final User 1 bands/gain match the captured User 1 baseline exactly. A restart requires a new session and final authoritative readback. A log gap or uncertain outcome is a failure/stop, not permission to retry.
7. Capture the terminal Flash result screen and final verified User 1 state.
8. If Flash fails: do not press Flash again. Stop mutation. Reacquire exact JA11 read-only and capture active program, the full User 1 bank/gain, last completed command and whether Save was reached. Release the Pixel when state is safe. Investigate off-phone in the same mission.

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
