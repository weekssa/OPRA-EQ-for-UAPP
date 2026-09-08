# FiiO JA11 Direct Flash protocol notes

Status: **software implementation complete enough for CI; physical hardware validation pending**

These notes document only the observable run-mode behavior needed for EQ Library's approved Direct Flash feature. They are not firmware-update documentation and must not be expanded into a bootloader/firmware flasher without a separately approved product scope.

## Product boundary

EQ Library supports the **FiiO JA11 on normal FiiO firmware** as its own hardware output. The feature may:

- identify the exact approved USB device;
- read and write the five-band PEQ;
- represent source preamp / EQ Library safety headroom through the JA11 global EQ-gain control when it fits the validated capability profile;
- apply, read back, and persist the PEQ through the observed run-mode save command;
- reset the five PEQ bands and global EQ gain to flat / 0 dB.

It must not enter bootloader mode, update firmware, cross-flash another device, change reconstruction filters, or manage unrelated DAC controls.

## File interchange status

The v0.5 investigation did **not** establish a sufficiently verified external JA11 preset-file interchange format that EQ Library can safely generate and claim as importable. JA11 therefore remains a hardware-only Direct Flash output in v0.5.

This is not a limitation on Direct Flash. The existing USB Flash path remains intact. If a real repeatable JA11 file format is independently verified later, file export may be added **alongside** Direct Flash; do not replace Direct Flash and do not invent a `.txt`, JSON, binary, or other file in the meantime.

## Clean-room / licensing boundary

The Kotlin implementation in this repository is independently written. Public reference projects with copyleft licenses may be consulted only to corroborate observable protocol facts. Their implementation code, structure, comments, UI, and algorithms are not copied into this Apache-2.0 project.

The relevant protocol facts were also checked against FiiO's public product/support behavior: JA11 exposes five-band PEQ control from Android and saves EQ state on the dongle. Hardware behavior still requires our own Pixel 9 + JA11 qualification before the app claims validated support.

## USB identity

Current exact identity used by EQ Library:

- Vendor ID: `0x2972`
- Product ID: `0x0102`
- HID report ID: `0x02`
- PEQ bands: `5`

The Android transport matches the exact VID/PID and then dynamically locates a HID interface that has interrupt IN and OUT endpoints. It does not accept arbitrary KT02H20-family devices.

## Run-mode packet envelope

The report ID is sent as the first HID-report byte. The protocol packet follows it.

Set packet:

```text
AA 0A 00 00 <command> <length> <payload...> EE
```

Read/response packet:

```text
BB 0B 00 00 <command> <length/subcommand> <payload...> EE
```

WebHID-style implementations may expose the packet without the report-ID prefix while lower-level HID APIs may include it. The decoder accepts either response form and still validates the protocol header and command.

## Commands used by EQ Library

| Purpose | Command | EQ Library use |
| --- | ---: | --- |
| Filter parameters | `0x15` | Read/write PEQ band 0..4 |
| Global EQ gain | `0x17` | Read/write source preamp/headroom representation |
| Apply | `0x18` | Apply staged/current PEQ state |
| Save | `0x19` | Persist the applied PEQ state |

No other JA11 commands belong to the v0.5 Direct Flash surface.

## Filter payload

Observed filter write payload after command/length:

```text
<band index>
<gain signed x10, big-endian 16-bit>
<frequency Hz, big-endian unsigned 16-bit>
<Q x100, big-endian unsigned 16-bit>
<filter type>
00
```

Filter type mapping used by the JA11 run-mode protocol:

- `0` = Peak
- `1` = Low Shelf
- `2` = High Shelf

Current provisional capability profile pending hardware qualification:

- frequency: 20 Hz .. 20 kHz
- per-band gain: -24 dB .. +12 dB
- Q: 0.1 .. 10.0
- global EQ gain: -12 dB .. +12 dB

Source values are never silently clamped. If a full source EQ cannot be represented directly, the shared deterministic five-band response fitter may produce an Optimized representation only when its error thresholds pass. If the source fits the five-band structure but needs only native target rounding, keep that structure and report native rounding separately rather than unnecessarily fitting a different curve. Missing source preamp uses derived target headroom and is Optimized. The canonical source profile remains unchanged.

## Global EQ gain encoding

Global gain is a signed 16-bit value in `1/2560 dB` units, little-endian on the wire. EQ Library keeps this separate from the acoustic five-band fit.

## Flash transaction

The v0.5 implementation uses this fail-closed order:

1. Build and validate the complete five-band representation before touching USB.
2. Read current global EQ gain.
3. Read at least one PEQ band as a preflight communication check.
4. Write all five PEQ slots, explicitly padding unused slots with flat Peak filters so stale previous bands cannot remain active.
5. Write the required global EQ gain.
6. Send Apply.
7. Read back all five bands and global gain; stop on any mismatch.
8. Send Save.
9. Read back all five bands and global gain again; stop on any mismatch.

A failed preflight produces no writes. A failed Apply or verification must never be reported as success. Save is not attempted until the first readback passes.

The user-facing **Direct Flash** action therefore includes the observed Apply + Save sequence. The app may describe persistence as saved to the JA11 only after the required physical qualification passes; software command presence alone does not waive the hands-on gate.

## Reset to flat

Reset writes all five slots as 0 dB Peak filters, sets global EQ gain to 0 dB, sends Apply, verifies the result, sends Save, and verifies again. Other DAC settings are outside the transaction.

## Hardware qualification gate

See `FIIO_JA11_HANDS_ON_CHECKLIST.md`. Until that checklist records PASS on the signed/identified candidate APK and physical JA11, Settings and release notes must continue to say **Hardware validation pending**.
