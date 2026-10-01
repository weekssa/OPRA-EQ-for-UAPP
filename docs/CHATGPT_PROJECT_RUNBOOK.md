# OPRA EQ for UAPP / EQ Library: ChatGPT Project Runbook

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
- `docs/V0.7_PRODUCT_SUCCESS_CRITERIA.md` for the testable v0.7 finished-product, cross-DAC parity, low/high-shelf, and release acceptance contract
- `docs/V0.7_RELEASE_READINESS_AUDIT.md` for the independently verified current v0.7 release blockers, fail-closed implementation corrections, and exact-source gate boundary
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

## 2026-09-22 EW300 evidence and release continuation

Owner reports E043-E046 are tied to signed executable source `7599dd52fc9e8c58c96e021f581b86a669dcc148`: E043 read-only capability PASS, E044 Flash PASS, E045 exact-baseline Restore PASS (`restorationVerified=true`), and E046 Reset PASS for the exact fingerprint. Each mutation recorded 11 writes, exactly one Save, zero permission requests before the first write, matching replacement identity/generation, final readback, and known state. Total permission requests were one per operation. Replay and competing-job telemetry remain null/unmeasured, not zero. Do not repeat these operations or the accepted E001 Save qualification.

The nine commits from 7599 to `b11190f9bbc08326f963190ad8b5a9f4b0872b2c` changed documentation files only; no executable source changed. Physical results remain attached to 7599, not relabeled. On exact source b11190, the applicable Android CI, CodeQL, catalog, priority-coverage, dependency-submission, and signed-candidate workflows all passed. The exact artifact provenance is recorded in E048 and live PR #23. Any later commit, including this documentation acceptance update, creates a new candidate SHA and needs fresh exact-head gates and signed provenance before that SHA is treated as ready.

The Restore proves exact baseline restoration when it completed. A separate later Reset also completed; do not claim the post-Reset state is byte-for-byte the earlier arbitrary baseline. The read-only report proves a known snapshot only. E001 remains accepted and is not repeated.

No further Apply, Flash, Restore, Reset, Save qualification, or read-only report is needed. If still unverified, the only possible owner check is non-hardware-mutating Personal EQ capture/value/provenance confirmation; open/cancel My EQs Flash review only if the successful Flash was not already launched there. Stop before final write confirmation. Capture remains “not yet evidenced,” not unsupported hardware.

The approved cross-DAC product acceptance, including the shared graph/state/manual Refresh flow, capability-by-capability Black Pearl comparison, and low/high-shelf corpus behavior, is defined in `docs/V0.7_PRODUCT_SUCCESS_CRITERIA.md`. The current independent release audit is `docs/V0.7_RELEASE_READINESS_AUDIT.md`; earlier green gates and candidate provenance do not transfer to later source SHAs. At this 2026-09-22 checkpoint, PR #23 remained draft and v0.7.0 had not cleared. That release status was superseded by the owner-approved v0.7.0 publication on 2026-09-28, recorded below. PR #23 remains historical context; current release status is maintained in `docs/PUBLIC_RELEASE_CHECKLIST.md`.

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

## Local development build tooling

The repository includes a checked-in Gradle Wrapper for reproducible local and Codex builds. Use `./gradlew` on macOS/Linux or `gradlew.bat` on Windows; no globally installed Gradle is required. The wrapper is pinned to Gradle `9.4.1` for the repository's Android Gradle Plugin `9.2.0` setup. Keep the wrapper scripts and `gradle/wrapper/` files in version control, but never commit downloaded Gradle distributions or caches.

Android SDK discovery is machine-local: configure `ANDROID_HOME`/`ANDROID_SDK_ROOT` or the ignored `local.properties` file's `sdk.dir`. Do not commit a user-specific SDK path. This project currently compiles against API 36 and Build Tools 36.0.0.

The first wrapper invocation may download and cache Gradle locally. Common development checks are `./gradlew :app:testDebugUnitTest`, `./gradlew lintDebug`, and `./gradlew assembleDebug`. Use the smallest relevant check during iteration, then run the complete applicable gates on a coherent candidate head.

For Codex on macOS, prefer `./tools/codex-android <Gradle task or Android command>`. The helper selects the local JDK/SDK, exposes `adb`, `android`, `avdmanager`, and `emulator`, and routes Gradle/Android user caches to writable temporary locations. Examples are `./tools/codex-android :app:testDebugUnitTest`, `./tools/codex-android adb devices`, and `./tools/codex-android emulator -list-avds`. Instrumented/emulator gates remain `NOT RUN` unless an emulator or physical device actually executes them.

