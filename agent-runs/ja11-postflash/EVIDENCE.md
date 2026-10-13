# JA11 post-Flash evidence

## Mission authority and acceptance gates — 2026-10-13

Owner explicitly adopts RUNBOOK.md. Only Sol owns canonical conclusions and production edits. Pixel and JA11 access forbidden until exact owner unlock phrase; no device listing, discovery, pairing, connection, query, install, or physical mutation. No public tag or release. Main-only signing and explicit merge authorization remain intact; signing readiness must not conceal a merge gate.

Required gates: Phase 0 capability audit; >=95% root-cause confidence; production-path regression fails before and passes after fix; healthy first read; bounded failure; session replacement/cancellation/late-result rejection; no mutation replay; process-start parity; local focused/full JVM/lint/assembly; fresh Luna High adversarial APPROVE; exact-source hosted build/API26/API35/UI/CodeQL/catalog gates; safely signed exact candidate with independent provenance. Physical acceptance remains NOT RUN overnight.

## Repository capability digest

- Applicable instructions: root AGENTS.md and docs/CHATGPT_PROJECT_RUNBOOK.md; no parent or nested AGENTS.md found on applicable path. Owner mission overrides older physical procedures. Worktree initially clean/detached at current main; origin is https://github.com/weekssa/OPRA-EQ-for-UAPP.git. No .codex specialist configuration or repository-local DAC skills present in this checkout (do not invent availability).
- Phase 0 inspected root/runbook, Markdown inventory, architecture/session portions, approved My DAC state flow, v0.8.1 Constitution/State/Gate Matrix, capability matrix and append-only validation ledger, protocol/transaction lifecycle sections, release signing, build files and test inventory. Historical release snapshots are not live state.
- Skills: installed android-dev baseline, android-testing, kotlin-coroutines, kotlin-flows; Unlazy completion discipline. Owner's four-file persistence limit takes precedence over separate Unlazy report trees; gates recorded here. Project Android CLI/testing/security/adaptive skills available; no installation needed.
- Build: tools/codex-android selects JDK17 and local SDK API36/build-tools36, invokes checked-in Gradle9.4.1 wrapper. Single :app module, AGP9.2.0, minSDK26, target36; manual DI; ViewModel-scoped hardware repository.
- Tests: :app:testDebugUnitTest with --tests for focused classes; :app:lintDebug; :app:assembleDebug; release assembly plus tools/verify-r8-mapping.sh. Robolectric4.17/resources enabled. Existing JA11 protocol, flasher, DEVICE repository, USB-session, snapshot/domain/UI tests; extracted FiioJa11FlashCompletionTest. Instrumentation/Compose via connectedDebugAndroidTest hosted on disposable emulator only.
- Emulator helpers: android-ci-exact-head.yml creates explicit temporary AVD paths, checks boot/process/API, API35 connected UI tests and API26 signed-release smoke. tools/verify_stable_release_smoke.py provides persisted-state/install/upgrade smoke. Local generic ADB enumeration forbidden; use hosted emulators to avoid phone interaction.
- CI: Android CI Exact Head workflow_dispatch accepts source_sha; every real job checks actual checkout equals requested SHA. Three jobs: build(full JVM/lint/debug/release/R8/contracts), emulator-ui-test(API35), min-api-smoke(API26). Existing PR CodeQL, catalog currentness, priority community and dependency workflows. Current normal Android CI reports zero-job failure; exact-head fallback exists specifically for this.
- GitHub CLI: /Users/stephenweeks/.local/bin/gh-v2.102.0. Fresh remote refs/PR88/89/release/runs queried. Actions pins must remain exact. Repository Python catalog/community validation helpers already exist.
- Signing: github-release.yml candidate-only/main-only; signed-beta.yml main-only with hosted API35 smoke and separate mobile-test-apk writer. No alternate signing path or feature-ref signing. Pinned certificate 65c1c1256dae3c49e3548f334c91f0ba991969e9be9e0b223ba4e253d2114747. Never dispatch promote-signed-release.yml or publisher overnight.
- Conventions: immutable canonical EQ, one authoritative session, generation-fenced read adoption, one Flash/Apply/Save sequence; no mutation replay. Production branch separately from orchestration. Preserve private raw hardware captures outside repository.

## Fresh historical baseline

- v0.8.0 annotated tag peeled source: 54823e1a464f8b716a32c4f4808037fc0cdd99bd.
- v0.8.1 annotated tag peeled source: d0b6fe143957012c138613d153896923d6d76369; published 2026-10-13T00:18:55Z. Latest public release retained.
- PR88 source ab920db3d5792d835b389714ba72c6ec4a1b8a70; merge a24cc5a9458677ddc1ea3ea43b52b0a8341aac95. Changed HardwareEqRepository.kt and added extracted completion test.
- PR89 source d6519f878a0dc0aafa3713a0ed178bb351f6b909; final head b9a0dc1b12687f0ba39a32895a6b9b277d061ba4; merge/current main 4b2d3e67a00f1c7545e27165135c56fc901206b9. Same repository/test files only.
- Current-main signing runs 38352091152 and 38352849322 succeeded; they are failed physical candidates, not proof of this mission's fix. Current-main CodeQL 38352080663 succeeded; Android CI 38352079683 failed before real jobs. Historical fallback 38350870911 succeeded on f9e5a6f0, not this candidate.

## Authoritative physical premise (owner runbook; no new device access)

Same MainActivity process alive after one successful Flash; replacement session current; DEVICE snapshot current; My EQ blank; EQ bundle absent/not current; first acquisition yielded no accepted bundle; zero retry attempts. Lower-level reason not captured. Process restart repairs state. PR89 signed candidate SHA256 b617f79b7980ae45083f5f2d24f769d50da6f57f6d6ba03c668187670fb4b644 failed. This is ground truth, not newly captured physical evidence.

## Phase 1/2 reconstruction and specialist reconciliation

Two bounded read-only agents used gpt-6-luna (Luna 6.0): historical_mapper/Medium, state_machine/High. They received only the shared digest/premise and target files. No agent wrote production code, ran a build, or touched devices. High received one narrow follow-up on primary probe results; no swarm, CI agent, or premature adversarial review.

Normal startup: session Connected -> repository connection collector refreshes EQ once under shared mutex -> readVerifiedSnapshot captures generation -> FiioJa11SnapshotReader -> repository publishes bundle -> ViewModel combines EQ state -> My DAC EQ displays filters/editor. Independently, ViewModel connection collector reads DEVICE -> current DEVICE state.

