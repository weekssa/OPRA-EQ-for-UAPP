# Final review - Black Pearl and JA11 remediation

Status: COMPLETE - independent read-only review passed against reviewed branch head
`0c77e081fd6abd12a6e20b482ce269ae9f5bb764`; draft PR #49 remote checks also passed.

## Review method

Because this Codex runtime exposed project-local role configuration but no direct specialist-agent
dispatch API, the primary worker performed a separate read-only review pass after implementation:

1. Re-read the complete working diff and `git diff --check`.
2. Compared changed production files against the two contracts and the scope guardrails.
3. Audited all changed tests and fake transports for exact no-write, one-Save, final-readback,
   session, mismatch, and no-retry assertions.
4. Checked the changed-file list for protocol, transport, identity, endpoint, workflow, manifest,
   canonical-EQ, or unrelated DAC modifications.
5. Re-ran complete unit, lint, debug assembly, and API 36 instrumentation gates after the final
   gating predicate repair.
6. Re-read the refreshed `origin/main` merge and the signed-beta workflow diff; verified the new
   combined target is explicit, the manifest assertions require both named products, and the
   main-only signing guard and existing publication path remain unchanged.
7. Inspected draft PR #49 and its exact-head checks: Android CI `#1873`, CodeQL `#1757`, priority
   community `#1626`, catalog currentness `#2141`, and dependency submission `#2230` all completed
   successfully.

This review is independent of the earlier focused implementation inspection and did not edit,
commit, push, or invoke hardware during the review itself.

## Findings

### Black Pearl - PASS

- Existing final ten-band/raw-gain readback gate and exact verified wording remain intact.
- All ten fields are compared in native raw wire-domain form, including index, active slot, type,
  frequency raw, gain raw, and Q raw.
- Active session is checked before writes, between writes, before reads, and after final readback.
- Readable final gain mismatch returns not verified and reconciles the anti-stacking baseline to
  observed truth; unavailable/malformed/mismatched/session-uncertain results mark unknown.
- Unknown baseline blocks later Flash/Reset mutation until a complete current snapshot re-establishes
  a known baseline. No automatic write, rewrite, restore, tolerance widening, or retry was added.
- Shared terminal success/failure feedback remains the only result surface; no duplicate banner or
  content-pushing card was introduced.

### JA11 - PASS

- The Edit action predicate covers connection, current device session, matching snapshot generation,
  active User 1, current five-band snapshot, native global gain, no current read, no pending restart,
  no busy operation, and no competing operation.
- Off and built-in programs are represented truthfully and cannot open the User 1 editor.
- Baseline is one immutable token, not merged UI fields; Apply re-reads the full token immediately
  before the first write even when the USB generation is unchanged.
- All pre-Apply actions are local. Review includes all five values, filter type/frequency/gain/Q,
  global gain, headroom consequences, warnings, and the first-write boundary.
- Apply is limited to Review and safe/no-blocking plans, then uses exact existing JA11 command order,
  volatile verification, one Save/reconnect, and final verification. Save is never sent after a
  failed volatile readback and is never retried.
- Failure and uncertainty copy does not promise automatic retry; operation trace records the manual
  editor operation and final state semantics.

## Regression and scope audit

- Focused Black Pearl/JA11 tests: PASS.
- Complete unit tests: PASS.
- Existing JA11 Flash/Reset, Black Pearl, EW300, shared editor, snapshot, operation-presentation,
  and shared My DAC tests: PASS in the complete suite.
- API 36 instrumentation: PASS, 20 tests.
- Draft PR #49 remote checks: PASS on exact reviewed head; Android CI, CodeQL, priority-community,
  catalog currentness, and dependency submission all completed successfully.
- No change to `BlackPearlProtocol`, `FiioJa11Protocol`, Android USB transport, identity matcher,
  endpoint, timing, retry policy, or signing behavior.
- The only release-workflow change is the additive `black-pearl-ja11` candidate target and strict
  manifest branch. No signing secret, signer check, main-only guard, artifact publication boundary,
  or existing target was weakened. Ruby YAML parsing passed; `actionlint` was unavailable.
- No hardware mutation, merge, tag, publication, or main push occurred.

## Review conclusion

No unresolved defect was found within the two authorized issues. Software and promotion preparation
are verified on the branch. The release remains `MERGE_APPROVAL_REQUIRED` solely because the
trusted signed-candidate job is main-only; the branch now has a truthful combined manifest target,
but no main merge or signed run has occurred. This is an owner-controlled release boundary, not a
software test failure.
