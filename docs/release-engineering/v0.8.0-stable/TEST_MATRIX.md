# v0.8.0 stable promotion test matrix

| Gate | Scope | Result | Evidence |
|---|---|---|---|
| Local release-tool syntax | `py_compile` for publisher, publisher tests and emulator helper | PASS | 2026-10-08 local run |
| Release-publisher tests | `python3 tools/test_promote_release_candidate.py` | PASS, 52/52 | 2026-10-08 local run; covers v0.8.0-only beta baseline and future stable baseline behavior |
| Workflow/release contract | `python3 tools/promote_release_candidate.py check-contract` | PASS | 2026-10-08 local run |
| Workflow YAML/shell parse | Ruby Psych parses `.github/workflows/promote-signed-release.yml`; extracted API 35 step passes `bash -n` | PASS | 2026-10-08 local run; actionlint is not installed locally |
| Android JVM/unit | Local source compilation completed, but Gradle test worker bootstrap failed before assertions | PENDING exact stable PR-head CI | Local cache lacks generated `gradle-worker.jar`; 124 executors reported `ClassNotFoundException: worker.org.gradle.process.internal.worker.GradleWorkerMain`. No unit assertion ran or failed. |
| Android lint/static analysis | Local `:app:lintDebug --max-workers=2` passed; exact stable PR head pending | PENDING PR CI | Local lint PASS; exact-head CI remains authoritative |
| API 35 instrumentation/Compose UI | Android-test source compiled locally; exact stable PR-head execution including stable What's New renderer assertion | PENDING PR CI | `:app:compileDebugAndroidTestKotlin --max-workers=2` PASS; no local instrumentation execution |
| API 26/min SDK | Exact stable PR head | NOT RUN | Awaiting PR CI |
| Release assembly/R8 | Exact stable PR head | NOT RUN | Awaiting PR CI |
| Signed v0.7.2 -> stable persisted-state upgrade | Exact official stable APK on API 35 | NOT RUN | Main-only promotion workflow |
| Signed beta -> stable persisted-state upgrade | Exact official beta and stable APKs on API 35 | NOT RUN | Main-only promotion workflow |
| Stable signed clean install/core smoke | Exact official stable APK on API 35 | NOT RUN | Main-only promotion workflow |
| Public release verification | Tag, source, assets, checksum, signer, provenance, latest | NOT RUN | After publication |

The current dependency graph refresh is recorded in `MISSION.md`. The current alert inventory could not be retrieved and no clean-scan claim is made.
