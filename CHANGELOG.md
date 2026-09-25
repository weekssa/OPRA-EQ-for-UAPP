# Changelog

All notable changes to **OPRA EQ for UAPP / EQ Library** will be documented in this file.

The project uses Semantic Versioning. Development releases remain in the `0.x` series until the first stable `v1.0.0` release.

## [Unreleased]

### 2026-09-25 JA11 Flash verification evidence

- Recorded an owner-reported FiiO JA11 Flash failure in which the app reached verification but
  rejected a global-EQ-gain readback mismatch after a displayed 9-to-5-band optimized conversion.
  The supplied frame sequence additionally shows connected hardware at `-3.80 dB` against the
  Jaytiss plan's `-3.90 dB`, followed by a connected flat `0.00 dB` view after reconnect. The
  source SHA, signed APK, firmware, raw packets, and restoration state are not established; JA11
  remains hardware-validation pending and the existing fail-closed behavior is preserved.
- JA11 Save now has an explicit Android reconnect boundary based on FiiO's documented Save
  power-cycle behavior; final readback is not attempted until the optional replacement-session
  boundary is satisfied. JA11 reads and ordinary writes now reject a detach/session-generation
  change spanning the exchange. Added Save-boundary/final-mismatch coverage, the Jaytiss `-3.9 dB`
  codec vector, and the 9-to-5 optimizer fixture. This working-tree correction has no signed APK
  or release provenance yet.

### 2026-09-24 Rtings/AutoEQ Favorite provenance follow-up

- Favorites now source-bound-canonicalize exact current OPRA compatibility rows when the
  multi-source snapshot has not published that same source record yet. The resolver preserves
  the exact OPRA source ID and rejects changed, stale, or ambiguous projections; it does not
  accept a similar profile or save unresolved legacy data.
- Added adapter and repository regression coverage for the Edition XS Rtings/AutoEQ failure
  path, including altered-projection refusal.

### 2026-09-24 EW300 delayed reconnect verification candidate

- Added a fail-closed, read-only reconciliation path for EW300 Flash/Reset operations whose Save
  window ended before Android completed replacement-session permission/reconnect. A later exact
  fingerprint, newer session plus detach generation, and complete byte-for-byte target readback
  can now promote the existing uncertain trace to `Verified` without replaying any write or Save.
- Genuine mismatches, same-state operations, stale sessions, malformed/incomplete reads, and newer
  operations remain uncertain and continue to block unsafe follow-up writes.
- Reconciled terminal traces now replace stale EW300 warning presentation and emit the verified
  completion feedback once the fresh readback proves the target.

### 2026-09-24 canonical Favorite star recovery

- Managed My EQs Favorite actions now resolve the current canonical projection before persisting,
  preventing a stale managed snapshot from causing a valid profile to be rejected.
- Added the exact OPRA-backed `Rtings/AutoEQ` Edition XS revision visible in the legacy catalog so
  its source record and immutable revision can be verified before saving a new Favorite.
- Added focused resolver, canonical Room persistence, and published-catalog provenance coverage;
  missing or ambiguous canonical provenance remains fail-closed.

### 2026-09-23 release-readiness corrective candidate

- Refreshed the v0.7 candidate against live `main` and recorded the PR #24 review: its stale
  diverged adapter variant is not integrated because it would reject valid source-neutral records;
  the current lineage already contains the useful fail-closed parser behavior.
- Added canonical-catalog promotion validation for finite/positive filter semantics, finite
  preamp/headroom, required provenance, and unsupported-filter source identity.
- Prevented malformed saved General EQ rows from entering EW300 hardware matching and quarantined
  malformed profile/category data as visible but non-actionable.
- Exact source `fdc60bb2443aed00c6e58ea8806e4735272e00d9` passed Android CI #1776, CodeQL #1661,
  catalog currentness #2074, priority coverage #1559, and dependency submission #2097. No signed
  APK is available until trusted-main signing; accepted EW300 physical evidence is unchanged.
- Corrected EW300 operation lifecycle presentation so a verified Reset/Flash terminal trace replaces
  an older reconnect warning, and Reset execution remains owned by the ViewModel across USB
  re-enumeration. Detection snackbars now dismiss when My DAC opens or a supported DAC connects,
  expire explicitly, and remain suppressed during an active EW300 reconnect. Android USB permission
  denial/timeout is presented as an explicit recovery state; the platform prompt is still required
  when Android has revoked permission for the replacement USB instance.

### 2026-09-23 canonical Favorite and General EQ persistence

- Added full canonical profile/revision/source-reference persistence for newly saved catalog
  Favorites and General EQs; legacy `OpraEqProfile` records remain derived compatibility views.
- Added nullable Room 8→9 columns only. Existing Favorites and General EQ rows are preserved
  unchanged with no guessed canonical provenance or backfill.
- Resolve saves only against the exact current canonical projection, make a General EQ batch
  atomic, and keep malformed new canonical rows visible but ineligible for Flash/export actions.
- Added focused canonical round-trip, mixed-source identity, atomic-save, fail-closed corruption,
  and non-destructive migration coverage. Implementation snapshot
  `b57b2466160bc9464d1cb16a004d1522940c553a` passed Android CI #1772 (including connected
  instrumentation, R8, and API-26 minified install/cold-launch), CodeQL #1657, catalog #2070, and
  priority coverage #1555. The documentation synchronization following that snapshot creates a
  new source SHA; consult the live PR for that SHA's own gate results and never carry these results
  forward to another SHA. No signed APK is available.

### 2026-09-23 source-review follow-up

- Prevented catalog refresh and selection-save from reviving obsolete UAPP/ToneBoosters XML after
  the current source is no longer representable or a fresh conversion returns no artifact.
- Keep the exact app-owned old UAPP document visible under My EQs **Needs attention** as
  source-unverified/non-recoverable, including when its current managed snapshot cannot be decoded
  or disagrees with the stored product/profile identity. Recovery rechecks this at the repository
  boundary and in the save transaction; removal remains explicit and exact-file scoped, unrelated
  export targets are unaffected, and the row no longer overstates every case as an unsupported
  filter.
- Removed caller-provided source-order authority from the public ToneBoosters XML builder and
  preserved the selected canonical source's metadata when duplicate provenance keys collide.
- Added focused regressions for failed conversion, unselected stale exports, exact UAPP ownership
  identity/path, primary-reference collision, and product-only OPRA identity mismatch. Exact-head
  CI is pending on the live PR; no hardware operation was repeated.
- Added an Android Room instrumentation regression for a current UAPP source becoming
  unrepresentable after recovery preflight but before the save transaction; it asserts that no
  Personal EQ is saved and the exact ownership row is unchanged. Android CI #1767 caught a
  compile-time callback-argument issue in the test fixture before any connected test ran; the
  explicit named-argument correction is covered by the passing connected tests in #1772.
