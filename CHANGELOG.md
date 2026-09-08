# Changelog

All notable changes to **OPRA EQ for UAPP / EQ Library** will be documented in this file.

The project uses Semantic Versioning. Development releases remain in the `0.x` series until the first stable `v1.0.0` release.

## [Unreleased]

### Added

- Scheduled public GitHub/Gist community ingestion now takes discovered headphone PEQ through exact source retrieval, strict PEQ parsing, canonical headphone identity, creator/source provenance, acoustic dedupe, living-archive validation, and Unverified publication instead of leaving mechanically valid community data indefinitely review-only.
- Community-ingestion reports record fetched/parsed/published/deduplicated/quarantined counts and machine-readable quarantine reasons; one malformed or ambiguous record cannot block unrelated valid candidates.
- Broad General GitHub discovery is actively audited for exact parametric structure. Fixed/graphic-EQ data without source-provided Q/filter types is rejected rather than converted by invention.
- Added bounded public RSS/thread automation for Head-Fi and Audio Science Review, with exact-PEQ parsing, provenance retention, identity resolution, quarantine/failure isolation, source-health state, and living-archive-safe publication.
- Added automated Squiglink ecosystem currentness using the public Squiglink-compatible site registry without turning measurement curves into invented source-authored PEQ.
- Added the unified `Automated source currentness` GitHub Actions lane for cadence-aware Head-Fi/ASR refresh, Squiglink currentness, qualified General-source probing, atomic archive validation, persistent source-health state, and `catalog-live` publication from `main`.
- Added an automation contract regression test that requires zero recurring `manual` currentness owners in the registered-source set.

### Changed

- `github-community` is a daily scheduled source with source-health ownership. Public exact structured community PEQ uses the established Unverified community policy while specific source restrictions remain binding.
- Source maintenance is now automation-first: every registered source with a legitimate stable public retrieval path is scheduled or runtime-managed; sources that cannot currently be automated safely are explicitly paused rather than depending on recurring manual input.
- Head-Fi and Audio Science Review moved from manual/curated currentness to weekly scheduled public adapters.
- Squiglink-compatible sources moved from manual intake to weekly scheduled ecosystem-currentness/provenance monitoring. Measurement-only `phone_book`/frequency-response data remains non-PEQ and is never converted into fabricated source-authored filters.
- ParaEQ General presets moved to weekly scheduled qualified-source currentness probing; upstream source changes remain review-gated rather than silently mutating qualified preset classification or acoustic history.
- Topping Community is paused instead of relying on recurring manual capture. It can become scheduled only when TOPPING provides an authorized public API/feed or explicit permission for the required automated retrieval.
- oratory1990 direct Reddit currentness is paused while Reddit access is unavailable; structured values may continue to arrive through separately automated qualified carriers such as OPRA.
- Reddit remains paused; no anonymous Reddit scanning, scraping, circumvention, or manual-currentness substitute was introduced.

### Validation

- Initial GitHub community ingestion fetched all 50 current headphone candidates: 39 parsed as exact supported PEQ, producing 28 new Unverified profiles and 11 exact-duplicate provenance merges; 11 unmatched/ambiguous headphone identities were quarantined. Atomic living-archive validation passed before the candidate catalog was committed.
- All three current broad General GitHub candidates were processed and classified `no_exact_parametric_structure`; none was published with invented Q/filter types.
- Added regression coverage for valid Unverified publication, missing-preamp preservation, exact-duplicate provenance merging, short model identity with manufacturer context, target-folder handling, malformed/unsupported PEQ quarantine, unknown headphone quarantine, and General graphic-EQ rejection.
- Live branch automation recorded successful source-health scans for Head-Fi, Audio Science Review, Squiglink ecosystem currentness, and ParaEQ, with zero consecutive failures for those newly automated lanes.
- The unified automated-source workflow passed its adapter/automation-contract tests and atomic living-archive validation on the feature branch before final PR closeout.

## [0.5.0] - Unreleased

### Added

