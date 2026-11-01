# Candidate B — root-cause investigation (EQ persistence and digital filters)

Status: **IN PROGRESS.** Owner directive 2026-11-01 replaces the microphone workaround as the answer
and requires a genuine, evidence-backed mechanism for both goals. UAC toggling is expressly rejected.
Nothing here authorizes hardware writes beyond the existing session authorization.

Workflow note: the repository has no separate "research workflow Markdown". The canonical
`/Users/stephenweeks/Documents/Codex/Codex-Usage-Policies/WORKFLOW.md` and the project `AGENTS.md`
govern this work; that access gap is recorded rather than claimed as compliance with a missing file.

## Established facts (do not re-derive; J034 and earlier)

- Native User 1 selection + volume survived genuine power removal (J034 test 1).
- `0x19` payload 3, as exercised, committed the working five-band area into User 1 and did **not**
  persist a built-in program selection; with Classic active it replaced the User 1 bank (J034 test 2).
- The app reported that mismatch as a failure, not success.
- The User 1 bank was restored exactly through the editor and verified across another power cycle.
- The tested unit returned no valid `0x11` reply (digital filter row disabled; no write sent).
- A microphone-toggle-induced USB re-enumeration has historically preserved program + volume.

## Phase 1 — evidence reconstruction (repository)

### Command surface as implemented

| Command | Direction | Payload | Implemented meaning | Evidence |
|---|---|---|---|---|
| `0x02` | R/W | 1 byte 0–60 | device volume | official/qualified |
| `0x09` | R | – | sample-rate code | qualified |
| `0x0B` | R | – | firmware major/minor | qualified |
| `0x11` | R/W | 1..5 | reconstruction filter (claimed) | reference only; unanswered on this unit |
| `0x12` | R/W | 0/1 | headset/mic remote | qualified (restarts session) |
| `0x15` | R/W | band record | PEQ band staging | qualified |
| `0x16` | R/W | program code | active EQ program | qualified (runtime) |
| `0x17` | R/W | signed tenths | global EQ gain | qualified |
| `0x18` | W | 1 | Apply working area | qualified |
| `0x19` | W | 3 | "save to flash" | J034 shows it targets User 1 coefficients |
| `0x20` | R/W | 0/1 | UAC mode | qualified (restarts session) |

### State model the app currently assumes

`activeRuntimeProgram` and `deviceVolume` are runtime registers; `workingBands`/`globalGain` are the
staged PEQ area; Save(3) persists the working area (observed to land in User 1). Nothing in the
implemented surface claims to write a "startup program" or "startup volume" record.

### Competing hypotheses for Goal A

| ID | Hypothesis | Distinguishing evidence | Status |
|---|---|---|---|
| A1 | `0x19` is EQ-coefficient-only (always targets User 1); no settings commit exists in this command | A payload/command sweep would show variants; reference source/other implementations show only payload 3 | Open |
| A2 | `0x19` has other payload values that commit settings (e.g. 1 = EQ, 2 = settings, 3 = both) | Vendor code, captures, or a documented payload table | Open |
| A3 | A separate command commits the settings record (unknown opcode) | Vendor app/web-bundle code, firmware notes, captures | Open |
| A4 | The device flushes settings to NVM only on USB detach/power-down, which is why the mic round trip worked | Volume-only runtime change + power cycle (no Save) and program-only change + power cycle | Open; **this is the cheapest discriminating test** |
| A5 | The host re-applies settings after reconnect and the "persistence" was host behavior | Trace the mic path: does it write after reconnect? (Code says no replay) | Largely excluded by code, worth confirming |

### Competing hypotheses for Goal B

| ID | Hypothesis | Distinguishing evidence | Status |
|---|---|---|---|
| B1 | `0x11` is not a JA11 command; the reference label is borrowed from Savitech CB5100 | Firmware notes, vendor bundle, other JA11 implementations | Open |
| B2 | `0x11` is correct but needs a different transport/interface/report type | Compare hidapi/WebHID usage in other implementations; inspect descriptors | Open |
| B3 | The reply exists but the parser rejects it (length/footer/report id) | Capture raw IN data from the unit (read-only) or compare parsers | Open |
| B4 | Firmware 2.20 lacks the feature although the chipset supports it | Vendor documentation/firmware history | Open |
| B5 | Filters are exposed through a different vendor interface or mode | Vendor app feature set for JA11 | Open |

## Phase 2 — external research

