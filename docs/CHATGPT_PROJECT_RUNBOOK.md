# OPRA EQ for UAPP — ChatGPT Project Runbook

This document is the maintained source of truth for work on **OPRA EQ for UAPP**. Product and UX decisions recorded here are authoritative unless the user explicitly changes them.

At the start of substantive work, read this file and then read the current architecture supplement:

- `docs/ARCHITECTURE.md`

If additional current architecture or UX documents are referenced here later, read those as well before implementation.

## 1. Repository boundary and authority

### Writable repository

The only repository that may be modified is:

`weekssa/OPRA-EQ-for-UAPP`

Before every repository write, confirm this exact repository is the target.

### Read-only behavioral reference

`weekssa/opra-uapp-converter`

This repository contains the proven OPRA → UAPP/ToneBoosters conversion behavior and is the behavioral reference for the Android/Kotlin port. Do not modify it unless the user explicitly changes that instruction.

### OPRA upstream and runtime catalog

- OPRA upstream: `https://github.com/opra-project/OPRA`
- Runtime OPRA catalog: `https://opra.roonlabs.net/database_v1.jsonl`

Normal app runtime must consume the runtime `database_v1.jsonl` catalog. Do not scrape GitHub during normal app operation.

## 2. Product identity and Android baseline

- App name: **OPRA EQ for UAPP**
- Android application ID / namespace: `com.weekssa.opraeqforuapp`
- Product type: standalone native Android app
- Primary test device: Pixel 9
- Minimum SDK: 26 unless a real validated technical reason requires a change
- Current stable compile/target baseline: Android 16 / API 36
- Prefer Kotlin, Jetpack Compose, clear UI/domain/data separation, Room, and WorkManager or current Android-recommended equivalents.

The app converts user-selected OPRA parametric EQ profiles into UAPP/ToneBoosters XML locally on the device. Do not bundle Python in the APK.

The current build-tool and dependency baseline is documented in `docs/ARCHITECTURE.md`; do not casually bump versions without checking current official compatibility requirements and validating CI.

## 3. Product principles, privacy, and scope

The app must ship with **ZERO headphones at install**.

Normal use requires no login/account, cloud backend, analytics, telemetry, ChatGPT, GitHub account, or Google Drive account. User selections and app preferences remain local. Preset conversion is local.

Do not introduce data collection, remote-account dependencies, or telemetry without an explicit product decision.

Do not download OPRA artwork by default in v1.

Network access in v1 is limited to what the product actually needs, principally:

- the runtime OPRA catalog;
- public app-release metadata / update links.

## 4. Phase gate and current status

### Phase 0 — complete and approved

Phase 0 design was explicitly approved as a whole on **2026-08-15**. All required UX areas have approved decisions in this runbook.

The approved Phase 0 UX remains authoritative during implementation. A major user-facing behavior that would materially change an approved decision must be explained to the user before implementation and requires a new explicit product decision.

### Phase 1 — active

Phase 1 implementation began on **2026-08-15**.

The first validated foundation slice now exists and includes:

- native Android project scaffold;
- app ID / minSdk / stable API baseline;
- Kotlin + Jetpack Compose app shell;
- approved My Headphones / Browse OPRA peer navigation shell;
- Settings secondary-screen behavior;
- persistent System / Light / Dark appearance preference;
- persistent three-way profile-visibility preferences;
- initial domain compatibility invariant for Not-compatible profiles;
- `CHANGELOG.md`;
- `docs/ARCHITECTURE.md`;
- Android CI running unit tests and building a debug APK.

At foundation commit `556ba9f769da31ce157654bba10f5a075707f219`, GitHub Actions validation passed `:app:testDebugUnitTest` and `:app:assembleDebug`.

The first foundation intentionally does **not** fabricate catalog data. Runtime catalog sync/cache, Room-managed headphone state, Kotlin conversion, export, WorkManager change checks, release checks, and production app-icon assets remain subsequent Phase 1 slices.

## 5. Runtime catalog, cache, and offline behavior

Normal operation consumes `https://opra.roonlabs.net/database_v1.jsonl`.

Required behavior:

1. Download the runtime OPRA catalog from `database_v1.jsonl`.
2. Validate a candidate catalog before promoting it to current state.
3. Cache the last known-good catalog locally.
4. Work offline after the first successful sync using that cached catalog.
5. Provide a user-initiated **Refresh** action.
6. Perform approximately daily background checks for catalog changes.
7. Keep the current cached catalog usable while refresh is in progress.
8. Never replace known-good cached state with a partial, malformed, or otherwise invalid candidate catalog.
9. Do not scrape GitHub during normal runtime.
10. Do not download OPRA artwork by default in v1.
11. Catalog comparison/update behavior must be deterministic and testable.

