# Final review - Black Pearl and JA11 remediation

## Current authoritative closeout — 2026-09-28

Owner authorization for final software/release closeout is recorded. The closeout documentation
delta has no production-code delta from the exact tested executable source
`e1ab5fa5a65dc2d64624d871ac53d436f792ea6a`.

- Software implementation and independent review: **PASS**.
- Exact signed beta and Pixel 9 Black Pearl AFUL Explorer result: **PASS** for the bounded
  candidate/device/starting-state transaction; restoration was not run by Luna.
- JA11 software path: **PASS**; physical evidence remains bounded by the maintained ledger and
  is not silently transferred to the combined candidate.
- Final local unit/lint/debug/release/R8/mapping/API-36 emulator gates: **PASS**.
- `actionlint`: **NOT RUN** locally because unavailable; no workflow changed.
- Public release action: authorized and pending the trusted main-only signed release workflow.

The historical review entries below are retained as an audit trail and do not override this
authoritative closeout section.

Status: `PHYSICAL_FAIL` for the exercised Black Pearl Direct Flash path. Independent review,
software/emulator gates, and exact signed provenance passed; the owner-authorized AFUL Explorer
mutation was correctly reported as not verified after a final raw-gain mismatch. JA11 remains
independently `PHYSICAL_INCONCLUSIVE`; neither device is publicly supported by this result.

## Exact candidate review addendum — source `2cba1322245221103b8790bfcef705a38022c2ed`

- Owner-authorized minimum main integration completed successfully at source
  `2cba1322245221103b8790bfcef705a38022c2ed`; the later handoff commit is documentation-only.
- Signed workflow [#36368698803](https://github.com/weekssa/OPRA-EQ-for-UAPP/actions/runs/36368698803)
  completed successfully for `candidate_target=black-pearl-ja11`.
- Candidate `EQ-Library-v0.7.0-beta-2cba132.apk`; APK SHA-256
  `50cee56aa59a8980a61bff42625fe9d839a28189068ba5adddd1a03530ea02d6`.
- Package/version: `com.weekssa.opraeqforuapp`, `0.7.0` / code `7`.
- Signer SHA-256 `65c1c1256dae3c49e3548f334c91f0ba991969e9be9e0b223ba4e253d2114747`; v2/v3 verified.
- Immutable artifact ID/digest: `10948632352` /
  `sha256:3e9fb31a814a8feb30ff0c48ac69a71cddd579e71b90401ddf34e491b8fc45ab`.
- Emulator diagnostics artifact ID/digest: `10948497670` /
  `sha256:fd92f4bf31c57616580ccfab399e4d13ef4f15973f5463154740fbcb4a6ebda2`.
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
  gain. Because the requested hardware state was not confirmed and no owner-directed recovery
  refresh/reconnect or restoration followed, the physical Flash qualification is
  **PHYSICAL_FAIL**. The existing ViewModel's normal post-operation read refresh does not qualify
  the requested state or restoration.
- The named truth defect's exercised failure behavior is a **PASS**: no false success claim,
  no duplicate retry, and no later write was attempted. This is not a pass for the requested EQ's
  hardware application or restoration.

### Read-only recovery result

After the terminal failure, one owner-authorized app-session reconnect and full read-only refresh
was completed. My DAC showed `Verified current hardware`, `Matches My EQs`, `Explorer`, 10 filters,
active slot 1, and playback gain `-25.00 dB` / raw `-6400`; the selected target required raw
`-7398`. The recovery confirms that the PEQ bands reached hardware but the global-gain target did
not, so the complete Flash remains **PHYSICAL_FAIL** and restoration remains unverified.

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

## Write-side diagnostic review addendum — 2026-09-27

The later owner-authorized write-side diagnostic did not change production source. Review of the
temporary diff confirms that the application-id suffix and logger were removed, the original signed
package was not overwritten, the temporary package was uninstalled, and `git diff --check` passed.
The diagnostic logger's byte predicate was incorrect (`report[0]` was checked instead of
`report[1]`), so the captured log contains no raw write transfer evidence. The only usable result is
the post-operation UI observation for the AutoEq/Jaytiss Explorer profile: verified current
hardware, matching My EQs, 10 filters, active slot 1, and `-31.00 dB` playback gain.

