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
- J029 physical evidence is tied to PR #81 head `78deeddd2cce65c22a98fce7e288a608ceb813fd`; all eight required checks passed on that head before the run. Diagnostic code commit: `fc409b7d038632e67dccdeccbd5144f3d3b53bdd` (`Add diagnostic JA11 Flash report access`). The subsequent closeout commit is documentation-only and leaves the tested executable candidate unchanged.
- PR #80 remains open/draft/unmerged as historical evidence and is unchanged.

## Exact production SHA and APK SHA

- Frozen production/JA11 transaction source SHA: `9493cf030acb440f92e547fc667f6a5399616045`; no `app/src/main` file changed for report access.
- Diagnostic APK: `app/build/outputs/apk/ja11Diagnostic/app-ja11Diagnostic.apk`.
- APK SHA-256: `44040493c7263ae7d7e6f41be7389534d07977427cc8002494097fbf3c868848`.
- Package: `com.weekssa.opraeqforuapp.ja11diag`; version/code: `0.8.1-ja11diag` / `12`.
- Signer certificate SHA-256: `73aa7581c8dc7dcc8ccea7586771119a98f9a74d7d8cf23716e09c557c9f6b41`.
- Diagnostic implementation adds a separate **JA11 Flash Report** launcher entry, retains the existing completed direct-`FLASH` trace in process memory until explicit dismissal, and exposes readable and technical JSON shares. It does not survive process death. No transaction, Flash, Save, reconnect, identity, or readback behavior changed; no automatic replay or Save was added.

## Current hardware truth

- J029 tested the exact diagnostic APK listed above with production/transaction source `9493cf030acb440f92e547fc667f6a5399616045`. Initial read-only state: Mic On, UAC 2.0, User 1, volume 60/60, sample rate 384 kHz, firmware 2.20, VID/PID `0x2972:0x0102`, gain -3.70 dB, and the five-band User 1 bank recorded in the validation ledger. One direct Flash from Off selected and verified User 1 before EQ writes, wrote the target once, applied once, saved exactly once, observed detach/reconnect, and completed authoritative final readback matching all six target values.
- The report records Save on session generation 1/detach generation 0 and final readback on generation 2/detach generation 1. Its serial is redacted; the descriptor token matched a generic JA11 value. Model D permits the sole supported candidate plus fresh-generation readback; do not claim same-unit continuity.
- J029 operation ID: `ae6d16fe-5b78-4c9a-bbc6-a08ec9bf2740`. Private readable report SHA-256 `da2137a798ede7cb420c459e48507a1bdf16be656dcebb91ceba601db7d42f77`; JSON report SHA-256 `a4de38b5265077562177aa2d0decbf2871832e14f13eded55a0b26794452f228`; evidence directory `/Users/stephenweeks/.codex/evidence/ja11-v081-test-c-20261010T184850Z/`.
- A post-save Refresh showed Mic On, UAC 2.0, User 1, volume 60/60, 384 kHz, firmware 2.20, and VID/PID `0x2972:0x0102`. The temporary My EQs profile and exact temporary Downloads target were removed; the pre-existing similarly named download was untouched. My EQs returned to empty. No JA11 restoration remains. The diagnostic APK remains installed, and the Pixel has been released.

## Physical tests already passed on the current production source

- J028: Mic was already On, so no Mic write occurred; UAC 2.0→1.0→2.0 passed; Test D observed volume 60 and User 1 after one owner power removal/reconnect; baseline restoration passed. These remain tied to J028's recorded source/APK.
- J029: Test C passed on the exact diagnostic APK and unchanged production source. The paired readable/JSON reports and private verification record prove User 1 select/read before writes; five band writes, one gain write, one Apply, one Save; no replay; fresh generation after detach; and final authoritative readback equal to the intended User 1 target.
- J029 does not prove general PEQ retention after true power loss or same-unit serial continuity. It also does not add Save behavior to runtime volume or built-in program controls.

## Unresolved defects

- No JA11 transaction defect was established. The J028 Save-count gap is historical; J029 closes the Test C evidence gap for its exact candidate tuple.
- General PEQ power-cycle persistence, same-unit continuity without a usable serial match, and public JA11 support remain unproven/out of scope for this evidence.

## Restoration obligations

- J028 restoration was complete.
- J029 final program is User 1 and the final five-band/global-gain readback matches the original User 1 state. Mic remained On; UAC 2.0 and volume 60/60 were unchanged; sample rate remained 384 kHz. The temporary profile and its unique source file were removed. No restoration is outstanding.
- The Pixel is released. Do not repeat physical tests without a concrete invalidation and a new owner-confirmed window.

## Remaining release-critical tests

- Test C is complete for the recorded J029 tuple. No Mic, UAC, volume, power-cycle, or Flash rerun is required.
- Keep merge, publication, general persistence claims, and public support as separate owner-controlled gates.

## Gate invalidation rules

- Diagnostic source/resource or diagnostic test changes invalidate diagnostic lint/build/test/artifact provenance, affected independent review, exact-head CI, and the candidate tuple.
- Production source changes invalidate the applicable focused regressions, full JVM, lint, debug/release build, R8, emulator, independent review, CI, and artifact provenance.
- Helper-only changes invalidate helper fixtures/review; test-only changes invalidate the affected test; documentation-only changes do not alter executable candidate identity or completed Android validation.
- This diagnostic-only change leaves production JA11 gates and prior physical evidence valid for the exact source/artifact on which each was observed. Preserve incomplete/failed results; never relabel them as passes.

## Exact next action

No further phone or JA11 action. Next, review PR #81 and decide whether to approve merge; do not merge or publish without explicit owner approval. Do not replay Flash to reconstruct J028 evidence or repeat J029 without a concrete invalidation.
