# EQ Library — Architecture

This document supplements `docs/CHATGPT_PROJECT_RUNBOOK.md`. The runbook remains authoritative for product/UX rules. For current v0.5 output/device decisions also read `docs/V0.5_KT02H20_IMPLEMENTATION_PLAN.md`, `docs/V0.5_IMPORT_COMPATIBILITY_NOTES.md`, and the applicable protocol/hands-on notes.

## Android baseline

EQ Library is a single native Android application using Kotlin and Jetpack Compose.

- application ID / namespace: `com.weekssa.opraeqforuapp`
- user-facing name: **EQ Library**
- minSdk: 26
- primary physical validation device: Pixel 9
- Room: durable app-owned My EQs / selection / revision / generated-output / export ownership state
- Preferences DataStore: appearance, enabled outputs, active output, Direct Flash toggles, hidden canonical IDs, export-tree preference, update presentation state
- WorkManager: background catalog/currentness scheduling
- Storage Access Framework / DocumentFile: explicit user-folder file export and app-owned cleanup
- Android USB host/HID integration: hardware Direct Flash transports behind narrow device-specific coordinators

Do not bundle Python in the APK. Do not casually change Android/dependency baselines without compatibility review and CI.

## Architectural invariant

EQ Library is a **source-agnostic canonical EQ library** with target-specific derivation at the output boundary.

Conceptual flow:

`many sources -> authentic parse -> identity/provenance -> acoustic dedupe/revisions -> canonical EQ -> output registry -> target representation -> file export OR explicit hardware Flash`

The canonical EQ is never rewritten to fit a target. Preserve complete supported source filter count/order, types, frequency, gain, Q, creator, details/target, provenance, source preamp, and revision identity. Missing source preamp remains null. Generated safety headroom is derived metadata, not source authorship.

## Package responsibilities

### `com.weekssa.opraeqforuapp.ui`

Compose screens, shell/navigation, active-output selector, My EQs, EQ Library, Settings, confirmation dialogs, accessibility, update UX, and presentation of Exact/Optimized/Not-suitable state.

UI must not implement source parsing, DSP fitting, wire encoding, or storage ownership rules. Hardware confirmation preview must consume the same domain plan used by the transaction rather than recreating device logic in Compose.

### `com.weekssa.opraeqforuapp.domain`

Framework-independent business/DSP rules:

- canonical models and identity;
- selection/review semantics;
- saved/favorite/general/personal EQ semantics;
- output registry and capabilities;
- fidelity/exportability assessment and concise adaptation reasons;
- UAPP/ToneBoosters conversion;
- generic and target-specific file-format conversion;
- deterministic finite-target/hardware response adaptation;
- Black Pearl/JA11/JM12 transaction planning/protocol encoding where Android-free;
- deterministic naming/currentness/fingerprints;
- export planning and SemVer/update comparison.

Critical rules should remain unit-testable without Android framework dependencies.

### `com.weekssa.opraeqforuapp.data`

Android/platform-backed state and I/O:

- validated catalog acquisition and last-known-good cache;
- Room entities/DAOs/repositories and reconciliation;
- Preferences DataStore;
- WorkManager scheduling;
- SAF export ownership/currentness/cleanup;
- public release metadata;
- Android USB transports/coordinators;
- app-side hardware gain-state stores required for safe relative replacement/reset behavior.

Repository-side source discovery/crawling/qualification/publication remains outside Android runtime.

## Android runtime state and platform boundaries

The top-level Android runtime follows a manual dependency-injection / unidirectional-data-flow boundary rather than using `Activity` as a service locator.

