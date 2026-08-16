# Phase 1 product decisions

This document records implementation-time product decisions that refine the approved Phase 0 behavior. `docs/CHATGPT_PROJECT_RUNBOOK.md` remains authoritative; `docs/ARCHITECTURE.md` references this file so it is read during substantive implementation work.

## Selection defaults — approved 2026-08-15

When a headphone has never been managed before:

- **Automatically include new OPRA profiles for this headphone** starts ON.
- Every currently selectable profile starts checked.
- Not-compatible profiles remain visible according to Profile visibility settings but are never checked and cannot be checked.
- The user can uncheck any current selectable profiles before Save; with automatic inclusion still ON, those unchecked profiles become explicit exclusions.

This initial state therefore means “use all current and future compatible profiles” until the user changes it.

## Missing creator/author — approved 2026-08-15

Creator/author completeness is separate from acoustic compatibility.

If an OPRA profile has complete, safely convertible EQ data but its creator/author is missing:

- classify compatibility from the acoustic EQ data normally;
- keep the profile selectable if its compatibility outcome is selectable;
- allow it to be converted and exported;
- visibly use the literal label **Creator information missing** anywhere a creator name is required, including deterministic exported preset naming;
- retain the original missing/null creator state in local OPRA metadata rather than pretending the placeholder is a real source author;
- never invent or infer a person or organization as the creator.

## Selected profile later becomes Not compatible — approved 2026-08-15

If an already-selected OPRA profile changes upstream and the newly validated profile can no longer be converted safely:

- automatically clear its current selected state;
- show it as Not compatible with a disabled/uncheckable selection control and an explicit review warning;
- do not include it in Select all, automatic future inclusion, selected counts, new conversions, or new exports while it remains Not compatible;
- preserve the last successfully generated local XML/state;
- preserve any existing exported preset; do not delete or rewrite it automatically;
- require an explicit user action for any later removal/cleanup.

The transition must be covered by deterministic reconciliation tests and must never destroy the last known-good generated/exported preset.

## My Headphones membership and Browse export — approved 2026-08-15 during device testing

**My Headphones** is the persistent local library of headphones the user intentionally manages. A headphone enters or updates that library when either of these actions occurs:

- the user explicitly adds/saves the headphone from its Browse profile-selection screen; or
- the user explicitly exports/downloads XML presets for that headphone from its Browse profile-selection screen.

Required behavior:

- A never-managed headphone whose compatible profiles are preselected by the approved default must offer an enabled **Add to My Headphones** action immediately. The user must not be forced to toggle a checkbox merely to make the initial default state saveable.
- Browse continues to provide the local manufacturer/model search used to find headphones.
- The headphone profile-selection screen provides an explicit **Export XMLs** action when at least one profile is selected.
- If the headphone is new or has staged changes, **Export XMLs** first persists the staged selection/future-profile setting using the same save/removal-confirmation rules, then exports that headphone's selected presets.
- Exporting therefore creates or updates the matching My Headphones record before files are written.
- If the headphone is already managed and unchanged, Export XMLs exports it without manufacturing an artificial state change.
- My Headphones is keyed by the OPRA product identity. Re-adding or re-exporting a headphone updates the existing record rather than creating duplicates.
- Exporting zero selected profiles is not offered; a managed headphone with a staged zero-selection state follows the existing remove-headphone confirmation flow instead.
- The existing My Headphones **Export presets** action remains the explicit bulk export for all selected presets across the managed library.
