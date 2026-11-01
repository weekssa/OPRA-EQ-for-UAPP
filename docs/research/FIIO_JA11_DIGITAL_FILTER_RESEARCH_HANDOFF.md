# FiiO JA11 digital filter research handoff

Status: **DEFERRED from v0.9.0.** Vendor-exposed digital-filter control is not available for JA11
firmware 2.20; the feature is not delivered and this document preserves the investigation for a future
version. Written 2026-11-01. A future agent should be able to resume from here without chat history.

## A. Project objective and scope

The intended feature was one **reconstruction-filter selector** in My DAC → DEVICE, directly below EQ
program, with five mutually exclusive device-native modes (FAST-LL, FAST-PC, Slow-LL, Slow-PC, NON-OS),
readback, runtime selection, truthful persistence semantics and restoration of the original value.

This is **not** the parametric EQ band type. PEQ bands use `0x15` with types PEAK/LOW_SHELF/HIGH_SHELF
(`0,1,2`). The reconstruction filter is a separate DAC oversampling/roll-off control.

Investigated hardware: FiiO / JadeAudio JA11, firmware 2.20, USB `2972:0101`/`2972:0102`. Deferred
because the official vendor implementation does not expose the control for this model/firmware and the
device returned no valid reply to the vendor command. Success in a future version requires a
device-specific capability declaration or a validated device reply, plus physical qualification.

## B. Hardware and firmware identity

- Model: JadeAudio JA11 (FiiO). Vendor device_type **109**, device model id 1.
- USB: VID `0x2972`, PID `0x0101` (UAC 1.0) / `0x0102` (UAC 2.0). HID report id `0x02`.
- Firmware tested: **2.20** (read via `0x0B`). UAC 2.0 active; sample rate read via `0x09`.
- Vendor database `ota_way = KT02H2X` and `command_builder_type = 61442`; firmware command `0x0B`.
- Related KT02H20-family devices share framing, not necessarily this capability. Do not transfer.

## C. Vendor application research (do not re-derive)

### Web app (`https://fiiocontrol.fiio.com/`)

- Main bundle `static/js/index-7hb30NUo.js`, SHA-256
  `4bc6204460a14434d9790fa1b016182fbf28bc2ec35d810f60abf091b9913d1e` (unchanged as of 2026-11-01).
- Audio Config route chunk `static/js/index-C70qb620.js`, SHA-256
  `c5f3d0bccce5154d745285b328cb5275122402dae003e7dea8f97a6b9a4d4f30`; 42 lazy chunks in total.
- Command builders: `Ye(170,10,cmd,[payload])` = `AA 0A`, `Ye(187,11,cmd,...)` = `BB 0B`, with sequence,
  length, payload, checksum and `0xEE`.
- Filter helpers `getFilterModeByCtrl`/`setFilterModeByCtrl` exist (read `BB 0B 04`, write `AA 0A 04
  [value]`, enum SD_SHARP 0, SHARP 1, SD_SLOW 2, SLOW 3, NO_OVERSAMPLING 4) but **no examined UI chunk
  calls them**. They are shared dead code in this bundle version.
- JA11 device config in the bundle: presets Vocal 0 / Classic 1 / Bass 2 / USER1 3 / closeEq 4, band
  types PEAK/LS/HS, gain -12..+12, device id 109, no filter flag.

### Android app (decisive)

- Official APK: `https://fiio-firmware.fiio.net/FiiOControl/FiiOControl%20V4.6.0.apk`,
  365,535,288 bytes, SHA-256 `ab0eb9cb4f6efd58a4e9b4edf3ad6ec11961b5e53d12fd6ecd1e80091730ac8d`.
  It is a Flutter app; **do not commit the APK, bundles or extracted vendor data**.
- Device database: `assets/flutter_assets/assets/database/sc_config_default.db` (770,048 bytes).
- Inspection steps: `unzip -p <apk> <db path> > sc.db`; `sqlite3 sc.db ".schema sc_func_info"`;
  `sqlite3 sc.db "select * from sc_product_info where device_type=109"`;
  `sqlite3 sc.db "select device_version, function_msg from sc_func_info where device_type=109"`.
- `sc_product_info` id 47: device_type 109, model 1, `JadeAudio JA11`, connect_mode 3 (HID),
  command_builder 61442, firmware command `0x0B`, `server_config_or_not = 1`, `ota_way = KT02H2X`.
- `sc_func_info` has **one** row for device 109: version **2.20** (exact match), 3,170-byte JSON.
- That JSON declares: stateItems sample rate, `mic_detect` 0x12, `uac_version_nui` 0x20, `vol_output`
  0x02; `devicePeqInfo` band 0x15, preset 0x16, gain 0x17, eqSwitch 0x1A (support false), eqResetCmd 0;
  settingItems firmware upgrade, hidden name/reset. `isSupportAudio=false`, `deviceCommandInterval=200`.
- **No filter entry and no `0x04`.** The schema enumerates controls with get/set commands and show flags,
  so absence is meaningful, not an optional field. The app parses `functionMsg` into `stateItems`,
  `settingItems` and `devicePeqInfo` (AOT strings), so the UI is data-driven from this record.
- Caveat: `server_config_or_not = 1`; no server-config endpoint or download path was found in the APK,
  so the bundled record is the shipped declaration, but a server-side update cannot be fully excluded.