- `MainActivity` is the Android composition/lifecycle boundary. It owns platform-only actions such as the SAF activity-result contract, persistable URI permission acquisition, external browser intents, and final Android resource-string resolution.
- `EqLibraryViewModel` owns screen-level orchestration and exposes one immutable `StateFlow<EqLibraryUiState>`. It receives repositories/coordinators through constructor dependencies and never receives an Android `Context`.
- `EqLibraryActions` is the stable event surface from Compose into the ViewModel/platform boundary. Compose should not depend on repository-specific result types merely to format operation messages.
- `EqLibraryDependencyFactory` is the manual composition root. Application `Context` may be consumed there to construct Android data sources, Room, DataStore access, USB transports, and other platform adapters; repositories/ViewModels receive those narrower dependencies instead of retaining `Context`.
- `ExportDocumentStore` is the repository-facing SAF contract. `AndroidSafDocumentStore` is the Android implementation. SAF lookup semantics explicitly distinguish **Found**, **Missing**, and **Unavailable** so cleanup may forget ownership only after confirmed absence, never after a permission/provider query failure.
- Room/DataStore remain the durable sources of truth. Transient editing/navigation state that should survive activity recreation uses Bundle-safe `rememberSaveable`; large/durable business state is not copied into Compose saved-state bundles.
- Hardware USB sessions are owned by the ViewModel-scoped `HardwareEqRepository`, so configuration changes do not replace a live session. The repository closes all transports when the ViewModel is actually cleared.
- Repository filesystem/database/network work uses explicit IO dispatcher boundaries. CPU-heavy export-record derivation/currentness preparation may use an injected computation dispatcher. Compose/UI callbacks remain nonblocking.

A configuration change must not change canonical selection, export ownership, device plan, or hardware transaction semantics. State-lifetime improvements are not permission to move DSP/wire/storage rules into UI code.

## Runtime catalog architecture

Android consumes a validated published canonical catalog and never scrapes GitHub/forums in normal operation.

Rules:

- fully parse/validate a candidate before promotion;
- retain last-known-good cache during refresh/failure/offline operation;
- failed refresh does not advance successful-currentness timestamps;
- partial/malformed candidates never replace good data;
- reconciliation is deterministic;
- source disappearance does not delete previously published acoustic history;
- ordinary source/catalog updates do not require an APK release while schema/device/DSP behavior remains compatible.

Approximately-daily WorkManager is a backup/currentness path; foreground use can opportunistically refresh stale data; Settings provides manual Refresh.

## Canonical identity, acoustic dedupe, and living archive

Identity cleanup happens before canonical acoustic dedupe. Normalize only proven-safe spelling/punctuation/manufacturer differences. Preserve explicitly sourced configurations/revisions; never invent variants from filenames/IDs.

Equivalent acoustic fingerprints may merge provenance. Materially changed same-lineage tuning becomes an immutable new revision. Formatting/provenance-only changes do not create fake acoustic revisions.

The published catalog is append/preserve history. Validation rejects silent disappearance of a published profile/revision or in-place mutation of an archived acoustic fingerprint. Source URL/availability/lifecycle is provenance metadata, not deletion permission.

Local Hide/Unhide is a presentation projection keyed by stable canonical identity. Hidden state does not delete canonical data or mutate existing My EQs/export/Favorite/Flash state.

## Selection and My EQs architecture

My EQs is output-specific while canonical source is shared.

A never-managed headphone begins with no selected EQs. Selection is always explicit. The persisted legacy `autoIncludeNewProfiles` field is notification compatibility state only; **Notify me about new EQs** may create review attention but never auto-selects future profiles.

Add/Save persists the active-output membership. For file-capable outputs it initiates normal initial export once SAF access exists. For hardware-only outputs it persists the local derived representation/currentness but performs no hardware write.

User-facing terminology keeps local membership, files, and USB actions separate: **Add to My EQs / Save** is local membership, **Export** is a verified external file, **Direct Flash** is an explicit USB write, and “saved to device/persists” is reserved for hardware whose persistence is established.

Favorites/personal imports/general EQs normalize into the same canonical/derived-output model rather than bypassing output capability rules.

## Output registry

`ExportDevice` is the authoritative UI/domain registry. An output declares:

- stable enum identity;
- display/folder name;
- category (`Hardware DACs`, `Apps`, `Universal formats`);
- file extension/MIME type where applicable;
- `OutputFormatKind`;
- settings subtitle and validation label;
- `DeviceEqCapabilities` where applicable;
- current product selectability.

`ExportDevice.selectableOutputs` is the only list Settings/output-selector UI should expose. Nonselectable registry entries may exist for implementation/reference work without becoming product features.

The active output is operating context only. It never filters canonical browse/search.

### File vs hardware semantics

File-capable outputs produce deterministic `DevicePresetVariant`/export candidates and are managed through SAF ownership/currentness.

`OutputFormatKind.HARDWARE_ONLY` targets produce no export file. Their target representation is local derived state and is consumed only by explicit Direct Flash/Reset actions.