- A unified output registry that groups selectable targets as **Hardware DACs**, **Apps**, and **Universal formats**, with one declared source of truth for display labels, file-vs-hardware behavior, format kind, capability profile, and validation status.
- Additional selectable app/universal output contexts, including Equalizer APO-style parametric text, EasyEffects-compatible parametric import, portable AutoEq GraphicEQ, and **TOPPING Tune** AutoEq text alongside the existing UAPP/ToneBoosters, Poweramp, and Wavelet paths.
- **TOPPING Tune** as a selectable App output using documented AutoEq `.txt` import, a 10-band finite-target adapter, documented ±12 dB preamp/filter-gain and Q 0.1–15 limits, and conservative Optimized fidelity until downstream device storage precision is independently qualified.
- **FiiO JA11** as a hardware-only five-band PEQ output with an independent Direct Flash toggle (OFF by default), strict USB identity, read/apply/readback/save verification, and Reset EQ to flat.
- **JCALLY JM12** on stock firmware as a separate hardware-only five-band PEQ output with its own Direct Flash toggle (OFF by default), strict USB identity/register protocol, readback verification, fail-safe EQ bypass during replacement, tracked EQ Library playback-gain delta, and Reset EQ to flat.
- A shared deterministic finite-hardware response adapter used by TRN Black Pearl (10 bands), FiiO JA11 (5 bands), and stock JCALLY JM12 (5 bands). It preserves native exact representations where target quantization permits and otherwise fits the complete source response under fixed RMS/max-error gates.
- Versioned derived-representation semantics/fingerprints so a hardware/output adaptation-rule change makes previously generated target state detectably stale without changing canonical source fingerprints.
- Device-specific JA11/JM12 protocol notes and Pixel 9 hands-on qualification checklists.

### Changed

- Hardware adaptation no longer treats over-budget Black Pearl/JA11/JM12 profiles as a first-N truncation problem. The shared adapter evaluates and fits the **complete canonical response** while retaining the complete source unchanged.
- Black Pearl file export and Direct Flash now consume the same shared 10-band device representation. This supersedes the v0.3 Black Pearl first-10 adaptation rule; the independent UAPP/ToneBoosters first-10 rule remains unchanged.
- Black Pearl `.txt` export now follows the verified pyBlackPearl AutoEq importer syntax for shelves and peaks (`PK` / `LS` / `HS`) while generic AutoEq outputs keep their own standard tokens. The file preserves the same derived bands and true playback preamp as Direct Flash instead of clamping values to a third-party importer's convenience range.
- If Black Pearl playback preamp falls outside pyBlackPearl's observed `-16..+6 dB` import range, EQ Library keeps the true exported value and surfaces the importer limitation; Direct Flash remains an independent hardware path and is not disabled merely because a third-party importer will adjust the text value.
- Missing source preamp for finite hardware is handled by conservative target-specific headroom derived from the final quantized target response. Generated headroom is representation metadata and never rewrites canonical `preampGainDb` or canonical safety-headroom metadata.
- Exact/Optimized classification now accounts for actual device quantization rather than only broad parameter ranges/band count. Native target rounding, generated headroom, and complete-response fitting are explicitly Optimized and expose concise reasons separately from safety cautions.
- TOPPING Tune over-budget profiles use the complete-response finite-target fitter rather than silently taking the first 10 source bands. Source preamp outside Tune's documented range is rejected instead of clamped, while the public lack of downstream storage-resolution documentation prevents an Exact product claim for now.
- Hardware-only outputs explicitly produce no file export artifact. FiiO JA11 and stock JCALLY JM12 therefore keep Direct Flash without inventing a preset-file interchange format; Add/Save stores output-specific state but never automatically writes hardware.
- Settings nests device-specific Direct Flash controls under enabled hardware outputs and continues to present JA11/JM12 as **Hardware validation pending** until their physical gates pass.
- Signed-beta publication now supports `v0.*` development branches and produces stable plus versioned mobile-test APK names for exact-candidate hardware testing.
- Android app orchestration now follows explicit MAD-style boundaries: `MainActivity` is the lifecycle/platform boundary, `EqLibraryViewModel` owns immutable `StateFlow` UI state, a manual composition root constructs Android data sources, repositories use injected dispatcher/platform contracts instead of retaining `Context`, and hardware USB sessions survive configuration changes until the ViewModel is actually cleared.
- Transient selection/export UI state that users expect to survive activity recreation now uses Bundle-safe saved state, including unsaved headphone profile selections, General EQ batch selections, What’s New presentation state, and the pending export intent while Android’s folder picker is open.

### Fixed

