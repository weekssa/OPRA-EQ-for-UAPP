# Pixel 9 device validation — OPRA EQ for UAPP

This is the hands-on validation gate after automated CI is green. It is intentionally short and user-oriented; automated unit/lint/build checks remain in GitHub Actions.

## Current device-test progress — 2026-08-15

The user has reported that the functional Pixel 9 checks exercised so far are passing. Confirmed successful end-to-end behavior includes installing/running the debug build, runtime OPRA browsing/search, adding/updating My Headphones from Browse, exporting generated XML through Android's folder picker, and importing the generated presets successfully into USB Audio Player PRO/ToneBoosters.

During device testing, the first-add/My Headphones membership bug was found and corrected. A never-managed headphone is now directly addable with the approved default selections, and per-headphone **Export XMLs** persists/updates the same My Headphones record before export. The corrected build passed the automated unit-test, Android-lint, and debug-APK build gate before retesting.

The user subsequently reported that the additional functional checks performed so far are also passing. Do not mark the overall device gate complete until the remaining hands-on appearance/accessibility and Settings/About/privacy/attribution checks below have been explicitly exercised. OPRA-change cases that require a naturally occurring upstream change may remain covered primarily by deterministic automated regression tests unless a practical device fixture is introduced.

## Install and first launch

1. Install the latest `opra-eq-for-uapp-debug` APK artifact from the successful Android CI run.
2. Launch the app on the Pixel 9.
3. Confirm it opens directly to **My Headphones** with zero managed headphones and no onboarding/account/storage-permission wall.
4. Confirm the first OPRA catalog download starts automatically and Browse becomes usable after it completes.
5. Close/reopen the app and confirm saved catalog data appears immediately.
6. With network unavailable, confirm Browse/Search and already-managed local presets remain usable and Refresh fails without clearing saved data.

## Browse, search, and selection

1. Search for several known manufacturers/models, including spacing/punctuation variations.
2. Open a never-managed headphone and confirm every currently selectable profile starts checked and **Automatically include new OPRA profiles** is ON.
3. Confirm **Not compatible** profiles are visible by default, unchecked, disabled, skipped by Select all, and tappable for an explanation.
4. Confirm **Compatible with limitation** profiles remain selectable and clearly explain the 10-band limitation when applicable.
5. Confirm a profile with missing author displays **Creator information missing** but remains selectable when its EQ data is otherwise compatible.
6. Uncheck one compatible profile with auto-inclusion ON, Save, reopen it, and confirm that exact exclusion persists.
7. Turn auto-inclusion OFF, Save, reopen, and confirm the saved exact selection persists.
8. Change compatibility visibility settings and confirm hidden categories disappear without changing underlying saved selection.

## My Headphones and OPRA changes

1. Confirm managed headphones are grouped by manufacturer and selected counts are correct.
2. Open a managed headphone and confirm its profile state, auto-inclusion setting, compatibility labels, and retained OPRA metadata are understandable.
3. Confirm opening a headphone clears transient New/Updated attention after review, while **No longer available in OPRA** persists.
4. Confirm a removed-upstream retained profile can be explicitly removed.
5. Confirm a previously selected profile that became Not compatible is disabled/unselected while its last generated preset is retained and explicitly removable.
6. Confirm removing a profile/headphone offers **Also remove saved preset files created by OPRA EQ for UAPP** unchecked by default.

## Export and UAPP/ToneBoosters

1. Press **Export presets**. Confirm Android’s system folder picker opens and the app explains the suggested `Documents/OPRA EQ for UAPP/Presets` location without requiring it.
2. Select a folder and confirm Manufacturer/Model subfolders plus deterministic XML filenames are created.
3. Export again and confirm already-current files are not duplicated.
4. Change a selected OPRA profile through a test/catalog update if available, then confirm a later explicit Export updates the app-managed file rather than background-writing it.
5. Place an unrelated file with a conflicting deterministic name and confirm the app reports a conflict rather than overwriting it or creating `(2)`.
6. Remove a profile while keeping its saved file; confirm future exports leave that retained file alone.
7. Repeat removal with saved-file cleanup enabled and confirm only app-owned files are deleted.
8. Import representative generated XML into USB Audio Player PRO/ToneBoosters and verify the preset loads and the displayed bands/preamp match the selected OPRA profile, including one >10-band limited profile.
9. Verify an exported missing-author profile uses **Creator information missing** in the creator slot rather than inventing a person/source.

## Refresh and background behavior

1. Manual Refresh with working network: confirm current catalog remains usable and completion is nonblocking.
2. Manual Refresh without network: confirm saved catalog remains intact.
3. If a managed-headphone change is available, confirm My Headphones reports it and Review shows the affected profile.
4. Leave the app installed long enough for WorkManager’s roughly daily check and confirm it does not request notification permission or generate a system notification.

## Appearance and accessibility

1. Test **System default**, **Light**, and **Dark** themes.
2. Enable Android themed icons and confirm the monochrome launcher icon remains recognizable as headphones + EQ controls.
3. Test large font/display sizes and confirm important manufacturer/model/creator/details/warning/error/action text remains readable without meaningful clipping.
4. Enable TalkBack and verify bottom navigation, Refresh, Settings, profile checkbox state, compatibility state, disabled Not-compatible profiles, dialogs, update banners, and export actions have useful spoken labels/states.
5. Confirm Not-compatible profiles remain TalkBack-discoverable and their reason can be opened even though selection is disabled.
6. Confirm important state is understandable without relying on color alone.

## Updates, privacy, and attribution

1. Open Settings → About & updates and verify installed version/update actions are understandable; a failed public check while the repository is private should be non-destructive.
2. Verify Browse and Settings show OPRA attribution and the official OPRA logo, and individual profile creators remain prominent where provided.
3. Verify privacy text accurately states local selections/conversion, no account, no analytics/telemetry, and network use for the OPRA catalog/public release metadata.
4. Verify the non-endorsement statement is visible in Credits & licenses.

## Pass condition

Device validation passes when there are no blocking functional, data-loss, conversion, export, accessibility, or misleading-attribution issues. Any failure should be recorded with the exact screen/action, expected result, actual result, and—where useful—a screenshot before release/signing work begins.