Post-Flash: Compose caller -> repository marks EQ stale -> exclusive JA11 flasher -> one qualified mutation/Save/replacement/final verification -> mutex released -> connection collector may acquire EQ using one-attempt policy; caller completion separately acquires EQ using two-attempt policy -> completion returns original mutation result regardless of accepted bundle -> ViewModel may refresh DEVICE independently.

Exact source references at investigated main:

- HardwareEqRepository.kt:104-110: collectLatest Connected observer calls refresh without retry flag. :232-243: Flash calls completeFiioJa11Flash and enables retry only in its callback. :377-404: actual refresh, shared mutex and publication. :478-487: refresh return type Unit; finally awaits callback but does not verify publication. :494-532: helper has currentness guards, one/two attempts, and terminal failure.
- DacSessionRepository.kt:154-155: shared FIIO mutex; :172-175: current generation predicate; :188-192 and :255-266: snapshot read is accepted only when connected/generation matches before and after.
- HardwareEqSnapshotReaders.kt:54-79: nullable active program, band, gain or factory result; built-in programs intentionally return no coefficient bundle. None identifies the cause of this physical null.
- HardwareEqSnapshotState.kt:4-33: missing bundle, isReading=false/readFailed=true is valid terminal state. No automatic reacquisition follows that state alone.
- EqLibraryViewModel.kt:335-338 and :401-419: repo EQ flow is UI backing state; :448-456, :579-594: independent DEVICE collector/read. DEVICE success cannot prove EQ publication.
- ProfileSelectionEditor.kt:139, :458-460: Flash initiator uses Compose rememberCoroutineScope. EqLibraryApp.kt:449-463 rethrows cancellation. A live process does not prove that initiating coroutine survives. Injected cancellation demonstrates vulnerability, not physical causation.
- FiioJa11MyDacContent.kt:350-407: independently current DEVICE User 1 with missing bundle shows unavailable coefficients; editor requires current bundle. UI includes unavailable wording, so a probe must not claim the source literally renders an empty layout.

History correction: git log/diff found no HardwareEqRepository, DacSessionRepository or HardwareEqSnapshotReaders change between v0.8.0 and v0.8.1. The one-shot EQ observer and separate DEVICE ownership predate v0.8.1 (HardwareEqRepository predecessor dc6478a2). v0.8.1 makes Save/replacement handling stricter and orders User 1 before writes; these are accepted safety behavior and are not blamed/reverted. PR88 adds synchronous caller completion but retains one-shot observer and ignores accepted-bundle postcondition. PR89 adds two-attempt policy only to that caller path. Thus neither establishes a universal convergence invariant. This establishes weakness, not which historical change caused the physical occurrence.

## Retained capture verification (off-device file reads only)

Original private capture: /private/tmp/opra-live-failure-20261013-010130. Captured PID is 5485. UI shows FiiO JA11 and Connected · State current. There are 32 app-PID lines in retained logcat; none records EQ acquisition owner, helper attempt, state publication, guard exit, or coroutine cancellation. Activity dump timed out and supplies no such internal timeline. Do not equate keyboard ImeTracker onCancelled messages with coroutine cancellation. Owner runbook's internal-state premise remains accepted, but this raw capture cannot independently prove its acquisition route/retry attribution. No new phone query, process inspection, or screenshot was issued.

Capture logcat SHA256: 38233ec70199c6de5b0bfd220d6e470f3d19e5cbe5515620ec3ddcd4eacc53a2. UI XML SHA256: 2b9fb1e595463005d4f3f27925f324c98b7635053ddb6d68eaed0ee63583c30a. Raw captures remain private/outside Git. Retained candidate /private/tmp/opra-physical-candidate.V8eqWX/candidate-manifest.json records source4b2d3e67, package com.weekssa.opraeqforuapp, 0.8.1/code12 and pinned signer. Recomputed APK SHA256 matches owner's failed-candidate b617f79b7980ae45083f5f2d24f769d50da6f57f6d6ba03c668187670fb4b644. This verifies retained bytes, not new physical execution.

## Discriminating automated experiments

Added only FiioJa11ConvergenceContractProbeTest.kt on orchestration branch; production source untouched. Four characterization probes PASS:

1. Observer policy/current generation/normal null read returns normally with no bundle, readFailed=true, exactly one read, zero retries. This is a sufficient source route for the observed zero-retry state, but physical invocation is not proven.
2. Post-Flash retry-enabled policy/current generation/normal null read necessarily executes two reads. The physical zero-retry premise therefore cannot be attributed to that exact completed path without another unobserved distinction.
3. Both retrying reads return null; completion still returns successful mutation result, no accepted bundle, exactly one mutation. Waiting for refresh completion is demonstrably weaker than requiring accepted EQ state.
4. Injected caller cancellation during completion boundary prevents acquisition. Shows lifecycle vulnerability; cancellation remains unproven physically.

These are framework-independent contract characterizations. They do NOT drive the full production Android repository/session/ViewModel lifetime or select the actual physical route. REGRESSION_REPRODUCED remains NO; no claim of a faithful red-before/green-after physical regression. No exception is substituted for the normal-null premise.

## Local validation and recovered infrastructure

Initial tools/codex-android focused run failed in :app:kspDebugKotlin because a shared Gradle transform output.bin was missing. No source defect inferred. Recovered autonomously by creating /private/tmp/opra-ja11-reconstruction-gradle-home, cloning only dependency/wrapper caches from prior retry cache, and rebuilding transforms in isolated cache. No unrelated cache was deleted.

All successful commands below use prefix GRADLE_USER_HOME=/private/tmp/opra-ja11-reconstruction-gradle-home ./tools/codex-android:

- :app:testDebugUnitTest --tests '*FiioJa11FlashCompletionTest' --tests '*FiioJa11ControlRepositoryTest' --tests '*HardwareEqSnapshotReadersTest' --tests '*FiioJa11FlasherTest' --tests '*AndroidKt02h20HidSessionTest' --console=plain: PASS, 49 tests, zero failures/errors/skips. Logs tmp/baseline-focused-isolated.log.
- :app:testDebugUnitTest --tests '*FiioJa11ConvergenceContractProbeTest' --console=plain: PASS, 4 probes. Logs tmp/contract-probes.log.
- :app:testDebugUnitTest :app:lintDebug :app:assembleDebug --console=plain: PASS, 776 JVM tests, zero failures/errors/skips; lint zero errors,118 warnings,2 hints; debug assembled. Logs tmp/full-validation.log. Debug APK is local/unqualified and is not a signed owner candidate.
- git diff --check: PASS.

