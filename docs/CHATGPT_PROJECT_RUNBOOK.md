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

## 2. Product identity and Android baseline

- App name: **OPRA EQ for UAPP**
- Android application ID: `com.weekssa.opraeqforuapp`
- Product type: standalone native Android app
- Primary test device: Pixel 9
- Preferred minimum SDK: 26 unless a real technical reason requires a change
- Prefer Kotlin, Jetpack Compose, clear UI/domain/data separation, Room, and WorkManager or current Android-recommended equivalents.

The app converts user-selected OPRA parametric EQ profiles into UAPP/ToneBoosters XML locally on the device. Do not bundle Python in the APK.

## 3. Product principles and privacy

The app must ship with **ZERO headphones at install**.

Normal use requires no login/account, cloud backend, analytics, telemetry, ChatGPT, GitHub account, or Google Drive account.

User selections remain local. Conversion is local. Do not introduce data collection or remote-account dependencies without an explicit product decision.

Do not download OPRA artwork by default in v1.

## 4. Runtime catalog, local storage, and offline behavior

Normal operation consumes `https://opra.roonlabs.net/database_v1.jsonl`.

Required behavior:

1. Obtain the runtime OPRA catalog from `database_v1.jsonl`.
2. Cache the catalog locally.
3. Work offline after the first successful sync using that cached catalog.
4. Provide a user-initiated **Refresh** action.
5. Perform approximately daily background checks for catalog changes.
6. Do not scrape GitHub during normal runtime.
7. Do not download OPRA artwork by default in v1.
8. Catalog update behavior must be deterministic and testable.

## 5. UX approval gate and project phases

### Phase 0 — design only

Phase 0 is design-only. Do not create Android implementation code for major user-facing features during this phase.

Before Phase 1, review and receive user approval for:

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

Do not start Phase 1 until the user explicitly approves Phase 0. For every major user-facing feature, explain UX/behavior in plain language or simple wireframes before implementation and wait for approval.

## 6. Approved Phase 0 UX decisions

### Overall navigation — approved 2026-08-15

- **My Headphones** and **Browse OPRA** are peer top-level destinations in bottom navigation.
- My Headphones is the local management area; Browse OPRA is discovery and begins Manufacturer → Model, with deeper verified OPRA path segments only when source data genuinely requires them.
- Changing selections while browsing does not automatically switch to My Headphones.
- The top app bar exposes **Refresh** and **Settings**.
- App-update information may appear as a non-blocking banner; its permanent home is **Settings → About & updates**.
- Bottom-navigation changes do not create an endlessly growing back stack.
- Back inside Browse unwinds one hierarchy/detail level at a time.
- Back from My Headphones detail returns to its list; Back from Settings/secondary screens returns to the opener.
- At the root of either primary destination, system Back follows normal Android exit/background behavior rather than switching tabs.
- Each primary destination should retain useful navigation/scroll state where practical.

Do not redesign this without a new explicit product decision.

### First launch — approved 2026-08-15

- No blocking onboarding wizard, account screen, tutorial carousel, or special first-launch destination.
- Open directly to My Headphones, genuinely empty because the app ships with zero headphones.
- Automatically begin the first runtime-catalog download.
- Show the empty state with **Browse OPRA** as the obvious action and optionally **“Your selections stay on this device.”**
- If Browse opens while the first catalog download is running, show the normal Browse loading state rather than redirecting elsewhere.
- After first successful sync, use cached catalog immediately on future launches and allow normal offline use.
- Do not request storage/folder access merely to browse/select; folder access belongs to export.
- Do not interrupt first use with update prompt, changelog modal, attribution wall, or other nonessential blocking surface.
- If the first-ever catalog download fails, My Headphones may still render; detailed loading/offline/error presentation follows its dedicated Phase 0 design.
- First launch adds no special Back-stack destination.

Do not redesign this without a new explicit product decision.

### My Headphones — approved 2026-08-15

