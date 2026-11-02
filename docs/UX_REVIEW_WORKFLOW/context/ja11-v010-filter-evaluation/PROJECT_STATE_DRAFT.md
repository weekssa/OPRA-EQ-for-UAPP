# PROJECT_STATE.md — proposed additions

Status: **DRAFT.** These two additions are prepared for the “Active work and known limits”
section of `docs/PROJECT_STATE.md`. They are not applied. Editing the maintained status document
requires separate authorization; this file records the exact wording for owner review.

## Proposed addition 1 — filter deferral

> JA11 reconstruction filters remain deferred from v0.9.0: **device-specific not supported
> (vendor-exposed)** on the tested firmware 2.20 tuple. The official vendor-record finding and
> J034/J035 do not prove silicon impossibility or exclude a hidden/future interface. Reopening
> requires handoff section H evidence; no filter write is authorized. Preserved local APK
> provenance requires reconciliation before new sibling findings are treated as authoritative.

Classification: INSPECTION-ONLY consolidation of historical DEMONSTRATED results. The tested
tuple is one Pixel 9 and one JA11 at firmware 2.20; the statement does not generalize to other
units, firmware or products.

## Proposed addition 2 — built-in save ordering

> JA11 built-in Save retains the physically qualified v0.9.0 sequence. Vendor-exact ordering—selection
> → `0x19` only, skipping restage/Apply—remains **NOT EXERCISED**. If filter evidence reopens,
> propose separately authorized Off/Classic readback and power-cycle qualification in the same
> consolidated owner session. Until qualified, retain the shipped sequence.

Classification: the shipped sequence is DEMONSTRATED for the J035 tuple; the vendor-exact
ordering is NOT EXERCISED. The second sentence documents a conditional proposal, not a plan that
has been scheduled.

## Notes for the applying task

- Add both entries without altering the dated release history above them.
- Keep the classification wording exact; do not upgrade either statement to a silicon or
  general-firmware claim.
- If the v0.10.0 decision later changes through new H evidence, update these entries in the same
  workstream rather than layering a conflicting statement.
