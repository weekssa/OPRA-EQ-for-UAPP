# v0.7.2 artifact and remote state

## Source

- Repository: `weekssa/OPRA-EQ-for-UAPP`
- Worktree: `/Users/stephenweeks/.codex/worktrees/a79b/OPRA-EQ-for-UAPP`
- Branch: `codex/v0.7.2-stabilization`
- Base/tag: `v0.7.1`
- Base commit: `c48f6a5daa08a5e03475b2e415fe80b41d3357db`
- Starting metadata: versionName `0.7.1`, versionCode `8`
- Target metadata: versionName `0.7.2`, versionCode `9`
- Latest fetched `origin/main` at the last refresh: `afed3dc90b5873218d5e333882528f8c5ddd54a2` (2026-10-02)
- Latest main merge on this branch: `1530d02f`, with `afed3dc90b5873218d5e333882528f8c5ddd54a2` as its main parent; the v0.7.1 source remains an ancestor
- Latest pushed stabilization checkpoint before this local candidate: `d37e114d466d681138140a0280a7c988235c3c94`
- Current working changes: complete v0.7.2/code 9 stabilization candidate, not yet committed or pushed
- Physical DAC writes: none

## Local verification

- Full Gradle gate on the dirty worktree: 727 JVM tests passed with 0 failures/errors/skips; lint passed with 0 errors, 111 warnings, and 2 hints; debug and release APK assembly passed
- Forced Kotlin compile passed; its two warnings are in unchanged files
- R8 mapping verification passed; at least one app class is renamed
- API 35 instrumentation passed 25 tests with 0 failures/errors/skips on `opra-v072-api35` (ARM64, emulator 37.1.11)
- Python tool suite passed 235 tests; release contract, registry validation, and Favorite sample validation passed
- actionlint 1.7.12 passed all workflow YAML; ShellCheck 0.11.0 passed repository shell scripts
- Independent read-only candidate review found no confirmed correctness or regression finding; exact PR-head review remains required

## API 26 smoke artifacts

- Latest local smoke used a fresh `-wipe-data` API 26 ARM64 emulator with a 48 MiB heap-growth limit. It rendered manufacturer, model, and profile lists; process PID 4425 remained alive and resumed after the 60-second observation, with no AndroidRuntime errors.
- Unsigned minified v0.7.2/code 9 APK SHA-256: `67a2663d53cedc30ad3395117636e02891f86b9a98f4ada4c6e552a4b000480c`
- Temporary smoke-signed APK SHA-256: `fd56fe111f17221ce673ac3a51f58defcaa05890d963bef03a6cfd1e59347ef1`
- Temporary smoke signer certificate SHA-256: `e8d962e436a507a3edb891a38a2867c7d1191a62b3da9ff3aa0e8477bd2ca10d`; it is not the project release signer
- `aapt` verified package `com.weekssa.opraeqforuapp`, versionName `0.7.2`, versionCode `9`, and minSdk 26; `apksigner verify` passed v2/v3
- Final Dalvik allocation was 47,094 KiB of 49,152 KiB, leaving 2,058 KiB free
- Full local evidence and the separated earlier 13-minute smoke are under `.unlazy/v0.7.2-autonomous-release/evidence/api26-final/`
- The API 26 smoke used dirty source and a temporary signer. It is not final source, CI x86_64, or release candidate provenance. Repeat after checkpoint commit.

## Remote promotion state

- Remote branch after previous checkpoint: `d37e114d466d681138140a0280a7c988235c3c94`; fetch and push state must be refreshed before the next push
- Pull request: none
- No candidate or PR CI exists yet. The latest observed branch run was unrelated Automatic Dependency Submission run `37057657032` on checkpoint `d37e114d466d681138140a0280a7c988235c3c94`; it succeeded.
- Merge SHA: none
- Signed candidate workflow/run/artifact: none
- Release APK SHA-256: none
- Release signer certificate SHA-256: expected pinned project identity, to be independently verified on the candidate
- Candidate manifest: none
- Remote `v0.7.2` tag: absent at the last remote check
- GitHub Release: absent at the last remote check

Update this file after every branch push, PR/CI change, merge, candidate, tag, publication, and public asset verification. Never copy a previous candidate checksum or signer verification forward as evidence for a changed source.
