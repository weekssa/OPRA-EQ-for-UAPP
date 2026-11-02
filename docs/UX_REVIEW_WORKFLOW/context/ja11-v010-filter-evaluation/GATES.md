# JA11 digital-filter evaluation — task contract

Outcome: Produce a classified JA11 v0.10.0 filter evaluation and three-way
decision, readiness audit, physical-session draft, two status additions,
and two issue drafts. No product implementation or device execution.

Approval: APPROVED 2026-11-02 by the owner, against the corrected plan revision.
Base: ab243d4f582f93fa1e79246006d987504693e5fd
Branch origin: codex/workflow-gates-20261102; this branch, not main, until the
workflow branch is merged, because it carries the root GATES.md pointer.

OWNS: GATES.md,
  docs/UX_REVIEW_WORKFLOW/context/ja11-v010-filter-evaluation/GATES.md,
  docs/UX_REVIEW_WORKFLOW/context/ja11-v010-filter-evaluation/GATES.md.sha,
  docs/UX_REVIEW_WORKFLOW/context/ja11-v010-filter-evaluation/EVALUATION.md,
  docs/UX_REVIEW_WORKFLOW/context/ja11-v010-filter-evaluation/READINESS.md,
  docs/UX_REVIEW_WORKFLOW/context/ja11-v010-filter-evaluation/OWNER_SESSION_DRAFT.md,
  docs/UX_REVIEW_WORKFLOW/context/ja11-v010-filter-evaluation/PROJECT_STATE_DRAFT.md,
  docs/UX_REVIEW_WORKFLOW/context/ja11-v010-filter-evaluation/ISSUE_DRAFTS.md

External research-note writes, if separately authorized:
  /Users/stephenweeks/.codex/research/ja11-v010-slice{0,1,2,3,4,5,6}.md
Exact vendor JSON stays private; never commit APKs or extracted vendor data.

## Gates

These document-display commands support substantive review; they do not
establish the truth of document contents.

1. **Approval, seal, and scope**

   CHECK:

   ```sh
   test -s docs/UX_REVIEW_WORKFLOW/context/ja11-v010-filter-evaluation/GATES.md.sha &&
   /Users/stephenweeks/Documents/Codex/Codex-Usage-Policies/tools/scope-guard --root . --base ab243d4f582f93fa1e79246006d987504693e5fd
   ```

   EXPECT: owner approval is recorded for the final contract; the seal exists
   and matches; the root pointer resolves correctly; scope-guard exits 0 with
   no outside or denied changes. Check before any commit. Missing seal or
   approval fails even if scope-guard passes.

2. **Evidence and determination**

   CHECK:

   ```sh
   cat docs/UX_REVIEW_WORKFLOW/context/ja11-v010-filter-evaluation/EVALUATION.md
   ```

   EXPECT: all seven slices have bounded outcomes, named sources, classifications
   and null-result meanings; APK discrepancy is preserved; exact sibling JSON has
   private provenance or remains explicitly unverified; the correct newer-record
   query is specified; B4/B5 and C7(a)/(b)/(c) are adjudicated without unsupported
   positive claims.

3. **Implementation readiness**

   CHECK:

   ```sh
   cat docs/UX_REVIEW_WORKFLOW/context/ja11-v010-filter-evaluation/READINESS.md
   ```

   EXPECT: dormant paths, false flag, UI gating, decoder weakness, missing device
   fixtures, future test scenarios, and readback/lifecycle/persistence/restoration
   requirements are explicit. No current API or code change, and no prepared
   filter-write payload.

4. **Owner package and enforcement gaps**

   CHECK:

   ```sh
   cat docs/UX_REVIEW_WORKFLOW/context/ja11-v010-filter-evaluation/OWNER_SESSION_DRAFT.md docs/UX_REVIEW_WORKFLOW/context/ja11-v010-filter-evaluation/PROJECT_STATE_DRAFT.md docs/UX_REVIEW_WORKFLOW/context/ja11-v010-filter-evaluation/ISSUE_DRAFTS.md
   ```

   EXPECT: one conditional consolidated session with fresh baseline and
   restoration; two accurate status additions; exactly two issue drafts, one per
   gap, each containing the H criteria and the current classification verbatim.
   No issue filing or device authority implied.

5. **Final diff and handoff**

   CHECK:

   ```sh
   git diff --check &&
   git diff --name-only ab243d4f582f93fa1e79246006d987504693e5fd
   ```

   EXPECT: clean whitespace and only OWNS paths changed; no binaries, vendor
   extracts, production code, historical-ledger edits, or unrelated changes;
   final outcome, unknowns, approvals, and next action recorded.

Stop conditions: missing approval/seal; wrong repository/model; DEFER mode;
research pool above 70% when fan-out is needed; missing Luna or writer roster
entry; unhealthy proxy; changed contract/base without renewed approval; any
outside-OWNS write; prohibited re-investigation; ambiguous provenance promoted
as authoritative; unapproved external or device action; invalid or null physical
read; uncertain mutation; or restoration failure. Stop the affected work and
report; do not substitute or retry.

