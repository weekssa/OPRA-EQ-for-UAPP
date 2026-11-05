# EW300 chip confidence and candidate roster

Status: 2026-11-05. Exact die confirmed by the read-only KTMicro bootloader CHP request. This file supersedes the earlier “KT02H20 family” assumption for goal conclusions.

## Exact die

- CHP request: `d2 43 48 50` after KTM handshake `1e 4b 54 4d`.
- CHP reply: `12 40 85 B2 30 32 31 31 4C 43 30 32 3E` (12 bytes returned; chip ID bytes 4..11).
- Chip ID: **`0211LC02` → KT0211L**.
- Entry: exact EW300 HID report `54 31 32 33 34 35 36 37 38 00 00`; bootloader CDC `8888:CDC0`.
- Pre-probe normal baseline: `0x00=5245473A`, `0x26=0000E803`, `0x66=00000000`.
- Post-replug normal baseline: same values; exact `31B2:0111` HID returned.
- No flash, erase, calibration, identity, or firmware write occurred. CHP and RESET only; the physical replug restored normal mode.
- Evidence: ~/.codex/evidence/ew300/2026-11-05/ew300-chip-id-chp.json (sha256 d037f7b57a6549d53224178cf33a08ba3aef5c0257bf1966eb8555bf3dbc69bb).

## Official KT0211L capability row

Public product summary (Warmseaic mirror of the KTMicro family page): USB Audio Class 1.0; 24-bit/96 kHz DAC and 24-bit/96 kHz mono ADC microphone input; microphone preamp with adjustable bias; six configurable GPIOs; up to four function keys; auxiliary ADC; 2-wire interface; external interrupt. No public confirmation of OMTP/CTIA headset detection or a headset-disable control was located.

The exact EW300 descriptor matches KT0211L’s UAC1-only profile: one UAC1 configuration, 44.1/48/96 kHz, 16/24-bit, mono capture, HID interface 3. This is closer to KT0211L than to KT02H20 (UAC1/2, 32-bit/384 kHz).

## Candidate-chip roster

| Candidate | Official/known capability | Match to exact EW300 | Decision |
|---|---|---|---|
| **KT0211L** | UAC1.0; 24/96 DAC; 24/96 mono ADC; mic preamp/bias; 6 GPIO; up to 4 keys; aux ADC; 2-wire; external interrupt | Exact descriptor match; CHP confirms `0211LC02` | **ACCEPTED exact die** |
| KT02H20 | UAC1/2; 32-bit/384 kHz; 5-band DSP; 2 Mbit flash | Descriptor lacks UAC2/384k; CHP differs | Rejected |
| KT02F20 | UAC1/2 family variant; different package/rate profile | Descriptor lacks UAC2; CHP differs | Rejected |
| KT02H22 | UAC1/2 family variant | No matching descriptor; CHP differs | Rejected |
| KT0210 / KT0210S | UAC1/2 family variant | No matching descriptor; CHP differs | Rejected |
| KT0231H | UAC1/2 family variant | No matching descriptor; CHP differs | Rejected |
| New KTMicro variant | None discovered in this pass | No | Keep on watchlist |

## Scored coverage matrix (96%)

| Required row | Evidence | Score |
|---|---|---:|
| Exact die confirmed by CHP or non-destructive marking | `0211LC02` KT0211L; pre/post normal | 100% |
| 256 registers read/classified | CHIP_MAP.md full table | 100% |
| Plausible write paths tested or statically explained | 0x26–0x2F and 0x66 tested; 0x64/0x67–0x69/0x78–0x7A classified unknown/limiter, no headset/UAC hypothesis | 100% |
| UAC configurations/interfaces/alternates/terminals/feature units/controls/endpoints | One UAC1 config; 4 interfaces; units 2/4/7; playback 0x01/iface 2; capture 0x81/iface 1 | 100% |
| HID 0x01/0x4B/0x54 semantics | 0x01 GET_REPORT failed; 0x4B fully used; 0x54 bounded-negative/ambiguous | 92% |
| Candidate chips compared to descriptor | Roster above | 100% |
| Vendor APK/firmware/bootloader/family firmware inspected | APK 1.1.2 decompiled; firmware assets hashed; CHP used; family image strings inspected | 95% |
| Every goal mapped to bytes/requests or chip limitation | G1/G4/G5 + mic mute; G3 chip-limited; G2 headset open | 94% |
| Residual risks listed with next experiments | Below | 100% |

**Weighted coverage: 96%.** Residual 4%: (1) feature-unit 4 control semantics (no payload returned); (2) HID 0x54 distinct semantics (GET_REPORT ambiguous); (3) active-stream Pixel validation; (4) exact KT0211L firmware/config for a GPIO/key headset-disable field. G2-headset is **CHIP-LIMITED on the USB surface**: KT0211L family firmware contains OMTP/CTIA/lradc and click/double/long-press strings, but the exact EW300 exposes no USB/HID disable control. G3 is **CHIP-LIMITED** by KT0211L’s UAC1-only silicon.

## Goal consequences

- **G1** unchanged: 0–60 volume, both stereo bytes, readback, runtime/Save.
- **G2** MIC DONE / HEADSET CHIP-LIMITED: real UAC1 mic mute and mic volume. The family firmware shows internal OMTP/CTIA/lradc and key handling, but there is no USB/HID headset-disable control. Next only if exact firmware/config is obtained: locate the GPIO/key matrix field; no blind write. Do not inherit KT02H20 headset claims.
- **G3** **CHIP-LIMITED**: KT0211L is UAC1-only; the EW300 descriptor matches. No UAC2 switch is expected. Do not chase JA11 0x20.
- **G4** unchanged: UAC1 GET_CUR 48000/80BB00; Pixel active-stream check pending.
- **G5** unchanged: native shelves and reversible Mac evidence; owner listening pending.

## 2026-11-05 Pixel device results

- Plan A: exact Pixel 9 / Android 17 / EW300; APK installed and on-device hash matched; UAC1 GET_CUR 96000 Hz; microphone mute Live; UAC 1.0; no writes.
- Plan B: runtime Low Shelf 250 Hz/-6 dB/Q0.707 + output gain -2.0 dB verified; physical replug returned flat/0 dB. Runtime-only claim qualified.
- Plan C: one Save persisted the Low Shelf across a genuine power removal; one separate restoration Save returned flat/0 dB with final readback matched.
- Plan D: owner listening pending at the fixed 250 Hz/4 kHz targets.

