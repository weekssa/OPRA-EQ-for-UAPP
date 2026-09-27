# Post-Pixel closure prompt — Black Pearl and JA11 remediation

Use only after the owner returns evidence from the exact candidate identified in `05-release-handoff.md`.

Validate install/launch and each DAC independently. Record exact candidate provenance, Pixel identity, DAC fingerprint/firmware, fresh read-only baseline, user-visible result, final readback/Save/reconnect evidence, restoration status, and `PASS`, `FAIL`, `INCONCLUSIVE`, or `NOT EXERCISED` for each named defect. Do not retry an uncertain mutation. Update the validation ledger, capability/status documents, and final outcome only from the returned evidence.
# Post-Pixel 9 closure prompt - Black Pearl and JA11 remediation

Use this prompt only after the owner has supplied evidence from the exact signed candidate whose
source SHA, APK SHA-256, signer certificate, workflow run, and immutable artifact ID are recorded
in `05-release-handoff.md`.

> Validate the supplied Pixel 9 evidence against the exact candidate provenance before interpreting
> any result. Classify TRN Black Pearl and FiiO JA11 independently as `PHYSICAL_PASS`,
> `PHYSICAL_FAIL`, or `PHYSICAL_INCONCLUSIVE`. Reconcile each operation trace/readable report to
> the exact device identity, session generation, requested values, final native readback, Save
> count, and restored-state evidence. A mismatch, missing report, wrong candidate, permission
> prompt during mutation, disconnect, or unproven restoration is not a pass and must not be
> auto-retried. Record the precise stop condition and leave the capability/status ledger unchanged
> except for the evidence actually proved.
>
> For Black Pearl, require all ten native fields plus raw global gain and current session for
> verified success. Confirm a readable final-gain mismatch is reported as not verified and that
> subsequent mutation is blocked or reconciled only by a fresh authoritative read. Confirm the
> shared compact success wording and no duplicate terminal feedback.
>
> For JA11, require active verified User 1, no pre-Apply writes, complete five-band review, one
> explicit Apply, exact transaction order, one Save/reconnect boundary, final readback, truthful
> result, and original-state restoration. Confirm Off and built-in programs remain non-editable.
>
> Update the append-only validation ledger, the relevant capability matrix, current status,
> release-readiness audit, changelog, and owner go/no-go package. Do not merge, publish, tag, or
> make a public hardware-support claim without separate owner approval.

Current post-session state: `PHYSICAL_INCONCLUSIVE`. The exact signed candidate was used for a
bounded JA11 Apply/restore session. Immediate readback and original-state restoration matched, but
the Android UAPP routing dialog obscured transient terminal feedback and no exported operation trace
proving exact Save count was captured. Do not classify the complete JA11 issue as accepted until the
owner resolves or explicitly accepts this evidence boundary. Black Pearl was not exercised.
