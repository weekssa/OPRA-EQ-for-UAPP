# Release handoff - Black Pearl and JA11 remediation

Status: **FINAL CLOSEOUT AUTHORIZED.** The exact Black Pearl AFUL Explorer retest is
`PHYSICAL_PASS` on the exact `e1ab5fa` candidate after the owner adjusted the DAC volume. JA11's
software remediation is complete; its physical claim remains bounded by the maintained ledger.
The earlier pre-adjustment failure and superseded candidate records remain below as history.

## Superseded exact signed candidate — historical owner Pixel 9 gate

This was the prior owner-review candidate and is retained for provenance only. Do not install or
use it for final closeout; the authoritative final candidate and result are recorded in the
`Final 0.7.0 closeout handoff — authoritative summary` section below. It was a non-public testing
artifact and did not establish physical qualification or public hardware support.

- Repository: `https://github.com/weekssa/OPRA-EQ-for-UAPP.git`
- Owner-authorized minimum main integration completed at source SHA
  `2cba1322245221103b8790bfcef705a38022c2ed`; the later handoff commit is documentation-only.
- Candidate target: `black-pearl-ja11`.
- Workflow: [Signed EQ Library Beta Candidate #36368698803](https://github.com/weekssa/OPRA-EQ-for-UAPP/actions/runs/36368698803),
  successful for `candidate_target=black-pearl-ja11`.
- APK: `EQ-Library-v0.7.0-beta-2cba132.apk`.
- APK SHA-256: `50cee56aa59a8980a61bff42625fe9d839a28189068ba5adddd1a03530ea02d6`.
- Package/version: `com.weekssa.opraeqforuapp`, `0.7.0` / code `7`.
- Signer certificate SHA-256: `65c1c1256dae3c49e3548f334c91f0ba991969e9be9e0b223ba4e253d2114747`.
- Signature verification: v2 PASS; v3 PASS; one signer; RSA 4096-bit certificate.
- Immutable artifact ID/digest: `10948632352` /
  `sha256:3e9fb31a814a8feb30ff0c48ac69a71cddd579e71b90401ddf34e491b8fc45ab`.
- Emulator diagnostics artifact ID/digest: `10948497670` /
  `sha256:fd92f4bf31c57616580ccfab399e4d13ef4f15973f5463154740fbcb4a6ebda2`.
- Exact candidate URL:
  `https://raw.githubusercontent.com/weekssa/OPRA-EQ-for-UAPP/mobile-test-apk/candidates/EQ-Library-v0.7.0-beta-2cba132.apk`.
- Signed emulator install: PASS (`Success`). Cold launch: PASS (`Status: ok`, `LaunchState: COLD`,
  `Activity: com.weekssa.opraeqforuapp/.MainActivity`, `Complete`).
- Before the owner-authorized physical session, Luna did not connect, mutate, flash, apply, reset,
  save, restore, or otherwise operate a DAC.

## Owner-authorized Pixel 9 result — Black Pearl / AFUL Explorer

This addendum supersedes the pre-session checklist below for the exercised Black Pearl path.

- Exact candidate: `b61f02e8c91656f14ffc639e4c6d937b2162a1b7`, APK SHA-256
  `276587734fc863277b83e4310c040cee22c27261075e21fa75d766ded9eef27e`, workflow #1372,
  immutable artifact `10941292707` / `sha256:188d2e4b973e7aab420d4c7d3890dba3407415c1d2c8f935a418acdacdbd8fc3`.
- Pixel: Google Pixel 9 / `tokay` / API 37 / wireless serial
  `[Pixel TLS-connect alias redacted]`.
- DAC identity: TTGK Technology TE-C, VID `0x3302`, PID `0x43E8`, serial `330243E8260129`.
- Baseline: app-connected, verified Flat, 10 filters, active slot 1, playback gain `-25.00 dB`.
- One authorized action: My EQs -> AFUL -> Explorer -> Flash. No second mutation was attempted.
- Result: **PHYSICAL_FAIL** for requested-state verification. Terminal UI reported
  `Final hardware readback did not confirm the requested EQ` and
  `expected raw -7398, actual raw -6400`.
- The app correctly withheld success and instructed reconnect/refresh before any later hardware
  action. The existing ViewModel performed its normal read-only post-operation refresh; no retry,
  Reset, Save, Restore, reconnect, or owner-directed recovery write was performed, so restoration
  is **NOT VERIFIED** and the requested post-operation state remains unqualified.
- The Black Pearl truthfulness defect's failure-path assertion is **PASS**; the Direct Flash
  application/restoration gate is **FAIL**. Do not label Black Pearl physically fixed or publicly
  supported.

### Read-only recovery result

The owner-authorized recovery closed and reopened the app session, tapped `Connect` once, and
performed no hardware write. Fresh My DAC readback showed `Verified current hardware`,
`Matches My EQs` for `Explorer`, 10 filters, active slot 1, and playback gain `-25.00 dB` / raw
`-6400`; the requested target raw gain was `-7398`. This confirms partial PEQ application but not a
complete verified Flash or restoration.

## Latest authorized write-side diagnostic — not a candidate

After the read-only protocol capture, the owner authorized exactly one write-side diagnostic Flash.
The isolated debug package used the catalog AutoEq/Jaytiss Explorer profile, not the earlier
Hifigues community Explorer target. The one operation's post-operation My DAC surface showed
`Verified current hardware`, `Matches My EQs`, 10 filters, active slot 1, and playback gain
`-31.00 dB`.

This run is not a signed beta, not an exact-candidate result, and not evidence that the earlier
`-7398` gain mismatch is repaired. The temporary raw logger used the wrong byte position and
captured no write transfer result. The temporary package and instrumentation were removed; the
original signed package was left installed. No Reset, retry, Save, Restore, or second mutation was
performed. Current state is **REPAIR_REQUIRED**, not `READY_FOR_PIXEL_9`; no branch push is
recommended.

The physical Black Pearl state after this diagnostic was not restored by Luna because the approved
boundary prohibited an automatic restore. The owner should treat the device as containing the
AutoEq/Jaytiss diagnostic state until a separately authorized, exact restoration or follow-up test
is chosen.

## Historical pre-candidate source boundary — `9f5cb852994e1c88fce80598f249a97fae047429`

This section preserves the pre-integration evidence. It is superseded by the exact candidate
section above; its old `MERGE_APPROVAL_REQUIRED` state and missing-candidate statements do not apply
to the merged source.

- Refreshed `origin/main`: `acaf4dd32ddd9379ec2860e45e34fb8039219583`.
- Final local source SHA: `9f5cb852994e1c88fce80598f249a97fae047429`.
- Local debug APK SHA-256: `79e75c4a39e3a0aeb8b7231643ca4306e29db3578c23124028402b2945ac5fc6`.
- Local unsigned minified release APK SHA-256:
  `945e330a1eb510d68405603f19f50770f06abda6b931c77ce9dbe33d6d40d746`.
- Package/version: `com.weekssa.opraeqforuapp`, `0.7.0` / code `7`.
- Signer, workflow run, immutable artifact ID/digest: `NOT AVAILABLE` for this source.
- `actionlint`: `NOT RUN` because it is unavailable locally.
- No push, merge, tag, publication, DAC connection, or DAC mutation occurred in this follow-up.

### Software evidence for this source

- Full unit suite: PASS.
- API-36 emulator `codex-api36`, serial `emulator-5554`: focused JA11 terminal UI 4/4 PASS;
  full instrumentation 24/24 PASS.
- `lintDebug`, `assembleDebug`, `assembleRelease`, and R8 mapping verification: PASS.
- Independent read-only review: PASS; see `04-final-review.md`.

### Owner-controlled next step

The next action is not a hardware test. The owner must separately authorize the minimum main
integration or a narrowly reviewed combined-manifest/signing workflow path so the trusted workflow
can create an exact signed candidate for `9f5cb852994e1c88fce80598f249a97fae047429`. Until then,
the state is `MERGE_APPROVAL_REQUIRED`, not `READY_FOR_PIXEL_9`.

## Historical pre-candidate software handoff and prior physical evidence

- Repository: `https://github.com/weekssa/OPRA-EQ-for-UAPP.git`
- Branch: `codex/black-pearl-ja11-remediation-20260927`
- Refreshed live base: `origin/main` `0adcc8159a467790104cf2dc797f1279ef2c53ed`
- Final implementation source SHA: `dc6478a25b1745b4f78f833c69e96e066c615d56`
- Promotion-preparation source SHA: `0f080516e8d4aa34e9d0a04b16464bf65b2b6d7d`
- Reviewed implementation/workflow head: `0c77e081fd6abd12a6e20b482ce269ae9f5bb764`
- Final documentation evidence head before owner-approved merge: `3e8ff5d3f6cb750a627f76aba50f09645c0b41d3`
  (documentation-only commits atop the reviewed implementation/workflow head).
- Owner-approved merge commit on `main`: `acaf4dd32ddd9379ec2860e45e34fb8039219583`.
- Review PR: [#49](https://github.com/weekssa/OPRA-EQ-for-UAPP/pull/49), owner-approved and merged;
  all required remote review checks passed on the final exact head.
- Worktree: `/Users/stephenweeks/.codex/worktrees/black-pearl-ja11-remediation-20260927/OPRA-EQ-for-UAPP`
- Black Pearl issue: software PASS; success remains final-readback verified and anti-stacking
  uncertainty is fail-closed.
- JA11 issue: software PASS; User 1-only editor path is complete through exact transaction and
  one-Save/final-readback result.
- Focused/full unit tests, lint, debug assembly, API 36 instrumentation, clean install/cold launch,
  large-text launch, and UI hierarchy smoke: PASS.
- Remote Android CI `#1874`, CodeQL `#1758`, priority-community `#1627`, catalog currentness
  `#2142`, and dependency submission `#2231`: PASS on the final documentation evidence head.
- Signed beta workflow run [#1371](https://github.com/weekssa/OPRA-EQ-for-UAPP/actions/runs/36336477287):
  PASS for `candidate_target=black-pearl-ja11` and exact source `acaf4dd32ddd9379ec2860e45e34fb8039219583`.
- Candidate APK: `EQ-Library-v0.7.0-beta-acaf4dd.apk`; SHA-256
  `af83a5e0148263057b1c43e3b775157ab6aedd9d2c1e3ab0558cef8fa3cea665`.
- Package/version: `com.weekssa.opraeqforuapp`, `0.7.0` / code `7`.
- Signer SHA-256: `65c1c1256dae3c49e3548f334c91f0ba991969e9be9e0b223ba4e253d2114747`.
- Immutable artifact ID/digest: `10937890771` /
  `sha256:88b1555e9a14642874110d05d5c5ae3554c25cc64b9385898a78287ccea9f52d`.
- Exact candidate: `https://raw.githubusercontent.com/weekssa/OPRA-EQ-for-UAPP/mobile-test-apk/candidates/EQ-Library-v0.7.0-beta-acaf4dd.apk`.
- Signed emulator install and cold launch: PASS; `Status: ok`, `LaunchState: COLD`.
- Hardware before the controlled session: NOT RUN by Luna. No DAC was connected or mutated.

## Pixel 9 controlled JA11 session — 2026-09-27

The exact signed candidate above was installed and verified on a Google Pixel 9 (`tokay`, Android
17/API 37). The FiiO JA11 was identified as VID/PID `0x2972/0x0102` in Android USB host mode.

- Baseline: User 1, five PEAK bands at 80/250/1000/4000/12000 Hz, all `+0.00 dB`, Q `0.70`,
  global EQ gain `0.00 dB`.
- Apply: one explicit reviewed change of Band 1 to `-1.00 dB`; final current hardware readback
  showed `-1.00 dB` and all other values unchanged.
- Restore: one explicit reviewed change of Band 1 back to `0.00 dB`; final current hardware
  readback showed the original flat state.
- USB host connection count: `273 -> 275 -> 277` across the two reconnect boundaries.
- The Android prompt to open USB Audio Player PRO appeared after each reconnect; the owner canceled
  both prompts. UAPP was not allowed to take ownership.
- Installed candidate SHA-256 after the session:
  `af83a5e0148263057b1c43e3b775157ab6aedd9d2c1e3ab0558cef8fa3cea665`.
- Evidence: `/tmp/opra-pixel-automated.ZRPv17/` (`editor-apply1-review.xml`,
  `apply1-after-cancel.png`, `editor-restore-review.xml`, `restore2-after-cancel.png`, and USB
  snapshots).

Classification: JA11 immediate readback and original-state restoration **PASS**; complete physical
qualification **PHYSICAL_INCONCLUSIVE** because the routing dialog obscured transient terminal
feedback and the post-dialog surface did not retain an observable editor success sentence or expose
an exported operation trace proving the exact Save count. Black Pearl remains **NOT EXERCISED**.
This does not establish power-cycle persistence, acoustic fidelity, or public hardware support.

## Why this stops before broader physical qualification

The trusted main-only boundary has been satisfied by owner approval. The exact signed candidate and
its provenance are now verified above. The remaining boundary is owner physical validation only.
This handoff does not claim either issue physically fixed or publicly supported. The bounded JA11
session has now occurred, but the evidence boundary above must be resolved or explicitly accepted by
the owner before `OWNER_ACCEPTED`.

## Pixel 9 checklist after an exact signed candidate exists

This checklist is actionable for the owner. Use only the exact candidate APK and record every result
against its source SHA, APK SHA-256, signer certificate, workflow run, and immutable artifact ID.

### Before connecting either DAC

- Confirm package `com.weekssa.opraeqforuapp`, version/code, signer, source SHA, and APK checksum
  match the immutable candidate manifest.
- Install cleanly or upgrade only after capturing the pre-test app/device state.
- Confirm the app identifies the exact TRN Black Pearl or FiiO JA11 identity; stop on ambiguity,
  permission prompt during mutation, disconnect, competing operation, or session replacement.
- Capture the baseline report and current state before any owner-authorized mutation.

### Black Pearl bounded review

- Use the existing approved exact Black Pearl profile and Direct Flash path only.
- Confirm progress text says the final hardware state is being verified.
- Confirm success appears only when all ten native fields and raw global gain match in final
  readback, with the compact verified wording.
- If any readback is missing/mismatched or the session changes, classify as NOT VERIFIED and do not
  retry automatically. Confirm later Flash/Reset is blocked until a fresh authoritative baseline.
- Restore the original hardware state using the approved owner procedure and verify restoration.

### FiiO JA11 bounded review

- Confirm current program is User 1. For Off/Vocal/Classic/Bass, confirm no User 1 coefficients are
  presented as current and Edit EQ is unavailable.
- With a fresh verified User 1 read, open Edit EQ and make local changes. Confirm no transport write
  occurs during open, edit, reset-local-edits, Review, Back, Close, or Cancel.
- Confirm Review lists all five type/frequency/gain/Q values, global EQ gain, headroom consequences,
  warnings, and that Apply is the first write.
- Apply once. Confirm exact five-band write, quantized global gain, User 1 selection, Apply, volatile
  readback, exactly one Save/reconnect boundary, final readback, and truthful success/failure.
- Stop on any mismatch, timeout, disconnect, permission prompt, or replacement session. Do not retry
  the mutation. Restore the original User 1 state and verify it.

### After the session

- Export readable and technical reports, checksum each artifact, record restoration and each DAC's
  independent result, and classify `PASS`, `FAIL`, or `INCONCLUSIVE`.
- Do not call either issue physically fixed or publicly supported from software/emulator evidence.
- Use `06-post-pixel-closure-prompt.md` for the next bounded closure update.

## Read-only Black Pearl diagnosis addendum — 2026-09-27

The owner-authorized read-only capture completed on the same Pixel 9 and exact Black Pearl identity
after the failed AFUL Explorer Flash. The diagnostic session reached Connected, refreshed My DAC,
and captured native reads without Flash, Reset, Apply, Save, Restore, or retry. Evidence is in
`/tmp/opra-black-pearl-diagnostic-20260927-195236/`; the filtered log SHA-256 is
`5250ef7845041f55facea336790e65940d3a37dd7d2b8ffbee84bf6f678f11bf` and the full log SHA-256 is
`7512e300995553cb4093bd4a1820e5b32558b7cea590b49b42994c6657003b04`.

The decisive read was global gain request `4B 80 03 ...` and response `4B 80 03 02 00 E7 FF FF ...`,
which decodes to raw `-6400` / `-25.00 dB`. This matches the prior failure's observed final gain
and the refreshed My DAC state. The maintained codec/read path is corroborated, but no write-side
defect is proven. The temporary diagnostic changes were reverted, so the branch has no new source
fix and no new beta candidate to push. The exact signed candidate remains a truthful-failure
candidate only; it must not be presented as a successful Black Pearl Flash candidate.

Current handoff status: `REPAIR_REQUIRED`. Do not request another hardware write from this evidence.
A future write-side diagnosis would require a separately authorized, exact operation with explicit
stop conditions; Luna did not mutate hardware during this diagnostic capture.

## Current engineering blocker — endpoint path requires owner decision

The supplied working reference uses the Black Pearl HID interrupt OUT endpoint when available and
falls back to HID `SET_REPORT` otherwise. The exact Black Pearl descriptor captured in this run has
OUT endpoint `0x05`; OPRA currently ignores it and uses `controlTransfer` for every write. This is
the leading software hypothesis for the earlier global-gain mismatch, but changing the endpoint
path is explicitly outside the current remediation scope.

Status is **REPAIR_REQUIRED / BLOCKED — boundary requires owner decision**. There is no signed beta,
no pushable source fix, and no Pixel 9 handoff. The required decision is whether to authorize one
narrow source change that uses the existing interrupt OUT endpoint for Black Pearl writes, preserves
the current HID reports/timing/retry policy, and retains control-transfer fallback when OUT is absent.

## Current source checkpoint — signed candidate still pending

The owner has now authorized the exact narrow endpoint-path change. The implementation is committed
at `eb761f1bc510a612acde7b71b453631e1ff23a8f` on
`codex/black-pearl-ja11-remediation-20260927`. It uses an interrupt OUT endpoint when present,
retains the existing HID `SET_REPORT` fallback, requires a complete 64-byte interrupt transfer, and
preserves report bytes, timing, read retry, session, and no-mutation-retry behavior. Focused/full
unit tests, API-36 emulator instrumentation, lint, debug assembly, release lint, release/R8
assembly, and final independent review are complete and passing.

This does **not** create a signed beta. The local debug APK and unsigned minified release APK are
development artifacts only; no signer, workflow run, or immutable artifact ID exists for this SHA.
The separately authorized non-public branch was pushed after the software gates for implementation
source `eb761f1bc510a612acde7b71b453631e1ff23a8f`; no signed beta resulted. No DAC was connected
or mutated by Luna during this source change.

Current handoff status: **SOFTWARE_READY_PENDING_SIGNED_CANDIDATE**. Do not use the local APK for
Pixel 9 hardware qualification. The next required boundary is an exact signed candidate tied to
this source SHA through the trusted release path; only then can the owner decide whether to begin the
final Pixel 9 review. This source result is not a physical-fix or public-support claim.

## Exact signed candidate — owner Pixel 9 gate

The owner authorized minimum main integration at candidate source SHA
`2cba1322245221103b8790bfcef705a38022c2ed`; the later handoff commit is documentation-only, so the
candidate remains pinned to that source. The trusted workflow
`Signed EQ Library Beta Candidate` completed **PASS** as run
[#36368698803](https://github.com/weekssa/OPRA-EQ-for-UAPP/actions/runs/36368698803) with
`candidate_target=black-pearl-ja11`.

Candidate provenance:

- APK: `EQ-Library-v0.7.0-beta-2cba132.apk`
- SHA-256: `50cee56aa59a8980a61bff42625fe9d839a28189068ba5adddd1a03530ea02d6`
- Package/version: `com.weekssa.opraeqforuapp`, `0.7.0` / code `7`
- Signer certificate SHA-256: `65c1c1256dae3c49e3548f334c91f0ba991969e9be9e0b223ba4e253d2114747`
- Immutable signed APK artifact: ID `10948632352`, digest
  `sha256:3e9fb31a814a8feb30ff0c48ac69a71cddd579e71b90401ddf34e491b8fc45ab`
- Candidate manifest: exact source SHA, combined target, package/version, APK checksum, signer, R8
  mapping, and combined Black Pearl/JA11 capability profile all verified.
- Signed emulator: Android 35 `opra_signed_beta`; install `Success`; package identity/version
  checks passed; cold launch `Status: ok` and `LaunchState: COLD`.
- Diagnostics artifact: ID `10948497670`, digest
  `sha256:fd92f4bf31c57616580ccfab399e4d13ef4f15973f5463154740fbcb4a6ebda2`.
- The existing `mobile-test-apk` testing branch contains the exact candidate checksum under
  `candidates/`; this is not a public release or support claim.

## Pre-physical handoff status: READY_FOR_PIXEL_9

The owner may now perform the final exact-candidate Pixel 9 review. Use only the candidate above and
record every result against its source SHA, APK SHA-256, signer, workflow run, and artifact ID.

Before connecting either DAC:

1. Confirm the installed APK matches the exact candidate tuple above.
2. Capture the baseline state and confirm the exact DAC identity/session.
3. Test Black Pearl and JA11 independently; do not infer one result from the other.
4. Stop on a disconnect, permission prompt during mutation, session replacement, stale/missing
   readback, or any result that is not explicitly verified. Do not retry a mutation automatically.
5. Restore the original state using the approved owner procedure and record restoration evidence.

This is an owner physical-validation handoff. It is not a claim that either DAC is physically fixed
or publicly supported. Luna did not connect to or mutate a DAC.

## Latest exact-candidate Black Pearl result — superseding physical disposition

The owner-authorized test used the exact candidate above with corrected provenance: source
`2cba1322245221103b8790bfcef705a38022c2ed`, APK `EQ-Library-v0.7.0-beta-2cba132.apk`, installed
SHA-256 `50cee56aa59a8980a61bff42625fe9d839a28189068ba5adddd1a03530ea02d6`, workflow
`36368698803`, and immutable artifact `10948632352` /
`sha256:3e9fb31a814a8feb30ff0c48ac69a71cddd579e71b90401ddf34e491b8fc45ab`.

On Pixel 9 `tokay`, one AFUL Explorer (Hifiguides/Jaytiss Latest) Direct Flash was submitted after
a fresh read-only baseline. The app reported `Flash result could not be verified`; final native
playback-gain readback was expected raw `-8934` and actual raw `-8960`. The app instructed reconnect
or refresh before any later hardware action and no retry, Reset, Save, Restore, or second mutation
was performed. The requested state and restoration are **NOT VERIFIED**.

Current handoff status: **REPAIR_REQUIRED** for Black Pearl. The software truthfulness gate is
PASS, but the interrupt-OUT transport candidate did not produce a verified complete Flash. JA11
must remain independently classified; no public hardware-support claim follows. The next step is
deterministic source-side diagnosis and a new exact signed candidate, not another owner write on
this candidate.

## Current engineering boundary after deterministic diagnosis

The source-side trace is reproducible and unchanged: baseline raw `-7936`, AFUL Explorer preamp
`-3.90 dB`, rounded 256-unit delta `-998`, expected raw `-8934`, and final readback raw `-8960`.
The supplied independent Black Pearl implementation corroborates the report framing, interrupt-OUT
transport, and raw/256 representation, but does not prove a global-gain quantization rule. The
current source and focused tests therefore remain truthfully fail-closed; no tolerance widening,
readback substitution, retry, wire-byte change, or speculative rounding has been made.

There is no new candidate for Pixel testing. Leave the Black Pearl untouched. Owner support is
needed only after a proven source correction has passed the full software gates and produced a new
exact signed candidate with immutable provenance.

## New repair checkpoint — exact candidate not yet available

The current software repair is implementation commit
`7183d2c5beb76dca697e8497ba15b5c7627a320b`, based on `origin/main`
`62584133bbd78228f26abe69bc8ab65c76be2cd5`. Local gates passed: full unit tests, API 36
`codex-api36` instrumentation 24/24, lint, debug assembly, minified release/R8 assembly, mapping
verification, and `git diff --check`. `actionlint` is **NOT RUN** because it is unavailable locally.

The repair is limited to native whole-dB global/preamp planning for direct Black Pearl Flash/editor
headroom. It preserves the signed 16-bit 1/256 wire representation, report bytes/order/timing,
transport, identity, session, retry policy, canonical EQ, and file export. No DAC was connected or
mutated for this checkpoint.

The signed-beta workflow remains main-only. Therefore the exact candidate tuple is currently:

- source SHA: **NOT AVAILABLE** for a signed candidate; implementation checkpoint is
  `7183d2c5beb76dca697e8497ba15b5c7627a320b`;
- APK, SHA-256, signer, workflow/run, immutable artifact ID/digest: **NOT AVAILABLE**;
- package/version target remains `com.weekssa.opraeqforuapp`, `0.7.0` / code `7` once produced;
- candidate target must be the combined `black-pearl-ja11` profile and must not be labeled JA11-only.

Current status: **MERGE_APPROVAL_REQUIRED**. The owner must authorize the minimum main integration
of the reviewed branch tip before the trusted main-only workflow can create an exact signed beta.
After provenance is complete, the owner’s next routine intervention is one exact-candidate Pixel 9
review; no hardware action is requested before that.

The final reviewed branch tip after the file-export wording follow-up is
`1393150ba6cfff7a0b73c385f564a57d29c157a3` plus the durable documentation commit that records this
handoff. No exact signed artifact exists for either tip because the trusted workflow remains
main-only.

## Current source gate — `3f818d89`

The latest pushed branch tip is `3f818d89`, based on refreshed `origin/main`
`62584133bbd78228f26abe69bc8ab65c76be2cd5`. The editor whole-dB regression and the maintained
file/Flash representation wording are complete. Unit, lint, debug, release/R8, API-36 emulator,
clean-install/cold-launch, UI-dump, diff, and gate-lint checks are passing; `actionlint` is **NOT
RUN** because it is unavailable locally.

This is not yet a Pixel handoff. The exact signed candidate tuple (source SHA, APK filename and
checksum, signer, workflow run, immutable artifact ID/digest) is **NOT AVAILABLE** because the
trusted signing workflow is main-only. The mandatory independent read-only review of `3f818d89` is
also the final software gate still in progress. Current status is **MERGE_APPROVAL_REQUIRED**.

Owner action required, after independent review returns PASS: reply exactly
`Authorize minimum main integration for 3f818d89`. That approval authorizes only the minimum
main-integration step needed to bind the trusted signed-beta workflow to this combined
`black-pearl-ja11` source; it does not authorize hardware mutation, publication, tagging, or a
public support claim. Luna has not mutated hardware.

## Final software handoff boundary — reviewed tip `59caced95234aab494bdd44fb9a8beb5bb045e2c`

Independent review is **PASS** for the final pushed branch tip
`59caced95234aab494bdd44fb9a8beb5bb045e2c`. The production/test source is
`3f818d89`; the reviewed tip adds only documentation and handoff-record closure after that source.
The exact signed candidate tuple remains **NOT AVAILABLE** because signing is trusted and main-only.
All local software/emulator gates are green. This is not `READY_FOR_PIXEL_9` and is not a physical
qualification or public support claim.

Owner action required: reply exactly
`Authorize minimum main integration for production source 3f818d89 from reviewed branch tip 59caced95234aab494bdd44fb9a8beb5bb045e2c`.
That authorizes the minimum main integration needed to create the combined `black-pearl-ja11`
signed candidate. It does not authorize hardware mutation, publication, tagging, or a public claim.
After the exact signed artifact is proven, the next owner intervention will be one exact-candidate
Pixel 9 review. Luna has not mutated hardware.
## Exact signed beta — READY_FOR_PIXEL_9

The authorized minimum main integration completed at source SHA
`e1ab5fa5a65dc2d64624d871ac53d436f792ea6a`. The trusted workflow
[#36375994853](https://github.com/weekssa/OPRA-EQ-for-UAPP/actions/runs/36375994853) completed
**PASS** with the combined `black-pearl-ja11` target.

Use only this exact candidate for the owner Pixel 9 review:

- APK: `EQ-Library-v0.7.0-beta-e1ab5fa.apk`
- APK SHA-256: `7fffba26f32991ce8c936f725bdc6c3c4b6956d3a0800c539b8d45e6b52a2501`
- Package/version: `com.weekssa.opraeqforuapp`, `0.7.0` / code `7`
- Signer certificate SHA-256: `65c1c1256dae3c49e3548f334c91f0ba991969e9be9e0b223ba4e253d2114747`
- Immutable APK artifact: ID `10950858857`, digest
  `sha256:b13ca6973a203529c2cf7ea3247bb83f46af17cc89ff9506e460f33488b543b1`
- Diagnostics artifact: ID `10950719717`, digest
  `sha256:6f9e42122a3579f9ac6230cad24e912688592562b2cc1d6bd5512f4ae4f12e94`
- Download: `https://raw.githubusercontent.com/weekssa/OPRA-EQ-for-UAPP/mobile-test-apk/candidates/EQ-Library-v0.7.0-beta-e1ab5fa.apk`
- Checksum: `https://raw.githubusercontent.com/weekssa/OPRA-EQ-for-UAPP/mobile-test-apk/candidates/EQ-Library-v0.7.0-beta-e1ab5fa.apk.sha256`

Workflow evidence: signed-emulator install **PASS**, cold launch **PASS**. `actionlint` remains
**NOT RUN** locally because it is unavailable; this did not prevent the remote workflow from
passing. This exact candidate is a testing handoff only, not a physical-fix or public-support claim.

Pixel 9 owner checklist:

1. Verify the installed package and APK SHA-256 match the tuple above before connecting a DAC.
2. Test Black Pearl and JA11 independently; do not infer one result from the other.
3. For Black Pearl, capture a fresh read-only baseline, then authorize only the specified single
   Flash operation. Stop on any permission prompt during mutation, disconnect, session replacement,
   stale/missing readback, or non-verified result; do not retry automatically.
4. For JA11, capture fresh verified User 1 state, edit locally, Review, then Apply once. Confirm
   the exact five-band/global-gain final readback and one Save boundary; stop on any uncertainty.
5. Record restoration separately for each DAC. Use `06-post-pixel-closure-prompt.md` afterward.

Current handoff state: **READY_FOR_PIXEL_9**. Luna did not mutate hardware.

## Latest owner-authorized Black Pearl result — AFUL Explorer

The exact signed candidate was installed on Pixel 9 and verified before the one authorized action:

- Source: `e1ab5fa5a65dc2d64624d871ac53d436f792ea6a`
- APK: `EQ-Library-v0.7.0-beta-e1ab5fa.apk`
- APK SHA-256: `7fffba26f32991ce8c936f725bdc6c3c4b6956d3a0800c539b8d45e6b52a2501`
- Package/version: `com.weekssa.opraeqforuapp`, `0.7.0` / code `7`
- Pixel: `tokay`, API 37, wireless serial `[Pixel TLS-connect alias redacted]`
- Signed-beta workflow: `36375994853`; immutable artifact `10950858857`;
  digest `sha256:b13ca6973a203529c2cf7ea3247bb83f46af17cc89ff9506e460f33488b543b1`

One action was authorized and confirmed: `My EQs -> AFUL -> Explorer -> Flash`. The app performed
its safe preflight and stopped before hardware mutation because the planned native playback-gain
adjustment `-4.00 dB` would exceed the Black Pearl validated volume range. Terminal wording was:
`Flash failed — Applying -4.00 dB of playback gain would exceed the Black Pearl's validated volume
range. Adjust the DAC volume and try again.` It also said: `Reconnect or refresh the DAC before any
later hardware action. Do not retry automatically.`

Current disposition: **PHYSICAL_FAIL / PRECHECK_BLOCKED**, requested EQ **NOT VERIFIED**, final
native readback **NOT RUN** because no write was permitted. No Reset, Save, Restore, retry, second
Flash, or other DAC mutation followed. Luna did not mutate the DAC in this attempt.

Owner next step, if another attempt is desired: adjust the Black Pearl volume so the validated
range permits the `-4.00 dB` change, refresh/reconnect the DAC, and then provide a new explicit
authorization for one fresh exact-candidate AFUL Explorer Flash. The prior authorization is
consumed; do not treat this as permission to retry.

## Latest owner-authorized Black Pearl result — AFUL Explorer PASS

The owner adjusted the Black Pearl volume and authorized one fresh Flash using the same exact signed
candidate. The fresh baseline was `Verified current hardware`, `Flat`, `10 filters`, `Active slot 1`,
playback gain `-25.00 dB`; the confirmation showed the expected `-4.00 dB` adjustment.

After the single Flash, the app's read-only My DAC view showed:

- `Verified current hardware`
- `Matches My EQs`
- `Explorer · Jaytiss · Latest`
- `9 active bands`, `10 filters`, `Active slot 1`
- Playback gain `-29.00 dB`

Disposition: **PHYSICAL_PASS** for the exact AFUL Explorer transaction on this exact candidate,
Pixel 9, and Black Pearl identity. Restoration was **NOT RUN** because the owner authorized only
the Flash; no second mutation, Reset, Save, Restore, or retry was performed. This is not a claim of
power-cycle persistence, broad revision support, or public hardware support. The next step is owner
direction on whether to leave this AFUL Explorer state in place or separately authorize restoration;
JA11 remains an independent result.

## Final `0.7.0` closeout handoff — authoritative summary

Use this section for final owner closeout. Earlier sections preserve the chronological evidence
record; this section is the current consolidated disposition.

### Current disposition

- **Black Pearl AFUL Explorer exact transaction:** `PHYSICAL_PASS` on the exact signed candidate
  below, Pixel 9, and the identified Black Pearl unit.
- **Black Pearl restoration:** `NOT RUN`. The owner authorized one Flash only, so Luna did not
  Reset, Restore, Save, retry, or perform a second mutation. The DAC remains in the verified AFUL
  Explorer state from the successful test.
- **JA11 software remediation:** `PASS` through the complete safe User 1 editor path in source and
  deterministic tests.
- **JA11 physical release closure:** still pending/inconclusive in the maintained evidence. The
  ledger records successful Flash/Save/final-readback and observed restoration evidence on an earlier
  exact candidate, but explicit power-cycle retention/full qualification and exact-current-candidate
  closure are not yet recorded as final-release evidence.
- **Overall public release status:** `NO-GO` until the remaining release gates and owner approval
  below are closed. No public hardware-support claim follows from this handoff.

### What was implemented

1. **TRN Black Pearl Direct Flash truthfulness**

   - Preserved the existing Black Pearl protocol bytes, report order, timing, gain handling,
     identity matching, session ownership, and retry policy.
   - Success now requires a complete active-session final native readback: all ten native bands,
     active slot, filter type, frequency, gain, Q, and raw global gain.
   - Missing, malformed, stale, mismatched, or wrong-session readback is not success.
   - A readable gain mismatch reconciles the persistent anti-stacking baseline to observed truth;
     unavailable or session-uncertain baselines block later mutation until a fresh authoritative
     read exists.
   - The compact verified wording and single terminal feedback surface remain truthful and
     actionable.
   - The scoped Android transport repair selects the HID interrupt OUT endpoint when present and
     retains `SET_REPORT` fallback while preserving report bytes, timing, and retry policy.

2. **FiiO JA11 My DAC -> EQ editor/apply path**

   - Added the discoverable User 1-only editor route from the existing My DAC current-hardware
     section.
   - Editing, Reset-local-edits, Cancel, Close, Back, and Review remain local and perform zero
     transport writes.
   - The immutable baseline token binds exact identity/fingerprint, session generation, active
     `USER_1`, all five native bands, native global-gain units, freshness, and verification time.
   - Review presents all five bands, filter/frequency/gain/Q values, global gain, response/headroom
     consequences, warnings, and the fact that Apply is the first write.
   - Apply delegates to the existing JA11 transaction boundary with exact five-band writes,
     quantized global gain, User 1 selection, one Save/persistence boundary, reconnect handling,
     final readback, cancellation, and fail-closed uncertainty handling.
   - EQ Off and built-in Vocal/Classic/Bass states remain non-editable; no built-in coefficients
     are invented.

### Exact signed beta produced for testing — not a public release

The bug-fixed software was produced as one non-public signed testing candidate:

- Source SHA: `e1ab5fa5a65dc2d64624d871ac53d436f792ea6a`
- Candidate target: combined `black-pearl-ja11`
- APK: `EQ-Library-v0.7.0-beta-e1ab5fa.apk`
- APK SHA-256: `7fffba26f32991ce8c936f725bdc6c3c4b6956d3a0800c539b8d45e6b52a2501`
- Package/version: `com.weekssa.opraeqforuapp`, `0.7.0` / code `7`
- Signer certificate SHA-256: `65c1c1256dae3c49e3548f334c91f0ba991969e9be9e0b223ba4e253d2114747`
- R8 mapping SHA-256: `a713de5e165a7b2da9dba9ca2ae20cc8a6123748b4c271ac1706dda995ca0985`
- Signed-beta workflow: [run 36375994853](https://github.com/weekssa/OPRA-EQ-for-UAPP/actions/runs/36375994853)
- Immutable APK artifact: ID `10950858857`, digest
  `sha256:b13ca6973a203529c2cf7ea3247bb83f46af17cc89ff9506e460f33488b543b1`
- Diagnostics artifact: ID `10950719717`, digest
  `sha256:6f9e42122a3579f9ac6230cad24e912688592562b2cc1d6bd5512f4ae4f12e94`
- Testing APK:
  [EQ-Library-v0.7.0-beta-e1ab5fa.apk](https://raw.githubusercontent.com/weekssa/OPRA-EQ-for-UAPP/mobile-test-apk/candidates/EQ-Library-v0.7.0-beta-e1ab5fa.apk)
- Testing checksum:
  [EQ-Library-v0.7.0-beta-e1ab5fa.apk.sha256](https://raw.githubusercontent.com/weekssa/OPRA-EQ-for-UAPP/mobile-test-apk/candidates/EQ-Library-v0.7.0-beta-e1ab5fa.apk.sha256)
- Signed-emulator install and cold launch: `PASS`

Important: this beta was published only to the non-public `mobile-test-apk` testing surface. No
public GitHub `v0.7.0` release, public tag, or public APK release was published with these fixes.

### Physical evidence completed

#### Black Pearl — exact AFUL Explorer pass

- Pixel: Google Pixel 9, `tokay`, API 37, wireless ADB serial
  `[Pixel TLS-connect alias redacted]`.
- DAC: TTGK Technology `TE-C`, VID `0x3302`, PID `0x43E8`, serial `330243E8260129`.
- Fresh baseline: verified current hardware, `Flat`, `10 filters`, `Active slot 1`, playback gain
  `-25.00 dB`.
- One authorized operation: AFUL Explorer Flash with the intended native rounded `-4.00 dB`
  adjustment.
- Final app readback: `Verified current hardware`, `Matches My EQs`, Explorer active, `9 active
  bands`, `10 filters`, `Active slot 1`, playback gain `-29.00 dB`.
- Result: `PHYSICAL_PASS` for this exact candidate/device/DAC/starting state and AFUL Explorer
  transaction.
- Limitation: the transient success banner expired before capture; the durable verified-current-
  hardware and Matches My EQs screen is the retained app evidence. This does not establish
  power-cycle persistence, other Black Pearl revisions, other presets, or public support.

#### JA11 — current release limitation

The software path is complete and the maintained ledger contains successful physical JA11
Flash/Save/final-readback plus observed restoration records on an earlier exact signed candidate.
However, the current remediation handoff still classifies complete JA11 physical qualification as
inconclusive because the transient terminal/Save evidence was not retained, and explicit
power-cycle retention/full qualification is not closed for the exact combined `e1ab5fa` candidate.
Do not silently transfer the earlier JA11 hardware record to the current combined beta.

### Remaining closeout checklist

The final public release should not be tagged or published until each item below is resolved and
recorded against the final source SHA.

- [ ] Decide the public `0.7.0` scope. The existing `docs/releases/v0.7.0.md` describes EW300,
  while the tested bug-fix candidate is the combined `black-pearl-ja11` target. Reconcile the
  release notes, manifest, capability claims, and public support wording.
- [ ] Close JA11 exact-candidate evidence: either run the bounded current-candidate JA11 physical
  closure including final readback, exact one-Save trace, reconnect/persistence evidence, and
  restoration, or explicitly ship JA11 as hardware-validation-pending and remove any stronger
  public claim.
- [ ] Decide whether Black Pearl restoration is required for the release gate. If required,
  authorize one separate restoration operation and record the final restored state. Do not reuse
  the Flash authorization for restoration.
- [ ] Synchronize the authoritative [v0.7 release-readiness audit](../../../V0.7_RELEASE_READINESS_AUDIT.md),
  [JA11 validation ledger](../../../FIIO_JA11_VALIDATION_LEDGER.md), capability matrix,
  `06-post-pixel-closure-prompt.md`, changelog, and release notes with this final evidence. Remove
  stale NO-GO/PENDING statements only when replaced by exact evidence; preserve historical records.
- [ ] Freeze the final source SHA after documentation and scope decisions. Run the complete
  applicable Android unit/lint/debug/release/R8, API-36 emulator, UI/accessibility, CodeQL,
  catalog-currentness, priority-community, dependency-submission, security, and workflow checks.
- [ ] Produce the final signed `v0.7.0` release APK from the trusted main-only release workflow.
  Verify source SHA, package/version, signer, APK checksum, R8 mapping, immutable artifact ID and
  digest, install/upgrade behavior, cold launch, and release manifest.
- [ ] Obtain explicit owner approval for merge, tag, public GitHub release, and any public
  hardware-support claim. These actions remain owner-controlled and were not performed here.
- [ ] After publication approval, verify the public tag/release page/APK/checksum/update metadata
  and record the final owner acceptance in the validation ledger.

### Bottom line for final closeout

The two named software defects are implemented and software-verified. The exact Black Pearl AFUL
Explorer transaction now passes after the volume was adjusted. The bug-fixed candidate exists as a
non-public signed beta, but no public `v0.7.0` release was published. The remaining work is release
scope reconciliation, JA11 exact-candidate/qualification closure or explicit deferral wording,
optional-but-release-governed Black Pearl restoration, authoritative documentation synchronization,
final signed-release gates, and explicit owner publication approval.

## Owner authorization — 2026-09-28

The owner approved final software/release closeout and publication. This approval authorizes the
release workflow and documentation synchronization; it does not authorize an additional DAC write
or convert the maintained JA11 evidence boundary into a broader physical-support claim. The final
release must retain the exact candidate provenance and limitations above.

## Public `v0.7.0` release — published 2026-09-28

The final public release is now available at
[github.com/weekssa/OPRA-EQ-for-UAPP/releases/tag/v0.7.0](https://github.com/weekssa/OPRA-EQ-for-UAPP/releases/tag/v0.7.0).

Release identity:

- Tag: `v0.7.0`, latest, published, non-prerelease.
- Release ID: `397979580`.
- Tagged source SHA: `4f325d673159b40515086fe5143df12b29ddb076`.
- Executable source exercised on Pixel 9: `e1ab5fa5a65dc2d64624d871ac53d436f792ea6a`; the tagged head contains documentation-only closeout changes after that executable candidate.
- Package/version: `com.weekssa.opraeqforuapp`, `0.7.0` / code `7`.
- Signer certificate SHA-256: `65c1c1256dae3c49e3548f334c91f0ba991969e9be9e0b223ba4e253d2114747`.
- Workflow: [trusted main-only run `36381764266`](https://github.com/weekssa/OPRA-EQ-for-UAPP/actions/runs/36381764266).
- Immutable artifact: ID `10952494777`; ZIP SHA-256 `08cffddad84f4c5648a4ec2e884784188689a24041f020cc9925e7af8040bccc`.
- APK: [`EQ-Library-v0.7.0.apk`](https://github.com/weekssa/OPRA-EQ-for-UAPP/releases/download/v0.7.0/EQ-Library-v0.7.0.apk); SHA-256 `27dada499bcbf9be9bd21d1349164858c93a5d2b83f78fd61134de13b4eb4025`.
- Checksum: [`EQ-Library-v0.7.0.apk.sha256`](https://github.com/weekssa/OPRA-EQ-for-UAPP/releases/download/v0.7.0/EQ-Library-v0.7.0.apk.sha256); asset SHA-256 `137e44db9333f69d5df872df4176c1a25d7001b46b392fddf3c2bd4676c54f55`.

The public GitHub release API independently reports both assets as uploaded and reports the APK
digest matching the verified local APK. The release notes use sentence-case headings, direct
language, parallel lists, descriptive links, and explicit accessibility and hardware boundaries
per the Google developer documentation style guidance.

### Plain-language closeout checklist

- Install only the public APK linked above and verify the published checksum before installation.
- The Black Pearl claim is limited to the exact AFUL Explorer transaction that passed on the owner
  Pixel 9 after the owner adjusted the DAC volume. It does not claim power-cycle persistence,
  other Black Pearl revisions, other presets, or restoration performed by Luna.
- The JA11 User 1 editor/apply software path is included and software-verified. The release does
  not claim broader JA11 power-cycle qualification beyond the exact records in the maintained
  ledger.
- No additional release gate is pending. Any future Black Pearl restoration or JA11 power-cycle
  session is a separate owner-authorized physical activity and is not required to validate the
  published software provenance.

Current handoff state: **OWNER_ACCEPTED** for the published software release and its bounded
evidence claims. Luna did not mutate hardware.
