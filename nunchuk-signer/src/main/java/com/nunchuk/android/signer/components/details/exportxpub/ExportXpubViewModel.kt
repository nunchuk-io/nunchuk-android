package com.nunchuk.android.signer.components.details.exportxpub

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nunchuk.android.signer.components.details.exportxpub.exporter.XpubAction
import com.nunchuk.android.signer.components.details.exportxpub.exporter.XpubExporter
import com.nunchuk.android.signer.components.details.exportxpub.exporter.XpubExporters
import com.nunchuk.android.signer.components.details.exportxpub.exporter.XpubInput
import com.nunchuk.android.signer.components.details.exportxpub.exporter.XpubPathRules
import com.nunchuk.android.signer.components.details.exportxpub.exporter.XpubRequest
import com.nunchuk.android.signer.components.details.exportxpub.exporter.XpubStep
import com.nunchuk.android.type.AddressType
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Drives Export XPUB for any key type: the key's [XpubExporter] decides what to ask for, and
 * this only relays its steps to the screen.
 *
 * At most one export runs at a time. Resubmitting the same path while it runs is ignored, a
 * different path replaces it, and [cancelExport] drops it; a replaced or canceled export never
 * reaches the screen, even if its SDK call finishes afterwards.
 */
@HiltViewModel
internal class ExportXpubViewModel @Inject constructor(
    private val savedStateHandle: SavedStateHandle,
    exporters: XpubExporters,
    private val pathRules: XpubPathRules,
) : ViewModel() {
    val args = ExportXpubArgs.fromSavedStateHandle(savedStateHandle)
    private val exporter = exporters.find(args.key)

    /** False opens straight on the export screen with the key's own spec. */
    val selectsAccount = exporter?.selectsAccount == true

    private val _state = MutableStateFlow(
        ExportXpubUiState(
            descriptor = savedStateHandle[KEY_DESCRIPTOR] ?: args.descriptor,
            customPath = savedStateHandle[KEY_CUSTOM_PATH] ?: "",
        )
    )
    val state = _state.asStateFlow()

    // Buffered: a stored key starts exporting in init, before the screen collects.
    private val _event = Channel<ExportXpubEvent>(Channel.BUFFERED)
    val event = _event.receiveAsFlow()

    private var validatePathJob: Job? = null
    private var activeExport: ActiveExport? = null
    private var lastExportId = 0L

    /**
     * The path of the export in progress, kept across a passphrase prompt or a card tap — and,
     * like the custom-path draft, across process death, so a restored prompt still continues
     * the path the user asked for.
     */
    private var pendingPath: String?
        get() = savedStateHandle[KEY_PENDING_PATH]
        set(value) {
            savedStateHandle[KEY_PENDING_PATH] = value
        }

    init {
        if (_state.value.customPath.isNotBlank()) onCustomPathChanged(_state.value.customPath)
        if (!selectsAccount && _state.value.descriptor.isEmpty()) {
            submit(key = null, closeOnError = true, resolvePath = { Result.success(null) }) { export(it) }
        }
    }

    /** The path is resolved inside the export, so canceling or replacing it covers that step too. */
    fun exportPreset(isSingleSig: Boolean, addressType: AddressType, accountIndex: Int) {
        submit(
            key = PresetSelection(isSingleSig, addressType, accountIndex),
            resolvePath = { pathRules.presetPath(isSingleSig, addressType, accountIndex) },
        ) { export(it) }
    }

    /** Opens the path editor, starting from the current preset path unless one was typed already. */
    fun openCustomPath(isSingleSig: Boolean, addressType: AddressType, accountIndex: Int) {
        viewModelScope.launch {
            if (_state.value.customPath.isBlank()) {
                pathRules.presetPath(isSingleSig, addressType, accountIndex)
                    .onSuccess { onCustomPathChanged(it) }
            }
            _event.send(ExportXpubEvent.OpenCustomPath)
        }
    }

    fun onCustomPathChanged(path: String) {
        savedStateHandle[KEY_CUSTOM_PATH] = path
        _state.update { it.copy(customPath = path, isCustomPathValid = null) }
        validatePathJob?.cancel()
        if (path.isBlank()) return
        validatePathJob = viewModelScope.launch {
            val isValid = pathRules.isValid(path.trim())
            _state.update { if (it.customPath == path) it.copy(isCustomPathValid = isValid) else it }
        }
    }

    fun exportCustomPath() {
        if (_state.value.isCustomPathValid != true) return
        val path = _state.value.customPath.trim()
        submit(key = path, resolvePath = { Result.success(path) }) { export(it) }
    }

    /** What the user or the device supplied for the last [ExportXpubEvent.NeedInput]. */
    fun onInput(input: XpubInput) {
        val path = pendingPath
        if (selectsAccount && path == null) return
        submit(
            key = path,
            closeOnError = !selectsAccount,
            input = input,
            resolvePath = { Result.success(path) },
        ) { resume(it, input) }
    }

    /** The user declined the input the last [ExportXpubEvent.NeedInput] asked for. */
    fun onInputCanceled() {
        pendingPath = null
    }

    /**
     * The user dismissed the progress dialog: drop the export so its result is never shown. A
     * stored key has nothing else to show without its spec, so that returns to Key info.
     */
    fun cancelExport() {
        activeExport?.job?.cancel()
        activeExport = null
        pendingPath = null
        _state.update { it.copy(loading = ExportXpubLoading.NONE) }
        if (!selectsAccount && _state.value.descriptor.isEmpty()) {
            viewModelScope.launch { _event.send(ExportXpubEvent.Close) }
        }
    }

    /**
     * Runs one export, from resolving its path to showing its result. [key] names what the user
     * asked for: asking again for the running one is ignored, anything else replaces it.
     */
    private fun submit(
        key: Any?,
        closeOnError: Boolean = false,
        input: XpubInput? = null,
        resolvePath: suspend () -> Result<String?>,
        step: suspend XpubExporter.(XpubRequest) -> XpubStep,
    ) {
        val exporter = exporter ?: run {
            viewModelScope.launch {
                _event.send(
                    ExportXpubEvent.Error(IllegalStateException("Export XPUB isn't supported for this key"), close = true)
                )
            }
            return
        }
        val running = activeExport
        if (running != null && running.job.isActive) {
            if (running.key == key && running.input == input) return
            running.job.cancel()
        }
        val id = ++lastExportId
        val isNfc = input is XpubInput.Nfc
        _state.update { it.copy(loading = if (isNfc) ExportXpubLoading.NFC else ExportXpubLoading.DEFAULT) }
        // Lazy so [activeExport] names this export before any of it runs.
        val job = viewModelScope.launch(start = CoroutineStart.LAZY) {
            val result = try {
                resolvePath().fold(
                    onSuccess = { path ->
                        // UseCase turns a cancellation into a failed Result, so check before going on.
                        ensureActive()
                        pendingPath = path
                        exporter.step(
                            XpubRequest(
                                masterSignerId = args.masterSignerId,
                                masterFingerprint = args.masterFingerprint,
                                derivationPath = args.derivationPath,
                                path = path,
                            )
                        )
                    },
                    onFailure = { XpubStep.Failed(it) },
                )
            } finally {
                if (activeExport?.id == id) _state.update { it.copy(loading = ExportXpubLoading.NONE) }
            }
            ensureActive()
            if (activeExport?.id != id) return@launch
            activeExport = null
            when (result) {
                is XpubStep.Done -> {
                    pendingPath = null
                    savedStateHandle[KEY_DESCRIPTOR] = result.descriptor
                    _state.update { it.copy(descriptor = result.descriptor) }
                    _event.send(ExportXpubEvent.OpenExport)
                }
                is XpubStep.NeedInput -> _event.send(ExportXpubEvent.NeedInput(result.action))
                // The path stays pending: a failed card read is retried with the same path.
                is XpubStep.Failed -> _event.send(ExportXpubEvent.Error(result.error, close = closeOnError, isNfc = isNfc))
            }
        }
        activeExport = ActiveExport(id = id, key = key, input = input, job = job)
        job.start()
    }

    private data class PresetSelection(val isSingleSig: Boolean, val addressType: AddressType, val accountIndex: Int)

    private class ActiveExport(val id: Long, val key: Any?, val input: XpubInput?, val job: Job)

    companion object {
        private const val KEY_PENDING_PATH = "pending_path"
        private const val KEY_DESCRIPTOR = "descriptor"
        private const val KEY_CUSTOM_PATH = "custom_path"
    }
}

internal enum class ExportXpubLoading { NONE, DEFAULT, NFC }

internal data class ExportXpubUiState(
    val descriptor: String = "",
    val customPath: String = "",
    /** Null while the path is blank or still being checked. */
    val isCustomPathValid: Boolean? = null,
    val loading: ExportXpubLoading = ExportXpubLoading.NONE,
)

internal sealed class ExportXpubEvent {
    data class NeedInput(val action: XpubAction) : ExportXpubEvent()
    data object OpenCustomPath : ExportXpubEvent()
    data object OpenExport : ExportXpubEvent()

    /** Nothing is left to show: return to Key info. */
    data object Close : ExportXpubEvent()

    /**
     * [close] when there's nothing left to show, e.g. a stored key whose spec couldn't load;
     * [isNfc] when it came off a card read, so the NFC error handling sees it first.
     */
    data class Error(val e: Throwable, val close: Boolean = false, val isNfc: Boolean = false) : ExportXpubEvent()
}
