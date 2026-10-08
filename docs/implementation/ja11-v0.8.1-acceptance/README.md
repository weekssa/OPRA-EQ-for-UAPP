# FiiO JA11 v0.8.1 acceptance package

This folder contains the exact phone-session procedure and small host tools for the one prepared Pixel/JA11 window. The phone gate remains unrequested until the frozen candidate, exact-head checks and fresh Mac ADB preflight are complete.

## Frozen diagnostic candidate

- Application source SHA: `3f5e0c3a39687e27d962dd7f7f80d2667ff396ae`
- APK filename: `opra-eq-ja11diag-0.8.0-source-3f5e0c3a.apk`
- APK SHA-256: `85e06ca0db818586a7eb2eab3378a1b21949b3c8593e1318536ec651d8369305`
- Package: `com.weekssa.opraeqforuapp.ja11diag`
- versionName/versionCode: `0.8.0-ja11diag` / `11`
- Debug certificate SHA-256: `73aa7581c8dc7dcc8ccea7586771119a98f9a74d7d8cf23716e09c557c9f6b41`

The local candidate sidecar `CANDIDATE.md` records the local APK location and complete exact-head evidence. This package appends no Pixel serials, private network identifiers, raw logs, or hardware-specific personal data to the repository.

## Preparation tools

- `make-baseline-profile.py` converts the latest complete `SNAPSHOT_READ_COMPLETE` event for the exact candidate SHA into a temporary Equalizer APO profile and a value summary. It rejects missing fields, wrong source, unsupported types, out-of-range values, values outside JA11 native quantization, and existing output paths.
- `phone-session.sh` requires one explicit ADB serial, checks that it identifies Google Pixel 9 before package actions, verifies the frozen APK checksum and confirms the `.ja11diag` package is absent before install, and operates only on that package. The `uninstall` action targets only that package. Log and screenshot output stays in a caller-provided private local evidence directory.

Set `JA11_ADB_BIN`, `JA11_CANDIDATE_APK`, and `JA11_EVIDENCE_DIR` from the local candidate sidecar when the owner is participating. The exact path/serial are runtime values and do not belong in committed evidence. The script never chooses among devices automatically.

## Procedure

Follow [PHONE-PLAN.md](PHONE-PLAN.md) in order. It preserves a full baseline, tests mic and UAC restart/readback, executes one Flash from Off using the baseline-identical profile, checks full-power volume/program truth, restores the original state, and defines stop conditions. Do not repeat uncertain writes or Flash.
