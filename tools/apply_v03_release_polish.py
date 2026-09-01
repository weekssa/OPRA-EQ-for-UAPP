#!/usr/bin/env python3
from __future__ import annotations

import json
import os
import subprocess
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]


def path(rel: str) -> Path:
    return ROOT / rel


def replace_once(rel: str, old: str, new: str) -> None:
    target = path(rel)
    text = target.read_text(encoding="utf-8")
    count = text.count(old)
    if count != 1:
        raise RuntimeError(f"Expected exactly one match in {rel}, found {count}: {old[:120]!r}")
    target.write_text(text.replace(old, new, 1), encoding="utf-8")


def insert_after(rel: str, anchor: str, addition: str) -> None:
    replace_once(rel, anchor, anchor + addition)


def write(rel: str, content: str) -> None:
    target = path(rel)
    target.parent.mkdir(parents=True, exist_ok=True)
    target.write_text(content, encoding="utf-8")


def run(*args: str) -> None:
    subprocess.run(args, cwd=ROOT, check=True)


# ---- Android Back behavior -------------------------------------------------
replace_once(
    "app/src/main/java/com/weekssa/opraeqforuapp/ui/EqLibraryApp.kt",
    "import android.net.Uri\nimport androidx.activity.compose.rememberLauncherForActivityResult",
    "import android.net.Uri\nimport androidx.activity.compose.BackHandler\nimport androidx.activity.compose.rememberLauncherForActivityResult",
)
replace_once(
    "app/src/main/java/com/weekssa/opraeqforuapp/ui/EqLibraryApp.kt",
    "    Scaffold(\n",
    "    BackHandler(enabled = selectedDestination == EqLibraryDestination.Settings) {\n"
    "        selectedManagedProductId = null\n"
    "        selectedDestinationIndex = EqLibraryDestination.MyEqs.ordinal\n"
    "    }\n\n"
    "    Scaffold(\n",
)
replace_once(
    "app/src/main/java/com/weekssa/opraeqforuapp/ui/EqLibraryApp.kt",
    "                        onToggleGeneralPreset = onToggleGeneralPreset,",
    "                        onToggleGeneralPreset = { preset ->\n"
    "                            val selected = onToggleGeneralPreset(preset)\n"
    "                            if (selected) requestExportGeneralEq(preset.id)\n"
    "                            selected\n"
    "                        },",
)
replace_once(
    "app/src/main/java/com/weekssa/opraeqforuapp/ui/EqLibraryApp.kt",
    "                        onOpenUrl = onOpenUrl,\n                        modifier = Modifier.fillMaxSize(),\n                    )\n                    EqLibraryDestination.Settings",
    "                        onOpenUrl = onOpenUrl,\n"
    "                        onBackFromRoot = {\n"
    "                            selectedManagedProductId = null\n"
    "                            selectedDestinationIndex = EqLibraryDestination.MyEqs.ordinal\n"
    "                        },\n"
    "                        modifier = Modifier.fillMaxSize(),\n"
    "                    )\n"
    "                    EqLibraryDestination.Settings",
)

replace_once(
    "app/src/main/java/com/weekssa/opraeqforuapp/ui/screens/BrowseOpraScreen.kt",
    "    onRefreshCatalog: () -> Unit,\n    onOpenUrl: (String) -> Unit,\n    modifier: Modifier = Modifier,",
    "    onRefreshCatalog: () -> Unit,\n"
    "    onOpenUrl: (String) -> Unit,\n"
    "    onBackFromRoot: () -> Unit,\n"
    "    modifier: Modifier = Modifier,",
)
replace_once(
    "app/src/main/java/com/weekssa/opraeqforuapp/ui/screens/BrowseOpraScreen.kt",
    "    BackHandler(\n        enabled = selectedProductId != null || selectedVendorId != null || searchQuery.isNotBlank(),\n    ) {\n        when {\n            selectedProductId != null -> selectedProductId = null\n            selectedVendorId != null -> selectedVendorId = null\n            else -> searchQuery = \"\"\n        }\n    }",
    "    BackHandler {\n"
    "        when {\n"
    "            selectedProductId != null -> selectedProductId = null\n"
    "            selectedVendorId != null -> selectedVendorId = null\n"
    "            searchQuery.isNotBlank() -> searchQuery = \"\"\n"
    "            else -> onBackFromRoot()\n"
    "        }\n"
    "    }",
)

replace_once(
    "app/src/main/java/com/weekssa/opraeqforuapp/ui/screens/ProfileSelectionEditor.kt",
    "    BackHandler(enabled = dirty) { showDiscardDialog = true }",
    "    BackHandler { requestBack() }",
)

