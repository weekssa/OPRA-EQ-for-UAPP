# TRN Black Pearl — v0.5 focused hardware regression

Status: **QUALIFIED / PASS**

The v0.5 Black Pearl hardware/DSP regression passed on the exact signed behavior candidate recorded below. The informational Reset-result wording issue found during that regression was corrected and the focused follow-up confirmation also passed. The later architecture refactor preserves device/DSP/conversion behavior; its exact signed candidate is pinned below for release provenance without reopening the completed Black Pearl hardware qualification.

This regression is required because v0.5 changes Black Pearl target derivation through the shared finite-target response adapter and changes the Black Pearl AutoEq file serializer. It does **not** replace the earlier v0.3 transport qualification or v0.4 Reset-to-flat qualification.

## Tested behavior candidate

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

### Informational wording issue found and resolved

The initially tested build reported a successful Reset using wording such as:

`Current Black Pearl EQ slot reset to flat · playback gain restored +3.90 dB`

The behavior was correct, but `restored +3.90 dB` can sound like a new positive boost and `previous level` would also be inaccurate after profile-to-profile Flash sequences.

The approved wording describes the actual state operation instead:

`Black Pearl EQ reset to flat · removed EQ Library's -3.90 dB playback-gain adjustment`

The implementation change is informational only: it negates the reported restoration delta for display so the message names the tracked EQ Library adjustment that was removed. It does not change Black Pearl gain math, USB writes, Flash, Reset ordering, persistence, or DSP derivation.

The project owner subsequently completed the focused post-copy Reset confirmation: Reset still succeeded and the success result described the removed EQ Library playback-gain adjustment with the correct sign/meaning.

Focused follow-up result: **PASS**

## 7. Outside-validated-range caution / Flash anyway

The known HIFIMAN Edition XS **Altruistic-Farmer275** case containing approximately `13,500 Hz / -11.9 dB / Q 4.0` was exercised.

- confirmation identified the out-of-validated-range value;
- the value was described as being sent unchanged rather than clamped;
- Cancel caused no write;
- `Flash anyway` completed successfully;
- no unrelated DAC-setting regression was reported.

Result: **PASS**

## Final signed candidate provenance

After the completed Black Pearl behavior qualification, the branch incorporated the MAD-style architecture refactor at candidate source commit `30535bd3b1bce9940d23e8735d88a4d9b6a9a4ef`. That commit explicitly preserves device/DSP/conversion behavior while moving orchestration, dependency-injection, SAF/platform, and lifecycle ownership boundaries.

The exact signed candidate for that source is pinned as follows:

- Candidate source commit SHA: `30535bd3b1bce9940d23e8735d88a4d9b6a9a4ef`
- Signed APK: `EQ-Library-v0.5.0-beta-30535bd.apk`
- Signed APK SHA-256: `5a2d4ff47097b1ba37b6bd625a4bfd3de444bf1895d4c0d0484fa2075adea042`
- Signer certificate SHA-256: `65c1c1256dae3c49e3548f334c91f0ba991969e9be9e0b223ba4e253d2114747`, matching `release-signing-cert.sha256`
- Signer: `CN=OPRA EQ for UAPP, O=weekssa`; RSA 4096; APK Signature Scheme v2/v3 verified; one signer
- Signed-beta workflow: **Signed EQ Library Beta Candidate** run #691, run ID `34295020653`
- GitHub Actions artifact: ID `10082967650`, `EQ-Library-signed-beta-30535bd3b1bce9940d23e8735d88a4d9b6a9a4ef`
- Artifact ZIP SHA-256: `c55d7b53e7b355e85a5b54b2b9a0da6925448c35563f85796605885ad6de91c3`
- Android CI run #1018: **PASS**
- CodeQL run #899: **PASS**
- Signed EQ Library Beta Candidate run #691: **PASS**
- Catalog currentness, priority community coverage, and automatic dependency submission for the candidate source: **PASS**

Documentation-only reconciliation commits after this candidate do not change the pinned APK. Any later Android/device/DSP behavior change would require a new exact candidate assessment; documentation-only release closeout does not by itself invalidate this completed Black Pearl qualification.

## Overall result

Black Pearl v0.5 **hardware/DSP qualification: PASS**.

The destructive/fidelity/persistence regression passed on signed behavior candidate `34a9cd819466cb301456c052eecadb02e6271e5e`, the focused Reset-result wording follow-up passed after the informational correction, and the behavior-preserving architecture candidate `30535bd3b1bce9940d23e8735d88a4d9b6a9a4ef` has a verified signed APK and green automated gates.

FiiO JA11 and stock JCALLY JM12 remain independent **Hardware validation pending** targets until their own pinned Pixel 9 checklists are run. Their pending status does not invalidate the Black Pearl result and must not be presented as qualified hardware support.