## Constraints C1-C9

- **C1 - Approval boundary:** approval covers this plan and read-only research only; implementation, repository writes, commits, PRs, installs, connections, device writes, firmware actions, signing, merge, publication, and issue filing each require separate explicit owner authorization recorded in the task; missing authorization stops the dependent action without substitution.
- **C2 - Non-executing planning:** planning ends at handoff with no edits, tests, builds or device operations; execution uses a fresh `commandcode/deepseek-deepseek-v4.1-flash` thread on the approved plan, one writer, explicitly named subagent models, research scratch writes only where authorized, and no retries, swaps or escalations beyond the peak-aware workflow.
- **C3 - Firmware triple boundary:** firmware documentation, version and channel investigation is read-only; any flash is a separate future task with its own contract, this-unit consent, written brick/downgrade/restoration disclosure, and no bundled test; this plan supplies only the decision summary and schedules, prepares or promises no flash.
- **C4 - Valid read before write:** no JA11 filter-write command or payload of any value or interface is planned, prepared or sent before a complete validated device-specific read establishes the control on current firmware; garbage, wrong shape or null means no write and no retry; transport variation remains read-only and never repeats J034/J035 timing out of hope.
- **C5 - Contract before execution:** the task gate file, root `active:` pointer, OWNS scope, owner approval and matching seal must exist before evaluation execution starts; `tools/scope-guard` must pass before every commit; any outside-OWNS write is stop-the-ship.
- **C6 - Classified evidence:** every step declares DEMONSTRATED, CONFIGURED, NOT EXERCISED, INSPECTION-ONLY or UNSUPPORTED and its negative meaning; vendor semantics never prove JA11 persistence, and one unit or firmware result proves only its recorded tuple.
- **C7 - Fixed determination:** apply only outcomes (a) qualified v0.10.0 delivery, (b) separately gated firmware path worth pursuing, or (c) stronger consolidated closure; implementation reopening requires an exact H criterion, and insufficient evidence selects (c), never continued probing.
- **C8 - Minimal physical work:** only after plan approval and owner readiness, one separately authorized consolidated window captures a fresh baseline, performs read-only work first, handles expected re-enumeration explicitly, batches separately authorized qualifying follow-ons, avoids uncertain mutation retries, and verifies full original-state restoration across power removal.
- **C9 - Budget and sitting:** carry current preflight, window and fallback status, fit bounded work inside the owner's sitting, and stop at its boundary; create no unattended recurring schedule or background continuation.

## Review

verdict: PASS for gate 2, gate 3 and gate 4 at the document level. Implementation remains
blocked: the determination is C7(c), and no H criterion is satisfied.

ran:
- gate 2: read EVALUATION.md; all seven slices carry outcomes, sources, classifications and
  null-result meanings; APK discrepancy preserved; sibling rows provisional; corrected query
  specified; B4/B5 and C7(a)/(b)/(c) adjudicated.
- gate 3: read READINESS.md; dormant paths, false flag, UI gating, decoder weakness, missing
  fixtures, future tests and lifecycle requirements recorded; no code change or payload.
- gate 4: read the three draft files; one conditional session with fresh baseline and restoration;
  two status additions; exactly two issue drafts with H and the classification verbatim.
- gate 5: git diff --check and scoped name check before commit.
- gate 1: scope-guard run before commit.

findings:
- The evaluation is INSPECTION-ONLY consolidation; it did not re-run any probe, capture, install,
  connection or flash.
- Preserved APK provenance is unresolved: neither local copy matches the pinned
  `ab0eb9cb…` digest. Sibling rows stay provisional and were not promoted.
- Decoder weakness (`packetView` + `oneByteValueFromResponse`) is recorded in READINESS.md as a
  delivery blocker, not repaired.
- Documentation drift outside OWNS (`0x11` comments in the transport and capability matrix) is
  recorded for a future authorized docs task; it was not edited here.
- This review block was appended under the owner's explicit instruction. That changes the file
  digest, so the local seal was refreshed to cover the review record. The substantive contract,
  scope, base and OWNS list are unchanged; no scope change was approved by that refresh.

### Review round 1 — independent (gpt-6-luna)

verdict: defects (5, all textual or precision)
ran: scope-guard --root . --base ab243d4f… -> exit 0, seven files in scope
addressed: full APK digests restored; C7(c) reasoning now separates record-level
reopening from device-read qualification; issue 2 states H does not gate Save
ordering; owner session marks Save qualification independently actionable; vendor
record qualified as the inspected local copy. Determination unchanged: C7(c).

### Review round 2 — independent (gpt-6-luna)

verdict: clean
ran: scope-guard --root . --base ab243d4f… -> exit 0, seven files in scope
findings: none. APK digests complete; sibling rows provisional; C7(c) separates
record-level reopening from device-read qualification; H criteria and
classification verbatim in both issue drafts; Save ordering independently
actionable; claims bounded to cited evidence.