## Root-cause gate disposition and canonical decision

Sol conclusion: physical root cause confidence NOT ESTABLISHED (<95% required threshold). Split refresh policies and missing accepted-publication postcondition are proven, but are not sufficient to select the actual zero-retry cause. No statistical probability is inferred from green tests.

Root gate answers: exact wrong state and My EQ owner are known; expected publication point and independent DEVICE path known; startup reconstruction explains a new opportunity to read. Exact failing post-Flash route, triggering historical regression, why the physical PR88/PR89 executions failed, and why that particular retry did not execute remain unobserved. Do not replace these unknowns with a cancellation or transport story.

Rejected conclusions: an exception caused first acquisition (not evidenced); transport currentness is broken (not proven); cancellation occurred physically (not proven); enabling a third retry fixes this incident (not proven); adopting flasher target/cached values as current EQ (unsafe substitution); reverting v0.8.0 (does not establish cause and would regress accepted safety).

Runbook §§17/30 requires stopping production edits below95%. Hard stop honored. No production fix branch, production PR, fresh adversarial fix review, hosted final-fix CI, CodeQL for final fix, emulator physical regression, or new signed candidate was produced. Existing current-main hosted CodeQL is historical baseline only. Exact-head fallback is available; its existence is not a pass for an absent final fix. No remaining recoverable tooling issue blocks this investigation; the blocker is missing acquisition-route evidence.

Smallest next diagnostic: correlate one EQ acquisition with Flash operation and session generation; record owner (ConnectedObserver/FlashCompletion), start/end currentness and generation, each read attempt index and null/rejection reason, accepted publication and ViewModel adoption, cancellation at boundary. A diagnostic-only trace must never log serial, fingerprint, raw payload, keys or user EQ values, and must not change protocol, timing, mutex ownership, retries, or mutations. Build/validate off-phone first, freeze source/APK/hash/signer, then owner phone unlock and a separately owner-controlled single Flash. On failure retain same process/state, do not restart/reflash/unplug automatically, capture trace and stop. This is a diagnostic proposal, not a ready candidate or phone-access authorization.

## Final historical-chat evidence check

Read prior chats Find the blocked EQ retry predicate (01a133dc-37d5-72d1-8994-d17e07f2a360) and Audit JA11 My EQ recovery state (01a1337e-992f-75a3-b63a-335c8e6eacd8) as historical evidence only, without messaging/continuing either. The former is interrupted and its uncommitted test substitutes a Unit-returning refresh callback that never invokes the real lower retry helper. The latter claims a state-level reproduction and has local unmerged commit89b51262, but explicitly retains unknown incomplete-read versus generation-rejection causes. Neither supplies a captured acquisition-owner/attempt/cancellation trace for the physical candidate. Do not import those claims as a >=95% causal conclusion or transplant their patches. Source/probe evidence above is authoritative for this audit.

## Final gate and repository verification

Evidence at tested code/analysis commit `7d6658891d6f793372bd4dc525156e3dbe5446ce` includes unchanged production tree plus four probes. This commit was pushed to origin/codex/ja11-postflash-reconstruction; remote ref independently read back equal. The subsequent checkpoint changes documentation only. No production/main/Gradle/workflow diff exists against investigated main. Tmp logs are locally preserved and ignored via exact path in Git info/exclude; no private raw captures staged.

Manual mission ledger (completion is not claimed):

| Gate | Disposition |
|---|---|
| G01 Phase0 capability/instructions audit | MET |
| G02 Fresh exact history/remote baseline | MET |
| G03 Physical causal confidence>=95% | UNMET |
| G04 Faithful physical production regression red/green | UNMET |
| G05 Minimal correct production fix | UNMET |
| G06 Final-fix healthy/first-read/bounded-failure coverage | UNMET |
| G07 Final-fix replacement/cancellation/late-result coverage | UNMET |
| G08 Final-fix no mutation replay proof | UNMET |
| G09 Process-start parity | UNMET |
| G10 Local focused characterization validation | MET |
| G11 Local full JVM validation | MET |
| G12 Local lint | MET |
| G13 Local debug assembly | MET |
| G14 Fresh adversarial final-fix review | UNMET |
| G15 Final-fix exact-head hosted build | UNMET |
| G16 Final-fix minimumAPI smoke | UNMET |
| G17 Final-fix API35/emulatorUI smoke | UNMET |
| G18 Final-fix CodeQL | UNMET |
| G19 Final-fix catalog/community validation | UNMET |
| G20 Exact signed candidate provenance | UNMET |
| G21 Pixel/JA11 untouched | MET |
| G22 No public tag/release | MET |
| G23 Durable mission/handoff state | MET |

Totals:8 MET,15 UNMET,0 abandoned. G10-G13 verify diagnostic source/tests only; they do not qualify an absent fix. Physical acceptance is separately forbidden/pending owner authorization and is not counted as an overnight gate. Root gate G03 is the earliest blocker; dependent gates stay explicit, not removed or rewritten green.

## Owner-present authorization — 2026-10-13

Owner sent exact unlock phrase OWNER PRESENT — AUTHORIZE PHONE ACCESS. Phone access now authorized; overnight no-access record remains true. This does not establish a fix, qualify a candidate, authorize automatic Flash, or permit publication. Initial action is non-mutating device/app/session observation and preservation of any surviving failed process; no install/restart/Flash requested. Android debugging skill applied for current process/package/log/UI/USB evidence gathering.

## Authorized live baseline and lifetime correction — 2026-10-13

Private captures: /private/tmp/opra-ja11-owner-observation-20261013T064626Z. Pixel9 reachable over existing wireless ADB. Package com.weekssa.opraeqforuapp, version0.8.1/code12; process PID5485 matches prior failure capture. Installed APK pulled and independently hashed: b617f79b7980ae45083f5f2d24f769d50da6f57f6d6ba03c668187670fb4b644, exactly the failed PR89 candidate. Host apksigner verifies pinned certificate65c1c1256dae3c49e3548f334c91f0ba991969e9be9e0b223ba4e253d2114747. No new APK was installed.

Initial phone was at launcher. The complete initial activities dump contains no EQ Library Activity, although the process exists. Attempted reorder-to-front returned LaunchState=WARM and opened a new MainActivity task81460. Process stayed5485. Thus this is NOT proof that the original Activity/ViewModel/repository lifetime survived. Initial resumed app showed Device detected, and My DAC showed Disconnected/no EQ bundle. The earlier commentary inference that repository remained alive was too strong and was explicitly corrected. Same PID alone is insufficient lifetime evidence.

