# JA11 v0.8.1 Mission State

As of 2026-10-10. This snapshot applies to the clean implementation branch. Historical physical outcomes remain append-only in the validation ledger.

## Candidate and repository

- Repository: `weekssa/OPRA-EQ-for-UAPP`
- Worktree: `/Users/stephenweeks/.codex/worktrees/ja11-v081-minimal/OPRA-EQ-for-UAPP`
- Branch: `codex/ja11-v0.8.1-minimal`
- Base: `9b9a1f610025b3d6233d4ff6f0c2309a668577cc`, equal to refreshed `origin/main`.
- Frozen production and JA11 transaction source SHA: `9493cf030acb440f92e547fc667f6a5399616045`.
- Diagnostic report implementation commit: `fc409b7d038632e67dccdeccbd5144f3d3b53bdd` (`Add diagnostic JA11 Flash report access`); it adds only diagnostic application/activity/resources and a diagnostic-only test source set. No `app/src/main` file changed.
- Draft PR [#81](https://github.com/weekssa/OPRA-EQ-for-UAPP/pull/81) remains open and unmerged. Verify its live exact head and required checks before physical work.
- PR #80 remains open/draft/unmerged as historical evidence; it was not changed.

## Diagnostic-only report access

The new `ja11Diagnostic` build variant adds a separate **JA11 Flash Report** launcher activity. In the diagnostic process, an Application lifecycle observer reads the already-running `MainActivity` ViewModel's completed operation state and retains only the matching direct-`FLASH` trace. The report stays available in memory until explicitly dismissed; it does not survive process death. It provides the existing readable and technical JSON serializers through Android share intents. The phone procedure must return Home and open this separate launcher icon after Flash without force-stopping or clearing the app.

No transaction, Flash, Save, reconnect, identity, or readback code changed. There is no automatic Flash replay or automatic Save. The release variant does not include the diagnostic source set. Existing physical Mic, UAC, and power-removal evidence remains valid for its recorded production source and prior artifact; this continuation did not touch a phone or JA11.

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
- Before any phone use, verify all required CI checks pass for the live exact PR head and obtain a new owner-confirmed phone window.
- Production JA11 focused regressions, full JVM, release R8, helper fixtures, and prior production API 35 instrumentation remain passed on the unchanged production source; none was rerun because no relevant source changed.

## Physical evidence and current hardware truth

At the last verified J028 readback before phone release: Mic On, UAC 2.0, active program User 1, volume 60/60, sample rate 384 kHz, firmware 2.20, VID/PID `0x2972:0x0102`, gain -3.70 dB, and the full User 1 bank recorded in J028. The temporary EQ was removed and My EQs returned to the empty pre-test state. No serial/fingerprint was retained; same-unit continuity is not claimed. No live state has been checked since release.

J028: Mic was already On, so no Mic write occurred; UAC 2.0→1.0→2.0 passed with fresh readbacks. One Flash from Off produced a fresh User 1 readback matching baseline, but its transaction report was not captured and Save count remains unknown. Test D observed volume 60 and User 1 after power removal/reconnection and restoration matched the captured baseline. This is one observation, not a general persistence claim. Test C remains incomplete.

The diagnostic APK above has not been installed on the Pixel or used with the JA11. The J028 phone window is closed. Do not repeat Mic, UAC, power-cycle, or Flash tests except the single future Test C Flash explicitly planned below.

## Restoration obligations

- J028 restoration was complete and the Pixel was released; no J028 restoration is outstanding.
- In the next Test C session, if the fresh Mic state is Off, restore Mic to On first using expected-reset handling, then freshly verify Mic and the unrelated DEVICE/EQ baseline. Mic On is the required final state and must not be reverted to Off. If Mic is already On, make no Mic write.
- Restore the original active program only if the Test C setup changed it; keep all other unchanged baseline fields intact; remove only the temporary test profile; release the Pixel immediately after sufficient evidence is captured.

## Remaining release-critical work

- Verify all required CI checks on the live exact PR head before physical work.
- One new owner-confirmed, Test-C-only physical session to capture both reports and verify counts/order, reconnect generation, and final authoritative readback. No UAC, Mic round-trip, volume, or power-cycle test.
- Hardware qualification, merge/publication, and public support remain separate gates.

## Exact next action

Verify the live exact PR head and all required CI checks, refresh the candidate tuple, and only then request a new Test C phone window. Follow `JA11_V081_COMPACT_HANDOFF.md` and `ja11-v0.8.1-acceptance/PHONE-PLAN.md`. Do not touch Pixel/JA11 before those gates are ready.
