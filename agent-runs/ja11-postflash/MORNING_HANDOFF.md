EXACT SIGNED CANDIDATE VERIFIED — HOSTED INSTALL AND UPGRADE VALIDATION RUNNING

PR #94 is merged with owner approval. The minimal production fix, faithful RED/GREEN regression, fresh adversarial Luna High review and all exact-main source checks pass. The official signed APK is independently verified and frozen. Its hosted clean-install and v0.8.1 upgrade checks are still running; physical readiness remains NO until they pass. No release or public tag was published. No Pixel/JA11 access occurred during this software continuation.

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
- Exact official signed clean-install and v0.8.1 upgrade: **RUNNING**, [run38475878287](https://github.com/weekssa/OPRA-EQ-for-UAPP/actions/runs/38475878287), actual signed-candidate-smoke job115566320966. Candidate source9983 is distinct from verification workflow source `c5be0d9af579a8ba264884db2f13f81ad6785e1a`. Four ordinary mode jobs are intentionally skipped and never counted as passes. Tooling is isolated, read-only and unmerged; no publisher, tag creation or APK signing/rebuild occurs in it.

Preserved pre-merge UI window-focus failures and the fixture race are described in EVIDENCE.md. The final merged-source suite passed on its first attempt. No assertions were weakened and no speculative emulator settings were added. Canonical gate count remains **22 MET /1 UNMET /0 abandoned**; G20 awaits exact signed install/upgrade. Physical fix acceptance remains separately **NOT RUN**.

## Next physical gate after remaining software checks

Why a phone is needed: a hosted emulator can verify installation, storage, UI and simulated session/cancellation behavior, but cannot prove the JA11's real Save detach/re-enumeration and EQ/DEVICE convergence on this exact signed APK. One owner-controlled acceptance is required.

Wait for the owner to authorize a new session with `OWNER PRESENT — AUTHORIZE PHONE ACCESS AGAIN`. Then verify/install these exact bytes without clearing production data, verify installed identity, capture nonmutating current baseline and prepare evidence. Use the original My EQs managed-headphone/profile route, not the separate diagnostic Personal EQ route. Preserve current User1 program, five bands, gain and DEVICE settings. Compare intended Flash values with baseline; if they differ, establish an exact owner-approved restoration plan before any mutation. Do not improvise a second Flash.

At the final confirmation, stop for the owner to press Flash once. Keep the app open. Observe the same process through Save/reconnection: current replacement-session EQ and DEVICE, visible graph/filters, enabled and usable Edit EQ, live My DAC/navigation and no restart. Existing production operation reports can provide Save count, generations and final readback; the diagnostic lifecycle recorder remains disabled in release. UI recording alone does not prove USB/provenance facts.

At the first failure, preserve live evidence and stop: no second Flash, restart, unplug, manual recovery or settings change. No Mic/UAC/volume/preset/power-cycle campaign. Any required restoration or repeated mutation is separately owner-controlled.

## Preserved diagnostic and hardware boundary

J030 was one healthy control on isolated `.ja11trace`, sourcef6bc1d6d, v0.8.1-ja11trace/code12, with one Save, seven final generation2 reads and107 trace records/zero drops. Its baseline was verified restored and temporary source/profile removed; no restoration remains. It used a different Personal EQ route and does not qualify this production fix. Frozen draft PR93 and historical failures remain preserved. Diagnostic package/report remains installed; account for its separate storage at the next authorized session.

The phone was explicitly released after J030. No discovery, query, installation, launch, Flash, setting change or unplug has occurred during this software continuation. Public v0.8.1 remains unchanged. Publication remains prohibited.

Sol owns decisions, integration and these four canonical files; bounded Luna workhorses perform execution and independent review. Canonical branch `codex/ja11-postflash-reconstruction`, production branch `codex/fix-ja11-postflash-convergence-final`, verifier branch `codex/ja11-signed-candidate-validation`; checkpoints are pushed. Raw captures and private reports remain outside Git.
