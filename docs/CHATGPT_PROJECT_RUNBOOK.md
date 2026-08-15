# OPRA EQ for UAPP — ChatGPT Project Runbook

This document is the maintained source of truth for work on **OPRA EQ for UAPP**.

## 1. Repository boundary and authority

### Writable repository

The only repository that may be modified is:

`weekssa/OPRA-EQ-for-UAPP`

Before every repository write, confirm this exact repository is the target.

### Read-only behavioral reference

`weekssa/opra-uapp-converter`

This repository contains proven OPRA → UAPP/ToneBoosters conversion behavior and is the behavioral reference for the Android/Kotlin port. Do not modify it unless the user explicitly changes that instruction.

### OPRA upstream and runtime catalog

- OPRA upstream: `https://github.com/opra-project/OPRA`
- Runtime OPRA catalog: `https://opra.roonlabs.net/database_v1.jsonl`

Normal app operation must use the runtime `database_v1.jsonl` catalog. Do not scrape GitHub during normal app runtime.

## 2. Product identity

- App name: **OPRA EQ for UAPP**
- Android application ID: `com.weekssa.opraeqforuapp`
- Product type: standalone native Android app
- Primary test device: Pixel 9
- Preferred minimum SDK: 26 unless a real technical reason requires a change

The app converts user-selected OPRA parametric EQ profiles into UAPP/ToneBoosters XML locally on the device.

## 3. Product principles and privacy

The app must ship with **ZERO headphones at install**.

Normal use requires no:

- login or account;
- cloud backend;
- analytics;
- telemetry;
- ChatGPT;
- GitHub account;
- Google Drive account.

User selections remain local. Conversion is local. Do not introduce data collection or remote-account dependencies without an explicit product decision.

Do not download OPRA artwork by default in v1.

## 4. Runtime catalog, local storage, and offline behavior

Normal operation consumes:

`https://opra.roonlabs.net/database_v1.jsonl`

Required behavior:

1. Obtain the runtime OPRA catalog from `database_v1.jsonl`.
2. Cache the catalog locally.
3. Work offline after the first successful sync using that cached catalog.
4. Provide a user-initiated **Refresh** action.
5. Perform approximately daily background checks for catalog changes.
6. Do not scrape GitHub during normal runtime.
7. Do not download OPRA artwork by default in v1.

Catalog update behavior must be deterministic and testable.

## 5. UX approval gate and project phases

### Phase 0 — design only

Phase 0 is design-only. Do not create Android implementation code for major user-facing features during this phase.

Before Phase 1, review and receive user approval for the UX/behavior of:

- overall navigation;
- first launch;
- My Headphones;
- Browse OPRA;
- search;
- profile selection;
- Select all / Select none;
- future-profile behavior;
- refresh and change reporting;
- export;
- Settings / About;
- app-update and changelog / What’s new UX;
- loading states;
- offline states;
- error states;
- accessibility;
- three original app-icon concepts.

Do not start Phase 1 until the user explicitly approves Phase 0.

For every major user-facing feature, explain UX/behavior in plain language or simple wireframes before implementation and wait for approval.

## 6. Approved Phase 0 UX decisions

### Overall navigation — approved 2026-08-15

- **My Headphones** and **Browse OPRA** are peer top-level destinations in bottom navigation.
- **My Headphones** is the local management area for headphones and selected profiles.
- **Browse OPRA** is the discovery area and begins at Manufacturer → Model, with deeper verified OPRA path segments only when source data genuinely requires them.
- Changing selections while browsing does not automatically switch the user to My Headphones.
- A visible **Refresh** action is available from the top app bar because catalog freshness affects both primary areas.
- A visible **Settings** action is available from the top app bar.
- App-update information may appear as a non-blocking banner when an update exists; the permanent home for installed-version, What’s new/changelog, and Get update information is **Settings → About & updates**.
- Bottom-navigation changes do not create an endlessly growing back stack.
- Back navigation inside Browse OPRA unwinds one hierarchy/detail level at a time.
- Back from a My Headphones detail returns to the My Headphones list.
- Back from Settings or another secondary screen returns to the top-level screen that opened it.
- At the root of either primary destination, system Back follows normal Android exit/background behavior rather than switching to the other bottom-navigation destination.
- Each primary destination should retain useful navigation and scroll state where practical.

