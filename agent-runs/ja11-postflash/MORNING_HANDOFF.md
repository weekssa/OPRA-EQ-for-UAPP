PHYSICAL ACTION REQUIRED — PRESS FLASH ONCE

PR #94 is merged with owner approval. The minimal production fix, faithful RED/GREEN regression, fresh adversarial Luna High review and all exact-main source checks pass. The official signed APK is independently verified and frozen. Its exact signed clean-install and v0.8.1 upgrade checks pass. All 23 off-phone gates are MET; the candidate is ready for one physical acceptance, which remains NOT RUN. No release or public tag was published. No Pixel/JA11 access occurred during this software continuation.

New authorized physical preparation is complete: the exact frozen production APK is installed and independently pulled/verified, app data and previous APK preserved. Current Pixel9/API37 and sole JA11 baseline captured, PID25666. Current hardware EQ/DEVICE are populated and verified. The original My EQs→AFUL Explorer→LoboNautics final Flash confirmation is now open, Optimized8→5/gain−3.70. No hardware Flash has occurred in this session. Passive video/process-filtered/USB-tag evidence capture runs in session20261013T223839Z-e82bff0f, with a hard10-minute cap starting2026-10-13T22:38:39Z. If delayed beyond the capture window, owner should reply ready before pressing Flash so evidence readiness can be refreshed without repeating any mutation.

Concrete owner test/restoration plan: this original profile's five adapted coefficients are not previewed before Flash, so unchanged gain does not prove unchanged EQ. The current baseline is preserved as a task-only Personal EQ `JA11 v082 Original Baseline`; imported five bands/gain were reviewed against current UI and the restoration review confirms Exact/source values preserved/−3.70. Its review was canceled without hardware writes. By pressing the original profile's final Flash once, owner approves that one acceptance operation. After known success/current readback, if native EQ values differ from the baseline, pause for a separate owner-controlled restoration using the prepared Personal EQ. Never restore automatically, replay a failed operation, or perform restoration while state is unknown. At the first failure preserve evidence and stop. Task-only Personal/download cleanup remains pending until safe acceptance/restoration.

## Cause and correction

The regular-app video shows Save disconnect changing Automatic output from FiiO to UAPP, a transient empty My EQs library, automatic profile-screen removal, reconnect, current DEVICE values and a blank EQ tab. Real Compose/flasher and ViewModel/repository/session tests establish the architectural chain: output-key mismatch removed owned rows and disposed the screen-owned Flash caller; cancellation bypassed terminal operation publication and the existing completion read policy. Stranded Running FLASH suppressed EQ content while independent DEVICE could recover. Confidence is at least 95% for this architectural path. The original low-level null-field reason remains unknown; no diagnostic cancellation telemetry is attributed to the video.

The fix retains globally owned managed, Personal and general rows during output changes while derived export currentness stays keyed to the selected target. Existing JA11 Flash/EQ/DEVICE completion now runs in the retained ViewModel scope. Losing a screen await no longer cancels completion; actual ViewModel clearing still cancels and closes the session. No protocol command, mutation ordering/count, retry policy, delay, mutex, identity or security change. Black Pearl and EW300 runtime paths are unchanged.

## Exact production candidate