One normal Connect action opened a fresh read-only session. Current hardware EQ, User1 five-band graph/filters/global gain and enabled Edit EQ appeared. DEVICE showed Current device state with verified values. PID remained5485. No Flash, Reset, Apply, Save, editor mutation, Mic/UAC/volume/program write, install, force-stop, process restart or unplug action was issued. No automatic physical retry occurred. Wire-level mutation counts and session generations were not exported; do not fabricate them. The capture proves startup/fresh-connect baseline functionality in a newly opened Activity within the surviving process, not original post-Flash convergence or a fix. No hardware restoration obligation was created.

Causal confidence remains below95%. Diagnostic correlation must include Activity, ViewModel and repository instance lifetime as well as acquisition owner, operation ID, session generation, guard exit, attempts, null reason, publication/UI adoption and cancellation. The original failed lifecycle is no longer available for a faithful live inspection. Main remains4b2d3e67 after fresh git fetch.

## Concrete diagnostic-only proposal, pending owner decision

Objective: one report that identifies the actual failed state transition before choosing a fix. Preserve release behaviour and JA11 protocol/mutation/read policies.

Narrow event hooks are needed in HardwareEqRepository.kt (ConnectedObserver versus FlashCompletion, acquisition start/end, accepted publication and cancellation), DacSessionRepository.kt / HardwareEqSnapshotReaders.kt (generation guard or first missing/rejected field reason), and EqLibraryViewModel.kt (instance lifetime and UI adoption). Bind recorder only for ja11Diagnostic in EqLibraryDependencyFactory.kt; reuse existing diagnostic report/share surface. Release uses no recorder and no diagnostic UI. No second read/retry, delay, mutation, reconnect or lifecycle fix accompanies these hooks.

Record only opaque instance/operation IDs, generations, attempt indices, phase/reason enums and booleans. Exclude serials, fingerprints, raw USB payloads, EQ values and unrelated app data. Prove with tests that recorder cannot issue writes, release binding is disabled, cancellation is unchanged, and actual Connected/Flash completion wiring is observable. Run applicable local/emulator/CI/review and freeze diagnostic source/APK/hash/package/signer before asking for one owner-controlled physical Flash. No release signing-path bypass or publication.

Owner runbook§17 forbids production edits before the95% gate. These trace hooks would touch shared main-source files even though their recorder is diagnostic-only; phone unlock alone does not explicitly amend that source gate. Therefore only the bounded diagnostic exception is requested, not permission for a speculative fix. Production files remain untouched while awaiting that decision.

The overnight G21 no-access evidence remains MET for its original overnight interval. Current phone observation occurred only after owner unlock; it does not rewrite historical no-access evidence.

## Explicit diagnostic source exception approved

Owner answered Approve diagnostic-only tracing to the bounded exception request. Shared-source event hooks may now be added before physical causal confidence reaches95%, exclusively for diagnostic observation. No production fix, retry/USB/timing/mutation policy change is authorized by this exception. Scope and proposed evidence fields are recorded above. Phone authorization remains active, but further phone use waits for validated exact diagnostic candidate.

## Immediate phone revocation and off-phone continuation

Owner went offline/asleep again and revoked all phone/JA11 access immediately. New required unlock is OWNER PRESENT — AUTHORIZE PHONE ACCESS AGAIN; the earlier unlock no longer authorizes access. No further Pixel discovery, ADB query/connect, install, launch, JA11 action, Flash or setting change is permitted. Approved diagnostic-source exception remains active. Owner explicitly authorizes all safe off-phone diagnostic implementation, privacy/cancellation/session tests, full applicable validation, exact-head hosted CI/CodeQL, independent review, APK freeze/provenance and checkpoint pushes until ready or genuine hard blocker. No release publication. Final physical-gate handoff must begin PHYSICAL PHONE ACCESS REQUIRED — WAITING FOR OWNER and include exact diagnostic tuple and all off-phone gates.


## Diagnostic implementation and fresh independent review checkpoint

All implementation is isolated in managed worktree `/Users/stephenweeks/.codex/worktrees/ja11-convergence-trace/OPRA-EQ-for-UAPP`, branch `codex/ja11-postflash-diagnostic-trace`, based on unchanged main `4b2d3e67a00f1c7545e27165135c56fc901206b9`. Recorder is typed, bounded to 2048 records, synchronized, process-local, and has no transport/file/coroutine/callback ownership. Hooks record acquisition owner, lifetime IDs, attempts, scalar session/bundle state, typed read rejection and cancellation. Release/debug flag is false; only isolated diagnostic package `.ja11trace` is enabled. JA11 transport/flasher/protocol/settings production files are unchanged. No extra reads, retries, delays, mutation/reconnect policy or cancellation swallowing were added. Synchronous observation has ordinary finite logging overhead; no claim of zero timing perturbation is made.

Fresh Luna 6.0 High diagnostic reviewer initially BLOCKED automatic report-screen file persistence and three explicit-read callers sharing a label. Both were corrected: only explicit Save acquisition trace locally persists; Share sends JSON without file persistence; EQ-program write/restart verification/editor baseline have distinct owners. Follow-up verdict PASS, no remaining source-review blocker. Reviewer also inspected exact-source helper/workflow boundaries; hosted results remain pending. Review did not establish root cause or hardware evidence.

Focused real-wiring JVM tests pass, including real DacSession/Android HID lifecycle with simulated USB replacement: old generation read completion is rejected and correlated, replacement bundle publishes; cancelled/closed acquisition does not retry/publish. Characterization retains observer one attempt versus Flash-completion two. New helper fixtures pass 6/6; actionlint passes. Initial compiler/test-infrastructure errors (Robolectric application cast, custom AGP9.2 host-test component API, JUnit coroutine return type) were corrected without weakening behavior assertions. Full final local gates are running; exact-source freeze/hosted result tuple is not yet claimed.

All off-phone checks use local JVM/build tools or isolated hosted emulators. No local ADB/server/emulator startup, wireless discovery or Pixel/JA11 action was issued after revocation. Diagnostic candidate will use normal pre-existing Gradle debug signing on its isolated package, not the main-only official release-signing workflow. No release will be published or feature branch merged to obtain signing.


## Diagnostic source freeze and local gates

