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

package com.nunchuk.android.transaction.components.receive.address.unused

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nunchuk.android.core.domain.utils.GetTrezorAddressDeeplinkUseCase
import com.nunchuk.android.core.domain.utils.ParseTrezorAddressResponseUseCase
import com.nunchuk.android.core.push.PushEvent
import com.nunchuk.android.core.push.PushEventManager
import com.nunchuk.android.core.util.TrezorCallbackMethod
import com.nunchuk.android.core.util.orUnknownError
import com.nunchuk.android.core.util.parseTrezorCallback
import com.nunchuk.android.model.Wallet
import com.nunchuk.android.type.WalletType
import com.nunchuk.android.type.SignerTag
import com.nunchuk.android.usecase.GetAddressPathUseCase
import com.nunchuk.android.usecase.GetAddressesUseCase
import com.nunchuk.android.usecase.GetWalletUseCase
import com.nunchuk.android.usecase.MarkAddressAsUsedUseCase
import com.nunchuk.android.usecase.NewAddressUseCase
import com.nunchuk.android.usecase.coin.IsMyCoinUseCase
import com.nunchuk.android.usecase.wallet.GetAddressIndexUseCase
import com.nunchuk.android.utils.onException
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
internal class UnusedAddressViewModel @Inject constructor(
    private val addressesUseCase: GetAddressesUseCase,
    private val newAddressUseCase: NewAddressUseCase,
    private val getAddressPathUseCase: GetAddressPathUseCase,
    private val getTrezorAddressDeeplinkUseCase: GetTrezorAddressDeeplinkUseCase,
    private val parseTrezorAddressResponseUseCase: ParseTrezorAddressResponseUseCase,
    private val getWalletUseCase: GetWalletUseCase,
    private val getAddressIndexUseCase: GetAddressIndexUseCase,
    private val isMyCoinUseCase: IsMyCoinUseCase,
    private val markAddressAsUsedUseCase: MarkAddressAsUsedUseCase,
    private val pushEventManager: PushEventManager,
) : ViewModel() {
    private val _state = MutableStateFlow(UnusedAddressState())
    val state: StateFlow<UnusedAddressState> = _state.asStateFlow()

    private val _event = MutableSharedFlow<UnusedAddressEvent>()
    val event: SharedFlow<UnusedAddressEvent> = _event.asSharedFlow()

    private lateinit var walletId: String
    private var lastHandledTrezorCallback: String = ""

    fun init(walletId: String) {
        this.walletId = walletId
        getUnusedAddresses()
        getWallet()
    }

    fun isLiquidWallet(): Boolean = _state.value.wallet.walletType == WalletType.LIQUID

    private fun getWallet() {
        viewModelScope.launch {
            getWalletUseCase.execute(walletId)
                .onException { }
                .collect { _state.update { state -> state.copy(wallet = it.wallet) } }
        }
    }

    fun getAddressPath(address: String) {
        viewModelScope.launch {
            getAddressPathUseCase(GetAddressPathUseCase.Params(walletId, address))
                .onSuccess {
                    _event.emit(UnusedAddressEvent.GetAddressPathSuccessEvent(it))
                }
        }
    }

    fun isSingleSignWallet(): Boolean {
        val wallet = _state.value.wallet
        val requireSigns = wallet.totalRequireSigns
        val totalSigns = wallet.signers.size
        return requireSigns == 1 && totalSigns == 1
    }

    fun isTrezorWallet(): Boolean {
        return _state.value.wallet.signers.any { signer ->
            signer.tags.contains(SignerTag.TREZOR)
        }
    }

    fun requestVerifyAddressByTrezor(address: String) {
        val wallet = _state.value.wallet
        if (wallet.id.isBlank() || address.isBlank()) return
        val trezorSigner = wallet.signers.firstOrNull { signer ->
            signer.tags.contains(SignerTag.TREZOR)
        }
        if (trezorSigner == null) return

        viewModelScope.launch {
            getAddressPathUseCase(
                GetAddressPathUseCase.Params(
                    walletId = walletId,
                    address = address,
                    signer = trezorSigner
                )
            ).onSuccess { path ->
                getTrezorAddressDeeplinkUseCase(
                    GetTrezorAddressDeeplinkUseCase.Param(
                        wallet = wallet,
                        address = address,
                        path = path
                    )
                ).onSuccess { deeplink ->
                    _event.emit(UnusedAddressEvent.ShowOpenTrezorSuiteConfirmationEvent(deeplink))
                }.onFailure {
                    _event.emit(UnusedAddressEvent.VerifyAddressErrorEvent(it.message.orUnknownError()))
                }
            }.onFailure {
                _event.emit(UnusedAddressEvent.VerifyAddressErrorEvent(it.message.orUnknownError()))
            }
        }
    }

    fun handleTrezorCallback(callbackUri: String?): Boolean {
        if (callbackUri.isNullOrBlank()) return false
        val callback = parseTrezorCallback(callbackUri) ?: return false
        if (callback.method != TrezorCallbackMethod.GET_ADDRESS) return false
        if (lastHandledTrezorCallback == callback.rawUri) return true
        lastHandledTrezorCallback = callback.rawUri
        if (callback.response.isBlank()) return true

        val resolvedWalletId = callback.walletId.ifBlank { walletId }
        if (resolvedWalletId != walletId) return true
        viewModelScope.launch {
            parseTrezorAddressResponseUseCase(callback.response).onSuccess { address ->
                isMyCoinUseCase(
                    IsMyCoinUseCase.Param(
                        walletId = walletId,
                        address = address
                    )
                ).onSuccess { isMyAddress ->
                    if (isMyAddress) {
                        _event.emit(UnusedAddressEvent.VerifyAddressSuccessEvent)
                    } else {
                        _event.emit(UnusedAddressEvent.VerifyAddressErrorEvent("Address verification failed"))
                    }
                }.onFailure {
                    _event.emit(UnusedAddressEvent.VerifyAddressErrorEvent(it.message.orUnknownError()))
                }
            }.onFailure {
                _event.emit(UnusedAddressEvent.VerifyAddressErrorEvent(it.message.orUnknownError()))
            }
        }
        return true
    }

    private fun getUnusedAddresses() {
        viewModelScope.launch {
            addressesUseCase.execute(walletId = walletId)
                .onException { _state.update { it.copy(addresses = emptyList()) } }
                .collect { onSuccess(it) }
        }
    }

    fun markAddressAsUsed(address: String) {
        viewModelScope.launch {
            markAddressAsUsedUseCase(MarkAddressAsUsedUseCase.Params(walletId, address))
                .onSuccess {
                    _state.update { state ->
                        state.copy(addresses = state.addresses.filter { it != address })
                    }
                    _event.emit(UnusedAddressEvent.MarkAddressAsUsedSuccessEvent(address))
                    pushEventManager.push(PushEvent.ReloadUsedAddress(address))
                }.onFailure { }
        }
    }

    private fun onSuccess(addresses: List<String>) {
        _state.update { it.copy(addresses = addresses) }
        if (addresses.isEmpty()) {
            generateAddress()
        }
    }

    fun generateAddress() {
        viewModelScope.launch {
            newAddressUseCase.execute(walletId = walletId)
                .onException { _event.emit(UnusedAddressEvent.GenerateAddressErrorEvent(it.message.orEmpty())) }
                .collect { getUnusedAddresses() }
        }
    }

    suspend fun getAddressIndex(address: String): Int {
        return getAddressIndexUseCase(GetAddressIndexUseCase.Params(walletId, address)).getOrThrow()
    }
}
