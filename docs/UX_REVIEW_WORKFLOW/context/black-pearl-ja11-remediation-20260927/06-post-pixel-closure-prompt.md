# Post-Pixel closure prompt — Black Pearl and JA11 remediation

## Current source boundary

The exact candidate for the final owner review is source
`b61f02e8c91656f14ffc639e4c6d937b2162a1b7`, APK SHA-256
`276587734fc863277b83e4310c040cee22c27261075e21fa75d766ded9eef27e`, signer
`65c1c1256dae3c49e3548f334c91f0ba991969e9be9e0b223ba4e253d2114747`, workflow #1372, and
immutable artifact ID `10941292707` / digest
`sha256:188d2e4b973e7aab420d4c7d3890dba3407415c1d2c8f935a418acdacdbd8fc3`. The prior Pixel 9
session used source `acaf4dd32ddd9379ec2860e45e34fb8039219583` and must not be transferred. The
Android UAPP routing prompt observed on the stock app is expected platform/device behavior and
should not be treated as an app defect.

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

Current post-session state: `PHYSICAL_FAIL` for the single Black Pearl AFUL Explorer Direct Flash
exercise and `PHYSICAL_INCONCLUSIVE` for the previously exercised JA11 session. The exact candidate
was verified on the Pixel 9 and the Black Pearl identity matched, but final raw playback-gain
readback was `expected -7398`, `actual -6400`; the app correctly withheld success. Its existing
post-operation read-only refresh ran, but no retry, owner-directed recovery reconnect/refresh,
Reset, Save, Restore, or restoration claim followed. Treat the requested post-operation state as
unqualified, do not transfer the historical `acaf4dd` result, and do not classify either named issue
as owner-accepted or publicly supported.

The authorized read-only recovery subsequently established a fresh current snapshot: Explorer's
10-filter hardware state matched, active slot 1 remained selected, and playback gain remained
`-25.00 dB` / raw `-6400` rather than the requested `-7398`. This is partial-state evidence only;
it does not convert the complete Flash to a pass and does not prove restoration.