- Black Pearl Flash confirmation preview now consumes the actual `BlackPearlFlashPlan` used by the transaction instead of guessing Exact/Optimized from the source band count, preventing UI fidelity/gain/warning disagreement with the state that would be written.
- Black Pearl informational adaptation reasons are now separate from hardware safety cautions, so ordinary Optimized plans use the normal **Flash** action while **Flash anyway** is reserved for an actual caution such as a protocol-encodable filter gain outside the currently validated ±10 dB range.
- Managed hardware rows and Flash confirmations now distinguish reasons such as source values preserved, native hardware rounding, complete-response band fitting, and generated headroom rather than presenting every Optimized result as the same generic transformation.
- Stock JM12 digital-gain register updates preserve unrelated register bytes instead of reconstructing bytes that EQ Library does not own.
- Shared hardware adaptation rejects unsupported source filters and quality-gate failures instead of silently dropping or clamping them.
- Obsolete tests that encoded pre-v0.5 first-N Black Pearl behavior were replaced with regression coverage for the approved complete-response/shared-adapter contract rather than weakening validation.
- The SAF export/cleanup boundary now distinguishes a confirmed missing owned document/path from a provider/permission lookup failure, so transient access failures cannot be mistaken for deletion and cannot silently discard EQ Library’s ownership record.

### Validation

- Automated coverage includes native/quantized Exact behavior, deterministic over-budget response fitting, target-derived headroom without canonical mutation, quality-gate rejection, Black Pearl file/Flash representation parity and `PK`/`LS`/`HS` import syntax, TOPPING Tune finite-target behavior and conservative fidelity, device protocol golden vectors, full-slot overwrite/padding, readback/failure ordering, wrong-device protection, Direct Flash default-OFF state, reset behavior, output registry/file-vs-hardware semantics, safety-warning/adaptation-reason separation, and versioned currentness.
- The v0.5 branch was synchronized with the newer `main` catalog/currentness state before final candidate validation so the release candidate does not drop current published catalog data.
- Final v0.5 qualification still requires an exact signed candidate after all software/documentation closeout changes, followed by the applicable Pixel 9 physical gates.
- FiiO JA11 and stock JCALLY JM12 remain **Hardware validation pending** until their exact-candidate hands-on checklists pass.
- Because v0.5 changes Black Pearl DSP derivation through the shared response adapter, the exact v0.5 candidate also requires a focused Black Pearl regression smoke even though the Black Pearl transport protocol and v0.4 flat-reset behavior were previously physically qualified.
- Stock JM12 power-cycle persistence is not claimed: no independently corroborated explicit Save command is used, and persistence/tracked-gain reconciliation must be resolved by the physical checklist before qualification.

## [0.4.0] - 2026-09-06

### Added

- TRN Black Pearl **Reset EQ to flat** from My EQs. When Black Pearl is the active output and Direct Flash is enabled, the compact Black Pearl control row places **Connect/Connected** beside an outlined reset action; Reset is enabled only while connected and requires confirmation.
- Reset uses the DAC's current EQ slot, overwrites all 10 hardware bands with zero-gain flat bands, latches/saves the slot, and removes only the playback-gain delta previously tracked as applied by EQ Library. It is a hardware action, not a catalog/General/Favorite/My EQs preset and does not create an export file.

### Changed

- Black Pearl flat reset uses fail-safe ordering: validate the recoverable baseline gain first, flatten/latch/save the EQ slot before restoring playback gain, and clear the tracked EQ Library gain delta only after the gain restoration succeeds. A PEQ failure therefore leaves the prior playback gain/tracked state intact; a final gain-write failure retains the tracked delta for safe retry.

### Validation

- Added domain regression coverage for active-slot preservation, all-ten-band zero-gain reset, no-op gain restoration, unsafe baseline rejection, PEQ-transfer failure, final gain-restore failure, and retry-safe tracked-gain behavior.
- Exact hardware candidate `15f220bd055a2aec49c0cb97c16acbd43ac588da` passed Android unit tests, lint, debug/release assembly, CodeQL, signed-beta alignment/signature verification, and the pinned release-certificate check. The signed candidate SHA-256 was `96d9ea12caf8c7944ecd059f7fdda533d1c936c5ed9583910a3d3ab01168c3cf`.
- The same signed candidate passed the focused Pixel 9 / TRN Black Pearl Reset EQ to flat hands-on qualification on 2026-09-06. Sections 1–7 of the focused checklist passed; controlled mid-transfer failure injection was not required on hardware because the retry/failure ordering is covered by automated domain tests.
- The final v0.4.0 release-preparation head changes only version/release/documentation metadata after the hardware-qualified device/DSP commit, so it must repeat automated/release signing gates but does not require another Black Pearl hands-on pass unless Android/device/DSP behavior changes again.

