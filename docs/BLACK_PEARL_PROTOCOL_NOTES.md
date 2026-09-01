# TRN Black Pearl Flash protocol notes

Status: implementation evidence for v0.3 direct Flash. This document records observable protocol behavior only. It is not a copy of any reference implementation.

## Licensing boundary

EQ Library is Apache-2.0. The public Black Pearl reference projects reviewed during protocol research are GPL-3.0:

- `cheesyserg/BlackPearlControl-Android`
- `cheesyserg/pyBlackPearl`
- `DisYaBoiRalph/BlackPearlControl`

Their source code must not be copied into EQ Library. We may independently implement the externally observable USB/HID protocol behavior and standard biquad mathematics, with our own structure, naming, tests, and UX.

## Corroborated device identity and HID envelope

Reviewed reference implementations establish:

- USB vendor ID: `0x3302`
- USB product ID: `0x43E8`
- HID report ID: `0x4B`
- fixed report size: 64 bytes
- write operation byte: `0x01`
- read operation byte: `0x80`
- global playback gain command: `0x03`
- PEQ command: `0x09`
- temporary/latch command: `0x0A`
- flash/save command: `0x01`
- ten hardware PEQ bands
- an active-slot byte returned in PEQ reads and reused in subsequent PEQ writes

EQ Library reads the active slot from the connected DAC before Flash rather than inventing a slot value.

## Filter support

The Windows/Python reference exposes and writes distinct protocol type codes for:

- Peak: `0x02`
- Low shelf: `0x03`
- High shelf: `0x04`

The Android reference currently writes only peak filters, so its peak-only UI is not evidence that the hardware protocol itself is peak-only. The independently implemented EQ Library protocol codec preserves all three corroborated types while keeping final behavior gated by hardware validation.

Observed parameter ranges used by the references are approximately:

- frequency: 20 Hz to 20 kHz
- gain: -10 dB to +10 dB
- Q: 0.1 to 10

The frequency and Q ranges remain hard validation limits until evidence establishes a wider safe/working device range. The observed **-10 dB to +10 dB filter-gain range is treated differently**: it is the range currently validated/recommended by the reference behavior, but it is not the wire-format limit. The PEQ packet stores band gain as a signed little-endian 16-bit value in 1/256 dB units, so values such as `-11.9 dB` are protocol-encodable without clamping. For a finite protocol-encodable band gain outside ±10 dB, EQ Library preserves the exact value, allows file export, and allows confirmed Direct Flash only after an explicit caution that physical-hardware behavior outside the validated range still needs verification. Unsupported filter types, non-finite values, out-of-range frequency/Q, or gain values that cannot fit the signed protocol field remain hard failures.

The packet stores frequency as a little-endian 16-bit integer, Q/gain metadata in 1/256 units, and five normalized biquad coefficients as little-endian 32-bit floats. The coefficient sample rate used by both references is 48 kHz.

## Global playback gain

The Black Pearl global playback gain is observable through command `0x03`. The reviewed Android controller uses raw range `-9472..6440` and derives gain/headroom in 1/256 dB units. Its AutoEq importer uses a convenience percentage approximation for negative preamp; EQ Library does **not** copy that approximation.

EQ Library independently uses the protocol's raw 1/256 dB scale:

- read the current signed little-endian raw gain;
- convert the required source preamp / generated safety headroom directly to a raw delta at 256 units per dB;
- validate the resulting absolute raw gain against the corroborated hardware range;
- fail clearly rather than clamp when the requested adjustment cannot fit.

The global playback-gain range remains a hard representability limit. It is separate from the per-filter ±10 dB validated/caution range described above.

## Direct-Flash transaction

Approved direct Flash may adjust global playback gain only when required to faithfully apply the selected EQ's preamp/headroom. It still does not expose or alter unrelated Black Pearl controls.

Transaction behavior:

1. Read a PEQ band to obtain the current active-slot byte.
2. Read current global playback gain.
3. Remove/replace the previous gain delta tracked as applied by EQ Library, then calculate the new required gain. This prevents repeated Flash operations from cumulatively lowering volume.
4. If the target gain differs, write global playback gain using command `0x03`.
5. Write all ten PEQ bands for the active slot. If the preset uses fewer than ten bands, explicitly write zero-gain padding bands so stale hardware filters cannot remain active.
6. Send the temporary/latch command.
7. Send the flash/save command.

The confirmation dialog discloses the exact required playback-gain offset before this transaction begins. If a selected source-priority band is protocol-encodable but outside the currently validated ±10 dB filter-gain range, the same confirmation also identifies the affected band/value, states that it will be sent unchanged and not clamped, and requires the explicit **Flash anyway** action. Cancel performs no write.

Direct Flash must not send commands for:

- reconstruction/DAC filter
- gain mode
- amplifier topology
- balance
- microphone gain

## Preamp/headroom rule

The selected profile's source preamp is preferred when present. When the source omits preamp and EQ Library has calculated separate safety headroom, that derived value is used as the required playback-gain adjustment without rewriting the canonical source preamp.

A profile remains not flashable if it lacks both source preamp and generated safety headroom, has unsupported or truly unrepresentable filter data, or would require an absolute global gain outside the validated hardware range. A per-band gain outside ±10 dB alone is not classified as unrepresentable when the signed protocol field can encode the exact value; it is a caution pending hardware validation.

File export remains independent and preserves the effective playback preamp as a `Preamp:` line. Finite source band gains are likewise preserved exactly in Black Pearl import text rather than being rejected or clamped merely because they are outside the currently validated ±10 dB Flash range.

## Validation still required on physical hardware

Before direct Flash is considered release-ready, confirm on the user's TRN Black Pearl:

- Android USB permission/connection lifecycle on the primary Pixel 9 test device
- active-slot readback and reuse
- global-gain read/write and exact expected dB change
- repeated Flash replacement behavior (no cumulative attenuation)
- a 0 dB profile restoring a prior EQ Library-applied attenuation
- Peak, Low Shelf, and High Shelf type behavior
- ten-band overwrite and zero-gain padding
- a real protocol-encodable filter outside the previously validated ±10 dB range (current Edition XS test case: `-11.9 dB`) is presented with a caution, can be cancelled without a write, and after **Flash anyway** is accepted/applied by the physical DAC without silent clamping
- latch then flash persistence across reconnect/power cycle
- no change to DAC reconstruction filter, gain mode, amplifier topology, balance, microphone settings, or other unrelated controls
- graceful handling of detach/failure during a transaction

A successful protocol unit test is necessary but not sufficient for release readiness. Until the physical out-of-range test passes, EQ Library must describe those values as protocol-encodable but outside the currently validated hardware range, not as proven safe/working hardware behavior.
