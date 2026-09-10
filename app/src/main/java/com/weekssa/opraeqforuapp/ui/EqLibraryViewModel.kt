package com.weekssa.opraeqforuapp.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.weekssa.opraeqforuapp.R
import com.weekssa.opraeqforuapp.data.blackpearl.BlackPearlConnectionState
import com.weekssa.opraeqforuapp.data.catalog.AppCatalogRepository
import com.weekssa.opraeqforuapp.data.catalog.CatalogState
import com.weekssa.opraeqforuapp.data.export.ExportCurrentness
import com.weekssa.opraeqforuapp.data.export.PresetCleanupRepository
import com.weekssa.opraeqforuapp.data.export.PresetCleanupSummary
import com.weekssa.opraeqforuapp.data.export.PresetExportRepository
import com.weekssa.opraeqforuapp.data.export.PresetExportSummary
import com.weekssa.opraeqforuapp.data.hardware.HardwareEqRepository
import com.weekssa.opraeqforuapp.data.kt02h20.Kt02h20ConnectionState
import com.weekssa.opraeqforuapp.data.library.SavedEqRepository
import com.weekssa.opraeqforuapp.data.library.SavedGeneralEqRepository
import com.weekssa.opraeqforuapp.data.managed.ManagedHeadphonesRepository
import com.weekssa.opraeqforuapp.data.preferences.AppPreferencesRepository
import com.weekssa.opraeqforuapp.data.sync.CatalogSyncCoordinator
import com.weekssa.opraeqforuapp.data.sync.CatalogSyncOutcome
import com.weekssa.opraeqforuapp.data.update.AppUpdateCheckResult
import com.weekssa.opraeqforuapp.data.update.AppUpdateCoordinator
import com.weekssa.opraeqforuapp.domain.blackpearl.BlackPearlCaptureDecision
import com.weekssa.opraeqforuapp.domain.blackpearl.BlackPearlEditorApplyResult
import com.weekssa.opraeqforuapp.domain.blackpearl.BlackPearlFlashResult
import com.weekssa.opraeqforuapp.domain.blackpearl.BlackPearlFlatResetResult
import com.weekssa.opraeqforuapp.domain.blackpearl.BlackPearlHardwareEqMatchResolver
import com.weekssa.opraeqforuapp.domain.blackpearl.buildBlackPearlMyEqsCandidates
import com.weekssa.opraeqforuapp.domain.blackpearl.decideBlackPearlCapture
import com.weekssa.opraeqforuapp.domain.catalog.GeneralEqPreset
import com.weekssa.opraeqforuapp.domain.catalog.OpraEqProfile
import com.weekssa.opraeqforuapp.domain.dac.DacDeviceId
import com.weekssa.opraeqforuapp.domain.dac.DacRecognitionState
import com.weekssa.opraeqforuapp.domain.dac.HardwareEqEditSpecs
import com.weekssa.opraeqforuapp.domain.dac.HardwareEqEditor
import com.weekssa.opraeqforuapp.domain.dac.HardwareEqEditorStartResult
import com.weekssa.opraeqforuapp.domain.dac.HardwareEqMatchResolution
import com.weekssa.opraeqforuapp.domain.dac.HardwareEqSnapshotState
import com.weekssa.opraeqforuapp.domain.export.DevicePresetFidelity
import com.weekssa.opraeqforuapp.domain.export.ExportDevice
import com.weekssa.opraeqforuapp.domain.kt02h20.Kt02h20FlashResult
import com.weekssa.opraeqforuapp.domain.kt02h20.Kt02h20FlatResetResult
import com.weekssa.opraeqforuapp.domain.library.EqFilterType
import com.weekssa.opraeqforuapp.domain.library.SavedEqHeadphoneAssociation
import com.weekssa.opraeqforuapp.domain.library.SavedEqRecord
import com.weekssa.opraeqforuapp.domain.library.SavedGeneralEqRecord
import com.weekssa.opraeqforuapp.domain.managed.ManagedHeadphoneRecord
import com.weekssa.opraeqforuapp.domain.settings.AppPreferences
import com.weekssa.opraeqforuapp.domain.settings.ThemeMode
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private data class LibraryDataState(
    val outputId: String = ExportDevice.UAPP.name,
    val managedHeadphones: List<ManagedHeadphoneRecord> = emptyList(),
    val savedEqs: List<SavedEqRecord> = emptyList(),
    val savedGeneralEqs: List<SavedGeneralEqRecord> = emptyList(),
)

private data class LibraryUiState(
    val data: LibraryDataState,
    val exportCurrentness: ExportCurrentness,
)

private data class ExportCurrentnessInput(
    val preferences: AppPreferences,
    val library: LibraryDataState,
)

private data class HardwareConnectionUiState(
    val blackPearl: BlackPearlConnectionState,
    val fiioJa11: Kt02h20ConnectionState,
    val jcallyJm12: Kt02h20ConnectionState,
    val blackPearlHardwareEqState: HardwareEqSnapshotState,
    val blackPearlHardwareEqMatch: HardwareEqMatchResolution?,
    val blackPearlEditorState: MyDacEditorUiState,
)

data class EqLibraryUiState(
    val appPreferences: AppPreferences = AppPreferences(),
    val catalogState: CatalogState = CatalogState.Loading,
    val managedHeadphones: List<ManagedHeadphoneRecord> = emptyList(),
    val savedEqs: List<SavedEqRecord> = emptyList(),
    val savedGeneralEqs: List<SavedGeneralEqRecord> = emptyList(),
    val exportCurrentness: ExportCurrentness = ExportCurrentness(),
    val dacRecognitionState: DacRecognitionState = DacRecognitionState(),
    val blackPearlConnectionState: BlackPearlConnectionState = BlackPearlConnectionState.Disconnected,
    val fiioJa11ConnectionState: Kt02h20ConnectionState = Kt02h20ConnectionState.Disconnected,
    val jcallyJm12ConnectionState: Kt02h20ConnectionState = Kt02h20ConnectionState.Disconnected,
    val blackPearlHardwareEqState: HardwareEqSnapshotState = HardwareEqSnapshotState(),
    val blackPearlHardwareEqMatch: HardwareEqMatchResolution? = null,
    val blackPearlManagedHeadphones: List<ManagedHeadphoneRecord> = emptyList(),
    val blackPearlSavedEqs: List<SavedEqRecord> = emptyList(),
    val blackPearlEditorState: MyDacEditorUiState = MyDacEditorUiState(),
)

