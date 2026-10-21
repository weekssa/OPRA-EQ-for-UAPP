# Issue #103: API 35 emulator Activity-focus reliability

## Scope and guardrails

- Primary tracker: [issue #103](https://github.com/weekssa/OPRA-EQ-for-UAPP/issues/103).
- Base: `origin/main` at `6047d45d0e8377f29bc9deb5580191ae44b37793`.
- Branch: `codex/issue-103-api35-focus` in the isolated issue-103 worktree.
- Keep changes limited to emulator/test infrastructure unless evidence proves an application defect.
- Keep the v0.8.3 release configuration, all 72 existing UI tests, their assertions, and required CI safeguards intact.
- Do not merge without owner approval. Draft PRs #108 and #111 are separate and must remain untouched.

## Historical evidence

Issue #103 records exact-head Android CI run `39507302003`, attempt 1, on source
`e955f62db0bb7ab96182ee3964aa579a6cb9d186`: 8 of 72 tests failed and 0 were skipped. The unchanged-SHA
retry passed 72/72 with 0 skipped. The attempt-1 instrumentation log shows failures across unrelated
UI paths, including D-pad export/editor/recovery, Back navigation, hardware-key search, accessibility,
and Black Pearl reset lifecycle. Diagnostics report `windowFocus=false`, `decorHasFocus=false`, no
focused view, 1080x2400 viewport, and hidden IME; Espresso also reports `RootViewWithoutFocusException`.
The retry passed despite an emulator-console startup warning, so that warning alone does not explain
the failure. The evidence demonstrates a real instrumentation failure but does not identify a single
root cause.

## Code inspection

- The CI workflow waited for ADB and `sys.boot_completed`, sent keyevent 82, then launched tests. It
  did not confirm that the emulator was unlocked or that its Home window held focus before starting
  instrumentation.
- `UiModernizationFlowsTest.waitForActivityWindowFocus` already waits up to 10 seconds for a resumed
  Activity with window focus, then asserts with window and IME diagnostics. The focus assertions and
  test count must remain intact.
- `MainActivity` has no custom window-focus lifecycle handling. Several affected Compose-only tests
  run through the test host rather than the product Activity. No application focus defect is
  demonstrated.

## Candidate correction

The workflow now dismisses the emulator keyguard, wakes the display, and waits up to 30 seconds for
an unlocked focused Home window before launching the suite. Failure of this precondition fails the CI
job with window and Activity diagnostics; no UI assertions are skipped or retried. After the tests,
CI captures logcat, window state, and Activity state as downloadable artifacts. No application,
test-source, dependency, version, or release configuration is changed.

This is a bounded readiness mitigation for the most plausible infrastructure gap, not proof that
early boot caused every historical focus failure. Repeat exact-head API 35 results are required before
claiming reduced flakiness.

## Validation record

- Unmodified base `6047d45d0e8377f29bc9deb5580191ae44b37793`, local API 35 Google APIs Pixel 6 arm64
  AVD `issue103_api35_pixel6`: `:app:connectedDebugAndroidTest` passed 72/72, 0 skipped, 0 failed,
  in 2m51s. This did not reproduce the historical failure and is not an x86_64 hosted-runner result.
- Candidate's unlocked focused-Home readiness shell logic passed on that API 35 AVD.
- Candidate on the same local API 35 AVD passed three sequential full runs: 72/72, 0 skipped, 0 failed
  each, in 2m03s, 2m03s, and 2m00s. The historical failure was not reproduced; these runs show no
  regression under local repetition but do not establish an x86_64 hosted-runner failure rate.
- Candidate source head `2f8409e9e7c3d2a8ee7b5351791eb3b536db4437`, PR #115, exact-head Android CI run
  `39551959584`: its build job passed documentation and release-promotion contracts, unit tests,
  lint, debug/release assembly, and minified-release/R8 checks. API 35 job `119066463106` passed
  72/72 with 0 skipped and 0 failed in 8m59s. The separate API 26 smoke job
  initially failed its catalog readiness assertion after observing a retryable catalog error; its
  same-head rerun `119066461359` passed. No smoke assertion was changed.
- A further API 35 repeat on the same exact source head, job `119069475293`, passed 72/72 with
  0 skipped and 0 failed. Its log shows the emulator readiness gate passed, every progress sample
  reported 0 skipped and 0 failed, and post-run logcat, WindowManager, and ActivityManager captures
  were uploaded in artifact `android-emulator-ui-test-reports` (ID `12323449036`). The run also
  emitted the same emulator-console startup warning as the historical passing retry, confirming
  that warning is not predictive of the focus failure.
- The CodeQL PR run `39551959547` passed on the same source head. The hosted API 35 repetition plus
  the local arm64 runs provide evidence that the readiness mitigation introduces no observed UI
  test regressions. Because the historical failure was intermittent and not reproduced, this small
  sample cannot prove a lower long-term failure rate or a unique causal mechanism.
- API 26 first-attempt smoke failure and successful unchanged rerun demonstrate a separate transient
  catalog-readiness event; its exact-head safeguard passed on retry and remains unchanged.
- A documentation-only validation update will produce a new PR head. Re-run the normal Android CI
  workflow and CodeQL on that final exact head, including at least two hosted API 35 runs, before
  handing the PR off.

## Resume checklist

1. Update this record with the final documentation commit SHA and rerun outcomes.
2. Verify normal Android CI and CodeQL against that exact final PR head, including two API 35 UI runs;
   inspect all test and emulator diagnostics.
3. Update issue #103 and PR #115 with exact SHAs, run/job IDs, pass/fail/skip counts, and uncertainty.
   Preserve application and release configuration, and leave PR #115 as draft.
4. Confirm draft PRs #108 and #111 remain untouched, and stop before merge for owner approval.
