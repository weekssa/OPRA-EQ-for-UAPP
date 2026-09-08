# JCALLY JM12 stock-firmware Direct Flash protocol notes

Status: **software implementation complete enough for CI; physical hardware validation pending**

These notes cover only the observable **stock-firmware run-mode DSP register protocol** needed by EQ Library. They are intentionally separate from the FiiO JA11 protocol even though both products use KT02H20-family silicon.

## Product boundary

EQ Library supports the **JCALLY JM12 on stock firmware** as its own hardware output. The feature may:

- identify the exact approved stock-JM12 USB identity;
- handshake with the normal run-mode HID interface;
- read/write the DAC-side five-band PEQ register block;
- use the DAC digital-gain register as a tracked relative playback-gain adjustment when needed for source preamp/headroom;
- read back every register written;
- reset the DAC-side five-band PEQ to flat and remove only EQ Library's tracked gain delta.

It must not update firmware, enter bootloader mode, alter USB identity, cross-flash FiiO firmware, manipulate ADC EQ/DRC/reconstruction controls, or expose unrelated DAC settings.

## File interchange status

The v0.5 investigation did **not** establish a sufficiently verified stock-JM12 external preset-file interchange contract that EQ Library can safely generate and claim as importable. Stock JM12 therefore remains a hardware-only Direct Flash output in v0.5.

This does not remove Direct Flash. If a real repeatable stock-JM12 file format is independently verified later, file export may be added **alongside** Direct Flash. Do not invent a `.txt`, JSON, binary, or other preset format merely to make the device file-capable.

## Clean-room / licensing boundary

The Kotlin implementation is independently written from observable protocol facts. Copyleft/public reverse-engineering projects may be used to corroborate wire behavior, but their source implementation is not copied into this Apache-2.0 repository.

## USB identity

Current exact stock-firmware identity used by EQ Library:

- Vendor ID: `0x31B2`
- Product ID: `0x0111`
- HID report ID: `0x4B`
- report size: `11` bytes
- PEQ bands: `5`

The Android transport requires this exact VID/PID and dynamically locates a HID interface with interrupt IN and OUT endpoints. It does not match arbitrary KT02H20-family devices.

## Run-mode reports

### Handshake

```text
4B 00 00 00 00 43 00 00 00 00 00
```

A valid response echoes command `0x43`; observed accepted status values at response byte 7 are `0x03` or `0x4F`.

### Register read

```text
4B <address LE32> 52 00 00 00 00 00
```

The response must echo the report ID, exact requested address, and command `0x52`. The 32-bit register value is returned little-endian in bytes 7..10.

### Register write

```text
4B <address LE32> 57 00 <value LE32>
```

The response must echo report ID, exact address, and command `0x57`, with ACK value `0x00000003` in bytes 7..10.

EQ Library treats any mismatched address, command, short response, failed ACK, or failed readback as failure.

## Registers used by EQ Library

| Address | Purpose |
| ---: | --- |
| `0x01` | protocol/model flags; bit `0x0200` denotes the observed single-DAC layout |
| `0x24` | DAC EQ enable; bit 0 only |
| `0x26`..`0x2F` | five DAC EQ bands, two registers per band |
| `0x66` | DAC digital playback gain byte(s) |

No other DSP registers belong to the v0.5 transaction surface.

## PEQ band encoding

For band N, register A is `0x26 + N*2`, register B is A+1.

Register A:

```text
high16 = frequency in Hz
low16  = signed gain x10
```

Register B:

```text
bits 16..18 = filter type
low16       = Q x1000
```

Filter types used by EQ Library:

- `0` = Peak
- `3` = Low Shelf
- `4` = High Shelf

Current provisional stock-JM12 fitting profile pending physical qualification:

- frequency: 20 Hz .. 20 kHz
- per-band gain: -30 dB .. +30 dB
- Q: 0.1 .. 20.0
- playback-gain adjustment used by EQ Library: -24 dB .. +12 dB

Source values are never silently clamped in canonical data. If a source fits the five-band structure but needs only native stock-JM12 rounding, keep the source filter structure and report **Optimized · native hardware rounding only** rather than unnecessarily fitting a different curve. When direct representation is impossible, the shared deterministic five-band fitter may generate an Optimized complete-response approximation only if error thresholds pass. Missing source preamp uses generated target headroom and is Optimized. Canonical source data remains unchanged.

## Digital playback gain and tracked delta

Register `0x66` exposes signed gain byte(s) in 0.5 dB steps.

- Single-DAC layout: byte 0 is the DAC digital gain. EQ Library preserves byte 1 and all upper bytes unchanged.
- Stereo layout: bytes 0 and 1 are the left/right digital gains. Upper bytes remain unchanged.

This is not treated as a dedicated JA11-style EQ preamp. EQ Library stores only the relative gain delta it previously applied. On a later Flash it derives the user's baseline from `current gain - previous EQ Library delta`, then replaces the old delta with the new one. This prevents attenuation from stacking across repeated flashes and lets Reset restore the prior baseline.

## Flash transaction

The v0.5 implementation uses this order:

1. Build/validate the five-band representation before hardware writes.
2. Handshake.
3. Read protocol flags, current DAC-EQ enable register, current digital gain, and at least the first band pair as a preflight.
4. Derive the user baseline and target tracked playback gain; reject unsafe/unrepresentable targets before PEQ writes.
5. Disable DAC EQ by changing only bit 0 of register `0x24`.
6. Write all five bands, including explicit flat padding, with readback verification for every A/B register.
7. Write the tracked digital-gain target if needed and verify its relevant gain byte(s).
8. Re-read/verify all five bands and gain.
9. Re-enable DAC EQ by restoring bit 0 while preserving every other bit.
10. Return success only after final verification.

If a band/register transfer fails after EQ has been bypassed, the implementation deliberately leaves the EQ bypassed rather than re-enabling a partially written curve.

## Persistence status

**No independently corroborated stock-firmware run-mode “save EQ to flash” command has been established for this protocol.** The implementation therefore does not invent one and does not claim persistence merely because live register writes succeed.

The UI and result messages state that **power-cycle persistence is hardware-validation pending**. The hands-on checklist explicitly tests unplug/reconnect/full power-cycle behavior. If stock firmware does persist the registers automatically, that fact can be documented after a physical PASS. If it does not, v0.5 must continue to describe Direct Flash as a verified live write rather than persistent save.

The absence of an explicit Save command is independent from the absence of a verified external preset-file format. Neither uncertainty is a reason to remove the working Direct Flash implementation.

## Reset to flat

Reset:

1. handshakes and reads flags/EQ-enable/current gain;
2. derives the pre-EQ baseline from the stored EQ Library delta;
3. bypasses DAC EQ;
4. writes and verifies five flat Peak bands;
5. restores only EQ Library's tracked playback-gain delta back to baseline and verifies it;
6. clears the stored delta only after the hardware baseline is confirmed;
7. re-enables a flat DAC EQ and verifies it.

Other register bits/settings remain unchanged.

## Hardware qualification gate

See `JCALLY_JM12_HANDS_ON_CHECKLIST.md`. Until that checklist records PASS on the identified candidate APK and physical stock-firmware JM12, Settings and release notes must continue to say **Hardware validation pending**, and power-cycle persistence must remain an explicit open item.
