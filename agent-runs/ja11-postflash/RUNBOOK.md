# OPRA EQ for UAPP — JA11 Post-Flash Convergence Autonomous Runbook

**Repository:** `weekssa/OPRA-EQ-for-UAPP`\
**Primary orchestrator:** Codex Sol 6.1\
**Sub-agents:** Luna 6.0, using the lowest thinking level sufficient for each task\
**Owner state:** OFFLINE / ASLEEP\
**Physical-device access:** FORBIDDEN until explicit owner authorization\
**Primary objective:** Root-cause and fix the JA11 post-Flash My EQ convergence regression, fully validate it off-device, and prepare the exact signed candidate for one owner-controlled physical acceptance.

---

# 1. Core Mission

Fix the JA11 post-Flash application-state regression correctly.

The required end state is:

```text
Flash once
→ JA11 performs expected reset/re-enumeration
→ replacement session becomes current
→ DEVICE state becomes current
→ authoritative EQ state for that same current session becomes available
→ My EQ is populated and editable
→ My DAC remains functional
→ no app restart required
```

Do not optimize for preserving previous attempted fixes.

Optimize for:

- correct root cause;
- minimal production change;
- preservation of proven JA11 safety behavior;
- reproducible automated regression;
- efficient token use;
- one decisive physical test when the owner returns.

---

# 2. Hard Owner / Phone Embargo

The owner is offline.

Until the owner explicitly sends:

```text
OWNER PRESENT — AUTHORIZE PHONE ACCESS
```

do NOT perform or attempt:

```text
adb connect
adb pair
adb devices
adb mdns
adb shell
adb install
fastboot
wireless-debugging discovery
Pixel discovery
JA11 discovery through the Pixel
application launch on the Pixel
candidate installation on the Pixel
physical-device state inspection
```

Do not determine whether the Pixel is currently online.

Do not request a Flash.

Do not request plug/unplug.

Do not wake or block on the owner.

The overnight task must proceed entirely from:

- repository state;
- git history;
- existing captured evidence;
- unit/integration/UI tests;
- emulator tests;
- CI;
- signed build infrastructure;
- local tools and skills;
- read-only sub-agent analysis.

---

# 3. Known Authoritative Physical Evidence

Treat this evidence as ground truth.

The JA11 Flash operation itself has succeeded physically.

The newest live failure was captured before restarting the application:

```text
app process unchanged since Flash: YES
MainActivity PID remained alive
My DAC/device state healthy: YES
DEVICE snapshot current: YES
My EQ blank: YES
EQ bundle present: NO
EQ bundle current: NO
retry executed: NO
first EQ acquisition: no accepted current-session EQ bundle
second/retry acquisition: not executed
```

Captured evidence exists for that failure:

```text
logcat: YES
UI hierarchy: YES
screenshot: YES
state timeline: YES
```

The exact lower-level reason that the first complete EQ acquisition did not yield an accepted bundle was not exposed.

Do not treat that missing detail as permission to invent another retry.

The more important proven state is:

```text
DEVICE(current replacement session) = available
EQ(current replacement session) = absent
same app process = alive
My EQ = blank
```

A process restart reconstructs state and restores My EQ.

---

# 4. Proven v0.8.1 Behavior That Must Not Be Reopened Without New Evidence

The following areas were previously qualified and are outside the default scope:

- User 1 selection before custom EQ writes;
- five-band EQ write ordering;
- global-gain handling;
- Apply ordering;
- Save ordering;
- Save exactly once;
- expected JA11 detach/re-enumeration handling;
- old-session invalidation;
- no blind mutation replay;
- replacement-session readback;
- Mic persistence;
- UAC persistence;
- User 1 custom EQ/global-gain persistence;
- playback volume is runtime-only across true power loss;
- active EQ program/preset is runtime-only across true power loss.

Do not modify these unless new executable evidence proves that the convergence defect requires it.

---

# 5. Relevant Historical Checkpoints

Public v0.8.1 release source:

```text
d0b6fe143957012c138613d153896923d6d76369
```

PR #88 attempted to fix asynchronous post-Flash EQ snapshot ordering.

Relevant reported fix commit:

```text
ab920db3d5792d835b389714ba72c6ec4a1b8a70
```

A signed physical candidate containing that approach still failed.

PR #89 attempted a bounded read-only convergence retry.

Merged source:

```text
4b2d3e67a00f1c7545e27165135c56fc901206b9
```

