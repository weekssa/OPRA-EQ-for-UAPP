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
- a serial string was present (its value is intentionally omitted from protocol notes and shareable reports);
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

### Owner stock-state capture

On exact signed source `503ffe2226a12e9c4d426360098763bae1f760c5`, the owner completed the bounded snapshot. All 12 requests were accepted as 11-byte interrupt-OUT reports and all 12 responses were 11-byte interrupt-IN reports that exactly echoed report ID `0x4B`, the requested register, and READ command `0x52`. No WRITE, COMMIT, CLEAR, save, reset, or firmware command was sent. The exact untouched payloads are now preserved as a byte-for-byte regression fixture:

```text
24 00 00 00 52 00 01 00 00 00
26 00 00 00 52 00 F5 FF 64 00
27 00 00 00 52 00 20 03 00 00
28 00 00 00 52 00 F7 FF C8 00
29 00 00 00 52 00 20 03 00 00
2A 00 00 00 52 00 FC FF 2C 01
2B 00 00 00 52 00 E8 03 00 00
2C 00 00 00 52 00 D0 FF 40 1F
2D 00 00 00 52 00 DC 05 00 00
2E 00 00 00 52 00 05 00 58 1B
2F 00 00 00 52 00 F4 01 00 00
66 00 00 00 52 00 F8 F8 00 00
```

The public decoder yields current slot `1`, five PK filters with gains `-1.1`, `-0.9`, `-0.4`, `-4.8`, and `+0.5` dB; Q values `0.8`, `0.8`, `1.0`, `1.5`, and `0.5`; raw frequency words `100`, `200`, `300`, `8000`, and `7000`; and global-gain byte `-8`. Under the fallback's still-unverified two-times frequency rule those frequencies are `200`, `400`, `600`, `16000`, and `14000` Hz. These decoded meanings remain provisional; the raw bytes are authoritative.

The original written fixture transcribed register `0x2E` as `FB FF` (`-0.5` dB). Repeated owner-device readback, including the fail-closed `stock-gate-byte-compare-db6cbf2` report, shows the untouched authoritative bytes are `05 00` (`+0.5` dB under the provisional decoder). The gate correctly stayed locked and sent no write while this discrepancy was resolved. The corrected fixture changes only those two bytes and remains exact-match gated.

This completes non-mutating stock-state preservation and establishes exact READ transport/framing. It does not yet prove WRITE framing, field acoustics, quantization boundaries, persistence, or reset semantics. The narrow proposed next test would make a very small temporary change to band 1, read it back, restore the exact four stock bytes immediately, and read the restoration back, without COMMIT/CLEAR/save/reset. That is the first write-capable boundary and requires explicit owner approval after this captured backup.

The owner explicitly approved that reversible write/readback/restore test on 2026-09-16. Its implementation remains gated on an exact fresh match to the preserved register `0x26` payload. It changes only the signed gain word from `F5 FF` (-1.1 dB under the public decoder) to `F6 FF` (-1.0 dB), leaving the frequency bytes `64 00` unchanged. It reads the temporary value, always attempts restoration with the exact captured `F5 FF 64 00`, and reads restoration back. It sends no COMMIT, CLEAR, save, reset, slot, global-gain, or firmware command. This is a qualification diagnostic, not production EW300 support.

### Owner reversible qualification result

On signed source `eb8f16fcb8f8e0c38ebfafe8ce2c1cbfb78bbeec`, the corrected complete stock gate matched and the owner completed the authorized test. The owner-visible transaction evidence is:

```text
baseline READ response:  4B 26 00 00 00 52 00 F5 FF 64 00
temporary WRITE payload: 4B 26 00 00 00 57 00 F6 FF 64 00
temporary READ response: 4B 26 00 00 00 52 00 F6 FF 64 00
restore WRITE payload:   4B 26 00 00 00 57 00 F5 FF 64 00
restore READ response:   4B 26 00 00 00 52 00 F5 FF 64 00
```

Each write received an initial non-READ `0x57` response, which the diagnostic ignored while waiting for the exact expected READ echo. The temporary readback and exact preserved restoration both verified `true`. This is direct evidence that this one bounded volatile write/readback/restore sequence works on the owner's cable. It does not establish persistence, a save/commit command, acoustic scaling, broader filter semantics, global-gain semantics, reset behavior, or production support.

