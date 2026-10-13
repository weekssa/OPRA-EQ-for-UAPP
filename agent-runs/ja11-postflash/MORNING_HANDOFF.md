OWNER MERGE APPROVAL REQUIRED — SIGNED CANDIDATE BLOCKED

The JA11 architectural cause is established at >=95% confidence, the original failure state is reproduced automatically, the minimal production fix is implemented, fresh adversarial Luna High review approves it, and all pre-merge automated checks pass. The remaining blocker is explicit owner approval to merge [PR #94](https://github.com/weekssa/OPRA-EQ-for-UAPP/pull/94) and run the existing main-only candidate signer. No release, public tag, phone installation or further JA11 action has occurred during this software work.

## Cause and correction

The owner’s regular-app video shows Save disconnect changing Automatic output from FiiO to UAPP, a transient empty My EQs library, automatic profile-screen removal, reconnect, current DEVICE values and a literally blank EQ tab. Source and real Compose/flasher plus ViewModel/repository/session tests establish the chain: output-key mismatch removed owned rows and disposed the screen-owned Flash caller; cancellation bypassed terminal operation publication and the existing completion read policy; stranded Running FLASH suppressed EQ content while independent DEVICE could recover. Original low-level null-field attribution remains unknown. Confidence concerns this architectural path, not captured cancellation telemetry or a statistical estimate.

The fix retains globally owned managed/Personal/general rows during output changes while derived export currentness remains keyed to the selected target. The existing JA11 Flash/EQ/DEVICE completion now runs in the retained ViewModel scope; losing a screen await does not cancel it. Actual ViewModel clearing still cancels and closes the session. No protocol, USB command, Save count, retry policy, delay, mutex, identity or security change. Black Pearl and EW300 runtime paths are unchanged.

## Exact reviewed software

| Field | Verified state |
| --- | --- |
| Production source SHA | `a39965d34974edf704c351441038394191d052e9` |
| Branch | `codex/fix-ja11-postflash-convergence-final` |
| Worktree | `/Users/stephenweeks/.codex/worktrees/ja11-flash-owner-fix/OPRA-EQ-for-UAPP` |
| PR | [#94](https://github.com/weekssa/OPRA-EQ-for-UAPP/pull/94), open/draft, attached, clean merge state |
| Main incorporated / current remote | `dbefc0fe225ff13031ef99323f4391a9a978ed02` / `d193eb35db862b728075d32890fe5f65b40fb36d`; later drift is catalog-only |
| Prospective package/version/code | `com.weekssa.opraeqforuapp` / `0.8.2` / `13` |
| Qualified production APK SHA-256 | **NOT CREATED** |
| Official signer required | `65c1c1256dae3c49e3548f334c91f0ba991969e9be9e0b223ba4e253d2114747` |
| Signed provenance/install/upgrade | **NOT RUN / blocked by merge and official signing** |
| Physical fix acceptance / ready | **NOT RUN / NO** |

Fresh Luna High source review APPROVE on this exact SHA. Initial ownership-row P2 and the later deterministic fixture barrier were independently reviewed and corrected. There is no remaining source-review blocker. Official signing is guarded by [AGENTS.md](/Users/stephenweeks/.codex/worktrees/ja11-flash-owner-fix/OPRA-EQ-for-UAPP/AGENTS.md:141) (owner merge approval) and the unchanged [candidate workflow](/Users/stephenweeks/.codex/worktrees/ja11-flash-owner-fix/OPRA-EQ-for-UAPP/.github/workflows/github-release.yml:20) (main only). No feature signing bypass.

## Automated evidence

- Stable desired-behavior RED at exact test-only `84965810e0f7cf79a63f7474995b0645a0e676f5`: runtime equals original main4b2d3e67; fixture byte-identical to final candidate. It compiles/runs and fails with Running FLASH, reads2, stale EQgen1, current DEVICEgen2 and exactly one Save. This is preserved evidence, not a candidate. Global-ownership regression also failed before correction.
- Final focused five lifetime/ownership cases and full local/hosted **777 tests: zero failures/errors/skips**. Cases cover initial startup, caller loss, healthy first read, normal-null recovery, bounded exhaustion, actual owner clear and global row retention. Primary independently reran two critical cases and parsed reports.
- Lint **zero errors,118 warnings,2 hints**. Debug/release/diagnostic/test APK assembly, R8 mapping, actionlint and existing signing-promotion contracts pass. Local and CI debug/unsigned APKs are unqualified validation artifacts.
- Exact-head [run38468082823](https://github.com/weekssa/OPRA-EQ-for-UAPP/actions/runs/38468082823), attempt2: all four real jobs PASS on unchanged source. API26 minified-release launch passes. API35 UI **68/68 PASS**, including all four new JA11 cases.
- Exact-head CodeQL analysis1946956720 binds the exact source/ref/workflow/category and reports **zero findings, empty error**. Standard PR CodeQL, catalog, community and dependency checks pass.
- Luna and primary independently verified exact artifact IDs, API ZIP digests/CRCs, unit/UI XML counts, source guards and API26 launch. Final UI artifact11851224635 ZIP SHA-256 `44d38af9f26f72cf1d94a5beaad9d2db8c9cb27d05b9e55b5d680cee6e48f5c2`.

Initial final-head UI attempt had eight existing Activity-window-focus failures; all four new cases passed. Only that failed job reran with unchanged source and assertions. Original artifact11850992419/logs remain preserved. Emulator evidence does not prove a power/keyguard cause; no speculative CI setting change was added. Earlier hosted unit failure exposed a test precondition race, not grounds for a production change: a stale-wait was already satisfied by Flash entry. Final fixture explicitly waits for the real disconnect collector before reconnecting; the same fixed fixture remains RED on original runtime. Historical failures are never relabeled green.

Durable private software evidence: `/Users/stephenweeks/.codex/evidence/ja11-final-software-a39965d-20261013/`; stable RED evidence: `/Users/stephenweeks/.codex/evidence/ja11-stable-red-20261013/`. EVIDENCE.md contains exact artifact/run/job hashes and boundaries. Canonical count: **22 MET / 1 UNMET / 0 abandoned**; G20 signed-candidate provenance includes the pending exact signed install/upgrade checks. Physical acceptance is separate.

## Next authorized work after owner approval

Refresh live main/PR head and merge only the reviewed PR. Record and validate the resulting exact main SHA, then dispatch existing candidate-only signing for v0.8.2. Independently verify workflow/run/artifact/source/embedded provenance/APK SHA/package/version/code/pinned certificate and freeze unchanged candidate bytes. Validate the exact signed APK’s clean install and v0.8.1 upgrade on a hosted emulator using the existing read-only archive/APK verifiers and smoke helper. A reviewed validation-only tooling branch can reuse the registered workflow; distinguish its workflow SHA from candidate source SHA. **Do not dispatch the full promotion workflow:** it proceeds from smoke tests to public tag creation/publication. No new signing path or weakened publisher guard.

Only after those artifact gates pass should the handoff begin `PHYSICAL PHONE ACCESS REQUIRED — WAITING FOR OWNER` with the exact signed tuple and one bounded owner-controlled acceptance procedure. No phone action is needed for the current merge gate.

## Preserved physical/diagnostic boundary

J030 was one owner-authorized healthy diagnostic control on frozen source `f6bc1d6d87b9618bc35354f33103504c753c7f6a`, APK SHA-256 `4b00ab3e1c23e435b6ca848a1a0123068ac65e41a00f2fdbae1b1cb6fae796f9`, package `.ja11trace`, version0.8.1-ja11trace/code12, debug signer `5857646924badad4df93d8f953a57792cff75662c99d3af808d910d4081170fc`. [PR93](https://github.com/weekssa/OPRA-EQ-for-UAPP/pull/93) remains frozen/draft, not a fix or release.

That control recorded one Save, seven final generation2 reads, complete EQ/Edit UI and107 trace records/zero drops. Exact original EQ/settings/program were verified restored and temporary profile/source removed; no restoration remains. The original failure was not physically reproduced in that healthy run. The later video establishes the visible failure sequence; owner identifies regular app, but the video embeds no APK provenance. Pixel was explicitly released. No further discovery, query, install, launch, Flash, DAC setting change or unplug occurred in this software continuation. Public v0.8.1 remains unchanged.

Canonical files are on `codex/ja11-postflash-reconstruction`. The app archived checkouts during a pause; pushed checkpoints and the fixture snapshot were recovered. Production worktree is active/restored, canonical filesystem and Git checkpoints remain usable, and the obsolete RED worktree is archived after evidence retention. Signing keys, raw hardware captures and private reports are never committed.
