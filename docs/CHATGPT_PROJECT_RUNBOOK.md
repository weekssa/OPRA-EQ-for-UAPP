# OPRA EQ for UAPP / EQ Library — ChatGPT Project Runbook

This is the maintained operational source of truth for work on **OPRA EQ for UAPP / EQ Library**. Read it before substantive work. Later explicit user decisions supersede older planning text; when that happens, update this runbook in the same workstream rather than restoring obsolete behavior.

## 1. Mandatory reading

Before substantive work, read this file and the current documents relevant to the task:

- `docs/ARCHITECTURE.md`
- `docs/PHASE1_DECISIONS.md`
- `docs/SOURCE_INGESTION_STRATEGY.md`
- `docs/FUTURE_SOURCE_AUTOMATION_PLAN.md`
- `docs/V0.3_LOCKED_EXECUTION_PLAN.md`
- `docs/V0.3_RELEASE_POLISH_PLAN.md`
- `docs/V0.5_KT02H20_IMPLEMENTATION_PLAN.md` for the current output registry, shared hardware response adapter, FiiO JA11, and stock JCALLY JM12 work
- `docs/V0.5_IMPORT_COMPATIBILITY_NOTES.md` when file-import/export compatibility, TOPPING Tune, Black Pearl text import, or output-fidelity wording is involved
- `docs/V0.5_HANDS_ON_RELEASE_CHECKLIST.md` for the current v0.5 **Phase 2** Pixel 9 release-candidate testing record
- `docs/V0.6_MY_DAC_APPROVED_DESIGN.md` for the approved v0.6 My DAC UX/behavior contract
- `docs/V0.6_MY_DAC_IMPLEMENTATION_PLAN.md` for the current v0.6 architecture, sequencing, tests, and release gates
- `docs/V0.7_EW300_DSP_IMPLEMENTATION_PLAN.md` for the approved next-hardware scope, evidence gates, additive framework contract, automated validation, and signed owner-testing handoff
- `docs/V0.6_MY_DAC_STATUS.md` for the concise current v0.6 branch/hardware/UX state
- `docs/V0.6_LIBRARY_OWNERSHIP_AND_RECOVERY.md` for the current device-agnostic My EQs ownership and Needs attention recovery contract; where older architecture text still says My EQs is output-specific, this newer authority wins
- `docs/BLACK_PEARL_PROTOCOL_NOTES.md` when Black Pearl behavior is involved
- `docs/BLACK_PEARL_V0.6_UAC_MODE.md` when Black Pearl UAC detection/manual-switch help is involved
- `docs/BLACK_PEARL_V0.6_FACTORY_DEFAULTS_RESEARCH.md` and `docs/BLACK_PEARL_V0.6_RESTORE_DEFAULTS_HANDS_ON_CHECKLIST.md` for the current Black Pearl app-owned Restore-defaults contract and focused physical gate
- `docs/FIIO_JA11_PROTOCOL_NOTES.md` and `docs/FIIO_JA11_HANDS_ON_CHECKLIST.md` when JA11 behavior is involved
- `docs/JCALLY_JM12_PROTOCOL_NOTES.md` and `docs/JCALLY_JM12_HANDS_ON_CHECKLIST.md` when JM12 behavior is involved
- `docs/V0.3_HANDS_ON_CHECKLIST.md` for the qualified v0.3 Black Pearl/foundation record
- `docs/BLACK_PEARL_FLAT_RESET_HANDS_ON_CHECKLIST.md` for the qualified v0.4 Black Pearl reset record
- `CHANGELOG.md`

Historical plans remain useful context, but this runbook, current architecture, current release-specific plan, and later explicit decisions control where wording conflicts.

## 2. Repository boundary

### Only writable repository

`weekssa/opra-eq-for-uapp`

Confirm this repository before every write. The GitHub API may display canonical repository casing as `weekssa/OPRA-EQ-for-UAPP`; it is the same repository.

### Read-only behavioral reference

`weekssa/opra-uapp-converter`

Use it only as the proven OPRA → UAPP/ToneBoosters behavioral reference. Never modify it unless the user explicitly asks.

### Upstream/reference sources

- OPRA upstream: `https://github.com/opra-project/OPRA`
- OPRA runtime feed: `https://opra.roonlabs.net/database_v1.jsonl`

Do not commit credentials, signing secrets, tokens, passwords, or private keys.

## 3. Product baseline and privacy