Three bounded read-only agents were dispatched (Luna after InferX rate limits): persistence/native
storage, digital-filter discovery, and transport/firmware. Findings are consolidated below when they
return; each item keeps its source and confidence.


## Phase 2 findings — official FiiO implementation (primary source)

Fetched fresh from `https://fiiocontrol.fiio.com/` on 2026-11-01: bundle
`static/js/index-7hb30NUo.js`, SHA-256
`4bc6204460a14434d9790fa1b016182fbf28bc2ec35d810f60abf091b9913d1e` (unchanged from the earlier
repository review). The minified bundle contains the HID command builders and a JA11 configuration.

**Frame builders:** `Ye(170,10,cmd,[payload])` = `AA 0A`, `Ye(187,11,cmd,…)` = `BB 0B`, with a
sequence byte, length, payload, checksum and `0xEE`. Confirmed commands: `0x01` vol max, `0x02`
volume, `0x04` filter mode R/W, `0x07` balance, `0x08` vol output, `0x0B` firmware, `0x10` screen
orientation, `0x12` mic (elsewhere), `0x15` PEQ band R/W, `0x16` EQ preset R/W, `0x17` global gain
R/W, `0x18` band count read, `0x19` **save with payload**, `0x1A` EQ switch, `0x1B` reset PEQ,
`0x21` factory reset.

**Save semantics — root cause of J034.** `setSaveEqToDeviceByCtrl()` calls
`Rk(this.eqPre.value)`, i.e. `0x19` with the **currently selected preset value**
(Vocal 0, Classic 1, Bass 2, User 1 3, Off 4). Our candidate always sent 3. Saving with Classic
active therefore wrote Classic's working curve into the User 1 slot and left the startup preset at 3
— exactly the J034 damage. The official flow also stages User 1 bands before saving (`isEqPreUser`)
and does not touch microphone or UAC.

**Digital filter — root cause of the missing reply.** The vendor reads `BB 0B 04` and writes
`AA 0A 04 [value]`; the mode enum is SD_SHARP=0, SHARP=1, SD_SLOW=2, SLOW=3, NO_OVERSAMPLING=4,
 labelled "Short Delay Sharp / Sharp Roll-off / Short Delay Slow / Slow Roll-off / Non-Oversampling".
`0x11` is a Savitech/CB5100-derived command from the third-party reference, not the JA11 vendor
command. Probing `0x11` on a unit that answers `0x04` explains the missing reply without proving the
firmware lacks filters.

**JA11 configuration in the bundle:** product id 109; presets vocal 0 / classic 1 / bass 2 / USER1 3 /
closeEq 4; PEQ band types PEAK, LOW_SHELF, HIGH_SHELF; global gain -12..+12 dB; custom preset naming
supported. No JA11-specific exclusion for filter mode was found, but the UI gating was not fully
resolved.

## Phase 3 — revised hypotheses

| ID | Hypothesis | Status after Phase 2 |
|---|---|---|
| A1 | `0x19` is EQ-only, constant payload | **Rejected**: payload is the destination preset |
| A2 | `0x19` payload selects the slot/commit target | **Adopted** (vendor source); selection persistence still needs physical proof |
| A3 | Separate settings-commit command | Still open; vendor save uses `0x19`, `0x16` sets the selection |
| A4 | Device flushes settings only on detach | Open; volume-only power-cycle test would decide |
| B1 | `0x11` borrowed from Savitech | **Adopted**: JA11 uses `0x04` |
| B2 | Wrong transport | Unlikely; vendor uses the same output/input report path |
| B3 | Parser rejects a valid reply | Possible for other commands, not the filter (wrong command id) |
| B4 | Firmware lacks filters | Open; a valid `0x04` read would refute it |


## Phase 2b — official vendor call path (2026-11-01, lead-agent direct analysis)

Verified identities before use: main bundle `index-7hb30NUo.js` SHA-256
`4bc6204460a14434d9790fa1b016182fbf28bc2ec35d810f60abf091b9913d1e` (unchanged); the Audio Config
route chunk `index-C70qb620.js` SHA-256 `c5f3d0bccce5154d745285b328cb5275122402dae003e7dea8f97a6b9a4d4f30`.
All 42 lazy chunks referenced by the bundle were fetched and searched, plus the main bundle.

Findings:

- `getFilterModeByCtrl` and `setFilterModeByCtrl` each appear **once**, as definitions in the main
  bundle. No lazy chunk and no template calls them; the `filterMode` state is never read by UI code.
