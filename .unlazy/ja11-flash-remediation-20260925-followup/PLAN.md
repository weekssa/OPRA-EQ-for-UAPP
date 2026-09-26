# Plan: JA11 Flash remediation follow-up

## Contract

- Read-only specialist work precedes any production edit.
- The primary agent owns synthesis, edits, tests, documentation, candidate provenance, and final verification.
- Preserve canonical EQ data, target derivation, the authoritative session, generation checks, fail-closed verification, and the physical-qualification boundary.
- Do not remove verification, add unexplained tolerance/offset/retry, copy readback into the target, or label an unproven protocol behavior supported.
- Do not merge, publish, release, or make a public hardware-support claim without explicit owner approval.

## Work tree

- Repository: `/Users/stephenweeks/Documents/Codex/OPRA-EQ-for-UAPP`
- Owner reports: `FiiO JA11 operation report (1)` and `FiiO JA11 operation report JSON (1)` under the owner’s Google Drive evidence directory.
- Exact repeated candidate source: `5b4b40bfccabae91e3839de9ff2f7b1edcb0d67a`

## Depth tree

1. Evidence and source baseline
   - Current repo/branch/HEAD/dirty state and maintained docs
   - Exact owner report hashes and value trace
2. Protocol adjudication
   - Current Android codec/transport/transaction
   - Cyfine, Ircama, and fiiocontrol-oss source/ref/license matrix
   - Alternative field/scale/quantization interpretations
3. Deterministic software proof
   - Fake transport exact mismatch reproduction
   - Golden vectors and fault injection
   - Root-cause decision and smallest justified correction
4. Candidate and handoff
   - Focused tests, complete applicable gates, signed provenance
   - Documentation synchronization
   - One bounded Pixel/JA11 physical session only if still required

## Stop conditions

- Stop code changes if the evidence does not prove a defect.
- Stop physical work on any mismatch, disconnect, permission loop, uncertain state, unexpected unrelated change, or missing final readback; do not retry automatically.
