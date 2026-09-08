package com.weekssa.opraeqforuapp.ui

import androidx.compose.runtime.saveable.Saver

/** Bundle-safe saver for immutable String sets used by transient Compose editing state. */
internal val StringSetSaver: Saver<Set<String>, ArrayList<String>> = Saver(
    save = { values -> ArrayList(values) },
    restore = { values -> values.toSet() },
)