That profile is not the earlier Hifigues community Explorer target that failed at expected raw
`-7398` versus actual raw `-6400`. No source correction is proven, no new candidate exists, and no
additional hardware action is authorized or recommended from this run. Final disposition remains
**REPAIR_REQUIRED**; the prior truthfulness failure-path result remains the only qualified evidence
for the named Black Pearl defect.

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

## Read-only Black Pearl protocol-diagnosis review — 2026-09-27

The post-failure capture was reviewed against the maintained Black Pearl protocol and the exact
physical evidence. It is diagnostic evidence only: the diagnostic app connected and refreshed reads,
but issued no setting write or persistence command. The native global-gain response was
`4B 80 03 02 00 E7 FF FF ...`, raw `-6400`, matching both the current app snapshot (`-25.00 dB`)
and the earlier final-readback mismatch (`expected -7398`, `actual -6400`). The ten-band read
responses were present and parseable.

Independent review conclusion: the existing codec and read envelope are corroborated; the capture
does not establish a write-side byte/order defect. No tolerance, gain math, report order, timing,
retry behavior, or transport change is justified. The temporary diagnostic instrumentation was
removed and the production source is clean of diagnostic changes. The prior mandatory independent
software review remains PASS;
this follow-up does not upgrade the physical result. Current release disposition is
`REPAIR_REQUIRED`, with no new source fix or pushable candidate produced.

## External reference review addendum — 2026-09-27

The supplied `Matr1x01/trnBlackPearlEq` Android implementation was compared read-only. At commit
`45bbf3c65c899181395eb7936615ced1fbd5d4be`, it uses an available interrupt OUT endpoint for writes
and retains HID `SET_REPORT` only as fallback. The Black Pearl descriptor captured in this run has
that OUT endpoint (`0x05`) alongside IN endpoint `0x86`; OPRA currently ignores OUT and uses only
`controlTransfer`.

This is actionable evidence and a plausible explanation for the prior gain-write failure, but not
proof of a fix. The required change would alter USB endpoint behavior, which the remediation scope
forbids. Independent review therefore finds the named Black Pearl defect unresolved and the next
source change **blocked pending owner authorization for that exact boundary**. No external code was
copied, no protocol bytes were changed, and no hardware was touched during this comparison.

## Final repaired-commit independent review — 2026-09-27

The first independent review correctly rejected the initial uncommitted implementation because its
interrupt OUT path treated zero and short transfers as success. The implementation was repaired to
require an exact 64-byte transfer, and focused/full unit, API-36 emulator, lint, and release/R8 gates
were rerun before commit `eb761f1bc510a612acde7b71b453631e1ff23a8f`.

A second independent read-only reviewer then reviewed that exact committed source and returned
**PASS — no P0/P1/P2 code defect**. The reviewer verified:

- interrupt OUT selection is limited to OUT + interrupt endpoints;
- interrupt success requires the complete requested report length;
- existing `SET_REPORT` parameters/result policy remain unchanged;
- report bytes, 250 ms transfer timeout, settle delays, read polling, bounded two-attempt read
  retry, mutex/session ownership, and no automatic write retry remain unchanged;
- the commit scope is exactly one transport, one small policy helper, and its unit test; and
- no hardware was connected or mutated during review.

The reviewer recorded two non-blocking P2 evidence gaps: the tests do not mock
`UsbDeviceConnection` to assert the actual Android call arguments/bytes, and no signed artifact or
remote check result exists yet for the pushed implementation source `eb761f1bc510a612acde7b71b453631e1ff23a8f`.
These gaps prevent signed-candidate,
physical-qualification, and release approval, but do not require another source repair for this
software checkpoint. They are explicitly retained as follow-up evidence requirements rather than
silently treated as proof.

Final software review disposition: **PASS**. Current release state remains
**SOFTWARE_READY_PENDING_SIGNED_CANDIDATE**, not `READY_FOR_PIXEL_9`; no physical-fix or public
hardware-support claim is made.

## Signed-candidate provenance review — 2026-09-28

