# EW300 v0.7 bounded hands-on checklist

## Accepted physical evidence and no-repeat boundary

The exact-fingerprint reports are tied to signed executable source `7599dd52fc9e8c58c96e021f581b86a669dcc148`: E043 read-only PASS, E044 Flash PASS, E045 exact-baseline Restore PASS (`restorationVerified=true`), and E046 Reset PASS. E037-E040 preserve the earlier source-381 Apply/Flash/Restore/Reset results. Each reported mutation used 11 writes, one Save, zero permission requests before first write, exact replacement identity/generation, final readback, and known state. Replay/competing-job telemetry remains null/unmeasured.

The physical reports do not identify whether Flash was launched from My EQs or EQ Library and do not establish end-to-end Personal EQ capture. Restore was exact when it completed; a separate later Reset then completed. Do not claim the later state still equals an arbitrary pre-Flash baseline.

**Do not repeat Apply, Flash, Restore, Reset, E001 Save qualification, or the already-passing read-only report.** No further hardware mutation is authorized or needed for this closeout.

## Candidate gate before any owner check

Before an owner is asked to use a candidate, verify in live PR #23 that its source SHA equals the PR head and that Android CI, CodeQL, catalog currentness, priority coverage, dependency submission, and signed-candidate workflow all pass on that exact SHA. The pre-criteria head `b11190f9bbc08326f963190ad8b5a9f4b0872b2c` passed all six; its exact APK/checksum/signer/artifact evidence is E048. A later documentation or code commit is a new candidate and needs fresh provenance. Never use source 7599 or b11190 APK as if it were a later head's artifact.

## Only possible owner checks — non-hardware-mutating

Only after applicable gates pass on the final candidate, and only if existing evidence does not already answer the question:

1. In My DAC → EQ, save one captured five-band Peak state as a Personal EQ; verify the five values and exact device provenance. This is a local library operation, not a hardware write.
2. Open the My EQs Flash review only if the verified Flash was not already launched from My EQs. Verify target/review details and cancel before final write confirmation.

Stop without any final write if values/provenance or the review path are unclear. Do not collect another read-only report.

## Product closeout before requesting the owner

The candidate must also meet `docs/V0.7_PRODUCT_SUCCESS_CRITERIA.md`: shared My DAC design/flow parity; EW300 compact response graph and truthful EQ status; manual Refresh; concise DEVICE presentation; a capability-by-capability Black Pearl crosswalk; source-neutral low/high-shelf corpus coverage; accessibility/error-state and shared-DAC regression coverage; and exact-source software/security/signing/install/upgrade gates.

Keep PR #23 draft and v0.7.0 NO-GO. Do not request mutation testing, merge, publication, or a public EW300 support claim without explicit owner approval.