class EqLibraryViewModel(
    private val preferencesRepository: AppPreferencesRepository,
    private val catalogRepository: AppCatalogRepository,
    private val managedHeadphonesRepository: ManagedHeadphonesRepository,
    private val savedEqRepository: SavedEqRepository,
    private val savedGeneralEqRepository: SavedGeneralEqRepository,
    private val exportRepository: PresetExportRepository,
    private val cleanupRepository: PresetCleanupRepository,
    private val syncCoordinator: CatalogSyncCoordinator,
    private val updateCoordinator: AppUpdateCoordinator,
    private val hardwareRepository: HardwareEqRepository,
    private val computationDispatcher: CoroutineDispatcher = Dispatchers.Default,
    private val nowMillis: () -> Long = System::currentTimeMillis,
) : ViewModel() {
    private var lastForegroundRefreshAttemptMillis: Long = 0L
    private val exportInvalidation = MutableStateFlow(0L)
    private val mutableBlackPearlEditorState = MutableStateFlow(MyDacEditorUiState())

    private val activeOutputId = preferencesRepository.preferences
        .map { preferences -> preferences.exportTargets.activeTarget.name }
        .distinctUntilChanged()

    private val libraryData: StateFlow<LibraryDataState> = activeOutputId.flatMapLatest { outputId ->
        combine(
            managedHeadphonesRepository.observeHeadphones(outputId),
            savedEqRepository.observeForOutput(outputId),
            savedGeneralEqRepository.observeForOutput(outputId),
        ) { managedHeadphones, savedEqs, savedGeneralEqs ->
            LibraryDataState(
                outputId = outputId,
                managedHeadphones = managedHeadphones,
                savedEqs = savedEqs,
                savedGeneralEqs = savedGeneralEqs,
            )
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
        initialValue = LibraryDataState(),
    )

    /**
     * My DAC matching intentionally reads the connected device's own output-specific My EQs state.
     * It must not follow or silently switch the user's separate global active-output context.
     */
    private val blackPearlLibraryData: StateFlow<LibraryDataState> = combine(
        managedHeadphonesRepository.observeHeadphones(ExportDevice.BLACK_PEARL.name),
        savedEqRepository.observeForOutput(ExportDevice.BLACK_PEARL.name),
        savedGeneralEqRepository.observeForOutput(ExportDevice.BLACK_PEARL.name),
    ) { managedHeadphones, savedEqs, savedGeneralEqs ->
        LibraryDataState(
            outputId = ExportDevice.BLACK_PEARL.name,
            managedHeadphones = managedHeadphones,
            savedEqs = savedEqs,
            savedGeneralEqs = savedGeneralEqs,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
        initialValue = LibraryDataState(outputId = ExportDevice.BLACK_PEARL.name),
    )

    private val blackPearlHardwareEqMatch = combine(
        hardwareRepository.blackPearlSnapshotState,
        blackPearlLibraryData,
    ) { snapshotState, library -> snapshotState to library }
        .mapLatest { (snapshotState, library) ->
            val actual = snapshotState.bundle?.fingerprint ?: return@mapLatest null
            withContext(computationDispatcher) {
                BlackPearlHardwareEqMatchResolver.resolve(
                    actual = actual,
                    candidates = buildBlackPearlMyEqsCandidates(
                        managedHeadphones = library.managedHeadphones,
                        savedEqs = library.savedEqs,
                        savedGeneralEqs = library.savedGeneralEqs,
                    ),
                )
            }
        }

    private val exportCurrentness = combine(
        preferencesRepository.preferences,
        libraryData,
        exportInvalidation,
    ) { preferences, library, _ ->
        ExportCurrentnessInput(preferences, library)
    }.mapLatest { input ->
        val device = input.preferences.exportTargets.activeTarget
        if (!device.supportsFileExport || input.library.outputId != device.name) {
            ExportCurrentness()
        } else {
            val records = withContext(computationDispatcher) {
                input.library.toExportRecords()
            }
            exportRepository.evaluateCurrentness(
                treeUri = input.preferences.exportTreeUri,
                headphones = records,
                device = device,
            )
        }
    }

    private val libraryUi = combine(libraryData, exportCurrentness) { library, currentness ->
        LibraryUiState(library, currentness)
    }

    private val hardwareConnectionsWithoutEditor = combine(
        hardwareRepository.blackPearlConnectionState,
        hardwareRepository.fiioJa11ConnectionState,
        hardwareRepository.jcallyJm12ConnectionState,
        hardwareRepository.blackPearlSnapshotState,
        blackPearlHardwareEqMatch,
    ) { blackPearl, fiioJa11, jcallyJm12, blackPearlHardwareEqState, blackPearlMatch ->
        HardwareConnectionUiState(
            blackPearl = blackPearl,
            fiioJa11 = fiioJa11,
            jcallyJm12 = jcallyJm12,
            blackPearlHardwareEqState = blackPearlHardwareEqState,
            blackPearlHardwareEqMatch = blackPearlMatch,
            blackPearlEditorState = MyDacEditorUiState(),
        )
    }

    private val hardwareConnections = combine(
        hardwareConnectionsWithoutEditor,
        mutableBlackPearlEditorState,
    ) { hardware, editor -> hardware.copy(blackPearlEditorState = editor) }

    val uiState: StateFlow<EqLibraryUiState> = combine(
        preferencesRepository.preferences,
        catalogRepository.state,
        libraryUi,
        hardwareConnections,
        hardwareRepository.recognitionState,
    ) { preferences, catalogState, library, hardware, recognition ->
        val activeOutputId = preferences.exportTargets.activeTarget.name
        val matchingLibrary = library.data.takeIf { it.outputId == activeOutputId }
        val blackPearlLibrary = blackPearlLibraryData.value
        EqLibraryUiState(
            appPreferences = preferences,
            catalogState = catalogState,
            managedHeadphones = matchingLibrary?.managedHeadphones.orEmpty(),
            savedEqs = matchingLibrary?.savedEqs.orEmpty(),
            savedGeneralEqs = matchingLibrary?.savedGeneralEqs.orEmpty(),
            exportCurrentness = if (matchingLibrary == null) {
                ExportCurrentness()
            } else {
                library.exportCurrentness
            },
            dacRecognitionState = recognition,
            blackPearlConnectionState = hardware.blackPearl,
            fiioJa11ConnectionState = hardware.fiioJa11,
            jcallyJm12ConnectionState = hardware.jcallyJm12,
            blackPearlHardwareEqState = hardware.blackPearlHardwareEqState,
            blackPearlHardwareEqMatch = hardware.blackPearlHardwareEqMatch,
            blackPearlManagedHeadphones = blackPearlLibrary.managedHeadphones,
            blackPearlSavedEqs = blackPearlLibrary.savedEqs,
            blackPearlEditorState = hardware.blackPearlEditorState,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
        initialValue = EqLibraryUiState(),
    )

    init {
        viewModelScope.launch { updateCoordinator.initialize() }
        viewModelScope.launch {
            catalogRepository.initialize()
            (catalogRepository.state.value as? CatalogState.Ready)?.let { ready ->
                managedHeadphonesRepository.reconcileCatalog(ready.catalog)
            }
            refreshCatalogIfDue()
        }
    }

    fun onAppResumed() {
        viewModelScope.launch { refreshCatalogIfDue() }
    }

    /**
     * My DAC session access is intentionally independent from the global output selector and Direct
     * Flash toggles. It opens/requests Android USB access only for an exact supported DAC that is
     * physically present now; no EQ or device-control write is issued here.
     */
    fun connectDacForMyDac(deviceId: DacDeviceId) {
        if (deviceId !in hardwareRepository.recognitionState.value.presentDeviceIds) return
        when (deviceId) {
            DacDeviceId.TRN_BLACK_PEARL -> hardwareRepository.connectBlackPearl()
            DacDeviceId.FIIO_JA11 -> hardwareRepository.connectFiioJa11()
            DacDeviceId.JCALLY_JM12_STOCK -> hardwareRepository.connectJcallyJm12()
        }
    }

    /**
     * Opens the Black Pearl editor only from a fresh verified read. This refresh is read-only; editor
     * entry cannot invoke Flash, Reset, or any generic device-control write.
     */
    fun openBlackPearlEditor() {
        val current = mutableBlackPearlEditorState.value
        if (current.isOpening || current.applyStatus == MyDacEditorApplyStatus.APPLYING) return
        if (hardwareRepository.blackPearlConnectionState.value !is BlackPearlConnectionState.Connected) {
            mutableBlackPearlEditorState.value = MyDacEditorUiState(error = MyDacEditorError.NOT_CONNECTED)
            return
        }

        mutableBlackPearlEditorState.value = MyDacEditorUiState(isOpening = true)
        viewModelScope.launch {
            val refreshed = hardwareRepository.readBlackPearlSnapshot()
            if (refreshed == null) {
                mutableBlackPearlEditorState.value = MyDacEditorUiState(error = MyDacEditorError.READ_FAILED)
                return@launch
            }
            val trackedGainDeltaDb = hardwareRepository.readBlackPearlTrackedGainDeltaDb()
            val result = withContext(computationDispatcher) {
                HardwareEqEditor.startFromCurrent(
                    snapshotState = hardwareRepository.blackPearlSnapshotState.value,
                    spec = HardwareEqEditSpecs.TRN_BLACK_PEARL,
                    trackedPlaybackGainDeltaDb = trackedGainDeltaDb,
                )
            }
            mutableBlackPearlEditorState.value = when (result) {
                is HardwareEqEditorStartResult.Ready -> {
                    val selectedBandIndex = result.workingCopy.filters.firstOrNull()?.index
                    MyDacEditorUiState(
                        stage = MyDacEditorStage.EDIT,
                        workingCopy = result.workingCopy,
                        selectedBandIndex = selectedBandIndex,
                    )
                }
                HardwareEqEditorStartResult.CurrentSnapshotRequired ->
                    MyDacEditorUiState(error = MyDacEditorError.READ_FAILED)
                HardwareEqEditorStartResult.WrongDevice ->
                    MyDacEditorUiState(error = MyDacEditorError.WRONG_DEVICE)
            }
        }
    }

    fun closeMyDacEditor() {
        if (mutableBlackPearlEditorState.value.applyStatus == MyDacEditorApplyStatus.APPLYING) return
        mutableBlackPearlEditorState.value = MyDacEditorUiState()
    }

    /** Returns true when Back was consumed inside the editor workflow. */
    fun backMyDacEditor(): Boolean {
        val current = mutableBlackPearlEditorState.value
        if (current.applyStatus == MyDacEditorApplyStatus.APPLYING) return true
        val next = when {
            current.isOpening -> MyDacEditorUiState()
            current.stage == MyDacEditorStage.REVIEW || current.stage == MyDacEditorStage.ALL_BANDS ->
                current.copy(
                    stage = MyDacEditorStage.EDIT,
                    error = null,
                    applyStatus = MyDacEditorApplyStatus.IDLE,
                    applyFailureReason = null,
                )
            current.stage == MyDacEditorStage.EDIT -> MyDacEditorUiState()
            else -> return false
        }
        mutableBlackPearlEditorState.value = next
        return true
    }

    fun selectBlackPearlEditorBand(bandIndex: Int) {
        mutableBlackPearlEditorState.update { current ->
            val working = current.workingCopy ?: return@update current
            if (working.filters.none { filter -> filter.index == bandIndex }) return@update current
            current.copy(
                stage = MyDacEditorStage.EDIT,
                selectedBandIndex = bandIndex,
                error = null,
                applyStatus = MyDacEditorApplyStatus.IDLE,
                applyFailureReason = null,
            )
        }
    }

    fun showBlackPearlEditorAllBands() {
        mutableBlackPearlEditorState.update { current ->
            if (current.workingCopy == null || current.applyStatus == MyDacEditorApplyStatus.APPLYING) current
            else current.copy(
                stage = MyDacEditorStage.ALL_BANDS,
                error = null,
                applyStatus = MyDacEditorApplyStatus.IDLE,
                applyFailureReason = null,
            )
        }
    }

    fun showBlackPearlEditorReview() {
        mutableBlackPearlEditorState.update { current ->
            if (current.workingCopy == null || current.applyStatus == MyDacEditorApplyStatus.APPLYING) current
            else current.copy(
                stage = MyDacEditorStage.REVIEW,
                error = null,
                applyStatus = MyDacEditorApplyStatus.IDLE,
                applyFailureReason = null,
            )
        }
    }

    fun updateBlackPearlEditorBand(
        bandIndex: Int,
        type: EqFilterType,
        frequencyHz: Double,
        gainDb: Double,
        q: Double,
    ) {
        mutableBlackPearlEditorState.update { current ->
            if (current.applyStatus == MyDacEditorApplyStatus.APPLYING) return@update current
            val working = current.workingCopy ?: return@update current
            val updated = HardwareEqEditor.updateFilter(
                workingCopy = working,
                spec = HardwareEqEditSpecs.TRN_BLACK_PEARL,
                bandIndex = bandIndex,
                type = type,
                frequencyHz = frequencyHz,
                gainDb = gainDb,
                q = q,
            )
            current.copy(
                workingCopy = updated,
                selectedBandIndex = bandIndex,
                error = null,
                applyStatus = MyDacEditorApplyStatus.IDLE,
                applyFailureReason = null,
            )
        }
    }

    fun useSafeBlackPearlEditorGain() {
        mutableBlackPearlEditorState.update { current ->
            if (current.applyStatus == MyDacEditorApplyStatus.APPLYING) return@update current
            val working = current.workingCopy ?: return@update current
            current.copy(
                workingCopy = HardwareEqEditor.useSafeGain(
                    workingCopy = working,
                    spec = HardwareEqEditSpecs.TRN_BLACK_PEARL,
                ),
                error = null,
                applyStatus = MyDacEditorApplyStatus.IDLE,
                applyFailureReason = null,
            )
        }
    }

    fun resetBlackPearlEditorLocalEdits() {
        mutableBlackPearlEditorState.update { current ->
            if (current.applyStatus == MyDacEditorApplyStatus.APPLYING) return@update current
            val working = current.workingCopy ?: return@update current
            current.copy(
                stage = MyDacEditorStage.EDIT,
                workingCopy = HardwareEqEditor.resetLocalEdits(
                    workingCopy = working,
                    spec = HardwareEqEditSpecs.TRN_BLACK_PEARL,
                ),
                error = null,
                applyStatus = MyDacEditorApplyStatus.IDLE,
                applyFailureReason = null,
            )
        }
    }

    fun applyBlackPearlEditor(allowCautions: Boolean) {
        val current = mutableBlackPearlEditorState.value
        val workingCopy = current.workingCopy ?: return
        if (current.stage != MyDacEditorStage.REVIEW || current.applyStatus == MyDacEditorApplyStatus.APPLYING) return
        if (current.applyStatus == MyDacEditorApplyStatus.CONFIRMATION_REQUIRED && !allowCautions) return

        mutableBlackPearlEditorState.value = current.copy(
            applyStatus = MyDacEditorApplyStatus.APPLYING,
            applyFailureReason = null,
        )
        viewModelScope.launch {
            val result = hardwareRepository.applyBlackPearlEditor(
                workingCopy = workingCopy,
                allowCautions = allowCautions,
            )
            mutableBlackPearlEditorState.value = when (result) {
                is BlackPearlEditorApplyResult.Verified -> MyDacEditorUiState(
                    applyStatus = MyDacEditorApplyStatus.VERIFIED,
                )
                is BlackPearlEditorApplyResult.ConfirmationRequired -> current.copy(
                    applyStatus = MyDacEditorApplyStatus.CONFIRMATION_REQUIRED,
                    applyFailureReason = null,
                )
                is BlackPearlEditorApplyResult.InvalidPlan -> failedEditorApply(result.reason)
                is BlackPearlEditorApplyResult.StaleBaseline -> failedEditorApply(result.reason)
                is BlackPearlEditorApplyResult.DeviceUnavailable -> failedEditorApply(result.reason)
                is BlackPearlEditorApplyResult.TransferFailed -> failedEditorApply(result.reason)
                is BlackPearlEditorApplyResult.VerificationFailed -> failedEditorApply(result.reason)
            }
        }
    }

    private fun failedEditorApply(reason: String): MyDacEditorUiState = MyDacEditorUiState(
        applyStatus = MyDacEditorApplyStatus.FAILED,
        applyFailureReason = reason,
    )

    /**
     * Captures only a freshly read Black Pearl hardware state. Exact native matches are linked to
     * existing My EQs identities rather than duplicated, and Flat never creates a Personal EQ.
     */
    suspend fun captureBlackPearlDacEq(
        displayName: String,
        association: SavedEqHeadphoneAssociation?,
    ): UiText {
        val name = displayName.trim()
        if (name.isEmpty()) return UiText.Dynamic("EQ name is required.")
        if (hardwareRepository.blackPearlConnectionState.value !is BlackPearlConnectionState.Connected) {
            return UiText.Dynamic("Connect TRN Black Pearl before saving its EQ.")
        }

        val bundle = hardwareRepository.readBlackPearlSnapshot()
            ?: return UiText.Dynamic("Could not read the current Black Pearl EQ. No Personal EQ was saved.")
        if (!hardwareRepository.isBlackPearlSessionCurrent(bundle.snapshot.sessionGeneration)) {
            return UiText.Dynamic("The Black Pearl connection changed while reading. Reconnect and try again.")
        }

        val library = loadLibraryData(ExportDevice.BLACK_PEARL.name)
        val resolution = withContext(computationDispatcher) {
            BlackPearlHardwareEqMatchResolver.resolve(
                actual = bundle.fingerprint,
                candidates = buildBlackPearlMyEqsCandidates(
                    managedHeadphones = library.managedHeadphones,
                    savedEqs = library.savedEqs,
                    savedGeneralEqs = library.savedGeneralEqs,
                ),
            )
        }

        return when (val decision = decideBlackPearlCapture(resolution.match)) {
            BlackPearlCaptureDecision.Flat ->
                UiText.Dynamic("The Black Pearl EQ is already flat. No duplicate Personal EQ was created.")
            is BlackPearlCaptureDecision.ExistingMatch -> {
                if (decision.savedEqs.size == 1) {
                    UiText.Dynamic("Already in My EQs: ${decision.savedEqs.single().displayName}")
                } else {
                    UiText.Dynamic(
                        "This Black Pearl EQ already exactly matches ${decision.savedEqs.size} saved EQs. No duplicate was created.",
                    )
                }
            }
            BlackPearlCaptureDecision.Capture -> runCatching {
                savedEqRepository.captureBlackPearlEq(
                    displayName = name,
                    snapshotBundle = bundle,
                    association = association,
                )
            }.fold(
                onSuccess = { record -> UiText.Dynamic("Saved ${record.displayName} to Black Pearl My EQs.") },
                onFailure = { error ->
                    UiText.Dynamic(error.message ?: "Could not save the current Black Pearl EQ.")
                },
            )
        }
    }

    fun connectBlackPearl() {
        viewModelScope.launch {
            val preferences = preferencesRepository.snapshot()
            if (
                preferences.directBlackPearlFlashEnabled &&
                preferences.exportTargets.activeTarget == ExportDevice.BLACK_PEARL
            ) {
                hardwareRepository.connectBlackPearl()
            }
        }
    }

    fun connectFiioJa11() {
        viewModelScope.launch {
            val preferences = preferencesRepository.snapshot()
            if (
                preferences.directFiioJa11FlashEnabled &&
                preferences.exportTargets.activeTarget == ExportDevice.FIIO_JA11
            ) {
                hardwareRepository.connectFiioJa11()
            }
        }
    }

    fun connectJcallyJm12() {
        viewModelScope.launch {
            val preferences = preferencesRepository.snapshot()
            if (
                preferences.directJcallyJm12FlashEnabled &&
                preferences.exportTargets.activeTarget == ExportDevice.JCALLY_JM12
            ) {
                hardwareRepository.connectJcallyJm12()
            }
        }
    }

    suspend fun resetBlackPearlToFlat(): UiText {
        val preferences = preferencesRepository.snapshot()
        if (preferences.exportTargets.activeTarget != ExportDevice.BLACK_PEARL) {
            return resource(R.string.error_select_black_pearl_reset)
        }
        if (!preferences.directBlackPearlFlashEnabled) {
            return resource(R.string.error_enable_black_pearl_reset)
        }
        if (hardwareRepository.blackPearlConnectionState.value !is BlackPearlConnectionState.Connected) {
            return resource(R.string.error_connect_black_pearl_reset)
        }

        return when (val result = hardwareRepository.resetBlackPearl()) {
            is BlackPearlFlatResetResult.Success -> {
                if (kotlin.math.abs(result.restoredPlaybackGainDb) < ZERO_GAIN_EPSILON) {
                    resource(R.string.black_pearl_reset_success)
                } else {
                    resource(
                        R.string.black_pearl_reset_removed_adjustment,
                        -result.restoredPlaybackGainDb,
                    )
                }
            }
            is BlackPearlFlatResetResult.NotRepresentable ->
                resource(R.string.black_pearl_reset_failed, result.reason)
            is BlackPearlFlatResetResult.DeviceUnavailable -> UiText.Dynamic(result.reason)
            is BlackPearlFlatResetResult.TransferFailed -> UiText.Dynamic(result.reason)
        }
    }

    suspend fun resetFiioJa11ToFlat(): UiText {
        val preferences = preferencesRepository.snapshot()
        if (preferences.exportTargets.activeTarget != ExportDevice.FIIO_JA11) {
            return resource(R.string.error_select_fiio_reset)
        }
        if (!preferences.directFiioJa11FlashEnabled) {
            return resource(R.string.error_enable_fiio_reset)
        }
        if (hardwareRepository.fiioJa11ConnectionState.value !is Kt02h20ConnectionState.Connected) {
            return resource(R.string.error_connect_fiio_reset)
        }
        return when (val result = hardwareRepository.resetFiioJa11()) {
            is Kt02h20FlatResetResult.Success -> resource(R.string.fiio_reset_success)
            is Kt02h20FlatResetResult.NotSuitable ->
                resource(R.string.fiio_reset_failed, result.reason)
            is Kt02h20FlatResetResult.DeviceUnavailable -> UiText.Dynamic(result.reason)
            is Kt02h20FlatResetResult.TransferFailed -> UiText.Dynamic(result.reason)
            is Kt02h20FlatResetResult.VerificationFailed ->
                resource(R.string.fiio_reset_verification_failed, result.reason)
        }
    }

    suspend fun resetJcallyJm12ToFlat(): UiText {
        val preferences = preferencesRepository.snapshot()
        if (preferences.exportTargets.activeTarget != ExportDevice.JCALLY_JM12) {
            return resource(R.string.error_select_jm12_reset)
        }
        if (!preferences.directJcallyJm12FlashEnabled) {
            return resource(R.string.error_enable_jm12_reset)
        }
        if (hardwareRepository.jcallyJm12ConnectionState.value !is Kt02h20ConnectionState.Connected) {
            return resource(R.string.error_connect_jm12_reset)
        }
        return when (val result = hardwareRepository.resetJcallyJm12()) {
            is Kt02h20FlatResetResult.Success ->
                resource(R.string.jm12_reset_success, result.restoredPlaybackGainDb)
            is Kt02h20FlatResetResult.NotSuitable ->
                resource(R.string.jm12_reset_failed, result.reason)
            is Kt02h20FlatResetResult.DeviceUnavailable -> UiText.Dynamic(result.reason)
            is Kt02h20FlatResetResult.TransferFailed -> UiText.Dynamic(result.reason)
            is Kt02h20FlatResetResult.VerificationFailed ->
                resource(R.string.jm12_reset_verification_failed, result.reason)
        }
    }

    suspend fun flashManagedProfile(productId: String, profileId: String): UiText {
        val outputId = activeOutputId()
        val managed = managedHeadphonesRepository.getHeadphone(productId, outputId)
            ?: return resource(R.string.error_headphone_not_saved)
        val profile = managed.profiles.firstOrNull { it.profileId == profileId && it.selected }
            ?: return resource(R.string.error_eq_not_selected)
        return flashHardwareProfile(profile.lastKnownProfile)
    }

    suspend fun flashSavedEq(entryId: String): UiText {
        val outputId = activeOutputId()
        val record = savedEqRepository.getForOutput(outputId, entryId)
            ?: return resource(R.string.error_eq_not_saved)
        return flashHardwareProfile(record.profile)
    }

    suspend fun flashGeneralEq(presetId: String): UiText {
        val outputId = activeOutputId()
        val record = savedGeneralEqRepository.getForOutput(outputId, presetId)
            ?: return resource(R.string.error_general_eq_not_saved)
        return flashHardwareProfile(record.profile)
    }

    suspend fun refreshCatalog(): CatalogSyncOutcome = syncCoordinator.refresh()

    suspend fun loadManagedHeadphone(productId: String): ManagedHeadphoneRecord? =
        managedHeadphonesRepository.getHeadphone(productId, activeOutputId())

    suspend fun saveSelection(
        productId: String,
        selectedIds: Set<String>,
        autoInclude: Boolean,
    ) {
        val outputId = activeOutputId()
        val ready = catalogRepository.state.value as? CatalogState.Ready ?: return
        if (selectedIds.isEmpty()) {
            managedHeadphonesRepository.removeHeadphone(productId, outputId)
        } else {
            managedHeadphonesRepository.saveSelection(
                catalog = ready.catalog,
                productId = productId,
                stagedSelectedProfileIds = selectedIds,
                autoIncludeNewProfiles = autoInclude,
                outputId = outputId,
            )
        }
    }

    suspend fun removeHeadphone(productId: String) {
        managedHeadphonesRepository.removeHeadphone(productId, activeOutputId())
    }

    suspend fun removeManagedProfile(
        productId: String,
        profileId: String,
        deleteSavedFiles: Boolean,
    ): PresetCleanupSummary? {
        val outputId = activeOutputId()
        val managed = managedHeadphonesRepository.getHeadphone(productId, outputId) ?: return null
        val record = managed.profiles.firstOrNull { it.profileId == profileId } ?: return null
        val cleanup = if (deleteSavedFiles) {
            cleanupRepository.deleteForProfiles(setOf(profileId)).also { invalidateExportCurrentness() }
        } else {
            null
        }
        val ready = catalogRepository.state.value as? CatalogState.Ready
        val currentProfiles = ready?.catalog?.profilesForProduct(productId).orEmpty()
        val currentProfile = currentProfiles.firstOrNull { it.id == profileId }

        if (record.noLongerAvailable || currentProfile == null || ready == null) {
            managedHeadphonesRepository.removeUnavailableProfile(productId, profileId, outputId)
            val remaining = managedHeadphonesRepository.getHeadphone(productId, outputId)
            if (remaining?.profiles?.none { it.selected || it.noLongerAvailable } != false) {
                managedHeadphonesRepository.removeHeadphone(productId, outputId)
            }
        } else {
            val selectionState = managed.toSelectionState()
            val remainingCurrentSelected = currentProfiles
                .filter(selectionState::isSelected)
                .mapTo(mutableSetOf()) { it.id }
                .also { it.remove(profileId) }
            val retainedSelectedRemain = managed.profiles.any {
                it.profileId != profileId && it.selected && it.noLongerAvailable
            }
            if (remainingCurrentSelected.isEmpty() && !retainedSelectedRemain) {
                managedHeadphonesRepository.removeHeadphone(productId, outputId)
            } else {
                managedHeadphonesRepository.saveSelection(
                    catalog = ready.catalog,
                    productId = productId,
                    stagedSelectedProfileIds = remainingCurrentSelected,
                    autoIncludeNewProfiles = managed.autoIncludeNewProfiles,
                    outputId = outputId,
                )
            }
        }
        return cleanup
    }

    suspend fun removeManagedHeadphone(
        productId: String,
        deleteSavedFiles: Boolean,
    ): PresetCleanupSummary? {
        val outputId = activeOutputId()
        val cleanup = if (deleteSavedFiles) {
            cleanupRepository.deleteForProduct(productId).also { invalidateExportCurrentness() }
        } else {
            null
        }
        managedHeadphonesRepository.removeHeadphone(productId, outputId)
        return cleanup
    }

    suspend fun deleteSavedFilesForProfiles(profileIds: Set<String>): PresetCleanupSummary =
        cleanupRepository.deleteForProfiles(profileIds).also { invalidateExportCurrentness() }

    suspend fun deleteSavedFilesForProduct(productId: String): PresetCleanupSummary =
        cleanupRepository.deleteForProduct(productId).also { invalidateExportCurrentness() }

    suspend fun markReviewed(productId: String) = managedHeadphonesRepository.markReviewed(productId)

    suspend fun toggleFavorite(
        profile: OpraEqProfile,
        manufacturer: String,
        model: String,
    ): Boolean = savedEqRepository.toggleFavorite(
        activeOutputId(),
        profile,
        manufacturer,
        model,
    )

    suspend fun saveGeneralPreset(preset: GeneralEqPreset): Boolean =
        savedGeneralEqRepository.saveForOutput(activeOutputId(), preset)

    suspend fun hideCanonicalProfiles(profileIds: Set<String>) =
        preferencesRepository.hideCanonicalProfiles(profileIds)

    suspend fun unhideCanonicalProfiles(profileIds: Set<String>) =
        preferencesRepository.unhideCanonicalProfiles(profileIds)

    suspend fun importPersonal(
        manufacturer: String,
        model: String,
        displayName: String,
        target: String?,
        peqText: String,
    ): SavedEqRecord = savedEqRepository.importPersonal(
        outputId = activeOutputId(),
        manufacturer = manufacturer,
        model = model,
        displayName = displayName,
        target = target,
        peqText = peqText,
    )

    suspend fun deleteSavedEq(entryId: String) =
        savedEqRepository.removeFromOutput(activeOutputId(), entryId)

    suspend fun removeGeneralEq(presetId: String) =
        savedGeneralEqRepository.removeFromOutput(activeOutputId(), presetId)

    suspend fun setExportTree(uri: String, label: String) = preferencesRepository.setExportTree(uri, label)

    suspend fun exportSelected(treeUri: String, device: ExportDevice): PresetExportSummary {
        val library = loadLibraryData(device.name)
        val records = withContext(computationDispatcher) { library.toExportRecords() }
        return exportWithInvalidation {
            exportRepository.exportSelected(treeUri, records, device)
        }
    }

    suspend fun exportProduct(
        treeUri: String,
        productId: String,
        device: ExportDevice,
    ): PresetExportSummary {
        val managed = managedHeadphonesRepository.getHeadphone(productId, device.name)
            ?: return PresetExportSummary(emptyList())
        return exportWithInvalidation {
            exportRepository.exportSelected(treeUri, listOf(managed), device)
        }
    }

    suspend fun exportManagedProfile(
        treeUri: String,
        productId: String,
        profileId: String,
        device: ExportDevice,
    ): PresetExportSummary {
        val managed = managedHeadphonesRepository.getHeadphone(productId, device.name)
            ?: return PresetExportSummary(emptyList())
        val profile = managed.profiles.firstOrNull { it.profileId == profileId && it.selected }
            ?: return PresetExportSummary(emptyList())
        return exportWithInvalidation {
            exportRepository.exportSelected(
                treeUri,
                listOf(managed.copy(profiles = listOf(profile))),
                device,
            )
        }
    }

    suspend fun exportSavedEq(
        treeUri: String,
        entryId: String,
        device: ExportDevice,
    ): PresetExportSummary {
        val record = savedEqRepository.getForOutput(device.name, entryId)
            ?: return PresetExportSummary(emptyList())
        val exportRecord = withContext(computationDispatcher) {
            savedEqRepository.toManagedHeadphone(record)
        }
        return exportWithInvalidation {
            exportRepository.exportSelected(treeUri, listOf(exportRecord), device)
        }
    }

    suspend fun exportGeneralEq(
        treeUri: String,
        presetId: String,
        device: ExportDevice,
    ): PresetExportSummary {
        val record = savedGeneralEqRepository.getForOutput(device.name, presetId)
            ?: return PresetExportSummary(emptyList())
        val exportRecord = withContext(computationDispatcher) {
            savedGeneralEqRepository.toExportRecord(record)
        }
        return exportWithInvalidation {
            exportRepository.exportSelected(treeUri, listOf(exportRecord), device)
        }
    }

    suspend fun exportGeneralEqs(
        treeUri: String,
        presetIds: Set<String>,
        device: ExportDevice,
    ): PresetExportSummary {
        val records = presetIds.sorted().mapNotNull { presetId ->
            savedGeneralEqRepository.getForOutput(device.name, presetId)
        }
        val exportRecords = withContext(computationDispatcher) {
            records.map(savedGeneralEqRepository::toExportRecord)
        }
        return exportWithInvalidation {
            exportRepository.exportSelected(treeUri, exportRecords, device)
        }
    }

    suspend fun checkForUpdates(): AppUpdateCheckResult = updateCoordinator.checkNow()

    suspend fun dismissUpdate(version: String) = preferencesRepository.dismissUpdate(version)

    suspend fun dismissPostUpdate() = preferencesRepository.dismissPostUpdateCard()

    fun setThemeMode(themeMode: ThemeMode) {
        viewModelScope.launch { preferencesRepository.setThemeMode(themeMode) }
    }

    fun setExportTargetEnabled(device: ExportDevice, enabled: Boolean) {
        viewModelScope.launch { preferencesRepository.setExportTargetEnabled(device, enabled) }
    }

    fun setActiveExportTarget(device: ExportDevice) {
        viewModelScope.launch { preferencesRepository.setActiveExportTarget(device) }
    }

    fun setDirectBlackPearlFlashEnabled(enabled: Boolean) {
        viewModelScope.launch { preferencesRepository.setDirectBlackPearlFlashEnabled(enabled) }
    }

    fun setDirectFiioJa11FlashEnabled(enabled: Boolean) {
        viewModelScope.launch { preferencesRepository.setDirectFiioJa11FlashEnabled(enabled) }
    }

    fun setDirectJcallyJm12FlashEnabled(enabled: Boolean) {
        viewModelScope.launch { preferencesRepository.setDirectJcallyJm12FlashEnabled(enabled) }
    }

    override fun onCleared() {
        hardwareRepository.close()
        super.onCleared()
    }

    private suspend fun flashHardwareProfile(profile: OpraEqProfile): UiText {
        val preferences = preferencesRepository.snapshot()
        return when (preferences.exportTargets.activeTarget) {
            ExportDevice.BLACK_PEARL -> flashBlackPearlProfile(profile, preferences)
            ExportDevice.FIIO_JA11 -> flashFiioJa11Profile(profile, preferences)
            ExportDevice.JCALLY_JM12 -> flashJcallyJm12Profile(profile, preferences)
            else -> resource(R.string.error_select_supported_hardware)
        }
    }

    private suspend fun flashBlackPearlProfile(
        profile: OpraEqProfile,
        preferences: AppPreferences,
    ): UiText {
        if (preferences.exportTargets.activeTarget != ExportDevice.BLACK_PEARL) {
            return resource(R.string.error_select_black_pearl_flash)
        }
        if (!preferences.directBlackPearlFlashEnabled) {
            return resource(R.string.error_enable_black_pearl_flash)
        }
        if (hardwareRepository.blackPearlConnectionState.value !is BlackPearlConnectionState.Connected) {
            return resource(R.string.error_connect_black_pearl_flash)
        }
        return when (val result = hardwareRepository.flashBlackPearl(profile)) {
            is BlackPearlFlashResult.Success -> result.warning?.let { warning ->
                resource(
                    R.string.black_pearl_flash_success_warning,
                    result.appliedPlaybackGainDb,
                    warning,
                )
            } ?: resource(R.string.black_pearl_flash_success, result.appliedPlaybackGainDb)
            is BlackPearlFlashResult.NotRepresentable ->
                resource(R.string.black_pearl_not_flashable, result.reason)
            is BlackPearlFlashResult.DeviceUnavailable -> UiText.Dynamic(result.reason)
            is BlackPearlFlashResult.TransferFailed -> UiText.Dynamic(result.reason)
        }
    }

    private suspend fun flashFiioJa11Profile(
        profile: OpraEqProfile,
        preferences: AppPreferences,
    ): UiText {
        if (!preferences.directFiioJa11FlashEnabled) {
            return resource(R.string.error_enable_fiio_flash)
        }
        if (hardwareRepository.fiioJa11ConnectionState.value !is Kt02h20ConnectionState.Connected) {
            return resource(R.string.error_connect_fiio_flash)
        }
        return when (val result = hardwareRepository.flashFiioJa11(profile)) {
            is Kt02h20FlashResult.Success -> resource(
                if (result.representation.fidelity == DevicePresetFidelity.EXACT) {
                    R.string.fiio_flash_success_exact
                } else {
                    R.string.fiio_flash_success_optimized
                },
                result.representation.playbackGainDb,
            )
            is Kt02h20FlashResult.NotSuitable ->
                resource(R.string.fiio_not_suitable, result.reason)
            is Kt02h20FlashResult.DeviceUnavailable -> UiText.Dynamic(result.reason)
            is Kt02h20FlashResult.TransferFailed -> UiText.Dynamic(result.reason)
            is Kt02h20FlashResult.VerificationFailed ->
                resource(R.string.fiio_flash_verification_failed, result.reason)
        }
    }

    private suspend fun flashJcallyJm12Profile(
        profile: OpraEqProfile,
        preferences: AppPreferences,
    ): UiText {
        if (!preferences.directJcallyJm12FlashEnabled) {
            return resource(R.string.error_enable_jm12_flash)
        }
        if (hardwareRepository.jcallyJm12ConnectionState.value !is Kt02h20ConnectionState.Connected) {
            return resource(R.string.error_connect_jm12_flash)
        }
        return when (val result = hardwareRepository.flashJcallyJm12(profile)) {
            is Kt02h20FlashResult.Success -> resource(
                if (result.representation.fidelity == DevicePresetFidelity.EXACT) {
                    R.string.jm12_flash_success_exact
                } else {
                    R.string.jm12_flash_success_optimized
                },
                result.representation.playbackGainDb,
            )
            is Kt02h20FlashResult.NotSuitable ->
                resource(R.string.jm12_not_suitable, result.reason)
            is Kt02h20FlashResult.DeviceUnavailable -> UiText.Dynamic(result.reason)
            is Kt02h20FlashResult.TransferFailed -> UiText.Dynamic(result.reason)
            is Kt02h20FlashResult.VerificationFailed ->
                resource(R.string.jm12_flash_verification_failed, result.reason)
        }
    }

    private suspend fun loadLibraryData(outputId: String): LibraryDataState = combine(
        managedHeadphonesRepository.observeHeadphones(outputId),
        savedEqRepository.observeForOutput(outputId),
        savedGeneralEqRepository.observeForOutput(outputId),
    ) { managedHeadphones, savedEqs, savedGeneralEqs ->
        LibraryDataState(
            outputId = outputId,
            managedHeadphones = managedHeadphones,
            savedEqs = savedEqs,
            savedGeneralEqs = savedGeneralEqs,
        )
    }.first()

    private fun LibraryDataState.toExportRecords(): List<ManagedHeadphoneRecord> = buildList {
        addAll(managedHeadphones)
        addAll(savedEqs.map(savedEqRepository::toManagedHeadphone))
        addAll(savedGeneralEqs.map(savedGeneralEqRepository::toExportRecord))
    }

    private suspend fun exportWithInvalidation(
        export: suspend () -> PresetExportSummary,
    ): PresetExportSummary {
        val summary = export()
        invalidateExportCurrentness()
        return summary
    }

    private fun invalidateExportCurrentness() {
        exportInvalidation.update { version -> version + 1L }
    }

    private suspend fun activeOutputId(): String =
        preferencesRepository.snapshot().exportTargets.activeTarget.name

    private suspend fun refreshCatalogIfDue() {
        val ready = catalogRepository.state.value as? CatalogState.Ready ?: return
        val now = nowMillis()
        if (now - ready.lastSuccessfulRefreshMillis < FOREGROUND_REFRESH_INTERVAL_MILLIS) return
        if (now - lastForegroundRefreshAttemptMillis < FOREGROUND_RETRY_THROTTLE_MILLIS) return
        lastForegroundRefreshAttemptMillis = now
        syncCoordinator.refresh()
    }

    private fun resource(resourceId: Int, vararg args: Any): UiText.Resource =
        UiText.Resource(resourceId, args.toList())

    class Factory(
        private val dependenciesProvider: () -> Dependencies,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            require(modelClass.isAssignableFrom(EqLibraryViewModel::class.java))
            val dependencies = dependenciesProvider()
            return EqLibraryViewModel(
                preferencesRepository = dependencies.preferencesRepository,
                catalogRepository = dependencies.catalogRepository,
                managedHeadphonesRepository = dependencies.managedHeadphonesRepository,
                savedEqRepository = dependencies.savedEqRepository,
                savedGeneralEqRepository = dependencies.savedGeneralEqRepository,
                exportRepository = dependencies.exportRepository,
                cleanupRepository = dependencies.cleanupRepository,
                syncCoordinator = dependencies.syncCoordinator,
                updateCoordinator = dependencies.updateCoordinator,
                hardwareRepository = dependencies.hardwareRepository,
            ) as T
        }
    }

    data class Dependencies(
        val preferencesRepository: AppPreferencesRepository,
        val catalogRepository: AppCatalogRepository,
        val managedHeadphonesRepository: ManagedHeadphonesRepository,
        val savedEqRepository: SavedEqRepository,
        val savedGeneralEqRepository: SavedGeneralEqRepository,
        val exportRepository: PresetExportRepository,
        val cleanupRepository: PresetCleanupRepository,
        val syncCoordinator: CatalogSyncCoordinator,
        val updateCoordinator: AppUpdateCoordinator,
        val hardwareRepository: HardwareEqRepository,
    )

    companion object {
        private const val STOP_TIMEOUT_MILLIS = 5_000L
        private const val ZERO_GAIN_EPSILON = 0.000_001
        private const val FOREGROUND_REFRESH_INTERVAL_MILLIS = 24L * 60L * 60L * 1000L
        private const val FOREGROUND_RETRY_THROTTLE_MILLIS = 15L * 60L * 1000L
    }
}
