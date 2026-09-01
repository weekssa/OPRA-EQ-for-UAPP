# OPRA EQ for UAPP / EQ Library — ChatGPT Project Runbook

This document is the maintained operational source of truth for work on **OPRA EQ for UAPP / EQ Library**. It intentionally summarizes the current product rules and points to the detailed v0.3 design/architecture documents. Older Phase 0 behavior that has been explicitly superseded by later approved v0.3 decisions must not be restored.

If a later explicit user decision conflicts with this file, the later user decision wins and this runbook must be updated in the same workstream.

## 1. Mandatory reading before substantive work

At the start of substantive work, read this file and then the current detailed sources of truth:

- `docs/ARCHITECTURE.md`
- `docs/PHASE1_DECISIONS.md`
- `docs/SOURCE_INGESTION_STRATEGY.md`
- `docs/V0.3_LOCKED_EXECUTION_PLAN.md`
- `docs/V0.3_RELEASE_POLISH_PLAN.md` for PR #4 / final v0.3.0 work
- `docs/BLACK_PEARL_PROTOCOL_NOTES.md` when Black Pearl behavior is involved
- `docs/V0.3_HANDS_ON_CHECKLIST.md` before producing or validating a v0.3 device-test APK
- `CHANGELOG.md`

`docs/AUTONOMOUS_V0.3_PLAN.md` records the implementation plan that led into the locked plan. Where wording differs, the later locked plan, `docs/V0.3_RELEASE_POLISH_PLAN.md`, this runbook, and explicit later decisions are authoritative.

## 2. Repository boundary

### Only writable repository

`weekssa/opra-eq-for-uapp`

Confirm this exact repository before every write.

### Read-only behavioral reference

`weekssa/opra-uapp-converter`

Use it as the proven OPRA → UAPP/ToneBoosters behavioral reference. Never modify it unless the user explicitly asks.

### Upstream sources

- OPRA upstream: `https://github.com/opra-project/OPRA`
- Runtime OPRA catalog: `https://opra.roonlabs.net/database_v1.jsonl`

Normal app runtime consumes the validated published EQ Library catalog built from OPRA and other qualified sources. Do not scrape GitHub during normal app operation.

## 3. Product identity, privacy, and Android baseline

- Application ID: `com.weekssa.opraeqforuapp`
- Native Android app, Kotlin + Jetpack Compose
- minSdk 26 unless a validated reason changes it
- Primary validation device: Pixel 9
- Prefer clear UI/domain/data/platform-integration boundaries, Room, and WorkManager or current Android-recommended equivalents
- Do not bundle Python in the APK

The app ships with **zero bundled headphones/EQs**. End users need no login, cloud backend, analytics, telemetry, ChatGPT, GitHub account, or Google Drive account. User selections and preferences remain local.

Normal runtime network use is limited to the catalog and public app-release metadata/update links. Do not download OPRA artwork by default in v1.

## 4. Current v0.3 information architecture and UX

The active v0.3 top-level destinations are:

- **My EQs**
- **EQ Library**
- **Settings**

The active output is an **operating context**, not a catalog filter. Changing output changes conversion/export/Flash capability and the output-specific My EQs collection, but does not hide otherwise valid canonical curves from EQ Library.

Android Back follows the in-app hierarchy before leaving the activity: selection editor → headphone detail → My EQs, EQ Library model → manufacturer/search → EQ Library root, and root EQ Library/Settings → My EQs. Only Back from the My EQs root exits the app. Visible back arrows and the Android Back gesture/button must agree.

Favorites are manageable from both EQ Library and My EQs. A managed-profile row in My EQs exposes the same filled/outlined star state as EQ Library; toggling the star changes only the active-output Favorite membership and must not change headphone selection, export currentness, or Flash state. Favorite snapshot rows use the filled star as the remove-from-favorites action; personal imports retain their explicit remove action.

