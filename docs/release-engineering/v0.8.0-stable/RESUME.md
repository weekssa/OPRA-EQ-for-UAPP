# v0.8.0 stable promotion — resume

Updated: 2026-10-08T05:36:14Z

## Current state

- Worktree: `/Users/stephenweeks/.codex/worktrees/v080-stable-promotion/OPRA-EQ-for-UAPP`
- Branch: `codex/v0.8.0-stable-promotion`
- Base HEAD/tree: `0f4236b64c6642b4cd7c1a0ffe2b0d9778330f01` / `e31fe181c012a5cd48d0573efe7e8a3d88e383a8`
- Expected exact beta source parent: `4190c6ca51694ea0a80583a83fd3cb09b5088a7d`
- App production source tree is unchanged from beta: `857d02a53d0df44fb0bd46e5ddad3b319dc48dab`.
- Draft PR [#73](https://github.com/weekssa/OPRA-EQ-for-UAPP/pull/73) is open against `main` from `codex/v0.8.0-stable-promotion`. Its live head/checks must be refreshed after this checkpoint update; no merge SHA, stable candidate APK, stable tag, or stable release exists yet.
- Live GitHub latest is stable `v0.7.2`; immutable prerelease `v0.8.0-beta` remains published; `v0.8.0` tag is absent.

## Completed

- Audited main ancestry and classified all post-beta commits: the only post-beta tracked changes are publisher workflow/tooling and release documentation from PR #72. No `app/src/main` change occurred.
- Set Gradle metadata to `0.8.0` / code `11`.
- Added curated `docs/releases/v0.8.0.md` and the stable changelog entry.
- Extended the main-only stable promotion verifier to download and verify the immutable beta APK for v0.8.0 only, then execute both required persisted-state upgrades and a separate candidate clean-install/core smoke. Later stable tags use the latest stable baseline and clean install without depending on the historical v0.8.0-beta artifact.
- Added stable What's New formatter coverage against the prepared release-note format.
- Local Python syntax check, promotion verifier suite (52 tests), publisher contract check, workflow YAML parse, extracted API 35 workflow shell syntax check, and `git diff --check` passed at 2026-10-08T05:33:15Z. The new tests prove generic stable tags skip beta lookup and v0.8.0 requires the exact beta baseline. The Android UI test has not yet run on the candidate; exact-head CI must run it.
- Refreshed `releaseRuntimeClasspath` once. The push response reported 56 repository vulnerabilities; the public Dependabot page returned 404 and the item-level runtime mapping remains unavailable. See `MISSION.md`; do not claim a clean scan.
- The three independent readiness reports were parent-reviewed and their Unlazy leaf/node records now pass. Report SHA-256 values: workflow `0094f7f03122a4772fdabf9601236e4d654e4b5ab04496dbfd0b003ac0f9e486`; upgrade `84386e8d006161da70db76b67014ba5ce9c609f5fd28364b24c5dffb54a7e03e`; docs/dependency `8506d15cb8858754c0f8d11d1dd97ad77eba4a9cd558cd481fae78b615e3a39a`.
- Local checks: Python compilation, all 52 promotion tests, promotion contract, YAML parse, extracted workflow `bash -n`, diff check, `:app:lintDebug --max-workers=2`, and `:app:compileDebugAndroidTestKotlin --max-workers=2` passed. The combined local `:app:testDebugUnitTest :app:lintDebug` invocation failed before unit tests executed: 124 Gradle Test Executor JVMs reported `ClassNotFoundException: worker.org.gradle.process.internal.worker.GradleWorkerMain`; no test assertion failure was reported. A reviewer traced this to the missing generated Gradle worker JAR in the local cache. This is not a passing unit-test result; exact-head GitHub CI remains required.
- Stable notes and changelog were polished to describe interruption recovery in user-facing terms without reset/recovery protocol jargon.

## Next action

Refresh PR #73's exact head/base/checks, complete one independent review of that exact candidate, resolve only demonstrated findings, and merge after all ordered pre-merge gates pass. Do not retry the local Gradle worker-launch failure in the unchanged cache; remote exact-head unit CI is authoritative for G7. Do not update README current-version links until the stable public artifact is independently verified. Do not use devices or hardware. After merge, use the repository's main-only signed-candidate and promotion workflows in the order in `MISSION.md`.

## Preserve

- Do not modify the beta worktree or rewrite beta evidence.
- Preserve ignored `.unlazy/locks/` and `.unlazy/v080-stable-promotion/` evidence.
- Do not delete unrelated open PRs or user-owned untracked files.
- Do not publish stable while a required signed upgrade, clean install, signer, source, or public-asset gate is pending or failed.