# ---- Favorite/star access from My EQs -------------------------------------
replace_once(
    "app/src/main/java/com/weekssa/opraeqforuapp/ui/screens/ManagedHeadphoneDetailScreen.kt",
    "import androidx.compose.material.icons.automirrored.outlined.ArrowBack\n",
    "import androidx.compose.material.icons.automirrored.outlined.ArrowBack\n"
    "import androidx.compose.material.icons.outlined.Star\n"
    "import androidx.compose.material.icons.outlined.StarBorder\n",
)
replace_once(
    "app/src/main/java/com/weekssa/opraeqforuapp/ui/screens/ManagedHeadphoneDetailScreen.kt",
    "import androidx.compose.material3.Icon\nimport androidx.compose.material3.ListItem",
    "import androidx.compose.material3.Icon\nimport androidx.compose.material3.IconButton\nimport androidx.compose.material3.ListItem",
)
replace_once(
    "app/src/main/java/com/weekssa/opraeqforuapp/ui/screens/ManagedHeadphoneDetailScreen.kt",
    "                ManagedProfileRow(\n                    profile = profile,\n                    activeOutput = activeOutput,",
    "                ManagedProfileRow(\n"
    "                    profile = profile,\n"
    "                    isFavorite = profile.profileId in favoriteProfileIds,\n"
    "                    activeOutput = activeOutput,",
)
replace_once(
    "app/src/main/java/com/weekssa/opraeqforuapp/ui/screens/ManagedHeadphoneDetailScreen.kt",
    "                    onOpenSource = profile.lastKnownProfile.link?.let { sourceUrl -> { onOpenUrl(sourceUrl) } },\n                    onRemove = {",
    "                    onOpenSource = profile.lastKnownProfile.link?.let { sourceUrl -> { onOpenUrl(sourceUrl) } },\n"
    "                    onToggleFavorite = {\n"
    "                        scope.launch {\n"
    "                            val favorited = onToggleFavorite(\n"
    "                                profile.lastKnownProfile,\n"
    "                                headphone.vendorName,\n"
    "                                headphone.productName,\n"
    "                            )\n"
    "                            onMessage(\n"
    "                                if (favorited) {\n"
    "                                    \"Saved to My EQs favorites.\"\n"
    "                                } else {\n"
    "                                    \"Removed from My EQs favorites.\"\n"
    "                                },\n"
    "                            )\n"
    "                        }\n"
    "                    },\n"
    "                    onRemove = {",
)
replace_once(
    "app/src/main/java/com/weekssa/opraeqforuapp/ui/screens/ManagedHeadphoneDetailScreen.kt",
    "private fun ManagedProfileRow(\n    profile: ManagedProfileRecord,\n    activeOutput: ExportDevice,",
    "private fun ManagedProfileRow(\n"
    "    profile: ManagedProfileRecord,\n"
    "    isFavorite: Boolean,\n"
    "    activeOutput: ExportDevice,",
)
replace_once(
    "app/src/main/java/com/weekssa/opraeqforuapp/ui/screens/ManagedHeadphoneDetailScreen.kt",
    "    onFlash: () -> Unit,\n    onOpenSource: (() -> Unit)?,\n    onRemove: (() -> Unit)?,",
    "    onFlash: () -> Unit,\n"
    "    onOpenSource: (() -> Unit)?,\n"
    "    onToggleFavorite: () -> Unit,\n"
    "    onRemove: (() -> Unit)?,",
)
replace_once(
    "app/src/main/java/com/weekssa/opraeqforuapp/ui/screens/ManagedHeadphoneDetailScreen.kt",
    "            Row(verticalAlignment = Alignment.CenterVertically) {\n                if (showExport) {",
    "            Row(verticalAlignment = Alignment.CenterVertically) {\n"
    "                IconButton(onClick = onToggleFavorite) {\n"
    "                    Icon(\n"
    "                        imageVector = if (isFavorite) Icons.Outlined.Star else Icons.Outlined.StarBorder,\n"
    "                        contentDescription = if (isFavorite) \"Remove favorite\" else \"Add favorite\",\n"
    "                    )\n"
    "                }\n"
    "                if (showExport) {",
)

replace_once(
    "app/src/main/java/com/weekssa/opraeqforuapp/ui/screens/MyEqsHomeScreen.kt",
    "                        leadingContent = if (record.kind == SavedEqKind.Favorite) {\n                            { Icon(Icons.Outlined.Star, contentDescription = null) }\n                        } else {\n                            null\n                        },\n",
    "",
)
replace_once(
    "app/src/main/java/com/weekssa/opraeqforuapp/ui/screens/MyEqsHomeScreen.kt",
    "                                IconButton(\n                                    onClick = {\n                                        scope.launch {\n                                            onDeleteSavedEq(record.entryId)\n                                            onMessage(\"EQ removed from My EQs. Existing exported files were kept.\")\n                                        }\n                                    },\n                                ) {\n                                    Icon(Icons.Outlined.Delete, contentDescription = \"Remove ${record.displayName}\")\n                                }",
    "                                if (record.kind == SavedEqKind.Favorite) {\n"
    "                                    IconButton(\n"
    "                                        onClick = {\n"
    "                                            scope.launch {\n"
    "                                                onDeleteSavedEq(record.entryId)\n"
    "                                                onMessage(\"Removed from My EQs favorites. Existing exported files were kept.\")\n"
    "                                            }\n"
    "                                        },\n"
    "                                    ) {\n"
    "                                        Icon(\n"
    "                                            Icons.Outlined.Star,\n"
    "                                            contentDescription = \"Remove ${record.displayName} from favorites\",\n"
    "                                        )\n"
    "                                    }\n"
    "                                } else {\n"
    "                                    IconButton(\n"
    "                                        onClick = {\n"
    "                                            scope.launch {\n"
    "                                                onDeleteSavedEq(record.entryId)\n"
    "                                                onMessage(\"EQ removed from My EQs. Existing exported files were kept.\")\n"
    "                                            }\n"
    "                                        },\n"
    "                                    ) {\n"
    "                                        Icon(Icons.Outlined.Delete, contentDescription = \"Remove ${record.displayName}\")\n"
    "                                    }\n"
    "                                }",
)