- Present selected headphones as a simple library grouped by manufacturer, models alphabetically.
- Each row shows model name, any genuinely verified deeper OPRA path distinction when needed, and concise selected-profile count.
- No OPRA artwork required/downloaded for this list in v1.
- Normal rows remain quiet; show short attention lines only when needed, e.g. **“1 profile updated,” “2 new OPRA profiles,” “1 no longer available in OPRA.”**
- New-profile attention is based on the user’s previously known local catalog state.
- Opening the headphone shows newly discovered profiles and whether each is selected according to future-profile behavior.
- Tapping a headphone opens detail/management showing identity, selected count, status, and profile management.
- Every selected profile has an explicit **Remove** action; swipe-to-delete is not the primary removal mechanism.
- Removing a profile or whole headphone requires explicit confirmation.
- At removal, ask whether to also remove corresponding saved preset files created by OPRA EQ for UAPP. Preset deletion is opt-in; keeping files is the default.
- The app may delete only files it created and can still manage through valid retained document-tree access.
- If an external file is no longer accessible, local removal still succeeds and the UI explains the file could not be removed.
- The first-launch empty state is also used whenever no headphones are selected.
- Back from headphone detail returns to My Headphones and should preserve useful list/scroll state.

Do not redesign this without a new explicit product decision.

### Browse OPRA — approved 2026-08-15

- Simple alphabetic Manufacturer → Model discovery flow.
- Manufacturer/model names come from OPRA source data; never invent/reinterpret names from IDs.
- No OPRA artwork in Browse for v1.
- A manufacturer opens alphabetic models; model rows may show available profile count.
- Managed headphones may show subdued state such as **“2 selected”** or **“In My Headphones.”**
- Tapping a model opens its EQ-profile destination; changing selections does not auto-switch tabs.
- Preserve deeper hierarchy only when OPRA source data genuinely establishes additional path segments that must be represented.
- Never split IDs/filenames and assign invented semantics such as Variant, Revision, Pad, etc.
- Never create a deeper level merely because an internal ID/filesystem path contains extra text.
- Back unwinds from EQ profiles to a verified deeper level when one exists, otherwise Models, then Manufacturers.
- Retain useful Browse navigation/scroll state when switching tabs and returning.

Do not redesign this without a new explicit product decision.

### Search — approved 2026-08-15

- Visible **Search headphones…** field beneath the Browse root app bar.
- Search only the locally cached catalog; no request per keystroke; remains usable offline after initial sync.
- v1 scope is manufacturer/vendor name plus headphone model/product name.
- Do not expose internal OPRA identifiers or include EQ author/details/frequencies/profile metadata in main Browse search.
- Matching is case-insensitive and tolerant of ordinary spacing/punctuation differences, while displayed names remain OPRA-provided originals.
- Results are headphone/model-first and show manufacturer, model, profile count, and subdued selected state when relevant.
- Tapping a result opens that headphone’s EQ-profile destination.
- Clear control restores manufacturer list; no-results message is brief.
- Do not fall back to web/GitHub/remote search when local catalog has no match.
- Back while actively searching first dismisses keyboard/search interaction as appropriate.
- Results update after successful catalog refresh updates the cache.

Do not redesign this without a new explicit product decision.

### Profile selection, Select all / Select none, and future-profile behavior — approved 2026-08-15

- Every usable OPRA parametric EQ profile is a checkbox.
- Profile rows show enough metadata to distinguish them, including creator/author and details when present.
- Provide **Select all** and **Select none** for current usable profiles.
- Checkbox and future-profile-switch edits are staged until **Save changes**.
- If Save removes selected profiles, require confirmation and offer opt-in deletion of corresponding app-created saved preset files.
- Saving zero selected profiles is treated as removing the headphone and uses the approved headphone-removal confirmation.
- Back with unsaved changes offers keep editing or discard; never silently save/discard.
- **Automatically include new OPRA profiles for this headphone defaults to ON for every newly managed headphone.**
- The user may turn it OFF per headphone. Select all/none do not silently change this setting.
- ON + all current selected = follow all current and future profiles.
- ON + some current unchecked = preserve those exact unchecked profiles as explicit exclusions while automatically including future unrelated profiles.
- OFF = fixed exact selection; future profiles appear but are not silently selected.
- Persist explicit exclusions as domain state, not merely inferred from counts.
- New profiles may show **New** based on previously known local catalog state; ON causes a new non-excluded profile to appear checked, OFF leaves it unchecked.
- Rows retain room for conversion warnings such as >10 bands or unsupported filters.
- Problematic/unsupported profiles remain visible; never silently disappear.

