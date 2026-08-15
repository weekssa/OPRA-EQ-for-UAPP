# OPRA EQ for UAPP — ChatGPT Project Runbook

This document is the maintained source of truth for work on **OPRA EQ for UAPP**.

## 1. Repository boundary and authority

### Writable repository

The only repository that may be modified is:

`weekssa/OPRA-EQ-for-UAPP`

### Read-only behavioral reference

The following repository is reference material only unless the user explicitly changes that instruction:

`weekssa/opra-uapp-converter`

It contains proven OPRA → UAPP/ToneBoosters conversion behavior and is the behavioral reference for the Android/Kotlin port. Do not modify it.

### OPRA upstream and runtime catalog

- OPRA upstream: `https://github.com/opra-project/OPRA`
- Runtime OPRA catalog: `https://opra.roonlabs.net/database_v1.jsonl`

Normal app operation must use the runtime `database_v1.jsonl` catalog. It must not scrape GitHub during normal runtime.

Before any repository write, confirm that the target is `weekssa/OPRA-EQ-for-UAPP`.

## 2. Product identity

- App name: **OPRA EQ for UAPP**
- Android application ID: `com.weekssa.opraeqforuapp`
- Product type: standalone native Android app
- Primary test device: Pixel 9
- Preferred minimum SDK: 26 unless a real technical reason requires a change

The app converts user-selected OPRA parametric EQ profiles into UAPP/ToneBoosters XML locally on the device.

## 3. Product principles and privacy

The app must ship with **ZERO headphones at install**.

End users must not need any of the following for normal use:

- login or account;
- cloud backend;
- analytics;
- telemetry;
- ChatGPT;
- GitHub account;
- Google Drive account.

User selections remain local. Normal conversion is local. Do not introduce data collection or remote account dependencies without an explicit product decision.

The app should not download OPRA artwork by default in v1.

## 4. Runtime catalog, local storage, and offline behavior

Normal operation consumes:

`https://opra.roonlabs.net/database_v1.jsonl`

Required behavior:

1. Obtain the runtime OPRA catalog from `database_v1.jsonl`.
2. Cache the catalog locally.
3. Work offline after the initial successful sync using the cached catalog.
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

For each major user-facing feature, explain its UX/behavior in plain language or simple wireframes before implementation and wait for approval.

## 6. Primary information architecture

The two primary product areas are:

- **My Headphones**
- **Browse OPRA**

Browse OPRA begins with the hierarchy:

**Manufacturer → Model**

The hierarchy must support deeper path segments when the actual OPRA data genuinely requires them.

Rules:

- Preserve verified deeper OPRA path structure when needed.
- Never invent variants.
- Never invent folder meaning.
- Never force deeper path segments into a semantic interpretation that the source data does not support.

### Approved Phase 0 navigation — 2026-08-15

Overall navigation is approved with the following behavior:

- **My Headphones** and **Browse OPRA** are peer top-level destinations in bottom navigation.
- **My Headphones** is the local management area for headphones and selected profiles.
- **Browse OPRA** is the discovery area and begins at Manufacturer → Model, with deeper verified OPRA path segments only when source data genuinely requires them.
- Changing selections while browsing does not automatically switch the user to My Headphones.
- A visible **Refresh** action is available from the top app bar because OPRA catalog freshness affects both primary areas.
- A visible **Settings** action is available from the top app bar.
- App-update information may appear as a non-blocking banner when an update exists; the permanent home for installed-version, What’s new/changelog, and Get update information is **Settings → About & updates**.
- Bottom-navigation changes do not create an endlessly growing back stack.
- Back navigation within Browse OPRA unwinds one hierarchy/detail level at a time.
- Back from a My Headphones detail returns to the My Headphones list.
- Back from Settings or another secondary screen returns to the top-level screen that opened it.
- At the root of either primary destination, system Back follows normal Android exit/background behavior rather than switching to the other bottom-navigation destination.
- Each primary destination should retain useful navigation state where practical.

The user approved this overall navigation on 2026-08-15. Do not redesign it without a new explicit product decision.

### Approved Phase 0 first launch — 2026-08-15

First-launch behavior is approved as follows:

