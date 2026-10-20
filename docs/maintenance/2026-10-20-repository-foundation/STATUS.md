# Repository Foundation Status

Last updated: 2026-10-20
Current phase: P10 final remote PR review and owner approval pending
Overall: PRE-MERGE REVIEW, NOT INTEGRATED

## Verified starting state

- Original checkout: exact local path is recorded in private audit files.
- Original branch/HEAD: `codex/ja11-protocol-evidence` / `dc34db33258159a7f73e92df0c792908c92166c8`; 17 commits behind its fetched remote-tracking ref, with extensive pre-existing untracked and ignored files.
- Cleanup worktree/branch: dedicated isolated worktree / `chore/repository-foundation-v0.8.3`.
- Cleanup base: live `origin/main` at `35e91c2170786ae4afc7a6679ba9ba34a01511a8`.
- Original checkout HEAD and reported dirty status remained unchanged after fetch/worktree creation.
- Private local full-copy backup verified by path/type/mode/size/SHA-256/xattr manifest: 16,383 entries, 14,131 regular files, no symlinks or special files, no missing/extra/different entries.
- Full-copy Git object database passes `git fsck --full` with exit code 0 and no missing/corrupt-object errors; dangling-object diagnostics are present. A separate `ditto` restore check opens at the original HEAD, preserves the shallow flag, and matches the original worktree status. The Git bundle verifies against the source and lists 291 captured refs, but cannot independently clone because the original repository is shallow and its bundle omits a parent beyond the shallow boundary. The full-copy backup, not the bundle, is the complete recovery copy for this source state. Git LFS filters are configured globally, but the `git-lfs` CLI is unavailable. No `.gitattributes` files or `.git/lfs` object store exist in the fetched repository; no repository-scoped LFS usage was found, though the CLI could not independently enumerate remote pointers. No submodules are configured.
- No suitable external backup drive was found; the only mounted volume observed was an Android Studio application volume.
- Android helper reported no connected ADB devices. No device command changed state.

## Current findings

- GitHub default branch is `main`; latest refreshed tip at the audit snapshot is `35e91c2170786ae4afc7a6679ba9ba34a01511a8`.
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
- P9: draft PR #100 exists. Previous head `e955f62db0bb7ab96182ee3964aa579a6cb9d186` exact-head CI attempt 2 passed all four jobs; current documentation correction advances that head and needs its own exact-head run.
- P12 tracking: issue #99 is the parent. Separate actionable follow-ups #101-#107 were created for security, push workflow, emulator flakiness, device provenance, backup retention, external instructions, and historical branch disposition.

## Remaining before merge

1. Finish stale evidence/status reconciliation in this dossier.
2. Complete the final read-only review of the actual remote PR head, including the full diff, active instructions/skills, project docs/runbooks, validator/workflow permissions, exact-head CI/checks, and ruleset.
3. Verify exact-head CI for the final PR SHA. Record successful, failed, skipped, pending and no-job outcomes on issue #99/PR #100; do not create a commit just to repeat those results.
4. Present GO/NO-GO and any true blockers. PR #100 remains draft and unmerged until explicit owner approval.

## Immediately after approved merge

- Verify the merge commit and actual GitHub default-branch tip.
- Create a clean local development worktree based on that merged default branch. Preserve the original Mac checkout, all its untracked/ignored files, and the verified backup.
- Verify root `AGENTS.md`, `docs/PROJECT_STATE.md`, `docs/OPERATING_RUNBOOK.md`, Android skill routing, and release guidance from the new development workspace.
- Recheck original checkout HEAD/status and backup references, record the merge SHA and post-merge checks, and update this mission state.
- Do not begin these steps before owner approval; no reset, overwrite, cleanup, branch deletion, or merge has occurred.

## Separate follow-ups, not PR #100 scope

- [#101](https://github.com/weekssa/OPRA-EQ-for-UAPP/issues/101) P1: assess all 56 Dependabot alerts, prioritizing the 3 critical; determine dependency paths and runtime reachability before proposing upgrades.
- [#102](https://github.com/weekssa/OPRA-EQ-for-UAPP/issues/102) P2: investigate the failed ordinary push workflow that produced zero jobs/no logs.
- [#103](https://github.com/weekssa/OPRA-EQ-for-UAPP/issues/103) P2: diagnose emulator Activity-focus test flakiness without masking assertions.
- [#104](https://github.com/weekssa/OPRA-EQ-for-UAPP/issues/104) P2: verify installed v0.8.3 device provenance only when the device is available and read-only access is freshly authorized.
- [#105](https://github.com/weekssa/OPRA-EQ-for-UAPP/issues/105) P3: determine safe external backup availability and retention/restore-check cadence.
- [#106](https://github.com/weekssa/OPRA-EQ-for-UAPP/issues/106) P3: reconcile obsolete external Codex/ChatGPT instructions while preserving unique history.
- [#107](https://github.com/weekssa/OPRA-EQ-for-UAPP/issues/107) P2: recommend disposition of unmerged historical branches; do not delete or merge automatically.

## Exact-head CI snapshot and live source

On previous PR head `e955f62db0bb7ab96182ee3964aa579a6cb9d186`, exact-head run [39507302003](https://github.com/weekssa/OPRA-EQ-for-UAPP/actions/runs/39507302003) attempt 2 passed all four jobs: `build`, `CodeQL exact head`, `min-api-smoke`, and `emulator-ui-test`; no job was skipped. Attempt 1 on the same SHA failed 8/72 emulator UI tests because Activity focus was false (0 skipped); attempt 2 passed all 72 without source changes. PR check-runs `Analyze Kotlin`, `CodeQL`, and `submit-gradle` passed with none pending or skipped. Ordinary push workflow [39507297134](https://github.com/weekssa/OPRA-EQ-for-UAPP/actions/runs/39507297134) failed with zero jobs and no downloadable logs (API 404); cause unknown.

The current corrected dossier commit advances the PR head and therefore requires a fresh exact-head run. Because reporting that result alone must not create another commit, issue #99 and PR #100 are the live record for the latest SHA and completed run. The active `Protect main` ruleset prevents deletion and non-fast-forward updates but defines no required review count or check contexts; the owner approval gate remains mandatory for this mission regardless of configured rules.

## Original checkout and branch preservation

The original checkout's baseline HEAD and dirty-status snapshot are recorded in private audit evidence. No existing branch was deleted, merged, or automatically archived during this mission. PR #100 remains unmerged. Recheck the original checkout after any future approved merge/workspace creation.