- Application ID: `com.weekssa.opraeqforuapp`
- User-facing product: **EQ Library**
- Native Android, Kotlin + Jetpack Compose
- minSdk 26 unless a validated reason changes it
- Primary physical validation device: Pixel 9
- Prefer clear UI/domain/data/platform boundaries, Room, Preferences DataStore, WorkManager, and Android's Storage Access Framework
- Do not bundle Python in the APK

The app ships with **zero bundled headphones/EQs**. End users need no login, cloud backend, analytics, telemetry, ChatGPT, GitHub account, or Google Drive account. Selections/preferences/generated state remain local.

Normal runtime network use is limited to validated catalog acquisition/currentness and public app-release metadata/update links. Do not scrape GitHub/forums during normal Android operation and do not download OPRA artwork by default in v1.

## 4. Current information architecture

Base top-level destinations:

- **My EQs**
- **EQ Library**
- **Settings**

For approved v0.6 My DAC behavior, after a supported DAC is recognized in the current app session the destinations become:

- **My EQs**
- **My DAC**
- **EQ Library**
- **Settings**

`My DAC` sits immediately beside `My EQs`. Once shown in the current app session it remains present after disconnect so navigation does not jump; disconnected device state is explicitly stale/Last read. A later cold launch with no supported DAC may return to the three-destination baseline. USB attach/open behavior, EQ/DEVICE tabs, and all My DAC states follow `docs/V0.6_MY_DAC_APPROVED_DESIGN.md`.

A supported DAC uses **one app-wide authoritative connection/session**. My EQs, My DAC and EQ Library must not independently reconnect to or reread the same hardware just because the user navigates. A successful connection/reconnection automatically refreshes EQ + DEVICE state; successful hardware changes automatically refresh affected state. Manual Refresh remains an escape hatch for external changes rather than a required ordinary step.

EQ choice belongs where the EQ lives: **My EQs** and **EQ Library** expose Flash when the current supported DAC permits it. My DAC no longer contains a second **Change EQ** chooser. **My DAC -> EQ** shows actual hardware state/edit/capture/reset; **My DAC -> DEVICE** is one concise settings list with current values and edit affordances on the same rows.

Routine qualified DEVICE choices apply immediately and verify readback; protocol/qualification mechanics remain internal. Level-sensitive changes may use a short plain-language confirmation. Do not add a generic Save-device-settings or Restore-defaults action without exact device semantics.

The active output is a global **operating/action context**, not a catalog or ownership filter. It changes target-specific conversion/fidelity, derived currentness, file export, connection controls, and Flash availability. It must **not** change which headphones/EQs belong to My EQs, clear an open My EQ detail, or silently add/remove local selections. A valid canonical curve remains visible regardless of target compatibility.

A physically connected DAC and the active output are distinct concepts. Recognizing/connecting a DAC for My DAC must not silently change the user's global active output unless the approved Automatic-output behavior explicitly makes that connected DAC the effective output context. My DAC always represents actual connected hardware.

Android Back unwinds in-app hierarchy first. Root EQ Library/Settings return to My EQs; only Back from the My EQs root exits. My DAC editor/detail flows unwind to the My DAC root before leaving the destination.

Favorites and local Hide/Unhide are presentation/saved-state features. Hiding a canonical lineage does not delete archive history, My EQs membership, exported files, favorite state, or Flash state.

## 5. Canonical EQ model

Canonical EQ data is source-agnostic and device-independent. Preserve the complete source and metadata including:

- preamp/overall gain;
- frequency;
- gain;
- Q;
- supported filter type;
- source band priority/order;
- creator/author;
- details/target/intent when sourced;
- provenance/attribution;
- immutable acoustic revision identity.

Never silently alter canonical acoustic values, invent creator/variant meaning, ignore unsupported active filters, or truncate canonical source merely to satisfy an output.

Missing source preamp remains `null`. EQ Library-generated safety headroom is derived metadata, never a rewritten source preamp.

The canonical catalog is a **living archive**. Genuine published acoustic profiles/revisions remain represented even if a source moves, disappears, pauses, or retires. Publication/currentness validation must reject silent loss or in-place acoustic mutation of archived history.

## 6. Catalog, cache, refresh, and source automation

Android runtime requirements:

1. consume a validated published EQ Library catalog;
2. validate a candidate before promotion;
3. retain last-known-good local cache;
4. work offline after initial successful sync;
5. support manual Refresh;
6. perform approximately daily background/currentness checks;
7. keep cached state usable during refresh/failure;
8. never replace good state with a partial or malformed candidate.

A changed selected profile regenerates deterministic derived output/currentness and is surfaced to the user. A removed/unavailable upstream profile keeps its archived/local generated state and is marked appropriately; users remove it explicitly.

Repository-side source maintenance follows `docs/FUTURE_SOURCE_AUTOMATION_PLAN.md`: automate legitimate stable public retrieval paths, quarantine ambiguous/malformed records without blocking unrelated publication, preserve source-health state, and pause sources that cannot be automated safely rather than creating a recurring manual-currentness queue. Measurement curves are never converted into invented source-authored PEQ.

Ordinary catalog publication remains independent of APK releases while client schema/device/DSP behavior is unchanged.

## 7. Selection, review, My EQs, and recovery

A never-managed headphone starts with **zero selected EQ profiles**. Every usable canonical PEQ is an explicit checkbox with Select all / Select none. A valid canonical EQ stays visible/selectable even if the active output reports it as Not suitable/Not exportable.

The final approved behavior supersedes the older automatic-future-selection model: **Notify me about new EQs** starts ON for newly managed headphones but is attention-only. It never silently selects a future profile.

When notification is ON, eligible new EQs and materially changed selected tunings may create review attention. New review rows start unchecked; **Add selected** adds only checked rows, **Dismiss** reviews the current batch without adding/hiding/deleting unchosen EQs, and Back leaves the batch pending. Turning notification OFF clears attention without changing the stored selection.

The persisted compatibility field name `autoIncludeNewProfiles` may remain internally for migration compatibility, but it must not be interpreted as permission to auto-select future profiles.

**My EQs ownership and selection are device/output-agnostic.** A headphone, Favorite, Personal EQ, captured DAC EQ, or General EQ saved to My EQs remains the same local item when the active target changes. Saving to My EQs does not itself export a file, download anything, or Flash hardware. Target-specific representations/currentness remain derived state and are created/used only by explicit Export/Flash paths.

Legacy output-selection tables/`outputId` parameters may remain temporarily for migration/source compatibility, but they must not be used to decide current My EQs identity or visibility. A DAC capture records device identity as provenance only; after capture it behaves like any other Personal EQ.

Persisted app-managed exported artifacts that no longer have a confident current My EQ association remain visible under **Needs attention** rather than disappearing. Recovery is limited to exact app-owned/persisted-access artifacts, uses strict parsing, requires the user to provide any missing headphone/name association, preserves decoded EQ values and original-file provenance, and never scans or deletes arbitrary external files. Malformed/unsupported artifacts remain visible but are not falsely recoverable. An unresolved artifact remains present across restart/rescan until recovered, explicitly removed, or confirmed missing according to the storage ownership rules.

A hardware EQ captured through My DAC is stored as a **Personal EQ** with actual device-native values and capture provenance. It may be associated with an existing/saved/library headphone or intentionally left unassociated. Never invent a creator/source association. If the hardware exactly matches an existing saved EQ, link to it rather than creating a duplicate.

## 8. Output registry and fidelity

`ExportDevice` is the single UI/domain output registry. Selectable outputs are grouped as:

- **Hardware DACs**
- **Apps**
- **Universal formats**

Each target declares presentation, file-vs-hardware semantics, format kind, capability profile, current selectability, and validation status where applicable.

Hardware-only outputs do not invent export files. Their deterministic derived representation remains local until the user explicitly taps **Flash**.

Use fidelity states consistently:

- **Exact** — source is natively representable at the target's actual limits/quantization without target-side acoustic alteration or generated headroom.
- **Optimized** — EQ Library deterministically derives a faithful target representation and it passes that target's quality/safety gates. Native target rounding, complete-response fitting, and EQ Library-generated target headroom are all Optimized rather than Exact.
- **Not suitable / Not exportable** — a safe/faithful representation cannot be produced.

For finite hardware, show a concise reason separate from source/catalog description text. Prefer wording such as:

- `Exact · source values preserved`
- `Optimized · native hardware rounding only`
- `Optimized · 14 → 10 bands · full-response fit`
- `Optimized · generated headroom −3.0 dB`

Do not force redundant `N → N bands` wording when no band-budget change occurred. If a source fits the target band structure and needs only native rounding, preserve that structure rather than unnecessarily invoking the response fitter.