Do not create a fake file interchange format just to make hardware-only targets fit a file API. FiiO JA11 and stock JCALLY JM12 remain fileless in v0.5 because no sufficiently verified preset interchange contract has been established; adding a future verified file path would be additive and would not remove Direct Flash.

## Capability and fidelity model

`DeviceEqCapabilities` describes target constraints such as:

- max bands (nullable for effectively unbounded text targets);
- supported filter types;
- frequency range;
- band gain range;
- Q range;
- playback/preamp range where independently represented.

Fidelity is target-specific:

- **Exact**: source is natively representable at actual target resolution without target-side acoustic alteration or generated headroom.
- **Optimized**: deterministic target derivation is required and passes quality/safety gates. Native target rounding, complete-response fitting, or generated target headroom are Optimized.
- **Not suitable / Not exportable**: a reliable representation cannot be produced.

Finite-target UI should expose the reason separately from source description, for example `source values preserved`, `native hardware rounding only`, `14 → 10 bands · full-response fit`, or `generated headroom −3.0 dB`.

Canonical selection is not gated by this status.

## UAPP / ToneBoosters path

UAPP/ToneBoosters remains a dedicated conversion contract protected by the Python reference implementation.

- maximum 10 bands;
- preserve source priority/order and first 10 applicable source-priority bands;
- explicit limitation warning;
- deterministic headphone-first naming;
- ISO-8859-1-safe XML while full Unicode metadata remains local;
- reject unsupported/out-of-contract source state rather than silently altering it.

UAPP's first-10 rule is not a generic hardware-adaptation rule.

## Generic app/universal text targets

Parametric text targets declare capabilities and use target-format serializers after capability assessment. Limited text targets may use the shared deterministic response adapter when their finite target budget requires acoustic fitting; effectively unbounded targets preserve supported source filters directly.

Wavelet/portable GraphicEQ output is a response rendering transformation and is therefore Optimized rather than pretending to preserve independent parametric/preamp controls exactly.

### TOPPING Tune import target

TOPPING Tune is a selectable **Apps** output using standard AutoEq-style parametric `.txt` and a ten-band budget. Official TOPPING material establishes direct AutoEq import, up to ten EQ bands, ±12 dB preamp and filter gain, Q 0.1..15, supported Peak/Low Shelf/High Shelf types, and direct numeric parameter entry.

Product-facing TOPPING Tune generation uses the shared finite-target response machinery when adaptation is required and never first-10 truncates a canonical source. Source preamp outside the documented Tune range is not silently clamped merely to make a file.

TOPPING's public documentation does not establish the downstream device storage quantization used after Tune imports a file. `ToppingTuneFileExport` therefore treats its deterministic steps as **text serialization precision only**, not hardware resolution, and conservatively reports TOPPING Tune output as Optimized until import/storage precision is independently qualified. Hidden DX5 II / DX1 II registry entries remain nonselectable implementation/reference state and are not Direct Flash qualification.

## Shared finite-hardware response adapter

The deterministic hardware response adapter currently serves:

- `HardwareEqDeviceSpecs.TRN_BLACK_PEARL` — 10 bands;
- `HardwareEqDeviceSpecs.FIIO_JA11` — 5 bands;
- `HardwareEqDeviceSpecs.JCALLY_JM12_STOCK` — 5 bands.

The same response machinery may be reused by a finite app/file target such as TOPPING Tune, but hardware physical capability/quantization claims remain separate from file text precision.

The legacy Kotlin names `Kt02h20FiveBandOptimizer`, `FiveBandDeviceSpec`, etc. remain compatibility names; the implementation contract is generic finite-target response adaptation.

### Exact path

For a supported hardware source:

1. parse every source band safely;
2. verify source count and every parameter against physical target capability;
3. quantize using the target's actual storage/wire resolution;
4. call the representation Exact only if quantization preserves every source band and a source-authored playback gain exactly, with no generated headroom.

### Native-rounding path

If the source already fits the target band structure and supported ranges but one or more values require only native target quantization:

1. keep the same source filter structure;
2. quantize those values at the target boundary;
3. compare the quantized response against the source response and require the same quality gates;
4. report **Optimized · native hardware rounding only** rather than invoking a different response fit.

