# v0.9.0 recovery amendment — built-in program/volume persistence and digital filter

Status: **ACTIVE — owner-approved and locked 2026-11-01.** This amendment is the controlling scope for
the v0.9 recovery workstream. It links the original approved plan; it does not rewrite it.

| Item | Value |
|---|---|
| Repository | `weekssa/OPRA-EQ-for-UAPP` |
| Planning chat (approval) | `01a19514-4cdf-7551-8585-c36c622d29b6` |
| Execution chat (this workstream) | `01a19534-aca1-7842-a1fc-c55db5316e0d` |
| Isolated worktree | `/Users/stephenweeks/Documents/Codex/OPRA-EQ-v0.9.0-recovery` |
| Branch | `codex/v0.9.0-recovery` (from `origin/main` `8233d87b1ed7ccb59d6df8bec864da3af761b21b`) |
| Sealed plan | [PLAN.md](PLAN.md) — SHA-256 `a108a2182a28a82252c64956216d31b6dcba9c67bf522a51d6d86a073d6e729b` (unchanged) |
| Seal manifest | [SEAL.sha256](SEAL.sha256) — SHA-256 `b258bc43c156f247cb13ef4372286aafe20e643b52980bd6a7bf4a035f912a6b` (unchanged) |
| Requirements | [REQUIREMENTS.md](REQUIREMENTS.md) (reconciled in place; original mappings retained) |
| Evidence report | [EXECUTION_REPORT.md](EXECUTION_REPORT.md) (beta.1 history) and [RECOVERY_REPORT.md](RECOVERY_REPORT.md) (this workstream) |

The sealed plan and seal manifest are **not** edited. Missing beta.1 features are never recorded as
delivered by rewriting the original plan; the delta lives here and in the reconciled requirement
status.

## 1. Why this amendment exists

The owner-approved v0.9.0-beta.1 candidate shipped a **native defaults path restricted to User 1**.
Every other built-in program (Vocal, Classic, Bass, Off) still required the microphone round trip to
reach the device's startup configuration. The owner reports that built-in presets changed at runtime
but **reverted after physically unplugging the JA11**, which is the exact failure this recovery must
remove.

The owner has now locked a broader product contract:

1. Saving the **selected built-in program and device volume** survives a genuine unplug/replug.
2. No **automatic microphone off/on cycle** occurs for volume/program saving.
3. A **Digital filter** selector (FAST-LL, FAST-PC, Slow-LL, Slow-PC, NON-OS) sits directly below
   EQ program in My DAC → DEVICE.
4. A private, separately installed **EQ Library 0.9 Beta** is prepared for the owner's phone session.

The beta.1 safety work (truthful defaults failure publication, rejection of changed-EQ false success,
fresh-staging/Apply/one-Save transaction fencing, no replay) is carried forward. Nothing here lowers
a beta.1 safety guarantee.

## 2. Supersessions (explicit, narrow, owner-authorized)

| Beta.1 requirement | Recovery rule |
|---|---|
| Official merge → main-only signing → physical qualification | Private, locally test-signed beta qualifies first; merge/publication stay separately owner-gated. This supersedes only the **ordering** for this recovery. |
| Candidate identity `0.9.0-beta.1` / code `15` | Recovery candidate is `0.9.0-beta.2` / code `16`; later corrected candidates advance code consistently (`17`, `18`, …) and version (`beta.3`, …). |
| Package `com.weekssa.opraeqforuapp` | Private beta package `com.weekssa.opraeqforuapp.v090beta`; public stable and the older `.v083beta` beta keep their data and identities. |
| Native defaults = User 1 only; other programs use the mic round trip | Native program/volume save covers **all five** programs with zero microphone writes. |
| All five digital filters deferred, no control | One Digital filter selector is implemented; availability is negotiated with the device at runtime and unsupported/unknown modes never become functioning controls. |

Unchanged and still binding: no firmware/bootloader work, no architecture replacement, no speculative
opcodes, no automatic retry of an uncertain Save, no replay after reconnect, no public hardware claim
without physical evidence, and merge/signing/publication/hardware-mutation approval remains owner-held.

## 3. Verified starting state (2026-11-01 03:43–04:00 UTC, Saturday off-peak)

