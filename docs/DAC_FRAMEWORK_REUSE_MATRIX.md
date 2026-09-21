# DAC framework reuse matrix — EW300 v0.7

| Concern | Shared/generalized path | EW300-specific addition | Evidence / remaining gate |
| --- | --- | --- | --- |
| Product registry and active-output ownership | Existing output registry, source-neutral canonical EQ, My EQs ownership | `SIMGOT_EW300` registration and exact capability profile | Existing v0.6 tests; no duplicate library. |
| Session ownership | ViewModel-scoped `DacSessionRepository`, per-DAC operation mutex, reconnect policy | `Ew300ReconnectGate` shared by transport and reconnect observer | New gate unit tests; CI and one physical session remain. |
| USB transport | Existing HID session: permission, attach/detach, generation, fingerprint, idempotent open | VID/PID, strings, interface 3, report codec | Protocol notes and exact identity tests. |
| EQ optimization | Shared finite five-band optimizer and canonical conversions | EW300 direct-Hz Peak bounds | Existing optimizer/protocol tests. |
| Mutation safety | Shared strict baseline/restore and fail-closed result types | EW300 one-Save commit and pre-Save volatile readback | Current code and flasher tests; hardware edit and exact restoration observed, but candidate Apply telemetry is incomplete. |
| Re-enumeration | Shared generation/detach observation | Save releases replacement reconnect only after accepted commit | New gate plus physical observation; persisted readback and exact restoration passed, while candidate operation trace is incomplete. |
| UI shell | Shared My DAC, editor, readback, feedback and report affordances | Peak-only capability wording and EW300 operation-report sharing | Emulator/accessibility gate required. |
| DEVICE controls | Capability-driven surface | No guessed Black Pearl controls; report only | Matrix above; no extra hardware session. |
| Release provenance | Shared signed workflow, pinned signer, package/version checks | Candidate manifest includes EW300 profile and evidence IDs | Full signed workflow on frozen head. |

No Black Pearl command bytes, filter meanings, limits, persistence semantics, reset semantics, or
DEVICE controls are reused as EW300 behavior.