Source `0b2321477999ae0283304b91adf6859c4111d173` is committed/pushed on the isolated diagnostic branch; worktree is clean. Draft PR #93 is attached to this task. Local final log `/private/tmp/ja11-convergence-trace-final-local.log`: full debug 785 tests / diagnostic 788 tests, zero failures/errors/skips; lint debug zero errors/118 warnings/2 hints, diagnostic zero errors/116 warnings/2 hints; debug, release, diagnostic and diagnostic instrumentation assembly pass. R8 mapping verification passes. Generated release BuildConfig has `JA11_CONVERGENCE_DIAGNOSTICS_ENABLED=false`; diagnostic has true; release merged manifest contains neither diagnostic Application nor report Activity. DI tests exercise provider rejection in default build and enabled binding in diagnostic build. Artifact-verifier 6 fixtures and actionlint pass. Fresh source review PASS is scoped to this final implementation; it does not stand in for hosted results.

Exact-head hosted dispatch run `38364872660` was created on the same feature/workflow/source SHA. It includes original debug regression/UI/API26 jobs plus diagnostic full JVM/lint/APK build, unchanged-candidate API26/API35 tests and diagnostic CodeQL. PR catalog/community and standard CodeQL checks also started. Existing normal android-ci.yml zero-job failure is preserved; the registered exact-head fallback is the executable Android gate. No check is treated as passed yet.


## Prepared consolidated physical diagnostic gate (not executed)

Unresolved decision: which real acquisition owner, lifetime transition, null/rejection reason or cancellation prevents EQ adoption after successful JA11 Flash/replacement while DEVICE is current. Fresh Activity/read-only-connect baseline does not answer it. The diagnostic app is an isolated new package with fresh private storage; account for this difference and do not claim identical app-data history or transfer official-candidate acceptance.

After the exact new reauthorization phrase only: verify unchanged candidate bytes/signer/source; preserve existing production and historical diagnostic installations; capture complete current JA11 original settings/EQ/program state before any mutation; verify healthy current-session DEVICE and complete editable EQ; export initial lifecycle/acquisition trace. Owner controls one previously qualified Flash using the agreed profile, with no automatic mutation retry. Keep the app process/Activity alive; observe replacement DEVICE and EQ convergence; before Connect/relaunch/restart/unplug or a second Flash, open the separate report Activity and explicitly Save the acquisition trace plus existing operation report. Correlate opaque Activity→ViewModel→hardware repository IDs, operation/acquisition IDs, generation, attempt, typed rejection, accepted publication and exposed UiState. UI_ADOPTION proves observed ViewModel UiState, not pixel rendering. Trace export itself contains no raw command payloads/serials/fingerprints/EQ coefficients; existing operation report retains its pre-existing independently governed contents.

Stop without another Flash if candidate/provenance/baseline is wrong, original state cannot be captured, transaction/reconnect state is uncertain, retained evidence is missing/truncated, or the owner withdraws access. Preserve failure before any recovery. Any required restoration is a separate owner-controlled plan based on captured original state, never automatic replay of an uncertain mutation.

Outcome meaning: (a) faithful split state plus complete trace identifies the route for off-phone executable reproduction/root diagnosis; (b) EQ publishes on the replacement generation but exposed UiState remains absent directs analysis to state adoption; (c) cancellation/lifetime replacement or generation guard identifies the corresponding lifecycle/session boundary; (d) healthy convergence does not prove the original cause or a fix and does not authorize repeating Flash until the trace is analyzed; (e) incomplete/truncated trace is inconclusive. No phone commands or owner wake/contact were issued to prepare this plan.


Diagnostic CodeQL completed successfully in exact-head run38364872660. Independent GitHub code-scanning API record id1941592143 confirms commit `0b2321477999ae0283304b91adf6859c4111d173`, ref `refs/heads/codex/ja11-postflash-diagnostic-trace`, category `/language:java-kotlin/ja11-convergence-diagnostic`, created `2026-10-13T07:51:19Z`, empty error and zero results. PR catalog run38364876721 and priority-community run38364876683 pass; dependency-submission run38364833645 passes. Standard PR CodeQL remains pending. Hosted candidate/emulator/build gates remain pending; API result does not imply their completion.

Host inspection of assembled unsigned release APK via aapt confirms no diagnostic Application/report Activity/package suffix in its manifest. Exact main-to-diagnostic diff is empty for JA11 data/domain kt02h20 transport/flasher/protocol, FiioJa11ControlRepository and security paths; no production setting or signing gate was weakened. The old report launcher label/function remain pre-existing diagnostics, with newly scoped trace exports only in the diagnostic source set.


## Hosted artifact parser failure and autonomous recovery

First exact-head run38364872660 on0b232147 completed all original build/min-API/API35 UI and diagnostic CodeQL jobs successfully. Full API35 debug UI64 tests passed. Diagnostic build also passed788 JVM tests/lint/APK assembly, but artifact creation failed because the helper required nonempty versionCode for the instrumentation APK. Actual local aapt and hosted traceback confirm AGP test APK package `com.weekssa.opraeqforuapp.ja11trace.test` intentionally has versionCode/versionName empty. This was a verifier metadata-parser defect, not a production/test failure. Candidate upload did not occur; downstream diagnostic API jobs were skipped and are NOT RUN, never treated as pass. Original run remains failed evidence.

Separate helper-only recovery commit `f6bc1d6d87b9618bc35354f33103504c753c7f6a`: parser represents omitted test versionCode as None; target APK still requires positive code, diagnostic version suffix and exact manifest version. Both APK digests/packages/signers/source requirements stay enforced. Nine helper fixtures pass, including observed AGP badging and rejection of absent target version. Actual local exact-source APK pair rebuilt and helper create/verify passes; this local debug-key pair is validation only and is not the hosted frozen candidate. Luna High bounded two-file follow-up PASS confirms provenance checks preserved. No Android/production/transport/workflow code changed in recovery.

New exact-head run38366296490 dispatched on the new identical workflow/source headf6bc1d6d. Old-head APK/check evidence does not qualify its source identity. Final artifact and exact-head gates remain pending. Main remains4b2d3e67; phone remains revoked/untouched since revocation.


## Exact hosted diagnostic candidate verified (full debug UI gate still running)

New exact-head candidate job PASSED on `f6bc1d6d87b9618bc35354f33103504c753c7f6a`, workflow `.github/workflows/android-ci-exact-head.yml`, dispatch run38366296490 attempt1. GitHub artifact11797477044, `JA11-convergence-candidate-f6bc1d6d87b9618bc35354f33103504c753c7f6a`, created2026-10-13T08:06:10Z, expiry2027-01-11T08:00:02Z; API workflow_run binds branch/source/run exactly. Downloaded ZIP SHA256 independently equals GitHub digest `c784db08afd11a460b5a3d947749187122f2c8220f959dfe591a82efc7572598`; CRC and exact three-file membership pass. Host helper independently verifies signed APK pair, embedded source, exact package/version/code and shared signer; zipalign checks target4-byte/16KiB native-page alignment successfully.