## [0.3.0] - 2026-08-31

### Added

- A per-headphone **Notify me about new EQs** review preference in My EQs. It starts ON for newly managed headphones, can surface newly published verified or unverified EQs and changed selected tunings for explicit review, and never silently selects a profile.
- A none-selected-by-default new-EQ review flow: **Add selected** adds only explicitly checked new profiles, **Dismiss** marks the current batch reviewed without adding/hiding/deleting unchosen profiles, and Back leaves the batch pending.
- A **living canonical archive** regression gate that rejects candidate catalog publication if a previously published genuine canonical profile/revision disappears or an archived revision's acoustic fingerprint changes in place.
- Reversible global local **Hide/Unhide** for headphone and General EQ lineages, with Hide in EQ Library, batch General Hide, persisted stable canonical IDs, and **Settings → Hidden EQs** batch Unhide without deleting archive/My EQs/export state. Hidden lineages are also suppressed from new-EQ review attention while hidden.
- A dedicated personal-EQ import surface with compact **+ Import**, explicit clipboard Paste and Android Choose file actions, **Equalizer APO / AutoEq text** content recognition, authoritative preamp/filter preview, and strict malformed/unsupported active-filter validation.
- Initial populated **General EQ** catalog from the qualified MIT-licensed ParaEQ built-in presets: Bass Boost, Vocal Clarity, Treble Boost, Loudness, Podcast, Electronic, and Rock, with source-authored Genre classification only where explicitly provided.
- Managed preset rows in **My EQs** now expose the same Favorite star state/action as EQ Library.
- Global active-output context across **My EQs** and **EQ Library**, with locally enabled outputs in Settings. Initial selectable outputs are UAPP/ToneBoosters, TRN Black Pearl, Universal Parametric EQ, Poweramp/Poweramp Equalizer, and Wavelet.
- Output-specific **My EQs** collections for managed headphone selections, General EQs, favorites/saved snapshots, and personal imports. Existing pre-output-context saved state migrates to UAPP/ToneBoosters.
- **Headphones** and **General EQs** library sections, with General EQ filters for **All**, **Sound**, **Genre**, and **Utility**.
- Device-independent canonical source usability plus active-output **Exact**, **Optimized**, and **Not exportable** status. A valid canonical curve remains visible/selectable even when the active output cannot represent it.
- Multi-source canonical catalog foundation for OPRA, AutoEq, qualified creator/repository data, public community EQ submissions, immutable acoustic revisions, provenance, verification state, and general presets.
- **Unverified** community EQ presentation with original source links and manual selection. Unverified profiles may appear in explicit new-EQ review but are never silently selected.
- Conditional **Export all** and per-item **Export** recovery actions based on the current active-output candidate, app-owned SAF ownership metadata, generated fingerprint/content hash, and the actual exported document when available.
- Optional TRN Black Pearl **Direct Flash** from My EQs, including DAC connection state, current-slot discovery, confirmation, source-preamp/headroom playback-gain adjustment through the observed global-gain command, PEQ transfer, and success/error reporting.
- Independent Black Pearl EQ protocol implementation for native Peak, Low Shelf, and High Shelf filters with a 10-band hardware limit and explicit unused-band flattening.
- Black Pearl Flash cautions for protocol-encodable per-filter gains outside the currently validated `-10 dB..+10 dB` range, with exact-value/no-clamp disclosure and explicit **Flash anyway** confirmation while keeping hard protocol/global-gain limits blocking.
- Output-scoped Room association tables and non-destructive migrations for managed headphone selections, General EQs, and favorites/personal imports.

### Changed

