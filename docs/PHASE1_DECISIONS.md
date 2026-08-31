# Phase 1 product decisions

This document records implementation-time product decisions that refine the approved Phase 0 behavior. `docs/CHATGPT_PROJECT_RUNBOOK.md` remains authoritative, and `docs/V0.3_LOCKED_EXECUTION_PLAN.md` is the controlling v0.3 execution plan. Where an older prototype decision conflicts with this file or the locked plan, the locked v0.3 behavior below wins.

## Selection defaults — updated and locked 2026-08-30

When a headphone has never been saved for the current output:

- **Automatically include new EQs** starts ON.
- Every current, verified, structurally usable canonical parametric-EQ profile starts checked.
- Unverified profiles are not selected automatically, but remain manually selectable.
- Historical revisions are not selected automatically, but remain manually selectable when history is shown.
- The active output's capabilities never hide an otherwise valid curve and never make it unselectable.
- A user may explicitly select a usable curve even when the active output reports **Not exportable**.
- With automatic inclusion ON, manually unchecked current verified profiles become exact exclusions; future unrelated verified profiles are still included automatically.
- With automatic inclusion OFF, the selection is fixed and future profiles appear without being silently added.

A source row is unavailable for selection only when the canonical record itself is not a usable parametric-EQ source, not merely because one output cannot represent it.

## Missing creator/author — approved 2026-08-15

Creator/author completeness is separate from acoustic/source usability.

If a profile has complete usable EQ data but its creator/author is missing:

- keep the profile selectable;
- evaluate each output normally as Exact, Optimized, or Not exportable;
- visibly use the literal label **Creator information missing** anywhere a creator name is required, including deterministic exported preset naming;
- retain the original missing/null creator state in local source metadata rather than pretending the placeholder is a real source author;
- never invent or infer a person or organization as the creator.

## Selected profile later changes — updated and locked 2026-08-30

Source usability and output representability are separate states.

If an already-selected profile changes upstream:

- if the new canonical source remains a usable parametric EQ, keep the user's selection even if the current output now reports **Not exportable**;
- if the canonical source itself becomes structurally unusable, stop selecting that current source and surface it for review rather than guessing missing values;
- if a selected profile changes and remains exportable to the active output, regenerate the active-output artifact and report the change;
- never silently clamp, invent, drop, or translate unsupported filters outside a validated target-specific conversion rule;
- do not automatically delete an already exported app-owned file merely because the newly current source can no longer be represented by that output;
- never reuse a stale generated artifact as though it represented a changed current source.

If a profile is removed upstream entirely:

- keep the last known local source snapshot and last generated/exported artifact;
- mark it **No longer available in EQ Library**;
- let the user remove it explicitly.

## My EQs membership and output-specific collections — updated and locked 2026-08-30

**My EQs** is the persistent local collection for the currently active output.

Required behavior:

- navigation is **My EQs | EQ Library | Settings**;
- saved headphone selections, General EQ selections, favorites/saved snapshots, and personal imports are scoped to the active output;
- switching output may therefore show a different My EQs collection without changing the canonical catalog;
- existing pre-output-context headphone selections migrate to UAPP/ToneBoosters;
- existing pre-output-context favorites and personal imports also migrate to UAPP/ToneBoosters rather than appearing on every output;
- underlying immutable/local source snapshots may be shared internally when the same EQ is selected for multiple outputs, but output membership is independent;
- removing an EQ from one output must not silently remove it from another output where it is still selected;
- adding/saving a headphone from EQ Library creates or updates that output's matching My EQs record rather than creating duplicate product identities.

## Export currentness — locked 2026-08-30

Export is driven by the active output and app-owned file state.

- The active output is already chosen globally; normal export does not ask the user to choose a target again.
- **Export all** is meaningful when at least one exportable EQ in the current My EQs collection has never been exported, its generated output changed, or its app-owned exported file is missing/stale.
- A per-item **Export** action follows the same rule and may disappear when that exact output file is current.
- Export status is checked against the deterministic active-output candidate, retained ownership metadata, fingerprint/content hash, and the actual app-owned SAF document when folder access is available.
- A source that is **Not exportable** to the active output remains saved/visible but does not falsely appear as an exportable pending file.
- Export remains independent of direct hardware Flash.
- EQ Library only replaces or removes files it can identify as created/owned by this app; same-name unowned files remain conflicts.

## Community EQ auto-publication and Unverified status — approved 2026-08-30

Public structured community EQ discovery is a normal catalog lane rather than a development-blocking manual curation project.

Primary community surfaces to expand continuously are:

1. Reddit audio communities;
2. Head-Fi;
3. Audio Science Review;
4. The HEADPHONE Community / Headphones.com.

A community EQ or structured form submission may be published automatically as **Unverified** when automated checks establish all of the following:

- the EQ data is structurally parseable and passes source/schema validation;
- the original public source URL is retained;
- creator/username provenance is present or the source explicitly has no usable creator identity;
- headphone identity is safely resolved, or the record is a general preset that does not claim a headphone identity;
- the candidate is not an obvious duplicate/repost masquerading as a new tuning;
- source-policy and redistribution rules permit structured coefficient publication.

Ambiguous identity, malformed acoustic data, unattributed/repost ambiguity, unclear rights, inaccessible/private content, or other material uncertainty remains quarantined/review-only rather than published.

Unverified behavior:

- show a visible **Unverified** status and compact risk wording such as **Community submission — not independently verified. Review the source before use.**;
- preserve and expose the original source link;
- allow manual selection whenever the canonical PEQ source itself is usable, regardless of the active output's representability;
- show Exact, Optimized, or Not exportable separately for the active output;
- do **not** silently include an Unverified EQ through **Automatically include new EQs**;
- explicit Select all may include visible Unverified profiles and must not drop existing hidden/history selections;
- verification is metadata/state promotion, not a new acoustic revision when filter data is unchanged;
- source changes that materially change the acoustic fingerprint remain genuine revisions regardless of verification state.

The repository **Submit an EQ source** form remains a lightweight contribution route, but mechanically valid submissions may progress automatically into the public catalog as Unverified instead of requiring human verification first.

## Source and sound-impact presentation — approved 2026-08-30

Each public/community profile should retain enough context for a user to judge it without treating community claims as objective facts.

- Preserve the creator/username, original source/platform, original URL, source date when known, explicit target/tuning label, and source-authentic acoustic values.
- Preserve a short source-provided sound-intent/impact description when redistribution permits and it is useful to distinguish the tuning.
- Where the app/library generates a sound-impact summary, label or structure it as an **EQ Library summary** rather than implying the creator wrote it.
- Prefer neutral descriptions of filter action or measured stock-to-EQ change. Do not convert subjective praise into factual claims.
- Keep a small Source action/link available from profile details.

## General / Effect / Genre presets — approved 2026-08-30

EQ Library may ingest and publish presets that are not tied to one headphone. These are separate from headphone correction/tuning profiles and must be classified explicitly.

User-facing General EQ filters are:

- **All**;
- **Sound** — e.g. Bass boost, Sub-bass boost, Warm, Treble reduction, Vocal presence, Bright;
- **Genre** — only when the source itself provides a genre intent;
- **Utility** — e.g. Loudness/low-volume and Speech/Podcast.

Rules:

- do not claim a general or genre preset is objectively correct for a genre or headphone;
- preserve arbitrary source filter counts exactly in the canonical library;
- apply the same provenance, verification, dedupe, revision, and Unverified rules used by other public profiles;
- General EQs are standalone saved/exportable presets in v0.3;
- do not silently layer/combine a General EQ with a headphone profile in v0.3;
- any future layering feature must explicitly recompute device conversion, headroom/clipping behavior, band constraints, and labeling and remains a separate UX decision.

## Outputs and catalog visibility — updated and locked 2026-08-30

Settings includes an **Outputs** area that controls which devices/apps appear in the global output selector. Output choice is operating context, not a library filter.

Initial v0.3 selector choices are:

- USB Audio Player PRO / ToneBoosters;
- TRN Black Pearl;
- Universal Parametric EQ;
- Poweramp / Poweramp Equalizer;
- Wavelet.

Hardware targets whose format/device behavior is still awaiting validation may remain implemented internally but are not normal v0.3 selectable outputs until validated.

Required behavior:

- UAPP/ToneBoosters is enabled by default for new/upgrading users unless they later change their enabled outputs;
- remember the active output locally;
- Settings controls selector membership; the active output is changed from the global selector on My EQs/EQ Library rather than duplicated as another Settings choice;
- each usable profile is evaluated for the active output as **Exact**, **Optimized**, or **Not exportable**;
- **all usable canonical library curves remain visible and selectable regardless of output**;
- changing output changes compatibility information, conversion/export behavior, direct-device actions, and the My EQs collection only;
- changing outputs never deletes canonical data, source history, or another output's saved selections;
- the obsolete prototype setting **Show presets that none of my devices can export** is removed/ignored and must not affect presentation.

## Device limits never constrain canonical EQ data — approved 2026-08-30

Canonical ingestion/storage preserves the complete source EQ regardless of export limits.

For example, a 14-band source remains a 14-band canonical EQ. A target that can represent all 14 bands may export it Exact. A target such as current UAPP/ToneBoosters with a 10-band representation may generate an Optimized target-specific result according to its validated exporter rules while the original 14-band source remains untouched.

New export targets therefore operate on the existing complete canonical EQ rather than requiring re-ingestion or device-specific catalog copies.

## TRN Black Pearl direct Flash — locked 2026-08-30

Direct Flash is an optional My EQs action for the Black Pearl output only.

- Settings exposes **Enable direct Flash**, OFF by default, only in the Black Pearl context/when that output is enabled.
- My EQs shows **Connect to DAC** in red while disconnected and a green connected state after successful connection.
- Flash remains visible for Black Pearl rows but is disabled while disconnected or when the source is Not exportable/flashable.
- Flash requires confirmation and writes to the DAC's currently active EQ slot discovered from the device rather than inventing a slot-selection UI.
- A successful operation reports **Flash successful**; the row subsequently returns to its normal Flash action.
- Export remains available independently of connection state.
- The app manages EQ only. It must not change global volume, DAC filter, gain mode, amplifier topology, balance, microphone, or other DAC controls.
- The independently implemented protocol supports the corroborated native Peak, Low Shelf, and High Shelf EQ types and a 10-band hardware limit.
- More than 10 source-priority bands may use the first 10 only with an explicit Optimized warning; the canonical source remains untouched.
- The observed EQ protocol has no independently verified per-EQ preamp field. A profile requiring nonzero preamp/headroom is therefore Not flashable rather than silently changing global DAC volume.
- GPL reference projects may be studied for observable protocol behavior, but their implementation code is not copied into this Apache-2.0 project.
