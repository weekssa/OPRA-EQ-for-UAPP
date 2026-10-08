# FiiO JA11 v0.8.1 acceptance package

This folder contains the exact phone-session procedure and small host tools for the one prepared Pixel/JA11 window. The phone gate remains unrequested until the frozen candidate, exact-head checks and fresh Mac ADB preflight are complete.

## Frozen diagnostic candidate

- Application source SHA: `a78808443c71d688e0f338e96495847569fe12f7`
- APK filename: `opra-eq-ja11diag-0.8.0-source-a7880844.apk`
- APK SHA-256: `7beb5bcebbc0dc40a68b33de911cc8722d76d3f0ff2e98685b1fa25e17caed61`
- Package: `com.weekssa.opraeqforuapp.ja11diag`
- versionName/versionCode: `0.8.0-ja11diag` / `11`
- Debug certificate SHA-256: `73aa7581c8dc7dcc8ccea7586771119a98f9a74d7d8cf23716e09c557c9f6b41`

This replacement candidate is locally validated and bound to the committed source above. Exact-head PR checks and a fresh host-only ADB preflight are still required before requesting another phone window. It has been installed/launched only on clean API 35 emulator `emulator-5560`; `APP_BUILD_INFO` reported the exact source SHA. The prior J019 physical candidate is superseded and must not be reinstalled.

The prior APK left on the Pixel by J019 has SHA-256 `85e06ca0db818586a7eb2eab3378a1b21949b3c8593e1318536ec651d8369305`, package `com.weekssa.opraeqforuapp.ja11diag`, version `0.8.0-ja11diag`/11, and the same debug certificate SHA-256. The install helper verifies this exact prior package before `adb install -r` of the replacement, preserving its app data.

The local candidate sidecar `CANDIDATE.md` records the local APK location and complete exact-head evidence. This package appends no Pixel serials, private network identifiers, raw logs, or hardware-specific personal data to the repository.

## Preparation tools

- `make-baseline-profile.py` converts the latest complete `SNAPSHOT_READ_COMPLETE` event for the exact candidate SHA into a temporary Equalizer APO profile and a value summary. It rejects missing fields, wrong source, unsupported types, out-of-range values, values outside JA11 native quantization, and existing output paths.
- `phone-session.sh` requires one explicit ADB serial, checks that it identifies Google Pixel 9 before package actions, verifies the frozen APK checksum, and operates only on `.ja11diag`. If the prior J019 diagnostic package is installed, it pulls and verifies the exact prior APK checksum and signer before updating in place with `adb install -r`, preserving app data. A different installed build or signature is a stop. The `uninstall` action targets only `.ja11diag`; log, pulled APK, package dump and screenshot output stays in a caller-provided private local evidence directory.

Before Test A or Test B, read a complete snapshot and check the diagnostic `RESTART_IDENTITY_AVAILABILITY` event. It records only a boolean and session generation. If `identityAvailable=true` and `sessionCurrent=true` are not both present for the current snapshot, stop before any restart-control write; that JA11 cannot be safely fenced across re-enumeration by this candidate.

Set `JA11_ADB_BIN`, `JA11_APKSIGNER_BIN`, `JA11_CANDIDATE_APK`, and `JA11_EVIDENCE_DIR` from the local candidate sidecar when the owner is participating. The exact path/serial are runtime values and do not belong in committed evidence. The script never chooses among devices automatically.

## Procedure

Follow [PHONE-PLAN.md](PHONE-PLAN.md) in order. It preserves a full baseline, tests mic and UAC restart/readback, executes one Flash from Off using the baseline-identical profile, checks full-power volume/program truth, restores the original state, and defines stop conditions. Do not repeat uncertain writes or Flash.