Target `app-ja11Diagnostic.apk` SHA256 `4b00ab3e1c23e435b6ca848a1a0123068ac65e41a00f2fdbae1b1cb6fae796f9`; package `com.weekssa.opraeqforuapp.ja11trace`, version `0.8.1-ja11trace`, code12; signer SHA256 `5857646924badad4df93d8f953a57792cff75662c99d3af808d910d4081170fc`, normal hosted Gradle debug key, APK v2 verification passes. This is not the official pinned release signer. Test APK SHA256 `a7d79771f54d933cf217e854f3fa39b09da2d9ebb477cc64979cc77d82b52612`, package `.ja11trace.test`, same signer, intentionally omitted version fields.

Both hosted diagnostic API26 and35 jobs PASSED using these same unchanged uploaded APK bytes. Downloaded test logs independently confirm OK(6 tests) each; runtime app-produced reports embed exactf6bc source/package/version12, records22/23 respectively, zero dropped events. These tests cover existing report retention/share, explicit convergence Save/privacy/runtime provenance and Activity recreation versus retained ViewModel lifetime. Simulated/hosted evidence only, no hardware claim. Reports artifacts11797665040(API26),11796329646(API35) ZIP digests verified independently. Hosted full diagnostic JVM report788 tests, zero failures/errors/skips (artifact11797492112 verified ZIP digest).

Exact new-head diagnostic CodeQL analysis API id1941674168 has category `/language:java-kotlin/ja11-convergence-diagnostic`, exactf6bc head ref, empty error and zero results. Standard PR Analyze Kotlin run38366271164/CodeQL passes too; new-head catalog38366271170, priority-community38366271403 and dependency38366265528 pass. Exact-head build and API26 minified release launch jobs pass. Full API35 debug UI64 gate is still running; final candidate READY is not yet claimed.


## Final diagnostic qualification and physical stop

Exact-head run38366296490 completed SUCCESS: all seven real jobs PASS onf6bc1d6d source/workflow head. Downloaded full debug API35 UI artifact11796774105 has independently verified ZIP digest556fc458ce60a926795e2d96f9173789c7ece70ddd7060f4d802ad8fdbd29c4c; XML confirms64 tests/0 failures/0 errors/0 skipped. No original zero-job normal-workflow failure or skipped downstream diagnostic job is counted as pass. Final PR #93 remains OPEN/DRAFT and exact headf6bc1d6d; no merge/publication was attempted. Live remote main is still4b2d3e67, public latest releasev0.8.1 id411100198 and its assets unchanged.

After all gates passed, verified candidate files plus original GitHub artifact ZIP copied to `/Users/stephenweeks/Downloads/JA11-convergence-diagnostic-f6bc1d6d87b9618bc35354f33103504c753c7f6a/`. Independent helper verification passed again on the final copy, target digest unchanged; files/directories made read-only. No rebuild or re-sign occurred during freeze. Candidate identity above is the final diagnostic tuple. No hardware evidence/root/fix claim transfers from these gates.

| Diagnostic gate | Outcome |
| --- | --- |
| D01 Owner-approved bounded source scope, production JA11 policy preserved | MET |
| D02 Structural privacy, bounded recorder, explicit-only file export | MET |
| D03 Debug/release binding disabled, release APK diagnostic UI absent | MET |
| D04 Retry/cancellation/session/generation behavior equivalence and real wiring | MET |
| D05 Focused/full JVM checks (785 debug /788 diagnostic) | MET |
| D06 Lint/assemblies/R8 | MET |
| D07 Fresh Luna High review and bounded recovery follow-up | MET |
| D08 Exact-source/workflow hosted CI, seven real successful jobs | MET |
| D09 API26/35 exact-pair six tests each, mainUI64/minAPI release launch | MET |
| D10 Exact diagnostic CodeQL zero findings plus standard PR analysis | MET |
| D11 Catalog/community/dependency/helper9/actionlint | MET |
| D12 Exact APK/package/version/signer/source/artifact integrity and durable freeze | MET |

Diagnostic totals12 MET/0 UNMET/0 abandoned. Original final-fix gates remain8 MET/15 UNMET; root confidence below95%, faithful physical-failure regression and minimal correct production fix are not achieved. Task stops at the owner-authorized diagnostic physical gate, not at release readiness. MORNING_HANDOFF begins the exact newly required heading. Canonical RUNBOOK now includes the direct owner overrides/new unlock at its top, preserving original text as history. No local ADB/emulator server, phone discovery/query/install/launch, Flash or setting action occurred after revocation; earlier authorized fresh-Activity baseline remains separately classified.


## Fresh owner authorization and physical preparation

Owner explicitly sent OWNER PRESENT — AUTHORIZE PHONE ACCESS AGAIN. Previous no-access interval remains preserved. Exact frozen diagnostic APK independently reverified: sourcef6bc1d6d /hash4b00ab3e /signer58576469, isolated .ja11trace package. Single explicit current target verifies Google Pixel9; reported SDK37. JA11 attached with expected FIIO VID10610/PID258. Production app has no live process and no Activity at initial launcher observation, so original failed lifetime cannot be preserved. Diagnostic and old .ja11diag packages absent. Production APK is privately preserved before installing the isolated diagnostic. No Flash/settings mutation has been authorized or issued in this preparation; owner-controlled action remains gated on complete baseline. Private captures outside Git at /private/tmp/ja11-convergence-owner-present-nqftlf0m.

## 2026-10-13 J030 — completed diagnostic healthy control, original failure not reproduced

Owner reauthorization opened this session; the earlier revoked interval is unchanged. The installed diagnostic APK was independently pulled and verified against the frozen source `f6bc1d6d87b9618bc35354f33103504c753c7f6a`, SHA-256 `4b00ab3e1c23e435b6ca848a1a0123068ac65e41a00f2fdbae1b1cb6fae796f9`, package `com.weekssa.opraeqforuapp.ja11trace`, version/code `0.8.1-ja11trace` / `12`, debug signer certificate `5857646924badad4df93d8f953a57792cff75662c99d3af808d910d4081170fc`. No substitution/rebuild/re-sign occurred. Existing production installation was preserved privately (APK SHA-256 `b617f79b7980ae45083f5f2d24f769d50da6f57f6d6ba03c668187670fb4b644`) and was not targeted by the diagnostic install or launch. Diagnostic package had fresh isolated storage. Test plan was the consolidated physical gate in MORNING_HANDOFF at orchestration commit `f690626bbd5a479472e1808f2efa26aecf8f3a15`, with the owner controlling the single final Flash confirmation. Pixel 9 reported SDK37; this is physical evidence on that OS, separate from hosted API26/35 evidence.

