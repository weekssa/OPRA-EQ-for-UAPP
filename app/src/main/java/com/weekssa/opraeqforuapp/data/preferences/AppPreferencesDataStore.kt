package com.weekssa.opraeqforuapp.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore

private val Context.eqLibraryDataStore by preferencesDataStore(name = "app_preferences")

/**
 * Android composition-root accessor for the process-wide preferences DataStore.
 *
 * Repositories receive the DataStore itself so Android Context never crosses into a repository.
 */
internal val Context.eqLibraryPreferencesDataStore: DataStore<Preferences>
    get() = applicationContext.eqLibraryDataStore