## 6. Approved Phase 0 UX decisions

All items in this section were approved on 2026-08-15 unless an amendment date is stated.

### 6.1 Overall navigation

- **My Headphones** and **Browse OPRA** are peer top-level destinations in bottom navigation.
- My Headphones is local management; Browse OPRA is discovery.
- Browse begins Manufacturer → Model, with deeper path levels only when OPRA source data genuinely establishes a required distinction.
- Changing profile selections while browsing does not automatically switch to My Headphones.
- Top app bar exposes **Refresh** and **Settings**.
- App-update information may appear as a non-blocking banner; permanent update information lives under **Settings → About & updates**.
- Bottom-navigation changes do not create an endlessly growing back stack.
- Back inside Browse unwinds one hierarchy/detail level at a time.
- Back from a My Headphones detail returns to its list.
- Back from Settings/secondary screens returns to the screen that opened them.
- At the root of either primary destination, system Back follows normal Android exit/background behavior rather than switching tabs.
- Preserve useful navigation/scroll state where practical.

### 6.2 First launch

- No onboarding wizard, account screen, tutorial carousel, attribution wall, or other blocking special first-launch destination.
- Open directly to **My Headphones**.
- It is genuinely empty because the app ships with zero headphones.
- Automatically begin the first runtime-catalog download.
- Empty state prominently offers **Browse OPRA** and may say **“Your selections stay on this device.”**
- If Browse opens before first catalog sync completes, show its normal loading state rather than redirecting elsewhere.
- After the first successful sync, future launches show cached local data immediately and can work offline.
- Do not request storage/folder access merely to browse or select; folder access belongs to export.
- Do not interrupt first use with update/changelog prompts.
- A first-download failure still leaves the normal app shell available and follows the approved loading/offline/error behavior below.

### 6.3 My Headphones

- Present managed headphones as a simple library grouped by manufacturer with models alphabetically ordered.
- Each row shows model name, a genuinely verified deeper OPRA distinction when required, and a concise selected-profile count.
- No OPRA artwork required/downloaded for this list in v1.
- Normal rows remain quiet; show concise attention text only when needed, e.g. **“1 profile updated,” “2 new OPRA profiles,” “1 no longer available in OPRA.”**
- New-profile attention is based on the user’s previously known local catalog state, not merely the latest network request.
- Opening a managed headphone shows its profiles and current selection state according to the future-profile rules below.
- Tapping a headphone opens detail/management with identity, selected count, status, and profile management.
- Selected profiles have an explicit **Remove** action; swipe-to-delete is not the primary deletion mechanism.
- Removing a profile requires explicit confirmation.
- Removing a whole headphone requires explicit confirmation.
- Removal confirmation asks whether to also delete corresponding preset files created by OPRA EQ for UAPP. File deletion is opt-in; **keep saved files is the default**.
- Delete only external files the app created and can still manage via valid retained Android document-tree access.
- If optional external-file deletion fails or access is lost, local removal still succeeds and the UI explains that the external file could not be removed.
- The first-launch empty state is reused whenever no headphones are managed.
- Back from headphone detail returns to the My Headphones list while preserving useful list state.

### 6.4 Browse OPRA

- Browse is a simple alphabetical Manufacturer → Model discovery flow.
- Manufacturer and model names come from OPRA source data.
- Never invent/reinterpret display names from IDs.
- No OPRA artwork in Browse for v1.
- Manufacturer opens alphabetic models.
- Model rows may show available-profile count.
- Managed models may show subdued status such as **“2 selected”** or **“In My Headphones.”**
- Tapping a model opens its EQ-profile destination.
- Selection changes do not auto-switch tabs.
- Preserve a deeper hierarchy only when source data genuinely establishes additional distinctions that must be represented.
- Never split an internal ID or filename and invent meanings such as Variant, Revision, Pad, etc.
- Never create an extra level merely because an internal ID/filesystem path contains extra text.
- Back unwinds from EQ profiles to a verified deeper level if one exists, otherwise Models, then Manufacturers.
- Retain useful Browse navigation/scroll state across tab switches.

### 6.5 Search