This distinction prevents harmless native rounding from being conflated with structural/acoustic fitting.

### Optimized response-fit path

When direct/native-rounded representation is impossible but source semantics are supported:

1. evaluate the **complete source response** over a fixed log-frequency grid;
2. seed deterministic target-compatible candidates from the source;
3. fit/refine no more than the physical target's available bands;
4. quantize every candidate to target resolution;
5. compare quantized target response with source response;
6. require target-specific RMS and max-absolute-error gates;
7. return Not suitable if those gates cannot be met.

Never silently truncate source bands for Black Pearl/JA11/JM12.

### Playback gain / generated headroom

Source preamp is not a fit parameter. If source preamp exists, map/quantize it separately and fail if target playback-gain capability cannot represent it safely.

If source preamp is absent, derive conservative headroom from the final quantized target response. Mark that representation generated/**Optimized** and leave canonical `preampGainDb` and `eqLibrarySafetyHeadroomDb` untouched. Generated target headroom can never qualify as Exact because it was not source-authored.

### Representation versioning

Each device specification carries a representation version. Generated fingerprints/currentness include target identity + representation version so an intentional DSP-adapter contract change makes stale derived output detectable without mutating canonical fingerprints.

## Hardware specification registry

`HardwareEqDeviceSpecs` is the authoritative physical-capability/quantization/error-gate registry for Direct Flash hardware. Device-specific protocol classes remain separate.

The capability profile may distinguish physical wire ranges from conservative optimizer search ranges. A target may be able to encode a value that EQ Library does not choose during fitting; exact source-preserving transmission and optimizer search are not the same policy.

## TRN Black Pearl architecture

Black Pearl transport/protocol was qualified in v0.3 and flat reset in v0.4.

v0.5 routes Black Pearl file export and Direct Flash through the same shared 10-band device representation. This supersedes the earlier v0.3 first-10 Black Pearl adaptation rule while leaving the UAPP first-10 rule intact.

Key safety properties:

- current active EQ slot is respected;
- global playback gain reads current hardware state and replaces the previous EQ Library-applied tracked delta instead of stacking it;
- complete PEQ state is written/padded/latch-saved according to the qualified protocol;
- protocol-encodable per-filter gains outside the currently validated ±10 dB listening range remain unchanged but require the explicit affected-value caution / `Flash anyway` gate;
- hard unsupported/non-finite/unencodable/global-gain limits fail, never clamp;
- confirmation preview is built from the actual `BlackPearlFlashPlan` used by Flash.

Black Pearl file export serializes the same derived filters/playback gain in the verified pyBlackPearl AutoEq syntax: `PK / LS / HS`. pyBlackPearl's importer limits preamp to approximately `-16..+6 dB`; EQ Library still writes the true derived value and discloses importer-side adjustment rather than clamping. The external file-import constraint is independent of Direct Flash. Do not claim generic compatibility with controllers whose importers do not preserve shelf types.

Reset retains the v0.4 fail-safe ordering: validate recoverable baseline, flatten the current slot, latch/save, then restore only EQ Library's tracked playback-gain delta and clear tracking after success.

## FiiO JA11 architecture

JA11 Direct Flash is a separate transport/protocol implementation with strict identity. Current target:

- VID/PID `0x2972:0x0102`;
- HID report ID `0x02`;
- five Peak/Low Shelf/High Shelf bands;
- filter parameters command `0x15`;
- global EQ gain `0x17`;
- Apply `0x18`;
- Save `0x19`.

Android dynamically locates the HID interface containing interrupt IN + OUT endpoints. It does not assume an interface index.

Flash preflights/readbacks device state, validates the entire target before destructive writes, writes all five slots including flat padding, writes global gain, Applies, verifies, Saves, and performs final verification. Success is not reported early.

Reset writes five flat bands/global 0 dB, Applies/verifies/Saves/verifies.

No verified JA11 external preset-file format is exposed in v0.5. Direct Flash remains intact.

JA11 remains **Hardware validation pending** until its exact signed Pixel 9 checklist passes.

## Stock JCALLY JM12 architecture

JM12 stock-firmware support is separate from JA11 despite chipset-family similarity.

Current target:

- VID/PID `0x31B2:0x0111`;
- HID report ID `0x4B`;
- 11-byte run-mode register protocol;
- handshake `0x43`, read `0x52`, write `0x57`;
- EQ enable/bypass register `0x24`;
- five PEQ bands in `0x26..0x2F`;
- digital/global gain register `0x66`.

The protocol layer preserves unrelated register bytes. Flash tracks EQ Library's relative playback-gain delta, bypasses EQ before replacing state, writes/verifies all target band registers and gain, and re-enables EQ only after full success. Partial failure remains fail-safe/bypassed rather than reporting a successful mixed state.

No independently corroborated explicit persist/save command is used. Power-cycle persistence remains a physical qualification question. If power loss resets the hardware while DataStore retains a prior app-side gain delta, the tracking contract must be hardened before qualification.

No verified stock-JM12 external preset-file format is exposed in v0.5. Direct Flash remains intact.

JM12 remains **Hardware validation pending** until its exact signed Pixel 9 checklist passes.

## Android USB architecture

USB device selection is strict VID/PID for the active hardware output. Do not connect to a generic chipset-family device merely because it resembles a supported target.

The HID session:

- discovers a matching USB device;
- requests Android user permission as required;
- locates an interface with interrupt IN and OUT endpoints dynamically;
- claims/releases it through the Android USB lifecycle;
- exposes narrow read/write transactions to device-specific transport code;
- transitions to error/disconnected state safely on permission denial/removal/transfer failure.

No firmware-update/bootloader path exists in the app.

## Hardware gain-state stores

When applying an EQ requires modifying a device-global playback gain that users can also change independently, EQ Library stores only the delta it applied so later Flash/Reset can replace/remove its own adjustment rather than assuming ownership of absolute user volume.

The store is not evidence that hardware still contains the prior state after a power loss. Every device contract must validate physical persistence/reconciliation semantics before qualification.

## Export/currentness architecture

File export uses SAF and app-owned tracked identity:

- preferred deterministic name is presentation, not ownership identity;
- stable ownership identity includes output/product/profile plus the actual provider-returned document URI;
- store actual display name, generated fingerprint, and content hash;
- provider filename normalization is accepted and tracked;
- an unowned same-name file is never overwritten/deleted;
- internal same-name presets receive stable identity-derived fallback names;
- app-owned files are updated through tracked URI;
- cleanup deletes only proven app-owned files;
- per-file failures do not roll back unrelated successes.

`Export` / `Export all` are currentness-recovery controls and remain hidden when expected app-owned artifacts are present/current.

Hardware-only outputs bypass file export entirely.

## Updates, attribution, privacy

Public app updates use GitHub Release metadata at modest cadence with nonblocking What's new / Get update UX. No silent self-update, install-unknown-apps permission, notification permission, or credentialed backend is required.

Preserve OPRA and individual creator/source attribution. Do not imply endorsement by OPRA, Roon Labs, UAPP, ToneBoosters, TRN, FiiO, JCALLY, TOPPING, app-output vendors, or headphone manufacturers.

No analytics or telemetry. Local selections/settings/generated state stay on-device.

## Validation architecture

Automated gates protect both canonical and target-specific behavior:

- Kotlin UAPP parity/golden tests;
- canonical identity/archive/reconciliation tests;
- selection/review/visibility tests;
- export ownership/currentness tests;
- output registry/capability/fidelity tests;
- hardware response-adapter deterministic/golden/error-gate tests, including native-rounding vs response-fit behavior and generated-headroom fidelity;
- Black Pearl file syntax/plan parity and pyBlackPearl importer-caveat tests;
- TOPPING Tune AutoEq syntax/ten-band fitting/no-clamp/conservative precision-pending fidelity tests;
- Black Pearl/JA11/JM12 protocol and staged-failure tests;
- Android unit tests, lint, debug/release assembly;
- catalog/currentness and priority-source validation;
- CodeQL/security/dependency submission;
- signed-beta alignment/signature/pinned-certificate verification and mobile-test publication.

Physical device support requires the exact signed candidate plus Pixel 9 hands-on checklist. A behavior-affecting change after a hardware PASS invalidates that candidate for the affected hardware.

Because v0.5 changes Black Pearl DSP derivation through the shared adapter, the exact v0.5 candidate also requires a focused Black Pearl regression smoke even though its USB protocol was previously qualified.
