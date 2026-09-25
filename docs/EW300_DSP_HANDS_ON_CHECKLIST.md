# EW300 v0.7 bounded hands-on checklist

## Accepted physical evidence and no-repeat boundary

**EW300 closeout status (2026-09-24): PASS.** The owner confirmed the final signed-main candidate
passes the output-gain, balanced-stereo-audio, and Flash/readback checks. The result is recorded as
E053 in `docs/EW300_VALIDATION_LEDGER.md`.

The exact-fingerprint reports are tied to signed executable source `7599dd52fc9e8c58c96e021f581b86a669dcc148`: E043 read-only PASS, E044 Flash PASS, E045 exact-baseline Restore PASS (`restorationVerified=true`), and E046 Reset PASS. E037-E040 preserve the earlier source-381 Apply/Flash/Restore/Reset results. Each reported mutation used one Save, zero permission requests before first write, exact replacement identity/generation, final readback, and known state. Replay/competing-job telemetry remains null/unmeasured.

The physical reports do not establish end-to-end Personal EQ capture. Restore was exact when it completed; a separate later Reset then completed. Do not claim the later state still equals an arbitrary pre-Flash baseline.

**Do not repeat Apply, Flash, Restore, Reset, E001 Save qualification, or the already-passing read-only report.** No further hardware mutation is authorized or needed for this closeout.

## Candidate gate before any owner/next-agent check

**Current state: MERGED, SIGNED, AND OWNER-VALIDATED.** Use only the exact signed-main candidate
`EQ-Library-v0.7.0-beta-7ec01be.apk`, source `7ec01be2f413afb69778a4990950064639e1c4e9`,
SHA-256 `40905806a65902bdd94035f0905617218fa76394f09ab187f1937b19b56598bf`, from signed workflow
[#1357](https://github.com/weekssa/OPRA-EQ-for-UAPP/actions/runs/36080002546). Earlier candidates are
superseded and must not be treated as final v0.7 evidence.

Before anyone installs a future release-closeout beta, verify that its source SHA, package
`com.weekssa.opraeqforuapp`, version 0.7.0/code 7, pinned release-signing identity, and all required
workflow gates match that exact artifact. Never use an older candidate as if it were a later head's
artifact.

The signed candidate manifest and release documents must point to accepted physical evidence E001,
E043-E048, and owner-reported closeout E053. They must not claim that another EW300 mutation session
or stereo-audio confirmation is outstanding.

### Release-ready physical checkpoint after signed APK

Only after exact signed APK provenance is available:

1. Install the exact signed APK and begin at minimum volume.
2. Use read-only Refresh and verify a coherent EW300 state; if the stereo `0x66` gain bytes are unequal, stop.
3. Play a known left/right stereo signal at a safe level and confirm both channels without crackle; stop immediately on crackle, heat, or unsafe loudness.
4. Do not repeat Apply, Flash, Restore, Reset, Save qualification, or exploratory writes.

## Required final beta validation — product behavior, not protocol requalification

Use the final handoff as the detailed script. The next testing/verification agent should validate:

1. Clean install of the exact signed beta and cold launch.
2. Upgrade from the public v0.6.0 app to the exact signed beta while preserving expected local data/settings.
3. My DAC → EW300 EQ: response graph, 0 dB reference, active-band markers, Flat/exact/ambiguous/Unknown presentation, truthful current-versus-last-read state, manual Refresh, local edit/review behavior, and accessible semantic graph/status text.
4. My DAC → EW300 DEVICE: compact verified playback/global-gain readout, physically confirmed
   output-gain control, active Peak-band count, connection freshness, manual Refresh, and no
   unqualified filter/gain-mode/balance/mic/UAC/firmware controls.
5. Offline/error/reconnect presentation and recovery without duplicated USB sessions or silent state assumptions.
6. Cross-DAC smoke regression for Black Pearl and FiiO/other currently supported DAC flows so shared My DAC changes do not regress existing devices.
7. Representative shelf-source behavior: qualifying shelf profiles are labeled Optimized and remain Peak-only on EW300; profiles outside the error/safety envelope are Not suitable; canonical source filters remain unchanged.

Stop on any source-SHA/signature mismatch, crash, lost upgrade data, incorrect DAC identity, misleading current/stale state, unqualified control, inaccessible critical state, or shelf/canonical mutation regression. Record the exact failure before retrying.

## Only possible owner/device checks — non-mutating unless separately re-authorized

Only after the exact candidate gates pass, and only if existing evidence does not already answer the question:

1. In My DAC → EQ, save one captured five-band Peak state as a Personal EQ; verify the five EQ values and device provenance. Playback/global gain must **not** be represented as captured EQ identity. This is a local library operation, not a hardware write.
2. Open the My EQs Flash review only if existing evidence cannot establish that entry route. Verify target/review details and **cancel before final write confirmation**.

Stop without any final write if values/provenance or the review path are unclear. Do not collect another read-only capability report.

## New audio-channel investigation — CLOSED

The owner reported a right-channel-only symptom and crackling on the left at high level on
2026-09-24. The source defect was corrected by writing/verifying both stereo `0x66` gain bytes,
and E053 records the owner's successful exact-signed-candidate audio confirmation. Follow
`docs/V0.7_EW300_AUDIO_CHANNEL_INVESTIGATION.md` for the durable root-cause record; do not repeat
the audio or mutation session merely because documentation changes create a new SHA.

The owner has now reproduced the symptom with the EW300 DSP cable on a computer while the
alternate cable/DAC works. Treat the runtime Android/UAPP/EQ Library audio-routing hypothesis as
excluded. The preserved E033 raw state now identifies a likely app-owned stereo-gain write defect:
the old path changed only byte 0 of `0x66`. The corrected candidate must be tested only after exact
source/signing gates pass; the remaining physical check is corrected readback plus safe audio.

- Start at minimum volume with a known left/right stereo recording.
- Do not Flash, Reset, Save, Restore, send exploratory output reports, or test at maximum volume.
- Record Android/UAPP balance and mono settings, the behavior with EQ Library closed, the behavior
  after read-only Refresh, and the result with the same IEMs/adapter/source on another DAC.
- If safe and mechanically applicable, swapping the detachable earpiece sides helps distinguish an
  earbud/connector fault from an EW300 output-channel fault.
- Capture the existing read-only EW300 register report, including `0x66`, without changing state.
- Confirm the report includes protocol-layout register `0x01` and that the two stereo `0x66` gain
  bytes agree before listening. If they do not agree, stop and share the report; do not improvise a
  recovery write.
- Stop immediately on crackle, heat, unsafe loudness, or any unexpected hardware state change.

Do not treat a successful EQ readback as proof of stereo-channel health. Do not add a balance,
routing, mono/stereo, UAC, or volume control without exact EW300 evidence.

## Product closeout already completed before this checklist

The implementation/review work has closed the previously listed product gaps: shared EW300 graph/status, authoritative-session manual Refresh, concise DEVICE presentation, the capability-by-capability Black Pearl comparison, and cross-source low/high-shelf regression coverage. These closures do not broaden the qualified EW300 hardware-control boundary.

PR #31 and PR #32 are merged and the exact signed-main candidate is owner-validated. Keep v0.7.0
unpublished until final release QA, documentation synchronization, final exact-head signing, and
explicit owner publication approval are complete. Do not request another EW300 mutation session
from this checklist.