Complete read-only baseline: FiiO JA11 firmware2.20, VID/PID `0x2972:0x0102`, Mic On, UAC2, active User1, output volume60/60, sample rate384kHz, global gain−3.70dB; bands Low Shelf76Hz/+4.0dB/Q0.38, Peak3800Hz/+3.0dB/Q0.38, Peak1500Hz/−1.5dB/Q1.00, Peak2700Hz/+1.0dB/Q2.67, Peak200Hz/−1.0dB/Q1.73. The temporary Personal EQ `DiagnosticCurrentEQ` exactly matched that baseline, including native codec precision. Healthy connected-session DEVICE and editable EQ were captured before mutation. Fresh diagnostic preferences enabled FiiO/Direct Flash only; no JA11 setting was changed to prepare the test.

Owner tapped the final confirmation once and answered “Flash finished.” Operation `88d718f4-47c0-4bd0-9fff-a1d70b06f311` report contains contiguous events1–32, outcome Success, stateKnown true, comparison FINAL_READBACK. Independent packet decoding confirms exactly one User1 select write0x16, five band writes0x15 (indices0–4 once each), one gain write0x17, one Apply0x18 and one Save0x19. Save was on generation1/detach0; there are no writes after Save. All seven successful final program/band/gain reads are on generation2/detach1 and match the complete original User1 baseline. No claim of same-unit continuity beyond the sole returning qualified JA11 is made. Runtime signerVerified=false is expected for the independently verified debug signer; it is not an official release signature.

Acquisition trace after Flash has96 records/zero drops. The CONNECTED_OBSERVER acquisition6 queued while Flash held the shared mutex, entered after mutation return and published a complete current generation2 bundle on its first attempt. FLASH_COMPLETION acquisition7 then entered and likewise published generation2 on its first attempt, ending before FLASH_RETURNED. Both EQ and DEVICE UiState were current generation2. Main Activity1, ViewModel4, hardware repository3 and session repository2 remained the same; an Activity pause/resume was observed during the operation, without destruction, ViewModel clear or repository close. No missing-field rejection, generation rejection, cancellation, failed read or second attempt occurred. UI_ADOPTION proves observed ViewModel UiState; independent final screen captures separately prove all five bands/global gain rendered and Edit EQ enabled. Final retained trace has107 records/zero drops and still no lifetime replacement/cancellation.

This is a HEALTHY CONTROL, not reproduction of the original physical split state, identification of its root cause, proof of a fix, or release qualification. Diagnostic storage, build/minification, trace overhead and the pre-existing diagnostic UiState subscriber differ from the original production run. The subscriber shares the upstream StateFlow; hardware acquisition has its own repository scope, but observational differences cannot be dismissed without controlled off-phone evidence. Original failed lifetime was already unavailable before this session. Root confidence remains below95%; faithful failed-state regression and production fix remain pending.

After evidence retention, read-only DEVICE Refresh verified Mic On/UAC2/User1/volume60 and rendered EQ matched all baseline values. Only the temporary Personal EQ and its exact task-created Downloads source were removed. No second Flash, Reset, Mic/UAC/volume/program write, reconnect recovery, app restart or unplug was used. Exact baseline target plus decoded final readback and fresh DEVICE/UI observation establish original-state restoration; none remains outstanding. Diagnostic APK/report remain installed. Phone was explicitly released; no further hardware action is planned. A calendar notification briefly interrupted later navigation after critical capture; unrelated capture was excluded from the curated archive, no calendar data is committed or used as mission evidence.

Private curated evidence is outside Git at `/Users/stephenweeks/.codex/evidence/ja11-postflash-diagnostic-control-20261013`, restricted to the relevant24 captures. Raw serials/fingerprints, screenshots, logs and packets remain private. Independently rechecked digests:

| Artifact | SHA-256 |
| --- | --- |
| baseline-state-private.json | `50d30d55ab28509fb95f88913f0e796fe7d4e344dd56e0c718a590f1e9d5974f` |
| post-flash-convergence-trace.json | `2e84a8ff82fd63237bcce88c4f8b354b9a9d602194619501935414be8d76c0dc` |
| post-flash-operation-exact-share.json | `16bae426d2b9736391f4762bd14ad222f63f24bbec7b6044e625cb5b485ee27c` |
| post-flash-operation-exact-share.txt | `0c8b1fe7a8b888102dc2b47165a5b568a65cf8593ad271ff18799f505db39b0d` |
| final-convergence-trace.json | `8b8396036ad18eb5217344b53610174d884430524c987583f8b28245cc38ab2a` |
| physical-verification-private.json | `1d24d9a321fec76e1429fa9396c21bf4b08165d80b4d219d638719a449341e43` |

## Original failed Flash owner recollection and bounded follow-up

Owner reports staying on the screen until the earlier Flash finished and identifies its origin as My EQs headphone/profile. Today’s control used a Personal EQ card. This excludes attributing the original failure to owner-reported manual navigation; it does not prove an uninterrupted composition/Activity lifetime. Current source uses different composition-owned Flash coroutines for these screens. Primary and bounded read-only Luna follow-up are checking whether Automatic output / output-specific library data can dispose the managed-headphone caller during Save detach without navigation. This is a hypothesis until demonstrated; no speculative production fix or hardware repeat follows from the route difference. Diagnostic source/candidate remain frozen and unchanged.

Owner subsequently recalls that the app automatically returned to the My EQs list after the failed headphone/profile Flash. Bounded Luna/source follow-up locates the conditional route: DETACHED publishes absent/disconnected; Automatic resolver falls back without a session override; ViewModel emits no managed rows on output/library mismatch; EqLibraryApp replaces detail when selected row is absent and clears selection; detail-owned coroutine cancels. Catalog readiness alone does not remove the detail screen. Owner recollection corroborates the navigation consequence; original override/fallback/read-result/timing remain unknown. Two test-only characterizations in UiModernizationFlowsTest use real EqLibraryApp/detail/completion helper plus the production pure output resolver, simulated mutation and a gated refresh: fallback/disappearing row versus retained JA11 output. They compile/assemble successfully locally; hosted execution is pending. Production runtime files and frozen diagnostic worktree remain unchanged.

## Owner-supplied failure video and exact visible transition

