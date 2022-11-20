package com.nunchuk.android.signer.tapsigner.backup.upload

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nunchuk.android.core.util.orUnknownError
import com.nunchuk.android.model.KeyUpload
import com.nunchuk.android.model.MembershipStep
import com.nunchuk.android.share.membership.MembershipStepManager
import com.nunchuk.android.signer.util.TAPSIGNER_KEY_1_NAME
import com.nunchuk.android.signer.util.TAPSIGNER_KEY_2_NAME
import com.nunchuk.android.type.SignerType
import com.nunchuk.android.usecase.UploadBackupFileKeyUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class UploadBackUpTapSignerViewModel @Inject constructor(
    private val uploadBackupFileKeyUseCase: UploadBackupFileKeyUseCase,
    private val membershipStepManager: MembershipStepManager,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {
    private val args = UploadBackUpTapSignerFragmentArgs.fromSavedStateHandle(savedStateHandle)
    private val _event = MutableSharedFlow<UploadBackUpTapSignerEvent>()
    val event = _event.asSharedFlow()

    private val _state = MutableStateFlow(UploadBackUpTapSignerState())
    val state = _state.asStateFlow()

    private var isAddNewKey = true

    fun init(isAddNewKey: Boolean) {
        this.isAddNewKey = isAddNewKey
    }

    fun upload() {
        val keyName = if (membershipStepManager.currentStep == MembershipStep.ADD_TAP_SIGNER_1) {
            TAPSIGNER_KEY_1_NAME
        } else {
            TAPSIGNER_KEY_2_NAME
        }
        viewModelScope.launch {
            uploadBackupFileKeyUseCase(
                UploadBackupFileKeyUseCase.Param(
                    step = membershipStepManager.currentStep ?: MembershipStep.ADD_TAP_SIGNER_1,
                    keyName = keyName,
                    keyType = SignerType.NFC.name,
                    xfp = args.masterSignerId,
                    filePath = args.filePath,
                    isAddNewKey = isAddNewKey,
                    plan = membershipStepManager.plan
                )
            ).collect {
                if (it.isSuccess) {
                    when (val content = it.getOrThrow()) {
                        is KeyUpload.Progress -> {
                            _state.update { state ->
                                state.copy(
                                    percent = content.value,
                                    isError = false
                                )
                            }
                        }
                        is KeyUpload.Data -> {
                            _state.update { state ->
                                state.copy(serverFilePath = content.filePath)
                            }
                        }
                        is KeyUpload.KeyVerified -> {
                            _event.emit(UploadBackUpTapSignerEvent.KeyVerified(content.message))
                        }
                    }
                } else {
                    _state.update { state -> state.copy(isError = true) }
                    _event.emit(UploadBackUpTapSignerEvent.ShowError(it.exceptionOrNull()?.message.orUnknownError()))
                }
            }
        }
    }

    fun getServerFilePath(): String = state.value.serverFilePath

    fun onContinueClicked() {
        viewModelScope.launch {
            if (_state.value.isError) {
                upload()
                _state.update { state -> state.copy(percent = 0, isError = false) }
            } else {
                _event.emit(UploadBackUpTapSignerEvent.OnContinueClicked)
            }
        }
    }
}

data class UploadBackUpTapSignerState(
    val percent: Int = 0,
    val isError: Boolean = false,
    val serverFilePath: String = ""
)

sealed class UploadBackUpTapSignerEvent {
    object OnContinueClicked : UploadBackUpTapSignerEvent()
    data class KeyVerified(val message: String) : UploadBackUpTapSignerEvent()
    data class ShowError(val message: String) : UploadBackUpTapSignerEvent()
}