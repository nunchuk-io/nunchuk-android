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

package com.nunchuk.android.transaction.components.send.receipt

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nunchuk.android.core.domain.utils.ParseSignerStringUseCase
import com.nunchuk.android.core.mapper.SingleSignerMapper
import com.nunchuk.android.core.miniscript.ScriptNodeType
import com.nunchuk.android.core.signer.SignerModel
import com.nunchuk.android.core.util.MusigKeyPrefix
import com.nunchuk.android.core.util.isValueKeySetDisable
import com.nunchuk.android.core.util.orUnknownError
import com.nunchuk.android.model.ScriptNode
import com.nunchuk.android.model.Wallet
import com.nunchuk.android.nav.args.AddReceiptArgs
import com.nunchuk.android.transaction.components.send.receipt.AddReceiptEvent.AcceptedAddressEvent
import com.nunchuk.android.transaction.components.send.receipt.AddReceiptEvent.AddressRequiredEvent
import com.nunchuk.android.transaction.components.send.receipt.AddReceiptEvent.InvalidAddressEvent
import com.nunchuk.android.transaction.components.send.receipt.AddReceiptEvent.ParseBtcUriEvent
import com.nunchuk.android.transaction.components.send.receipt.AddReceiptEvent.ShowError
import com.nunchuk.android.transaction.components.utils.privateNote
import com.nunchuk.android.type.AddressType
import com.nunchuk.android.type.WalletTemplate
import com.nunchuk.android.usecase.CheckAddressValidUseCase
import com.nunchuk.android.usecase.GetDefaultAntiFeeSnipingUseCase
import com.nunchuk.android.usecase.GetScriptNodeFromMiniscriptTemplateUseCase
import com.nunchuk.android.usecase.ParseBtcUriUseCase
import com.nunchuk.android.usecase.wallet.GetUnusedWalletAddressUseCase
import com.nunchuk.android.usecase.wallet.GetWalletDetail2UseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
internal class AddReceiptViewModel @Inject constructor(
    private val checkAddressValidUseCase: CheckAddressValidUseCase,
    private val parseBtcUriUseCase: ParseBtcUriUseCase,
    private val getUnusedWalletAddressUseCase: GetUnusedWalletAddressUseCase,
    private val getWalletDetail2UseCase: GetWalletDetail2UseCase,
    private val singleSignerMapper: SingleSignerMapper,
    private val getDefaultAntiFeeSnipingUseCase: GetDefaultAntiFeeSnipingUseCase,
    private val getScriptNodeFromMiniscriptTemplateUseCase: GetScriptNodeFromMiniscriptTemplateUseCase,
    private val parseSignerStringUseCase: ParseSignerStringUseCase,
) : ViewModel() {
    private val _subNodeFollowParents: MutableSet<List<Int>> = mutableSetOf()

    private val _state = MutableStateFlow(AddReceiptState())
    val state = _state.asStateFlow()

    private val _event = MutableSharedFlow<AddReceiptEvent>()
    val event = _event.asSharedFlow()

    fun init(args: AddReceiptArgs) {
        _state.update { AddReceiptState(address = args.address, privateNote = args.privateNote) }
        if (args.walletId.isNotEmpty()) getWalletDetail(args.walletId)

        viewModelScope.launch {
            getDefaultAntiFeeSnipingUseCase(Unit)
                .collect { result ->
                    if (result.isSuccess) {
                        _state.update { it.copy(antiFeeSniping = result.getOrThrow()) }
                    }
                }
        }
    }

    private fun getWalletDetail(walletId: String) {
        viewModelScope.launch {
            getWalletDetail2UseCase(walletId).onSuccess { wallet ->
                _state.update {
                    it.copy(
                        addressType = wallet.addressType,
                        isValueKeySetDisable = wallet.isValueKeySetDisable,
                        wallet = wallet
                    )
                }
                if (wallet.miniscript.isNotEmpty()) {
                    getMiniscriptInfo(wallet)
                } else {
                    val signers = wallet.signers.map { signer ->
                        singleSignerMapper(signer)
                    }.associateBy { it.fingerPrint }
                    _state.update {
                        it.copy(
                            signers = signers,
                        )
                    }
                }
            }.onFailure {
                _event.emit(ShowError(it.message.orUnknownError()))
            }
        }
    }

    private suspend fun getMiniscriptInfo(wallet: Wallet) {
        _subNodeFollowParents.clear()
        getScriptNodeFromMiniscriptTemplateUseCase(wallet.miniscript).onSuccess { result ->
            val signers = parseSignersFromScriptNode(result.scriptNode)
            if (wallet.addressType == AddressType.TAPROOT && wallet.walletTemplate != WalletTemplate.DISABLE_KEY_PATH && wallet.totalRequireSigns > 1) {
                val muSigSignerMap =
                    wallet.signers.take(wallet.totalRequireSigns).mapIndexed { index, signer ->
                        "$MusigKeyPrefix$index" to singleSignerMapper(signer)
                    }.toMap()
                _state.update {
                    it.copy(
                        subNodeFollowParents = _subNodeFollowParents,
                        scriptNode = result.scriptNode,
                        signers = signers + muSigSignerMap,
                    )
                }
            } else {
                _state.update { it.copy(scriptNode = result.scriptNode, signers = signers, subNodeFollowParents = _subNodeFollowParents) }
            }
        }
    }

    private suspend fun parseSignersFromScriptNode(node: ScriptNode): Map<String, SignerModel> {
        val signerMap = mutableMapOf<String, SignerModel>()
        if (node.type == ScriptNodeType.AND.name) {
            _subNodeFollowParents.addAll(node.subs.map { it.id })
        }
        node.keys.forEach { key ->
            parseSignerStringUseCase(key).getOrNull()?.let {
                singleSignerMapper(it)
            }?.let {
                signerMap[key] = it
            }
        }
        node.subs.forEach { subNode ->
            signerMap.putAll(parseSignersFromScriptNode(subNode))
        }
        return signerMap
    }

    fun parseBtcUri(content: String) {
        viewModelScope.launch {
            parseBtcUriUseCase(content)
                .onSuccess { btcUri ->
                    _state.update {
                        it.copy(
                            address = btcUri.address,
                            privateNote = btcUri.privateNote,
                            amount = btcUri.amount
                        )
                    }
                    _event.emit(ParseBtcUriEvent)
                }.onFailure {
                    _event.emit(ShowError(it.message.orUnknownError()))
                }
        }
    }

    fun parseBtcUriAndContinue(content: String) {
        viewModelScope.launch {
            _event.emit(AddReceiptEvent.Loading(true))
            parseBtcUriUseCase(content).onSuccess { btcUri ->
                _state.update { it.copy(address = btcUri.address) }
            }
            handleContinueEvent(true)
        }
    }

    fun handleContinueEvent(isCreateTransaction: Boolean) {
        viewModelScope.launch {
            _event.emit(AddReceiptEvent.Loading(true))
            val currentState = _state.value
            val address = currentState.address
            when {
                address.isEmpty() -> {
                    _event.emit(AddReceiptEvent.Loading(false))
                    _event.emit(AddressRequiredEvent)
                }
                else -> {
                    val result =
                        checkAddressValidUseCase(CheckAddressValidUseCase.Params(listOf(address)))
                    _event.emit(AddReceiptEvent.Loading(false))
                    if (result.isSuccess && result.getOrThrow().isEmpty()) {
                        _event.emit(
                            AcceptedAddressEvent(
                                isCreateTransaction = isCreateTransaction,
                                isMiniscript = currentState.scriptNode != null
                            )
                        )
                    } else {
                        _event.emit(InvalidAddressEvent)
                    }
                }
            }
        }
    }

    fun updateAddress(address: String) {
        _state.update { it.copy(address = address) }
    }

    fun getFirstUnusedAddress(walletId: String) {
        viewModelScope.launch {
            getUnusedWalletAddressUseCase(walletId).onSuccess { addresses ->
                _state.update { it.copy(address = addresses.first()) }
            }.onFailure {
                _event.emit(ShowError(it.message.orUnknownError()))
            }
        }
    }

    fun handleReceiptChanged(address: String) {
        _state.update { it.copy(address = address) }
    }

    fun handlePrivateNoteChanged(privateNote: String) {
        _state.update { it.copy(privateNote = privateNote) }
    }

    fun getAddReceiptState() = _state.value
}