- The earlier automatic-future-selection model is superseded for final v0.3: a never-added headphone starts with no EQ profiles selected; future EQs are never silently selected; newly managed headphones instead default **Notify me about new EQs** ON as an attention-only preference.
- The legacy persisted/domain field name `autoIncludeNewProfiles` is retained through the v0.3 migration boundary for compatibility, but its final meaning is notification/review only and it no longer authorizes automatic selection.
- General EQ review now uses none-selected-by-default batch controls with **Select all**, **Select none**, **Save selected**, and **Hide selected**; batch Save initiates the normal active-output initial export.
- Personal EQ import now normalizes supported file/paste contents into the device-independent canonical PEQ before output conversion. Filename extension does not select the converter, missing preamp remains null, full supported filter count is retained canonically, and successful Save initiates active-output export without automatically flashing hardware.
- Android Back now follows the in-app hierarchy and returns root EQ Library/Settings to My EQs; only Back from the My EQs root exits the app. Clean and dirty preset-selection editor states both handle system Back naturally.
- General EQ selection now initiates its initial active-output export when added, matching the established Add/Save workflow.
- General presets with no source preamp keep preamp null while EQ Library stores conservative generated playback headroom separately.
- Output selection is now an operating context rather than a catalog filter. Choosing a device/app changes conversion, export, Flash availability, capability status, and the My EQs collection, but never hides valid library curves.
- The obsolete prototype setting **Show presets that none of my devices can export** is ignored/removed from user-facing behavior.
- Add/Save persists the selected EQs and initiates their export for the active output. Normal Export/Export all controls stay hidden while the expected app-managed files are present and current, and reappear only for recovery when files are missing or stale.
- Export ownership/currentness now follows stable output/product/profile identity and the exact SAF document URI returned for an app-created file instead of requiring the provider to preserve the originally requested display name byte-for-byte.
- Internal same-name presets receive stable identity-derived filenames, and a same-name unowned external file is preserved while EQ Library creates a separately named app-owned fallback instead of leaving the preset in a permanent retry conflict.
- Selection and Select all/none are based on canonical source usability and trust/history state, not UAPP compatibility; notification state never changes selection.
- UAPP/ToneBoosters compatibility is enforced only at the UAPP conversion/export boundary. UAPP XML is optional generated state rather than a prerequisite for saving a canonical EQ.
- Personal PEQ imports preserve a missing source preamp as null rather than silently inventing `0 dB`.
- TRN Black Pearl conversion/Flash preserves corroborated native shelf/peak filter types instead of approximating shelves with synthetic peaking filters.
- Black Pearl direct Flash applies the selected profile's required preamp/headroom through the observed `0x03` global playback-gain command in 1/256 dB units. It reads the current gain, replaces the previous EQ Library-applied adjustment instead of stacking reductions, allows a later 0 dB preset to restore that prior adjustment, and fails rather than clamping if the requested absolute gain is outside the validated device range.
- Black Pearl file export preserves finite source filter gain exactly even when it is outside the currently validated ±10 dB Direct-Flash range; Direct Flash sends the exact protocol-encodable value only after the explicit caution rather than rejecting or clamping it.
- Black Pearl profiles over 10 bands use the first 10 source-priority bands only with an explicit Optimized warning; canonical source data remains complete and unchanged.
- Navigation and terminology now use **My EQs**, **EQ Library**, and **Settings** instead of the earlier My Headphones/Browse OPRA framing.
- EQ Library browse and Settings now use a source-agnostic task-first information hierarchy: OPRA/source attribution and feedback/submission links no longer occupy primary browse real estate and remain available in Settings.
- Managed-headphone detail now uses one compact side-by-side **Connect/Connected** + **Manage presets** action row when Black Pearl is active, and pending `new` / `updated` review attention is shown as an inline tappable status beside the selected/available counts instead of a separate full-width review button.

### Fixed

