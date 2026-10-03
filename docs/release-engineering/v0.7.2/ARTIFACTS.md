# v0.7.2 artifact and remote state

## Current release checkpoint (2026-10-03)

- PR #65 merged normally as `a60411bebfdbd1cea4218d3bde45013bb7ed26a9` at 2026-10-03 03:29:16 UTC. Merged `main` currently points to that SHA; its tree exactly equals reviewed and fully checked PR head `e7f2fc937e9b265770296dbdc4cbb40a4e5e13c9`.
- The exact PR head passed all eight checks: Android build/UI/API26 run `37092536385`; Analyze Kotlin and CodeQL run `37092536382`; Catalog `37092536383`; Priority community `37092536380`; dependency submission `37092533146`. Independent review returned PASS with no P0-P2 findings.
- Exact-run report artifacts: API35 UI `11263771217` (25/25), API26 smoke `11262861589`, lint `11263656445`. Debug APK `11262801785` is test evidence only.
- Post-merge local gate on `a60411be`: `:app:testDebugUnitTest --rerun-tasks`, `:app:lintDebug`, `:app:assembleDebug`, and `:app:assembleRelease` passed; 731 tests, 0 failures/errors/skips; lint 0 errors, 111 warnings, 2 hints. `bash tools/verify-r8-mapping.sh` passed. Actionlint 1.7.12 passed on all workflow YAML.
- Signed beta run `37093821378`, [workflow run](https://github.com/weekssa/OPRA-EQ-for-UAPP/actions/runs/37093821378), succeeded from exact main SHA `a60411bebfdbd1cea4218d3bde45013bb7ed26a9`, target `ja11`. Beta Actions artifact `11262918395` has GitHub digest `sha256:0354532be92b304249565c67f66a997c0c2efadc1cef9c70ef5aea3f184c011f`; APK SHA-256 is `9e74a8c35b15ce90585303e3a208b2be280ad2179cce34c23da4ace42a62c80a`, signer matches the pinned project fingerprint, package/version are `com.weekssa.opraeqforuapp` / `0.7.2` / code `9`, and manifest/R8 mapping agree. The hosted API 35 beta emulator installed and cold-launched it. This was the Signed EQ Library Beta Candidate workflow, not the promotion-required Signed Release Candidate workflow, so it is not eligible for release promotion.
- The beta workflow's separate publisher updated the temporary `mobile-test-apk` branch at commit `19ded401683b87ae56528ba5fc3af783cacec4bb`. This branch is a testing surface, not a GitHub Release or hardware qualification claim.
- Official Signed Release Candidate run `37095180116`, [workflow run](https://github.com/weekssa/OPRA-EQ-for-UAPP/actions/runs/37095180116), succeeded from `main` at exact SHA `a60411bebfdbd1cea4218d3bde45013bb7ed26a9` with tag `v0.7.2`. Artifact `11264251526` is named `EQ-Library-v0.7.2-signed-a60411bebfdbd1cea4218d3bde45013bb7ed26a9`, GitHub ZIP digest `sha256:1da409dcf47368ec254a5432e7c1d316473920c28e27c19416b10cc8e29969d9`, not expired, expires 2027-01-01. Candidate APK SHA-256 `f3afaa102a31491286828faa37cfe1454853721d1e4aa736da57bf4919e89ded`; package `com.weekssa.opraeqforuapp`, versionName `0.7.2`, versionCode `9`; pinned signer `65c1c1256dae3c49e3548f334c91f0ba991969e9be9e0b223ba4e253d2114747`; R8 mapping SHA-256 `2ef382f4838c49c89fbea9985fdba976bedef5202aa59711dcbe263690515021`. v2/v3 signature, zip alignment, APK checksum, exact candidate archive digest, mapping and release manifest passed independent local verification. The verifier downloaded and confirmed the latest public v0.7.1 baseline APK SHA-256 `abd8837f78aaf72d28abef3db956a1c171f791616effbc8f7875814c2c28002b`, versionCode 8 and same signer.
- Promotion run `37096259477`, [workflow run](https://github.com/weekssa/OPRA-EQ-for-UAPP/actions/runs/37096259477), ended failure on exact main SHA `a60411bebfdbd1cea4218d3bde45013bb7ed26a9`. Verify job `111126707279` passed, including clean API 35 install of public v0.7.1, exact candidate in-place upgrade, and cold launch. Tag job `111127079868` failed at the immutable artifact digest comparison; publish job `111127097619` was skipped. Diagnostics artifact `11264990554`; verified archive transfer artifact `11264736023` has digest `sha256:9a842620230c81b3d6725b2199a913678f97d0c2e98e197032b764b13ad33a09`.
- The local promotion verifier correctly rejected beta run `37093821378` with `candidate run was not produced by the signed release candidate workflow`; no tag or release mutation occurred. The official promotion run passed the API 35 gate but failed before any tag/ref or release draft was created.
- Root cause in the release publisher: `validate_github_candidate()` returns the GitHub digest with its `sha256:` prefix, while `command_tag()` normalizes only the expected input before equality comparison. The values differ only by prefix. Correct normalization and regression coverage, merge that correction, then build a fresh candidate from the new exact main SHA.
- Recovery fix is now in the working tree: both `command_tag()` and `command_publish()` use validated normalization of both digest inputs. Direct tag-command coverage reproduces the prefixed GitHub digest and confirms it reaches candidate tag creation. Focused promotion tests pass 39/39; the full Python tool suite passes 238/238 under bundled Python 3.12; release contract passes. Independent review and PR checks/merge are pending.
- Current public release remains v0.7.1. Remote `v0.7.2` tag is absent; no v0.7.2 GitHub Release exists.
- Live Dependabot review: 51 transitive Maven alerts (3 critical, 20 high, 26 medium, 2 low) belong to Gradle/build/emulator/test dependencies in the current SBOM. No alerted coordinate was found in the candidate app `releaseRuntimeClasspath`; mapped minified DEX search returned zero flagged package-class matches. Build-toolchain risk remains and requires a separate critical/high dependency remediation task; no broad upgrade was made here.
- No physical DAC writes were performed.

Earlier remote snapshots below are historical and superseded where this current checkpoint records later state.

## Source

- Repository: `weekssa/OPRA-EQ-for-UAPP`
- Worktree: `/Users/stephenweeks/.codex/worktrees/a79b/OPRA-EQ-for-UAPP`
- Branch: `codex/v0.7.2-stabilization`
- Base/tag: `v0.7.1`
- Base commit: `c48f6a5daa08a5e03475b2e415fe80b41d3357db`
- Starting metadata: versionName `0.7.1`, versionCode `8`
- Target metadata: versionName `0.7.2`, versionCode `9`
- Latest fetched `origin/main` at the last refresh: `afed3dc90b5873218d5e333882528f8c5ddd54a2` (2026-10-03)
- Latest main merge on this branch: `1530d02f`, with `afed3dc90b5873218d5e333882528f8c5ddd54a2` as its main parent; the v0.7.1 source remains an ancestor
- Product source candidate already in the PR: commit `1c36349ca3edb69061a34b44d385670380f60512`.
- Production source commit before the docs-only status sync is `e63fc4bf5be629135b0fd56449bad4c1b0bfd1b4`; recovery commits `40b4f5d8c7b255ebed7cc886158d6e6621a911c4` and `e63fc4bf5be629135b0fd56449bad4c1b0bfd1b4` are in the branch. Earlier PR snapshot `3ccf6728a9edd6c85a76ece1f5e71bc93fa37b74` passed its exact gates. Later exact head `e7f2fc937e9b265770296dbdc4cbb40a4e5e13c9` and its merge are recorded in the current checkpoint above.
- Physical DAC writes: none

## Local verification

- Fresh full Gradle gate on source commit `e63fc4bf5be629135b0fd56449bad4c1b0bfd1b4`: 731 JVM tests passed with 0 failures/errors/skips; lint passed with 0 errors, 111 warnings, and 2 hints; debug and release APK assembly passed.
- Focused `Ew300PersistenceQualificationTest`: 17/17 passed, including pre-write checkpoint failures and failed `UNCERTAIN` checkpoints after either baseline-restoration path.
- R8 mapping verification on source commit `e63fc4bf5be629135b0fd56449bad4c1b0bfd1b4` passed; at least one app class is renamed.
- Forced Kotlin compile passed; its two warnings are in unchanged files
- API 35 instrumentation passed 25 tests with 0 failures/errors/skips on `opra-v072-api35` (ARM64, emulator 37.1.11)
- PR-head emulator UI/API35 instrumentation on `3ccf6728`: 25 tests, 0 failures/errors/skips; report artifact ID `11262353847`, run `37090841176`.
- Python tool suite passed 235 tests; release contract, registry validation, and Favorite sample validation passed
- actionlint 1.7.12 passed all workflow YAML; ShellCheck 0.11.0 passed repository shell scripts
- Independent reviews of `af3f4c59` and `13bf1f20` returned FAIL with two P2 replay paths, each now fixed in the pushed `e63fc4bf` source. Exact review of `8db499af` and its corrected docs descendant `3ccf6728` returned PASS; this ledger sync's descendant needs a fresh review.

## API 26 smoke artifacts

- Exact-source smoke for commit `1c36349ca3edb69061a34b44d385670380f60512` used a fresh `-wipe-data` API 26 ARM64 emulator with a 48 MiB heap-growth limit. It rendered manufacturer, model, and profile lists; PID 4267 remained alive and resumed after the 60-second observation, with no AndroidRuntime errors.
- Unsigned minified v0.7.2/code 9 APK SHA-256: `67a2663d53cedc30ad3395117636e02891f86b9a98f4ada4c6e552a4b000480c`
- Temporary smoke-signed APK SHA-256: `ea4da8e12ff7ffd680f3e7d5345a86bb7df5af0bee2e2d61fa8c2a8d51e517fd`
- Temporary smoke signer certificate SHA-256: `bba3818c88f1c0faf315b02dc725ca5f53a28fe4f5d001085b616973754d7b50`; it is not the project release signer
- `aapt` verified package `com.weekssa.opraeqforuapp`, versionName `0.7.2`, versionCode `9`, and minSdk 26; `apksigner verify` passed v2/v3
- Final Dalvik allocation was 47,111 KiB of 49,152 KiB, leaving 2,041 KiB free
- Full exact-source evidence, the earlier dirty-source smokes, and one separately recorded contaminated attempt are under `.unlazy/v0.7.2-autonomous-release/evidence/api26-final/`
- The clean local ARM64 run used committed source but a temporary signer. It is not CI x86_64 or release candidate provenance. The separate exact PR CI x86_64 evidence below passes on `8db499af`.
- PR-head API 26 x86_64 smoke on `8db499af`: fresh wipe, minified release APK built and temporary smoke-signed, cold install succeeded, MainActivity launch returned `Status: ok`, Manufacturers appeared, and the app remained alive and resumed through a 60-second observation with no app-process AndroidRuntime error. Diagnostics artifact `11261199392`, run `37088331227`. The smoke-signed APK is not retained as release provenance.
- Downloaded UI debug APK from run `37088331227`: package `com.weekssa.opraeqforuapp`, versionName `0.7.2`, versionCode `9`, SHA-256 `bf18e80806756814bd30bd4a7d35e3c0615cb211689accc44a69995a8e367b9c`. It is CI test evidence only, not the release candidate.

## Historical exact-head snapshot (3ccf6728; superseded by the current checkpoint above)

- PR #65 was open, non-draft, and `CLEAN` against base `afed3dc90b5873218d5e333882528f8c5ddd54a2` at exact head `3ccf6728a9edd6c85a76ece1f5e71bc93fa37b74`.
- All eight GitHub checks passed on that exact head: Android build/UI/API 26 run `37090841176` (jobs `111110648767`, `111110648890`, `111110648953`); Analyze Kotlin run `37090841130` (job `111110648040`); CodeQL check `111111725873`; dependency submission run `37090838488` (job `111110640795`); Catalog run `37090841097` (job `111110647550`); Priority community run `37090841101` (job `111110647754`).
- Independent read-only review returned PASS on exact head `3ccf6728` against base `afed3dc90b5873218d5e333882528f8c5ddd54a2`, with no actionable P0-P2 issue or unsupported release claim. No review was posted to GitHub.
- Exact-head API 35 report artifact `11262353847` records 25 tests, 0 failures/errors/skips. API 26 report artifact `11261474879` records a fresh minified install, visible Manufacturers list, live PID, resumed MainActivity after the observation period, and no app-process AndroidRuntime error. Lint artifact `11262258835`. Debug APK artifact `11262004385` has SHA-256 `084e3e778023d9dd11dbe9e85a0531c7a08a1dabefb2b563c53f2279a2869caa`; it is CI test evidence only, not a release candidate.
- The later docs-only sync advanced this snapshot to `e7f2fc93`; it passed exact-head CI and independent review before merging normally as `a60411be`.

## Remote promotion state (historical; current state is recorded at the top)

- Prior PR branch heads `af3f4c596e99e0cf8dce408b982c641c6898de6a` and `13bf1f20002f32da622392980eea145a0eb2a776` are superseded by current source head `e63fc4bf5be629135b0fd56449bad4c1b0bfd1b4`.
- Pull request at the pre-merge snapshot: [#65](https://github.com/weekssa/OPRA-EQ-for-UAPP/pull/65), open, base `main` at `afed3dc90b5873218d5e333882528f8c5ddd54a2`
- All eight checks passed on exact PR head `af3f4c596e99e0cf8dce408b982c641c6898de6a`: Android build run `37082764610`, CodeQL run `37082764637`, Catalog run `37082764642`, Priority community run `37082764613`, dependency-submission run `37082761608`, and the associated UI/API 26 jobs. API 26 report artifact `11259384390` and emulator UI report artifact `11258319894` were downloaded and inspected. These results predate the current local recovery fix.
- Exact PR head `8db499af0212795d05b97d0439a0c82f462722b9`: all eight checks passed. Android build/UI/API26 run `37088331227`, Analyze Kotlin `37088331231`, CodeQL check `111104328833`, Catalog `37088331236`, Priority community `37088331239`, and dependency submission `37088326577`.
- Independent exact-head review of `8db499af0212795d05b97d0439a0c82f462722b9` returned PASS with no actionable P0-P2 issue or unsupported release claim.
- Follow-up independent review of docs-only head `adf871f2` found one P2 stale restart instruction, corrected in this release-ledger update. Review the resulting live head before merge.
- Earlier PR-head artifacts from `8db499af`: API 26 diagnostics `11261199392`, emulator UI reports `11261079560`, lint reports `11261154040`, and debug APK `11261034576`. They are test evidence only. Artifacts `11259384390` and `11258319894` predate both recovery fixes.
- Independent review of `af3f4c59` found replay risk in the post-cycle restore path. Review of `13bf1f20` found the same risk in `restoreBeforeCommitOrFail()`. Both paths now persist terminal `RESTORATION_ATTEMPTED` before their first baseline write; if the checkpoint fails, no baseline write is sent.
- Full local validation on `e63fc4bf`: 731 JVM tests, zero failures/errors/skips; lint 0 errors, 111 warnings, 2 hints; debug/release assembly and R8 mapping pass. The focused EW300 suite passes 17/17.
- At the pre-merge refresh, the active `Protect main` ruleset prevented deletion and non-fast-forward updates, with no bypass actors. It defined no required reviewer or named status checks; task CI and independent-review gates still applied.
- Merge SHA at the pre-merge snapshot: none
- Signed candidate workflow/run/artifact at the pre-merge snapshot: none
- Release APK SHA-256 at the pre-merge snapshot: none
- Release signer certificate SHA-256: expected pinned project identity, to be independently verified on the candidate
- Candidate manifest at the pre-merge snapshot: none
- Remote `v0.7.2` tag at the pre-merge snapshot: absent
- GitHub Release at the pre-merge snapshot: absent

The details in this historical section are immutable head-specific snapshots. Resolve live GitHub state before acting. Never copy a previous candidate checksum or signer verification forward as evidence for a changed source. Current candidate, tag, and public-release status are summarized at the beginning of this file.
