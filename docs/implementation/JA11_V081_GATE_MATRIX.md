# JA11 v0.8.1 Gate Matrix

This matrix applies with [the mission Constitution](JA11_V081_CONSTITUTION.md). A check is valid only for the exact inputs recorded with its result.

## Gate definitions and current disposition

| Gate | Evidence required | Current disposition |
| --- | --- | --- |
| G0 Gate ledger integrity | Lint the current gate ledger and preserve exact source/artifact/PR provenance, rejected artifacts, and prior physical outcomes. | Pass: current ledger lint output SHA-256 `f27cfadc49451ca83380a6087fedcaa5c21ada937f1ff02424f1b506e5c6087b`. |
| G1 Repository, PR, and base reconciliation | Verify repository, branch, local HEAD, origin/main, exact PR #80 head/state, and current check state. | Local source is `3d7bc1d9`; current remote PR head is `186c43b2` and does not contain it. |
| G2 Focused production regressions | Focused JA11 Flasher and affected transport/session/repository tests for the exact production source. | Pass for source `3d7bc1d91e6c39327477d1341bde80e8a37bfbd4`; helper changes do not invalidate it. |
| G3 Full local app qualification | JVM, lint, debug/release/diagnostic builds, Android-test compile/assembly, and R8 for the exact production source. | Pass for source `3d7bc1d91e6c39327477d1341bde80e8a37bfbd4`; helper/docs changes do not invalidate it. |
| G3a Acceptance helper fixtures | Shell/Python syntax, privacy/redaction, permission-denial rejection, Model D single-candidate acceptance, missing-serial acceptance, both-serial mismatch rejection, multiple-candidate rejection, stale-session rejection, prior-open-generation binding, and detach-order checks. | Pass: unlazy output SHA-256 `944d79f424382f0f7979d2e85d610c46cbc3030adab5726b8635b7a9faed34be`. |
| G4 Independent review | Read-only review of current production source and corrected Model D acceptance helper; review policy against this Constitution. | Pass: production source review found no actionable issue; final corrected-helper/procedure review found no remaining actionable issue. The helper reviewer confirmed prior-open-generation binding, permission-denial rejection, fresh-window confirmation, Model D optional-serial behavior, and redacted output. No ADB or hardware was used. |
| G5 Candidate artifact and emulator | Exact source-bound APK hash/package/version/signer, cold launch and runtime source SHA on isolated API 35, applicable source-matched instrumentation. | Pass: APK `3b74672a587daeaaf8f562634c5dcecea073f7ad6e01df736f874ee448fc6261`; 64/64 tests. |
| G6 Exact-head CI | All required PR checks on the exact pushed PR #80 head containing production source `3d7bc1d9` and the coherent helper/procedure updates. | Pending. Current GitHub head `186c43b2` does not contain source `3d7bc1d9`. |
| G7 Physical acceptance | Separate confirmed phone window; frozen candidate install/runtime verification; read-only exact-one-candidate current-session baseline; first Mic restoration only if Off; expected-reset/readback; bounded remaining plan; full restoration and release. | Not started for current candidate. Phone is not needed now. |
| G8 Release integration and approval | Complete release provenance and obtain separate explicit owner authorization for merge/publication. | Owner-gated; not authorized by testing approval. |
| G9 Public artifact verification | Verify public tag/release/APK/checksum/signer/update metadata after authorized publication. | Owner-gated; not authorized by testing approval. |

## Invalidation rules

- Production source change: rerun affected regressions, app builds, emulator tests, independent source review, and exact-head CI. A changed source SHA requires a newly bound candidate artifact.
- Test-only change: rerun the affected test gate. Rebuild the production APK only when test validity or bundled production inputs changed.
- CI workflow change: validate the changed workflow and rerun only affected CI checks.
- Acceptance helper change: rerun helper syntax/fixtures and independent helper review. Do not rebuild the production APK or rerun app/emulator gates unless app or test validity changed.
- Documentation/evidence-only change: update records and review the changed wording. It does not invalidate Android build or CI results.
- Prior evidence proven invalid: rerun only the invalidated gate and its dependent gates. Preserve the original result and record the reason for invalidation.

## Candidate tuple and physical prerequisite

Record together: production source SHA, exact PR head SHA, diagnostic APK SHA-256, package, version/versionCode, and signer certificate SHA-256. Physical evidence must identify this tuple and its test-plan version. The phone request is allowed only after all applicable off-phone gates pass; include purpose, planned writes/resets, Android permission interaction, expected occupancy, pass/fail meaning, and restoration obligation, then wait for confirmation.

The physical read-only gate uses Model D: exactly one supported JA11, permissioned current claimed session, valid current generation, complete source-bound baseline, and no later detach/close/ambiguity. Serial status is optional. If both old and new serials are available, mismatch fails closed. A serialless success proves state on the sole returning supported JA11, not same-unit identity.
