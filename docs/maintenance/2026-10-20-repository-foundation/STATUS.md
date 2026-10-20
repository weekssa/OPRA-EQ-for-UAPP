# Repository Foundation Status

Last updated: 2026-10-20\
Current phase: P9 draft PR and delivery\
Overall: IN PROGRESS

## Verified starting state

- Original checkout: exact local path is recorded in private audit files.
- Original branch/HEAD: `codex/ja11-protocol-evidence` / `dc34db33258159a7f73e92df0c792908c92166c8`; 17 commits behind its fetched remote-tracking ref, with extensive pre-existing untracked and ignored files.
- Cleanup worktree/branch: dedicated isolated local worktree / `chore/repository-foundation-v0.8.3`
- Cleanup base: live `origin/main` at `35e91c2170786ae4afc7a6679ba9ba34a01511a8`.
- Original checkout HEAD and reported dirty status remained unchanged after fetch/worktree creation.
- Private local full-copy backup verified by path/type/mode/size/SHA-256/xattr manifest: 16,383 entries, 14,131 regular files, no symlinks or special files, no missing/extra/different entries.
- Full-copy Git object database passes `git fsck --full` with exit code 0 and no missing/corrupt-object errors; dangling-object diagnostics are present. A separate `ditto` restore check opens at the original HEAD, preserves the shallow flag, and matches the original worktree status. The Git bundle verifies against the source and lists 291 captured refs, but cannot independently clone because the original repository is shallow and its bundle omits a parent beyond the shallow boundary. The full-copy backup, not the bundle, is the complete recovery copy for this source state. Git LFS filters are configured globally, but the `git-lfs` CLI is unavailable. No `.gitattributes` files or `.git/lfs` object store exist in the fetched repository; no repository-scoped LFS usage was found, though the CLI could not independently enumerate remote pointers. No submodules are configured.
- No suitable external backup drive was found; the only mounted volume observed was an Android Studio application volume.
- Android helper reports no connected ADB devices. No device command changed state.

## Current findings

- GitHub default branch is `main`; latest refreshed tip is `35e91c2170786ae4afc7a6679ba9ba34a01511a8`.
- Public release/tag `v0.8.3` is immutable stable; annotated tag object `1a584839b699ce2f0ff447582719fbeb19915392` peels to release source `899755507f812c940f3ab9381a856c80c6030885`.
- Official stable APK tuple is package `com.weekssa.opraeqforuapp`, version `0.8.3` / code `14`, SHA-256 `396244ffc26cec108b64c523a49779261656b41ab02f1bc2b63197327c576247`, signer SHA-256 `65c1c1256dae3c49e3548f334c91f0ba991969e9be9e0b223ba4e253d2114747`.
- Release documentation says the owner's phone retains the separate debug-signed Beta v1 package/data. Current installed package cannot be read because ADB sees no device. Exact installed source/artifact is UNKNOWN for this task.
- The fetched base `origin/main` root `AGENTS.md` still called v0.7.0/code 7 the current EW300 milestone; the cleanup branch corrects that stale line.
- Current main contains 22 automated catalog/source-currentness commits after v0.8.3 documentation closeout. The current-main diff from the release tag is catalog/source-health and documentation only; app source, Gradle identity and main manifest match the release tag. Live CI and open-PR details are recorded in `GITHUB_TRIAGE.md`.

## Completed

- P0 read-only preflight, local baseline record, remote fetch, isolated worktree.
- P1 verified local backup, private integrity manifest, full-copy `git fsck`, and separate full-copy restore check. The supplementary bundle's shallow-boundary limitation is explicit.
- Read full active root instructions and project runbook; read attached operating runbook as task specification.
- Read Android baseline and Unlazy skills; no device mutation or production source edit.
- P2-P5 branch/release provenance, skills/documentation and GitHub/CI/release inventories completed; issue #99 is the tracking item.
- P6-P7 canonical documentation and future development/release workflow consolidated; existing CI now runs the documentation validator.
- P8 documentation/scope checks and `./tools/codex-android :app:testDebugUnitTest lintDebug assembleDebug` passed. Independent read-only review found no remaining actionable findings.

## Remaining

- Refresh and record PR #100 CI on the final documented candidate head.
- Update issue #99 and final delivery records after the final PR check results.
- Do not merge or publish; leave final integration as an owner decision.

## Checkpoint

Current cleanup HEAD at draft PR creation: `33066be171b4bb2d0639341019a9c7c52cdc9415`\
Current branch status at draft PR creation: clean. The authored and pushed maintenance commits are `9de1b4d509fb6de0d97f63ed36ad1dcc81e34c9e` and `33066be171b4bb2d0639341019a9c7c52cdc9415`. Draft PR #100 is open; exact-head CI refresh remains pending.
