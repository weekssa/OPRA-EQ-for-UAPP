# JA11 B1 repeat evidence — 2026-11-03

Status: **DEMONSTRATED READ-ONLY RESULT; BOUNDED NEGATIVE.** No filter write,
Apply, Save, install or firmware action was performed.

The separately approved repeat ran on macOS against one `FIIO JadeAudio JA11`,
USB `2972:0102`, version `0x0220` (2.20). A 6.015 ms volume response passed the
complete-frame CRC and value checks and returned 60. The three registered `0x11`
reads then each received no input report during the fixed 700 ms window.

This is a bounded negative for the tested unit/firmware/host tuple. It does not
establish silicon absence, mode meanings, selection behavior, acoustic effects,
persistence or restoration.

The executed harness waited through each silent window but did not persist
per-window elapsed timestamps. The total process duration was approximately
2.05 s, consistent with the three full windows plus the volume response. A later
timestamp-preserving harness was prepared but is not part of this executed
evidence and was not run.

The public archive omits the stable serial hash and session location identifier.
An unredacted local research copy is retained privately outside the repository.

Files:

- `harness.swift`: exact executed native IOKit source.
- `manifest.json`: identity, request plan, raw volume reply, outcomes and summary.
- `session.jsonl`: ordered send, response and silence events.
- `SHA256SUMS`: archive digests.
