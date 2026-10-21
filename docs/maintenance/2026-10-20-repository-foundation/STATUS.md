# Repository Foundation Status

Last updated: 2026-10-21
Current phase: P0-P10 original foundation complete; P11 complete; dossier synchronization pending review
Overall: ORIGINAL FOUNDATION DoD SATISFIED; CURRENT MAIN DOSSIER CORRECTION PENDING

## Current reconciliation (2026-10-21)

- Canonical evidence remains this dated folder: `docs/maintenance/2026-10-20-repository-foundation/`. The P10-pending/P11-not-started text still visible on current `main` is a stale 2026-10-20 checkpoint, not a template or a new active gate. Draft PR #108 contains the post-merge correction but is based on `1599ab451f711d8a27adfefbcba10293cd53dd84` and GitHub currently reports it conflicting (`mergeable=false`, `mergeable_state=dirty`). Do not merge or rebase without owner authorization.
- Current GitHub `main`: `779ef3bb56702c94b49a5769c1ad6f160ba2246e`.
- Mac development worktree `/Users/stephenweeks/Documents/Codex/OPRA-EQ-development`: branch `codex/development-main`, HEAD `6047d45d0e8377f29bc9deb5580191ae44b37793`, tracking `origin/main`, 19 commits behind at refresh. Two pre-existing untracked files remain untouched. The original checkout remains separately preserved at `dc34db33258159a7f73e92df0c792908c92166c8` with its original user-owned state.
- Original P0-P10 Definition of Done is satisfied. PR #100 merged with owner approval; P11 post-merge verification and initial Mac handoff completed. Follow-up backlog is not part of the original foundation DoD.
- Issues #101, #102, and #103 are CLOSED. #104's authorized read-only Pixel 9 provenance check found package `com.weekssa.opraeqforuapp`, version `0.8.3` / code `14`, and on-device APK SHA-256 exactly matching public v0.8.3; confidence is high for artifact byte identity. The signer/source association follows the exact public release artifact record; neither was independently read from the installed app. Draft PR #116 is open; its validator check failed because `PROJECT_STATE.md` must preserve an installed-device evidence-boundary statement, its emulator job failed while preparing a corrupt/incomplete SDK image, and its CodeQL/API 26 checks passed. No APK was extracted and no device was modified.
- #105 external backup availability is still a separate protection improvement; the verified full-copy local recovery backup satisfies original P1. No suitable external volume is currently available. #106 and #107 remain separate follow-ups. #114 and #117 are independent backlog; #117 must not become a foundation acceptance gate.
- Draft PR #111 is the #101 documentation closeout, separate from this mission. Draft PR #116 carries the later #104 state and exact-reviewed-head approval rule. Neither is merged.

## Verified starting state (historical 2026-10-20 preflight; unchanged where stated)

- Original checkout: exact local path is recorded in private audit files.
- Original branch/HEAD: `codex/ja11-protocol-evidence` / `dc34db33258159a7f73e92df0c792908c92166c8`; 17 commits behind its fetched remote-tracking ref, with extensive pre-existing untracked and ignored files.
- Cleanup branch: `chore/repository-foundation-v0.8.3`, merged and retained.
- Pre-merge cleanup base: `35e91c2170786ae4afc7a6679ba9ba34a01511a8`.
- Post-merge development worktree: local branch `codex/development-main`, tracking `origin/main` at `1599ab451f711d8a27adfefbcba10293cd53dd84`, clean after validation.
- Original checkout HEAD and reported dirty status remained unchanged after fetch/worktree creation.
- Private local full-copy backup verified by path/type/mode/size/SHA-256/xattr manifest: 16,383 entries, 14,131 regular files, no symlinks or special files, no missing/extra/different entries.
- Full-copy Git object database passes `git fsck --full` with exit code 0 and no missing/corrupt-object errors; dangling-object diagnostics are present. A separate `ditto` restore check opens at the original HEAD, preserves the shallow flag, and matches the original worktree status. The Git bundle verifies against the source and lists 291 captured refs, but cannot independently clone because the original repository is shallow and its bundle omits a parent beyond the shallow boundary. The full-copy backup, not the bundle, is the complete recovery copy for this source state. Git LFS filters are configured globally, but the `git-lfs` CLI is unavailable. No `.gitattributes` files or `.git/lfs` object store exist in the fetched repository; no repository-scoped LFS usage was found, though the CLI could not independently enumerate remote pointers. No submodules are configured.
- No suitable external backup drive was found; the only mounted volume observed was an Android Studio application volume.
- Android helper reported no connected ADB devices. No device command changed state.

