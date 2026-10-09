# FiiO JA11 v0.8.1 acceptance package

This folder contains the one-session Pixel/JA11 procedure and small host tools. J020 remains
incomplete: its replacement session read Mic Off but did not establish restart identity, and
restoration to Mic On remains outstanding. The corrected application source is frozen at
`da1f8e25918065667648d676cb669fed4c803f17`; its source-bound diagnostic APK, isolated API 35
emulator launch, and host-only ADB preparation are complete. All eight required checks passed on
corrected-code PR head `ef688ca1a2c805c349439eb0e9ac24fb456641ad`; the frozen app source is
`da1f8e25918065667648d676cb669fed4c803f17`. Check PR #80 for the live status of any later
documentation-only head. Do not request or use the Pixel until current PR-head review and CI pass
and the owner is available.

J021 used this `da1f8e2` APK for a read-only initial-session observation and found that
`UsbDevice.serialNumber` returned null despite permission being granted. It did not test a
replacement session or perform a write. Do not use this APK for physical mutation. The next
candidate must include the JA11-only opened-connection serial fallback, then receive a new exact
source/APK tuple, emulator verification, helper constants, and exact-head PR checks before this
procedure can begin.

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

## Corrected diagnostic candidate

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

The prior APK left by J020 has SHA-256
`7beb5bcebbc0dc40a68b33de911cc8722d76d3f0ff2e98685b1fa25e17caed61`, package
`com.weekssa.opraeqforuapp.ja11diag`, version `0.8.0-ja11diag`/11, and debug certificate SHA-256
`73aa7581c8dc7dcc8ccea7586771119a98f9a74d7d8cf23716e09c557c9f6b41`. The install helper verifies
this exact prior package before an in-place `adb install -r` of the corrected APK, preserving app
data. A mismatch stops before installation.

The local candidate sidecar `CANDIDATE.md` records the local APK location and exact-head evidence. This package appends no Pixel serials, private network identifiers, raw logs, or hardware-specific personal data to the repository.

`start-logcat` runs as a foreground process so its lifetime is owned by the terminal session and its
full output is flushed into the private evidence directory. Keep that terminal session open during
the physical procedure and send Ctrl-C there when capture should stop; do not launch it as a detached
background job.

## Preparation tools

- `make-baseline-profile.py` converts the latest complete `SNAPSHOT_READ_COMPLETE` event for the exact candidate SHA into a temporary Equalizer APO profile and a value summary. It rejects missing fields, wrong source, unsupported types, out-of-range values, values outside JA11 native quantization, and existing output paths.
- `phone-session.sh` requires one explicit ADB serial, checks that it identifies Google Pixel 9 before package actions, verifies the frozen APK checksum, and operates only on `.ja11diag`. If the prior J020 diagnostic package is installed, it pulls and verifies the exact prior APK checksum and signer before updating in place with `adb install -r`, preserving app data. The `rollback` action is available only for a safe software revert: it verifies the installed corrected APK, reinstalls the exact J020 APK with `-r`, then verifies the restored APK checksum, package/version, and signer. Never use the superseded J020 build for further JA11 interaction. A different installed build or signature is a stop. The `uninstall` action targets only `.ja11diag`; log, pulled APK, package dump and screenshot output stays in a caller-provided private local evidence directory.

Before any restart-control write, run `verify-identity` after launching the exact corrected candidate
and obtaining a complete fresh snapshot. It requires that candidate's `APP_BUILD_INFO` plus a
same-process snapshot and `RESTART_IDENTITY_AVAILABILITY` on the same session generation. The
identity event reveals only booleans and generation; per-session serial diagnostics reveal a status
category only. Stop before mutation unless the identity key is uniquely available and the session
is current. Repeat the identity check after every expected restart.

Set `JA11_ADB_BIN`, `JA11_APKSIGNER_BIN`, `JA11_CANDIDATE_APK`, and `JA11_EVIDENCE_DIR` from the local candidate sidecar when the owner is participating. The exact path/serial are runtime values and do not belong in committed evidence. The script never chooses among devices automatically.

## Procedure

Follow [PHONE-PLAN.md](PHONE-PLAN.md) in order. It begins with current baseline and identity, restores
Mic On first when the current state is Off, and uses that one Off-to-On write as corrected Test A.
It continues with UAC, one Flash from Off using the baseline-identical profile, full-power
volume/program truth, complete original-state readback and stop conditions. Do not repeat uncertain
writes or Flash.
