# EW300 v0.7 bounded hands-on checklist

## Accepted physical evidence and no-repeat boundary

The exact-fingerprint reports are tied to signed executable source `7599dd52fc9e8c58c96e021f581b86a669dcc148`: E043 read-only PASS, E044 Flash PASS, E045 exact-baseline Restore PASS (`restorationVerified=true`), and E046 Reset PASS. E037-E040 preserve the earlier source-381 Apply/Flash/Restore/Reset results. Each reported mutation used one Save, zero permission requests before first write, exact replacement identity/generation, final readback, and known state. Replay/competing-job telemetry remains null/unmeasured.

The physical reports do not establish end-to-end Personal EQ capture. Restore was exact when it completed; a separate later Reset then completed. Do not claim the later state still equals an arbitrary pre-Flash baseline.

**Do not repeat Apply, Flash, Restore, Reset, E001 Save qualification, or the already-passing read-only report.** No further hardware mutation is authorized or needed for this closeout.

## Candidate gate before any owner/next-agent check

**Current state: BLOCKED.** Do not install the prior a09d or older candidate. The revised signing
workflow is manual/main-only and no corrected-source APK is available. Testing can begin only
after owner-authorized integration, exact-head software/security checks, pinned-signature and
post-sign-alignment verification, and an immutable artifact manifest all pass. The remaining
physical mutation/read-only work is closed; this checklist never asks to repeat it.

Before anyone installs the final beta, verify in live PR #23 that its source SHA equals the candidate source recorded in the handoff and that Android CI, CodeQL, catalog currentness, priority-community coverage, dependency submission, and the signed-beta workflow all pass on that exact SHA. The signed artifact must preserve package `com.weekssa.opraeqforuapp`, version 0.7.0/code 7, and the pinned release-signing identity. Never use an older candidate as if it were a later head's artifact.

The signed candidate manifest must point to accepted physical evidence E001 and E043-E048 rather than obsolete pre-qualification placeholders, and it must not claim that another consolidated hardware mutation session is outstanding.

## Required final beta validation — product behavior, not protocol requalification

Use the final handoff as the detailed script. The next testing/verification agent should validate:

1. Clean install of the exact signed beta and cold launch.
2. Upgrade from the public v0.6.0 app to the exact signed beta while preserving expected local data/settings.
3. My DAC → EW300 EQ: response graph, 0 dB reference, active-band markers, Flat/exact/ambiguous/Unknown presentation, truthful current-versus-last-read state, manual Refresh, local edit/review behavior, and accessible semantic graph/status text.
4. My DAC → EW300 DEVICE: compact verified playback/global-gain readout, active Peak-band count, connection freshness, manual Refresh, and no unqualified volume/filter/gain/balance/mic/UAC/firmware controls.
5. Offline/error/reconnect presentation and recovery without duplicated USB sessions or silent state assumptions.
6. Cross-DAC smoke regression for Black Pearl and FiiO/other currently supported DAC flows so shared My DAC changes do not regress existing devices.
7. Representative shelf-source behavior: qualifying shelf profiles are labeled Optimized and remain Peak-only on EW300; profiles outside the error/safety envelope are Not suitable; canonical source filters remain unchanged.

Stop on any source-SHA/signature mismatch, crash, lost upgrade data, incorrect DAC identity, misleading current/stale state, unqualified control, inaccessible critical state, or shelf/canonical mutation regression. Record the exact failure before retrying.

## Only possible owner/device checks — non-mutating unless separately re-authorized

Only after the exact candidate gates pass, and only if existing evidence does not already answer the question:

1. In My DAC → EQ, save one captured five-band Peak state as a Personal EQ; verify the five EQ values and device provenance. Playback/global gain must **not** be represented as captured EQ identity. This is a local library operation, not a hardware write.
2. Open the My EQs Flash review only if existing evidence cannot establish that entry route. Verify target/review details and **cancel before final write confirmation**.

Stop without any final write if values/provenance or the review path are unclear. Do not collect another read-only capability report.

## Product closeout already completed before this checklist

The implementation/review work has closed the previously listed product gaps: shared EW300 graph/status, authoritative-session manual Refresh, concise DEVICE presentation, the capability-by-capability Black Pearl comparison, and cross-source low/high-shelf regression coverage. These closures do not broaden the qualified EW300 hardware-control boundary.

Keep PR #23 draft and v0.7.0 NO-GO until the exact beta validation, final review, and explicit owner approval are complete. Do not request mutation testing, merge, publication, or a public EW300 support claim from this checklist alone.
