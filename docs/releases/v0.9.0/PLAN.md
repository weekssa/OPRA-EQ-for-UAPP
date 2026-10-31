# OPRA v0.9.0-beta.1 — Consolidated Requirements, Execution Boundaries, and Release Plan

## 1. Binding contract and complete source coverage

This plan replaces the earlier drafts and incorporates the uploaded specification and subsequent decisions from this chat. Implementation begins only after approval.

The complete [uploaded specification](</Users/stephenweeks/.codex/attachments/717262dc-0f7f-403d-9b02-1263bed133d9/Pasted text.txt>) remains binding. Its SHA-256 is:

```text
5d3833e11a014f23753d68bb67b710b53c86062493ed3f352a83518a104b77f7
```

The verified source contains 556 lines, 12 numbered sections, 170 bullet entries, 46 numbered procedure entries, and 16 acceptance rows, plus prose requirements. These counts assist reconciliation; they do not replace semantic review.

After approval, create one canonical plan under the new worktree’s `docs/releases/v0.9.0/` directory, with:

- The complete upload preserved verbatim as its source appendix.
- One plain Markdown requirements checklist mapping every source obligation, prohibition, procedure, and acceptance condition.
- Explicit entries for the decisions added in this chat.
- A generated `SEAL.sha256`.
- One concise execution report containing results, candidate identities, correction counts, and outliers.

Each independently verifiable obligation receives an ID, source location, verification method, status, and evidence. Repeated requirements may share evidence but retain their source mappings. No agent may omit a requirement because it appears difficult, repetitive, or inconvenient.

The following source-coverage groups are all mandatory to reconcile:

| Source section | Required coverage |
|---|---|
| 1. Success | Working installable beta; preserved functionality; correct persistence; truthful failures; companion preservation; Android correctness; automated and physical qualification; provenance |
| 2. Fixed scope | Every existing control; native-persistence investigation; five individual digital filters; all exclusions |
| 3. Android engineering | Architecture/state, USB/lifecycle, UI/accessibility, security/build requirements |
| 4. Seal | All original seal fields, discovery classification, amendment rules |
| 5. Baseline | Repository safety, reversible preservation, actual inventory, baseline builds/tests, licensing |
| 6. Development | Evidence comparison, protocol implementation, integration, complete applicable verification, bounded remediation |
| 7. Escalation | Genuine blockers, recoverable failures, evidence requirements, minimum owner action |
| 8. Acceptance | Original A01–A16 plus the additional explicit checks below |
| 9. Hardware | Frozen candidate, preparation, complete 19-step sequence, power removal, stop conditions, restoration |
| 10. Release gates | Approval, autonomous work, main-only signing, hardware participation, separate publication approval |
| 11. Reporting | Every requested execution-report field, evidence limits, completion dispositions |
| 12. Operating rules | Preserve behavior, reuse architecture, avoid expansion/replay, qualify exact artifacts, stop at completion |

The agreed amendments are preserved explicitly:

1. Main-only signing, with exact-PR merge approval before signing and final hardware testing.
2. Three diagnosed correction cycles per mandatory failing gate.
3. Reference-first native persistence as the primary engineering path, with a qualified fallback.
4. Mandatory individual investigation of all five digital-filter modes.
5. Verified unchanged EQ staging is permitted when demonstrated necessary.
6. Wireless ADB recovery is used only when needed at the final physical stage.
7. Unrelated discoveries are reviewed afterward rather than implemented during this release.

## 2. Product outcome and permitted implementation

### Core persistence problem

Persist the selected JA11 EQ program/preset and approved volume across genuine unplug/replug **without microphone toggling and without losing or changing the existing EQ configuration**.

Keep these concepts separate:

- Active EQ programs: **Vocal, Classic, Bass, User 1, Off**.
- The stored five-band User 1 EQ and global gain.
- Digital reconstruction-filter modes.
- Runtime Apply versus persistent Set as Default.

A successful runtime readback does not establish persistence. Detecting a failed save does not satisfy the separate requirement that saving actually works.

### Required historical comparison

Use the preserved candidate `620e1a46761de2d3621dbcd32135193efb42cf49` as failure evidence:

- Volume 60→56 while Off read back successfully, without recorded physical power-loss proof.
- Off→User 1 followed by native Save produced `FINAL_EQ_MISMATCH`: the non-flat User 1 bank became flat.
- Returning to Off did not restore the previous bank.

That candidate already used `0x19` with payload `3` for User 1. Reintroducing the same command alone is not a fix. Compare its missing staging/Apply behavior against the external implementation, without presuming the firmware-level cause.

The later microphone-round-trip mechanism preserved the tested volume 19 / Classic pair across recorded power removal, but its User 1 bank comparison was unavailable. Preserve those evidence limits. Do not resume the historical task, repeat its mutations, or claim its original bank was restored.

### Required external implementation reference