- Browse root has a visible **Search headphones…** field below the app bar.
- Search operates only on the locally cached catalog; no network request per keystroke.
- Search remains usable offline after initial sync.
- v1 search scope is manufacturer/vendor name plus headphone model/product name.
- Do not expose internal OPRA IDs in normal search.
- Do not include author, profile details, filter frequencies, or other EQ metadata in the primary Browse search.
- Matching is case-insensitive and tolerant of ordinary spacing/punctuation differences (for example `HD600` vs `HD 600`).
- Matching normalization is for search only; display original source names.
- Results are model-first and show manufacturer, model, profile count, and subdued selected state where relevant.
- Tapping a result opens that model’s EQ-profile destination.
- Clear restores the Manufacturer list.
- No-result state is brief.
- Never fall back to web/GitHub/remote search when local catalog has no match.
- Back while actively searching dismisses keyboard/search interaction as appropriate before normal root behavior.
- Results update when a successfully validated catalog refresh updates the cache.

### 6.6 Profile selection, Select all / Select none, and future profiles

- Every **selectable** usable OPRA parametric EQ profile has a checkbox.
- Profile rows show enough metadata to distinguish profiles, including creator/author and details when present.
- Provide **Select all** and **Select none** for current selectable profiles.
- Profile checkbox changes and the future-profile switch are staged until **Save changes**.
- If Save removes one or more currently selected profiles, require the approved removal confirmation and optional app-created preset-file deletion choice.
- Saving zero selected profiles is treated as removing the headphone and uses the whole-headphone removal confirmation.
- Back with unsaved changes offers **keep editing** or **discard**; never silently save or discard.
- **Automatically include new OPRA profiles for this headphone** defaults to **ON for every newly managed headphone**.
- The user may turn it OFF per headphone.
- Select all / Select none do not silently change the auto-inclusion setting.

Future-profile semantics:

- ON + all current selectable profiles selected: follow all current and future compatible profiles.
- ON + some current selectable profiles unchecked: preserve those exact profiles as explicit exclusions; automatically include future unrelated compatible profiles.
- OFF: fixed exact saved selection; future profiles can appear but are not silently selected.
- Explicit exclusions are persisted domain state, not inferred merely from selected counts.
- New profiles may show a subtle **New** marker based on previously known local catalog state.
- With auto-inclusion ON, a new compatible non-excluded profile appears selected.
- With auto-inclusion OFF, a new compatible profile appears unselected.

### 6.7 Compatibility outcomes

Every OPRA parametric EQ relevant to a headphone is classified into one of three user-facing outcomes:

1. **Fully compatible** — converts without a known UAPP/ToneBoosters limitation. Selectable and exportable. Normally no warning badge is needed.
2. **Compatible with limitation** — conversion is supported and exportable, but a meaningful known limitation applies. Selectable; the limitation is shown clearly.
3. **Not compatible** — the established conversion cannot represent the EQ faithfully enough to create a preset. Discoverable but never selectable/exportable while that limitation remains.

For **Not compatible** profiles:

- keep the OPRA profile discoverable so source content never silently disappears;
- show a disabled/uncheckable checkbox or equivalent disabled selection affordance;
- user cannot check it manually;
- **Select all** always skips it;
- automatic future-profile inclusion always skips it;
- it never counts as a selected/exportable preset;
- tapping the row opens an explanation of the exact incompatibility;
- a future app version may reevaluate it only if a proven, validated mapping is implemented.

Current reference-converter behavior maps:

- `peak_dip`;
- `low_shelf`;
- `high_shelf`.

Current OPRA filter types `low_pass`, `high_pass`, `band_pass`, and `band_stop` remain **Not compatible** unless/until a separately proven, validated UAPP/ToneBoosters mapping is established. Never approximate or silently drop them.

Missing/invalid acoustic information required for faithful conversion is **Not compatible**, rather than guessed, clamped, or silently changed. Preserve the reference converter’s no-silent-coercion behavior for established frequency/gain/Q limits.

Missing optional descriptive metadata is not the same as acoustic incompatibility:

- missing optional `details` or source link does not by itself make the EQ incompatible;
- harmless ISO-8859-1 display/name substitutions or compact display wording do not make the EQ incompatible because the original/full Unicode metadata is retained locally;
- documented OPRA defaults may be applied exactly as defined by OPRA and are not treated as missing-data errors;
- required creator/attribution data is a separate data-quality/attribution concern and must never be invented or silently discarded.

### 6.8 Profile visibility settings — amendment approved 2026-08-15

Settings includes **Profile visibility** with three independent options, all **ON by default**:

- **Fully compatible**
- **Compatible with limitation**
- **Not compatible**

These options control presentation only. They never mutate underlying compatibility, saved selection, explicit exclusions, auto-inclusion state, or export eligibility.

- Hiding Compatible with limitation must not silently deselect a limited profile already selected.
- Not compatible remains unselectable/unexportable whether shown or hidden.
- Select all / auto-inclusion use underlying compatibility rules, not merely what is visible.
- If filtering hides profiles for a headphone, disclose it with concise text such as **“2 OPRA profiles hidden by your compatibility filter.”**
- User can restore hidden categories in Settings.

