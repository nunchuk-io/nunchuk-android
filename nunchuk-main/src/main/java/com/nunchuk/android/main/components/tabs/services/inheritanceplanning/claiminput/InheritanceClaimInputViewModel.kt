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

package com.nunchuk.android.main.components.tabs.services.inheritanceplanning.claiminput

import android.util.Base64
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nunchuk.android.core.domain.ImportBackupKeyContentUseCase
import com.nunchuk.android.core.domain.membership.GetInheritanceClaimStateUseCase
import com.nunchuk.android.core.domain.membership.InheritanceClaimDownloadBackupUseCase
import com.nunchuk.android.core.mapper.MasterSignerMapper
import com.nunchuk.android.core.network.NunchukApiException
import com.nunchuk.android.core.util.orUnknownError
import com.nunchuk.android.model.BackupKey
import com.nunchuk.android.model.MasterSigner
import com.nunchuk.android.usecase.DeleteMasterSignerUseCase
import com.nunchuk.android.utils.ChecksumUtil
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class InheritanceClaimInputViewModel @Inject constructor(
    private val inheritanceClaimDownloadBackupUseCase: InheritanceClaimDownloadBackupUseCase,
    private val importBackupKeyContentUseCase: ImportBackupKeyContentUseCase,
    private val masterSignerMapper: MasterSignerMapper,
    private val getInheritanceClaimStateUseCase: GetInheritanceClaimStateUseCase,
    private val deleteMasterSignerUseCase: DeleteMasterSignerUseCase
) : ViewModel() {

    private val _event = MutableSharedFlow<InheritanceClaimInputEvent>()
    val event = _event.asSharedFlow()

    private val _state = MutableStateFlow(InheritanceClaimInputState())
    val state = _state.asStateFlow()

    fun downloadBackupKey(passphrase: String) = viewModelScope.launch {
        val stateValue = _state.value
        _event.emit(InheritanceClaimInputEvent.Loading(true))
        val result = inheritanceClaimDownloadBackupUseCase(
            InheritanceClaimDownloadBackupUseCase.Param(
                passphrase,
                stateValue.formattedBackupPasswords
            )
        )
        _event.emit(InheritanceClaimInputEvent.Loading(false))
        if (result.isSuccess) {
            val backupKeys = result.getOrThrow()
            if (backupKeys.size != stateValue.formattedBackupPasswords.size) {
                _event.emit(InheritanceClaimInputEvent.Error("Invalid password. Unable to restore backup"))
                return@launch
            }
            val importMasterSigners = ArrayList<MasterSigner>()
            var errorMsg: String? = null
            backupKeys.forEachIndexed { index, backupKey ->
                val backupData = Base64.decode(backupKey.keyBackUpBase64, Base64.DEFAULT)
                if (ChecksumUtil.verifyChecksum(backupData, backupKey.keyCheckSum)
                        .not()
                ) return@launch
                stateValue.formattedBackupPasswords.forEach { backupPassword ->
                    val resultImport = importBackupKeyContentUseCase(
                        ImportBackupKeyContentUseCase.Param(
                            backupData,
                            backupPassword,
                            "$INHERITED_KEY_NAME #${index + 1}"
                        )
                    )
                    if (resultImport.isSuccess) {
                        importMasterSigners.add(resultImport.getOrThrow())
                    } else {
                        errorMsg = "Invalid password. Unable to restore backup"
                    }
                }
            }
            if (importMasterSigners.size != stateValue.formattedBackupPasswords.size) {
                importMasterSigners.forEach {
                    deleteMasterSignerUseCase(it.id)
                }
                _event.emit(
                    InheritanceClaimInputEvent.Error(
                        errorMsg ?: "Invalid password. Unable to restore backup"
                    )
                )
                return@launch
            }
            getStatus(importMasterSigners, passphrase, backupKeys)
        } else {
            val e = result.exceptionOrNull()
            if (e is NunchukApiException && e.code == 831) {
                _event.emit(InheritanceClaimInputEvent.NoInheritanceClaimFound)
            } else {
                _event.emit(
                    InheritanceClaimInputEvent.Error(
                        result.exceptionOrNull()?.message.orUnknownError()
                    )
                )
            }
        }
    }

    private fun getStatus(
        masterSigners: List<MasterSigner>,
        magic: String,
        backupKeys: List<BackupKey>
    ) = viewModelScope.launch {
        _event.emit(InheritanceClaimInputEvent.Loading(true))
        val signers = masterSigners.map { masterSignerMapper(it) }
        val derivationPaths = backupKeys.map { it.derivationPath }

        val result = getInheritanceClaimStateUseCase(
            GetInheritanceClaimStateUseCase.Param(
                signerModels = signers,
                magic = magic,
                derivationPaths = derivationPaths
            )
        )
        _event.emit(InheritanceClaimInputEvent.Loading(false))
        if (result.isSuccess) {
            _event.emit(
                InheritanceClaimInputEvent.GetInheritanceStatusSuccess(
                    inheritanceAdditional = result.getOrThrow(),
                    signers = signers,
                    magic = magic,
                    derivationPaths = derivationPaths
                )
            )
        } else {
            _event.emit(InheritanceClaimInputEvent.Error(result.exceptionOrNull()?.message.orUnknownError()))
        }
    }

    fun updateBackupPassword(password: String, index: Int) {
        val updatedPasswords = _state.value.backupPasswords.toMutableList()
        updatedPasswords[index] = password
        _state.update { it.copy(backupPasswords = updatedPasswords) }
    }

    companion object {
        const val INHERITED_KEY_NAME = "Inheritance key"
    }
}