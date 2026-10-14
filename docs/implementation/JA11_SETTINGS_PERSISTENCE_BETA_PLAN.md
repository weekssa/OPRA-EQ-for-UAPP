# JA11 native settings Save beta — Luna High execution handoff

Status: **PLAN READY; IMPLEMENTATION, BUILD, CI AND PHYSICAL ACCEPTANCE NOT RUN.**
Owner-approved scope: 2026-10-13. This document supersedes this chat's earlier Set as default button and UAC round-trip proposals.
The current task creates this handoff only. The Luna worker conducts implementation and builds.

Handoff verification: independent read-only plan review **PASS** after clarifying bank-identity limits, the beta-only report path, and final-APK emulator verification. Document scope/format checks passed; these are planning checks, not software or hardware acceptance. The isolated branch contains this one uncommitted Markdown addition; the original checkout remains clean.

## 1. Intake, authority, and exact starting state

- Repository: `weekssa/OPRA-EQ-for-UAPP`; no other repository is writable.
- Prepared worktree: `/Users/stephenweeks/.codex/worktrees/ja11-settings-persistence-beta/OPRA-EQ-for-UAPP`.
- Prepared branch: `codex/ja11-settings-persistence-beta`.
- Base and current executable HEAD: `421f03331e45e6c1bdc77e5442db1de467432930`, independently rechecked against remote main.
- At handoff, only this Markdown file is new/uncommitted. No application code has changed. No APK exists for this feature.
- Latest public stable is v0.8.2; leave its package, tag, release, and artifacts unchanged.
- Use this prepared worktree. Inspect origin, branch, HEAD, and status before writes. Preserve unrelated changes. Do not reset, clean, rebase, or silently adopt a different executable base.
- Commit this handoff as a documentation commit before implementation; retain the base above as the implementation comparison point.
- Read root `AGENTS.md` and all of `docs/CHATGPT_PROJECT_RUNBOOK.md`, then relevant architecture, approved My DAC design, current JA11 protocol/checklist/capability/ledger, and signing documents. This explicit owner-approved beta scope overrides older instructions forbidding Save for runtime volume/program **only within this beta**.
- Refresh live PR/check/release facts before using their historical status. Stop on a material conflicting executable change; catalog/documentation movement alone is not a reason to restart the mission.
- Load Android development/testing skills and narrow Compose, UX, coroutine/Flow, and Gradle skills as applicable. Use test-first development. Follow repository specialist-review requirements with bounded read-only investigations; the primary worker owns edits/integration.
- Use Luna High for the worker. Do not create premium-model research pipelines or expand into a general DAC/release audit.

## 2. Locked product behavior and exclusions

| Explicit owner action in the beta | Required sequence |
| --- | --- |
| My DAC → DEVICE → Device volume → Apply | Write requested volume; verify; send one native Save carrying actual active EQ program; handle normal Save restart; verify final state. |
| My DAC → DEVICE → EQ program → choose a different entry | Write selected program; verify; send one native Save carrying that verified program; handle normal Save restart; verify final state. |

These are exactly two **new** Save triggers. Existing custom-EQ Flash/editor/Reset keep their established Save behavior.

- Keep native volume 0–60; retain existing Cancel/Apply dialog, steppers, Current action, picker, typography, colors, spacing, navigation, and accessibility.
- Support Vocal, Classic, Bass, User 1, and Off in the program picker. Keep unchanged-value Apply and the Current program entry disabled as today; opening/cancelling/staging/recomposition causes no write or Save.
- Save may commit the companion current setting too. Disclose that the current volume and program are saved together; do not promise independent startup defaults.
- Save's own firmware disconnect/reconnect is expected. Send one Save request, observe the firmware lifecycle, and never issue a second reset to manufacture a reconnect. Do not promise an exact firmware reboot count from software alone.
- Do not send UAC or microphone writes, select User 1 as a helper step, rewrite EQ bands/gain, or add EQ Apply `0x18` for these actions.
- No extra button, raw command controls, diagnostic launcher, output/identity expansion, reconnect-time cached defaults, settings migration, dependency update, CI repair, or other-DAC change.
- No merge, official signing workflow, public APK upload, tag/release publication, public support claim, or modification to preserved evidence PRs is authorized.

## 3. Evidence and the actual Save primitive

The local `FiioJa11Protocol.saveToFlashReport()` emits:
`02 AA 0A 00 00 19 01 03 00 EE`.
Its no-argument User 1 packet must remain byte-for-byte unchanged.

