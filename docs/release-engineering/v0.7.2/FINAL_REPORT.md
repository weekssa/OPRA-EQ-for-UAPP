# v0.7.2 final report

STATUS: COMPLETE AND PUBLICLY RELEASED

MISSION: v0.7.2 stabilization, validation, signing, and public promotion completed.

BASELINE: v0.7.1, commit `c48f6a5daa08a5e03475b2e415fe80b41d3357db`, versionName `0.7.1`, versionCode `8`.

REPOSITORY: `weekssa/OPRA-EQ-for-UAPP`.

WORKTREE: `/Users/stephenweeks/.codex/worktrees/a79b/OPRA-EQ-for-UAPP`; isolated branch `codex/v0.7.2-stabilization`. The owner's primary checkout was not modified.

RELEASE: EQ Library `0.7.2`, versionCode `9`, package `com.weekssa.opraeqforuapp`. Public source commit `b8e90b9b53fc63ea00fefa499d7d4bd6ce4d55ea`. The annotated `v0.7.2` tag targets that exact commit. [Public release](https://github.com/weekssa/OPRA-EQ-for-UAPP/releases/tag/v0.7.2), release ID `402346895`, is non-draft, non-prerelease, and latest.

SOURCE CHANGES: fixed API 26 cold-start memory failures in canonical-catalog loading/projection; independently measured dense EQ response and improved optimizer/editor headroom and final-fit checks; persisted terminal restoration-attempt markers before EW300 baseline restoration writes; and repaired prefixed Actions artifact digest comparison in release promotion. No protocol command bytes or hardware identity authorization changed.

DSP VERIFICATION: an independent RBJ response oracle measured a 4.392811513 dB miss for one high-Q peak and an 8.683458274 dB miss for coincident peaks on the old 96-point grid. Dense generated-headroom and final-fit validation now rejects unsafe coarse-grid results. Exact maxima, frequency boundaries, coincident boosts, shelves, and hardware target quantization have regression coverage.

PULL REQUESTS: PR #65 merged normally as `a60411bebfdbd1cea4218d3bde45013bb7ed26a9` after eight required checks and independent review passed. PR #66 fixed digest normalization and merged normally at `b8e90b9b53fc63ea00fefa499d7d4bd6ce4d55ea`; its exact checked head `11275d510f3a0b9b1ad4aa4edae2c25bd2044b4b` passed all seven required checks and independent code/documentation review.

AUTOMATED VALIDATION: exact PR #65 head passed API 35 UI instrumentation (25/25) and a fresh API 26 x86_64 minified cold-install, catalog-readiness, and 60-second survival smoke. Post-merge local Gradle gates on product source passed 731 JVM tests, lint (0 errors, 111 warnings, 2 hints), debug and release assembly, and R8 mapping verification. Python tools passed 238/238 on the digest-fix tree; the promotion contract passed. Registry validation passed 15 sources. Favorite sample validation passed 14 samples, 13 profiles, and 2 explicit exclusions.

SIGNED CANDIDATE: official candidate run [37099431204](https://github.com/weekssa/OPRA-EQ-for-UAPP/actions/runs/37099431204), artifact `11265653006`, exact source `b8e90b9b53fc63ea00fefa499d7d4bd6ce4d55ea`, archive SHA-256 `dc2607ad3b8c43aae4b0d41502ff5f13ce03d8b7b635511941f8271be255ab33`.

RELEASE APK: `EQ-Library-v0.7.2.apk`, SHA-256 `efdd63ddb305d0624f805cc53e4ce27aae7d1ddeb169e8f965302d0f290ba64a`. Pinned signer SHA-256 `65c1c1256dae3c49e3548f334c91f0ba991969e9be9e0b223ba4e253d2114747`. R8 mapping SHA-256 `2ef382f4838c49c89fbea9985fdba976bedef5202aa59711dcbe263690515021`. Independent verification passed v2/v3 signature, zip alignment, package/version/manifest, APK checksum, candidate archive digest, signer, mapping, and provenance.

PROMOTION: run [37099991693](https://github.com/weekssa/OPRA-EQ-for-UAPP/actions/runs/37099991693) verified the exact candidate; API 35 clean install of public v0.7.1, in-place candidate upgrade, and cold launch passed. It created the annotated tag and published the release. The failed earlier promotion run `37096259477` stopped before tag creation; PR #66 fixed its cause, and the corrected fresh candidate/promotion passed.

PUBLIC ASSETS: all six uploaded release assets were downloaded and their bytes matched GitHub's SHA-256 metadata; GitHub also lists two generated source archives. Public APK bytes match the candidate and published checksum. `/releases/latest` resolves to v0.7.2.

HARDWARE SAFETY: no physical DAC writes, Save, Flash, Restore, or Reset were performed. No new DAC-support claim is made.

REMAINING TRACKED RISK: 51 transitive Maven Dependabot alerts (3 critical, 20 high, 26 medium, 2 low) remain in Gradle/build/emulator/test tooling. None was found in the v0.7.2 app `releaseRuntimeClasspath` or mapped minified DEX. Build-environment risk remains and requires separate remediation.

DOCUMENTATION CLOSEOUT: this report is part of the documentation-only synchronization. Its normal PR checks are post-publication maintenance; all release gates are complete.

HUMAN ACTION REQUIRED: NONE.
