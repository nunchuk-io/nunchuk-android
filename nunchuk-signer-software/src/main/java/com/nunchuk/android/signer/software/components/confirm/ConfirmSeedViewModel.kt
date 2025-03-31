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

package com.nunchuk.android.signer.software.components.confirm

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nunchuk.android.core.push.PushEvent
import com.nunchuk.android.core.push.PushEventManager
import com.nunchuk.android.signer.software.components.confirm.ConfirmSeedEvent.ConfirmSeedCompletedEvent
import com.nunchuk.android.signer.software.components.confirm.ConfirmSeedEvent.SelectedIncorrectWordEvent
import com.nunchuk.android.signer.software.components.create.PHRASE_SEPARATOR
import com.nunchuk.android.type.SignerType
import com.nunchuk.android.usecase.MarkHotKeyBackedUpUseCase
import com.nunchuk.android.usecase.byzantine.GetReplaceSignerNameUseCase
import com.nunchuk.android.usecase.wallet.MarkHotWalletBackedUpUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
internal class ConfirmSeedViewModel @Inject constructor(
    private val markHotWalletBackedUpUseCase: MarkHotWalletBackedUpUseCase,
    private val coroutineScope: CoroutineScope,
    private val getReplaceSignerNameUseCase: GetReplaceSignerNameUseCase,
    private val markHotKeyBackedUpUseCase: MarkHotKeyBackedUpUseCase,
    private val pushEventManager: PushEventManager,
    savedStateHandle: SavedStateHandle
) : ViewModel() {
    private val _event = MutableSharedFlow<ConfirmSeedEvent>()
    private val _state = MutableStateFlow(ConfirmSeedState())
    val event = _event.asSharedFlow()
    val state = _state.asStateFlow()

    private val args = ConfirmSeedFragmentArgs.fromSavedStateHandle(savedStateHandle)

    private val mnemonic: String = args.mnemonic
    private val phrases: List<String> = mnemonic.split(PHRASE_SEPARATOR)

    init {
        _state.value = _state.value.copy(groups = phrases.random3LastPhraseWords())
        if (args.replacedXfp.isNotEmpty()) {
            viewModelScope.launch {
                getReplaceSignerNameUseCase(
                    GetReplaceSignerNameUseCase.Params(
                        walletId = args.walletId,
                        signerType = SignerType.SOFTWARE
                    )
                ).onSuccess { name ->
                    _state.update { it.copy(replaceSignerName = name) }
                }
            }
        }
    }

    fun handleContinueEvent() {
        viewModelScope.launch {
            val isAllSelectedCorrect = _state.value.groups.all { it.isCorrectSelected() }
            if (isAllSelectedCorrect) {
                if (args.backupHotKeySignerId.isNotEmpty()) {
                    pushEventManager.push(PushEvent.SignedChanged(args.backupHotKeySignerId))
                }
                _event.emit(ConfirmSeedCompletedEvent)
            } else {
                _event.emit(SelectedIncorrectWordEvent)
            }
        }
    }

    fun updatePhraseWordGroup(phraseWordGroup: PhraseWordGroup) {
        val groups = ArrayList(_state.value.groups)
        groups.forEachIndexed { index, group ->
            if (group.index == phraseWordGroup.index) {
                groups[index] = phraseWordGroup
                return@forEachIndexed
            }
        }
        _state.value = _state.value.copy(groups = groups)
    }

    fun markHotWalletBackedUp(walletId: String) {
        coroutineScope.launch {
            markHotWalletBackedUpUseCase(walletId)
        }
    }

    fun markHotKeyBackedUp() {
        coroutineScope.launch {
            markHotKeyBackedUpUseCase(args.backupHotKeySignerId)
        }
    }

    private fun PhraseWordGroup.isCorrectSelected() = (firstWord.selected && firstWord.correct) ||
            (secondWord.selected && secondWord.correct) ||
            (thirdWord.selected && thirdWord.correct)

}