Use [Ircama/ja11-config at revision `affddff6c9808c33ce8b35b0b9759ff0d7f6e405`](https://github.com/Ircama/ja11-config/tree/affddff6c9808c33ce8b35b0b9759ff0d7f6e405) as the primary implementation reference before broader research.

| Decimal | Hex | Reference command | Role |
|---:|---:|---|---|
| 17 | `0x11` | `DAC_FILTER` | Select digital filter |
| 21 | `0x15` | `FILTER_PARAMS` | Stage a PEQ band |
| 23 | `0x17` | `GLOBAL_GAIN` | Read/write global preamp |
| 24 | `0x18` | `APPLY` | Apply staged settings to RAM |
| 25 | `0x19` | `SAVE_FLASH` | Commit to nonvolatile memory |

The reference uses Apply payload `1` and Save payload `3`. Its Save helper stages five bands with 25 ms waits, waits another 10 ms, issues Apply, waits 50 ms, then sends Save. Its separate full-Apply path also stages gain and filter state. Examine the complete call paths and documentation together. [Protocol definitions](https://github.com/Ircama/ja11-config/blob/affddff6c9808c33ce8b35b0b9759ff0d7f6e405/ja11-config-tui.h), [implementation](https://github.com/Ircama/ja11-config/blob/affddff6c9808c33ce8b35b0b9759ff0d7f6e405/ja11-config-tui.c)

Required work:

- Obtain and preserve the pinned source.
- Compile the configuration target in an isolated supported environment using `make ja11-config-tui`.
- Review EUPL-1.2 obligations and compatibility before incorporating source.
- Reuse or adapt applicable implementation and derive comparative tests.
- Reconcile discrepancies with qualified OPRA encoding; do not regress established codecs.
- Establish Save-payload semantics, staging, ordering, timing, counts, and field-specific persistence.
- Research specific unresolved gaps rather than restarting protocol discovery.
- Do not build or execute firmware-update/bootloader tools as part of this work.

### EQ-preserving native transaction

The native path must:

- Preserve every User 1 band’s frequency, gain, Q, and type, plus global gain.
- Preserve the requested active program and volume.
- Perform no microphone writes.
- Preserve UAC and unrelated configuration.
- Avoid Reset, flat/default substitution, unrelated curve flashing, and helper program changes without demonstrated necessity.
- Reject incomplete, stale, or ambiguous state.
- Permit only read-only recovery after uncertain mutation.

As approved, identical restaging is allowed **only from a fresh authoritative device snapshot**, when required by established transaction behavior. Cached graphs, library profiles, optimizer output, and Off/Flat presentation are not substitutes for stored-bank evidence.

An expected USB restart/re-enumeration may be intrinsic to Save. It must not reset or alter the EQ.

### Clear fallback

The native approach receives the required research, implementation, tests, and bounded correction effort.

If it remains unqualified, retain the established explicit microphone-round-trip mechanism for the affected settings only if mandatory beta acceptance still passes. Report the native replacement as unqualified; do not describe fallback delivery as successful native implementation.

Never trigger the microphone workaround automatically after an uncertain native Save. A fallback candidate change requires a new artifact identity and affected verification.

### Five separate digital-filter obligations

| Mode | Reference `0x11` value | Required treatment |
|---|---:|---|
| FAST-LL | `1` | Individual research, implementation decision, tests, qualification/disposition |
| FAST-PC | `2` | Same, independently tracked |
| Slow-LL | `3` | Same, independently tracked |
| Slow-PC | `4` | Same, independently tracked |
| NON-OS | `5` | Same, independently tracked |

For each mode, establish meaning, mapping, valid values, replies/readback, exact-device applicability, runtime/persistent behavior, sequencing, companion effects, provenance, licensing, and independent corroboration. Related forks do not constitute independent confirmation.

Use typed JA11 values and existing DEVICE rows. Unsupported or unverified values must not appear as functioning controls. Distinguish unavailable hardware capability from insufficient evidence.

### Preserved functionality and Android requirements

Individually cover five-band EQ, global gain, volume, all EQ programs, microphone/headset controls, UAC, available sample-rate and firmware readback, Flash, editor, Reset, and explicit Set as Default.

Reuse the authoritative session, mutation gate, repository boundaries, canonical EQ pipeline, and My DAC shell. Cover permission grant/denial/invalidation, rapid attach/detach, mid-command disappearance, ambiguous devices, background/foreground, recreation, process termination, stale sessions, cancellation, and read-only reconnect.

Verify truthful states, existing navigation/Material patterns, screen readers, touch targets, display scaling, manifest/USB intents, PendingIntent/receiver rules, minimal permissions, privacy, SDK compatibility, signer continuity, upgrade behavior, and R8.

Correct the inspected defaults failure-publication defect and unexpected-EQ-change success handling through reproducing tests. Other suspected defects require evidence before correction.

## 3. Firm boundaries, finite troubleshooting, and outliers

### What the seal freezes

The seal freezes approved requirements, baseline identity, permitted change areas, commands, evidence references, and acceptance rules. It does **not** require knowing every implementation defect beforehand.

A newly discovered defect is eligible for correction only when a short record identifies:

1. The existing requirement or mandatory gate it prevents.
2. Concrete evidence or reproduction.
3. The smallest necessary correction.
4. The verification that will prove it.

A permitted file area is not permission for unrelated edits inside that area.

Ordinary corrections within scope do not require resealing. Feature expansion or changes to the approved baseline/permitted areas require an owner-approved amendment.

### Explicit exclusions

No firmware updates, bootloaders, speculative amplifier controls, unrelated device protocols/hardware, architecture replacement, general redesign, opportunistic features, repository-wide cleanup, or broad unrelated dependency upgrades.

No new controller, scheduler, continuous audit service, custom enforcement framework, general diagnostic product, or configuration-history system. Use existing tools, Git inspection, the checklist, and concise evidence records.

### Bounded research and correction

For unresolved native persistence and each filter, complete three substantive passes:

1. Understand the existing implementation, reference, original evidence, and provenance.
2. Corroborate through relevant official/independent sources and supported access alternatives.
3. Validate through appropriate comparative, negative, transaction, and lifecycle tests; identify remaining proof precisely.

Do not manufacture unsuccessful edits or unsafe hardware tests to satisfy a cycle count.

For a mandatory failing gate, allow three diagnosed correction-and-verification cycles and one unchanged-failure rerun for a documented transient condition. Changing agent, branch, candidate name, or failure label does not reset the same failure’s counter.

After the budget is exhausted, record the unresolved mandatory gate and evidence. Finish independent planned work, then provide one consolidated escalation. Do not relabel the requirement optional.

Before reporting inability to access or fix something, inspect the exact error, owning source, environment, logs, official guidance, and supported alternatives. An initial test, SDK, Gradle, emulator, or connection failure is not automatically a blocker. A demonstrated authorization boundary does not justify bypass attempts.

### Outliers and stopping rules

Maintain one outlier list in the execution report: issue, evidence, release relevance, and suggested later workstream.

- If an issue does not prevent an existing approved requirement, record it and continue.
- If it blocks a mandatory gate, diagnose it and apply only the minimum permitted correction.
- If correction requires material expansion, escalate that dependency rather than expanding the release.
- Do not launch broad searches for adjacent defects.
- Do not reopen passing work without changed relevant inputs or contradictory evidence.
- Do not repeatedly rebase or rebuild to chase unrelated catalog/documentation changes.
- Do not reopen the entire project after each review or correction.

At handoff, review outliers once and recommend whether they merit a separate issue, plan, or project. Do not start those fixes or create new projects automatically.

## 4. Preparation, verification, and final hardware qualification

### Safe preparation

Fresh checks confirm public stable v0.8.3 and remote main `35f0c90a0e55e701d4343ed978a440d34aae13aa`.

After approval:

1. Verify repository, ancestry, status, and remote state.
2. Use an isolated worktree on `codex/v0.9.0-ja11-persistence`.
3. Preserve unrelated changes and historical branches/worktrees.
4. Create a verified reversible archive of pre-existing untracked content outside Git, retaining paths, hashes, and metadata; leave historical controller files untouched.
5. Select one exact reproducible baseline.
6. Inventory actual modules, application ID, wrapper/AGP/Kotlin/JVM/SDK, transport/session implementation, tests, emulators, signing, and minification.
7. Establish consistent JDK 17, SDK, and Python 3.10+ environments.
8. Run baseline compilation and relevant tests before product changes; separate pre-existing failures.
9. Seal all original source-required fields, including source-tree/build digests, approved areas, exact commands, protocol revision, acceptance, and evidence locations.

The retired controller remains unused. Preserve existing provider configuration and use the canonical native/OpenCodex v1 workflow with bounded investigators and one coordinated implementation writer.

### Computer-based verification

Run focused checks during implementation. Complete the applicable suite on a coherent candidate; after later changes, rerun affected gates and broaden only when evidence warrants it. This clarifies the upload’s rerun wording without removing a required gate.

Discovered primary tasks include:

```bash
./tools/codex-android :app:testDebugUnitTest :app:lintDebug :app:lintRelease \
  :app:assembleDebug :app:assembleRelease :app:assembleDebugAndroidTest \
  --console=plain -PCANDIDATE_SOURCE_SHA=<candidate-sha>

bash tools/verify-r8-mapping.sh
```

Record the complete actual command inventory before sealing, including repository/Python, release-contract, publisher, catalog/sample, security, and hosted checks.

Use disposable emulators for API 35 instrumentation, API 26 minified/populated-library smoke, target-API 36 lifecycle/UI checks, accessibility/offline/error states, and exact-signed clean-install/in-place-upgrade validation.

Preserve the original acceptance matrix:

| ID | Required result |
|---|---|
| A01 | Qualified EQ/gain encoding preserved |
| A02 | Correct framing and typed filter values |
| A03 | Safe malformed/unknown reply handling |
| A04 | Correct Save ordering, timing, and counts |
| A05 | No microphone writes from native Save |
| A06 | EQ and companion configuration preserved |
| A07 | Non-flat-to-flat mismatch detected |
| A08 | No replay after failure/reconnect |
| A09 | Stale/ambiguous sessions rejected |
| A10 | Bounded detach/cancellation/recovery |
| A11 | Existing controls usable without optional features |
| A12 | Debug/release/lint/R8 gates pass |
| A13 | Emulator lifecycle/install/upgrade/UI gates pass |
| A14 | Required persistent state survives actual power loss |
| A15 | Exact source/artifact/signer provenance |
| A16 | Functionality and original configuration restored |

Supplement these with explicit native program/volume persistence and unchanged-staging tests. Separate progress states such as PENDING/NOT RUN from final PASS/FAIL/DEFERRED/NOT APPLICABLE outcomes.

Missing tasks, skips, neutral checks, simulations, or another field’s persistence cannot establish a pass. Mandatory failures cannot be deferred or hidden behind non-applicability.

### Final physical gate only

Do not initiate earlier wireless pairing or JA11 tests.

Freeze APK hash, source, plan/seal, version/code, signer, build/dependency identity, completed off-device evidence, and one complete physical checklist.

Confirm Pixel 9/Android, exact JA11/firmware, USB host connection/adapter, actual current baseline, and a safe restoration procedure. Do not invent restoration of the historical lost bank.

If the intended phone is already authorized and connected, use it. Otherwise apply the [wireless ADB recovery skill](/Users/stephenweeks/.agents/skills/android-wireless-adb-recovery/SKILL.md): inspect executable/version, physical target, authorization, mDNS, current endpoint, route, and TCP reachability. Respect pairing/connect/disruptive-action boundaries. Never request pairing codes in chat or automatically reset ADB/keys/network state.

The final checklist retains the upload’s complete sequence:

1. Connect JA11 and grant USB permission.
2. Read relevant configuration.
3. Capture the known-good baseline.
4. Apply the approved non-flat User 1 test configuration.
5. Execute the qualified persistence operation once.
6. Verify outcome and fresh readback.
7. Physically remove JA11 power.
8. Confirm detach and old-session invalidation.
9. Reconnect.
10. Establish permission and a fresh session.
11. Read authoritatively before mutation.
12. Compare each persisted field separately.
13. Verify no replay.
14. Force-stop/relaunch and prove fresh device-derived state.
15. Verify microphone preservation.
16. Run safe Flash/editor/Reset regressions.
17. Qualify newly enabled filters.
18. Restore original configuration.
19. Verify restoration authoritatively.

Add the native settings-save cases to this consolidated session, including preservation of non-flat User 1 data while saving selected program/volume. Exhaust program-transition, boundary, and failure matrices off-device rather than creating unnecessary physical loops.

Stop mutations immediately on ambiguity, contradictory readback, uncertain outcome, stale session, unexpected microphone change, unsupported/unsafe response, or unreliable restoration. No automatic Save/Flash retry or hidden fallback. A failed session requires diagnosis; a changed APK requires new identity and affected requalification.

## 5. Approval, release, and exact finish conditions

Use production package `com.weekssa.opraeqforuapp`, versionName `0.9.0-beta.1`, and versionCode `15`, subject to checking intervening releases.

Necessary release-tooling changes are limited to candidate-versus-stable metadata validation, candidate-only main signing for the beta version, and immutable exact-artifact prerelease promotion. Preserve signing protections and historical publisher behavior. Signing must not accidentally distribute a candidate.

Sequence:

1. Approve this plan.
2. Complete preparation, source reconciliation, implementation, automated checks, and independent review.
3. Present the exact PR head for merge approval.
4. After approval, merge normally, verify exact-main gates, and sign through the main-only path.
5. Independently verify the signed artifact and applicable emulator checks.
6. Conduct the owner-confirmed final physical session.
7. Prepare the final go/no-go packet.
8. Obtain separate publication/external-distribution approval.
9. Publish only the qualified bytes with `prerelease=true` and `make_latest=false`.
10. Verify tag/source, assets, checksum, signer, provenance, prerelease status, and unchanged stable latest.

A changed PR head requires renewed merge approval. Beta approval does not authorize stable v0.9.0.

Maintain the append-only hardware ledger, capability matrix, protocol/status/runbook documentation, README, changelog, draft PR, release notes, checksum, upgrade/rollback guidance, limitations, attribution, and concise execution report. Preserve every original report field and evidence limit. Keep private raw evidence and build artifacts outside product source.

Record later evidence/documentation commits separately from the qualified candidate. Do not rebuild solely to rename historical evidence or silently substitute a newer artifact.

Before handoff, perform one final reconciliation:

- Zero unmapped source obligations or chat amendments.
- All five filter investigations completed and individually dispositioned.
- Native replacement outcome explicitly distinguished from fallback delivery.
- Every mandatory gate has current evidence.
- Exact restoration confirmed.
- Scope and diff reviewed.
- Every outlier listed without unauthorized implementation.
- Exact candidate and remaining approval boundaries stated.

**READY FOR BETA:** mandatory application, persistence, safety, provenance, and restoration requirements pass; any optional implementation deferrals are evidence-backed and visible.

**NOT READY:** a mandatory requirement fails, mandatory evidence is insufficient, or safe qualification/restoration cannot be completed.

Once those conditions are decided and the authorized release/handoff work is complete, stop. Review outliers afterward as possible separate work; do not turn them into another development cycle inside v0.9.0-beta.1.


## Execution binding (approved facts; no additional product scope)

- Repository: weekssa/OPRA-EQ-for-UAPP
- Branch: codex/v0.9.0-ja11-persistence
- Source baseline: `35f0c90a0e55e701d4343ed978a440d34aae13aa`
- Baseline Git tree: `7d6d88477ef2a45a50b96382eceff6b777093f3c`
- Relevant-input SHA-256: `e4502e4d19202834bea52c5bb610f583d1c9d21edec6f281be9c8207b24aeb72`; sorted baseline file SHA-256/path inventory retained privately.
- App module/ID: :app / com.weekssa.opraeqforuapp. Candidate: 0.9.0-beta.1 / versionCode 15, subject to version-conflict check.
- Toolchain: wrapper Gradle 9.4.1; AGP 9.2.0; Kotlin 2.4.21; KSP 2.3.12; Java 17; minSdk 26; compile/targetSdk 36; Build Tools 36.0.0.
- Pinned reference: Ircama/ja11-config@affddff6c9808c33ce8b35b0b9759ff0d7f6e405. Only configuration target may build; never execute firmware/boot tools.
- Baseline results: 799 JVM tests, 0 failures/errors/skips; lintDebug and assembleDebug PASS. Exact logs/XML hashes retained privately.
- Private evidence: `/Users/stephenweeks/Library/Caches/OPRA-EQ/v0.9.0-beta.1/execution-20261030T231605Z`. Raw data, backup archives, APKs and network/device identifiers are excluded from Git.
- Approval: owner's explicit IMPLEMENT instruction, followed by resume instruction authorizing DeepSeek v1 implementation. No merge/signing/physical session/publication approval inferred.
- One product implementation writer; primary owns contract, synthesis, integration, verification and scope. Bounded read-only specialists may inspect without editing.
- Each previously verified check is rerun only after relevant input change or contradictory evidence.
- Checklist source/approved-plan definitions are binding; mutable result updates and candidate identities belong in REQUIREMENTS.md and EXECUTION_REPORT.md, not a resealed plan.

### Permitted file areas

Permission is conditional on an existing requirement mapping; membership alone never authorizes an unrelated edit.

```text
app/src/main/java/com/weekssa/opraeqforuapp/domain/kt02h20/FiioJa11*.kt
app/src/main/java/com/weekssa/opraeqforuapp/domain/fiio/FiioJa11*.kt
app/src/main/java/com/weekssa/opraeqforuapp/data/dac/FiioJa11*.kt
app/src/main/java/com/weekssa/opraeqforuapp/data/dac/DacSessionRepository.kt
app/src/main/java/com/weekssa/opraeqforuapp/data/kt02h20/**
app/src/main/java/com/weekssa/opraeqforuapp/data/hardware/HardwareEqRepository.kt
app/src/main/java/com/weekssa/opraeqforuapp/ui/EqLibraryViewModel.kt
app/src/main/java/com/weekssa/opraeqforuapp/ui/FiioJa11*.kt
app/src/main/java/com/weekssa/opraeqforuapp/ui/dac/FiioJa11*.kt
app/src/main/java/com/weekssa/opraeqforuapp/ui/screens/FiioJa11*.kt
app/src/main/java/com/weekssa/opraeqforuapp/ui/screens/DacDeviceStatus.kt
app/src/main/java/com/weekssa/opraeqforuapp/ui/screens/DacDeviceControl*.kt
app/src/main/res/values/strings.xml
app/src/test/**
app/src/androidTest/**
app/build.gradle.kts
app/proguard-rules.pro
.github/workflows/github-release.yml
.github/workflows/publish-beta-prerelease.yml
tools/*release*.py
tools/*beta*.py
tools/validate-repository-foundation.py
tools/*v090*
docs/releases/v0.9.0/**
docs/releases/v0.9.0-beta.1.md
docs/ARCHITECTURE.md
docs/CHATGPT_PROJECT_RUNBOOK.md
docs/FIIO_JA11_*.md
docs/PROJECT_STATE.md
docs/PUBLIC_RELEASE_CHECKLIST.md
docs/RELEASE_SIGNING.md
docs/V0.6_MY_DAC_APPROVED_DESIGN.md
CHANGELOG.md
README.md
NOTICE
```

### Discovered commands and parameter bindings

Replace candidate SHA with the exact coherent head and emulator serial with a deliberately created disposable emulator. Use the bundled Python >=3.10 path recorded in the execution report. API26 minified populated-library and API35/36 signed clean/upgrade/lifecycle checks use existing repository smoke helpers; actual flags are recorded before use. No adb physical-device command is authorized at this stage.

```sh
./tools/codex-android :app:tasks --all --console=plain
./tools/codex-android :app:testDebugUnitTest :app:lintDebug :app:lintRelease :app:assembleDebug :app:assembleRelease :app:assembleDebugAndroidTest --console=plain --max-workers=2 -PCANDIDATE_SOURCE_SHA=<candidate-sha>
./tools/codex-android :app:testDebugUnitTest --tests '*FiioJa11*' --tests '*AndroidKt02h20HidSessionTest' --tests '*DacReconnectPolicyTest' --tests '*SupportedDacUsbIdentityTest' --tests '*HardwareEqSnapshotReadersTest'
bash tools/verify-r8-mapping.sh
/Users/stephenweeks/.cache/codex-runtimes/codex-primary-runtime/dependencies/python/bin/python3 tools/validate-repository-foundation.py
/Users/stephenweeks/.cache/codex-runtimes/codex-primary-runtime/dependencies/python/bin/python3 -m unittest discover -s tools -p 'test_*.py' -v
/Users/stephenweeks/.cache/codex-runtimes/codex-primary-runtime/dependencies/python/bin/python3 tools/promote_release_candidate.py check-contract
/Users/stephenweeks/.cache/codex-runtimes/codex-primary-runtime/dependencies/python/bin/python3 tools/publish_beta_prerelease.py check-contract --project-root .
/Users/stephenweeks/.cache/codex-runtimes/codex-primary-runtime/dependencies/python/bin/python3 tools/catalog_pipeline.py validate-registry --registry config/source_registry.json
/Users/stephenweeks/.cache/codex-runtimes/codex-primary-runtime/dependencies/python/bin/python3 tools/verify_favorite_source_samples.py --check
ANDROID_SERIAL=<disposable-emulator> ./tools/codex-android :app:connectedDebugAndroidTest
make ja11-config-tui
```

## Original uploaded specification — verbatim source appendix

# OPRA Android v0.9.0 — JA11 Configuration & Persistence

**Project status:** Proposed — awaiting initial plan approval  
**Target release:** v0.9.0-beta.1  
**Starting public version:** v0.8.3, subject to fresh repository verification  
**Primary objective:** Deliver a stable Android beta with verified JA11 configuration behavior, safe persistence handling, and no regressions to qualified functionality.

## 1. Definition of Success

Deliver a complete, installable Android beta that:

- Preserves all established JA11 controls and existing user functionality.
- Correctly saves and reads back supported five-band User 1 EQ configuration.
- Detects persistence failures rather than reporting false success.
- Preserves unrelated configuration, particularly microphone state.
- Supports additional JA11 digital-filter controls only when the protocol and device capability are independently established.
- Correctly handles Android USB attachment, detachment, permission changes, session replacement, and application lifecycle events.
- Passes the complete applicable computer-based test suite autonomously.
- Passes final Pixel 9 and JA11 hardware qualification, including physical unplug/replug persistence testing.
- Produces a traceable beta release candidate tied to an exact source commit, build, signer, and test record.

**The target is a working Android application, not an open-ended investigation.**

A beta may include persistence hardening without a new native Save implementation if the existing qualified mechanism is demonstrably safer.

Unsupported optional features are deferred rather than approximated.

## 2. Fixed Scope

### 2.1 Required functionality

Preserve and regression-test existing qualified functionality:

- Five-band parametric equalizer.
- Global gain.
- Device volume.
- EQ program selection.
- Microphone and headset configuration.
- UAC mode.
- Available sample-rate and firmware readback.
- User 1 Flash behavior.
- EQ editor and Reset interactions.
- Explicit Set as Default microphone round-trip, unless a replacement is independently qualified.

Investigate and address the previously recorded non-flat User 1 to flat readback mismatch.

The application must not report successful persistence when a fresh authoritative readback contradicts the saved configuration.

### 2.2 Conditional enhancement: native persistence

Investigate the JA11 native Save transaction, including:

- Required parameter staging.
- Apply and Save ordering.
- Save payload values.
- Timing requirements.
- Exact command counts.
- Device-family compatibility.
- Flash persistence following power removal.
- Interactions with existing EQ, microphone, and companion state.
- Failure, cancellation, and unexpected-detachment behavior.

The pinned reference is:

https://github.com/Ircama/ja11-config/tree/affddff6c9808c33ce8b35b0b9759ff0d7f6e405

The reference stages five EQ bands, issues Apply, waits approximately 50 ms, and then sends command `0x19` with payload `3`.

**These values are research evidence, not authorization to assume they are universally correct for OPRA's JA11 hardware.**

The implementation must corroborate actual command semantics and timing before enabling a native Save path.

If native Save cannot be qualified, retain the established behavior and record the limitation.

Do not automatically retry an uncertain Flash mutation.

### 2.3 Conditional enhancement: digital filters

Investigate support for:

- FAST-LL
- FAST-PC
- Slow-LL
- Slow-PC
- NON-OS

Add Android controls only when command mappings, valid values, responses, and supported-device applicability are independently established.

Use typed JA11-specific control values and existing application architecture.

Unsupported or unverified filter values must not appear as functioning controls.

Do not reinterpret a value from another device family as proven JA11 behavior.

### 2.4 Explicit exclusions

The release must not expand into:

- Firmware flashing or firmware updates.
- Bootloader operations.
- Speculative amplifier controls.
- Unrelated device-family protocols.
- Whole-app architecture replacement.
- General UI redesign.
- Opportunistic feature development.
- Repository-wide refactoring or cleanup.
- New hardware support unrelated to the qualified JA11.
- Broad dependency upgrades unrelated to building or validating v0.9.0.

Existing unrelated issues may be recorded separately but must not expand the release scope unless they directly prevent building, testing, or safely using the approved functionality.

## 3. Android Engineering Requirements

Follow Android's official platform guidance and the established conventions of the existing application.

### Architecture and state

- Preserve the current application architecture unless a narrowly justified change is necessary.
- Maintain a single authoritative connection/session state.
- Serialize device mutations to prevent overlapping USB commands.
- Separate device-reported configuration from cached or locally edited values.
- Represent loading, current, stale, unsupported, and failed states truthfully.
- Reject commands belonging to stale sessions.
- Prevent reconnect handling from silently replaying writes.
- Ensure cancellation and detach invalidate operations safely.
- Keep hardware transport logic testable independently from UI components.

A locally cached value is not proof of device persistence.

### USB and lifecycle correctness

Account for:

- Android USB host discovery and permissions.
- Permission expiration or invalidation after detachment.
- Rapid attach/detach sequences.
- Device disappearance during a command.
- Multiple or ambiguous compatible devices.
- Application backgrounding and foregrounding.
- Activity recreation and process termination.
- Reconnection with a fresh device session.

Only a verified current session may authorize device mutation.

Reconnection should default to read-only synchronization, not automatic reapplication of stored configuration.

### UI and accessibility

Reuse existing My DAC DEVICE rows and established application patterns.

New UI must:

- Clearly distinguish supported and unsupported features.
- Show accurate operation results.
- Avoid displaying success before device confirmation.
- Avoid blocking existing controls when an optional feature is unsupported.
- Follow existing Material, accessibility, and navigation conventions.
- Remain usable with screen readers, appropriate touch targets, and applicable display scaling.

No additional screen architecture or visual redesign is authorized merely to implement a new control.

### Android security and build requirements

- Preserve compatible Android manifest and USB intent handling.
- Review PendingIntent mutability and receiver registration rules where applicable.
- Request no unnecessary permissions.
- Avoid logging sensitive data or retaining unnecessary device identifiers.
- Validate declared SDK compatibility and release-toolchain configuration.
- Preserve release signing and upgrade compatibility.
- Validate R8/minification when enabled.
- Keep any dependency changes minimal, justified, and recorded.

## 4. Project Seal and Scope Control

After initial plan approval, create one canonical release plan:

`docs/releases/v0.9.0/PLAN.md`

Create one generated seal manifest:

`docs/releases/v0.9.0/SEAL.sha256`

The seal must identify:

1. Approved plan content and SHA-256.
2. Exact source baseline commit.
3. Relevant source-tree digest.
4. Gradle wrapper and Android build configuration.
5. Application ID, module names, and version configuration.
6. Approved functionality and exclusions.
7. Permitted source and test file areas.
8. Exact discovered build/test commands.
9. Referenced protocol evidence revision.
10. Required acceptance tests and final hardware qualification.
11. Artifact and evidence storage location.

The source baseline and test commands must be determined from the actual checkout, not assumed from this document.

**Once sealed, no feature expansion is permitted during execution.**

New discoveries are categorized as:

- **Required correction:** Necessary to satisfy the approved scope or prevent a regression.
- **Optional supported enhancement:** May proceed only if already explicitly included as conditional scope.
- **Deferred issue:** Recorded for future work without changing v0.9.0.
- **True blocker:** Meets the escalation criteria in Section 7.

A required safety correction can be made within the approved scope. A material scope expansion requires a separately approved amendment and new seal.

Ordinary implementation corrections and added regression tests do not require resealing.

## 5. Baseline Preparation

Begin only after the plan is approved.

### Repository safety

- Verify the current source branch and repository ancestry.
- Refresh remote references.
- Confirm whether development can safely fast-forward to the selected baseline.
- Preserve pre-existing untracked files through a verified reversible archive outside the repository.
- Record original paths, file integrity hashes, and necessary metadata.
- Do not force-reset, clean, or overwrite existing work.
- Preserve unrelated branches and worktrees.
- Leave archived files restorable.

Select one exact, clean, reproducible baseline.

If branch ancestry prevents a safe fast-forward, attempt a bounded non-destructive resolution. Escalate only if no safe baseline can be selected without changing or discarding protected work.

### Initial inventory

Determine the actual:

- App module and application ID.
- Gradle and Android Gradle Plugin configuration.
- Kotlin/JVM and SDK targets.
- Existing USB transport and command serialization.
- JA11 protocol and persistence implementations.
- Current session/repository behavior.
- Test infrastructure.
- Emulator and local Android SDK availability.
- Release/minification/signing configuration.

Execute baseline compilation and relevant existing tests before making product changes.

Record pre-existing failures separately from new regressions.

Inspect the reference GitHub implementation under the granted research and code-use authorization, while respecting its actual license and attribution requirements.

## 6. Autonomous Development Sequence

All stages below are computer-based and should proceed without routine human intervention.

### Stage A — Evidence and protocol specification

Compare:

1. The current qualified User 1 Flash flow.
2. The existing Set as Default microphone round-trip.
3. The preserved User 1 readback failure.
4. The pinned JA11 reference implementation.
5. Existing local protocol captures or known-good tests.

Produce a bounded protocol behavior specification.

Do not implement a speculative command merely because a source repository contains it.

### Stage B — Protocol and persistence implementation

Implement only established behavior.

Requirements include:

- Correct command framing and typed values.
- Explicit operation sequencing.
- Safe serialization.
- Fresh readback validation.
- No unexpected microphone writes in a proposed native Save path.
- Preservation of existing EQ encoding.
- Explicit failure results.
- No mutation replay following ambiguous outcomes.

Keep the previous qualified path available when a native replacement has not passed qualification.

### Stage C — Session and Android integration

Connect verified functionality through the existing device session, repository, and UI architecture.

Ensure that unsupported optional controls are disabled or absent without affecting established functionality.

Do not add architectural layers or dependencies without demonstrating that the existing structure cannot satisfy a specific approved requirement.

### Stage D — Automated verification

Run the applicable available verification suite:

- Protocol JVM unit tests.
- Persistence transaction tests.
- Session lifecycle and transport tests.
- Existing regression tests.
- Full JVM tests.
- Android lint.
- Debug build.
- Release build.
- R8/minification validation.
- Repository-defined checks.
- Android emulator instrumentation.
- Available UI and accessibility tests.
- Emulator install, upgrade, and application-lifecycle tests.

Discover and record the real Gradle task names before sealing.

Do not declare a test passed because its task was missing or skipped.

Tests requiring physical JA11 communication must be explicitly separated from emulator tests.

### Stage E — Automated failure resolution

For any failed build or test:

1. Capture the exact failure and relevant evidence.
2. Identify whether it is baseline, environment, test, or implementation-related.
3. Apply a bounded correction within approved scope.
4. Rerun the affected tests.
5. Rerun the full applicable verification suite after the correction.
6. Stop after the defined remediation budget is exhausted.

Limit repeated attempts on an unchanged failure. Do not continue looping indefinitely.

Ordinary build failures, emulator startup issues, missing local SDK components, and deterministic code defects should first be handled autonomously.

## 7. Blocker and Escalation Rules

Human intervention is permitted only for a genuine blocking condition.

### True blockers

Examples include:

- Required repository credentials or inaccessible private resources.
- A protected source state that cannot safely be reconciled.
- A missing essential hardware capability that prevents mandatory qualification.
- An operation requiring physical interaction with the Pixel 9 or JA11.
- An unsafe or ambiguous hardware mutation whose outcome cannot be established.
- Failure of a mandatory release invariant after bounded remediation.
- A material conflict between the approved product requirements and the available verified protocol behavior.

### Not automatically blockers

Examples include:

- A test initially failing.
- A dependency needing repair.
- A recoverable Gradle configuration issue.
- An emulator failing to boot once.
- A test fixture requiring adjustment.
- An optional unsupported filter.
- An unrelated pre-existing defect.
- A possible future improvement.
- A code-style preference that does not violate a required standard.

These must be fixed autonomously where necessary or recorded and deferred.

An escalation must include the exact problem, evidence, attempted bounded corrections, its impact on required acceptance, and the minimum owner action needed.

Do not request decisions about routine engineering choices already covered by the approved plan.

## 8. Acceptance Matrix

| ID | Acceptance requirement | Primary verification |
|---|---|---|
| A01 | Qualified EQ and gain encoding unchanged | JVM regression tests |
| A02 | Correct command framing and typed filter values | Protocol tests |
| A03 | Malformed and unknown replies handled safely | JVM negative tests |
| A04 | Save ordering, timing policy, and command counts correct | Deterministic transport tests |
| A05 | No microphone writes from proposed native Save | Command-trace assertions |
| A06 | No loss of established EQ or companion configuration | Before/after state comparisons |
| A07 | Non-flat User 1 to flat mismatch is detected, not concealed | Regression tests |
| A08 | No mutation replay following failed readback or reconnect | Lifecycle/transport tests |
| A09 | Stale sessions and ambiguous replacements rejected | Session tests |
| A10 | Detach, cancellation, and recovery remain bounded and safe | JVM and emulator tests |
| A11 | Existing controls function when optional features are unsupported | UI and regression tests |
| A12 | Debug, release, lint, and applicable R8 checks pass | Gradle verification |
| A13 | Emulator lifecycle, installation, upgrade, and UI checks pass | Android instrumentation |
| A14 | Persistent device state survives physical power removal | Final JA11 hardware test |
| A15 | Exact candidate and signing identity are traceable | Build/artifact manifest |
| A16 | Existing functionality is restored after hardware testing | Final device readback |

Each result must be recorded as **PASS, FAIL, DEFERRED, or NOT APPLICABLE**, with evidence.

A deferred optional enhancement may be excluded from the release. A failed mandatory requirement cannot be marked successful or bypassed.

Volume, program selection, microphone configuration, and User 1 EQ persistence must be tracked as separate claims.

No persistence claim is accepted merely because another configuration field persisted.

## 9. Final Hardware Gate — Pixel 9 + JA11

This is the only mandatory physical-device qualification session.

**Do not initiate wireless ADB pairing or physical JA11 testing during earlier development stages.**

The final hardware gate begins only after applicable computer-based checks pass.

### Before the session

Freeze the exact candidate:

- APK hash.
- Source commit.
- Plan/seal identity.
- App version and versionCode.
- APK signing certificate identity.
- Relevant dependency/build configuration.
- Completed off-device verification evidence.

Prepare one complete physical test checklist in advance.

No candidate changes are allowed during testing without invalidating the affected qualification results.

### Hardware preparation

1. Confirm Pixel 9, Android version, JA11 device identity, and relevant firmware details.
2. Confirm the Android USB host connection method and required adapter if applicable.
3. Configure wireless ADB only at this final stage.
4. Confirm that wireless ADB permits diagnostics while the JA11 occupies the required USB connection.
5. Establish the application baseline and record the existing device configuration.

If wireless ADB is unavailable, investigate pairing/network/tooling conditions without modifying the app's product scope.

Physical testing can proceed without wireless log collection only when the mandatory evidence can still be obtained reliably.

### Persistence qualification

Perform the following controlled sequence:

1. Connect the JA11 and grant the required Android USB permission.
2. Read back all available relevant device configuration.
3. Capture the known-good starting state.
4. Apply the approved non-flat User 1 configuration.
5. Execute the qualified persistence operation once.
6. Confirm transaction outcome and fresh readback.
7. Physically unplug the JA11 from the Pixel 9, removing device power.
8. Verify that Android and the app recognize detachment and invalidate the old session.
9. Reconnect the JA11.
10. Re-establish the required USB permission and a new device session.
11. Perform an authoritative read-only configuration retrieval before any mutation.
12. Compare each supported persisted field against its expected value.
13. Confirm no automatic configuration replay occurred.
14. Force-stop and relaunch the application, reconnect as necessary, and verify that displayed values come from the fresh device state rather than a stale local cache.
15. Verify microphone preservation.
16. Run the established EQ Flash/editor and Reset regressions under the approved safe test procedure.
17. Verify any newly enabled digital-filter controls and readback where supported.
18. Restore the original user configuration.
19. Perform a final authoritative readback confirming restoration.

At least one genuine JA11 power-loss cycle is mandatory.

### Hardware stop conditions

Stop mutation testing immediately when:

- Device identity is ambiguous.
- Readback contradicts expected persistence.
- A prior mutation has an uncertain outcome.
- The connection or session is stale.
- Microphone state unexpectedly changes.
- The device reports an unsupported command or unsafe state.
- Reliable restoration cannot be established.

Do not automatically retry Save or Flash after an uncertain response.

A failed hardware test produces a bounded defect report and targeted correction. Any changed APK must be rebuilt and retested under a new candidate identity before release qualification is claimed.

## 10. Release Sequence and Gates

### Gate 2 — Plan approval

**Human action required.**

Approve the bounded requirements and release target.

After approval, baseline selection, sealing, development, test execution, and ordinary corrections proceed autonomously.

### Automated development and verification

No routine human approval required.

Produce an off-device-verified release candidate. All applicable automated tests must be completed and recorded.

### Gate 4 — Final physical-device qualification

**Human participation required only for necessary device actions and hardware conditions.**

Perform wireless ADB setup, JA11 connection, unplug/replug, state verification, and restoration.

No earlier physical-device testing is required for ordinary development work.

### Gate 5 — Release authorization

Following successful hardware qualification, prepare the final beta release packet.

Publishing, merging, and external distribution remain subject to the owner's release authorization.

The first authorized release is **v0.9.0-beta.1**.

A later stable v0.9.0 release requires successful beta evaluation and confirmation that its release requirements are met. Beta qualification is not automatically equivalent to stable release approval.

## 11. Evidence and Completion Report

Maintain one concise execution report containing:

- Sealed plan identity.
- Exact baseline commit.
- Changed files and scope compliance.
- Implemented features.
- Deferred optional features.
- Protocol evidence and decisions.
- Automated test results.
- Emulator results.
- Hardware qualification results.
- Known release-relevant limitations.
- Candidate APK hash and signer.
- Restoration confirmation.
- Final beta readiness disposition.

Keep logs and generated artifacts outside product source where practical.

Do not accumulate unnecessary repeated evidence or expansive investigation reports.

### Completion states

**READY FOR BETA:** All mandatory checks and required hardware persistence tests pass, with no unresolved blocking defects.

**NOT READY:** A mandatory requirement fails, evidence is insufficient for a mandatory behavior, or safe hardware qualification cannot be completed.

**OPTIONAL FEATURE DEFERRED:** The application remains qualified, but an independently scoped enhancement lacks sufficient supporting evidence.

No unsupported feature is represented as implemented or verified.

## 12. Final Operating Rules

- Deliver the approved Android v0.9.0 scope.
- Preserve established working behavior.
- Prefer existing code and Android patterns.
- Make corrections without expanding requirements.
- Automate all available computer-based development and testing.
- Keep physical testing and wireless ADB at the end.
- Never interpret speculative protocol evidence as a confirmed feature.
- Never silently replay uncertain hardware mutations.
- Never confuse emulator verification with physical persistence qualification.
- Never publish an artifact that differs from the qualified candidate.
- Stop when mandatory acceptance is met, rather than continuing to discover unrelated work.

**Final intended outcome:** A bounded, reproducible, independently testable Android v0.9.0-beta.1 release with verified JA11 persistence behavior, preserved existing features, and a clear path from beta qualification to stable release.