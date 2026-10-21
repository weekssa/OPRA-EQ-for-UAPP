# Current Project State

Last verified: 2026-10-20. This is a concise state summary. The dated release and validation records linked below remain the detailed evidence.

## Public release and source identity

- Product: EQ Library / OPRA EQ for UAPP
- Android application ID: `com.weekssa.opraeqforuapp`
- Current public stable release: **v0.8.3**, versionCode `14`
- Public release: [v0.8.3](https://github.com/weekssa/OPRA-EQ-for-UAPP/releases/tag/v0.8.3), immutable stable release and `/releases/latest`
- Annotated tag: `v0.8.3` (object `1a584839b699ce2f0ff447582719fbeb19915392`)
- Release source: `899755507f812c940f3ab9381a856c80c6030885`
- Exact stable APK SHA-256: `396244ffc26cec108b64c523a49779261656b41ab02f1bc2b63197327c576247`
- Pinned signer SHA-256: `65c1c1256dae3c49e3548f334c91f0ba991969e9be9e0b223ba4e253d2114747`
- Full source/build/APK/checksum/signer/public asset evidence: [`release-engineering/v0.8.3-stable/ARTIFACTS.md`](release-engineering/v0.8.3-stable/ARTIFACTS.md)
- Latest `origin/main` verified for dependency assessment #101: `9ca5beb6f20a5dec0b6a9c466719e0299252cb44`, merge commit for PR #109. The dependency remediation does not change `app/src`, `app/build.gradle.kts`, the main manifest, or the public release baseline. Production version metadata remains `0.8.3` / code `14`.

The exact APK/source tuple above proves the public release identity. It does **not** prove which artifact is currently installed on an owner's phone.

## Installed device state

The v0.8.3 release records say the owner's Pixel retained the separate debug-signed Beta v1 package and its data after acceptance; the stable production APK was not installed over it. This audit's ADB query found no connected device. The current installed package, version, signing identity, and source/artifact match are therefore **UNKNOWN / NOT VERIFIED**. No device operation was performed. See [`v0.8.3 Beta state`](implementation/v0.8.3-defaults-beta/STATE.md) and [`release checklist`](implementation/v0.8.3-defaults-beta/RELEASE_CHECKLIST.md) for the historic report.

## Active work and known limits

- Open draft PR #96 reports `FINAL_EQ_MISMATCH` on JA11 Off-to-User 1 selection after Save; restoration of the original state is not verified. Mutation remains on hold. No replay is authorized by this status summary.
- Open draft PR #93 investigates JA11 post-Flash state acquisition; root cause and physical reproduction remain unresolved. Its owner phone authorization was revoked in that handoff.
- Current-main Dependabot run `39514901376` failed because its Gradle security-update job requested dependencies absent from the dependency snapshot. This is an automation/snapshot mismatch; investigate separately. Do not treat this as a failed Android CI build or as proof that the app is vulnerable or clear.
- Issue #101: after PR #109 merged, GitHub shows 49 of the original 56 Maven alerts fixed and seven still open (2 critical, 1 high, 4 moderate). Root `buildEnvironment` selects patched versions, but the dependency-submission report and `dependencyInsight` identify vulnerable selections in the app's separate `androidLintTool` and UTP result-listener configurations. A narrowly scoped continuation candidate pins those two configurations to patched HttpClient, Commons Lang, and Bouncy Castle versions. Local tests, lint, debug/release assembly and R8 mapping checks pass; exact-head CI/emulator tests and GitHub submission refresh are pending. See [`DEPENDENCY_SECURITY_ASSESSMENT.md`](security/DEPENDENCY_SECURITY_ASSESSMENT.md). The published v0.8.3 remains unchanged.
- Historical EW300, v0.7 and Favorite pull requests remain open for evidence/history; do not close them as part of routine maintenance.

See [`KNOWN_ISSUES.md`](KNOWN_ISSUES.md) and [`PUBLIC_RELEASE_CHECKLIST.md`](PUBLIC_RELEASE_CHECKLIST.md) for current trackers and detailed release state.

## Current validation snapshot

- v0.8.3 exact-main Android CI run `38648012952` passed all four jobs on release source `899755507f812c940f3ab9381a856c80c6030885`.
- On merged main `9ca5beb6f20a5dec0b6a9c466719e0299252cb44`, Dependency Submission run `39527844565` and Automatic Dependency Submission run `39527843547` succeeded, but the refreshed alert set remains seven open because it records the separate vulnerable Lint/UTP tool configurations.
- Standard Android CI run `39527843364` produced zero jobs; issue #102 tracks the unresolved workflow dispatch anomaly. Do not interpret this as failed Android build work or as the result of exact-head validation.
- Local continuation candidate checks passed: 799/799 JVM unit tests on retry; `:app:lintDebug`; `:app:assembleDebug`; `:app:assembleRelease` with R8 minification; and `bash tools/verify-r8-mapping.sh`. Exact-head CI and hosted emulator coverage are not yet available for the continuation branch.
- No device validation was run in this audit.
- New source/PR SHAs require their own applicable checks. Historical checks do not transfer to a later candidate.

## Source-of-truth links

- Approved goals and constraints: [`GOALS_AND_SCOPE.md`](GOALS_AND_SCOPE.md)
- Development and release process: [`OPERATING_RUNBOOK.md`](OPERATING_RUNBOOK.md)
- Product architecture: [`ARCHITECTURE.md`](ARCHITECTURE.md)
- Detailed chronological product and release evidence: [`CHATGPT_PROJECT_RUNBOOK.md`](CHATGPT_PROJECT_RUNBOOK.md)
- Validation and release status: [`PUBLIC_RELEASE_CHECKLIST.md`](PUBLIC_RELEASE_CHECKLIST.md)