- Made owned-file recovery idempotent at its repository transaction boundary and disabled the
  dialog while a recovery is in flight. Repeated taps/requests return the already-associated
  Personal EQ instead of inserting duplicates; instrumented coverage checks the saved row and
  ownership link remain singular. An explicitly selected orphan whose Personal EQ was deleted may
  be recovered as a new item; malformed or mismatched associations fail closed.
- Clarified that historical “unmeasured zero” replay/competing-job telemetry wording does not mean
  zero was measured; the values remain null/unmeasured.
- Corrected the stale UAPP >10-band test expectation: optimization requires verified OPRA
  source-priority provenance; ambiguous band order remains Not representable and canonical bands
  remain untouched.
- Pinned the upstream OPRA band-priority rule to schema commit
  `0b88ecd4e2bef7cf69fd5d50f1d06fb586c10865`, while clarifying that the rule does not generalize
  to arbitrary non-OPRA or mixed-source lists. Narrowed the remaining canonical-persistence gap to
  Favorites, General EQs, and pre-canonical legacy rows; Personal imports, DAC captures, recovered
  imports, and catalog snapshots already persist canonical data.

### Historical v0.7 closeout audit snapshot — superseded by the latest implementation result above

- Corrected the stale beta-testing handoff: the live PR baseline when rechecked was `a09d442be728b77dc83b1487814f2428159ddf19`, not the older SHA labeled frozen in that handoff. Earlier signed APKs are not test artifacts for later source changes.
- The a09d baseline's Android CI #1745, CodeQL #1630, catalog #2043, priority coverage #1528, dependency #2052, and signed beta #1350 passed on that exact source. Audit found release minification disabled and no post-sign alignment check; therefore those passes do not close the stricter current release gate.
- Added fail-closed EW300 revision authorization, Peak-only hardware write encoding, and serial redaction in shareable reports. Enabled R8 with a mapping check that requires an application class to be renamed, restricted signing workflows to manual trusted-main execution, delayed secrets until signing, disabled checkout-persisted credentials, and added post-sign alignment/provenance outputs.
- A second independent code review found the dormant Save-qualification service did not itself enforce exact revision or Peak filter authorization, failure text could leak the unit serial, the signed beta still enabled the already-completed Save-qualification flag, and the release workflow had a duplicate YAML key plus a publisher that rebuilt rather than promoted the exact tested APK. Added service-level authorization/Peak guards, hard-disabled that test action in release builds, expanded report redaction, moved R8 mapping verification ahead of signing secrets, fixed the workflow key, and disabled public publication until an exact-artifact promotion workflow is separately reviewed.
- On exact code source `01d524228ec02199e9f41d1dd2c72df5d9e72f66`, Android CI #1748 (unit tests, lint, debug/release assembly, R8 mapping, and connected emulator UI), CodeQL #1633, catalog #2046, and priority coverage #1531 passed. Dependency submission and signed beta did not run on that PR source; no corrected signed APK or immutable artifact provenance is available. No physical test is requested. The status-document update itself is documentation-only and creates a new head requiring fresh exact-head gates.
- At that audit snapshot, corrected-source tests, CI/security/catalog/dependency gates, signed artifact, and product verification remained pending exact-head evidence. The local environment had no Gradle wrapper/system Gradle or Android SDK/ADB, so no local Android build or emulator run was claimed. No previous APK could be reused to fill that gap.
- At that snapshot, the acceptance audit also left canonical persistence/legacy projections, malformed active-filter handling, ToneBoosters ten-band ordering/parity, API-26 and v0.6 upgrade checks, accessibility/offline/error/cross-DAC product validation unresolved. The current implementation result is recorded in `docs/V0.7_RELEASE_READINESS_AUDIT.md`.
- E001 and E043–E046 remain accepted historical physical evidence; no new Apply, Flash, Restore, Reset, Save qualification, or read-only report is requested. PR #23 remains draft; v0.7.0 remains NO-GO pending blocker closure, exact-source gates/signing, final review, and explicit owner approval.

- Owner reports E043-E046 add read-only PASS plus Flash, exact-baseline Restore, and Reset PASS on signed candidate source `7599dd52fc9e8c58c96e021f581b86a669dcc148`; E037-E040 remain the source-381 Apply/Flash/Restore/Reset evidence. Do not repeat any mutation or E001 Save qualification. All six gates passed on report-source 7599; this docs-only closeout makes that artifact stale for new-head provenance, so refresh gates/signing on the exact docs head. The only possible remaining owner check is non-mutating Personal EQ capture/value/provenance, plus My EQs review/cancel only if the successful Flash was not launched from My EQs. Release remains NO-GO pending scope closure and explicit approval.
- Aligned the EW300 My DAC surface with the shared DAC UX: the verified EQ path is presented as usable
  readback/edit/Apply/capture/Flash/Reset/reconnect functionality, while the exact profile's
  playback/global-gain value remains an honest reported device state rather than an unqualified
  standalone volume control.
- Gated EW300 capability-report, operation-report, and exact-baseline-restoration controls to the
  signed validation candidate; public release builds no longer expose testing-only buttons.
- Fixed the My EQs hardware Flash availability gate for the exact EW300 path by reusing the tested Direct Flash plus connected-session predicate; this exposes Flash for a saved EQ only when the existing safety gate passes and does not change protocol or mutation behavior.


### v0.7 EW300 physical evidence continuation

- Recorded current-source physical `APPLY` report E039 (10 writes, one Save, replacement identity and final readback verified) and `RESET` report E040 (11 writes, one Save, replacement identity and final readback verified) on `381286eb7ea29b0707e6da75f3565bada1d73928`. Replay and competing-job fields remain unmeasured/null; the Reset report does not independently export before/after playback-gain bytes.
- Recorded E037/E038 as the same-source physical `FLASH` and exact-baseline post-Save `RESTORE` passes. No further Flash, Restore, Apply, Reset, or Save qualification is authorized from these results alone.
- Recorded latest verified signed documentation head before this documentation-only synchronization `2f99bc7bf183a556bbe9466266abb44cdc02079b` and workflow #1305; APK `EQ-Library-v0.7.0-beta-2f99bc7.apk`, APK SHA-256 `59accf6953dc34f3fdf56efcaa4665b5a29bcaebe9351d18552e2146d4398f64`, artifact digest `sha256:8805075d990eb7ac6c6dc5b7a70ccf14d78309098e81099a3f46f4e318745eb6`. All current-head software/security/signed-artifact gates passed. This synchronization changes documentation only and requires fresh gates; physical E037-E040 remains tied to source 381. Personal EQ capture remains unevidenced; release remains NO-GO pending applicable remaining scope, review, and explicit owner approval.

