# v0.8.0 stable promotion — resume

Updated: 2026-10-08T06:34:37Z

## Current state

- Worktree: `/Users/stephenweeks/.codex/worktrees/v080-stable-promotion/OPRA-EQ-for-UAPP`
- Branch: `codex/v0.8.0-stable-promotion`
- Last refreshed `main`: `0f4236b64c6642b4cd7c1a0ffe2b0d9778330f01`, tree `e31fe181c012a5cd48d0573efe7e8a3d88e383a8`.
- Last exact app/test candidate verified by PR CI: `3e557e1a0f7868bc5cf54f9e363da0c9e3c41bbd`, tree `680564d2488e54c194488d67d8d006ba22a6ee3f`.
- Qualified beta merge/source: `4190c6ca51694ea0a80583a83fd3cb09b5088a7d`; the production app-source tree remains `857d02a53d0df44fb0bd46e5ddad3b319dc48dab` on both beta and stable-promotion candidate.
- PR [#73](https://github.com/weekssa/OPRA-EQ-for-UAPP/pull/73) is open and draft against `main`. Its last live head/base read was `3e557e1a0f7868bc5cf54f9e363da0c9e3c41bbd` / `0f4236b64c6642b4cd7c1a0ffe2b0d9778330f01`; all four exact-head PR workflows passed. This status-only checkpoint advances the branch; after it is pushed, refresh the new live PR head and applicable checks before marking ready or merging.
- GitHub `/releases/latest` currently resolves to stable `v0.7.2`; the published `v0.8.0-beta` remains a prerelease. The `v0.8.0` tag is absent from the current remote tag listing. Do not move latest before the ordered stable gates pass.

## Completed

- Audited main ancestry and classified the post-beta changes. The stable candidate preserves the qualified production app-source tree; its changes are version metadata, tests, release tooling, and documentation. No beta physical or production behavior gate is invalidated.
- Set stable metadata to package `com.weekssa.opraeqforuapp`, versionName `0.8.0`, versionCode `11` (beta code is `10`).
- Added curated stable release notes, a stable changelog entry, and a readable What's New formatting assertion.
- Extended the main-only stable promotion verifier to verify immutable beta provenance for the `v0.8.0` promotion, then run both signed persisted-state upgrade paths and a separate stable clean-install/core smoke.
- Local Python syntax, 52 release-publisher tests, publisher contract, workflow YAML parse, extracted API 35 shell syntax, lint, Android-test Kotlin compilation, and whitespace checks passed. A prior local JVM-test launch failed before assertions because the local Gradle cache lacks its generated worker bootstrap JAR; remote exact-head Android CI subsequently passed the unit-test step.
- Exact PR CI on `3e557e1a0f7868bc5cf54f9e363da0c9e3c41bbd` passed: Android CI run `37736600658` (unit tests, lint, debug/release assembly, R8 verification, API 26 smoke, API 35 UI), CodeQL run `37736600633`, Catalog currentness run `37736600706`, and Priority community coverage run `37736600726`.
- API 35 UI job `113177438418` passed 64/64 tests, with 0 failures, errors, or skips. Former failures `interruptedEqResetIsNotReplayedAndLeavesAnActionableRecoveryState` and `recoverySurvivesProductionMyDacTabAndRootNavigationWithoutReplayingCallbacks` passed after a test-only host/input harness correction. The UI report artifact is `11531339710`, digest `sha256:2692842828d81789c085ea2a09d52c5ad6566997a5c819142104190a60e31888`.
- API 26 `min-api-smoke` job `113177438121` passed; diagnostic artifact `11532521333` has digest `sha256:1ecf8c9203030da778b8e02d1e23cecd89628b0e379eb0a67e3adad64375db46`. Release assembly/minification verification passed in Android CI job `113177438410`.
- One independent reviewer passed the complete stable delta and its narrow test-only correction, finding no correctness blocker.
- Refreshed `releaseRuntimeClasspath` once. GitHub reported 56 repository vulnerabilities, while item-level alerts/current alert-to-runtime mapping could not be retrieved. The dependency caveat remains open and no clean scan or zero-runtime-alert claim is made.

## Next action

After pushing this status-only checkpoint, refresh live PR #73 head/base/checks. Require applicable checks on the resulting head to pass, synchronize the PR body with the exact results, mark it ready, and merge the exact reviewed candidate under repository rules. Verify the actual merged `main` SHA/tree before starting the main-only signed stable candidate workflow. Keep README current-version/download links on v0.7.2 until stable publication is independently verified. Do not use Pixel, ADB/USB, or DAC hardware.

## Preserve

- Do not modify the beta worktree or rewrite beta evidence.
- Preserve ignored `.unlazy/locks/` and `.unlazy/v080-stable-promotion/` evidence.
- Do not delete unrelated open PRs or user-owned untracked files.
- Do not publish stable while a required signed upgrade, clean install, signer, source, or public-asset gate is pending or failed.