Changing output capability code must not mutate canonical data.

## 9. UAPP / ToneBoosters conversion

Treat `weekssa/opra-uapp-converter` as behavioral reference and require Kotlin parity for established conversion semantics.

ToneBoosters output remains limited to 10 bands:

- preserve source priority/order;
- use the first 10 applicable source-priority bands;
- surface the limitation clearly;
- retain all source bands canonically.

Use deterministic headphone-first names:

`Model [Variant] - Creator - Details`

Only include deeper variant/configuration identity when the source genuinely verifies it.

ToneBoosters XML must remain ISO-8859-1-safe while full Unicode metadata remains local.

## 10. Shared finite-hardware response adapter

Finite PEQ hardware derivation for **TRN Black Pearl (10 bands)**, **FiiO JA11 (5 bands)**, and the historical stock JCALLY JM12 implementation uses the shared deterministic hardware response adapter described in `docs/V0.5_KT02H20_IMPLEMENTATION_PLAN.md`.

Rules:

- complete canonical source remains unchanged;
- unsupported source filter types fail rather than being ignored;
- exact native values pass only when target range and target quantization truly preserve them and the source provides an exactly representable preamp;
- if the source fits the available band structure but needs only native target rounding, preserve the same filter structure and report Optimized/native rounding rather than fitting a different curve;
- otherwise fit the **complete source response** to the available target bands;
- never silently take the first N bands for these hardware targets;
- quantize only at the device boundary;
- require fixed RMS/max-error gates for Optimized output;
- generate missing-preamp safety headroom from the final quantized target response without mutating canonical preamp/headroom metadata, and always classify that generated target headroom as Optimized;
- version derived representation semantics so output currentness changes when the adapter contract changes.

Historical internal class names containing `Kt02h20` or `FiveBand` are implementation-compatibility names only; do not infer a product limitation from those names.

My DAC manual editing must reuse the same deterministic response/headroom principles. Do not create a Compose-only clipping heuristic. A local edit plan evaluates the complete planned native response, determines required safe headroom using verified device semantics, separates headroom warnings from device-limit/unsupported-value warnings, and requires review before any hardware write.

### Planned v0.7 SIMGOT EW300 DSP cable

Current evidence and beta status: `docs/V0.7_EW300_DSP_STATUS.md`, `docs/EW300_DSP_PROTOCOL_NOTES.md`, `docs/EW300_PRELAUNCH_RUNBOOK.md`, and `docs/EW300_CAPABILITY_BATCH.md`. The recovery branch reuses the preserved Phase B evidence but does not ship the separate discovery APK/tooling. Any future diagnostic artifact must pass DEX class-definition and exact signed cold-launch checks before handoff.

The next planned hardware addition is the USB-C DAC/DSP cable supplied with the SIMGOT EW300 DSP. It is **not supported by v0.6.0**. Work follows `docs/V0.7_EW300_DSP_IMPLEMENTATION_PLAN.md` and must reuse the current output registry, immutable device capabilities, shared finite-hardware response adapter, authoritative DAC session, and approved My DAC/Flash/capture/reset UX.

The exact USB identity and raw five-band transport are documented from the owner’s evidence. A reproducible same-earpiece acoustic cross-check qualifies direct-Hz Peak decoding; non-Peak acoustic labels remain gated. Register `0x66` is conservatively classified as ordinary digital DAC/playback gain, not a dedicated EQ preamp, so it is excluded from EQ identity and captured profiles. Writing global gain, persistence, and reset remain hardware-validation gates. Do not infer compatibility from a chipset, browser tool, community report, or similarity to FiiO/JCALLY behavior. The current recovery implementation uses a strong EW300 string/interface/session fingerprint and keeps VID/PID-only discovery read-only. The shared EQ pipeline is source-neutral across OPRA, AutoEQ, community/general sources, imports, Personal EQs, and DAC captures. An Android-free declarative capability batch provides strict read-only preflight/reporting; mutating cases require an explicit signed diagnostic plan. Persistent Flash, editor Apply, and Reset remain gated until their exact-device semantics pass. User-visible status must remain Hardware validation pending until the exact signed candidate passes the consolidated physical gate. Merge and release require explicit owner authorization.

## 11. TRN Black Pearl

Black Pearl protocol/USB behavior was physically qualified in the v0.3 foundation and Reset EQ to flat was qualified in v0.4. Preserve those protocol boundaries and fail-safe gain-reset rules.