| Field | Verified value |
| --- | --- |
| Merged source SHA | `9983edf0b7fe437aeeec9ac3dcb3864705afa16a` |
| Reviewed pre-merge source | `a39965d34974edf704c351441038394191d052e9` |
| Production PR | [#94](https://github.com/weekssa/OPRA-EQ-for-UAPP/pull/94), MERGED with owner approval |
| APK | `EQ-Library-v0.8.2.apk` |
| APK SHA-256 | `df1ac78e7462da588e01f4aa39f5f23d0c05797d27eb2222d0650ce319c5d6e3` |
| Package / version / code | `com.weekssa.opraeqforuapp` / `0.8.2` / `13` |
| Official signer SHA-256 | `65c1c1256dae3c49e3548f334c91f0ba991969e9be9e0b223ba4e253d2114747` |
| Candidate-only signer | [run38474740277](https://github.com/weekssa/OPRA-EQ-for-UAPP/actions/runs/38474740277), attempt1, main, manual dispatch; actual job115562554099 PASS |
| Actions artifact | `11853910967`; expires2027-01-11T22:00:29Z |
| Original artifact ZIP SHA-256 | `c82b5490286d27cd3616740d6f9d8ce51c9f3ac598b1f40dac87ecaac1a76b88` |
| Embedded source / signature / alignment | Exact9983; v2/v3 signature verified; zipalign PASS |
| R8 mapping SHA-256 | `7afd37afd57430b52233a1efb3c1545540544649314381b275c7892698dae00a` |
| Artifact review | Independent Luna High APPROVE; primary verifies computed bytes and source tuple |

Frozen unchanged original ZIP/APK/manifest/checksum/idsig/signature/alignment/R8 outputs are read-only at `/Users/stephenweeks/Downloads/JA11-v0.8.2-candidate-9983edf0b7fe437aeeec9ac3dcb3864705afa16a/`. No rebuild or resign. Signing keys stayed in the unchanged trusted main-only workflow.

## Automated validation

- Faithful stable RED: test-only84965810 with runtime byte-identical to original main4b2d and final fixture identical to reviewed fix. It compiles/runs and fails with Running FLASH, stale EQ generation1/current DEVICE generation2, reads2 and exactly one Save. The same fixture is GREEN after the fix. Ownership-row regression also failed before correction.
- Focused54 tests, including five new lifetime/ownership cases, PASS. Full local and exact-main hosted unit tests: **777, zero failures/errors/skips**. Startup, screen loss, healthy acquisition, normal-null recovery, bounded exhaustion, actual owner clear, late publication and global ownership are covered.
- Lint: **zero errors,118 warnings,2 hints**. Debug/release/diagnostic/test assembly and R8 PASS.
- Exact merged-source [run38473044839](https://github.com/weekssa/OPRA-EQ-for-UAPP/actions/runs/38473044839), attempt1: all four actual jobs PASS. API26 minified-release launch PASS; API35 UI **68/68 PASS**, including all four new JA11 cases.
- Exact-main CodeQL analyses1947184551 and1947188327: zero findings, empty error. Catalog38473329555, community38473332667 and dependency checks PASS on exact9983.
- Fresh adversarial Luna High production review APPROVE. Reviewed validation tooling: six new and58 existing promotion tests, actionlint, compilation and diff checks PASS. Initial security-review blockers were corrected before integration/dispatch.
- Exact official signed clean-install and v0.8.1 upgrade: **PASS**, [run38475878287](https://github.com/weekssa/OPRA-EQ-for-UAPP/actions/runs/38475878287), actual signed-candidate-smoke job115566320966. Candidate source9983 is distinct from verification workflow source `c5be0d9af579a8ba264884db2f13f81ad6785e1a`. API35 x86_64 verified upgrade preserves the Personal EQ Room row, Manual DataStore preference and populated Library. Clean install checks first-run My EQs, Library, General EQs, Settings and crash buffer. APK SHA remains identical before/after and matches the frozen candidate. Evidence artifact11854541031 has ZIP SHA-256 `efdd5d39c7814c0eac7d5ed025e729244db6925b1ec96ee6646607f97e282c78`. Four ordinary mode jobs are intentionally skipped and never counted as passes. Tooling is isolated, read-only and unmerged; no publisher, tag creation or APK signing/rebuild occurs in it.

Preserved pre-merge UI window-focus failures and the fixture race are described in EVIDENCE.md. The final merged-source suite passed on its first attempt. No assertions were weakened and no speculative emulator settings were added. Canonical gate count is **23 MET /0 UNMET /0 abandoned**; G20 is satisfied by verified exact artifact provenance and signed install/upgrade. Physical fix acceptance remains separately **NOT RUN**.

## Required owner physical gate

Why phone access is required: a hosted emulator can verify installation, storage, UI and simulated session/cancellation behavior, but cannot prove the JA11's real Save detach/re-enumeration and EQ/DEVICE convergence on this exact signed APK. One owner-controlled acceptance is required.

The owner has now authorized a new session with `OWNER PRESENT — AUTHORIZE PHONE ACCESS AGAIN`. Verify/install these exact bytes without clearing production data, verify installed identity, capture nonmutating current baseline and prepare evidence. Use the original My EQs managed-headphone/profile route, not the separate diagnostic Personal EQ route. Preserve current User1 program, five bands, gain and DEVICE settings. Compare intended Flash values with baseline; if they differ, establish an exact owner-approved restoration plan before any mutation. Do not improvise a second Flash.

At the final confirmation, stop for the owner to press Flash once. Keep the app open. Observe the same process through Save/reconnection: current EQ and DEVICE, visible graph/filters, enabled and usable Edit EQ, live My DAC/navigation and no restart. Source review establishes that direct profile Flash has no production operation-report exporter; the available share card is for Editor Apply only. Do not perform another mutation to obtain a report. The diagnostic lifecycle recorder remains absent from this production candidate. UI/process/USB-event evidence must not be relabeled as native packet counts or internal generation telemetry.

At the first failure, preserve live evidence and stop: no second Flash, restart, unplug, manual recovery or settings change. No Mic/UAC/volume/preset/power-cycle campaign. Any required restoration or repeated mutation is separately owner-controlled.

## Preserved diagnostic and hardware boundary

J030 was one healthy control on isolated `.ja11trace`, sourcef6bc1d6d, v0.8.1-ja11trace/code12, with one Save, seven final generation2 reads and107 trace records/zero drops. Its baseline was verified restored and temporary source/profile removed; no restoration remains. It used a different Personal EQ route and does not qualify this production fix. Frozen draft PR93 and historical failures remain preserved. Diagnostic package/report remains installed; account for its separate storage at the next authorized session.

The phone was explicitly released after J030. No discovery, query, installation, launch, Flash, setting change or unplug has occurred during this software continuation. Public v0.8.1 remains unchanged. Publication remains prohibited.

Sol owns decisions, integration and these four canonical files; bounded Luna workhorses perform execution and independent review. Canonical branch `codex/ja11-postflash-reconstruction`, production branch `codex/fix-ja11-postflash-convergence-final`, verifier branch `codex/ja11-signed-candidate-validation`; checkpoints are pushed. Raw captures and private reports remain outside Git. Exact-main CI evidence is preserved at `/Users/stephenweeks/.codex/evidence/ja11-main-9983-20261013/`; signed install/upgrade evidence is preserved at `/Users/stephenweeks/.codex/evidence/ja11-signed-9983-20261013/`. Original failures remain append-only evidence. All off-phone work is complete; stop here until owner authorization.
