# EW300 validation ledger

Append-only release evidence for the v0.7 EW300 candidate. This ledger distinguishes accepted
historical qualification from the current implementation candidate. It does not authorize a
merge, publication, or public support claim.

## Evidence records

| ID | Exact source / artifact | Result and claim | Restoration / limits |
| --- | --- | --- | --- |
| E001 | `847eaba8dfa14b8e63ab9ed2326f1e3b0f07814f`; `EQ-Library-v0.7.0-beta-847eaba.apk`; SHA-256 `51392e8b79b4b7ffe07b3298a4142577f3df7490de56cd146baa8094aa9d6a54`; signer `65C1C1256DAE3C49E3548F334C91F0BA991969E9BE9E0B223BA4E253D2114747` | Accepted frozen exact-device qualification: Save once, Peak persistence, playback-gain persistence, exact baseline restoration, and two complete power removals. | Exact observed fingerprint only; do not repeat Save qualification. |
| E002 | `f8707788531cfdef33cd46d79c5c53e649480232`; signed beta workflow #1256 | Stopped before a verified Flash result after Android showed a permission prompt and competing USB-app chooser during EW300 reconnect. | No Flash PASS inferred; no retry with this APK. |
| E003 | `6b3a35c6e8901c89f52ec3588d0feac744cba017`; `EQ-Library-v0.7.0-beta-6b3a35c.apk`; SHA-256 `810db843cd989a4277978e58749dea215ca5f9c0593927db9c26922505a0d8e8`; signer above; workflow #1257 | Software/security/signing/install/cold-launch gates passed. Owner stopped when the normal Android permission prompt appeared immediately before visible Flash progress. | No Flash PASS or post-Save state inferred. Source-level cause was the EW300 automatic reconnect observer calling `connect()` before the mutation had released replacement reconnect. |
| E004 | Current branch implementation after the reconnect-gate change | Automatic EW300 reconnect is blocked from mutation entry through pre-Save writes, released only after the single Save send, and cleared in `finally`; operation reports expose permission and Save invariants. | Requires targeted automated tests, one coherent CI candidate, and the single owner hardware session. |
| E005 | Current branch tests and protocol fixtures | Exact fingerprint, direct-Hz Peak path, strict four-byte baseline, no write retry, volatile restoration, and bounded failure behavior remain covered. | Does not qualify an unobserved retail revision or unsupported DEVICE controls. |
| E006 | Section 7 Extra-High gate 1 | `EXTRA-HIGH GATE READY`: review whether the root cause, reconnect gate, Save release point, replacement authorization, race/idempotency behavior, and no-replay tests are sufficient. | Smallest action: perform one bounded Extra-High code review on the frozen diff; do not rerun hardware or restart research. |
| E007 | Section 7 Extra-High gate 2 | `EXTRA-HIGH GATE READY`: after CI freezes the candidate, review release provenance, invariant report fields, software gates, and hardware stop conditions. | Smallest action: perform one bounded Extra-High release audit; then either request the single physical session or record the specific defect. |
| E008 | `855364e8a9d758f12e7d2a48bdaf89f457e28070`; signed workflow #1258; artifact digest `sha256:151555c3d9bd8664e119a59f2b99d4ab4a3ac115dbb5cfa06ead2ee061589fc4` | Exact signed candidate passed the complete software/release gate: Android unit/UI/build validation, lint, CodeQL, dependency, catalog, priority-community, package/version, pinned-signer, alignment, install, and cold-launch checks. APK `EQ-Library-v0.7.0-beta-855364e.apk` SHA-256 is `bfcb77f4774e8f0a9e48c1da13a262e9033c8e0f42a7a18774e864c0b1774b44`; signer is `65C1C1256DAE3C49E3548F334C91F0BA991969E9BE9E0B223BA4E253D2114747`. | Candidate is ready for the one consolidated owner hardware session. No physical result is inferred, no merge/publication is authorized, and the temporary `mobile-test-apk` surface is not a release publication. |

## Exact identity boundary

```text
vid=31b2|pid=111|manufacturer=LE XIAN|product=SIMGOT EW300 DSP|interface=3
```

Serial values are retained only in the private accepted hardware evidence and are not exported by
the operation report. The product must continue to match the exact manufacturer/product/HID
interface profile; VID/PID alone is insufficient.

## Candidate gate status

The exact signed candidate at `855364e8a9d758f12e7d2a48bdaf89f457e28070` has passed the software,
security, signing, installation, cold-launch, accessibility/emulator, and release-polish gates in
one frozen workflow cycle. The generated candidate manifest is retained in signed workflow
artifact `EQ-Library-signed-beta-855364e8a9d758f12e7d2a48bdaf89f457e28070`.

The physical session is one consolidated session only, with no automatic mutation retry. The
bounded Extra-High release audit remains `EXTRA-HIGH GATE READY`: review whether the exact artifact
provenance, operation-report invariants, software gates, and hardware stop conditions are complete;
the smallest action is one focused review of the frozen diff and candidate record.