Codex skill routing uses the installed global `android-skills:android-dev` baseline plus the narrow Android skills relevant to the task: `android-skills:android-testing`, `android-skills:android-debugging`, `android-skills:compose`, `android-skills:android-ux`, `android-skills:kotlin-coroutines`, `android-skills:kotlin-flows`, `android-skills:android-gradle-logic`, `android-skills:android-source-search`, and `android-skills:modularization`. The Android CLI-managed project skills currently installed under `.agents/skills/` are `android-cli`, `testing-setup`, `android-profiler`, `r8-analyzer`, `android-permissions-security`, `android-intent-security`, `adaptive`, and `edge-to-edge`; repository-local DAC skills remain authoritative for protocol, transaction, physical-validation, and release-readiness work.

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

New catalog Favorites and General EQs retain the full canonical profile, exact selected revision,
and source references. Legacy `OpraEqProfile` values are derived compatibility views, not saved
source authority. Saving requires exact resolution against the current canonical catalog; General
EQ batch saves are all-or-nothing. Room migrations add nullable columns only: older projection-only
rows remain unchanged and must never receive inferred canonical provenance.

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

- **Exact**: source is natively representable at the target's actual limits/quantization without target-side acoustic alteration or generated headroom.
- **Optimized**: EQ Library deterministically derives a faithful target representation and it passes that target's quality/safety gates. Native target rounding, complete-response fitting, and EQ Library-generated target headroom are all Optimized rather than Exact.
- **Not suitable / Not exportable**: a safe/faithful representation cannot be produced.

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

