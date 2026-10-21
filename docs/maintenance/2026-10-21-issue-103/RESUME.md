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

The second candidate matched a real Home activity window (`$home_package/`) and rejected an
`Application Not Responding:` focus surface. It still failed: the job's captured last-ANR time was
06:50:44, about one second before the readiness step reported the focused Home package. The
launcher ANR therefore raced the snapshot, and the gate started instrumentation immediately after
the ANR record was written.

Candidate `fbb03e96da28fccd0000c5e5d0b82831837dc564` added last-ANR inspection but its readiness
job failed before instrumentation because `cmd package resolve-activity ... HOME` returned
`com.google.android.googlesdksetup/.DefaultActivity` while WindowManager showed the real
Nexus Launcher focused and `topDisplayFocusedRootTask=... type=home`, with no ANR. The Android HOME
intent resolver selected a transient setup activity. Candidate `b1ad3e831facc5ae00e1d6d74d534a470a5c86b9`
then used the HOME role holder, but the hosted emulator temporarily returned
`com.google.android.googlesdksetup` there as well. The current gate no longer trusts either transient
package selection: it checks for a `type=home` top focused task and verifies that WindowManager's
focused window belongs to its focused ActivityRecord.

The current correction checks `dumpsys window lastanr` during the readiness wait for the specific
Home-launcher failure `Input dispatching timed out (Application does not have a focused window)`.
If that event occurred before instrumentation, CI reboots the fresh emulator once, waits for boot,
and requires a responsive, unlocked Home task with no new no-focused-window ANR record. A second
readiness recovery attempt showed another Home no-focused-window ANR during that same startup
sequence. The workflow had sent legacy `KEYCODE_MENU` (`82`) after both initial boot and recovery,
before confirming Home focus. Its timing coincides with the repeated ANR, so the current candidate
removes that input; wake and keyguard dismissal remain. This is an infrastructure hypothesis to
validate with exact-hosted runs, not a proven cause. If recovery does not establish readiness, the
job fails with window, last-ANR, ActivityManager, and emulator logs. No test is retried or skipped,
and all 72 tests must run on a ready emulator. Post-test logcat, window, and ActivityManager
snapshots remain downloadable. No application, test-source, dependency, version, or release
configuration is changed.

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
- First stricter-focus candidate `659ce3d68068bd5ff4ca9ee905b0d016f0cd29b7`, run
  `39557792944`: build, unit tests, lint, debug/release assembly, R8, API 26 smoke, and CodeQL passed.
  API 35 job `119081562822` again failed 8/72 with 0 skipped and `RootViewWithoutFocusException`.
  It reports a focused Home activity at readiness, but the uploaded artifact `12326698357` records
  a Home no-focused-window ANR at 06:50:44, roughly one second earlier. This confirms a race between
  the actual system ANR and a single focus snapshot; the test gate did not hide or retry it.
- Last-ANR candidate `fbb03e96da28fccd0000c5e5d0b82831837dc564`, Android CI run
  `39559156367`: API 35 readiness failed closed before starting the 72 tests because the package
  resolver selected SDK Setup instead of the actual HOME role holder. No UI result is claimed from
  that attempt; build, API 26, and CodeQL were still pending when this record was updated.
- HOME-role candidate `b1ad3e831facc5ae00e1d6d74d534a470a5c86b9`, Android CI run
  `39559603074`: API 35 readiness detected the no-focused-window ANR, rebooted once, then detected
  the same ANR pattern after the second boot and failed closed before instrumentation.
  Artifact log timestamps place the repeated ANR at 07:09:42 after recovery, with `mCurrentFocus`
  showing a SystemUI ANR dialog and `mFocusedApp` the Nexus Launcher. The workflow still sent
  legacy `KEYCODE_MENU` after each boot; that input is now removed, and the Home check is task-based.
- Automatic Dependency Submission run `39555003303` succeeded on SHA
  `9f1bdecb3e8fee29843b1ad9d9d68c6e6e95e9b7`. Separate historical KSP 2.3.12 failure remains tracked
  in issue #114; no dependency change is in this PR.
- The current last-ANR-aware, Home-task-based correction removes the pre-focus `KEYCODE_MENU` input.
  Run the full normal Android workflow and CodeQL on that exact head, repeat API 35 at least twice,
  and inspect whether startup reaches a responsive Home window without an ANR. Failure results must
  remain visible; earlier passing runs do not erase these reproductions.

## Resume checklist

1. Run the full normal Android CI workflow and CodeQL on the last-ANR-aware workflow head; repeat
   API 35 at least twice and inspect readiness logs, test reports, and emulator snapshots.
2. Update issue #103 and PR #115 with exact SHAs, run/job IDs, pass/fail/skip counts, and uncertainty.
   Preserve application and release configuration, and leave PR #115 as draft.
3. Confirm draft PRs #108 and #111 remain untouched, and stop before merge for owner approval.