### 2026-09-22 owner reports on signed source 7599dd5

- Capability JSON (17) passed read-only identity/state validation for the exact fingerprint. Operation JSON (11) verified Flash, JSON (12) verified exact-baseline Restore (`restorationVerified=true`), and JSON (13) verified Reset. Each operation used one Save, had zero permission requests before its first write, matched the replacement session, passed final readback, and ended with known state. Replay and competing-job telemetry remain null/unmeasured.
- The Restore report proves exact baseline restoration at its completion; a separate later Reset then completed, so do not claim the device remained at an arbitrary original baseline. No further Apply, Flash, Restore, Reset, or Save qualification is requested.
- The reports do not identify whether Flash originated from My EQs or EQ Library; they also do not demonstrate end-to-end Personal EQ capture UX. These are presentation/evidence gaps, not unsupported hardware claims.
- Source 7599 passed Android CI #1712, CodeQL #1597, catalog #2010, priority coverage #1495, dependency submission #2017, and signed candidate #1316. This documentation-only follow-up creates a new head and requires fresh exact-head gates and provenance. PR #23 remains draft; v0.7.0 remains NO-GO pending final review and explicit owner approval.

### v0.7 EW300 full-build continuation

- Extended the exact EW300 My DAC Device tab with read-only playback/global-gain state, active Peak-band count, connection freshness, and readable/JSON operation status. The surface remains capability-driven and does not copy unverified Black Pearl controls.
- Added explicit full-response low/high-shelf adaptation for EW300 source conversion. When the existing RMS/max-error gates pass, source shelves produce an `Optimized` five-band Peak representation; native EW300 readback, capture, editor, and Flash remain Peak-only.
- Recorded implementation candidate `381286eb7ea29b0707e6da75f3565bada1d73928`: `EQ-Library-v0.7.0-beta-381286e.apk`, APK SHA-256 `12ae91cb9f9eed88bba45c17f56322c526133ce5de61bbd68d108eacbb84bb9f`, signer `65C1C1256DAE3C49E3548F334C91F0BA991969E9BE9E0B223BA4E253D2114747`, signed workflow #1303, and artifact digest `sha256:4672b6c03153f56d00a3aceab1f49a4d8f83f4e78acb5584a9fbee35087789f1`. Android CI #1690, CodeQL #1575, catalog #1988, priority-community #1473, dependency #1990, signed alignment/signature, install, cold-launch, and temporary candidate-publication gates passed. Current-head physical Flash/restoration, merge, publication, and public EW300 support remain unverified or unauthorized.

- Recorded the current exact signed restoration candidate `dea8439956f41cbf83cda314f5f9f53d878c6181`: `EQ-Library-v0.7.0-beta-dea8439.apk`, APK SHA-256 `72c95bfaedae0f03303c7aac975aa67ca1e7af63b6c40b95809c022238971792`, signer `65C1C1256DAE3C49E3548F334C91F0BA991969E9BE9E0B223BA4E253D2114747`, signed workflow #1296, and artifact digest `sha256:59b15d4a0a726613452b923cdf6bfda483f43974be1510bdf85f636f80bcb559`. Android CI, CodeQL, catalog, priority-community, dependency, signed alignment/signature, install, cold-launch, and temporary candidate-publication gates passed. Physical Flash/restoration, merge, publication, and public EW300 support remain unverified or unauthorized.
- Corrected the restoration success regression assertion after CI showed that Flash and exact restoration each perform 11 qualified register writes; the expected combined count is 22. No production protocol or transaction behavior changed.
- Added the architecture-aligned exact-baseline post-Save restoration transaction for the exact qualified EW300 fingerprint. A verified Flash now retains the complete pre-test five-band/global-gain raw baseline for the signed validation candidate; restoration performs one guarded Save, exact replacement fingerprint/generation validation, and byte-for-byte final readback before reporting `restorationVerified=true`. Ordinary builds do not expose the validation action, and physical Flash/restoration remain unverified.
- Added restoration operation evidence for baseline provenance and explicit failure/uncertainty reasons, plus focused tests for exact restoration, one Save, caller cancellation during detach, wrong identity/generation, final-readback mismatch, exception after Save, no replay, and truthful unmeasured telemetry. Replacement generation checks now require a strictly newer generation after detach.
- Updated the EW300 status, full-build status, capability matrix, hands-on checklist, project runbook, validation ledger, and maintained recovery handoff to distinguish the new software implementation from the still-open physical Flash gate. No Save qualification was repeated.

- E034 lifecycle recovery: source `41aa0b0aca879d8d9b7844e8574a68a5949cc8c8` moves EW300 hardware mutation ownership into the authoritative DAC-session scope, changes EQ Library Flash to a non-suspending ViewModel event, and surfaces terminal Flash feedback from durable operation-trace state rather than Compose coroutine lifetime.
- Added cancellation/re-enumeration regressions covering UI-caller disposal during detach, one Save, exact replacement generation/fingerprint, final readback, uncertain mismatch handling, and truthful unmeasured telemetry. No protocol command, identity scope, Save qualification, or automatic mutation retry was added. Physical Flash remains unverified after E033.
- Added the EW300 reconnect mutation gate and privacy-safe readable/JSON operation report. Automatic reconnect is blocked before the single Save send, no mutation is replayed, and the report records permission-before-write, write, Save, replacement, restoration, and final-readback invariants. Current-head CI and the one consolidated owner hardware session remain pending.