The upstream OPRA schema defines `parameters.bands` as priority-sorted and directs limited-band
software to truncate: [pinned `eq_info.json` at OPRA commit
`0b88ecd4e2bef7cf69fd5d50f1d06fb586c10865`](https://github.com/opra-project/OPRA/blob/0b88ecd4e2bef7cf69fd5d50f1d06fb586c10865/schemas/eq_info.json).
Apply the first-ten rule only when the trusted OPRA adapter supplies verified source-priority
provenance. Never apply it to arbitrary flattened/legacy, imported, Personal, or mixed-source bands;
reject those over-budget cases unless an independently established ordering contract exists.

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

### v0.7 SIMGOT EW300 DSP cable: current capability and evidence boundary

The exact EW300 USB identity, five-band raw transport, direct-Hz Peak decoding, and bounded transaction behavior are maintained in the EW300 protocol/status documents. The recovery implementation uses the shared output registry, finite-hardware adapter, authoritative DAC session, and My DAC shell; it does not copy Black Pearl commands or controls. The exact profile currently exposes native five-band Peak readback, local edit/review, guarded Apply/Flash, Peak-only Personal EQ capture, qualified Reset-to-flat, reconnect/final-readback feedback, and the evidenced read-only Device state/report surface. Source low/high shelves may be represented only by a complete-response Optimized fit to the five-Peak target when the shared quality gates pass; native EW300 shelf readback/capture/edit/write is not established. Other controls remain unclaimed unless exact-profile evidence establishes them.

On signed candidate source `7599dd52fc9e8c58c96e021f581b86a669dcc148`, capability report JSON (17) passed; operation reports JSON (11)–(13) verified Flash, exact-baseline Restore, and Reset. These reports are E043-E046. E001 Save qualification remains accepted and is not repeated. Their replay/competing-job fields are null/unmeasured. The pre-criteria head b11190's six exact-head gates passed (E048); the current criteria documentation commit creates a newer source that needs its own same-head gates/signing, not a physical retest.

No unknown revision is authorized. VID/PID alone is insufficient. Firmware, bootloader, erase, calibration, recovery, and cross-flash remain outside scope. Android permission for a re-enumerated USB instance remains OS-controlled. PR #23 remains draft; release/public-support approval is owner-controlled.

## 11. TRN Black Pearl

Black Pearl protocol/USB behavior was physically qualified in the v0.3 foundation and Reset EQ to flat was qualified in v0.4. Preserve those protocol boundaries and fail-safe gain-reset rules.

v0.5 changes **derived DSP adaptation**, not the qualified Black Pearl USB identity. File export and
Direct Flash share canonical ownership and ten-band filter adaptation, but their global-gain
representations are intentionally distinct: Direct Flash/editor mutation uses the established
native whole-dB application step, while file export retains its signed 1/256-dB serialization:

- Direct Flash and Black Pearl file export consume the same shared filter representation;
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


## 2026-09-15 corrective candidate: PASS

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

## Historical 2026-09-22 EW300 v0.7 checkpoint

At that checkpoint, PR #23 was the authoritative location for the exact branch SHA, signed APK filename, SHA-256, package/version, signer, artifact digest, artifact ID, and gate links. The instructions below describe that historical release state; they are superseded by the later v0.7.0 publication and current checklist.

Physical transaction qualification is complete on sources 381 and 7599 (E037-E046); retain those tested SHAs in the ledger and do not repeat any mutation or read-only report. After all product work and exact-head gates pass, the only possible owner checks are non-hardware-mutating Personal EQ capture/value/provenance confirmation and opening/canceling My EQs Flash review only if the verified Flash did not already originate from My EQs. No Apply, Flash, Reset, Restore, or Save is requested.

Historical gate instruction from 2026-09-22: keep PR #23 draft and v0.7.0 NO-GO until scope and final review are closed and the owner explicitly approves merge/publication/public support.

### 2026-09-23 source-review continuation

The follow-up review found that unsupported or failed UAPP conversion could revive old generated
XML, unselected profiles could retain stale UAPP output, exact app-owned stale UAPP documents could
disappear from **Needs attention**, and caller-supplied provenance could authorize ten-band
truncation. It also found a same-key canonical source-reference collision and missing product-only
provenance coverage. The follow-up centralizes managed export-artifact state, keeps stale exact UAPP
files visible but non-recoverable until explicit deletion, makes source-order authority private to
the verified conversion path, preserves the selected source reference during deduplication, and
adds focused regressions. This does not change EW300 protocol behavior or accepted physical
evidence. Check all remote gates and artifacts on the live exact PR head. The trusted-main-only
signed candidate cannot be produced from this feature branch without explicit owner-authorized
integration; do not merge, sign or publish under the guise of a test run.

## 2026-09-30 AFUL Explorer Favorite candidate

The AFUL Explorer Reddit/LoboNautics and HiFiGuides/Jaytiss community Favorites failed because
canonical selection resolution retained synthetic compatibility product IDs after the effective
catalog overlay displayed the same canonical rows through the legacy OPRA `aful::explorer` product
alias. The bounded correction rebases only compatibility vendor/product IDs after validating the
effective alias and displayed records; exact canonical profile, revision, fingerprint, and source
references remain authoritative. Strict `matchesSelection` validation remains in place.

Implementation commit `fcb0f5e03b22fc51c778d1989ec1c0ad8c7ddb38` is based on current
`origin/main` `6ada7efc84425f7b149f9f9e9fb4e9599bc80cf2`. The candidate passed focused and full JVM
unit tests, Android-test Kotlin compilation, the Room-backed API 36 instrumentation class, and a
fresh debug-app API 36 Favorites/restart smoke. The complete source, fixture, test, and evidence
record is `docs/AFUL_EXPLORER_FAVORITE_REVIEW.md`. The branch is local/unpushed with no PR, remote
CI, signed candidate, release qualification, or hardware test. Status is **CANDIDATE FIX VERIFIED:
ready for independent review**, not resolved or release-ready. No hardware validation ledger or
capability matrix changed because this candidate does not exercise hardware or establish a device
capability. The next stage is the Sol 5.6 independent review, followed by a new Codex Luna Extra
High worker task on every outcome. Sol records PASS/FAIL/INCONCLUSIVE plus a durable result and
copyable Luna prompt under `docs/AFUL_EXPLORER_REVIEW_TO_LUNA_WORKFLOW.md`. FAIL returns concrete
fix and test criteria; INCONCLUSIVE returns the exact access gap. Sol may update review/runbook
documentation only and must leave implementation work to Luna.

### 2026-09-30 AFUL task-intake routing correction

The Luna intake found that `docs/AFUL_EXPLORER_NEXT_TASK.md` was absent while the review outcome
and maintained workflow still marked the candidate **PENDING SOL 5.6 REVIEW**. No implementation
work or tests were run. The review outcome records this intake as `REVIEW_REQUIRED` without
changing the technical review status, and the new next-task baton routes to a fresh Sol independent
review of candidate `fcb0f5e03b22fc51c778d1989ec1c0ad8c7ddb38` on branch
`codex/aful-favorite-identity` at expected HEAD `ecdcb6cab8ff7e5688d7092cf2b356566e88cc84`.
See `docs/AFUL_EXPLORER_REVIEW_OUTCOME.md` and `docs/AFUL_EXPLORER_NEXT_TASK.md`. The next task
class is **GPT-5.6 Sol independent review**; Luna work follows only after a recorded Sol outcome.


### 2026-09-30 AFUL Sol review access recovery

The preceding GPT-5.6 Sol review attempt was **INCONCLUSIVE** because that Sol runtime could not
access the required local macOS worktree or candidate implementation commit through its connected
GitHub context. Sol ran no tests and made no implementation finding; the result is an access gap,
not a code assessment.

Luna verified the repository-local baton at
/Users/stephenweeks/.codex/worktrees/aful-favorite-fix/OPRA-EQ-for-UAPP on branch
codex/aful-favorite-identity, HEAD ecdcb6cab8ff7e5688d7092cf2b356566e88cc84. The local
implementation commit fcb0f5e03b22fc51c778d1989ec1c0ad8c7ddb38 is based directly on
6ada7efc84425f7b149f9f9e9fb4e9599bc80cf2, which a live remote-main lookup confirmed on
2026-09-30. The exact production and test diff is accessible in this worktree; this repository-state
check made no independent implementation judgment and ran no tests.

The next role is a new GPT-5.6 Sol independent review using the complete prompt in
docs/AFUL_EXPLORER_NEXT_TASK.md. The reviewer must preserve all existing uncommitted documentation,
inspect the exact source/test diff, classify the candidate PASS, FAIL, or INCONCLUSIVE, and limit
writes to the documented review/handoff files. See
docs/AFUL_EXPLORER_REVIEW_OUTCOME.md and docs/AFUL_EXPLORER_LUNA_WORKER_HANDOFF.md.

### 2026-09-30 AFUL Explorer Favorite Luna final verification

The owner supplied a direct Luna final-verification and closeout task for this candidate. That
explicit request superseded the earlier Sol-first routing baton recorded above. The prior Sol
attempt remains **INCONCLUSIVE** because it could not access this local worktree; it made no
implementation finding and ran no tests. A separate read-only reviewer then returned **PASS** on
the exact source and test diff, with no actionable implementation finding.

On branch `codex/aful-favorite-identity`, starting HEAD
`ecdcb6cab8ff7e5688d7092cf2b356566e88cc84`, Luna reviewed implementation commit
`fcb0f5e03b22fc51c778d1989ec1c0ad8c7ddb38` against base
`6ada7efc84425f7b149f9f9e9fb4e9599bc80cf2`. The alias path changes only compatibility IDs after
the effective alias resolves to the displayed product and its unique product/existing vendor are
present. Canonical profile, exact selected revision, fingerprint, and source references remain
unchanged; strict projection matching and persistence revalidation remain active. The existing
exact legacy OPRA fallback and wrong, stale, ambiguous, and unrelated selection cases were
inspected in source and tests.

The following checks passed:

- Focused command `./tools/codex-android :app:testDebugUnitTest --tests 'com.weekssa.opraeqforuapp.data.library.CanonicalFavoriteAliasIntegrationTest' --tests 'com.weekssa.opraeqforuapp.data.library.CanonicalFirstCatalogRepositoryTest' --tests 'com.weekssa.opraeqforuapp.domain.library.CanonicalLegacyCatalogAdapterTest' --tests 'com.weekssa.opraeqforuapp.domain.library.CatalogOverlayTest'`: 22 tests.
- Full command `./tools/codex-android :app:testDebugUnitTest`: 711 tests, 0 failures, 0 errors, 0 skipped.
- `./tools/codex-android :app:compileDebugAndroidTestKotlin`.
- API 36 install `./tools/codex-android :app:installDebug :app:installDebugAndroidTest` and
  `./tools/codex-android adb shell am instrument -w -e class com.weekssa.opraeqforuapp.data.library.SavedEqCanonicalSelectionPersistenceTest com.weekssa.opraeqforuapp.test/androidx.test.runner.AndroidJUnitRunner`: `OK (6 tests)`.
- Debug UI Favorite/restart smoke on AVD `codex-api36`, serial `emulator-5554`, API 36. Both
  community Favorites restored as the same AFUL Explorer with their Reddit and HiFiGuides source
  identities; the unrelated AutoEQ Favorite remained. Luna removed the test Favorites afterward
  and restored the initially empty emulator library. The detailed record is
  `docs/AFUL_EXPLORER_FAVORITE_UI_SMOKE.md`.

`adb devices -l` showed no phone before emulator start. No physical or wireless test was needed.
This candidate is **VERIFIED FOR REPOSITORY PROMOTION**, not app release readiness, signed-candidate
provenance, or hardware qualification. No production or test code changed during this closeout.

Before publication, the live remote check found no candidate branch ref, no PR for
`codex/aful-favorite-identity`, and no Actions runs. `gh` was unavailable; GitHub REST queries were
read-only. The owner task authorizes committing the closeout documentation, pushing this branch,
and opening its normal integration PR. Record the resulting commit, push, PR, and live check state
in the follow-up entry once those actions complete.

### 2026-09-30 AFUL Favorite repository promotion

The verified branch was pushed to `origin/codex/aful-favorite-identity`. Closeout documentation
commit `e9f79089802b30c7678215ff5d157354d9995d3c` was the branch head when PR #51 was opened against
[main](https://github.com/weekssa/OPRA-EQ-for-UAPP/pull/51). The PR was open, non-draft, and
reported mergeable at the live refresh. At that point, Catalog currentness CI (run 2145), CodeQL
(run 1771), Android CI (run 1886), and Priority community coverage CI (run 1630) were all
`in_progress`; the local test results remain separate from those remote checks.

This promotion update is documentation-only. Its pushed commit advances the PR head; the workflow
state above is a snapshot for the initial PR head. The final task closeout records the latest branch
head and check state. The next owner action is to review PR #51 and merge it through the normal
protected-branch process when approved. This does not authorize merge, release signing, or
publication by the verification worker. This owner action was subsequently completed: PR #51 is
merged as `2a39bc53d7ee3caf2b98fe2fb043aff7bf4bdc74`. Later PR #52 merged as
`70a240a458f1e3cb6607d8349bb422d78cc95699`.

### 2026-10-01 AFUL Favorite source-kind coverage and patch preparation

The owner asked for evidence that the Favorite alias correction applies across catalog sources, not
only the AFUL Explorer community examples. PR #51 is merged. A test-only matrix was added on branch
`codex/aful-source-matrix`; its final tested head is `0cdfe66caf045696f9055197fc2ef617b0b72146`,
and it merged as `70a240a458f1e3cb6607d8349bb422d78cc95699`. It exercises all seven source kinds used by canonical headphone
catalog Favorites across AFUL Explorer, Sony WH-1000XM5, and Sennheiser HD 600, while checking exact
profile, revision, and source-reference retention. `DEVICE_CAPTURE` and `PERSONAL_IMPORT` use
separate local saved-EQ flows and are not inputs to this catalog Favorite resolver.

The matrix passed as part of the 23-test focused alias/catalog JVM run and the 712-test full JVM
run, both with no failures, errors, or skips. Android-test Kotlin compilation and API 36 Room
instrumentation also passed (`OK (6 tests)`). The existing API 36 Favorite/restart UI smoke remains
applicable because production code did not change. Exact commands and coverage boundaries are in
`docs/AFUL_EXPLORER_FAVORITE_REVIEW.md`. The updated matrix derives its expected set from all
`EqSourceKind` values and explicitly excludes only the two separate local flows. This evidence does
not quantify a statistical 95% probability or independently validate every external feed parser.

On final PR head `0cdfe66`, Android CI run 1891, CodeQL run 1776, Catalog currentness CI run 2149,
and Priority community coverage CI run 1634 all passed. Android CI included unit tests, lint,
debug/release builds, the API 26 cold-install smoke, and API 36 connected UI tests. The exact merge
commit's main checks also passed: Android CI run 36820154922, Kotlin analysis run 36820154963,
Gradle dependency submission run 36820154755, and dependency submission run 36820154934.

The Favorite fix is the only app-production behavior change after public v0.7.0. Release-preparation
PR #53 merged normally to main at `5c05b0c3ac06e4d8eb868b6232a81651ac060da5`; all exact-head checks
passed. The owner authorized a main-only v0.7.1 signed candidate and API 36 install/restart
verification. The signed candidate passed those gates. Its exact provenance, observed UI behavior,
and remaining publication boundary are recorded in `docs/PUBLIC_RELEASE_CHECKLIST.md`.

The latest public release remains v0.7.0. No public v0.7.1 tag or release has been created. The
repository still has no exact-artifact public promotion workflow. Public publication therefore
remains an explicit owner decision and cannot be performed by rebuilding or re-signing the
qualified candidate.

### 2026-10-01 v0.7.1 signed candidate and API 36 Favorite verification

Signed Release Candidate workflow run #11 / run ID `36831766816` succeeded from main at exact
source SHA `5c05b0c3ac06e4d8eb868b6232a81651ac060da5`, tag input `v0.7.1`. It uploaded immutable
artifact `11148046368`, named
`EQ-Library-v0.7.1-signed-5c05b0c3ac06e4d8eb868b6232a81651ac060da5`, ZIP size 2,218,563 bytes,
ZIP SHA-256 `5735a433e52ec34e075aa4a6a5044aae77d9efb8a8a6a7386a614b26d5c87acb`, expiring
2026-12-30. The manifest records APK `EQ-Library-v0.7.1.apk`, package
`com.weekssa.opraeqforuapp`, version `0.7.1` / code `8`, APK SHA-256
`cfdc688a0ff9392f5617b05719b09c9c478cf07390aa7dc79e8d4e33c6eecba3`, signer certificate
SHA-256 `65c1c1256dae3c49e3548f334c91f0ba991969e9be9e0b223ba4e253d2114747`, and R8 mapping
SHA-256 `715a5a42e51e021a21c7957bb78ed235edcd851bd15cf7c884af8e2745f67a0d`.

The downloaded archive digest matched the Actions artifact digest. The APK checksum was recomputed
from the extracted file and matched both the manifest and checksum sidecar. Independent Android
Build Tools verification passed for the pinned signer, APK v2/v3 signatures, zip alignment, package
ID, version, and version code. The public v0.7.0 APK used for the upgrade control matched its
published SHA-256 and had the same release signer.

On a fresh API 36 AVD, the exact public v0.7.0 APK reproduced the Jaytiss/Hifiguides Favorite
failure, reporting that the exact source revision was unavailable. The exact signed v0.7.1 APK
installed in place over it. On v0.7.1, the same Jaytiss Favorite changed to `Remove favorite` and
reported a successful save. A separate Fahryst/AutoEQ Favorite also saved. My EQs showed both
source identities after force-stop and cold relaunch. The starting app library was empty, so this
run verifies the in-place version upgrade and persistence of newly created Favorites, but it does
not test migration of a pre-existing nonempty Favorite set. The temporary `codex-v071-smoke` AVD
was removed after the run; the pre-existing `codex-api36` AVD was not changed. This is emulator
evidence, not physical-device evidence.

No DAC hardware or physical test was required for the signed v0.7.1 candidate's canonical catalog
Favorite verification. At that point, the synthetic source-kind matrix was the available automated
coverage; it spans all seven catalog kinds, with six synthetic alias cases, but it did not provide
one actual current profile for each source ID. The follow-up below supersedes that matrix as the
per-source coverage basis. `DEVICE_CAPTURE` and `PERSONAL_IMPORT` remain separate local flows.

### 2026-10-01 real source-ID Favorite coverage and OPRA alias correction

The owner requested one actual canonical sample per ingested source and a permanent requirement for
new sources. On isolated branch `codex/favorite-source-review`, based on refreshed `origin/main`
`84998be61769950cf8b13801a7846523631603f9` (the review began on `bfb1e3b49e37dd764675e512cee10670d22570a1`),
the review found 14 distinct source IDs in canonical profile revision references, represented by 13
unique profiles. `opra` and `oratory1990` are separate source references on one Edition XS
profile/revision. Twelve samples use the headphone Favorite resolver; `milciossq-eq-general` and
`paraeq` use the separate General resolver. The exact profile/revision/source-record table is in
`docs/AFUL_EXPLORER_FAVORITE_REVIEW.md`.

The registry contains 15 IDs. `squiglink` is intentionally excluded because it supplies measurement
ecosystem/provenance monitoring rather than source-authored PEQ. `topping-community` is paused and
has no canonical catalog profile. The unregistered catalog spelling `headphone-community` remains
covered as its own source-ID sample with an explicit policy alias to registered
`headphones-community`; it is not silently dropped or merged.

The first real OPRA alias test failed strict selection matching: after compatibility IDs changed,
the projection lost OPRA band-order provenance because it compared the displayed IDs to the
canonical source IDs. `CanonicalLegacyCatalogAdapter.projectSelection()` now validates that marker
against the profile-scoped canonical OPRA identity while preserving the displayed compatibility
IDs. The identity deliberately spans the profile's revisions; all complete OPRA vendor/product
pairs must agree before any revision receives the trust marker. Conflicting identities suppress the
marker for every revision, including a revision matching the first pair encountered. A regression
covers both revisions and the alias-rebase path. Using the selected revision's IDs alone would allow
a revision to validate its own conflicting identity.

The checked-in fixture `app/src/test/resources/catalog/favorite-source-samples.json`, checker
`tools/verify_favorite_source_samples.py`, and its five negative/positive Python tests are integrated
into catalog currentness CI. The JVM test forces a different displayed product ID for every
headphone candidate, checks exact canonical profile/revision/fingerprint/filter/source-reference
retention, and tests stale projections fail closed. General samples use the distinct exact General
resolver. A new catalog source ID absent from the registry or fixture fails CI; a registry-only ID
requires an explicit reviewed no-profile reason.

Local verification passed:

- `python3 tools/verify_favorite_source_samples.py --check`: 14 source samples, 13 unique profile
  records, and 2 reviewed no-profile exclusions.
- `python3 -m unittest tools.test_favorite_source_samples -v`: 5 tests passed, including missing
  sample, stale profile, unknown source ID, and unreviewed registry-only source negative controls.
- `./tools/codex-android :app:testDebugUnitTest --tests 'com.weekssa.opraeqforuapp.data.library.CanonicalFavoriteAliasIntegrationTest' --tests 'com.weekssa.opraeqforuapp.domain.library.CanonicalLegacyCatalogAdapterTest'`:
  passed after the OPRA correction, including profile-wide mismatch rejection during alias rebasing.
- `./tools/codex-android :app:testDebugUnitTest`: all 713 JVM tests passed.
- The full repository Python CI discovery passed all 199 tests under the bundled workspace Python
  runtime. The machine's system `python3` is 3.9 and cannot import two existing tests using Python
  3.10 union type syntax; CI and this successful run use a supported interpreter.
- `actionlint` was not available locally; the changed workflow still requires exact-head remote
  catalog CI before merge.

At this pre-PR checkpoint, the earlier signed v0.7.1 candidate predated this follow-up OPRA
band-order correction. That candidate was superseded by the fresh merged-source signed beta recorded
in the final closeout below. No phone, USB DAC, or wireless debugging was needed for this repository
resolver test. These deterministic samples provide current source-ID boundary coverage, not a
statistical 95% probability or parser, provenance, license, access, or acoustic quality
certification.

The source fix was committed locally as `1c04e6e8bce6af50d79f11b5599e2d55dcd843d9` on
`84998be61769950cf8b13801a7846523631603f9`. Two automated catalog-only commits then advanced
`origin/main`, first to `32e7937a1e3c9b646d69bd5caebcccd98b2b0d77` and then to
`9c67f8ee780e65ab3f966dbfc5f2aa5efab462ed`. The parsed catalog data in the latest refresh is
identical to its parent after removing `generated_at`; both contain 8,931 profiles and 8,933
revisions. The 14 source IDs, selected profile/revision samples, and two reviewed exclusions remain
unchanged. The source-sample checker also passed directly against the latest catalog.

The attached verification instructions prohibit rebasing, so the feature branch merged live main
without rewriting the source commit. The latest-main merge point is
`cbb6a3911dea1ff2ec14acca80e5c8d568f0c6aa`, with main `9c67f8ee780e65ab3f966dbfc5f2aa5efab462ed`
as its second parent. The PR diff against latest main contains only the 16 intended source, test,
workflow, and documentation files; generated catalog formatting is not in the PR diff.

Local checks passed at the latest-main merge point: 14 source samples / 13 profiles; 5 checker
unit tests; all 199 repository Python tests using the bundled Python 3.12 runtime; both focused
Kotlin test classes; the full JVM suite (713 tests, zero failures/errors/skips); and the Room-backed
Favorite persistence class (6 tests, zero failures/errors/skips) on a temporary API 36 emulator.
The OPRA Edition XS Favorite UI smoke was run on merge point `68ca6117230f4dab709be23b0a56cd66ee732dc7`
before the second metadata-only catalog formatting refresh; application code was unchanged at the
latest merge point. The test Favorite was removed, the temporary emulator was deleted, and the
existing `codex-api36` emulator was left unchanged. No physical phone, wireless debugging, DAC, or
hardware operation was used. `actionlint` is not installed locally. At this pre-PR snapshot, exact-
head GitHub automation and independent review remain pending. Existing draft
[PR #55](https://github.com/weekssa/OPRA-EQ-for-UAPP/pull/55) is unrelated and remains unchanged.
The latest public release is still v0.7.0. No public tag or release was created.

### 2026-10-01 merged source-wide Favorite fix and signed beta closeout

The owner approved autonomous completion of the source-wide review, source-onboarding
documentation, exact-head automation, merge when those gates passed, and a fresh signed candidate.
PR #56 head `87842777b4d40f5f95e8d6e30e9f775a736e0b34` passed independent read-only review and all
8/8 applicable PR checks. It merged at `66d32756d682405c8617380a4c64f5e25a5699f6`. Applicable
merge-commit checks passed; the post-merge ingestion job also succeeded.

The sample fixture has 14 source IDs across 13 profiles, with 12 headphone samples and 2 General EQ
samples, plus 2 reviewed no-profile exclusions. PR #56 requires each newly ingested source ID to
add an authentic sample or a reviewed exclusion and exercises the corresponding production
resolver in catalog CI. After merge, automated ingestion added catalog data at
`68d5e2e745dacbf26ecf15947d199cdd8667951a` without changing app code. Its source-coverage check
still passes: 14 samples, 13 profiles, 2 exclusions; all five checker tests pass against that
catalog. The new revision uses an already-covered source ID.

Signed EQ Library Beta Candidate run #1375 / ID `36905949756` completed successfully from the exact
PR merge SHA `66d32756d682405c8617380a4c64f5e25a5699f6`. It built version `0.7.1` / code `8` for
package `com.weekssa.opraeqforuapp`, verified R8, pinned signing certificate, APK signature, and
alignment, then installed and cold-launched on the hosted API 35 emulator. Artifact ID
`11184356919` has ZIP SHA-256
`b5fad8c72744ae5fd03e41225db58986070396634a3ceaec3113b5aa7d9fec41` and expires 2026-10-15. The
APK SHA-256 is `9c4cf12d8f95525e8ba4b42a640512fdac91ba551583714c49d642f6eaa2639f`; signer
certificate SHA-256 is `65c1c1256dae3c49e3548f334c91f0ba991969e9be9e0b223ba4e253d2114747`. The
superseded immutable APK is
[`EQ-Library-v0.7.1-beta-66d3275.apk`](https://raw.githubusercontent.com/weekssa/OPRA-EQ-for-UAPP/mobile-test-apk/candidates/EQ-Library-v0.7.1-beta-66d3275.apk).
The immutable testing-channel copy is present at `mobile-test-apk` branch commit
`b24610d8861b4f57b176b990894c12a465f5bd0a`; its downloaded APK hash matches the workflow
manifest and artifact copy. Post-run review found that the manifest repeated stale
`merge/publication approval` wording, so this artifact is superseded and must not be used as the
current handoff candidate.

The run #1375 target field was `ja11` because the workflow default was used. It was metadata only
and did not prove JA11 physical behavior. PR #57 corrected the workflow manifest predicate;
reviewer `favorite_pr56_review` returned PASS on exact head
`3878ee23d36da69847edb1695a92e18a734f7c57`, and the local jq fixture covered 12 manifest
assertions. PR #57 merged at `38302d6b880fcb1b384a4c290d8539260bd4a138` after all six merge-commit
checks passed.

Replacement signed beta run #1376 / ID `36913222922` completed successfully from exact main SHA
`38302d6b880fcb1b384a4c290d8539260bd4a138`, with target `ja11`. Artifact ID `11188327665`, ZIP
SHA-256 `26894f2c0a8a490450418693c8b2c34f6217c6e8f64e9a5e1411ac3992cb76d0`, expires 2026-10-15
19:27:31 UTC. APK SHA-256 is
`dfdac7782d0545a652cd5eec6e8d6ede60e748da746c0fbec4514c8b3ddaa7d2`; signer SHA-256 is
`65c1c1256dae3c49e3548f334c91f0ba991969e9be9e0b223ba4e253d2114747`; R8 mapping SHA-256 is
`048f3f5267c387c0a6b4f483c356a52e7334c636eaa0e7b46692762aab3affb8`. Manifest fields verify
package `com.weekssa.opraeqforuapp`, version `0.7.1` / code `8`, R8 enabled, and test plan
`docs/FIIO_JA11_HANDS_ON_CHECKLIST.md`. The outstanding condition is JA11 Flash/readback/power-cycle
persistence validation plus final public release/support approval. Artifact checksum, APK checksum,
pinned single signer, v2/v3 signatures, package/version, and ZIP alignment passed local independent
checks. The hosted API 35 emulator installed and cold-launched the APK. The immutable mobile-test
copy matches its APK hash; its branch tip is `dc3fdae75998f4a74f90581d0f53b3f860841ee7`:
[EQ-Library-v0.7.1-beta-38302d6.apk](https://raw.githubusercontent.com/weekssa/OPRA-EQ-for-UAPP/mobile-test-apk/candidates/EQ-Library-v0.7.1-beta-38302d6.apk).

This Favorite resolver defect required no phone, wireless debugging, USB DAC, or physical
qualification. At this recorded checkpoint, the latest public release was v0.7.0 and no public
v0.7.1 tag or release had been created. Conditional merge approval was exercised for PR #56, #57,
and #58 after their applicable exact-head gates passed. On 2026-10-01, the owner separately
authorized the public v0.7.1 release on the condition that all review, CI, exact-candidate,
promotion, and post-publication gates pass. That approval does not authorize any broader
hardware-support claim. The exact-artifact publisher and final release evidence belong in the
follow-up closeout entry below this history.
