# JA11 digital-filter implementation readiness

Status: audit only. No API, production source, build flag, test fixture or save sequence was
changed. No filter-write payload or transaction is prepared.

**Verdict: not ready to implement or enable.** The capability gate fails at the device layer: no
validated current-firmware read establishes the control, the vendor record for the tested tuple
declares none, and the decoder cannot yet prove a complete device frame. The dormant code is
structurally reusable, but it is not qualified for delivery.

Classifications in this document are **INSPECTION-ONLY** unless stated otherwise. The build flag
state is **CONFIGURED**. Nothing here is device evidence.

## Dormant implementation inventory

| Area | Location | Current behavior | Classification |
| --- | --- | --- | --- |
| Command constant | `domain/kt02h20/FiioJa11Protocol.kt:49` | `CMD_DIGITAL_FILTER_MODE = 0x04` | INSPECTION-ONLY |
| Mode enum | `FiioJa11Protocol.kt:95` | `DigitalFilterMode` FAST_LL 0, FAST_PC 1, SLOW_LL 2, SLOW_PC 3, NON_OS 4 | INSPECTION-ONLY |
| Read frame | `FiioJa11Protocol.kt:136` | `readDigitalFilterModeReport()` | INSPECTION-ONLY |
| Write frame | `FiioJa11Protocol.kt:138` | `writeDigitalFilterModeReport(mode)` | INSPECTION-ONLY |
| Decoder | `FiioJa11Protocol.kt:234` | `digitalFilterModeFromResponse` accepts value 0..4 only | INSPECTION-ONLY |
| Control id | `domain/fiio/FiioJa11DeviceControls.kt:48` | `fiio_ja11.digital_filter` | INSPECTION-ONLY |
| Descriptor | `FiioJa11DeviceControls.kt:74` | Discrete, ADVANCED section, five options | INSPECTION-ONLY |
| Value mapping | `FiioJa11DeviceControls.kt:120` | Snapshot null maps to no value | INSPECTION-ONLY |
| Write-id set | `FiioJa11DeviceControls.kt:100` | Includes DIGITAL_FILTER | INSPECTION-ONLY |
| Production qualification | `FiioJa11DeviceControls.kt:108` | `productionQualifiedWriteControlIds` is empty | INSPECTION-ONLY |
| Flag default | `data/dac/FiioJa11ControlRepository.kt:211` | `filterControlEnabled = false` | INSPECTION-ONLY |
| Write gate | `FiioJa11ControlRepository.kt:798` | Rejects DIGITAL_FILTER while the flag is off | INSPECTION-ONLY |
| Snapshot probe | `FiioJa11ControlRepository.kt:985` | Reads `0x04` only when the flag is on | INSPECTION-ONLY |
| Null-baseline guard | `FiioJa11ControlRepository.kt:838` | Refuses a filter write without a validated baseline | INSPECTION-ONLY |
| Write dispatch | `FiioJa11ControlRepository.kt:1020` | Maps a discrete value to the write path | INSPECTION-ONLY |
| Probe transport | `data/kt02h20/AndroidFiioJa11UsbTransport.kt:99` | One probe per USB session; caches null | INSPECTION-ONLY |
| Probe cache | `data/kt02h20/FiioJa11FilterProbeCache.kt` | Session-scoped, null cached, invalidated on write | INSPECTION-ONLY |
| Probe diagnostics | `data/kt02h20/Ja11FilterProbeDiagnostics.kt` | One-line request/response/session record | INSPECTION-ONLY |
| Flag wiring | `EqLibraryDependencyFactory.kt:118` | Passes `BuildConfig.JA11_FILTER_CONTROL_ENABLED` | INSPECTION-ONLY |
| Build flag | `app/build.gradle.kts:46` | `JA11_FILTER_CONTROL_ENABLED = false` | **CONFIGURED false** |

The dormant path is coherent: the repository refuses writes while the flag is off, refuses a
filter write when the snapshot has no validated filter value, and caches a null probe so a silent
unit does not pay a timeout on every refresh. Those are safety properties, not capability
evidence.

## UI gating

The JA11 DEVICE row, its selector dialog and the UI callback chain are absent from the shipped
screen. `app/src/androidTest/java/com/weekssa/opraeqforuapp/ui/screens/FiioJa11MyDacContentTest.kt:170`
asserts that no "Digital filter" text appears for the deferred configuration. That test covers the
current bespoke screen; it does not prove a future generic renderer would hide the control.

