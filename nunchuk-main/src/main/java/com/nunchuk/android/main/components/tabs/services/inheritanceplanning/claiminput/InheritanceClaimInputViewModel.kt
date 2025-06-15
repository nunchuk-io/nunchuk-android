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
import com.nunchuk.android.core.util.lastWord
import com.nunchuk.android.core.util.orUnknownError
import com.nunchuk.android.core.util.replaceLastWord
import com.nunchuk.android.utils.ChecksumUtil
import com.nunchuk.android.model.BackupKey
import com.nunchuk.android.model.MasterSigner
import com.nunchuk.android.model.Result
import com.nunchuk.android.usecase.DeleteMasterSignerUseCase
import com.nunchuk.android.usecase.GetBip39WordListUseCase
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
    private val getBip39WordListUseCase: GetBip39WordListUseCase,
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

    private var bip39Words = ArrayList<String>()

    init {
        viewModelScope.launch {
            val result = getBip39WordListUseCase.execute()
            if (result is Result.Success) {
                bip39Words = ArrayList(result.data)
            }
        }
    }

    fun downloadBackupKey() = viewModelScope.launch {
        val stateValue = _state.value
        if (stateValue.magicalPhrase.isBlank()) return@launch
        _event.emit(InheritanceClaimInputEvent.Loading(true))
        val result = inheritanceClaimDownloadBackupUseCase(InheritanceClaimDownloadBackupUseCase.Param(stateValue.magicalPhrase, stateValue.backupPasswords))
        _event.emit(InheritanceClaimInputEvent.Loading(false))
        if (result.isSuccess) {
            val backupKeys = result.getOrThrow()
            if (backupKeys.size != stateValue.backupPasswords.size) return@launch
            val importMasterSigners = ArrayList<MasterSigner>()
            var errorMsg: String? = null
            backupKeys.forEachIndexed { index, backupKey ->
                val backupData = Base64.decode(backupKey.keyBackUpBase64, Base64.DEFAULT)
                if (ChecksumUtil.verifyChecksum(backupData, backupKey.keyCheckSum).not()) return@launch
                stateValue.backupPasswords.forEach { backupPassword ->
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
                        errorMsg = resultImport.exceptionOrNull()?.message.orUnknownError()
                    }
                }
            }
            if (importMasterSigners.size != stateValue.backupPasswords.size) {
                importMasterSigners.forEach {
                    deleteMasterSignerUseCase(it.id)
                }
                _event.emit(InheritanceClaimInputEvent.Error(errorMsg ?: "Error importing backup keys"))
                return@launch
            }
            getStatus(importMasterSigners, stateValue.magicalPhrase, backupKeys)
        } else {
            val exception = result.exceptionOrNull()
            if (exception is NunchukApiException) {
                when (exception.code) {
                    400 -> _event.emit(InheritanceClaimInputEvent.SubscriptionExpired)
                    803 -> _event.emit(InheritanceClaimInputEvent.InActivated(result.exceptionOrNull()?.message.orUnknownError()))
                    829 -> _event.emit(InheritanceClaimInputEvent.PleaseComeLater(result.exceptionOrNull()?.message.orUnknownError()))
                    830 -> _event.emit(InheritanceClaimInputEvent.SecurityDepositRequired(result.exceptionOrNull()?.message.orUnknownError()))
                    else -> _event.emit(InheritanceClaimInputEvent.Error(result.exceptionOrNull()?.message.orUnknownError()))
                }
            } else {
                _event.emit(InheritanceClaimInputEvent.Error(result.exceptionOrNull()?.message.orUnknownError()))
            }
        }
    }

    private fun getStatus(masterSigners: List<MasterSigner>, magic: String, backupKeys: List<BackupKey>) =
        viewModelScope.launch {
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
        val updatedPasswords = _state.value._backupPasswords.toMutableList()
        updatedPasswords[index] = password
        _state.update { it.copy(_backupPasswords = updatedPasswords) }
    }

    fun handleInputEvent(mnemonic: String) {
        if (mnemonic != _state.value.magicalPhrase) {
            _state.update { it.copy(_magicalPhrase = mnemonic) }
            val word = mnemonic.lastWord()
            if (word.isNotEmpty()) {
                filter(word)
            }
        } else {
            _state.update { it.copy(_magicalPhrase = mnemonic) }
        }
    }

    private fun filter(word: String) {
        val filteredWords =
            if (word.isNotBlank()) bip39Words.filter { it.startsWith(word) } else emptyList()
        _state.update { it.copy(suggestions = filteredWords) }
    }

    fun handleSelectWord(word: String) {
        _state.update { it.copy(suggestions = bip39Words) }
        val updatedMnemonic = _state.value.magicalPhrase.replaceLastWord(word)
        _state.update { it.copy(_magicalPhrase = "$updatedMnemonic ") }
    }

    companion object {
        private const val INHERITED_KEY_NAME = "Inherited key"
    }
}