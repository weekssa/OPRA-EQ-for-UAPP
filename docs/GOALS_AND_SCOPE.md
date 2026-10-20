# Product Goals and Scope

This document summarizes approved direction. Source code shows what is implemented; it does not by itself define product intent. Detailed decisions remain in the linked approved records.

## Product goal

EQ Library helps people manage source-neutral headphone EQ profiles and use them with supported Android audio outputs, including qualified USB DACs. Keep the workflow private, local-first and usable offline after catalog data is available. See [`ARCHITECTURE.md`](ARCHITECTURE.md), [`PHASE1_DECISIONS.md`](PHASE1_DECISIONS.md), and [`CHATGPT_PROJECT_RUNBOOK.md`](CHATGPT_PROJECT_RUNBOOK.md).

## Approved product constraints

- Canonical EQ data remains immutable, source-neutral and device-independent. Device adapters derive output representations without changing canonical acoustic values.
- Preserve source, creator, attribution, revision and conversion provenance.
- A valid canonical curve stays available when a selected output cannot export it; unsupported filters or invalid values fail closed instead of being silently altered or dropped.
- User selections, settings and generated state remain local. The app ships with zero bundled headphone/EQ records and does not require login, analytics, telemetry or a cloud backend for normal use.
- Use the validated catalog and currentness flows described in the maintained runbook; do not scrape GitHub/forums as normal app behavior.
- Keep the active output context separate from physical DAC identity and My EQs ownership.
- Reuse shared DAC capabilities and session flows. Device command bytes, limits, control meanings, persistence and reset behavior require evidence for that exact device/revision.
- User-visible loading, offline, empty, permission, stale, failure, recovery and success states must be truthful and accessible.
- Physical mutation, release publication, merge, signing configuration, and public support claims remain explicit approval boundaries in root [`AGENTS.md`](../AGENTS.md).

## Current implemented product and release

The latest public stable implementation is v0.8.3. Its release source, APK identity and limits are recorded in [`PROJECT_STATE.md`](PROJECT_STATE.md) and [`release-engineering/v0.8.3-stable/ARTIFACTS.md`](release-engineering/v0.8.3-stable/ARTIFACTS.md). Do not infer current installed-device state from the public release record.

## Explicitly out of scope for routine maintenance

- Changing production application behavior or architecture without a separately scoped, approved task.
- Claiming support from a chipset, USB VID/PID, visual similarity, third-party compatibility, or a version label alone.
- Adding firmware update, bootloader, cross-flash or undocumented protocol features without exact evidence and approval.
- Converting historical plans or open PRs into current requirements without reviewing their later disposition.

## Requirements and evidence

Approved decisions are normative. Maintained implementation and tests establish current behavior. Exact release and hardware records prove only their named source, artifact, device, operation and test boundary. Historical notes are context. Unverified claims remain explicitly unverified.
