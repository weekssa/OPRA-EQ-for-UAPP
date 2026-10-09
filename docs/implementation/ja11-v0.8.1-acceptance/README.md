# FiiO JA11 v0.8.1 acceptance package

This folder contains the one-session Pixel/JA11 procedure and host tools. **Current policy: the
owner-approved Model D makes USB serial optional continuity evidence.** Initial JA11 reads and
same-session controls require one exact supported JA11 candidate, valid HID endpoints, current
permission, a fresh claimed connection, and a current generation. Expected-reset verification also
requires an accepted write, expected detach, old-session invalidation, fresh permission/open/claim,
one returning candidate, a new generation, and authoritative readback. Compare serials only when
both sessions provide usable serials; mismatch fails closed. Without serial, success means state was
verified on the sole returning JA11, not that the same physical unit was proven. Uncertain writes
are never replayed.

J020 remains the latest mutation and Mic Off the last verified value; restoring original Mic On is
outstanding. J024 was a read-only test of the prior candidate: both Android serial readers returned
null, and no write occurred. That historical observation does not block Model D, and that APK must
not be repeated. Finish all off-phone gates, freeze and verify the exact candidate, then request
**PHONE WINDOW READY — PIXEL + JA11 NEEDED** with the source/APK hashes, tests, expected writes and
resets, Android permission interaction, occupancy, pass/fail conditions, and restoration obligations.
Wait for a fresh explicit confirmation before any physical command.

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

J024 installed the `61695803` APK during its recorded session. A private base APK captured from that
installed package on 2026-10-09 was reverified off-phone by checksum, package/version/code, and
signer; the helper pins it as the prior-installed artifact and rollback APK. At the next phone
window the helper will still pull and verify the actual installed package before `adb install -r`.
Unknown checksum, package/version, or signer remains a stop. The new candidate will be installed in
place so app data is preserved.

## Historical J024 diagnostic candidate — superseded; do not reuse

This exact candidate passed its then-applicable off-phone and PR gates. J024's serial-required stop
conclusion is superseded by Model D. These results do not qualify the Model D source. Its physical
candidate freeze remains gated on all checks passing for the exact final PR head. Application source
commit `616958037349e2f0e0784a556c6430b0de6ceb18`,
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
- All eight PR #80 checks passed on exact pushed head `f2f6c3b2317f22cf4fe23ce9ddd4878cf957d03d`.
  J024 then showed that this candidate cannot establish the required identity. The software results
  do not establish JA11 hardware acceptance, and this APK must not be repeated for the same test.

## Current Model D candidate — off-phone qualification

The following source-bound diagnostic APK is under qualification. Its artifact bytes, package,
version, signer, and embedded source SHA are verified. The exact APK cold-launched on the isolated
API 35 AVD and emitted matching `APP_BUILD_INFO`; all 64 applicable instrumentation tests passed
against the same production source snapshot's debug test variant. All eight checks pass on the
app-source commit. The docs/helper successor must also pass all eight checks on its exact live PR
head before the physical candidate is frozen. Candidate and rollback artifact pins were reverified
host-only; no Pixel or Android device was enumerated.

- Production source commit/tree: `1d19067c9150aae1b09e01fafb8af3647bf65f71` /
  `e108d58a6863e594a6dadbc0d2f4fb745583c5a3`.
- Diagnostic APK: `opra-eq-ja11diag-0.8.0-source-1d19067c.apk`.
- APK SHA-256: `23adf9f4955b056f110562362717c706767b7e3ecc50225c07439a3ff5c23711`.
- Package/version/code: `com.weekssa.opraeqforuapp.ja11diag`, `0.8.0-ja11diag` / `11`.
- Debug signer certificate SHA-256: `73aa7581c8dc7dcc8ccea7586771119a98f9a74d7d8cf23716e09c557c9f6b41`; APK v2 signature verified.
- Private APK and sidecar: `/private/tmp/ja11-v0.8.1-model-d-1d19067c/`.
- PR #80 is open and draft. All eight checks pass on source commit `1d19067c`; after the docs/helper
  successor is pushed, confirm all eight on that exact PR head. Checks on prior heads do not qualify
  the final PR state.