# ---- General EQ safety/projection -----------------------------------------
replace_once(
    "tools/general_preset_ingest.py",
    "ALLOWED_PURPOSES = {\"effect\", \"genre\"}\n",
    "from curated_community_publish import safety_headroom_db, signature\n\n"
    "ALLOWED_PURPOSES = {\"effect\", \"genre\"}\n",
)
replace_once(
    "tools/general_preset_ingest.py",
    "    fingerprint = acoustic_fingerprint(parsed)\n    publication_eligible = redistribution_policy == \"structured-data-only\"\n    return {",
    "    fingerprint = acoustic_fingerprint(parsed)\n"
    "    publication_eligible = redistribution_policy == \"structured-data-only\"\n"
    "    generated_headroom_db = None\n"
    "    if publication_eligible and parsed.preamp_db is None:\n"
    "        _, max_boost_db, _ = signature(parsed.filters)\n"
    "        generated_headroom_db = safety_headroom_db(max_boost_db)\n"
    "    return {",
)
replace_once(
    "tools/general_preset_ingest.py",
    "                \"preamp_gain_db\": parsed.preamp_db,\n                \"filters\": parsed.filters if publication_eligible else [],",
    "                \"preamp_gain_db\": parsed.preamp_db,\n"
    "                \"eq_library_safety_headroom_db\": generated_headroom_db,\n"
    "                \"filters\": parsed.filters if publication_eligible else [],",
)
replace_once(
    "tools/test_general_preset_ingest.py",
    "    def test_missing_source_preamp_remains_null(self):\n        candidate = self.candidate(include_preamp=False)\n        self.assertIsNone(candidate[\"revisions\"][0][\"preamp_gain_db\"])\n",
    "    def test_missing_source_preamp_remains_null(self):\n"
    "        candidate = self.candidate(include_preamp=False)\n"
    "        revision = candidate[\"revisions\"][0]\n"
    "        self.assertIsNone(revision[\"preamp_gain_db\"])\n"
    "        self.assertIsNotNone(revision[\"eq_library_safety_headroom_db\"])\n"
    "        self.assertLessEqual(revision[\"eq_library_safety_headroom_db\"], 0.0)\n",
)
replace_once(
    "app/src/main/java/com/weekssa/opraeqforuapp/domain/library/CanonicalLegacyCatalogAdapter.kt",
    "                    soundImpactSummary = revision.soundImpactSummary,",
    "                    soundImpactSummary = revision.soundImpactSummary\n"
    "                        ?: SoundImpactSummary.fromFilters(revision.filters)\n"
    "                            ?.let { \"EQ Library summary: $it\" },",
)

# ---- Qualified ParaEQ General EQ source ----------------------------------
publisher = r'''#!/usr/bin/env python3
"""Publish qualified source-authored General EQ manifests into the canonical catalog."""

from __future__ import annotations

import argparse
import json
from pathlib import Path
from typing import Any

from catalog_merge import merge_candidates
from community_peq_ingest import parse_peq
from general_preset_ingest import build_candidate


def _registry_source(registry: dict[str, Any], source_id: str) -> dict[str, Any]:
    for source in registry.get("sources") or []:
        if str(source.get("id") or "") == source_id:
            return source
    raise ValueError(f"General preset source missing from registry: {source_id}")


def manifest_candidates(manifest: dict[str, Any], registry: dict[str, Any]) -> list[dict[str, Any]]:
    source_id = str(manifest.get("source_id") or "").strip()
    source = _registry_source(registry, source_id)
    if source.get("lifecycle") != "active":
        raise ValueError(f"General preset source is not active: {source_id}")
    if source.get("redistribution") != "structured-data-only":
        raise ValueError(f"General preset source is not publication-qualified: {source_id}")

    source_kind = str(manifest.get("source_kind") or source.get("kind") or "").strip()
    if source_kind != str(source.get("kind") or "").strip():
        raise ValueError(f"General preset source kind does not match registry: {source_id}")

    creator = str(manifest.get("creator") or "").strip()
    source_url = str(manifest.get("source_url") or "").strip()
    source_version = str(manifest.get("source_version") or "").strip() or None
    verification_status = str(manifest.get("verification_status") or "verified").strip().lower()
    discovered_at = manifest.get("discovered_at_epoch_seconds")
    presets = manifest.get("presets") or []
    if not presets:
        raise ValueError("General preset manifest contains no presets")

    candidates: list[dict[str, Any]] = []
    seen_record_ids: set[str] = set()
    for preset in presets:
        record_id = str(preset.get("source_record_id") or "").strip()
        if not record_id or record_id in seen_record_ids:
            raise ValueError("Every General preset needs a unique source_record_id")
        seen_record_ids.add(record_id)
        candidate = build_candidate(
            parse_peq(str(preset.get("peq_text") or "")),
            purpose=str(preset.get("purpose") or ""),
            creator=str(preset.get("creator") or creator),
            tuning_label=str(preset.get("tuning_label") or ""),
            source_id=source_id,
            source_kind=source_kind,
            source_url=str(preset.get("source_url") or source_url),
            source_record_id=record_id,
            redistribution_policy="structured-data-only",
            source_version=str(preset.get("source_version") or source_version or "") or None,
            discovered_at_epoch_seconds=discovered_at if isinstance(discovered_at, int) else None,
            verification_status=str(preset.get("verification_status") or verification_status),
        )
        summary = str(preset.get("sound_impact_summary") or "").strip()
        if summary:
            candidate["revisions"][0]["sound_impact_summary"] = summary
        candidates.append(candidate)
    return candidates


def publish_manifest(
    snapshot: dict[str, Any],
    manifest: dict[str, Any],
    registry: dict[str, Any],
) -> tuple[dict[str, Any], dict[str, int]]:
    generated_at = max(
        str(snapshot.get("generated_at") or ""),
        str(manifest.get("catalog_generated_at") or ""),
    ) or None
    return merge_candidates(
        snapshot,
        manifest_candidates(manifest, registry),
        generated_at=generated_at,
        source_registry_version=str(registry.get("registry_version") or "") or None,
    )


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--catalog", type=Path, required=True)
    parser.add_argument("--manifest", type=Path, required=True)
    parser.add_argument("--registry", type=Path, required=True)
    parser.add_argument("--output", type=Path, required=True)
    parser.add_argument("--report", type=Path)
    args = parser.parse_args()

    snapshot = json.loads(args.catalog.read_text(encoding="utf-8"))
    manifest = json.loads(args.manifest.read_text(encoding="utf-8"))
    registry = json.loads(args.registry.read_text(encoding="utf-8"))
    merged, outcomes = publish_manifest(snapshot, manifest, registry)
    args.output.parent.mkdir(parents=True, exist_ok=True)
    args.output.write_text(json.dumps(merged, indent=2, ensure_ascii=False) + "\n", encoding="utf-8")

    report = {
        "source_id": manifest.get("source_id"),
        "candidate_count": len(manifest.get("presets") or []),
        "outcomes": outcomes,
    }
    if args.report:
        args.report.parent.mkdir(parents=True, exist_ok=True)
        args.report.write_text(json.dumps(report, indent=2, sort_keys=True) + "\n", encoding="utf-8")
    print(json.dumps(report, sort_keys=True))
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
'''
write("tools/general_preset_publish.py", publisher)