EQ Library contains headphone EQs and General EQs. The initial qualified General EQ seed is sourced from the MIT-licensed ParaEQ built-in preset definitions and includes Sound, Utility, and source-authored Genre examples; the canonical catalog keeps exact source coefficients/preamp state and separate EQ Library-generated safety headroom when the source omits preamp. Headphone browse starts Manufacturer → Model and may include deeper verified source segments only when the source genuinely requires them. Never invent variants or meanings from IDs, filenames, or path fragments.

EQ Library is also a **living archive**. Once a genuine canonical EQ or genuine acoustic revision has been validly published, it remains represented in the current canonical catalog even if its source moves, goes offline, is removed, pauses, or is retired. Provenance/source-availability metadata may change, but source disappearance is never an instruction to delete archived acoustic history.

Users may locally **Hide** canonical headphone or General EQ profiles without deleting or mutating the archive. Hidden state is global local visibility state keyed by stable canonical profile identity, survives restart/refresh, and does not remove an already-saved My EQs entry, export state/file, favorite, or Flash state. Settings exposes **Hidden EQs** with batch Select all/none and Unhide selected. General EQ review provides none-selected-by-default batch controls for **Save selected** and **Hide selected**.

Personal PEQ import is a canonical-ingestion path, not a separate output converter. The compact **+ Import** action belongs with the Saved snapshots & personal imports section rather than as a large button competing with Black Pearl Connect. The v0.3 import surface supports pasted or chosen-file **Equalizer APO / AutoEq text**, parses contents rather than trusting file extension, previews the exact canonical interpretation before Save, blocks malformed/unsupported active filters instead of silently producing a partial EQ, preserves omitted preamp as null, and performs initial active-output export after a successful save when exportable. Import never automatically flashes hardware.

### New headphone selection and new-EQ review — final approved v0.3 behavior

A never-added headphone starts with **zero EQ profiles selected**. The user explicitly selects the profiles they want; no current or future EQ is ever silently selected merely because it is verified, newly published, or covered by a notification preference.

Every usable canonical parametric EQ is represented as a selectable checkbox. Active-output capability is presented separately as Exact, Optimized, or Not exportable; a valid canonical EQ remains visible and selectable/savable even when the active output cannot represent it. Output capability must never become a catalog-visibility or canonical-selection filter. Provide Select all and Select none as explicit selection actions.

Once a headphone is saved in My EQs, it has a per-headphone **Notify me about new EQs** preference. For newly managed headphones this notification preference starts **ON**. It is a review/attention preference only and never changes the saved selection by itself.

When notification is ON:

- newly published verified or unverified usable EQs for that headphone may create a pending in-app review;
- a materially changed selected tuning may also create a pending review;
- the review starts with no new EQ rows selected;
- **Add selected** adds only the explicitly checked new EQs to that output-specific My EQs selection and starts their normal initial export where representable;
- **Dismiss** marks the current batch reviewed without selecting, hiding, deleting, or exporting the unchosen EQs;
- Android/visible **Back** leaves the batch pending rather than silently dismissing it.

When notification is OFF, future EQ arrivals remain available in EQ Library but do not create the per-headphone new-EQ review prompt. Turning the preference off clears the currently pending attention state for that headphone without changing its saved EQ selection.

A locally hidden canonical lineage must not generate new-EQ review badges/prompts while it is hidden, including future revisions of that lineage. Hiding an already-selected EQ still preserves its My EQs membership, exported files/currentness, favorite state, and Flash availability. Unrelated new EQ lineages remain visible and reviewable normally.

The legacy Room/domain storage field name `autoIncludeNewProfiles` is retained through the v0.3 migration boundary for compatibility, but its v0.3-final meaning is **Notify me about new EQs** only. It must not be used to auto-select future profiles. Older automatic-inclusion rules in pre-final planning text are superseded by this section.

Selections remain output-specific. A selection under one output does not silently become selected under another output.

### Add/Save and export — approved v0.3 behavior

For an export-capable output, **Add/Save persists the selected EQs and initiates their initial export**. The normal workflow must not require a second routine Export action immediately after Add/Save.

**Export** and **Export all** are recovery actions. They are shown only when expected app-managed files for the active output are missing, stale, or otherwise need recovery. When expected files are present and current, those actions stay hidden.