Do not redesign this without a new explicit product decision.

### First launch — approved 2026-08-15

- Do not use a blocking onboarding wizard, account screen, tutorial carousel, or special first-launch destination.
- Open directly to **My Headphones**, genuinely empty because the app ships with zero headphones.
- Automatically begin the first download of the OPRA runtime catalog from `database_v1.jsonl`.
- Show an empty-state explanation with **Browse OPRA** as the obvious primary action.
- The empty state may include **“Your selections stay on this device.”**
- If Browse OPRA is opened while the first catalog download is still running, show the normal Browse loading state rather than redirecting elsewhere.
- After the first successful sync, use the locally cached catalog immediately on future launches and allow normal offline use.
- Do not request storage/folder access merely to browse or select headphones; folder access belongs to export.
- Do not interrupt first use with an update prompt, changelog modal, attribution wall, or other nonessential blocking surface.
- If the first-ever catalog download cannot complete, My Headphones may still render while detailed offline/error behavior follows its later Phase 0 design.
- First launch adds no special destination to the Android Back stack.

Do not redesign this without a new explicit product decision.

### My Headphones — approved 2026-08-15

- Present selected headphones as a simple, scannable library grouped by manufacturer.
- Within each manufacturer, order models alphabetically.
- Each headphone row shows model name, any genuinely verified deeper OPRA path distinction when required, and a concise selected-profile count.
- Do not require or download OPRA artwork for this list in v1.
- Keep normal rows visually quiet. Add a short attention line only when something needs awareness, such as **“1 profile updated”**, **“2 new OPRA profiles”**, or **“1 no longer available in OPRA.”**
- New-profile attention is based on the user’s previously known local catalog state.
- Opening the headphone shows newly discovered profiles and whether each is selected according to the user’s future-profile setting.
- Tapping a headphone opens its detail/management screen.
- The detail shows manufacturer/model identity, selected-profile count, status, and profile management.
- Selected profiles are visible there, and every selected profile has an explicit **Remove** action.
- Do not use swipe-to-delete as the primary removal mechanism for profiles or headphones.
- Removing an individual profile requires explicit confirmation.
- Removing an entire headphone requires explicit confirmation.
- When removing either a profile or a headphone, ask whether the user also wants to remove corresponding saved preset files created by OPRA EQ for UAPP.
- Preset-file deletion is **opt-in** at removal time; keeping files is the safe default.
- The app may delete only files it created and can validly manage through retained Android document-tree access.
- If an exported file is no longer accessible, removal of the local selection must still succeed and the UI must explain that the inaccessible file could not be removed rather than pretending it was deleted.
- The first-launch empty state is also used whenever My Headphones contains no headphones.
- Back from headphone detail returns to My Headphones and should preserve useful list/scroll state.

Do not redesign this without a new explicit product decision.

### Browse OPRA — approved 2026-08-15

- Browse is a simple, alphabetic discovery flow beginning at **Manufacturer → Model**.
- Manufacturer and model names come from OPRA source data; never invent or reinterpret names from internal identifiers.
- Do not use OPRA artwork in Browse for v1.
- A manufacturer opens an alphabetic model list.
- A model row may show a concise available-EQ-profile count.
- If the headphone already has local selections, Browse may show a subdued state such as **“2 selected”** or **“In My Headphones.”**
- Tapping a normal model opens its EQ-profile destination.
- Changing selections while browsing does not automatically switch to My Headphones.
- Preserve deeper hierarchy only when OPRA source data genuinely establishes additional path segments that must be represented.
- Never split IDs or filenames and assign invented semantics such as Variant, Revision, Pad, or similar labels.
- Never create a deeper level merely because an internal identifier or filesystem structure contains extra text.
- Back navigation unwinds from EQ profiles to a verified deeper level when one exists, otherwise to Models, then Manufacturers.
- Browse should retain useful navigation and scroll state when users switch top-level destinations and return.

Do not redesign this without a new explicit product decision.

### Search — approved 2026-08-15

