# EW300 / JA11 parity — local experimental build

Accepted owner plan, 2026-11-05. Persisted verbatim as the task contract source of truth.

## 1. Executive decision

Extend the working EW300 implementation through the existing My DAC shell, editor, session, and transaction framework. Preserve qualified five-band Peak behavior while adding **session-only Apply, runtime output gain, explicit persistent Save, clearer DEVICE status, About information, and capability-driven filter controls**.

Include native Low Shelf and High Shelf as gated research. Your blinded listening result may qualify them for this local experiment, with the label **“Experimental · owner-listening-qualified; unmeasured.”** It will not establish calibrated shelf response, exact Q semantics, or JA11 acoustic equivalence.

Decisions established in this planning conversation:

| Decision | Selected behavior |
|---|---|
| Editor Apply | Runtime changes, verified by readback; zero Save commands |
| DEVICE output gain | Runtime change; explicit Save persists the current EQ and gain together |
| My EQs / EQ Library Flash | Preserve the existing persistent transaction |
| Reset to flat | Preserve the existing persistent Reset, with clear confirmation |
| Native shelves | Prepare gated support and an owner listening test |
| Successful listening test | Permit local experimental controls with bounded claims |
| Production | Existing Peak-only capability and persistence behavior remain behind their current policy |

**Planning evidence:** remote `main` was verified twice at `180f4a962a53d22d294311121dd49d92a570d89a`. Local `main` is `30054466fa110adfaa9f9b2058eb8af700fbc56d`, 67 commits behind. Source inspection used the verified remote tree. The untracked planning packet remains preserved.

Workflow selftest passed; OpenCodex was healthy; the active planning model was verified as `gpt-6.1-sol`. Two explicitly named DeepSeek investigations and one GLM corroboration completed read-only. No application tests, builds, emulator operations, APK installations, hardware requests, or file edits occurred.

## 2. Current-state regression baseline

The maintained EW300 evidence distinguishes accepted hardware behavior from implemented features that still have narrower evidence.

| Behavior | Current baseline and preservation requirement |
|---|---|
| Exact-device authorization | Preserve the complete allowlisted fingerprint, revision, HID descriptor/interface checks, and session generation fences |
| Five-band Peak readback | Preserve direct-Hz frequency, signed tenths-of-dB gain, Q thousandths, and all five band pairs |
| Edit / Review | Preserve immutable working copy, baseline comparison, blocking validation, safe-gain assessment, and explicit review |
| Apply | Currently persistent; change only under the experimental policy |
| Flash | Preserve staging, volatile verification, one Save, replacement-session handling, and final readback |
| Reset | Preserve the qualified flat target and persistent transaction; do not reinterpret it as factory reset |
| Reconnect | Preserve sole exact replacement, fresh permission/open/claim, newer generation after detach, and no mutation replay |
| Personal EQ capture | Preserve Peak capture and provenance; playback gain remains device state rather than canonical EQ preamp |
| Output gain | Preserve stereo channel handling and −64.0–0.0 dB / 0.5 dB envelope |
| Reports | Preserve truthful operation counts and `null`/unmeasured fields |
| UI | Preserve current/last-read distinctions, connection recovery, and operation feedback |

Accepted hardware records remain **E001, E037–E040, and E043–E046**, tied to their original source/artifact tuples. They are not repeated because the source SHA changes.

Two qualifications must remain explicit:

- End-to-end Personal EQ capture UX has an outstanding physical evidence gap.
- The standalone output-gain control is implemented, but its independent physical qualification is pending. E001 establishes playback-gain persistence within its recorded transaction.

Preserve and extend these tests:

`Ew300ProtocolTest`, `Ew300FlasherTest`, `Ew300EditorApplyTest`, `Ew300MutationExecutorTest`, `Ew300ReconnectGateTest`, `Ew300CapabilityProfileTest`, `Ew300CapabilityBatchTest`, `Ew300DeviceControlsTest`, `Ew300OperationTraceTest`, `Ew300QualificationExportTest`, `Ew300PersistenceQualificationTest`, `Ew300ShelfCorpusRegressionTest`, `Ew300HardwareEqMatchTest`, `Ew300CapturedEqTest`, `Ew300DeviceStatusTest`, and `Ew300OperationStatusTest`.

Run the existing EW300-focused suite before implementation and preserve its results separately from later candidate results.

## 3. Complete JA11 feature inventory and EW300 disposition

**Established** below means existing EW300 evidence supports the named behavior. **Experimental** means new software or physical qualification is required. **Unknown** means insufficient evidence, not proven hardware absence.