- Recorded the current signed candidate’s exported EW300 `APPLY` evidence: one Save, permission only after the first-write boundary during replacement reconnect, exact replacement identity verification, and final hardware readback match. The report is not a Flash result; replay and competing-job counters remain explicitly unmeasured.
- Added truthful EW300 reconnect feedback: after a verified Apply/Flash/Reset, My DAC states that the DAC reconnected and final hardware readback matched; the connecting state explains that Android may require permission again after re-enumeration. Android’s OS permission prompt remains required when the USB instance is replaced.
- Synchronized the current signed documentation-head candidate `48fb2d6df4cdf59e6d5f7fe48718c9e82a0ef665`: `EQ-Library-v0.7.0-beta-48fb2d6.apk`, SHA-256 `96e094258d186da68555945d389c17817f1687e820beaa04f47917eb36ce6525`, signed workflow #1283. All current-head software, security, signing, installation, launch, and emulator gates passed; no physical retest or release approval is inferred.
- Recorded the final pushed PR head `9e49876cdd5c49ae9263637204b02407f8e36c32`: `EQ-Library-v0.7.0-beta-9e49876.apk`, SHA-256 `cbeb840f712f83c8a742566603d615b4e466319ec9bbfa51117a80e01c1c7d37`, signed workflow #1284. All current-head software, security, signing, installation, launch, emulator, and temporary-candidate publication gates passed; no physical retest or release approval is inferred.
- Recorded E030 from signed candidate `dbc86b638982465ad498556fb23a150aef537ebd`: the exact-device read-only report passed after the app reported an unverified Flash, but the attached files did not include the required Flash operation JSON. No write, Save, final-readback, or restoration claim is inferred.
- Recorded E031 from signed candidate `dbc86b638982465ad498556fb23a150aef537ebd`: the exported Flash report proves 11 writes, one Save, and volatile readback, then reports `ForgottenCoroutineScopeException` after detach before replacement-session handling and final readback. The bounded follow-up moves the EW300 Flash launch to the app-level scope; it remains pending CI and physical retest.
- Recorded E032 from signed candidate `33566e0dba835d356fd6c90231a5c144802643c6`: the targeted EW300 Flash lifecycle fix passed all current-head software and signed-candidate gates and is ready for one bounded physical Flash retest. No physical PASS or release approval is inferred.
- Recorded E033 from the exact-device Flash report on signed candidate `33566e0dba835d356fd6c90231a5c144802643c6`: the app still raised `ForgottenCoroutineScopeException` after 11 writes, one Save, volatile readback, and USB detach; final readback and restoration were not reached. Before/after read-only reports show the global playback gain changed from -53.0 dB to -57.0 dB, so no further mutation is authorized until the lifecycle owner is moved beyond Compose scopes.

- Accepted the frozen signed-candidate evidence for the exact allowlisted EW300 fingerprint (unit serial omitted from this changelog): read-only PASS and `VERIFIED` Save, Peak, playback-gain, exact restoration, and two power-removal checks.
- Added the exact-fingerprint production capability profile and removed the completed Save qualification action from the normal product UI. The Save qualification must not be run again.
- Added a shared strict EW300 baseline/restore coordinator and guarded persistent Apply, Flash, Reset, and reconnect-aware verification paths.
- Added focused authorization and transaction test coverage; complete CI, signing, installation, launch, and final consolidated hardware validation remain required before PR #23 can be considered for approval.
- Final software commit `7887b0795ba3802368f77808a9ff2851e2fd7bdd` passed the complete CI, security, signed-APK, alignment, installation, and cold-launch gates. The exact signed beta artifact is `EQ-Library-v0.7.0-beta-7887b07.apk` with SHA-256 `714500157de686fe82d768dc71fe2bdab28694ba24899a6684854bdc3ae680d1`; one consolidated owner hardware session remains.
- Corrected the shared EW300 editor label so the exact device is shown instead of the legacy Black Pearl label, added regression coverage, and produced signed candidate `048465f4a7ea6ae62b89076de1ecafc0c5dc8bce`. The artifact is `EQ-Library-v0.7.0-beta-048465f.apk` with SHA-256 `e1beeaec461a449d0f3a01df5ec9861f0ff6aa0dc508e295e2c6e6870295d8b0`; all software, security, signing, installation, and launch gates passed.
- Follow-up review fixed stale EW300 DEVICE-tab wording and enabled the exact-profile managed EW300 Flash entry only when Direct Flash, the active EW300 output, and the connected exact session all agree. The fix adds no new USB commands and preserves the no-guess multi-DAC guard. Signed candidate `f8707788531cfdef33cd46d79c5c53e649480232` is `EQ-Library-v0.7.0-beta-f870778.apk` with SHA-256 `b512a949c22848ad991b38831c082c59d9ea223ed3099108c39e1aaec353f033`; all software, security, signing, installation, and launch gates passed.
- The first owner Flash attempt on that candidate was stopped as unconfirmed when Android showed a USB permission prompt and a competing-app chooser during EW300 reconnect. Removed the manifest-wide USB attachment handler that could relaunch EQ Library as a system USB choice; retained exact in-app identity checks, scoped permission, and dynamic reconnect handling. A replacement signed candidate and full gates are required before any Flash retry.
- Replacement candidate `6b3a35c6e8901c89f52ec3588d0feac744cba017` removes the manifest-wide USB attachment handler that caused the competing-app chooser during EW300 reconnect. Android’s normal in-app USB permission flow and exact identity checks remain. Signed workflow #1257 passed all software, security, signing, installation, alignment, source-SHA, and cold-launch gates. The exact APK is `EQ-Library-v0.7.0-beta-6b3a35c.apk` with SHA-256 `810db843cd989a4277978e58749dea215ca5f9c0593927db9c26922505a0d8e8`. The prior `f870778` Flash attempt remains unconfirmed; no hardware PASS is inferred.

### Fixed

- Added a guided, read-only EW300 capability report in My DAC with readable and JSON sharing.
- Added a release-signed-candidate-only EW300 Save qualification tied to the exact source SHA and pinned installed signer. It preserves the complete baseline, uses two small safer reductions, requires two detected full unplug/reconnect checks, restores the baseline exactly, and makes every uncertain mutation a terminal no-retry stop. Ordinary builds hard-disable and omit the action from the UI.
- Serialized EW300 diagnostics through the device operation gate and retained first-failure safe-stop behavior.
- Gated EW300 Personal EQ capture until frequency scaling was resolved, then enabled peak-only capture while keeping ordinary playback gain out of the canonical EQ profile.
- Added an Android emulator gate that verifies the EW300 diagnostic's safety wording, run action, result state, and both report-sharing actions.
- Resolved EW300 frequency scaling without another hardware session by fitting the captured stock filters to same-earpiece DSP-versus-passive acoustic measurements; direct raw Hz fits at 0.989 correlation and 0.152 dB RMS, while the public 2× fallback does not.
- Classified EW300 register `0x66` conservatively as digital DAC/playback gain rather than a dedicated EQ preamp, matching the shared framework rule already used for Black Pearl playback gain.

- Added strict EW300 identity matching beyond VID/PID: expected USB strings, HID interface shape, and a session fingerprint now gate any device-scoped state.
- Made EW300 qualification and applied-gain state fingerprint-scoped, fixed byte-array content comparison in editor readback, and added pre-commit restoration after partial band mutations.
- Disabled persistent EW300 Flash and Reset in the product candidate until command `0x53` and full power-cycle behavior are proven on the exact cable. The default capability utility is read-only and exports a plain-language/JSON report.
- Updated the candidate to versionCode 7 / versionName 0.7.0 and standardized source-neutral wording around EQ profiles from OPRA, AutoEQ, community/general sources, imports, Personal EQs, and DAC captures.
- Added the software-prepared EW300 My DAC shell to the recovery beta, including the shared EQ/DEVICE tab structure. The DEVICE tab is capability-driven and explicitly reports when no additional EW300 controls are verified instead of copying Black Pearl commands.
- Added guarded EW300 five-band readback, editor, capture, reconnect, and Direct Flash paths through the shared registry/session/finite-hardware framework. Global gain, persistence, and Reset remain hardware-validation gated and are not claimed as qualified.

