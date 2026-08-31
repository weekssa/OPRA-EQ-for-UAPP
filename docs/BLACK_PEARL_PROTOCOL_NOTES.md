# TRN Black Pearl EQ-only protocol notes

Status: implementation evidence for v0.3 direct Flash. This document records observable protocol behavior only. It is not a copy of any reference implementation.

## Licensing boundary

EQ Library is Apache-2.0. The public Black Pearl reference projects reviewed during protocol research are GPL-3.0:

- `cheesyserg/BlackPearlControl-Android`
- `cheesyserg/pyBlackPearl`
- `DisYaBoiRalph/BlackPearlControl`

Their source code must not be copied into EQ Library. We may independently implement the externally observable USB/HID protocol behavior and standard biquad mathematics, with our own structure, naming, tests, and UX.

## Corroborated device identity and HID envelope

The two independently reviewed reference implementations agree on:

- USB vendor ID: `0x3302`
- USB product ID: `0x43E8`
- HID report ID: `0x4B`
- fixed report size: 64 bytes
- write operation byte: `0x01`
- read operation byte: `0x80`
- PEQ command: `0x09`
- temporary/latch command: `0x0A`
- flash/save command: `0x01`
- ten hardware PEQ bands
- an active-slot byte returned in PEQ reads and reused in subsequent PEQ writes

EQ Library must read the active slot from the connected DAC before Flash rather than inventing a slot value.

## Filter support

The Windows/Python reference exposes and writes distinct protocol type codes for:

- Peak: `0x02`
- Low shelf: `0x03`
- High shelf: `0x04`

The Android reference currently writes only peak filters, so its peak-only UI is not evidence that the hardware protocol itself is peak-only. The independently implemented EQ Library protocol codec therefore preserves all three corroborated types while keeping the direct-Flash path gated by tests and hardware validation.

Observed parameter bounds used by both references are approximately:

- frequency: 20 Hz to 20 kHz
- gain: -10 dB to +10 dB
- Q: 0.1 to 10

The packet stores frequency as a little-endian 16-bit integer, and Q/gain metadata in 1/256 units. Five normalized biquad coefficients are written as little-endian 32-bit floats. The coefficient sample rate used by both references is 48 kHz.

## Safe EQ-only transaction

EQ Library's direct-Flash transaction is intentionally narrower than the reference control panels:

1. Read a PEQ band to obtain the DAC's current active-slot byte.
2. Write all ten PEQ bands for that slot. If the preset uses fewer than ten bands, explicitly write zero-gain padding bands so stale hardware filters cannot remain active.
3. Send the temporary/latch command.
4. Send the flash/save command.

The direct-Flash protocol layer must not send commands for:

- global/master volume
- reconstruction/DAC filter
- gain mode
- amplifier topology
- balance
- microphone gain

## Preamp/headroom rule

The reviewed reference applications implement imported negative preamp/headroom by changing the DAC's global/master volume. That is outside EQ Library's approved EQ-only scope.

Therefore direct Flash must **not** translate source preamp or generated safety headroom into a Black Pearl global-volume change. Until a separate PEQ-local preamp mechanism is independently demonstrated on hardware, a preset requiring independent attenuation must be reported as not faithfully flashable (or otherwise require an explicitly approved future optimization). File export may still preserve the preamp as metadata for external tools.

## Validation still required on physical hardware

Before direct Flash is considered release-ready, confirm on the user's TRN Black Pearl:

- Android USB permission/connection lifecycle on the primary Pixel 9 test device
- active-slot readback and reuse
- Peak, Low Shelf, and High Shelf type behavior
- ten-band overwrite and zero-gain padding
- latch then flash persistence across reconnect/power cycle
- no change to global volume or any non-EQ DAC setting
- graceful handling of detach/failure during a transaction

A successful protocol unit test is necessary but not sufficient for release readiness.