## D. Protocol findings

| Item | Value |
|---|---|
| Vendor filter read | `BB 0B 04` |
| Vendor filter write | `AA 0A 04 [value]` |
| Modes | SD_SHARP 0, SHARP 1, SD_SLOW 2, SLOW 3, NO_OVERSAMPLING 4 |
| Older probe | `0x11`, values 1..5 (Savitech/CB5100 lineage, wrong for JA11 discovery) |
| Known-good read | e.g. `BB 0B 02` returns the volume byte at offset 6 |
| Framing | report id 2 + command block, length byte, footer `0xEE`; sequence/checksum bytes are tolerated as zero by this device for other commands |
| Transport | HID output report request, input report reply, interrupt endpoints, 700 ms deadline, 80 ms transfer timeout, queue drain |

`0x11` came from Ircama/ja11-config at pinned revision `affddff6c9808c33ce8b35b0b9759ff0d7f6e405` (header marks it a Savitech command adapted for JA11) and
Audiocular-Aura at pinned revision `2ea1896287448fc8d6170994cf32048185917b8d` (CB5100 `0x4B` flow). It is not the vendor JA11 command and returned no reply. The other pinned references: adithyasource/fiiocontrol-oss `f38994b3bd51bbc898cfceb5d182a403180df33e`, Cyfine/ja11-web-control `4d4eb83df6fcdf9e20b52e1bdf59a77f463b2c30`.
No filter write was ever attempted. No USB bus capture exists; J034/J035 recorded application-level
counts and UI readback only. A standard valid `0x04` reply would be accepted by our decoder (values
0..4); a non-standard frame would not.

## E. Research history (chronological, with negative results)

1. Initial filters investigation (`0x11`) completed three passes: source reconstruction, official and
   independent corroboration, and comparative tests. All five modes were deferred for insufficient
   exact-device evidence (see `docs/releases/v0.9.0/REFERENCE_EVIDENCE.md`).
2. J034 (2026-11-01) probed `0x11` on firmware 2.20: no valid reply, no write.
3. Vendor web bundle analysis found the `0x04` command family and mode enum; the app was re-implemented
   to probe `0x04`. J035 probed it: again no valid reply, no write.
4. All 42 web-app lazy chunks were fetched and searched: no UI caller for the filter helpers.
5. The official Android APK and its device database were inspected (lead-agent work): the JA11 2.20
   record has no filter control. This is the decisive evidence.
6. An independent adversarial review confirmed the web-chunk finding and the classification, and
   identified the Android app as the missing avenue (now covered).

## F. Evidence strength

- **Established:** the official vendor implementation does not expose digital-filter control for the
  tested JA11 firmware 2.20.
- **Supported inference:** the firmware likely does not implement the control interface.
- **Not established:** that the chipset cannot filter, that no hidden mechanism exists, or that a future
  firmware cannot expose the feature. The server-config caveat remains.

## G. Existing implementation and reusable assets

- `domain/kt02h20/FiioJa11Protocol.kt`: `DigitalFilterMode` enum, `readDigitalFilterModeReport`,
  `writeDigitalFilterModeReport`, `digitalFilterModeFromResponse`; unit-tested frames.
- `domain/fiio/FiioJa11DeviceControls.kt`: `DIGITAL_FILTER` id, descriptor, snapshot field, value mapping.
- `data/dac/FiioJa11ControlRepository.kt`: source interface methods, gated write path, snapshot field
  (probe only when `filterControlEnabled`).
- `data/kt02h20/AndroidFiioJa11UsbTransport.kt`: probe with raw diagnostics; `FiioJa11FilterProbeCache`.
- `data/kt02h20/Ja11FilterProbeDiagnostics.kt`: one-line request/response/session record (no user data;
  an unrelated queued HID report could in principle be logged).
- Tests: protocol frames, repository research-path tests (`filterControlEnabled = true`), diagnostics
  formatter, and the disabled-path rejection test.
- Build flag `JA11_FILTER_CONTROL_ENABLED` is **false**; no probe or write runs in normal workflows and
  the DEVICE row is removed. beta.4 (`65110098…`) added diagnostics; beta.5 is the release candidate.

## H. Reopening criteria and next steps

Reopen only with: a JA11 firmware whose vendor device record exposes a filter control; a first-party
capability statement; a validated device-specific `0x04` reply; or an independently verified exact-device
implementation. For each: obtain the vendor record or capture, add a protocol fixture, enable the flag
only in a research candidate, verify readback and supported values, then run a controlled save/power
cycle test with restoration.

Next-agent checklist: read this document, ledger J034/J035, `FIIO_JA11_PROTOCOL_NOTES.md` and
`CANDIDATE_B_INVESTIGATION.md`; do not repeat the `0x11` work or the vendor-bundle search; do not
install beta.4; require device evidence before any write; prefer a read-only probe first.

## I. Safety and acceptance for future work

Capability evidence must be device-specific. Writes require validated readback, correct value mapping,
verification, restoration of the original value, persistence and lifecycle evaluation, exact hardware
qualification, and no firmware modification or speculative probing.

## J. Final research status

**Deferred from v0.9.0 — vendor-exposed digital-filter control unavailable for JA11 firmware 2.20;
functionality not delivered; research preserved for a future version.**
