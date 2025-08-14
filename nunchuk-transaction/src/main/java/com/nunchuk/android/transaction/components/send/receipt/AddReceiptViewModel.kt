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

import androidx.lifecycle.viewModelScope
import com.nunchuk.android.arch.vm.NunchukViewModel
import com.nunchuk.android.core.domain.utils.ParseSignerStringUseCase
import com.nunchuk.android.core.mapper.SingleSignerMapper
import com.nunchuk.android.core.signer.SignerModel
import com.nunchuk.android.core.util.MusigKeyPrefix
import com.nunchuk.android.core.util.isValueKeySetDisable
import com.nunchuk.android.core.util.orUnknownError
import com.nunchuk.android.model.ScriptNode
import com.nunchuk.android.model.Wallet
import com.nunchuk.android.transaction.components.send.receipt.AddReceiptEvent.AcceptedAddressEvent
import com.nunchuk.android.transaction.components.send.receipt.AddReceiptEvent.AddressRequiredEvent
import com.nunchuk.android.transaction.components.send.receipt.AddReceiptEvent.InvalidAddressEvent
import com.nunchuk.android.transaction.components.send.receipt.AddReceiptEvent.ParseBtcUriEvent
import com.nunchuk.android.transaction.components.send.receipt.AddReceiptEvent.ShowError
import com.nunchuk.android.transaction.components.utils.privateNote
import com.nunchuk.android.type.AddressType
import com.nunchuk.android.type.WalletTemplate
import com.nunchuk.android.usecase.CheckAddressValidUseCase
import com.nunchuk.android.usecase.GetChainTipUseCase
import com.nunchuk.android.usecase.GetDefaultAntiFeeSnipingUseCase
import com.nunchuk.android.usecase.GetScriptNodeFromMiniscriptTemplateUseCase
import com.nunchuk.android.usecase.ParseBtcUriUseCase
import com.nunchuk.android.usecase.wallet.GetUnusedWalletAddressUseCase
import com.nunchuk.android.usecase.wallet.GetWalletDetail2UseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
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
    private val getChainTipUseCase: GetChainTipUseCase
) : NunchukViewModel<AddReceiptState, AddReceiptEvent>() {

    override val initialState = AddReceiptState()

    fun init(args: AddReceiptArgs) {
        updateState { initialState.copy(address = args.address, privateNote = args.privateNote) }
        if (args.walletId.isNotEmpty()) getWalletDetail(args.walletId)

        viewModelScope.launch {
            getDefaultAntiFeeSnipingUseCase(Unit)
                .collect { result ->
                    if (result.isSuccess) {
                        updateState { copy(antiFeeSniping = result.getOrThrow()) }
                    }
                }
        }

        viewModelScope.launch {
            while (true) {
                getChainTipUseCase(Unit).onSuccess { chainTip ->
                    updateState { copy(currentBlockHeight = chainTip) }
                }
                delay(60000) // Refresh every minute
            }
        }
    }

    private fun getWalletDetail(walletId: String) {
        viewModelScope.launch {
            getWalletDetail2UseCase(walletId).onSuccess { wallet ->
                updateState {
                    copy(
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
                    updateState {
                        copy(
                            signers = signers,
                        )
                    }
                }
            }.onFailure {
                setEvent(ShowError(it.message.orUnknownError()))
            }
        }
    }

    private suspend fun getMiniscriptInfo(wallet: Wallet) {
        getScriptNodeFromMiniscriptTemplateUseCase(wallet.miniscript).onSuccess { result ->
            val signers = parseSignersFromScriptNode(result.scriptNode)
            if (wallet.addressType == AddressType.TAPROOT && wallet.walletTemplate != WalletTemplate.DISABLE_KEY_PATH && wallet.totalRequireSigns > 1) {
                val muSigSignerMap = wallet.signers.take(wallet.totalRequireSigns).mapIndexed { index, signer ->
                    "$MusigKeyPrefix$index" to singleSignerMapper(signer)
                }.toMap()
                updateState {
                    copy(
                        scriptNode = result.scriptNode,
                        signers = signers + muSigSignerMap,
                    )
                }
            } else {
                updateState { copy(scriptNode = result.scriptNode, signers = signers) }
            }
        }
    }

    private suspend fun parseSignersFromScriptNode(node: ScriptNode): Map<String, SignerModel> {
        val signerMap = mutableMapOf<String, SignerModel>()
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
            val result = parseBtcUriUseCase(content)
            if (result.isSuccess) {
                val btcUri = result.getOrThrow()
                updateState {
                    copy(
                        address = btcUri.address,
                        privateNote = btcUri.privateNote,
                        amount = btcUri.amount
                    )
                }
                setEvent(ParseBtcUriEvent)
            } else {
                setEvent(ShowError(result.exceptionOrNull()?.message.orUnknownError()))
            }
        }
    }

    fun handleContinueEvent(isCreateTransaction: Boolean) {
        viewModelScope.launch {
            val currentState = getState()
            val address = currentState.address
            when {
                address.isEmpty() -> event(AddressRequiredEvent)
                else -> {
                    val result =
                        checkAddressValidUseCase(CheckAddressValidUseCase.Params(listOf(address)))
                    if (result.isSuccess && result.getOrThrow().isEmpty()) {
                        setEvent(
                            AcceptedAddressEvent(
                                isCreateTransaction = isCreateTransaction,
                                isMiniscript = currentState.scriptNode != null
                            )
                        )
                    } else {
                        setEvent(InvalidAddressEvent)
                    }
                }
            }
        }
    }

    fun updateAddress(address: String) {
        updateState { copy(address = address) }
    }

    fun getFirstUnusedAddress(walletId: String) {
        viewModelScope.launch {
            getUnusedWalletAddressUseCase(walletId).onSuccess { addresses ->
                updateState { copy(address = addresses.first()) }
            }.onFailure {
                setEvent(ShowError(it.message.orUnknownError()))
            }
        }
    }

    fun handleReceiptChanged(address: String) {
        updateState { copy(address = address) }
    }

    fun handlePrivateNoteChanged(privateNote: String) {
        updateState { copy(privateNote = privateNote) }
    }

    fun getAddReceiptState() = getState()
    fun setEventHandled() {
        setEvent(AddReceiptEvent.NoOp)
    }
}