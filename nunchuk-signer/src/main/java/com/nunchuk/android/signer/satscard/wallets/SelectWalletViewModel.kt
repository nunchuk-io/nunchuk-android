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

package com.nunchuk.android.signer.satscard.wallets

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nunchuk.android.core.domain.ParseWalletDescriptorUseCase
import com.nunchuk.android.core.domain.membership.InheritanceClaimCreateTransactionUseCase
import com.nunchuk.android.core.util.toAmount
import com.nunchuk.android.model.EstimateFeeRates
import com.nunchuk.android.model.Wallet
import com.nunchuk.android.model.defaultRate
import com.nunchuk.android.share.model.ExtendTransaction
import com.nunchuk.android.usecase.EstimateFeeUseCase
import com.nunchuk.android.usecase.GetAddressesUseCase
import com.nunchuk.android.usecase.GetWalletsUseCase
import com.nunchuk.android.usecase.NewAddressUseCase
import com.nunchuk.android.utils.onException
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SelectWalletViewModel @Inject constructor(
    private val getWalletsUseCase: GetWalletsUseCase,
    private val getAddressesUseCase: GetAddressesUseCase,
    private val newAddressUseCase: NewAddressUseCase,
    private val estimateFeeUseCase: EstimateFeeUseCase,
    private val inheritanceClaimCreateTransactionUseCase: InheritanceClaimCreateTransactionUseCase,
    private val parseWalletDescriptorUseCase: ParseWalletDescriptorUseCase,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val args = SelectWalletFragmentArgs.fromSavedStateHandle(savedStateHandle)

    private val _state = MutableStateFlow(SelectWalletState())
    private val _event = MutableSharedFlow<SelectWalletEvent>()

    val state = _state.asStateFlow()
    val event = _event.asSharedFlow()

    init {
        viewModelScope.launch {
            getWalletsUseCase.execute()
                .onStart { _event.emit(SelectWalletEvent.Loading(true)) }
                .onException {
                    _event.emit(SelectWalletEvent.Error(it))
                }
                .onCompletion { _event.emit(SelectWalletEvent.Loading(false)) }
                .collect { wallets ->
                    val claimWallet = args.claimParam?.bsms?.let { bsms ->
                        parseWalletDescriptorUseCase(bsms).getOrDefault(Wallet())
                    }
                    _state.value = _state.value.copy(selectWallets = wallets.filter { it.wallet.id != claimWallet?.id }.map {
                        SelectableWallet(
                            it.wallet,
                            it.isShared
                        )
                    })
                }
        }
    }

    fun setWalletSelected(walletId: String) {
        val selectWallets =
            _state.value.selectWallets.map { selectableWallet -> selectableWallet.copy(isSelected = selectableWallet.wallet.id == walletId) }
        _state.value = _state.value.copy(selectWallets = selectWallets, selectedWalletId = walletId)
    }

    fun getWalletAddress(isCreateTransaction: Boolean) {
        viewModelScope.launch {
            _event.emit(SelectWalletEvent.Loading(true))
            getAddressesUseCase.execute(walletId = selectedWalletId)
                .flatMapLatest {
                    if (it.isEmpty()) {
                        return@flatMapLatest newAddressUseCase.execute(walletId = selectedWalletId)
                            .map { newAddress -> listOf(newAddress) }
                    }
                    return@flatMapLatest flowOf(it)
                }.onException {
                    _event.emit(SelectWalletEvent.Loading(false))
                    _event.emit(SelectWalletEvent.Error(it))
                }
                .collect {
                    _event.emit(SelectWalletEvent.Loading(false))
                    _event.emit(
                        SelectWalletEvent.GetAddressSuccess(
                            it.first(),
                            isCreateTransaction
                        )
                    )
                    _state.value = _state.value.copy(selectWalletAddress = it.first())
                }
        }
    }

    fun getEstimateFeeRates() {
        viewModelScope.launch {
            _event.emit(SelectWalletEvent.Loading(true))
            val result = estimateFeeUseCase(Unit)
            _event.emit(SelectWalletEvent.Loading(false))
            if (result.isSuccess) {
                _state.value = _state.value.copy(feeRates = result.getOrThrow())
                _event.emit((SelectWalletEvent.GetFeeRateSuccess(result.getOrThrow())))
            } else {
                _event.emit(SelectWalletEvent.Error(result.exceptionOrNull()))
            }
        }
    }

    fun createInheritanceTransaction() = viewModelScope.launch {
        _event.emit(SelectWalletEvent.Loading(isLoading = true, isClaimInheritance = true))
        val claimInheritanceTxParam = args.claimParam
        val result = inheritanceClaimCreateTransactionUseCase(
            InheritanceClaimCreateTransactionUseCase.Param(
                address = _state.value.selectWalletAddress,
                feeRate = _state.value.feeRates.priorityRate.toAmount(),
                masterSignerIds = claimInheritanceTxParam?.masterSignerIds.orEmpty(),
                magic = claimInheritanceTxParam?.magicalPhrase.orEmpty(),
                derivationPaths = claimInheritanceTxParam?.derivationPaths.orEmpty(),
                isDraft = false,
                amount = claimInheritanceTxParam?.customAmount ?: 0.0,
                bsms = claimInheritanceTxParam?.bsms,
                antiFeeSniping = false,
            )
        )
        _event.emit(SelectWalletEvent.Loading(false))
        if (result.isSuccess) {
            _event.emit(SelectWalletEvent.CreateTransactionSuccessEvent(result.getOrThrow()))
        } else {
            _event.emit(SelectWalletEvent.Error(result.exceptionOrNull()))
        }
    }

    val selectedWalletId: String
        get() = _state.value.selectedWalletId

    val selectWalletAddress: String
        get() = _state.value.selectWalletAddress

    val manualFeeRate: Int
        get() = _state.value.feeRates.defaultRate
}

sealed class SelectWalletEvent {
    data class GetAddressSuccess(val address: String, val isCreateTransaction: Boolean) :
        SelectWalletEvent()

    data class GetFeeRateSuccess(val estimateFeeRates: EstimateFeeRates) : SelectWalletEvent()
    data class Loading(val isLoading: Boolean, val isClaimInheritance: Boolean = false) :
        SelectWalletEvent()

    data class Error(val e: Throwable?) : SelectWalletEvent()
    data class CreateTransactionSuccessEvent(val extendTransaction: ExtendTransaction) :
        SelectWalletEvent()
}

data class SelectWalletState(
    val selectWallets: List<SelectableWallet> = emptyList(),
    val selectedWalletId: String = "",
    val feeRates: EstimateFeeRates = EstimateFeeRates(),
    val selectWalletAddress: String = ""
)