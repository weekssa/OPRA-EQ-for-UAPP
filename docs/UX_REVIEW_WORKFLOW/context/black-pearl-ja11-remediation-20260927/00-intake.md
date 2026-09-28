# Intake - Black Pearl and JA11 remediation

Date: 2026-09-27
Status: INTAKE_COMPLETE / implementation and software verification complete
Worker: GPT-5.6 Luna — Extra High, sole bounded implementation worker

## Repository and source lock

- Repository: `weekssa/OPRA-EQ-for-UAPP`
- Remote: `https://github.com/weekssa/OPRA-EQ-for-UAPP.git`
- Refreshed base: `origin/main`
- Refreshed base SHA: `ce5efdf7985e4fc48f975b14fcedb1f592d43772`
- Working branch: `codex/black-pearl-ja11-remediation-20260927`
- Isolated worktree: `/Users/stephenweeks/.codex/worktrees/black-pearl-ja11-remediation-20260927/OPRA-EQ-for-UAPP`
- Source checkout at intake: clean; the original handoff checkout's untracked/user-owned files were preserved and not copied or modified.
- Required source refresh: `git remote -v`, `git status --short --branch`, `git fetch origin --prune` completed in the original checkout; the worktree was then created from the verified `origin/main`.

## Reference artifact

- Path: `/Users/stephenweeks/Library/CloudStorage/GoogleDrive-weekssa@gmail.com/My Drive/OPRA UAPP Presets/EQ Library Testing/EQ-Library-v0.7.0.apk`
- Availability: AVAILABLE
- SHA-256 observed: `3d723ffa17042fbef7e6e192c14ecce460628d0f08a55eeb30caa59566ff8731`
- Package/version: `com.weekssa.opraeqforuapp`, `0.7.0` / code `7`
- Embedded source metadata supplied by owner: `ce5efdf7985e4fc48f975b14fcedb1f592d43772`
- Boundary: reference-only; not copied into Git and not treated as a candidate, acceptance, or physical proof.

## Authorized scope

1. TRN Black Pearl Direct Flash must use typed verified success only after complete final native readback, with the compact verified wording aligned to JA11/EW300.
2. FiiO JA11 My DAC → EQ must expose a safe current verified User 1 five-band editor path through local edit, Review, explicit Apply, existing JA11 transaction/Save/reconnect/final-readback boundary, and truthful result.

Excluded: hardware mutation, automatic mutation retry, protocol-byte/identity/endpoint/timing redesign, unrelated DAC behavior, canonical EQ ownership, merge, publish, tag, public support claim, and main push.

## Required skills loaded

`android-skills:android-dev`, `android-skills:android-testing`, `android-skills:android-debugging`, `android-skills:android-ux`, `android-skills:compose`, `android-skills:kotlin-coroutines`, `android-skills:kotlin-flows`, `android-skills:modularization`, `android-dac-transaction-verification`, `android-dac-research-recovery`, `android-pixel-physical-validation`, `android-release-readiness-orchestrator`, `android-cli`, `testing-setup`, and `unlazy`.

## Specialist roles

The project-local `.codex/config.toml` defines `repo_state`, `architecture`, `failure_analysis`, `dac_transaction`, and `reviewer` roles. This Codex app runtime exposes thread coordination APIs but not a direct native specialist-dispatch tool; no owner-facing task was created. The primary worker retains all writes, integration, testing, and final verification responsibility.

## Initial evidence boundary

The live `origin/main` source and maintained documents are authoritative. Prior local-only Black Pearl work, detached worktrees, old APKs, screenshots, and historic physical results are not proof for this candidate. Luna will not mutate hardware.

## Final intake disposition

The two named software defects were implemented and verified on the branch. The exact signed beta
boundary was rechecked after implementation: signing is restricted to trusted `main`, and the
current candidate manifest did not represent a combined Black Pearl + JA11 target. After the
owner's continuation instruction, the branch was updated with a non-destructive merge of the
refreshed live `origin/main` catalog commits (`0adcc8159a467790104cf2dc797f1279ef2c53ed`) and the
trusted workflow was narrowly extended with a `black-pearl-ja11` manifest target. The workflow
still requires `refs/heads/main`; at that pre-integration checkpoint no signed candidate or Pixel 9
physical action existed from this branch. The pre-integration status was
`MERGE_APPROVAL_REQUIRED`; the current status is recorded below.

## Follow-up intake disposition — terminal-result repair

The owner-authorized follow-up repair was applied after the owner reported that the Android UAPP
routing prompt also occurs in the stock app. The prompt is therefore treated as expected
platform/device behavior and remains unchanged. The final follow-up source is
`9f5cb852994e1c88fce80598f249a97fae047429` on the same remediation branch; only JA11 terminal
presentation, global-feedback suppression, and focused tests changed. Full unit, API-36 emulator,
lint, debug, minified release/R8, and independent review gates pass. No DAC was connected or
mutated by Luna. The owner subsequently authorized minimum main integration. PR #50 merged into
`main` at `b61f02e8c91656f14ffc639e4c6d937b2162a1b7`, and signed-beta workflow #1372 produced the
exact combined candidate recorded in `05-release-handoff.md`. Current disposition is
`READY_FOR_PIXEL_9`; physical validation remains owner-controlled.