| JA11 surface / interaction | EW300 now | Experimental disposition | Evidence |
|---|---|---|---|
| Device heading and connection state | Present | Preserve; align with shared status components | Established |
| EQ / DEVICE tabs | Present while connected | Keep visible for connected and last-read states; disable mutations when unavailable | Software change |
| Refresh / Read | Present | Preserve one authoritative session; no independent connection path | Established |
| Device volume, 0–60 | Different control: Output gain | Keep “Output gain,” dB units, and conservative range; runtime Apply | Experimental persistence split |
| EQ programs | One observed bank | Describe “One five-band EQ bank”; no program selector | Multiple programs unknown |
| Tuning / Ellyn | No equivalent established | Exclude; document register collision | Out of scope |
| Startup settings | No separate status | Add “Saved settings” with session-scoped status and explicit Save | Experimental |
| Microphone / headset remote | No control established | Omit switches; explain evidence limit in capability details | Unknown |
| USB Audio Class selection | No control established | Omit mode selector | Unknown |
| Current sample rate | No accepted current-rate read path | Omit; descriptor-supported rates are not current playback rate | Unknown |
| About: model | Descriptor identity | Display observed model | Established read-only |
| About: firmware | New dump reports `1.0.0` | Display only after fresh bounded read and parser verification | New read-only evidence |
| About: USB identity | Available | Display VID/PID, revision, and interface; protect serial in shareable reports | Established |
| About: validation | Exact profile available | Separate qualified Peak baseline from experimental features | Established + explicit limits |
| EQ program/status heading | Active Peak count | Show current five-band bank, freshness, and persistence separately | Software change |
| Response graph | Present | Reuse; shelf graph labelled predicted/unmeasured | Established Peak; shelf hypothesis |
| Per-band values | Less prominent than JA11 | Add five compact type/frequency/gain/Q rows | Software change |
| Edit current EQ | Shared editor | Preserve | Established |
| Editor type selection | Shared UI hardcodes Peak/LS/HS, despite EW300 rejection | Generate options from capability policy; normal EW300 exposes Peak only | Confirmed UI/domain mismatch |
| Frequency / gain / Q | Shared fields | Preserve Peak ranges; experimental shelf restrictions below | Established Peak |
| Safe gain / headroom | Shared evaluator, using playback gain | Preserve evaluator; distinguish playback gain from dedicated EQ preamp | Established software |
| All Bands | Shared | Preserve | Established |
| Review | Shared | Show runtime versus persistent consequence before action | Software change |
| Apply | Persistent | Runtime only in experiment | New transaction |
| Reset to flat | Persistent | Preserve; confirmation explicitly says it saves flat EQ to DAC | Established |
| Capture readback | Peak-only | Preserve Peak capture; native shelf capture deferred | Implemented, evidence-bounded |
| Flash from saved/library EQ | Present | Preserve existing persistent path | Established |
| Operation result and reports | Present | Extend operation names and status; retain readable/JSON access | Established |
| Accessibility | Existing semantics | Extend new rows, type restrictions, persistence, and recovery states | Emulator qualification |

Additional capability decisions:

| Capability | Decision |
|---|---|
| Peak limits | Five bands; 20–20,000 Hz; −12.0–+12.0 dB; Q 0.1–10.0 |
| Peak quantization | 1 Hz, 0.1 dB, 0.001 Q |
| Source shelves | Preserve complete-response optimization to five Peaks |
| Native Low/High Shelf | Dormant initially; local experimental exposure after listening qualification |
| Initial shelf envelope | Cut-only −6.0–0.0 dB; Q fixed at 0.707; begin with 250 Hz Low Shelf and 4,000 Hz High Shelf |
| Wider shelf frequency/Q/boost support | Deferred; listening at two settings does not qualify the full parameter space |
| EQ enable at `0x24` | Read-only; no guessed on/off writes |
| LPF / HPF | Raw type history only; no product exposure |
| Reconstruction filter | Insufficient EW300 evidence; exclude |
| Balance / gain mode / output topology | Insufficient evidence; exclude |
| Firmware update / erase / calibration / recovery | Out of scope |

## 4. Byte-level protocol and register evidence

### Transport and commands

Exact EW300 identity: `31B2:0111`, manufacturer `LE XIAN`, product `SIMGOT EW300 DSP`, revision `1.01`, HID interface **3**, interrupt IN `0x82` and OUT `0x02`, maximum packet size **16 bytes**. Vendor report `0x4B` contains a **10-byte payload**, producing an **11-byte report**.

```text
Byte       0      1..4           5        6          7..10
Request    4B     address LE32   command  reserved   value LE32
```

For ordinary register reads, the reply echoes the requested address and `0x52`; value bytes are 7–10. Byte 6 must not be described as the proven write/Save acknowledgment: family references place acknowledgment values in the returned value field.

| Command | Meaning | Task policy |
|---|---|---|
| `0x52` | Register read | Only hardware command permitted by the Mac probe harness |
| `0x57` | Register write | Product allowlist only; physical approval required |
| `0x53` | Persist live configuration | Explicit Save/Flash/Reset only; one attempt per transaction |
| `0x43` | Family handshake / clear terminology varies | Never send in this task |
| `0x08` | Firmware-dependent extended memory read | Document as adjacent research; never send |

The current Android writer establishes output transfer completion and subsequent register readback. It **does not parse a qualified EW300 write/Save ACK**. Family values `0x03` and `0x4F` must remain family evidence until an exact EW300 response is captured.

Preserve current timing initially:

- Transfer timeout 300 ms; response window 700 ms; polling transfer interval 80 ms.
- Register reads: up to three attempts, 100 ms between attempts.
- Write settle: 200 ms; Save settle: 1 second.
- Replacement reconnect deadline: 20 seconds; permission fallback: 10 seconds.
- Final readback: up to four read-only attempts with 250 ms gaps.
- No write or Save retry.

