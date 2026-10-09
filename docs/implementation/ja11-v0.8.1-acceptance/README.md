# FiiO JA11 v0.8.1 acceptance package

This folder contains the one-session Pixel/JA11 procedure and host tools. J020 remains incomplete:
its replacement session read Mic Off but did not establish restart identity, and restoration to Mic
On remains outstanding. J021 was a read-only initial session on the prior `da1f8e2` candidate; it
showed that Android returned null from `UsbDevice.serialNumber` despite granted USB permission.
The current replacement source is `616958037349e2f0e0784a556c6430b0de6ceb18`; its exact-source
diagnostic APK and local/emulator results are recorded below. The owner has made the Pixel available.
Use it only after every required check passes on the latest live PR #80 head and the exact APK tuple
is refreshed. Do not use the prior J020 or J021 APK for physical acceptance.

J021 used this `da1f8e2` APK for a read-only initial-session observation and found that
`UsbDevice.serialNumber` returned null despite permission being granted. It did not test a
replacement session or perform a write. Do not use this APK for physical mutation. The current
replacement below adds the JA11-only opened-connection serial fallback while preserving the
unique nonblank serial requirement.

## J020 diagnostic candidate — superseded; do not install again

- Application source SHA: `a78808443c71d688e0f338e96495847569fe12f7`
- APK filename: `opra-eq-ja11diag-0.8.0-source-a7880844.apk`
- APK SHA-256: `7beb5bcebbc0dc40a68b33de911cc8722d76d3f0ff2e98685b1fa25e17caed61`
- Package: `com.weekssa.opraeqforuapp.ja11diag`
- versionName/versionCode: `0.8.0-ja11diag` / `11`
- Debug certificate SHA-256: `73aa7581c8dc7dcc8ccea7586771119a98f9a74d7d8cf23716e09c557c9f6b41`

J020 used this candidate for one Mic On-to-Off mutation. The replacement session read Mic Off but
reported identity unavailable; no restart-verifier event was recorded and restoration to Mic On
remains outstanding. This APK is superseded for physical use. Its earlier CI/emulator evidence does
not transfer to the corrected source.

## J021 diagnostic candidate — superseded; read-only observation only

**Off-phone artifact and corrected-code CI verified; physical qualification pending.** App-source commit:
`da1f8e25918065667648d676cb669fed4c803f17`; source tree:
`aa41aba1ddfe37006aab0f1cfdb21c4df63c2481`.

- APK: `opra-eq-ja11diag-0.8.0-source-da1f8e25.apk`
- APK SHA-256: `767b42591adc92f0e1662480112bf9efe87ce15f51060d20a7aa39118b8d8c24`
- Package/version/code: `com.weekssa.opraeqforuapp.ja11diag`, `0.8.0-ja11diag`, `11`
- Debug signer certificate SHA-256: `73aa7581c8dc7dcc8ccea7586771119a98f9a74d7d8cf23716e09c557c9f6b41`
- Build: `./tools/codex-android :app:assembleJa11Diagnostic -PCANDIDATE_SOURCE_SHA=da1f8e25918065667648d676cb669fed4c803f17 --max-workers=2`
- G2 focused regressions and G3 JVM/lint/debug/release/diagnostic/Android-test/R8 gates passed;
  the exact XML count is 818 JVM tests, 0 failures, errors, or skips, across 129 suites.
- API 35 instrumentation passed 64/64 on the clean AVD; this APK was installed and cold-launched
  only on the isolated emulator. Its `APP_BUILD_INFO` reports the exact source SHA above.
- Private sidecar with exact command output, APK, package dump, emulator event log, and host-only
  preflight: `/private/tmp/ja11-v0.8.1-acceptance-da1f8e25/CANDIDATE.md`.

This debuggable diagnostic APK is not the official release artifact and has no physical acceptance
claim. Do not install the J020 APK again.

The latest candidate installed before the new physical session is the J021 APK with SHA-256
`767b42591adc92f0e1662480112bf9efe87ce15f51060d20a7aa39118b8d8c24`, package
`com.weekssa.opraeqforuapp.ja11diag`, version `0.8.0-ja11diag`/11, and debug certificate SHA-256
`73aa7581c8dc7dcc8ccea7586771119a98f9a74d7d8cf23716e09c557c9f6b41`. The install helper verifies
this exact prior package before an in-place `adb install -r` of the replacement APK, preserving app
data. A mismatch stops before installation.

## Current replacement diagnostic candidate

**Off-phone build and emulator validation complete; exact latest PR-head checks and physical
qualification remain pending.** Application source commit `616958037349e2f0e0784a556c6430b0de6ceb18`,
tree `fdb6c8d10c8aa865da1a4818d22a7c14b2559b21`.

