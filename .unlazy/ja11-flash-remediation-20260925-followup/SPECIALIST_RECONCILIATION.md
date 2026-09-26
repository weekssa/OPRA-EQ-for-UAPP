# JA11 specialist reconciliation

Date: 2026-09-25

All six bounded passes were read and reconciled by the primary agent. No specialist modified
production code, tests, documentation, branches, commits, hardware, or external state.

| Specialist | Evidence-backed conclusion | Evidence class | Unresolved point |
| --- | --- | --- | --- |
| `repo_state` | Latest report pair is valid; source is `5b4b40bf`; current production source is unchanged; local Android toolchain/wrapper is unavailable. | Repository, artifact, physical-report | Its snapshot predates the primary branch switch and should not be used for current branch identity. |
| `architecture` | Canonical EQ → shared optimizer → JA11 codec → one session → Apply → volatile verification → Save/reconnect → final readback is structurally coherent; no shared-abstraction defect explains J012. | Repository source and architecture docs | Preview/transaction optimizer duplication and command-only stale-response correlation remain regression surfaces, not proven causes. |
| `researcher` | Cyfine, Ircama, and adithyasource independently corroborate VID/PID, five bands, `0x17` signed little-endian `2560` scale, and Save framing; no source documents `D900 → D9FF` or proves PEQ persistence. | External source and license evidence | Oracle disagreements remain around Apply, Save completeness, response matching, and packet documentation; no alternate semantics are justified. |
| `failure_analysis` | J012 repeats the exact stable-session mismatch on firmware 2.20: `D900` write, `D9FF` readback, five bands pass, no Save. Device/firmware transformation is strongest hypothesis; stale response and framing remain plausible. | Exact physical report, deterministic arithmetic, repository source | Device-side transformation versus delayed same-command response is not distinguished. |
| `dac_transaction` | Current target/wire comparison and fail-closed Save gate are correct for the maintained protocol contract; exact `D9FF` fake fixture is the smallest safe regression. | Repository source and simulated fixture | No physical persistence or restoration evidence; operation-level and response-freshness fault cases remain future test candidates. |
| `reviewer` | Hold. Do not alter codec, tolerance, offset, retry, Save gating, or public support status. Synchronize ledger/checklist/status/release documents append-only. | Independent review of source, reports, docs, and safety boundary | Restoration is not proven; report provenance is joined to ledger rather than self-contained APK hash/signer fields. |

## Primary reconciliation

Confirmed across the reports:

- canonical, selected, and quantized target gain: `-3.9 dB`;
- write: command `0x17`, raw `0xD900`, bytes `00 D9`;
- readback: command `0x17`, raw `0xD9FF`, bytes `FF D9`, decoded `-3.800390625 dB`;
- delta: `255` raw units / `0.099609375 dB`, beyond `0.001 dB` tolerance;
- all five target bands match;
- session/detach generations `1/0`, permission requests `0`, Save count `0`;
- outcome `VerificationFailed` before persistence.

The only material specialist disagreement is stale repository-state context from the read-only
`repo_state` snapshot: it reports the prior branch name because it completed before the primary
agent switched to `codex/ja11-protocol-evidence`. The primary `git status --short --branch` is the
authoritative current state.

Disposition: no code defect is proven; the production protocol remains unchanged; the exact
observed mismatch is covered by new deterministic tests and the physical candidate is consumed.
