# v0.11 EW300 prototype — session handoff and resume pack

Written 2026-11-06 at the owner's request so that nothing from this session depends on chat
history, memory, or cache. Everything below is in version control on the branch named here.

## Resume prompt (paste this into a fresh thread)

> Work in /Users/stephenweeks/.codex/worktrees/v011-ew300-prototype/OPRA-EQ-development on
> branch codex/v0.11-ew300-prototype. Read AGENTS.md, then
> docs/implementation/v011-ew300-prototype-20261105/SESSION_HANDOFF.md and CHECKPOINT.md.
> Continue from the Open work list in the handoff. Do not push, merge, publish, or change signing
> without explicit approval. Device work requires the owner to attach the phone and DAC.

## Repository and artifact

| Item | Value |
|---|---|
| Base | origin/main 97e72cc065edb948c121431aeaeb0e5e73576456 |
| Branch | codex/v0.11-ew300-prototype |
| Worktree | /Users/stephenweeks/.codex/worktrees/v011-ew300-prototype/OPRA-EQ-development |
| Code head | 513755050711d5383d782cde8dc315c061da38e5 (APK built from it) |
| Docs head at pause | this commit |
| Working tree | clean |
| Package | com.weekssa.opraeqforuapp.v011beta |
| Version | 0.11.0-beta.1 / versionCode 23 |
| Build type | v011beta (release-derived, unminified, update checks off) |
| APK | app/build/outputs/apk/v011beta/app-v011beta.apk |
| APK SHA-256 | 45a00243b060f10958c2108ec0261f85f7aca961bc29cb8bbb17b169e4cd0c03 |
| Signer SHA-256 | 945614206b73258162a2e0722ddcf4a53ae3018652e0fb65d2b9bcc3e38521a5 (local debug cert) |
| Build time | 2026-11-06 00:13:15 CST |
| Gate | docs/implementation/v011-ew300-prototype-20261105/GATES.md, sealed; scope-guard IN SCOPE |

Local commits, newest first (all on this branch, nothing pushed):

615e9d42 Document the session handoff and resume pack
51375505 Make My DAC reachable from the navigation bar
a7d45114 Record the Settings v2 density and large-text check
a6295d80 Record the Settings v2 head
39514232 Rebuild the Settings screen around five groups and short rows
34545291 Say the EW300 is attached when only the session is missing
17e9de84 Record the My DAC selection fix head
5f14345c Follow the attached DAC in My DAC instead of a stale selection
887f9e66 Record the Pixel verification of the polished DEVICE page
b862fd10 Record the polished v0.11 candidate head
4c486d01 Polish the v0.11 device page: startup Save, fixed USB class, shorter copy
3b56cd02 Record the Pixel qualification of the corrected v0.11 device page
96f36290 Record the corrected v0.11 candidate head
57fd5836 Correct the v0.11 DEVICE surface and remove the microphone control
7f160f34 Record the v0.11 qualified code head
974b6b90 Qualify the v0.11 prototype on the Pixel and correct the shelf copy
8f49418b Record the v0.11 prototype artifact identity and checkpoint
2320a217 Productize the EW300 surfaces as the v0.11 local prototype

Earlier commits on this branch come from the EW300 parity work that the prototype was built on;
they are listed by `git log --oneline origin/main..HEAD` and recorded in TEST_EVIDENCE_LOG.md.

## What the prototype contains

Included: EW300 0-60 device volume (0x66), UAC1 microphone mute plumbing (no longer surfaced),
current sample rate, runtime Apply vs explicit Save, EQ Library / My EQs Flash through the existing
single route with the five-Peak fallback, read-only UAC status, About identity.

Excluded or compiled off: headset-remote switch and UAC mode switch (chip-limited on KT0211L),
native Low/High Shelf product controls and shelf A/B controls (engineering variant only, acoustics
unmeasured), Ellyn/DCR tuning, firmware/bootloader/erase/calibration paths. The product build is
Peak-only for shelf-bearing profiles and reports them as Optimized.

## Decisions made this session (with why)

1. **Microphone mute removed from the product surface.** The device accepts a UAC1 mute request and
   the value verifies, but the Pixel audio policy exposes no USB input device, no capture path
   reaches the EW300, and the owner heard no difference in either position. A GET_CUR value is not
   a working mute. About carries the limitation that no effect on captured audio has been measured.
2. **DEVICE page rebuilt on the JA11 structure** (Audio / USB & System / About), removing the
   duplicate output-gain row, Equalizer, Connection, Headset remote, Read USB audio, Experimental
   features and the engineering panel.
3. **Startup settings offers Save** and persists across a real unplug/replug.
4. **USB audio class moved to About** because it is fixed for the device and was presented like a
   settable row; USB & System keeps only the current sample rate.
5. **Settings rebuilt as five groups** (EQ target, Outputs, Library, Appearance, About) with one
   control per row; root copy 93->81 strings and 867->616 words.
6. **My DAC is a navigation destination** whenever a supported DAC is recognized, because the
   device card was the only route and an experimental build without the card stranded the workspace.
   Unsupported (JCALLY) identities still create no entry.
