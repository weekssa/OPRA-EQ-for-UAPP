# v0.8.0 stable promotion test matrix

The exact application/test candidate below was `3e557e1a0f7868bc5cf54f9e363da0c9e3c41bbd` (tree `680564d2488e54c194488d67d8d006ba22a6ee3f`). The production app-source tree is unchanged from qualified beta. Refresh the live PR head/checks before merge; a later documentation-only commit does not alter this test-bearing candidate.

| Gate | Result | Evidence |
|---|---|---|
| Local release-tool syntax | PASS | Python compilation of publisher, publisher tests and emulator helper; 2026-10-08 local run |
| Release-publisher tests | PASS, 52/52 | `python3 tools/test_promote_release_candidate.py`; includes the v0.8.0-only beta baseline and generic future-stable behavior |
| Workflow/release contract | PASS | `python3 tools/promote_release_candidate.py check-contract` |
| Workflow YAML/shell parse | PASS | Ruby Psych parsed `.github/workflows/promote-signed-release.yml`; extracted API 35 script passed `bash -n`; `actionlint` is unavailable locally |
| Android JVM/unit | PASS on exact candidate | Android CI run `37736600658`, build job `113177438410`, unit-test step passed |
| Android lint/static analysis | PASS on exact candidate | Android CI run `37736600658`, build job `113177438410`, lint step passed; local `:app:lintDebug --max-workers=2` also passed |
| Release assembly/R8 | PASS on exact candidate | Android CI run `37736600658`, build job `113177438410`, debug/release assembly and minified-output verification passed |
| API 35 instrumentation/Compose UI | PASS, 64/64 | Android CI run `37736600658`, job `113177438418`; 0 failures/errors/skips. Artifact `11531339710`, digest `sha256:2692842828d81789c085ea2a09d52c5ad6566997a5c819142104190a60e31888`. Both formerly failing lifecycle/recovery tests pass. |
| API 26/min SDK smoke | PASS | Android CI run `37736600658`, job `113177438121`; diagnostics artifact `11532521333`, digest `sha256:1ecf8c9203030da778b8e02d1e23cecd89628b0e379eb0a67e3adad64375db46` |
| CodeQL | PASS | Run `37736600633`, Analyze Kotlin job `113177438081` |
| Catalog currentness | PASS | Run `37736600706`, validation job `113177438706` |
| Priority community coverage | PASS | Run `37736600726`, validation job `113177438768` |
| Android-test Kotlin source compilation | PASS locally | `./tools/codex-android :app:compileDebugAndroidTestKotlin --max-workers=2` |
| Local JVM test launch | NOT EXECUTED | A previous local Gradle launch failed before assertions because the custom cache lacked generated `gradle-worker.jar`; remote exact-head Android CI passed the unit-test step. No local unit assertion failure occurred. |
| Signed v0.7.2 -> stable persisted-state upgrade | ATTEMPTED; gate not passed | Promotion run `37749785239`, API 35 job `113219908517`, installed and launched the v0.7.2 baseline, then stopped while seeding persisted state because the helper failed to recognize its exact `Import personal EQ` form. No fixture was saved, the v0.8.0 candidate was not installed, and no in-place upgrade occurred; see the continuation below. |
| Signed beta -> stable persisted-state upgrade | NOT RUN | Exact official beta and stable APKs; main-only promotion workflow after signing |
| Stable signed clean install/core smoke | NOT RUN | Exact official stable APK; main-only promotion workflow after signing |
| Public release verification | NOT RUN | Tag, source, assets, checksum, signer, provenance, and latest metadata after publication |

## Stable promotion continuation — 2026-10-08

PR #74 merged at main `984477c773f9f388e93583a61f6459dbc8e27ae1`, tree `eede4100c1917019358fdb2c1afc7b1b656f2c61`. The exact main-only stable candidate was signed successfully by run `37748076250`; artifact `11536942838` has ZIP SHA-256 `6347f3a141d829df9d0848cd729099f36116799a3f0c09e1145880f799d827d3`. Its APK SHA-256 is `ce9ee7f1cef006b9c02e9236144504505547789fc5adad05bf8ec8e812d9f7ff`; package/version/code and pinned signer were independently verified. This signing result is tied to main `984477c...` and must be renewed if the follow-up helper change merges.

Promotion run #8 (`37749785239`) installed and launched the signed v0.7.2 baseline, then failed while seeding its persisted-state fixture. Diagnostics artifact `11537199862` (SHA-256 `7a9d962855e84c4dd054ce6b380ba8ebe234642388f3d2165c7af7e7b98f515b`) captures the legacy form labels. The helper expected `Import personal PEQ` and a `Parametric EQ text` field, while the observed form is titled `Import personal EQ` and exposes `Equalizer APO / AutoEq text`. No fixture was saved; the v0.8.0 candidate was not installed, so no in-place upgrade was attempted. The defect is confined to release-test tooling; it does not establish an app failure or upgrade result. Candidate-tag and publication jobs were skipped. The beta-to-stable lane and clean-install lane were not reached. A narrow matcher/field correction and regression tests are in PR #75; its local suite passes 56/56 and contract validation passes, while exact-head CI is still running and a new main-only stable artifact/promotion run remains required.

## First API 35 attempts and correction

The original UI job `113167112522` and one diagnosed retry `113170893509` each reported two test-harness failures (64 tests, 2 failures, 0 errors/skips). The report showed the accessibility assertion ran without window focus after saved-state restoration, and native key injection did not reach the fake read callback. The artifacts contained no app crash, ANR, or OOM. Commit `3e557e1a` contains a test-only host/input harness correction preserving no-replay assertions and independent native Android D-pad coverage; the exact-head API 35 suite then passed 64/64.

The dependency graph refresh is recorded in `MISSION.md`. GitHub reported 56 repository vulnerabilities; item-level current alerts/runtime mapping could not be retrieved. This is not a clean security scan and does not rule out runtime exposure.