### Band encoding

For band `i = 0..4`, registers are `A = 0x26 + 2i`, `B = A + 1`.

```text
A = ((frequency_Hz & 0xFFFF) << 16) | (signed_gain_tenths & 0xFFFF)
B = ((type & 0x7) << 16) | (Q_thousandths & 0xFFFF)

A wire bytes = gain low, gain high, frequency low, frequency high
B wire bytes = Q low, Q high, type, reserved
```

| Type code | Family label | Exact EW300 disposition |
|---|---|---|
| 0 | Peak | Qualified product baseline |
| 1 | Low pass | Historical raw echo/restoration only |
| 2 | High pass | Historical raw echo/restoration only |
| 3 | Low Shelf | Historical raw echo/restoration; listening qualification pending |
| 4 | High Shelf | Historical raw echo/restoration; listening qualification pending |

`0x66` uses signed 8-bit half-dB steps in active channel bytes. With protocol flag bit `0x0200` clear, preserve stereo L/R behavior; unequal channels must not silently collapse into one scalar.

### Complete register-space disposition

The saved [256-register dump](/Users/stephenweeks/.codex/evidence/ew300/2026-11-04/register-dump.json) has no misses and SHA-256:

`40a40c97ff3f4b3400a33e7877837048121b54bdd739e658b61248f3466256b5`

The following table covers `0x00–0xFF` by named ranges plus a default rule. Values are from that saved snapshot, not a new live probe. Raw values are observed; most control meanings outside the accepted EQ path remain family interpretations.

| Registers | Observed value / decoded content | Meaning and confidence | Write policy |
|---|---|---|---|
| `00` | `3A474552` / `REG:` | Observed identity marker | Never |
| `01` | `00000001` | Layout flags; stereo interpretation used by existing codec | Never |
| `04–05` | `2E302E31, 00000030` | Firmware string `1.0.0`; parser verification required | Never |
| `08–0D` | `206C754A, 32203120, 00343230, 0, 323A3531, 39343A34` | Build date/time | Never |
| `10–11` | `36366235, 00363938` | Build identifier `5b66896` | Never |
| `18` | `1` | Family ADC EQ enable | Never |
| `1A–23` | A words `03E80000,07D00000,13880000,1F400000,27100000`; every B `000002C3` | ADC bank, family interpretation | Never |
| `24` | `1` | Observed DAC-bank state; bit semantics unresolved | Never |
| `26–2F` | Every A `03E80000`; every B `000003E8` | Five flat Peak bands, 1 kHz / 0 dB / Q 1 | Guarded EQ transactions |
| `36–37` | `66726550, 3A676643` | `PerfCfg:` marker, adjacent interpretation | Never |
| `3A–3B` | `5, 13` | EW300 PGA fields; not JA11 Ellyn | Never |
| `40–47` | `5820454C,004E4149`, then zeros | Manufacturer string | Never |
| `48–4F` | `474D4953,4520544F,30303357,50534420`, then zeros | Product string | Never |
| `50–56` | `34323032,2D37302D,302D3330,2D303030,30303030,3030302D,00000030` | Serial string; private continuity evidence | Never |
| `5B` | `011131B2` | Packed PID/VID | Explicitly forbidden |
| `61` | `2A2A2A31` | Configuration content; semantics unqualified | Never |
| `64` | `1` | Unknown configuration word | Never |
| `65` | `0` | Family ADC digital gain | Never |
| `66` | `0` | DAC L/R digital gain | Guarded gain/headroom transactions |
| `67–69` | `8000F2F4,000007D0,01470001` | Unknown configuration | Never |
| `71–73` | `3CE2DAD8,0014000A,01F4000A` | Family noise-gate interpretation | Never |
| `78–7A` | `000000FD,012C000A,0064000A` | Family limiter/configuration interpretation | Never |
| `E1` | `12345678` | Observed magic | Never |
| **Every address not listed above, through `FF`** | **`00000000`** | **Observed zero; semantics unknown, not “unused” or writable** | **Never** |

The execution evidence packet will mechanically expand this into one row per address, retaining raw value, interpretation provenance, confidence, and write policy.

### Golden fixtures

| Fixture | Exact bytes | Provenance / expected result |
|---|---|---|
| Read band 1 A | TX `4B 26 00 00 00 52 00 00 00 00 00` | Existing protocol TX fixture |
| Historical stock read | RX `4B 26 00 00 00 52 00 F5 FF 64 00` | Accepted historical raw response |
| Historical temporary write | TX `4B 26 00 00 00 57 00 F6 FF 64 00` | Accepted reversible test |
| Temporary readback | RX `4B 26 00 00 00 52 00 F6 FF 64 00` | Exact historical readback |
| Historical restore | TX `4B 26 00 00 00 57 00 F5 FF 64 00` | Restoration readback equals historical stock response |
| Flat 1 kHz Peak A | TX `4B 26 00 00 00 57 00 00 00 E8 03` | Codec-derived golden; not a new physical capture |
| Flat Q/type B | TX `4B 27 00 00 00 57 00 E8 03 00 00` | Q 1.000 / Peak |
| Research LS B | TX `4B 27 00 00 00 57 00 C3 02 03 00` | Q 0.707 / type 3; acoustic hypothesis |
| Research HS B | TX `4B 27 00 00 00 57 00 C3 02 04 00` | Q 0.707 / type 4; acoustic hypothesis |
| Save | TX `4B 00 00 00 00 53 00 00 00 00 00` | Existing qualified request; exact ACK remains unknown |

