# Release handoff - Black Pearl and JA11 remediation

Status: `MERGE_APPROVAL_REQUIRED`

## Current software handoff

- Repository: `https://github.com/weekssa/OPRA-EQ-for-UAPP.git`
- Branch: `codex/black-pearl-ja11-remediation-20260927`
- Refreshed live base: `origin/main` `0adcc8159a467790104cf2dc797f1279ef2c53ed`
- Final implementation source SHA: `dc6478a25b1745b4f78f833c69e96e066c615d56`
- Promotion-preparation source SHA: `0f080516e8d4aa34e9d0a04b16464bf65b2b6d7d`
- Reviewed branch head: `0c77e081fd6abd12a6e20b482ce269ae9f5bb764`
- Draft review PR: [#49](https://github.com/weekssa/OPRA-EQ-for-UAPP/pull/49), open and mergeable;
  all required remote review checks passed on the exact head. It was not merged.
- Worktree: `/Users/stephenweeks/.codex/worktrees/black-pearl-ja11-remediation-20260927/OPRA-EQ-for-UAPP`
- Black Pearl issue: software PASS; success remains final-readback verified and anti-stacking
  uncertainty is fail-closed.
- JA11 issue: software PASS; User 1-only editor path is complete through exact transaction and
  one-Save/final-readback result.
- Focused/full unit tests, lint, debug assembly, API 36 instrumentation, clean install/cold launch,
  large-text launch, and UI hierarchy smoke: PASS.
- Remote Android CI `#1873`, CodeQL `#1757`, priority-community `#1626`, catalog currentness
  `#2141`, and dependency submission `#2230`: PASS on the exact reviewed head.
- Hardware: NOT RUN by Luna. No DAC was connected or mutated.

## Why this stops before Pixel 9

The repository's trusted `.github/workflows/signed-beta.yml` job still has this exact condition:
`github.ref == 'refs/heads/main'`. The branch now adds the truthful `black-pearl-ja11`
`candidate_target`, with a combined capability profile, this handoff as the test plan, both
software evidence IDs, and owner Pixel 9 hardware validation as outstanding. A branch debug APK is
not a signed beta, and no signed artifact, signer tuple, workflow run, immutable artifact
ID/digest, or exact signed install proof exists for this branch.

The next owner-controlled boundary is:

1. Review and merge this exact branch into trusted `main` if the owner approves the prepared
   promotion; then dispatch the existing signed-beta workflow with
   `candidate_target=black-pearl-ja11`.
2. Verify the resulting exact source SHA, package/version, pinned signer, APK checksum, workflow
   run, immutable artifact ID/digest, candidate manifest, clean install, and cold launch before
   any Pixel 9 hardware session.

No merge to `main`, signing request, tag, public release, public support claim, or hardware mutation
was made by Luna. The workflow branch was prepared and the existing non-public publication path was
not executed.

## Pixel 9 checklist after an exact signed candidate exists

This checklist is prepared but not actionable from the current branch. After the signed workflow
passes, use only the exact candidate APK and record every result against its source SHA, APK
SHA-256, signer certificate, workflow run, and immutable artifact ID.

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
