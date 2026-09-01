#!/usr/bin/env python3
"""One-shot documentation sync for the approved PR #4 release-polish implementation."""
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]


def read(path: str) -> str:
    return (ROOT / path).read_text(encoding="utf-8")


def write(path: str, text: str) -> None:
    (ROOT / path).write_text(text, encoding="utf-8")


def replace_once(path: str, old: str, new: str) -> None:
    text = read(path)
    count = text.count(old)
    if count != 1:
        raise SystemExit(f"{path}: expected one target, found {count}: {old[:120]!r}")
    write(path, text.replace(old, new, 1))


def replace_slice(path: str, start_marker: str, end_marker: str, replacement: str) -> None:
    text = read(path)
    start = text.find(start_marker)
    if start < 0:
        raise SystemExit(f"{path}: start marker not found: {start_marker!r}")
    end = text.find(end_marker, start)
    if end < 0:
        raise SystemExit(f"{path}: end marker not found: {end_marker!r}")
    write(path, text[:start] + replacement + text[end:])


# Hands-on: one consolidated focused pass for the actual final PR #4 surface.
replace_slice(
    "docs/V0.3_HANDS_ON_CHECKLIST.md",
    "## Final release-polish addendum\n",
    "## STOP conditions\n",
    """## Final release-polish addendum\n\nUse the final exact-head signed candidate only. Start each batch-selection screen with no rows selected unless the checklist explicitly says otherwise.\n\n1. **Back hierarchy:** From My EQs → headphone → Manage preset selection, use Android Back repeatedly. Clean editor → headphone detail → My EQs root must occur before the app exits. With unsaved editor changes, Back must show Discard changes / Keep editing. In EQ Library, Model → Manufacturer/search → root must unwind naturally; Back from root EQ Library and from Settings must return to My EQs; only Back from My EQs root exits. Check visible back arrows match system Back.\n2. **Favorite stars in My EQs:** Open a managed headphone. Confirm every managed preset row has an outlined/filled star matching its Favorite state. Toggle one on and off without changing selected state, export status, or Flash state. Confirm a Favorite snapshot row uses the filled star to unfavorite while a personal import retains its remove action.\n3. **Populated General EQs + batch Save:** Open EQ Library → General EQs. Confirm Bass Boost, Vocal Clarity, Treble Boost, Loudness, Podcast, Electronic, and Rock are present with sensible Sound/Genre/Utility classification and source attribution. Confirm the batch checkboxes start with none selected, Select all/Select none work, and Save selected saves the chosen General EQs and starts one active-output initial-export flow. Confirm no General preset is silently layered with a headphone EQ.\n4. **Hide from EQ Library:** Hide at least one General EQ using Hide selected and one headphone EQ using its Hide action. Confirm both disappear from ordinary EQ Library browse/search. Hide an EQ that is already present in My EQs and confirm its My EQs selection, favorite state, current exported file/status, and Black Pearl Flash availability are not removed or reset.\n5. **Settings → Hidden EQs / Unhide:** Confirm Settings shows the hidden count. Open Hidden EQs and confirm none are selected initially, Select all/Select none work, and Unhide selected restores only those EQs to normal library visibility. Confirm there is no Delete action. Fully close/reopen the app and confirm hidden state persists.\n6. **Hide survives refresh:** Hide a canonical EQ, use Settings → Refresh now, and confirm it remains hidden after the refreshed catalog is promoted. Unrelated newly visible catalog entries must not inherit the hidden state. An already-saved My EQs copy remains untouched.\n7. **Compact personal import entry:** In My EQs, confirm the old large top Import PEQ button is gone. Confirm compact **+ Import** appears with **Saved snapshots & personal imports** and does not crowd the Black Pearl Connect/Connected control.\n8. **Equalizer APO / AutoEq paste import:** Open + Import, explicitly paste a valid Equalizer APO / AutoEq PEQ, and confirm the authoritative preview shows the exact preamp, active-filter count, type, frequency, gain, and Q values. Save & export; confirm it appears in My EQs and the active-output initial export starts. Import must not flash Black Pearl.\n9. **No-preamp + strict error handling:** Import a valid preset without a Preamp line and confirm the preview says **No preamp supplied** and the canonical value is not rewritten to 0 dB. Then make one Filter line malformed in an otherwise valid multi-filter preset and confirm Save is blocked with a visible line-level error rather than silently importing a partial EQ. Repeat with an unsupported active filter type and confirm Save remains blocked.\n10. **Choose file is content-based:** Choose a file containing valid Equalizer APO / AutoEq text using a non-`.txt` filename/extension when practical and confirm it imports from its contents. Choose a file whose contents are not a supported importer and confirm a clear **This EQ format isn't supported yet** style error with no saved-state change. A UTF-8 BOM-bearing text file should still recognize its first Preamp/Filter line.\n11. **Output/export regression smoke:** Import one freshly exported UAPP/ToneBoosters preset into UAPP. If Black Pearl is available, connect and flash one previously validated ordinary in-range preset to confirm the UI/data polish did not disturb the established Flash path. A repeat of the -11.9 dB hardware qualification is not required unless related Black Pearl/device/DSP code changed.\n\n**Focused PASS:** all eleven release-polish checks pass; the final signed candidate may proceed to the public v0.3.0 release gate.\n\n""",
)

