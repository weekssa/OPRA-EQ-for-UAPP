# EW300 DSP reversible write qualification

Status: **PENDING OWNER PHYSICAL TEST**

This checklist covers only the first exact-device EW300 write framing test. It is not production support and does not qualify persistence, reset, global gain, slot selection, firmware, or any other device-management function.

## Fixed transaction

The signed diagnostic must:

1. require exact VID:PID `31B2:0111`, manufacturer `LE XIAN`, product `SIMGOT EW300 DSP`, HID interface 3 and interrupt endpoints `0x82`/`0x02`;
2. remain locked until the full preserved 12-register stock snapshot matches byte-for-byte;
3. re-read Band 1 register `0x26` and refuse to write unless it equals `26 00 00 00 52 00 F5 FF 64 00`;
4. send only temporary WRITE payload `26 00 00 00 57 00 F6 FF 64 00`;
5. read register `0x26` and require temporary response `26 00 00 00 52 00 F6 FF 64 00`;
6. regardless of temporary readback success, send the exact captured restoration WRITE payload `26 00 00 00 57 00 F5 FF 64 00`;
7. read register `0x26` and require exact restored response `26 00 00 00 52 00 F5 FF 64 00`;
8. release the HID interface and direct the owner to reconnect the cable.

There is no COMMIT `0x53`, CLEAR `0x43`, save, reset, slot, global-gain, firmware, bootloader, recovery, or retrying write loop.

## Owner procedure

- [ ] Stop audio playback.
- [ ] Install the exact signed candidate identified below.
- [ ] Connect the EW300 DSP cable and open the diagnostic.
- [ ] Complete **Request read-only descriptor capture**.
- [ ] Complete **Capture provisional stock-EQ snapshot** and confirm `Exact preserved stock snapshot match: true`.
- [ ] Tap **Run reversible +0.1 dB write test** once.
- [ ] Confirm the final report says `Exact preserved Band 1 restoration verified: true` and `RESTORED`.
- [ ] Reconnect the cable.
- [ ] Send a screenshot of the complete result.

If the report shows **ATTENTION**, stop playback, do not repeat the test, leave the cable otherwise untouched, and send the complete report.

## Exact candidate

Source commit, workflow run, APK checksum, signer verification, and automated-gate result remain pending the signed workflow.