### 6.9 Refresh and OPRA change reporting

Manual **Refresh**:

- downloads the runtime catalog;
- validates it;
- compares it with prior cached/local state;
- keeps the existing catalog usable while refreshing.

If no relevant managed-headphone changes exist, show a brief confirmation such as **“OPRA catalog is up to date.”**

If relevant changes exist, show a concise non-blocking summary such as **“3 of your headphones have changes”** with **Review**.

Prominent reporting is scoped to **My Headphones** rather than unrelated global catalog churn.

Approximately daily background checks perform the same comparison:

- no relevant changes → silent;
- relevant changes → no Android system notification / notification permission in v1; show a small non-blocking in-app banner on the next app open.

Relevant categories:

- new OPRA profiles for a managed headphone;
- selected OPRA profile updated;
- selected/retained profile no longer available in OPRA.

New profile:

- auto-inclusion ON → new compatible, non-excluded profile becomes selected and may show **New**;
- auto-inclusion OFF → new profile appears unselected and may show **New**.

Changed selected profile:

- regenerate deterministic local XML/generated state from the new OPRA data;
- report the change;
- do **not** silently rewrite an already-exported external preset in the background;
- mark an app-managed exported copy update-ready for the next explicit Export.

Changed unselected profile does not require prominent My Headphones attention.

Removed selected/generated OPRA profile:

- keep the last generated XML/local record;
- show **“No longer available in OPRA”**;
- never silently delete it;
- persist this status until explicit removal or upstream availability returns;
- removal uses the approved removal flow and optional saved-file deletion.

Review semantics:

- My Headphones may summarize multiple transient changes, e.g. **“2 new profiles · 1 updated.”**
- **New** and **Updated** mean not yet reviewed for that headphone, not simply “since the last network refresh.”
- Re-refreshing must not clear unreviewed New/Updated state.
- Opening/reviewing the affected headphone clears transient reviewed markers for what was reviewed.
- **No longer available in OPRA** does not clear merely by viewing.
- Catalog refresh state and reviewed/unreviewed state are distinct domain concepts.

Failures:

- failed manual refresh keeps cached/local state and may say **“Couldn’t refresh OPRA. Using your saved catalog.”**
- failed background check is quiet/noninterrupting and leaves known-good state intact.

### 6.10 Export

- My Headphones has a clear **Export presets** action for all currently selected profiles across managed headphones.
- An individual headphone may also offer **Export this headphone**.
- First export uses Android’s system folder/document picker.
- Suggest `Documents/OPRA EQ for UAPP/Presets` where the platform allows, but the user chooses the actual destination.
- Persist supported directory access while valid.
- Do not request broad storage permission.
- Do not write into UAPP/another app’s private storage.
- Folder hierarchy begins `Manufacturer/Model/`, with deeper folders only for genuinely verified OPRA distinctions.
- Generated XML filename and embedded ToneBoosters preset name use the same deterministic headphone-first naming rule: `Model [Variant] - Creator - Details`.
- Export is incremental and may summarize new, updated, and already-current presets where practical.
- Track which external files the app created so only app-managed files can be safely updated/deleted.
- Newly auto-included profiles may generate local state when discovered but are written externally only on explicit Export.
- Changed selected profiles regenerate locally and mark exported copies update-ready; background refresh never silently rewrites external user-folder files.
- Next explicit Export replaces an app-managed exported copy with the regenerated version.
- Removed-upstream profiles retain last generated XML; existing exported preset is not automatically deleted.
- If the user removes/deselects a profile but chooses to keep its saved preset, future exports leave that retained external file alone.
- If the deterministic filename conflicts with a file the app cannot establish it manages, **do not overwrite**, **do not append `(2)`**, and **do not mutate the deterministic name**. Report a conflict/review path.
- Lost retained folder access is explained and the user can reselect a folder; never escalate to broad storage permission.
- My Headphones may show status like **“2 presets ready to export”** or **“All selected presets exported.”**
- External user-folder mutation happens only when the user explicitly presses Export or explicitly opts into an app-created-file deletion during removal.
- Export includes selected Fully compatible and Compatible-with-limitation profiles.
- Not-compatible profiles are never exported.
- Export summaries never count a failed or Not-compatible profile as successfully exported.
- Independent successful file writes remain successful if a different independent file fails.

### 6.11 Settings / About

Keep Settings sparse and focused.

Primary areas include:

- Presets / export folder;
- Profile visibility;
- Appearance;
- OPRA catalog;
- About / privacy / credits / updates.

