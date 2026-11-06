# Library transfer — backup and restore

Owner-approved slice, 2026-11-06. It exists because production
(`com.weekssa.opraeqforuapp`) and the prototype (`com.weekssa.opraeqforuapp.v011beta`) are different
packages with different private databases, and production is a release build, so `adb run-as` is not
available. A user-facing file is the only honest path between them.

## What moves

| Stored row | What it is | Where it goes |
|---|---|---|
| Saved EQ, kind `favorite` | A headphone/profile saved from the catalog, with its canonical selection | Same row, same entry id, canonical selection intact |
| Saved EQ, kind `personal` | An imported or captured curve | Same row, same entry id |
| General EQ | A saved General EQ preset | Same row keyed by preset id |

The bundle carries the stored JSON verbatim — `profileJson`, `canonicalSnapshotJson`,
`canonicalSelectionJson`, `captureMetadataJson` — instead of re-deriving it, so the restored record
is the record that was exported. Timestamps are preserved, so ordering in My EQs survives.

## What does not move

- **Preferences.** Theme, output targets, Direct Flash switches, the export folder, and hidden EQ ids
  are device settings, not library data.
- **Catalog.** Both builds ship the same catalog and fetch the same revisions; the catalog is not
  copied into the file.
- **The DAC target.** The EW300 is a device, not a field on a saved EQ. My EQs ownership is global
  in this app (`SavedEqRecord` has no output id), so there is no per-target attachment to carry.
- **Unreadable rows.** A row whose stored data fails validation is skipped and counted, never
  guessed at.

## Import rules

1. An entry whose id already exists is left alone and counted as already present. The import never
   overwrites what the receiving app holds.
2. A row whose `profileJson` no longer decodes is reported and not written.
3. A Favorite whose canonical selection cannot be read in the receiving build is restored as a
   **Personal EQ**: the curve, name, manufacturer, model and timestamps survive, the catalog binding
   does not, and the summary says so plainly.
4. A file that is not a bundle, a bundle from a newer format version, or an empty bundle is refused
   with a message and no writes.

A Favorite exported with no stored selection (the legacy shape) keeps its kind; only a selection
that is present and unreadable forces the Personal fallback. That split is tested on the emulator.
The decodable-selection path is the same code path with the gate passing, and the codec is identical
in both packages, but it is not exercised against a real catalog selection in this slice because no
test fixture builds one; treat "canonical selection intact after transfer" as reasoned from the
pass-through, not measured on a device.

## Format

```json
{
  "format": "eq-library-bundle",
  "version": 1,
  "exportedAtMillis": 0,
  "sourcePackage": "com.weekssa.opraeqforuapp",
  "sourceVersionName": "0.10.0",
  "sourceVersionCode": 22,
  "savedEqs": [ { "entryId": "…", "kind": "favorite", "profileJson": "…", "canonicalSelectionJson": "…" } ],
  "generalEqs": [ { "presetId": "…", "displayName": "…", "category": "SOUND", "profileJson": "…" } ],
  "skipped": []
}
```

Reads are capped at 4 MB; unknown keys are ignored so an older reader keeps working.

## Where it lives

- `data/library/EqLibraryBundle.kt` — the format, the codec, and the row-source interface.
- `data/library/EqLibraryBundleImporter.kt` — `EqLibraryBundleStore`, which reads and writes the
  chosen document and hands rows to the repositories.
- `SavedEqRepository` / `SavedGeneralEqRepository` — `exportBundleRows` and `importBundleRows`.
- Settings → Library → Backup (`Export`) and Restore (`Import`), with the system file picker.
- Help → Backup explains the feature in one paragraph.

## Verification

- `EqLibraryBundleTest` — codec round trip, foreign JSON, newer version, unknown keys.
- `EqLibraryBundleImporterTest` — export then import through a fake document store, already-present
  counting, Favorite-to-Personal reporting, missing file, empty library.
- `LibraryBundleDatabaseTest` (emulator) — real Room round trip: seed, export rows, wipe, import,
  compare stored JSON byte for byte; double import changes nothing; unreadable selection downgrades;
  damaged curve is reported and not written.
- `UiModernizationFlowsTest.settingsOffersLibraryBackupAndRestoreFromTheLibraryGroup` — the two rows
  exist in Library and invoke their callbacks.

No hardware is touched anywhere in this path: it reads and writes one file and one database.

