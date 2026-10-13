COMPLETE — EXACT JA11 CONVERGENCE CANDIDATE QUALIFIED

The authorized mission is complete. All23 off-phone gates pass, PR94 is merged, the exact official signed v0.8.2 candidate is frozen, and **J031 physical convergence acceptance PASS** on that installed APK. One owner-controlled Flash used the original My EQs→AFUL Explorer→LoboNautics route. No restart, repeated Flash, manual reconnect/Refresh, editor Apply/Reset or DEVICE setting change was needed. No release/public tag was published; immutable public v0.8.1 is unchanged. Phone work and passive recording are finished.

## Exact physical result — J031

The video shows the original Explorer/LoboNautics detail stays visible through verification, real USB detach and the temporary fallback to UAPP/ToneBoosters. It returns to FiiO Connected/State current with the app's Flash-complete/saved-and-verified feedback. The earlier unwanted automatic return to the My EQs list did not recur.

System USB logs record detach17:40:19.375 and reattach17:40:20.886 on2026-10-13 local−0500. AppPID25666 is unchanged across pretest, post-Flash, editor/navigation, cleanup and recording end. Post-Flash My DAC shows Current hardware EQ, populated graph, all five bands and enabled Edit. Edit opens successfully with five active bands and is closed without Apply. DEVICE is Current/verified; navigation back to the original managed detail works.

All visible native-codec-precision EQ and DEVICE values match the fresh baseline: User1; gain−3.70; LS76/+4/Q.38; Peak3800/+3/Q.38; Peak1500/−1.5/Q1; Peak2700/+1/Q2.67; Peak200/−1/Q1.73; volume60/60; MicOn; UAC2;384k; firmware2.20/VIDPID2972:0102. No restoration Flash was needed. Only the temporary baseline Personal EQ and its verified task download/UI XML/media were removed; original selections/source remain. No restoration or cleanup is outstanding.

Independent Luna High verdict **PASS_WITH_LIMITS**; primary independently verifies actual installed-source/checksum/signature evidence, UI/PID/JSON comparisons, editor/navigation, cleanup and capture completion. This qualifies the requested visible same-process convergence. Direct-profile Flash has no production report exporter, so raw native packet values, wire Save/write/replay counts, internal session generations and ViewModel identity remain UNMEASURED. One owner Flash action is not a USB Save-count claim. Generic serial does not establish unique same-unit continuity; this was the sole returning supported JA11. Power-loss persistence or unrelated hardware capabilities were not tested.

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

Preserved pre-merge UI window-focus failures and the fixture race are described in EVIDENCE.md. The final merged-source suite passed on its first attempt. No assertions were weakened and no speculative emulator settings were added. Canonical gate count is **23 MET /0 UNMET /0 abandoned**; G20 is satisfied by verified exact artifact provenance and signed install/upgrade. Physical fix acceptance is **PASS J031** with the observation limits stated above.

## Durable evidence and closure

Plan checkpoint: `f880fef32789b282742251579af5e0a9c1eb8765`. Exact installed/source/artifact tuple remains in the table above. Nonunique evidence-bound physical-profile fingerprint: `18fc59c521ac87672c52d1d29253c2618b95261e6fa1591e62ee386edb964251` (not a unit identity).

Private J031 evidence: `/Users/stephenweeks/.codex/evidence/ja11-v082-physical-20261013/`, including before/after APKs, baseline/UI/PID captures, post-Flash comparison, editor/navigation/cleanup evidence, three stopped/pulled video segments, app/USB logs and independent review. Key records: `post-flash-verification.json` SHA256 `2756ecd98685676e58aa2356f38fb2e0476867d41960e48c3db852377cffd271`; `cleanup-verification.json` SHA256 `0eea781716bd392f7d65f5dcd8d45ab3e81fa67be03be24518e1c851636a0199`; `independent-video-review.json` SHA256 `4583edb7bd7289fd6078f2dc76ba9246074b65561fee7bc3545479278cfea862`. Raw phone identity/logs/private reports are not committed.

Exact-main CI evidence: `/Users/stephenweeks/.codex/evidence/ja11-main-9983-20261013/`. Signed install/upgrade evidence: `/Users/stephenweeks/.codex/evidence/ja11-signed-9983-20261013/`. The unchanged official APK/bundle remains read-only in Downloads. Earlier J030 was a separate healthy diagnostic control; its frozen draft PR93/report remains preserved and is not relabeled as this physical production acceptance. Original failures, fixture races and hosted focus failures remain historical evidence.

Sol owns canonical conclusions/integration; bounded Luna workhorses executed verification/import/cleanup and independent review. Canonical four-file mission checkpoint is on `codex/ja11-postflash-reconstruction`; production source is merged9983 and verifier toolingc5 remains separate/unmerged. No new APK/source revision follows from this evidence-only closeout. No remaining authorized mission action; no further phone test. Public tag/release/public support publication still requires separate owner approval and has not occurred.
