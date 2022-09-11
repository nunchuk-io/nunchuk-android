package com.nunchuk.android.wallet.personal.components

import android.app.Application
import android.net.Uri
import android.nfc.NdefRecord
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nunchuk.android.core.domain.ImportWalletFromMk4UseCase
import com.nunchuk.android.core.util.getFileFromUri
import com.nunchuk.android.core.util.orUnknownError
import com.nunchuk.android.domain.di.IoDispatcher
import com.nunchuk.android.usecase.GetCompoundSignersUseCase
import dagger.Lazy
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
class WalletIntermediaryViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val getCompoundSignersUseCase: Lazy<GetCompoundSignersUseCase>,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
    private val application: Application,
    private val importWalletFromMk4UseCase: ImportWalletFromMk4UseCase
) : ViewModel() {
    private val _state = MutableStateFlow(WalletIntermediaryState())
    private val _event = MutableSharedFlow<WalletIntermediaryEvent>()
    val event = _event.asSharedFlow()

    init {
        val args = WalletIntermediaryFragmentArgs.fromSavedStateHandle(savedStateHandle)
        if (args.isQuickWallet) {
            viewModelScope.launch {
                getCompoundSignersUseCase.get().execute().collect {
                    _state.value = WalletIntermediaryState(it.first.isNotEmpty() || it.second.isNotEmpty())
                }
            }
        }
    }

    fun extractFilePath(uri: Uri) {
        viewModelScope.launch {
            _event.emit(WalletIntermediaryEvent.Loading)
            val result = withContext(ioDispatcher) {
                getFileFromUri(application.contentResolver, uri, application.cacheDir)
            }
            _event.emit(WalletIntermediaryEvent.OnLoadFileSuccess(result?.absolutePath.orEmpty()))
        }
    }

    fun importWalletFromMk4(records: List<NdefRecord>) {
        viewModelScope.launch {
            _event.emit(WalletIntermediaryEvent.Loading)
            val result = importWalletFromMk4UseCase(records)
            if (result.isSuccess && result.getOrThrow() != null) {
                _event.emit(WalletIntermediaryEvent.ImportWalletFromMk4Success(result.getOrThrow()!!.id))
            } else {
                _event.emit(WalletIntermediaryEvent.ShowError(result.exceptionOrNull()?.message.orUnknownError()))
            }
        }
    }

    val hasSigner: Boolean
        get() = _state.value.isHasSigner
}

sealed class WalletIntermediaryEvent {
    object Loading : WalletIntermediaryEvent()
    class OnLoadFileSuccess(val path: String) : WalletIntermediaryEvent()
    class ImportWalletFromMk4Success(val walletId: String) : WalletIntermediaryEvent()
    class ShowError(val msg: String) : WalletIntermediaryEvent()
}

data class WalletIntermediaryState(val isHasSigner: Boolean = false)

