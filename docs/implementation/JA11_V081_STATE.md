# JA11 v0.8.1 Mission State

As of 2026-10-09. This is the current concise state; older outcomes remain in the append-only validation ledger.

## Candidate and repository

- Worktree: `/Users/stephenweeks/.codex/worktrees/ja11-v0-8-1-fix/OPRA-EQ-for-UAPP`
- Branch: `codex/ja11-v0.8.1-fix`
- Production source SHA: `3d7bc1d91e6c39327477d1341bde80e8a37bfbd4`
- Exact PR #80 head currently on GitHub: `186c43b22490281ba2fbfceae52e7fa5d13b9c3b` (does not yet contain current source `3d7bc1d9`)
- Frozen diagnostic APK SHA-256: `3b74672a587daeaaf8f562634c5dcecea073f7ad6e01df736f874ee448fc6261`
- Package/version: `com.weekssa.opraeqforuapp.ja11diag`, `0.8.0-ja11diag` / version code `11`
- Signer certificate SHA-256: `73aa7581c8dc7dcc8ccea7586771119a98f9a74d7d8cf23716e09c557c9f6b41`
- Exact artifact and off-phone evidence: `/private/tmp/ja11-v0.8.1-acceptance-3d7bc1d91e6-corrected/CANDIDATE.md`

## Qualification

- Local production gates G2/G3 pass on source `3d7bc1d9`; exact diagnostic artifact provenance, isolated API 35 cold launch, and source-matched 64/64 instrumentation pass. G5 is valid for this source and APK.
- The production implementation follows approved Model D: serial optional, mismatch rejected when both serials exist, and multiple candidates fail closed. The JA11-only opened-connection serial fallback remains diagnostic evidence and is not an operation prerequisite.
- Acceptance helper and procedure follow Model D. G3a syntax/fixtures pass with output SHA-256 `944d79f424382f0f7979d2e85d610c46cbc3030adab5726b8635b7a9faed34be`; independent helper review is complete with no remaining actionable findings. The review's missing prior-open-generation and permission-denial fixture findings were corrected before final review.
- Late-Save/reconnect readback-only correction is implemented in parent `ad894129b5002fa33a2a46d47222772657a7a0e3`, inherited by source `3d7bc1d9`, and passed its local/emulator gates. It has not yet passed physical acceptance on this exact candidate.
- Current PR #80 head checks: Android CI, CodeQL, Catalog currentness CI, and Priority community coverage CI report success on `186c43b2`; exact candidate source `3d7bc1d9` is not on that head. The full required exact-head result is therefore pending.

## Physical evidence and restoration

- Latest completed physical session: source `92c11fb0`; one Flash accepted exactly one Save then detached 677 ms later during final band 4 readback. No mutation was replayed. A fresh complete readback matched the captured original baseline; Test C failed for that candidate and Test D was not run.
- Last verified hardware state at release: original baseline restored, including Mic On, UAC 2.0, User 1, volume 60, 384 kHz, gain -3.7 dB, and original bands. Current live state after release is unknown.
- No restoration is outstanding from the last completed session. Do not infer current state without a newly authorized read-only session.
- Physical qualification for source `3d7bc1d9` is pending. No physical command has been issued for this candidate.

## Current blocker and next action

- Blocker: push the coherent source/docs/helper candidate and obtain all required checks on the exact live PR head, then complete host-only candidate preflight.
- Phone needed now: **No.** Keep all work off-phone. After off-phone gates and exact candidate preflight complete, request a separate phone window and wait for owner confirmation.
- Next phone session, when authorized: install/verify the exact frozen candidate; run a read-only permission/session/cardinality/generation/full-baseline check with optional serial; if Mic is Off, restore Mic On as the first mutation with expected-reset handling and authoritative readback; otherwise skip that write. Capture evidence, restore original state, and release the Pixel.
