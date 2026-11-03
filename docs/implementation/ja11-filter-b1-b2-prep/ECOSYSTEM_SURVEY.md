# KT02H20 / JA11 tooling ecosystem survey

Status: **INSPECTION-ONLY.** Revisions were read from each repository's default
branch through the GitHub API. No code was executed, installed, connected or
used to mutate a device. “No filter found” is bounded to the inspected source and
documentation, not proof that the hardware lacks filters.

Distinguish the DAC reconstruction filter from PEQ band types. None of the
inspected tools other than Ircama exposes a reconstruction-filter selector.

| Repository and revision | Filter implementation | Transport and framing | JA11 evidence | Licence and Apache-project reuse |
|---|---|---|---|---|
| ParkWardRR runtime control `c59b923dc46a4666af2e7ae4b898256c5d92a293` | No reconstruction filter found; five-band PEQ and device-state controls. | Rust `rusb`/libusb; HID-class interface; report `02`; `AA 0A`/`BB 0B`; BE sequence; CRC-8/MAXIM over sequence through payload; `EE`. | Repository-reported JA11 PEQ readback, save and power-cycle retention. Firmware payload `02 14` is interpreted by the author as `1.4`, separate from OPRA's current convention. No filter test. | BlueOak-1.0.0; reuse with required licence text/link and notices. |
| ParkWardRR toolkit `1ede0a9afdb3c5b607c6ee714b2610843117226b` | None; firmware unlock/flash tooling. | HID boot transition then CDC serial; no runtime filter frame. | Reports reflashes including JA11 V2.2; no filter behavior. | BlueOak-1.0.0; review vendor assets separately. |
| Ircama/ja11-config `affddff6c9808c33ce8b35b0b9759ff0d7f6e405` | Explicit `CMD_DAC_FILTER=0x11`, values `1..5`, called a Savitech command adapted for JA11. | C HIDAPI documented with libusb backend; report `02`; read `02 BB 0B 00 00 11 00 00 EE`; set `02 AA 0A 00 00 11 01 vv 00 EE`; one 500 ms input-report read. | Advertises five filters but inspected material has no firmware-bound filter readback or measurement. A displayed FAST-LL survives read failure. | EUPL-1.2; a direct copied/translated implementation carries reciprocal obligations. Prefer independently implemented protocol facts. |
| Cyfine/ja11-web-control `4d4eb83df6fcdf9e20b52e1bdf59a77f463b2c30` | No reconstruction filter found; PEQ, preset, volume, mic and UAC controls. | WebHID; report ID `02`; zero sequence/check byte JA11 format; `AA 0A`/`BB 0B`; `EE`. | README requires firmware 1.9+; protocol notes report physical 2.20 tests for controls and PEQ, not reconstruction filters. | MIT; reuse with copyright/permission notice. |
| gxcreator/ktmicro-tools `39e620d8cd68c4afb711b91153b4ae91042adc32` | No reconstruction filter found; ADC/DAC PEQ register pairs. | Python HIDAPI/hidraw; different 11-byte register protocol: report `4B`, LE32 address/value, read `52`, write `57`. | Stock KTMicro/JM12-style target; no JA11 filter qualification. | BSD-3-Clause; retain notices/disclaimer and do not imply endorsement. |
| SepTGut/kt02h20-control `18d6a361aa8d6ceb56c2bcfd2993755afc5b1cce` | No reconstruction filter found; PEQ and EQ controls. | WebHID; report ID `02`; `AA 0A`/`BB 0B`, zero sequence/check byte, length, `EE`. | Names JA11 firmware as a target but provides no exact firmware filter evidence. | No licence found; reuse permission not established. |
| eyad-elghareeb/kt0231h `2a420394db3397747b0fa93ce1f8ab1c706857b7` | No reconstruction filter found; multi-chip PEQ register access. | WebHID; `4B`/LE32 register protocol; chip-specific write ACKs. | Analyzes JA11 V2.2 image and reports a reflash on a Moondrop-branded KT02H20; no factory-JA11 filter proof. | No licence found; reuse permission not established. |
| txtsamu/kt02h20-flasher `5d6883f8290abf4cfde1d283b777cae27964e2cc` | None; firmware flasher. | HIDAPI/hidraw boot transition then CDC serial with CRC-32 firmware blocks. | Reports flashing JA11 V2.2 onto a JCALLY KT02H20; no runtime filter proof. | MIT; retain notice for covered code. |

## Reuse judgment

No inspected library can be adopted to ship a JA11 filter. OPRA's existing
codec sits at the correct abstraction layer. ParkWardRR supplies the strongest
lead for complete sequence/CRC framing, which B1 evaluates as a read-only
hypothesis. Cyfine and SepTGut help normalize WebHID report-ID behavior;
gxcreator demonstrates that the register-based KTMicro family is a different
protocol surface. Ircama remains the sole explicit filter implementation, but
successful JA11 filter selection is **UNSUPPORTED**.

ParkWardRR's parser records CRC mismatch in `checksum_ok` instead of rejecting the
frame. A parse success therefore does not prove a valid checksum. Its hardware
log similarly distinguishes device-generated reply CRCs from proof that the
device validates incoming request CRCs.

Working zero-CRC OPRA volume, preset and Save traffic contradicts a **universal**
request-CRC requirement. A command-specific CRC or sequence requirement for
`0x11` remains possible but is unproven. B1 is designed to test that narrow
question; it is not a licence to repeat J034 outside its approved session.

## Source links

- <https://github.com/ParkWardRR/fiio-ja11-jcally-jm12-moondropkt02-kt02h20-dac-amp-control/tree/c59b923dc46a4666af2e7ae4b898256c5d92a293>
- <https://github.com/ParkWardRR/fiio-ja11-jcally-jm12-moondropkt02-kt02h20-dac-amp-toolkit/tree/1ede0a9afdb3c5b607c6ee714b2610843117226b>
- <https://github.com/Ircama/ja11-config/tree/affddff6c9808c33ce8b35b0b9759ff0d7f6e405>
- <https://github.com/Cyfine/ja11-web-control/tree/4d4eb83df6fcdf9e20b52e1bdf59a77f463b2c30>
- <https://github.com/gxcreator/ktmicro-tools/tree/39e620d8cd68c4afb711b91153b4ae91042adc32>
- <https://github.com/SepTGut/kt02h20-control/tree/18d6a361aa8d6ceb56c2bcfd2993755afc5b1cce>
- <https://github.com/eyad-elghareeb/kt0231h/tree/2a420394db3397747b0fa93ce1f8ab1c706857b7>
- <https://github.com/txtsamu/kt02h20-flasher/tree/5d6883f8290abf4cfde1d283b777cae27964e2cc>