After disconnecting and reconnecting the cable, the owner ran a separate read-only full snapshot. It again completed all 12 READ `0x52` requests and reported `Exact preserved stock snapshot match: true`. This confirms that the cable returned to the preserved stock bytes after the temporary transaction; it is not persistence evidence because neither the test nor this confirmation sent a save/commit command.

The owner completed the first consolidated volatile qualification session. All seven temporary values read back exactly, every baseline value restored exactly, and the final full 12-register snapshot matched exactly. This directly qualifies only the tested temporary write/readback/restore operations: Band 1 gain/frequency/Q and gains for Bands 2–5. It does not qualify acoustic scaling, persistence, filter type, slot, global gain, reset, or production support.

The owner then approved one final consolidated volatile session for the remaining per-band fields: frequency and Q raw words for Bands 2–5. The candidate is limited to eight one-step temporary data changes: `0x28 F7 FF C9 00`, `0x29 2A 03 00 00`, `0x2A FC FF 2D 01`, `0x2B F2 03 00 00`, `0x2C D0 FF 41 1F`, `0x2D E6 05 00 00`, `0x2E 05 00 59 1B`, and `0x2F FE 01 00 00`. Before any write it must reread the full 12-register snapshot exactly. Every check reads the baseline, writes one temporary four-byte field value, requires the exact READ echo, restores the captured four bytes, requires the exact restoration echo, and stops after the first failure. A final full exact snapshot is required before success. Filter-type bytes, slot, global gain, persistence, COMMIT, CLEAR, save, reset, and firmware controls remain absent.

### Owner final volatile field-qualification result

On 2026-09-17 the owner completed the eight remaining temporary frequency/Q checks for Bands 2–5.
Every temporary value returned the exact expected `0x52` READ echo, every original four-byte value
was restored and read back exactly, and the final 12-register snapshot matched the untouched
capture byte-for-byte. Combined with the earlier seven-check batch, this directly establishes the
volatile `0x57` WRITE → `0x52` READ echo → exact restore path for gain, frequency, and Q raw fields
in all five observed band register pairs on this exact cable.

This is transport qualification, not a claim that the provisional public decoder's frequency scale,
gain scale, Q scale, or filter type semantics are correct. Filter-type bytes, slot state, global gain,
persistence, COMMIT, CLEAR, save, reset, and firmware operations remain untested and prohibited.
The final report explicitly states that no COMMIT, CLEAR, save, reset, slot, global-gain, or firmware
command was sent.

Public source locations:

- <https://eq.hangout.audio/shared/plugins/devicePEQ/usbDeviceConfig.js>
- <https://eq.hangout.audio/shared/plugins/devicePEQ/ktmicroUsbHidHandler.js>
- <https://eq.hangout.audio/shared/plugins/devicePEQ/usbHidConnector.js>
- <https://github.com/gxcreator/ktmicro-tools/blob/master/KT_USB_PROTOCOL.md> (third-party KT02H20
  register-map reference; not treated as EW300 production proof)

### Owner filter-type qualification result

On 2026-09-17 the owner completed the consolidated reversible Band 1 filter-type test on the
exact cable. Codes `1`, `2`, `3`, and `4` were each written to the observed type byte at register
`0x27`, read back through the exact `0x52` response echo, and restored to the captured stock bytes
after the attempt. Every temporary readback and restoration verified, and the final 12-register
snapshot matched the untouched capture exactly. The public labels are retained only as a provisional
hypothesis: `1=LPF`, `2=HPF`, `3=low-shelf`, and `4=high-shelf`. This qualifies raw type-byte
transport and restoration on this cable; it does not prove acoustic behavior, frequency/gain/Q
units, persistence, save semantics, reset, slot, global gain, or production support.

The historical PR used `Ew300QualifiedVolatileProtocol`, `Ew300VolatileTransaction`, and
`Ew300ProvisionalDecoder` as diagnostic-only helpers. Those classes are intentionally not carried
into the recovery shipping source: their provisional register-map helpers could be mistaken for a
qualified product contract. The recovery branch keeps the raw fixtures and evidence ledger here,
while the production boundary is the smaller guarded `Ew300Protocol` plus the shared DAC session.
No diagnostic helper exposes persistence, reset, slot, global-gain, or arbitrary EQ-application
operations as a product capability.