## Original audit findings (historical 2026-10-20 snapshot; superseded where the 2026-10-21 reconciliation above differs)

- GitHub default branch is `main`; PR #100 merged by merge commit `1599ab451f711d8a27adfefbcba10293cd53dd84`. Its tree equals validated PR head `ccec0184742d7798778bcb91d173e4a7ea44531e`.
- Public release/tag `v0.8.3` is immutable stable; annotated tag object `1a584839b699ce2f0ff447582719fbeb19915392` peels to release source `899755507f812c940f3ab9381a856c80c6030885`.
- Official stable APK tuple is package `com.weekssa.opraeqforuapp`, version `0.8.3` / code `14`, SHA-256 `396244ffc26cec108b64c523a49779261656b41ab02f1bc2b63197327c576247`, signer SHA-256 `65c1c1256dae3c49e3548f334c91f0ba991969e9be9e0b223ba4e253d2114747`.
- Release documentation says the owner's phone retained the separate debug-signed Beta v1 package/data. Current installed package cannot be read because ADB sees no device. Exact installed source/artifact is UNKNOWN / NOT VERIFIED.
- The fetched base `origin/main` root `AGENTS.md` still called v0.7.0/code 7 the current EW300 milestone; the cleanup branch corrects that stale line.
- Current main contains 22 automated catalog/source-currentness commits after v0.8.3 documentation closeout. The current-main diff from the release tag is catalog/source-health and documentation only; app source, Gradle identity, and main manifest match the release tag. Live CI and open-PR details are recorded in `GITHUB_TRIAGE.md`.

## Completed phases

- P0: read-only preflight, original baseline record, remote fetch, isolated worktree.
- P1: verified local backup, private integrity manifest, full-copy `git fsck`, and separate non-destructive full-copy restore check. The supplementary bundle's shallow-boundary limitation is explicit.
- P2-P5: branch/release provenance, skills/documentation and GitHub/CI/release inventories.
- P6-P7: canonical documentation and future development/release workflow consolidated; existing CI now runs the documentation validator.
- P8: documentation/scope checks and `./tools/codex-android :app:testDebugUnitTest lintDebug assembleDebug` passed. The PR diff contains no application source, Gradle, manifest, signing, dependency, or release changes.
- P9: PR #100 was reviewed at exact head `ccec0184742d7798778bcb91d173e4a7ea44531e`, passed exact-head CI and all PR checks, and merged with owner approval at `1599ab451f711d8a27adfefbcba10293cd53dd84`.
- P10: owner approval was received; PR was marked ready and merged with a normal merge commit.
- P11: verified actual `main` tip and merge tree; created clean local development worktree `codex/development-main`; instructions and release guidance are present; repository validator and documented Android checks passed. Original checkout and backup remain unchanged.
- P12 tracking: issue #99 is the parent. Separate actionable follow-ups #101-#107 were created for security, push workflow, emulator flakiness, device provenance, backup retention, external instructions, and historical branch disposition.

## Merge status

PR #100 is merged and no pre-merge gate remains. See issue #99 and the post-merge GitHub evidence below. The zero-job push workflow anomaly and Dependabot updater failure remain separate follow-up work.

## Post-merge verification

