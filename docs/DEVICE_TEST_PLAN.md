# Pixel 9 device validation — OPRA EQ for UAPP

This is the hands-on validation gate after automated CI is green. Automated unit/lint/build checks remain in GitHub Actions.

## Device validation status — PASSED 2026-08-15

Pixel 9 hands-on validation is complete for the current development build. The user confirmed successful end-to-end behavior for first launch and initial OPRA catalog acquisition, offline reuse, Browse/Search, My Headphones management, selection persistence, profile export through Android's folder picker, repeated export behavior, optional app-owned file cleanup, appearance/accessibility checks, Settings/About/privacy/attribution presentation, and importing generated XML successfully into USB Audio Player PRO/ToneBoosters.

During device testing, three issues were found and corrected before the gate was closed:

1. A never-managed headphone initially showed the approved default profiles selected but did not allow the user to add it until an artificial checkbox change was made. The corrected flow immediately enables **Add to My Headphones**, and per-headphone **Export XMLs** persists/updates that same My Headphones record before export.
2. Optional saved-preset cleanup initially appeared not to remove files. Cleanup was corrected to use the persisted Storage Access Framework tree grant and to attempt deletion before local managed-state removal. A fresh-folder retest with files created by the currently installed build confirmed that ownership-tracked app-created XML files are deleted successfully. Files orphaned by a debug-app uninstall remain deliberately untouched because the app can no longer prove ownership.
3. A fresh install could initially show **“The OPRA catalog couldn’t be saved on this device.”** and then succeed immediately on Retry. The root cause was a race between the foreground first catalog download and an immediately eligible WorkManager periodic sync sharing the same candidate cache file. The fix delays the first periodic run by roughly 24 hours and serializes catalog refresh/promotion across repository instances. The final fresh-state Pixel 9 retest passed without requiring Retry.

The full automated gate was green after the final catalog-race fix: unit tests, Android lint, debug APK assembly, and artifact upload all passed. The remaining prerequisites before public distribution are release signing, release/distribution visibility, final version/release notes, and a signed release build—not additional functional Pixel 9 testing unless release-build behavior materially differs.

## Install and first launch — validated

1. Install the latest `opra-eq-for-uapp-debug` APK artifact from the successful Android CI run.
2. Launch the app on the Pixel 9.
3. Confirm it opens directly to **My Headphones** with zero managed headphones and no onboarding/account/storage-permission wall.
4. Confirm the first OPRA catalog download starts automatically and Browse becomes usable after it completes without requiring Retry.
5. Close/reopen the app and confirm saved catalog data appears immediately.
6. With network unavailable, confirm Browse/Search and already-managed local presets remain usable and Refresh fails without clearing saved data.

## Browse, search, and selection — validated

1. Search for several known manufacturers/models, including spacing/punctuation variations.
2. Open a never-managed headphone and confirm every currently selectable profile starts checked and **Automatically include new OPRA profiles** is ON.
3. Confirm **Not compatible** profiles are visible by default, unchecked, disabled, skipped by Select all, and tappable for an explanation.
4. Confirm **Compatible with limitation** profiles remain selectable and clearly explain the 10-band limitation when applicable.
5. Confirm a profile with missing author displays **Creator information missing** but remains selectable when its EQ data is otherwise compatible.
6. Uncheck one compatible profile with auto-inclusion ON, Save, reopen it, and confirm that exact exclusion persists.
7. Turn auto-inclusion OFF, Save, reopen, and confirm the saved exact selection persists.
8. Change compatibility visibility settings and confirm hidden categories disappear without changing underlying saved selection.

## My Headphones and OPRA changes — validated where practical on device

1. Confirm managed headphones are grouped by manufacturer and selected counts are correct.
2. Open a managed headphone and confirm its profile state, auto-inclusion setting, compatibility labels, and retained OPRA metadata are understandable.
3. Confirm opening a headphone clears transient New/Updated attention after review, while **No longer available in OPRA** persists.
4. Confirm a removed-upstream retained profile can be explicitly removed when such a state is available.
5. Confirm a previously selected profile that became Not compatible is disabled/unselected while its last generated preset is retained and explicitly removable when such a state is available.
6. Confirm removing a profile/headphone offers **Also remove saved preset files created by OPRA EQ for UAPP** unchecked by default.

Naturally occurring upstream-change cases that were not practical to manufacture on-device remain protected by deterministic automated reconciliation tests and must not be weakened merely to obtain a manual fixture.

## Export and UAPP/ToneBoosters — validated

1. Press **Export presets**. Confirm Android’s system folder picker opens and the app explains the suggested `Documents/OPRA EQ for UAPP/Presets` location without requiring it.
2. Select a folder and confirm Manufacturer/Model subfolders plus deterministic XML filenames are created.
3. Export again and confirm already-current files are not duplicated.
4. Confirm later explicit Export updates an app-managed file when its selected OPRA profile changes rather than background-writing it.
5. Confirm an unrelated same-name file is treated as a conflict rather than overwritten or renamed to `(2)`.
6. Remove a profile while keeping its saved file and confirm future exports leave that retained file alone.
7. Repeat removal with saved-file cleanup enabled and confirm ownership-tracked app-created files are deleted.
8. Import representative generated XML into USB Audio Player PRO/ToneBoosters and verify the preset loads successfully.
9. Verify exported missing-author profiles use **Creator information missing** in the creator slot rather than inventing a person/source.

## Refresh and background behavior — validated

1. Manual Refresh with working network keeps the current catalog usable and completes nonblockingly.
2. Manual Refresh without network keeps saved catalog state intact.
3. Managed-headphone change reporting remains covered by both device-visible flows where available and deterministic reconciliation tests.
4. Approximately daily WorkManager checking requires no notification permission and does not generate a system notification in v1. The first periodic run is intentionally delayed so it cannot race the foreground first-catalog acquisition.

## Appearance and accessibility — validated

1. **System default**, **Light**, and **Dark** app appearance modes are usable.
2. Android themed icon presentation remains recognizable.
3. Large font/display sizes keep important manufacturer/model/creator/details/warning/error/action text usable.
4. TalkBack can navigate bottom navigation, Refresh, Settings, profile checkbox state, compatibility state, dialogs, and export actions with useful labels/states.
5. Not-compatible profiles remain discoverable and their reason can be opened even though selection is disabled.
6. Important state is understandable without relying on color alone.

## Updates, privacy, and attribution — validated

1. Settings → About & updates is understandable and a failed public-release check while the repository is private is non-destructive.
2. Browse and Settings show OPRA attribution and the official OPRA logo, with individual profile creators prominent where provided.
3. Privacy text accurately describes local selections/conversion, no account, no analytics/telemetry, and network use for the OPRA catalog/public release metadata.
4. Credits & licenses includes the non-endorsement statement.

## Pass condition

**PASSED on Pixel 9, 2026-08-15.** No blocking functional, data-loss, conversion, export, accessibility, or misleading-attribution issue remains from the exercised device-validation scope. Any future release-build-only regression must be recorded and resolved before public release.