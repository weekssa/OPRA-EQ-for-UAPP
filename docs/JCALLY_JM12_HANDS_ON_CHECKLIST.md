# JCALLY JM12 stock-firmware hands-on qualification

Status: **HARDWARE VALIDATION PENDING — DEFERRED TO NEXT INCREMENTAL RELEASE**

This physical gate is explicitly **not a v0.5.0 publication blocker** because the stock JM12 hardware is not yet available. Keep the shipped/in-app status **Hardware validation pending**, and do not claim power-cycle persistence. When the hardware arrives, run this checklist against the exact signed candidate for the next incremental release being qualified; do not automatically reuse the historical v0.5.0 candidate below.

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
- JM12 stock firmware/device version: `TBD`
- Date: `TBD`
- Result: **PENDING — Hardware validation pending / deferred**
- Power-cycle persistence result: `TBD`

The record above documents the v0.5.0 software/signing state only. Before deferred physical qualification begins, replace or supplement it with the exact source SHA, signed APK/hash, signer verification, device/build/date, and workflow/artifact identity for the incremental release candidate actually being tested. Any Android/device/DSP behavior change always requires a new exact candidate.

## STOP conditions

Stop immediately and record the step if any of these occurs:

- the app identifies/writes any device other than the exact stock-JM12 USB identity;
- any firmware update/cross-flash behavior is invoked;
- Cancel causes a device write;
- a partial PEQ write is followed by blindly re-enabling the old/partially written EQ;
- unrelated register bits or unrelated DSP/DAC controls change;
- the tracked playback-gain adjustment stacks on repeated Flash operations;
- Reset removes user gain beyond the delta previously applied by EQ Library;
- a profile labeled Exact is materially changed;
- an Optimized profile is just source-band truncation or exceeds the software response-error gate;
- the app claims persistence without the full-power-cycle test passing;
- a failed write/readback is reported as success.

## 1. Output and Settings UX

1. Enable **JCALLY JM12** in Settings → Outputs.
2. Confirm it remains labeled **Hardware validation pending**.
3. Confirm **Enable direct Flash** is OFF by default.
4. Enable Direct Flash and select JCALLY JM12 as active output.
5. Confirm EQ Library still shows all canonical curves; only output capability status changes.
6. Confirm status language is Exact / Optimized / Not suitable.
7. Confirm no normal file Export action or invented JM12 preset-file format is offered.
8. Confirm the UI explicitly says power-cycle persistence is still pending where relevant.

## 2. Strict USB discovery and lifecycle

1. Open My EQs disconnected; Connect is available and Flash is disabled.
2. Tap Connect disconnected and confirm a clear not-detected message.
3. Attach the stock JM12 and tap Connect.
4. Approve Android USB permission.
5. Confirm green Connected state.
6. Disconnect; confirm the state drops and Flash disables.
7. Reconnect without restarting and connect again.
8. If another KT02H20-family dongle is available with a different VID/PID, confirm it is not accepted as JM12.

## 3. Confirmation is write gate

1. Tap Flash for a suitable preset.
2. Confirm the dialog identifies JCALLY JM12, Exact/Optimized status, and the tracked playback-gain change.
3. Confirm the dialog does **not** promise persistent Save.
4. Tap Cancel and verify PEQ/gain are unchanged using a trusted register/readback tool if available.
5. Repeat Reset → Cancel and verify no change.

## 4. Exact five-or-fewer-band Flash

1. Record current DAC EQ enable state, five DAC EQ bands, and digital DAC gain.
2. Flash an exactly representable 1–5 band preset.
3. Confirm the app reports success only after register readback verification.
4. Verify all five slots: intended bands plus flat padding for unused slots.
5. Verify Peak/Low Shelf/High Shelf type codes, frequency, gain and Q match expected wire quantization.
6. Verify the DAC EQ is enabled after the successful complete transaction.
7. Verify unrelated register bits/settings are unchanged.

