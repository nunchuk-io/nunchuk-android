/**************************************************************************
 * This file is part of the Nunchuk software (https://nunchuk.io/)        *
 * Copyright (C) 2022, 2023 Nunchuk                                       *
 *                                                                        *
 * This program is free software; you can redistribute it and/or          *
 * modify it under the terms of the GNU General Public License            *
 * as published by the Free Software Foundation; either version 3         *
 * of the License, or (at your option) any later version.                 *
 *                                                                        *
 * This program is distributed in the hope that it will be useful,        *
 * but WITHOUT ANY WARRANTY; without even the implied warranty of         *
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the          *
 * GNU General Public License for more details.                           *
 *                                                                        *
 * You should have received a copy of the GNU General Public License      *
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.  *
 *                                                                        *
 **************************************************************************/

package com.nunchuk.android.signer.tapsigner.backup.verify.byself

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nunchuk.android.core.domain.GetTapSignerStatusByIdUseCase
import com.nunchuk.android.core.domain.utils.NfcFileManager
import com.nunchuk.android.domain.di.IoDispatcher
import com.nunchuk.android.model.VerifyType
import com.nunchuk.android.usecase.GetDownloadBackUpKeyReplacementUseCase
import com.nunchuk.android.usecase.GetDownloadBackUpKeyUseCase
import com.nunchuk.android.usecase.SaveLocalFileUseCase
import com.nunchuk.android.usecase.membership.SetKeyVerifiedUseCase
import com.nunchuk.android.usecase.membership.SetReplaceKeyVerifiedUseCase
import com.nunchuk.android.utils.ChecksumUtil
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject

@HiltViewModel
class CheckBackUpBySelfViewModel @Inject constructor(
    private val setKeyVerifiedUseCase: SetKeyVerifiedUseCase,
    private val setReplaceKeyVerifiedUseCase: SetReplaceKeyVerifiedUseCase,
    private val getTapSignerStatusByIdUseCase: GetTapSignerStatusByIdUseCase,
    private val getDownloadBackUpKeyUseCase: GetDownloadBackUpKeyUseCase,
    private val getDownloadBackUpKeyReplacementUseCase: GetDownloadBackUpKeyReplacementUseCase,
    private val nfcFileManager: NfcFileManager,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
    private val saveLocalFileUseCase: SaveLocalFileUseCase,
    savedStateHandle: SavedStateHandle
) : ViewModel() {
    private val args: CheckBackUpBySelfFragmentArgs =
        CheckBackUpBySelfFragmentArgs.fromSavedStateHandle(savedStateHandle)
    private val _event = MutableSharedFlow<CheckBackUpBySelfEvent>()
    val event = _event.asSharedFlow()

    private var ident: String = ""
    var downloadBackupFilePath: String = ""
        private set

    init {
        viewModelScope.launch {
            getTapSignerStatusByIdUseCase(args.masterSignerId).onSuccess { status ->
                ident = status.ident.orEmpty()
            }
        }
    }

    fun onBtnClicked(event: CheckBackUpBySelfEvent) {
        if (event is OnDownloadBackUpClicked) {
            handleDownloadBackupKey(event.keyId, event.groupId, event.walletId)
        } else {
            viewModelScope.launch {
                _event.emit(event)
            }
        }
    }

    private fun handleDownloadBackupKey(keyId: String, groupId: String, walletId: String) {
        viewModelScope.launch {
            getTapSignerStatusByIdUseCase(args.masterSignerId).onSuccess { status ->
                val newFile = downloadBackupKey(keyId.isNotEmpty(), groupId, walletId)
                downloadBackupFilePath = newFile.absolutePath
                _event.emit(GetBackUpKeySuccess(downloadBackupFilePath))
            }
        }
    }

    fun setKeyVerified(groupId: String, isOnChainBackUp: Boolean) {
        viewModelScope.launch {
            val result =
                setKeyVerifiedUseCase(
                    SetKeyVerifiedUseCase.Param(
                        groupId,
                        args.masterSignerId,
                        verifyType = VerifyType.SELF_VERIFIED
                    )
                )
            if (result.isSuccess) {
                _event.emit(OnExitSelfCheck)
            } else {
                _event.emit(ShowError(result.exceptionOrNull()))
            }
        }
    }

    fun setReplaceKeyVerified(keyId: String, groupId: String, walletId: String, isOnChainBackUp: Boolean) {
        viewModelScope.launch {
            setReplaceKeyVerifiedUseCase(
                SetReplaceKeyVerifiedUseCase.Param(
                    keyId = keyId,
                    checkSum = getChecksum(true, groupId = groupId, walletId = walletId),
                    isAppVerified = false,
                    groupId = groupId,
                    walletId = walletId
                )
            ).onSuccess {
                _event.emit(OnExitSelfCheck)
            }.onFailure {
                _event.emit(ShowError(it))
            }
        }
    }

    private suspend fun getChecksum(isReplaceKey: Boolean, groupId: String, walletId: String): String = withContext(ioDispatcher) {
        val newFile = downloadBackupKey(isReplaceKey, groupId, walletId)
        ChecksumUtil.getChecksum(newFile.readBytes())
    }

    private suspend fun downloadBackupKey(isReplaceKey: Boolean, groupId: String, walletId: String): File {
        val newFile = withContext(ioDispatcher) {
            var file: File
            runCatching {
                if (File(args.filePath).exists().not()) {
                    throw Exception("File not found")
                }
            }.also {
                file = if (it.isSuccess) {
                    File(args.filePath)
                } else {
                    File(
                        if (isReplaceKey.not()) {
                            getDownloadBackUpKeyUseCase(
                                GetDownloadBackUpKeyUseCase.Param(
                                    xfp = args.masterSignerId,
                                    groupId = groupId
                                )
                            ).getOrThrow()
                        } else {
                            getDownloadBackUpKeyReplacementUseCase(
                                GetDownloadBackUpKeyReplacementUseCase.Param(
                                    xfp = args.masterSignerId,
                                    groupId = groupId,
                                    walletId = walletId
                                )
                            ).getOrThrow()
                        }
                    )
                }
            }
            file.copyTo(
                nfcFileManager.getBackUpKeyFile(ident),
                true
            )
        }
        return newFile
    }

    fun saveLocalFile() {
        viewModelScope.launch {
            if (downloadBackupFilePath.isEmpty()) {
                return@launch
            }
            val result = saveLocalFileUseCase(SaveLocalFileUseCase.Params(filePath = downloadBackupFilePath))
            _event.emit(SaveLocalFile(result.isSuccess))
        }
    }
}

sealed class CheckBackUpBySelfEvent
data class OnDownloadBackUpClicked(val groupId: String, val walletId: String, val keyId: String) : CheckBackUpBySelfEvent()
data object OnVerifiedBackUpClicked : CheckBackUpBySelfEvent()
data object OnExitSelfCheck : CheckBackUpBySelfEvent()
data class GetBackUpKeySuccess(val filePath: String) : CheckBackUpBySelfEvent()
data class ShowError(val e: Throwable?) : CheckBackUpBySelfEvent()
data class SaveLocalFile(val isSuccess: Boolean) : CheckBackUpBySelfEvent()