# v0.11 autonomous hardening — sealed plan

Written 2026-11-06 at head 44735e50. Revised to defer TalkBack. This plan is the scope of record
for the next block of work. It is proposed for owner approval; nothing in it begins until the plan
itself is approved and the gate is resealed with it in scope.

## In scope (fully autonomous, no device or owner action)

| # | Workstream | Deliverable | Acceptance |
|---|---|---|---|
| A | Reset persistence | A failing fake-transport test that reproduces \"Reset to flat, Save, new session reads the old EQ\", then the smallest fix in the reset/Save sequencing if the cause is in our code | Repro test red before, green after; existing reset and Save tests unchanged and green; no change to device-facing behaviour unless the fix requires it |
| B | Vocabulary | One vocabulary for target / output / Direct Flash / Save across Settings, My EQs, My DAC and the flash dialogs | No screen uses two words for one concept; copy tests updated |
| C | M3 audit | Ten-category audit of Settings and My DAC, scored, in the evidence log | Every category scored with the reason, not a claim of compliance |
| D | Lint and dead code | Unused parameters, dead composables and lint warnings removed where safe | Lint warning count falls; no behaviour change; suites green |
| E | Regression hardening | Tests for connecting from the bar while another DAC session is open, and for the several-DACs picker | Both cases asserted; no new UI |
| F | Documentation | Handoff, checkpoint, design language and beta notes match HEAD after each workstream | No stale claims; screenshots refreshed if copy changes |

## Out of scope

- Any Pixel observation or hardware action; those need the owner and are listed at the end.
- PR, merge, release, tag, signing, or public support claims.
- Chip, UAC, headset, firmware or native-shelf research.
- Production data migration beyond what already exists.
- Accessibility and TalkBack work, including semantics-audit tasks; deferred by the owner.
- Flash, Save, volume, protocol, session or transport behaviour changes, except the reset fix in A,
  and only when a test demonstrates the defect.

## Method

1. One workstream at a time, in the order A → E. Each starts with a failing test when the work is a
   defect, and ends with JVM + lint + one instrumented run on emulator-5580.
2. Instrumented runs always uninstall both packages first; DELETE_FAILED_INTERNAL_ERROR is an
   emulator flake, handled by uninstalling or cold-booting, never by reverting work.
3. Each workstream commits separately with its documentation, after `scope-guard --root .` reports
   IN SCOPE. No push, no PR, no merge.
4. Any new file needs its own scope amendment (CONTRACT.md + GATES.md, reseal) committed before use.

## Checklist

- [ ] Plan approved and committed, gate resealed with this file in scope
- [x] A1 reset-persistence tests written (persistence-modelling fake)
- [x] A2 app-side transaction sound in the model; hardware run passed (T-043, T-044)
- [x] A3 no code fix warranted: model shows correct single Save and fail-closed rejection
- [x] B1 vocabulary sheet written (T-045)
- [x] B2 copy applied: bar selector now says EQ target; duplicate disclaimer removed
- [ ] C1 M3 audit scored for Settings
- [ ] C2 M3 audit scored for My DAC
- [ ] D1 dead code and unused parameters removed
- [ ] D2 lint warning count recorded before and after
- [ ] E1 bar-connect-while-other-session test
- [ ] E2 several-DACs picker test
- [ ] F1 documentation updated for every workstream
- [ ] F2 final gates recorded and the tree left clean

## Stop conditions (only these)

1. A physical action is required (unplug, replug, a real connection) - I ask, then continue.
2. The gate refuses a scope change and needs owner approval.
3. A genuine product choice cannot be resolved from this plan.
4. A destructive or irreversible action would be required.

Anything else - failing tests, build failures, emulator flakes, an inconvenient result - is work, not a
stop.

## Owner-only items held back

- Pixel re-check of reset persistence after a fix (one unplug, one replug).
- Any merge, release or signing decision.