# Product decisions: lock archive visibility and personal import semantics outside conversation history.
replace_once(
    "docs/PHASE1_DECISIONS.md",
    "## Selection defaults — updated and locked 2026-08-31\n",
    """## Living archive and reversible Hide/Unhide — approved 2026-08-31\n\n- The published canonical catalog is a living archive. Once a genuine canonical EQ or genuine acoustic revision is validly published, source movement, disappearance, pausing, retirement, or URL loss must not remove that acoustic record from the current archive.\n- Publication/currentness regression validation must fail if a candidate silently drops a previously published canonical profile/revision or mutates an archived revision's acoustic fingerprint in place. Git history alone is not sufficient preservation.\n- Hide is global local visibility state, not deletion and not output membership. Store stable canonical profile-lineage IDs in Preferences DataStore.\n- Hiding a lineage hides its genuine revisions from normal EQ Library browse/search but never removes the canonical catalog/cache record, Room/My EQs state, favorite membership, export ownership/currentness, or Black Pearl Flash state.\n- General EQ review uses none-selected-by-default batch checkboxes with Select all / Select none, Save selected, and Hide selected.\n- Settings exposes Hidden EQs with a count, none selected by default, Select all / Select none, and Unhide selected. There is no Delete action.\n- Hidden state survives restart and catalog refresh. Future genuine revisions of the same canonical lineage remain hidden; unrelated new profiles remain visible by default.\n\n## Personal PEQ import redesign — approved 2026-08-31\n\n- The large top-area Import PEQ button is replaced by compact **+ Import** beside **Saved snapshots & personal imports** in My EQs. It must not visually compete with Black Pearl Connect/Connected.\n- Import opens a dedicated screen/sheet with explicit **Paste PEQ text** and Android system **Choose file** actions, required manufacturer/model/EQ name, optional target/note, and an authoritative parsed preview before Save. Clipboard content is read only after the user taps Paste.\n- The initial user-facing input format is **Equalizer APO / AutoEq text**. Filename extension does not choose conversion; contents are parsed into the canonical PEQ first. Valid supported text may import regardless of extension, while unsupported contents fail clearly without changing saved state.\n- User-facing import validation is stricter than tolerant source discovery: blank/comment lines and explicitly OFF filters may be ignored, but malformed filter-looking lines and unsupported active filters block Save. Never silently import a partial EQ, clamp, truncate, invent, or substitute filters.\n- Missing source preamp remains null. Preserve the complete supported canonical filter count; active-output device limits are applied only at conversion/export.\n- Successful personal Save initiates initial active-output export when representable. Import never automatically flashes Black Pearl. Manual filter-by-filter editing and additional XML/JSON/CSV/Peace input adapters remain outside v0.3.\n\n## Selection defaults — updated and locked 2026-08-31\n""",
)
replace_once(
    "docs/PHASE1_DECISIONS.md",
    """If a profile is removed upstream entirely:\n\n- keep the last known local source snapshot and last generated/exported artifact;\n- mark it **No longer available in EQ Library**;\n- let the user remove it explicitly.\n""",
    """If an original source later moves or becomes unavailable:\n\n- keep the canonical profile and every genuine published revision in the living archive;\n- preserve the last known local source snapshot and last generated/exported artifact for My EQs;\n- update/mark source availability and provenance rather than deleting acoustic history;\n- let the user remove local My EQs membership/files explicitly without deleting the public canonical archive.\n""",
)

