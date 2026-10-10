# JA11 v0.8.1 Gate Matrix

This matrix applies with the [mission Constitution](JA11_V081_CONSTITUTION.md). A result qualifies only the exact source, artifact, or PR head recorded with it.

## Gate definitions and current disposition

| Gate | Evidence required | Current disposition |
| --- | --- | --- |
| G0 Repository and history | Verify repository/base/branch/PR and preserve earlier findings. | Pass: base `9b9a1f610025b3d6233d4ff6f0c2309a668577cc`; production source `9493cf030acb440f92e547fc667f6a5399616045`; PR #80 preserved, open/draft/unmerged. PR #81 remains open/draft/unmerged; verify the live exact head before physical work. |
| G1 Focused production JA11/session regressions | Model D identity, expected resets/readback, ordering, one Save, detach fencing, and no replay. | Pass on unchanged production source `9493cf0`; seven USB-session Robolectric tests plus focused JA11 regressions. Prior output fingerprint `6607bbdbc194446f8216d0f3509d8db3791799ba288420d532b60a1f449737b9`. No invalidation: production transaction source is unchanged. |
| G2 Full JVM suite | Complete `:app:testDebugUnitTest`. | Pass on `9493cf0`: 768 tests, zero failures/errors/skips; fingerprint `263381d7b944cc27e80f01107295f21ebaf6f8c9b2dfe33fddbc92dcb407e102`. No invalidation. |
| G3 Diagnostic lint | Lint the changed diagnostic variant. | Pass: `:app:lintJa11Diagnostic`. The only new resource spelling warning is the known JA11 dictionary warning; no changed-source lint error. Release/debug lint gates were not invalidated. |
| G4 Diagnostic build and artifact | Assemble only the diagnostic variant and verify APK provenance. | Pass: `:app:assembleJa11Diagnostic`; package/version/code, source metadata, signer, alignment, and APK digest verified. APK SHA-256 `44040493c7263ae7d7e6f41be7389534d07977427cc8002494097fbf3c868848`. |
| G5 Release R8 | Verify minified release mapping. | Reuse pass on production source `9493cf0`; diagnostic source set and AndroidTest variant enablement do not affect release R8. |
| G6 Emulator | Run affected diagnostic report instrumentation on a disposable API 35 emulator. | Pass: 4/4 `Ja11FlashReportDiagnosticTest` tests (trace filtering; report retention/dismiss; readable and JSON share serializers; no early replacement). XML SHA-256 `d314ae93051f52cd9a398c6734209f830916607c74ad8d8977b537528e53ebaf`. Prior 64/64 production Android USB-session instrumentation remains valid and is not repeated. No Pixel selected. |
| G7 Phone-helper fixtures | Exercise guarded install and restoration helper with fake ADB. | Pass on existing evidence: fixtures cover absent install, verified update, query/hash/signer/version errors, and filename collisions. Helper is unchanged. |
| G8 Independent review | Review diagnostic access and the future Test C procedure. | Pass: reviewer confirmed the separate launcher/report retention and exports; no production/transaction change; and corrected procedure requiring a fresh replacement session plus full unrelated DEVICE/EQ readback after Mic reconnect, with Mic On as the final target when restored. |
| G9 Exact-head CI | All required CI checks pass for the exact pushed PR head. | Must be verified on PR #81's live exact head before physical work. The previous eight-check pass on `885fe90ec46ba30ffbd2bcef1f28c075eb8546f7` does not qualify this diagnostic candidate. |
| G10 Candidate tuple | Record source, PR head, APK hash, package/version, signer. | Production/transaction SHA `9493cf030acb440f92e547fc667f6a5399616045`; diagnostic code commit `fc409b7d038632e67dccdeccbd5144f3d3b53bdd`; APK SHA-256 `44040493c7263ae7d7e6f41be7389534d07977427cc8002494097fbf3c868848`; package `com.weekssa.opraeqforuapp.ja11diag`; version/code `0.8.1-ja11diag` / `12`; signer SHA-256 `73aa7581c8dc7dcc8ccea7586771119a98f9a74d7d8cf23716e09c557c9f6b41`. Refresh the final exact PR head after push. |
| G11 Scope audit | Confirm diagnostic-only code and no unrelated behavior. | Pass: no `app/src/main` file changed; diagnostic `Application`, launcher activity, strings, and focused test are in variant-only source sets. Existing transaction, Flash, Save, reconnect, identity, and readback behavior is unchanged. |
| G12 Physical acceptance | New owner-confirmed session after all applicable software gates; exact restoration and immediate Pixel release. | J028 remains partial and closed. Its one Flash returned a fresh User 1 readback matching baseline, but the report was not captured and Save count is unknown. Mic/UAC/Test D and restoration evidence remain tied to their recorded exact source/APK. Test C must be one new Flash/report capture; no Mic round-trip, UAC, volume, or power-cycle reruns. |

## Invalidation rules

- Diagnostic source/resource changes invalidate diagnostic lint/build/test, APK provenance, affected independent review, and exact-head CI. They do not invalidate production JA11 transaction, release R8, helper, or earlier physical evidence when no `app/src/main` behavior changed.
- Production-source changes invalidate the applicable focused regressions, full JVM, lint, builds, R8, emulator tests, independent review, exact-head CI, and artifact provenance.
- Test-only changes invalidate only affected tests. Helper-only changes require helper fixtures/review. Documentation-only changes do not change the executable candidate but do produce a new PR head requiring exact-head CI when PR checks are head-specific.
- Preserve every incomplete/failed result and its exact candidate attribution. Never reinterpret a missing report as a pass or repeat a completed physical test without a concrete invalidation.

## Candidate and phone-window rule

Freeze together the production source SHA, diagnostic code revision, exact PR head, APK SHA-256, package/version, and signer certificate SHA-256. The J028 window is closed. Do not touch Pixel or JA11 until the diagnostic candidate's applicable CI/review gates pass and a new owner-confirmed window is obtained.

Future Test C begins with the exact candidate and a read-only baseline/identity gate. Verify the target exactly matches the fresh User 1 bank/gain. If Mic reads Off, restore Mic On first with expected-reset handling, then freshly reread Mic and the complete unrelated DEVICE/EQ baseline before any further mutation; never revert Mic to Off. If Mic is already On, do not write it. Set program Off only if needed, run one direct Flash, export both reports immediately, validate all order/count/generation/readback evidence, restore only authorized changes, and release Pixel. See `ja11-v0.8.1-acceptance/PHONE-PLAN.md` for full procedure and stop rules.

Merge, publication, public persistence claims, and public support remain separate owner-approval gates.
