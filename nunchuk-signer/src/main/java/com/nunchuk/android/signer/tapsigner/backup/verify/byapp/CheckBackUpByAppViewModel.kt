package com.nunchuk.android.signer.tapsigner.backup.verify.byapp

import android.app.Application
import android.nfc.tech.IsoDep
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nunchuk.android.core.domain.GetTapSignerBackupUseCase
import com.nunchuk.android.core.domain.VerifyTapSignerBackupUseCase
import com.nunchuk.android.signer.R
import com.nunchuk.android.usecase.membership.SetKeyVerifiedUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CheckBackUpByAppViewModel @Inject constructor(
    private val verifyTapSignerBackupUseCase: VerifyTapSignerBackupUseCase,
    private val getTapSignerBackupUseCase: GetTapSignerBackupUseCase,
    private val setKeyVerifiedUseCase: SetKeyVerifiedUseCase,
    savedStateHandle: SavedStateHandle,
    private val application: Application
) : ViewModel() {
    private val args: CheckBackUpByAppFragmentArgs =
        CheckBackUpByAppFragmentArgs.fromSavedStateHandle(savedStateHandle)
    private val _event = MutableSharedFlow<CheckBackUpByAppEvent>()
    val event = _event.asSharedFlow()

    var decryptionKey by mutableStateOf("")
        private set
    var errorMessage by mutableStateOf("")
        private set

    var tryCount = 0

    fun onContinueClicked(masterSignerId: String) {
        viewModelScope.launch {
            val result =
                verifyTapSignerBackupUseCase(VerifyTapSignerBackupUseCase.Data(
                    masterSignerId = masterSignerId,
                    backUpKey = args.filePath,
                    decryptionKey = decryptionKey
                ))
            if (result.isSuccess && result.getOrThrow()) {
                val apiResult = setKeyVerifiedUseCase(masterSignerId)
                if (apiResult.isSuccess) {
                    _event.emit(CheckBackUpByAppEvent.OnVerifyBackUpKeySuccess)
                } else {
                    _event.emit(CheckBackUpByAppEvent.ShowError(apiResult.exceptionOrNull()))
                }
            } else {
                tryCount ++
                errorMessage = application.getString(R.string.nc_decryption_failed_try_again)
                if (tryCount == MAX_TRY) {
                    _event.emit(CheckBackUpByAppEvent.OnVerifyFailedTooMuch)
                }
            }
        }
    }

    fun getTapSignerBackup(isoDep: IsoDep,  masterSignerId: String, cvc: String) {
        viewModelScope.launch {
            _event.emit(CheckBackUpByAppEvent.NfcLoading(true))
            val result = getTapSignerBackupUseCase(GetTapSignerBackupUseCase.Data(isoDep, cvc, masterSignerId))
            _event.emit(CheckBackUpByAppEvent.NfcLoading(false))
            if (result.isSuccess) {
                _event.emit(CheckBackUpByAppEvent.GetTapSignerBackupKeyEvent(result.getOrThrow()))
            } else {
                _event.emit(CheckBackUpByAppEvent.ShowError(result.exceptionOrNull()))
            }
        }
    }

    fun onDecryptionKeyChange(value: String) {
        decryptionKey = value
    }

    companion object {
        private const val MAX_TRY = 5
    }
}

sealed class CheckBackUpByAppEvent {
    object OnVerifyBackUpKeySuccess : CheckBackUpByAppEvent()
    object OnVerifyFailedTooMuch : CheckBackUpByAppEvent()
    data class NfcLoading(val isLoading: Boolean) : CheckBackUpByAppEvent()
    data class GetTapSignerBackupKeyEvent(val filePath: String) : CheckBackUpByAppEvent()
    data class ShowError(val e: Throwable?) : CheckBackUpByAppEvent()
}