- The Audio Config page renders only output volume, mic monitor volume, and the mic switch. It has no
  filter control and no JA11-specific branch.
- The JA11 configuration in the bundle covers presets (Vocal 0, Classic 1, Bass 2, User 1 3, Off 4),
  band types (PEAK/LS/HS), gain range (-12..+12), device id 109 — and no filter capability flag.
- The independent open-source JA11 driver (`adithyasource/fiiocontrol-oss`, pinned) contains no filter
  command either.

Conclusion: the vendor web app carries shared filter code that this version never exposes, so it
cannot be cited as evidence that the JA11 supports filters. It also does not prove the firmware lacks
them.


## Phase 2c — official Android app device table (decisive vendor evidence)

Source: official FiiO Control Android APK V4.6.0,
`https://fiio-firmware.fiio.net/FiiOControl/FiiOControl%20V4.6.0.apk`, 365,535,288 bytes, SHA-256
`ab0eb9cb4f6efd58a4e9b4edf3ad6ec11961b5e53d12fd6ecd1e80091730ac8d` (downloaded, not installed).
It is a Flutter app; the per-device capability data is a bundled SQLite database at
`assets/flutter_assets/assets/database/sc_config_default.db` (770,048 bytes).

`sc_product_info` maps **JadeAudio JA11** to `device_type = 109`. `sc_func_info` has the entry
`device_type 109`, `device_version 2.20` — exactly our unit — whose `function_msg` declares:

- `stateItems`: sample rate, `mic_detect` (cmd 18 / `0x12`), `uac_version_nui` (32 / `0x20`),
  `vol_output` (2 / `0x02`).
- `devicePeqInfo`: `eqMap` Vocal 0 / Classic 1 / Bass 2 / USER1 3 / closeEq 4; `bandCount` 5;
  `filterList` `[0,1,2]` (PEQ band types PEAK/LS/HS); `eqPreset` 22 (`0x16`); `masterGain` 23
  (`0x17`); `eqBand` 21 (`0x15`); `eqSwitch` 26 (`0x1A`, `support=false`); `eqResetCmd` 0.
- `settingItems`: device name (hidden), firmware upgrade, factory reset (`show=false`), instructions.
- `isSupportAudio = false`; `audioItems` empty; `deviceCommandInterval = 200` ms.
- **No filter-mode entry and no command `0x04` anywhere in the JA11 2.20 capability record.**

Interpretation: the vendor application does not expose a digital reconstruction filter for this
exact model and firmware. Combined with the unanswered `0x11` and `0x04` reads, this is strong
affirmative evidence that the feature is not available through the documented vendor protocol on this
device. It does not prove a hidden firmware capability is impossible — only the vendor can confirm
that — so the residual is stated rather than overstated.

Corroboration: the vendor table confirms our command mappings (`0x02`, `0x12`, `0x15`, `0x16`,
`0x17`, `0x20`), the five-preset model, and the 200 ms command interval our transport already uses.
## Phase 3b — ranked hypothesis matrix for the missing `0x04` reply

| ID | Hypothesis | Evidence for | Evidence against | Confidence | Lowest-risk test |
|---|---|---|---|---|---|
| A | Request never transmitted | – | The same transport sends `0x02`, `0x0B`, `0x16`, `0x15`, `0x19` successfully; framing is identical | Low | Raw probe diagnostic (verify request bytes emitted) |
| B | Transmitted, no response | Vendor command exists; no reply on two commands | Other reads answer | Medium | Raw probe length/timing capture |
| C | Response consumed elsewhere | Shared reader paths | One reader at a time; queue drained before send | Low | Raw probe with correlation fields |
| D | Response rejected by parsing | Decoder requires command at offset 4 and value 0..4 | No evidence of a valid-but-mismatched reply yet | Low–medium | Log raw bytes and decode offline |
| E | Undocumented prerequisite | Filters often need init/mode | Vendor UI never uses the command, so no known prerequisite | Low | Read-only probe after a full successful session read |
| F | Firmware does not expose it | No vendor UI caller, no OSS JA11 filter code, no firmware-note mention, no reply to two commands | Absence of evidence is not proof | Medium | Same probe; if no reply plus no vendor UI, F becomes the best explanation |
| G | Insufficient evidence | Current state | – | Current status | Probe, then decide |

