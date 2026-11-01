# Current Project State

Workstream last verified: 2026-10-30. Public/installed tuples below retain their dated evidence. This is a concise state summary. The dated release and validation records linked below remain the detailed evidence.

## v0.9.0-beta.2 recovery workstream (2026-11-01, in progress)

- Candidate build: **v0.9.0-beta.2**, versionCode `16`

The owner-approved [recovery amendment](releases/v0.9.0/RECOVERY_AMENDMENT.md) supersedes only the
beta.1 release *ordering* for this workstream: a private, locally test-signed beta (package
`com.weekssa.opraeqforuapp.v090beta`) is prepared and qualified before merge or official signing.
Public stable remains v0.8.3/code14, and the official beta.1 candidate below remains historical
evidence rather than a superseded claim.

The recovery extends native program/volume persistence from User 1 to all five JA11 programs with no
microphone writes, adds the digital reconstruction-filter selector below EQ program, and applies the
runtime-then-Set-as-Default save policy with separate runtime, saved and unknown startup states.
Computer evidence and the 2026-11-01 physical session are recorded in
[the recovery report](releases/v0.9.0/RECOVERY_REPORT.md) and J034 of the JA11 ledger. Physical
result: User 1 + volume survived genuine power removal; **built-in program persistence failed** and
Save(3) rewrote the stored User 1 bank, which was then restored exactly and verified. The native
all-program path is unsafe; the beta.1 User 1-only restriction was correct. Merge, official signing
and publication remain owner-gated.

## v0.9.0-beta.1 workstream (2026-10-30)

- Historical candidate build: **v0.9.0-beta.1**, versionCode `15`

