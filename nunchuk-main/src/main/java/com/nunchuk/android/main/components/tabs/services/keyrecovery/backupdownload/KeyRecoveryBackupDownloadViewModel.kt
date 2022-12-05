package com.nunchuk.android.main.components.tabs.services.keyrecovery.backupdownload

import android.util.Base64
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nunchuk.android.core.domain.ImportTapsignerMasterSignerContentUseCase
import com.nunchuk.android.core.domain.VerifyTapSignerBackupContentUseCase
import com.nunchuk.android.core.mapper.MasterSignerMapper
import com.nunchuk.android.core.util.orUnknownError
import com.nunchuk.android.domain.di.IoDispatcher
import com.nunchuk.android.main.R
import com.nunchuk.android.main.util.ChecksumUtil
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class KeyRecoveryBackupDownloadViewModel @Inject constructor(
    private val verifyTapSignerBackupContentUseCase: VerifyTapSignerBackupContentUseCase,
    private val importTapsignerMasterSignerContentUseCase: ImportTapsignerMasterSignerContentUseCase,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
    private val masterSignerMapper: MasterSignerMapper,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val args = BackupDownloadFragmentArgs.fromSavedStateHandle(savedStateHandle)

    private val _event = MutableSharedFlow<BackupDownloadEvent>()
    val event = _event.asSharedFlow()

    private val _state = MutableStateFlow(BackupDownloadState())
    val state = _state.asStateFlow()

    fun onContinueClicked() = viewModelScope.launch(ioDispatcher) {
        val stateValue = _state.value
        if (stateValue.password.isBlank()) return@launch
        val backupData = Base64.decode(args.backupKey.keyBackUpBase64, Base64.DEFAULT)
        if (ChecksumUtil.verifyChecksum(backupData, args.backupKey.keyCheckSum)) {
            val resultVerify = verifyTapSignerBackupContentUseCase(
                VerifyTapSignerBackupContentUseCase.Param(
                    content = backupData,
                    masterSignerId = args.signer.id,
                    backUpKey = stateValue.password
                )
            )
            if (resultVerify.isSuccess) {
                val resultImport = importTapsignerMasterSignerContentUseCase(
                    ImportTapsignerMasterSignerContentUseCase.Param(
                        backupData,
                        stateValue.password,
                        stateValue.keyName
                    )
                )
                if (resultImport.isSuccess) {
                    _event.emit(
                        BackupDownloadEvent.ImportTapsignerSuccess(
                            masterSignerMapper(
                                resultImport.getOrThrow()
                            )
                        )
                    )
                } else {
                    _event.emit(BackupDownloadEvent.ProcessFailure(resultImport.exceptionOrNull()?.message.orUnknownError()))
                }
            } else {
                _state.update {
                    it.copy(error = resultVerify.exceptionOrNull()?.message.orUnknownError())
                }
            }
        }
    }

    fun setKeyName(keyName: String) = viewModelScope.launch {
        _state.update {
            it.copy(keyName = keyName)
        }
    }

    fun updatePassword(password: String) = viewModelScope.launch {
        _state.update {
            it.copy(password = password)
        }
    }
}