Convert historical positive RX examples into sanitized automated fixtures. Do not synthesize an “observed” ACK.

## 5. Flash and transaction state machine

Use the existing exclusive mutation owner and repository-owned state. Compose must not own pending hardware targets.

```text
Fresh baseline
    → validate reviewed target and session
    → write volatile target
    → authoritative same-session readback
    → Runtime verified

Runtime verified
    → explicit Save
    → fresh comparison with pending target
    → one 0x53 attempt
    → expected reconnect, if observed
    → authoritative final readback
    → Save verified in this session

Any uncertain mutation
    → State unknown
    → bounded read-only reconciliation
    → no replay / no automatic second Save
```

Required operation distinctions:

- `APPLY_RUNTIME`: bands plus required safe gain; zero Save.
- `SET_GAIN_RUNTIME`: guarded stereo gain change; zero Save.
- `SAVE_RUNTIME`: fresh comparison, one Save, reconnect and final verification; no hidden restaging.
- `FLASH_PROFILE`: preserve the current complete persistent profile transaction.
- `RESET_FLAT`: preserve current persistent Reset.
- `RESTORE_BASELINE`: separately authorized restoration transaction.

Retain safer-gain-before-bands ordering when attenuation increases, and delay gain increases until the target bands are verified.

Persistence states:

| State | User-facing meaning |
|---|---|
| Unknown | Current startup persistence has not been established |
| Runtime only | This app verified an unsaved live change |
| Saving | One persistent Save is in progress |
| Saved in this session | Save and final readback verified in this operation |
| Save not verified | Save may have completed; read before another action |

Ordinary reconnect, process death, external mutation, or ambiguous identity invalidates pending Save authority and cached saved-state claims. Expected Save re-enumeration retains only the operation-bound target needed for final verification.

Add a successful-runtime completion path to the reconnect gate. Today, ending a mutation without `markSaveSent()` leaves automatic reconnect blocked; a verified runtime operation needs a distinct healthy completion, while failed pre-Save operations remain fenced.

Reconcile playback-gain bookkeeping in the same slice: explicit absolute gain changes must rebase/invalidate the tracked EQ-gain delta so a later profile Flash cannot subtract stale compensation twice.

Before Save, deterministic failure may use the existing bounded volatile restoration only while the exact session remains trustworthy. After uncertain transfer, unexpected detach, or Save uncertainty, stop writes. Read-only reconciliation must not become an automatic restoration Save.

## 6. UI/UX design concept

Reuse the existing My DAC workspace, tabs, graph, shared editor, Review, value rows, operation header, and report components. No new navigation root or duplicate JA11 screen.

```text
SIMGOT EW300 DSP
Connected / Last read / Permission required                [Refresh]

[ EQ ] [ DEVICE ]

EQ
Current EQ · Five-band bank
[shared response graph]
1  Peak       1,000 Hz    0.0 dB    Q 1.000
2  Peak       ...
3  Peak       ...
4  Peak       ...
5  Peak       ...

Output gain                                  −6.0 dB
[Edit current EQ] [Reset to flat]
[Save readback as Personal EQ]

Saved settings
Runtime only · EQ and gain have not been saved       [Save]
[operation result / report access]
```

```text
DEVICE
Device settings                                           [Refresh]

Audio
Output gain                                  −6.0 dB      >
Equalizer                         Five bands · Peak only
Saved settings                             Runtime only  [Save]

Connection
Current / Last read / Reconnecting

About
Model                                  SIMGOT EW300 DSP
Firmware                               1.0.0 · read from DAC
USB identity                           31B2:0111 · revision 1.01
Validation                             Qualified Peak profile
Experimental features                  Runtime Apply / shelf status
```

Copy and behavior:

- Editor Apply helper: **“Applies for this session. Save to DAC to keep it after unplugging.”**
- DEVICE Save helper: **“Saves the current EQ and output gain. Audio may briefly disconnect.”**
- Reset confirmation: **“Reset and save the DAC EQ to flat?”**
- Unknown capabilities belong in concise capability details: **“App controls for presets, microphone, UAC switching, and tuning have not been established for this EW300.”**
- Shelf graphs say **“Predicted response · unmeasured”**; never imply measured agreement.
- Do not expose ordinary shelf capture or source-derived native shelf Flash in this scope. Peak capture and existing shelf-to-Peak optimization remain available.

All states retain meaningful content: loading, offline catalog, no readback, stale snapshot, disconnected, permission denied, reconnecting, verification failure, and recovery. Last-read values remain labelled and non-actionable.

Use 48 dp targets, merged row semantics, heading semantics, explicit value/unit announcements, accessible textual band values, and state announcements independent of color. Verify light/dark, landscape, and system font scale 1.5× and 2×.

## 7. File-level implementation and interfaces

All paths below refer to the future isolated worktree; current inspection was against the pinned remote tree.

