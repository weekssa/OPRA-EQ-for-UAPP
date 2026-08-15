# OPRA EQ for UAPP

OPRA EQ for UAPP is a standalone native Android app that converts user-selected OPRA parametric EQ profiles into UAPP/ToneBoosters XML locally on the device.

- **Repository:** `weekssa/OPRA-EQ-for-UAPP`
- **Android application ID:** `com.weekssa.opraeqforuapp`
- **Runtime OPRA catalog:** `https://opra.roonlabs.net/database_v1.jsonl`
- **OPRA upstream:** `https://github.com/opra-project/OPRA`
- **Read-only behavioral reference:** `weekssa/opra-uapp-converter`

## Project status

The project is currently in **Phase 0: design only**. No Android application code has been created yet. Major user-facing features must receive UX/behavior approval before implementation begins.

The app will ship with **zero headphones at install**. Normal operation will obtain the OPRA runtime catalog, cache it locally, and work offline after the initial sync.

## Source of truth

The maintained project runbook is:

`docs/CHATGPT_PROJECT_RUNBOOK.md`

The runbook defines repository boundaries, approved product behavior, UX gates, conversion requirements, testing expectations, privacy, export, update behavior, attribution, and development workflow.