publisher_test = r'''import unittest

from general_preset_publish import publish_manifest


class GeneralPresetPublishTest(unittest.TestCase):
    def registry(self):
        return {
            "schema_version": 1,
            "registry_version": "test-general",
            "sources": [
                {
                    "id": "example-general",
                    "kind": "community_repository",
                    "lifecycle": "active",
                    "redistribution": "structured-data-only",
                }
            ],
        }

    def manifest(self):
        return {
            "schema_version": 1,
            "source_id": "example-general",
            "source_kind": "community_repository",
            "creator": "Example Creator",
            "source_url": "https://example.test/source",
            "source_version": "abc123",
            "verification_status": "verified",
            "discovered_at_epoch_seconds": 1788220800,
            "catalog_generated_at": "2026-09-01T03:00:00Z",
            "presets": [
                {
                    "source_record_id": "sound",
                    "purpose": "effect",
                    "tuning_label": "Bass Boost",
                    "peq_text": "Filter 1: ON LS Fc 80 Hz Gain 4.0 dB Q 0.7\nFilter 2: ON PK Fc 200 Hz Gain 1.0 dB Q 1.0",
                },
                {
                    "source_record_id": "genre",
                    "purpose": "genre",
                    "tuning_label": "Rock",
                    "sound_impact_summary": "Source-authored genre intent.",
                    "peq_text": "Filter 1: ON PK Fc 1000 Hz Gain 2.0 dB Q 1.0",
                },
            ],
        }

    def snapshot(self):
        return {
            "schema_version": 1,
            "generated_at": "2026-08-31T00:00:00Z",
            "source_registry_version": "old",
            "profiles": [],
        }

    def test_publishes_sound_and_genre_without_fake_headphones(self):
        merged, outcomes = publish_manifest(self.snapshot(), self.manifest(), self.registry())
        self.assertEqual(2, outcomes["new_profile"])
        self.assertEqual({"effect", "genre"}, {profile["purpose"] for profile in merged["profiles"]})
        self.assertTrue(all(profile["scope"] == "general" for profile in merged["profiles"]))
        self.assertTrue(all(profile["headphone"] is None for profile in merged["profiles"]))
        sound = next(profile for profile in merged["profiles"] if profile["purpose"] == "effect")
        revision = sound["revisions"][0]
        self.assertIsNone(revision["preamp_gain_db"])
        self.assertLess(revision["eq_library_safety_headroom_db"], 0.0)
        genre = next(profile for profile in merged["profiles"] if profile["purpose"] == "genre")
        self.assertEqual("Source-authored genre intent.", genre["revisions"][0]["sound_impact_summary"])
        self.assertEqual("test-general", merged["source_registry_version"])

    def test_rejects_non_publishable_registry_source(self):
        registry = self.registry()
        registry["sources"][0]["redistribution"] = "review-required"
        with self.assertRaisesRegex(ValueError, "not publication-qualified"):
            publish_manifest(self.snapshot(), self.manifest(), registry)


if __name__ == "__main__":
    unittest.main()
'''
write("tools/test_general_preset_publish.py", publisher_test)

freqs = [31, 63, 125, 250, 500, 1000, 2000, 4000, 8000, 16000]

def peq_text(gains: list[float]) -> str:
    lines = []
    for index, (freq, gain) in enumerate(zip(freqs, gains), start=1):
        kind = "LS" if index == 1 else ("HS" if index == len(freqs) else "PK")
        lines.append(f"Filter {index}: ON {kind} Fc {freq} Hz Gain {gain:g} dB Q 1.41")
    return "\n".join(lines)

manifest = {
    "schema_version": 1,
    "source_id": "paraeq",
    "source_kind": "community_repository",
    "creator": "wabsto1",
    "source_url": "https://github.com/wabsto1/ParaEQ/blob/51d6f6d29aef607a3a2e26b829549f9c8d7fec6e/Sources/Models.swift",
    "source_version": "51d6f6d29aef607a3a2e26b829549f9c8d7fec6e",
    "verification_status": "verified",
    "discovered_at_epoch_seconds": 1788220800,
    "catalog_generated_at": "2026-09-01T03:00:00Z",
    "presets": [
        {
            "source_record_id": "EQPreset.bassBoost",
            "purpose": "effect",
            "tuning_label": "Bass Boost",
            "peq_text": peq_text([6.0, 4.5, 2.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0]),
        },
        {
            "source_record_id": "EQPreset.vocalClarity",
            "purpose": "effect",
            "tuning_label": "Vocal Clarity",
            "peq_text": peq_text([-2.0, -1.5, 0.0, 2.0, 3.0, 3.5, 2.0, 0.0, 0.0, 0.0]),
        },
        {
            "source_record_id": "EQPreset.trebleBoost",
            "purpose": "effect",
            "tuning_label": "Treble Boost",
            "peq_text": peq_text([0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 3.0, 4.5, 5.0]),
        },
        {
            "source_record_id": "EQPreset.loudness",
            "purpose": "effect",
            "tuning_label": "Loudness",
            "sound_impact_summary": "Equal-loudness compensation for low listening levels (mild V shape).",
            "peq_text": peq_text([5.0, 4.0, 2.0, 0.0, -1.0, -1.0, 0.0, 1.5, 3.0, 3.5]),
        },
        {
            "source_record_id": "EQPreset.podcast",
            "purpose": "effect",
            "tuning_label": "Podcast",
            "sound_impact_summary": "Spoken word: cut rumble, lift presence and articulation.",
            "peq_text": peq_text([-8.0, -4.0, 0.0, 1.0, 2.0, 3.0, 3.0, 2.0, 0.5, -1.0]),
        },
        {
            "source_record_id": "EQPreset.electronic",
            "purpose": "genre",
            "tuning_label": "Electronic",
            "sound_impact_summary": "Sub-bass weight plus air, mids untouched.",
            "peq_text": peq_text([4.5, 3.5, 1.0, 0.0, -0.5, 0.0, 0.5, 1.0, 2.5, 3.5]),
        },
        {
            "source_record_id": "EQPreset.rock",
            "purpose": "genre",
            "tuning_label": "Rock",
            "sound_impact_summary": "Gentle V with guitar presence around 2–4 kHz.",
            "peq_text": peq_text([3.0, 2.0, 0.5, -0.5, -1.0, 0.5, 2.0, 2.5, 2.0, 1.0]),
        },
    ],
}
write(
    "catalog/discovery/paraeq_general_presets.json",
    json.dumps(manifest, indent=2, sort_keys=True, ensure_ascii=False) + "\n",
)

