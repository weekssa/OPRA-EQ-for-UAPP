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

The latest physical candidate, source `92c11fb0`, passed the read-only session gate; Mic Off-to-On
and UAC 2.0-to-1.0-to-2.0 were verified. USB permission was granted and did not time out. Its one
Flash from Off reached Apply and pre-Save verification, accepted exactly one Save, then detached
677 ms later during band 4 final readback. No Save was retried. A fresh session read a complete
snapshot matching the original state; Test C is a physical failure for that candidate, Test D was
not run, and the Pixel was released. Evidence remains private at
`/private/tmp/ja11-v0.8.1-acceptance-92c11fb0/owner-phone-session-20261009T183705Z-continued/`.

The current late Save/reconnect candidate is source
`3d7bc1d91e6c39327477d1341bde80e8a37bfbd4`, which inherits the correction from
`ad894129b5002fa33a2a46d47222772657a7a0e3`. Its local gates, exact APK provenance, API 35 cold
launch, 64/64 source-matched instrumentation, and production-source review pass. Its frozen APK
SHA-256 is `3b74672a587daeaaf8f562634c5dcecea073f7ad6e01df736f874ee448fc6261`. Current PR #80 head
`186c43b22490281ba2fbfceae52e7fa5d13b9c3b` does not contain this source, so exact-head checks are
pending. The helper and current procedure follow Model D; G3a syntax/fixtures pass, and independent helper/procedure review is complete with no remaining actionable findings.
Do not use the Pixel until all applicable exact-head checks pass,
then send `PHONE WINDOW READY — PIXEL + JA11 NEEDED` with the exact candidate and test plan and
wait for the owner's confirmation. The read-only gate requires exact app/source provenance, one
supported JA11 candidate, current permissioned claimed session/generation, and a complete baseline.
Serial is optional. A mismatch fails when both sessions provide a usable serial; without one, do not
claim the same physical unit. The owner previously selected Always allow, but the separate phone
window confirmation remains required for this next session.

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

## Previous User 1 ordering candidate — physical final-readback failure

Production source `92c11fb0e41ae11b118b2e7bb105234d6606dbdb` selects User 1 when necessary and
verifies the selection before band/gain writes. That candidate was used in the physical run above.
Its late detach happened after accepted Save, during final readback; it does not establish that this
User 1 ordering correction failed.

- Diagnostic APK: `opra-eq-ja11diag-0.8.0-source-92c11fb0.apk`.
- APK SHA-256: `ce3f417f20c275fd4d535cf5e70f658d8430af8fdd3f87ea950705fbfd574637`.
- Package/version/code: `com.weekssa.opraeqforuapp.ja11diag`, `0.8.0-ja11diag` / `11`.
- Debug signer certificate SHA-256: `73aa7581c8dc7dcc8ccea7586771119a98f9a74d7d8cf23716e09c557c9f6b41`; APK v2 signature verified.
- G2/G3/G3a local gates pass, including 861 JVM tests, lint, debug/release/diagnostic builds,
  Android-test compile/assembly, R8, and helper fixtures. Independent app/helper reviews found no
  actionable issue.
- The exact APK cold-launched on isolated API 35 AVD `ja11-v081-api35-clean-20261008`; runtime
  build info reported the exact source SHA. Source-matched debug instrumentation passed 64/64 with
  no failures or skips. No phone ADB target was queried.
- Exact-head PR #80 CI passed for an older source only; those results do not qualify this candidate.
- Private APK and emulator evidence: `/private/tmp/ja11-v0.8.1-order-92c11fb0/`.

## Current late Save reconnect candidate — exact-head CI pending

- Application source: `ad894129b5002fa33a2a46d47222772657a7a0e3`.
- Diagnostic APK: `/private/tmp/ja11-v0.8.1-save-reconnect-ad894129/ja11diag-ad894129.apk`.
- APK SHA-256: `c9df96657fac0167a254da4303aca2aff9481794c88dc91af33e755591f0b47d`.
- Package/version/code: `com.weekssa.opraeqforuapp.ja11diag`, `0.8.0-ja11diag` / `11`.
- Debug signer certificate SHA-256: `73aa7581c8dc7dcc8ccea7586771119a98f9a74d7d8cf23716e09c557c9f6b41`.
- Full local gates and the isolated API 35 cold launch passed. Source-matched instrumentation passed
  64/64 with zero failures or skips. Independent source review found no actionable retry or
  lifecycle defect. Exact PR-head CI remains pending.
- The transport holds the accepted Save under its operation token through a 1-second detach
  observation and 45-second reconnect deadline. A detach during final readback permits readback-only
  verification on one fresh supported replacement; it does not resend data writes, Apply, or Save.
- Physical gate: wait for all exact-head checks, install/verify this APK, complete read-only JA11
  identity/cardinality/complete-baseline verification, and only then proceed. If the baseline Mic
  state is Off, restore Mic On first; skip that write if already On.

Private provenance, emulator output, and acceptance results are indexed by
`/private/tmp/ja11-v0.8.1-save-reconnect-ad894129/CANDIDATE.md`.

## Previous Model D candidate — physical baseline only

