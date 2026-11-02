# Issue drafts — JA11 filter and save-ordering gaps

Status: **NOT FILED.** These are exactly two draft issues. Filing requires separate explicit
authorization; nothing here authorizes implementation, installation, connection, writes, firmware
action or release claims.

Current classification (verbatim): **device-specific not supported (vendor-exposed)**.

Reopening criteria H (verbatim):

> Reopen only with: a JA11 firmware whose vendor device record exposes a filter control; a
> first-party capability statement; a validated device-specific `0x04` reply; or an independently
> verified exact-device implementation.

## Issue 1 — Reevaluate JA11 reconstruction filters against exact-device reopening evidence

**Current classification:** device-specific not supported (vendor-exposed)

Filters were deferred from v0.9.0 and are not delivered. J034/J035 recorded no valid filter read
and no filter write; vendor helpers and dormant code do not establish JA11 capability.

**Reopening criteria (verbatim):**

> Reopen only with: a JA11 firmware whose vendor device record exposes a filter control; a
> first-party capability statement; a validated device-specific `0x04` reply; or an independently
> verified exact-device implementation.

**Work if reopened:** reconcile vendor-artifact provenance, consolidate B4/B5 evidence, and apply
the three-way decision. A record-level finding satisfies H at the record level only; a validated
device read is still required before any UI or write. Decoder success alone is insufficient:
validate the complete frame and the current hardware/firmware session.

**Do not repeat:** the `0x11` probe, the 42-chunk web search, or the closed V4.6.0 JA11 query.

**Closure:** record either qualifying evidence and a separately approved implementation/
qualification plan, or stronger evidence-backed deferral.

**Authority:** this issue authorizes no implementation, installation, connection, write, firmware
action or release claim.

## Issue 2 — Qualify JA11 built-in saves with vendor-exact ordering

**Current classification:** device-specific not supported (vendor-exposed)

Separately, built-in Save ordering without restage/Apply is **NOT EXERCISED**. J035 qualified the
shipped sequence; vendor similarity alone does not authorize replacing it.

**Reopening criteria (verbatim), for the filter question only.** These criteria do not gate the Save-ordering gap tracked in this issue; that gap is independently actionable and is not waiting on filter evidence.

> Reopen only with: a JA11 firmware whose vendor device record exposes a filter control; a
> first-party capability statement; a validated device-specific `0x04` reply; or an independently
> verified exact-device implementation.

**Important boundary:** those criteria govern filter reopening, not proof of Save behavior. They do
not establish that the vendor-exact ordering works on the JA11.

**Work if reopened:** include separately approved Off/Classic ordering qualification in the same
owner window: selection, then preset-valued `0x19` only, with no band/gain restaging or Apply.
Verify the startup selection and volume across power removal, the unchanged User 1 bank and global
gain, microphone and UAC, and exact restoration.

**Closure:** either retain the qualified shipped sequence with the unresolved refinement documented,
or record exact-candidate physical qualification before changing it.

**Authority:** filing this issue would not authorize code changes or hardware work.

## Filing note

Both drafts preserve the same classification and the same H criteria because they gate the same
workstream. Issue 2 records a save-behavior gap; it must not be read as filter capability evidence.
