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

## Community EQ auto-publication and Unverified status — approved 2026-08-30

Public structured community EQ discovery is a normal catalog lane rather than a development-blocking manual curation project.

Primary community surfaces to expand continuously are:

1. Reddit audio communities;
2. Head-Fi;
3. Audio Science Review;
4. The HEADPHONE Community / Headphones.com.

A community EQ or structured form submission may be published automatically as **Unverified** when automated checks establish all of the following:

- the EQ data is structurally parseable and passes acoustic/schema validation;
- the original public source URL is retained;
- creator/username provenance is present or the source explicitly has no usable creator identity;
- headphone identity is safely resolved, or the record is a general preset that does not claim a headphone identity;
- the candidate is not an obvious duplicate/repost masquerading as a new tuning;
- source-policy and redistribution rules permit structured coefficient publication.

Ambiguous identity, malformed acoustic data, unattributed/repost ambiguity, unclear rights, inaccessible/private content, or other material uncertainty remains quarantined/review-only rather than published.

Unverified behavior:

- show a visible **Unverified** badge/status;
- explain briefly that the tuning has not been independently verified and may produce unexpected sound or level changes;
- preserve and expose the original source link so users can investigate the tuning themselves;
- allow manual selection and export when at least one selected export target can safely represent or adapt it;
- do **not** silently include an Unverified EQ through **Automatically include new profiles for this headphone**;
- verification is metadata/state promotion, not a new acoustic revision when the filter data is unchanged;
- source changes that materially change the acoustic fingerprint remain genuine revisions regardless of verification state.

The repository **Submit an EQ source** form remains a lightweight contribution route, but mechanically valid submissions may now progress automatically into the public catalog as Unverified instead of requiring human verification first. A small, non-disruptive link to the submission form should be available from relevant catalog/profile surfaces.

## Source and sound-impact presentation — approved 2026-08-30

Each public/community profile should retain enough context for a user to judge it without treating community claims as objective facts.

- Preserve the creator/username, original source/platform, original URL, source date when known, explicit target/tuning label, and source-authentic acoustic values.
- Preserve a short source-provided sound-intent/impact description when redistribution permits and it is useful to distinguish the tuning.
- Where the app/library generates a sound-impact summary, label or structure it as an EQ Library summary rather than implying the creator wrote it.
- Prefer neutral descriptions of filter action or measured stock-to-EQ change. Do not convert subjective praise into factual claims.
- The source link should be easy to reach from profile details so users can inspect context and risk themselves.

## General / Effect / Genre presets — approved 2026-08-30

EQ Library may ingest and publish presets that are not tied to one headphone. These are separate from headphone correction/tuning profiles and must be classified explicitly.

Initial semantic categories include:

- **General effect** — e.g. Bass boost, Sub-bass boost, Warm, Treble reduction, Vocal presence, Bright, Loudness/low-volume compensation, Speech/Podcast;
- **Genre preset** — e.g. Rock, Electronic, Classical, or another source-authored genre intent;
- **Headphone correction/tuning** — a profile tied to a specific headphone/IEM;
- **Personal/community tuning** — a person's preference for a specific headphone/IEM.

Rules:

- do not claim a general or genre preset is objectively correct for a genre or headphone;
- preserve arbitrary source filter counts exactly in the canonical library;
- apply the same provenance, verification, dedupe, revision, and Unverified rules used by other public profiles;
- general/effect/genre presets are standalone exportable presets in v0.3;
- do not silently layer/combine a general preset with a headphone profile in v0.3;
- any future layering feature must explicitly recompute device conversion, headroom/clipping behavior, band constraints, and labeling and remains a separate UX decision.

## Export targets and catalog visibility — approved 2026-08-30

Settings includes a local **Export targets** area where users choose the formats/devices they use. The canonical catalog remains device-independent and retains all profiles regardless of these choices.

Initial target choices follow the supported export architecture, including UAPP/ToneBoosters, TRN Black Pearl, and hardware-validation-pending TOPPING targets where appropriate.

Required behavior:

- existing/upgrading users retain UAPP/ToneBoosters as an enabled target so an update does not unexpectedly empty their visible library;
- each profile is evaluated independently against each selected target as **Exact/preserved**, **Optimized**, or **Not faithfully representable**;
- a profile is normally visible when at least one selected target can export it Exact/preserved or Optimized;
- changing selected export targets changes presentation/export choices only and never deletes canonical data, saved profile history, or managed selections;
- already-managed profiles remain retained even if later hidden by target filtering;
- selected export targets are the targets offered during normal export rather than making users browse unrelated devices/formats;
- an Unverified profile is never automatically included solely because it is exportable to a selected target.

Settings also includes:

**Show presets that none of my devices can export**

- default: **ON**;
- ON: show the broader library, clearly marking profiles that are not exportable to any currently selected target;
- OFF: hide those profiles from normal browsing while retaining them in the catalog and any existing managed state;
- when filtering hides profiles, disclose that profiles are hidden by export-target settings and provide a route back to Settings.

This visibility rule applies equally to headphone-specific and General/Effect/Genre presets.

## Device limits never constrain canonical EQ data — approved 2026-08-30

Canonical ingestion/storage preserves the complete source EQ regardless of export limits.

For example, a 14-band source remains a 14-band canonical EQ. A target that can represent all 14 bands may export it Exact/preserved. A target such as current UAPP/ToneBoosters with a 10-band representation may generate an Optimized target-specific result according to the validated exporter rules while the original 14-band source remains untouched.

New export targets therefore operate on the existing complete canonical EQ rather than requiring re-ingestion or device-specific catalog copies.
