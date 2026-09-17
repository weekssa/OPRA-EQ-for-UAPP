# EW300 DSP reversible write qualification

Status: **INITIAL BAND-1 TEST PASSED; BATCH CANDIDATE PENDING SOFTWARE VALIDATION**

This checklist covers the owner-approved exact-device volatile field-qualification batch. It is not production support and does not qualify persistence, reset, global gain, slot selection, firmware, or any other device-management function.

## Fixed transaction

The signed diagnostic must:

1. require exact VID:PID `31B2:0111`, manufacturer `LE XIAN`, product `SIMGOT EW300 DSP`, HID interface 3 and interrupt endpoints `0x82`/`0x02`;
2. remain locked until the full preserved 12-register stock snapshot matches byte-for-byte;
3. re-read the complete 12-register snapshot immediately before any write and refuse the batch unless it matches exactly;
4. run only these small temporary data changes, one at a time: Band 1 gain, Band 1 frequency raw word, Band 1 Q raw word, and gains for Bands 2–5;
5. require the exact expected READ echo for every temporary value;
6. regardless of temporary readback success, send the exact captured four-byte restoration value for that register and require its exact READ echo before continuing;
7. stop the batch after the first failed temporary readback or failed restoration; do not retry that check or begin another;
8. complete one final full 12-register READ snapshot and require an exact match before reporting success;
9. release the HID interface and direct the owner to reconnect the cable.

There is no COMMIT `0x53`, CLEAR `0x43`, save, reset, slot, global-gain, firmware, bootloader, recovery, or retrying write loop.

## Owner procedure

- [ ] Stop audio playback.
- [ ] Install the exact signed candidate identified below.
- [ ] Connect the EW300 DSP cable and open the diagnostic.
- [ ] Complete **Request read-only descriptor capture**.
- [ ] Complete **Capture provisional stock-EQ snapshot** and confirm `Exact preserved stock snapshot match: true`.
- [ ] Keep the IEMs out of your ears and stop playback.
- [ ] Tap **Run approved reversible EQ-field batch** once.
- [ ] Confirm every temporary readback and exact restoration reports `true`.
- [ ] Confirm the final report says `Exact preserved final full snapshot match: true` and `RESTORED`.
- [ ] Reconnect the cable.
- [ ] Send a screenshot of the complete result.

If the report shows **ATTENTION**, stop playback, do not repeat the test, leave the cable otherwise untouched, and send the complete report.

## Exact candidate

Source commit, workflow run, APK checksum, signer verification, and automated-gate result remain pending the signed workflow.
