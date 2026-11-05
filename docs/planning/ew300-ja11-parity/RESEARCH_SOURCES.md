# Research sources and search log — EW300 / JA11 parity

Status: research log, 2026-11-04. This is a source map, not a capability claim.

## How to use this file

Planning and execution must run a fresh, documented search pass before claiming a new capability or implementing an unproven register behavior. Record the query, date, source, claim, confidence, and decision. Do not copy code from a source merely because it supports a related device. Prefer behavior descriptions, public protocol evidence, and exact-device tests.

## Current source map

| Source | Type | License / provenance | Relevance | Limits |
| --- | --- | --- | --- | --- |
| KT Micro KT02H20 product page: https://www.ktmicro.com/?usb_104/394.html | Vendor | Vendor page; no code | 5-band DSP EQ, DRC/noise gate, UAC 1.0/2.0, 384 kHz/32-bit, 2 Mbit internal FLASH, customer secondary development | Hardware-level; does not document the HID register protocol |
| gxcreator/ktmicro-tools: https://github.com/gxcreator/ktmicro-tools | Community tool | BSD-3-Clause | 0x4B report, 0x52/0x57 protocol, DAC EQ 0x26-0x2F, 0x66 gain, 5-band layout | Family-level; not EW300-specific |
| jeromeof/devicePEQ: https://github.com/jeromeof/devicePEQ | Community tool | 0BSD (verify current) | Generic KTMicro handler, 0x53 commit, 5-band constraints, browser UI patterns | Generic fallback; product-name matching varies; not exact EW300 proof |
| Ircama/ja11-config: https://github.com/Ircama/ja11-config | Community tool | Check repository license before use | JA11 runtime Apply, 0x19 Save, filter types, 5-band workflow | JA11 protocol differs from EW300; not a byte source |
| eyad-elghareeb/kt0231h: https://github.com/eyad-elghareeb/kt0231h | Community field guide | Check repository license; fork lineage | Chip-family matrix, firmware forensics, 0x53 save, chip IDs, 5-band map | Community synthesis; no exact EW300 identity |
| Bukutsu/glacier-eq: https://github.com/Bukutsu/glacier-eq | Community app | GPL-3.0 | Independent JA11 implementation, 5-band limits, RAM apply vs save, UI/state-machine patterns | GPL-3.0; do not copy code into this project without license review. JA11 protocol is not EW300 |
| potatosalad775/eqcaps: https://github.com/potatosalad775/eqcaps | Device-profile data/tool | MIT code; data CC0 | KT02H20 profile, filter constraints, frequency-halving quirk, generic device matching patterns | Profile is for JCALLY/KT02H20 HIFI Audio, not EW300; quirk must be verified |
| mandy321/Audiocular-Aura: https://github.com/mandy321/Audiocular-Aura | Community app | GPL-3.0 | Five-band JA11 UI, Peak/Low Shelf/High Shelf mapping, device-specific protocol selection | GPL-3.0; JA11 report 2 / 0x15 protocol is not EW300 0x4B |
| txtsamu/claude-research: https://github.com/txtsamu/claude-research | Research notes | No license found; treat as notes only | KTMicro dongle comparison, register diffs, 0x53 save, firmware flash caveats, OTG notes | Unlicensed notes; verify claims independently; do not copy code |
| lokal-music/mobile: https://github.com/lokal-music/mobile/blob/main/docs/audio/simgot-ew300-usb-dac-regression.md | Adjacent app regression note | GPL-3.0 | SIMGOT EW300 USB UAC2 direct-stream lifecycle, registration, transfer cancellation, detach handling | Audio streaming, not EQ; listed PID 0x0011 conflicts with observed 0x0111 and is not trusted |
| jaakkopasanen/AutoEq: https://github.com/jaakkopasanen/AutoEq | Measurement/EQ corpus | MIT | EW300 and EW300 DSP measurements, target curves, 5-band EQ fits; useful for shelf acoustic cross-checks | Measurement rig differs; use as corroboration, not exact-device proof |
| jcharvet/chu2-studio: https://github.com/jcharvet/chu2-studio | Community tool/protocol | Check license | KT0210 hardware-tested protocol, family comparison | Different chip/product; not EW300 |
| onbot7/BunnyDSPLinux: https://github.com/onbot7/BunnyDSPLinux | Community tool | Check license | KT0210 5-band/save behavior, Linux control pattern | Different chip/product; not EW300 |
| Cyfine/ja11-web-control: https://github.com/Cyfine/ja11-web-control | Community tool | Check license | JA11 2.20 state-control protocol, pacing, volume/mic/UAC observations | JA11-specific; not EW300 |
| ParkWardRR toolkit: https://github.com/ParkWardRR/fiio-ja11-jcally-jm12-moondropkt02-kt02h20-dac-amp-toolkit | Community cross-flash toolkit | Blue Oak 1.0.0 (verify) | KT02H20 bootloader, firmware flashing, no readback, recovery context | Firmware flashing is out of scope; no EW300 stock image |
| txtsamu/kt02h20-flasher: https://github.com/txtsamu/kt02h20-flasher | Community flasher | Check repository license | Bootloader flow, chip ID CHP response, no firmware readback | Firmware flashing is out of scope |
| SIMGOT Control: https://app.simgot.com/index.php/home/web/appDownload | Vendor app page | Vendor page | Official app compatibility reference | EW300 DSP was not listed in the current compatibility page; do not infer protocol support |
| Android USB host: https://developer.android.com/guide/topics/connectivity/usb/host | Platform docs | Platform docs | USB host, permissions, interfaces, endpoints, detach behavior | Not DAC-specific |

