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

Signed source `77e64437953cbd38ceb21626ccd499d22216b021` repeated the non-forced claim (`false`), used Android's isolated forced-claim option on HID interface 3 (`true`), read all 74 declared bytes, and released the interface (`true`). The owner then reconnected the cable as instructed. No HID report, interrupt transfer, or vendor command was sent.

The exact report descriptor is:

`05 0C 09 01 A1 01 85 01 15 00 25 01 75 01 95 02 09 E9 09 EA 81 02 95 04 09 CD 09 CF 09 B6 09 B5 81 02 95 02 81 01 06 01 FF 85 4B 75 08 95 0A 09 01 81 03 95 0A 09 02 91 02 85 54 75 08 95 0A 09 03 81 03 95 0A 09 04 91 02 C0`

The first transcription of this evidence incorrectly recorded the second four-bit Consumer Control usage as `0xCE`. The owner's original capture and the later exact-gate result both show `0xCF`. Signed provisional source `fa1ed1a945a7ded5389fbaf50c939b8409bab449` therefore kept its READ button locked and sent no command. The corrected gate uses the captured `0xCF` byte and has an exact 74-byte regression test.

Evidence established directly by those bytes:

- Consumer Control report ID `0x01` carries the standard volume-up, volume-down, play/pause and scan controls in one byte plus padding.
- Vendor usage page `0xFF01` declares report ID `0x4B` with a 10-byte input payload and a 10-byte output payload.
- The same vendor page declares report ID `0x54` with a 10-byte input payload and a 10-byte output payload.
- The descriptor assigns vendor usages `0x01`/`0x02` to the `0x4B` input/output pair and `0x03`/`0x04` to the `0x54` pair. It does not define their semantics.

No output report is safe merely because its size is known. Signed source `30ef7b24e9c5c0945715e1f44e5ae2c60070a7fa` parsed the descriptor, issued only HID class `GET_REPORT` IN requests for the two exactly declared vendor input reports, and released the interface. The owner's Pixel 9 returned exactly `0 bytes` for both `0x4B` and `0x54`. It requested no feature report and sent no output data. This empty result is evidence and does not justify an output probe.

HID report IDs and sizes are exact. Protocol framing and semantics, firmware identity beyond the exposed strings/revision, and untouched EQ state remain pending. The official SIMGOT Control compatibility page currently lists EG280 and DEW0S wired DACs, not EW300 DSP; it is therefore not used as EW300 compatibility or protocol evidence. The current public DevicePEQ registry likewise has no `SIMGOT`, `EW300`, or `31B2` entry. These absences prohibit compatibility inference; they do not establish that the cable lacks configurable DSP.

Reference-only sources checked on 2026-09-16:

- [SIMGOT Control compatibility page](https://app.simgot.com/index.php/home/web/appDownload)
- [DevicePEQ USB-HID registry](https://github.com/jeromeof/devicePEQ/blob/master/devicePEQ/usbDeviceConfig.js) (0BSD; not adopted)
- [KTMicro tools](https://github.com/gxcreator/ktmicro-tools) (BSD-3-Clause; unrelated chipset implementation, not adopted)

## Current diagnostic boundary

Scan uses Android enumeration only. The separately requested capture first tries a non-forced host claim of the HID interface. Evidence shows Android refuses it on the owner's Pixel 9, so the follow-up may briefly detach Android's driver from interface 3 using the platform's forced-claim option. It performs standard IN GET_DESCRIPTOR (`bRequest=0x06`, report type `0x22`, interface recipient), parses only complete vendor input declarations, and uses HID class IN GET_REPORT (`bRequest=0x01`, input report type) for those exact IDs and sizes. The exact-device result is zero bytes for both declared vendor input reports. Signed source `823c30fefcc70c2a0e804bba6f7771a564b48613` additionally made exactly three 250 ms passive reads on the descriptor-declared interrupt-IN `0x82`, sending no data; all three returned `-1` (timeout/no incoming bytes). It released the interface successfully. Therefore this passive observation did not expose stock EQ or any vendor message. It sends no output report, interrupt-OUT transfer, vendor request, EQ write, save or reset. A report read is not yet a proven backup of untouched EQ; any returned fields require evidence before interpretation and the complete state still must be preserved before any write.

Protocol framing, filter count/types, limits, quantization, preamp, bypass, persistence, readback and reset semantics remain unresolved. Public leads in the approved implementation plan are unverified and no third-party protocol code has been adopted by this correction.

## Provisional public web-tool evidence

On 2026-09-16 the owner explicitly authorized a higher-risk, trial-and-error discovery strategy using public web information. The public Hangout.Audio Device PEQ assets fetched that day contain no EW300 product-name entry. Instead, `usbDeviceConfig.js` routes every USB vendor ID `0x31B2` device through a generic KT Micro fallback with five filters, report ID `0x4B`, a default first register `0x26`, and a two-times frequency compensation assumption. This is broad fallback behavior, not proof that every field is correct for EW300.

The paired `ktmicroUsbHidHandler.js` expresses a read request as a 10-byte report-`0x4B` payload `[register, 00, 00, 00, 52, 00, hint, 00, 00, 00]`, expects a response echoing the register and command `0x52`, reads the current slot at register `0x24`, filter register pairs beginning at `0x26`, and global gain at `0x66`. The same source separately defines WRITE `0x57`, COMMIT `0x53`, and CLEAR `0x43`; those commands remain excluded from the provisional capture.

Reproducibility hashes for the fetched public assets:

- `usbDeviceConfig.js`: `711510cdb9c5ea31d043273acda7d0a571f259b74e5113125fb4738b47c2c808`
- `ktmicroUsbHidHandler.js`: `546a46652767c527b203af30101405be1b74a0b1d04b60a8fd8b95920197205d`
- `usbHidConnector.js`: `0332353195b3f409ba54b727cefd5d1ad920bf6eaf0d61868fabba1b9e3c043f`

The next diagnostic is deliberately narrower than the web tool. It requires exact VID:PID, strings, interface, endpoints, and the already captured 74-byte HID descriptor. It sends only bounded command `0x52` reads and stops on the first missing or non-echoing response. It preserves every raw response and displays ambiguous frequency both raw and under the fallback's two-times interpretation. It sends no WRITE, COMMIT, CLEAR, save, reset, or firmware command. A successful response would establish framing on the owner's cable, but decoded meanings remain provisional until controlled physical evidence confirms them.

Public source locations:

- <https://eq.hangout.audio/shared/plugins/devicePEQ/usbDeviceConfig.js>
- <https://eq.hangout.audio/shared/plugins/devicePEQ/ktmicroUsbHidHandler.js>
- <https://eq.hangout.audio/shared/plugins/devicePEQ/usbHidConnector.js>

## Third-party browser connection observation

On 2026-09-16, the owner directly connected the exact cable to a macOS Chrome session and selected the device shown by the browser as `SIMGOT EW300 DSP`. This is an exact-device connection observation, not a compatibility inference from VID:PID or another product. The page then displayed a write-only action labelled `Save To SIMGOT EW300 DSP`, a local prompt asking whether to adjust earphones to flat, and a page warning that its own profile supports five PEQ filters. It offered no labelled pull, backup, read, or stock-state export action.

No flat adjustment, Save, reset, or EQ action was selected. The site was explicitly disconnected after inspection. The page's five-filter statement and local curve are third-party UI assertions only: neither is adopted as a cable capability, state readback, protocol definition, or evidence of persistence. Because this connection route provides no demonstrated stock-state pull, it cannot satisfy the required untouched-EQ preservation gate and does not authorize a write.

## Diagnostic build evidence

The `c008345` signed diagnostic omitted the launcher class and is rejected. See `V0.7_EW300_DSP_STATUS.md`. Android's primary documentation establishes the required Kotlin source-directory API: <https://developer.android.com/build/migrate-to-built-in-kotlin>. This is build-system evidence only.
