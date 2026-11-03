# JA11 B1 read-only session package

Status: **PREPARED — NOT EXERCISED.** No device connection or request was made
while preparing this package. Running the four requests below requires a separate,
current owner approval that names this exact session, unit, host and evidence
directory. Approval of Option B is not session consent.

## Question

On one host and in one stable JA11 session, does acceptance of `0x11` differ
between the exact J034 request, computed CRC at sequence zero, and computed CRC
at sequence two?

The session is read-only. The harness has no filter-set, Apply, Save, reset,
firmware, volume-write or discovery command. A positive result proves only
readback on the exact tested tuple. Mode meanings, selection behavior, acoustic
effect, persistence and restoration remain unproven.

## Approval exception

Issue #132 says: **“Do not repeat: the `0x11` probe.”** The zero-CRC J034
replicate is authorized only inside one separately approved B1 session because
same-host and same-session framing are the evidence question. It does not
authorize repeating J034 elsewhere, in another session, on another host, after
a failed request, or after the B1 session ends.

## Exact requests

All requests include report ID `02`, use empty payloads, and have payload length
`00`. Send them once each in this order.

| Order | Purpose | Complete request | CRC basis |
|---:|---|---|---|
| 1 | Volume control | `02 BB 0B 00 01 02 00 3A EE` | Computed CRC of `00 01 02 00` |
| 2 | Exact J034 replicate | `02 BB 0B 00 00 11 00 00 EE` | Literal zero CRC |
| 3 | Computed CRC, sequence zero | `02 BB 0B 00 00 11 00 28 EE` | Computed CRC of `00 00 11 00` |
| 4 | Computed CRC, sequence two | `02 BB 0B 00 02 11 00 67 EE` | Computed CRC of `00 02 11 00` |

CRC-8/MAXIM uses init `00`, reflected polynomial `8C`, and no final XOR. The
scope follows ParkWardRR's implemented encoder: sequence high through the last
payload byte. These are offline source-derived vectors, not captured traffic.

## Coverage limit

| Filter CRC form | Sequence `0000` | Sequence `0002` |
|---|---|---|
| Zero CRC | Request 2 | **NOT EXERCISED** |
| Computed CRC | Request 3 | Request 4 |

No fifth frame is authorized. The session compares CRC at sequence zero and
sequence under computed CRC. It cannot fully distinguish a sequence-only rule
from a combined CRC-and-sequence rule.

## Hardware and prerequisites

- Use the owner's exact JA11, preferably the previously tested firmware-2.20
  unit. Do not identify an exotic or cross-flashed unit from its product string.
- Use the same host and one continuous device session. Record host label, device
  provenance, USB descriptor identity, and the exact HID path chosen.
- Use an existing approved Python HIDAPI setup and the exact device path. If the
  binding or path is unavailable, stop; do not install or auto-discover a
  substitute.
- Before every request after the volume control, the harness re-enumerates the
  exact path/VID/PID and stops if the descriptor identity, serial or product
  string changes. This is a local identity check, not an extra device command.
- Freeze the exact script hash before the session. Preserve `session.jsonl`,
  `manifest.json`, capture hashes, transfer timings and any owner notes in a new
  or empty evidence directory.
- Account before consent for any host transfer or reconnection that could reset
  runtime state. Do not add a power-cycle test.

Prepared harness identity:

```text
tools/ja11_filter_probe.py
SHA-256 b487c1bb0ea5ac64b393a785ed8461ef6d6c48def8b9bc4008adb7ba980de03a
```

Freeze and re-hash the file in the approved session. A mismatch requires review
before execution.

Prepared invocation after approval and with placeholder values replaced:

```sh
python3 tools/ja11_filter_probe.py run \
  --execute --owner-approved-session '<current session approval reference>' \
  --device-path '<exact HIDAPI path>' \
  --vendor-id 0x2972 --product-id 0x0102 \
  --evidence-dir '<new evidence directory>' \
  --operator '<owner>' \
  --host-label '<host>' \
  --device-provenance '<unit/firmware provenance>'
```

## Session sequence and stop conditions

1. Send the volume control once. Poll input only through a 700 ms observation
   window; do not resend.
2. Continue only for a complete volume reply correlated to command `02` and
   sequence `0001`, with valid length, CRC, terminator and value `0..60`.
3. Send request 2 once and observe for 700 ms. Silence does not prevent the next
   planned request.
4. If the session is stable, send request 3 once and observe for 700 ms.
5. If the session is stable, send request 4 once and observe for 700 ms.
6. A qualifying filter reply must be complete and correlated to command `11` and
   the request sequence, with valid length, CRC and terminator, and a value in
   `1..5`.
7. Requests 2 and 3 share command and sequence. Preserve elapsed time and any
   queued traffic. Do not assign an ambiguously late report to either request.
8. Stop immediately on detach, identity change, partial/failed output, malformed
   response, unexpected state, or ambiguous attribution. No retry and no
   transport, timeout, command or payload change is permitted.

## Pre-registered interpretation

`R` means a complete qualifying reply in the request's 700 ms window. `S` means
silence throughout it. The table applies only when the volume control passes,
all three filter windows complete, and responses are unambiguously attributed.

| Request 2 zero CRC / seq 0 | Request 3 CRC / seq 0 | Request 4 CRC / seq 2 | Meaning |
|---|---|---|---|
| R | R | R | Reopen. Both CRC forms and both sequences work in this session; neither computed CRC nor nonzero sequence is necessary for these reads. |
| S | R | R | Reopen. Supports CRC-sensitive acceptance at sequence zero; sequence two is unnecessary for the positive reads. Order/transient effects prevent declaring CRC universally required. |
| S | S | R | Reopen. Supports sequence- or order-sensitive acceptance under computed CRC. Sequence-only versus combined CRC-and-sequence remains indistinguishable. |
| S | S | S | Bounded negative for all three exact requests. Keep filters deferred; do not infer hardware impossibility. |
| R | S | R | Reopen. Contradicts a simple computed-CRC requirement; the middle silence requires interpretation without retry. |
| R | R | S | Reopen. Sequence zero works with both CRC forms; the later silence supports a sequence, order or transient question, not a nonzero-sequence requirement. |
| R | S | S | Reopen on the J034 replicate. Computed CRC is not necessary for that positive read; why computed variants were silent remains unresolved. |
| S | R | S | Reopen on request 3. Supports CRC-sensitive acceptance at sequence zero; sequence/order effects remain unresolved. |

Volume failure is inconclusive and stops before any filter request. Transfer
failure, malformed data, identity loss or ambiguous attribution is also
inconclusive; preserve any earlier qualifying reply without inferring mode
semantics. Differing qualifying values are a readback anomaly requiring review,
not evidence of a filter change.

## Restoration and evidence closeout

No setting is intentionally changed. Record the starting state available without
extra commands and preserve the owner's connection context. Do not claim full
EQ, microphone or UAC restoration from these reads. If an unexpected state change
occurs, stop and report restoration as unverified; corrective writes require
separate authorization and no automatic restoration routine is allowed.

The closeout classifies the four outcomes and records what remains unproven:
mode meanings, selection behavior, acoustic effect, persistence and restoration.
