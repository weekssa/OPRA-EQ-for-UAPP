# v0.8.3 JA11 defaults beta state

Status: **IMPLEMENTATION COMPLETE; SOFTWARE GATES PASS WITH ONE API 26 BROAD-SUITE EXCEPTION; CANDIDATE NOT FROZEN**

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

- Source and UI changes are uncommitted and still under review. The private beta uses a `beta` build type so standard production signing and CI output paths remain unchanged; the beta gets the requested app ID suffix, version label, update policy and local test signer.
- The shared Debug JVM suite passes with expanded defaults transaction and User 1 comparison coverage. Debug/Beta lint, Debug/Release/Beta packaging, R8, and Beta Android-test compilation pass on current source.
- The full 72-test Beta Android UI suite passes on API 35. The 9-test JA11 class passes on API 26; the broad API 26 run has two migration-fixture SQLite syntax failures and one unrelated Compose idle timeout. See `TEST_RESULTS.md`.
- Exact installed-package coexistence and final-candidate APK checks remain.
- No owner phone or JA11 hardware has been accessed. No physical claim is established.
- Independent review confirmed the UI and variant fixes. Optional User 1 reads and fingerprint comparison now run inside the shared JA11 operation gate; repository tests cover matched, changed and unavailable outcomes. Remaining broad API 26 suite exception, remote exact-head checks, exact artifact gates, physical EQ-bank evidence and final-candidate gates are tracked in `GATES.md` and `TEST_RESULTS.md`.
