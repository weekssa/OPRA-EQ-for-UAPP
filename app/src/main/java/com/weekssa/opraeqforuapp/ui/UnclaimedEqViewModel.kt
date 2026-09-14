package com.weekssa.opraeqforuapp.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.weekssa.opraeqforuapp.data.library.UnclaimedEqRepository
import com.weekssa.opraeqforuapp.domain.library.SavedEqRecord
import com.weekssa.opraeqforuapp.domain.library.UnclaimedEqRecord
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

/** Narrow feature ViewModel so SAF recovery stays out of Compose and Android Context stays in DI. */
class UnclaimedEqViewModel(
    private val repository: UnclaimedEqRepository,
) : ViewModel() {
    val items: StateFlow<List<UnclaimedEqRecord>> = repository.observeUnclaimed().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
        initialValue = emptyList(),
    )

    suspend fun recover(
        documentUri: String,
        manufacturer: String,
        model: String,
        displayName: String,
    ): SavedEqRecord = repository.recoverToPersonal(
        documentUri = documentUri,
        manufacturer = manufacturer,
        model = model,
        displayName = displayName,
    )

    suspend fun delete(documentUri: String): Boolean = repository.deleteUnclaimed(documentUri)

    class Factory(
        private val repositoryProvider: () -> UnclaimedEqRepository,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            require(modelClass.isAssignableFrom(UnclaimedEqViewModel::class.java))
            return UnclaimedEqViewModel(repositoryProvider()) as T
        }
    }

    companion object {
        private const val STOP_TIMEOUT_MILLIS = 5_000L
    }
}
