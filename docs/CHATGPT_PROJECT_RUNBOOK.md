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
- `docs/BLACK_PEARL_PROTOCOL_NOTES.md` when Black Pearl behavior is involved
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
- Primary physical test device: Pixel 9
- Prefer clear UI/domain/data/platform boundaries, Room, Preferences DataStore, WorkManager, and Android's Storage Access Framework
- Do not bundle Python in the APK

The app ships with **zero bundled headphones/EQs**. End users need no login, cloud backend, analytics, telemetry, ChatGPT, GitHub account, or Google Drive account. Selections/preferences/generated state remain local.

Normal runtime network use is limited to validated catalog acquisition/currentness and public app-release metadata/update links. Do not scrape GitHub/forums during normal Android operation and do not download OPRA artwork by default in v1.

## 4. Current information architecture

Top-level destinations:

- **My EQs**
- **EQ Library**
- **Settings**

The active output is a global **operating context**, not a catalog filter. It changes output-specific My EQs membership, conversion/fidelity, file export, connection controls, and Flash availability. It must never hide an otherwise valid canonical curve from EQ Library.

Android Back unwinds in-app hierarchy first. Root EQ Library/Settings return to My EQs; only Back from the My EQs root exits.

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
3. retain a last-known-good local cache;
4. work offline after initial successful sync;
5. support manual Refresh;
6. perform approximately daily background/currentness checks;
7. keep cached state usable during refresh/failure;
8. never replace good state with a partial or malformed candidate.

A changed selected profile regenerates deterministic derived output/currentness and is surfaced to the user. A removed/unavailable upstream profile keeps its archived/local generated state and is marked appropriately; users remove it explicitly.

Repository-side source maintenance follows `docs/FUTURE_SOURCE_AUTOMATION_PLAN.md`: automate legitimate stable public retrieval paths, quarantine ambiguous/malformed records without blocking unrelated publication, preserve source-health state, and pause sources that cannot be automated safely rather than creating a recurring manual-currentness queue. Measurement curves are never converted into invented source-authored PEQ.

Ordinary catalog publication remains independent of APK releases while client schema/device/DSP behavior is unchanged.

## 7. Selection, review, and My EQs

A never-managed headphone starts with **zero selected EQ profiles**. Every usable canonical PEQ is an explicit checkbox with Select all / Select none. A valid canonical EQ stays visible/selectable even if the active output reports it as Not suitable/Not exportable.

The final approved behavior supersedes the older automatic-future-selection model: **Notify me about new EQs** starts ON for newly managed headphones but is attention-only. It never silently selects a future profile.

When notification is ON, eligible new EQs and materially changed selected tunings may create review attention. New review rows start unchecked; **Add selected** adds only checked rows, **Dismiss** reviews the current batch without adding/hiding/deleting unchosen EQs, and Back leaves the batch pending. Turning notification OFF clears attention without changing the stored selection.

The persisted compatibility field name `autoIncludeNewProfiles` may remain internally for migration compatibility, but it must not be interpreted as permission to auto-select future profiles.

Selections are output-specific. Favorites/personal imports may also be output-scoped without duplicating/mutating the canonical source.

## 8. Output registry and fidelity

`ExportDevice` is the single UI/domain output registry. Selectable outputs are grouped as:

- **Hardware DACs**
- **Apps**
- **Universal formats**

Each target declares presentation, file-vs-hardware semantics, format kind, capability profile, current selectability, and validation status where applicable.

Hardware-only outputs do not invent export files. Their deterministic derived representation remains local until the user explicitly taps **Flash**.

Use fidelity states consistently:

- **Exact** — source is natively representable at the target's actual limits/quantization without target-side acoustic alteration or generated headroom.
- **Optimized** — EQ Library deterministically derives a faithful target representation and it passes that target's quality/safety gates.
- **Not suitable / Not exportable** — a safe/faithful representation cannot be produced.

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

