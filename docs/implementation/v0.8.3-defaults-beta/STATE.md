# v0.8.3 JA11 defaults beta state

Status: **BETA V1 OWNER ACCEPTED; v0.8.3 STABLE PUBLISHED; RELEASE CLOSEOUT COMPLETE**

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
- The owner accepted the exact bounded emulator-gate substitution on 2026-10-14. On the authorized Pixel 9 (API 37), the frozen Beta v1 APK (SHA-256 `ac982eafd76f4db80f48e0f26040bf1f849388c4f0a51f778b51c8c013c59ce5`) installed and cold-launched successfully as package `com.weekssa.opraeqforuapp.v083beta`, version `0.8.3-beta.1`, code 14. Stable v0.8.2 and the earlier `com.weekssa.opraeqforuapp.ja11beta` package remain installed. UAPP was force-stopped to avoid USB contention; its app data was not changed. The connected JA11 baseline and three successful defaults transactions are detailed in `TEST_RESULTS.md` and J032. Mic restoration succeeded after each, and target/companion state readbacks matched. App-reported write counts were 1/0/2, 0/0/2 and 0/1/2 (volume/program/mic). After the owner-confirmed physical power removal, the fresh readback remained 19 / Classic; mic On, UAC 2.0, firmware 2.20, VID:PID 2972:0102. Current sample rate is 384 kHz versus initial 48 kHz. User 1 EQ comparison was unavailable; no bank-preservation claim is supported. The exact Beta v1 and data remain installed for owner use.
- Independent review confirmed the implementation flow and the shared JA11 operation gate for optional User 1 reads/fingerprint comparison; repository tests cover matched, changed and unavailable outcomes. PR [#97](https://github.com/weekssa/OPRA-EQ-for-UAPP/pull/97) merged normally as `899755507f812c940f3ab9381a856c80c6030885`. Exact-main Android CI run `38648012952` passed all four jobs. Its product source matches the frozen beta implementation; no app source changed after physical qualification.

## Owner acceptance and release authority (2026-10-14)

The owner reported: “My testing confirmed success on ‘User 1 EQ-bank preservation or unique same-unit continuity. The app’s operation counts are not USB bus captures. The original mic state was restored; no native EQ Save, UAC change, or EQ-band write was used.’ Additionally, all other manual tests passed.” This is recorded as owner-reported acceptance for the identified Beta v1; no new standalone report, USB bus capture, serial identity, or independent EQ-bank payload was supplied. The previously documented User 1 comparison limitation and no-unique-unit-identity boundary remain in force; this acceptance does not support a public EQ-bank-preservation or same-unit-continuity claim.

The owner explicitly authorized autonomous final go-live work. The frozen Beta v1 application source remains `7cea4545dddbee2da0b436e0684a9c489835e3cd`; pre-merge changes were documentation-only. PR #97 was the integration path and the exact release chain is complete below. Keep the exact beta/data installed on the owner's phone; no substitute build or additional hardware mutation was used. Final phone state per J032 is volume 19 / Classic, Mic On, UAC 2.0.

## v0.8.3 release closeout (2026-10-14)

Owner manual acceptance and publication authorization are recorded in J033. The exact official candidate was signed from main `899755507f812c940f3ab9381a856c80c6030885`, independently checked, passed hosted API 35 clean-install and v0.8.2 in-place upgrade gates, and was published without rebuild or re-sign. Public release ID `413252769`, annotated tag `v0.8.3`, and all six asset digests are in `docs/release-engineering/v0.8.3-stable/ARTIFACTS.md`. The stable APK is `com.weekssa.opraeqforuapp` `0.8.3` / code 14, SHA-256 `396244ffc26cec108b64c523a49779261656b41ab02f1bc2b63197327c576247`, pinned signer `65c1c1256dae3c49e3548f334c91f0ba991969e9be9e0b223ba4e253d2114747`. The owner's phone still has Beta v1 and its data installed; reported final JA11 state remains 19 / Classic, Mic On, UAC 2.0. No User 1 preservation, bus-level counter, or unique same-unit claim is added.

Documentation closeout is being merged in PR #98; the full append-only release history is recorded in the current runbook and J034 of `docs/FIIO_JA11_VALIDATION_LEDGER.md`.
