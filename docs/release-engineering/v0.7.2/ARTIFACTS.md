# v0.7.2 artifact and remote state

## Published release checkpoint (2026-10-03)

- Public release [v0.7.2](https://github.com/weekssa/OPRA-EQ-for-UAPP/releases/tag/v0.7.2), release ID `402346895`, is published, non-draft, non-prerelease, and returned by `/releases/latest`.
- The annotated `v0.7.2` tag targets exact release source `b8e90b9b53fc63ea00fefa499d7d4bd6ce4d55ea`. Its tag object is `d04a10895d56dcc96218ea3056bc5f593ea9b2fe`; it binds candidate run `37099431204`, artifact `11265653006`, and archive SHA-256 `dc2607ad3b8c43aae4b0d41502ff5f13ce03d8b7b635511941f8271be255ab33`.
- PR #65 merged normally at `a60411bebfdbd1cea4218d3bde45013bb7ed26a9`. PR #66 fixed the promotion digest comparison and merged normally; exact checked head `11275d510f3a0b9b1ad4aa4edae2c25bd2044b4b` has the same tree as release source `b8e90b9b53fc63ea00fefa499d7d4bd6ce4d55ea`. Its seven checks and independent code/documentation review passed.
- Official candidate run [37099431204](https://github.com/weekssa/OPRA-EQ-for-UAPP/actions/runs/37099431204) succeeded on exact source `b8e90b9b53fc63ea00fefa499d7d4bd6ce4d55ea`, producing artifact `11265653006`. The downloaded archive digest matched GitHub metadata.
- Candidate APK is package `com.weekssa.opraeqforuapp`, versionName `0.7.2`, versionCode `9`, SHA-256 `efdd63ddb305d0624f805cc53e4ce27aae7d1ddeb169e8f965302d0f290ba64a`. Pinned signer SHA-256 is `65c1c1256dae3c49e3548f334c91f0ba991969e9be9e0b223ba4e253d2114747`; R8 mapping SHA-256 is `2ef382f4838c49c89fbea9985fdba976bedef5202aa59711dcbe263690515021`. v2/v3 signature, zip alignment, manifest, checksum, archive digest, mapping, and provenance were independently verified.
- Promotion run [37099991693](https://github.com/weekssa/OPRA-EQ-for-UAPP/actions/runs/37099991693) passed exact candidate verification, clean API 35 install of public v0.7.1, candidate in-place upgrade, and cold launch. It created the annotated tag and published the release.
- All six uploaded release assets were downloaded and each file's SHA-256 matched GitHub's metadata. GitHub also lists two generated source archives. The public APK bytes match the candidate and checksum file. `/releases/latest` resolves to v0.7.2.
- Verified public asset SHA-256 values:
  - `apksigner-verification.txt`: `ad34f86ffe5fb59711128eb260f44bcce0fad0813d13a73a2a13fd9a711ccccb`
  - `candidate-manifest.json`: `90477d83c53658b3e0866cc04068ad5335756abc0834e3c77b9b7f6400a95e1e`
  - `EQ-Library-v0.7.2.apk`: `efdd63ddb305d0624f805cc53e4ce27aae7d1ddeb169e8f965302d0f290ba64a`
  - `EQ-Library-v0.7.2.apk.sha256`: `7596348f9d0d1bbc43a3936e7b0ac473d8a3762895b83845ed85db3fe8ba227d`
  - `release-provenance.json`: `0c8fe1e3df23968a97064e394f75d012184b8a2fcc8eb138508a3e947952ccde`
  - `zipalign-verification.txt`: `83ca947ae8659afc6e41b81ffcc470ba829e64f24362d15244f91775ffaf2d51`
- Historical failed promotion run `37096259477` stopped before tag creation because GitHub returned a prefixed archive digest and the verifier normalized only one comparison operand. PR #66 corrected this. Its fresh candidate and promotion runs passed; no tag mutation occurred in the failed run.
- Historical beta run `37093821378` remains testing evidence only and is not promotion eligible.
- Local Python tool suite passed 238/238 under bundled Python 3.12; the promotion contract passed. Registry validation passed for 15 sources; Favorite sample validation passed for 14 samples, 13 profiles, and 2 explicit exclusions.
- 51 open transitive Maven Dependabot alerts (3 critical, 20 high, 26 medium, 2 low) map to Gradle/build/emulator/test dependencies. None was found in v0.7.2 `releaseRuntimeClasspath` or mapped minified DEX. This does not eliminate build-toolchain risk; track it separately.
- No physical DAC writes were performed and no new hardware-support claim was made.

Earlier remote snapshots below are historical and superseded where this published-release checkpoint records later state.

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
