ARCHITECTURAL ROOT ESTABLISHED — OFF-PHONE REGRESSION AND FIX

The owner reauthorized phone access and completed the single diagnostic Flash. The run converged normally: exactly one Save, seven current returning-session final reads, complete rendered EQ and enabled Edit EQ. Original JA11 settings/EQ/program were verified restored; the temporary Personal EQ/source were removed. The Pixel was explicitly released. No second Flash or other JA11 setting mutation is planned. All prior off-phone diagnostic gates pass; no release was published.

The original failed lifetime was unavailable at session start; this new diagnostic run is a healthy control, not a failure reproduction or a fix. It records both ConnectedObserver and FlashCompletion acquiring/publishing generation2 on their first attempts, with retained Activity/ViewModel/repository identity and no cancellation/rejection. Final trace107 records/zero drops. Later owner video and off-phone source/executable evidence now establish >=95% architectural causal confidence; desired-behavior red/green regression and production fix remain pending. The diagnostic APK contains no convergence fix.

The owner now recalls that the earlier failing Flash originated from My EQs headphone/profile, that they stayed until it finished, and that the app automatically returned to the My EQs list. Today used a Personal EQ card. Source contains a conditional route: Save detach can change Automatic output to the manual fallback; a mismatched/absent managed-headphone list then removes the detail screen and cancels its composition-owned Flash caller. A session output override or retained headphone can prevent this route. This is strongly supported by the owner recollection, but original fallback/override values and original acquisition/cancellation timing are still unobserved. Two test-only Compose characterizations are being validated off-phone; no production change follows before the runbook root/reproduction gates.

Later owner-supplied video directly shows the transition, including a transient empty My EQs library during FiiO→UAPP fallback at7.0179s, automatic detail removal, spinner disappearance7.0288s, FiiO reconnect/current DEVICE, and literally blank EQ. Owner identifies regular app; video SHA-256 `57180f9a8ab2d0145e8ca393ad0144c6a4f04100857b75093eff796a8196478a`. Source shows cancellation may also strand Running FLASH; its EQ UI branch displays no content. A third real-Compose/real-flasher/simulated-Save test is validating that exact visible shape without hardware. This adds operation-status evidence to the earlier acquisition hypothesis; no literal-blank claim should be attributed only to a missing bundle. First new-test CI's two ambiguous-dialog selectors failed and are corrected; actual execution is pending. Prior diagnostic qualification remains valid on unchangedf6bc.

## Exact diagnostic candidate

