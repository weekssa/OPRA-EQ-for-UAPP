# JA11 v0.8.1 acceptance package

This package contains the separate-package candidate install guard and consolidated physical acceptance plan. The prior J028 session is closed and Test C remains incomplete because its transaction report was not captured.

## Current off-phone status

- Branch `codex/ja11-v0.8.1-minimal`, based on `9b9a1f610025b3d6233d4ff6f0c2309a668577cc`.
- Draft PR #81 is open/unmerged. Verify its live exact head and all required checks before physical work.
- Frozen production/JA11 transaction source SHA: `9493cf030acb440f92e547fc667f6a5399616045`.
- Diagnostic report implementation commit: `fc409b7d038632e67dccdeccbd5144f3d3b53bdd`; no `app/src/main` changes.
- Diagnostic APK SHA-256: `44040493c7263ae7d7e6f41be7389534d07977427cc8002494097fbf3c868848`.
- Package/version/versionCode: `com.weekssa.opraeqforuapp.ja11diag`, `0.8.1-ja11diag` / `12`.
- Signer certificate SHA-256: `73aa7581c8dc7dcc8ccea7586771119a98f9a74d7d8cf23716e09c557c9f6b41`.
- Local diagnostic gates: lint and assemble pass; 4/4 focused API 35 instrumentation tests pass. Independent review confirms the diagnostic-only scope and corrected Mic restoration procedure. Verify all required checks on the live exact PR head before phone use.
- Test C remains incomplete. J028 final readback matched baseline but Save count is unknown because the report was not captured.
- J028 physical results and final restoration remain valid for their recorded production source and previous exact APK. The new diagnostic APK has not been installed on Pixel or used with JA11.
- The owner-reported official FiiO app per-setting behavior is reference evidence only. Custom EQ is commit/reset; Mic is expected-reset; built-in program and volume are runtime controls without Save behavior unless protocol evidence says otherwise.
- The J028 phone window is closed. Do not repeat Mic, UAC, volume, power-cycle, or Flash tests until the exact candidate gates pass and a new Test C window is confirmed.

## Files

- `phone-session.sh`: requires one explicit ADB serial, verifies Pixel 9, checks the exact candidate hash/signer/version, queries only `.ja11diag`, preserves a verified prior APK before in-place update, and uses plain install only when absent. Ambiguous state fails before install.
- `test-phone-session.sh`: runs install/update/absence/failure fixtures with fake ADB; it never enumerates a real device.
- `PHONE-PLAN.md`: future Test-C-only steps, report checks, Mic restoration exception, exact stop rules, and immediate Pixel release. No phone/ADB action is authorized before the exact candidate gates and new window.

Keep APKs, sidecars, raw reports, screenshots, ADB captures, serials, fingerprints, and device-specific state in a private local evidence directory. Do not commit them.
