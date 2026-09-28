# Implementation report - Black Pearl and JA11 remediation

Status: `PHYSICAL_FAIL` for the owner-authorized Black Pearl Direct Flash exercise. The exact signed
candidate, software/emulator gates, and mandatory independent review passed; the single AFUL Explorer
hardware mutation did not reach verified success because final raw playback-gain readback mismatched.
The Black Pearl truthfulness defect's failure-path behavior passed, but the requested hardware state
was not verified and restoration was not attempted. JA11 remains independently `PHYSICAL_INCONCLUSIVE`.

## Exact signed candidate addendum — source `b61f02e8c91656f14ffc639e4c6d937b2162a1b7`

- Repository: `https://github.com/weekssa/OPRA-EQ-for-UAPP.git`.
- Owner-approved PR: [#50](https://github.com/weekssa/OPRA-EQ-for-UAPP/pull/50), merged into `main`.
- Merged source SHA: `b61f02e8c91656f14ffc639e4c6d937b2162a1b7`.
- Candidate target: `black-pearl-ja11`.
- Signed workflow: [run #1372](https://github.com/weekssa/OPRA-EQ-for-UAPP/actions/runs/36347148408),
  completed successfully in 9m53s; requested tasks included unit tests, lint, and minified release
  assembly, followed by signed-emulator install and cold launch.
- Candidate APK: `EQ-Library-v0.7.0-beta-b61f02e.apk`.
- APK SHA-256: `276587734fc863277b83e4310c040cee22c27261075e21fa75d766ded9eef27e`.
- Package/version: `com.weekssa.opraeqforuapp`, `0.7.0` / code `7`.
- Signer certificate SHA-256: `65c1c1256dae3c49e3548f334c91f0ba991969e9be9e0b223ba4e253d2114747`;
  APK signature schemes v2 and v3 verified.
- R8 mapping SHA-256: `864c6a6d27ccfdcae2e421ad318be988ea1c846d7f8421a1b8038fd523b608f5`.
- Immutable signed artifact: ID `10941292707`, digest
  `sha256:188d2e4b973e7aab420d4c7d3890dba3407415c1d2c8f935a418acdacdbd8fc3`.
- Exact candidate URL:
  `https://raw.githubusercontent.com/weekssa/OPRA-EQ-for-UAPP/mobile-test-apk/candidates/EQ-Library-v0.7.0-beta-b61f02e.apk`.
- Emulator diagnostics artifact: ID `10941003406`, digest
  `sha256:e041ee672816ff85ba0c866ff0bdfa8b8a616ec540f95c93f3fd632f19197d57`.
- Clean signed-emulator install: PASS (`Success`, install command completed).
- Cold launch: PASS (`Status: ok`, `LaunchState: COLD`, `Activity: ...MainActivity`, `Complete`).
- Before the owner-authorized physical session, Luna did not connect, mutate, flash, apply, reset,
  save, restore, or otherwise operate a DAC.

## Owner-authorized Pixel 9 physical execution addendum — 2026-09-27

- Exact candidate installed: source `b61f02e8c91656f14ffc639e4c6d937b2162a1b7`; APK
  `EQ-Library-v0.7.0-beta-b61f02e.apk`; APK SHA-256
  `276587734fc863277b83e4310c040cee22c27261075e21fa75d766ded9eef27e`; package
  `com.weekssa.opraeqforuapp`, version `0.7.0` / code `7`.
- Pixel: Google Pixel 9, `tokay`, Android API 37, wireless ADB serial
  `adb-46141FDAQ003KZ-3AwgSo._adb-tls-connect._tcp`.
- Black Pearl identity: vendor `0x3302`, product `0x43E8`, manufacturer `TTGK Technology`,
  product `TE-C`, serial `330243E8260129`; identity matched the maintained exact profile.
- Read-only baseline before mutation: app `Connected`; My DAC showed `Verified current hardware`,
  `Flat`, 10 filters, active slot 1, and playback gain `-25.00 dB`. The editor opened and closed
  without a transport write.
- One and only one authorized mutation: My EQs -> AFUL -> Explorer -> Flash. The confirmation
  identified `TRN Black Pearl: Optimized · native hardware rounding only`, source Hifiguides, and
  a displayed playback-gain adjustment of `-3.90 dB`.
- Terminal result: **not verified**. The app displayed `TRN Black Pearl Flash was not verified.
  Final hardware readback did not confirm the requested EQ.` It reported
  `Final Black Pearl playback-gain readback did not match: expected raw -7398, actual raw -6400.`
- Safety behavior: the app instructed `Reconnect or refresh the DAC before any later hardware
  action. Do not retry automatically.` The existing ViewModel then performed its normal read-only
  post-operation refresh while the session remained connected; no retry, Reset, Save, Restore,
  reconnect, or owner-directed recovery write was performed after the mismatch. The requested
  post-operation state remains **not verified**, and no restoration claim is made.
- Evidence directory: `/tmp/opra-black-pearl-flash-aful-192520/`. Terminal UI XML SHA-256
  `b853450bed1000ca07de478cda78b1d6eb6f575f4690dc960e4a95c579a8f9f3`; terminal screenshot SHA-256
  `9d9769f2b3452d88344ed24d1003702f8d915170739ed4aaf94cb0461f0a1a3b`; USB evidence SHA-256
  `ad17ca6edca249fce742bda2f28e6e4aae5d6a28f88795e17c16823a7aa65a3d`.
- Classification: Black Pearl truthfulness failure-path behavior **PASS**; AFUL Explorer requested
  Direct Flash / final hardware verification **PHYSICAL_FAIL**; Black Pearl restoration **NOT
  VERIFIED**. This does not establish public hardware support.

## Owner-authorized read-only recovery addendum — 2026-09-27

- The app session was closed and reopened, then `Connect` was tapped once using the existing
  Android USB permission. No DAC write control was invoked during recovery.
- Fresh My DAC readback showed `Verified current hardware`, `Matches My EQs`, `Explorer`,
  `Optimized · native hardware rounding only`, 10 filters, active slot 1, and playback gain
  `-25.00 dB` (raw `-6400`). The AFUL Explorer Flash target had required raw gain `-7398`.
- Recovery therefore proves a partial result: the ten-band EQ state matched the selected Explorer
  representation, while the requested global playback-gain target did not. The original Flash
  remains **PHYSICAL_FAIL** as a complete transaction, and no restoration was attempted.
- Evidence directory: `/tmp/opra-black-pearl-recovery-1933/`. Fresh My DAC XML SHA-256
  `12a1a3195a6bc392aae9ac344c880ef62edd39a8ab945eb3e65c33ad63635c1c`; screenshot SHA-256
  `9dacda703ca0fa9a85b68cb7432307021944178589be063499a57b2baf72efce`; USB evidence SHA-256
  `ad17ca6edca249fce742bda2f28e6e4aae5d6a28f88795e17c16823a7aa65a3d`.

## Historical pre-candidate repair addendum — source `9f5cb852994e1c88fce80598f249a97fae047429`

This addendum records the pre-integration owner-authorized, narrowly scoped repair after the
owner reported that the Android UAPP routing prompt also occurs in the stock app. That prompt is
expected stock-device/platform behavior and remains unchanged; it is not reclassified as an app
failure and was not retested on hardware. The exact-candidate addendum above supersedes this
pre-candidate status.

### Exact source and boundary

- Repository: `https://github.com/weekssa/OPRA-EQ-for-UAPP.git` (remote verified exact).
- Refreshed `origin/main`: `acaf4dd32ddd9379ec2860e45e34fb8039219583`.
- Branch: `codex/black-pearl-ja11-remediation-20260927`.
- Branch merge-base with refreshed `origin/main`: `3e8ff5d3f6cb750a627f76aba50f09645c0b41d3`.
- Final local source SHA: `9f5cb852994e1c88fce80598f249a97fae047429`.
- Local commit: `9f5cb852 Repair JA11 editor terminal feedback`.
- Worktree: `/Users/stephenweeks/.codex/worktrees/black-pearl-ja11-remediation-20260927/OPRA-EQ-for-UAPP`.
- No merge, tag, publication, branch push, or DAC mutation occurred in this follow-up.

### Repair result

- `FiioJa11OperationPresentation` now uses truthful `Apply successful` wording for verified
  `EDITOR_APPLY`, retains the required final-readback sentence, and labels failed Apply as not
  verified without automatic retry guidance.
- `FiioJa11MyDacContent` now renders exactly one compact live-region terminal surface for a
  completed `EDITOR_APPLY` whose trace ID matches the completed status ID. It exposes readable and
  technical operation reports, a 48 dp dismiss action, deterministic eight-second verified-success
  expiry, and persistent actionable failure/uncertainty until dismissed or recovered.
- `EqLibraryApp` clears a prior JA11 Flash feedback surface when `EDITOR_APPLY` starts or
  completes, preventing simultaneous stale/global feedback with the inline Apply result.
- No `FiioJa11Flasher`, JA11 protocol, USB transport, reconnect, Save, session, retry, or hardware
  mutation behavior changed. The stock Android UAPP routing prompt remains outside this repair.

### Follow-up checks

- `./tools/codex-android :app:testDebugUnitTest` — PASS.
- `ANDROID_SERIAL=emulator-5554 ./tools/codex-android :app:connectedDebugAndroidTest
  -Pandroid.testInstrumentationRunnerArguments.class=com.weekssa.opraeqforuapp.ui.screens.FiioJa11MyDacContentTest` — PASS, 4/4.
- `ANDROID_SERIAL=emulator-5554 ./tools/codex-android :app:connectedDebugAndroidTest` — PASS,
  24/24 on `codex-api36` API 36; the Pixel 9 was visible but not selected or mutated.
- `./tools/codex-android :app:lintDebug` — PASS.
- `./tools/codex-android :app:assembleDebug` — PASS.
- `./tools/codex-android :app:assembleRelease` — PASS; local unsigned minified release only.
- `bash tools/verify-r8-mapping.sh app/build/outputs/mapping/release/mapping.txt` — PASS.
- `git diff --check` — PASS.
- `actionlint` — `NOT RUN`; unavailable locally, matching the owner-provided boundary.
- Remote CI, CodeQL, dependency submission, and signed-beta provenance for this new SHA — `NOT
  RUN`/unavailable because no push or main integration occurred; prior runs belong to prior SHAs.

### Candidate boundary

No exact signed beta was produced for `9f5cb852994e1c88fce80598f249a97fae047429`. Local artifacts
are not handoff candidates: debug `app-debug.apk` SHA-256
`79e75c4a39e3a0aeb8b7231643ca4306e29db3578c23124028402b2945ac5fc6` and unsigned minified
`app-release-unsigned.apk` SHA-256
`945e330a1eb510d68405603f19f50770f06abda6b931c77ce9dbe33d6d40d746`.

The owner must separately authorize the minimum main integration or a narrowly reviewed combined
manifest/signing workflow path before an exact signed candidate can exist. This worker must not
weaken the main-only signing guard or label the earlier `acaf4dd` artifact as this repair.

## Source and evidence identity

- Repository: `https://github.com/weekssa/OPRA-EQ-for-UAPP.git`
- Refreshed base before implementation: `origin/main` at `ce5efdf7985e4fc48f975b14fcedb1f592d43772`
- Live `origin/main` after the required refresh: `0adcc8159a467790104cf2dc797f1279ef2c53ed`
- Branch: `codex/black-pearl-ja11-remediation-20260927`
- Worktree: `/Users/stephenweeks/.codex/worktrees/black-pearl-ja11-remediation-20260927/OPRA-EQ-for-UAPP`
- Final implementation source SHA: `dc6478a25b1745b4f78f833c69e96e066c615d56`
- Promotion-preparation source SHA: `0f080516e8d4aa34e9d0a04b16464bf65b2b6d7d`
- Reviewed implementation/workflow head: `0c77e081fd6abd12a6e20b482ce269ae9f5bb764`
- Final documentation evidence head before owner-approved merge: `3e8ff5d3f6cb750a627f76aba50f09645c0b41d3`
  (documentation-only commits atop the reviewed implementation/workflow head).
- Owner-approved merge commit on `main`: `acaf4dd32ddd9379ec2860e45e34fb8039219583`.
- Review PR: [#49](https://github.com/weekssa/OPRA-EQ-for-UAPP/pull/49), exact head
  `3e8ff5d3f6cb750a627f76aba50f09645c0b41d3`, base `0adcc8159a467790104cf2dc797f1279ef2c53ed`;
  owner-approved and merged as `acaf4dd32ddd9379ec2860e45e34fb8039219583`.
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
- `.github/workflows/signed-beta.yml` Ruby YAML parse - PASS; `actionlint` v1.7.12 - PASS on the
  exact candidate workflow using `/Users/stephenweeks/.local/bin/actionlint`.
- GitHub Actions `Automatic Dependency Submission (Gradle)` run `#2229`
  (`36333163159`) - PASS on exact source `7898378e20a610a4667e861220fef40c9d8ac751`.

### PR #49 remote checks at final documentation evidence head `3e8ff5d3f6cb750a627f76aba50f09645c0b41d3`

- Android CI run `#1874` (`36334738869`) - PASS: build, min-API smoke, and emulator UI test.
- CodeQL run `#1758` (`36334738860`) - PASS: Analyze Kotlin.
- Priority community coverage CI run `#1627` (`36334738871`) - PASS.
- Catalog currentness CI run `#2142` (`36334738846`) - PASS.
- Automatic Dependency Submission (Gradle) run `#2231` (`36334735111`) - PASS.
- PR #49 was mergeable after checks and was subsequently approved and merged by the owner. The
  remote push warning reported 51 existing default-branch Dependabot findings; this worker did not
  reinterpret or suppress them.

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
- Result: PASS; trusted workflow run [#1371](https://github.com/weekssa/OPRA-EQ-for-UAPP/actions/runs/36336477287)
  built and published the combined `black-pearl-ja11` candidate from exact source
  `acaf4dd32ddd9379ec2860e45e34fb8039219583`.
- APK: `EQ-Library-v0.7.0-beta-acaf4dd.apk`.
- APK SHA-256: `af83a5e0148263057b1c43e3b775157ab6aedd9d2c1e3ab0558cef8fa3cea665`.
- Package/version: `com.weekssa.opraeqforuapp`, version `0.7.0`, code `7`.
- Signer certificate SHA-256: `65c1c1256dae3c49e3548f334c91f0ba991969e9be9e0b223ba4e253d2114747`,
  matching the pinned `release-signing-cert.sha256`.
- Immutable runtime artifact: ID `10937890771`; digest
  `sha256:88b1555e9a14642874110d05d5c5ae3554c25cc64b9385898a78287ccea9f52d`.
- Exact candidate URL:
  `https://raw.githubusercontent.com/weekssa/OPRA-EQ-for-UAPP/mobile-test-apk/candidates/EQ-Library-v0.7.0-beta-acaf4dd.apk`.
- Exact checksum URL:
  `https://github.com/weekssa/OPRA-EQ-for-UAPP/blob/mobile-test-apk/candidates/EQ-Library-v0.7.0-beta-acaf4dd.apk.sha256`.
- Signed emulator evidence: `opra_signed_beta`, Android 35, x86_64; signed APK install succeeded
  in 3172 ms, and cold launch returned `Status: ok`, `LaunchState: COLD`, activity
  `com.weekssa.opraeqforuapp/.MainActivity`.
- Candidate workflow job [108668179602](https://github.com/weekssa/OPRA-EQ-for-UAPP/actions/runs/36336477287/job/108668179602)
  completed successfully in 10m 28s. No debug APK is presented as the physical-test candidate.

## Promotion-preparation validation

- `git merge --no-ff --no-edit origin/main` - PASS; only the three refreshed catalog files were
  brought onto the remediation branch.
- `git diff --check` - PASS after the workflow change.
- Ruby YAML parse of `.github/workflows/signed-beta.yml` - PASS.
- `/Users/stephenweeks/.local/bin/actionlint .github/workflows/signed-beta.yml` - PASS; actionlint
  v1.7.12, exact candidate workflow, no findings.
- The owner-approved signed workflow dispatch completed as run `#1371` after merge to `main`; exact
  candidate provenance is recorded in the signed-candidate section above.

## Pixel 9 controlled JA11 transaction evidence — 2026-09-27

This session used the exact signed candidate from source SHA
`acaf4dd32ddd9379ec2860e45e34fb8039219583`:

- APK: `EQ-Library-v0.7.0-beta-acaf4dd.apk`
- APK SHA-256: `af83a5e0148263057b1c43e3b775157ab6aedd9d2c1e3ab0558cef8fa3cea665`
- Package/version: `com.weekssa.opraeqforuapp`, `0.7.0` / code `7`
- Pixel: Google Pixel 9, codename `tokay`, Android 17 / API 37
- ADB target: `adb-46141FDAQ003KZ-3AwgSo._adb-tls-connect._tcp`
- JA11 identity: VID/PID `0x2972/0x0102`; Android USB host mode remained active
- Evidence folder: `/tmp/opra-pixel-automated.ZRPv17/`

The read-only baseline was verified as User 1 with five PEAK bands at 80/250/1000/4000/12000 Hz,
all gains `+0.00 dB`, Q `0.70`, and global EQ gain `0.00 dB`. Opening, editing, Review, local
Reset edits, Back, and Close were exercised before the first write; the USB connection count stayed
at `273` throughout those local-only actions.

### Controlled Apply and restore

1. **Apply test — hardware state PASS:** one reviewed change, Band 1 gain `0.0 -> -1.0 dB`.
   Review showed all five bands, global gain, headroom, one hardware value change, and the
   first-write boundary. One Apply was sent. After the JA11 reconnect boundary and dismissal of
   the Android “Open USB Audio Player PRO to handle JadeAudio JA11?” prompt, the fresh current
   hardware view showed Band 1 `-1.00 dB`, all other values unchanged, and global gain `0.00 dB`.
   USB connection count advanced `273 -> 275`.
2. **Restore test — hardware state PASS:** one reviewed change, Band 1 gain `-1.0 -> 0.0 dB`.
   One restoration Apply was sent. The same Android UAPP routing prompt appeared and was canceled.
   The final fresh current hardware view showed the original flat five-band state and global gain
   `0.00 dB`. USB connection count advanced `275 -> 277`.

No Apply was retried, no Reset/Flash/Restore action was used, and no Black Pearl or EW300 hardware
was touched. The installed APK was pulled after the session and its SHA-256 still matched the exact
candidate checksum.

### Physical evidence classification

- JA11 immediate hardware readback and original-state restoration: **PASS**.
- Complete named JA11 physical qualification: **PHYSICAL_INCONCLUSIVE**. The Android USB-routing
  dialog obscured the transient terminal feedback during each reconnect, and the post-dialog app
  surface did not retain an observable editor success sentence or expose an exported operation
  trace proving the exact Save count to this worker. The observed final states are evidence of the
  requested readback values, not permission to infer missing trace fields or a broader persistence,
  acoustic, or public-support claim.
- Black Pearl physical qualification: **NOT EXERCISED**.
- Luna performed only the two owner-authorized JA11 Apply operations and the required read-only
  final verification; no additional hardware action followed the final flat readback.

## Hardware boundary

The earlier pre-Pixel statement is superseded by the controlled JA11 session above. Luna did perform
the two owner-authorized JA11 Apply operations and restored the original state. No Black Pearl or
EW300 hardware was connected or mutated. No public hardware-support claim is made.

## Owner-authorized Black Pearl read-only protocol-diagnosis capture — 2026-09-27

This capture followed the exact-candidate Black Pearl physical failure and was explicitly read-only.
The diagnostic APK used a temporary debug-only application-id suffix so the signed candidate package
`com.weekssa.opraeqforuapp` and its data were not replaced. The temporary raw-read logger and build
suffix were reverted after capture, and the diagnostic package was uninstalled. No Flash, Reset,
Apply, Save, Restore, or retry was invoked by this capture.

Device and identity:

- Pixel 9, codename `tokay`, Android API 37.
- ADB serial: `adb-46141FDAQ003KZ-3AwgSo._adb-tls-connect._tcp`.
- Black Pearl: TTGK Technology / TE-C, VID `0x3302`, PID `0x43E8`, serial `330243E8260129`.
- The first temporary-permission response did not grant a usable session and Android re-enumerated
  the DAC; one bounded second connection used the persistent permission choice. The successful
  session then reached `Connected` and My DAC showed `Verified current hardware`, `Unknown EQ`,
  10 filters, active slot 1, and playback gain `-25.00 dB`.

Read-only raw evidence:

- Evidence directory: `/tmp/opra-black-pearl-diagnostic-20260927-195236/`.
- Filtered diagnostic log SHA-256:
  `5250ef7845041f55facea336790e65940d3a37dd7d2b8ffbee84bf6f678f11bf`.
- Full threadtime log SHA-256:
  `7512e300995553cb4093bd4a1820e5b32558b7cea590b49b42994c6657003b04`.
- UI XML SHA-256: `f70154becea4f84d2a81f96fc0c4f61909ef1b9262ea36eefc3b3cc0f98b7e3b`.
- Screenshot SHA-256: `fb3a4b326b2c3f51b4ae0bb3318fb2f765e32f5032a0329c4a61002d0752a74c`.
- The trace captured the complete ten-band read sequence, global-gain read, version/feature reads,
  and session/control reads. The global-gain request was `4B 80 03 00 00 00 00 ...`; the response
  was `4B 80 03 02 00 E7 FF FF ...`. Bytes `00 E7` are the maintained little-endian signed raw
  value `-6400`, matching the UI's `-25.00 dB` and the prior final-readback mismatch.

Diagnosis:

- The capture corroborates the maintained report envelope, read opcode, global-gain command, and
  little-endian raw readback representation. An independent read-only Black Pearl controller source
  also uses report ID `0x4B`, read `0x80`, global-gain command `0x03`, and the same control-transfer
  envelope; it is research corroboration only and no code was copied.
- The capture does not contain a write, so it cannot prove whether the device ignored the earlier
  requested gain write or whether a device/firmware/variant boundary caused the mismatch.
- No safe source fix is proven. The production source, protocol bytes, gain math, tolerance, timing,
  report order, retry policy, and UI behavior remain unchanged. The exact signed candidate is not a
  successful Black Pearl hardware candidate, and no new beta push is recommended from this evidence.

The final statement for this run is: Black Pearl truthfulness/fail-closed behavior **PASS**;
requested AFUL Explorer Flash and restoration **PHYSICAL_FAIL / NOT VERIFIED**; read-only protocol
diagnosis **COMPLETE, no source correction identified**. Luna did not mutate hardware during this
diagnostic capture.

## Owner-authorized write-side diagnostic Flash — 2026-09-27

This section records one later owner-authorized mutation separately from the read-only capture above.
The authorization was: `Authorize one Black Pearl write-side diagnostic Flash for AFUL Explorer with
raw request/response capture and no automatic retry.` The owner then approved it with `apporoved`.
Exactly one Flash confirmation was accepted; no second mutation, retry, Reset, Save, Restore, or
owner-directed recovery write followed.

### Diagnostic package and setup

- Pixel: Google Pixel 9, codename `tokay`, API 37.
- ADB serial: `adb-46141FDAQ003KZ-3AwgSo._adb-tls-connect._tcp`.
- Black Pearl: TTGK Technology / TE-C, VID `0x3302`, PID `0x43E8`, serial `330243E8260129`.
- Temporary package: `com.weekssa.opraeqforuapp.bpwdiag`; it was built from the clean source head
  `01a968cac7a61f8cccd30cd26dfbce4c1fab7235` plus uncommitted debug-only instrumentation and was
  uninstalled after the attempt. The signed package `com.weekssa.opraeqforuapp` remained installed.
- Build command: `./tools/codex-android :app:assembleDebug
  -PBLACK_PEARL_DIAGNOSTIC_APPLICATION_ID_SUFFIX=.bpwdiag`.
- The isolated app was populated locally with the catalog profile
  `eq-library:autoeq-8caf93a11a25c9d37760cfa5@rev-3ae1a6c820b3402dc7fc8b54`; its source text was
  `AutoEq · Latest · Database: Jaytiss · Measurement: Jaytiss · Source: AutoEQ` and its preamp was
  `-6.00 dB`.

### Result and evidence

- The Flash confirmation displayed `TRN Black Pearl: Optimized · native hardware rounding only`
  and a `-6.00 dB` playback-gain adjustment. One confirmation tap was accepted.
- The post-operation My DAC surface showed `Verified current hardware`, `Matches My EQs`, the
  AutoEq/Jaytiss Explorer profile, 10 filters, active slot 1, and playback gain `-31.00 dB`.
- Evidence directory: `/tmp/opra-black-pearl-write-diagnostic-20260927-2015/`.
- Final My DAC UI XML SHA-256:
  `e61bb28ddc6003e868b7960b8c06afda72c7afd7fd74372364124979bf1a93ff`.
- Final My DAC screenshot SHA-256:
  `bed1d65d4f57fea38c78bb3a8ccbc0b0f54ddd7406b1c3191ab90197bb8d48a1`.
- Captured terminal log SHA-256:
  `a6d8470cc05e6b5333e75a44a09e87174a02c2bcf5601e9bd7d972128a50f2d7`.
- The final screenshot visibly shows the connected Black Pearl, verified current hardware, matching
  My EQs, 10 filters, active slot 1, and `Playback gain -31.00 dB`.

### Capture limitation and disposition

The temporary logger attempted to identify write reports with `report[0] == 0x01`. The maintained
Black Pearl report layout uses report ID `0x4B` at byte 0 and the write marker `0x01` at byte 1, so
the logger emitted zero `BlackPearlUsb` write-report lines. Consequently, this run did **not**
capture raw write bytes or the `controlTransfer` return value; it is not raw request/response proof.

The UI result is an observation for the AutoEq/Jaytiss Explorer profile only. It is not evidence that
the earlier Hifigues community Explorer target (expected raw `-7398`, observed raw `-6400`) was
repaired, and it does not justify a protocol or gain-codec change. The current physical state was
not restored by Luna because the authorization prohibited an automatic restore. The temporary code
was reverted, `git diff --check` passed, the diagnostic package was uninstalled, and the source
worktree returned clean at `01a968cac7a61f8cccd30cd26dfbce4c1fab7235`. Current disposition remains
**REPAIR_REQUIRED** with no new pushable fix.

## External reference comparison and guardrail blocker — 2026-09-27

The owner supplied the public `Matr1x01/trnBlackPearlEq` implementation as a working reference.
It was inspected read-only at Android branch commit
`45bbf3c65c899181395eb7936615ced1fbd5d4be` and protocol branch commit
`cd1ed0783134723d3c0a69088d739ac965354883`. No source was copied and the repository was not
modified. The reference Android transport (`UsbHidTransport.kt`) discovers both interrupt endpoints;
when an OUT endpoint exists it sends the unchanged 64-byte report with `bulkTransfer`, and uses HID
`SET_REPORT` only as a fallback. Its protocol reports corroborate OPRA's existing report ID, command
IDs, 64-byte framing, and little-endian gain representation; no wire-byte or codec correction is
justified by this comparison.

The exact Black Pearl descriptor captured during the authorized run shows HID interface 0 with
interrupt IN endpoint `0x86` and interrupt OUT endpoint `0x05`, both with 64-byte maximum packets.
OPRA's `AndroidBlackPearlUsbTransport` currently retains only the IN endpoint and sends every write
through `controlTransfer`. This transport-path difference is the strongest current hypothesis for
the earlier observation that PEQ readback changed while the global-gain readback remained at raw
`-6400`, but it is not proven without a controlled write using the alternate path.

Implementing that alternate path would change OPRA's USB endpoint behavior. The remediation prompt
explicitly prohibits changing endpoints, so no source edit was made. Current status remains
**REPAIR_REQUIRED / BLOCKED — boundary requires owner decision**; there is no candidate to push.
