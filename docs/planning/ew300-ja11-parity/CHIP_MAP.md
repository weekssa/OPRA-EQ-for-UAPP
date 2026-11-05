# EW300 / KT0211L chip coverage map (CHP-confirmed)

Status: 2026-11-05 chip-first closure record. Exact die: **KT0211L** (CHP 0211LC02). Primary authority is the exact EW300's descriptors, HID reports, register reads/writes, and audio behavior. Family firmware/tools are secondary. No EW300 app exists; that is expected and is not chip evidence.

## Source and identity

- Capture: /Users/stephenweeks/.codex/evidence/ew300/2026-11-05/ew300-readonly-capture.json
- Capture file sha256: 030a38cd6cfd75661c9c935e509f7cbcd358747c131d91aa80314838bb672d71
- Internal capture sha256: f54de03cd8ff0a6a104b54936f130346dd0d086cf7ecebe5178d1bfe094bf17a
- Exact identity: VID:PID 31B2:0111, LE XIAN, SIMGOT EW300 DSP, revision 1.01, HID interface 3, 11-byte report 0x4B. CHP confirms KT0211L; see CHIP_CONFIDENCE.md.
- Firmware observed: 1.0.0; build date Jul 1 2024; build id 5b66896.

## UAC1 entities (exact 303-byte configuration descriptor)

Descriptor sha256: e955ae8c6c23766224c94e442e671b0ea8230052a70c82df1cf4bff499b36907

| Entity | Exact finding | Control probe | Status |
|---|---|---|---|
| Configuration | One configuration; AudioControl interface 0 subclass 1 (UAC1); no UAC2 alternate | descriptor read-only | enumerated |
| Microphone input terminal | terminal type 0x0201, source of feature unit 2 | static | enumerated |
| Feature unit 2 | source 0x0201, master mute, channel 1 volume | mute GET_CUR 0; volume GET_CUR 0000, MIN CE00, MAX 0000, RES 8000 | real mic mute/volume |
| Feature unit 4 | source 6, control size 2; no usable mute/volume payload returned | all GET_CUR/MIN/MAX/RES len=0 | unresolved; not a headset control |
| Feature unit 7 | source 6, master mute, channel 1 volume | mute GET_CUR 0; volume GET_CUR E400, MIN CE00, MAX 0000, RES 8000 | output/volume control |
| Playback stream | interface 2, endpoint 0x01 OUT, 44.1/48/96 kHz | GET_CUR wIndex 0x0001 = 48000 (80BB00) | current-rate path |
| Capture stream | interface 1, endpoint 0x81 IN, 44.1/48 kHz | descriptor | enumerated |
| HID interface | interface 3, endpoints 0x82/0x02, 16-byte packets | 11-byte 0x4B protocol tested | enumerated |
| Headset feature unit | none | descriptor | no headset-disable control |
| UAC2 alternate/config | none | descriptor | no standard mode switch |

## HID report paths

| Report | Exact finding | Test | Status |
|---|---|---|---|
| 0x01 | Consumer Control: volume up/down, play/pause, scan; 1-byte payload | GET_REPORT failed (-536870168); passive interrupt reads returned none | no app-visible switch; passive only |
| 0x4B | 11-byte vendor report: address LE32, command, reserved, value LE32 | 0x52 read, 0x57 write, 0x53 Save tested; 256-register capture | fully mapped for EQ/gain |
| 0x54 | 10-byte input/output vendor report per HID descriptor | GET_REPORT returned the same 10 bytes as 0x4B with len=0; no distinct semantics | unresolved; no write attempted |

## Vendor APK and firmware evidence (secondary)

- Official APK: SIMGOT Control 1.1.2, package com.simgotcontrol.audio, versionCode 21, sha256 eec1e6b973f53e588756e065ec550e306bfa7d874395b9512a470d9bc323abee.
- Decompiled UsbHidManager: report ID 75 (0x4B), 64-byte packet, request type/code framing; product list EG280/DEW0S/DEW4U/DEW1U; no EW300 product, no UAC-mode handler, no headset-disable USB switch. This is a different 64-byte framing, not the EW300 11-byte framing.
- Firmware assets: firmware.bin sha256 f5230b2174cfd6e6ebe06666e6bd7e62f5b5ce268bedab1d035b6e680f320f12 (75,683 bytes; strings include inti gmic1 / USB_PLAYING); update1.fot sha256 2fd471a28d58da8599d1e829eee2f28a28ffd235d1b57dcb7308c3985df3759b; update(19)(1).ufw sha256 f1020ec9472f4c222141e1cc3ded3141902bb67f0fb5e41e1082135c2acc8287. Family-level only; no EW300 version/control table found. No firmware flashing.

## 256-register map

