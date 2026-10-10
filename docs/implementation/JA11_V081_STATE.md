# JA11 v0.8.1 Mission State

As of 2026-10-10. This snapshot applies to the clean implementation branch. Historical physical outcomes remain append-only in the validation ledger.

## Candidate and repository

- Repository: `weekssa/OPRA-EQ-for-UAPP`
- Worktree: `/Users/stephenweeks/.codex/worktrees/ja11-v081-minimal/OPRA-EQ-for-UAPP`
- Branch: `codex/ja11-v0.8.1-minimal`
- Branch base at creation: `9b9a1f610025b3d6233d4ff6f0c2309a668577cc`.
- Frozen production and JA11 transaction source SHA: `9493cf030acb440f92e547fc667f6a5399616045`.
- Diagnostic report implementation commit: `fc409b7d038632e67dccdeccbd5144f3d3b53bdd` (`Add diagnostic JA11 Flash report access`); it adds only diagnostic application/activity/resources and a diagnostic-only test source set. No `app/src/main` file changed.
- J029 physical evidence is tied to PR [#81](https://github.com/weekssa/OPRA-EQ-for-UAPP/pull/81) head `78deeddd2cce65c22a98fce7e288a608ceb813fd`; all eight required checks passed on that exact head before the run. The subsequent closeout commit is documentation-only and does not alter the tested executable candidate.
- PR #80 remains open/draft/unmerged as historical evidence; it was not changed.

## Diagnostic-only report access

The new `ja11Diagnostic` build variant adds a separate **JA11 Flash Report** launcher activity. In the diagnostic process, an Application lifecycle observer reads the already-running `MainActivity` ViewModel's completed operation state and retains only the matching direct-`FLASH` trace. The report stays available in memory until explicitly dismissed; it does not survive process death. It provides the existing readable and technical JSON serializers through Android share intents. The phone procedure must return Home and open this separate launcher icon after Flash without force-stopping or clearing the app.

No transaction, Flash, Save, reconnect, identity, or readback code changed. There is no automatic Flash replay or automatic Save. The release variant does not include the diagnostic source set. J029 physically exercised the diagnostic report path while retaining production/transaction source `9493cf030acb440f92e547fc667f6a5399616045` unchanged.

## Exact candidate tuple

- Production/transaction source SHA embedded in the APK: `9493cf030acb440f92e547fc667f6a5399616045`.
- Diagnostic implementation commit: `fc409b7d038632e67dccdeccbd5144f3d3b53bdd`; documentation is a follow-up only.
- APK: `app/build/outputs/apk/ja11Diagnostic/app-ja11Diagnostic.apk`.
- APK SHA-256: `44040493c7263ae7d7e6f41be7389534d07977427cc8002494097fbf3c868848`.
- Package: `com.weekssa.opraeqforuapp.ja11diag`; version/code: `0.8.1-ja11diag` / `12`.
- Signer certificate SHA-256: `73aa7581c8dc7dcc8ccea7586771119a98f9a74d7d8cf23716e09c557c9f6b41`.

## Off-phone gates and invalidation

- Diagnostic-only instrumentation: **PASS**, 4/4 on disposable API 35 emulator. XML SHA-256: `d314ae93051f52cd9a398c6734209f830916607c74ad8d8977b537528e53ebaf`.
- `lintJa11Diagnostic`, `assembleJa11Diagnostic`, APK checksum, package/version, source metadata, signer, alignment, install, and report-activity checks: **PASS**.
- Independent review: **PASS**. The reviewer confirmed the corrected Test C procedure requires a fresh replacement session and full unrelated DEVICE/EQ baseline after Mic reconnect, and never reverts Mic On to Off.
- Physical-candidate exact-head CI: **PASS**, all eight required checks passed on PR #81 head `78deeddd2cce65c22a98fce7e288a608ceb813fd` before the J029 physical run. No executable changes followed. The documentation-only closeout does not invalidate the tested artifact or production gates; its exact-head PR checks are separately required for merge.
- Production JA11 focused regressions, full JVM, release R8, helper fixtures, and prior production API 35 instrumentation remain passed on the unchanged production source; none was rerun because no relevant source changed.

## Physical evidence and current hardware truth

J029 used the exact diagnostic APK above and unchanged production/transaction source `9493cf030acb440f92e547fc667f6a5399616045`. The report proves one User 1 select/read before writes, five band writes, one gain write, one Apply, one Save, no replay, one detach/reconnect generation change, and successful final authoritative readback of the complete intended User 1 state. Outcome is Success, `stateKnown=true`, and `comparisonPhase=FINAL_READBACK` with `VERIFIED`.

The fresh post-save read showed Mic On, UAC 2.0, active program User 1, volume 60/60, sample rate 384 kHz, firmware 2.20, VID/PID `0x2972:0x0102`, global gain -3.70 dB, and the original five-band bank. The descriptor serial value was generic/unusable; only state on the sole returning JA11 is claimed, not same-unit continuity. The temporary profile/source file were removed; My EQs returned to empty; Pixel was released. No restoration remains. J029 does not prove general power-cycle PEQ persistence.

J028 remains a separate earlier record: Mic was already On, UAC 2.0→1.0→2.0 passed, and Test D observed volume 60/User 1 after one power removal. Its Flash report remains missing and Save count unknown; do not relabel J028 or repeat its Flash.

## Restoration obligations

- J028 restoration was complete.
- J029 returned the active program to User 1 through its one Flash, verified the complete final EQ bank/gain, left Mic On, UAC 2.0, volume 60/60, and 384 kHz unchanged, removed only its temporary My EQs profile and exact Downloads file, and released the Pixel. No restoration remains.

## Remaining release-critical work

- Test C evidence is complete for J029. No additional phone test is needed for this evidence item.
- Merge/publication, any broader hardware qualification, general persistence claims, and public support remain separate owner-controlled gates.

## Exact next action

Continue from `JA11_V081_COMPACT_HANDOFF.md`. Do not use the Pixel or JA11 again for Test C without a concrete invalidation and a new owner-confirmed window. The current phone session is released; remaining work is documentation/PR governance and owner-controlled merge/publication decisions.
