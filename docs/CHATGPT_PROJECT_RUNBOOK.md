# OPRA EQ for UAPP / EQ Library — ChatGPT Project Runbook

This document is the maintained operational source of truth for work on **OPRA EQ for UAPP / EQ Library**. It intentionally summarizes the current product rules and points to the detailed v0.3 design/architecture documents. Older Phase 0 behavior that has been explicitly superseded by later approved v0.3 decisions must not be restored.

If a later explicit user decision conflicts with this file, the later user decision wins and this runbook must be updated in the same workstream.

## 1. Mandatory reading before substantive work

At the start of substantive work, read this file and then the current detailed sources of truth:

- `docs/ARCHITECTURE.md`
- `docs/PHASE1_DECISIONS.md`
- `docs/SOURCE_INGESTION_STRATEGY.md`
- `docs/V0.3_LOCKED_EXECUTION_PLAN.md`
- `docs/BLACK_PEARL_PROTOCOL_NOTES.md` when Black Pearl behavior is involved
- `docs/V0.3_HANDS_ON_CHECKLIST.md` before producing or validating a v0.3 device-test APK
- `CHANGELOG.md`

`docs/AUTONOMOUS_V0.3_PLAN.md` records the implementation plan that led into the locked plan. Where wording differs, the later locked plan and explicit later decisions are authoritative.

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

Normal app runtime consumes the runtime catalog. Do not scrape GitHub during normal app operation.

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

EQ Library contains headphone EQs and, where populated, General EQs. Headphone browse starts Manufacturer → Model and may include deeper verified source segments only when the source genuinely requires them. Never invent variants or meanings from IDs, filenames, or path fragments.

### New headphone selection defaults — approved v0.3 behavior

A never-added headphone starts with:

- **zero EQ profiles selected**;
- **Automatically include new EQs** OFF.

Every usable canonical parametric EQ is represented as a selectable checkbox unless the active output cannot export/flash it; output capability is presented as Exact, Optimized, or Not exportable without deleting/hiding the canonical source curve.

Provide Select all and Select none.

Automatic-future behavior:

- ON + all current eligible profiles selected: follow all current/future eligible profiles.
- ON + some current profiles unchecked: preserve those exact exclusions and automatically include future unrelated eligible profiles.
- OFF: fixed exact selection; future profiles may appear but are not silently selected.
- Unverified community profiles are never silently auto-included; they require explicit manual selection.

Selections are output-specific. A selection under one output does not silently become selected under another output.

### Add/Save and export — approved v0.3 behavior

For an export-capable output, **Add/Save persists the selected EQs and initiates their initial export**. The normal workflow must not require a second routine Export action immediately after Add/Save.

**Export** and **Export all** are recovery actions. They are shown only when expected app-managed files for the active output are missing, stale, or otherwise need recovery. When expected files are present and current, those actions stay hidden.

If the user removes the final selected EQ for a headphone under the active output, that headphone is removed from that output's My EQs collection. Retained exported files are not silently deleted.

Use Android's Storage Access Framework/system picker. Suggest a sensible Documents location but let the user choose. Persist supported directory access. Do not request broad storage permission and do not write to another app's private storage. Manage/delete only files the app can prove it created.

## 5. Catalog, cache, refresh, and upstream changes

Normal operation:

1. Download `database_v1.jsonl`.
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

Removed upstream profile:

- retain last generated output/state;
- mark **No longer available in OPRA**;
- never silently delete it;
- let the user remove it explicitly.

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

The Flash confirmation must disclose the listening-volume/playback-gain change when nonzero.

Unrelated Black Pearl settings remain outside the Flash path.

## 7. Testing and validation

Never weaken validation merely to get green.

Treat the Python converter as behavioral reference and keep deterministic/golden coverage for at least:

- normalization and preamp;
- supported/unsupported filters;
- deterministic XML;
- 10-band handling;
- naming/encoding;
- selection modes and future-profile behavior;
- output-specific selections;
- catalog updates/removals;
- export/currentness/ownership;
- Black Pearl protocol encoding, active slot, filter mapping, playback-gain read/write, non-cumulative replacement, 0 dB restoration, transfer failure, and out-of-range rejection.

Before a v0.3 hardware-test APK is handed to the user, the exact source head must pass:

- Android unit tests;
- Android lint;
- debug assembly;
- release assembly;
- catalog currentness check;
- priority-community coverage check;
- CodeQL;
- signed-beta workflow including pinned signing-certificate verification.

Then use `docs/V0.3_HANDS_ON_CHECKLIST.md` on Pixel 9 / TRN Black Pearl. PR #3 stays draft and unmerged until the applicable hands-on checklist passes.

The most important Black Pearl hardware checks are:

- real playback gain changes by the disclosed amount for a negative-preamp/headroom profile;
- a second Flash replaces rather than accumulates the prior EQ Library adjustment;
- a 0 dB Flash removes the prior EQ Library attenuation;
- Peak/Low Shelf/High Shelf and active-slot behavior remain correct;
- unrelated DAC settings remain unchanged.

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
9. Keep PR #3 draft/unmerged until the signed candidate passes hands-on validation.
10. Update this runbook and the relevant detailed decision/architecture documents whenever the maintained source of truth changes.

## 11. Current v0.3 status

v0.3 implementation is active on PR #3 / branch `eq-library-community-v0.3`.

The current approved behavior includes output-specific My EQs, canonical multi-source EQ handling, zero-selected/auto-OFF new-headphone defaults, Add/Save-triggered initial export with recovery-only Export actions, and Black Pearl Direct Flash with non-cumulative playback-gain adjustment.

Automated validation and signed-beta generation must be rerun on the exact final source head after any additional source/documentation correction. The PR must remain draft and unmerged until the Pixel 9 / Black Pearl hands-on gate passes.