# Architecture: make the implemented storage/ingestion boundaries explicit.
replace_once(
    "docs/ARCHITECTURE.md",
    """- `docs/V0.3_LOCKED_EXECUTION_PLAN.md`\n\n`docs/V0.3_LOCKED_EXECUTION_PLAN.md` contains the latest approved v0.3 product direction""",
    """- `docs/V0.3_LOCKED_EXECUTION_PLAN.md`\n- `docs/V0.3_RELEASE_POLISH_PLAN.md`\n\n`docs/V0.3_RELEASE_POLISH_PLAN.md` controls the final PR #4 archive/visibility/import scope. `docs/V0.3_LOCKED_EXECUTION_PLAN.md` contains the earlier v0.3 product direction""",
)
replace_once(
    "docs/ARCHITECTURE.md",
    """- Preferences DataStore: local appearance, active output, enabled outputs, export-tree, refresh/update presentation preferences\n""",
    """- Preferences DataStore: local appearance, active output, enabled outputs, global hidden canonical-profile IDs, export-tree, refresh/update presentation preferences\n""",
)
replace_once(
    "docs/ARCHITECTURE.md",
    """Verified/Unverified promotion is metadata-only when acoustic fingerprint is unchanged.\n\n## Community and source ingestion\n""",
    """Verified/Unverified promotion is metadata-only when acoustic fingerprint is unchanged.\n\n## Living archive and local visibility\n\nThe canonical catalog is an append/preserve archive of genuine acoustic history, not a disposable mirror of whatever source pages are reachable today. Publication validates against the prior published catalog and rejects disappearance of an already-published canonical profile/revision or an in-place acoustic-fingerprint mutation of an archived revision. Source availability and URLs are provenance/lifecycle metadata; they do not delete archived acoustic data.\n\nAndroid keeps the complete validated catalog/cache. Global Hide/Unhide is a Preferences DataStore set of stable canonical profile-lineage IDs applied only when building ordinary browse/search projections. A hidden lineage therefore stays available to existing Room-backed My EQs records and export/Flash state and can be unhidden without redownload/reconstruction.\n\n## Community and source ingestion\n""",
)
replace_once(
    "docs/ARCHITECTURE.md",
    """Do not invent genre/intent from filter shape. Classification must follow explicit source context.\n\n## Output/device context\n""",
    """Do not invent genre/intent from filter shape. Classification must follow explicit source context.\n\n## Personal PEQ import\n\nPersonal import is another canonical-ingestion front end, not an output-specific converter. The v0.3 path is:\n\n`explicit paste / Android document picker -> strict Equalizer APO / AutoEq parser -> authoritative preview -> canonical local EQ -> active-output conversion/export`\n\nThe document filename/extension is not a format selector. The current adapter recognizes supported Equalizer APO / AutoEq text from contents; future XML/JSON/CSV/Peace/device adapters must normalize into the same canonical model before output conversion. The strict personal-import layer blocks malformed filter-looking lines and unsupported active filters so tolerant discovery parsing can never silently create a partial user import. Missing source preamp remains null and canonical filter count is not truncated to a device limit.\n\n## Output/device context\n""",
)

