# TRN Black Pearl Flash protocol notes

Status: implementation and physical validation evidence for v0.3 Direct Flash plus the v0.4.0 **Reset EQ to flat** hardware qualification. v0.5 changes target derivation/file compatibility and therefore requires a focused Black Pearl regression on the new exact candidate. This document records observable protocol/import behavior only. It is not a copy of any reference implementation.

## Licensing boundary

EQ Library is Apache-2.0. The public Black Pearl reference projects reviewed during protocol research are GPL-3.0:

- `cheesyserg/BlackPearlControl-Android`
- `cheesyserg/pyBlackPearl`
- `DisYaBoiRalph/BlackPearlControl`

Their source code must not be copied into EQ Library. We may independently implement externally observable USB/HID/import behavior and standard biquad mathematics, with our own structure, naming, tests, and UX.

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

The Android reference currently writes only peak filters, so its peak-only UI is not evidence that the hardware protocol itself is peak-only. The independently implemented EQ Library protocol codec preserves all three corroborated types and the v0.3 hardware checkpoint passed the applicable native-type/active-slot behavior.

Observed parameter ranges used by the references are approximately:

- frequency: 20 Hz to 20 kHz
- gain: -10 dB to +10 dB
- Q: 0.1 to 10

The frequency and Q ranges remain hard validation limits until evidence establishes a wider safe/working device range. The observed **-10 dB to +10 dB filter-gain range is treated differently**: it is the generally validated/recommended reference range, but it is not the wire-format limit. The PEQ packet stores band gain as a signed little-endian 16-bit value in 1/256 dB units, so values such as `-11.9 dB` are protocol-encodable without clamping. For a finite protocol-encodable band gain outside ±10 dB, EQ Library preserves the exact value, allows file export, and allows confirmed Direct Flash only after an explicit caution. The Edition XS Altruistic-Farmer275 `13,500 Hz / -11.9 dB / Q 4.0` case passed physical Pixel 9 / TRN Black Pearl validation on 2026-08-31, including Cancel-without-write and **Flash anyway** without app-side clamping. That single successful point does not establish the entire wire-encodable range as hardware-validated, so the general outside-±10 caution remains. Unsupported filter types, non-finite values, out-of-range frequency/Q, or gain values that cannot fit the signed protocol field remain hard failures.

The packet stores frequency as a little-endian 16-bit integer, Q/gain metadata in 1/256 units, and five normalized biquad coefficients as little-endian 32-bit floats. The coefficient sample rate used by both references is 48 kHz.

## v0.5 target derivation and fidelity

Black Pearl v0.5 file export and Direct Flash consume the same shared ten-band device representation. Canonical source data remains unchanged.

- Source values already on the Black Pearl native grid with an exactly representable source-authored preamp may be **Exact**.
- A source that fits the same band structure but requires only Black Pearl native rounding remains structurally unchanged and is **Optimized · native hardware rounding only**; it is not unnecessarily curve-fitted.
- If the source cannot fit directly, the complete source response is deterministically fitted to at most ten target bands and measured after target quantization. Never silently take the first ten source bands for Black Pearl v0.5.
- If the source omits preamp, conservative headroom is derived from the final target response and the representation is **Optimized** even if every filter itself is natively exact. Generated headroom is derived target metadata, never source authorship.

This v0.5 derivation change invalidates the old software candidate for final hardware qualification and requires a focused Black Pearl regression on the new exact signed candidate.

## Global playback gain

The Black Pearl global playback gain is observable through command `0x03`. The reviewed Android controller uses raw range `-9472..6440` and derives gain/headroom in 1/256 dB units. Its AutoEq importer uses a convenience percentage approximation for negative preamp; EQ Library does **not** copy that approximation.

EQ Library independently uses the protocol's raw 1/256 dB scale:

- read the current signed little-endian raw gain;
- convert the required source preamp / generated safety headroom directly to a raw delta at 256 units per dB;
- validate the resulting absolute raw gain against the corroborated hardware range;
- fail clearly rather than clamp when the requested adjustment cannot fit.