## Search log

| Date | Query / location | Result | Decision |
| --- | --- | --- | --- |
| 2026-11-04 | GitHub repos: KT02H20, KTMicro PEQ, SIMGOT EW300, EW300 DSP | Found ktmicro-tools, devicePEQ, ja11-config, kt0231h, kt02h20-flasher, ParkWardRR, glacier-eq, eqcaps, Audiocular-Aura | Use as family/UX references; no exact EW300 protocol |
| 2026-11-04 | GitHub code: SIMGOT EW300, KT02H20 PEQ | Found AutoEq EW300/EW300 DSP measurements, lokal-music EW300 UAC2 regression, existing repo docs | Use AutoEq for acoustic cross-check; UAC note for Android lifecycle |
| 2026-11-04 | Web: SIMGOT EW300 DSP chip/firmware/teardown | No public EW300 protocol, firmware, or teardown found; official SIMGOT pages and retailer pages only | Keep exact-device evidence as authority |
| 2026-11-04 | Web: KT02H20 datasheet/linux/driver | Official KT Micro product page, reseller listings, TINHIFI custom EQ page | Vendor page is strongest; no official HID protocol document found |
| 2026-11-04 | Repo inspection: glacier-eq JA11 code | 5-band JA11, 0x19 save, RAM apply, 5-band limits | Use for design patterns; GPL-3.0 code is not copied |
| 2026-11-04 | Repo inspection: eqcaps KT02H20 profile | 5-band 12 dB, Peak-only profile for KT02H20 HIFI Audio, frequency-halving caveat | Do not transfer the Peak-only restriction or 2x quirk to EW300 without exact evidence |
| 2026-11-04 | Repo inspection: AuraPEQ | JA11 5-band Peak/LS/HS UI, 0x15 protocol | UI behavior reference only; protocol differs |
| 2026-11-04 | Repo inspection: Glacier EQ, AutoEq, lokal-music, KTMicro comparison notes | Independent JA11 app, EW300/EW300 DSP measurements, Android UAC2 regression note, register-diff notes | Use for UX/measurement/lifecycle corroboration; no exact EW300 protocol or firmware |

## What the search did not find

- No official SIMGOT EW300 HID/EQ protocol specification.
- No public EW300 firmware image, firmware updater, or software readback path.
- No public EW300 teardown that confirms the exact die marking.
- No EW300-specific entry in devicePEQ, eqcaps, glacier-eq, or AuraPEQ.
- No public evidence that the EW300 supports multiple EQ programs, microphone control, UAC switching, or the JA11 Ellyn tuning registers.

## What this means for the plan

- The exact-device register snapshot and repository ledger remain the strongest EW300 evidence.
- Glacier EQ and AuraPEQ can inform the five-band editor layout, filter-type UI, and apply/save state model, but their JA11 protocols are not EW300 protocols.
- eqcaps is a warning that family profiles can carry device-specific quirks; verify every field on the exact EW300.
- AutoEq and squig measurements can support shelf acoustic validation by comparing the EW300 with and without DSP; they do not prove register semantics.
- lokal-music/mobile is useful for Android USB lifecycle and direct-stream failure modes, not EQ.
- Firmware flashing remains out of scope.

## License caution

Do not copy code from GPL-3.0 projects (glacier-eq, Audiocular-Aura, lokal-music) into this repository without a deliberate license review. Do not copy unlicensed research notes. Reimplement behavior clean-room from protocol descriptions and exact-device evidence.

## Repeatable search checklist

Before claiming a new capability, search:

1. GitHub repositories and code for the chip, product, VID/PID, register, command, and filter type.
2. Vendor product pages, manuals, app bundles, firmware release notes, and support forums.
3. Independent protocol projects and issue trackers.
4. Measurement corpora (AutoEq, squig.link) for acoustic cross-checks.
5. Android USB host and audio documentation for lifecycle behavior.
6. Any new source's license and provenance.

Record the query, date, source, claim, confidence, and decision here or in the evidence log. Re-run the search if the prior pass is older than 30 days or a capability remains unresolved.

## 2026-11-05 execution note

