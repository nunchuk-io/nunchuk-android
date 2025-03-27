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

package com.nunchuk.android.signer.software.components.recover

import androidx.lifecycle.viewModelScope
import com.nunchuk.android.arch.vm.NunchukViewModel
import com.nunchuk.android.core.constants.NativeErrorCode
import com.nunchuk.android.core.util.countWords
import com.nunchuk.android.core.util.lastWord
import com.nunchuk.android.core.util.nativeErrorCode
import com.nunchuk.android.core.util.replaceLastWord
import com.nunchuk.android.model.Result.Success
import com.nunchuk.android.signer.software.components.recover.RecoverSeedEvent.CanGoNextStepEvent
import com.nunchuk.android.signer.software.components.recover.RecoverSeedEvent.InvalidMnemonicEvent
import com.nunchuk.android.signer.software.components.recover.RecoverSeedEvent.MnemonicRequiredEvent
import com.nunchuk.android.signer.software.components.recover.RecoverSeedEvent.UpdateMnemonicEvent
import com.nunchuk.android.signer.software.components.recover.RecoverSeedEvent.ValidMnemonicEvent
import com.nunchuk.android.type.SignerType
import com.nunchuk.android.usecase.CheckMnemonicUseCase
import com.nunchuk.android.usecase.GetBip39WordListUseCase
import com.nunchuk.android.usecase.GetMasterFingerprintUseCase
import com.nunchuk.android.usecase.byzantine.GetReplaceSignerNameUseCase
import com.nunchuk.android.usecase.wallet.RecoverHotWalletUseCase
import com.nunchuk.android.utils.onException
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
internal class RecoverSeedViewModel @Inject constructor(
    private val getBip39WordListUseCase: GetBip39WordListUseCase,
    private val checkMnemonicUseCase: CheckMnemonicUseCase,
    private val recoverHotWalletUseCase: RecoverHotWalletUseCase,
    private val getMasterFingerprintUseCase: GetMasterFingerprintUseCase,
    private val getReplaceSignerNameUseCase: GetReplaceSignerNameUseCase,
) : NunchukViewModel<RecoverSeedState, RecoverSeedEvent>() {

    private val bip39Words = mutableListOf<String>()
    override val initialState = RecoverSeedState()

    init {
        viewModelScope.launch {
            val result = getBip39WordListUseCase.execute()
            if (result is Success) {
                bip39Words.addAll(result.data)
            }
        }
    }

    fun getReplaceSignerName(walletId: String) {
        viewModelScope.launch {
            getReplaceSignerNameUseCase(
                GetReplaceSignerNameUseCase.Params(
                    walletId = walletId,
                    signerType = SignerType.SOFTWARE
                )
            ).onSuccess { name ->
                updateState { copy(replaceSignerName = name) }
            }
        }
    }

    fun handleInputEvent(mnemonic: String) {
        val withoutSpace = mnemonic.trim()
        if (withoutSpace != getState().mnemonic) {
            updateState { copy(mnemonic = withoutSpace) }
            val word = withoutSpace.lastWord()
            if (word.isNotEmpty()) {
                filter(word)
            } else {
                updateState { copy(suggestions = emptyList()) }
            }
            val canGoNext =
                withoutSpace.countWords() in MIN_ACCEPTED_NUM_WORDS..MAX_ACCEPTED_NUM_WORDS
            event(CanGoNextStepEvent(canGoNext))
        }
    }

    private fun filter(word: String) {
        val filteredWords = bip39Words.filter { it.startsWith(word) }
        updateState { copy(suggestions = filteredWords) }
    }

    fun handleContinueEvent(isHotWalletRecovery: Boolean) {
        val mnemonic = getState().mnemonic
        if (mnemonic.isEmpty()) {
            event(MnemonicRequiredEvent)
        } else if (isHotWalletRecovery) {
            recoverHotWallet(false)
        } else {
            checkMnemonic(mnemonic)
        }
    }

    fun handleSelectWord(word: String) {
        updateState { copy(suggestions = bip39Words) }
        val updatedMnemonic = getState().mnemonic.replaceLastWord(word)
        updateState { copy(mnemonic = updatedMnemonic) }
        val canGoNext =
            updatedMnemonic.countWords() in MIN_ACCEPTED_NUM_WORDS..MAX_ACCEPTED_NUM_WORDS
        event(CanGoNextStepEvent(canGoNext))
        event(UpdateMnemonicEvent(updatedMnemonic))
    }

    fun recoverHotWallet(replace: Boolean) {
        viewModelScope.launch {
            recoverHotWalletUseCase(RecoverHotWalletUseCase.Params(getState().mnemonic, replace))
                .onSuccess {
                    setEvent(RecoverSeedEvent.RecoverHotWalletSuccess(it.id))
                }.onFailure {
                    val errorCode = it.nativeErrorCode()
                    if (errorCode == NativeErrorCode.SIGNER_EXISTS) {
                        getMasterFingerprintUseCase(
                            GetMasterFingerprintUseCase.Param(
                                mnemonic = getState().mnemonic,
                                passphrase = ""
                            )
                        ).onSuccess {
                            setEvent(RecoverSeedEvent.ExistingSignerEvent(it.orEmpty()))
                        }
                    } else {
                        setEvent(InvalidMnemonicEvent)
                    }
                }
        }
    }

    private fun checkMnemonic(mnemonic: String) {
        checkMnemonicUseCase.execute(mnemonic)
            .flowOn(Dispatchers.IO)
            .onException { event(InvalidMnemonicEvent) }
            .onEach { event(ValidMnemonicEvent(mnemonic)) }
            .flowOn(Dispatchers.Main)
            .launchIn(viewModelScope)
    }

    companion object {
        private const val MAX_ACCEPTED_NUM_WORDS = 24
        private const val MIN_ACCEPTED_NUM_WORDS = 12
    }

}
