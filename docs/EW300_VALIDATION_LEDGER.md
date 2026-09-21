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
| E009 | Candidate `855364e8a9d758f12e7d2a48bdaf89f457e28070`; APPLY operation `afbfc691-8eab-4680-b160-3b6a9552c8d3`; readable and JSON reports exported from the exact signed APK | The consolidated session stopped safely at Apply preflight with `outcome=InvalidPlan`: `baselineCaptured=true`, `stateKnown=true`, `registerWriteCount=0`, `saveCommandCount=0`, `permissionRequestCount=0`, `mutationReplayCount=0`, and `competingConnectionJobCount=0`. The red UI warning identified a software headroom-baseline mapping defect; no hardware mutation occurred. | Do not retry this APK. The exact EW300 device state is known and no write was sent. A replacement candidate must correct the editor’s use of the verified global-gain register, pass targeted and complete gates, and only then return to the single physical session. |
| E010 | `8bb87aac689ce28b1e92e115a3f83f1b59ad1f65`; signed workflow #1260; artifact digest `sha256:d6214b9d2a7f4fe69a8a9a8b3690c6beb3173a2b29f4f7ac964add97bfa581d6`; APK `EQ-Library-v0.7.0-beta-8bb87aa.apk`; APK SHA-256 `f470a3d330951705f8bd94450f9adaa813040d8717541eeed75956f43b97607d`; signer `65C1C1256DAE3C49E3548F334C91F0BA991969E9BE9E0B223BA4E253D2114747` | Replacement candidate passed the complete software/release gate: Android unit/lint/build, emulator UI, CodeQL, dependency, catalog, priority-community, package/version, pinned-signer, alignment, installation, and cold-launch checks. The EW300 editor now uses the verified absolute device-global-gain baseline. | No physical mutation has been attempted with this candidate. It is the only APK authorized for the one consolidated owner session; no merge, publication, or public EW300 support claim is authorized. |
| E011 | `f41f985cb9f7a5d1622bc2e85ddfe5d14777e25d`; signed workflow #1261; artifact digest `sha256:faad9a66e0a510d6cc50f82d7bc7d01d9f9f35e3deefe960954f318a92fa5ef7`; APK `EQ-Library-v0.7.0-beta-f41f985.apk`; APK SHA-256 `0f70d9f290c487691992ac657d8f2dae1ccba844a99749c6b44e37895ce4c345`; signer `65C1C1256DAE3C49E3548F334C91F0BA991969E9BE9E0B223BA4E253D2114747` | Current replacement candidate passed the complete software/release gate after tightening EW300 automatic reconnect: Android unit/lint/build, emulator UI, CodeQL, dependency, catalog, priority-community, package/version, pinned-signer, alignment, installation, and cold-launch checks. A pre-Save failure now remains blocked from automatic permission prompting until explicit manual reconnect. | No physical mutation has been attempted with this candidate. It is the only APK authorized for the one consolidated owner session; no merge, publication, or public EW300 support claim is authorized. |
| E012 | `7035518b042a7b19c0495869cf247359ee27b4a2`; signed workflow [#1262](https://github.com/weekssa/OPRA-EQ-for-UAPP/actions/runs/35626998659); artifact digest `sha256:b0b3e427f346af9cbd7448906f9e0c815fad7eac5b2b82227d1ea6ceb22b807a`; APK `EQ-Library-v0.7.0-beta-7035518.apk`; APK SHA-256 `661ca49488939c15f290868f1be05ab24dfb1b1a870b977455dabfedce409c66`; signer `65C1C1256DAE3C49E3548F334C91F0BA991969E9BE9E0B223BA4E253D2114747` | Current signed candidate passed the complete software/release gate: Android unit/lint/build, emulator UI, CodeQL, dependency, catalog, priority-community, package/version, pinned-signer, alignment, installation, and cold-launch checks. The EW300 automatic reconnect guard now checks authorization at invocation time, closing the queued-callback race exposed by the prior candidate. | No physical mutation has been attempted with this candidate. It is the only APK authorized for the one consolidated owner session; no merge, publication, or public EW300 support claim is authorized. |
| E013 | Exact candidate `7035518b042a7b19c0495869cf247359ee27b4a2`; owner readable report `EW300 capability report (2)` and JSON `EW300 capability report JSON (7)` | Read-only capability report PASS for the exact qualified fingerprint; `stateKnown=true`, strict four-byte validation passed, and no failure stop occurred. The report confirms the current candidate source and records persistence qualification as `NOT_STARTED`, preserving the accepted Save qualification rather than repeating it. | Read-only only; no mutation, Save, Reset, or Personal EQ capture was performed. The single guarded Apply/Flash session remains pending. |

## Exact identity boundary

```text
vid=31b2|pid=111|manufacturer=LE XIAN|product=SIMGOT EW300 DSP|interface=3
```

Serial values are retained only in the private accepted hardware evidence and are not exported by
the operation report. The product must continue to match the exact manufacturer/product/HID
interface profile; VID/PID alone is insufficient.

## Candidate gate status

The current replacement signed candidate at `7035518b042a7b19c0495869cf247359ee27b4a2` passed the
software, security, signing, installation, cold-launch, accessibility/emulator, and release-polish
gates in one frozen workflow cycle. It has not yet been used for physical mutation. The generated
candidate manifest is retained in signed workflow artifact
`EQ-Library-signed-beta-7035518b042a7b19c0495869cf247359ee27b4a2`.

The physical session remains one consolidated session only, with no automatic mutation retry. The
current signed candidate is eligible for that session after the bounded Extra-High release review
gate. The smallest review action is to inspect whether the `DEVICE_GLOBAL_GAIN` mapping, the
pre-Save automatic-reconnect block, the invocation-time `connectAutomatically()` guard, targeted
gate tests, exact candidate provenance, and no-write stop evidence preserve the no-replay and
state-known invariants; do not restart research or repeat the accepted Save qualification.
