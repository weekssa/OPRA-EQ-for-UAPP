# Implementation report - Black Pearl and JA11 remediation

Status: SOFTWARE_VERIFIED; release status remains
`MERGE_APPROVAL_REQUIRED` because the trusted signed-beta workflow is main-only. Promotion
preparation is complete on the existing branch; no main merge or signed candidate was performed.

## Source and evidence identity

- Repository: `https://github.com/weekssa/OPRA-EQ-for-UAPP.git`
- Refreshed base before implementation: `origin/main` at `ce5efdf7985e4fc48f975b14fcedb1f592d43772`
- Live `origin/main` after the required refresh: `0adcc8159a467790104cf2dc797f1279ef2c53ed`
- Branch: `codex/black-pearl-ja11-remediation-20260927`
- Worktree: `/Users/stephenweeks/.codex/worktrees/black-pearl-ja11-remediation-20260927/OPRA-EQ-for-UAPP`
- Final implementation source SHA: `dc6478a25b1745b4f78f833c69e96e066c615d56`
- Promotion-preparation source SHA: `0f080516e8d4aa34e9d0a04b16464bf65b2b6d7d`
- Reviewed implementation/workflow head: `0c77e081fd6abd12a6e20b482ce269ae9f5bb764`
- Final documentation evidence head: `04a4a0ae2a4fd19371547970d58fab5e6036e97e` (documentation-only
  commit atop the reviewed implementation/workflow head).
