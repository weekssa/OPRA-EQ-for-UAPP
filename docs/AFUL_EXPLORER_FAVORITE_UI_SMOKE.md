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