The owner-authorized minimum main integration completed conflict-free at candidate source SHA
`2cba1322245221103b8790bfcef705a38022c2ed`; the later handoff commit is documentation-only. The trusted signed-beta workflow run
[#36368698803](https://github.com/weekssa/OPRA-EQ-for-UAPP/actions/runs/36368698803) completed
successfully for `candidate_target=black-pearl-ja11`.

The resulting candidate manifest binds source SHA `2cba1322245221103b8790bfcef705a38022c2ed` to
`EQ-Library-v0.7.0-beta-2cba132.apk`, package `com.weekssa.opraeqforuapp`, version `0.7.0`, code
`7`, APK SHA-256
`50cee56aa59a8980a61bff42625fe9d839a28189068ba5adddd1a03530ea02d6`, and signer certificate
SHA-256 `65c1c1256dae3c49e3548f334c91f0ba991969e9be9e0b223ba4e253d2114747`. The immutable signed
APK artifact is ID `10948632352` with digest
`sha256:3e9fb31a814a8feb30ff0c48ac69a71cddd579e71b90401ddf34e491b8fc45ab`.

The workflow also passed its signed-emulator install and cold-launch checks on Android 35
(`Success`, `Status: ok`, `LaunchState: COLD`). Diagnostics artifact ID `10948497670` has digest
`sha256:fd92f4bf31c57616580ccfab399e4d13ef4f15973f5463154740fbcb4a6ebda2`.

Final review disposition: **READY_FOR_PIXEL_9**. The state is a handoff to the owner’s exact-candidate
Pixel 9 review, not physical qualification. Black Pearl and JA11 must be classified independently;
Luna has not mutated hardware.

## Latest exact-candidate Black Pearl physical review — 2026-09-27 local / 2026-09-28 UTC

The Pixel 9 was running the exact signed candidate bound to source
`2cba1322245221103b8790bfcef705a38022c2ed`; installed APK SHA-256 was
`50cee56aa59a8980a61bff42625fe9d839a28189068ba5adddd1a03530ea02d6`, package version was `0.7.0`
code `7`, and signed artifact provenance was workflow `36368698803`, artifact `10948632352` /
`sha256:3e9fb31a814a8feb30ff0c48ac69a71cddd579e71b90401ddf34e491b8fc45ab`.

The owner authorized one exact AFUL Explorer Flash. The app showed the expected confirmation and,
after the single mutation, a truthful terminal failure with final raw gain `expected -8934, actual
-8960`. No automatic retry or additional mutation occurred. The visible result included the
required not-verified and reconnect/refresh wording, so the named truthfulness defect's failure
path remains **PASS**. The requested Flash state and restoration remain **PHYSICAL_FAIL / NOT
VERIFIED**.

This result is not evidence that the interrupt-OUT path is correct or incorrect in isolation; it
shows only that the exact candidate still failed the complete AFUL Explorer transaction at the
final global-gain readback boundary. Independent review disposition for the physical result is
**REPAIR_REQUIRED**. Do not request another write until deterministic source diagnosis produces a
new candidate and its exact provenance is complete.

### Source-diagnosis boundary

Read-only comparison of the maintained codec and the supplied independent reference confirms the
same signed little-endian 16-bit global-gain representation at 256 raw units per dB. The physical
`-8934` versus `-8960` result is a 26-raw-unit (`0.1015625 dB`) disagreement. There is not enough
evidence to reinterpret that as an accepted native quantization step, so no tolerance, codec, wire,
timing, or retry change is approved by this result. A new source correction requires deterministic
evidence before another physical candidate is produced.

The source-side value trace is now closed for the tested operation: baseline `-7936`, source
preamp `-3.90 dB`, rounded delta `-998`, expected target `-8934`, observed readback `-8960`.
The independent Android reference uses the same 64-byte report and raw/256 gain model, but does
not establish a device-side global-gain step. Focused deterministic tests pass; no defensible
production correction has been identified. Final review therefore remains **REPAIR_REQUIRED**,
with no additional owner write requested on the current candidate.

## Current implementation review — `7183d2c5beb76dca697e8497ba15b5c7627a320b`

A read-only review of the new source checkpoint finds the repair narrowly scoped and internally
consistent:

- `BlackPearlFlashPlan` alone switches from the shared file/protocol spec to the direct-Flash native
  gain overlay; the Black Pearl file exporter continues to use the shared 1/256 representation.
- `HardwareEqEditSpecs` applies the same whole-dB boundary only to editor-generated tracked
  headroom. The raw protocol codec and all transport/report paths are unchanged.
- The implementation does not copy or depend on GPL reference code. Its rule is supported by the
  physical trace and independent exact-identity implementation evidence recorded in the report.
- The added tests prove the fractional source preamp and observed raw transition while retaining
  all existing fail-closed, no-retry, session, mismatch, and anti-stacking regression coverage.
- Scope audit: no navigation, canonical EQ ownership, device identity, endpoint, timing, Save,
  retry, transport, public-support, or hardware behavior outside the named Black Pearl Direct
  Flash gain-planning defect changed.

The existing project-local independent JA11 reviewer remains **PASS** for the JA11 terminal/editor
path. The Black Pearl reviewer was requested to inspect this exact checkpoint read-only; until that
review and exact signed provenance are complete, this is not a candidate approval. Current release
state is **MERGE_APPROVAL_REQUIRED**, not `READY_FOR_PIXEL_9`.

## Final source-clarity follow-up review — `1393150ba6cfff7a0b73c385f564a57d29c157a3`

The follow-up changed only Black Pearl file-export transformation wording and the two focused
assertions that describe it. It preserves file contents, canonical EQ ownership, direct-Flash
planning, protocol bytes, transport, timing, retry policy, and hardware boundaries. The rerun full
unit/lint/debug/release/R8/API-36 gates are green. No new defect was found; the prior independent
review PASS and the exact signed-candidate boundary remain unchanged.

## Exact-tip re-review requested — `3f818d89` — 2026-09-27

The prior independent review returned `REPAIR_REQUIRED` for two specific gaps: no decisive editor
whole-dB Apply regression and stale maintained file/Flash representation wording. Both are repaired
on pushed tip `3f818d89`, and the full software/emulator gate set is green. The same read-only
reviewer was asked to re-review this exact tip without editing, committing, pushing, publishing,
or contacting hardware. This gate remains **PENDING** until that response is recorded.

## Final independent review — reviewed branch tip `59caced95234aab494bdd44fb9a8beb5bb045e2c`

**PASS for production scope and behavior.** The independent reviewer confirmed that the production
and test source remains `3f818d89`, the later branch delta is documentation/report-only, the three
authoritative wording repairs are complete, and the exact-tip handoff provenance is now explicit.
No unresolved protocol, transport, timing, retry, session, fail-closed, canonical-EQ, or hardware
mutation defect was found. The exact signed beta remains unavailable because trusted signing is
main-only; this review does not qualify physical hardware or authorize publication.

The final owner boundary is minimum main integration of production source `3f818d89` from reviewed
branch tip `59caced95234aab494bdd44fb9a8beb5bb045e2c`, followed by exact signed-beta provenance.
## Exact signed candidate review boundary — `e1ab5fa5a65dc2d64624d871ac53d436f792ea6a`

The minimum main integration boundary is complete and the trusted signed-beta workflow passed for
the combined `black-pearl-ja11` target. The candidate manifest binds source SHA
`e1ab5fa5a65dc2d64624d871ac53d436f792ea6a` to APK SHA-256
`7fffba26f32991ce8c936f725bdc6c3c4b6956d3a0800c539b8d45e6b52a2501`, signer certificate
`65c1c1256dae3c49e3548f334c91f0ba991969e9be9e0b223ba4e253d2114747`, workflow run
`36375994853`, and immutable APK artifact `10950858857` / digest
`sha256:b13ca6973a203529c2cf7ea3247bb83f46af17cc89ff9506e460f33488b543b1`. The signed-emulator
install and cold launch passed.

Final review status: **READY_FOR_PIXEL_9**. The candidate is not physical qualification, does not
claim Black Pearl is fixed, and does not authorize a public hardware-support claim. The next owner
boundary is one exact-candidate Pixel 9 review with Black Pearl and JA11 classified independently.

## Latest exact-candidate Black Pearl attempt — preflight safety stop

The owner authorized one exact-candidate AFUL Explorer Flash on Pixel 9. Candidate identity matched
source `e1ab5fa5a65dc2d64624d871ac53d436f792ea6a`, APK SHA-256
`7fffba26f32991ce8c936f725bdc6c3c4b6956d3a0800c539b8d45e6b52a2501`, package
`com.weekssa.opraeqforuapp` version `0.7.0` / code `7`, and signed workflow `36375994853`.
The device was the owner Pixel 9 `tokay`, API 37, wireless serial
`[wireless ADB route redacted]`; the connected target was `TRN Black Pearl`.

The read-only baseline showed verified current hardware, Flat, ten filters, and active slot 1. The
single confirmation displayed `-4.00 dB` native playback-gain adjustment and was accepted once.
The transaction stopped before hardware mutation with the truthful terminal result that applying
`-4.00 dB` would exceed the Black Pearl validated volume range and that the DAC volume must be
adjusted before trying again. The result also prohibited automatic retry and requested reconnect or
refresh before later hardware action.

Review disposition: **PHYSICAL_FAIL / PRECHECK_BLOCKED** for this AFUL Explorer request; the safety
failure-path assertion is **PASS**, but no final native readback occurred and the requested EQ is
**NOT VERIFIED**. No retry, Reset, Save, Restore, or second Flash was attempted. This result does
not qualify Black Pearl hardware or prove the named success path. A later attempt requires a fresh
baseline and a separate authorization after the DAC volume is adjusted into the validated range.

## Latest exact-candidate Black Pearl result — AFUL Explorer PASS

After the earlier preflight stop, the owner adjusted the Black Pearl volume and authorized one fresh
exact-candidate AFUL Explorer Flash. The candidate remained source
`e1ab5fa5a65dc2d64624d871ac53d436f792ea6a` with APK SHA-256
`7fffba26f32991ce8c936f725bdc6c3c4b6956d3a0800c539b8d45e6b52a2501`; the Pixel was `tokay`, API
37, and the DAC identity was TTGK Technology `TE-C`, VID `0x3302`, PID `0x43E8`, serial
`330243E8260129`.

The fresh baseline was verified Flat with ten filters, active slot 1, and playback gain `-25.00 dB`.
The one confirmation showed the expected `-4.00 dB` change. The subsequent read-only My DAC screen
reported `Verified current hardware`, `Matches My EQs`, `Explorer · Jaytiss · Latest`, nine active
bands, ten filters, active slot 1, and playback gain `-29.00 dB`.

Review disposition: exact-candidate AFUL Explorer requested-state **PHYSICAL_PASS**. This is a
bounded pass for the tested candidate/device/identity/starting volume only; restoration was **NOT
RUN** because it was not authorized, and persistence, other revisions, other presets, and public
support remain unproven. The transient success banner was not captured before it expired, but the
durable app readback screen explicitly verified current hardware and matched My EQs.

## Public release verification — 2026-09-28

The owner-approved release gate is complete:

- [Public `v0.7.0` release](https://github.com/weekssa/OPRA-EQ-for-UAPP/releases/tag/v0.7.0) is latest, published, and non-prerelease.
- Tag `v0.7.0` points to `4f325d673159b40515086fe5143df12b29ddb076`; the executable behavior remains the exact tested `e1ab5fa5a65dc2d64624d871ac53d436f792ea6a` candidate, with documentation-only synchronization on the tagged head.
- The trusted main-only [release workflow `36381764266`](https://github.com/weekssa/OPRA-EQ-for-UAPP/actions/runs/36381764266) passed and produced immutable artifact `10952494777` / `sha256:08cffddad84f4c5648a4ec2e884784188689a24041f020cc9925e7af8040bccc`.
- The public APK asset has SHA-256 `27dada499bcbf9be9bd21d1349164858c93a5d2b83f78fd61134de13b4eb4025`; its public checksum asset is uploaded and independently reported by the GitHub API.
- Release notes were reviewed against the Google developer documentation style guidance: sentence-case headings, direct language, parallel lists, descriptive links, and explicit accessibility and hardware boundaries.

Final review status: **OWNER_ACCEPTED** for publication and evidence-bounded release claims. This
does not expand the Black Pearl result beyond the exact AFUL Explorer transaction, does not add a
JA11 power-cycle claim, and does not claim restoration performed by Luna. No DAC was mutated during
publication.