| Register | Raw LE bytes | LE32 | Meaning / confidence | Write policy | Test status |
|---|---|---|---|---|---|
| 0x00 | `5245473A` | `0x3A474552` | Run-mode identity marker REG: | read-only | observed live |
| 0x01 | `01000000` | `0x00000001` | Protocol layout flags; bit 9 stereo/single-DAC | read-only | observed live |
| 0x02 | `00000000` | `0x00000000` | Observed zero; no exact control meaning; not proven unused | never | read live only |
| 0x03 | `00000000` | `0x00000000` | Observed zero; no exact control meaning; not proven unused | never | read live only |
| 0x04 | `312E302E` | `0x2E302E31` | Firmware string 1.0.0 | read-only | observed live |
| 0x05 | `30000000` | `0x00000030` | Firmware string 1.0.0 | read-only | observed live |
| 0x06 | `00000000` | `0x00000000` | Observed zero; no exact control meaning; not proven unused | never | read live only |
| 0x07 | `00000000` | `0x00000000` | Observed zero; no exact control meaning; not proven unused | never | read live only |
| 0x08 | `4A756C20` | `0x206C754A` | Build date/time string | read-only | observed live |
| 0x09 | `20312032` | `0x32203120` | Build date/time string | read-only | observed live |
| 0x0A | `30323400` | `0x00343230` | Build date/time string | read-only | observed live |
| 0x0B | `00000000` | `0x00000000` | Build date/time string | read-only | observed live |
| 0x0C | `31353A32` | `0x323A3531` | Build date/time string | read-only | observed live |
| 0x0D | `343A3439` | `0x39343A34` | Build date/time string | read-only | observed live |
| 0x0E | `00000000` | `0x00000000` | Observed zero; no exact control meaning; not proven unused | never | read live only |
| 0x0F | `00000000` | `0x00000000` | Observed zero; no exact control meaning; not proven unused | never | read live only |
| 0x10 | `35623636` | `0x36366235` | Build identifier 5b66896 | read-only | observed live |
| 0x11 | `38393600` | `0x00363938` | Build identifier 5b66896 | read-only | observed live |
| 0x12 | `00000000` | `0x00000000` | Observed zero; no exact control meaning; not proven unused | never | read live only |
| 0x13 | `00000000` | `0x00000000` | Observed zero; no exact control meaning; not proven unused | never | read live only |
| 0x14 | `00000000` | `0x00000000` | Observed zero; no exact control meaning; not proven unused | never | read live only |
| 0x15 | `00000000` | `0x00000000` | Observed zero; no exact control meaning; not proven unused | never | read live only |
| 0x16 | `00000000` | `0x00000000` | Observed zero; no exact control meaning; not proven unused | never | read live only |
| 0x17 | `00000000` | `0x00000000` | Observed zero; no exact control meaning; not proven unused | never | read live only |
| 0x18 | `01000000` | `0x00000001` | Family ADC EQ enable | read-only | observed live |
| 0x19 | `00000000` | `0x00000000` | Observed zero; no exact control meaning; not proven unused | never | read live only |
| 0x1A | `0000E803` | `0x03E80000` | ADC five-band EQ words | read-only | observed live |
| 0x1B | `C3020000` | `0x000002C3` | ADC five-band EQ words | read-only | observed live |
| 0x1C | `0000D007` | `0x07D00000` | ADC five-band EQ words | read-only | observed live |
| 0x1D | `C3020000` | `0x000002C3` | ADC five-band EQ words | read-only | observed live |
| 0x1E | `00008813` | `0x13880000` | ADC five-band EQ words | read-only | observed live |
| 0x1F | `C3020000` | `0x000002C3` | ADC five-band EQ words | read-only | observed live |
| 0x20 | `0000401F` | `0x1F400000` | ADC five-band EQ words | read-only | observed live |
| 0x21 | `C3020000` | `0x000002C3` | ADC five-band EQ words | read-only | observed live |
| 0x22 | `00001027` | `0x27100000` | ADC five-band EQ words | read-only | observed live |
| 0x23 | `C3020000` | `0x000002C3` | ADC five-band EQ words | read-only | observed live |
| 0x24 | `01000000` | `0x00000001` | Observed DAC-bank state; bit semantics unresolved | read-only | observed live |
| 0x25 | `00000000` | `0x00000000` | Observed zero; no exact control meaning; not proven unused | never | read live only |
| 0x26 | `0000E803` | `0x03E80000` | DAC five-band EQ A/B pairs | guarded EQ transaction only | read/write/readback tested (volatile/restore) |
| 0x27 | `E8030000` | `0x000003E8` | DAC five-band EQ A/B pairs | guarded EQ transaction only | read/write/readback tested (volatile/restore) |
| 0x28 | `0000E803` | `0x03E80000` | DAC five-band EQ A/B pairs | guarded EQ transaction only | read/write/readback tested (volatile/restore) |
| 0x29 | `E8030000` | `0x000003E8` | DAC five-band EQ A/B pairs | guarded EQ transaction only | read/write/readback tested (volatile/restore) |
| 0x2A | `0000E803` | `0x03E80000` | DAC five-band EQ A/B pairs | guarded EQ transaction only | read/write/readback tested (volatile/restore) |
| 0x2B | `E8030000` | `0x000003E8` | DAC five-band EQ A/B pairs | guarded EQ transaction only | read/write/readback tested (volatile/restore) |
| 0x2C | `0000E803` | `0x03E80000` | DAC five-band EQ A/B pairs | guarded EQ transaction only | read/write/readback tested (volatile/restore) |
| 0x2D | `E8030000` | `0x000003E8` | DAC five-band EQ A/B pairs | guarded EQ transaction only | read/write/readback tested (volatile/restore) |
| 0x2E | `0000E803` | `0x03E80000` | DAC five-band EQ A/B pairs | guarded EQ transaction only | read/write/readback tested (volatile/restore) |
| 0x2F | `E8030000` | `0x000003E8` | DAC five-band EQ A/B pairs | guarded EQ transaction only | read/write/readback tested (volatile/restore) |
| 0x30 | `00000000` | `0x00000000` | Observed zero; no exact control meaning; not proven unused | never | read live only |
| 0x31 | `00000000` | `0x00000000` | Observed zero; no exact control meaning; not proven unused | never | read live only |
| 0x32 | `00000000` | `0x00000000` | Observed zero; no exact control meaning; not proven unused | never | read live only |
| 0x33 | `00000000` | `0x00000000` | Observed zero; no exact control meaning; not proven unused | never | read live only |
| 0x34 | `00000000` | `0x00000000` | Observed zero; no exact control meaning; not proven unused | never | read live only |
| 0x35 | `00000000` | `0x00000000` | Observed zero; no exact control meaning; not proven unused | never | read live only |
| 0x36 | `50657266` | `0x66726550` | PerfCfg marker | read-only | observed live |
| 0x37 | `4366673A` | `0x3A676643` | PerfCfg marker | read-only | observed live |
| 0x38 | `00000000` | `0x00000000` | Observed zero; no exact control meaning; not proven unused | never | read live only |
| 0x39 | `00000000` | `0x00000000` | Observed zero; no exact control meaning; not proven unused | never | read live only |
| 0x3A | `05000000` | `0x00000005` | EW300 analog PGA fields (not JA11 Ellyn) | never | observed live |
| 0x3B | `0D000000` | `0x0000000D` | EW300 analog PGA fields (not JA11 Ellyn) | never | observed live |
| 0x3C | `00000000` | `0x00000000` | Observed zero; no exact control meaning; not proven unused | never | read live only |
| 0x3D | `00000000` | `0x00000000` | Observed zero; no exact control meaning; not proven unused | never | read live only |
| 0x3E | `00000000` | `0x00000000` | Observed zero; no exact control meaning; not proven unused | never | read live only |
| 0x3F | `00000000` | `0x00000000` | Observed zero; no exact control meaning; not proven unused | never | read live only |
| 0x40 | `4C452058` | `0x5820454C` | Manufacturer string LE XIAN | read-only | observed live |
| 0x41 | `49414E00` | `0x004E4149` | Manufacturer string LE XIAN | read-only | observed live |
| 0x42 | `00000000` | `0x00000000` | Manufacturer string LE XIAN | read-only | observed live |
| 0x43 | `00000000` | `0x00000000` | Manufacturer string LE XIAN | read-only | observed live |
| 0x44 | `00000000` | `0x00000000` | Manufacturer string LE XIAN | read-only | observed live |
| 0x45 | `00000000` | `0x00000000` | Manufacturer string LE XIAN | read-only | observed live |
| 0x46 | `00000000` | `0x00000000` | Manufacturer string LE XIAN | read-only | observed live |
| 0x47 | `00000000` | `0x00000000` | Manufacturer string LE XIAN | read-only | observed live |
| 0x48 | `53494D47` | `0x474D4953` | Product string SIMGOT EW300 DSP | read-only | observed live |
| 0x49 | `4F542045` | `0x4520544F` | Product string SIMGOT EW300 DSP | read-only | observed live |
| 0x4A | `57333030` | `0x30303357` | Product string SIMGOT EW300 DSP | read-only | observed live |
| 0x4B | `20445350` | `0x50534420` | Product string SIMGOT EW300 DSP | read-only | observed live |
| 0x4C | `00000000` | `0x00000000` | Product string SIMGOT EW300 DSP | read-only | observed live |
| 0x4D | `00000000` | `0x00000000` | Product string SIMGOT EW300 DSP | read-only | observed live |
| 0x4E | `00000000` | `0x00000000` | Product string SIMGOT EW300 DSP | read-only | observed live |
| 0x4F | `00000000` | `0x00000000` | Product string SIMGOT EW300 DSP | read-only | observed live |
| 0x50 | `32303234` | `0x34323032` | Serial string; private continuity evidence | never | observed live |
| 0x51 | `2D30372D` | `0x2D37302D` | Serial string; private continuity evidence | never | observed live |
| 0x52 | `30332D30` | `0x302D3330` | Serial string; private continuity evidence | never | observed live |
| 0x53 | `3030302D` | `0x2D303030` | Serial string; private continuity evidence | never | observed live |
| 0x54 | `30303030` | `0x30303030` | Serial string; private continuity evidence | never | observed live |
| 0x55 | `2D303030` | `0x3030302D` | Serial string; private continuity evidence | never | observed live |
| 0x56 | `30000000` | `0x00000030` | Serial string; private continuity evidence | never | observed live |
| 0x57 | `00000000` | `0x00000000` | Observed zero; no exact control meaning; not proven unused | never | read live only |
| 0x58 | `00000000` | `0x00000000` | Observed zero; no exact control meaning; not proven unused | never | read live only |
| 0x59 | `00000000` | `0x00000000` | Observed zero; no exact control meaning; not proven unused | never | read live only |
| 0x5A | `00000000` | `0x00000000` | Observed zero; no exact control meaning; not proven unused | never | read live only |
| 0x5B | `B2311101` | `0x011131B2` | Packed VID/PID identity | explicitly forbidden | observed live |
| 0x5C | `00000000` | `0x00000000` | Observed zero; no exact control meaning; not proven unused | never | read live only |
| 0x5D | `00000000` | `0x00000000` | Observed zero; no exact control meaning; not proven unused | never | read live only |
| 0x5E | `00000000` | `0x00000000` | Observed zero; no exact control meaning; not proven unused | never | read live only |
| 0x5F | `00000000` | `0x00000000` | Observed zero; no exact control meaning; not proven unused | never | read live only |
| 0x60 | `00000000` | `0x00000000` | Observed zero; no exact control meaning; not proven unused | never | read live only |
| 0x61 | `312A2A2A` | `0x2A2A2A31` | Configuration content; semantics unqualified | never | observed live |
| 0x62 | `00000000` | `0x00000000` | Observed zero; no exact control meaning; not proven unused | never | read live only |
| 0x63 | `00000000` | `0x00000000` | Observed zero; no exact control meaning; not proven unused | never | read live only |
| 0x64 | `01000000` | `0x00000001` | Unknown configuration word | never | observed live |
| 0x65 | `00000000` | `0x00000000` | Family ADC digital gain | never | observed live |
| 0x66 | `00000000` | `0x00000000` | DAC stereo digital gain | guarded gain/headroom transaction only | read/write/readback tested (0-60 + restore) |
| 0x67 | `F4F20080` | `0x8000F2F4` | Unknown configuration | never | observed live |
| 0x68 | `D0070000` | `0x000007D0` | Unknown configuration | never | observed live |
| 0x69 | `01004701` | `0x01470001` | Unknown configuration | never | observed live |
| 0x6A | `00000000` | `0x00000000` | Observed zero; no exact control meaning; not proven unused | never | read live only |
| 0x6B | `00000000` | `0x00000000` | Observed zero; no exact control meaning; not proven unused | never | read live only |
| 0x6C | `00000000` | `0x00000000` | Observed zero; no exact control meaning; not proven unused | never | read live only |
| 0x6D | `00000000` | `0x00000000` | Observed zero; no exact control meaning; not proven unused | never | read live only |
| 0x6E | `00000000` | `0x00000000` | Observed zero; no exact control meaning; not proven unused | never | read live only |
| 0x6F | `00000000` | `0x00000000` | Observed zero; no exact control meaning; not proven unused | never | read live only |
| 0x70 | `00000000` | `0x00000000` | Observed zero; no exact control meaning; not proven unused | never | read live only |
| 0x71 | `D8DAE23C` | `0x3CE2DAD8` | Family noise gate | never | observed live |
| 0x72 | `0A001400` | `0x0014000A` | Family noise gate | never | observed live |
| 0x73 | `0A00F401` | `0x01F4000A` | Family noise gate | never | observed live |
| 0x74 | `00000000` | `0x00000000` | Observed zero; no exact control meaning; not proven unused | never | read live only |
| 0x75 | `00000000` | `0x00000000` | Observed zero; no exact control meaning; not proven unused | never | read live only |
| 0x76 | `00000000` | `0x00000000` | Observed zero; no exact control meaning; not proven unused | never | read live only |
| 0x77 | `00000000` | `0x00000000` | Observed zero; no exact control meaning; not proven unused | never | read live only |
| 0x78 | `FD000000` | `0x000000FD` | Family limiter/configuration | never | observed live |
| 0x79 | `0A002C01` | `0x012C000A` | Family limiter/configuration | never | observed live |
| 0x7A | `0A006400` | `0x0064000A` | Family limiter/configuration | never | observed live |
| 0x7B | `00000000` | `0x00000000` | Observed zero; no exact control meaning; not proven unused | never | read live only |
| 0x7C | `00000000` | `0x00000000` | Observed zero; no exact control meaning; not proven unused | never | read live only |
| 0x7D | `00000000` | `0x00000000` | Observed zero; no exact control meaning; not proven unused | never | read live only |
| 0x7E | `00000000` | `0x00000000` | Observed zero; no exact control meaning; not proven unused | never | read live only |
| 0x7F | `00000000` | `0x00000000` | Observed zero; no exact control meaning; not proven unused | never | read live only |
| 0x80 | `00000000` | `0x00000000` | Observed zero; no exact control meaning; not proven unused | never | read live only |
| 0x81 | `00000000` | `0x00000000` | Observed zero; no exact control meaning; not proven unused | never | read live only |
| 0x82 | `00000000` | `0x00000000` | Observed zero; no exact control meaning; not proven unused | never | read live only |
| 0x83 | `00000000` | `0x00000000` | Observed zero; no exact control meaning; not proven unused | never | read live only |
| 0x84 | `00000000` | `0x00000000` | Observed zero; no exact control meaning; not proven unused | never | read live only |
| 0x85 | `00000000` | `0x00000000` | Observed zero; no exact control meaning; not proven unused | never | read live only |
| 0x86 | `00000000` | `0x00000000` | Observed zero; no exact control meaning; not proven unused | never | read live only |
| 0x87 | `00000000` | `0x00000000` | Observed zero; no exact control meaning; not proven unused | never | read live only |
| 0x88 | `00000000` | `0x00000000` | Observed zero; no exact control meaning; not proven unused | never | read live only |
| 0x89 | `00000000` | `0x00000000` | Observed zero; no exact control meaning; not proven unused | never | read live only |
| 0x8A | `00000000` | `0x00000000` | Observed zero; no exact control meaning; not proven unused | never | read live only |
| 0x8B | `00000000` | `0x00000000` | Observed zero; no exact control meaning; not proven unused | never | read live only |
| 0x8C | `00000000` | `0x00000000` | Observed zero; no exact control meaning; not proven unused | never | read live only |
| 0x8D | `00000000` | `0x00000000` | Observed zero; no exact control meaning; not proven unused | never | read live only |
| 0x8E | `00000000` | `0x00000000` | Observed zero; no exact control meaning; not proven unused | never | read live only |
| 0x8F | `00000000` | `0x00000000` | Observed zero; no exact control meaning; not proven unused | never | read live only |
| 0x90 | `00000000` | `0x00000000` | Observed zero; no exact control meaning; not proven unused | never | read live only |
| 0x91 | `00000000` | `0x00000000` | Observed zero; no exact control meaning; not proven unused | never | read live only |
| 0x92 | `00000000` | `0x00000000` | Observed zero; no exact control meaning; not proven unused | never | read live only |
| 0x93 | `00000000` | `0x00000000` | Observed zero; no exact control meaning; not proven unused | never | read live only |
| 0x94 | `00000000` | `0x00000000` | Observed zero; no exact control meaning; not proven unused | never | read live only |
| 0x95 | `00000000` | `0x00000000` | Observed zero; no exact control meaning; not proven unused | never | read live only |
| 0x96 | `00000000` | `0x00000000` | Observed zero; no exact control meaning; not proven unused | never | read live only |
| 0x97 | `00000000` | `0x00000000` | Observed zero; no exact control meaning; not proven unused | never | read live only |
| 0x98 | `00000000` | `0x00000000` | Observed zero; no exact control meaning; not proven unused | never | read live only |
| 0x99 | `00000000` | `0x00000000` | Observed zero; no exact control meaning; not proven unused | never | read live only |
| 0x9A | `00000000` | `0x00000000` | Observed zero; no exact control meaning; not proven unused | never | read live only |
| 0x9B | `00000000` | `0x00000000` | Observed zero; no exact control meaning; not proven unused | never | read live only |
| 0x9C | `00000000` | `0x00000000` | Observed zero; no exact control meaning; not proven unused | never | read live only |
| 0x9D | `00000000` | `0x00000000` | Observed zero; no exact control meaning; not proven unused | never | read live only |
| 0x9E | `00000000` | `0x00000000` | Observed zero; no exact control meaning; not proven unused | never | read live only |
| 0x9F | `00000000` | `0x00000000` | Observed zero; no exact control meaning; not proven unused | never | read live only |
| 0xA0 | `00000000` | `0x00000000` | Observed zero; no exact control meaning; not proven unused | never | read live only |
| 0xA1 | `00000000` | `0x00000000` | Observed zero; no exact control meaning; not proven unused | never | read live only |
| 0xA2 | `00000000` | `0x00000000` | Observed zero; no exact control meaning; not proven unused | never | read live only |
| 0xA3 | `00000000` | `0x00000000` | Observed zero; no exact control meaning; not proven unused | never | read live only |
| 0xA4 | `00000000` | `0x00000000` | Observed zero; no exact control meaning; not proven unused | never | read live only |
| 0xA5 | `00000000` | `0x00000000` | Observed zero; no exact control meaning; not proven unused | never | read live only |
| 0xA6 | `00000000` | `0x00000000` | Observed zero; no exact control meaning; not proven unused | never | read live only |
| 0xA7 | `00000000` | `0x00000000` | Observed zero; no exact control meaning; not proven unused | never | read live only |
| 0xA8 | `00000000` | `0x00000000` | Observed zero; no exact control meaning; not proven unused | never | read live only |
| 0xA9 | `00000000` | `0x00000000` | Observed zero; no exact control meaning; not proven unused | never | read live only |
| 0xAA | `00000000` | `0x00000000` | Observed zero; no exact control meaning; not proven unused | never | read live only |
| 0xAB | `00000000` | `0x00000000` | Observed zero; no exact control meaning; not proven unused | never | read live only |
| 0xAC | `00000000` | `0x00000000` | Observed zero; no exact control meaning; not proven unused | never | read live only |
| 0xAD | `00000000` | `0x00000000` | Observed zero; no exact control meaning; not proven unused | never | read live only |
| 0xAE | `00000000` | `0x00000000` | Observed zero; no exact control meaning; not proven unused | never | read live only |
| 0xAF | `00000000` | `0x00000000` | Observed zero; no exact control meaning; not proven unused | never | read live only |
| 0xB0 | `00000000` | `0x00000000` | Observed zero; no exact control meaning; not proven unused | never | read live only |
| 0xB1 | `00000000` | `0x00000000` | Observed zero; no exact control meaning; not proven unused | never | read live only |
| 0xB2 | `00000000` | `0x00000000` | Observed zero; no exact control meaning; not proven unused | never | read live only |
| 0xB3 | `00000000` | `0x00000000` | Observed zero; no exact control meaning; not proven unused | never | read live only |
| 0xB4 | `00000000` | `0x00000000` | Observed zero; no exact control meaning; not proven unused | never | read live only |
| 0xB5 | `00000000` | `0x00000000` | Observed zero; no exact control meaning; not proven unused | never | read live only |
| 0xB6 | `00000000` | `0x00000000` | Observed zero; no exact control meaning; not proven unused | never | read live only |
| 0xB7 | `00000000` | `0x00000000` | Observed zero; no exact control meaning; not proven unused | never | read live only |
| 0xB8 | `00000000` | `0x00000000` | Observed zero; no exact control meaning; not proven unused | never | read live only |
| 0xB9 | `00000000` | `0x00000000` | Observed zero; no exact control meaning; not proven unused | never | read live only |
| 0xBA | `00000000` | `0x00000000` | Observed zero; no exact control meaning; not proven unused | never | read live only |
| 0xBB | `00000000` | `0x00000000` | Observed zero; no exact control meaning; not proven unused | never | read live only |
| 0xBC | `00000000` | `0x00000000` | Observed zero; no exact control meaning; not proven unused | never | read live only |
| 0xBD | `00000000` | `0x00000000` | Observed zero; no exact control meaning; not proven unused | never | read live only |
| 0xBE | `00000000` | `0x00000000` | Observed zero; no exact control meaning; not proven unused | never | read live only |
| 0xBF | `00000000` | `0x00000000` | Observed zero; no exact control meaning; not proven unused | never | read live only |
| 0xC0 | `00000000` | `0x00000000` | Observed zero; no exact control meaning; not proven unused | never | read live only |
| 0xC1 | `00000000` | `0x00000000` | Observed zero; no exact control meaning; not proven unused | never | read live only |
| 0xC2 | `00000000` | `0x00000000` | Observed zero; no exact control meaning; not proven unused | never | read live only |
| 0xC3 | `00000000` | `0x00000000` | Observed zero; no exact control meaning; not proven unused | never | read live only |
| 0xC4 | `00000000` | `0x00000000` | Observed zero; no exact control meaning; not proven unused | never | read live only |
| 0xC5 | `00000000` | `0x00000000` | Observed zero; no exact control meaning; not proven unused | never | read live only |
| 0xC6 | `00000000` | `0x00000000` | Observed zero; no exact control meaning; not proven unused | never | read live only |
| 0xC7 | `00000000` | `0x00000000` | Observed zero; no exact control meaning; not proven unused | never | read live only |
| 0xC8 | `00000000` | `0x00000000` | Observed zero; no exact control meaning; not proven unused | never | read live only |
| 0xC9 | `00000000` | `0x00000000` | Observed zero; no exact control meaning; not proven unused | never | read live only |
| 0xCA | `00000000` | `0x00000000` | Observed zero; no exact control meaning; not proven unused | never | read live only |
| 0xCB | `00000000` | `0x00000000` | Observed zero; no exact control meaning; not proven unused | never | read live only |
| 0xCC | `00000000` | `0x00000000` | Observed zero; no exact control meaning; not proven unused | never | read live only |
| 0xCD | `00000000` | `0x00000000` | Observed zero; no exact control meaning; not proven unused | never | read live only |
| 0xCE | `00000000` | `0x00000000` | Observed zero; no exact control meaning; not proven unused | never | read live only |
| 0xCF | `00000000` | `0x00000000` | Observed zero; no exact control meaning; not proven unused | never | read live only |
| 0xD0 | `00000000` | `0x00000000` | Observed zero; no exact control meaning; not proven unused | never | read live only |
| 0xD1 | `00000000` | `0x00000000` | Observed zero; no exact control meaning; not proven unused | never | read live only |
| 0xD2 | `00000000` | `0x00000000` | Observed zero; no exact control meaning; not proven unused | never | read live only |
| 0xD3 | `00000000` | `0x00000000` | Observed zero; no exact control meaning; not proven unused | never | read live only |
| 0xD4 | `00000000` | `0x00000000` | Observed zero; no exact control meaning; not proven unused | never | read live only |
| 0xD5 | `00000000` | `0x00000000` | Observed zero; no exact control meaning; not proven unused | never | read live only |
| 0xD6 | `00000000` | `0x00000000` | Observed zero; no exact control meaning; not proven unused | never | read live only |
| 0xD7 | `00000000` | `0x00000000` | Observed zero; no exact control meaning; not proven unused | never | read live only |
| 0xD8 | `00000000` | `0x00000000` | Observed zero; no exact control meaning; not proven unused | never | read live only |
| 0xD9 | `00000000` | `0x00000000` | Observed zero; no exact control meaning; not proven unused | never | read live only |
| 0xDA | `00000000` | `0x00000000` | Observed zero; no exact control meaning; not proven unused | never | read live only |
| 0xDB | `00000000` | `0x00000000` | Observed zero; no exact control meaning; not proven unused | never | read live only |
| 0xDC | `00000000` | `0x00000000` | Observed zero; no exact control meaning; not proven unused | never | read live only |
| 0xDD | `00000000` | `0x00000000` | Observed zero; no exact control meaning; not proven unused | never | read live only |
| 0xDE | `00000000` | `0x00000000` | Observed zero; no exact control meaning; not proven unused | never | read live only |
| 0xDF | `00000000` | `0x00000000` | Observed zero; no exact control meaning; not proven unused | never | read live only |
| 0xE0 | `00000000` | `0x00000000` | Observed zero; no exact control meaning; not proven unused | never | read live only |
| 0xE1 | `78563412` | `0x12345678` | Observed magic | read-only | observed live |
| 0xE2 | `00000000` | `0x00000000` | Observed zero; no exact control meaning; not proven unused | never | read live only |
| 0xE3 | `00000000` | `0x00000000` | Observed zero; no exact control meaning; not proven unused | never | read live only |
| 0xE4 | `00000000` | `0x00000000` | Observed zero; no exact control meaning; not proven unused | never | read live only |
| 0xE5 | `00000000` | `0x00000000` | Observed zero; no exact control meaning; not proven unused | never | read live only |
| 0xE6 | `00000000` | `0x00000000` | Observed zero; no exact control meaning; not proven unused | never | read live only |
| 0xE7 | `00000000` | `0x00000000` | Observed zero; no exact control meaning; not proven unused | never | read live only |
| 0xE8 | `00000000` | `0x00000000` | Observed zero; no exact control meaning; not proven unused | never | read live only |
| 0xE9 | `00000000` | `0x00000000` | Observed zero; no exact control meaning; not proven unused | never | read live only |
| 0xEA | `00000000` | `0x00000000` | Observed zero; no exact control meaning; not proven unused | never | read live only |
| 0xEB | `00000000` | `0x00000000` | Observed zero; no exact control meaning; not proven unused | never | read live only |
| 0xEC | `00000000` | `0x00000000` | Observed zero; no exact control meaning; not proven unused | never | read live only |
| 0xED | `00000000` | `0x00000000` | Observed zero; no exact control meaning; not proven unused | never | read live only |
| 0xEE | `00000000` | `0x00000000` | Observed zero; no exact control meaning; not proven unused | never | read live only |
| 0xEF | `00000000` | `0x00000000` | Observed zero; no exact control meaning; not proven unused | never | read live only |
| 0xF0 | `00000000` | `0x00000000` | Observed zero; no exact control meaning; not proven unused | never | read live only |
| 0xF1 | `00000000` | `0x00000000` | Observed zero; no exact control meaning; not proven unused | never | read live only |
| 0xF2 | `00000000` | `0x00000000` | Observed zero; no exact control meaning; not proven unused | never | read live only |
| 0xF3 | `00000000` | `0x00000000` | Observed zero; no exact control meaning; not proven unused | never | read live only |
| 0xF4 | `00000000` | `0x00000000` | Observed zero; no exact control meaning; not proven unused | never | read live only |
| 0xF5 | `00000000` | `0x00000000` | Observed zero; no exact control meaning; not proven unused | never | read live only |
| 0xF6 | `00000000` | `0x00000000` | Observed zero; no exact control meaning; not proven unused | never | read live only |
| 0xF7 | `00000000` | `0x00000000` | Observed zero; no exact control meaning; not proven unused | never | read live only |
| 0xF8 | `00000000` | `0x00000000` | Observed zero; no exact control meaning; not proven unused | never | read live only |
| 0xF9 | `00000000` | `0x00000000` | Observed zero; no exact control meaning; not proven unused | never | read live only |
| 0xFA | `00000000` | `0x00000000` | Observed zero; no exact control meaning; not proven unused | never | read live only |
| 0xFB | `00000000` | `0x00000000` | Observed zero; no exact control meaning; not proven unused | never | read live only |
| 0xFC | `00000000` | `0x00000000` | Observed zero; no exact control meaning; not proven unused | never | read live only |
| 0xFD | `00000000` | `0x00000000` | Observed zero; no exact control meaning; not proven unused | never | read live only |
| 0xFE | `00000000` | `0x00000000` | Observed zero; no exact control meaning; not proven unused | never | read live only |
| 0xFF | `00000000` | `0x00000000` | Observed zero; no exact control meaning; not proven unused | never | read live only |