If the user removes the final selected EQ for a headphone under the active output, that headphone is removed from that output's My EQs collection. Retained exported files are not silently deleted.

Use Android's Storage Access Framework/system picker. Suggest a sensible Documents location but let the user choose. Persist supported directory access. Do not request broad storage permission and do not write to another app's private storage. Manage/delete only files the app can prove it created.

Human-readable deterministic names remain the preferred requested export names, but **a physical filename is not the preset's ownership identity**. Export/currentness/cleanup must follow the stable output + product + profile identity and the exact SAF document URI actually returned for the app-created file, together with generated fingerprint/content hash. If a document provider normalizes or adjusts the requested display name, keep and track the successful newly created document instead of deleting it merely because its name changed. Store the provider-returned actual display name for fallback traversal/cleanup. If the preferred name is already occupied by an unowned file, never overwrite or delete that file; request a stable EQ-Library-disambiguated fallback name and accept a safe provider-created unique name if the provider further normalizes it. Internal same-name app presets must likewise receive stable identity-derived names rather than becoming permanent retry conflicts. Existing tracked app-owned files do not need to be renamed solely because naming logic improves.

## 5. Catalog, cache, refresh, and upstream changes

Normal operation:

1. Download the validated published EQ Library catalog.
2. Validate the candidate before promotion.
3. Keep a last-known-good local cache.
4. Work offline after initial successful sync.
5. Support manual Refresh.
6. Perform approximately daily background checks.
7. Keep known-good cached state usable while refreshing or after failure.
8. Never replace good state with a partial/malformed candidate.

Changed selected profile:

- regenerate deterministic local generated state;
- report the change;
- make the expected output eligible for automatic Add/Save generation or recovery export according to current v0.3 export-currentness rules;
- do not silently corrupt or overwrite unowned external files.

Source moved/unavailable/retired:

- keep the genuine canonical EQ and every genuine archived revision in the current published catalog;
- update a source URL only when the new location is confidently identified;
- otherwise mark source availability/lifecycle appropriately while retaining the archived record and provenance;
- retain any selected/saved local state and generated/exported state according to the normal My EQs/currentness rules;
- never treat source disappearance as permission to delete canonical acoustic history.

Catalog publication/currentness validation must fail when a candidate would silently remove a previously published genuine canonical profile or revision. Safe identity remaps may change routing/presentation only when the archived acoustic lineage is preserved.

## 6. Canonical EQ and conversion rules

Canonical source data is device-independent. Output capability is evaluated at the output boundary.

Preserve source metadata and acoustic intent including:

- preamp/overall gain;
- frequency;
- gain;
- Q;
- filter/band priority and order;
- creator/author;
- details;
- provenance/attribution.

Never silently alter acoustic values, invent creator metadata, ignore unsupported filters, or mutate canonical source data just to satisfy an output.

### UAPP / ToneBoosters

Treat `weekssa/opra-uapp-converter` as behavioral reference and require Kotlin parity for established conversion behavior.

ToneBoosters output is limited to 10 bands:

- preserve source priority/order;
- use the first 10 applicable source-priority bands;
- present the limitation clearly;
- retain the complete canonical source locally.

Use headphone-first deterministic names:

`Model [Variant] - Creator - Details`

Only use Variant/deeper identity when source data genuinely verifies it.

ToneBoosters XML must remain ISO-8859-1-safe while full Unicode metadata is retained locally.

### TRN Black Pearl Direct Flash — approved v0.3 behavior

Direct Flash remains an explicit user action with confirmation. It uses the DAC's currently active EQ slot and preserves the established Peak, Low Shelf, High Shelf, 10-band overwrite/latch/save behavior.

Black Pearl playback-gain handling uses the observed global-gain protocol documented in `docs/BLACK_PEARL_PROTOCOL_NOTES.md`:

- command `0x03`;
- signed little-endian gain value;
- 1/256 dB units;
- read the current global gain before the Flash sequence;
- apply the selected profile's required source-preamp/generated-headroom adjustment when representable;
- replace the previous EQ Library-applied gain delta rather than stacking a new reduction on top of it;
- a later 0 dB profile can remove the prior EQ Library-applied attenuation and restore the underlying baseline, subject to independent user volume changes;
- if the requested absolute gain is outside the validated representable device range, fail clearly rather than clamping;
- record a successful gain write before later PEQ writes so a retry cannot accidentally stack the same attenuation after a later transfer failure.

The Black Pearl's observed/recommended **per-filter gain** range of `-10 dB..+10 dB` is not treated as the same thing as the hard global-gain range. Because the PEQ packet stores band gain as a signed 16-bit 1/256 dB value, a finite source value outside ±10 dB that still fits that protocol field is preserved exactly for Black Pearl file export and may be Direct-Flashed only behind an explicit caution. The confirmation must identify the affected band/value, say that the exact value will be sent unchanged and not clamped, and provide **Cancel** / **Flash anyway**. The physical Pixel 9 / TRN Black Pearl test passed for the Edition XS Altruistic-Farmer275 `13,500 Hz / -11.9 dB / Q 4.0` case, including Cancel-without-write and Flash-anyway without app-side clamping. That one successful value does not establish every possible gain outside ±10 dB as validated, so the general caution remains. Unsupported filter types, non-finite/unencodable gain, and currently validated frequency/Q hard limits remain blocking. The global playback-gain representability range remains a hard limit.

The Flash confirmation must disclose the listening-volume/playback-gain change when nonzero and combine it with any 10-band or out-of-validated-range caution that applies.

Unrelated Black Pearl settings remain outside the Flash path.

## 7. Testing and validation

Never weaken validation merely to get green.

Treat the Python converter as behavioral reference and keep deterministic/golden coverage for at least:

- normalization and preamp;
- supported/unsupported filters;
- deterministic XML;
- 10-band handling;
- naming/encoding;
- explicit zero-default selection and notification-only new-EQ behavior, including no silent verified/unverified selection;
- new-EQ review Add selected / Dismiss / Back semantics and hidden-lineage prompt suppression;
- output-specific selections;
- catalog updates/removals plus living-archive preservation of previously published canonical profiles/revisions;
- local Hide/Unhide persistence, browse/search filtering, future-revision behavior, and preservation of already-saved My EQs/export state;
- Equalizer APO / AutoEq personal-import exactness, null-preamp preservation, strict malformed/unsupported-filter rejection, content-based format recognition independent of filename extension, parsed preview, and initial export;
- export/currentness/ownership, including provider-adjusted SAF names, stable same-name disambiguation, unowned-name collisions, exact-URI updates, and safe cleanup;
- Black Pearl protocol encoding, active slot, filter mapping, playback-gain read/write, non-cumulative replacement, 0 dB restoration, transfer failure, hard out-of-range rejection, and protocol-encodable-but-outside-validated filter-gain cautions without clamping.

Before a hardware-test APK is handed to the user, the exact source head must pass:

- Android unit tests;
- Android lint;
- debug assembly;
- release assembly;
- catalog currentness check;
- priority-community coverage check;
- CodeQL;
- signed-beta workflow including pinned signing-certificate verification.

The v0.3 signed candidate at `c70c523e1f530b8b197ebbccc41dfb4af1e27fc4` passed those gates and then passed `docs/V0.3_HANDS_ON_CHECKLIST.md` on Pixel 9 / TRN Black Pearl on 2026-08-31. PR #3 was subsequently fast-forward merged to `main`, preserving that tested commit as the merge commit. Release-preparation documentation or catalog-only automation may advance `main`; any final public-release source head still must pass the release workflow before publication. Code/DSP/device-behavior changes after the hardware-tested candidate require renewed hands-on validation as appropriate.

The most important Black Pearl hardware checks that passed include:

- real playback gain changes by the disclosed amount for a negative-preamp/headroom profile;
- a second Flash replaces rather than accumulates the prior EQ Library adjustment;
- a 0 dB Flash removes the prior EQ Library attenuation;
- Peak/Low Shelf/High Shelf and active-slot behavior;
- the `-11.9 dB` Edition XS case showing the caution, cancelling without a write, and then flashing without app-side clamping;
- unrelated DAC settings remaining unchanged.

