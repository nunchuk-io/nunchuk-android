package com.nunchuk.android.signer.mk4.inheritance.backup.myself

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nunchuk.android.core.domain.utils.NfcFileManager
import com.nunchuk.android.domain.di.IoDispatcher
import com.nunchuk.android.usecase.GetDownloadBackUpKeyReplacementUseCase
import com.nunchuk.android.usecase.GetDownloadBackUpKeyUseCase
import com.nunchuk.android.usecase.SaveLocalFileUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject

@HiltViewModel
class ColdCardVerifyBackUpMyselfViewModel @Inject constructor(
    private val nfcFileManager: NfcFileManager,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
    private val getDownloadBackUpKeyUseCase: GetDownloadBackUpKeyUseCase,
    private val getDownloadBackUpKeyReplacementUseCase: GetDownloadBackUpKeyReplacementUseCase,
    private val saveLocalFileUseCase: SaveLocalFileUseCase
) : ViewModel() {

    private val _event = MutableSharedFlow<ColdCardVerifyBackUpMyselfEvent>()
    val event = _event.asSharedFlow()

    var downloadBackupFilePath: String = ""
        private set

    fun handleDownloadBackupKey(
        backUpFileName: String, filePath: String, xfp: String, groupId: String,
        walletId: String, isReplaceKey: Boolean
    ) {
        viewModelScope.launch {
            val newFile = withContext(ioDispatcher) {
                var file: File
                runCatching {
                    if (File(filePath).exists().not()) {
                        throw Exception("File not found")
                    }
                }.also {
                    file = if (it.isSuccess) {
                        File(filePath)
                    } else {
                        File(
                            if (isReplaceKey.not()) {
                                getDownloadBackUpKeyUseCase(
                                    GetDownloadBackUpKeyUseCase.Param(
                                        xfp = xfp,
                                        groupId = groupId
                                    )
                                ).getOrThrow()
                            } else {
                                getDownloadBackUpKeyReplacementUseCase(
                                    GetDownloadBackUpKeyReplacementUseCase.Param(
                                        xfp = xfp,
                                        groupId = groupId,
                                        walletId = walletId
                                    )
                                ).getOrThrow()
                            }
                        )
                    }
                }
                file.copyTo(
                    nfcFileManager.getBackUpKeyFileWithName(backUpFileName),
                    true
                )
            }
            downloadBackupFilePath = newFile.absolutePath
            _event.emit(ColdCardVerifyBackUpMyselfEvent.GetBackUpKeySuccess(downloadBackupFilePath))
        }
    }

    fun saveLocalFile() {
        viewModelScope.launch {
            if (downloadBackupFilePath.isEmpty()) {
                return@launch
            }
            val result = saveLocalFileUseCase(SaveLocalFileUseCase.Params(filePath = downloadBackupFilePath))
            _event.emit(ColdCardVerifyBackUpMyselfEvent.SaveLocalFile(result.isSuccess))
        }
    }
}

sealed class ColdCardVerifyBackUpMyselfEvent {
    data class GetBackUpKeySuccess(val filePath: String) : ColdCardVerifyBackUpMyselfEvent()
    data class SaveLocalFile(val isSuccess: Boolean) : ColdCardVerifyBackUpMyselfEvent()
}