# AFUL Explorer Favorite smoke run

## Environment

- App: `com.weekssa.opraeqforuapp`, debug build from the uncommitted candidate worktree based on `6ada7efc84425f7b149f9f9e9fb4e9599bc80cf2`.
- Device: fresh `medium_phone` API 36 AVD, serial `emulator-5554`.
- Catalog: app loaded the current canonical and OPRA feeds. The AFUL Explorer row exposed the AutoEQ, Reddit, and HiFiGuides records.
- Initial My EQs state: 0 headphone EQs and 0 General EQs.

## Results

### Action: Verify AFUL Explorer appears in EQ Library ✅

- **Commands**:
  - `adb shell am start -n com.weekssa.opraeqforuapp/com.weekssa.opraeqforuapp.MainActivity`
  - `adb shell input tap 540 2274` to open EQ Library
  - `adb shell input tap 540 588` and `adb shell input text AFUL` to search
  - `android layout --device emulator-5554 --pretty`
- **Comment**: The AFUL results included Explorer. Opening it displayed the source cards, including LoboNautics / Reddit and Jaytiss / HiFiGuides.

### Action: Save a known-good Favorite before the affected rows ✅

- **Commands**:
  - `adb shell input tap 975 1200` on the AutoEQ Jaytiss card's `Add favorite` control
  - `android layout --device emulator-5554 --pretty`
- **Comment**: The action changed to `Remove favorite` and the app showed `Saved to My EQs favorites.` This AutoEQ profile was used as the known-good Favorite control.

### Action: Favorite LoboNautics ✅

- **Commands**:
  - `adb shell input tap 975 1613` on the LoboNautics card's `Add favorite` control
  - `android layout --device emulator-5554 --pretty`
- **Comment**: The action changed to `Remove favorite` and the app showed `Saved to My EQs favorites.`

### Action: Favorite Jaytiss from HiFiGuides ✅

- **Commands**:
  - `adb shell input tap 975 1123` on the HiFiGuides Jaytiss card's `Add favorite` control
  - `android layout --device emulator-5554 --pretty`
- **Comment**: The action changed to `Remove favorite` and the app showed `Saved to My EQs favorites.`

### Action: Verify both affected Favorites appear in My EQs ✅

- **Commands**:
  - `adb shell input tap 172 2274` to open My EQs
  - `android layout --device emulator-5554 --pretty`
- **Comment**: The saved list showed Jaytiss with `Database: Hifiguides` and LoboNautics with `Database: Reddit`; both had `Remove ... from favorites` controls.

### Action: Remove the LoboNautics Favorite ✅

- **Commands**:
  - `adb shell input swipe 540 1900 540 1250 700` to reveal the lower saved row
  - `adb shell input tap 975 1392` on `Remove LoboNautics ... from favorites`
  - `android layout --device emulator-5554 --pretty`
- **Comment**: LoboNautics disappeared from the saved list. HiFiGuides Jaytiss and the AutoEQ Jaytiss known-good Favorite remained.

### Action: Restart the app and verify persistence ✅

- **Commands**:
  - `adb shell am force-stop com.weekssa.opraeqforuapp`
  - `adb shell am start -n com.weekssa.opraeqforuapp/com.weekssa.opraeqforuapp.MainActivity`
  - `android layout --device emulator-5554 --pretty`
- **Comment**: After process restart, HiFiGuides Jaytiss and the AutoEQ Jaytiss Favorite were present. LoboNautics remained removed.

### Action: Verify invalid identity still fails closed ✅

- **Commands**:
  - `./tools/codex-android :app:testDebugUnitTest`
  - `adb shell am instrument -w -e class com.weekssa.opraeqforuapp.data.library.SavedEqCanonicalSelectionPersistenceTest com.weekssa.opraeqforuapp.test/androidx.test.runner.AndroidJUnitRunner`
