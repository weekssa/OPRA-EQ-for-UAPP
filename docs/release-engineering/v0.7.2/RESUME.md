# v0.7.2 release handoff

MISSION: stabilization and public release

RELEASE STATUS: complete and published. See [EQ Library v0.7.2](https://github.com/weekssa/OPRA-EQ-for-UAPP/releases/tag/v0.7.2).

REPOSITORY: `weekssa/OPRA-EQ-for-UAPP`

WORKTREE: `/Users/stephenweeks/.codex/worktrees/a79b/OPRA-EQ-for-UAPP`

BRANCH: `codex/v0.7.2-stabilization`

RELEASE SOURCE: `b8e90b9b53fc63ea00fefa499d7d4bd6ce4d55ea`; the annotated `v0.7.2` tag targets this exact commit.

VERSION: `0.7.2` / versionCode `9`; package `com.weekssa.opraeqforuapp`.

CANDIDATE: run `37099431204`, artifact `11265653006`, archive SHA-256 `dc2607ad3b8c43aae4b0d41502ff5f13ce03d8b7b635511941f8271be255ab33`.

PROMOTION: run `37099991693`; candidate verification, API 35 clean install of public v0.7.1, candidate in-place upgrade, and cold launch passed. It created the annotated tag and published the release.

APK: SHA-256 `efdd63ddb305d0624f805cc53e4ce27aae7d1ddeb169e8f965302d0f290ba64a`; signer SHA-256 `65c1c1256dae3c49e3548f334c91f0ba991969e9be9e0b223ba4e253d2114747`; R8 mapping SHA-256 `2ef382f4838c49c89fbea9985fdba976bedef5202aa59711dcbe263690515021`.

PUBLIC ASSETS: all six uploaded release assets were downloaded and matched GitHub's SHA-256 metadata; GitHub also lists two generated source archives. APK bytes match the candidate and published checksum; `/releases/latest` resolves to v0.7.2.

SOURCE CHECKS: PR #65 merged normally as `a60411bebfdbd1cea4218d3bde45013bb7ed26a9` after eight checks and independent review passed. PR #66 merged normally after all seven checks and independent code/documentation review passed; exact tested head `11275d510f3a0b9b1ad4aa4edae2c25bd2044b4b` has the release source tree. Python tests passed 238/238 and the promotion workflow contract passed.

HARDWARE: no physical DAC writes were performed. No new hardware support claim was added.

TRACKED RISK: 51 transitive Maven alerts remain in Gradle/build/emulator/test tooling. No flagged coordinate was found in the release runtime classpath or mapped minified DEX; build-environment risk remains for separate remediation.

HUMAN ACTION REQUIRED: NONE.

DOCUMENTATION FOLLOW-UP: the current branch contains the release-closeout ledger update. Review its diff, open a docs-only PR, and merge only after normal checks pass. This is post-publication documentation synchronization; no release, candidate, hardware, or publication action remains.