- Do not use a blocking onboarding wizard, account screen, tutorial carousel, or special first-launch screen.
- Open directly to **My Headphones**, which is genuinely empty because the app ships with zero headphones.
- Automatically begin the first download of the OPRA runtime catalog from `database_v1.jsonl`.
- Show an empty-state explanation with **Browse OPRA** as the obvious primary action.
- The empty state may include the brief reassurance **“Your selections stay on this device.”**
- If Browse OPRA is opened while the first catalog download is still in progress, show a normal Browse loading state rather than redirecting elsewhere.
- After the first successful sync, use the locally cached catalog immediately on future launches and allow normal offline use.
- Do not request storage/folder permissions merely to browse or select headphones; folder access belongs to the later export flow.
- Do not interrupt first use with an update prompt, changelog modal, attribution wall, or other nonessential blocking surface.
- If the first-ever catalog download cannot complete, the app may still show the My Headphones shell, while detailed offline/error presentation remains a separate Phase 0 design item.
- First launch adds no special destination to the Android Back stack; normal approved navigation and Back behavior apply.

The user approved this first-launch approach on 2026-08-15. Do not redesign it without a new explicit product decision.

### Approved Phase 0 My Headphones — 2026-08-15

My Headphones is approved with the following behavior:

- Present the user's selected headphones as a simple, scannable library grouped by manufacturer.
- Within a manufacturer, order models alphabetically.
- Each headphone row shows the model name, any genuinely verified deeper OPRA path distinction when required, and a concise count such as **“3 profiles selected.”**
- Do not require or download OPRA artwork for this list in v1.
- Keep normal rows visually quiet. Add a short attention line only when something needs action or awareness, such as **“1 profile updated”** or **“1 no longer available in OPRA.”**
- Tapping a headphone opens its detail/management screen.
- The headphone detail shows manufacturer/model identity, selected-profile count, status, and profile management.
- Selected profiles must be visible from this management experience, and each selected profile must have an explicit **Remove** action so the user can remove one profile without removing the entire headphone.
- Do not use swipe-to-delete as the primary removal mechanism for profiles or headphones.
- Removing an individual profile requires an explicit confirmation.
- Removing an entire headphone requires an explicit confirmation.
- When removing either a profile or a headphone, ask whether the user also wants to remove the corresponding saved preset files created by OPRA EQ for UAPP.
- Preset-file deletion is **opt-in** at removal time; keeping saved presets is the safe default so files are never silently deleted.
- The app may delete only preset files it created and can validly manage through its retained Android document-tree access. If an exported file is no longer accessible to the app, removal of the local selection must still succeed and the UI should explain that the inaccessible external file could not be removed rather than pretending it was deleted.
- Detailed profile checkbox selection, Select all/none, automatic future-profile behavior, export mechanics, and refresh/change reporting remain separate Phase 0 design items and should not be prematurely redefined here.
- The approved first-launch empty state remains the My Headphones empty state whenever the user has no selected headphones.
- Back from headphone detail returns to the My Headphones list and should preserve useful list/scroll state.

The user approved this My Headphones approach, including per-profile removal and the saved-preset deletion choice, on 2026-08-15. Do not redesign it without a new explicit product decision.

### Approved Phase 0 Browse OPRA — 2026-08-15

Browse OPRA is approved with the following behavior:

- Browse is a simple, alphabetic discovery flow beginning at **Manufacturer → Model**.
- Manufacturer and model names must come from OPRA source data; do not invent or reinterpret names from identifiers.
- Do not use OPRA artwork in Browse for v1.
- A manufacturer opens an alphabetic model list.
- A model row may show a concise available-EQ-profile count.
- If the headphone already has local selections, Browse may show a subdued state such as **“2 selected”** or **“In My Headphones.”**
- Tapping a normal model opens its EQ-profile destination.
- Changing selections while browsing does not automatically switch the user to My Headphones.
- Preserve deeper hierarchy only when the OPRA source data genuinely establishes additional path segments that must be represented.
- Never split IDs or filenames and assign invented semantics such as Variant, Revision, Pad, or similar labels.
- Never create a deeper level merely because an internal identifier or filesystem structure contains extra text.
- Search, checkbox/profile-selection controls, Select all/none, automatic future-profile behavior, and detailed refresh/change reporting remain separate Phase 0 design items.
- Back navigation unwinds from EQ profiles to a verified deeper level when one exists, otherwise directly to Models, then to Manufacturers.
- Browse should retain useful navigation and scroll state when the user switches top-level destinations and later returns.

The user approved this Browse OPRA approach on 2026-08-15. Do not redesign it without a new explicit product decision.