Export folder:

- before first export: show **Not chosen yet**;
- after selection: show current destination and a **Change folder** action;
- Change folder opens Android’s system picker again;
- changing destination applies to future exports and does not automatically move/delete files from the previous folder.

OPRA catalog status:

- show last successful refresh where available;
- explain approximate daily background checks;
- provide **Refresh now**;
- no background-refresh toggle in v1.

Do not add in v1:

- account section;
- cloud/sync configuration;
- GitHub credentials;
- Google Drive setup;
- editable OPRA URL/server setting;
- developer options;
- broad-storage-permission page;
- large collections of cosmetic switches.

Privacy page explains:

- selections and app settings stay on device;
- conversion is local;
- no account;
- no analytics/telemetry;
- network use needed for runtime catalog and public release metadata.

No blocking privacy/legal wall on first launch.

Credits & licenses:

- clearly credit OPRA;
- preserve/credit individual EQ creators/authors and sources;
- include relevant open-source license information;
- state no endorsement by OPRA, Roon Labs, USB Audio Player PRO/UAPP, or ToneBoosters.

Permanent app/version/update destination is **Settings → About & updates**.

### 6.12 Appearance — amendment approved 2026-08-15

Settings includes **Appearance → Theme** with exactly three normal choices:

- **System default** — default; follow the Android device appearance and change when system theme changes.
- **Light** — keep app light regardless of system theme.
- **Dark** — keep app dark regardless of system theme.

The preference remains local on the device.

Theme choice affects presentation only and must never change compatibility, selection, conversion, export, refresh, catalog, or other domain behavior.

Light/dark appearance applies consistently across:

- My Headphones;
- Browse OPRA;
- search;
- profile management/compatibility;
- Settings;
- dialogs;
- banners;
- loading/offline/error states;
- export results;
- What’s new/update surfaces.

Both appearances must preserve readable contrast and the rule that important status is communicated with text/semantics, not color alone.

### 6.13 App updates and What’s new / changelog

- Initial distribution uses GitHub Releases.
- End users must not need a GitHub account.
- The app checks public release metadata at a modest cadence; do not hammer GitHub on screen changes.
- v1 has one normal release channel; do not expose stable/beta channel settings or normally offer prerelease/test builds.
- A newer applicable version shows a small non-blocking in-app update banner with version plus **What’s new** / **Get update**.
- Banner may be dismissed for that release; the update remains discoverable in Settings → About & updates.
- A later newer release may surface again.
- About & updates shows installed version and either up-to-date state or available newer version plus **What’s new**, **Check for update**, and **Get update** as applicable.
- **What’s new** shows friendly release notes grouped by version rather than raw Git history.
- Repository `CHANGELOG.md` is the durable changelog source of truth from Phase 1 onward; GitHub Release notes should correspond to it.
- `CHANGELOG.md` was intentionally not created during Phase 0 and was created at the start of Phase 1. Maintain it going forward.
- On first launch after an installed app-version change, a small one-time non-blocking **Updated to vX.Y.Z** card may offer What’s new / Dismiss.
- Do not show that card on first-ever installation.
- Once dismissed, do not repeat it for the same installed version.
- Automatic update-check failure is quiet.
- Manual Check for update failure shows a concise retry-later message.
- If a background check finds an update while the app is not active, surface it on next app open; no notification permission in v1.
- **Get update** opens the specific public GitHub Release in the browser.
- Do not silently download APKs, self-update, silently install, request “install unknown apps” permission in v1, or launch a hidden installer.
- No forced updates.
- SemVer: development `0.x`; first stable release `v1.0.0`.
- Release-readiness requirement: the metadata/download destination used by end users must be publicly accessible without GitHub authentication. The repository is currently private; this must be resolved before public distribution without introducing end-user GitHub credentials.

### 6.14 Loading, offline, and error states

General rule: **OPRA EQ for UAPP is a local-first app that occasionally synchronizes OPRA.** If valid local data exists, keep showing and using it. A failed network/catalog/conversion/export operation must be contained to the operation that failed and must not silently destroy known-good user state.

First catalog load:

- My Headphones still renders its normal empty shell with a simple indeterminate **Downloading OPRA catalog…** state.
- Browse before first catalog arrives shows indeterminate **Loading OPRA catalog…**.
- Do not invent fake percentages.

Normal launch after first sync:

- show cached catalog immediately;
- background freshness work must not blank/block the UI.

Offline with a valid cache:

- supported operating mode, not an app-wide error;
- My Headphones, Browse, local search, profile management, local conversion, and export continue where they do not otherwise need network access;
- a small non-blocking status may say **“Offline · Using saved OPRA catalog.”**
- Settings catalog status can show last successful catalog time and offer Try refresh.
- only genuinely network-dependent actions should report offline inability.