registry_path = path("config/source_registry.json")
registry = json.loads(registry_path.read_text(encoding="utf-8"))
if registry.get("registry_version") != "0.3.0-community-public-4":
    raise RuntimeError(f"Unexpected source registry version: {registry.get('registry_version')}")
if any(source.get("id") == "paraeq" for source in registry.get("sources") or []):
    raise RuntimeError("ParaEQ source already exists")
registry["registry_version"] = "0.3.0-community-public-5"
registry["sources"].append(
    {
        "id": "paraeq",
        "kind": "community_repository",
        "name": "ParaEQ built-in General EQ presets",
        "scope": "MIT-licensed source-authored device-independent tonal, utility, and genre presets from wabsto1/ParaEQ",
        "lifecycle": "active",
        "cadence": "manual",
        "parser": "general-preset-qualified-peq",
        "parser_version": "1",
        "cursor_strategy": "pinned repository commit plus source-record acoustic fingerprint",
        "redistribution": "structured-data-only",
        "attribution_required": True,
        "license_notes": "ParaEQ is MIT licensed (copyright 2026 wabsto1). EQ Library publishes only the exact built-in preset filter parameters explicitly represented in Sources/Models.swift at pinned commit 51d6f6d29aef607a3a2e26b829549f9c8d7fec6e, retains creator/source provenance, keeps missing source preamp null, and stores any EQ Library-generated safety headroom separately.",
    }
)
registry_path.write_text(json.dumps(registry, indent=2, ensure_ascii=False) + "\n", encoding="utf-8")

# Long-term deterministic General-preset publication workflow.
general_workflow = r'''name: General EQ catalog publication

on:
  push:
    branches:
      - main
      - 'v0.3-*'
    paths:
      - '.github/workflows/general-preset-currentness.yml'
      - 'tools/general_preset_publish.py'
      - 'tools/general_preset_ingest.py'
      - 'tools/curated_community_publish.py'
      - 'tools/catalog_merge.py'
      - 'catalog/discovery/*_general_presets.json'
      - 'config/source_registry.json'
  workflow_dispatch:

permissions:
  contents: write

concurrency:
  group: eq-catalog-writer-${{ github.ref }}
  cancel-in-progress: false

jobs:
  publish-general:
    if: github.event_name != 'push' || github.actor != 'github-actions[bot]'
    runs-on: ubuntu-latest
    timeout-minutes: 20
    steps:
      - name: Check out current branch tip
        uses: actions/checkout@3d3c42e5aac5ba805825da76410c181273ba90b1 # v7
        with:
          ref: ${{ github.ref_name }}
          fetch-depth: 1

      - name: Validate source registry
        run: python3 tools/catalog_pipeline.py validate-registry --registry config/source_registry.json

      - name: Test General EQ ingestion
        run: python3 -m unittest tools/test_general_preset_ingest.py tools/test_general_preset_publish.py

      - name: Publish qualified General EQ manifests
        shell: bash
        run: |
          set -euo pipefail
          mkdir -p "${RUNNER_TEMP}/general-reports"
          cp catalog/catalog.json "${RUNNER_TEMP}/catalog.general.current.json"
          mapfile -t manifests < <(find catalog/discovery -maxdepth 1 -type f -name '*_general_presets.json' -print | sort)
          if (( ${#manifests[@]} == 0 )); then
            echo "No General EQ manifests found."
            exit 1
          fi
          for manifest in "${manifests[@]}"; do
            base="$(basename "${manifest}" .json)"
            next="${RUNNER_TEMP}/catalog.general.next.json"
            report="${RUNNER_TEMP}/general-reports/${base}-report.json"
            python3 tools/general_preset_publish.py \
              --catalog "${RUNNER_TEMP}/catalog.general.current.json" \
              --manifest "${manifest}" \
              --registry config/source_registry.json \
              --output "${next}" \
              --report "${report}"
            mv "${next}" "${RUNNER_TEMP}/catalog.general.current.json"
          done

      - name: Validate candidate atomically
        run: >-
          python3 tools/catalog_pipeline.py publish
          --candidate "${RUNNER_TEMP}/catalog.general.current.json"
          --published "${RUNNER_TEMP}/validated-general-catalog.json"
          --last-known-good "${RUNNER_TEMP}/validated-general-lkg.json"

      - name: Minify Android transport catalog
        run: |
          python3 - <<'PY'
          import json, os
          path = os.environ['RUNNER_TEMP'] + '/validated-general-catalog.json'
          with open(path, encoding='utf-8') as handle:
              payload = json.load(handle)
          with open(path, 'w', encoding='utf-8') as handle:
              handle.write(json.dumps(payload, sort_keys=True, separators=(',', ':'), ensure_ascii=False) + '\n')
          PY

      - name: Stage validated General EQ catalog
        run: cp "${RUNNER_TEMP}/validated-general-catalog.json" catalog/catalog.json

      - name: Commit General EQ catalog changes
        run: |
          set -euo pipefail
          if git diff --quiet -- catalog/catalog.json; then
            echo "General EQ catalog already current."
            exit 0
          fi
          git config user.name "github-actions[bot]"
          git config user.email "41898282+github-actions[bot]@users.noreply.github.com"
          git add catalog/catalog.json
          git commit -m "Refresh General EQ catalog"
          git pull --rebase origin "${GITHUB_REF_NAME}"
          git push origin "HEAD:${GITHUB_REF_NAME}"

      - name: Upload General EQ publication reports
        uses: actions/upload-artifact@ea165f8d65b6e75b540449e92b4886f43607fa02 # v4
        with:
          name: general-eq-publication-${{ github.run_id }}
          path: |
            ${{ runner.temp }}/general-reports/
            ${{ runner.temp }}/validated-general-catalog.json
          retention-days: 14
'''
write(".github/workflows/general-preset-currentness.yml", general_workflow)

