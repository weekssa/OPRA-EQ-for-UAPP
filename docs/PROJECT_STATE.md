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
- Latest fetched development branch during this audit: `main` at `35e91c2170786ae4afc7a6679ba9ba34a01511a8`. Since documentation closeout `0c8a594b07549aa7d7e8d9cd1fbf27004d48315d`, 22 automated catalog/source-currentness commits have advanced main. The current-main diff from the v0.8.3 tag is catalog/source-health and documentation only; `app/src`, `app/build.gradle.kts`, and the main manifest are unchanged from the release tag. Production version metadata remains `0.8.3` / code `14`.

The exact APK/source tuple above proves the public release identity. It does **not** prove which artifact is currently installed on an owner's phone.

## Installed device state

The v0.8.3 release records say the owner's Pixel retained the separate debug-signed Beta v1 package and its data after acceptance; the stable production APK was not installed over it. This audit's ADB query found no connected device. The current installed package, version, signing identity, and source/artifact match are therefore **UNKNOWN / NOT VERIFIED**. No device operation was performed. See [`v0.8.3 Beta state`](implementation/v0.8.3-defaults-beta/STATE.md) and [`release checklist`](implementation/v0.8.3-defaults-beta/RELEASE_CHECKLIST.md) for the historic report.

## Active work and known limits

- Open draft PR #96 reports `FINAL_EQ_MISMATCH` on JA11 Off-to-User 1 selection after Save; restoration of the original state is not verified. Mutation remains on hold. No replay is authorized by this status summary.
- Open draft PR #93 investigates JA11 post-Flash state acquisition; root cause and physical reproduction remain unresolved. Its owner phone authorization was revoked in that handoff.
- Current-main Dependabot run `39467268390` failed because its Gradle security-update job requested dependencies absent from the dependency snapshot. This is an automation/snapshot mismatch; investigate separately. Do not treat this as a failed Android CI build or as proof that the app is vulnerable or clear.
- Historical EW300, v0.7 and Favorite pull requests remain open for evidence/history; do not close them as part of routine maintenance.

See [`KNOWN_ISSUES.md`](KNOWN_ISSUES.md) and [`PUBLIC_RELEASE_CHECKLIST.md`](PUBLIC_RELEASE_CHECKLIST.md) for current trackers and detailed release state.

## Current validation snapshot

- v0.8.3 exact-main Android CI run `38648012952` passed all four jobs on release source `899755507f812c940f3ab9381a856c80c6030885`.
- Current-main source-health run `39471956137` and dependency-submission run `39466646926` succeeded on `35e91c2170786ae4afc7a6679ba9ba34a01511a8`.
- Current-main Dependabot run `39467268390` failed with the dependency snapshot error described above.
- No device validation was run in this audit.
- New source/PR SHAs require their own applicable checks. Historical checks do not transfer to a later candidate.

## Source-of-truth links

- Approved goals and constraints: [`GOALS_AND_SCOPE.md`](GOALS_AND_SCOPE.md)
- Development and release process: [`OPERATING_RUNBOOK.md`](OPERATING_RUNBOOK.md)
- Product architecture: [`ARCHITECTURE.md`](ARCHITECTURE.md)
- Detailed chronological product and release evidence: [`CHATGPT_PROJECT_RUNBOOK.md`](CHATGPT_PROJECT_RUNBOOK.md)
- Validation and release status: [`PUBLIC_RELEASE_CHECKLIST.md`](PUBLIC_RELEASE_CHECKLIST.md)
