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