| Subsystem | Exact source targets and changes |
|---|---|
| Codec / authorization | [Ew300Protocol.kt](/Users/stephenweeks/Documents/Codex/OPRA-EQ-development/app/src/main/java/com/weekssa/opraeqforuapp/domain/ew300/Ew300Protocol.kt), [Ew300CapabilityProfile.kt](/Users/stephenweeks/Documents/Codex/OPRA-EQ-development/app/src/main/java/com/weekssa/opraeqforuapp/domain/ew300/Ew300CapabilityProfile.kt): add an explicit experimental policy; retain production defaults and exact fingerprint |
| Transactions | [Ew300EditorApply.kt](/Users/stephenweeks/Documents/Codex/OPRA-EQ-development/app/src/main/java/com/weekssa/opraeqforuapp/domain/ew300/Ew300EditorApply.kt), [Ew300Flasher.kt](/Users/stephenweeks/Documents/Codex/OPRA-EQ-development/app/src/main/java/com/weekssa/opraeqforuapp/domain/ew300/Ew300Flasher.kt), [Ew300TransactionCoordinator.kt](/Users/stephenweeks/Documents/Codex/OPRA-EQ-development/app/src/main/java/com/weekssa/opraeqforuapp/domain/ew300/Ew300TransactionCoordinator.kt): split runtime verification from explicit Save; reuse existing staging/restoration/reconciliation |
| Session / repository | [Ew300ReconnectGate.kt](/Users/stephenweeks/Documents/Codex/OPRA-EQ-development/app/src/main/java/com/weekssa/opraeqforuapp/data/dac/Ew300ReconnectGate.kt), [Ew300MutationExecutor.kt](/Users/stephenweeks/Documents/Codex/OPRA-EQ-development/app/src/main/java/com/weekssa/opraeqforuapp/data/dac/Ew300MutationExecutor.kt), [HardwareEqRepository.kt](/Users/stephenweeks/Documents/Codex/OPRA-EQ-development/app/src/main/java/com/weekssa/opraeqforuapp/data/hardware/HardwareEqRepository.kt): healthy runtime completion, serialized Save, repository-owned pending target |
| Gain bookkeeping | [Ew300GainStatePreferences.kt](/Users/stephenweeks/Documents/Codex/OPRA-EQ-development/app/src/main/java/com/weekssa/opraeqforuapp/data/kt02h20/Ew300GainStatePreferences.kt), [Ew300GainQualification.kt](/Users/stephenweeks/Documents/Codex/OPRA-EQ-development/app/src/main/java/com/weekssa/opraeqforuapp/domain/ew300/Ew300GainQualification.kt): prevent stale baseline/delta reuse after runtime changes |
| Shared capability / editor | [HardwareEqDeviceSpecs.kt](/Users/stephenweeks/Documents/Codex/OPRA-EQ-development/app/src/main/java/com/weekssa/opraeqforuapp/domain/hardware/HardwareEqDeviceSpecs.kt), [HardwareEqEditor.kt](/Users/stephenweeks/Documents/Codex/OPRA-EQ-development/app/src/main/java/com/weekssa/opraeqforuapp/domain/dac/HardwareEqEditor.kt), [HardwareEqSnapshotFactory.kt](/Users/stephenweeks/Documents/Codex/OPRA-EQ-development/app/src/main/java/com/weekssa/opraeqforuapp/domain/dac/HardwareEqSnapshotFactory.kt): consistent permitted types across render, validation, encode, and snapshot |
| UI state / actions | [EqLibraryViewModel.kt](/Users/stephenweeks/Documents/Codex/OPRA-EQ-development/app/src/main/java/com/weekssa/opraeqforuapp/ui/EqLibraryViewModel.kt), [MyDacEditorUiState.kt](/Users/stephenweeks/Documents/Codex/OPRA-EQ-development/app/src/main/java/com/weekssa/opraeqforuapp/ui/MyDacEditorUiState.kt): separate runtime/Save results, freshness, capability options; operations remain in ViewModel scope |
| Screens | [Ew300MyDacContent.kt](/Users/stephenweeks/Documents/Codex/OPRA-EQ-development/app/src/main/java/com/weekssa/opraeqforuapp/ui/screens/Ew300MyDacContent.kt), [MyDacEqEditorScreen.kt](/Users/stephenweeks/Documents/Codex/OPRA-EQ-development/app/src/main/java/com/weekssa/opraeqforuapp/ui/screens/MyDacEqEditorScreen.kt), [DacDeviceStatus.kt](/Users/stephenweeks/Documents/Codex/OPRA-EQ-development/app/src/main/java/com/weekssa/opraeqforuapp/ui/screens/DacDeviceStatus.kt): capability-driven chips, persistence, About, reusable internal row primitives |
| Trace / metadata transport | [Ew300OperationTrace.kt](/Users/stephenweeks/Documents/Codex/OPRA-EQ-development/app/src/main/java/com/weekssa/opraeqforuapp/domain/ew300/Ew300OperationTrace.kt), [AndroidEw300UsbTransport.kt](/Users/stephenweeks/Documents/Codex/OPRA-EQ-development/app/src/main/java/com/weekssa/opraeqforuapp/data/kt02h20/AndroidEw300UsbTransport.kt): truthful operation kinds, bounded metadata reads, optional raw ACK observation without changing acceptance policy |
| Build isolation | [app/build.gradle.kts](/Users/stephenweeks/Documents/Codex/OPRA-EQ-development/app/build.gradle.kts): debug-derived `ew300Parity`, package suffix `.ew300parity`, updates disabled, existing local debug signer |

