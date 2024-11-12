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

package com.nunchuk.android.main.components.tabs.services.keyrecovery.backupdownload

import android.util.Base64
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nunchuk.android.core.domain.ImportBackupKeyContentUseCase
import com.nunchuk.android.core.domain.ImportTapsignerMasterSignerContentUseCase
import com.nunchuk.android.core.domain.membership.MarkRecoverStatusUseCase
import com.nunchuk.android.core.mapper.MasterSignerMapper
import com.nunchuk.android.core.util.orUnknownError
import com.nunchuk.android.domain.di.IoDispatcher
import com.nunchuk.android.utils.ChecksumUtil
import com.nunchuk.android.model.MasterSigner
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class KeyRecoveryBackupDownloadViewModel @Inject constructor(
    private val importTapsignerMasterSignerContentUseCase: ImportTapsignerMasterSignerContentUseCase,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
    private val masterSignerMapper: MasterSignerMapper,
    private val markRecoverStatusUseCase: MarkRecoverStatusUseCase,
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
            val resultImport = importTapsignerMasterSignerContentUseCase(
                ImportTapsignerMasterSignerContentUseCase.Param(
                    backupData,
                    stateValue.password,
                    stateValue.keyName
                )
            )
            if (resultImport.isSuccess) {
                markRecoverStatusSuccess(resultImport.getOrThrow())
            } else {
                _event.emit(BackupDownloadEvent.ProcessFailure(resultImport.exceptionOrNull()?.message.orUnknownError()))
            }
        }
    }

    private fun markRecoverStatusSuccess(masterSigner: MasterSigner) {
        viewModelScope.launch {
            markRecoverStatusUseCase(
                MarkRecoverStatusUseCase.Param(
                    xfp = args.backupKey.keyXfp,
                    status = "SUCCESS"
                )
            ).onSuccess {
                _event.emit(
                    BackupDownloadEvent.ImportTapsignerSuccess(
                        masterSignerMapper(
                            masterSigner
                        )
                    )
                )
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