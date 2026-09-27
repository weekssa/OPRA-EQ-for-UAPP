# Release handoff - Black Pearl and JA11 remediation

Status: `MERGE_APPROVAL_REQUIRED`

## Current software handoff

- Repository: `https://github.com/weekssa/OPRA-EQ-for-UAPP.git`
- Branch: `codex/black-pearl-ja11-remediation-20260927`
- Base: `origin/main` `ce5efdf7985e4fc48f975b14fcedb1f592d43772`
- Final source SHA: `TO_BE_FILLED_AFTER_FINAL_COMMIT`
- Worktree: `/Users/stephenweeks/.codex/worktrees/black-pearl-ja11-remediation-20260927/OPRA-EQ-for-UAPP`
- Black Pearl issue: software PASS; success remains final-readback verified and anti-stacking
  uncertainty is fail-closed.
- JA11 issue: software PASS; User 1-only editor path is complete through exact transaction and
  one-Save/final-readback result.
- Focused/full unit tests, lint, debug assembly, API 36 instrumentation, clean install/cold launch,
  large-text launch, and UI hierarchy smoke: PASS.
- Hardware: NOT RUN by Luna. No DAC was connected or mutated.

## Why this stops before Pixel 9

The repository's trusted `.github/workflows/signed-beta.yml` job has this exact condition:
`github.ref == 'refs/heads/main'`. Its `candidate_target` choices are only `ja11` and `ew300`;
there is no truthful combined Black Pearl + JA11 target. A branch debug APK is not a signed beta,
and no signed artifact, signer tuple, workflow run, immutable artifact ID/digest, or exact signed
install proof exists for this branch.

The owner must separately authorize one of these minimum boundaries before Pixel 9 testing:

1. Integrate this exact branch into trusted `main`, then run the existing signed-beta workflow with
   a manifest/evidence target that truthfully covers both named defects; or
2. Authorize a narrowly reviewed workflow/manifest change that binds the exact branch SHA to a
   non-public signed candidate without weakening signing or hardware evidence rules.

No merge, main push, workflow change, signing request, tag, publication, or public support claim
was made by Luna.

## Pixel 9 checklist after an exact signed candidate exists

This checklist is prepared but not actionable from the current branch. The owner should use the
exact candidate APK and record every result against its source SHA, APK SHA-256, signer certificate,
workflow run, and immutable artifact ID.

### Before connecting either DAC

- Confirm package `com.weekssa.opraeqforuapp`, version/code, signer, source SHA, and APK checksum
  match the immutable candidate manifest.
- Install cleanly or upgrade only after capturing the pre-test app/device state.
- Confirm the app identifies the exact TRN Black Pearl or FiiO JA11 identity; stop on ambiguity,
  permission prompt during mutation, disconnect, competing operation, or session replacement.
- Capture the baseline report and current state before any owner-authorized mutation.

### Black Pearl bounded review

- Use the existing approved exact Black Pearl profile and Direct Flash path only.
- Confirm progress text says the final hardware state is being verified.
- Confirm success appears only when all ten native fields and raw global gain match in final
  readback, with the compact verified wording.
- If any readback is missing/mismatched or the session changes, classify as NOT VERIFIED and do not
  retry automatically. Confirm later Flash/Reset is blocked until a fresh authoritative baseline.
- Restore the original hardware state using the approved owner procedure and verify restoration.

### FiiO JA11 bounded review

- Confirm current program is User 1. For Off/Vocal/Classic/Bass, confirm no User 1 coefficients are
  presented as current and Edit EQ is unavailable.
- With a fresh verified User 1 read, open Edit EQ and make local changes. Confirm no transport write
  occurs during open, edit, reset-local-edits, Review, Back, Close, or Cancel.
- Confirm Review lists all five type/frequency/gain/Q values, global EQ gain, headroom consequences,
  warnings, and that Apply is the first write.
- Apply once. Confirm exact five-band write, quantized global gain, User 1 selection, Apply, volatile
  readback, exactly one Save/reconnect boundary, final readback, and truthful success/failure.
- Stop on any mismatch, timeout, disconnect, permission prompt, or replacement session. Do not retry
  the mutation. Restore the original User 1 state and verify it.

### After the session

- Export readable and technical reports, checksum each artifact, record restoration and each DAC's
  independent result, and classify `PASS`, `FAIL`, or `INCONCLUSIVE`.
- Do not call either issue physically fixed or publicly supported from software/emulator evidence.
- Use `06-post-pixel-closure-prompt.md` for the next bounded closure update.
