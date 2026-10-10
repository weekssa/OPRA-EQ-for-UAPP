# JA11 v0.8.1 acceptance package

This directory contains the separate-package candidate install guard and one consolidated physical acceptance plan. It is not permission to access the Pixel. The candidate is not frozen yet.

## Current off-phone status

- Clean branch: `codex/ja11-v0.8.1-minimal`
- Base: `9b9a1f610025b3d6233d4ff6f0c2309a668577cc`
- Exact production candidate, APK digest, package tuple, and signer: pending final gates
- Pixel/JA11 commands: none on this branch
- PR #80: preserved as open/draft historical evidence; no changes made

Do not run the helper against a real device until all local, emulator, independent-review, and exact-head CI gates pass, the final tuple is recorded, and the owner confirms the phone window.

## Files

- `phone-session.sh`: requires one explicit ADB serial, verifies Pixel 9, checks the exact candidate hash and signer, queries only the `.ja11diag` package, preserves and validates an exact prior APK before `adb install -r`, uses plain install only if the package is absent, and verifies the installed artifact. Ambiguous state, query failure, version/hash/signer mismatch, or invalid candidate fails before install.
- `test-phone-session.sh`: runs install/update/absence/failure fixtures with fake ADB and fake APK signer. It never enumerates a real device.
- `PHONE-PLAN.md`: gated single-session plan. It begins with exact candidate verification and a read-only full baseline. It requires expected-reset readback, one Flash action with one Save, conditional readback-only reconnect, exact original-state restoration, evidence capture, and prompt Pixel release.

Keep APKs, sidecars, raw reports, screenshots, ADB captures, serials, fingerprints, and device-specific state under a private local evidence directory. Do not commit them.