First-ever launch with no catalog/network:

- My Headphones still renders its shell and explains first catalog could not be downloaded with **Try again**;
- Browse explains the catalog has not been downloaded yet and offers **Try again**;
- do not ship/fabricate a fallback headphone catalog.

Catalog validation failure:

- reject an invalid new candidate as a whole;
- preserve prior good cache/local state;
- with prior cache, explain **“Couldn’t use the new OPRA catalog. Your previous saved catalog is still available.”**
- with no prior valid catalog, explain the downloaded catalog could not be processed and offer retry;
- do not show raw parser stack traces to normal v1 users.

Conversion presentation:

- Fully compatible: normally no warning.
- Compatible with limitation: selectable/exportable; explain limitation.
- Not compatible: disabled/uncheckable and never exported; tapping row explains exact reason.
- Harmless missing optional descriptive metadata is not presented as an alarming conversion failure when acoustic EQ is intact.
- Never silently approximate, clamp, omit unsupported acoustic behavior, or claim success for a Not-compatible profile.

Export/cleanup failures:

- per-file resilience where technically safe;
- partial result can say **“8 presets saved · 1 preset could not be exported”** with Review;
- never count failed/Not-compatible as successful;
- Review explains exact issue such as lost folder access or non-managed filename conflict;
- if local removal succeeds but optional external deletion fails, local removal remains successful and the cleanup failure is explained.

Background failures/severity/retry:

- transient background catalog/update-check failures stay quiet unless materially affecting usable state;
- informational presentation for loading/refreshing/offline-with-cache;
- attention presentation for update available/partial export/relevant OPRA changes;
- stronger action-required error for no initial catalog, lost export access, or an inspected Not-compatible reason;
- avoid alarming full-screen red errors for ordinary recoverable network failures;
- every visible recoverable network failure offers straightforward Try again / Refresh;
- retry never clears known-good local state first.

### 6.15 Accessibility — approved and amended 2026-08-15

Accessibility is part of core design, not a final cosmetic pass.

Use standard Android/Compose controls and semantics where practical, ensuring the same meaningful information/actions are available visually, through TalkBack, with large text, in both light/dark appearances, and for users with limited precision.

- Interactive controls/useful tap areas should meet Android recommended touch targets, including at least 48 × 48 dp.
- Profile rows expose creator/details, compatibility outcome, selected/unselected state, disabled state, and control role as appropriate.
- Not-compatible profile stays discoverable to TalkBack; its selection control is disabled, while its explanatory row remains focusable/actionable to open the reason.
- Selectable profile rows should provide a comfortable selection tap area rather than requiring precise checkbox contact.
- Not-compatible rows never toggle selection.
- Do not communicate New, Updated, Compatible with limitation, Not compatible, No longer available in OPRA, offline status, export readiness, errors, or other important state through color/icon alone.
- Respect Android font/text scaling.
- Avoid rigid row heights that clip important identity/warning/error/action text.
- Prefer wrapping/reflowing important information over ellipsis where truncation would hide meaning.
- Icon-only controls such as Refresh and Settings have meaningful accessibility labels.
- Decorative visuals do not create unnecessary TalkBack stops.
- Focus/traversal order follows conceptual screen order.
- Destructive dialogs use explicit labels such as **Remove profiles**, **Keep editing**, **Discard**, not ambiguous Yes/No.
- Optional preset-file deletion is a separately understandable unchecked/default-off choice.
- Do not unnecessarily place initial accessibility focus on a destructive action.
- User-triggered Refresh announces meaningful transitions without repetitive chatter.
- Quiet background checks remain quiet unless they produce user-relevant information.
- Profile visibility Settings expose understandable labels plus checked state.
- Appearance Settings expose System default / Light / Dark plus selected state.
- Bottom navigation exposes destination names and selected state.
- Browse tab affordance must not be confused with the separate headphone Search field.
- No important function may require swipe, long press, precise drag, or hidden gesture. Such gestures may exist only as optional shortcuts with an equivalent visible accessible action.

Phase 1 accessibility validation on Pixel 9 includes:

- manual TalkBack;
- large system text/font scaling;
- logical focus order;
- touch-target checks;
- disabled Not-compatible behavior;
- compatibility-state announcements;
- dialogs;
- profile visibility;
- Light / Dark / System-follow appearance;
- reasonable contrast/color-independence checks.

Automated accessibility checks may supplement but do not replace manual validation.