Add internal operation/persistence types rather than a public API. No canonical schema or Room migration is required. Pending operation authority is session-scoped and must not survive process death.

Use separate flags for parity, qualification-only shelf access, and listening-qualified shelf editor exposure. All default/release flags remain false. The prequalification candidate permits fixed shelf test targets only through the private test harness; ordinary editor shelves remain unavailable.

Rollback is to stop using the separate experiment package and return to the existing app. Uninstalling the experiment does not restore DAC settings; hardware restoration must complete first.

## 8. Test and qualification ladder

The EW300 remains attached to the Mac until readiness is complete.

| Level | Execution check | Acceptance / limitation |
|---|---|---|
| Baseline | `./tools/codex-android :app:testDebugUnitTest --tests '*Ew300*'` | Existing tests pass before changes; does not prove current hardware |
| Golden protocol | Extend `Ew300ProtocolTest` with positive RX, signed boundaries, type/reserved fields, stereo gain, malformed/foreign replies | Byte contract; no acoustic claim |
| Transactions | Extend Apply/Flasher/Coordinator/MutationExecutor/Reconnect tests | Runtime zero Save; explicit Save one; no replay; stale/external change rejects |
| Gain | Controls and gain bookkeeping regressions | No double compensation; no unintended EQ persistence from runtime gain |
| Capabilities | Editor/spec/snapshot tests | Normal EW300 Peak-only; JA11 retains its types; experimental gates consistent |
| Mac probe | New `/Users/stephenweeks/Documents/Codex/OPRA-EQ-development/tools/ew300_readonly_probe.py` | Command allowlist contains only `0x52`; exact identity; complete dump; raw frames/hash; stop on mismatch |
| Mac harness selftest | `python3 tools/ew300_readonly_probe.py --self-test` | Fake HID proves it cannot emit `57/53/43/08/54` or boot commands |
| Mac live read | `python3 tools/ew300_readonly_probe.py --capture --output <private-evidence-dir>` | Future authorized execution only; safe current values may differ from saved snapshot |
| Measurement corpus | AutoEq/squig source audit and existing shelf-corpus regression | Preserve fitting behavior; public curves cannot qualify native type codes |
| Compose | Extend DEVICE/editor tests; add `Ew300ParityFlowTest` | Real domain transaction + fake transport behind screen callbacks; runtime/Save and error states exercised |
| Build | `./tools/codex-android :app:testDebugUnitTest :app:lintDebug :app:assembleDebug :app:assembleEw300Parity :app:assembleEw300ParityAndroidTest` | Coherent candidate; APK identity/hash/signature recorded |
| Emulator | Install/instrument with explicit `adb -s emulator-…` | UI, fake-transport flows, lifecycle, accessibility; no hardware claim |
| Production isolation | Release compilation plus policy tests | Experimental behavior unreachable in release; no release publication |
| Pixel | Explicit physical serial only after readiness | Separate package install/launch; read-only first |
| EW300 physical | Approved plan in section 9 | New behavior only; exact restoration required |

Do not use an unqualified connected-test command that might target the Pixel accidentally. Emulator instrumentation must name the emulator serial and treat instrumentation failures as failures even if the shell exits zero.

A full app-wide fake USB session does not currently exist. This scope uses real domain transactions with fake transport plus Compose integration, and does not claim end-to-end Android USB emulation.

