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
import com.nunchuk.android.core.domain.ImportTapsignerMasterSignerContentUseCase
import com.nunchuk.android.core.domain.membership.GetInheritanceClaimStateUseCase
import com.nunchuk.android.core.domain.membership.InheritanceClaimDownloadBackupUseCase
import com.nunchuk.android.core.mapper.MasterSignerMapper
import com.nunchuk.android.core.network.NunchukApiException
import com.nunchuk.android.core.signer.SignerModel
import com.nunchuk.android.core.util.lastWord
import com.nunchuk.android.core.util.orUnknownError
import com.nunchuk.android.core.util.replaceLastWord
import com.nunchuk.android.main.util.ChecksumUtil
import com.nunchuk.android.model.Result
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
    private val importTapsignerMasterSignerContentUseCase: ImportTapsignerMasterSignerContentUseCase,
    private val masterSignerMapper: MasterSignerMapper,
    private val getInheritanceClaimStateUseCase: GetInheritanceClaimStateUseCase,
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
        if (stateValue.magicalPhrase.isBlank() || stateValue.backupPassword.isBlank()) return@launch
        _event.emit(InheritanceClaimInputEvent.Loading(true))
        val result = inheritanceClaimDownloadBackupUseCase(stateValue.magicalPhrase)
        _event.emit(InheritanceClaimInputEvent.Loading(false))
        if (result.isSuccess) {
            val backupKey = result.getOrThrow()
            val backupData = Base64.decode(backupKey.keyBackUpBase64, Base64.DEFAULT)
            if (ChecksumUtil.verifyChecksum(backupData, backupKey.keyCheckSum)) {
                val resultImport = importTapsignerMasterSignerContentUseCase(
                    ImportTapsignerMasterSignerContentUseCase.Param(
                        backupData,
                        stateValue.backupPassword,
                        INHERITED_KEY_NAME
                    )
                )
                if (resultImport.isSuccess) {
                    getStatus(
                        masterSignerMapper(resultImport.getOrThrow()),
                        stateValue.magicalPhrase,
                        result.getOrThrow().derivationPath
                    )
                } else {
                    _event.emit(InheritanceClaimInputEvent.Error(resultImport.exceptionOrNull()?.message.orUnknownError()))
                }
            }
        } else {
            val exception = result.exceptionOrNull()
            if (exception is NunchukApiException) {
                when (exception.code) {
                    400 -> _event.emit(InheritanceClaimInputEvent.SubscriptionExpired)
                    803 -> _event.emit(InheritanceClaimInputEvent.InActivated(result.exceptionOrNull()?.message.orUnknownError()))
                    else -> _event.emit(InheritanceClaimInputEvent.Error(result.exceptionOrNull()?.message.orUnknownError()))
                }
            } else {
                _event.emit(InheritanceClaimInputEvent.Error(result.exceptionOrNull()?.message.orUnknownError()))
            }
        }
    }

    private fun getStatus(signer: SignerModel, magic: String, derivationPath: String) =
        viewModelScope.launch {
            _event.emit(InheritanceClaimInputEvent.Loading(true))
            val result = getInheritanceClaimStateUseCase(
                GetInheritanceClaimStateUseCase.Param(
                    signer = signer,
                    magic = magic,
                    derivationPath = derivationPath
                )
            )
            _event.emit(InheritanceClaimInputEvent.Loading(false))
            if (result.isSuccess) {
                _event.emit(
                    InheritanceClaimInputEvent.GetInheritanceStatusSuccess(
                        inheritanceAdditional = result.getOrThrow(),
                        signer = signer,
                        magic = magic,
                        derivationPath = derivationPath
                    )
                )
            } else {
                _event.emit(InheritanceClaimInputEvent.Error(result.exceptionOrNull()?.message.orUnknownError()))
            }
        }

    fun updateBackupPassword(password: String) {
        _state.update { it.copy(backupPassword = password) }
    }

    fun handleInputEvent(mnemonic: String) {
        val withoutSpace = mnemonic.trim()
        if (withoutSpace != _state.value.magicalPhrase) {
            _state.update { it.copy(magicalPhrase = withoutSpace) }
            val word = withoutSpace.lastWord()
            if (word.isNotEmpty()) {
                filter(word)
            }
        } else {
            _state.update { it.copy(magicalPhrase = mnemonic) }
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
        _state.update { it.copy(magicalPhrase = "$updatedMnemonic ") }
    }

    companion object {
        private const val INHERITED_KEY_NAME = "Inherited key"
    }
}