PR [#119](https://github.com/weekssa/OPRA-EQ-for-UAPP/pull/119) merged normally into `main` as merge commit `0e378a3266ba3e5bfd08ad8a69c9c0909386cd2c` from approved head `333bd5d6948383547d0ae6b7eb16f3e00b9c6162` (baseline `35f0c90a0e55e701d4343ed978a440d34aae13aa`). All PR-head hosted contexts passed. Main-only signing run `41167513158` produced the official beta candidate `EQ-Library-v0.9.0-beta.1-beta-0e378a3.apk` (SHA-256 `845c42e77180aa0a5f36eae9cb95c9c765be294e17ae58829c1b12d102c09f79`, signer `65c1c1256dae3c49e3548f334c91f0ba991969e9be9e0b223ba4e253d2114747`), which installed and launched on API 35 and upgraded in place from public v0.8.3. Public stable remains v0.8.3/code14; the development target is 0.9.0-beta.1/code15. This does not change the public stable tuple below. Physical persistence and exact restoration remain NOT RUN, so the candidate is not yet ready for beta. The [canonical plan/seal](releases/v0.9.0/PLAN.md), [complete checklist](releases/v0.9.0/REQUIREMENTS.md), and [execution report](releases/v0.9.0/EXECUTION_REPORT.md) govern current candidate status.

Native User1 defaults is implemented for computer qualification with unchanged fresh staging/Apply/one Save and no microphone writes. Other programs use the explicit mic round trip. All five optional filters completed research but remain individually deferred for insufficient exact-device evidence. No candidate hardware, signer or release readiness claim is made. Exact-PR merge approval precedes main-only signing; final physical qualification/restoration and separate publication approval remain required. Historical PR #93/#96 and retired controller tasks are preserved and are not resumed.

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
- Historical `origin/main` checkpoint after PR #110 (2026-10-21, before PR #111): `41aba8a08ce18f7df2342e4cce3b09b00858e6ef`. This is not the current main tip. PR #111 later merged the dependency-alert documentation closeout; see [`repository foundation evidence`](maintenance/2026-10-20-repository-foundation/EVIDENCE_INDEX.md). The dependency remediation does not change `app/src`, `app/build.gradle.kts`, the main manifest, or the public release baseline. Production version metadata remains `0.8.3` / code `14`.

The exact APK/source tuple above proves the public release identity. It does **not** prove which artifact is currently installed on an owner's phone.

## Installed device state

The v0.8.3 release records captured the Pixel's separate debug-signed Beta v1 state on 2026-10-14; that remains a valid historical snapshot. A later owner-authorized, read-only ADB verification on 2026-10-21 identified the connected physical Pixel 9 separately from `emulator-5554` and queried only `com.weekssa.opraeqforuapp`. Package metadata reported versionName `0.8.3`, versionCode `14`, and one installed `base.apk`. Its SHA-256, computed on-device without pulling the APK, exactly matched the public v0.8.3 APK SHA-256: `396244ffc26cec108b64c523a49779261656b41ab02f1bc2b63197327c576247`.

This is **HIGH confidence** that the installed bytes are the exact public stable v0.8.3 APK. The pinned signer SHA-256 is `65c1c1256dae3c49e3548f334c91f0ba991969e9be9e0b223ba4e253d2114747`; its match is established through the exact APK-byte identity and release provenance, not a direct certificate digest read from the device. The installed artifact is tied by the official release record to source `899755507f812c940f3ab9381a856c80c6030885`; no source revision was read from the installed application itself. No APK extraction, private app-data access, unrelated-app query, device-serial recording, DAC access, or device/app modification occurred. See [`issue #104 verification`](maintenance/2026-10-21-issue-104/VERIFICATION.md), the [v0.8.3 artifact record](release-engineering/v0.8.3-stable/ARTIFACTS.md), and the historic [v0.8.3 Beta state](implementation/v0.8.3-defaults-beta/STATE.md).

## Foundation closeout

The original foundation phases P0-P10 and P11 are evidenced complete. PR #108, PR #116, and PR #111 are merged. PR #111 records the verified dependency remediation: all 56 original Dependabot alerts are fixed, with 0 open and 0 dismissed after default-branch dependency submission. The installed Pixel 9 v0.8.3 APK SHA-256 remains the exact public release APK digest documented above; release source provenance remains distinct from what was directly read on-device. The merge milestones and evidence limits are recorded in the dated [foundation evidence index](maintenance/2026-10-20-repository-foundation/EVIDENCE_INDEX.md).

## Active work and known limits

- Open draft PR #96 reports `FINAL_EQ_MISMATCH` on JA11 Off-to-User 1 selection after Save; restoration of the original state is not verified. Mutation remains on hold. No replay is authorized by this status summary.
- Open draft PR #93 investigates JA11 post-Flash state acquisition; root cause and physical reproduction remain unresolved. Its owner phone authorization was revoked in that handoff.
- Current-main Dependabot run `39514901376` failed because its Gradle security-update job requested dependencies absent from the dependency snapshot. This is an automation/snapshot mismatch; investigate separately. Do not treat this as a failed Android CI build or as proof that the app is vulnerable or clear.
- Issue #101: PR #110 merged normally as `41aba8a08ce18f7df2342e4cce3b09b00858e6ef`. Default-branch Dependency Submission run `39538830388` succeeded and refreshed the graph; GitHub now reports all 56 original alerts fixed, with 0 open and 0 dismissed (0 critical, high, moderate, or low). Alerts #35, #50, #51, #1, #8, #17, and #18 are fixed. The submitted graph and official Gradle extraction show patched selections in both `androidLintTool` and `unified-test-platform-android-test-plugin-result-listener-gradle`. Exact-head Android CI passed on `f94d90050b75dc33dd577bc3c1f09f0b084f29ff` before merge. Issue #101 is closed after verification. Issue #102 remains open for the separate zero-job standard Android CI run. See [`DEPENDENCY_SECURITY_ASSESSMENT.md`](security/DEPENDENCY_SECURITY_ASSESSMENT.md). The published v0.8.3 remains unchanged.
- Historical EW300, v0.7 and Favorite pull requests remain open for evidence/history; do not close them as part of routine maintenance.

See [`KNOWN_ISSUES.md`](KNOWN_ISSUES.md) and [`PUBLIC_RELEASE_CHECKLIST.md`](PUBLIC_RELEASE_CHECKLIST.md) for current trackers and detailed release state.

## Current validation snapshot

- v0.8.3 exact-main Android CI run `38648012952` passed all four jobs on release source `899755507f812c940f3ab9381a856c80c6030885`.
- At the historical 2026-10-21 main checkpoint after PR #110 (`41aba8a08ce18f7df2342e4cce3b09b00858e6ef`), default-branch Dependency Submission run `39538830388` and Automatic Dependency Submission run `39538861347` succeeded. The graph contained patched selections in both affected AGP tooling configurations. PR #111 later recorded the final alert readback: all 56 original alerts fixed (0 open, 0 dismissed).
- Exact-head Android CI run `39534623261` passed on PR head `f94d90050b75dc33dd577bc3c1f09f0b084f29ff`, including build/unit/lint/debug and release assembly with R8 checks, CodeQL, API 35 emulator UI tests (72/72), and API 26 release smoke. Standard Android CI run `39538829244` on merged main produced zero jobs; issue #102 tracks the separate workflow failure. Do not interpret it as failed Android build work or as replacing the passing exact-head validation.
- Local continuation candidate checks passed: 799/799 JVM unit tests on retry; `:app:lintDebug`; `:app:assembleDebug`; `:app:assembleRelease` with R8 minification; and `bash tools/verify-r8-mapping.sh`. Exact-head CI run `39533178884` passed build/unit/lint/debug+release/R8, CodeQL, API 35 emulator tests (72/72), and API 26 release smoke on `ac799df6a471eefd7d806fe6da768995d8f65999`. One prior code-equivalent run had hosted emulator window-focus failures; the final-head run passed. Dependency Submission run `39533178861` succeeded but explicitly did not update default-branch dependency results; the post-merge submission and alert results are recorded above.
- No device validation was run in this audit.
- New source/PR SHAs require their own applicable checks. Historical checks do not transfer to a later candidate.

## Source-of-truth links

- Approved goals and constraints: [`GOALS_AND_SCOPE.md`](GOALS_AND_SCOPE.md)
- Development and release process: [`OPERATING_RUNBOOK.md`](OPERATING_RUNBOOK.md)
- Product architecture: [`ARCHITECTURE.md`](ARCHITECTURE.md)
- Detailed chronological product and release evidence: [`CHATGPT_PROJECT_RUNBOOK.md`](CHATGPT_PROJECT_RUNBOOK.md)
- Validation and release status: [`PUBLIC_RELEASE_CHECKLIST.md`](PUBLIC_RELEASE_CHECKLIST.md)