## 8. Releases, signing, updates, and changelog

Use SemVer; development remains `0.x`; first stable release is `v1.0.0`.

Initial distribution is through GitHub Releases. Maintain `CHANGELOG.md` from the beginning and keep release notes aligned with it.

The app may check latest public release metadata and show an in-app update banner, What's new, and a Get update link. No notification permission or APK-install permission in v1.

Never commit signing keys, passwords, tokens, credentials, or secrets. Use one stable release-signing identity once release signing is intentionally introduced. Signed beta/release workflows must verify the pinned public signing identity.

## 9. Attribution and claims

Follow OPRA attribution requirements and clearly credit:

- OPRA;
- individual EQ creators/authors;
- relevant sources/provenance.

Do not imply endorsement by OPRA, Roon Labs, UAPP/USB Audio Player PRO, ToneBoosters, TRN, or other output/device vendors.

## 10. Working rules for ChatGPT

For substantive work:

1. Read the mandatory files in section 1.
2. Confirm the only writable repo is `weekssa/opra-eq-for-uapp` before every write.
3. Keep `weekssa/opra-uapp-converter` read-only unless explicitly told otherwise.
4. Use connected GitHub tools directly whenever possible; manual Git/Terminal steps are a last resort.
5. Do not implement a major user-facing feature without first explaining its UX/behavior and receiving approval.
6. Do not reinterpret old Phase 0 text as overriding later approved v0.3 behavior.
7. Make focused changes and validate them without weakening checks.
8. After changes, state exactly what changed and whether validation passed.
9. Keep hardware-gated feature PRs unmerged until the applicable signed candidate passes hands-on validation; PR #3 satisfied that gate before its v0.3 merge.
10. Update this runbook and the relevant detailed decision/architecture documents whenever the maintained source of truth changes.

## 11. Current v0.3 status

The v0.3 implementation from PR #3 has been fast-forward merged to `main` after the exact signed candidate `c70c523e1f530b8b197ebbccc41dfb4af1e27fc4` passed the Pixel 9 / TRN Black Pearl hands-on checklist on 2026-08-31.

The approved behavior includes output-specific My EQs, canonical multi-source EQ handling, zero-selected new-headphone defaults, per-headphone notification/review for newly arriving EQs without any silent future selection, Add/Save-triggered initial export with recovery-only Export actions, SAF export ownership anchored to the actual app-created document URI rather than exact provider filename spelling, and Black Pearl Direct Flash with non-cumulative playback-gain adjustment plus explicit caution for protocol-encodable per-band gains outside the generally validated ±10 dB range. The specific Edition XS `-11.9 dB` test case passed physical hardware validation, but the caution remains for the broader outside-±10 range.

v0.3.0 is in final release-polish validation on branch `v0.3-release-polish` / PR #4. The approved final scope includes hierarchical Android Back behavior, Favorite-star controls in My EQs, the first populated qualified General EQ catalog, living-archive preservation gates, reversible Hide/Unhide with Settings management, the explicit per-headphone new-EQ notification/review flow, and a redesigned personal PEQ import flow using strict previewed Equalizer APO / AutoEq canonical ingestion plus initial export. `docs/V0.3_RELEASE_POLISH_PLAN.md` is the controlling checklist. The implementation is present; the remaining software gate is to validate the final synchronized source/documentation head and signed candidate.

Because Android UI/data behavior changed after the prior hardware-tested merge, the fresh final exact-head signed candidate must pass the focused Pixel 9 Back/Favorite/General-EQ/Hide-Unhide/new-EQ-review/personal-import checklist before public publication. Black Pearl protocol requalification is not required because this polish did not touch Black Pearl protocol/DSP code; repeat it only if a later diff does. `docs/FUTURE_SOURCE_AUTOMATION_PLAN.md` records the post-v0.3 work to finish real scheduled adapters/currentness enforcement for every active source and monthly discovery of additional sources; do not silently treat that deferred automation as already complete.