## 7. Profile selection model

Every usable OPRA parametric EQ profile must be represented as a checkbox.

Provide:

- **Select all**
- **Select none**
- **Automatically include new OPRA profiles for this headphone**

The automatic-inclusion behavior is fixed as follows.

### Automatic inclusion ON + all current profiles selected

The headphone follows all current profiles and all future profiles.

### Automatic inclusion ON + some current profiles unchecked

Preserve those exact exclusions. Future unrelated OPRA profiles for the headphone are automatically included.

In other words, unchecked profiles remain explicitly excluded while newly appearing profiles that are not one of those exclusions are included automatically.

### Automatic inclusion OFF

The current selection is a fixed exact selection. Future profiles may appear in the UI after catalog updates, but they must not be silently added to the user’s selection.

This selection model must be represented explicitly in domain logic and covered by tests.

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

Only use a variant when the source data actually provides a verified variant/path distinction. Do not invent one.

### Encoding

ToneBoosters XML must remain ISO-8859-1-safe.

Full Unicode metadata must still be retained locally so the app does not lose source information merely because the exported XML format has stricter encoding constraints.

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
- changed profiles;
- removed profiles;
- export behavior.

Never weaken validation just to make tests pass. If Kotlin and the reference behavior differ, understand and resolve the difference rather than relaxing the test without a justified product decision.

## 10. Android architecture baseline

Unless a concrete technical reason changes the decision, prefer:

- Kotlin;
- Jetpack Compose;
- clear UI / domain / data separation;
- Room for structured local persistence;
- WorkManager for approximately daily background catalog checks, or current Android-recommended equivalents if the ecosystem changes materially;
- minSdk 26.

No Android code, Gradle files, Kotlin files, signing configuration, GitHub Actions, app icons, or release files are part of the documentation/bootstrap step that created this runbook.

## 11. Export behavior

Use Android’s system folder/document picker rather than broad storage permissions.

Desired experience:

- Suggest `Documents/OPRA EQ for UAPP/Presets` as the preferred location where Android’s picker/API allows a useful suggestion.
- Let the user choose the actual destination.
- Persist supported directory access using the platform mechanism where appropriate.
- Do not request broad storage access.
- Do not write directly into another app’s private storage.
- Manage only files created by OPRA EQ for UAPP.

Export folder layout begins:

`Manufacturer/Model/`

It may add deeper path segments only when those segments are verified by the OPRA source hierarchy and are genuinely needed. Never invent folder meaning.

## 12. Upstream OPRA changes

### Changed selected profile

When a selected OPRA profile changes upstream:

1. detect the change through catalog refresh/update handling;
2. regenerate its XML;
3. report the change to the user.

### Removed OPRA profile

When a previously selected/generated OPRA profile is removed upstream:

1. keep the last generated XML;
2. retain the local record needed to explain its state;
3. mark it **“No longer available in OPRA”**;
4. do not delete it automatically;
5. allow the user to remove it explicitly.

These behaviors must be covered by catalog-update tests.

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

This documentation/bootstrap step intentionally does **not** create `CHANGELOG.md`; the user explicitly deferred its creation.

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

When release signing is intentionally introduced later, use one stable release-signing identity. Signing configuration is not part of Phase 0/bootstrap.

## 15. Attribution and product claims

Follow OPRA attribution requirements.

Clearly credit:

- OPRA;
- individual EQ creators/authors;
- relevant sources represented by the OPRA data.

Do not imply endorsement by:

- OPRA;
- Roon Labs;
- UAPP;
- ToneBoosters.

Preserve attribution through the conversion/export pipeline to the extent supported by the output format, while retaining richer metadata locally when required.

## 16. Communication and implementation workflow

Work in clear phases.

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
10. Prefer connected GitHub tools directly; give the user manual Git/Terminal steps only when truly required.

The repository is the maintained source of truth. When a later approved decision changes this runbook, update the runbook so future work does not depend on conversational memory alone.

## 17. Current project state

At bootstrap, the repository contains documentation only.

Phase 0 is active. Android implementation has not begun.

Overall navigation, first-launch behavior, My Headphones, and Browse OPRA were approved on 2026-08-15. The next Phase 0 UX area is **Search**.

The next design work must proceed one approved UX area at a time. Do not advance to a later Phase 0 UX area when the user has explicitly asked to approve the current area first.