The global playback-gain range remains a hard representability limit. It is separate from the per-filter ±10 dB validated/caution range described above.

## Direct-Flash transaction

Approved Direct Flash may adjust global playback gain only when required to faithfully apply the selected EQ's preamp/headroom. It still does not expose or alter unrelated Black Pearl controls.

Transaction behavior:

1. Read a PEQ band to obtain the current active-slot byte.
2. Read current global playback gain.
3. Remove/replace the previous gain delta tracked as applied by EQ Library, then calculate the new required gain. This prevents repeated Flash operations from cumulatively lowering volume.
4. If the target gain differs, write global playback gain using command `0x03`.
5. Write all ten PEQ bands for the active slot. If the preset uses fewer than ten bands, explicitly write zero-gain padding bands so stale hardware filters cannot remain active.
6. Send the temporary/latch command.
7. Send the flash/save command.

The confirmation dialog discloses the exact required playback-gain offset before this transaction begins. If a selected source/derived target band is protocol-encodable but outside the generally validated ±10 dB filter-gain range, the same confirmation identifies the affected band/value, states that it will be sent unchanged and not clamped, and requires the explicit **Flash anyway** action. Cancel performs no write.

Direct Flash must not send commands for:

- reconstruction/DAC filter
- gain mode
- amplifier topology
- balance
- microphone gain

## Black Pearl AutoEq file import contract

Black Pearl file export is independent from USB Flash but uses the **same derived filters and playback gain** as the Direct Flash plan.

The verified file-import compatibility target is `cheesyserg/pyBlackPearl`, which imports text files containing `Preamp:` plus up to ten AutoEq-style filters. Its parser recognizes shelf tokens as `LS` and `HS`; therefore the Black Pearl-specific EQ Library serializer uses:

- Peak → `PK`
- Low Shelf → `LS`
- High Shelf → `HS`

Example:

```text
Preamp: -6.00 dB
Filter 1: ON LS Fc 105 Hz Gain 4.00 dB Q 0.750
Filter 2: ON PK Fc 1000 Hz Gain -2.50 dB Q 1.250
Filter 3: ON HS Fc 8000 Hz Gain -1.50 dB Q 0.750
```

Do **not** reuse generic AutoEq/Equalizer APO `LSC` / `HSC` shelf tokens in the Black Pearl serializer: the reviewed pyBlackPearl importer does not identify those strings as its Low Shelf/High Shelf tokens.

The reviewed pyBlackPearl importer limits imported preamp to approximately `-16 dB..+6 dB`. EQ Library does not pre-clamp the exported value to that convenience range. If the real Black Pearl plan needs `-18 dB`, the file still contains `Preamp: -18.00 dB`, remains exportable/importable, and the UI/export metadata warns that pyBlackPearl will limit the imported preamp. Direct Flash remains independent and uses the actual hardware plan when it passes Black Pearl safety checks.

The reviewed `cheesyserg/BlackPearlControl-Android` importer currently hard-codes imported filter type to Peak. That is not a reason to distort EQ Library shelf output. Do not claim that one Black Pearl text file is acoustically preserved by every third-party controller.

## Reset EQ to flat — v0.4.0 behavior

When TRN Black Pearl is the active output and Direct Flash is enabled, My EQs exposes **Connected / Connect** and **Reset EQ to flat** as a compact side-by-side control row. Reset is enabled only while the DAC is connected. It is a device action, not a canonical/library EQ: it is not stored in EQ Library, My EQs, General EQs, Favorites, or exported as a preset.

Reset requires confirmation. The confirmation states that the current hardware EQ slot will be overwritten, any playback-gain adjustment previously applied by EQ Library will be removed, listening volume may change, and unrelated DAC settings will not be changed.

The fail-safe reset transaction is:

1. Read the current active EQ slot.
2. Read the current global playback gain and the prior EQ Library-applied gain delta.
3. Calculate the underlying baseline gain as `current gain - tracked EQ Library delta` and reject the reset before any write if that baseline would be outside the validated global-gain range.
4. Write all ten hardware bands as zero-gain flat bands for the active slot.
5. Latch and persist the flat EQ slot.
6. Only after the slot is confirmed written flat, restore the underlying baseline playback gain if it differs from the current gain.
7. Clear EQ Library's tracked applied-gain delta only after both the flat slot and the baseline gain are successfully established.

The ordering is intentionally different from ordinary profile Flash. Reset flattens and persists the PEQ slot **before** removing EQ Library attenuation so a mid-transfer USB failure cannot leave an old/partially reset boosted EQ playing at a newly increased volume. If PEQ transfer fails, playback gain and tracked delta remain unchanged. If the final gain-restore write fails after the slot is already flat, the tracked delta is retained so a retry can finish safely without guessing or double-restoring gain.

## Preamp/headroom rule

The selected profile's source preamp is preferred when present. When the source omits preamp, v0.5 derives Black Pearl-specific safety headroom from the final target response. That generated value is used as the required playback-gain adjustment without rewriting canonical source preamp/headroom metadata and makes the target representation Optimized.

A profile remains not flashable if target headroom cannot be derived safely, has unsupported or truly unrepresentable filter data, or would require an absolute global gain outside the validated hardware range. A per-band gain outside ±10 dB alone is not classified as unrepresentable when the signed protocol field can encode the exact value; it remains a caution unless that specific wider value/range has been physically validated.

File export remains independent and writes the actual target playback gain as a `Preamp:` line. Finite target band gains are likewise preserved rather than being rejected or clamped merely because they are outside the generally validated ±10 dB Flash range.

## Physical hardware validation result

The signed v0.3 candidate at `c70c523e1f530b8b197ebbccc41dfb4af1e27fc4` passed the Pixel 9 / TRN Black Pearl hands-on checkpoint on 2026-08-31. The reported pass covers the applicable checklist items for:

- Android USB permission/connection lifecycle on the primary Pixel 9 test device
- active-slot readback and reuse
- global-gain read/write and expected disclosed dB change
- repeated Flash replacement behavior without cumulative attenuation
- a 0 dB profile restoring the prior EQ Library-applied attenuation
- Peak, Low Shelf, and High Shelf behavior where exercised by the checklist
- the then-current v0.3 ten-band/first-10 handling and zero-gain padding behavior
- the Edition XS `-11.9 dB` test case showing the explicit caution, cancelling without a write, and then flashing without app-side clamping
- latch/save behavior exercised by the hands-on flow
- no observed change to unrelated DAC reconstruction filter, gain mode, amplifier topology, balance, microphone settings, or other unrelated controls
- normal graceful behavior across the tested connection/transaction flows

This pass qualified the v0.3 Direct Flash path. It does **not** qualify the changed v0.5 response-derivation algorithm or turn every possible protocol-encodable value outside ±10 dB into a generally validated hardware range. v0.5 therefore keeps the caution path and requires its focused regression.

The **Reset EQ to flat** implementation was qualified separately on 2026-09-06 using the exact signed candidate at `15f220bd055a2aec49c0cb97c16acbd43ac588da` on Pixel 9 / TRN Black Pearl. The candidate had already passed Android unit tests, lint, debug/release assembly, CodeQL, signed-beta alignment/signature verification, and the pinned release-signing certificate check. The focused hardware pass covered disconnected/connected control state, confirmation and Cancel, all-ten-band flattening in the current slot, persistence, restoration of the tracked EQ Library playback-gain adjustment, the no-tracked-gain case, preservation of a later independent user volume change, and unchanged unrelated DAC settings. Controlled mid-transfer failure injection was not exercised on hardware; automated domain tests cover PEQ-transfer and final gain-write failure ordering plus retry-safe state retention. This result qualifies the reset device behavior for v0.4.0, subject to the v0.5 derivation regression requirement when v0.5 is released.