Transport audit: the Android path selects one HID interface with one interrupt IN and one interrupt
OUT endpoint, drains up to eight queued reports, sends the request as an output report, polls with an
80 ms transfer timeout and 700 ms deadline, and accepts report-ID-prefixed or unprefixed replies.
The successful `0x02`/`0x0B`/`0x15`/`0x16`/`0x19` exchanges use the same path, framing, and timeout,
which makes A/C unlikely and leaves B/D/F.

## Phase 5b — Goal A vendor-ordering refinement (decision)

The vendor stages bands only for User 1; for built-ins it sends only `0x19` with the preset. Our
beta.3 stages the active bank + gain and applies once before saving. Analysis:

- The staged values are the device's own just-read working coefficients, so the extra writes were
  no-ops; J035 preserved the User 1 bank across four built-in saves.
- Removing the staging changes a physically qualified transaction. Whether `0x19` alone persists a
  built-in preset on this firmware has not been demonstrated.
- Vendor similarity alone is therefore insufficient to alter the working sequence. **Decision: retain
  the beta.3 transaction**; document the difference and propose a discriminating test (new candidate,
  built-in save without staging, power cycle, User 1 check).

## Goal B status and next evidence

Classification: **Device-specific unsupported (vendor-exposed capability)**. The official Android app's
JA11 2.20 device record declares every supported control and contains no filter mode or `0x04`; the
unit answered neither `0x11` nor `0x04`. Residual: a hidden firmware capability cannot be excluded
without vendor confirmation. No filter write has been attempted.

A read-only diagnostic was added for the next authorized session: the filter probe now logs the
request hex, response length and hex, session generation and stability, without user data. Candidate
`0.9.0-beta.4` / code 18 (APK `651100980a7d…`, source `5e28c9fb…`) contains only this diagnostic plus
the Goal A tests; it is **not installed** over the qualified beta.3.

Next minimal read-only step (requires authorization): launch beta.4, open My DAC, and capture one
`Ja11FilterProbe` log line plus one successful volume read as a transport control. That distinguishes
B, D and F without any write. If a valid reply appears, the full filter feature (selection, readback,
persistence, restoration) follows on a new qualification candidate.
## Phase 4 — off-device reproduction

- The stateful fake models runtime versus retained flash state and a simulated power cycle; it
  reproduces the J034 failure shape when the save payload is a constant 3 (the working curve lands in
  User 1 while the preset reverts).
- `everyProgramVolumeBoundaryAndMicBaselineSavesTheSelectedPreset` now asserts, for all five programs,
  that `0x19` carries the requested program code, plus counts, zero mic/UAC writes, and simulated
  power-cycle retention.
- Protocol tests assert the `0x04` filter read/write frames and values 0..4, and reject out-of-range
  replies and the old `0x16` confusion.

## Phase 5 — corrected implementation

- `FiioJa11Protocol.saveToFlashReport(program)` now sends `0x19` with the requested program code;
  the transport and repository pass the selected preset. The editor/Flash path keeps User 1.
- Digital filter discovery and writes use `0x04` with values 0..4; the enum, wire frames and tests
  were updated. The runtime capability gate is unchanged: no write until a valid read reply exists.
- The defaults transaction still reads the active bank fresh, restages it exactly, applies once,
  verifies, saves once and verifies the final state with one bounded re-enumeration.

## Phase 6 — verification

- 830 JVM tests pass, including the 16-case defaults suite and the filter codec tests; androidTest
  compiles. Full lint/build/R8/instrumentation gates still need a rerun on the final candidate.

## Phase 7 — minimal physical validation plan (owner-gated)

Preconditions: fresh baseline capture (volume, preset, User 1 bank, mic, UAC) and the exact candidate
hash. Steps 1–3 are read-only. Abort on any mismatch.

1. **Read-only filter probe:** one `0x04` read. If a valid 0..4 reply arrives, the control is real;
   record it and send no write in this step. If it fails, record the exact length/timing.
2. **Preset-bank tracking:** select each preset read-only and read its five bands. Confirm each
   preset exposes its own coefficients (the working area follows the selection). This is the safety
   precondition for step 4.
3. **Volume-only persistence (no save):** change volume only, power-cycle, read. Decides hypothesis
   A4 (does the device persist volume without any save?).
4. **Off preset save:** select Off, verify the bank is flat, one Set as Default (`0x19` payload 4),
   power-cycle, read. Confirms whether Save(preset) persists the startup preset with the least
   destructive slot.
