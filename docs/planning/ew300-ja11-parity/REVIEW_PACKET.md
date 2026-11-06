# Review packet — EW300 / JA11 parity

Status: planning input, 2026-11-04. Not a capability claim and not maintained product state.

## Canonical paths

Project root:

/Users/stephenweeks/Documents/Codex/OPRA-EQ-development

Review packet root:

/Users/stephenweeks/Documents/Codex/OPRA-EQ-development/docs/planning/ew300-ja11-parity

Repository-relative packet path:

docs/planning/ew300-ja11-parity

## Review order

1. README.md — handoff strategy and hardware order.
2. BRIEF.md — current EW300 baseline, JA11 inventory, byte map, gap analysis, test inventory.
3. RESEARCH_SOURCES.md — source map, licenses, search log, and repeatable search checklist.
4. PLANNING_PROMPT.md — the planning instruction.
5. EXECUTION_PROMPT.md — the autonomous execution handoff.
6. PROGRESS_TEMPLATE.md — the continuity log.
7. FUNCTIONAL_MAPPING.md — the living feature/register/UI/test map.
8. TEST_EVIDENCE_LOG.md — the append-only test record.

## Current facts to confirm during review

- The current EW300 five-band Peak EQ, Apply, Flash, Reset, reconnect, capture, and output-gain path works on the attached device.
- The EW300 My DAC page is bare bones compared with JA11: current DEVICE rows are Output gain, Equalizer, and Connection.
- The JA11 My DAC page has Device volume, EQ program, Tuning (Ellyn), Startup settings, Microphone, USB & System, and About.
- Ellyn is JA11 Tuning (DCR/gate), not an EQ filter. On EW300, 0x3A/0x3B are analog PGA registers; do not transplant.
- Product Flash is the 0x53 EQ persistence save. Firmware cross-flash is out of scope.
- The EW300 stays on the Mac for read-only probes, golden-frame checks, JVM tests, and emulator work.
- The Pixel wireless-ADB session is last, after the readiness gate.
- No public EW300-specific protocol or firmware was found. Adjacent projects cover family protocol, JA11 UX, and EW300 measurements; see RESEARCH_SOURCES.md.

## Review outcome

The review should confirm or amend:

- the intended experimental scope;
- which JA11 rows are in, out, or “not available on EW300”;
- the native filter types to attempt;
- the Apply-versus-Flash decision;
- the final-session readiness checklist;
- the persistent execution contract;
- the documentation-update rule and evidence-log format.
- the internet research pass and source-quality decisions.

## Persistent execution expectation

The owner has pre-authorized local implementation, test, build, and emulator iteration. Execution should not pause after each test or ask whether to try another one. It should continue through failures, fixes, rebuilds, and additional tests until the accepted gates pass or a real stop condition is reached.

Real stop conditions are limited to:

- a material product choice not covered by the accepted plan;
- a required physical device action or hardware write that is not yet authorized;
- a destructive or irreversible action;
- a gate turn/stop limit;
- a hard blocker that cannot be resolved without owner input;
- evidence that contradicts the accepted plan.

A failed test, a passed test, a rebuild, an emulator restart, a new test case, or “this will take more work” is not a stop condition.

## Documentation expectation

Every code slice and every test run must update FUNCTIONAL_MAPPING.md and TEST_EVIDENCE_LOG.md. Durable facts must also be folded into the maintained EW300 protocol notes, capability matrix, validation ledger, and runbook where appropriate. Documentation is not deferred to the end.
