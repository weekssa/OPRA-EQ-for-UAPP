# FiiO JA11 hands-on qualification

## 2026-09-25 owner-reported failed Flash record — DO NOT REPEAT YET

The owner supplied a screenshot showing a connected FiiO JA11 and a failed Flash verification
for a Jaytiss profile displayed as `Optimized · 9 → 5 bands · full-response fit`. The exact
message was `JA11 global EQ gain readback did not match the intended value.`

- Evidence category: **owner-reported physical UI artifact / negative result**.
- Screenshot: [Screenshot (Sep 25, 2026 9:59:19 AM)](/Users/stephenweeks/Library/CloudStorage/GoogleDrive-weekssa@gmail.com/My%20Drive/OPRA%20UAPP%20Presets/EQ%20Library%20Testing/Screenshot%20%28Sep%2025,%202026%209%3A59%3A19%20AM%29).
- Not established by the screenshot: APK/source/checksum/signer, firmware, UAC PID, intended gain, raw `0x17` write/readback bytes, whether Save was sent, and final device-state/restoration status.
- Supplemental owner report: unplug/replug returned the device to `0`; this has no attached raw readback or exact candidate provenance and is not sufficient to identify the persistence or codec behavior.
- The earlier supplied sequence adds two UI observations: connected My DAC displayed the optimized five-band target with `Global EQ gain -3.80 dB`, while the Jaytiss source plan carries `-3.90 dB`; a later connected view displayed flat bands and `0.00 dB`. This narrows the evidence to a volatile gain mismatch followed by persistence loss, but does not replace raw transaction evidence.
- Result: **FAILURE OBSERVED; NOT A QUALIFICATION RESULT**. Keep JA11 labeled Hardware validation pending.

Do not repeat Flash or Reset merely to obtain a green result. Before any future mutation, recover
the exact candidate provenance and capture a read-only baseline plus the intended gain, outgoing
`0x17` bytes, raw response bytes, decoded value, command timestamps, active program, all five
bands, firmware, PID/UAC mode, and whether the mismatch was pre-Save or post-Save. Stop if the
device state or original-state restoration cannot be established.

## 2026-09-25 exact-candidate repeat — diagnostic evidence required before another mutation

The owner subsequently installed the exact signed `EQ-Library-v0.7.0-beta-609911e.apk` over the
prior build. The owner reports that every EQ/Flash path failed immediately with the same global
EQ-gain readback mismatch, while the optimized graph appeared. The Save/reconnect correction was
exercised, but no visible progress, restart, disconnect/reconnect, Save, or successful EQ was
observed. The first supplied video includes an intentional Reset-to-EQ action; later unplug/reconnect
was only to show that the state did not persist. This is a physical negative result tied to the
exact signed candidate, not a qualification result.

- Video: `/Users/stephenweeks/Library/CloudStorage/GoogleDrive-weekssa@gmail.com/My Drive/OPRA UAPP Presets/EQ Library Testing/screen-20260925-130921-1790359660539.mp4`; SHA-256 `afa7bd92db069bec2d543d90d98fc4d4639810a0c062954607f4d73191b2d2d7`.
- Screenshot: `/Users/stephenweeks/Library/CloudStorage/GoogleDrive-weekssa@gmail.com/My Drive/OPRA UAPP Presets/EQ Library Testing/Screenshot (Sep 25, 2026 12:40:41 PM)`; SHA-256 `52ed5324861b2a51f154332258d9e4729ae8fc31677a0e2a49be3ea07653df3c`.
- Candidate source: `609911e2e51a254fc6f45b87fbdf4106c0049740`; APK SHA-256 `583ff7014fc3c0977b6679cd8bf56d3a4f615411a629fa6014bab088ece082ef`; signer certificate SHA-256 `65c1c1256dae3c49e3548f334c91f0ba991969e9be9e0b223ba4e253d2114747`.