Finite PEQ hardware derivation for **TRN Black Pearl (10 bands)**, **FiiO JA11 (5 bands)**, and **stock JCALLY JM12 (5 bands)** uses the shared deterministic hardware response adapter described in `docs/V0.5_KT02H20_IMPLEMENTATION_PLAN.md`.

Rules:

- complete canonical source remains unchanged;
- unsupported source filter types fail rather than being ignored;
- exact native values pass only when target range and target quantization truly preserve them;
- otherwise fit the **complete source response** to the available target bands;
- never silently take the first N bands for these hardware targets;
- quantize only at the device boundary;
- require fixed RMS/max-error gates for Optimized output;
- generate missing-preamp safety headroom from the final quantized target response without mutating canonical preamp/headroom metadata;
- version derived representation semantics so output currentness changes when the adapter contract changes.

Historical internal class names containing `Kt02h20` or `FiveBand` are implementation-compatibility names only; do not infer a product limitation from those names.

## 11. TRN Black Pearl

Black Pearl protocol/USB behavior was physically qualified in the v0.3 foundation and Reset EQ to flat was qualified in v0.4. Preserve those protocol boundaries and fail-safe gain-reset rules.

v0.5 changes **derived DSP adaptation**, not the qualified Black Pearl USB identity:

- Direct Flash and Black Pearl file export consume the same shared device representation;
- profiles over 10 bands are complete-response fitted instead of first-10 truncated;
- exact protocol-encodable values are not silently clamped;
- per-filter gains outside the currently validated ±10 dB listening range retain the explicit affected-value caution and **Flash anyway** gate when still wire-encodable;
- unsupported/non-finite/unencodable filters and absolute global-gain limits remain blocking;
- UI Flash preview must consume the same `BlackPearlFlashPlan` as the transaction so fidelity/gain/warnings cannot disagree.

Global playback gain uses the observed `0x03` command in 1/256 dB units. Replace the previous EQ Library-applied tracked delta rather than stacking attenuation. Preserve unrelated DAC settings.

Because v0.5 changes Black Pearl response derivation, require a focused Black Pearl regression smoke on the exact release candidate even though the transport protocol itself was previously qualified.

## 12. FiiO JA11 Direct Flash

JA11 v0.5 support is hardware-only Direct Flash and is **Hardware validation pending** until its Pixel 9 checklist passes.

Strict identity/protocol facts are maintained in `docs/FIIO_JA11_PROTOCOL_NOTES.md`. Current implementation targets VID/PID `0x2972:0x0102`, HID report ID `0x02`, five Peak/Low Shelf/High Shelf bands, global EQ gain, Apply, Save, and readback.

Direct Flash toggle defaults OFF. The app must build/validate the entire target before writes, write all five slots including validated flat padding, apply, read back, save, and verify as required. Do not report success early. Reset returns all five bands and global EQ gain to flat/0 dB, verifies, saves, and verifies again.

No firmware/bootloader/cross-flash or unrelated-control commands.

## 13. Stock JCALLY JM12 Direct Flash

JM12 v0.5 support is hardware-only Direct Flash for **stock firmware** and is **Hardware validation pending** until its Pixel 9 checklist passes.

Strict identity/protocol facts are maintained in `docs/JCALLY_JM12_PROTOCOL_NOTES.md`. Current implementation targets VID/PID `0x31B2:0x0111`, HID report ID `0x4B`, the observed run-mode register protocol, five PEQ bands, EQ enable/bypass, and digital/global gain.

The implementation preserves unrelated register bytes and tracks only EQ Library's relative playback-gain delta so later Flash/Reset can replace/remove the app-applied adjustment.

There is **no independently corroborated explicit stock-JM12 Save command**. Never claim persistence across a full power cycle until physical testing proves it. If the device resets EQ/gain on power loss, app-side tracked-gain state must be made robust against stale hardware state before qualification.