## Post-candidate diagnostic disposition — 2026-09-27

The owner-authorized Black Pearl AFUL Explorer Direct Flash did not verify the requested final
gain (`expected raw -7398`, `actual raw -6400`). A later owner-authorized reconnect/full refresh
confirmed the live hardware baseline remained raw `-6400` (`-25.00 dB`). A separate diagnostic
debug package then captured a read-only native HID trace on the exact Black Pearl identity; the
global-gain read response was `4B 80 03 02 00 E7 FF FF ...`, which decodes to raw `-6400` under the
maintained codec. The capture did not identify a safe protocol correction, so the existing source
remains unchanged and no new fix branch push is warranted. Current disposition is `REPAIR_REQUIRED`;
the prior exact signed candidate remains evidence of truthful failure handling, not proof of a
successful Black Pearl Flash.

## Owner-authorized write-side diagnostic disposition — 2026-09-27

The owner then approved one narrowly scoped write-side diagnostic Flash for AFUL Explorer, with no
automatic retry. A temporary debug package selected the AutoEq/Jaytiss Explorer profile (not the
earlier Hifigues community Explorer profile that produced the `expected raw -7398` / `actual raw
-6400` mismatch), connected to the exact Black Pearl identity, and performed exactly one confirmed
Flash. The post-operation My DAC surface showed `Verified current hardware`, `Matches My EQs`, 10
filters, active slot 1, and `Playback gain -31.00 dB` for the AutoEq/Jaytiss profile.

The temporary logger used the wrong report-byte predicate and therefore captured zero raw write
transfer lines; it did not provide the requested raw write proof. The result cannot establish a
source correction for the earlier Hifigues target. The diagnostic package and temporary source
instrumentation were removed, the signed package remained installed and untouched, and no Reset,
retry, Save, Restore, or second mutation was performed. Current disposition remains
`REPAIR_REQUIRED`; no new candidate is available or recommended for push.

## External Black Pearl implementation comparison — 2026-09-27

The owner supplied `https://github.com/Matr1x01/trnBlackPearlEq`. It was inspected read-only at
Android branch commit `45bbf3c65c899181395eb7936615ced1fbd5d4be` and protocol branch commit
`cd1ed0783134723d3c0a69088d739ac965354883`; no source was copied and the other repository was not
modified. Its Android transport detects an interrupt OUT endpoint and uses `bulkTransfer` for writes
when present, falling back to HID `SET_REPORT` only when no OUT endpoint exists.

The captured Black Pearl descriptor for serial `330243E8260129` exposes HID interface 0 with
interrupt IN endpoint `0x86` and interrupt OUT endpoint `0x05`, both 64-byte. OPRA currently stores
only the IN endpoint and always sends writes through `controlTransfer`. This is a credible explanation
for the observed PEQ/global-gain divergence, but it is not yet source or physical proof. Changing the
OPRA write endpoint is explicitly outside the current remediation guardrails, so the source remains
unchanged and the task is blocked pending an owner decision on that boundary.

## Scoped endpoint-path authorization and software verification — 2026-09-27

The owner subsequently authorized the exact narrow source change: use the Black Pearl HID interrupt
OUT endpoint when present, retain HID `SET_REPORT` fallback, preserve report bytes/timing/retry
policy, and perform no hardware write during development. No source was copied from the supplied
reference repository.

- Final source commit: `eb761f1bc510a612acde7b71b453631e1ff23a8f`.
- Changed files: `AndroidBlackPearlUsbTransport.kt`, `BlackPearlOutputPath.kt`, and
  `BlackPearlOutputPathTest.kt` only.
- Interrupt OUT sends the unchanged report and is successful only when all requested bytes are
  transferred. Zero, negative, and short transfers fail closed. If no interrupt OUT endpoint is
  present, the existing control-transfer path and its parameters remain unchanged.
- Existing read draining, bounded two-attempt read retry, session-current predicate, mutation
  serialization, and PEQ/command/Flash settle delays are unchanged. No write retry was added.
- Focused tests, full unit tests, API-36 emulator instrumentation, lint, debug assembly, release
  lint, and release/R8 assembly pass. Independent final review is recorded in `04-final-review.md`.
- Local APKs are development outputs only: debug SHA-256
  `65973da872db8b1283197750ac20425bfe4d4d633f3bc3aa734e6edf3634686c`; unsigned minified release
  SHA-256 `2d516a7057f8120e7c03e76592266f05c076402399d59f8b2f69eb9845f92160`.
- No DAC was connected, written, flashed, applied, reset, saved, restored, or mutated by Luna for
  this source change. No signed beta was produced and no branch was pushed.

Current disposition for this source checkpoint is **SOFTWARE_READY_PENDING_SIGNED_CANDIDATE**.
The earlier physical failure and the historical endpoint blocker remain evidence only; this source
checkpoint is not physical qualification and is not a public hardware-support claim.