# Publish the new manifest into the branch catalog now.
tmp_catalog = path("catalog/catalog.general-polish.json")
tmp_report = path("catalog/general-polish-report.json")
run(
    sys.executable,
    "tools/general_preset_publish.py",
    "--catalog",
    "catalog/catalog.json",
    "--manifest",
    "catalog/discovery/paraeq_general_presets.json",
    "--registry",
    "config/source_registry.json",
    "--output",
    str(tmp_catalog.relative_to(ROOT)),
    "--report",
    str(tmp_report.relative_to(ROOT)),
)
payload = json.loads(tmp_catalog.read_text(encoding="utf-8"))
path("catalog/catalog.json").write_text(
    json.dumps(payload, sort_keys=True, separators=(",", ":"), ensure_ascii=False) + "\n",
    encoding="utf-8",
)
tmp_catalog.unlink()
tmp_report.unlink()

# ---- Documentation / release source of truth ------------------------------
runbook_nav_anchor = "The active output is an **operating context**, not a catalog filter. Changing output changes conversion/export/Flash capability and the output-specific My EQs collection, but does not hide otherwise valid canonical curves from EQ Library.\n"
insert_after(
    "docs/CHATGPT_PROJECT_RUNBOOK.md",
    runbook_nav_anchor,
    "\nAndroid Back follows the in-app hierarchy before leaving the activity: selection editor → headphone detail → My EQs, EQ Library model → manufacturer/search → EQ Library root, and root EQ Library/Settings → My EQs. Only Back from the My EQs root exits the app. Visible back arrows and the Android Back gesture/button must agree.\n\nFavorites are manageable from both EQ Library and My EQs. A managed-profile row in My EQs exposes the same filled/outlined star state as EQ Library; toggling the star changes only the active-output Favorite membership and must not change headphone selection, export currentness, or Flash state. Favorite snapshot rows use the filled star as the remove-from-favorites action; personal imports retain their explicit remove action.\n",
)
replace_once(
    "docs/CHATGPT_PROJECT_RUNBOOK.md",
    "EQ Library contains headphone EQs and, where populated, General EQs. Headphone browse starts Manufacturer → Model and may include deeper verified source segments only when the source genuinely requires them. Never invent variants or meanings from IDs, filenames, or path fragments.",
    "EQ Library contains headphone EQs and General EQs. The initial qualified General EQ seed is sourced from the MIT-licensed ParaEQ built-in preset definitions and includes Sound, Utility, and source-authored Genre examples; the canonical catalog keeps exact source coefficients/preamp state and separate EQ Library-generated safety headroom when the source omits preamp. Headphone browse starts Manufacturer → Model and may include deeper verified source segments only when the source genuinely requires them. Never invent variants or meanings from IDs, filenames, or path fragments.",
)
replace_once(
    "docs/CHATGPT_PROJECT_RUNBOOK.md",
    "v0.3.0 is in GitHub release preparation. `docs/releases/v0.3.0.md` and the dated changelog are prepared. The final release head may contain documentation or catalog-only commits after the hardware-tested merge; it must pass the signed release workflow and pinned-signing verification before public publication. Any subsequent code/DSP/device-behavior change requires renewed validation rather than inheriting the prior hardware pass automatically.",
    "v0.3.0 is in final release-polish validation on branch `v0.3-release-polish`. The approved final polish adds hierarchical Android Back behavior, Favorite-star controls in My EQs, and the first populated qualified General EQ catalog. Because Android UI code changed after the prior hardware-tested merge, a fresh signed candidate and focused Pixel 9 Back/Favorite/General-EQ regression pass are required before public publication; Black Pearl protocol requalification is not required unless the final diff unexpectedly touches device/DSP behavior.",
)

architecture_anchor = "General presets remain standalone in v0.3. Do not silently layer/combine them with headphone-specific EQs.\n"
insert_after(
    "docs/ARCHITECTURE.md",
    architecture_anchor,
    "\nThe initial populated General EQ source is the MIT-licensed `wabsto1/ParaEQ` built-in preset set at a pinned source commit. EQ Library publishes exact source-authored filter parameters and labels through the generic General-preset pipeline; `Electronic` and `Rock` are Genre only because the source explicitly names them that way. A missing source preamp remains null and conservative playback safety headroom is stored only in `eq_library_safety_headroom_db`.\n",
)
my_eq_anchor = "My EQs is output-specific and may contain different saved profiles for different outputs without duplicating canonical source data.\n"
insert_after(
    "docs/ARCHITECTURE.md",
    my_eq_anchor,
    "\nAndroid Back uses My EQs as the start destination: nested management/detail states unwind first; root EQ Library and Settings return to My EQs; only Back at the My EQs root exits. Favorite membership is editable from managed My EQs rows as well as EQ Library using the same star state, without mutating selection/export/Flash state.\n",
)