- Preserved the separate EW300 discovery results as protocol evidence while keeping discovery APK/tooling out of the shipping recovery branch.
- Updated the read-only EW300 descriptor diagnostic to derive the exact HID report length from the standard descriptor, make a non-forced host claim of that interface, perform only the standard report-descriptor read, and release the interface. This addresses the owner-observed `-1` descriptor result without sending any HID report or vendor command.
- Recorded the Pixel 9 refusal of the non-forced EW300 HID-interface claim. The next read-only diagnostic may briefly detach Android's driver from HID interface 3, reads only its standard 74-byte report descriptor, and immediately releases it; no HID report or EQ command is sent.
- Recorded the complete owner-captured EW300 HID descriptor: vendor report IDs `0x4B` and `0x54` each declare 10-byte input and output payloads. Added descriptor-derived input-only GET_REPORT discovery; output reports remain prohibited until their semantics are evidenced.
- Recorded that both exact EW300 vendor input reports return zero bytes to read-only HID GET_REPORT. Hardware EQ support remains disabled; no speculative output request was added.
- Recorded an exact-product third-party browser connection that exposes a Save action but no labelled stock-state read or backup. The session was disconnected without an EQ action; its displayed five-filter limit is not adopted as hardware evidence.
- Added a bounded, descriptor-derived passive interrupt-IN observation to the separate EW300 diagnostic: three 250 ms incoming-only reads after the existing standard reads. It sends no HID output, vendor request, EQ, save, or reset command.
- Recorded the owner result from that passive observation: all three exact-device interrupt-IN reads timed out with no bytes. The diagnostic released the HID interface; hardware EQ support remains disabled because this does not establish stock-state readback or protocol semantics.
- Added an owner-authorized provisional stock-state diagnostic derived from the public Hangout.Audio `0x31B2` KT Micro fallback. It is gated on the exact EW300 identity, endpoints, and captured HID descriptor; sends only report `0x4B` command `0x52` READ requests; stops on the first invalid response; and records raw values. WRITE, COMMIT, CLEAR, save, reset, and firmware commands remain excluded.
- Corrected one transcribed Consumer Control usage in the EW300 descriptor safety gate (`0xCE` to the owner-captured `0xCF`). The mismatched candidate remained locked and sent no command; the complete 74-byte gate value now has direct regression coverage.
- Recorded the successful exact-device EW300 stock-state pull: all 12 bounded report-`0x4B` READ requests returned correctly echoed responses. Preserved the untouched slot, five filter pairs, and global-gain payload byte-for-byte as a regression/restoration fixture; no write, commit, clear, save, reset, or firmware command was sent.
- Added the owner-approved, stock-snapshot-gated EW300 reversible write qualification: Band 1 gain moves temporarily from -1.1 dB to -1.0 dB, is read back, then the exact captured four bytes are restored and read back. No commit, save, clear, reset, slot, global-gain, or firmware operation is included.
- Corrected the EW300 preserved stock fixture for register `0x2E` from the transcribed `FB FF` to the repeatedly observed untouched bytes `05 00`; the fail-closed diagnostic sent no write while identifying the discrepancy.
- The owner-qualified reversible EW300 Band 1 test completed: the exact stock snapshot gate passed, a temporary `-1.1 dB → -1.0 dB` gain-byte change was read back, and the original bytes were restored and read back exactly. This remains a diagnostic-only volatile-write qualification; persistence and production EW300 support are not claimed.
- A separate post-reconnect, read-only full snapshot again matched every preserved EW300 stock payload exactly, confirming the restored state without sending any command other than READ.
- The owner completed the seven-check consolidated EW300 field-qualification batch: all temporary values read back, each baseline restored, and the final full stock snapshot matched exactly. No persistence-style command was sent.
- Prepared the owner-approved, fail-closed remaining-field qualification: eight temporary frequency/Q raw-word checks for Bands 2–5, with exact readback, immediate per-check restoration, first-failure stop, and a required final full exact snapshot. Persistence and all Save/Commit-style commands remain excluded.
- The owner completed that final eight-check EW300 batch. Temporary frequency/Q writes for Bands 2–5 read back exactly, each baseline was restored, and the final full 12-register snapshot matched the untouched capture. Together with the prior batch, volatile gain/frequency/Q write/readback/restore transport is qualified across five observed bands; filter types, value semantics, persistence, reset, slot, and global-gain operations remain disabled.
- The owner completed the consolidated EW300 Band 1 filter-type qualification: provisional raw codes 1–4 each read back exactly, restored the captured bytes, and passed the final full-snapshot gate. Added the Android-free EW300 volatile protocol boundary and regression coverage for changing only the observed filter-type byte. Production registration remains intentionally disabled pending acoustic, range, persistence, and reset qualification.

### Documentation

- Added the approved v0.7 SIMGOT EW300 DSP implementation plan, including protocol-evidence gates, reuse of the scalable My DAC framework, automated validation, signed-candidate handoff, and the owner physical-test boundary.

## [0.6.0] - 2026-09-15

### Added

