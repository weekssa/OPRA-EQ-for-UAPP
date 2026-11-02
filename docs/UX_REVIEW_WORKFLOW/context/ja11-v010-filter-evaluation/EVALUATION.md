# JA11 v0.10.0 digital-filter evaluation

Status: evaluation complete. No product implementation, device execution, installation or
firmware action was performed. All repository work in this turn was document inspection.

**Determination: C7(c) — close the filter workstream again with stronger consolidated
evidence.** The current classification remains **device-specific not supported (vendor-exposed)**
for the tested JA11 firmware 2.20 tuple. Neither C7(a) nor C7(b) is met. This is not a claim that
the chipset cannot implement filters, and it does not exclude a hidden or future vendor interface.

Evidence classes used throughout:

- **DEMONSTRATED** — observed in a recorded physical session or captured artifact, bounded to
  its exact tuple.
- **CONFIGURED** — present in build configuration or code, with no runtime proof.
- **INSPECTION-ONLY** — read from repository or vendor artifacts without executing the behavior.
- **NOT EXERCISED** — the action was not run in this evaluation.
- **UNSUPPORTED** — no evidence establishes the claim.

This evaluation consolidates earlier DEMONSTRATED results; it does not re-demonstrate them. No
probe, capture, installation, connection or flash was performed, and the closed probes were not
re-run.

## Question and boundary

The question is whether JA11 reconstruction-filter control can be delivered in v0.10.0, whether
a separately gated firmware path is worth pursuing, or whether the deferral should be closed
again with stronger evidence. Reopening requires at least one of the handoff's H criteria:

> Reopen only with: a JA11 firmware whose vendor device record exposes a filter control; a
> first-party capability statement; a validated device-specific `0x04` reply; or an independently
> verified exact-device implementation.

None is satisfied. One nuance does not change the outcome: a newer JA11 vendor record that exposed a filter control would satisfy the first criterion at the record level only, and converting that into a delivered feature would still need a validated device read under C7(a). The determination below therefore rests on insufficiency, not on impossibility. The vendor record for the tested tuple declares no filter control; the two
device reads returned no valid reply; no first-party JA11 capability statement was found; and no
independent exact-device implementation exists. Sibling device records are artifact-local
observations with unresolved provenance.

## Slice outcomes

| Slice | Bounded outcome | Classification |
| --- | --- | --- |
| 0 — accepted evidence | Consolidated the release entry, handoff E/F/H/I, Candidate B investigation, reference evidence, protocol notes, the 2026-11-01 J034/J035 records and the capability matrix. | INSPECTION-ONLY consolidation of historical DEMONSTRATED results |
| 1 — silicon | No primary manufacturer or material source identifies the JA11 controller/DAC or documents reconstruction-filter capability. `KT02H2X` remains an OTA label. | UNSUPPORTED |
| 2 — vendor records | JA11 firmware 2.20 vendor record has no filter control. Preserved local APK provenance is unreconciled; 36 filter rows across 33 device types are provisional. No newer official release was found, so no refresh query was run. | INSPECTION-ONLY; integrity unresolved |
| 3 — firmware | Version read path, update-channel label, published history and update mechanics are documented. No update was performed; the V2.2 label's relation to device-read 2.20 is unverified. | INSPECTION-ONLY; updater NOT EXERCISED |
| 4 — novel read mechanisms | Mechanisms compared; at most one justified future read is allowed. None was exercised. | NOT EXERCISED |
| 5 — capture design | Capture topology and expected frames designed for a verified filter-capable sibling. No capture gear was acquired and no capture occurred. | INSPECTION-ONLY; capture NOT EXERCISED |
| 6 — readiness and gaps | Dormant code paths, false flag, UI gating, decoder weakness, missing fixtures and future tests inventoried. | INSPECTION-ONLY code findings; flag CONFIGURED false |

A null result in any slice means the covered question stays open at its current classification;
it never promotes a sibling behavior, a source vector or an app counter into JA11 capability
evidence.

## Slice 0 — accepted evidence

Sources: `docs/CHATGPT_PROJECT_RUNBOOK.md` release entry; `docs/research/FIIO_JA11_DIGITAL_FILTER_RESEARCH_HANDOFF.md`
sections E, F, H and I; `docs/releases/v0.9.0/CANDIDATE_B_INVESTIGATION.md`; `docs/releases/v0.9.0/CANDIDATE_B_READINESS.md`;
`docs/releases/v0.9.0/REFERENCE_EVIDENCE.md`; `docs/FIIO_JA11_PROTOCOL_NOTES.md`;
`docs/FIIO_JA11_VALIDATION_LEDGER.md` J034/J035 (2026-11-01); `docs/FIIO_JA11_CAPABILITY_MATRIX.md`.