Optional passthrough gets one bounded feasibility attempt after checking the installed emulator. Official notes document a QEMU USB-host option, but do not establish that this Mac/emulator/device combination can claim the EW300. If unavailable, record the reason and continue with Mac HID plus fake transport. No host-driver workarounds or repeated claim attempts. [Android emulator release notes](https://developer.android.com/studio/releases/emulator)

## 9. Bounded owner listening and persistence session

This is a proposed future physical protocol, not authorization to execute hardware writes now.

**Entry:** candidate frozen and hashed; local gates passed; rollback material prepared; exact test plan accepted; owner ready; one exact EW300; no competing controller.

**Owner actions:** connect the EW300 to the Pixel when requested, use a comfortable low listening level, keep the same earpieces/nozzles/insertion, listen to prepared local clips, answer plain-language prompts, and perform requested genuine power removals. The agent operates the app and interprets reports.

### Baselines and runtime qualification

1. Capture the complete original live register baseline and descriptor fingerprint.
2. With approval, power-cycle and capture the stored-startup baseline separately. Live and stored state may differ; neither may overwrite the other’s record.
3. Apply one cut-only Peak target and a bounded output-gain attenuation using the new runtime paths. Verify complete EQ/gain readback and **zero Save attempts**.
4. Power-cycle. Require return to the captured stored baseline. Failure means the “session-only” claim remains unqualified; stop that promotion.

### Native shelf listening

Use a fixed baseline with other bands flat, constant output gain, Q `0.707`, and no positive boosts:

- Low Shelf: 250 Hz, −6 dB, type 3.
- High Shelf: 4,000 Hz, −6 dB, type 4.
- Peak control: an attenuation-only Peak demonstrating that audio is passing through the controlled DAC path.

Pause or mute playback while applying each target; require exact readback before resuming. Keep transition timing identical so clicks/delays are not the identification cue.

Prepare randomized flat-versus-filter trials with the order hidden from the listener. Use bass-, mid-, and treble-rich passages so responses record **which region changed**, not merely that something differed.

Predeclare **12 scored trials per shelf; at least 10 correct per shelf**, plus a direction/region description consistent with low- or high-frequency attenuation. This is an experimental acceptance rule, not a calibrated response measurement or probability guarantee. Do not rerun a failed block merely to obtain a pass.

A pass supports audible native-filter behavior at the tested settings. It does not validate arbitrary frequency, gain, Q, sample rate, or JA11 equivalence.

### Persistent Save and restoration

After runtime and listening checks pass, an explicitly approved combined shelf target may serve as the **single new Save-current qualification**, avoiding a repeat of the established Peak Flash tests:

1. Verify the target and companions fresh; send one Save.
2. Reconcile expected re-enumeration; verify final state.
3. Perform genuine power removal; require exact target readback.
4. Restore the captured stored-startup EQ/gain using one separate restoration Save; power-cycle and verify.
5. If the original live state differed, restore that live state without Save and verify it separately.

Maximum: **one target Save and one restoration Save**. No Save during listening trials. No persistent shelf exposure if only volatile listening passes.

Stop on identity mismatch, ambiguous candidate, permission loss, unexpected detach, uncertain transfer, mismatched readback, unexpected sound, or restoration failure. Never retry an uncertain Save. A failed session preserves evidence and returns to diagnosis.

## 10. Fresh research packet and documentation updates

Research was performed on **November 4, 2026, America/Chicago**. Findings below are additions for the existing research log; no planning files were edited.

| Query / inspection | Source, class, license | Finding / confidence / decision |
|---|---|---|
| `"SIMGOT EW300 DSP" protocol firmware teardown register`; EW300 manual/app/firmware searches | Vendor/product searches; third-party results excluded as protocol authority | No authoritative EW300 HID specification, firmware package, or exact-die teardown located in this pass; bounded negative only |
| KT02H20 vendor page | [KTMicro](https://www.ktmicro.com/?usb_104/394.html), vendor, proprietary page | DSP and flash-configurable processing supported at family level; no EW300 register specification |
| KT02H20/KT0211L/KT02F20 protocol and save searches | [ktmicro-tools](https://github.com/gxcreator/ktmicro-tools), community, BSD-3-Clause; [kt0231h](https://github.com/eyad-elghareeb/kt0231h), community, no license found | Family framing/type/save corroboration; no exact EW300 promotion; no unlicensed code reuse |
| Five-band implementations | [devicePEQ](https://github.com/jeromeof/devicePEQ), community, 0BSD; [bunnyctl protocol](https://github.com/wzwz5200/bunnyctl/blob/main/docs/PROTOCOL.md), adjacent device, MIT | Strong implementation corroboration for type 3/4 and explicit Save; different device/firmware |
| JA11 Apply/Save patterns | [ja11-config](https://github.com/Ircama/ja11-config), license not established for reuse; [Cyfine control](https://github.com/Cyfine/ja11-web-control), community MIT | Useful UX/behavior references; JA11 bytes do not transfer |
| `"KT02H20" flash endurance write cycles 0x53` | Vendor/family search | No exact EW300 endurance limit established; minimize saves; do not invent a cycle budget |
| EW300 versus DSP measurements | [AutoEq](https://github.com/jaakkopasanen/AutoEq), community measurement corpus; [SupOrSalad](https://suporsalad.squig.link/), measurement publisher | Cross-check source curves and historical direct-Hz analysis; no controlled native-shelf proof located; dataset licenses checked per artifact before reuse |
| EW300 USB lifecycle | [lokal regression note](https://github.com/lokal-music/mobile/blob/main/docs/audio/simgot-ew300-usb-dac-regression.md), adjacent app, GPL-3.0 | Lifecycle hypotheses only; its PID conflicts with observed `0111`; no code copying |
| Android USB / ADB / emulator | [USB host](https://developer.android.com/develop/connectivity/usb/host), [ADB](https://developer.android.com/tools/adb), platform documentation | Permission/detach and explicit-target lifecycle requirements; no UAC2 or passthrough support inferred for this exact unit |

**Research correction:** the adjacent bunnyctl project reports a run-mode firmware dump on its TANCHJIM firmware, contradicting a universal “software firmware readback is impossible” statement. For EW300, the correct statement is **“no validated firmware backup path established.”** Do not attempt `0x08` to investigate it.

**Evidence correction:** the saved EW300 dump contains register values, not proof that it issued writes or Save. Historical transaction records establish those primitives.

During execution:

- Update [FUNCTIONAL_MAPPING.md](/Users/stephenweeks/Documents/Codex/OPRA-EQ-development/docs/planning/ew300-ja11-parity/FUNCTIONAL_MAPPING.md) after every code slice and test run.
- Append to [TEST_EVIDENCE_LOG.md](/Users/stephenweeks/Documents/Codex/OPRA-EQ-development/docs/planning/ew300-ja11-parity/TEST_EVIDENCE_LOG.md) after every relevant run, including failures.
- Append this research pass to [RESEARCH_SOURCES.md](/Users/stephenweeks/Documents/Codex/OPRA-EQ-development/docs/planning/ew300-ja11-parity/RESEARCH_SOURCES.md), preserving exact queries, inspected revisions, license checks, limits, and decisions.
- Fold durable changes into EW300 protocol notes, capability matrix, validation ledger, and runbook in the same slice.
- Preserve historical records and supersede by reference. Separate “qualified on an earlier artifact” from “verified on this candidate.”

Each evidence entry records source SHA and dirty-tree identity when applicable, command, environment, exact APK/hash/signer, test-plan version, fingerprint, raw artifact paths/hashes, bytes or operation trace, result, claim proved, claim not proved, Save count, restoration state, and mapping rows updated.

## 11. Risks, sealed-gate proposal, and execution contract

Principal risks are the Apply/Save split, stale gain compensation, reconnect release after a healthy no-Save operation, stale saved-state claims, unqualified shelf semantics, and accidental experimental exposure. Each has an explicit software regression and a bounded physical qualification above.

Create a new local branch `codex/ew300-ja11-parity` in an isolated worktree under:

`/Users/stephenweeks/.codex/worktrees/ew300-ja11-parity/OPRA-EQ-development`

Do not modify the saved checkout’s existing JA11 gate. Seed a new EW300 contract in the new worktree, using the accepted plan and `tools/seed-task`; do not hand-write a seal. Refresh remote state before execution and compare any new base with the inspected SHA.

The contract owns only the named implementation/test/documentation files, new read-only probe and evidence-verification tools, test fixtures, and its own plan/gate/progress records. Expand these to exact paths before sealing; no directory-wide production ownership.

Propose **five failing-capable gates**:

| Gate | Required check |
|---|---|
| 1. Baseline and contract | Workflow selftest; execution model/effort; base resolution; recorded baseline suite; valid seal and scope-guard |
| 2. Protocol and transactions | Focused/full JVM suites; positive golden RX; runtime zero Save; explicit Save one; reconnect, cancellation, identity, stale gain, and no-replay negatives |
| 3. UI and build isolation | Explicit-emulator Compose/fake-transport results; accessibility captures; variant policy tests; coherent build/lint |
| 4. Readiness and evidence | Read-only Mac capture; APK/package/version/hash/signer manifest; complete mapping/log cross-links; executable evidence verifier rejects missing/failed/mismatched results |
| 5. Independent review | Exactly one fresh `gpt-6-luna` review from the other account; findings closed; same reviewer confirms only those fixes |

Physical approval and the section 9 protocol are separate constraints, not a disguised software gate. A local-ready result may be handed off as **physical qualification pending**.

Use a proposed cap of **12 execution turns**. The execution start must obtain the actual review stop time and run `flow-check --turns 12 --stop <HH:MM>`; no unlimited continuation or automatic cap extension.

The writer is one fresh `commandcode/deepseek-deepseek-v4.1-flash` thread at high effort. Before its first write, verify the execution configuration and restate OWNS and gates. Register the final reviewer in the gate before spawning it. Run scope-guard before every local commit.

Local implementation, test, build, and emulator iteration continues through diagnosed failures without per-test permission requests. Stop only for the stated material choice, unauthorized physical action, destructive action, hard blocker, contradictory evidence, or gate limit. No push, PR, merge, signing-policy change, public release, or public hardware-support claim is included.

Planning preflight reported NORMAL capacity, Command Code pool approximately 10.84% used, global fallback closed and OpenAI hard-lock fallback available. Recheck capacity and fallback at execution start; restore the DeepSeek global default before opening that fresh thread.

## 12. Recommended scope, final readiness checklist, and next action

**Recommended experiment:** Peak-preserving My DAC parity, capability-correct editor options, runtime EQ/gain, explicit persistent Save, truthful saved-state reporting, read-only About, and dormant native shelf research. Promote only the tested shelf settings to local experimental use after the accepted listening and persistence evidence.

Before requesting the Pixel:

- [ ] Read-only Mac harness documented, self-tested, and captured successfully.
- [ ] Golden/protocol and complete applicable JVM tests pass.
- [ ] Explicit-emulator UI/fake-transport and accessibility checks pass.
- [ ] Debug experiment APK built, hashed, signed, and identified.
- [ ] Production flags and existing persistent paths verified.
- [ ] Independent review complete.
- [ ] Original-state capture/restoration procedure and rollback artifacts prepared.
- [ ] One-page listening guide and physical plan accepted, or session restricted to reads.
- [ ] EW300 can be moved safely from Mac to Pixel.
- [ ] Wireless ADB targets the exact Pixel; recovery skill used only if discovery/authorization fails. USB fallback is arranged before DAC testing.

**One next action:** review and accept this plan as the local experiment’s contract. The accepted plan is then sealed and handed to a fresh DeepSeek execution thread in the isolated worktree.
