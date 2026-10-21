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
root cause at the time of the initial investigation.

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

The first CI candidate dismissed the emulator keyguard, woke the display, and waited for an unlocked
focused Home window. Exact-head rerun on `9f1bdecb3e8fee29843b1ad9d9d68c6e6e95e9b7` disproved that
gate as sufficient: the API 35 job failed 8/72 tests with `RootViewWithoutFocusException`, matching
the historical failure class. Its uploaded `window.txt` shows a Nexus Launcher ANR at 06:36:09 with
reason `Input dispatching timed out (Application does not have a focused window)`. The gate checked
whether the current focus line contained the Home package; Android's `Application Not Responding:`
dialog includes that package name, so the unhealthy ANR surface passed the string check. The ANR
occurred before instrumentation and belongs to `com.google.android.apps.nexuslauncher`, not OPRA.

The corrected readiness gate matches a real Home activity window (`$home_package/`) and explicitly
rejects an `Application Not Responding:` focus surface. If the fresh emulator presents a Home ANR
before instrumentation, CI reboots that emulator once, waits for boot, and requires a responsive,
unlocked Home activity again. If recovery does not establish that precondition, the job fails with
window, last-ANR, ActivityManager, and emulator logs. No test is retried or skipped, and all 72 tests
must run on a ready emulator. Post-test logcat, window, and ActivityManager snapshots remain
downloadable. No application, test-source, dependency, version, or release configuration is changed.

## Validation record

- Unmodified base `6047d45d0e8377f29bc9deb5580191ae44b37793`, local API 35 Google APIs Pixel 6 arm64
  AVD `issue103_api35_pixel6`: `:app:connectedDebugAndroidTest` passed 72/72, 0 skipped, 0 failed,
  in 2m51s. This did not reproduce the historical failure and is not an x86_64 hosted-runner result.
- Candidate's unlocked focused-Home readiness shell logic passed on that API 35 AVD.
- Candidate on the same local API 35 AVD passed three sequential full runs: 72/72, 0 skipped, 0 failed
  each, in 2m03s, 2m03s, and 2m00s. The historical failure was not reproduced; these runs show no
  regression under local repetition but do not establish an x86_64 hosted-runner failure rate.
- Initial workflow candidate source head `2f8409e9e7c3d2a8ee7b5351791eb3b536db4437`, Android CI run
  `39551959584`: build, unit tests, lint, debug/release assembly, and R8 passed. API 35 jobs
  `119066463106` and `119069475293` passed 72/72, 0 skipped, 0 failed. API 26 initially failed its
  catalog readiness assertion, then passed unchanged rerun `119066461359`; no smoke assertion was
  changed. CodeQL run `39551959547` passed on this candidate.
- Exact final-documentation-head SHA `9f1bdecb3e8fee29843b1ad9d9d68c6e6e95e9b7`, run
  `39554995338`: CodeQL `39554995413`, build (unit tests, lint, debug/release assembly, R8), and API
  26 smoke passed. API 35 job `119072593792` passed 72/72, 0 skipped, 0 failed. Its same-head repeat
  `119076456986` reproduced 8 failures of 72, 0 skipped, including `RootViewWithoutFocusException`;
  artifact `12326003108` contains the evidence. The `window.txt` last-ANR record and focused-window
  dump identify the system Home ANR and the false-positive preflight check described above.
- Automatic Dependency Submission run `39555003303` succeeded on SHA
  `9f1bdecb3e8fee29843b1ad9d9d68c6e6e95e9b7`. Separate historical KSP 2.3.12 failure remains tracked
  in issue #114; no dependency change is in this PR.
- Corrected workflow and this failure analysis are now on a new candidate head. Run the full normal
  Android workflow and CodeQL on that exact head, repeat API 35 at least twice, and inspect whether
  the preflight performs recovery or refuses to start instrumentation on the ANR state. Failure
  results must remain visible; the prior passing runs do not erase this reproduced failure.

## Resume checklist

1. Run the full normal Android CI workflow and CodeQL on the new corrected-workflow head; repeat API
   35 at least twice and inspect readiness logs, test reports, and emulator snapshots.
2. Update issue #103 and PR #115 with exact SHAs, run/job IDs, pass/fail/skip counts, and uncertainty.
   Preserve application and release configuration, and leave PR #115 as draft.
3. Confirm draft PRs #108 and #111 remain untouched, and stop before merge for owner approval.
