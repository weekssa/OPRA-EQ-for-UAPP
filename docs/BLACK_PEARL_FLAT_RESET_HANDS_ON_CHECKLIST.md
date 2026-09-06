# TRN Black Pearl — Reset EQ to flat hands-on checklist

Status: **PASS** — focused hardware validation completed for the post-v0.3 **Reset EQ to flat** device action. This hardware-qualified feature is being promoted in v0.4.0.

Use this checklist only after the exact candidate commit has passed the required Android/unit/lint/build/security/signing gates. Record the exact commit SHA and signed APK artifact before testing. Do not merge or release the feature based only on automated tests.

## Test setup

- Device: Pixel 9
- DAC: TRN Black Pearl
- Install the signed candidate built from the exact PR head.
- Confirm TRN Black Pearl is the active output.
- Confirm **Enable direct Flash** is ON in Settings.
- Start at a conservative listening volume before any playback-gain test.
- Use an EQ slot whose contents may safely be overwritten.

Record:

- Candidate commit SHA: `15f220bd055a2aec49c0cb97c16acbd43ac588da`
- APK/artifact name: `EQ-Library-v0.3.0-beta-15f220b.apk`
- Signed APK SHA-256: `96d9ea12caf8c7944ecd059f7fdda533d1c936c5ed9583910a3d3ab01168c3cf`
- Test date: 2026-09-06
- Tester: Project owner

## 1. Disconnected / connected control state

1. Open **My EQs** with Black Pearl active and the DAC disconnected.
2. Verify the compact top control row shows **Connect** and **Reset EQ to flat** side by side.
3. Verify Reset is disabled while disconnected.
4. Connect the Black Pearl.
5. Verify the connection control becomes **Connected** and Reset becomes enabled.

PASS / FAIL: **PASS**

Notes: Hardware-qualified candidate behaved as designed.

## 2. Confirmation and Cancel perform no reset

1. Flash or otherwise select a clearly non-flat EQ in the current Black Pearl slot.
2. Tap **Reset EQ to flat**.
3. Verify the confirmation states that:
   - the current EQ slot will be overwritten;
   - all 10 bands will be reset to flat;
   - EQ Library's previously applied playback-gain adjustment will be removed;
   - listening volume may change;
   - unrelated DAC settings are not changed.
4. Tap **Cancel**.
5. Verify the existing EQ remains active and no reset is observed.

PASS / FAIL: **PASS**

Notes: Cancel performed no reset.

## 3. Reset all 10 bands in the current slot

1. Put a known non-flat EQ in the current slot, preferably one using fewer than 10 source bands so stale-band clearing is meaningful.
2. Confirm the active slot on the DAC/controller before Reset.
3. Tap **Reset EQ to flat** and confirm **Reset to flat**.
4. Verify the app reports that the current Black Pearl EQ slot was reset to flat.
5. Inspect the same hardware slot with the Black Pearl controller/tool available to you.
6. Verify all 10 PEQ bands are flat/zero-gain and the active slot did not change.
7. Power-cycle or reconnect the DAC if practical and verify the flat slot persisted.

PASS / FAIL: **PASS**

Notes: All ten bands reset flat in the current slot as expected.

## 4. Restore EQ Library-applied playback gain

1. Start from a conservative listening volume.
2. Flash an EQ through EQ Library that requires a clearly visible negative playback-gain adjustment, such as approximately -4 dB to -6 dB.
3. Confirm the app's Flash success message reports the expected gain adjustment.
4. Record the Black Pearl global playback gain after Flash if useful for troubleshooting.
5. Tap **Reset EQ to flat** and confirm.
6. Verify the EQ slot becomes flat.
7. Verify the global playback gain increases by the amount previously applied by EQ Library, returning to the underlying baseline rather than receiving a second/stacked change.
8. Verify the Reset success message reports the restored playback-gain amount.

PASS / FAIL: **PASS**

Notes: EQ Library-applied playback-gain attenuation was restored exactly once.

## 5. Reset when EQ Library has no tracked gain adjustment

1. Establish a flat or 0 dB-preamp EQ Library state where the tracked EQ Library gain adjustment is zero.
2. Record the Black Pearl global playback gain if useful for troubleshooting.
3. Put non-flat PEQ bands into the current slot without changing the global gain, or Flash a 0 dB-preamp EQ.
4. Run **Reset EQ to flat**.
5. Verify all 10 bands become flat.
6. Verify the global playback gain is unchanged.

PASS / FAIL: **PASS**

Notes: Reset flattened the bands without changing global playback gain.

## 6. Independent user volume change is preserved

1. Flash an EQ through EQ Library that applies a known negative playback-gain adjustment.
2. After Flash, manually change the Black Pearl playback volume/gain by a small known amount using the normal DAC control.
3. Run **Reset EQ to flat**.
4. Verify the EQ Library-applied attenuation is removed while the user's later manual volume change remains reflected in the resulting baseline.
5. Verify there is no cumulative or double restoration.

PASS / FAIL: **PASS**

Notes: Independent user volume change was preserved without cumulative restoration.

## 7. Unrelated DAC settings remain unchanged

Before Reset, record any conveniently observable unrelated settings such as reconstruction filter, gain mode, amplifier topology, balance, or microphone setting.

Run Reset and verify those unrelated settings remain unchanged.

PASS / FAIL: **PASS**

Notes: No unrelated DAC-setting regression was observed.

## 8. Connection-loss / retry smoke

Only perform failure injection that can be done safely without risking hearing or hardware.

1. With playback stopped and volume conservative, begin from a state that can be safely overwritten.
2. If practical, reproduce a connection loss during or immediately around Reset.
3. Verify the app reports failure rather than claiming success.
4. Reconnect and retry Reset.
5. Verify the final state is flat and the playback gain is restored exactly once, with no stacked volume change.

If a controlled mid-transfer disconnect is not practical/safe, mark this item **Not exercised on hardware**; automated domain tests cover PEQ-transfer and final gain-write failure ordering.

PASS / FAIL / NOT EXERCISED: **NOT EXERCISED ON HARDWARE**

Notes: Automated domain tests passed for PEQ-transfer failure, final gain-write failure, and retry-safe tracked-gain behavior. No unsafe hardware failure injection was required.

## Final decision

Required for a hardware PASS:

- Sections 1–7 pass.
- Section 8 either passes or is explicitly marked Not exercised with the automated failure-ordering tests green.
- No unrelated DAC-setting regression is observed.
- The exact tested commit and signed APK are recorded above.

Overall: **PASS**

Blocking observations: **None reported.**