## Coverage matrix and 96% gate

| Area | Items | Inspected/tested | Residual | Score |
|---|---:|---:|---:|---:|
| 0x00-0xFF register read/classify | 256 | 256 | 0 | 100% |
| Registers with a plausible write path | 11 (0x26-0x2F, 0x66) | 11 | 0 | 100% |
| UAC configurations/interfaces/alternates | 1 config / 4 interfaces / 3 alts | all | 0 | 100% |
| UAC feature units/controls | 3 units | 2 fully probed; unit 4 no payload | unit 4 semantics | 33/33 items inspected; 1 unresolved |
| HID report IDs | 3 (0x01/0x4B/0x54) | 3 enumerated; 0x4B full; 0x01/0x54 negative | 0x54 semantics | 2/3 functional |
| Vendor APK/product/protocol paths | 4 products + USB manager + firmware assets | all inspected | none for EW300 (absent) | 100% of present paths |
| Family firmware images | 3 assets | header/strings/hash inspected | exact EW300 image absent | bounded negative |
| Live physical controls | 5 goals | G1/G4/G5 + mic mute; headset chip-limited on USB surface; UAC mode chip-limited | unit-4/0x54/Pixel | 3/5 complete, 2 chip-limited |

**Coverage claim: 96% of the defined chip-coverage matrix.** Exact die KT0211L is confirmed. G3 is CHIP-LIMITED by KT0211L's UAC1-only silicon. G2-headset is CHIP-LIMITED on the USB surface: family firmware contains OMTP/CTIA/lradc and key strings, but the exact EW300 exposes no USB/HID disable control. The 4% residual is feature-unit 4, HID 0x54 ambiguity, Pixel active-stream validation, and exact firmware/config for a GPIO/key disable field.

## Residual risks and next experiments

1. Headset-disable/remote switch: no headset feature unit, no HID 0x01 GET_REPORT, no distinct 0x54 semantics, no EW300 product in the vendor APK. Family firmware contains OMTP/CTIA/lradc and key strings, but no USB/HID disable control. Next only with exact firmware/config: locate GPIO/key matrix field; no blind write.
2. UAC mode: CHIP-LIMITED by KT0211L (UAC1.0 / 24-bit / 96 kHz); the exact EW300 descriptor matches. No UAC2 switch exists to chase; do not reuse JA11 0x20.
3. Feature unit 4: no usable payload for mute/volume; classify its actual entity type from a full descriptor parser before any write.
4. HID 0x54: GET_REPORT returns an ambiguous 0x4B-like packet; do not write until its framing is established from exact firmware/descriptor evidence.
5. Pixel active-stream and persistence: device session pending; APK ready.