- Place a visible **Search headphones…** field directly below the Browse OPRA top app bar at the Browse root.
- Search the locally cached OPRA catalog; do not make a network request for each query or keystroke.
- Search remains fully usable offline after the first successful catalog sync.
- Primary v1 search scope is manufacturer/vendor name plus headphone model/product name.
- Do not expose or require internal OPRA identifiers for normal search.
- Do not include EQ author, EQ details, frequencies, or other profile metadata in the main Browse search for v1.
- Matching should be case-insensitive and tolerant of ordinary spacing and punctuation differences so queries such as `hd600`, `HD 600`, and `hd 600` can reasonably find the same product.
- Search normalization is for matching only; always display OPRA-provided manufacturer and model names.
- Search results are headphone/model-first and show manufacturer, model, available EQ-profile count, and subdued selected state when relevant.
- Tapping a search result opens that headphone’s EQ-profile destination.
- A visible clear control restores the normal alphabetic manufacturer list.
- A no-results state should be brief, such as **“No headphones found”** with guidance to try another manufacturer or model name.
- Do not fall back to web search, GitHub lookup, or another remote search when no local result matches.
- Android Back while actively searching should first dismiss the keyboard/search interaction as appropriate rather than unexpectedly leaving Browse; normal root Back behavior then applies.
- Results update naturally after successful catalog refresh updates the local cache.

Do not redesign this without a new explicit product decision.

### Profile selection, Select all / Select none, and future-profile behavior — approved 2026-08-15

- Every usable OPRA parametric EQ profile is represented as a checkbox.
- Each profile row shows enough OPRA metadata to distinguish it, including creator/author and details when present.
- Provide **Select all** and **Select none** controls for the current usable profiles on the headphone.
- Checkbox and future-profile-switch edits are staged until the user chooses **Save changes** rather than committing each tap immediately.
- If Save would remove one or more currently selected profiles, require an explicit removal confirmation.
- That removal confirmation asks whether to also remove corresponding saved preset files created by OPRA EQ for UAPP; preset deletion is opt-in and defaults to keeping the files.
- If the saved selection contains zero profiles, treat that as removing the headphone from My Headphones and use the approved headphone-removal confirmation, including the optional saved-preset deletion choice.
- If the user presses Back with unsaved selection changes, do not silently save or discard them; offer to keep editing or discard the staged changes.
- **Automatically include new OPRA profiles for this headphone defaults to ON for every newly managed headphone.**
- The user may turn automatic inclusion OFF independently for any headphone.
- Select all and Select none do not silently change the automatic-inclusion setting.
- Automatic inclusion ON with all current profiles selected means follow all current and future profiles.
- Automatic inclusion ON with some current profiles unchecked preserves those exact unchecked profiles as explicit exclusions while automatically including future unrelated profiles.
- Automatic inclusion OFF means the saved selection is fixed and exact; future profiles appear but are not silently selected.
- The app must persist explicit exclusions as domain state rather than infer them merely from a selected-count snapshot.
- Newly discovered profiles may show a subtle **New** marker based on the user’s previously known local catalog state.
- When automatic inclusion is ON, a newly discovered non-excluded profile appears checked; when it is OFF, the new profile appears unchecked.
- Profile rows must retain room for conversion warnings such as **more than 10 EQ bands** or a filter that cannot be converted safely.
- A problematic or unsupported profile must remain visible and must not disappear silently merely because conversion has a limitation.

Do not redesign this without a new explicit product decision.

### Refresh and change reporting — approved 2026-08-15

