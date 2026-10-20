# Validation Report

Status: LOCAL AND PR VALIDATION PASSED FOR RECORDED HEAD\
Base: `origin/main` `35e91c2170786ae4afc7a6679ba9ba34a01511a8`

## Safety and provenance checks completed

- Full source checkout copied with macOS `ditto`; inventory/hash/xattr verification passed with 16,383 matching entries and no differences.
- Full-copy Git database passes `git fsck --full` with exit 0 and no missing/corrupt-object errors; dangling-object diagnostics are present. A separate `ditto` restore test opens at the saved HEAD and preserves the source shallow state and saved worktree status. The supplementary bundle verifies against the source for 291 refs but cannot independently clone past the source's shallow boundary; the full copy is the recovery artifact.
- Remote refs fetched without pulling, switching, merging, rebasing, or pruning the original checkout.
- Dedicated cleanup worktree/branch created from fetched `origin/main`; original checkout HEAD remains unchanged and original dirty state is preserved.
- `./tools/codex-android adb devices -l` returned no connected devices. No install, flash, uninstall, reset, or mutation was attempted.
- Git LFS checks: no `.gitattributes` file or `.git/lfs` directory exists in the fetched repository; global filters are configured, but the `git-lfs` CLI was unavailable to enumerate remote pointers. No submodules are configured.
- Public v0.8.3 release evidence matches maintained artifact record: source `899755507f812c940f3ab9381a856c80c6030885`; package `com.weekssa.opraeqforuapp`; version `0.8.3` / code `14`; APK SHA-256 `396244ffc26cec108b64c523a49779261656b41ab02f1bc2b63197327c576247`; signer SHA-256 `65c1c1256dae3c49e3548f334c91f0ba991969e9be9e0b223ba4e253d2114747`.
- GitHub Dependabot alert API reports 56 open Maven alerts (3 critical, 22 high, 28 moderate, 3 low), all attributed to `settings.gradle.kts`; this audit does not classify runtime packaging/reachability or remediate them.

## Final PR validation snapshot

- Branch ancestry, ahead/behind, patch-equivalence and dispositions are recorded; GitHub issue/PR/workflow and release inventories are recorded with issue #99 created.
- `python3 tools/validate-repository-foundation.py` passed, including canonical docs, PR template and every task-dossier Markdown relative link.
- `git diff --check` passed. Application source, Gradle build identity and main manifest match `origin/main` unchanged.
- `./tools/codex-android :app:testDebugUnitTest lintDebug assembleDebug` completed with `BUILD SUCCESSFUL` in 3m 24s; existing deprecation, Kotlin/lint and native-symbol-strip warnings only.
- Independent factual/sensitivity review found no remaining actionable findings; no local paths or raw private reports in reviewed changes.
- Original checkout HEAD and full `git status --short --branch` match the private preflight snapshot exactly.
- PR #100's `Analyze Kotlin`, `CodeQL`, and `submit-gradle` checks passed on `d065462bbfe3051a7391b400d98f08a75462093c`.
- Exact-head run [39503776205](https://github.com/weekssa/OPRA-EQ-for-UAPP/actions/runs/39503776205) passed all four jobs (`build`, `CodeQL exact head`, `min-api-smoke`, `emulator-ui-test`) on the exact d065 SHA.
- Ordinary push workflow run [39503670690](https://github.com/weekssa/OPRA-EQ-for-UAPP/actions/runs/39503670690) on d065 failed before creating any jobs; its job list is empty and logs return 404. Cause remains unknown. The same no-job failure appeared on two superseded candidate heads and is recorded in `GITHUB_TRIAGE.md`.
- No checks are pending or skipped on the completed d065 exact-head run. A final report-only commit still needs an exact-head run; consult PR #100 and issue #99 for the final head.

## Limits

- No physical-device verification. No device was connected.
- No exact installed APK extraction or package version readback.
- Existing baseline emulator/device evidence remains tied to its exact documented source and artifact; it is not transferred to the cleanup branch.