No JA11 firmware is required or suggested. No firmware/bootloader/cross-flash or unrelated-control commands.

## 14. Hardware UX and approval/safety gates

Major user-facing features require UX/behavior approval before implementation. For already-approved hardware outputs:

- Settings contains the output plus an independent Direct Flash toggle, OFF by default where newly introduced;
- My EQs shows compact `Connect / Connected` + `Reset EQ to flat` controls when applicable;
- Reset is disabled while disconnected;
- Add/Save never automatically flashes hardware;
- Flash and Reset always require explicit confirmation;
- confirmation states Exact/Optimized representation, playback/global gain implications, persistence semantics, and any device-specific caution;
- unrelated DAC settings must remain untouched.

Final hardware support requires the exact-candidate Pixel 9 hands-on gate. Do not remove **Hardware validation pending** or make stable-release qualification claims before the applicable checklist passes.

## 15. Export and storage

For file-capable outputs, Add/Save initiates normal initial export once Storage Access Framework access exists. Export/Export all are recovery/currentness actions and stay hidden when expected app-owned files are current.

Use Android's system folder/document picker and persisted supported access. Suggest a sensible Documents location but let the user choose. Never request broad storage access or write to another app's private storage.

File ownership follows stable output + product + profile identity and the actual provider-returned SAF document URI, actual display name, generated fingerprint, and content hash. Never overwrite/delete an unowned same-name file. Manage/delete only files the app can prove it created.

Hardware-only outputs have no file export path.

## 16. Testing and validation

Never weaken validation simply to get green.

Treat the Python converter as the UAPP behavioral reference. Maintain golden/regression coverage for canonical normalization, preamp, filters, deterministic XML, UAPP 10-band handling, unsupported filters, naming/encoding, selection/review modes, catalog updates, removed/archived profiles, output currentness, and export ownership.

For v0.5 also require coverage for:

- output-registry categories/file-vs-hardware semantics;
- target capability/fidelity classification;
- exact hardware quantization;
- deterministic complete-response fitting;
- target-derived generated headroom without canonical mutation;
- protocol golden vectors/readback ordering/failure handling;
- wrong-device/permission/disconnect behavior;
- Direct Flash default-OFF behavior;
- reset behavior;
- Black Pearl file/Flash plan parity;
- versioned derived-representation fingerprints/currentness.

Before physical qualification, require the exact candidate to pass applicable Android unit tests, lint, debug/release assembly, catalog/currentness gates, priority-community coverage, CodeQL, dependency submission, signed-beta build/alignment/signature/certificate checks, and mobile-test publication.

Any behavior-affecting code change after a physical PASS creates a new hardware candidate and requires the relevant hands-on retest. Documentation-only changes may retain the prior hardware result only when the tested source commit is recorded clearly and APK/device/DSP behavior is unchanged; automated/software gates still rerun on the final head.

## 17. Releases, updates, attribution

Use SemVer. Development remains `0.x`; first stable is `v1.0.0`. Maintain `CHANGELOG.md` from the beginning.

Public distribution initially uses GitHub Releases and one stable release-signing identity. The app may check latest public release metadata and show a nonblocking update banner, What's new, and Get update link. No notification permission, silent APK download/install, or unknown-app install permission in v1.

Preserve OPRA and individual creator/source attribution. Do not imply endorsement by OPRA, Roon Labs, UAPP, ToneBoosters, TRN, FiiO, JCALLY, output-app vendors, or headphone manufacturers.

## 18. Communication and execution discipline

Work in clear phases and use connected GitHub tools directly whenever possible. Explain/obtain approval for genuinely new major UX before implementation. After changes, report exactly what changed and whether validation passed.

The user is not a developer. Do not push routine Git/Terminal work onto them. Ask only for decisions or physical-device steps that materially require the user. When hardware testing is required, give one safe checkpoint at a time and stop before destructive Flash/Reset actions until the preceding checkpoint is confirmed.
