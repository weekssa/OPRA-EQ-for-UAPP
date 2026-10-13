NOT READY — the physical zero-retry acquisition route is absent from captured evidence

ROOT CAUSE
- exact broken invariant: current replacement DEVICE state can coexist with absent EQ; Flash completion waits for a refresh function but does not require an accepted current-session EQ bundle.
- relevant historical change: one-shot connection EQ refresh predates v0.8.1; PR88 made caller refresh synchronous, PR89 enabled retry only on that caller path. The physical regression's triggering change is not established. v0.8.0 is a control, not a rollback target.
- why process restart repairs it: startup re-creates collectors and offers a fresh acquisition; why that acquisition succeeds remains unobserved.
- why PR #88 failed: it does not enforce accepted publication, and retains independent one-shot observer/caller cancellation vulnerability. Exact physical failing route unknown.
- why PR #89 failed: same missing postcondition/path-specific policy; its enabled helper necessarily retries a completed null on a current session. The actual zero-retry route was not traced.
- confidence: physical root cause NOT ESTABLISHED, below required95%; architecture weaknesses proven separately. Do not guess.

FIX
- branch: none; orchestration branch codex/ja11-postflash-reconstruction
- PR: none
- source SHA: investigated main4b2d3e67a00f1c7545e27165135c56fc901206b9
- production files: none changed
- protocol behavior changed: NO
- mutation behavior changed: NO

AUTOMATED REGRESSION
- physical state modeled: NO (four contract probes demonstrate sufficient/competing routes; full physical production lifetime not reproduced)
- fails before fix: NO qualifying regression; characterization probes PASS
- passes after fix: NOT RUN; no fix selected

VALIDATION
- focused: PASS49 existing JA11/session/snapshot tests +4 new contract probes
- unit: PASS776,0 failures,0 errors,0 skipped
- lint: PASS0 errors,118 warnings,2 hints
- assemble: PASS debug
- minimum API: NOT RUN for this mission
- API35: NOT RUN for this mission (Robolectric API35 JVM session tests are not emulator evidence)
- exact-head CI: NOT RUN for absent final fix; discovered existing requested-SHA fallback
- CodeQL: NOT RUN for absent final fix; baseline main run38352080663 passed
- adversarial review: NOT RUN; prerequisite root/regression/fix gate not met. Luna High root analysis + bounded probe follow-up confirms uncertainty.

SIGNED CANDIDATE
- ready: NO
- source SHA: none
- APK SHA-256: none
- signer: none
- provenance: root-cause gate blocks preparing a speculative candidate; signing also remains main-only/owner merge gated

PHONE ACCESSED OVERNIGHT:
NO

OWNER ACTION:
Phone access was authorized by the exact owner unlock phrase. Approve diagnostic-only shared-source trace hooks as a bounded exception to the95% production-edit gate; no speculative fix or automatic Flash. Current source is unchanged.

DIAGNOSTIC PROCEDURE
1. Off-phone first: add diagnostic-only correlation for acquisition owner, attempt index, generation/currentness guard, null/rejection cause, accepted publication/UI adoption, and cancellation. No payload/identity/private-value logging; no retry, transport, timing or mutation change. Validate trace behavior with fixtures; freeze source/APK/hash/package/signer and complete its applicable local/emulator/review/CI gates.
2. After exact owner unlock only: verify/install the frozen diagnostic candidate and establish a non-mutating baseline. Do not query phone availability before authorization.
3. Request one explicit owner Flash action; never Flash automatically. Capture trace immediately in the same process. No Mic/UAC/volume/program/power-cycle campaign.
4. Success distinguishes normal converged acquisition; failure distinguishes one-shot observer, caller cancellation/early guard, both-read failure, or lost publication. Retain process/state on failure; no app restart, second Flash, or automatic unplug. Select production fix only after evidence establishes the actual cause.

DURABLE CONTEXT
RUNBOOK.md preserves exact owner mission. STATE.md is canonical; EVIDENCE.md contains source references, history, probe outcomes, commands, private artifact hashes and limitations. Raw retained captures remain outside Git. Temporary local validation logs remain under tmp/. Only test characterization and mission documentation were added.

OWNER-PRESENT BASELINE UPDATE
Installed failed-candidate APK hash and release signer independently verified. PID5485 survived, but the original Activity was absent; warm launch opened a new Activity, so original ViewModel/repository continuity is not proven. One read-only Connect produced current EQ/DEVICE with five filters and enabled Edit EQ. No Flash, setting write, install, force-stop, or unplug. This is a fresh-Activity baseline, not post-Flash acceptance. Diagnostic trace must identify Activity/ViewModel/repository lifetime, not only PID. Phone is available for the owner; no capture stream remains active.
