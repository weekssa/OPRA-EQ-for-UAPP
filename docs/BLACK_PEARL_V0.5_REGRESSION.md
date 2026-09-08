# TRN Black Pearl — v0.5 focused hardware regression

Status: **HARDWARE/DSP PASS** on the exact signed behavior candidate below. One informational Reset-result wording issue was found and corrected afterward; the final signed wording-only build needs one focused Reset-message confirmation before v0.5 release closeout.

This regression is required because v0.5 changes Black Pearl target derivation through the shared finite-target response adapter and changes the Black Pearl AutoEq file serializer. It does **not** replace the earlier v0.3 transport qualification or v0.4 Reset-to-flat qualification.

## Tested candidate

- App version: `v0.5.0` candidate
- Commit SHA: `34a9cd819466cb301456c052eecadb02e6271e5e`
- Signed APK: `EQ-Library-v0.5.0-beta-34a9cd8.apk`
- Signed APK SHA-256: `b03bf244473b8640011718c0918c7ee7b6a2d7f4aa1483817d72660c70f8a43f`
- Device: Pixel 9
- DAC: TRN Black Pearl
- Date: 2026-09-09
- Tester: Project owner

All required automated/software/signing gates for this behavior candidate were green before hands-on testing.

## 1. Connection and USB lifecycle

With Black Pearl active and Direct Flash enabled:

- disconnected state exposed Connect and kept Flash disabled;
- Android USB permission/connection completed normally;
- My EQs changed to the green Connected state.

Result: **PASS**

## 2. Confirmation is a no-write gate

A saved Black Pearl EQ was opened through Flash confirmation. The confirmation showed target/fidelity/adaptation information and playback-gain implications. Cancel was used before any write.

- no Flash-success result was shown;
- DAC EQ/playback state did not change;
- connection remained stable.

Result: **PASS**

## 3. Exact Flash

An EQ labeled **Exact · source values preserved** was flashed.

- Flash completed successfully;
- the app remained connected;
- no unexpected warning/error occurred;
- the exact source-preserving path behaved as expected.

Result: **PASS**

## 4. Optimized complete-response Flash

An EQ labeled **Optimized** using the v0.5 complete-response adaptation path was flashed.

- Flash completed successfully;
- Optimized status/reason remained consistent;
- no first-10 source-band behavior was claimed or observed;
- connection remained stable.

Result: **PASS**

## 5. Power-cycle persistence

The newly flashed Optimized EQ was left on the DAC, the Black Pearl was fully disconnected from USB/power for approximately ten seconds, then reconnected.

- the flashed EQ survived the full power cycle;
- EQ Library reconnected normally.

Result: **PASS**

## 6. Reset EQ to flat

Reset was exercised from a non-flat EQ state.

- Reset confirmation Cancel performed no write;
- confirmed Reset flattened the current Black Pearl EQ slot;
- EQ Library's tracked playback-gain adjustment was removed;
- unrelated DAC settings remained unchanged;
- the flat state survived a full power cycle.

Result: **PASS**

### Informational wording issue found

The tested build reported a successful Reset using wording such as:

`Current Black Pearl EQ slot reset to flat · playback gain restored +3.90 dB`

The behavior was correct, but `restored +3.90 dB` can sound like a new positive boost and `previous level` would also be inaccurate after profile-to-profile Flash sequences.

The approved wording describes the actual state operation instead:

`Black Pearl EQ reset to flat · removed EQ Library's -3.90 dB playback-gain adjustment`

The implementation change is informational only: it negates the reported restoration delta for display so the message names the tracked EQ Library adjustment that was removed. It does not change Black Pearl gain math, USB writes, Flash, Reset ordering, persistence, or DSP derivation.

## 7. Outside-validated-range caution / Flash anyway

The known HIFIMAN Edition XS **Altruistic-Farmer275** case containing approximately `13,500 Hz / -11.9 dB / Q 4.0` was exercised.

- confirmation identified the out-of-validated-range value;
- the value was described as being sent unchanged rather than clamped;
- Cancel caused no write;
- `Flash anyway` completed successfully;
- no unrelated DAC-setting regression was reported.

Result: **PASS**

## Overall result

Black Pearl v0.5 **hardware/DSP regression: PASS** on signed candidate `34a9cd819466cb301456c052eecadb02e6271e5e`.

No additional destructive Black Pearl hardware sequence is required merely because the Reset success copy changed afterward. For the final post-copy signed candidate, perform only a focused smoke check:

1. connect the Black Pearl;
2. Flash one previously qualified ordinary profile that creates a nonzero tracked EQ Library playback-gain adjustment;
3. Reset to flat;
4. confirm the success message describes the **removed EQ Library playback-gain adjustment** with the correct sign and amount;
5. confirm Reset still succeeds.

If that focused message check passes and the exact-head automated/signing gates are green, the v0.5 Black Pearl physical gate is complete.

FiiO JA11 and stock JCALLY JM12 remain independent **Hardware validation pending** targets until their own Pixel 9 checklists can be run. Their pending status does not invalidate the Black Pearl result.
