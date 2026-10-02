# v0.7.2 test matrix

| Gate | Command / evidence | Environment | Baseline result | Final result |
|---|---|---|---|---|
| Debug assembly | ./tools/codex-android :app:assembleDebug | macOS Codex host, v0.7.1 then candidate | PASS on exact v0.7.1 base | PASS on current worktree source, still version 0.7.1; final versioned run pending |
| Fresh JVM suite | ./tools/codex-android :app:testDebugUnitTest --rerun-tasks | macOS Codex host, v0.7.1 then candidate | PASS, 713 tests, 0 failures/errors/skips | PASS on current worktree source, 714 tests, 0 failures/errors/skips; final versioned run pending |
| Android lint | ./tools/codex-android :app:lintDebug | macOS Codex host, v0.7.1 then candidate | PASS, 0 errors, 114 warnings, 2 hints | PASS on current worktree source, 0 errors, 114 warnings, 2 hints; final versioned run pending |
| Release assembly | ./tools/codex-android :app:assembleRelease | macOS Codex host, v0.7.1 then candidate | PASS on exact v0.7.1 base | PASS on current worktree source, still version 0.7.1; final versioned run pending |
| R8 mapping | bash tools/verify-r8-mapping.sh | after release assembly | PASS on exact v0.7.1 base | PASS on current worktree source; final versioned run pending |
| Instrumentation | ./tools/codex-android :app:connectedDebugAndroidTest | API 35 Google APIs ARM64 AVD, emulator 37.1.11 | PASS, 25 tests, 0 failures/errors/skips | pending coherent final source run |
| Minimum SDK smoke | reproduce current .github/workflows/android-ci.yml API-26 path | API 26 Google APIs ARM64 AVD, emulator 37.1.11 | FAIL on pristine v0.7.1: OOM while whole-file buffering canonical catalog | PASS after streaming fix: signed local minified APK installed, launched, stayed resumed, no AndroidRuntime crash; final versioned run pending |
| Python tool suite | discover exact repository CI invocation before running | supported workspace Python runtime | NOT RUN | NOT RUN |
| Catalog/sample validation | current workflow command(s) for favorite-source and catalog contracts | repository Python runtime | NOT RUN | NOT RUN |
| Release script tests | python unittest for tools/test_promote_release_candidate.py plus discovered contracts | supported Python runtime | NOT RUN | NOT RUN |
| Workflow validation | actionlint | all workflows, then changed workflows | NOT RUN | NOT RUN |
| Shell validation | shellcheck on changed shell scripts | installed ShellCheck | NOT RUN | NOT RUN |
| Security analysis | CodeQL / dependency submission / required PR checks | GitHub Actions, exact PR and merge SHA | NOT RUN | NOT RUN |
| Diff integrity | git diff --check | candidate branch and final comparison | NOT RUN | NOT RUN |
| Signed candidate | main-only Signed Release Candidate workflow | exact merged main SHA | NOT RUN | NOT RUN |
| Public promotion | Promote Signed Release Candidate workflow | exact immutable candidate run/artifact | NOT RUN | NOT RUN |

Counts, failures, errors, skips, lint diagnostics, run URLs, artifact IDs, checksums, signers, emulator identities, and unavailable gates must be recorded in the corresponding row or ARTIFACTS.md. An emulator/physical result is valid only when command output proves the device actually ran it.

The API 26 baseline failure was reproduced on a freshly wiped AVD with `dalvik.vm.heapgrowthlimit=48m`. The failure occurred while `CanonicalCatalogRepository.loadSnapshot()` used `File.readText()` on the 19,375,344-byte remote canonical catalog. The work branch now decodes directly from the file stream; the same API 26 cold-install launch path passed after that change. The temporary smoke keystore and APK were removed after use.