## 5. Optimized greater-than-five-band Flash

1. Use a source with >5 bands that the software classifies Optimized.
2. Confirm the UI says Optimized and describes five-band adaptation.
3. Flash and verify exactly five hardware bands.
4. Confirm the result is the deterministic full-response fit rather than simply the first five source bands.
5. Confirm the canonical source remains unchanged.
6. Confirm Not-suitable profiles keep Flash disabled.

## 6. Playback-gain tracking / no stacking

Use profiles with distinguishable required gain values.

1. Establish a user baseline digital DAC gain before EQ Library applies anything.
2. Flash preset A requiring a known negative gain delta.
3. Confirm the hardware gain changes by exactly that disclosed delta.
4. Flash preset A again. Confirm the delta does not stack.
5. Flash preset B with a different delta. Confirm EQ Library removes/replaces its previous tracked delta relative to the same user baseline.
6. Independently change user playback gain if the device permits doing so safely, then Flash again. Confirm the user's new baseline is preserved and only the EQ Library delta is replaced.
7. Confirm stereo/single-DAC layouts, if encountered, update only documented DAC-gain byte(s) and preserve unrelated bytes.

## 7. Safe partial-transfer behavior

Where practical and safe:

1. Disconnect during a band write after EQ has been bypassed.
2. Confirm failure is shown and the app does not report success.
3. Confirm the implementation does not knowingly re-enable a partially written EQ.
4. Reconnect and perform a complete Flash.
5. Verify all five bands and gain are correct after recovery.

If a physical mid-transfer disconnect cannot be induced safely, exact-head transaction/failure unit tests must be green.

## 8. Power-cycle persistence — decisive open question

This section determines what a future qualified release may claim publicly.

1. Flash a distinctive verified preset.
2. Confirm live readback matches.
3. Fully unplug the JM12 from USB/power.
4. Wait at least 10 seconds.
5. Reconnect without opening EQ Library first.
6. Read the DAC EQ bands and gain using a trusted stock-firmware tool/readback method.
7. Record one of these outcomes exactly:
   - **PERSISTS:** five-band PEQ and relevant gain survive the power cycle without another write.
   - **DOES NOT PERSIST:** device returns to prior/default state.
   - **INCONCLUSIVE:** reliable independent readback was not possible.

Do not change UI/release wording to “saved to device” unless this exact test yields PERSISTS reproducibly. If it does not persist, retain live-Direct-Flash wording and document that limitation.

## 9. Reset EQ to flat and baseline restoration

1. Begin with a non-flat EQ and an EQ Library tracked gain delta.
2. Reset → Cancel; confirm no write.
3. Reset → confirm.
4. Verify all five DAC EQ bands are flat.
5. Verify only EQ Library's tracked gain delta was removed and the user baseline was restored.
6. Verify the stored tracked delta is cleared only after hardware readback confirms baseline.
7. Verify DAC EQ ends enabled with flat bands and unrelated register bits remain unchanged.
8. Full power-cycle and record whether the flat state persists, using the same persistence classification as section 8.

## 10. Regression smoke

1. Switch back to UAPP and confirm existing UAPP selections/export remain unchanged.
2. Switch to Black Pearl if available and confirm its existing Connect/Flash/Reset path still behaves normally.
3. Switch to JA11 output if enabled and confirm the two hardware identities/settings do not leak into each other.

## Final gate

Record **PASS** only when all applicable live-write, safety, identity, gain-tracking, reset, and regression steps pass on the exact signed candidate for the incremental release being qualified and its Android CI/unit/lint/build gates are green.

Power-cycle persistence is a separately recorded fact. A JM12 hardware PASS does not by itself authorize claiming persistent Save unless section 8 specifically records **PERSISTS**. Until this future checklist reaches PASS, JCALLY JM12 remains **Hardware validation pending**; this pending state does not block v0.5.0 publication.