5. **Classic preset save:** only if step 4 passes and step 2 showed per-preset banks. Select Classic,
   save (`0x19` payload 1), power-cycle, read; then select User 1 and verify the custom bank is
   untouched.
6. **Restoration:** restore the captured preset/volume/bank and verify across a power cycle.

Stop conditions: any unexpected bank change, any program/volume mismatch after a save, any filter
write with an invalid reply, or uncertain restoration. The vendor factory-reset command (`0x21`) is
recorded as a recovery option but is not authorized for use without explicit approval.

## Candidate B artifact and final assessment (2026-11-01, after J035)

| Item | Value |
|---|---|
| Candidate | `0.9.0-beta.3` / code `17` |
| Package | `com.weekssa.opraeqforuapp.v090beta` |
| APK SHA-256 | `f522c9448b4925164422daaca9ebb203af1db89108e3c3c5cdbcfe0ac29be6a4` |
| Source | `db35f4290ae40f4e19720e99497951ab5c3a9bf2` |
| Signer | local test `0f89797ba477006eea3e1ee837eaf654e687a14fc50b5a213b40312cdd1f869f` |
| Physical record | `docs/FIIO_JA11_VALIDATION_LEDGER.md` J035 |

**Goal A — EQ program and volume persistence: PASS (physical, all five presets).**

Vocal, Classic, Bass, User 1 and Off were each saved with `0x19` carrying the selected preset,
survived a genuine USB power removal and cold reattachment, and left the User 1 bank and global gain
unchanged. Saved volume persisted; runtime-only volume did not (the unit returns to its stored pair).
No microphone or UAC write occurred and no reconnect workaround was used.

Limits: one unit, one firmware (2.20), one host. Volume was qualified at 60 (with earlier J034 runs
at 40/35); other volumes are not individually qualified. A vendor-ordering deviation remains: built-in
saves still restage the active bank and Apply before `0x19`, which the vendor does only for User 1;
the staged values were the device's own readback, so the writes were no-ops, but the exact ordering
should be aligned in the next candidate.

**Goal B — digital filters: device-specific not supported (vendor-exposed).**

The official Android app's JA11 2.20 device record declares every supported control and contains no
filter mode or `0x04`; the unit also answered neither `0x11` nor `0x04`. That is strong affirmative
evidence that the feature is not available through the vendor protocol on this device. Residual: only
the vendor could confirm whether a hidden firmware capability exists. No filter write was attempted.

**Open refinement:** implement the vendor ordering for built-ins (selection → `0x19` only, verify by
readback) and re-validate Off/Classic on a new identity.

## Independent review (2026-11-01)

A bounded read-only Luna review of the correction and J035 found no blocking defect and two P2s, both
fixed: a protocol test now encodes `saveToFlashReport(program)` for all five presets (guarding against
a future constant-payload regression), and stale `0x11` comments were corrected to `0x04`. The rebuilt
APK is byte-identical (`f522c944…`), so the physically verified identity remains valid. 831 JVM tests
pass.

## Current status (2026-11-01, after vendor call-path analysis)

| Goal | Status | Basis |
|---|---|---|
| A — EQ persistence | **PASS** on beta.3 (J035) | All five presets and volume persisted across genuine power removal; User 1 untouched; no mic/UAC writes; restoration verified |
| A — vendor refinement | **Retained as-is** | Removing staging is unverified on this firmware; a discriminating test is proposed |
| B — digital filters | **Device-specific unsupported (vendor-exposed)** | Vendor Android JA11 2.20 capability table has no filter mode/`0x04`; no reply to `0x11` or `0x04`; web app defines but never calls filter methods |

Diagnostic candidate: `0.9.0-beta.4` / code 18, APK `651100980a7d2361e5fc5ec486ae309e4ddaf8bb59f98409883281f3f53deebe`,
**not installed**. It adds only the raw `Ja11FilterProbe` log (request/response/length/session) plus
tests; the qualified beta.3 remains the installed build.

## Final evidence review (2026-11-01)

The focused vendor-chain verification and the per-goal statuses are consolidated in [CANDIDATE_B_READINESS.md](CANDIDATE_B_READINESS.md). Summary: Goal A PASS on beta.3 (J035); Goal B classified device-specific not supported (vendor-exposed), based on the exact JA11 2.20 record in the official Android app (no filter entry), the unused web helpers, and the unanswered 0x11/0x04 reads. beta.4 was evaluated and not recommended for installation.