Regression coverage must specifically protect the Not-compatible invariant: visible when configured to show, TalkBack-discoverable, reason accessible, selection disabled, Select all skips it, automatic inclusion skips it, export skips it.

### 6.16 App icon direction

Three original concepts were reviewed. Selected direction: **Concept B — Equalizer Headphones**.

- Original headphone/headband form surrounding three simplified EQ-control/slider elements.
- Outer geometry should also read as earcups so the mark conveys headphones + EQ/control without text.
- Keep geometry simple and recognizable at small launcher sizes.
- Design as an Android adaptive icon with generous safe-zone margins.
- No text, initials, model names, or lettering inside the icon.
- Do not copy/incorporate OPRA, Roon Labs, UAPP/USB Audio Player PRO, or ToneBoosters brand marks/logos.
- The selected concept is the approved direction; final production geometry, palette, adaptive foreground/background layers, monochrome/themed-icon treatment, and asset sizing are implementation details to validate later without changing the concept.
- App-icon artwork is unrelated to the rule against downloading OPRA headphone artwork.

## 7. Selection, compatibility, catalog, and export domain state

Domain state must be able to represent at least:

- currently selected profile identities;
- automatic future-profile inclusion ON/OFF;
- explicit exclusions while auto-inclusion is ON;
- compatibility outcome per relevant OPRA profile;
- compatibility-visibility settings independent of compatibility/selection;
- previously known profile identities needed for New detection;
- reviewed/unreviewed transient New/Updated state;
- retained/removal state for profiles no longer upstream;
- generated local XML/state;
- app-managed external-file ownership/state for incremental export;
- whether an exported copy is current/update-ready/retained;
- last known-good catalog separate from an unvalidated refresh candidate;
- persisted export-directory access where supported;
- local appearance preference.

These are domain/data behaviors, not merely presentation logic. They must be deterministic and tested.

## 8. Conversion requirements

Implement conversion natively in Kotlin and treat `weekssa/opra-uapp-converter` as the read-only behavioral reference.

Preserve OPRA:

- overall gain/preamp;
- frequency;
- gain;
- Q;
- filter/band priority and order;
- creator/author;
- details;
- source/attribution metadata.

Do not silently alter OPRA acoustic values. Do not silently ignore unsupported filters.

### Compatibility and established limits

- Fully compatible profiles preserve established supported behavior and are exportable.
- Compatible-with-limitation profiles are exportable only when an explicit deterministic validated rule exists for the limitation.
- Not-compatible profiles never produce an XML preset until a proven mapping exists.
- Current reference mapping supports `peak_dip`, `low_shelf`, `high_shelf`.
- Current OPRA `low_pass`, `high_pass`, `band_pass`, `band_stop` remain Not compatible absent a future proven mapping.
- Preserve the reference converter’s behavior of rejecting required values outside its established ToneBoosters/UAPP frequency, gain, and Q ranges rather than silently clamping/coercing them.
- Missing optional descriptive metadata does not itself change acoustic compatibility.
- Follow documented OPRA defaults exactly when applicable; do not invent undocumented defaults.

### ToneBoosters/UAPP 10-band limit

When a profile contains more than 10 applicable bands:

1. preserve OPRA priority/order;
2. use the first 10 according to OPRA priority;
3. classify as **Compatible with limitation**;
4. clearly warn that lower-priority bands cannot be included;
5. cover this behavior with golden fixtures.

### Naming and encoding

Use deterministic headphone-first preset names:

`Model [Variant] - Creator - Details`

Use a variant/deeper distinction only when source data genuinely verifies it.

ToneBoosters XML must be ISO-8859-1-safe while full Unicode/original metadata remains retained locally.

Harmless display substitutions/compacting do not alter stored OPRA source metadata.

Identical source data + selection/conversion state must produce deterministic output.

## 9. Testing strategy

Treat the Python converter as the behavioral reference. Build golden fixtures and require Kotlin parity/coverage for at least:

- normalization;
- preamp;
- supported filters;
- deterministic XML;
- 10-band handling + Compatible-with-limitation classification;
- unsupported filters + Not-compatible classification;
- missing/invalid required acoustic values;
- optional metadata and documented-default handling;
- preset naming;
- ISO-8859-1-safe XML/export encoding;
- full Unicode local metadata retention;
- selection modes;
- auto future-profile inclusion;
- explicit exclusions;
- fixed exact selections;
- Not-compatible disabled/uncheckable behavior;
- Select all skipping Not-compatible;
- auto-inclusion skipping Not-compatible;
- visibility filters not mutating selection/compatibility;
- hidden-profile disclosure;
- appearance preference persistence and System/Light/Dark behavior without domain mutation;
- catalog parsing/validation;
- last-known-good cache retention after network/parse/validation failures;
- offline behavior after first successful sync;
- catalog update comparison;
- New/reviewed state;
- changed selected profiles;
- removed profiles;
- incremental export;
- managed external-file ownership;
- filename conflicts;
- retained external files;
- lost folder access;
- partial export failures;
- accessibility-critical compatibility behavior and layout regressions where practical.