- `git fetch origin` clean; `origin/main` = `8233d87b1ed7ccb59d6df8bec864da3af761b21b` ("Merge pull
  request #123"); planning checkout `/Users/stephenweeks/Documents/Codex/OPRA-EQ-development` clean on
  `main` at the same SHA.
- Baseline commits resolved in the local object database: stable `89975550`, beta.1 baseline
  `35f0c90a`, beta.1 merge `0e378a32`.
- `docs/releases/v0.9.0/PLAN.md` SHA-256 re-verified `a108a218…`; `SEAL.sha256` unchanged.
- No `v0.9.0-beta.2` tag exists locally or in `git ls-remote --tags`; version `0.9.0-beta.2`/code `16`
  is available.
- Historical beta worktree `/Users/stephenweeks/Documents/Codex/OPRA-EQ-v0.9.0-ja11` preserved.
- Execution model verified as `deepseek/deepseek-flash` (this thread). OpenCodex proxy healthy; v1
  delegation; managed subagent default `deepseek/deepseek-flash`/High with
  `syncCodexSubagentDefaults: true`; global fallback closed; DeepSeek fallback closed; OpenAI fallback
  open (weekend off-peak); `combo/cheap-first` and `combo/research` open. Global `config.toml` model
  restored to `deepseek/deepseek-flash` per the workflow lock check (it had been left on
  `gpt-6.1-sol` by the planning GUI); no client restart and no credential change.
- Working tree of the recovery branch clean; no untracked preservation work was required (shared main
  checkout was already clean).

## 4. Normative contract carried from the owner

The owner's full approved text is preserved verbatim in **Appendix A**. The normative summary below
binds implementation, testing, and reporting.

### 4.1 Goal and baseline

Use v0.8.3 application behavior as the product baseline while keeping current repository and
infrastructure improvements. The missing feature is: **save the selected built-in JA11 program and
device volume across genuine unplug/replug without automatically switching the microphone off/on.**
Custom-EQ Flash is not the replacement target; preservation of the custom User 1 bank and global gain
is a guardrail. A successful runtime readback is not persistence.

### 4.2 Reference-first engineering

The pinned reference `Ircama/ja11-config@affddff6` and its retained build/comparator/licence evidence
remain primary. Established framing/staging/Apply/Save order and timing are adapted into the existing
Android transport, repository, session and mutation gate. Qualified Android codecs are preserved where
the reference arithmetic conflicts. Reference host-stored preset names are **not** JA11 built-in
program selection. No invented opcode, firmware update, raw production console, or chipset
generalization.

### 4.3 Approved UX and save policy

My DAC → DEVICE:

- **Audio:** Device volume; EQ program; **Digital filter** directly below EQ program.
- Digital filter is **one** selector with mutually exclusive FAST-LL, FAST-PC, Slow-LL, Slow-PC, NON-OS.
- **Microphone:** manual Headset / mic remote toggle, retained.
- Program/volume: if persistent save is nondisruptive, completing the edit applies and saves with no
  extra confirmation. If save restarts/disconnects the DAC, apply at runtime first, then offer **Set
  as Default** for the current program/volume pair and explain the interruption before saving.
- Intermediate volume adjustments stay local until committed; cancelled staging sends no command.
- Runtime success, saved startup state, stale state and uncertain failure are distinct. No cached
  state replay on reconnect.

### 4.4 Private beta

Clearly labeled **EQ Library 0.9 Beta**, `com.weekssa.opraeqforuapp.v090beta`, release-derived
minified build, consistent local test signer, stable update prompts off, exact source/version/APK
checksum/signer provenance. Stable and older beta apps/data remain intact. No public distribution and
no copying of app-private user data.

### 4.5 Mandatory autonomous gates

Every original obligation plus this amendment must map to actual evidence; contradictory "complete"
results reopen. Required coverage includes: protocol/repository/session/real ViewModel tests for all
five program targets with changed/unchanged volumes, bounds, companions, both original mic states,
zero automatic mic writes, exact counts/order; a stateful fake that distinguishes runtime from
retained bank/settings with simulated power loss; malformed replies, stale/replacement sessions,
permission denial, detach/delayed restart, timeout, partial mutation, cancellation, ambiguity,
mismatch, no replay, truthful failure publication and read-only recovery; complete feature UI tests
(edit/save policy/default action, filter choices, manual mic, cancel/no writes, rapid taps,
navigation/recreation, narrow/large text, accessibility, terminal states); existing catalog/library,
Favorites/My EQs, Personal EQ, import/export, settings/update, offline/error, custom Flash/editor/Reset
and proportional other-DAC regressions; JVM/lint debug+release+beta builds, androidTest assembly, R8
mapping, repository/security/release contracts; API 26 compatibility plus API 35/36 instrumentation on
this Mac, exact candidate install/coexistence, lifecycle/background/recreation/process restart,
offline/accessibility/large fonts; and exact minified private-beta install, signature/package/version/
hash/source verification with instrumentation compatibility.

Missing, skipped, zero-discovery or zero-job tasks are not PASS. Debug success is not a silent
substitute for minified-beta instrumentation.

### 4.6 Alternatives and owner checkpoint

Prepare evidence-supported **Candidate A** (native, no automatic mic) and, only if justified, a
separately identified **Candidate B** (explicit old mic workaround) with its own artifacts, checksums,
signer, tests and increasing version codes. The fallback must cover both original mic states and
restoration and is never invoked automatically by A.

The first planned owner checkpoint is **phone readiness after all independent work, tests, review and
candidate/fallback preparation**, with a concise readiness packet and the request for the final
wireless-ADB session. No phone access or DAC mutation happens before that gate.

### 4.7 Completion and authorization

Complete means working features + applicable automated gates + demonstrated exact hardware results +
restoration + consistent requirement/capability/ledger/runbook/PR/release records. Merge, official
signing, publication, credential changes and public hardware claims remain separately owner-gated. No
schedules, recurring automation, new Goals or external messages are authorized.

## 5. Design decisions recorded under this amendment

These are the smallest implementations that satisfy the contract; they are revisable only with
evidence or owner direction.

1. **Native save covers all programs through fresh round-trip staging.** The transaction reads the
   active program's five bands and global gain from the device, restages those exact values, applies
   once, verifies the working state, sends the single reference Save(3), then verifies a fresh final
   state (accepting one bounded re-enumeration). It never writes the microphone or UAC, never selects a
   helper program, and never substitutes a flat curve. The acoustic meaning of a built-in program's
   coefficient set stays unclaimed; only round-trip preservation is claimed.
2. **Save is treated as potentially disruptive.** The existing transport already tolerates a
   post-Save detach, so the approved two-step policy applies: completing a program/volume edit applies
   it at runtime, then the screen offers "Set as Default" for that pair with the interruption disclosed.
3. **Runtime versus saved state is shown separately.** A verified runtime change marks the pair as
   "set for this session"; only a verified Save marks it "saved for startup". Session loss returns the
   indicator to neutral instead of replaying cached state.
4. **Digital filter support is negotiated at runtime.** The row is always present. A strictly parsed
   `0x11` read reply with a value in 1..5 makes the selector usable; otherwise it shows "Not available
   on this JA11" and sends no write. Writes verify readback and companions; filter persistence is not
   claimed.
5. **Requirement status is reconciled in place.** REQUIREMENTS.md keeps every original row and gains
   amendment rows (prefix `RC-`); component records are updated where this workstream changes the
   evidence, never deleted.

## 6. Amendment obligation map

Statuses: PENDING / IN PROGRESS / PASS / FAIL / DEFERRED / NOT RUN. Evidence lands in
[RECOVERY_REPORT.md](RECOVERY_REPORT.md) and REQUIREMENTS.md component records.

| ID | Obligation | Verification | Status |
|---|---|---|---|
| RC-01 | This amendment recorded durably before feature edits, linked to both chats; sealed plan untouched | This file + hash check of PLAN/SEAL | PASS |
| RC-02 | Isolated recovery worktree, no shared-checkout edits | worktree list, branch, clean status | PASS |
| RC-03 | Native program/volume save for all five programs, no mic writes | Stateful-fake defaults suite (16 tests) | PASS software |
| RC-04 | Exact staging of fresh five bands + global gain, one Apply, one Save(3), counts/order | deterministic stateful fake | PASS software |
| RC-05 | Changed-EQ and companion mismatch detected, truthful failure, no replay | negative tests | PASS software |
| RC-06 | Runtime change then explicit Set as Default with disconnect disclosure | Compose DEVICE tests (API 35) | PASS |
| RC-07 | Intermediate volume staging local; cancel sends no command | Compose + state tests | PASS |
| RC-08 | Digital filter single selector below EQ program; 5 mutually exclusive modes | protocol + Compose tests | PASS software |
| RC-09 | Filter availability requires validated device reply; unsupported sends no write | protocol/negative tests | PASS software |
| RC-10 | Manual microphone control unchanged; no automatic mic cycle in native path | repository invariants + UI | PASS software |
| RC-11 | Session/lifecycle: stale, replacement, detach, timeout, cancellation, no replay | JVM + instrumentation | PASS software |
| RC-12 | Full regression coverage (catalog, My EQs, Personal EQ, import/export, settings, other DACs) | 830 JVM + 306 Python + 78/78 x2 instrumentation | PASS |
| RC-13 | JVM/lint debug+release+beta, R8 mapping, repository/Python/contract checks | Gradle + tools | PASS |
| RC-14 | API 26/35/36 emulator gates on this Mac | emulator runs | PASS |
| RC-15 | Private beta identity, minified build, provenance, signature/package/version/hash | artifact record | PASS |
| RC-16 | Minified private-beta install + coexistence with stable/older beta | emulator evidence | PARTIAL - install/identity/lifecycle PASS on API 26/35/36; minified-app instrumentation not achievable (diagnosed) |
| RC-17 | Candidate B decision documented (prepared only if justified) | report | DEFERRED until the phone session decides |
| RC-18 | Owner readiness packet and troubleshooting table | RECOVERY_READINESS.md | PASS |
| RC-19 | Maintained requirement/runbook/capability/ledger/state updates | docs diff | PASS |

## 7. Evidence log (append-only)

- 2026-11-01 03:43Z — execution context verified (`deepseek/deepseek-flash`); remote/preflight state
  captured above; worktree created; no source edits yet.
- 2026-11-01 03:5xZ — this amendment written; PLAN.md and SEAL.sha256 unmodified.
- 2026-11-01 05:10Z - implementation fd63a81e complete: 830 JVM, 306 Python/contract, lint/build/R8, API 35 and API 36 instrumentation 78/78 each, API 26 minified install/launch, API 36 lifecycle/font checks. Private beta com.weekssa.opraeqforuapp.v090beta 0.9.0-beta.2/16, APK 35bad66c...3a1 (final artifact, supersedes the intermediate 03e797c1 build), local test signer 0f89797b...869f. Minified-app instrumentation diagnosed as an AGP classpath limitation (RC-16 PARTIAL). Physical session not started.

## Appendix A — owner-approved contract text (verbatim, execution-chat input)

> Execute the owner-approved and locked OPRA-EQ v0.9 recovery plan
>
> This is the fresh execution chat requested by the owner in planning chat
> 01a19514-4cdf-7551-8585-c36c622d29b6. The owner confirmed and locked the plan, then explicitly
> instructed its implementation and creation of this fresh project chat. Primary must be
> deepseek/deepseek-flash, High reasoning. Verify the actual thread model before substantive
> execution. Use one implementation writer. Continue autonomously until all independent software work
> is complete and the private beta is ready for the final owner phone session. Do not stop at merely
> acknowledging this handoff.
>
> ## Start and durable authority
> Read completely AGENTS.md and docs/CHATGPT_PROJECT_RUNBOOK.md in the selected checkout and the canonical /Users/stephenweeks/Documents/Codex/Codex-Usage-Policies/WORKFLOW.md. Read docs/PROJECT_STATE.md; docs/releases/v0.9.0/PLAN.md (including the original specification), REQUIREMENTS.md, EXECUTION_REPORT.md, REFERENCE_EVIDENCE.md, HARDWARE_CHECKLIST.md, SEAL.sha256; relevant maintained JA11 architecture, UX, protocol, capability, validation, testing, and signing records; and the earlier docs/implementation/v0.8.3-defaults-beta records.
>
> Verify repository weekssa/OPRA-EQ-for-UAPP, remote state, current branch/worktree/user changes, releases, and original seal. Preserve unrelated changes and historical evidence. Establish an isolated recovery worktree; do not edit the shared main checkout. Do not use the retired controller. First report the verified starting state, then continue without another routine approval request.
>
> First substantive execution deliverable: preserve this complete approved contract durably in the isolated worktree as a linked recovery amendment, with a source/approval pointer to this chat and the originating planning chat. Reconcile maintained requirements and status. Never alter the original PLAN.md or SEAL.sha256 to pretend missing features were delivered. Do not rely on a chat-only handoff.
>
> ## Observed preflight and useful evidence (reverify, not authority)
> Planning checkout /Users/stephenweeks/Documents/Codex/OPRA-EQ-development was clean on main at 8233d87b1ed7ccb59d6df8bec864da3af761b21b; current remote main was verified at that SHA earlier in this planning session. Stable v0.8.3 source is 899755507f812c940f3ab9381a856c80c6030885. Original beta baseline was 35f0c90a0e55e701d4343ed978a440d34aae13aa. Beta PR119 merged as 0e378a3266ba3e5bfd08ad8a69c9c0909386cd2c. Historical beta worktree /Users/stephenweeks/Documents/Codex/OPRA-EQ-v0.9.0-ja11 is preserved at f0e0f84d on codex/v0.9.0-beta1-evidence.
>
> Original plan SHA256 a108a2182a28a82252c64956216d31b6dcba9c67bf522a51d6d86a073d6e729b was verified unchanged.
>
> Retained private evidence root: /Users/stephenweeks/Library/Caches/OPRA-EQ/v0.9.0-beta.1/execution-20261030T231605Z. Its reference/ contains original pinned source, compiled configuration tool, source manifest, comparator and results; do not run firmware/boot tools.
>
> Signed old beta APK SHA256 845c42e77180aa0a5f36eae9cb95c9c765be294e17ae58829c1b12d102c09f79, source 0e378a32, signing run41167513158. No formal physical qualification was recorded; never transfer old simulation results into hardware PASS.
>
> Planning UX preview: /Users/stephenweeks/.codex/visualizations/2026/11/01/01a19514-4cdf-7551-8585-c36c622d29b6/ja11-feature-placement.html. Its original separate-action mock was subsequently amended by the save policy below; this contract controls.
>
> Preflight at 2026-11-01 03:42 UTC (Saturday night Oct31 local Chicago): official DeepSeek pricing verified weekend off-peak, Flash rates USD0.003/cache-hit,0.15/cache-miss,0.60/output per1M. Recheck dispatch time. Proxy healthy; v1 enabled; managed subagent default DeepSeek/high and syncCodexSubagentDefaults true; global and DeepSeek fallback closed, OpenAI fallback open, route-scoped combos open. ocx status showed live proxy, managed service viable and shim stale but no explicit pending client restart. Planning GUI had global config model gpt-6.1-sol. Explicit DeepSeek selection for this fresh chat is intentional: recheck global/default and active thread; restore the supported required DeepSeek default before substantive execution if still mismatched, without restarting active clients or changing credentials. The planner made no configuration or project changes.
>
> ## 1. Actual goal and safe baseline
> Use v0.8.3 APPLICATION BEHAVIOR as the product baseline, preserving independent current repository/infrastructure improvements. This is not an instruction to reset shared main or downgrade the owner's phone. Beta production diff against stable was confined to four app source files plus version metadata. Carry forward reviewed, verified safety fixes and tests, including discarded defaultsFailure publication and rejection of changed-EQ false success.
>
> The actual missing feature is saving the selected built-in JA11 program and device volume across genuine unplug/replug without automatically switching the microphone off/on. Working custom-EQ Flash is not the replacement target; custom User1 bank/global gain preservation is a guardrail. Do not narrow the task to User1-native saving again.
>
> The original worker instruction explicitly narrowed native defaults to User1; other programs retained mic cycling. All five digital filters were deferred. The handoff later overstated software completion. Owner reports built-in presets worked at runtime but reverted after unplugging; exact tested artifact/action trace remains unconfirmed. Old beta source and exact signed APK still contain Apply/Set as Default labels, so a deleted button is NOT a proven source cause. Diagnose actual UI/state/package issue without asking owner to reconstruct old decisions.
>
> ## 2. Reference-first engineering
> Primary reference https://github.com/Ircama/ja11-config/tree/affddff6c9808c33ce8b35b0b9759ff0d7f6e405 remains included. Reuse retained source/build/comparator/license findings; do not restart broad discovery. Study complete call paths and adapt established framing/staging/Apply/Save order/timing into existing Android transport/repository/session/mutation gate. Preserve qualified Android codecs where external gain arithmetic conflicts.
>
> Its host-stored EQ presets are NOT JA11's built-in program selection. Its five-band Apply/Save(3) routine alone proves neither built-in-program/volume persistence nor five-filter applicability. Resolve those specific gaps. No invented opcode, firmware update, raw production diagnostic console, chipset generalization, or architecture replacement.
>
> All five filter modes remain visible recovery targets; absent controls cannot be completed merely by investigation. Do not enable unsupported commands. If missing exact-device evidence blocks them, finish independent work and consolidate precise proof gaps and smallest proposed validation step.
>
> ## 3. Approved UX / save policy
> My DAC → DEVICE.
>
> Audio: Device volume; EQ program; new Digital filter directly below EQ program.
>
> Digital filter is ONE selector with FAST-LL,FAST-PC,Slow-LL,Slow-PC,NON-OS, mutually exclusive.
>
> Microphone section retains manual Headset / mic remote toggle.
>
> Program/volume:
> - If established persistent save is nondisruptive, completing the edit applies and saves automatically, with no additional save confirmation.
> - If save restarts/disconnects the DAC, apply change in runtime first; then offer Set as Default for current program/volume pair and explain interruption before persistent saving.
> - Intermediate volume adjustments are local until committed, not repeated flash writes; cancelled staging sends no commands.
> - Use established transaction restart policy, not surprise experimental restarts.
> - Runtime success, saved startup state, stale state, and uncertain failure remain distinct; no cached-state replay on reconnect.
>
> Normal native candidate contains NO automatic microphone cycling for volume/program. Keep manual mic functional. Old mic round trip is a separately prepared fallback, not hidden automatic behavior after uncertain native Save.
>
> Preserve all custom bank fields and global gain. Restoration may reflash exact freshly captured original values and restore program through a documented verified sequence. Never substitute flat curve or assume lost values recoverable.
>
> ## 4. Private beta/local iteration
> Prepare clearly labeled EQ Library 0.9 Beta, separate applicationId com.weekssa.opraeqforuapp.v090beta, using existing release-derived MINIFIED beta pattern, consistent local test signer, stable update prompts off, exact source/version/APK checksum/signer provenance. Owner understands Android APKs require signing; this is local test signing, NOT official release signing. Stable and older beta apps/data stay intact. No public APK distribution/private data copying.
>
> Prefer successor 0.9.0-beta.2/code16 only after verifying version availability; subsequent candidate revisions increase code consistently.
>
> LOCAL development, tests, emulator operations, documentation and private beta preparation are approved. Official merge/main-only signing are NOT prerequisites to first phone session; this explicitly supersedes old sealed beta1 release order for this recovery. Do not infer release approval.
>
> ## 5. Mandatory autonomous test gates
> Map every original obligation plus amendment to actual evidence, preserving existing completed results and reopening contradictory unsupported completion.
> - Protocol/repository/session/real ViewModel; all five program targets, changed/unchanged volume/bounds, companions and both original mic states, no automatic mic writes in native candidate, precise counts/order.
> - Stateful fake distinguishes runtime and retained bank/settings; simulated power loss catches missing commits but is never hardware proof.
> - Malformed replies, stale/current/replacement sessions, permission denial, detach/delayed restart, timeout, partial mutation, cancellation, ambiguity, mismatch, no replay, truthful failure publication/read-only recovery.
> - Complete feature UI: edits/save policy/default action where applicable, all implemented filter choices, manual mic, cancel/no writes, rapid taps, navigation/recreation, narrow/large text, accessibility, terminal states.
> - Existing catalog/library, Favorites/MyEQs, PersonalEQ, import/export, settings/update behavior, offline/error states, custom Flash/editor/Reset regressions; proportional other-DAC shared-code coverage.
> - Applicable focused/full JVM, lint debug/release/beta, builds debug/release/beta/androidTest, R8/mapping, repository/security/release contracts.
> - Android emulators on THIS MAC: API26 compatibility plus API35/36 instrumentation, exact candidate installation/coexistence, lifecycle/background/recreation/process restart, offline/accessibility/large fonts.
> - Exact minified private beta install, signature/package/version/hash/source verification, instrumentation compatibility. Historical beta instrumentation crashed before discovery (tracing/R8 then Duration mismatch attempts); diagnose, do not count debug success as a silent substitute.
>
> Use checked-in wrapper via tools/codex-android, discover actual tasks. Missing/skipped/zero-discovery/zero-job tasks are not PASS. Local iteration then coherent hosted checks; no repeated merge/sign/download loop. Bounded independent review and original finite corrections, no budget reset by agent/branch/rename.
>
> ## 6. Prepared alternatives and final owner checkpoint
> Prepare only evidence-supported Candidate A(native,no auto mic) and, if justified, separately identified Candidate B(explicit old mic workaround). Preserve artifacts/source/checksums/signers/tests locally, distinct identities/increasing codes. Same new private package/signer can support controlled replacement; don't activate multiple competing USB apps. Fallback must cover both original mic states and restoration. It is not automatically used by A.
>
> Prepare troubleshooting table before owner involvement: runtime failure→stop persistence and diagnose; known failed retention→capture field-specific evidence; uncertain save/restoration→stop mutations/read-only recovery; understood failure + safely recovered baseline→deliberately propose prepared fallback; bad filter response→stop mode writes; connectivity issue→wireless recovery skill safe diagnostics/approval boundaries.
>
> Keep stable APK and DAC restoration material available. App reinstall isn't a DAC backup. Capture actual current configuration/complete recoverable bank before mutations; do not assume Android backs up all private databases. No app-private user-data copying.
>
> FIRST PLANNED OWNER CHECKPOINT is phone readiness AFTER all independent work, tests, review, candidate/fallback preparation. Present concise readiness packet (exact artifacts, all gates/limits, unresolved hardware questions, physical actions, stops/restoration). Then request final wireless-ADB session. No phone access/DAC mutation before this participation gate.
>
> Agent drives original full19-step checklist, adding revised save UX, each newly claimed program/volume retention, implemented filters, manual mic, bank/gain/mic/UAC protection, read-before-write, real power removal, no replay and exact restoration. Owner does only unavoidable unlock/permission/unplug/replug. Mark each actual step when evidence captured.
>
> Never automatically retry uncertain Save, switch mechanisms, or install fallback and repeat an ambiguous operation. Corrected APK needs new identity and affected automated gates before retest. Do not promise one session when a correction genuinely requires requalification.
>
> ## 7. Completion, durable handoffs, authorization
> Record exact outcome/files/branch/source/APK/CI/signer/checks/evidence limits/correction counts/blockers/first unmet requirement/next action in repository records at meaningful checkpoints. Interruption must not reset scope or counters. Persist original approval plus amendment before feature edits.
>
> Finish independent work before consolidated escalation. Do not ask routine product decisions already settled. Missing functionality is not complete because research finished. Complete means working features + applicable automated gates + demonstrated exact hardware results + restoration + consistent requirements/capability/ledger/runbook/PR/release records.
>
> Prepare production package/signature/upgrade checks after private beta acceptance; assess qualification affected by differences. Merge, official signing, publication, credential changes and public hardware-support claims remain separately owner-gated. Do not infer authorization for schedules, recurring automation, new Goals or external messages.
>
> Exact next action: verify DeepSeek/high execution context and workflow/checkout, establish isolated recovery worktree, durably record this entire approved contract as the recovery amendment, reconcile checklist, then perform implementation and validation autonomously to the final owner readiness gate.