# Source strategy: strengthen archive invariant and point crawling/currentness expansion to its future plan.
replace_once(
    "docs/SOURCE_INGESTION_STRATEGY.md",
    """- Genuine changed tunings become immutable revisions; application-modeling corrections must not create fake acoustic history.\n""",
    """- Genuine changed tunings become immutable revisions; application-modeling corrections must not create fake acoustic history.\n- Once a genuine canonical EQ/revision is validly published, retain it in the current living archive even if the original source later moves, disappears, pauses, or retires. Source status/provenance may change; ordinary source lifecycle events do not delete archived acoustic history.\n""",
)
replace_once(
    "docs/SOURCE_INGESTION_STRATEGY.md",
    """- source deletion/removal: preserve already-valid historical records where legally appropriate and mark source state rather than silently erasing the EQ\n""",
    """- source deletion/removal: retain every already-published genuine canonical EQ/revision in the living archive and mark/update source state/provenance rather than erasing acoustic history\n""",
)
replace_once(
    "docs/SOURCE_INGESTION_STRATEGY.md",
    """- removed source -> preserve provenance/history where appropriate and mark source removed/retired\n""",
    """- removed source -> retain archived canonical EQs/revisions, preserve provenance, and mark source removed/retired\n""",
)
replace_once(
    "docs/SOURCE_INGESTION_STRATEGY.md",
    """No failed source may invalidate the last-known-good canonical catalog.\n\n## Catalog publication discipline\n""",
    """No failed source may invalidate the last-known-good canonical catalog.\n\nThe current v0.3 repository has scaffolding plus several real currentness lanes, but not every registered forum/community yet has a fully autonomous live scanner. Completion of scheduled adapters, overdue-source enforcement, and monthly discovery of additional sources is intentionally tracked in `docs/FUTURE_SOURCE_AUTOMATION_PLAN.md` rather than hidden as an assumed v0.3 capability. Production source automation runs through GitHub Actions/repository tooling, not ChatGPT or the Android client.\n\n## Catalog publication discipline\n""",
)
replace_once(
    "docs/SOURCE_INGESTION_STRATEGY.md",
    """8. regression validation against the prior catalog\n\nPublication must be atomic.\n""",
    """8. regression validation against the prior catalog, including a hard living-archive check that previously published canonical profiles/revisions have not disappeared or changed acoustically in place\n\nPublication must be atomic.\n""",
)

# Earlier locked plan now explicitly redirects final work to PR #4 instead of sounding like PR #3 is open.
replace_once(
    "docs/V0.3_LOCKED_EXECUTION_PLAN.md",
    """Work should continue autonomously on branch `eq-library-community-v0.3` until software acceptance gates are green and a signed APK is ready for hands-on testing, or a genuine product/hardware blocker requires the user. PR #3 remains draft and unmerged until hands-on validation passes.\n""",
    """Historical execution note: the `eq-library-community-v0.3` / PR #3 implementation completed its signed Pixel 9 / TRN Black Pearl hands-on gate and was merged to `main`. Final v0.3.0 release-polish work now runs on `v0.3-release-polish` / PR #4 and is controlled by `docs/V0.3_RELEASE_POLISH_PLAN.md`; that later plan supersedes this document where the final scope differs.\n""",
)

# Release-polish plan: record code completion and preliminary exact-head automation before the doc-only final head.
replace_once(
    "docs/V0.3_RELEASE_POLISH_PLAN.md",
    """## 8. Final PR #4 / release sequence\n""",
    """## 8. Implementation checkpoint\n\nThe approved archive-preservation, Hide/Unhide, General batch Save/Hide, compact personal-import, strict Equalizer APO / AutoEq parsing/preview, and initial personal-export implementation is now present on `v0.3-release-polish`. Temporary patch-application workflow/helper files have been removed from the intended tree.\n\nPre-documentation implementation head `63e054ffd4c0b351cc469bc435b8d949bf6dca49` passed Android unit tests, Android lint, debug/release assembly, catalog currentness including living-archive regression tests, priority-community coverage, CodeQL, signed-beta build, APK alignment/signature verification, artifact upload, and stable mobile-test publication. This head is **not** the final hands-on candidate because the maintained release documentation is synchronized afterward; the final documentation head must pass the same exact-head gates again.\n\nBlack Pearl protocol/DSP code was not changed by this release-polish implementation, so the established full Black Pearl hardware qualification remains valid; only the focused ordinary Flash smoke test is required unless a later diff touches device/DSP behavior.\n\n## 9. Final PR #4 / release sequence\n""",
)