Never weaken validation merely to make tests pass. If Kotlin and the reference behavior differ, understand and resolve the difference rather than relaxing checks without a justified product decision.

CI should keep running unit tests and a debug APK build for relevant changes.

## 10. Architecture and implementation boundaries

`docs/ARCHITECTURE.md` is the current architecture supplement and must be read after this runbook for substantive implementation work.

Keep clear boundaries:

- **UI**: Compose presentation, navigation, accessibility semantics, user interaction.
- **Domain**: selection rules, compatibility, conversion behavior, catalog-change semantics, naming, export decisions.
- **Data**: runtime catalog acquisition/cache, Room persistence, repository implementations, mapping.
- **Platform integrations**: Storage Access Framework and WorkManager behind narrow interfaces rather than leaking platform behavior into conversion/domain logic.

Current planned Phase 1 slices are documented in `docs/ARCHITECTURE.md`. Work in focused validated increments rather than trying to implement the entire app in one change.

## 11. Android storage/export platform requirements

Use Android’s system folder/document picker rather than broad storage permissions.

Suggest `Documents/OPRA EQ for UAPP/Presets` where useful, let the user choose, and persist supported directory access.

Manage only files created by OPRA EQ for UAPP.

Folder layout begins `Manufacturer/Model/` and may add deeper verified OPRA path segments only when genuinely needed.

Background catalog updates may regenerate local state, but already-exported external preset files are rewritten only through explicit Export.

## 12. Upstream OPRA changes

### Changed selected profile

Detect it, regenerate deterministic local XML, report it, mark any app-managed exported copy update-ready rather than silently modifying external storage, and update that external copy on the next explicit Export.

### Removed OPRA profile

Keep last generated XML/local record, mark **“No longer available in OPRA,”** do not delete automatically, allow explicit user removal, and delete an exported app-created preset only if the user explicitly opts into deletion during removal.

## 13. App updates and changelog architecture

The approved update UX in section 6 is authoritative.

`CHANGELOG.md` was created at Phase 1 start and is now required to be maintained as the durable project changelog.

Initial distribution is through GitHub Releases. v1 uses manual user-driven updates and no notification or APK-install permission.

## 14. Security and signing

Never commit signing keys, passwords, tokens, credentials, or other secrets.

When release signing is intentionally introduced later, use one stable release-signing identity.

Do not add signing secrets to source control or GitHub workflow files.

## 15. Attribution and product claims

Follow OPRA attribution requirements.

Clearly credit:

- OPRA;
- individual EQ creators/authors;
- relevant sources.

Do not imply endorsement by OPRA, Roon Labs, UAPP/USB Audio Player PRO, or ToneBoosters.

Preserve attribution through conversion/export where supported while retaining richer/full metadata locally when necessary.

## 16. Communication and implementation workflow

For substantive work:

1. Read this runbook first.
2. Read `docs/ARCHITECTURE.md` and any additional current architecture/UX docs referenced here.
3. Confirm the writable repository is exactly `weekssa/OPRA-EQ-for-UAPP` before every write.
4. Treat `weekssa/opra-uapp-converter` as read-only reference unless the user explicitly changes that instruction.
5. Use connected GitHub tools directly whenever possible; give manual Git/Terminal steps only when truly required.
6. Preserve approved Phase 0 behavior unless a material UX change is first explained and explicitly approved.
7. Build in clear Phase 1 slices and validate each slice before depending on it.
8. After changes, state exactly what changed and whether relevant validation passed.
9. Ask only questions that materially affect the product.
10. Update this runbook and/or its referenced architecture documents when maintained source-of-truth state changes.

## 17. Current project state

**Phase 0 is complete and explicitly approved. Phase 1 is active.**

The validated Phase 1 foundation exists. Current implemented foundation includes the Android project, app shell, local appearance/profile-visibility preferences, initial compatibility invariant, changelog, architecture document, and Android CI.

The next planned focused slice is the runtime OPRA `database_v1.jsonl` client with validation, last-known-good local cache, and local Browse/Search catalog model. Do not fabricate bundled headphone data while building it.

Subsequent slices include Room-managed headphone/selection state, Kotlin converter parity, refresh/change reporting + WorkManager, SAF export, public release checks/changelog presentation, and final production icon/accessibility/release hardening.