- GitHub PR #100 merged at `1599ab451f711d8a27adfefbcba10293cd53dd84`; `main` points to that commit and its tree equals the validated PR head.
- Clean local development worktree `codex/development-main` tracks `origin/main` at the merge commit. Root `AGENTS.md`, project state, operating runbook, release guidance, and Android skill routing are present.
- `python3 tools/validate-repository-foundation.py` passed. The documented Android command `./tools/codex-android :app:testDebugUnitTest :app:lintDebug :app:assembleDebug` completed successfully in 55 seconds.
- Original checkout remains on `codex/ja11-protocol-evidence` at `dc34db33258159a7f73e92df0c792908c92166c8`, with preflight status unchanged. The verified backup and private manifest remain untouched. No historical branch or original file was deleted.

## Separate follow-ups, not original foundation DoD

- #101-#103 are closed; historical issue prompts and descriptions are not active instructions to repeat completed work.
- [#104](https://github.com/weekssa/OPRA-EQ-for-UAPP/issues/104) P2: read-only physical-device provenance verified; documentation draft PR #116 remains under review and must satisfy current validator/CI.
- [#105](https://github.com/weekssa/OPRA-EQ-for-UAPP/issues/105) P3: determine safe external backup availability and retention/restore-check cadence.
- [#106](https://github.com/weekssa/OPRA-EQ-for-UAPP/issues/106) P3: reconcile obsolete external Codex/ChatGPT instructions while preserving unique history.
- [#107](https://github.com/weekssa/OPRA-EQ-for-UAPP/issues/107) P2: recommend disposition of unmerged historical branches; do not delete or merge automatically.
- #114: separate Automatic Dependency Submission/KSP investigation.
- #117: separate DAC evidence-planning issue; not part of this foundation.

## Exact-head CI snapshot and live source

At the preceding PR checkpoint, head `e955f62db0bb7ab96182ee3964aa579a6cb9d186` exact-head run [39507302003](https://github.com/weekssa/OPRA-EQ-for-UAPP/actions/runs/39507302003) attempt 2 passed all four jobs: `build`, `CodeQL exact head`, `min-api-smoke`, and `emulator-ui-test`; no job was skipped. Attempt 1 on the same SHA failed 8/72 emulator UI tests because Activity focus was false (0 skipped); attempt 2 passed all 72 without source changes. Ordinary push workflow [39507297134](https://github.com/weekssa/OPRA-EQ-for-UAPP/actions/runs/39507297134) failed with zero jobs and no downloadable logs (API 404); cause unknown. The latest reviewed PR head and its exact-head results are in issue #99 and PR #100; do not infer them from this earlier snapshot.

Exact PR-head run [39513136561](https://github.com/weekssa/OPRA-EQ-for-UAPP/actions/runs/39513136561) on `ccec0184742d7798778bcb91d173e4a7ea44531e` passed all four jobs with no failures, skips, or pending jobs; PR checks Analyze Kotlin, CodeQL, and submit-gradle passed. The merge commit has the identical tree. On merge SHA `1599ab451f711d8a27adfefbcba10293cd53dd84`, Android push run [39514881784](https://github.com/weekssa/OPRA-EQ-for-UAPP/actions/runs/39514881784) failed before creating any jobs (zero-job list; logs unavailable), and Dependabot security update run [39514901376](https://github.com/weekssa/OPRA-EQ-for-UAPP/actions/runs/39514901376) failed with `dependency_not_found` because requested dependencies were absent from the snapshot. These remain follow-ups #102 and #101 respectively; neither involved an application edit. Main CodeQL run [39514883158](https://github.com/weekssa/OPRA-EQ-for-UAPP/actions/runs/39514883158) completed successfully on the merge commit. The active `Protect main` ruleset prevents deletion and non-fast-forward updates but defines no required review count or check contexts.

## Original checkout and branch preservation

The original checkout's baseline HEAD and dirty-status snapshot are recorded in private audit evidence. No historical branch was deleted, merged, or automatically archived during this mission. The authorized cleanup branch was merged through PR #100 and retained. The original checkout and verified backup were not altered.
