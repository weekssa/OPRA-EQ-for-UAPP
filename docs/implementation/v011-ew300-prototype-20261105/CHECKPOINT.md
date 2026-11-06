# v0.11 EW300 prototype — checkpoint

**Local v0.11 prototype ready; Pixel qualification pending.**

## Artifact

| Field | Value |
|---|---|
| Base | origin/main 97e72cc065edb948c121431aeaeb0e5e73576456 |
| Branch | codex/v0.11-ew300-prototype |
| Worktree | /Users/stephenweeks/.codex/worktrees/v011-ew300-prototype/OPRA-EQ-development |
| Code head | 2320a21742d22d58b0431900aa595f9b695eee01 |
| versionName / versionCode | 0.11.0-beta.1 / 23 |
| Package | com.weekssa.opraeqforuapp.v011beta |
| Build type | v011beta (release-derived, unminified, update checks off) |
| APK path | app/build/outputs/apk/v011beta/app-v011beta.apk |
| APK SHA-256 | f0a664686dd3e7773b5bf7b18535f8dbd39b5108a5a815bbf2c751353441a3c6 |
| Signer SHA-256 | 945614206b73258162a2e0722ddcf4a53ae3018652e0fb65d2b9bcc3e38521a5 (Android debug certificate, local test signer) |
| Build time | 2026-11-06T02:18:33Z (2026-11-05 20:18:33 CST) |

## Local validation

- JVM debug suite: 878 tests, 0 failures.
- Lint: 0 errors, 121 warnings, 3 hints.
- Emulator (emulator-5554, API 36): connected v011beta instrumented suite 79/79, 0 failures.
- Product-surface test asserts the device tab shows Device volume (0–60), Microphone mute, and
  Current sample rate while the native shelf engineering controls are absent.
- Library Flash routing and five-Peak fallback tests pass in the JVM suite.

## Scope shipped

Included: 0–60 volume (0x66), UAC1 microphone mute, current sample rate, runtime Apply vs explicit
Save, library Flash with the five-Peak fallback, read-only UAC status, About identity.

Compiled off: shelf editor chips, shelf A/B controls, research setup panel (engineering variant
only), headset-remote and UAC-mode switches (chip-limited), Ellyn/DCR, firmware/bootloader paths.

## Design language

The prototype uses the production My DAC shell, EQ/DEVICE tabs, shared editor/graph/Apply/Flash
components, and the standard Settings output rows. The parity research panel is absent from the
product build. Detail in docs/V0.11_PROTOTYPE_DESIGN_LANGUAGE.md.

## Library migration proof (local path)

Routing is covered by Ew300LibraryFlashRoutingTest (saved EQ and managed profile reaching the EW300
transaction, plus both blocker reasons). The hardware Save/readback for an owner library entry was
last demonstrated on the parity artifact (T-028); it has not been repeated on this prototype APK
because no device was connected.

## Remaining limitations

- Pixel Save/restore qualification is pending for this exact APK.
- Native shelf acoustics remain unmeasured; the product build is Peak-only.
- Headset-remote control and UAC mode switching are chip-limited on KT0211L.
- Production Personal/General EQ entries have no export path yet; catalog-derived entries migrate by
  re-selecting the same canonical identity. No database was copied.
- Not full FiiO JA11 parity.

## Pixel test plan

1. Wireless ADB access to the owner Pixel; install the recorded APK and verify the on-device hash.
2. Settings → select SIMGOT EW300 DSP and enable Direct Flash.
3. My DAC → Connect; check model, 31B2:0111, sample rate, microphone mute, 0–60 volume.
4. My EQs → AFUL Explorer → LoboNautics → Flash: expect Optimized 8→5, one Save, re-enumeration,
   exact readback, −3.5 dB tracked gain.
5. Restore: Set Flat, output gain 0.0 dB, one "Save EQ and output gain"; verify Flat at 0.0 dB.
6. The owner will be asked to unplug/replug the EW300 only if the app requires a physical
   re-enumeration that does not happen automatically.

Rollback: uninstall com.weekssa.opraeqforuapp.v011beta; no firmware or identity writes occur.

## Next action

Attach the Pixel over wireless ADB and run the six-step plan against this exact APK.