phase_intro = "This document records implementation-time product decisions that refine the approved Phase 0 behavior. `docs/CHATGPT_PROJECT_RUNBOOK.md` remains authoritative, and `docs/V0.3_LOCKED_EXECUTION_PLAN.md` is the controlling v0.3 execution plan. Where an older prototype decision conflicts with this file or the locked plan, the locked v0.3 behavior below wins.\n"
insert_after(
    "docs/PHASE1_DECISIONS.md",
    phase_intro,
    "\n## Final v0.3 Android Back behavior — approved 2026-08-31\n\n- Back must unwind in-app hierarchy before exiting: Manage preset selection → headphone detail → My EQs; EQ Library model → manufacturer/search/root; root EQ Library or Settings → My EQs.\n- Only Back from the My EQs root exits the app.\n- Unsaved selection edits keep the existing Discard changes / Keep editing gate before navigation.\n- Visible back arrows and the Android system Back gesture/button must produce the same destination.\n\n## Favorite access from My EQs — approved 2026-08-31\n\n- Managed headphone preset rows in My EQs expose the same filled/outlined Favorite star state used in EQ Library.\n- Toggling Favorite changes only Favorite membership for the active output; it does not select/deselect the managed preset, change export currentness, or trigger/alter Black Pearl Flash.\n- Favorite snapshot rows use the filled star as the remove-from-favorites action; personal imports keep their explicit remove action.\n\n## Initial populated General EQ catalog — approved 2026-08-31\n\n- Seed General EQs from qualified, source-authored presets rather than inventing unlabeled curves.\n- The first source is the MIT-licensed ParaEQ built-in preset set (`wabsto1/ParaEQ`, pinned source commit), publishing Bass Boost, Vocal Clarity, Treble Boost, Loudness, Podcast, Electronic, and Rock.\n- Electronic and Rock are Genre only because ParaEQ explicitly provides those genre names; no genre label is inferred from filter shape.\n- Source preamp remains null when absent. EQ Library-generated clipping-safety headroom is stored separately as derived metadata.\n- Selecting a General EQ adds it to the active output and initiates its initial export under the same Add/Save rule as other exportable saved EQs.\n",
)

source_anchor = "Explicitly qualified repositories/files are publication inputs; broad GitHub/Gist discovery remains review-only until originality and licensing are established.\n"
insert_after(
    "docs/SOURCE_INGESTION_STRATEGY.md",
    source_anchor,
    "\nInitial qualified General-EQ repository source: `wabsto1/ParaEQ`. Its built-in preset definitions are MIT licensed and source-authored in `Sources/Models.swift`; EQ Library pins the reviewed commit, republishes only exact structured EQ parameters/labels with attribution, and uses the generic `*_general_presets.json` publication lane. Missing source preamp stays null and generated safety headroom remains separate derived metadata.\n",
)

locked_general = "Only classify a preset as Sound/Genre/Utility when the source itself provides enough intent/context to support that classification. Preserve exact coefficients and source terminology. Do not invent a genre claim from filter shape alone.\n"
insert_after(
    "docs/V0.3_LOCKED_EXECUTION_PLAN.md",
    locked_general,
    "\nFinal release-polish implementation uses the MIT-licensed ParaEQ built-ins as the first populated General EQ source. The qualified seed contains Bass Boost, Vocal Clarity, Treble Boost, Loudness, Podcast, Electronic, and Rock; Electronic/Rock are Genre because the source explicitly names them as such.\n",
)
locked_nav = "The active output/device is an operating context, **not a catalog visibility filter**. Selecting Black Pearl, UAPP, Poweramp, Wavelet, or another output must never hide otherwise valid canonical EQ curves from EQ Library.\n"
insert_after(
    "docs/V0.3_LOCKED_EXECUTION_PLAN.md",
    locked_nav,
    "\nFinal release-polish Back behavior uses My EQs as the start destination: nested EQ Library/My EQs states unwind first, root EQ Library and Settings return to My EQs, and only Back at My EQs root exits. Managed My EQs preset rows also expose the Favorite star so users do not need to return to EQ Library merely to favorite/unfavorite a saved profile.\n",
)

# Focused release-polish hands-on addendum at the top of the existing historical checklist.
replace_once(
    "docs/V0.3_HANDS_ON_CHECKLIST.md",
    "Use the latest **signed beta** artifact from `eq-library-community-v0.3`. Do not use the ordinary debug APK for the upgrade test.\n\nThis is the final Pixel 9 / TRN Black Pearl checkpoint before v0.3 can move toward `main` or public release. PR #3 stays draft and unmerged until this checklist passes.\n",
    "Use the latest **signed beta** artifact from `v0.3-release-polish` for the final release-polish pass. Do not use an ordinary debug APK for the upgrade test.\n\nThe full checklist below records the already-passed v0.3 foundation/Black Pearl qualification. For the final post-merge UI/catalog polish, run the focused addendum immediately below plus a quick UAPP/Black Pearl smoke check; repeat the deeper hardware sections only if the final diff unexpectedly touches device/DSP behavior.\n\n## Final release-polish addendum\n\n1. **Back hierarchy:** From My EQs → headphone → Manage preset selection, use Android Back repeatedly. Clean editor → headphone detail → My EQs root must occur before the app exits. With unsaved editor changes, Back must show Discard changes / Keep editing. In EQ Library, Model → Manufacturer/search → root must unwind naturally; Back from root EQ Library and from Settings must return to My EQs; only Back from My EQs root exits. Check the visible back arrows match system Back.\n2. **Favorite stars in My EQs:** Open a managed headphone. Confirm every managed preset row has an outlined/filled star matching its Favorite state. Toggle one on and off without changing its selected state, export status, or Flash state. Confirm a Favorite snapshot row uses the filled star to unfavorite, while a personal import still uses its remove action.\n3. **Populated General EQs:** Open EQ Library → General EQs. Confirm the catalog contains the qualified ParaEQ seed: Bass Boost, Vocal Clarity, Treble Boost, Loudness, Podcast, Electronic, and Rock. Confirm Sound/Genre/Utility filtering is sensible, Electronic/Rock appear as Genre, source links/creator attribution are present, and no preset is silently layered with a headphone EQ.\n4. **General Add/export:** Add one General EQ for UAPP/ToneBoosters. Confirm it appears in My EQs for that output and its initial export starts automatically (including folder selection if needed). Confirm the exported preset applies the source coefficients and separate generated safety headroom when source preamp is absent.\n5. **Regression smoke:** Import one freshly exported UAPP/ToneBoosters preset. If Black Pearl is available, connect and flash one previously validated ordinary in-range preset to confirm the UI/navigation polish did not disturb the existing Flash path. A repeat of the -11.9 dB hardware qualification is not required unless related Black Pearl code changed.\n\n**Focused PASS:** all five release-polish checks pass; the final signed candidate may proceed to the public v0.3.0 release gate.\n",
)