Owner attached local video `screen-20261013-112632-1791908774031.mp4`, SHA-256 `57180f9a8ab2d0145e8ca393ad0144c6a4f04100857b75093eff796a8196478a`, duration15.55897s,1080×2424. Owner explicitly identifies the regular EQ Library app, not the separate tracing package. Video does not embed APK/source identity; last independently verified regular installation is b617f79b on main4b2d3e67, but that installation verification is not an embedded provenance claim for this recording. Original video preserved privately at `/Users/stephenweeks/.codex/evidence/ja11-owner-video-20261013/`; no phone query/replay was used to review it.

Decoded frame timestamps establish: Explorer/LoboNauticus managed-headphone profile, current FiiO target before confirmation; Flash confirmation3.51–4.11s; verification spinner and stale readback4.50–7.0103s; at7.0179s device Disconnected, target becomes UAPP/ToneBoosters, detail is replaced by the empty My EQs home while spinner remains; at7.0288s spinner disappears; at7.0354s saved headphone rows return under the fallback output; around9.1091s target returns to FiiO/current device read begins;9.1675s connected/current header;11.00–13.32s My DAC EQ tab contains literally no EQ status/graph/actions;15.01s DEVICE tab shows current read values, User1/volume60/MicOn/UAC2. No owner navigation precedes the detail removal. This specifically demonstrates transient empty library/output state even though the fallback library also contains Explorer; absence need only be transient, not permanent. Video is direct visual evidence, not a USB transaction report, exact Save-count proof or internal cancellation trace.

New source link: FiioJa11Flasher.flash rethrows CancellationException before trace completion/publication (`FiioJa11Flasher.kt:81–96`), leaving traceStore.status Running. My DAC EQ branch suppresses all ordinary EQ content while operationStatus is Running (`FiioJa11MyDacContent.kt:173–183`); the Running FLASH message returns null (`:503–511`). Therefore stranded Running FLASH can explain literal blank EQ even if an independent EQ observer later reads a bundle. The earlier missing-bundle premise remains separately accepted; it is not sufficient by itself to explain a literally empty layout. Root analysis now must include stranded operation state as well as acquisition ownership. Separate DEVICE collection can become current independently. No production fix is made before executable confirmation/root-gate reconciliation.

First new-characterization hosted run38430645363 on ba8fdd19: full JVM/lint/build and API26 minified release pass; API35 UI64 existing tests pass, but both new tests fail before mutation at an ambiguous confirmation selector (two Flash nodes: underlying row and dialog). This is a test-selector defect, not a observed cancellation result. Selector is corrected to Flash with a dialog ancestor, preserving assertions. A third test now drives real FiioJa11Flasher to a simulated Save suspension, real managed-screen removal/cancellation and real traceStore status, then the production one-attempt normal-null observer policy and current replacement DEVICE state; it asserts literal missing EQ content and no final read/replay. Fake transport owns no Android USB resource. Local instrumentation compile/assembly passes; hosted execution remains pending. Fresh bounded Luna review of the first two tests PASS with explicit lifetime-only boundaries; fresh review of the actual-flasher extension remains pending. The frozen diagnostic candidate/head is unchanged.

## Architectural root gate established and production regression preparation

Exact27bb044559cae4cd16b888cf662a84fb1f1dd302 run38432117344 attempt1: all three new Compose lifetime/real-flasher tests PASS, independently read from hosted XML (times5.488s/5.637s/5.434s). Build/full JVM/lint/release assembly/R8 and API26 smoke PASS. Full API35 job failed in existing tests reporting unfocused resumed Activity windows; initial failures remain preserved and the same-SHA failed job was rerun without changing source/assertions. No failed job is counted as full-suite pass.

Fresh bounded Luna High adversarial judgment accepts >=95% confidence in the architectural root when reconciling direct video, source, actual-flasher/Compose reproduction, and the owner-authoritative normal-null/zero-retry premise. Primary agrees: FiiO presence loss changes Automatic output; the transient library-key mismatch removes the selected managed row; detail scope cancels at Save/reconnect; flasher cancellation rethrows before terminal trace publication, leaving Running FLASH; caller-owned awaited completion cannot perform its bounded retry; independent one-shot EQ observer can return normally without an accepted current bundle; independent DEVICE can become current. Running FLASH deliberately suppresses EQ content with a null progress message. Low-level null field/rejection reason and original physical acquisition ID remain unknown; the fix does not require guessing or changing that read/protocol policy. This is an evidence-based architectural confidence judgment, not a statistical estimate or captured cancellation telemetry.

History independently verified by primary ancestry checks against both v0.8.0 and v0.8.1: managed Flash composition scope08d5daebac17872d00045ec221c845da99761d98; output-key library/mismatch30535bd3b1bce9940d23e8735d88a4d9b6a9a4ef; Automatic resolverf80543ac8dec4fb132834149653814b6c93c8aa3 and presence projection05a0cac7535efb055be1041f8d9195207bdf8d53; selected-row cleanup10e8599252e26f9e4abf86785c794528a33808f3; cancellation rethrow054298b866fad4ca97cb649790af54ccc6a4cfba; FLASH progress-null9808518d741a954144d310cdf3772c5410bacb35 and busy-branch suppressiondc6478a25b1745b4f78f833c69e96e066c615d56. These are pre-existing defects/exposures, not a new PR88/89 protocol regression. Those patches changed awaited completion/retry only; they retained its cancellable screen owner. Earlier specialist MyDacScreen selection-fallback attribution was rejected as unrelated to global Automatic output and corrected before synthesis.

G03 is now MET; original final-fix totals9 MET/14 UNMET. G04 stays UNMET until a desired-behavior regression is observed red against the actual ViewModel/repository/session path and green after the fix. A new clean managed worktree `/Users/stephenweeks/.codex/worktrees/ja11-flash-owner-fix/OPRA-EQ-for-UAPP`, branch `codex/fix-ja11-postflash-convergence-final`, starts from unchanged current main4b2d3e67. It carries only test changes so far. New FiioJa11FlashLifetimeTest uses production DI/ViewModel/HardwareEqRepository/DacSessionRepository/Flasher/readers, controlled in-process session transitions and fake Save/snapshot/DEVICE sources; no real USB resource or hardware is used. It targets retaining the same ViewModel while losing only the screen caller, first normal-null observer acquisition and bounded completion publication. Regression is running before any production behavior change. Proposed minimal repair owns the existing complete JA11 Flash/readback/EQ/DEVICE completion in viewModelScope, leaving caller cancellation and owner clear distinct; no USB/retry/mutation/identity policy changes or second Save.