- **Comment**: The JVM integration test rejects stale acoustics and a different canonical profile ID. The Room test pairs the exact saved selection with an unrelated product ID, expects `CANONICAL_SOURCE_UNAVAILABLE`, and verifies the database remains empty.

## Cleanup

The temporary `medium_phone` AVD was stopped and removed after the run. No physical DAC or hardware mutation was used.

## 2026-09-30 Luna final verification smoke

### Environment and initial state

- AVD: `codex-api36`, started with `./tools/codex-android emulator -avd codex-api36 -no-window -no-audio -no-boot-anim`.
- Device: `emulator-5554`, model `sdk_gphone64_arm64`, API 36.
- `./tools/codex-android adb devices -l` showed no device before the AVD started. No physical or wireless device was connected.
- The installed debug app initially showed `0 headphone EQs · 0 General EQs` and no saved Favorites.
- App and test APK installation: `./tools/codex-android :app:installDebug :app:installDebugAndroidTest` succeeded.

### Actions and observed results

1. Launched `com.weekssa.opraeqforuapp/.MainActivity`, opened EQ Library, searched `AFUL Explorer`, and opened the `Explorer` row with five profiles.
2. Added a known-good AutoEQ Favorite, then the HiFiGuides/Jaytiss and Reddit/LoboNautics community Favorites. Each action changed to `Remove favorite` and displayed `Saved to My EQs favorites.`
3. My EQs showed LoboNautics with `Database: Reddit`, Jaytiss with `Database: Hifiguides`, and the AutoEQ/Fahryst Favorite. The AFUL entries both showed `AFUL · Explorer`.
4. Ran `./tools/codex-android adb shell am force-stop com.weekssa.opraeqforuapp`, relaunched the same activity, and inspected the UI again. The same three Favorites and source/product identities remained.
5. Removed only the three Favorites created by this smoke. The UI returned to `0 headphone EQs · 0 General EQs` and the original empty saved list.

The UI was inspected with `./tools/codex-android android layout --device emulator-5554 --pretty` after navigation, save, restart, and cleanup. Navigation and Favorite actions used `./tools/codex-android adb shell input tap` and `input swipe`; search used `input text AFUL%sexplorer`. No catalog-update simulation was run because the maintained procedure above does not specify one. The AVD was shut down with `./tools/codex-android adb emu kill` after cleanup.

Result: **PASS**. Both affected community selections resolved after restart to the intended AFUL Explorer product and source; the unrelated Favorite remained intact. This is emulator evidence only.

## 2026-10-01 source-wide Favorite OPRA smoke

### Environment and initial state

- Candidate code head: `68ca6117230f4dab709be23b0a56cd66ee732dc7`, which includes current main.
- Temporary AVD: `medium_phone`, Android 16 / API 36, Google Play arm64 image, serial `emulator-5554`.
- `./tools/codex-android adb devices -l` showed only the temporary emulator; no physical or wireless device was connected.
- A fresh debug install showed an empty My EQs Favorites state.

### Actions and results

1. Opened EQ Library, searched `HIFIMAN edition XS`, and selected Edition XS.
2. Opened the Database filter and selected OPRA. The visible card was `oratory1990`, `Database: OPRA`, `Target: Harman`.
3. Tapped `Add favorite`. The card changed to `Remove favorite` and the app displayed `Saved to My EQs favorites.`
4. My EQs showed `oratory1990`, `Database: OPRA`, `Target: Harman`, and `HIFIMAN · Edition XS`.
5. Force-stopped and relaunched the app. The same OPRA Favorite and product identity remained.
6. Removed the test Favorite. My EQs returned to the initial empty state.

The screen state was inspected with `./tools/codex-android android layout --device emulator-5554 --pretty` after navigation, save, restart, and cleanup. The app was installed with `./tools/codex-android :app:installDebug`. No catalog refresh simulation was run. The temporary `medium_phone` AVD was stopped and removed; the existing `codex-api36` AVD was left unchanged. This is emulator software evidence only. It does not claim physical-device, DAC, acoustic, or parser qualification.