`AndroidFiioJa11UsbTransport.saveToFlash()` sends this one report and observes the established Save boundary. It does not itself select a program or write bands/gain/UAC/mic. Full custom-EQ Flash does those additional EQ steps separately and is unsuitable for these two actions.

Manufacturer-source evidence was independently inspected in:
[FiiO Control web bundle](https://fiiocontrol.fiio.com/static/js/index-B_yYxU3i.js),
SHA-256 `f223151bbdc964883d4573767625bbaf6a34ce9c4babe314ad33d7bfafb19921`.

- The JA11 map is Vocal=0, Classic=1, Bass=2, User 1=3, Off=4.
- `setSaveEqToDeviceByCtrl()` passes the selected `eqPre.value`.
- `bre(e)` constructs command 25/`0x19` with one-byte payload `[e]`.
- JA11 follows that common HID Save path. The Save UI helper writes bands only for user presets, then invokes Save for the selected program.
- This establishes manufacturer software behavior, not proof of all firmware effects or volume persistence. Preserve the existing qualified Android framing; do not transplant FiiO's sequence/CRC builder or unrelated code.
- [JA11 manufacturer FAQ, item 14](https://www.jadeaudio.com/details?_l=en&article_id=178) describes Save causing chip power-off/restart.

Owner-supplied video observations, at approximate recording times:

| Time | Visible outcome |
| --- | --- |
| 0:12 → 0:24 | Off without a saving trigger returns to User 1 after owner-reported unplug/replug. |
| 0:36 → 0:57 | 55/60 + Off, followed by UAC 1.0→2.0, returns as 55/60 + Off. |
| 1:30 → 1:39 | 60/60 without another saving trigger returns to saved 55/60. |
| 1:46 → 2:06 | 60/60 plus UAC switches returns as 60/60 + Off. |

The owner additionally reports equivalent retention after microphone toggles and custom-EQ Flash. Classify these as owner-reported observations; the recording does not expose packets, APK provenance, firmware identity, physical connector movement, or all built-in programs.

Private source video: the owner's local attachment `screen-20261013-220104-1791946731488.mp4`.
Keep account/directory information out of committed evidence. Do not commit, upload, or publish the recording or private derived images.

## 4. Implementation contract

### Variant and entry points

In `app/build.gradle.kts`, add `ja11Beta` initialized from debug with:
application ID `com.weekssa.opraeqforuapp.ja11beta`, versionName `0.8.2-ja11beta`, versionCode 13, production MainActivity, and variant resource label **EQ Library JA11 Beta**.

Add `JA11_SETTINGS_PERSISTENCE_BETA_ENABLED` default false and true only for ja11Beta. Embed the full frozen source SHA using existing `CANDIDATE_SOURCE_SHA` support. Enable this variant's Android tests as with the existing diagnostic variant. Keep official package/version metadata unchanged. Use the normal debug/test signer; Android requires a signed APK.

Keep the existing volume/program callbacks. Route just those two callbacks to the new operation when the beta flag is true. Mic/UAC and non-beta callbacks keep their ordinary paths. Provide a default-false injectable policy at the ViewModel dependency seam for testing the beta route without enabling it in other variants.

### Codec, transport, and repository

- Add a typed `saveCurrentSettingsReport(program: FiioJa11Protocol.EqProgram)` encoder using the existing frame with `program.code` in the Save payload.
- Add `saveCurrentSettings(program)` to the JA11 transport. Reuse the existing Android Save-boundary/late-detach implementation. Keep no-argument `saveToFlash()` behavior unchanged for Flash/editor/Reset.
- Add `writeControlAndSave(intent)` to `FiioJa11ControlRepository`, restricted to OUTPUT_VOLUME and EQ_PROGRAM. Expose read-only stored-bank/gain and program-aware Save through a narrow persistence-source capability implemented by the session source. Missing capability must stop before any mutation.
- Acquire the shared JA11 operation gate once for the complete transaction. Do not call public lock-taking write/read methods while holding that same non-reentrant mutex. Extract unlocked helpers as needed while preserving ordinary control behavior/tests.
- Read one coherent baseline: complete DEVICE fields, active program, current generation/detach state, and existing identity/permission/cardinality. Capture EQ parameters through read-only transport where their semantics are established. Under User 1, use the qualified User 1 bank/gain read. Under a built-in program, do not label raw parameter reads as stored User 1 without exact source/firmware evidence; the current acoustic reader intentionally cannot supply built-in coefficients.
- Validate range/program and generation immediately before the first write. Write only the requested field once. Read back in the same stable session and verify target plus unrelated DEVICE state. Stop before Save on mismatch, unexpected detach, or an incomplete required read. Capture the resulting program's readable parameter state immediately before Save for a like-for-like post-Save comparison; do not compare different active programs as though their bank identity were established.
- Derive Save's argument from verified hardware, never a cached UI value. Send exactly one Save.
- Reuse established Save observation and replacement-session rules, including optional usable-serial comparison and sole-candidate enforcement. Preserve all existing deadlines and no-replay rules.
- Read final state on one current generation. Verify target, companion volume/program, mic, original UAC/PID, and firmware. Compare established EQ bank/gain semantics using existing native comparisons; compare any otherwise meaningful same-program raw state to the immediately pre-Save state without claiming stored-bank identity. Unknown bank identity stays INSUFFICIENT_EVIDENCE in the report, not a fabricated preservation pass. Never select User 1 as a read helper or broaden comparison tolerance. Sample rate is informational and may legitimately change.
- If Save detaches late during final reads, allow at most one existing read-only replacement recovery, then reread the complete bundle. Never replay the target write or Save.
- Return phase-specific verified/failure results with last coherent actual state when available. Sending a packet or reconnecting alone is not success.

### Lifetime, state, and design

Current `FiioJa11DeviceUiState.markStale()` clears ordinary write state. Add explicit composite-save ownership, retained through expected detach, and include it in `isBusy`. Ordinary reconnect refresh must defer while the save owner is active.

Launch the transaction in `viewModelScope`; only terminal completion/failure or ViewModel clearing releases ownership. Do not wrap the whole transaction in NonCancellable. Guard rapid duplicate callbacks synchronously before launch. Disable/reject competing JA11 DEVICE and EQ mutations, including Flash/edit/reset; the shared gate also enforces serialization.

Use existing status/feedback components with **Saving settings…** throughout the save lifecycle. Add this beta-only help to the existing volume dialog and picker:
**Saves the current volume and EQ program. The DAC briefly reconnects.**

Publish success and fresh state only after final verification. Refresh the EQ tab appropriately: Off stays flat; unreadable built-in coefficients remain identified by program rather than invented. Preserve the v0.8.2 same-process completion/convergence fix.

On uncertain Save outcome, say **Could not confirm that the settings were saved.** Retain actual verified state if available, otherwise label values Last read. Read-only recovery may establish current state; no automated mutation retry/fallback. Cancel/staging/navigation never initiates Save.

### Concrete manual evidence path

The current My DAC report entry is filtered to EDITOR_APPLY; a new Save trace will not automatically appear. For these two beta operations only, reuse the existing completed-result/report card and readable/JSON sharing components. Keep it in the existing operation feedback area, with no new screen, diagnostic launcher, or mutation control. Preserve report access for existing non-beta operations.

Retain each completed/failed settings report until explicit dismissal or a new settings action, so the owner can share a failure before retrying. Include candidate source/operation ID, action, requested volume/program, baseline and final DEVICE values, Save program argument, software-recorded write/Save counts, session/detach/readback stages, bounded retry count, outcome/state-known status, and readable coefficient evidence with its bank-identity confidence. Label software-recorded transport counts as such; they are not independently observed firmware reboot or USB-bus counts. Do not export credentials or raw serial values.

Include the latest automatic read-only DEVICE refresh after a later owner unplug/replug in the retained report; do not initiate any Save/reset to collect it. A detach is not automatically a proven power removal: the owner supplies that fact. This makes the promised manual checks and issue report available without an ADB-driven test. When stored User 1 bank identity cannot be established under the original program, report it explicitly and leave independent stored-bank/power-cycle qualification unclaimed. Do not block the owner on a nonexistent graph view.

## 5. Test-first checks and software gates

Write the smallest failing regressions first. Require these cases before candidate preparation:

| Layer | Required coverage |
| --- | --- |
| Codec | Exact five program payloads; exact unchanged legacy User 1 packet; no changed existing command framing. |
| Repository | Both actions × every program × both UAC modes; volume 0/60; companion-state preservation; known-bank and same-program coefficient comparisons; unknown bank identity never becomes a pass; exactly one targeted write then one Save; zero UAC/mic/EQ-data/Apply/reset writes. |
| Negative paths | Invalid/stale/ambiguous/permission-less baseline; failure before target/Save; volatile mismatch; rejected or uncertain Save; timeout; replacement mismatch; firmware/UAC/mic/companion/bank drift; late detach; failed second read. No write replay on any path. |
| Lifetime/state | Busy remains true on detach; refresh cannot clobber the owner; rapid taps do not queue extra Saves; leaving the screen does not cancel the operation; ViewModel clearing cancels/cleans up normally. |
| UI | Existing buttons/picker and callback wiring; Cancel/current/unchanged cases do nothing; Saving settings feedback; disabled competing controls; terminal error/stale/success; retained readable/JSON report and latest read-only reconnect values; large font and light/dark layout. |
| Regression | Non-beta volume/program remain runtime-only; ordinary mic/UAC unchanged; existing Flash/editor/Reset retain User 1 packet/order/count; other DACs unaffected; post-Flash lifetime/convergence tests remain green. |

Existing anchors: `FiioJa11ProtocolTest`, `FiioJa11ControlRepositoryTest`, `FiioJa11DeviceUiStateTest`, `FiioJa11FlasherTest`, `FiioJa11FlashCompletionTest`, `FiioJa11FlashLifetimeTest`, `DeviceOperationStatusTest`, and instrumented `FiioJa11MyDacContentTest`. Add focused new beta-operation tests rather than duplicating unrelated catalog coverage.

Use the checked-in wrapper through `./tools/codex-android`. After focused iteration, run on a coherent candidate:

```bash
./tools/codex-android :app:testDebugUnitTest
./tools/codex-android :app:testJa11BetaUnitTest
./tools/codex-android :app:lintDebug :app:lintJa11Beta
./tools/codex-android :app:assembleDebug :app:assembleRelease
bash tools/verify-r8-mapping.sh
./tools/codex-android :app:compileJa11BetaAndroidTestKotlin
```

Run affected beta instrumented tests on a new disposable API 35 emulator with an explicit emulator serial (`ANDROID_SERIAL`); never address the owner's phone during software checks. Verify the exact beta APK cold launch and coexistence with stable, and API 26 installation/launch compatibility. A separate package does not prove stable-app in-place upgrade.

Get an independent read-only source/test/scope review. Address findings and repeat only affected checks. Do not run full gates repeatedly without a source change or unresolved failure.

### Exact-head remote checks

Normal `android-ci.yml` is malformed at the planning base (unindented heredoc around lines 453–462); main run `38501038729` failed before jobs existed. Preserve the failure and do not repair CI in this workstream.

After the final candidate is committed/pushed and the branch tip equals its full SHA, use the existing fallback:

```bash
gh workflow run android-ci-exact-head.yml \
  --repo weekssa/OPRA-EQ-for-UAPP \
  --ref codex/ja11-settings-persistence-beta \
  -f source_sha=FULL_FROZEN_CANDIDATE_SHA
```

Use the candidate branch ref, not main. Check every applicable actual result. The fallback covers debug/release, JVM, lint, R8, API 35 UI, API 26 cold launch, and exact-head CodeQL. It does not build/test the new beta variant or provide normal CI's intended API 26 populated-catalog/60-second observation; local beta/emulator gates above must supplement it. Never report normal CI as green. Prepare a draft integration PR and attach it if created; keep all APKs and private reports off public GitHub. No merge or publication.

## 6. Freeze, private artifact, and durable records

Freeze a clean executable source commit after code/tests/docs agree. Build with:

```bash
./tools/codex-android :app:assembleJa11Beta -PCANDIDATE_SOURCE_SHA=FULL_FROZEN_CANDIDATE_SHA
```

Record source/branch/PR/check SHAs, exact command, variant, package, version/code, APK SHA-256, test signer certificate SHA-256, signature verification, zip alignment, and emulator results. Verify actual APK metadata with Build Tools `aapt dump badging`, `apksigner verify --verbose --print-certs`, `zipalign -c -v 4`, and `shasum -a 256`. Perform API 35/API 26 beta install/cold-launch smoke on these final delivered bytes after this frozen-source build; a previous iteration's emulator pass does not qualify the final artifact. No private signing-key content belongs in records.

Deliver the exact private APK and checksum through local/private file links. Do not upload to mobile-test-apk or run official beta/release signing; those are main-only and the beta workflow publishes an APK.

Maintain `docs/implementation/ja11-settings-persistence-beta/STATE.md`, `ARTIFACTS.md`, and `OWNER_TEST_GUIDE.md` during execution. Update relevant maintained runbook/protocol/capability/ledger/changelog wording narrowly when facts actually change. Preserve chronological earlier observations and classify this beta mechanism separately. Never rewrite runtime-only evidence into a retroactive pass.

Keep source software guarantees separate from measured physical facts. Implement the concrete beta-only report path above; record missing physical packet/count telemetry as NOT MEASURED rather than inventing it. Do not add a diagnostic launcher or raw-command flow to obtain reports.

Status progression:
`PLAN READY → IMPLEMENTING → SOFTWARE VERIFIED → PRIVATE CANDIDATE READY → OWNER INSTALLATION PENDING → OWNER TESTING PENDING → PHYSICAL PASS/FAIL/INCONCLUSIVE`.
Do not report completed implementation, APK readiness, hardware persistence, or restoration before the corresponding evidence exists.

## 7. Phone installation and owner-led acceptance

**Hard checkpoint:** only after software gates, independent review, exact-head checks, and artifact provenance pass, ask:

> The private JA11 beta is ready. Please confirm when your phone is ready and wireless debugging is turned on so I can connect and install it.

Wait for the owner's reply. This handoff does not grant a present phone window. If Android requires pairing/address details, ask only for the missing connection information after readiness confirmation. Do not probe remembered wireless endpoints or install before the reply.

Installation authorization is limited to the verified separate beta APK:

- Connect to the explicitly confirmed phone via wireless debugging; verify the intended device and APK tuple.
- Preserve the stable app and its data; do not uninstall it. A pre-existing beta install may be updated only after package/signer compatibility is established. Never silently uninstall a conflicting package.
- Verify the installed beta package/version/source and launcher. Installation does not authorize program/volume/mic/UAC/Flash mutations.
- Return control to the owner for ordinary testing. Wireless debugging is for initial installation and, if an issue occurs, bounded diagnosis after the owner agrees. Do not continuously drive or monitor normal physical tests through ADB.
- Provide app-level steps; do not require Terminal commands, protocol choices, or log interpretation.

One consolidated manual session, using the exact candidate and one supported JA11:

1. Begin from a freshly power-cycled device. The owner records visible original DEVICE settings; the beta internally captures the coherent baseline before writing. Retain baseline/readback information in the report above. Capture User 1 bank/gain when its read identity is established; otherwise mark stored-bank identity INSUFFICIENT_EVIDENCE and never claim it from a built-in graph or raw values alone. Keep stable/FiiO apps closed so the beta owns USB. Stop before mutation if a required DEVICE baseline cannot be established; the owner need not interpret raw bank data.
2. In volume, choose a conservative lower level than the baseline and press Apply once. Wait for terminal completion. Unplug/replug and verify retained volume, original program, and unchanged unrelated state. If baseline is already 0, ask for a safe alternative rather than raising it automatically.
3. Select Vocal (or Classic if Vocal was the baseline) once. Wait for terminal completion; unplug/replug and verify the chosen built-in program and current volume.
4. Verify Off Save/power-cycle retention once, unless the volume case already qualified Save while Off. This covers the distinct Off payload without repeating an already accepted observation.
5. Restore original volume and program through the same approved beta controls; avoid redundant unchanged actions. Verify original UAC/mic and applicable bank/gain evidence, then verify DEVICE restoration after power removal. If the restored original program is User 1, compare the original and restored User 1 bank/gain. If stored-bank identity remained unknown, carry that limitation forward rather than adding helper program switches. Distinguish restored startup state from unmeasured transient state.
6. Owner reports visible results and shares retained reports for evidence, especially on failure. Record exact candidate/hardware/steps/outcome/restoration and limits in the append-only ledger. Use PASS_WITH_LIMITS when the two tested settings persist but independent stored-bank qualification or physical packet/reboot counts remain unmeasured. No general stored-bank-restoration, all-program, or all-firmware claim follows from that result.

Stop at the first failure, unexpected state change, repeated restart, or uncertain result. Preserve the current evidence and actual device state, diagnose before a repeat, and obtain explicit authorization for any recovery mutation. Never silently issue additional Save/Flash/UAC/mic writes.

End by stating software/CI/artifact/physical results separately, restoration status, clean-worktree or remaining files, and the next owner-controlled action. A private-beta pass is not merge/publication approval.

## 8. Copyable Luna worker prompt

Implement `docs/implementation/JA11_SETTINGS_PERSISTENCE_BETA_PLAN.md` in the prepared worktree on `codex/ja11-settings-persistence-beta`, using Luna High. This is the locked scope: only JA11 volume Apply and DEVICE program selection gain one program-aware native Save. Preserve production behavior, design, existing Flash/editor/Reset, UAC/mic state, shared session safeguards, and all historical evidence. Complete the software gates, independent review, exact-head checks, documentation, and private test-signed APK provenance. Do not stop at a code patch or debug build. Then ask me when my phone is ready and wireless debugging is on, wait for my confirmation, and install the exact separate beta. I will conduct normal connect/disconnect testing manually; use wireless debugging for diagnosis only if needed and authorized. Do not merge, run official signing, publish, or mutate hardware without the stated owner checkpoint.