- **My DAC** for supported hardware, with one ViewModel-scoped DAC session shared across My EQs, My DAC, and EQ Library; successful connection/reconnection automatically refreshes supported EQ + DEVICE state and later physical reattach can reopen the replacement session after one successful current-app-session connection.
- Black Pearl current-EQ inspection, response graph, local EQ editing with Review → Apply/readback verification, Personal EQ capture, qualified Reset EQ to flat, concise DEVICE controls, read-only UAC mode detection/manual startup help, and session-sticky Last-read state after disconnect.
- Black Pearl **Restore defaults** using the physically qualified EQ Library-owned preset: 50% volume, FAST-LL, HIGH gain, CLASS AB, centered balance, and microphone gain 0 dB, with an optional OFF-by-default **Also reset EQ to flat** action. The preset is not presented as verified TRN factory state.
- Capability-driven FiiO JA11 My DAC/session integration while preserving its independently established native device semantics; exact physical JA11 qualification remains pending.
- Persistent **Needs attention** recovery for exact app-owned/persisted-access preset artifacts that no longer have a confident current My EQ association. Strict recovery preserves supported PEQ values and original-file provenance, requires user-supplied missing headphone/name identity, and never scans arbitrary external storage.
- An owner-approved premium v0.6 UX audit/blueprint covering Apple, Material 3, a combined native-Android direction, information hierarchy, navigation, top bars, settings/list anatomy, spacing, typography, icons, feedback, and accessibility. Implementation is approved; v0.6.0 publication is complete.
- Scheduled public GitHub/Gist community ingestion now takes discovered headphone PEQ through exact source retrieval, strict PEQ parsing, canonical headphone identity, creator/source provenance, acoustic dedupe, living-archive validation, and Unverified publication instead of leaving mechanically valid community data indefinitely review-only.
- Community-ingestion reports record fetched/parsed/published/deduplicated/quarantined counts and machine-readable quarantine reasons; one malformed or ambiguous record cannot block unrelated valid candidates.
- Broad General GitHub discovery is actively audited for exact parametric structure. Fixed/graphic-EQ data without source-provided Q/filter types is rejected rather than converted by invention.
- Added bounded public RSS/thread automation for Head-Fi and Audio Science Review, with exact-PEQ parsing, provenance retention, identity resolution, quarantine/failure isolation, source-health state, and living-archive-safe publication.
- Added automated Squiglink ecosystem currentness using the public Squiglink-compatible site registry without turning measurement curves into invented source-authored PEQ.
- Added the unified `Automated source currentness` GitHub Actions lane for cadence-aware Head-Fi/ASR refresh, Squiglink currentness, qualified General-source probing, atomic archive validation, persistent source-health state, and `catalog-live` publication from `main`.
- Added an automation contract regression test that requires zero recurring `manual` currentness owners in the registered-source set.

### Changed

- **My EQs is one device/output-agnostic local library.** Managed headphone/profile selections, Favorites, Personal EQs, captured DAC EQs, and General EQs no longer change identity or visibility when the active target changes. Legacy per-output tables/signatures remain migration compatibility only.
- Save/Add to My EQs is now deliberately separate from **Export** and **Flash**. Saving changes local library state only and does not automatically open the folder picker, write a file, or write hardware.
- The active output is an action/derivation context: it may change compatibility, fidelity, target-derived currentness, Export behavior, connection controls, or Flash availability, but does not change My EQs ownership or dismiss an open managed-headphone detail.
- DAC-captured EQs use the source DAC as provenance only and become normal device-agnostic Personal EQs.
- EQ selection/Flash lives where the EQ already lives in **My EQs** and **EQ Library**; My DAC no longer duplicates this with a Change EQ chooser.
- Black Pearl DEVICE settings use one concise current-value/edit surface with immediate qualified write + verified readback for routine settings and short confirmation only for level-sensitive changes; full-EQ editing remains staged locally until Review → Apply.
- My DAC operation feedback now uses one shared bottom snackbar lifecycle for Applying, reconnect verification, verified success, and recoverable failure. The DEVICE header keeps a fixed connection summary so settings never jump during hardware work; the same contract is ready for additional capability-driven DAC adapters.
- `github-community` is a daily scheduled source with source-health ownership. Public exact structured community PEQ uses the established Unverified community policy while specific source restrictions remain binding.
- Source maintenance is now automation-first: every registered source with a legitimate stable public retrieval path is scheduled or runtime-managed; sources that cannot currently be automated safely are explicitly paused rather than depending on recurring manual input.
- Head-Fi and Audio Science Review moved from manual/curated currentness to weekly scheduled public adapters.
- Squiglink-compatible sources moved from manual intake to weekly scheduled ecosystem-currentness/provenance monitoring. Measurement-only `phone_book`/frequency-response data remains non-PEQ and is never converted into fabricated source-authored PEQ.
- ParaEQ General presets moved to weekly scheduled qualified-source currentness probing; upstream source changes remain review-gated rather than silently mutating qualified preset classification or acoustic history.
- Topping Community is paused instead of relying on recurring manual capture. It can become scheduled only when TOPPING provides an authorized public API/feed or explicit permission for the required automated retrieval.
- oratory1990 direct Reddit currentness is paused while Reddit access is unavailable; structured values may continue to arrive through separately automated qualified carriers such as OPRA.
- Reddit remains paused; no anonymous Reddit scanning, scraping, circumvention, or manual-currentness substitute was introduced.

### Fixed

- Corrective Black Pearl Restore-defaults retest passed on exact signed `eb19800`; the restore reached all approved DEVICE targets and the compact My EQs / General EQs layouts passed owner review.

- Black Pearl Restore defaults waits for its exact verified write cycle rather than aborting on an older screen snapshot, checks the original USB session and all final values, and explains retained safety volume on genuine failure. No automatic write retry or speculative recovery command is introduced; physical retest is pending.
- Managed-headphone details and General EQs use compact, scrollable headers and wrapping actions; notification/removal options and explanatory help move out of the main content area.

- Removed the final save-to-auto-export coupling from General EQ batch Save.
- Changing the active target no longer clears the currently open managed-headphone detail or drives selection reconciliation.
- Personal EQ import/parser integration now compiles with the strict `ParametricEqTextParser` path restored.
- Unresolved app-owned artifacts can no longer silently disappear merely because current library identity cannot be resolved; provider/permission unavailability remains unresolved instead of being treated as confirmed absence.
- Acoustic catalog deduplication now carries app-owned export ownership forward to the canonical profile ID, so removing a preset also cleans exports created before that identity migration instead of leaving duplicate **Needs attention** entries.
- Recovery/deletion remains bounded to exact ownership-tracked SAF documents and refuses malformed/unsupported content rather than silently dropping unsupported filters.

### Validation

- Public v0.6.0 release published at https://github.com/weekssa/OPRA-EQ-for-UAPP/releases/tag/v0.6.0 from merge commit `e5ffa5d00862edc3b79bf52e1508db5244845e94` via workflow run #35008912862; public APK SHA-256 is `93b5250d7f32b068f24702c9fbfacb697e7921100250ee0652981100e29adb62`.

- The approved DEVICE operation-feedback UX pass passed the full Android/software gates on exact source `eef5633e18a4ac311f110493e29633bf382675e3`; Signed EQ Library Beta Candidate #1229 published the exact Pixel 9 test APK with SHA-256 `4eee1f491971bd49ec72d10033b83a548c9b970c002befe79cc944f268a4cb59`.
- FiiO JA11 physical qualification is explicitly deferred and is not a blocker for the Black Pearl/UI v0.6 closeout; its capability/session implementation remains available for a later hardware-validation pass.
- The owner completed the exact signed Pixel 9 smoke pass for the stable DEVICE header, bottom operation feedback, and Restore-defaults flow on candidate `eef5633`; **OWNER-REPORTED PASS**. FiiO JA11 remains deferred.