J035 (2026-11-01, beta.3, source `db35f4290ae40f4e19720e99497951ab5c3a9bf2`, APK SHA-256
`f522c9448b4925164422daaca9ebb203af1db89108e3c3c5cdbcfe0ac29be6a4`) DEMONSTRATED, on one Pixel 9
with one JA11 at firmware 2.20 / USB `2972:0102`, that all five preset saves persisted across a
genuine power removal, the User 1 bank and global gain were unchanged, saved volume persisted,
runtime-only volume did not, and no microphone or UAC write occurred. The same record DEMONSTRATED
that the `0x04` filter read produced no valid reply and that no filter write was sent.

J034 (2026-11-01, beta.2) DEMONSTRATED the separate built-in-program persistence failure and its
restoration. It also recorded no valid `0x11` filter reply. The ledger contains a separate
2026-10-14 entry also labelled J034; that is a v0.8.3 release readback, not the filter session,
and must not be cited for filter evidence.

The handoff's E/F/H/I sections supply the chronological negative results, the evidence-strength
boundaries and the reopening criteria. Section F states the controlling boundary: the vendor
implementation does not expose the control for the tested tuple; firmware absence is a supported
inference; silicon impossibility, hidden mechanisms and future firmware exposure are not
established.

Null-result meaning: missing raw bytes remain missing. Application counters and UI readback are
not USB bus captures. No historical absence can be upgraded by re-reading this evidence.

## Slice 1 — silicon

The JA11 is identified by vendor record as device_type 109, model 1, with OTA label `KT02H2X` and
command builder `61442`. `KT02H2X` is an OTA/update label in the vendor record; it is not proof
of controller or DAC identity, and the repository does not hold a primary datasheet for it.

No primary manufacturer or material source found in this sitting identifies the actual controller
or DAC, or documents reconstruction-filter capability for it. Related KT02H20-family devices share
framing, not necessarily the capability. VID/PID, chipset family, visual similarity or
third-party compatibility do not identify silicon.

Classification: **UNSUPPORTED** for both silicon identity and built-in filter capability.
Null-result meaning: an unidentified controller does not imply a filter, and it does not rule one
out. This slice cannot be closed by inference from sibling devices.

## Slice 2 — vendor records and provenance

### Preserved APK discrepancy (preserved, unresolved)

The handoff pins the official Android artifact as 365,535,288 bytes, SHA-256
`ab0eb9cb4f6efd58a4e9b4edf3ad6ec11961b5e53d12fd6ecd1e80091730ac8d`. The preserved local copies
do not reproduce it:

- `/tmp/fiio460.apk` is malformed and hashes to `d7f1076e74f6a92e04616eed17e8d15b460491e822ce07c06d4ee3ac131cd6ac`.
- `/tmp/fiio460b.apk` has the recorded byte count but hashes to `516c6d882a6723b03d7860f8ab000ce8dc600d9fabe986bfe187ba15d37d110c`.
- The latter contains the inspected device database, SHA-256
  `1791f8a4210282d51ee7e5956f17bda3459b084fca9eaf8ad695aa5a0953fef9`.

Neither local hash matches the pinned artifact. The discrepancy is preserved as recorded; the
historical pinned hash was not replaced, and the local rows were not promoted to authoritative
vendor evidence. Every sibling observation below is therefore provisional provenance until a
hash-matching official artifact is obtained.

### JA11 record

`sc_product_info` id 47: device_type 109, model 1, `JadeAudio JA11`, connect_mode 3 (HID),
command_builder 61442, firmware command `0x0B`, `server_config_or_not = 1`, `ota_way = KT02H2X`.
`sc_func_info` holds one row for device 109 at version 2.20, a 3,170-byte JSON. That JSON declares
sample rate, `mic_detect` `0x12`, `uac_version_nui` `0x20`, `vol_output` `0x02`, PEQ band
`0x15`, preset `0x16`, gain `0x17`, eqSwitch `0x1A` (support false), and reset/firmware/name
settings. It contains **no filter entry and no `0x04`**. The schema enumerates controls with
get/set commands and show flags, so absence in this record is meaningful for this tuple.

The record is the declaration in the inspected local copy of the vendor app; its correspondence to the pinned release is unresolved because the APK digest does not match. It carries `server_config_or_not = 1`, which means a
server-side update cannot be fully excluded from the local artifact alone.

