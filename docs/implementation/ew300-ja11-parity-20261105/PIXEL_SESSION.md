# Pixel session plan — frozen EW300 parity APK

Status: waiting only on owner-provided wireless debugging access. Frozen APK: app/build/outputs/apk/ew300Parity/app-ew300Parity.apk; package com.weekssa.opraeqforuapp.ew300parity; 0.10.0-ew300parity / 22; SHA-256 20993f8b...; signer 94561420....

## Plan A — read-only (must pass first)

1. Verify the physical Pixel serial separately from emulator-5554/5580.
2. Verify the EW300 exact fingerprint 31B2:0111 / LE XIAN / SIMGOT EW300 DSP / interface 3.
3. Install and launch the frozen APK; verify package/version and on-device APK hash.
4. Play a known 48 kHz stream and read the app's UAC1 current-rate row; require 48000.
5. Read UAC1 status, microphone mute/volume, EQ, 0-60 volume, About; capture screenshots/reports.
6. No writes.

## Plan B — runtime writes, zero Save

1. Capture the complete live baseline (0x00/0x01/0x24/0x26-0x2F/0x66).
2. Apply one 0-60 volume step and one native shelf/Peak runtime target; verify readback.
3. Ask: "Please unplug the EW300 from the Pixel now. Wait five seconds. Plug it back in. Tell me when it is connected."
4. Verify the exact replacement session; read back; confirm runtime-only state did not persist.
5. Restore the original live state; verify exact bytes. No Save.

## Plan C — one Save plus restoration

1. Capture live and stored-startup baselines separately.
2. Apply one accepted target; verify volatile readback.
3. Send exactly one 0x53 Save; tolerate expected re-enumeration; verify final readback.
4. Ask for a genuine unplug/replug and, where required, power cycle; verify persistence.
5. Restore the captured stored baseline with one separate Save; verify exact restoration.
6. Never retry an uncertain Save.

## Plan D — owner shelf listening

Fixed targets: Low Shelf 250 Hz, -6.0 dB, Q 0.707, type 3; High Shelf 4,000 Hz, -6.0 dB, Q 0.707, type 4. Randomize flat-versus-filter, hide the order, use bass/mid/treble passages, 12 trials per shelf, at least 10 correct plus consistent direction/region. Stop on unexpected sound or failure.

## Plan E — fallback

Wireless ADB -> USB ADB -> Mac HID -> emulator. A failed path switches to the next, not to "blocked." The APK remains frozen and ready.

