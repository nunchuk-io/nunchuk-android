/**************************************************************************
 * This file is part of the Nunchuk software (https://nunchuk.io/)        *							          *
 * Copyright (C) 2022 Nunchuk								              *
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