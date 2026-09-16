# EW300 discovery and physical-test checklist

Phase B only. Full My DAC/Flash qualification remains blocked by missing protocol and untouched-EQ evidence.

## Before sending a diagnostic

- [ ] Exact source commit and download artifact recorded by candidate workflow.
- [ ] Unit tests, diagnostic lint and assembly pass.
- [ ] Launcher class exists in the actual APK DEX definitions.
- [ ] APK signature matches the pinned permanent certificate; alignment and checksum recorded.
- [ ] Exact signed APK opens twice on Android 36 and displays initial text and both controls.

## Focused owner capture

1. Install only the newly linked `.apk` and open the new diagnostic (application name `EW300 USB evidence`; launcher label `EW300 USB discovery`). An update uses the same package and permanent signer.
2. Confirm the screen remains open. Connect the untouched EW300 cable and tap **Scan connected USB devices**.
3. Tap **Request read-only descriptor capture**, approve Android USB permission, and send the full report screenshot. Stop if the app reports an error.
4. Do not use EQ controls. This step does not read or preserve stock EQ; those operations need separately established protocol evidence.

Owner startup result for `c008345`: **FAIL**, app keeps stopping. A replacement must pass the software launch gate before owner retry. Successful emulator startup does not substitute for the Pixel 9 USB capture.

The later hardware checklist remains the approved plan's Phase F checklist, gated on protocol evidence and a complete signed hardware candidate. No hardware PASS has been recorded.