- Manual **Refresh** downloads the runtime OPRA catalog, compares it with the locally cached previous state, and keeps the existing cached catalog usable while refresh is in progress; do not blank the UI during refresh.
- If a manual refresh succeeds with no relevant changes to managed headphones, show a brief success state such as **“OPRA catalog is up to date.”**
- If relevant changes are found, report a concise summary such as **“3 of your headphones have changes”** with a **Review** action.
- Prominent change reporting is scoped to headphones in **My Headphones**. Unrelated additions elsewhere in OPRA should not create noise.
- Approximately daily background checks perform the same catalog comparison.
- A successful background check with no relevant changes is silent.
- If a background check finds relevant changes, show a small non-blocking in-app banner on the next app open; v1 does not require Android notification permission for this behavior.
- Relevant managed-headphone change categories are: newly added profiles, changed selected profiles, and profiles that are no longer available in OPRA.
- For a newly discovered profile, automatic-inclusion ON causes the new non-excluded profile to be selected automatically; automatic-inclusion OFF leaves it unchecked. In both cases it can be marked **New** until reviewed.
- A changed **selected** OPRA profile is regenerated deterministically from the new OPRA data and reported to the user.
- A changed unselected profile does not require prominent My Headphones attention.
- If a selected/generated profile is removed upstream, keep its last generated XML and local record, keep it visible, mark it **“No longer available in OPRA,”** and do not delete it automatically.
- **No longer available in OPRA** is a persistent state, not a transient reviewed badge. It remains until the user removes the retained profile or it becomes available again.
- Removing a retained unavailable profile uses the already approved removal flow, including the opt-in choice to delete corresponding app-created saved preset files.
- My Headphones may summarize multiple transient changes concisely, for example **“2 new profiles · 1 updated.”**
- **New** and **Updated** mean changes the user has not yet reviewed for that headphone, not merely changes since the most recent network refresh.
- Another refresh must not silently clear unreviewed New/Updated indicators.
- Opening/reviewing the affected headphone’s profile/change view clears the transient New/Updated attention state for what was reviewed; the underlying profiles and selection state remain.
- A failed refresh must preserve the cached catalog and local user state and may show a concise message such as **“Couldn’t refresh OPRA. Using your saved catalog.”**
- A failed background check should not unnecessarily interrupt the user; retain the cached catalog and allow normal future checks/retries.

Do not redesign this without a new explicit product decision.

### Export — approved 2026-08-15

- **My Headphones** provides a clear **Export presets** action that exports all currently selected profiles across managed headphones.
- An individual headphone may also provide **Export this headphone** for convenience.
- On first export, use Android’s system folder/document picker and suggest `Documents/OPRA EQ for UAPP/Presets` where the platform allows a useful suggestion; the user chooses the actual folder.
- Persist supported directory access so repeated exports do not require picking the folder again while access remains valid.
- Do not request broad storage access and do not write directly into UAPP’s or another app’s private storage.
- Export hierarchy begins `Manufacturer/Model/` and may add deeper folders only for genuinely verified OPRA distinctions. Never invent folder meaning.
- Generated XML filenames and embedded ToneBoosters preset names use the same deterministic headphone-first naming rule: `Model [Variant] - Creator - Details`.
- Export is incremental. Before writing, summarize new, updated, and already-current selected presets where practical.
- The app tracks which external preset files it created so it can safely update only its own managed files.
- Newly auto-included profiles are generated locally when discovered but are written to the external preset folder only when the user explicitly invokes Export.
- When a selected OPRA profile changes, regenerate its XML locally and mark the corresponding external preset as update-ready; do **not** silently rewrite external preset files during background catalog refresh.
- When the user explicitly exports, replace the app-managed previously exported copy with the regenerated version.
- If an upstream profile is removed, retain its last generated XML and do not delete an existing exported preset automatically.
- If a user deselects/removes a profile but chooses to keep its saved preset during the approved removal confirmation, later Export operations leave that retained external file alone.
- If the destination already contains a same-named file that the app cannot establish it created/manages, do not silently overwrite it, do not invent a `(2)` name, and do not change deterministic preset naming. Report the conflict and offer a review path.
- If retained Android folder access is lost, explain that preset-folder access is no longer available and let the user choose a folder again; do not escalate to broad storage permission.
- My Headphones may show a concise export state such as **“2 presets ready to export”** or **“All selected presets exported.”**
- Catalog refresh and external-file mutation are deliberately separated: catalog updates may automatically update local generated state, while changes to the user’s external preset folder require an explicit Export action.

Do not redesign this without a new explicit product decision.

### Settings / About — approved 2026-08-15