- TRN Black Pearl automatic physical reconnect passed the focused Pixel 9 retest on exact signed source `a3bc1740ed44892f4e6b78f9d0a359e2a87ef663`; the app reopened a fresh replacement session without an app Connect/Refresh tap, refreshed EQ + DEVICE state, showed no Applying/Flashing transaction, and pushed no cached state. Evidence category: **OWNER-REPORTED**. The candidate APK SHA-256 is `3154748c72ad576a0b13b8adbdf549cc36eede6feef900dd68eecbc65bf1d64a`.
- TRN Black Pearl **Restore defaults** passed the focused Pixel 9 test on exact signed source `b0340842dd88dc85613d9441fcc72ceadf877b20`; Cancel made no change, checkbox OFF restored 50% / FAST-LL / HIGH / CLASS AB / Centered / microphone 0 dB while preserving the non-flat hardware EQ, checkbox ON established the same DEVICE targets plus Flat EQ, saved My EQs remained untouched, and restored state persisted through reconnect. Evidence category: **OWNER-REPORTED**. Candidate APK SHA-256: `dcae4f54881976af70e93c2c796b6993697d1a0a8f333698e9d925933c61ff37`.
- The parser/import repair at `0adb34b640261d16d934a80ab29ffe94d3dc253c` passed Android CI, CodeQL, catalog currentness, and priority-community coverage before the final library/documentation closeout.
- Added strict recovery/parser coverage for valid Personal PEQ text, deterministic EQ Library UAPP/ToneBoosters XML, exact preamp/band preservation, malformed/incomplete rejection, unsupported-filter rejection, GraphicEQ rejection, destination-independent unresolved ownership, exact-URI uniqueness, and recovered provenance preservation.
- Later device-agnostic library/recovery/documentation work does not replace the pinned Black Pearl hardware evidence unless it changes connection ownership, read timing, write sequencing, persistence, verification, or session behavior.
- Initial GitHub community ingestion fetched all 50 current headphone candidates: 39 parsed as exact supported PEQ, producing 28 new Unverified profiles and 11 exact-duplicate provenance merges; 11 unmatched/ambiguous headphone identities were quarantined. Atomic living-archive validation passed before the candidate catalog was committed.
- All three current broad General GitHub candidates were processed and classified `no_exact_parametric_structure`; none was published with invented Q/filter types.
- Added regression coverage for valid Unverified publication, missing-preamp preservation, exact-duplicate provenance merging, short model identity with manufacturer context, target-folder handling, malformed/unsupported PEQ quarantine, unknown headphone quarantine, and General graphic-EQ rejection.
- Live branch automation recorded successful source-health scans for Head-Fi, Audio Science Review, Squiglink ecosystem currentness, and ParaEQ, with zero consecutive failures for those newly automated lanes.
- The unified automated-source workflow passed its adapter/automation-contract tests and atomic living-archive validation on the feature branch before final PR closeout.

## [0.5.0] - 2026-09-09

### Added

- A unified output registry that groups selectable targets as **Hardware DACs**, **Apps**, and **Universal formats**, with one declared source of truth for display labels, file-vs-hardware behavior, format kind, capability profile, and validation status.
- Additional selectable app/universal output contexts, including Equalizer APO-style parametric text, EasyEffects-compatible parametric import, portable AutoEq GraphicEQ, and **TOPPING Tune** AutoEq text alongside the existing UAPP/ToneBoosters, Poweramp, and Wavelet paths.
- **TOPPING Tune** as a selectable App output using documented AutoEq `.txt` import, a 10-band finite-target adapter, documented ±12 dB preamp/filter-gain and Q 0.1–15 limits, and conservative Optimized fidelity until downstream device storage precision is independently qualified.
- **FiiO JA11** as a hardware-only five-band PEQ output with an independent Direct Flash toggle (OFF by default), strict USB identity, read/apply/readback/save verification, and Reset EQ to flat.
- **JCALLY JM12** on stock firmware as a separate hardware-only five-band PEQ output with its own Direct Flash toggle (OFF by default), strict USB identity/register protocol, readback verification, fail-safe EQ bypass during replacement, tracked playback-gain adjustment, and Reset EQ to flat.
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
- Candidate source `30535bd3b1bce9940d23e8735d88a4d9b6a9a4ef` passed Android CI run #1018, CodeQL run #899, Signed EQ Library Beta Candidate run #691, catalog currentness, priority community coverage, and automatic dependency submission. Its signed APK `EQ-Library-v0.5.0-beta-30535bd.apk` has SHA-256 `5a2d4ff47097b1ba37b6bd625a4bfd3de444bf1895d4c0d0484fa2075adea042` and matches the repository-pinned release signer certificate.
- Black Pearl v0.5 hardware/DSP qualification is complete: the destructive/fidelity/persistence regression passed on signed behavior candidate `34a9cd819466cb301456c052eecadb02e6271e5e`, and the required focused Reset-result wording follow-up also passed after the informational correction. The later `30535bd3...` architecture refactor preserves device/DSP/conversion behavior, so documentation-only release closeout does not invalidate that completed qualification.
- FiiO JA11 and stock JCALLY JM12 remain **Hardware validation pending** until their pinned exact-candidate Pixel 9 hands-on checklists pass.
- Stock JM12 power-cycle persistence is not claimed: no independently corroborated explicit Save command is used, and persistence/tracked-gain reconciliation must be resolved by the physical checklist before qualification.
- Phase 1 implementation/release preparation and Phase 2 Pixel 9 release-candidate testing both completed before publication.
- Final publication source `ff2fa351d5f38f9dcf37a77859f1e988bbdb76a8` passed Android CI #1049, CodeQL #930, and Automatic Dependency Submission #1174 before **Signed GitHub Release #5** / run ID `34341588059` rebuilt, signed, verified, and published v0.5.0 successfully.
- The public `v0.5.0` tag points to `ff2fa351d5f38f9dcf37a77859f1e988bbdb76a8`. Public APK `EQ-Library-v0.5.0.apk` has SHA-256 `58e6ac5c62f9af1caf354f97cf2e7d9e2bcddea9937fac3c35532c279cd429eb`, and GitHub latest-release metadata exposes v0.5.0.

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
- v0.4.0 was subsequently published through the controlled signed-release path while preserving the qualified Black Pearl device/DSP behavior and permanent signing identity.

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

## 2026-09-21 EW300 release-candidate gate — SOFTWARE PASS, HARDWARE PENDING