Do not perform another Flash or Reset with this opaque candidate. The next and only authorized
physical step is one bounded diagnostic session using a newly built, exact signed candidate whose
JA11 report can be shared from My DAC. Before that session, record the complete five-band/global-
gain baseline, firmware response, VID/PID/UAC mode, and sanitized device identity. During one
controlled Flash, export the report containing the canonical preamp, optimized target, quantized
wire target, raw `0x17` write/read packets, decoded readback, comparison phase, timestamps/order,
Save count, session/detach generations, and final state. Stop immediately on an unknown baseline,
disconnect outside the documented Save boundary, missing raw report, or inability to restore the
original state. A report showing a mismatch remains a failure; do not retry automatically.

The draft PR now contains an evidence-backed Save/reconnect correction at source commit
`b32c52a82a46899efd115efd8544deeb16b9eb4c`. The corrected head has passed the repository's
automated software gates, but the CI debug APK is unsigned and no signed artifact, checksum, or
physical qualification claim exists for the correction yet.

## Historical integrated software candidate — do not use for the correction

The signed candidate below contains the earlier JA11 Flash pacing/readback recovery. Its source
is the merged main commit `f3765b03a3d8517880956390c9c4eca3b4157222`; Android CI, CodeQL, catalog,
priority-community, dependency, signed-beta, and emulator cold-launch gates passed on that exact
source. It predates the current Save/reconnect correction and is not a candidate for validating
the current investigation. This is software evidence only. Do not treat it as proof of physical
JA11 Flash or power-cycle persistence.

Status: **HISTORICAL SOFTWARE EVIDENCE — DO NOT FLASH JA11 WITH THIS ARTIFACT FOR THIS INVESTIGATION**

## Previous physical-test candidate — INVALIDATED BY J006 / DO NOT REPEAT

The previously corrected candidate was merged and had a complete trusted-main signed provenance
tuple. J006 now records that exact candidate failing on owner hardware. It is no longer a test
candidate for this investigation and must not be flashed again.

