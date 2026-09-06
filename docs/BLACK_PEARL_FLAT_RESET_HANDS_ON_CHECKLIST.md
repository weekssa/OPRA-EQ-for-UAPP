# TRN Black Pearl — Reset EQ to flat hands-on checklist

Status: required focused hardware validation for the post-v0.3 **Reset EQ to flat** device action.

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

- Candidate commit SHA: ________________________________
- APK/artifact name: ___________________________________
- Test date: ___________________________________________
- Tester: ______________________________________________

## 1. Disconnected / connected control state

1. Open **My EQs** with Black Pearl active and the DAC disconnected.
2. Verify the compact top control row shows **Connect** and **Reset EQ to flat** side by side.
3. Verify Reset is disabled while disconnected.
4. Connect the Black Pearl.
5. Verify the connection control becomes **Connected** and Reset becomes enabled.

PASS / FAIL: __________

Notes: ______________________________________________________________________

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

PASS / FAIL: __________

Notes: ______________________________________________________________________

## 3. Reset all 10 bands in the current slot

1. Put a known non-flat EQ in the current slot, preferably one using fewer than 10 source bands so stale-band clearing is meaningful.
2. Confirm the active slot on the DAC/controller before Reset.
3. Tap **Reset EQ to flat** and confirm **Reset to flat**.
4. Verify the app reports that the current Black Pearl EQ slot was reset to flat.
5. Inspect the same hardware slot with the Black Pearl controller/tool available to you.
6. Verify all 10 PEQ bands are flat/zero-gain and the active slot did not change.
7. Power-cycle or reconnect the DAC if practical and verify the flat slot persisted.

PASS / FAIL: __________

Notes: ______________________________________________________________________

## 4. Restore EQ Library-applied playback gain

1. Start from a conservative listening volume.
2. Flash an EQ through EQ Library that requires a clearly visible negative playback-gain adjustment, such as approximately -4 dB to -6 dB.
3. Confirm the app's Flash success message reports the expected gain adjustment.
4. Record the Black Pearl global playback gain after Flash: __________
5. Tap **Reset EQ to flat** and confirm.
6. Verify the EQ slot becomes flat.
7. Verify the global playback gain increases by the amount previously applied by EQ Library, returning to the underlying baseline rather than receiving a second/stacked change.
8. Verify the Reset success message reports the restored playback-gain amount.

PASS / FAIL: __________

Notes: ______________________________________________________________________

## 5. Reset when EQ Library has no tracked gain adjustment

1. Establish a flat or 0 dB-preamp EQ Library state where the tracked EQ Library gain adjustment is zero.
2. Record the Black Pearl global playback gain: __________
3. Put non-flat PEQ bands into the current slot without changing the global gain, or Flash a 0 dB-preamp EQ.
4. Run **Reset EQ to flat**.
5. Verify all 10 bands become flat.
6. Verify the global playback gain is unchanged.

PASS / FAIL: __________

Notes: ______________________________________________________________________

## 6. Independent user volume change is preserved

1. Flash an EQ through EQ Library that applies a known negative playback-gain adjustment.
2. After Flash, manually change the Black Pearl playback volume/gain by a small known amount using the normal DAC control.
3. Run **Reset EQ to flat**.
4. Verify the EQ Library-applied attenuation is removed while the user's later manual volume change remains reflected in the resulting baseline.
5. Verify there is no cumulative or double restoration.

PASS / FAIL: __________

Notes: ______________________________________________________________________

## 7. Unrelated DAC settings remain unchanged

Before Reset, record any conveniently observable unrelated settings such as reconstruction filter, gain mode, amplifier topology, balance, or microphone setting.

Run Reset and verify those unrelated settings remain unchanged.

PASS / FAIL: __________

Notes: ______________________________________________________________________

## 8. Connection-loss / retry smoke

Only perform failure injection that can be done safely without risking hearing or hardware.

1. With playback stopped and volume conservative, begin from a state that can be safely overwritten.
2. If practical, reproduce a connection loss during or immediately around Reset.
3. Verify the app reports failure rather than claiming success.
4. Reconnect and retry Reset.
5. Verify the final state is flat and the playback gain is restored exactly once, with no stacked volume change.

If a controlled mid-transfer disconnect is not practical/safe, mark this item **Not exercised on hardware**; automated domain tests cover PEQ-transfer and final gain-write failure ordering.

PASS / FAIL / NOT EXERCISED: __________

Notes: ______________________________________________________________________

## Final decision

Required for a hardware PASS:

- Sections 1–7 pass.
- Section 8 either passes or is explicitly marked Not exercised with the automated failure-ordering tests green.
- No unrelated DAC-setting regression is observed.
- The exact tested commit and signed APK are recorded above.

Overall: **PASS / FAIL**

Blocking observations: ________________________________________________________
