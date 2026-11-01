# Candidate B — final evidence review and release readiness

Status: **AWAITING OWNER SCOPE DECISION.** Goal A is physically verified; Goal B is a documented
device-specific limitation, not a delivered feature. No merge, signing, publication or further device
mutation is authorized.

## Deliverable A — Goal A qualification summary

Physically verified on **beta.3** (`0.9.0-beta.3` / code 17, APK
`f522c9448b4925164422daaca9ebb203af1db89108e3c3c5cdbcfe0ac29be6a4`, source
`db35f4290ae40f4e19720e99497951ab5c3a9bf2`, local test signer `0f89797b…`), record **J035**.

- Vocal (payload 0), Classic (1), Bass (2), User 1 (3) and Off (4) each saved and survived genuine USB
  power removal and cold reattachment.
- Saved volume persisted; runtime-only volume changes reverted to the stored pair.
- The User 1 bank and global gain were identical to the captured baseline after every built-in save.
- Zero microphone and UAC writes; no reconnect workaround.
- Restoration verified: stored User 1 / volume 60, runtime Off / 60, bank exact.

Remaining correctness notes: one unit, firmware 2.20, one host; volume qualified at 60 (40/35 in J034).
The vendor-ordering difference (extra restage and Apply for built-ins) is retained deliberately and
recorded as a future optimisation, not a blocker.

## Deliverable B — Goal B device-capability assessment

**Final classification: digital-filter control is not exposed by the official vendor implementation for
the tested JA11 firmware 2.20.** No working filter selection has been demonstrated. This is not a claim
that the chipset cannot implement filters, nor that a future firmware could not expose them.

Vendor evidence:

- Official **FiiO Control Android** APK V4.6.0 from
  `https://fiio-firmware.fiio.net/FiiOControl/FiiOControl%20V4.6.0.apk`, 365,535,288 bytes, SHA-256
  `ab0eb9cb4f6efd58a4e9b4edf3ad6ec11961b5e53d12fd6ecd1e80091730ac8d` (downloaded, not installed).
- Bundled `assets/flutter_assets/assets/database/sc_config_default.db`: `sc_product_info` id 47,
  **device_type 109, model 1, JadeAudio JA11**, HID connect, command builder 61442, firmware command
  `0x0B`, `ota_way = KT02H2X`; `sc_func_info` has one row for device 109 at **device_version 2.20** —
  an exact model and firmware match.
- That 2.20 record declares sample rate, `mic_detect` (0x12), `uac_version_nui` (0x20), `vol_output`
  (0x02), PEQ band (0x15), preset (0x16), gain (0x17), `eqSwitch` (0x1A, support=false), and settings
  (firmware upgrade, hidden device name, hidden factory reset). **No filter mode and no `0x04` entry
  exists.**
- Web app (`index-7hb30NUo.js`, `4bc62044…`; Audio Config chunk `index-C70qb620.js`, `c5f3d0bc…`):
  the `0x04` helpers exist but no examined UI chunk calls them.
- Device: no valid reply to `0x11` (J034) or `0x04` (J035); no filter write was ever sent.

Focused verification of the vendor chain:

1. **Applicability** — exact device_type 109 and version 2.20; one row only.
2. **Other layers** — no server-config endpoint or download path was found in the APK; the
   `server_config_or_not = 1` flag exists but no reachable override was demonstrated. The bundled
   record is the shipped declaration; a server-side update cannot be fully excluded.
3. **Feature logic** — the app parses `functionMsg` into `stateItems`, `settingItems` and
   `devicePeqInfo`; controls are data-driven from this record.
4. **Meaning of absence** — the schema enumerates controls with explicit get/set commands and show
   flags; a filter control would require an entry. The absence is meaningful, not an optional field.
5. **Contradiction check** — JA11 FAQ and firmware notes through V2.2 do not mention reconstruction
   filters; the KA15 (different device) does. No first-party JA11 filter claim was found.

Reopening conditions: a JA11 firmware revision whose vendor record exposes a filter control; a
first-party capability declaration; a validated device-specific reply to `0x04`; or an independently
verified JA11 implementation.

## Deliverable C — lead-agent independent review

Personally verified: both vendor bundle identities and every lazy chunk; the Android APK device
database (schema, JA11 row, firmware row, control list); the AOT strings showing the app consumes those
fields; the transport comparison (same path carries 0x02/0x0B/0x15/0x16/0x19); and the J035 results.
Challenged: that a shared vendor command implies support (rejected); that no reply proves absence
(qualified); that our decoder would reject a valid standard reply (rejected — protocol tests accept
0x04 values 0..4). The delegated agents missed the Android app; that is where the decisive evidence
came from. No material gap remains within accessible sources.

## Deliverable D — beta.4 diagnostic decision

**Do not install beta.4 for this purpose.** It logs request and response bytes, elapsed time and session
state at the application layer. It can separate a rejected reply from no reply, but it is not a USB bus
capture. The same transport and framing carry every other command, the decoder accepts standard 0x04
replies, and the vendor app never reads the filter, so another unanswered probe would not change the
classification or the product behaviour. beta.4 stays prepared and uninstalled (`0.9.0-beta.4`/code 18,
APK `651100980a7d2361e5fc5ec486ae309e4ddaf8bb59f98409883281f3f53deebe`).

## Deliverable E — product integration

For the exact profile the Digital filter row is present, disabled, and reads "Not available on this
JA11" with "JA11 did not report a filter setting, so EQ Library sent no filter command." No write is
possible. Repository and Compose tests cover unsupported and ignored-write cases. This is truthful and
needs no change. Other devices and firmwares keep runtime capability negotiation; nothing is globally
disabled.

## Deliverable F — release readiness

- Qualified technical basis: **beta.3** (physically verified). beta.4 is diagnostic-only and must not
  replace it.
- Gates on the beta.3-era source: 830+ JVM tests, lint debug/release/privateBeta,
  debug/release/privateBeta/androidTest builds, R8 mapping, 306 Python/contract tests, 16/16 FiioJa11
  UI tests; independent review found no blockers.
- Outstanding: the owner scope decision on Goal B; merge, official signing, publication and any new
  device session remain unauthorized.
- If both goals are required for release, Goal B is a product-scope blocker. If a release may ship with
  Goal A complete and Goal B documented as a vendor-exposed limitation, the remaining work is release
  preparation plus the owner publication approval.

## Deliverable G — records

This document, the investigation, protocol notes, capability matrix, project state and runbook are
updated. J034 and J035 remain unchanged historical physical records.

## Final beta.5 candidate identity (2026-11-01)

| Item | Value |
|---|---|
| Version | `0.9.0-beta.5` / code `19` |
| Package | `com.weekssa.opraeqforuapp.v090beta` |
| Source | `655810c7b1a6ceae72767b9dbe48d1409aab2d70` |
| APK SHA-256 | `9f1c9c0e1a300ab021bc051abe8f685ebfde060bdda0d18b4358fe5e406c0ca6` |
| Signer | local test `0f89797ba477006eea3e1ee837eaf654e687a14fc50b5a213b40312cdd1f869f` |
| Variant | `privateBeta` (R8 minified, local test signing, update checks off) |
| Qualification | persistence code identical to J035-qualified beta.3; UI-only change, no new physical claim |

Gates: 834 JVM tests, lint debug/release/privateBeta, debug/release/privateBeta/androidTest builds, R8,
306 Python/contract tests, 78/78 API 35 instrumentation, API 26 and API 35 install/launch. The DEVICE
filter row is absent, the probe is dormant and filter writes are rejected when the flag is off.