- Completing a new/updated-EQ review now clears its attention status immediately on the managed-headphone detail screen; managed profile review flags are observed reactively instead of waiting for an unrelated UI refresh.
- Newly discovered EQs no longer become selected automatically under any notification setting.
- Hidden canonical lineages no longer produce persistent new/updated-EQ review attention while hidden; suppression is presentation-only and preserves already-selected My EQs/export/Favorite/Flash state.
- Personal import no longer accepts a valid subset while silently dropping a malformed or unsupported active Filter line; the strict import layer blocks Save and identifies the parse problem.
- System Back no longer falls through and exits the activity from clean nested management screens or secondary top-level destinations.
- Favorites no longer require returning to EQ Library merely to star/unstar a managed preset.
- The previously empty General EQ user-facing area now has qualified source-backed Sound, Genre, and Utility content.
- Export status no longer stays permanently available after an output file is current; it is recalculated after export and when the active output, folder, or saved collection changes.
- SAF providers that normalize or alter the requested filename no longer cause EQ Library to delete a successfully created preset and leave it permanently stuck in **needs export / needs review**. The provider-returned URI/name is retained and used for later currentness, updates, and cleanup.
- Same-name unowned files no longer create an unrecoverable export loop; EQ Library leaves the external file untouched and creates a stable separately owned fallback file.
- Two app-managed presets that resolve to the same preferred human-readable filename no longer block each other; stable identity-derived suffixes keep both exportable.
- Removing a favorite/personal EQ from one output no longer implicitly removes it from another output where it is still selected.
- A valid canonical profile that is unsupported by UAPP no longer becomes unselectable solely because of UAPP limits.
- ToneBoosters conversion now explicitly rechecks UAPP-specific compatibility so device-independent selection cannot bypass the established UAPP filter/range/preamp safety gate.
- Removed the superseded duplicate app shell that caused stale Black Pearl call signatures to break Android CI.

### Validation

- The final new-EQ review behavior adds regression coverage for empty first-time selection, notification-only future discovery, exact stored selection, hidden-lineage review suppression, and preservation of selected source state.
- All interim PR #4 signed candidates produced before the final notification/review/documentation sync are superseded. The final synchronized exact head must repeat Android unit/lint/debug/release assembly, catalog/currentness, priority-community, CodeQL, dependency submission, signed-beta alignment/signature verification, and focused Pixel 9 validation before public publication.
- Final release-polish changes remain isolated from Black Pearl protocol/DSP code, so the focused Pixel pass needs only an ordinary Black Pearl regression smoke unless a later diff touches device/DSP behavior.
- The final Pixel 9 delta pass also verifies the compact managed-headphone action row and that **Connect/Connected** and **Manage presets** retain their existing behavior after the layout-only compaction.
- Exact candidate `c70c523e1f530b8b197ebbccc41dfb4af1e27fc4` passed the earlier full Android/software/signing gates and Pixel 9 / TRN Black Pearl foundation qualification on 2026-08-31, including provider-adjusted SAF filename/collision recovery, playback-gain replacement/non-stacking behavior, and the Edition XS Altruistic-Farmer275 `13,500 Hz / -11.9 dB / Q 4.0` file-export/caution/Flash-anyway test without app-side clamping.
- PR #3 was then fast-forward merged to `main`, preserving that tested candidate as the merge commit; PR #4 remains draft/unmerged until its fresh exact-head focused hands-on PASS.

## [0.2.0] - 2026-08-28

### Added

- Visible product rebrand to **EQ Library** while preserving application ID `com.weekssa.opraeqforuapp` and the permanent Android release-signing identity for in-place upgrades.
- Explicit one-device-at-a-time export chooser for UAPP / ToneBoosters, TRN Black Pearl, Topping DX5 II, and Topping DX1 II.
- Device-first root-folder layout: device → manufacturer → headphone → exported preset.
- TRN Black Pearl text conversion constrained to a maximum of 10 PK filters to avoid passing unsupported/broken shelf filters through directly.
- Topping Tune text output for DX5 II and DX1 II, marked hardware-validation pending until physical devices are available.
- Full saved-library cleanup in addition to existing single-preset and single-headphone cleanup.

### Changed

- Selecting or deselecting EQ profiles is now a library-management action only; it does not automatically export or delete files.
- New-headphone preset selection is non-destructive and no longer warns that unselected default profiles will be removed.
- Export now requires an explicit target-device choice and writes only that target format.
- Cleanup actions are separated from ordinary selection and can optionally remove only files created by EQ Library.

### Validation

- Android CI and CodeQL passed on the exact beta commit promoted to release.
- The permanently signed beta candidate passed test/lint/release assembly, APK alignment, pinned signing-certificate fingerprint verification, and SHA-256 generation.
- Hands-on testing passed for in-place upgrade, OPRA browsing/selection, revised selection behavior, device-targeted export, app-owned file cleanup, UAPP import, and TRN Black Pearl import.
- DX5 II and DX1 II export formats remain implemented but hardware-untested.

## [0.1.0] - 2026-08-16

### Added

