#!/usr/bin/env python3
"""Stable entry point for curated community publication across qualified forum surfaces.

The underlying publisher predates the expanded source registry and keeps a conservative
surface-to-source map in module state. This entry point adds newly qualified community
surfaces without duplicating publication logic. New surface aliases should be explicit and
must map to a source ID already qualified in config/source_registry.json.
"""
from __future__ import annotations

import curated_community_publish as publisher

QUALIFIED_SURFACE_SOURCE_IDS = {
    "hifiguides": "hifiguides",
}


def configure_surface_sources() -> None:
    for surface, source_id in QUALIFIED_SURFACE_SOURCE_IDS.items():
        publisher.SOURCE_ID[surface] = source_id


def main() -> int:
    configure_surface_sources()
    return publisher.main()


if __name__ == "__main__":
    raise SystemExit(main())