Source `1d19067c9150aae1b09e01fafb8af3647bf65f71` and APK SHA-256
`23adf9f4955b056f110562362717c706767b7e3ecc50225c07439a3ff5c23711` were used in the prior
authorized physical session. That candidate's Mic restore passed, UAC restore was permission-timing
inconclusive, and Flash from Off failed before Save. It was rolled back and must not be retested.
All eight checks on its then-current head are historical and do not qualify source `92c11fb0`.

The helper below pins source `3d7bc1d91e6c39327477d1341bde80e8a37bfbd4` and its APK SHA plus the
verified prior-installed/rollback APK. Reverify all pins against the private sidecar before use.
After off-phone gates pass and the owner confirms the requested phone window, install and verify the
candidate, then complete the read-only Model D session/baseline gate. Do not send any hardware write
until that gate passes.

The private candidate sidecar `CANDIDATE.md` records the local APK location and exact provenance.
Emulator logs/screenshots and future phone evidence remain in private temporary directories; this
package appends no Pixel serials, private network identifiers, raw logs, or hardware-specific
personal data to the repository.

`start-logcat` runs as a foreground process so its lifetime is owned by the terminal session and its
full output is flushed into the private evidence directory. Keep that terminal session open during
the physical procedure and send Ctrl-C there when capture should stop; do not launch it as a detached
background job. The `verify`, `verify-ja11-session`, and `verify-ja11-reconnect` actions extract
`JA11_DIAG` events from this
retained local capture, rather than relying on Android's finite current log buffer. Start capture in
a fresh evidence directory before launching the candidate and keep its foreground ADB process
running through verification. The helper binds a private PID marker to the capture and confirms the
matching ADB `logcat -v threadtime` process for the selected Pixel serial is still alive; missing,
stopped, wrong-target, or replaced captures fail closed. The helper rechecks that binding after
event extraction and immediately before reporting verification success.

Run `test-phone-session.sh` for synthetic positive and fail-closed coverage of the retained-log
parser, Model D acceptance with serial unavailable or available from either reader, reconnect
acceptance with matching serials or one-sided/unavailable serials, mismatch and close-only rejection,
zero/multiple candidate rejection, stale sessions, later ambiguity, missing/stopped capture, wrong
ADB serial, missing build identity, and output redaction. It uses fake ADB and never enumerates or
contacts a device.

## Preparation tools

- `make-baseline-profile.py` converts the latest complete `SNAPSHOT_READ_COMPLETE` event for the exact candidate SHA into a temporary Equalizer APO profile and a value summary. It rejects missing fields, wrong source, unsupported types, out-of-range values, values outside JA11 native quantization, and existing output paths.
- `phone-session.sh` requires one explicit ADB serial, checks that it identifies Google Pixel 9 before package actions, verifies the frozen APK checksum, and operates only on `.ja11diag`. Candidate SHA, prior installed APK, rollback APK, and profile name are candidate-specific. Refresh and verify all of them against the final sidecar before any physical window; unknown installed builds or signatures are a stop. Updates use `adb install -r` to preserve app data. The `uninstall` action targets only `.ja11diag`; logs, pulled APKs, package dumps and screenshots stay in a caller-provided private local evidence directory.

Before the first write, run `verify-ja11-session` after installing/launching the frozen candidate
and obtaining a complete read-only snapshot. It requires exact `APP_BUILD_INFO`, a permissioned
descriptor for supported PID `0x0101` or `0x0102`, exactly one supported candidate, a current claimed
session/generation, and a complete snapshot for the same source SHA, process, and generation. Serial
status is reported only as a category and is optional. The helper never prints a serial or
fingerprint. Missing/stale session data, ambiguous selection, denied permission, or incomplete
baseline stops before mutation. After each expected restart, run `verify-ja11-session` for the fresh
current-session readback and inspect the operation-token-matched restart result; reject any reported
serial mismatch when both sessions had usable serials. A serialless result may prove requested state
on the sole returning JA11 but never the same physical unit. After Test D's planned physical unplug,
run `verify-ja11-reconnect` before reading or restoring volume/program; it requires generation-bound
detach-before-close evidence, one current supported candidate, and a complete current baseline. It
accepts `SERIAL_UNAVAILABLE` only when at least one session lacks a usable serial; a mismatch or
inconsistent status fails. If Mic is Off in the complete baseline, its first permitted Off-to-On
restoration transaction requires accepted write, expected detach, fresh permission/open/claim, a new
generation, and authoritative Mic-On readback. If Mic is already On, record restoration as
satisfied and skip the write.

Set `JA11_ADB_BIN`, `JA11_APKSIGNER_BIN`, `JA11_CANDIDATE_APK`, and `JA11_EVIDENCE_DIR` from the local candidate sidecar when the owner is participating. The exact path/serial are runtime values and do not belong in committed evidence. The script never chooses among devices automatically.

## Procedure

Follow [PHONE-PLAN.md](PHONE-PLAN.md) in order. It begins with current baseline and identity. If Mic
is Off, its first write restores Mic On and serves as corrected Test A. If Mic is already On, record
that restoration is satisfied and skip all microphone mutations. It continues with UAC, one Flash
from Off using the baseline-identical profile, full-power
volume/program truth, complete original-state readback and stop conditions. Do not repeat uncertain
writes or Flash.