The exact signed EW300 candidate from source `855364e8a9d758f12e7d2a48bdaf89f457e28070`
passed the complete automated, security, signing, emulator installation, cold-launch,
accessibility, and release-polish gates. APK SHA-256 is
`bfcb77f4774e8f0a9e48c1da13a262e9033c8e0f42a7a18774e864c0b1774b44`; the pinned signer remains
`65C1C1256DAE3C49E3548F334C91F0BA991969E9BE9E0B223BA4E253D2114747`. The one consolidated
physical EW300 session remains outstanding. No merge, publication, or public support claim is
authorized.

## 2026-09-21 EW300 signed candidate Apply stop — NO WRITE

The owner’s exact-candidate Apply attempt stopped safely before mutation. The exported operation
report records `InvalidPlan`, zero register writes, zero Save commands, zero permission requests,
and known device state. Source diagnosis found that the shared editor treated EW300’s verified
global-gain register as a dedicated EQ preamp; the recovery branch now maps it as an absolute
device-global-gain headroom mechanism. The old APK must not be retried. A replacement signed
candidate and complete gates are required before the consolidated physical session resumes.

## 2026-09-21 EW300 replacement candidate — SOFTWARE PASS, HARDWARE PENDING

Corrected the EW300 editor’s headroom mapping so the verified absolute device-global-gain
register is used as the baseline instead of an unavailable dedicated EQ preamp. Replacement
source `8bb87aac689ce28b1e92e115a3f83f1b59ad1f65` passed Android unit/lint/build, emulator UI,
CodeQL, catalog, priority-community, dependency, signing, alignment, installation, and cold-launch
gates. The exact signed APK is `EQ-Library-v0.7.0-beta-8bb87aa.apk` with SHA-256
`f470a3d330951705f8bd94450f9adaa813040d8717541eeed75956f43b97607d`; signed workflow #1260;
the immutable testing APK is [available here](https://raw.githubusercontent.com/weekssa/OPRA-EQ-for-UAPP/mobile-test-apk/candidates/EQ-Library-v0.7.0-beta-8bb87aa.apk).
No physical mutation has been attempted with this candidate. The one consolidated EW300 session
remains pending, and merge, publication, and any public support claim remain owner-controlled.

## 2026-09-21 EW300 physical session — HARDWARE STATE RESTORED, APP TELEMETRY INCOMPLETE

On exact candidate `7035518b042a7b19c0495869cf247359ee27b4a2`, the EW300 re-enumerated after the
reviewed edit and Android displayed the documented replacement USB permission request. The first
attempt timed out because the replacement authorization was not completed, but a subsequent
read-only report confirmed the edited `+4.00 dB` value. A required restoration transaction then
returned Band 1 to `+4.50 dB`, and the final complete read-only snapshot matched the original
baseline with global gain unchanged. The candidate Apply operation report was not exported, so
the app’s Save/readback counters and end-to-end operation outcome remain unverified. No further
physical mutation is authorized for this candidate.

## 2026-09-21 EW300 reconnect-gate candidate — SOFTWARE PASS, HARDWARE PENDING

After the `8bb87aa` candidate repeatedly surfaced an Android USB permission prompt when a
pre-Save operation stopped, automatic EW300 reconnect is now held until the owner explicitly
connects again. A Save-sent operation still releases the documented replacement-session readback
path. Source `f41f985cb9f7a5d1622bc2e85ddfe5d14777e25d` passed the complete software, security,
signing, emulator, installation, and release gates. The exact signed APK is
`EQ-Library-v0.7.0-beta-f41f985.apk` with SHA-256
`0f70d9f290c487691992ac657d8f2dae1ccba844a99749c6b44e37895ce4c345`; signed workflow #1261.
The previous physical attempt remains a permission-denied stop with no verified Apply result.

## 2026-09-21 EW300 queued-reconnect guard candidate — SOFTWARE PASS, HARDWARE PENDING

The EW300 automatic reconnect path now rechecks the reconnect gate at invocation time, preventing
a stale queued callback from requesting Android USB permission after a pre-Save stop has closed
the gate. Source `7035518b042a7b19c0495869cf247359ee27b4a2` passed Android unit/lint/build,
emulator UI, CodeQL, catalog, priority-community, dependency, signing, alignment, installation,
and cold-launch gates. The exact signed APK is
`EQ-Library-v0.7.0-beta-7035518.apk` with SHA-256
`661ca49488939c15f290868f1be05ab24dfb1b1a870b977455dabfedce409c66`; signed workflow #1262.
No physical mutation has been attempted with this candidate. The one consolidated EW300 session
remains pending, and merge, publication, and any public support claim remain owner-controlled.

## 2026-09-21 EW300 trace-boundary follow-up — SOFTWARE PASS, HARDWARE GATE OPEN

The bounded Extra-High review found one actionable evidence defect in the prior candidate: Apply
did not mark the operation trace immediately before its first hardware write. The follow-up at
source `d858cc56728ab3fd6deef0b158a35d1c04149f7e` adds that boundary and a direct regression test
that a queued automatic reconnect rechecks the mutation gate before launching USB connection.
Android unit/lint/build, emulator UI, CodeQL, catalog, priority-community, dependency, signing,
alignment, installation, cold-launch, and signed-beta publication checks all passed. The exact APK
is `EQ-Library-v0.7.0-beta-d858cc5.apk` with SHA-256
`96b453674b2b5cea6cdc3c18ba33d60187f56520cb9abf44db79fb66089613db`; signed workflow #1270.
This evidence-only follow-up was not physically tested; no further physical mutation is authorized.
Replay/competing-job counters remain unmeasured zero fields, and PR #23 stays draft with no merge,
publication, or public EW300 support claim authorized.

## 2026-09-21 EW300 final guard-seam candidate — SOFTWARE PASS, HARDWARE GATE OPEN

The final bounded Extra-High review found that the reconnect regression should exercise the
production guard path rather than duplicate its condition in a local test lambda. Source
`95e5e3597ce3bc7d3c1011bd0a79e978fc9a6e64` adds the minimal `runAutomaticReconnectIfAllowed` seam,
routes `connectAutomatically()` through it, and tests zero launches before Save release and one
afterward. The complete Android, emulator, security, catalog, coverage, dependency, signing,
installation, cold-launch, and signed-beta checks passed. The exact APK is
`EQ-Library-v0.7.0-beta-95e5e35.apk` with SHA-256
`adb672e09c0fb2237814bb05451df7fe9f1785008de082bda225f5efe5157914`; signed workflow #1272.
This final code candidate was not physically tested; no further physical mutation is authorized.
Replay/competing-job counters remain unmeasured zero fields, and PR #23 stays draft.

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

This PASS closes the corrective Black Pearl physical gate. It does not establish TRN factory-default semantics and does not qualify FiiO JA11 hardware behavior. PR #16 was merged and v0.6.0 publication is complete. FiiO JA11 physical qualification remains hardware-validation-pending.
