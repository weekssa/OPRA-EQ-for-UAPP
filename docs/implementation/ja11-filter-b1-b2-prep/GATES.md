# JA11 filter B1/B2 preparation — task contract

Outcome: Prepare the owner-selected Option B evidence tracks without hardware
access, purchases, installations, filter writes, product enablement, issue edits,
merges or publication. Deliver an offline-tested B1 read-only harness and session
package, a B2 capture/acquisition proposal, and a source-pinned ecosystem survey.

Approval: APPROVED 2026-11-03 by the owner, who selected Option B and requested
implementation of the plan. A later explicit approval covered one exact B1
read-only session after the JA11 was connected to the Mac; that attempt stopped
before any filter request. A further explicit approval covered one corrected
repeat: the volume gate passed and all three filter reads were silent. No further
device session is authorized. A final closeout approval covers the validation
ledger, archived B1 evidence, issue #132 annotation and closure, independent
review, and one documentation/evidence commit.
Base: 9016bc0dd549806b9ccceb68bd130bfb5f3ad07d
Branch: main; clean at intake.

OWNS: GATES.md,
  docs/implementation/ja11-filter-b1-b2-prep/GATES.md,
  docs/implementation/ja11-filter-b1-b2-prep/GATES.md.sha,
  docs/implementation/ja11-filter-b1-b2-prep/B1_SESSION.md,
  docs/implementation/ja11-filter-b1-b2-prep/B2_CAPTURE_PROPOSAL.md,
  docs/implementation/ja11-filter-b1-b2-prep/ECOSYSTEM_SURVEY.md,
  docs/research/FIIO_JA11_DIGITAL_FILTER_RESEARCH_HANDOFF.md,
  docs/FIIO_JA11_VALIDATION_LEDGER.md,
  docs/PROJECT_STATE.md,
  docs/FIIO_JA11_CAPABILITY_MATRIX.md,
  docs/implementation/ja11-filter-b1-b2-prep/evidence/2026-11-03-r2/README.md,
  docs/implementation/ja11-filter-b1-b2-prep/evidence/2026-11-03-r2/SHA256SUMS,
  docs/implementation/ja11-filter-b1-b2-prep/evidence/2026-11-03-r2/harness.swift,
  docs/implementation/ja11-filter-b1-b2-prep/evidence/2026-11-03-r2/manifest.json,
  docs/implementation/ja11-filter-b1-b2-prep/evidence/2026-11-03-r2/session.jsonl,
  tools/ja11_filter_probe.py,
  tools/test_ja11_filter_probe.py

## Gates

1. **Approval, seal and scope**

   CHECK:

   ```sh
   test -s docs/implementation/ja11-filter-b1-b2-prep/GATES.md.sha &&
   /Users/stephenweeks/Documents/Codex/Codex-Usage-Policies/tools/scope-guard --root . --base a8290ba1a2f51cf980a62f913cb98a298cf16f28
   ```

   EXPECT: the owner approval is recorded, the seal matches, and every change is
   inside OWNS.

2. **B1 vectors and offline safety**

   CHECK:

   ```sh
   python3 tools/test_ja11_filter_probe.py
   python3 tools/ja11_filter_probe.py plan
   ```

   EXPECT: tests pass. The plan prints exactly the four authorized requests in
   order, including the J034 replicate `02 BB 0B 00 00 11 00 00 EE`, computed
   CRCs `3A`, `28`, and `67`, one-send/700-ms limits, and no filter-set frame.
   Hardware mode rejects execution without the explicit session switches.

3. **B1 session package**

   CHECK:

   ```sh
   rg -n 'separate.*session|one send|700 ms|no retry|stop|J034 replicate|restoration|unproven' docs/implementation/ja11-filter-b1-b2-prep/B1_SESSION.md
   ```

   EXPECT: the exact four requests, same-host/session rule, volume gate,
   pre-registered 2x2 limits, stop conditions, evidence requirements, J034
   session-only exception, restoration limits and what remains unproven are all
   explicit.

4. **B2 capture and acquisition proposal**

   CHECK:

   ```sh
   rg -n 'KA15|Cynthion|Beagle USB 480|separate.*approval|acquisition|official app|Apply|Save|sibling-specific' docs/implementation/ja11-filter-b1-b2-prep/B2_CAPTURE_PROPOSAL.md
   ```

   EXPECT: the three named candidates and observed advertised prices are present;
   acquisition, setup and the future sibling-setting capture each require a
   separate owner decision; passive capture and restoration are specified; the
   result remains sibling-specific.

5. **Source map and final handoff**

   CHECK:

   ```sh
   git diff --check &&
   git diff --name-only a8290ba1a2f51cf980a62f913cb98a298cf16f28
   ```

   EXPECT: only OWNS paths changed; eight repositories have pinned revisions,
   licences, transport and filter classifications; filters stay disabled; no
   device or external action is claimed.

## Constraints

- Device work includes the owner-approved B1 attempt and corrected repeat recorded
  in B1_SESSION.md. No further filter request, install, flash, filter write,
  Apply or Save occurs in this task. Any further B1 attempt or B2 hardware
  session requires separate current owner approval.
- The only future JA11 session in this package is B1's four reads. Its zero-CRC
  J034 replicate is valid only inside that exact approved session and cannot be
  repeated elsewhere or after failure.
- No purchase occurs. B2 hardware must be presented for a separate acquisition
  decision after availability, compatibility and a current quotation are checked.
- B2's analyzer is passive; the future official-app filter selection and restore
  are setting writes and need explicit session approval. Sibling evidence never
  establishes JA11 capability.
- Product filter support remains disabled and deferred. A valid B1 reply proves
  only readback on the tested tuple; mode meanings, selection, acoustic effect,
  persistence and restoration remain unproven.
- Final closeout may update issue #132 and create one commit containing only the
  approved documentation, evidence and harness changes. No product code, release,
  purchase or further device action is authorized.

## Review

Implementation and evidence were verified after the B1 repeat.

- `python3 tools/test_ja11_filter_probe.py`: PASS, 12 tests.
- Full Python discovery: 305 tests attempted; 301 passed and 4 pre-existing errors
  remain. Two modules cannot import on the installed Python 3.9 because they use
  `str | None`; two v0.9-beta release-contract tests reject the current stable
  `0.9.0` tree. None touches the B1 harness or evidence.
- `./tools/codex-android :app:testDebugUnitTest --rerun-tasks` with a fresh
  task-specific Gradle home: PASS, 834 tests across 131 suites, zero failures,
  errors or skips.
- `./tools/codex-android :app:lintDebug`: PASS.
- `./tools/codex-android :app:assembleDebug`: PASS.
- `./tools/codex-android :app:compileDebugAndroidTestKotlin
  :app:assembleDebugAndroidTest :app:lintRelease :app:assembleRelease`: PASS
  (108 tasks, including R8-minified release packaging). No APK was installed.
- `python3 tools/validate-repository-foundation.py`: PASS.
- B1 evidence assertions: PASS for the exact `R/S/S/S` outcomes, volume `60`,
  identity `2972:0102`, four send events and no stopped session.
- `git diff --check` and sealed `scope-guard`: PASS.

The first forced JVM rerun hit a pre-existing Gradle cache failure where the test
worker lacked `GradleWorkerMain`; rerunning with a fresh task-local Gradle home
reached and passed all tests. No application source or configuration was changed
to work around it.

The B1 hardware result is bounded to one JA11 firmware-2.20 unit on macOS. Its
three approved `0x11` reads were silent; volume passed with a valid response CRC.
No filter write, Apply, Save, install or purchase occurred. The J034 replay
authority is exhausted.