Signed candidate SHA-256:

```text
b617f79b7980ae45083f5f2d24f769d50da6f57f6d6ba03c668187670fb4b644
```

That candidate also failed physical acceptance.

Most important observation from the second failure:

```text
first acquisition produced no accepted current-session EQ bundle
retry executed: NO
```

Do not assume the retry predicate itself is the ultimate root cause.

v0.8.0 must be used as a historical control/reference.

Do NOT blindly revert to v0.8.0.

---

# 6. Primary Strategic Rule

Do not stack a third speculative workaround onto the current code.

First reconstruct the relevant state transition across:

```text
v0.8.0
→ v0.8.1 JA11/session changes
→ PR #88
→ PR #89/current main
```

The central question is:

> What change made it possible for post-Flash replacement-session DEVICE state to converge while the EQ state required by My EQ remains absent indefinitely?

The process-restart path is also part of the specification:

> What initialization occurs during normal process startup that does not occur correctly during post-Flash replacement-session convergence?

---

# 7. Token-Efficiency Rules

Codex Sol 6.1 is the single orchestrator.

Do not create a large swarm of agents.

Do not ask multiple sub-agents to independently reread the entire repository.

The normal successful path should use no more than:

1. one Luna Medium historical/relevant-file mapper;
2. one Luna High root-cause/state-machine analyst;
3. one fresh Luna High adversarial reviewer after the fix exists.

Spawn additional agents only when evidence requires them.

Do not use Luna Max unless Sol + Luna High cannot resolve a material contradiction.

Do not use multiple agents for ordinary CI failures that Sol can classify from logs.

Every sub-agent receives a curated context package, not the entire mission history.

---

# 8. Phase 0 — Local Instructions, Skills, Tools, and Repository Capability Audit

Before bug analysis, Codex Sol 6.1 must inspect the local working directory and determine what instructions and Android-development capabilities already exist.

Discover applicable repository and parent-directory instructions, including where present:

```text
AGENTS.md
README.md
CONTRIBUTING.md
DEVELOPMENT.md
TESTING.md
RELEASING.md
docs/**/*.md
.github/workflows/*
tools/
scripts/
gradle/
buildSrc/
skills/
.skills/
.codex/
.agents/
.agent/
```

Also discover similarly named local instruction/tool directories.

Determine:

- which instruction files apply to the repository root;
- whether nested instructions override parent instructions;
- which Android build/test/emulator/ADB/release/signing tools already exist;
- which skills are intended for repository analysis, Kotlin/Android work, CI, testing, GitHub Actions, release builds, signing, and sub-agent usage;
- which existing scripts should be reused instead of reimplemented.

Read relevant Markdown and skill/instruction files before planning the fix.

Do NOT dump every file into every sub-agent context.

Produce one compact internal document:

```text
REPOSITORY CAPABILITY DIGEST
```

containing:

```text
applicable instructions
available skills
preferred build commands
preferred test commands
existing JA11 helpers
existing emulator helpers
existing CI helpers
release/signing workflow
exact-head CI mechanism
important repository conventions
```

All later agents receive only the relevant pieces of this digest.

---

# 9. Minimal Persistent Workspace

Create or reuse a dedicated orchestration branch:

```text
codex/ja11-postflash-reconstruction
```

Create:

```text
agent-runs/ja11-postflash/
```

Maintain only these durable files:

```text
agent-runs/ja11-postflash/
├── RUNBOOK.md
├── STATE.md
├── EVIDENCE.md
└── MORNING_HANDOFF.md
```

Do not create a dozen permanent reports.

Temporary sub-agent outputs may live under:

```text
agent-runs/ja11-postflash/tmp/
```

but summarize material conclusions into `EVIDENCE.md`.

The temporary folder may be excluded from production PRs.

---

# 10. STATE.md

Keep `STATE.md` short.

It is the canonical mission state.

Suggested structure:

```text
MISSION_STATUS=
CURRENT_PHASE=
OWNER_GATE=CLOSED
PHONE_ACCESS=FORBIDDEN

CURRENT_MAIN_SHA=
V080_SHA=
V081_SHA=

ANALYSIS_BRANCH=
FIX_BRANCH=
FIX_HEAD_SHA=

ROOT_CAUSE=
ROOT_CAUSE_CONFIDENCE=

REGRESSION_REPRODUCED=NO
FIX_IMPLEMENTED=NO
INDEPENDENT_REVIEW=NOT_RUN

FOCUSED_TESTS=
FULL_UNIT=
LINT=
ASSEMBLE=
EXACT_HEAD_CI=
CODEQL=
MIN_API=
API35=

SIGNED_CANDIDATE_READY=NO
SIGNED_CANDIDATE_SOURCE_SHA=
SIGNED_CANDIDATE_SHA256=

PHYSICAL_TEST_REQUIRED=YES
PHYSICAL_TEST_AUTHORIZED=NO

NEXT_ACTION=
```