# Runbook current status: implementation is no longer pending; final exact-head gates/hands-on remain.
replace_once(
    "docs/CHATGPT_PROJECT_RUNBOOK.md",
    """v0.3.0 is in final release-polish work on branch `v0.3-release-polish` / PR #4. The approved final scope now includes hierarchical Android Back behavior, Favorite-star controls in My EQs, the first populated qualified General EQ catalog, living-archive preservation gates, reversible Hide/Unhide with Settings management, and a redesigned personal PEQ import flow using strict previewed Equalizer APO / AutoEq canonical ingestion plus initial export. `docs/V0.3_RELEASE_POLISH_PLAN.md` is the controlling checklist for this work.\n\nBecause Android UI/data behavior changes after the prior hardware-tested merge, a fresh exact-head signed candidate and focused Pixel 9 Back/Favorite/General-EQ/Hide-Unhide/personal-import regression pass are required before public publication. Black Pearl protocol requalification is not required unless the final diff unexpectedly touches device/DSP behavior.\n""",
    """v0.3.0 is in final release-polish validation on branch `v0.3-release-polish` / PR #4. The approved final scope includes hierarchical Android Back behavior, Favorite-star controls in My EQs, the first populated qualified General EQ catalog, living-archive preservation gates, reversible Hide/Unhide with Settings management, and a redesigned personal PEQ import flow using strict previewed Equalizer APO / AutoEq canonical ingestion plus initial export. `docs/V0.3_RELEASE_POLISH_PLAN.md` is the controlling checklist. The implementation is present; preliminary exact-head automation passed before this documentation sync, so the remaining software gate is to validate the final documentation/source head and signed candidate.\n\nBecause Android UI/data behavior changed after the prior hardware-tested merge, the fresh final exact-head signed candidate must pass the focused Pixel 9 Back/Favorite/General-EQ/Hide-Unhide/personal-import checklist before public publication. Black Pearl protocol requalification is not required because this polish did not touch Black Pearl protocol/DSP code; repeat it only if a later diff does. `docs/FUTURE_SOURCE_AUTOMATION_PLAN.md` records the post-v0.3 work to finish real scheduled adapters/currentness enforcement for every active source and monthly discovery of additional sources; do not silently treat that deferred automation as already complete.\n""",
)