Classification: the absence of a filter control in the JA11 2.20 record is INSPECTION-ONLY, and
the artifact that contained it has unresolved provenance. It is strong evidence that the vendor
implementation does not expose the control; it is not proof of hardware absence.

### Provisional sibling rows

The inspected local database contains 36 filter rows covering 33 device types. USB examples
include KA1/KA2/KA3/KA5, Q11, KA11/KA13/KA17/KA15, QX13/QX11 and MELODY. Their declared get/set IDs
vary. None of the inspected other product records shares JA11's `KT02H2X` OTA label or builder
`61442`.

These rows stay **provisional** for two independent reasons: the enclosing APK provenance is
unresolved, and a sibling record can demonstrate only its own protocol surface. A verified newer
JA11 record exposing filters would satisfy H at the record level only; it would still require a
validated device read before any UI or write.

### Corrected refresh query

`sc_func_info` has no `device_model` column. To re-check a newer artifact, inspect the schema
first, establish model 1 through `sc_product_info`, then run:

```sql
SELECT device_version, function_msg FROM sc_func_info WHERE device_type=109;
```

The closed V4.6.0 JA11 query is not to be repeated. The official app history checked lists V4.6.0,
dated September 16, 2026. No newer official release was found, so no refresh query was executed
and no support inference follows from its absence.

Null-result meaning: a missing newer release yields no new evidence; it does not confirm the
current record forever.

## Slice 3 — firmware

Firmware version is read with `0x0B` (major/minor bytes). The tested unit reads **2.20**; the
update channel is labelled `KT02H2X`. The published firmware history checked lists V2.2 and does
not announce filter support. Whether that label corresponds exactly to device-read 2.20 remains
**unverified**; V2.2 must not be presented as an available upgrade beyond 2.20.

Update mechanics and published changelogs were inspected only. Any flash is outside this task and
would require its own contract, this-unit consent, and written brick, downgrade and restoration
disclosure.

Classification: documentation **INSPECTION-ONLY**; updater behavior and any upgrade **NOT
EXERCISED**. Missing filter release notes do not establish impossibility, and no flash is
prepared, scheduled or promised.

Null-result meaning: an absent changelog entry means only that no filter support was announced;
it does not prove the firmware lacks the control.

## Slice 4 — novel read mechanisms

Four mechanisms were compared: an Android raw-diagnostics candidate, hidapi report handling,
WebHID report-ID handling, and descriptor-supported transfer sizes. The raw logger accepts
received data before decoding, so an alleged pre-decoder rejection is not established. Report APIs
handle report IDs differently and require explicit normalization.

Facts that constrain any future probe: the transport, framing and report ID carry every other
command successfully; the decoder would accept a standard `0x04` reply with value 0..4; the probe
currently records response hex, elapsed time and session counters, but not the raw transfer
metadata a complete-frame finding needs.

Classification: all device mechanisms **NOT EXERCISED**. A future read is justified only by a
specific question with a predicted discriminating result, chosen before authorization. A complete,
independently validated current-device reply would be DEMONSTRATED for that exact tuple/read. Null,
garbage, wrong shape or instability ends that branch without retry and without a write.

Null-result meaning: a null read resolves only the tested mechanism on that tuple; it does not
justify a second mechanism, a retry or a write.

## Slice 5 — capture design

The default design for capturing the official Android app is a borrowed, suitable inline USB
analyzer between the Android host and a provenance-verified filter-capable sibling. Purchase
requires a separate owner cost decision; a second unrooted Android capture app is not an
established passive-sniff solution.

Linux `usbmon` is the lowest incremental-cost option only when Linux itself hosts the device and
the chosen vendor application supports that topology. It cannot observe another Android host's USB
connection, and its host-controller trace is not guaranteed to reproduce every physical bus
transaction exactly. FiiO's published instructions name Windows and macOS, so Linux vendor-web
compatibility remains unverified.

Expected frames must come from the selected sibling's verified record and builder; `BB 0B 04`
must not be assumed across products. A capture record must include descriptors, endpoints/report
IDs, direction, requested and actual length, timestamps, the user's control choice, firmware and
app version, a raw-capture hash, and a separate decoded interpretation.

Classification: tool documentation **INSPECTION-ONLY**; capture **NOT EXERCISED**. A sibling
capture can demonstrate that sibling's protocol, never JA11 capability or persistence. No sibling
mutation occurs without its own authorization.

Null-result meaning: a null capture resolves only what that topology observed; it cannot establish
absence of traffic the capture could not see.