Update after every major phase.

Only Codex Sol 6.1 may alter the canonical conclusion.

---

# 11. EVIDENCE.md

`EVIDENCE.md` is append-only in spirit.

Record:

- relevant physical evidence;
- historical SHAs;
- relevant commits;
- exact file/function references;
- root-cause evidence;
- competing hypotheses and why rejected;
- regression-test evidence;
- CI run IDs;
- candidate hashes;
- reviewer findings;
- material decisions.

Do not paste large raw logs unless necessary.

Prefer:

```text
artifact path
run URL/ID
commit SHA
short causal excerpt
conclusion
```

---

# 12. Phase 1 — Establish Exact Historical Baseline

Codex Sol should first determine exact git facts.

Resolve:

```text
v0.8.0 commit
v0.8.1 commit/tag
PR #88 commits/merge
PR #89 commits/merge
current main
```

Identify production files changed between the relevant states.

Focus on files involving:

- JA11 Flash orchestration;
- USB/session generation;
- replacement-session recovery;
- DEVICE snapshots;
- EQ snapshot/bundle state;
- HardwareEqRepository;
- ViewModels;
- My EQ backing state;
- My DAC state;
- navigation/lifecycle where relevant.

Do not perform a broad codebase review.

---

# 13. Luna Sub-Agent #1 — Historical Mapper

Use:

```text
Luna 6.0 Medium
```

Give it only:

- Repository Capability Digest portions relevant to source/history;
- exact physical-failure summary;
- relevant commits/tags;
- candidate file list/diffs.

Task:

1. map the relevant state-management changes from v0.8.0 → v0.8.1 → #88 → #89;
2. identify the smallest suspect commit/change set;
3. identify which changes could create:

```text
DEVICE=current
EQ bundle=absent
same process remains alive
```

4. identify differences between process-start initialization and post-Flash recovery.

Do not ask this agent to design the final fix.

Output should be concise.

Target:

```text
<= ~1500 words
```

Codex summarizes the useful result into `EVIDENCE.md`.

---

# 14. Historical Analysis Before Bisect

Do NOT automatically perform a full `git bisect`.

First use semantic history/diff analysis.

A literal bisect is justified only if:

- multiple candidate changes remain plausible;
- an automated regression can be applied cleanly to historical states;
- the result will materially reduce uncertainty.

Do not force-port modern test code into old revisions if doing so changes the behavior under investigation.

The objective is to identify the broken state contract, not merely a commit number.

---

# 15. Phase 2 — State-Machine Reconstruction

Codex should construct a concise state diagram for:

## Path A — normal process initialization

```text
app/process starts
→ device/session acquisition
→ DEVICE state acquisition
→ EQ state acquisition
→ My EQ backing model
→ UI ready
```

## Path B — post-Flash replacement session

```text
Flash starts
→ mutations
→ Save
→ expected detach
→ old session invalidated
→ replacement session acquired
→ DEVICE refresh
→ EQ refresh
→ EQ acceptance/publication
→ My EQ backing model
→ operation completion/UI ready
```

Locate every point where Path B differs materially from Path A.

Particularly distinguish:

```text
refresh coroutine/function returned
```

from:

```text
authoritative current-session EQ bundle was published and adopted
```

These are not equivalent unless source proves they are.

---

# 16. Luna Sub-Agent #2 — Root-Cause / State-Machine Analysis

Use:

```text
Luna 6.0 High
```

Provide only:

- authoritative physical evidence;
- Repository Capability Digest pieces needed for Android architecture;
- relevant source files/functions;
- relevant historical diff map from Luna Medium;
- process-start and post-Flash state diagrams;
- PR #88/#89 relevant diffs/tests.

Do not provide unrelated release history.

Ask Luna High to determine:

1. exact wrong/missing state;
2. where it is produced or lost;
3. why DEVICE can be current while EQ remains absent;
4. why process restart repairs the state;
5. why PR #88 was insufficient;
6. why PR #89 was insufficient;
7. why the physical candidate executed zero retry attempts;
8. which invariant should govern replacement-session convergence;
9. what deterministic automated test best models the real hardware failure.

Require:

```text
one primary root cause
```

and at most one competing hypothesis if genuinely unresolved.

Require exact source references.

---

# 17. Root-Cause Gate

Before production edits, Codex must be able to answer all of these:

```text
What exact state is wrong?
Which state object backs My EQ?
Where is it expected to become current?
Why does My DAC recover independently?
What post-Flash transition fails?
What normal process-start transition succeeds?
Which historical change introduced or enabled the broken contract?
Why did PR #88 fail?
Why did PR #89 fail?
Why did PR #89's retry not execute?
```

Target:

```text
ROOT_CAUSE_CONFIDENCE >= 95%
```

If confidence is below 95%, do not guess.

Use the cheapest discriminating source/test experiment first.

Hardware remains forbidden.

---

# 18. Phase 3 — Automated Regression Before Fix

Build a regression test matching the physical state exactly.

Required setup:

```text
same app/ViewModel lifetime
Flash/replacement-session path
replacement session current
DEVICE snapshot current
first full EQ acquisition completes/returns
no accepted current-session EQ bundle exists
My EQ backing state absent
```

Required failure:

```text
My DAC/device state may appear healthy
while
My EQ remains blank/unavailable
```

Do not model the first EQ attempt as throwing an exception unless source evidence shows the physical path actually throws.

The regression must fail against the broken implementation for the same architectural reason as the physical failure.

If the existing test seam cannot represent this state, improve the test seam minimally.

Do not redesign production architecture just for testing.

Set:

```text
REGRESSION_REPRODUCED=YES
```

only after the test faithfully represents the physical failure.

---

# 19. Fix Decision Rule

Only after the regression reproduces should Codex decide the repair.

Do not automatically preserve PR #88 or PR #89.

Codex may:

```text
keep
simplify
partially revert
fully supersede
```

those changes according to the proven state machine.

Prefer a correct convergence invariant over compensating retries.

Conceptually, EQ-dependent UI should never silently settle into:

```text
Connected
DEVICE current
EQ unknown/missing
operation considered fully complete
My EQ permanently blank
```

A valid terminal state must be either:

```text
A. authoritative current-session EQ state is available
```

or:

```text
B. explicit bounded loading/error/unknown state with a safe recovery path
```

Permanent silent blank state is invalid.

---

# 20. Safety Requirements for the Production Fix

Preserve:

```text
one Flash mutation sequence
correct User 1 selection
correct five-band writes
correct global gain
one Apply where required
one Save where required
expected detach handling
fresh-session reacquisition
old-session invalidation
no mutation replay
no stale-session read adoption
no stale-session writes
```

Do not add:

```text
infinite retry
unbounded polling
arbitrary production sleeps
second automatic Flash
automatic app restart
stale generation reuse
```

If bounded read retry remains after reconstruction, it must be:

```text
read-only
current-session-only
strictly bounded
cancelled on session replacement
not used as a substitute for a broken convergence contract
```

---

# 21. Required Regression Coverage

At minimum prove:

## Healthy path

```text
replacement session
→ DEVICE current
→ EQ current
→ My EQ populated
```

## Real physical-failure path

```text
replacement session current
DEVICE current
EQ acquisition yields no accepted bundle
```

and then prove the corrected implementation enters the intended recovery/error/convergence path.

## First acquisition succeeds

No unnecessary retry/recovery.

## Repeated EQ read failure

No infinite loop.

No silent permanent blank state.

## Session replaced during recovery

Superseded session result is rejected.

## Old-session bundle arrives late

Never accepted for the current replacement session.

## No mutation replay

EQ recovery never replays:

```text
band writes
global gain
Apply
Save
Flash
```

## Process-start parity

Where applicable, the state requirements for My EQ after post-Flash reconnect must match the relevant invariant established during normal process initialization.

---

# 22. Production Fix Branch

Once root cause is proven, create a clean branch from current `main`.

Suggested:

```text
codex/fix-ja11-postflash-convergence-final
```

Do not implement final production changes on the orchestration branch.

Keep the production diff narrow.

Avoid unrelated cleanup/refactoring.

---

# 23. Local Validation

Use repository-local skills/scripts discovered during Phase 0.

At minimum run the applicable equivalents of:

```text
focused regression
relevant JA11/session tests
:app:testDebugUnitTest
:app:lintDebug
:app:assembleDebug
```

If UI backing-state behavior changes and repository tools support it, run relevant UI/Robolectric/instrumentation coverage.

Record exact commands and outcomes in `EVIDENCE.md`.

---

# 24. Luna Sub-Agent #3 — Fresh Adversarial Review

Only after:

```text
root cause established
regression reproduces
production fix implemented
local tests green
```

spawn a fresh:

```text
Luna 6.0 High
```

Give it only:

- physical evidence summary;
- root-cause claim;
- relevant pre-fix source;
- production diff;
- regression tests;
- safety invariants.

Do not tell it to assume the fix is correct.

Ask it to falsify the solution.

It must inspect:

```text
coroutine ownership
Flow/StateFlow semantics
generation/session ownership
cancellation
late results
navigation/lifecycle interactions
failure terminal states
retry bounds
mutation replay risk
stale-session adoption
```

Require a concise verdict:

```text
APPROVE
or
BLOCK with exact evidence
```

Do not spawn another independent full investigation unless this reviewer exposes a real contradiction.

---

# 25. CI Strategy

Use the existing repository CI/tooling discovered in Phase 0.

The repository has an exact-head hosted CI fallback capability created during this investigation.

Use it when automatic PR event delivery is unreliable.

For the final production head:

```text
dispatch exact-head CI
explicitly provide requested source SHA
checkout requested source
assert git rev-parse HEAD == requested SHA
run real jobs
```

A zero-job workflow run is not a pass.

A run testing a different SHA is not a pass.

Require all relevant gates, including where applicable:

```text
focused regression
Android build
full unit tests
lint
minimum API smoke
emulator UI smoke
API 35 smoke
CodeQL
catalog/community validation
```

---

# 26. CI Self-Healing

Codex may autonomously repair clear infrastructure/test-helper failures such as:

```text
AVD paths
emulator lifecycle
ADB in CI
boot readiness
artifact routing
workflow dispatch
helper timing
checkout/provenance assertions
```

Rules:

- classify the first causal error first;
- do not weaken assertions;
- do not skip supported API levels;
- keep production code unchanged for infra-only fixes;
- use separate CI/test-infra commits or PRs where practical;
- rerun only evidence invalidated by the infra change.

Do not spawn a Luna CI agent unless Sol cannot classify the failure efficiently.

If needed, use:

```text
Luna 6.0 Medium
```

for one narrow CI-log diagnosis.

---

# 27. Production PR

Open one focused production PR.

PR description must explain:

```text
physical symptom
root cause
historical regression context
why v0.8.0 was used only as control
why PR #88 failed
why PR #89 failed
new state invariant
regression coverage
JA11 protocol/mutation behavior unchanged
physical acceptance still pending
```

Do not claim physical success before phone testing.

---

# 28. Signed Candidate

Once:

```text
production fix is stable
local tests are green
independent review approves
hosted exact-head CI is green
CodeQL is green
```

prepare one exact signed candidate if existing trusted repository infrastructure permits it safely.

Verify:

```text
source SHA
workflow/run
artifact ID
APK filename
APK SHA-256
package
versionName
versionCode
signer SHA-256
embedded source/provenance
```

Do not install it on the Pixel.

Do not access the Pixel.

If signing requires merging first, follow the repository's existing safe release procedure.

Do not invent a new signing path.

Do not expose secrets to untrusted workflow modifications.

---

# 29. No Public Release Overnight

Do NOT:

```text
publish a release
create a public release tag
replace v0.8.1
delete v0.8.1
rewrite release history
```

The likely eventual result may be a patch release such as v0.8.2, but publication occurs only after physical acceptance and owner approval.

---

# 30. Hard Stop Conditions

Stop production mutation if any of these occur:

```text
root cause remains below 95% confidence
automated evidence contradicts the proposed root cause
a protocol/mutation change appears necessary
candidate provenance cannot be proven
signing cannot be performed safely
repository credentials prevent required work
physical hardware is genuinely required to discriminate remaining hypotheses
```

When stopped, prepare the smallest possible morning question/action.

Do NOT stop merely because of:

```text
emulator problems
CI plumbing
test helper issues
workflow routing
ordinary mergeability cache state
temporary GitHub Actions irregularities
```

Solve recoverable infrastructure autonomously.

---

# 31. Morning Completion Targets

## Ideal State

