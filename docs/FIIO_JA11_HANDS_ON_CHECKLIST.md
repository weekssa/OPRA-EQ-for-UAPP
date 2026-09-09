# FiiO JA11 hands-on qualification

Status: **HARDWARE VALIDATION PENDING — DEFERRED TO NEXT INCREMENTAL RELEASE**

This physical gate is explicitly **not a v0.5.0 publication blocker** because the JA11 hardware is not yet available. Keep the shipped/in-app status **Hardware validation pending**. When the hardware arrives, run this checklist against the exact signed candidate for the next incremental release being qualified; do not automatically reuse the historical v0.5.0 candidate below.

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
