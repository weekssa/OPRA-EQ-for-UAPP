# EW300 DSP reversible write qualification

Status: **COMPLETE — ALL VOLATILE GAIN/FREQUENCY/Q FIELD CHECKS PASSED 2026-09-17**

This checklist covers the owner-approved exact-device volatile field-qualification batch. It is not production support and does not qualify persistence, reset, global gain, slot selection, firmware, or any other device-management function.

## Fixed transaction

The signed diagnostic must:

1. require exact VID:PID `31B2:0111`, manufacturer `LE XIAN`, product `SIMGOT EW300 DSP`, HID interface 3 and interrupt endpoints `0x82`/`0x02`;
2. remain locked until the full preserved 12-register stock snapshot matches byte-for-byte;
3. re-read the complete 12-register snapshot immediately before any write and refuse the batch unless it matches exactly;
4. run only these eight small temporary data changes, one at a time: frequency and Q raw words for Bands 2–5; Band 1's corresponding words were already qualified in the preceding batch;
5. require the exact expected READ echo for every temporary value;
6. regardless of temporary readback success, send the exact captured four-byte restoration value for that register and require its exact READ echo before continuing;
7. stop the batch after the first failed temporary readback or failed restoration; do not retry that check or begin another;
8. complete one final full 12-register READ snapshot and require an exact match before reporting success;
9. release the HID interface and direct the owner to reconnect the cable.

There is no COMMIT `0x53`, CLEAR `0x43`, save, reset, slot, global-gain, firmware, bootloader, recovery, or retrying write loop.

## Owner procedure

- [x] Stop audio playback and keep the IEMs out of the owner's ears.
- [x] Complete the exact descriptor and complete-stock gates.
- [x] Run the eight remaining frequency/Q raw-word checks once, with first-failure stop behavior.
- [x] Verify each temporary readback and exact restoration.
- [x] Verify `Exact preserved final full snapshot match: true` and `RESTORED`.
- [x] Reconnect the cable after the run.

## Result

The owner report from 2026-09-17 shows every remaining check passed and the final 12-register
snapshot exactly matches the untouched capture. This completes volatile transport qualification for
the observed gain, frequency, and Q fields across all five bands. It does **not** approve a
production EQ feature, filter-type writes, persistence, reset, slots, or global-gain operations.

If the report shows **ATTENTION**, stop playback, do not repeat the test, leave the cable otherwise untouched, and send the complete report.

## Exact candidate

Source commit: `6fa4539c3e7a7c9cac058ad700f097979f91bca3`

Workflow run: `35245283713`

Artifact: `10507417685`
APK SHA-256: `70aa33db2e2b6b53797c9d9c5685cd6955874fdee52eac8801c97b2446af5e95`