```text
root cause proven
real physical state reproduced automatically
fix implemented
tests green
fresh adversarial review approved
exact-head CI green
signed candidate ready and provenance verified
Pixel untouched
JA11 untouched
```

Set:

```text
PHYSICAL_TEST_READY=YES
```

## Acceptable Partial State

If signing alone cannot proceed safely:

```text
root cause proven
fix implemented
automated regression green
all tests/CI green
one exact signing blocker remains
```

Document only that blocker.

## Diagnostic State

If source/test work proves hardware is required to distinguish remaining behavior:

Do not guess.

Prepare one exact diagnostic/acceptance procedure and stop.

---

# 32. MORNING_HANDOFF.md

Continuously maintain:

```text
agent-runs/ja11-postflash/MORNING_HANDOFF.md
```

When the owner returns, it must begin with exactly one of:

```text
READY FOR OWNER PHYSICAL ACCEPTANCE
```

or:

```text
NOT READY — <single exact blocker>
```

Then include:

```text
ROOT CAUSE
- exact broken invariant:
- relevant historical change:
- why process restart repairs it:
- why PR #88 failed:
- why PR #89 failed:
- confidence:

FIX
- branch:
- PR:
- source SHA:
- production files:
- protocol behavior changed: YES/NO
- mutation behavior changed: YES/NO

AUTOMATED REGRESSION
- physical state modeled: YES/NO
- fails before fix: YES/NO
- passes after fix: YES/NO

VALIDATION
- focused:
- unit:
- lint:
- assemble:
- minimum API:
- API 35:
- exact-head CI:
- CodeQL:
- adversarial review:

SIGNED CANDIDATE
- ready: YES/NO
- source SHA:
- APK SHA-256:
- signer:
- provenance:

PHONE ACCESSED OVERNIGHT:
NO

OWNER ACTION:
<one exact action>
```

---

# 33. Morning Phone Unlock

Only after the owner sends:

```text
OWNER PRESENT — AUTHORIZE PHONE ACCESS
```

may Codex begin device interaction.

Then Codex may autonomously:

```text
restore wireless ADB
download/fetch exact signed candidate
verify candidate hash
install exact candidate
verify installed identity
launch EQ Library
prepare logcat/state capture
confirm JA11 availability
establish non-mutating baseline
```

Do not Flash automatically.

When ready, stop with exactly:

```text
PHYSICAL ACTION REQUIRED — PRESS FLASH ONCE
```

---

# 34. Final Physical Acceptance

After the owner presses Flash exactly once:

Codex resumes observation.

Do not ask for an app restart.

Verify in the same process:

```text
expected detach/re-enumeration
replacement session current
DEVICE snapshot current
authoritative EQ state available
My EQ populated
filters visible
Edit EQ available
My EQ interactive
My DAC interactive
navigation remains live
no app restart required
```

Do not repeat unrelated qualification.

No Mic test.

No UAC test.

No volume/preset persistence test.

No true-power-cycle campaign.

If acceptance fails:

```text
do not Flash again
do not restart app
do not unplug JA11 unless required to preserve safety
capture live evidence immediately
stop
```

---

# 35. Final Acceptance Criteria

The technical fix is qualified only when all applicable criteria are true:

```text
root cause confidence >=95%
actual physical-failure state reproduced automatically
fix corrects that regression
no protocol mutation introduced
no mutation replay introduced
session-generation safety preserved
focused regression PASS
full unit PASS
lint PASS
assemble PASS
minimum API PASS
API 35 PASS
exact-head hosted CI PASS
CodeQL PASS
fresh adversarial review APPROVE
signed-candidate provenance verified
one final physical Flash acceptance PASS
```

---

# 36. Orchestrator Autonomy Directive

Codex Sol 6.1 owns the mission.

Do not ask the sleeping owner routine engineering questions.

Use local repository skills, tools, scripts, and instructions before inventing new tooling.

Use Luna sub-agents only where their reasoning adds value.

Do not make multiple agents reread the same repository context.

Curate context.

Persist state.

Resolve recoverable CI/test/tooling blockers autonomously.

Do not preserve previous failed patches merely because they exist.

Do not regress proven JA11 safety behavior.

Do not touch the Pixel or JA11 before explicit authorization.

The overnight goal is:

> A root-cause-proven, regression-tested, independently reviewed, exact-source signed candidate waiting for one decisive physical verification — with the owner's phone and JA11 untouched overnight.