# Changelog and release notes remain explicit that a fresh final candidate is required.
insert_after(
    "CHANGELOG.md",
    "### Added\n\n",
    "- Initial populated **General EQ** catalog from the qualified MIT-licensed ParaEQ built-in presets: Bass Boost, Vocal Clarity, Treble Boost, Loudness, Podcast, Electronic, and Rock, with source-authored Genre classification only where explicitly provided.\n- Managed preset rows in **My EQs** now expose the same Favorite star state/action as EQ Library.\n",
)
insert_after(
    "CHANGELOG.md",
    "### Changed\n\n",
    "- Android Back now follows the in-app hierarchy and returns root EQ Library/Settings to My EQs; only Back from the My EQs root exits the app. Clean and dirty preset-selection editor states both handle system Back naturally.\n- General EQ selection now initiates its initial active-output export when added, matching the established Add/Save workflow.\n- General presets with no source preamp keep preamp null while EQ Library stores conservative generated playback headroom separately.\n",
)
insert_after(
    "CHANGELOG.md",
    "### Fixed\n\n",
    "- System Back no longer falls through and exits the activity from clean nested management screens or secondary top-level destinations.\n- Favorites no longer require returning to EQ Library merely to star/unstar a managed preset.\n- The previously empty General EQ user-facing area now has qualified source-backed Sound, Genre, and Utility content.\n",
)
insert_after(
    "CHANGELOG.md",
    "### Validation\n\n",
    "- Final release-polish changes are isolated from Black Pearl protocol/DSP code but require a fresh signed v0.3.0 candidate and focused Pixel 9 Back/Favorite/General-EQ regression pass before public publication.\n",
)

release_highlight = "- Adds **Headphones** and **General EQs** library areas, including Sound, Genre, and Utility general-preset categories where source intent supports them.\n"
insert_after(
    "docs/releases/v0.3.0.md",
    release_highlight,
    "- Populates General EQs with a qualified MIT-licensed ParaEQ seed: Bass Boost, Vocal Clarity, Treble Boost, Loudness, Podcast, Electronic, and Rock. Source preamp remains authentic/null when omitted; generated playback headroom is tracked separately.\n- Makes Android Back unwind the current screen hierarchy and return EQ Library/Settings to My EQs before exiting.\n- Adds Favorite star controls directly to managed My EQs preset rows.\n",
)
replace_once(
    "docs/releases/v0.3.0.md",
    "The exact signed beta candidate passed Android unit tests, lint, debug/release assembly, catalog/currentness validation, priority-community validation, CodeQL, APK alignment, pinned signing-certificate verification, and Pixel 9 / TRN Black Pearl hands-on testing. Hands-on coverage included upgrade/state preservation, output-specific collections, initial Add/Save export, recovery-only Export behavior, provider-adjusted filenames, ownership-safe collision handling, UAPP/ToneBoosters regression, Black Pearl connection and active-slot behavior, playback-gain replacement/non-stacking behavior, native filter types, the 10-band path, and the explicit -11.9 dB caution/Flash-anyway case.",
    "The v0.3 foundation candidate passed Android unit tests, lint, debug/release assembly, catalog/currentness validation, priority-community validation, CodeQL, APK alignment, pinned signing-certificate verification, and Pixel 9 / TRN Black Pearl hands-on testing, including the explicit -11.9 dB caution/Flash-anyway case. The final release-polish source adds only navigation/Favorite UI plus qualified General EQ catalog behavior; a fresh signed candidate must pass the focused Pixel 9 Back/Favorite/General-EQ checklist and normal automated release gates before publication.",
)

# Third-party data notice for the new qualified source.
insert_after(
    "NOTICE",
    "See DATA_LICENSE.md for data attribution and licensing details.\n",
    "\nThe runtime canonical catalog also includes qualified General EQ preset parameters derived from the MIT-licensed ParaEQ project by wabsto1. Source and license details are preserved in DATA_LICENSE.md and the catalog source registry.\n",
)
insert_after(
    "DATA_LICENSE.md",
    "CC BY-SA 4.0: https://creativecommons.org/licenses/by-sa/4.0/\n",
    "\n## ParaEQ-derived General EQ data\n\nThe initial populated General EQ set contains structured built-in preset parameters from `wabsto1/ParaEQ`, pinned to reviewed commit `51d6f6d29aef607a3a2e26b829549f9c8d7fec6e`. EQ Library preserves the source-authored labels and filter parameters, retains creator/source provenance, and does not claim those subjective tonal/genre choices are objectively correct.\n\nParaEQ source: https://github.com/wabsto1/ParaEQ\n\nMIT License notice for the copied preset definitions:\n\nCopyright (c) 2026 wabsto1\n\nPermission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the \"Software\"), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:\n\nThe above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.\n\nTHE SOFTWARE IS PROVIDED \"AS IS\", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.\n",
)

# Validate the updated catalog before the automation commits anything.
run(
    sys.executable,
    "tools/catalog_pipeline.py",
    "publish",
    "--candidate",
    "catalog/catalog.json",
    "--published",
    "/tmp/eq-library-polish-validated.json",
    "--last-known-good",
    "/tmp/eq-library-polish-lkg.json",
)

# Remove the one-shot implementation scaffolding from the final tree.
for rel in (
    "tools/apply_v03_release_polish.py",
    ".github/workflows/apply-v03-release-polish.yml",
):
    target = path(rel)
    if target.exists():
        target.unlink()