## Slice 6 — readiness

Detailed inventory is in `READINESS.md`. Summary: the filter enum, codec, descriptor, repository
gating, probe cache, transport probe and diagnostics remain in production source; the build flag
`JA11_FILTER_CONTROL_ENABLED` is **CONFIGURED false** at `app/build.gradle.kts:46`; the DEVICE row
and its callbacks are absent; writes are rejected while the flag is off. Tests cover the codec
frames and the absent row, but no test holds an exact-device `0x04` reply.

Decoder finding: `packetView` tolerates several prefixes, and `oneByteValueFromResponse` checks
minimum size, command byte and value without validating declared length, footer or checksum. A
future implementation must add complete-frame validation; decoder success alone does not establish
a device reply.

Classification: source findings **INSPECTION-ONLY**; the false flag **CONFIGURED**. Existing tests
do not establish device support. Nothing in this evaluation changes an API, a build flag or a
save sequence.

Null-result meaning: a missing fixture or test is a readiness gap, not device behavior evidence in
either direction.

## B4/B5 adjudication

**B4 — the firmware does not implement the control interface.** Supported inference, not
established. The vendor record for the tested tuple declares no filter control, both vendor
commands returned no valid reply, and the web bundle's filter helpers have no examined caller.
The server-config caveat and the absent raw capture keep it from being proven.

**B5 — a hidden or future interface exists.** Possible but unproven. No evidence demonstrates it,
and none excludes it. Vendor semantics never prove JA11 persistence.

Neither B4 nor B5 establishes silicon impossibility. The classification **device-specific not
supported (vendor-exposed)** describes the tested tuple's exposed interface, not the silicon.

## Three-way determination

- **C7(a) — deliver filters in v0.10.0:** not met. No H criterion is verified, no complete
  device-specific read establishes the control on current firmware, and no implementation or
  physical qualification exists. Reporting this as a delivery would be unsupported.
- **C7(b) — separately gated firmware path worth pursuing:** not met. No exact-JA11 first-party
  evidence identifies an exposing firmware. V2.2's relation to 2.20 is unverified, and no
  published filter support exists.
- **C7(c) — close again with stronger consolidated evidence:** selected. Evidence is insufficient
  for (a) or (b); provenance is unresolved; the authorized reads were null. Preserve the
  classification and the residual B5 uncertainty. Do not continue probing.

## Residual uncertainty and what would change the outcome

The outcome would change only with evidence that satisfies an H criterion: a verified newer JA11
vendor record that declares a filter control, a first-party capability statement, a complete and
independently validated `0x04` reply on the current firmware, or an independently verified
exact-device implementation. A verified filter-capable sibling could justify a capture design, but
its own record and traffic would not satisfy H for the JA11.

Open questions: which preserved APK, if any, can be reconciled to the pinned official artifact;
what primary source identifies the controller/DAC; whether V2.2 corresponds exactly to 2.20; and
whether a suitable sibling and capture gear are actually available. Each is an evidence question
with a fail-closed default.

## What this evaluation does not establish

- It does not establish that the JA11 hardware cannot filter.
- It does not establish a hidden command or a future firmware capability.
- It does not establish persistence of any filter value.
- It does not verify the installed app state on any phone.
- It does not authorize implementation, installation, connection, writes, firmware action,
  issue filing, merge or publication.

## Named sources

- `docs/research/FIIO_JA11_DIGITAL_FILTER_RESEARCH_HANDOFF.md` (A–J, especially E/F/H/I)
- `docs/FIIO_JA11_VALIDATION_LEDGER.md` (2026-11-01 J034 and J035; separate 2026-10-14 J034)
- `docs/releases/v0.9.0/CANDIDATE_B_INVESTIGATION.md` and `CANDIDATE_B_READINESS.md`
- `docs/releases/v0.9.0/REFERENCE_EVIDENCE.md`
- `docs/FIIO_JA11_PROTOCOL_NOTES.md` and `docs/FIIO_JA11_CAPABILITY_MATRIX.md`
- `docs/PROJECT_STATE.md`, `docs/CHATGPT_PROJECT_RUNBOOK.md`
- [FiiO app history](https://forum.fiio.com/phoneNote/showNoteContent.do?id=202105071628040377809)
- [JA11 firmware history](https://forum.fiio.com/phoneNote/showNoteContent.do?id=202406211150090736905&tid=17)
- [hidapi documentation](https://libusb.info/hidapi/group__API.html)
- [Linux usbmon documentation](https://cdn.kernel.org/doc/html/latest/usb/usbmon.html)
