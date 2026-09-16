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
3. Tap **Request read-only descriptor capture**, approve Android USB permission, and send the full report screenshot. The current follow-up reads only the two input reports exactly declared by the captured descriptor, then listens for at most three short incoming messages on the one declared HID input endpoint. It sends no output report. It may briefly detach Android's media-button driver from HID interface 3 only. Reconnect the cable immediately after the report appears. Stop if the app reports an error.
4. Do not use EQ controls. This step does not read or preserve stock EQ; those operations need separately established protocol evidence.

Owner startup result for `c008345`: **FAIL**, app keeps stopping. Replacement source `6ec2ce2` passed the software launch gate and opened successfully on the owner's Pixel 9. Enumeration and standard raw descriptors were captured. The HID report descriptor read returned `-1`. Source `1786a0b` then established that the Pixel 9 refuses the non-forced HID-interface claim while Android's driver owns it; the diagnostic stopped before transfer. The next bounded follow-up may briefly isolate interface 3 solely for its standard descriptor read, always releases it, and requires a cable reconnect afterward. Successful emulator startup does not substitute for the Pixel 9 USB capture.

Owner result for signed source `77e6443`: **PASS** for the descriptor stage. The isolated claim succeeded, all 74 bytes were returned, and release succeeded. Exact vendor report IDs `0x4B` and `0x54` each declare 10-byte input and output payloads. Semantics and untouched EQ state are not yet known; no output report is authorized.

Owner result for signed source `30ef7b2`: **PASS** for the input-only capture stage. HID `GET_REPORT` for the exact declared input reports `0x4B` and `0x54` returned zero bytes. No output report, vendor request, EQ write, save, or reset was sent. Do not proceed to a write test until a separately captured, exact-device request/response sequence establishes the stock-state read and restoration path.

Owner result for signed source `823c30f`: **PASS** for the bounded passive-observation stage. Each of the three 250 ms interrupt-IN reads on `0x82` returned `-1`, meaning no incoming bytes were available in the allowed window. The interface was released. No output report, vendor request, EQ write, save, or reset was sent. The current safe discovery checklist is complete; do not add output probes or write tests without an authoritative exact-device stock-state read/restoration sequence.

Browser-session observation: the owner connected the exact cable to a macOS Chrome third-party Device PEQ page. The page identified it as `SIMGOT EW300 DSP`, but offered only Save and no labelled stock-state Pull, Read, Backup, or Export function. The connection was closed without selecting flat adjustment, Save, reset, or EQ controls. Treat the page's five-filter warning as unverified third-party UI, not a qualified cable limit.

The later hardware checklist remains the approved plan's Phase F checklist, gated on protocol evidence and a complete signed hardware candidate. No hardware PASS has been recorded.
