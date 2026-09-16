# EW300 DSP cable evidence ledger

Status: discovery only, 2026-09-16. The USB-C cable is the device; headphone identity is separate.

## Owner enumeration evidence

The owner's Pixel 9 screenshot from the first read-only diagnostic reports:

- VID:PID `31B2:0111`, device class/subclass/protocol `0/0/0`.
- Seven Android interface entries (these are enumeration indices, not proven unique USB interface numbers).
- Entry 0: `1/1/0`, no endpoints.
- Entries 1 and 3: `1/2/0`, no endpoints.
- Entry 2: `1/2/0`, isochronous IN `0x81`, maximum packet 96, interval 1.
- Entries 4 and 5: `1/2/0`, isochronous OUT `0x01`, maximum packet 384 and 576 respectively, interval 1.
- Entry 6: `3/0/0`, interrupt IN `0x82` and OUT `0x02`, maximum packet 16, interval 1.

Evidence category: OWNER SCREENSHOT. Shared VID/PID with another product is not compatibility evidence.

## Owner standard-descriptor evidence

The owner completed the standard descriptor capture on signed diagnostic source `6ec2ce2ada86f43fb3bd7ebd59dc283f8c4d9d11`. The screenshot reports:

- manufacturer string `LE XIAN`;
- product string `SIMGOT EW300 DSP`;
- serial string `2024-07-03-0000-0000-0000` (uniqueness is not established);
- device descriptor: USB 2.00, EP0 maximum packet 64, VID:PID `31B2:0111`, device revision `0101`, one configuration;
- configuration total length 303 bytes, four interface numbers, self-powered/remote-wakeup attributes `0xA0`, maximum power 100 mA;
- USB Audio Class 1.0 AudioControl plus streaming alternates for mono capture and stereo playback at 16/24 bits and advertised 44.1/48/96 kHz rates;
- HID interface number 3, subclass/protocol `0/0`, interrupt IN `0x82` and OUT `0x02`, 16-byte maximum packets, interval 1;
- HID 1.10 descriptor with one report descriptor declared as exactly 74 bytes (`09 21 10 01 21 01 22 4A 00`).

The first report-descriptor request returned `-1`. The interface was not claimed first, so this is a host-side read failure and reveals no report format. Signed source `1786a0bc4afc8b88bc141f1fd30d44638198e568` then attempted a non-forced claim of HID interface 3. The owner's Pixel 9 reported `non-forced claim: false`, proving Android's driver retained that interface; the diagnostic correctly stopped without a transfer.

The next diagnostic first repeats the non-forced claim. If Android still retains the interface, it uses Android's isolated forced-claim option on HID interface 3 only, reads exactly the 74 bytes declared by the standard HID descriptor, and releases the interface in a `finally` block before closing the connection. The owner is instructed to reconnect the cable afterward so Android resumes normal ownership. This remains a standard descriptor read and does not send a HID report, interrupt transfer, or vendor command.

Exact HID report format, protocol framing, firmware identity beyond the exposed strings/revision, and untouched EQ state remain pending.

## Current diagnostic boundary

Scan uses Android enumeration only. The separately requested descriptor capture first tries a non-forced host claim of the HID interface. Evidence shows Android refuses it on the owner's Pixel 9, so the follow-up may briefly detach Android's driver from interface 3 using the platform's forced-claim option. It then performs only standard IN GET_DESCRIPTOR (`bRequest=0x06`, report type `0x22`, interface recipient), releases the interface, and closes the connection. It sends no HID report, interrupt transfer, vendor request, EQ write, save or reset. A descriptor report is not a backup of untouched EQ; that state still must be read and preserved before any write.

Protocol framing, filter count/types, limits, quantization, preamp, bypass, persistence, readback and reset semantics remain unresolved. Public leads in the approved implementation plan are unverified and no third-party protocol code has been adopted by this correction.

## Diagnostic build evidence

The `c008345` signed diagnostic omitted the launcher class and is rejected. See `V0.7_EW300_DSP_STATUS.md`. Android's primary documentation establishes the required Kotlin source-directory API: <https://developer.android.com/build/migrate-to-built-in-kotlin>. This is build-system evidence only.