| Field | Verified value |
| --- | --- |
| Source SHA | `f6bc1d6d87b9618bc35354f33103504c753c7f6a` |
| Branch / draft PR | `codex/ja11-postflash-diagnostic-trace` / [PR #93](https://github.com/weekssa/OPRA-EQ-for-UAPP/pull/93) |
| APK SHA-256 | `4b00ab3e1c23e435b6ca848a1a0123068ac65e41a00f2fdbae1b1cb6fae796f9` |
| Package | `com.weekssa.opraeqforuapp.ja11trace` |
| Version / code | `0.8.1-ja11trace` / `12` |
| Signer certificate SHA-256 | `5857646924badad4df93d8f953a57792cff75662c99d3af808d910d4081170fc` |
| Signer kind | Existing Gradle debug signing configuration; isolated package; not the official release key |
| Workflow / run / attempt | `.github/workflows/android-ci-exact-head.yml` / [38366296490](https://github.com/weekssa/OPRA-EQ-for-UAPP/actions/runs/38366296490) / `1` |
| Immutable GitHub artifact | `11797477044`, `JA11-convergence-candidate-f6bc1d6d87b9618bc35354f33103504c753c7f6a` |
| Artifact ZIP SHA-256 | `c784db08afd11a460b5a3d947749187122f2c8220f959dfe591a82efc7572598` |
| Test APK SHA-256 | `a7d79771f54d933cf217e854f3fa39b09da2d9ebb477cc64979cc77d82b52612` |

Frozen read-only local [diagnostic APK](/Users/stephenweeks/Downloads/JA11-convergence-diagnostic-f6bc1d6d87b9618bc35354f33103504c753c7f6a/candidate/app-ja11Diagnostic.apk) and [candidate manifest](/Users/stephenweeks/Downloads/JA11-convergence-diagnostic-f6bc1d6d87b9618bc35354f33103504c753c7f6a/candidate/candidate-manifest.json). The unchanged original artifact ZIP is retained beside the candidate directory. APK v2 signature, shared target/test signer, package/version/code, embedded source, runtime app-produced source header, exact artifact membership/ZIP digest/CRC and target APK alignment were independently verified. GitHub artifact expires2027-01-11; the local read-only copy is durable beyond the temporary build/log folders. Do not rebuild, re-sign or substitute APK bytes for this physical session.

## Off-phone validation

- Focused structural privacy, bounded/drop-count/concurrent recording, release/default binding, actual diagnostic DI, connected-observer versus Flash-completion wiring, unchanged one/two read policy, cancellation/rethrow/no retry/publication, and real session generation replacement/rejection tests: PASS.
- Full JVM tests: debug785 and diagnostic788, zero failures/errors/skips, including Black Pearl/EW300/shared regressions. New exact-head hosted diagnostic report independently confirms788; hosted debug gate passes.
- Debug and diagnostic lint: zero errors;118/116 warnings and2 hints respectively. Debug, release, diagnostic and diagnostic instrumentation assembly plus R8 mapping: PASS. Release recorder flagfalse; diagnostic Application/report UI absent from assembled release manifest.
- Fresh Luna6.0 High independent diagnostic review: PASS after explicit-export and caller-correlation findings were fixed; bounded helper correction follow-up: PASS.
- Exact-head hosted run: all7 jobs PASS on the exact source/workflow SHA. Full hosted API35 debug UI64/64 tests pass with zero failures/errors/skips; API26 minified release clean launch passes.
- Exact unchanged diagnostic APK pair: API26 and35 each6/6 tests PASS. Runtime source/package/version match; trace drop count0 in both exported hosted reports. Activity recreation is distinguished from retained ViewModel lifetime.
- Diagnostic exact-head CodeQL: PASS, zero findings, API analysis1941674168; standard PR Kotlin/CodeQL: PASS. Catalog, priority-community and dependency checks: PASS.
- Artifact helper9 fixtures, real local APK-pair create/verify, actionlint and independent hosted artifact verification: PASS. Earlier unversioned-test-APK parser failure is preserved in EVIDENCE; no skipped job counted as pass. Existing normal android-ci.yml zero-job failure remains historical infrastructure evidence; the registered exact-head fallback ran real qualifying jobs.

No JA11 transport/flasher/protocol/settings/security files changed. Recorder hooks own no transport, file or coroutine; only the explicit diagnostic Save button writes a private trace. Trace contains opaque IDs, generations, indices, typed phases/reasons and booleans, excluding serials/fingerprints/raw payloads/EQ values/arbitrary errors. Synchronous recording has finite observation overhead; retry, timing policy, mutex/session ownership and cancellation semantics remain unchanged. UI_ADOPTION denotes observed ViewModel UiState, not rendered-pixel proof. The new isolated package starts with separate app storage and preserves the production installation.

## Physical evidence and next work

J030 in EVIDENCE.md and the maintained JA11 validation ledger records the exact candidate, physical profile, operation, original-state restoration, and claim boundaries. Private captures remain outside Git at `/Users/stephenweeks/.codex/evidence/ja11-postflash-diagnostic-control-20261013`. Exact technical report SHA-256 is `16bae426d2b9736391f4762bd14ad222f63f24bbec7b6044e625cb5b485ee27c`; post-Flash acquisition trace `2e84a8ff82fd63237bcce88c4f8b354b9a9d602194619501935414be8d76c0dc`; final trace `8b8396036ad18eb5217344b53610174d884430524c987583f8b28245cc38ab2a`. Reports establish exactly one User1-select/five bands/gain/Apply/Save and no write after Save; all final reads match the complete baseline. Runtime signerVerified=false means the diagnostic debug key differs from the pinned official release key.

All three new Compose characterizations passed on exact27bb044559cae4cd16b888cf662a84fb1f1dd302; full API35 job requires same-head retry after existing window-focus failures. Fresh Luna High root judgment and primary synthesis establish >=95% confidence in automatic managed-detail disposal/caller cancellation/stranded Running FLASH. Individual null-read field and original acquisition ID remain unknown and are not guessed. Source history predates both releases; PR88/89 retained the cancellable caller and therefore could not enforce their completion/retry boundary.

Continue off-phone through the actual ViewModel/session/repository desired-behavior failing regression, then the narrow owner-lifetime repair and all relevant gates. Production worktree `/Users/stephenweeks/.codex/worktrees/ja11-flash-owner-fix/OPRA-EQ-for-UAPP`, branch `codex/fix-ja11-postflash-convergence-final`, is based on main4b2d3e67 and contains test preparation only. The frozen diagnostic source/APK remain unchanged. No phone query/discovery/install/launch, recovery, new Flash or settings action is needed for this continuation. Original fix gates9 MET/14 UNMET; diagnostic gates12 MET/0 UNMET. Merge, production-fix qualification and publication remain separate unapproved gates.

Canonical mission records remain the four files on `codex/ja11-postflash-reconstruction`; implementation worktree is `/Users/stephenweeks/.codex/worktrees/ja11-convergence-trace/OPRA-EQ-for-UAPP`. Both branches are preserved remotely. Main remains `4b2d3e67a00f1c7545e27165135c56fc901206b9`; public v0.8.1 remains unchanged.