# Changelog: user-visible behavior and validation state.
replace_once(
    "CHANGELOG.md",
    """### Added\n\n- Initial populated **General EQ** catalog""",
    """### Added\n\n- A **living canonical archive** regression gate that rejects candidate catalog publication if a previously published genuine canonical profile/revision disappears or an archived revision's acoustic fingerprint changes in place.\n- Reversible global local **Hide/Unhide** for headphone and General EQ lineages, with Hide in EQ Library, batch General Hide, persisted stable canonical IDs, and **Settings → Hidden EQs** batch Unhide without deleting archive/My EQs/export state.\n- A dedicated personal-EQ import surface with compact **+ Import**, explicit clipboard Paste and Android Choose file actions, **Equalizer APO / AutoEq text** content recognition, authoritative preamp/filter preview, and strict malformed/unsupported active-filter validation.\n- Initial populated **General EQ** catalog""",
)
replace_once(
    "CHANGELOG.md",
    """### Changed\n\n- Android Back now follows""",
    """### Changed\n\n- General EQ review now uses none-selected-by-default batch controls with **Select all**, **Select none**, **Save selected**, and **Hide selected**; batch Save initiates the normal active-output initial export.\n- Personal EQ import now normalizes supported file/paste contents into the device-independent canonical PEQ before output conversion. Filename extension does not select the converter, missing preamp remains null, full supported filter count is retained canonically, and successful Save initiates active-output export without automatically flashing hardware.\n- Android Back now follows""",
)
replace_once(
    "CHANGELOG.md",
    """### Fixed\n\n- System Back no longer""",
    """### Fixed\n\n- Personal import no longer accepts a valid subset while silently dropping a malformed or unsupported active Filter line; the strict import layer blocks Save and identifies the parse problem.\n- System Back no longer""",
)
replace_once(
    "CHANGELOG.md",
    """### Validation\n\n- Final release-polish changes are isolated""",
    """### Validation\n\n- Pre-documentation release-polish implementation head `63e054ffd4c0b351cc469bc435b8d949bf6dca49` passed Android unit tests, lint, debug/release assembly, catalog living-archive/currentness validation, priority-community coverage, CodeQL, signed-beta alignment/signature verification, artifact upload, and mobile-test publication. The final documentation/source head must repeat the exact-head gates before hands-on testing.\n- Final release-polish changes are isolated""",
)

# Release notes: surface the final features without overstating hands-on validation.
replace_once(
    "docs/releases/v0.3.0.md",
    """- Adds Favorite star controls directly to managed My EQs preset rows.\n- New headphones start with no profiles selected""",
    """- Adds Favorite star controls directly to managed My EQs preset rows.\n- Treats the published canonical catalog as a living archive: source movement/removal updates provenance/status but cannot silently erase a previously published genuine EQ or acoustic revision.\n- Adds reversible **Hide/Unhide** for canonical headphone and General EQ lineages, including batch General Hide and **Settings → Hidden EQs** management while preserving existing My EQs/export/Flash state.\n- Replaces the large My EQs Import PEQ button with compact **+ Import** and a dedicated importer for pasted or chosen-file **Equalizer APO / AutoEq text**. The importer recognizes contents rather than relying on extension, previews the exact canonical PEQ, preserves a missing preamp as null, and blocks malformed/unsupported active filters instead of silently importing a subset.\n- New headphones start with no profiles selected""",
)
replace_once(
    "docs/releases/v0.3.0.md",
    """- **Add/Save** now performs the initial export for the active output.""",
    """- **Add/Save** now performs the initial export for the active output, including General batch Save and successful personal PEQ Save; import never automatically flashes connected hardware.""",
)
replace_once(
    "docs/releases/v0.3.0.md",
    """The v0.3 foundation candidate passed Android unit tests, lint, debug/release assembly, catalog/currentness validation, priority-community validation, CodeQL, APK alignment, pinned signing-certificate verification, and Pixel 9 / TRN Black Pearl hands-on testing, including the explicit -11.9 dB caution/Flash-anyway case. The final release-polish source adds only navigation/Favorite UI plus qualified General EQ catalog behavior; a fresh signed candidate must pass the focused Pixel 9 Back/Favorite/General-EQ checklist and normal automated release gates before publication.""",
    """The v0.3 foundation candidate passed Android unit tests, lint, debug/release assembly, catalog/currentness validation, priority-community validation, CodeQL, APK alignment, pinned signing-certificate verification, and Pixel 9 / TRN Black Pearl hands-on testing, including the explicit -11.9 dB caution/Flash-anyway case. The later PR #4 release-polish implementation adds navigation/Favorite/General-EQ behavior plus living-archive validation, Hide/Unhide, and the redesigned personal importer. Its pre-documentation implementation head also passed the software/signing gates. A fresh final exact-head signed candidate must pass the focused Pixel 9 Back/Favorite/General-EQ/Hide-Unhide/personal-import checklist before publication; full Black Pearl protocol requalification is unnecessary unless a later diff touches device/DSP behavior.""",
)

print("Synchronized v0.3 release-polish source-of-truth documentation.")