The helper below pins this candidate's source and APK hashes plus the verified J024 base APK as
the prior-installed and rollback artifact. Reverify those pins against the private sidecar before
any later phone window. Do not run the helper's `install` or `rollback` actions until the final
phone-window confirmation and all exact-candidate gates pass.

The private candidate sidecar `CANDIDATE.md` records the local APK location and exact provenance.
Emulator logs/screenshots and future phone evidence remain in private temporary directories; this
package appends no Pixel serials, private network identifiers, raw logs, or hardware-specific
personal data to the repository.

`start-logcat` runs as a foreground process so its lifetime is owned by the terminal session and its
full output is flushed into the private evidence directory. Keep that terminal session open during
the physical procedure and send Ctrl-C there when capture should stop; do not launch it as a detached
background job. The `verify` and `verify-ja11-session` actions extract `JA11_DIAG` events from this
retained local capture, rather than relying on Android's finite current log buffer. Start capture in
a fresh evidence directory before launching the candidate and keep its foreground ADB process
running through verification. The helper binds a private PID marker to the capture and confirms the
matching ADB `logcat -v threadtime` process for the selected Pixel serial is still alive; missing,
stopped, wrong-target, or replaced captures fail closed. The helper rechecks that binding after
event extraction and immediately before reporting verification success.

Run `test-phone-session.sh` for synthetic positive and fail-closed coverage of the retained-log
parser, serialless single-candidate acceptance, zero/multiple candidate rejection, stale sessions,
later ambiguity, missing/stopped capture, wrong ADB serial, missing build identity, and output
redaction. It uses fake ADB and never enumerates or contacts a device.

## Preparation tools

- `make-baseline-profile.py` converts the latest complete `SNAPSHOT_READ_COMPLETE` event for the exact candidate SHA into a temporary Equalizer APO profile and a value summary. It rejects missing fields, wrong source, unsupported types, out-of-range values, values outside JA11 native quantization, and existing output paths.
- `phone-session.sh` requires one explicit ADB serial, checks that it identifies Google Pixel 9 before package actions, verifies the frozen APK checksum, and operates only on `.ja11diag`. Candidate SHA, prior installed APK, rollback APK, and profile name are candidate-specific. Refresh and verify all of them against the final sidecar before any physical window; unknown installed builds or signatures are a stop. Updates use `adb install -r` to preserve app data. The `uninstall` action targets only `.ja11diag`; logs, pulled APKs, package dumps and screenshots stay in a caller-provided private local evidence directory.

Before any write, run `verify-ja11-session` after installing/launching the frozen candidate and
obtaining a complete read-only snapshot. It requires exact `APP_BUILD_INFO`, a same-process
permissioned JA11 descriptor event, exactly one candidate in both `USB_SESSION_OPENED` and
`RESTART_IDENTITY_AVAILABILITY`, and a complete current snapshot for the same source SHA, process,
and generation. A readable serial is optional; the helper reports only serial availability/source
categories and never prints the serial or fingerprint. It confirms the app process remains live and
rejects later detach, close, or ambiguous attach events. This is a current-session/cardinality gate,
not proof of same-unit continuity when serial is absent. If Mic is Off in the complete baseline, its
first permitted Off-to-On restoration transaction requires accepted write, expected detach, fresh
permission/open/claim, a sole returning JA11, a new generation, and authoritative Mic-On readback.
If Mic is already On, record restoration as satisfied and skip the write. Stop before mutation if
candidate/source, session, baseline, permission, or cardinality evidence is missing or stale. Repeat
the current-session/cardinality/readback checks after each expected restart.

Set `JA11_ADB_BIN`, `JA11_APKSIGNER_BIN`, `JA11_CANDIDATE_APK`, and `JA11_EVIDENCE_DIR` from the local candidate sidecar when the owner is participating. The exact path/serial are runtime values and do not belong in committed evidence. The script never chooses among devices automatically.

## Procedure

Follow [PHONE-PLAN.md](PHONE-PLAN.md) in order. It begins with current baseline and identity. If Mic
is Off, its first write restores Mic On and serves as corrected Test A. If Mic is already On, record
that restoration is satisfied and skip all microphone mutations. It continues with UAC, one Flash
from Off using the baseline-identical profile, full-power
volume/program truth, complete original-state readback and stop conditions. Do not repeat uncertain
writes or Flash.