- APK: `opra-eq-ja11diag-0.8.0-source-61695803.apk`
- APK SHA-256: `ee0fe4fbfaae7b3f959d4122f0c21c128dffdf21d47586376ee383e2534ceb0d`
- Package/version/code: `com.weekssa.opraeqforuapp.ja11diag`, `0.8.0-ja11diag`, `11`
- Debug signer certificate SHA-256: `73aa7581c8dc7dcc8ccea7586771119a98f9a74d7d8cf23716e09c557c9f6b41`; v2 signature verified.
- Private APK: `/private/tmp/ja11-v0.8.1-acceptance-61695803/opra-eq-ja11diag-0.8.0-source-61695803.apk`
- Local full JVM result: 825 tests, 0 failures, 0 errors, 0 skipped; focused JA11 regressions, lint,
  diagnostic/debug/release builds, Android-test compilation and R8 mapping verification passed.
- Wiped API 35 emulator: exact `APP_BUILD_INFO` source SHA and cold launch passed; instrumentation
  passed 64/64.
- Exact PR #80 CI must pass on the latest live head before any phone command. These emulator and
  software results do not establish JA11 hardware behavior.

The J021 APK is the only configured software rollback target. It is superseded and must not be used
for further physical acceptance.

The local candidate sidecar `CANDIDATE.md` records the local APK location and exact-head evidence. This package appends no Pixel serials, private network identifiers, raw logs, or hardware-specific personal data to the repository.

`start-logcat` runs as a foreground process so its lifetime is owned by the terminal session and its
full output is flushed into the private evidence directory. Keep that terminal session open during
the physical procedure and send Ctrl-C there when capture should stop; do not launch it as a detached
background job.

## Preparation tools

- `make-baseline-profile.py` converts the latest complete `SNAPSHOT_READ_COMPLETE` event for the exact candidate SHA into a temporary Equalizer APO profile and a value summary. It rejects missing fields, wrong source, unsupported types, out-of-range values, values outside JA11 native quantization, and existing output paths.
- `phone-session.sh` requires one explicit ADB serial, checks that it identifies Google Pixel 9 before package actions, verifies the frozen APK checksum, and operates only on `.ja11diag`. If the prior J021 diagnostic package is installed, it pulls and verifies the exact prior APK checksum and signer before updating in place with `adb install -r`, preserving app data. The `rollback` action is available only for a safe software revert: it verifies the installed corrected APK, reinstalls the exact J021 APK with `-r`, then verifies the restored APK checksum, package/version, and signer. The J021 build is superseded and must not be used for further JA11 acceptance. A different installed build or signature is a stop. The `uninstall` action targets only `.ja11diag`; log, pulled APK, package dump and screenshot output stays in a caller-provided private local evidence directory.

Before any write, run `verify-identity` after launching the exact replacement candidate and obtaining
a complete fresh snapshot. It requires the candidate's `APP_BUILD_INFO`, then a same-process
`USB_IDENTITY_DESCRIPTOR_STATUS` showing permission granted, a null device serial and a nonblank
opened-connection serial (`serialSource=USB_CONNECTION`) for PID `257` or `258`, followed by
`USB_SESSION_OPENED` for the same product ID.
It then requires the complete snapshot and `RESTART_IDENTITY_AVAILABILITY` for that opened session's
generation and candidate source SHA. The identity event reveals only booleans and generation; the
USB descriptor event reveals only status categories and product ID. Neither event records the serial
or fingerprint. The helper's terminal summary also omits serial and fingerprint. Its mode-0600
private event file retains the captured diagnostic stream, including snapshot band and gain values,
under the mode-0700 evidence directory. It also confirms the emitting package PID is still live and
no later detach or session-close event invalidated the opened generation. This read-only gate proves
that the current JA11 session's fail-closed unique identity key uses a nonblank serial from the
opened-connection fallback. If Mic is Off in the complete baseline, its first permitted Off-to-On
restoration transaction verifies that identity survives real re-enumeration. If Mic is already On,
record restoration as satisfied and skip Mic writes; reconnect stability is then verified by the
next permitted expected-restart transaction. If any read-only evidence is missing, stale, mismatched,
or unavailable, stop before mutation and release the phone. Repeat the current-session identity
check after each expected restart.

Set `JA11_ADB_BIN`, `JA11_APKSIGNER_BIN`, `JA11_CANDIDATE_APK`, and `JA11_EVIDENCE_DIR` from the local candidate sidecar when the owner is participating. The exact path/serial are runtime values and do not belong in committed evidence. The script never chooses among devices automatically.

## Procedure

Follow [PHONE-PLAN.md](PHONE-PLAN.md) in order. It begins with current baseline and identity. If Mic
is Off, its first write restores Mic On and serves as corrected Test A. If Mic is already On, record
that restoration is satisfied and skip all microphone mutations. It continues with UAC, one Flash
from Off using the baseline-identical profile, full-power
volume/program truth, complete original-state readback and stop conditions. Do not repeat uncertain
writes or Flash.