- Source commit: `609911e2e51a254fc6f45b87fbdf4106c0049740` (PR [#41](https://github.com/weekssa/OPRA-EQ-for-UAPP/pull/41), merged into `main` with owner approval).
- Exact immutable APK: [`EQ-Library-v0.7.0-beta-609911e.apk`](https://raw.githubusercontent.com/weekssa/OPRA-EQ-for-UAPP/mobile-test-apk/candidates/EQ-Library-v0.7.0-beta-609911e.apk).
- App/package: `0.7.0` / version code `7` / `com.weekssa.opraeqforuapp`.
- APK SHA-256: `583ff7014fc3c0977b6679cd8bf56d3a4f615411a629fa6014bab088ece082ef`.
- Signer: `CN=OPRA EQ for UAPP, O=weekssa`; certificate SHA-256 `65c1c1256dae3c49e3548f334c91f0ba991969e9be9e0b223ba4e253d2114747`; RSA 4096.
- Signing verification: APK Signature Scheme v2 and v3 verified; one signer; zipalign verified.
- R8 mapping SHA-256: `9f9d0e28271b4cfefdc9e71c6416061bd4a99e38341ab72290b159825de6e6ec`.
- Candidate manifest: target `ja11`; capability profile **FiiO JA11 exact model; five-band PEQ; global EQ gain**; test plan is this checklist.
- Signed-beta workflow: [run #1362](https://github.com/weekssa/OPRA-EQ-for-UAPP/actions/runs/36165218849) — **PASS**; 11m 6s; exact source `609911e`.
- Signed artifact: `EQ-Library-signed-beta-609911e2e51a254fc6f45b87fbdf4106c0049740`, artifact ID `10877122640`, artifact ZIP SHA-256 `1b124c63cb799a5a069384d48a9477e32078167a509ccd8872b69693880988cd`.
- Exact candidate APK checksum was independently recomputed from the immutable candidate and matches the embedded manifest and checksum file.
- Automated gates: Android CI [run 36163197805](https://github.com/weekssa/OPRA-EQ-for-UAPP/actions/runs/36163197805), CodeQL [run 36163197784](https://github.com/weekssa/OPRA-EQ-for-UAPP/actions/runs/36163197784), priority community coverage [run 36163197775](https://github.com/weekssa/OPRA-EQ-for-UAPP/actions/runs/36163197775), and catalog currentness [run 36163197794](https://github.com/weekssa/OPRA-EQ-for-UAPP/actions/runs/36163197794) — all **PASS**.
- Hardware status: **FAILED ON J006 — not a qualification result**. The agent performed no physical
  mutation; JA11 remains hardware-validation pending.

Do not use this source/APK/hash/signer tuple for another JA11 mutation. Do not use the moving
convenience APK, the historical `505182e` candidate, the historical `f3765b03` candidate, or any
APK with a different source/hash tuple.

## Current signed diagnostic candidate — READY FOR OWNER HARDWARE TESTING / FINAL RELEASE BLOCKED

The bounded diagnostic correction is merged on `main` at source
`af8c68c35d320223a13c635fac69c0f2ebacdb3f`. It adds a shareable readable/JSON transaction report,
raw JA11 request/response capture while the authoritative Flash operation is active, complete
five-band/program/global-gain baseline capture, phase-aware readback comparison, and exact value
joins. It intentionally does not change the `0x17` codec, signedness, endian order, `2560` scale,
tolerance, retry policy, or fail-closed behavior.

Use only this immutable candidate:

- APK: [`EQ-Library-v0.7.0-beta-af8c68c.apk`](https://raw.githubusercontent.com/weekssa/OPRA-EQ-for-UAPP/mobile-test-apk/candidates/EQ-Library-v0.7.0-beta-af8c68c.apk).
- Package/version: `com.weekssa.opraeqforuapp`, `0.7.0` / version code `7`.
- APK SHA-256: `3b442cbab3cf8be59a9b8e4ddd0d7028e93f8c4067d94dbb833a5bbb60b1d37e`.
- Signer: `CN=OPRA EQ for UAPP, O=weekssa`; RSA 4096; certificate SHA-256 `65c1c1256dae3c49e3548f334c91f0ba991969e9be9e0b223ba4e253d2114747`; v2/v3 verified.
- Signed-beta [run #1363](https://github.com/weekssa/OPRA-EQ-for-UAPP/actions/runs/36180975485): **PASS**; signed artifact ID `10884371877`; ZIP SHA-256 `77477ad6bd52e9b114cf18e949368424d8d5c1dbc85246679c9c5fd561d5b44d`.
- R8 mapping SHA-256: `71036cf05464e6b84f07165e75c17c5a5cd5517a843bd1a8efb0bb49add0b374`.
- Candidate manifest test plan: this checklist; capability profile: `FiiO JA11 exact model; five-band PEQ; global EQ gain`.

The signed workflow passed build, tests, lint, release/R8, signer verification, emulator
install/cold launch, diagnostics, and immutable candidate publication. This is a diagnostic
candidate, not a protocol-root-cause fix, final release, or JA11 support qualification. Do not
use the old `609911e` candidate or a moving convenience APK.

### One bounded owner session

1. Verify the downloaded APK against the SHA-256 and signer tuple above. Do not proceed if either
   differs.
2. Connect the exact JA11 and record firmware response, VID/PID/UAC mode, sanitized identity,
   active program, all five bands, global gain, and original-state restoration plan. This is a
   read-only baseline.
3. Select one known Jaytiss profile and perform one controlled Flash. Do not repeat Flash or Reset
   automatically, even if verification fails.
4. Export both the readable and JSON JA11 operation reports from My DAC. The report must contain
   the canonical preamp, optimized target, quantized wire target, raw `0x17` request/response,
   decoded readback, comparison phase, timestamps/order, Save count, session/detach generations,
   and final transaction state.
5. Verify the original state or the documented intended User 1 state after the operation and
   after one reconnect/power-cycle check only if the Save boundary is reached.

Stop immediately on a missing/unknown baseline, missing raw report, unexpected disconnect,
uncertain session replacement, or inability to establish restoration. The report’s result is
evidence even when it is a failure; do not retry to obtain a different result.

### Interpretation boundary

- Request and readback both show the expected `0x17` raw value, but the UI comparison is wrong:
  investigate the UI/domain comparison.
- Readback differs from the request: investigate device transformation, quantization, protocol,
  firmware, or timing; do not widen tolerance.
- A delayed prior response is associated with the current read: investigate response correlation.
- Pre-Save readback passes but post-Save readback fails: investigate Save/re-enumeration timing or
  persistence semantics.

Final release and public JA11 support remain blocked until the report-backed evidence is reviewed.

## Historical v0.5.0 software candidate record

- App version: `v0.5.0` candidate
- Candidate source commit SHA: `30535bd3b1bce9940d23e8735d88a4d9b6a9a4ef`
- Signed APK: `EQ-Library-v0.5.0-beta-30535bd.apk`
- Signed APK SHA-256: `5a2d4ff47097b1ba37b6bd625a4bfd3de444bf1895d4c0d0484fa2075adea042`
- Signer certificate SHA-256: `65c1c1256dae3c49e3548f334c91f0ba991969e9be9e0b223ba4e253d2114747` (matches repository pin)
- Signer: `CN=OPRA EQ for UAPP, O=weekssa`; RSA 4096; APK Signature Scheme v2/v3 verified; one signer
- Signed-beta workflow: **Signed EQ Library Beta Candidate** run #691, run ID `34295020653`
- GitHub Actions artifact: ID `10082967650`, `EQ-Library-signed-beta-30535bd3b1bce9940d23e8735d88a4d9b6a9a4ef`
- Artifact ZIP SHA-256: `c55d7b53e7b355e85a5b54b2b9a0da6925448c35563f85796605885ad6de91c3`
- Pixel 9 Android version/build: `TBD`
- JA11 firmware: `TBD` (current public FiiO release should be checked before qualification)
- Date: `TBD`
- Result: **PENDING — Hardware validation pending / deferred**

The record above documents the v0.5.0 software/signing state only. Before deferred physical qualification begins, replace or supplement it with the exact source SHA, signed APK/hash, signer verification, device/build/date, and workflow/artifact identity for the incremental release candidate actually being tested. Any Android/device/DSP behavior change always requires a new exact candidate.

## STOP conditions

Stop immediately and record the step if any of these occurs:

- the app identifies or writes a USB device other than the exact FiiO JA11;
- Android USB permission is bypassed, loops, or remains stuck after reconnect;
- tapping Cancel on a Flash/Reset confirmation causes any device write;
- a profile labeled Exact is materially changed;
- an Optimized profile exceeds the documented response-error gate or the app silently truncates to the first five source bands;
- source values are silently clamped instead of either being represented by the approved fit or rejected as Not suitable;
- fewer than all five JA11 slots are intentionally written, leaving stale prior filters active;
- Flash reports success before readback verification and persistent Save complete;
- unplug/reconnect loses a Flash that the app reported as successfully saved;
- Reset changes an unrelated DAC setting;
- playback/global EQ gain differs from the value disclosed by the confirmation/result;
- any firmware-update/bootloader behavior is invoked.

## 1. Output and Settings UX

1. Install/update the exact candidate without clearing app data.
2. Enable **FiiO JA11** under Settings → Outputs.
3. Confirm the output is labeled **Hardware validation pending** until this checklist is completed.
4. Confirm **Enable direct Flash** is OFF by default.
5. Enable Direct Flash and select FiiO JA11 as the active output.
6. Confirm EQ Library still shows canonical curves regardless of output; only output status changes to Exact / Optimized / Not suitable.
7. Confirm JA11 does not expose normal file Export actions or ask for an invented JA11 preset-file format.

## 2. USB discovery and lifecycle

1. Open My EQs with JA11 disconnected. Confirm Connect is available and Flash is disabled.
2. Tap Connect while disconnected; confirm a clear not-detected message.
3. Attach JA11 by USB and tap Connect.
4. Approve Android's USB permission prompt.
5. Confirm the UI changes to green **Connected**.
6. Disconnect the cable. Confirm state returns to disconnected and Flash becomes disabled.
7. Reconnect and connect again without restarting the app.
8. Fully close/reopen the app and repeat connection once.

**Pass:** permission/connect/disconnect/reconnect are stable and device-specific.

## 3. Confirmation is write gate

1. Choose an Exact-compatible saved EQ.
2. Tap Flash and inspect the confirmation.
3. Confirm it shows FiiO JA11, Exact/Optimized status, and the global EQ-gain change.
4. Tap Cancel.
5. Read the JA11 state with FiiO Control or another trusted readback method and confirm PEQ/gain did not change.
6. Repeat for Reset and Cancel; confirm no change.

## 4. Exact five-or-fewer-band Flash

Use a profile with 1–5 Peak/Low Shelf/High Shelf bands whose values are exactly representable.

1. Record the five existing JA11 bands and global EQ gain.
2. Flash the selected EQ.
3. Confirm success is reported only after readback and Save.
4. Compare FiiO Control/readback with the expected frequency, gain, Q, filter type, and global EQ gain.
5. Confirm unused slots are flat rather than retaining stale bands from the previous preset.
6. Confirm the UI/result says **Exact**.
7. Confirm unrelated JA11 settings remain unchanged.

## 5. Optimized greater-than-five-band Flash

Use a source with more than five bands that the software gate classifies Optimized.

1. Confirm EQ Library says **Optimized**, not Exact.
2. Confirm the confirmation explains that the curve was adapted to five-band PEQ.
3. Flash and verify five hardware bands are present.
4. Confirm this is not simply the first five source-priority bands; compare against the deterministic optimizer output from the candidate tests/logged fixture if needed.
5. Confirm the source/canonical EQ remains unchanged in EQ Library.
6. Confirm no Not-suitable profile exposes an enabled Flash action.

## 6. Filter types and edge values

Exercise at least one of each supported type:

- Peak
- Low Shelf
- High Shelf

Also exercise representative low/high frequencies and negative/positive gains within the current capability profile. Confirm readback matches the wire quantization and no silent clamping occurs.

## 7. Preamp / generated headroom

1. Flash a profile with a known negative source preamp.
2. Confirm the confirmation discloses that value and JA11 global EQ gain matches it after Flash.
3. Flash a different profile and confirm gain is replaced, not cumulatively stacked.
4. If a source lacks preamp but EQ Library has generated safety headroom, confirm that derived value is used while source metadata still shows no source preamp.
5. Confirm an unrepresentable gain request is rejected before writes.

## 8. Persistent Save

1. Flash a known preset and verify readback.
2. Unplug JA11 completely from USB/power.
3. Wait at least 10 seconds.
4. Reconnect it without opening EQ Library first.
5. Read the PEQ in FiiO Control or another trusted method.
6. Confirm all five bands and global EQ gain survived exactly as saved.

**Pass:** a reported successful JA11 Flash survives a full power cycle.

## 9. Reset EQ to flat

1. Start from a clearly non-flat saved JA11 EQ with nonzero global EQ gain when practical.
2. Tap Reset EQ to flat and Cancel once; verify no change.
3. Repeat and confirm.
4. Verify all five bands are flat 0 dB and global EQ gain is 0 dB.
5. Full power-cycle the JA11 and verify the flat state persists.
6. Confirm unrelated DAC settings did not change.

## 10. Failure/recovery smoke

Where safe and practical:

1. Disconnect during a transfer once.
2. Confirm the app reports failure rather than success.
3. Reconnect and retry a normal Flash.
4. Confirm the retry reaches a fully verified/saved state without stacking gain or retaining stale bands.

Mid-transfer cases that cannot be safely induced are acceptable only when their exact-head unit tests are green.

## Final gate

Record **PASS** only when all applicable steps above pass on the exact signed candidate for the incremental release being qualified and its Android CI/unit/lint/build gates are green. Then update this file with the device/build/date/result before removing **Hardware validation pending** for FiiO JA11.

Until that future PASS, JA11 remains explicitly implemented-but-unqualified; this pending state does not block v0.5.0 publication.