- Native Android app for `com.weekssa.opraeqforuapp`, minSdk 26, targeting Android 16 / API 36 with Kotlin and Jetpack Compose.
- Approved **My Headphones** and **Browse OPRA** navigation, Settings, appearance preferences, profile-visibility preferences, accessibility semantics, and Android Back behavior.
- Runtime OPRA `database_v1.jsonl` download with full-candidate validation, last-known-good local cache, offline Browse/Search after first sync, manual Refresh, and approximately daily WorkManager checks.
- Manufacturer → Model browsing and local manufacturer/model search without runtime GitHub scraping or bundled headphone data.
- Room-backed managed-headphone state with exact selections, explicit exclusions, automatic future-profile inclusion, review state, retained removed profiles, generated XML, and app-owned export records.
- Native Kotlin OPRA → UAPP/ToneBoosters conversion with golden/reference parity tests, deterministic XML, OPRA preamp/frequency/gain/Q preservation, supported `peak_dip` / `low_shelf` / `high_shelf` mappings, first-10 priority handling for ToneBoosters' 10-band limit, deterministic naming, and ISO-8859-1-safe exported XML/name handling while full Unicode metadata locally.
- Explicit **Not compatible** handling for unsupported or unsafe OPRA profiles; no silent approximation, clamping, dropping, or invented creator metadata.
- `Creator information missing` handling for otherwise safely convertible profiles with missing OPRA creator data.
- Deterministic reconciliation for new, changed, removed, and newly incompatible managed profiles, including retention of last-good generated state where required.
- Android Storage Access Framework export with persisted tree access, Manufacturer/Model folder layout, deterministic filenames, app-owned file tracking, safe same-name conflict handling, and optional cleanup of files created by this app.
- Per-headphone **Export XMLs** plus bulk **Export presets** from My Headphones.
- Public GitHub Release update checks, SemVer comparison, non-blocking update UX, What’s new, and browser handoff without silent download/install or APK-install permission.
- OPRA attribution, CC BY-SA 4.0 data attribution, privacy disclosure, non-endorsement language, software provenance, and individual creator/source preservation where OPRA provides it.
- Original **Equalizer Headphones** adaptive launcher icon with round and monochrome/themed-icon treatment.
- Android CI covering unit tests, lint, debug assembly, and unsigned release assembly; advanced Kotlin CodeQL; Dependabot/security hardening; and a protected `main` branch ruleset.
- Permanent GitHub-distribution release-signing identity with a repository-pinned public certificate fingerprint and a controlled candidate/publish GitHub Actions workflow.

### Fixed

- A never-managed headphone with default compatible profiles already selected can be added immediately without forcing an artificial checkbox change.
- Optional saved-preset cleanup uses retained Storage Access Framework access and deletes only ownership-tracked app-created files.
- First-launch catalog initialization retries one transient network failure before showing an unavailable state.
- Foreground first-sync and WorkManager background sync are serialized so they cannot race over the candidate catalog cache.

### Documentation and validation

- Added and maintained `docs/CHATGPT_PROJECT_RUNBOOK.md`, `docs/ARCHITECTURE.md`, and `docs/PHASE1_DECISIONS.md` as the project source of truth and implementation decision record.
- Added `NOTICE`, `DATA_LICENSE.md`, `PRIVACY.md`, `CONTRIBUTING.md`, `SECURITY.md`, public issue templates, `docs/PUBLIC_RELEASE_CHECKLIST.md`, and `docs/RELEASE_SIGNING.md`.
- Completed Pixel 9 hands-on validation covering first launch, offline reuse, Browse/Search, My Headphones, selection persistence, SAF export/cleanup, UAPP/ToneBoosters preset import, appearance, large text, TalkBack, privacy, attribution, and launcher presentation.
- Generated and backed up the permanent Android signing identity; stored release signing material only in approved secure stores/GitHub Actions secrets.
- Built a signed `v0.1.0` candidate from commit `7bc0f687aece6f58f3431a71b5bb32794c0b7ffa`, verified its pinned signing certificate, installed it on the Pixel 9, and passed the release-build smoke test including UAPP/ToneBoosters import.
- Published GitHub Release `v0.1.0` from that exact tested commit with the signed APK, APK SHA-256 checksum, and `apksigner` verification output.
- Verified the public `releases/latest` metadata endpoint and confirmed the installed `v0.1.0` app reports **You're up to date** against the live public release metadata.
