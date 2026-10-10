# JA11 v0.8.1 Compact Handoff

## Approved non-negotiable design rules

- Authorize only an exactly supported FiiO JA11 after unique-candidate, permission, current-session/generation, and authoritative-readback checks. VID/PID alone is never identity.
- Model D permits an absent serial. Compare usable serials when both sessions expose them and reject mismatch. Without a match, claim state verified on the sole returning JA11, not same-unit continuity.
- Expected resets require old-session invalidation, a fresh claimed replacement, and authoritative readback. Never replay an uncertain write.
- Flash selects and verifies User 1 before EQ data writes, issues one Save, and requires fresh authoritative final readback. Missing Save evidence is not a pass.
- Official FiiO app observations are behavioral reference evidence only: custom EQ apply and Mic reset/re-enumerate; built-in program and volume do not and show no Save-style behavior. Classify custom EQ as persistent commit/reset, Mic as expected-reset, program and volume as runtime controls unless contrary protocol evidence appears. Do not add Save behavior to program or volume. Their return to defaults after true power loss may be intended firmware behavior.
- Preserve shared DAC defaults, exact hardware capability boundaries, privacy, and fail-closed errors. Do not claim hardware support or persistence beyond recorded physical evidence.

## Branch and PR

- Repository: `weekssa/OPRA-EQ-for-UAPP`; branch: `codex/ja11-v0.8.1-minimal`.
- PR #81 is open/draft/unmerged. Diagnostic code commit: `fc409b7d038632e67dccdeccbd5144f3d3b53bdd` (`Add diagnostic JA11 Flash report access`). Verify the live exact PR head and required-check state before physical work.
- PR #80 remains open/draft/unmerged as historical evidence and is unchanged.

## Exact production SHA and APK SHA

- Frozen production/JA11 transaction source SHA: `9493cf030acb440f92e547fc667f6a5399616045`; no `app/src/main` file changed for report access.
- Diagnostic APK: `app/build/outputs/apk/ja11Diagnostic/app-ja11Diagnostic.apk`.
- APK SHA-256: `44040493c7263ae7d7e6f41be7389534d07977427cc8002494097fbf3c868848`.
- Package: `com.weekssa.opraeqforuapp.ja11diag`; version/code: `0.8.1-ja11diag` / `12`.
- Signer certificate SHA-256: `73aa7581c8dc7dcc8ccea7586771119a98f9a74d7d8cf23716e09c557c9f6b41`.
- Diagnostic implementation adds a separate **JA11 Flash Report** launcher entry, retains the existing completed direct-`FLASH` trace in process memory until explicit dismissal, and exposes readable and technical JSON shares. It does not survive process death. No transaction, Flash, Save, reconnect, identity, or readback behavior changed; no automatic replay or Save was added.

## Current hardware truth

- Last verified before J028 release: Mic On, UAC 2.0, User 1, volume 60/60, 384 kHz, firmware 2.20, VID/PID `0x2972:0x0102`, gain -3.70 dB, and the full User 1 band bank recorded in the ledger. Temporary EQ removed; My EQs returned to its empty pre-test state. No serial/fingerprint retained; no same-unit claim. Current live state has not been checked.
- The 2026-10-10 diagnostic APK has not been installed on Pixel or used with JA11. The J028 phone window is closed.

## Physical tests already passed on the current production source

- Mic was already On during J028, so no Mic write occurred.
- UAC 2.0→1.0→2.0 passed with fresh readbacks.
- Test D observed volume 60 and User 1 after one owner power removal/reconnect; full baseline restoration passed. This does not prove general PEQ persistence.
- One Flash from Off produced a fresh User 1 readback matching baseline. J028 did not capture the transaction report, so Save count is unknown and Test C remains incomplete. These outcomes remain tied to J028's recorded production source and previous APK; the new diagnostic UI does not invalidate them.

## Unresolved defects

- No JA11 transaction defect was established. The open item is evidence access: J028 lacks the report needed to verify exact Save count and event order.
- Do not repeat Flash to reconstruct J028 evidence. Test C needs one new, gated physical capture.

## Restoration obligations

- J028 baseline was restored, temporary profile removed, and Pixel released; no J028 restoration is outstanding.
- In Test C, if fresh Mic readback is Off, restore Mic On first and reread Mic plus the full unrelated DEVICE/EQ baseline. Mic On is the required final target and must not be reverted Off. If Mic is already On, make no Mic write.
- Restore the original active program only if changed for Test C; preserve the verified User 1 target, remove the temporary profile, and release Pixel after sufficient evidence.

## Remaining release-critical tests

- Verify all required CI checks on the live exact PR head before physical work.
- Complete one new owner-confirmed Test-C-only phone session. Do not repeat UAC, Mic round-trip, volume, or power-cycle tests.
- Keep merge, publication, general persistence claims, and public support as separate owner-controlled gates.

## Gate invalidation rules

- Diagnostic source/resource or diagnostic test changes invalidate diagnostic lint/build/test/artifact provenance, affected independent review, exact-head CI, and the candidate tuple.
- Production source changes invalidate the applicable focused regressions, full JVM, lint, debug/release build, R8, emulator, independent review, CI, and artifact provenance.
- Helper-only changes invalidate helper fixtures/review; test-only changes invalidate the affected test; documentation-only changes do not alter executable candidate identity or completed Android validation.
- This diagnostic-only change leaves production JA11 gates and prior physical evidence valid for the exact source/artifact on which each was observed. Preserve incomplete/failed results; never relabel them as passes.

## Exact next action

Verify the live PR #81 head and all required CI checks, then refresh the candidate tuple. Only after those gates pass, request a new owner-confirmed phone window and follow the seven-step Test C procedure in `ja11-v0.8.1-acceptance/PHONE-PLAN.md`: verify the exact candidate, capture a fresh read-only baseline and exact User 1 target, make only required Off/Mic restoration changes, perform one direct Flash, immediately export both reports, verify selection/write/Apply/Save counts and reconnect/final readback evidence, restore only authorized changes, then release Pixel. No phone use before those gates pass.
