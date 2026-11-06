# v0.11 EW300 prototype — checkpoint

**Local v0.11 prototype ready; Pixel qualification complete on this artifact.**

## Artifact

| Field | Value |
|---|---|
| Base | origin/main 97e72cc065edb948c121431aeaeb0e5e73576456 |
| Branch | codex/v0.11-ew300-prototype |
| Worktree | /Users/stephenweeks/.codex/worktrees/v011-ew300-prototype/OPRA-EQ-development |
| versionName / versionCode | 0.11.0-beta.1 / 23 |
| Package | com.weekssa.opraeqforuapp.v011beta |
| Build type | v011beta (release-derived, unminified, update checks off) |
| APK path | app/build/outputs/apk/v011beta/app-v011beta.apk |
| APK SHA-256 | 9c2d78d018909aed778c79f496f58822beaa296b6f2a3ecb1bf1fc98eb1d366d |
| Signer SHA-256 | 945614206b73258162a2e0722ddcf4a53ae3018652e0fb65d2b9bcc3e38521a5 (Android debug certificate, local test signer) |
| Build time | 2026-11-06T02:35:39Z (2026-11-05 20:35:39 CST) |

The on-device base.apk hash matched this SHA-256 after install.

## Local validation

- JVM debug suite: 878 tests, 0 failures.
- Lint: 0 errors, 121 warnings, 3 hints.
- Emulator (emulator-5554, API 36): connected v011beta instrumented suite 79/79, 0 failures.
- Product-surface test asserts the device tab shows Device volume (0–60), Microphone mute, and
  Current sample rate while the native shelf engineering controls are absent.

## Pixel qualification (this exact APK)

- Device: Pixel 9 (tokay), Android 17, wireless ADB.
- Setup: Settings → SIMGOT EW300 DSP output selected and Direct Flash enabled; the app then showed
  "Direct Flash unavailable · SIMGOT EW300 DSP is not connected" until My DAC connected, which
  proves the reason tracks the real prerequisite.
- Device checks: model SIMGOT EW300 DSP, USB 31B2:0111 · revision 1.01 · HID interface 3, current
  sample rate 96000 Hz (UAC1 GET_CUR), UAC 1.0 read-only, microphone mute Live, headset remote
  "No disable control", 0–60 volume 60/60 · 0.0 dB.
- Library entry: EQ Library → AFUL → Explorer → LoboNautics (community, Source Reddit, 8 filters),
  added to My EQs and selected.
- Flash: one Save, confirmation read "Optimized · 8 → 5 bands · full-response fit … -3.50 dB tracked
  playback-gain adjustment". Result "Flash complete · EQ saved and verified. The DAC reconnected
  and the replacement session was verified."
- Final readback: "Exact saved match" for Explorer · LoboNautics — 20 Hz +3.5 dB Q 0.300;
  3,500 Hz +3.0 dB Q 0.825; 11,892 Hz +1.0 dB Q 0.500; 1,784 Hz -0.5 dB Q 1.000;
  2,700 Hz +1.0 dB Q 3.000; playback/global gain -3.5 dB.
- Shape: five-Peak fallback, not native shelf (the source's low shelves use Q 0.3, outside the
  experimental envelope).
- Restoration: "Reset to flat" → one Save; readback Flat, 0 active bands, five Peak 1 kHz/0 dB/Q1,
  playback/global gain 0.0 dB.
- Persistence: owner unplugged and replugged the EW300; after reconnection the DAC still read Flat
  at 0.0 dB, so the restoration Save persisted and the flashed EQ did not return.
- Save count: one target Save plus one restoration Save. No listening test.

## Scope shipped

Included: 0–60 volume (0x66), UAC1 microphone mute, current sample rate, runtime Apply vs explicit
Save, library Flash with the five-Peak fallback, read-only UAC status, About identity.

Compiled off: shelf editor chips, shelf A/B controls, research setup panel (engineering variant
only), headset-remote and UAC-mode switches (chip-limited), Ellyn/DCR, firmware/bootloader paths.

## Design language

The prototype uses the production My DAC shell, EQ/DEVICE tabs, shared editor/graph/Apply/Flash
components, and the standard Settings output rows. The parity research panel is absent from the
product build. Detail in docs/V0.11_PROTOTYPE_DESIGN_LANGUAGE.md.

## Remaining limitations

- Native shelf acoustics remain unmeasured; the product build is Peak-only and reports Optimized.
- Headset-remote control and UAC mode switching are chip-limited on KT0211L.
- Production Personal/General EQ entries have no export path yet; catalog-derived entries migrate by
  re-selecting the same canonical identity. No database was copied.
- Not full FiiO JA11 parity.

## Rollback

Uninstall com.weekssa.opraeqforuapp.v011beta. The build never writes firmware, bootloader state, or
persistent device identity; the DAC was left Flat at 0.0 dB.

## Next action

Owner review of this artifact as a possible next beta; any further work starts from the recorded
APK and code head.