## Third-party browser connection observation

On 2026-09-16, the owner directly connected the exact cable to a macOS Chrome session and selected the device shown by the browser as `SIMGOT EW300 DSP`. This is an exact-device connection observation, not a compatibility inference from VID:PID or another product. The page then displayed a write-only action labelled `Save To SIMGOT EW300 DSP`, a local prompt asking whether to adjust earphones to flat, and a page warning that its own profile supports five PEQ filters. It offered no labelled pull, backup, read, or stock-state export action.

No flat adjustment, Save, reset, or EQ action was selected. The site was explicitly disconnected after inspection. The page's five-filter statement and local curve are third-party UI assertions only: neither is adopted as a cable capability, state readback, protocol definition, or evidence of persistence. Because this connection route provides no demonstrated stock-state pull, it cannot satisfy the required untouched-EQ preservation gate and does not authorize a write.

## Diagnostic build evidence

The `c008345` signed diagnostic omitted the launcher class and is rejected. See `V0.7_EW300_DSP_STATUS.md`. Android's primary documentation establishes the required Kotlin source-directory API: <https://developer.android.com/build/migrate-to-built-in-kotlin>. This is build-system evidence only.

## Recovery research audit (2026-09-19)

The recovery audit rechecked the public leads before allowing any production
claim. The SIMGOT EW300 manual describes a USB-C cable with an integrated
DAC/DSP, but does not publish the EQ wire protocol, limits, persistence command,
or reset semantics. No official EW300 protocol specification or complete
manufacturer control API was found.

Two independent public references were inspected as hypotheses only:

- [gxcreator/ktmicro-tools](https://github.com/gxcreator/ktmicro-tools) documents
  a generic KT02H20 five-band PEQ tool and is BSD-3-Clause licensed. It is not an
  EW300-specific implementation and no code was copied.
- [jeromeof/devicePEQ](https://github.com/jeromeof/devicePEQ) contains a browser
  handler and generic KT Micro fallback. Its repository is 0BSD, but the broad
  `0x31B2` fallback is not exact-device proof and is not used to promote a
  capability.

Community reports of five PEQ filters and persistence are anecdotal and remain
non-authoritative. They do not close the global-gain, commit, power-cycle, or
Reset gates. The shipping recovery branch therefore keeps those operations
guarded and records the remaining facts in `V0.7_EW300_DSP_STATUS.md` rather
than starting another exploratory write loop.

### KT02H20-family cross-check (2026-09-19)

The recovery audit compared the exact EW300 evidence with three independent public
references. `gxcreator/ktmicro-tools` documents a generic KT02H20 five-band register
protocol with VID:PID `31B2:0111`, HID report `0x4B`, READ `0x52`, WRITE `0x57`,
register `0x24` EQ enable, band pairs `0x26..0x2F`, global gain `0x66`, and filter
codes for Peak/LPF/HPF/low-shelf/high-shelf. The exact EW300 captures match this
shape closely. `jeromeof/devicePEQ` independently implements a generic KT Micro
browser handler and is useful corroboration. `Ircama/ja11-config` documents another
KT02H20-family product, including five-band shelf filters and a distinct FiiO report
protocol with separate Apply and Save operations.

These sources establish a strong **KT02H20-family-compatible hypothesis**, not
manufacturer confirmation of the EW300's silicon or permission to copy a family-wide
command set. The repository uses independent behavior reconstruction, records the
BSD-3-Clause/0BSD/EUPL-1.2 license boundaries, and keeps exact EW300 identity and
firmware capabilities separate from FiiO/JCALLY protocol dialects. The public protocol
notes do not establish EW300 `0x53` as a safe persistent-save command; production Flash
and Reset therefore remain disabled until an exact-device capability plan proves
command ordering and full power-cycle behavior.

References:

- <https://github.com/gxcreator/ktmicro-tools>
- <https://github.com/gxcreator/ktmicro-tools/blob/master/KT_USB_PROTOCOL.md>
- <https://github.com/jeromeof/devicePEQ>
- <https://github.com/jeromeof/devicePEQ/blob/master/devicePEQ/ktmicroUsbHidHandler.js>
- <https://github.com/Ircama/ja11-config>

### Frequency-scale resolution by acoustic cross-check (2026-09-19)

The broad Hangout.Audio fallback applies a two-times frequency compensation, while the independent
`gxcreator/ktmicro-tools` KT02H20 reconstruction treats the stored word as Hz directly. Repository
evidence alone could not choose safely between those interpretations.

The recovery audit resolved that conflict without another owner hardware session by comparing the
captured untouched stock filters with public same-earpiece measurements from AudioAmigo. The source
set contains left/right measurements of **Simgot EW300 DSP Silver** through its stock USB-C DSP cable
and the same DSP earpieces through a passive 3.5 mm cable. `tools/analyze_ew300_frequency_scale.py`
averages channels, subtracts the passive-cable curve from the DSP-cable curve, removes only the
measurement-level offset, and fits the captured five Peak filters using RBJ biquad response math.

Fit over 30 Hz–10 kHz:

| Raw-word scale | Correlation | RMS error |
| --- | ---: | ---: |
| 0.5× | -0.011 | 1.421 dB |
| **1× (raw word = Hz)** | **0.989** | **0.152 dB** |
| 2× | 0.050 | 1.156 dB |
| 4× | -0.304 | 1.292 dB |

The 1× interpretation is therefore the evidence-backed EW300 frequency mapping. This conclusion is
specific to the captured EW300 stock profile and exact product family; it does not promote generic
VID/PID fallback behavior. The raw measurement files are not redistributed. Reproduction uses the
four public REW exports named in the AudioAmigo phone book:

- `Simgot EW300 DSP Silver L/R` (stock USB-C DSP cable)
- `Simgot EW300 DSP Silver 3.5mm L/R` (same earpieces, passive cable)
- <https://audioamigo.squig.link/?share=Simgot_EW300_DSP_Silver%2CSimgot_EW300_DSP_Silver_3.5mm>

This closes the frequency-scale question. Independent KT02H20-family evidence identifies `0x66` as
digital DAC/playback gain rather than a dedicated EQ preamp, so the shared canonical model excludes
it from EQ identity and captured Personal EQ profiles, as it already does for Black Pearl playback
gain. Peak-only Personal EQ capture is therefore enabled. This does **not** qualify writing `0x66`,
persistence, Reset, or acoustic shelf labels; snapshots containing non-Peak codes remain rejected.

### Signed-candidate persistence qualification boundary (2026-09-19)

Public protocol implementations and third-party UI behavior support `0x53` only as a candidate Save
command; they cannot demonstrate safe EW300 ordering, nonvolatile persistence, or restoration after
complete power loss. The remaining question therefore requires the exact cable, but it does not
justify another open-ended discovery loop.

The recovery implementation contains one bounded qualifier with these constraints:

- the action is hard-disabled and absent from the UI in ordinary builds, and enabled only by the controlled release-signing
  workflow; the installed app must match the pinned release certificate and embed the exact
  40-character source commit;
- an allowlisted, exact-fingerprint read-only report must pass before the UI enables confirmation;
- the complete 12-register baseline is captured and synchronously persisted before mutation;
- only Band 1 Peak gain (`-0.1 dB`) and `0x66` playback gain (`-0.5 dB`) are reduced;
- both temporary fields must read back exactly before one `0x53` Save is sent;
- a detected physical detach and fresh full snapshot must prove the temporary values survived;
- the two fields are restored to their exact baseline bytes, followed by one restoration Save;
- a second detected physical detach and fresh full snapshot must prove exact baseline restoration;
- a failed or uncertain mutation, Save, restoration, or verification becomes a terminal state and is
  never automatically retried.

A full PASS qualifies Save, Peak persistence, playback-gain persistence, and exact restoration only
for the connected exact fingerprint. A first-cycle exact baseline result is reported as safely not
persistent and leaves persistent features locked. No outcome qualifies shelf acoustics, firmware,
bootloader, erase, calibration, recovery, cross-flash, or arbitrary KT02H20-family behavior.
