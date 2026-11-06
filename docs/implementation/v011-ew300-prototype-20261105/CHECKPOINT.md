# v0.11 EW300 prototype — checkpoint

**Local v0.11 prototype ready; Pixel qualification complete on this artifact.**

## Artifact

| Field | Value |
|---|---|
| Base | origin/main 97e72cc065edb948c121431aeaeb0e5e73576456 |
| Branch | codex/v0.11-ew300-prototype |
| Worktree | /Users/stephenweeks/.codex/worktrees/v011-ew300-prototype/OPRA-EQ-development |
| Code head | 5f14345c9f8e15f2f1fc391c245e35f8a21ff375 |
| versionName / versionCode | 0.11.0-beta.1 / 23 |
| Package | com.weekssa.opraeqforuapp.v011beta |
| Build type | v011beta (release-derived, unminified, update checks off) |
| APK path | app/build/outputs/apk/v011beta/app-v011beta.apk |
| APK SHA-256 | 5e073d9db2311d552e908936cf9fef17e8f7aba75392f4b83e1f2622e32635c9 |
| Signer SHA-256 | 945614206b73258162a2e0722ddcf4a53ae3018652e0fb65d2b9bcc3e38521a5 (Android debug certificate, local test signer) |
| Build time | 2026-11-06T05:23:59Z (2026-11-05 23:23:59 CST) |

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


## Device surface after the UX correction (v0.11 candidate 84c36b93)

The DEVICE page now follows the JA11 structure and shares its components:

- Header: DeviceOperationStatusHeader with the device state and the single Refresh action.
- Audio: one Device volume row (0–60, tap to set) and Startup settings only when the saved state is
  actually known (Saved / Not saved). An unknown state renders no row.
- USB & System: Current sample rate and USB Audio Class as read-only values.
- About (expander): Model, USB identity, Filters ("Five-band Peak, verified"), and one Device
  limitations line. Firmware is shown only when it has been read.

Removed from the product surface: Output gain (duplicate of the same 0x66 control), Equalizer,
Connection, Headset remote, Read USB audio, the raw GET_CUR/feature-unit copy, the blocked-reason
paragraphs, the Experimental features row, and the native-shelf engineering section. Those
engineering surfaces remain only in the local ew300Engineering variant.

### Microphone decision (recorded)

The EW300 accepts a UAC1 feature-unit mute request and the app can verify that value. No capture
path reaches it from this Pixel: the audio policy exposes only the built-in microphones, with no USB
input device, and the owner heard no difference with the control in either position. A GET_CUR
acknowledgement is not a working mute, so the toggle and the Microphone section are removed. The
About limitations line states: the device accepts a microphone mute request, but no effect on
captured audio has been measured.

The unused UI plumbing (Ew300MyDacContent.onSetMicrophoneMuted, MyDacRootScreen/EqLibraryApp/
EqLibraryActions wiring, EqLibraryViewModel.setEw300MicrophoneMuted) was deleted in the same change.
The transport-level mute capability remains in the device layer, unused by the product surface.

### Screenshots

docs/screenshots/v011/device-top.png, about-expanded.png, device-bottom.png, limitations.png (all
from this candidate on emulator-5554), plus the earlier before-ew300-device-pixel.png. The page fits
a 360×640 viewport, so device-bottom.png was captured at 360×360 scrolled to the end to show the
bottom rather than repeating the top.


### Pixel qualification of this candidate (12974c73)

- Installed on the Pixel 9 over wireless ADB; on-device base.apk hash matched.
- DEVICE page verified live against the screenshots: Audio → USB & System → About, a single Device volume row, no Microphone section, About with model / USB identity / Filters / Device limitations.
- Blocker copy verified: "Direct Flash unavailable · SIMGOT EW300 DSP is not connected" before the session, cleared once connected.
- Flash: AFUL Explorer — LoboNautics, one Save, Optimized 8→5, −3.5 dB tracked gain, re-enumeration handled, exact saved-match readback (20 Hz +3.5 Q0.300; 3,500 Hz +3.0 Q0.825; 11,892 Hz +1.0 Q0.500; 1,784 Hz −0.5 Q1.000; 2,700 Hz +1.0 Q3.000).
- Restoration: Reset to flat, one Save, back to Flat at 0.0 dB.
- Save count: one target Save plus one restoration Save. The DAC was left flat at 0.0 dB.


### Pixel verification of the polish (candidate 1995927d)

- Startup settings shows "Not saved" with a Save action after a runtime change; Save flips it to
  "Saved" and the state survived a full unplug/replug with the EQ still Flat at 0.0 dB.
- USB & System holds only the current sample rate; USB audio class moved into About.
- Open finding: a "Reset to flat" on its own did not survive a disconnect in the previous session
  (the DAC came back holding the earlier flash), and the app reports the saved state as unknown
  after a reset. Reset plus an explicit Save did persist. Treat reset-alone persistence as
  unverified until it is reproduced with a power cycle.