- Keep Settings intentionally small and focused rather than exposing advanced or developer-oriented configuration in v1.
- Settings contains three primary areas: **Presets**, **OPRA catalog**, and **About**.
- **Export folder** shows whether a destination has been chosen and, when configured, displays the currently selected preset destination in a concise form.
- **Change folder** reopens Android’s system folder picker. Changing the destination does not automatically move or delete files from the previously selected folder.
- **Catalog status** shows the last successful OPRA refresh and may provide **Refresh now**.
- Explain that background catalog checks occur approximately daily.
- Do not add a background-refresh enable/disable toggle in v1.
- Do not expose account, sync-service, GitHub credential, Google Drive, OPRA URL/server, developer, broad-storage-permission, or similar configuration pages in v1.
- A short **Privacy** page explains that headphone selections and settings remain local, conversion occurs locally, no account is required, and the app does not use analytics or telemetry.
- Privacy may also explain that network access is used for the OPRA runtime catalog and public app-update metadata.
- Do not present a blocking privacy/legal wall on first launch.
- **Credits & licenses** clearly credits OPRA and explains that individual EQ creators/sources are credited with their profiles.
- Credits must state that OPRA EQ for UAPP is not endorsed by OPRA, Roon Labs, USB Audio Player PRO/UAPP, or ToneBoosters.
- Provide access to open-source license information as appropriate.
- The permanent app/version/update destination is **Settings → About & updates** rather than separate About and Updates areas.
- **About & updates** shows the app name and installed version and provides entry points for **What’s new**, update checking, and obtaining an update; detailed behavior of those update actions is governed by the separate app-update/changelog UX approval.

Do not redesign this without a new explicit product decision.

## 7. Selection domain rules

The approved selection behavior is a required domain model, not merely presentation logic.

For each managed headphone, domain state must be able to represent:

- currently selected profile identities;
- whether automatic future-profile inclusion is ON or OFF;
- explicit exclusions when automatic inclusion is ON;
- previously known profile identities needed to detect newly discovered profiles;
- reviewed/unreviewed transient change state for New/Updated reporting;
- local/removal state needed to preserve removed-upstream profiles and generated files as required elsewhere in this runbook.

These rules must be deterministic and covered by tests.

## 8. Conversion requirements

The Android app must implement conversion natively in Kotlin. Do not bundle Python in the APK.

`weekssa/opra-uapp-converter` is the read-only behavioral reference for proven conversion behavior.

Preserve OPRA data and behavior including:

- preamp;
- frequency;
- gain;
- Q;
- filter/band priority;
- author/creator;
- details;
- attribution.

Do not silently alter OPRA values.

Do not silently ignore unsupported filters. Unsupported filter behavior must be surfaced explicitly and covered by validation/tests.

### ToneBoosters/UAPP 10-band limit

UAPP/ToneBoosters output is limited to 10 bands.

When an OPRA profile contains more than 10 applicable bands:

1. preserve OPRA priority/order;
2. use the first 10 according to that established priority;
3. surface a warning to the user;
4. test the behavior against golden fixtures.

### Preset naming

Use headphone-first names in this form:

`Model [Variant] - Creator - Details`

Only use a variant when source data actually provides a verified variant/path distinction. Do not invent one.

### Encoding

ToneBoosters XML must remain ISO-8859-1-safe.

Full Unicode metadata must still be retained locally so source information is not lost merely because exported XML has stricter encoding constraints.

Conversion output must be deterministic for identical source data and selection state.

## 9. Testing strategy

Treat the Python converter in `weekssa/opra-uapp-converter` as the behavioral reference.

Build golden fixtures and require Kotlin parity for at least:

- normalization;
- preamp;
- filters;
- deterministic XML;
- 10-band handling;
- unsupported filters;
- preset naming;
- ISO-8859-1-safe export encoding;
- full Unicode local metadata retention where applicable;
- selection modes;
- automatic inclusion of future profiles;
- preserved exclusions;
- fixed exact selections;
- catalog updates;
- new-profile detection and reviewed/unreviewed state;
- changed profiles;
- removed profiles;
- export behavior, including incremental writes, managed-file ownership, conflicts, retained files, and lost folder access.

Never weaken validation merely to make tests pass. If Kotlin and the reference behavior differ, understand and resolve the difference rather than relaxing validation without a justified product decision.

## 10. Android architecture baseline

Unless a concrete technical reason changes the decision, prefer:

- Kotlin;
- Jetpack Compose;
- clear UI / domain / data separation;
- Room for structured local persistence;
- WorkManager for approximately daily background catalog checks, or current Android-recommended equivalents if the ecosystem changes materially;
- minSdk 26.

No Android code, Gradle files, Kotlin files, signing configuration, GitHub Actions, app icons, or release files belong to Phase 0 design work.

## 11. Export behavior

The approved Export UX in section 6 is authoritative.

Platform/storage requirements:

- Use Android’s system folder/document picker rather than broad storage permissions.
- Suggest `Documents/OPRA EQ for UAPP/Presets` where Android’s picker/API allows a useful suggestion.
- Let the user choose the actual destination.
- Persist supported directory access using the platform mechanism where appropriate.
- Do not request broad storage access.
- Do not write directly into another app’s private storage.
- Manage only files created by OPRA EQ for UAPP.
- Export folder layout begins `Manufacturer/Model/` and may add deeper verified OPRA path segments only when genuinely needed.
- Background catalog updates regenerate local state as required, but already-exported external preset files are rewritten only through an explicit Export action.

## 12. Upstream OPRA changes

### Changed selected profile

When a selected OPRA profile changes upstream:

1. detect the change through catalog refresh/update handling;
2. regenerate its XML deterministically;
3. report the change to the user;
4. mark any app-managed exported copy as update-ready rather than silently modifying it in the background;
5. update that external app-managed copy on the user’s next explicit Export action.

### Removed OPRA profile

When a previously selected/generated OPRA profile is removed upstream:

1. keep the last generated XML;
2. retain the local record needed to explain its state;
3. mark it **“No longer available in OPRA”**;
4. do not delete it automatically;
5. let the user remove it explicitly;
6. do not delete its exported preset unless the user explicitly opts to delete that app-created saved preset during removal.

These behaviors must be covered by catalog-update and export tests.

## 13. App updates and changelog architecture

Initial distribution is through **GitHub Releases**.

Update behavior for v1 is manual from the user’s perspective:

- the app may check public GitHub Release metadata for the latest release;
- it may show an in-app update banner when a newer release is available;
- it may show changelog / **What’s new** information;
- it may provide a **Get update** link to the relevant GitHub Release;
- it must not require notification permission for update checks in v1;
- it must not request APK-install permission in v1;
- it must not silently install updates.

### CHANGELOG.md architecture

A repository-level `CHANGELOG.md` is part of the project architecture and must be maintained from the beginning of implementation/release work so release notes and in-app What’s new content have a durable source of truth.

The documentation/bootstrap step intentionally did **not** create `CHANGELOG.md`; creation remains deferred until the appropriate implementation/release stage.

Use SemVer:

- `0.x` during development;
- `v1.0.0` for the first stable release.

## 14. Security and signing

Never commit:

- signing keys;
- passwords;
- tokens;
- credentials;
- other secrets.

When release signing is intentionally introduced later, use one stable release-signing identity. Signing configuration is not part of Phase 0.

## 15. Attribution and product claims

Follow OPRA attribution requirements.

Clearly credit:

- OPRA;
- individual EQ creators/authors;
- relevant sources represented by OPRA data.

Do not imply endorsement by:

- OPRA;
- Roon Labs;
- UAPP;
- ToneBoosters.

Preserve attribution through conversion/export to the extent supported by the output format while retaining richer metadata locally when required.

## 16. Communication and implementation workflow

For substantive work:

1. Read this runbook first.
2. Read any current architecture/UX documents referenced by this runbook or added later.
3. Confirm the writable repository is `weekssa/OPRA-EQ-for-UAPP` before writes.
4. Treat `weekssa/opra-uapp-converter` as read-only reference unless explicitly instructed otherwise.
5. Explain UX/behavior before implementing major user-facing features.
6. Respect the Phase 0 approval gate.
7. After changes, state exactly what changed.
8. State whether relevant validation passed.
9. Ask only questions that materially affect the product.
10. Prefer connected GitHub tools directly; give manual Git/Terminal steps only when truly required.

The repository is the maintained source of truth. When an approved decision changes, update this runbook so future work does not depend on conversational memory alone.

## 17. Current project state

The repository remains documentation-only for app development purposes. Phase 0 is active. Android implementation has not begun.

Approved on 2026-08-15:

- overall navigation;
- first launch;
- My Headphones;
- Browse OPRA;
- Search;
- profile selection;
- Select all / Select none;
- future-profile behavior, including default **ON** for automatic inclusion on newly managed headphones;
- Refresh and change reporting;
- Export;
- Settings / About.

The next Phase 0 UX area is **app updates and changelog / What’s new**.

Proceed one UX area at a time and do not advance when the user has asked to approve the current area first.