7. **Shelf acoustics remain unmeasured and the product build stays Peak-only.** No listening test
   was run or requested; no parity claim is made.

## Evidence recorded in this repository

- TEST_EVIDENCE_LOG.md T-023 through T-040: shelf A/B byte evidence, harness withdrawal, shelf
  acoustics UNMEASURED, Flash route completion, owner-library Flash on the Pixel with one Save and
  one restoration Save, DEVICE surface corrections, Settings v2 metrics, My DAC navigation.
- CHECKPOINT.md: artifact identity, scope, Pixel qualification, limitations.
- docs/releases/v0.11.0-beta.1.md: beta notes, included/excluded, pixel plan.
- docs/V0.11_PROTOTYPE_DESIGN_LANGUAGE.md: layout and copy comparison, accessibility/density check.
- docs/screenshots/v011/: before/after Settings at 412 dp, Settings at 360 dp with font scale
  1.0/1.3/2.0, DEVICE top/bottom/About/limitations, plus the earlier before-ew300-device-pixel.png.
- docs/planning/ew300-ja11-parity/: GOALS.md, FUNCTIONAL_MAPPING.md, CHIP_MAP.md, CHIP_CONFIDENCE.md,
  TEST_EVIDENCE_LOG.md - the protocol, chip and capability record this build rests on.

Latest validated state at pause: JVM 883 tests / 0 failures; lint 0 errors, 121 warnings, 2 hints;
connected v011beta instrumented suite 84/84 on emulator-5554 (2026-11-06 06:24Z).

## Pixel results that must not be lost

- Owner library EQ flashed and restored on APK 12974c73 (AFUL Explorer - LoboNautics, Optimized
  8->5, one Save, exact readback, one restoration Save). T-028, T-033.
- Microphone: device accepts the mute request, no capture path on the Pixel, no audible effect. T-032.
- Startup Save: Save flips to Saved and survives a full unplug/replug with the DAC still Flat at
  0.0 dB. T-035.
- **Open finding:** a Reset to flat on its own did not survive a disconnect - the DAC came back
  holding the previous flash at -3.5 dB, and the app reports the saved state as unknown after a
  reset. Reset plus an explicit Save did persist. Treat reset-alone persistence as unverified.
- My DAC outage: an experimental build with the card removed stranded the workspace; fixed by the
  navigation destination in 51375505.

## Open work, in priority order

1. **Reset persistence.** Reproduce: reset to flat, capture the app's result line, power-cycle, and
   check whether the previous EQ returns. If it does, the reset transaction is not reaching the
   DAC's stored state; compare against the explicit Save path (Ew300Flasher.resetToFlat vs the
   runtime Save). Do not change the Flash transaction without re-running the JVM suite and one
   Pixel Save/restore.
2. **Card removal.** My DAC is now a nav destination, so the status card duplicates it. Migrating
   the remaining tests that click "Open My DAC" (grep app/src/androidTest for that label) then
   deleting the card is one change. The navigation rule test already reflects the new contract.
3. **TalkBack pass.** Not done. Needs the phone: traverse Settings, My DAC and My EQs and confirm
   each row announces label, value and state, and that the Direct Flash switch is not merged into
   the DAC row.
4. **Vocabulary pass** across My DAC and My EQs so "target", "output", "Direct Flash" and "Save"
   match Settings.
5. **M3 ten-category audit** of Settings and My DAC with scores recorded, not just claims.
6. **Production data export path** for Personal and General EQs; only catalog-derived entries
   migrate today (by re-selecting the same canonical identity).

## How to build, test and capture

- Build: `./tools/codex-android :app:assembleV011beta`
- Unit tests: `./tools/codex-android :app:testDebugUnitTest`
- Lint: `./tools/codex-android :app:lintDebug`
- Instrumented: `ANDROID_SERIAL=emulator-5554 ./tools/codex-android :app:connectedV011betaAndroidTest`
  (uninstall both packages first if the run aborts with DELETE_FAILED_INTERNAL_ERROR)
- Screenshots: install app + androidTest APKs, then
  `adb shell am instrument -w -e class com.weekssa.opraeqforuapp.ui.screens.Ew300DeviceScreenshotTest
  com.weekssa.opraeqforuapp.v011beta.test/androidx.test.runner.AndroidJUnitRunner`,
  then pull /sdcard/Android/data/com.weekssa.opraeqforuapp.v011beta/files/v011-screens/.
- Gate: run `tools/scope-guard --root .` before every commit; new files must be added to CONTRACT.md
  and GATES.md and resealed with `--reseal --seal`.

## Process notes

- One writer. Local commits only; no push, PR, merge, release, or signing change was made.
- Every UI change was validated with JVM + lint + the emulator suite before committing.
- Hardware claims are limited to what was observed: no listening test, no acoustic claim, no full
  JA11 parity claim.
- Device state at pause: the EW300 was left Flat at 0.0 dB with the prototype installed; the owner
  is disconnecting wireless debugging.

