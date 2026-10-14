# v0.8.3 JA11 defaults beta state

Status: **IMPLEMENTATION COMPLETE; EXACT BETA V1 FROZEN; MINIFIED INSTRUMENTATION EXCEPTION AWAITS OWNER; PHONE GATE NOT OPEN**

## Repository and candidate

- Repository: `weekssa/OPRA-EQ-for-UAPP`
- Worktree: `/Users/stephenweeks/.codex/worktrees/f5aa/OPRA-EQ-for-UAPP`
- Branch: `codex/v0.8.3-defaults-beta`
- Frozen base: `575dab59fe774d3683a3714ca490a334f1b20764` (`origin/main` at intake)
- Public stable baseline: v0.8.2, source `9983edf0b7fe437aeeec9ac3dcb3864705afa16a`, version code 13
- Old worktree/branch: preserved at `/Users/stephenweeks/.codex/worktrees/ja11-settings-persistence-beta/OPRA-EQ-for-UAPP`, branch `codex/ja11-settings-persistence-beta`; do not modify or transplant its native-Save implementation.
- Requested branch and tag were absent from origin at intake. PR #96 remains a separate open draft for the old candidate.

## Current contract

Runtime Apply changes only the selected volume/program and verifies it. Volume/program dialogs stage values locally. Cancel, Back, dismissal, and row selection issue no write. Set as Default shows the current reviewed volume/program pair and only begins after explicit confirmation. The selected fallback persists by one target write if changed, then an original-state-aware mic round trip under one ViewModel-owned operation gate. Success requires exactly two mic writes, verified replacement sessions, original mic restoration, unchanged UAC/PID/firmware and applicable EQ state, and fresh companion readback. No native EQ Save, UAC, band/gain, EQ Apply, helper selection, or extra reset write is permitted.

The public capability matrix still classifies volume and program as runtime-only across true power loss. The new behavior is a candidate product action using the mic persistence side effect; one beta test result must not be represented as a general firmware contract or public capability without evidence and owner approval. The defaults action never writes EQ-band data. The shared JA11 gate encloses optional read-only User 1 snapshots before and after the mic round trip; fingerprints are compared only when both the initial and final current program expose a complete User 1 snapshot. Otherwise the report marks EQ-bank comparison unavailable. It does not switch programs to inspect a bank. Preservation remains unproven where comparison is unavailable.

## Acceptance gates and current phase

See [GATES.md](GATES.md) and [TEST_RESULTS.md](TEST_RESULTS.md). The intended owner checkpoints are a fresh phone-ready/unlocked/wireless-debugging confirmation after all off-phone gates, unavoidable physical JA11 unplug/replug for the persistence test, and the owner's identified-candidate pass-and-publish instruction.

## Current implementation and validation state

- The initial implementation is committed. Beta explicitly enables R8 and release rules. The frozen Beta instrumentation runner crashes before test discovery on API 26 and 35 because R8 removes `androidx.tracing.Trace` from the split app/test runtime path. Intermediate narrow keep-rule experiments and their later Kotlin `Duration.Companion` mismatch are preserved in `TEST_RESULTS.md`; ineffective app-level keeps were removed from the final candidate.
- Exact-source JVM unit tests, Debug/Beta lint, Debug/Release/Beta packaging, Beta R8 mapping and Beta instrumentation compilation pass on `7cea4545dddbee2da0b436e0684a9c489835e3cd`. The app's `CANDIDATE_SOURCE_SHA` default was corrected; the frozen APK embeds the exact SHA.
- Exact Beta v1 APK provenance, signer and direct install/launch/coexistence checks pass on API 26 and 35. On the same exact source, unminified Debug JA11 instrumentation passes 9/9 on API 26 and full Debug instrumentation passes 72/72 on API 35. Full API 26 Debug is 68/72 with four failures: two unsupported SQLite `DROP COLUMN` migration fixtures and two unrelated Compose idle timeouts. These are proposed gate substitutes, not silent passes.
- Candidate source: `7cea4545dddbee2da0b436e0684a9c489835e3cd`; exact APK identity and digests: `ARTIFACTS.md`. No source changes are authorized after this freeze without creating a new candidate and repeating its affected gates.
- No owner phone or JA11 hardware has been accessed. No physical claim is established. The requested phone readiness checkpoint waits for the owner's explicit acceptance of the exact emulator-gate substitution.
- Independent review confirmed the implementation flow and the shared JA11 operation gate for optional User 1 reads/fingerprint comparison; repository tests cover matched, changed and unavailable outcomes. Exact-head CI/PR review, owner exception, physical transaction evidence and publication gates remain tracked in `GATES.md` and `TEST_RESULTS.md`.
