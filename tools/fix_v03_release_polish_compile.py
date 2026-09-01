#!/usr/bin/env python3
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]


def replace_once(path: str, old: str, new: str) -> None:
    target = ROOT / path
    text = target.read_text(encoding="utf-8")
    count = text.count(old)
    if count != 1:
        raise SystemExit(f"{path}: expected exactly one replacement target, found {count}: {old!r}")
    target.write_text(text.replace(old, new, 1), encoding="utf-8")


replace_once(
    "app/src/main/java/com/weekssa/opraeqforuapp/ui/screens/ManagedHeadphoneDetailScreen.kt",
    """    onToggleFavorite: suspend (OpraEqProfile, String, String) -> Boolean,\n    onLoadManagedHeadphone: suspend (String) -> ManagedHeadphoneRecord?,\n""",
    """    onToggleFavorite: suspend (OpraEqProfile, String, String) -> Boolean,\n    onHideCanonicalProfile: suspend (String) -> Unit,\n    onLoadManagedHeadphone: suspend (String) -> ManagedHeadphoneRecord?,\n""",
)
replace_once(
    "app/src/main/java/com/weekssa/opraeqforuapp/ui/screens/ManagedHeadphoneDetailScreen.kt",
    """            onToggleFavorite = onToggleFavorite,\n            onLoadManagedHeadphone = onLoadManagedHeadphone,\n""",
    """            onToggleFavorite = onToggleFavorite,\n            onHideCanonicalProfile = onHideCanonicalProfile,\n            onLoadManagedHeadphone = onLoadManagedHeadphone,\n""",
)
replace_once(
    "app/src/main/java/com/weekssa/opraeqforuapp/ui/EqLibraryApp.kt",
    """                                onToggleFavorite = onToggleFavorite,\n                                onLoadManagedHeadphone = onLoadManagedHeadphone,\n""",
    """                                onToggleFavorite = onToggleFavorite,\n                                onHideCanonicalProfile = { canonicalProfileId ->\n                                    onHideCanonicalProfiles(setOf(canonicalProfileId))\n                                },\n                                onLoadManagedHeadphone = onLoadManagedHeadphone,\n""",
)
replace_once(
    "app/src/main/java/com/weekssa/opraeqforuapp/ui/screens/PersonalEqImportScreen.kt",
    """    val text = context.contentResolver.openInputStream(uri)?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }\n        ?: error(\"Couldn't open that file.\")\n""",
    """    val text = context.contentResolver.openInputStream(uri)?.bufferedReader(Charsets.UTF_8)?.use {\n        it.readText().removePrefix(\"\\uFEFF\")\n    } ?: error(\"Couldn't open that file.\")\n""",
)

print("Applied release-polish compile correction and UTF-8 BOM-safe import read.")