v0.5 changes **derived DSP adaptation**, not the qualified Black Pearl USB identity:

- Direct Flash and Black Pearl file export consume the same shared device representation;
- profiles over 10 bands are complete-response fitted instead of first-10 truncated;
- exact protocol-encodable values are not silently clamped;
- current active EQ slot and global playback-gain replacement semantics remain protected;
- Reset keeps its qualified fail-safe ordering.

v0.6 My DAC additionally exposes only independently established/qualified normal controls. UAC 1.0/2.0 current mode is read from standard USB AudioControl descriptors. No speculative Black Pearl UAC write command is used. The UI provides the approved manual startup sequence under the read-only UAC row and automatically re-detects the replacement session.

The later owner-approved **EQ Library Restore defaults** action is an app-owned preset, not a claim about TRN factory state. Its current target contract is displayed **50%** volume, **FAST-LL**, **HIGH** gain, **CLASS AB**, centered balance, and **0 dB microphone gain**. The confirmation dialog keeps **Also reset EQ to flat** OFF by default; when enabled it reuses the separately qualified Reset EQ to flat transaction. Preserve truthful non-factory wording and the focused exact-signed-candidate hardware gate in `docs/BLACK_PEARL_V0.6_RESTORE_DEFAULTS_HANDS_ON_CHECKLIST.md`.

## 12. Testing and release discipline

Treat the historical Python converter as behavioral reference where applicable. Never weaken validation merely to make CI green.

For Android hardware work, require the smallest meaningful layers of evidence:

- framework-independent domain tests for deterministic logic;
- repository/transport tests for protocol/session/write/readback behavior;
- Compose/UI tests for interaction-state regressions when practical;
- Android CI, lint/assembly and security/static gates;
- exact signed-candidate provenance when physical hardware validation is required;
- hardware testing only after software-side work is exhausted.

Physical evidence attaches to the exact transaction behavior tested. Pure Compose/navigation/string refactors do not invalidate protocol qualification unless connection ownership, read timing, write sequencing, values, persistence commands, verification rules, or session behavior change.

Use SemVer. Keep v0.x during development; v1.0.0 is the first stable release. Never merge or publish a development release without explicit project-owner authorization.
## 2026-09-15 corrective checkpoint

The owner-reported `431cbfa` test failed Restore defaults (premature stop at 0%) and page-density review. The current corrective work and genuine-failure policy are maintained at `docs/V0.6_MY_DAC_STATUS.md`. Restore completion is tied to the exact write cycle and original USB session; all final targets must match. No failed setting is automatically retried. Managed detail and General EQ headers/actions are compact and scrollable. A new exact signed beta needs focused physical review; historical PASS pins do not qualify these corrections. PR #16 remains open/draft and v0.5.0 remains public.


## 2026-09-15 corrective candidate — PASS

The project owner completed the focused Pixel 9 / TRN Black Pearl retest on exact signed source `eb1980076009001b5216ffbb531de8a28a4780eb` and reported **SUCCESS**.

The retest confirmed:

- Restore defaults completed normally on the first attempt and reached **50% / FAST-LL / HIGH / CLASS AB / Centered / microphone 0 dB**.
- The optional EQ-flat behavior and saved My EQs remained protected as specified.
- Reconnect persistence remained correct.
- The revised My EQs managed-headphone detail and General EQs pages were materially less cramped, with the revised action/help hierarchy.

Evidence category: **OWNER-REPORTED**.

Exact signed APK: https://raw.githubusercontent.com/weekssa/OPRA-EQ-for-UAPP/mobile-test-apk/candidates/EQ-Library-v0.6.0-beta-eb19800.apk

APK SHA-256: `dabf4bcdddf69853b09793f5a94bec0a3af7efb430f1cdfe26ffc35a93b783ad`

The exact candidate passed Android CI #1538, CodeQL #1420, Catalog currentness CI #1826, Priority community coverage CI #1311, and Signed EQ Library Beta Candidate #1213. The signing workflow verified the pinned certificate `65c1c1256dae3c49e3548f334c91f0ba991969e9be9e0b223ba4e253d2114747`.

This PASS closes the corrective Black Pearl physical gate. It does not establish TRN factory-default semantics and does not qualify FiiO JA11 hardware behavior. PR #16 remains open and draft; merge, release, and publication remain explicit owner-authorization gates.