- Draft review PR: [#49](https://github.com/weekssa/OPRA-EQ-for-UAPP/pull/49), exact head
  `04a4a0ae2a4fd19371547970d58fab5e6036e97e`, base `0adcc8159a467790104cf2dc797f1279ef2c53ed`.
  It remains intentionally open and draft; no merge was performed.
- Reference APK: available at the owner-supplied path; SHA-256
  `3d723ffa17042fbef7e6e192c14ecce460628d0f08a55eeb30caa59566ff8731`; package
  `com.weekssa.opraeqforuapp`, version `0.7.0` / code `7`; embedded source metadata
  `ce5efdf7985e4fc48f975b14fcedb1f592d43772`. It was not copied or used as candidate evidence.

## Implementation result

### TRN Black Pearl Direct Flash

- Preserved the existing protocol bytes, report order, timing, gain math, identity matching, and
  authoritative session ownership.
- Preserved the existing complete ten-band/latch/Flash sequence and existing verified compact copy:
  `Flash successful · TRN Black Pearl EQ was saved and verified · Final hardware readback matched.`
- Success still requires active-slot/gain baseline, representable expected native bands, current
  session before and during writes, all ten native fields matching in raw wire-domain values, raw
  global-gain equality, and valid final session state.
- A readable mismatched final gain now reconciles the persisted anti-stacking delta to
  `observedFinalGainRaw - baselineGainRaw`; it returns `VerificationFailed`, never success, and
  never rewrites automatically.
- Missing/malformed band or gain readback, session replacement, or final uncertainty marks the
  anti-stacking baseline unknown. Later Flash/Reset refuses mutation until a complete fresh
  authoritative snapshot establishes a zero app-owned delta baseline.

### FiiO JA11 My DAC -> EQ

- Added a discoverable `Edit EQ` action in the existing current-hardware EQ section. It is enabled
  only for connected/current-session User 1, matching device/EQ snapshot generation, fresh current
  five-band readback, native global EQ gain, no active read/restart/operation, and an implemented
  route.
- Off and Vocal/Classic/Bass remain truthful non-editable states; no User 1 coefficients are shown
  as their current acoustic response.
- Added one immutable editor baseline token containing exact transport identity/product,
  session generation, active User 1, all five native bands, native global-gain units, freshness,
  and verification time.
- Opening, editing, reset-local-edits, Review, Cancel, Close, and Back are local/read-only. Review
  presents all five type/frequency/gain/Q values, planned global EQ gain, response/headroom
  consequences, warnings, and that Apply is the first hardware write.
- Apply is accepted only from Review with changes, no blocking issue, safe headroom, and a baseline
  token. It re-reads the complete User 1/bands/global-gain token immediately before the first
  write even when USB generation is unchanged, then uses the exact five-band -> global gain -> User
  1 -> Apply -> volatile readback -> one Save/reconnect -> final readback sequence.
- A stale token, disconnect, timeout, transfer failure, mismatched readback, or replacement session
  is not success and does not retry. Existing operation trace fields record `EDITOR_APPLY`, target
  bands, save count, phases, identity, and state-known/unknown outcome.
- JA11 global-gain fingerprint representation is corrected to the maintained signed tenths-dB
  domain. Protocol wire bytes and the existing qualified transaction codec are unchanged; tests
  prove the editor uses the same quantized command path.

## Changed files

Production changes are limited to the shared Black Pearl gain-state guardrail, JA11 snapshot/editor
domain and transaction boundary, existing Hardware EQ repository/session plumbing, existing My DAC
screen/action wiring, and four review strings. Test changes add Black Pearl failure-safety,
JA11 transaction, snapshot-unit, editor, presentation, and Edit-action state coverage. Durable run
artifacts are in this folder. Maintained documentation changes are limited to the changelog,
My DAC status, and release-readiness audit. Promotion preparation also adds one combined
`black-pearl-ja11` choice to `.github/workflows/signed-beta.yml`; it leaves the main-only job guard,
signing inputs, certificate check, emulator checks, immutable artifact publication, and existing
`ja11`/`ew300` targets unchanged.

No protocol, transport, USB identity, endpoint, retry, canonical EQ, navigation, or unsupported
hardware-control file was changed.

## Validation matrix

Commands were run in the isolated worktree with the checked-in wrapper tooling.

- `node /Users/stephenweeks/.agents/skills/unlazy/scripts/gate-lint.mjs docs/UX_REVIEW_WORKFLOW/context/black-pearl-ja11-remediation-20260927/GATES.md` - PASS, `LINT OK`.
- `./tools/codex-android :app:testDebugUnitTest --tests '*BlackPearl*' --tests '*FiioJa11*'` - PASS; focused tests completed successfully. The later state-matrix test was included in the complete suite.
- `./tools/codex-android :app:testDebugUnitTest` - PASS, `UNIT_TESTS_PASS`.
- `./tools/codex-android :app:lintDebug :app:assembleDebug` - PASS, `LINT_ASSEMBLE_PASS`; lint report at `app/build/reports/lint-results-debug.html`. Only existing/deprecation/packaging warnings were reported; no new error.
- `./tools/codex-android :app:connectedDebugAndroidTest` - PASS, `EMULATOR_INSTRUMENTATION_PASS`; 20 tests on `codex-api36(AVD)`.
- `git diff --check` - PASS.
- Deterministic fake transports - PASS through the focused/full unit suite; no USB hardware transport was invoked.
- Release/R8/security/CodeQL/remote CI - local security/CodeQL tooling was unavailable, but the
  exact branch head completed the remote release and analysis checks recorded below.

### Promotion-preparation rerun at source/workflow tree `fc72d7d5be82da64c55d299535d24478c9af46ac`

- `node /Users/stephenweeks/.agents/skills/unlazy/scripts/gate-check.mjs --reverify docs/UX_REVIEW_WORKFLOW/context/black-pearl-ja11-remediation-20260927/GATES.md` - PASS; G0-G4 reran and passed, G5-G7 remained satisfied by the recorded manual review/handoff evidence.
- `./tools/codex-android :app:testDebugUnitTest` - PASS; build was up to date on the refreshed source.
- `./tools/codex-android :app:lintDebug :app:assembleDebug` - PASS.
- `./tools/codex-android :app:assembleRelease` - PASS; minified release/R8 path completed with existing warnings only.
- `bash tools/verify-r8-mapping.sh app/build/outputs/mapping/release/mapping.txt` - PASS; application class renaming was verified.
- `./tools/codex-android :app:connectedDebugAndroidTest` - PASS; `codex-api36(AVD)`, 20/20 tests, 0 skipped, 0 failed.
- `.github/workflows/signed-beta.yml` Ruby YAML parse - PASS; `actionlint` - NOT RUN because it is not installed.
- GitHub Actions `Automatic Dependency Submission (Gradle)` run `#2229`
  (`36333163159`) - PASS on exact source `7898378e20a610a4667e861220fef40c9d8ac751`.

### Draft PR remote checks at final documentation evidence head `04a4a0ae2a4fd19371547970d58fab5e6036e97e`

- Android CI run `#1874` (`36334738869`) - PASS: build, min-API smoke, and emulator UI test.
- CodeQL run `#1758` (`36334738860`) - PASS: Analyze Kotlin.
- Priority community coverage CI run `#1627` (`36334738871`) - PASS.
- Catalog currentness CI run `#2142` (`36334738846`) - PASS.
- Automatic Dependency Submission (Gradle) run `#2231` (`36334735111`) - PASS.
- PR #49 remained draft, open, and mergeable after checks. The remote push warning reported 51
  existing default-branch Dependabot findings; this worker did not reinterpret or suppress them.

## Emulator and app smoke evidence

- AVD: `codex-api36`; device serial: `emulator-5554`; API level: `36`; model: `sdk_gphone64_arm64`.
- `adb devices -l` reported the device ready and `getprop sys.boot_completed` returned `1`.
- Debug APK install succeeded from `app/build/outputs/apk/debug/app-debug.apk`.
- Cold launch succeeded twice with `am start -W -n com.weekssa.opraeqforuapp/.MainActivity` and
  `Status: ok`, `LaunchState: COLD`.
- Large-text smoke used `settings put system font_scale 1.3`, force-stop, and cold launch; the
  launch remained successful. Font scale was restored to `1.0`.
- `uiautomator dump` succeeded and exposed readable My EQs, EQ Library, Settings, Import, and
  empty-state labels in the Compose hierarchy. This is software accessibility smoke evidence, not
  a TalkBack certification.
- The two pre-install uninstall attempts returned `DELETE_FAILED_INTERNAL_ERROR`; the non-`-r`
  install then succeeded and no signature-conflicting package blocked installation. This was a
  disposable emulator only.
- Debug APK SHA-256 observed from the final gate-run assembly: `24785132fe18e7fd9b92a5a197b07175569c96b6eea768f7e79c73f6b24a9e45`.
  It is an unsigned/debug development artifact, not the signed beta candidate.

## Signed candidate provenance

- Signed beta workflow: `.github/workflows/signed-beta.yml`.
- Result: no signed candidate produced for this branch.
- Exact boundary: job condition still requires `github.ref == 'refs/heads/main'`; the branch now
  includes a `black-pearl-ja11` candidate input whose manifest names both software claims and the
  prepared owner Pixel 9 handoff. The workflow has not run for this branch.
- Therefore APK filename, signed APK SHA-256, signer certificate, workflow run, immutable artifact
  ID/digest, and signed install evidence remain `NOT AVAILABLE` for this branch. No debug APK is
  presented as a beta or physical-test candidate.

## Promotion-preparation validation

- `git merge --no-ff --no-edit origin/main` - PASS; only the three refreshed catalog files were
  brought onto the remediation branch.
- `git diff --check` - PASS after the workflow change.
- Ruby YAML parse of `.github/workflows/signed-beta.yml` - PASS.
- `actionlint .github/workflows/signed-beta.yml` - NOT RUN; `actionlint` is not installed in this
  environment.
- A signed workflow dispatch was NOT RUN because the job remains intentionally main-only and the
  branch was not merged to `main`. The draft PR review checks were completed separately and do not
  produce or imply a signed beta.

## Hardware boundary

Luna did not connect to, mutate, Flash, Apply, Reset, Save, Restore, or otherwise write any DAC.
No physical result is claimed. The next owner-controlled intervention is to merge this reviewed
branch into trusted `main`, if approved, then dispatch `signed-beta.yml` with
`candidate_target=black-pearl-ja11`; Pixel 9 remains blocked until that exact signed artifact is
verified.
