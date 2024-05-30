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
    private val nfcFileManager: NfcFileManager,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
    savedStateHandle: SavedStateHandle
) : ViewModel() {
    private val args: CheckBackUpBySelfFragmentArgs =
        CheckBackUpBySelfFragmentArgs.fromSavedStateHandle(savedStateHandle)
    private val _event = MutableSharedFlow<CheckBackUpBySelfEvent>()
    val event = _event.asSharedFlow()

    fun onBtnClicked(event: CheckBackUpBySelfEvent) {
        if (event is OnDownloadBackUpClicked) {
            handleDownloadBackupKey()
        } else {
            viewModelScope.launch {
                _event.emit(event)
            }
        }
    }

    private fun handleDownloadBackupKey() {
        viewModelScope.launch {
            getTapSignerStatusByIdUseCase(args.masterSignerId).onSuccess { status ->
                val newFile = withContext(ioDispatcher) {
                    File(args.filePath).copyTo(
                        nfcFileManager.getBackUpKeyFile(status.ident.orEmpty()),
                        true
                    )
                }
                _event.emit(GetBackUpKeySuccess(newFile.absolutePath))
            }
        }
    }

    fun setKeyVerified(groupId: String) {
        viewModelScope.launch {
            val result =
                setKeyVerifiedUseCase(
                    SetKeyVerifiedUseCase.Param(
                        groupId,
                        args.masterSignerId,
                        false
                    )
                )
            if (result.isSuccess) {
                _event.emit(OnExitSelfCheck)
            } else {
                _event.emit(ShowError(result.exceptionOrNull()))
            }
        }
    }

    fun setReplaceKeyVerified(keyId: String) {
        viewModelScope.launch {
            setReplaceKeyVerifiedUseCase(
                SetReplaceKeyVerifiedUseCase.Param(
                    keyId = keyId,
                    checkSum = getChecksum(),
                    isAppVerified = false
                )
            ).onSuccess {
                _event.emit(OnExitSelfCheck)
            }.onFailure {
                _event.emit(ShowError(it))
            }
        }
    }

    private suspend fun getChecksum(): String = withContext(ioDispatcher) {
        ChecksumUtil.getChecksum(File(args.filePath).readBytes())
    }
}

sealed class CheckBackUpBySelfEvent
data object OnDownloadBackUpClicked : CheckBackUpBySelfEvent()
data object OnVerifiedBackUpClicked : CheckBackUpBySelfEvent()
data object OnExitSelfCheck : CheckBackUpBySelfEvent()
data class GetBackUpKeySuccess(val filePath: String) : CheckBackUpBySelfEvent()
data class ShowError(val e: Throwable?) : CheckBackUpBySelfEvent()