Do not redesign this without a new explicit product decision.

### Refresh and change reporting — approved 2026-08-15

- Manual Refresh downloads the runtime catalog, compares it with cached previous state, and keeps the existing catalog usable while refreshing.
- If no relevant managed-headphone changes: brief **“OPRA catalog is up to date.”**
- If relevant changes exist: concise summary such as **“3 of your headphones have changes”** with **Review**.
- Prominent reporting is scoped to My Headphones; unrelated OPRA additions should not create noise.
- Approximately daily background checks perform the same comparison; no-change checks are silent.
- Relevant background changes produce a small non-blocking in-app banner on next app open; no notification permission required in v1.
- Relevant categories: new profiles, changed selected profiles, and profiles no longer available in OPRA.
- New-profile checkbox state follows auto-inclusion behavior and can remain **New** until reviewed.
- Changed selected profiles regenerate deterministic XML locally and are reported; changed unselected profiles need no prominent My Headphones attention.
- Removed selected/generated profiles keep last XML/local record, remain visible, and are marked **“No longer available in OPRA.”** This state persists until removal or reappearance.
- Multiple transient changes may be summarized, e.g. **“2 new profiles · 1 updated.”**
- **New**/**Updated** mean not yet reviewed for that headphone, not merely since last network refresh. Another refresh never silently clears them.
- Opening/reviewing affected headphone changes clears transient reviewed indicators but not underlying state.
- Failed refresh preserves cached catalog/local state and may say **“Couldn’t refresh OPRA. Using your saved catalog.”**
- Failed background checks do not unnecessarily interrupt the user.

Do not redesign this without a new explicit product decision.

### Export — approved 2026-08-15

- My Headphones provides **Export presets** for all currently selected profiles; individual headphone may offer **Export this headphone**.
- First export uses Android system folder/document picker and suggests `Documents/OPRA EQ for UAPP/Presets` where practical; user chooses actual destination.
- Persist supported directory access while valid.
- No broad storage access and no direct writes into UAPP/another app’s private storage.
- Hierarchy begins `Manufacturer/Model/`, with deeper folders only for verified OPRA distinctions.
- XML filename and embedded ToneBoosters preset name use the same deterministic `Model [Variant] - Creator - Details` rule.
- Export is incremental and may summarize new, updated, and already-current selected presets.
- Track which external files the app created so only app-managed files are updated/deleted.
- Newly auto-included profiles are generated locally but written externally only on explicit Export.
- Changed selected profiles regenerate locally and mark external copies update-ready; background catalog refresh does not silently rewrite external files.
- Explicit Export replaces app-managed previously exported copies with regenerated versions.
- Removed-upstream profiles retain last XML; existing exported presets are not automatically deleted.
- If a user removes/deselects but chooses to keep saved preset, future exports leave that external file alone.
- Same-named file not known to be app-managed is never silently overwritten; do not invent `(2)` names or alter deterministic naming. Report conflict and offer review.
- Lost folder access prompts user to choose a folder again; never escalate to broad storage permission.
- My Headphones may show **“2 presets ready to export”** or **“All selected presets exported.”**
- Catalog updates may automatically change local generated state; external mutations require explicit Export.

Do not redesign this without a new explicit product decision.

### Settings / About — approved 2026-08-15

- Keep Settings small and focused with primary areas **Presets**, **OPRA catalog**, and **About**.
- Export folder shows whether configured and the selected destination; **Change folder** opens Android picker again and does not automatically move/delete old files.
- Catalog status shows last successful refresh and may provide **Refresh now**; explain approximately daily checks.
- No background-refresh toggle in v1.
- No account, sync service, GitHub credential, Google Drive, OPRA URL/server, developer, or broad-storage-permission pages in v1.
- Privacy page explains local selections/settings, local conversion, no account, no analytics/telemetry, and network use for runtime catalog/public update metadata.
- No blocking privacy/legal wall on first launch.
- Credits & licenses clearly credits OPRA and individual creators/sources and states no endorsement by OPRA, Roon Labs, UAPP, or ToneBoosters.
- Provide open-source license information as appropriate.
- Permanent app/version/update destination is **Settings → About & updates**.
- About & updates shows app name/version and entry points for What’s new, update checking, and obtaining an update.

Do not redesign this without a new explicit product decision.

### App updates and What’s new / changelog — approved 2026-08-15

- Initial distribution is through GitHub Releases, with manual user-controlled installation.
- When a newer applicable release exists, show a small non-blocking in-app banner with the new version plus **What’s new** and **Get update** actions.
- The update banner may be dismissed for that version; the update remains discoverable in **Settings → About & updates**.
- About & updates shows installed version and either an up-to-date state or the available newer version, plus **What’s new**, **Check for update**, and **Get update** as applicable.
- **What’s new** presents friendly release notes grouped by version rather than raw Git history.
- A repository-level `CHANGELOG.md` becomes the durable source of truth from the beginning of implementation/release work; GitHub Release notes should correspond to it.
- Do **not** create `CHANGELOG.md` during Phase 0; its creation remains intentionally deferred until implementation/release work begins.
- On first launch after the installed app version changes, a small one-time non-blocking **Updated to vX.Y.Z** card may offer **What’s new** and **Dismiss**. Do not show this on first-ever installation and do not repeat it after dismissal for that installed version.
- Periodically check public release metadata at a modest cadence. Do not hammer GitHub on screen changes and do not require notification permission.
- If an update is found while the app is not actively in use, surface it on the next app open rather than through a required Android notification.
- **Get update** opens the specific GitHub Release in the user’s browser.
- The app must not silently download an APK, request “install unknown apps” permission in v1, launch a hidden installer, self-update, silently install updates, or require a GitHub account.
- Failed automatic update checks are quiet. If the user explicitly taps **Check for update** and it fails, show a concise retry-later message without repeated warning banners.
- v1 has one normal release channel. Do not add a stable/beta channel selector or offer prerelease/test builds as the normal update path.
- Use SemVer: `0.x` during development and `v1.0.0` for first stable release.
- Release-readiness requirement: end users must be able to access the release metadata/download location used by the app without GitHub authentication. The repository is currently private; Phase 0 does not change repository visibility, but public accessibility must be resolved before public distribution.
- No forced updates.

Do not redesign this without a new explicit product decision.

## 7. Selection and catalog domain rules

For each managed headphone, domain state must be able to represent:

- currently selected profile identities;
- automatic future-profile inclusion ON/OFF;
- explicit exclusions while auto-inclusion is ON;
- previously known profile identities needed to detect newly discovered profiles;
- reviewed/unreviewed transient New/Updated state;
- local/removal state needed to preserve removed-upstream profiles and generated files;
- app-managed external-file ownership/state needed for incremental export.

These rules are domain behavior, not merely presentation logic. They must be deterministic and tested.

## 8. Conversion requirements

Implement conversion natively in Kotlin and treat `weekssa/opra-uapp-converter` as the read-only behavioral reference.

Preserve OPRA preamp, frequency, gain, Q, band/filter priority/order, author/creator, details, and attribution. Do not silently alter OPRA values or silently ignore unsupported filters.

### ToneBoosters/UAPP 10-band limit

When a profile contains more than 10 applicable bands:

1. preserve OPRA priority/order;
2. use the first 10 according to that priority;
3. surface a warning;
4. cover the behavior with golden fixtures.

### Naming and encoding

Use `Model [Variant] - Creator - Details`, using a variant only when source data provides a verified distinction.

ToneBoosters XML must be ISO-8859-1-safe while full Unicode metadata remains retained locally. Conversion output must be deterministic for identical source data and selection state.

## 9. Testing strategy

Treat the Python converter as the behavioral reference. Build golden fixtures and require Kotlin parity for at least:

- normalization;
- preamp;
- filters;
- deterministic XML;
- 10-band handling;
- unsupported filters;
- preset naming;
- ISO-8859-1-safe export encoding;
- full Unicode local metadata retention;
- selection modes;
- automatic future-profile inclusion;
- preserved exclusions;
- fixed exact selections;
- catalog updates;
- new-profile detection and reviewed/unreviewed state;
- changed profiles;
- removed profiles;
- export behavior, including incremental writes, managed-file ownership, conflicts, retained files, and lost folder access.

Never weaken validation merely to make tests pass. If Kotlin and reference behavior differ, understand and resolve the difference rather than relaxing validation without a justified product decision.

## 10. Export platform requirements

Use Android’s system folder/document picker rather than broad storage permissions. Suggest `Documents/OPRA EQ for UAPP/Presets` where useful, let the user choose, and persist supported directory access. Manage only files created by OPRA EQ for UAPP. Folder layout begins `Manufacturer/Model/` and may add deeper verified OPRA path segments only when genuinely needed.

Background catalog updates regenerate local state as required, but already-exported external preset files are rewritten only through explicit Export.

## 11. Upstream OPRA changes

### Changed selected profile

Detect it, regenerate deterministic XML, report it, mark any app-managed exported copy update-ready rather than silently modifying it, and update that external copy on the user’s next explicit Export.

### Removed OPRA profile

Keep last generated XML and local record, mark **“No longer available in OPRA,”** do not delete automatically, allow explicit user removal, and delete an exported preset only if the user explicitly opts to delete that app-created saved preset during removal.

## 12. App updates and changelog architecture

The approved update UX in section 6 is authoritative.

A repository-level `CHANGELOG.md` is required from the beginning of implementation/release work but remains intentionally uncreated during Phase 0. Initial distribution is through GitHub Releases. v1 uses manual user-driven updates and no notification or APK-install permission.

## 13. Security and signing

Never commit signing keys, passwords, tokens, credentials, or other secrets. When release signing is intentionally introduced later, use one stable release-signing identity. Signing configuration is not part of Phase 0.

## 14. Attribution and product claims

Follow OPRA attribution requirements. Clearly credit OPRA, individual EQ creators/authors, and relevant sources. Do not imply endorsement by OPRA, Roon Labs, UAPP, or ToneBoosters. Preserve attribution through conversion/export as supported while retaining richer metadata locally when needed.

## 15. Communication and implementation workflow

For substantive work:

1. Read this runbook first.
2. Read any current architecture/UX documents referenced by this runbook or added later.
3. Confirm the writable repository is `weekssa/OPRA-EQ-for-UAPP` before writes.
4. Treat `weekssa/opra-uapp-converter` as read-only reference unless explicitly instructed otherwise.
5. Explain UX/behavior before implementing major user-facing features.
6. Respect the Phase 0 approval gate.
7. After changes, state exactly what changed and whether relevant validation passed.
8. Ask only questions that materially affect the product.
9. Prefer connected GitHub tools directly; give manual Git/Terminal steps only when truly required.

When an approved decision changes, update this runbook so future work does not depend on conversational memory alone.

## 16. Current project state

The repository remains documentation-only for app development purposes. **Phase 0 is active. Android implementation has not begun.**

Approved on 2026-08-15:

- overall navigation;
- first launch;
- My Headphones;
- Browse OPRA;
- Search;
- profile selection;
- Select all / Select none;
- future-profile behavior, including default ON for automatic inclusion on newly managed headphones;
- Refresh and change reporting;
- Export;
- Settings / About;
- app updates and What’s new / changelog UX.

The next Phase 0 UX area is the combined **Loading / Offline / Error states** design.

Proceed one UX area at a time and do not advance when the user has asked to approve the current area first.
