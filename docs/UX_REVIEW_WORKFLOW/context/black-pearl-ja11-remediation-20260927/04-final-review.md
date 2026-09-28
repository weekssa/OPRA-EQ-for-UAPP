# Final review - Black Pearl and JA11 remediation

Status: `PHYSICAL_FAIL` for the exercised Black Pearl Direct Flash path. Independent review,
software/emulator gates, and exact signed provenance passed; the owner-authorized AFUL Explorer
mutation was correctly reported as not verified after a final raw-gain mismatch. JA11 remains
independently `PHYSICAL_INCONCLUSIVE`; neither device is publicly supported by this result.

## Exact candidate review addendum — source `b61f02e8c91656f14ffc639e4c6d937b2162a1b7`

- PR #50 merged successfully into `main` at source `b61f02e8c91656f14ffc639e4c6d937b2162a1b7`.
- Signed workflow [#1372](https://github.com/weekssa/OPRA-EQ-for-UAPP/actions/runs/36347148408)
  completed successfully for `candidate_target=black-pearl-ja11`.
- Candidate `EQ-Library-v0.7.0-beta-b61f02e.apk`; APK SHA-256
  `276587734fc863277b83e4310c040cee22c27261075e21fa75d766ded9eef27e`.
- Package/version: `com.weekssa.opraeqforuapp`, `0.7.0` / code `7`.
- Signer SHA-256 `65c1c1256dae3c49e3548f334c91f0ba991969e9be9e0b223ba4e253d2114747`; v2/v3 verified.
- Immutable artifact ID/digest: `10941292707` /
  `sha256:188d2e4b973e7aab420d4c7d3890dba3407415c1d2c8f935a418acdacdbd8fc3`.
- Emulator diagnostics artifact ID/digest: `10941003406` /
  `sha256:e041ee672816ff85ba0c866ff0bdfa8b8a616ec540f95c93f3fd632f19197d57`.
- Signed emulator install and cold launch independently visible in the job log: PASS; `Status: ok`,
  `LaunchState: COLD`, `Complete`.
- No protocol, transport, retry, identity, Save, canonical-EQ, or hardware behavior changed in
  the candidate production diff. Luna later performed exactly one owner-authorized physical Flash
  action; the candidate truthfully rejected the mismatched final readback and no retry followed.

## Owner-authorized Pixel 9 physical execution review — 2026-09-27

- Candidate, package, APK checksum, Pixel identity, and Black Pearl identity matched the exact
  provenance recorded in `03-implementation-report.md` and `05-release-handoff.md`.
- The single authorized AFUL Explorer Direct Flash reached terminal failure, not success. The
  visible result contained the required not-verified wording and the actionable no-automatic-retry
  boundary.
- The decisive mismatch was `expected raw -7398, actual raw -6400` for final Black Pearl playback
  gain. Because the requested hardware state was not confirmed and no post-failure refresh or
  restoration was authorized, the physical Flash qualification is **PHYSICAL_FAIL** and the final
  hardware state is **unknown**.
- The named truth defect's exercised failure behavior is a **PASS**: no false success claim,
  no duplicate retry, and no later write was attempted. This is not a pass for the requested EQ's
  hardware application or restoration.

## Historical pre-candidate independent review — source `9f5cb852994e1c88fce80598f249a97fae047429`

The required independent reviewer completed a second read-only pass after the terminal-result
repair. Result: **PASS**.

- One inline live-region result surface is rendered only for a completed `EDITOR_APPLY` whose
  trace ID matches the completed status ID; the global Flash banner is not used for the same Apply.
- Verified Apply has explicit dismissal and deterministic eight-second expiry; failure/uncertainty
  remains truthful and actionable until dismissal or recovery.
- A prior JA11 Flash feedback surface is cleared when Apply starts and when it completes.
- Focused UI coverage verifies verified success, failed Apply wording, one-result rendering,
  readable/technical reports, dismissal, and test-clock expiry; unit coverage verifies success and
  failure wording.
- No JA11 flasher, protocol, transport, reconnect, Save, session, retry, Black Pearl, or hardware
  behavior changed. The stock Android UAPP routing prompt remains expected and out of scope.
- The reviewer performed no tests, builds, commits, pushes, or hardware actions.

This pre-candidate review passed the software boundary only. The exact candidate review addendum
above records the later signed provenance; earlier Pixel 9 evidence still does not transfer to the
new candidate.

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
7. Inspected PR #49 and its final exact-head checks: Android CI `#1874`, CodeQL `#1758`,
   priority community `#1627`, catalog currentness `#2142`, and dependency submission `#2231` all
   completed successfully.
8. Verified signed workflow run `#1371`, exact source `acaf4dd32ddd9379ec2860e45e34fb8039219583`,
   APK checksum, pinned signer certificate, immutable artifact ID/digest, package/version, signed
   emulator installation, and cold launch.

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
- PR #49 remote checks: PASS on final documentation evidence head; Android CI, CodeQL,
  priority-community, catalog currentness, and dependency submission all completed successfully.
- Trusted signed-beta run #1371: PASS; combined Black Pearl/JA11 candidate manifest and signed
  artifact provenance verified.
- No change to `BlackPearlProtocol`, `FiioJa11Protocol`, Android USB transport, identity matcher,
  endpoint, timing, retry policy, or signing behavior.
- The only release-workflow change is the additive `black-pearl-ja11` candidate target and strict
  manifest branch. No signing secret, signer check, main-only guard, artifact publication boundary,
  or existing target was weakened. Ruby YAML parsing and actionlint v1.7.12 passed on the exact
  candidate workflow.
- No hardware mutation, tag, or public publication occurred. Owner approval authorized the merge to
  `main` and the trusted signed-beta dispatch recorded in the implementation and release reports.

## Review conclusion

No unresolved defect was found within the two authorized issues. Software, owner-approved main
integration, and exact signed candidate provenance are verified. The handoff is now
`READY_FOR_PIXEL_9`; this is an owner physical-validation state, not a physical-fix or public-support
claim.

## Historical post-Pixel controlled-session review — 2026-09-27

The exact signed candidate was exercised on the owner Pixel 9 with a connected FiiO JA11. The
read-only baseline, local-only editor path, one Apply to `-1.00 dB`, final current-state readback,
one restoration Apply to `0.00 dB`, and final flat-state readback all matched the intended values.
The JA11 re-enumerated at both reconnect boundaries (`273 -> 275 -> 277`), and the Android UAPP
routing prompt was canceled each time so UAPP did not take ownership of the device.

The immediate hardware state and restoration evidence are **PASS**. Complete physical qualification
remains **PHYSICAL_INCONCLUSIVE** because the routing prompt obscured transient terminal feedback and
the post-dialog app surface did not retain an observable editor success sentence or export an
operation trace proving the exact Save count. This is an evidence/feedback boundary, not evidence
that the observed final readbacks were wrong. No Black Pearl hardware was exercised, and no public
support claim is made.
