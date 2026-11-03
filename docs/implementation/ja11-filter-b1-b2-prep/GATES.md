# JA11 filter B1/B2 preparation — task contract

Outcome: Prepare the owner-selected Option B evidence tracks without hardware
access, purchases, installations, filter writes, product enablement, issue edits,
merges or publication. Deliver an offline-tested B1 read-only harness and session
package, a B2 capture/acquisition proposal, and a source-pinned ecosystem survey.

Approval: APPROVED 2026-11-03 by the owner, who selected Option B and requested
implementation of the plan. Hardware sessions remain separately owner-gated.
Base: a8290ba1a2f51cf980a62f913cb98a298cf16f28
Branch: main; clean at intake.

OWNS: GATES.md,
  docs/implementation/ja11-filter-b1-b2-prep/GATES.md,
  docs/implementation/ja11-filter-b1-b2-prep/GATES.md.sha,
  docs/implementation/ja11-filter-b1-b2-prep/B1_SESSION.md,
  docs/implementation/ja11-filter-b1-b2-prep/B2_CAPTURE_PROPOSAL.md,
  docs/implementation/ja11-filter-b1-b2-prep/ECOSYSTEM_SURVEY.md,
  docs/research/FIIO_JA11_DIGITAL_FILTER_RESEARCH_HANDOFF.md,
  docs/PROJECT_STATE.md,
  docs/FIIO_JA11_CAPABILITY_MATRIX.md,
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

- Device work is limited to preparation. No connection, send, install, flash,
  filter write, Apply or Save occurs in this task. Each B1 or B2 hardware session
  requires separate current owner approval.
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