Execution performed no new research fan-out, as the accepted plan requires one writer and no execution research fan-out. The 2026-11-04 planning pass above remains the current source map. The read-only live capture in T-002 used only the exact unit and commands 0x52; it adds no protocol source. Native shelf acoustics remain unresolved: type-byte transport is established, acoustic meaning is not. The parity implementation therefore keeps ordinary shelf editing dormant and labels the fixed shelf test targets as an unmeasured hypothesis.

## 2026-11-05 five-goal UAC evidence

- Source: exact EW300 configuration descriptor read through macOS IOUSBDeviceInterface GetConfigurationDescriptorPtr (VID:PID 31B2:0111, LE XIAN, SIMGOT EW300 DSP). Descriptor SHA-256 e955ae8c6c23766224c94e442e671b0ea8230052a70c82df1cf4bff499b36907.
- Exact facts: one configuration; AudioControl interface 0, subclass 1 (UAC1); microphone input terminal type 0x0201 with feature unit 2, master mute supported; playback AS interface 2, endpoint 0x01, rates 44.1/48/96 kHz; capture AS interface 1, endpoint 0x81, rates 44.1/48 kHz; HID interface 3; no UAC2 alternate; no headset feature unit.
- Live controls: UAC1 sample-rate GET_CUR to endpoint 0x01 (wIndex 0x0001) returned 48000 (80BB00); microphone mute GET 0 -> SET 1 -> verify 1 -> restore 0.
- Decision: microphone mute is a real UAC1 control and is exposed in the parity DEVICE surface. Headset-disable and UAC-mode switching remain BLOCKED with this exact descriptor evidence; no JA11 command 0x20 reuse and no blind register sweep.
- Next experiments: static analysis of vendor APK/HID report 0x54 for a headset/UAC vendor control; active-stream sample-rate validation on the Pixel; owner microphone/listening checks.

## 2026-11-05 chip-first vendor/static pass

Source order for this pass: exact EW300 responses first; KT02H20-family firmware/tools second; community maps third; vendor apps/public pages last. The missing EW300 app is not evidence about the chip.

- Official SIMGOT Control APK: https://oss.simgot.com/app/SIMGOT%20Control1.1.2.apk; package com.simgotcontrol.audio 1.1.2 (21); sha256 eec1e6b973f53e588756e065ec550e306bfa7d874395b9512a470d9bc323abee. Decompiled with jadx 1.5.1. UsbHidManager uses report ID 75 (0x4B), a 64-byte request-type/code packet; product enum is EG280/DEW0S/DEW4U/DEW1U only. No EW300 product, no UAC-mode handler, no headset-disable USB switch. Secondary negative.
- Firmware assets in that APK: firmware.bin (75,683 bytes) sha256 f5230b2174cfd6e6ebe06666e6bd7e62f5b5ce268bedab1d035b6e680f320f12; update1.fot sha256 2fd471a28d58da8599d1e829eee2f28a28ffd235d1b57dcb7308c3985df3759b; update(19)(1).ufw sha256 f1020ec9472f4c222141e1cc3ded3141902bb67f0fb5e41e1082135c2acc8287. Strings/header inspection found family firmware markers (inti gmic1, USB_PLAYING) but no EW300 version/control table. No flashing or redistribution.
- Exact EW300 UAC feature units: unit 2 mic mute/volume, unit 7 output volume, unit 4 no usable payload; no headset unit; one UAC1 configuration. HID 0x01 GET_REPORT failed; 0x4B/0x54 GET_REPORT returned the same ambiguous 10-byte packet. These are exact-device negatives, recorded in CHIP_MAP.md.

## 2026-11-05 exact die confirmation and KT0211L reassessment

- CHP read-only request: KTM handshake `1e4b544d`, CHP `d2434850`; reply `12 40 85 B2 30 32 31 31 4C 43 30 32 3E`; chip ID `0211LC02` = **KT0211L**.
- Official/third-party KT0211L profile: USB Audio Class 1.0; 24-bit/96 kHz DAC; 24-bit/96 kHz mono ADC microphone input; microphone preamp with adjustable bias; six GPIOs; up to four function keys; auxiliary ADC; 2-wire; external interrupt. Source: https://warmseaic.com/kt/kt0211l.
- Decision: G3 is **CHIP-LIMITED** (UAC1-only silicon), not a missing vendor command. G2-headset remains open; the next experiment is KT0211L GPIO/key/aux-ADC/mic-bias configuration, not KT02H20 headset inheritance.
- The earlier “KT02H20 family” assumption is retained only as historical family evidence. The exact die is KT0211L.

### KT0211L headset boundary

- The family firmware asset (sha256 f5230b21…) contains OMTP (0x74b0), CTIA lradc_data:%d (0x778f), id %d click/double/long press, and host type:computer. This is family evidence for headset-type/key detection, not an EW300 USB disable control.
- Exact EW300 has no headset feature unit; HID 0x01 GET_REPORT failed; HID 0x54 is ambiguous. G2-headset is CHIP-LIMITED on the USB surface; only an exact firmware/config field could change that.