Latent exposure: `DacCapabilityCatalog` exposes `FiioJa11DeviceControls.descriptors`, which still
contains DIGITAL_FILTER. Today the row is hidden because the screen does not render that
descriptor. A future UI change that renders catalog descriptors generically would expose a
control the device does not report. Re-enabling must add an explicit visibility guard tied to the
flag and to a validated snapshot value, not rely on row omission.

## Decoder weakness

`packetView(report)` (`FiioJa11Protocol.kt:310`) accepts several leading-byte variants and returns a
view starting at the `BB 0B` marker. `oneByteValueFromResponse(report, expectedCommand)`
(`FiioJa11Protocol.kt:292`) then requires:

- at least 7 bytes;
- the expected command at index 4;
- a value byte at index 6.

It does **not** validate the declared length byte, the `0xEE` footer, any checksum or sequence
semantics, the report ID, or that the response length matches the command's expected shape. A
reply with a plausible command and value but a malformed remainder would be accepted. The
`0x04` decoder inherits this weakness, so a non-null decode cannot prove a valid device reply.

Any future implementation must validate the complete frame against documented rules before
treating a reply as capability evidence. Checksum rules must come from a documented source; they
must not be invented to fit one observed packet.

## Missing exact-device evidence

- No captured valid `0x04` response from the JA11 exists; the protocol tests use synthetic
  reference frames (`FiioJa11ProtocolTest.kt:217`, `:231`).
- No exact-device fixture exists for a rejected or malformed `0x04` reply; only generic
  wrong-command/out-of-range cases are asserted.
- J034/J035 recorded application-level outcomes, not raw request/response bytes or transfer
  metadata for the filter probe.
- No USB bus capture exists for the JA11 filter path, and no capture exists for any sibling.
- No stored-state read path exists for the JA11's startup program/volume, so persistence must be
  verified by power removal, not inferred from a runtime read.

## Documentation drift to resolve outside this task

These are outside OWNS and were not edited:

- `AndroidFiioJa11UsbTransport.kt:95` still describes the probe as `0x11`; the implementation
  uses `0x04`. The code is correct; the comment is stale.
- The v0.9.0-beta.2 section of `docs/FIIO_JA11_CAPABILITY_MATRIX.md` says the row is enabled when a
  parsed `0x11` reply returns 1..5. That wording predates the corrected vendor command and the
  deferral; later sections of the same matrix supersede it, but the older paragraph remains
  misleading.
- The capability matrix's per-mode table still lists the beta.2 candidate dispositions as
  "Physical read pending". Those reads are now closed with null results.

A future authorized documentation task should reconcile these against the maintained record.

## Future test scenarios

Proposed for a separately approved implementation, **NOT EXERCISED** here:

- valid frame for each of values 0..4;
- truncated frame;
- wrong command identity;
- wrong report ID or prefix variant;
- invalid declared length;
- invalid or missing footer;
- invalid checksum where a documented checksum exists;
- value outside the evidenced set;
- stale session generation;
- cached negative probe;
- disabled-path write rejection;
- readback mismatch after write;
- disconnect during write or readback;
- UI unavailable, loading, stale, failure, recovery and success states.

Use targeted JVM tests first, then the applicable wrapper unit/lint/build and existing UI/release
checks on a coherent candidate. Existing tests do not establish device support.

## Readback, lifecycle and persistence requirements

Any future filter capability work must include:

- immediate same-session readback after a write, with exact value comparison;
- no success claim from the outgoing packet alone;
- explicit handling of the Save restart boundary, including invalidation of the old session and a
  fresh identity/permission check after any re-enumeration;
- no replay of a write after an ambiguous disconnect or timeout;
- truthful persistence semantics: a runtime write is session-only until a qualified save and power
  removal prove otherwise;
- cache invalidation on write, disconnect and session change;
- restoration of the original filter value, verified after power removal, alongside the existing
  program, volume, User 1 bank, microphone and UAC restoration requirements;
- a real-device fixture and a firmware-bound capability declaration before the row appears.

## Gate assessment

- Capability gate: **FAIL** — no H criterion satisfied.
- Decoder readiness: **FAIL** — complete-frame validation absent.
- Fixture readiness: **FAIL** — no exact-device reply capture.
- UI gating: **PASS for the current bespoke screen**, with the latent catalog exposure noted.
- Safety gating: **PASS** — flag off, writes rejected, null baseline refused.
- Implementation status: **none proposed**; no API or code change is part of this evaluation.

Reopening requires the H evidence in `EVALUATION.md` and a separately approved implementation
plan that addresses every item above. Until then, retain the false flag, the absent row and the
shipped save sequence.
