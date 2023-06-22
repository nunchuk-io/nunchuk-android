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

package com.nunchuk.android.wallet.personal.components.add

import com.nunchuk.android.arch.vm.NunchukViewModel
import com.nunchuk.android.type.AddressType.*
import com.nunchuk.android.type.WalletType.ESCROW
import com.nunchuk.android.type.WalletType.MULTI_SIG
import com.nunchuk.android.wallet.personal.components.add.AddWalletEvent.WalletNameRequiredEvent
import com.nunchuk.android.wallet.personal.components.add.AddWalletEvent.WalletSetupDoneEvent
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
internal class AddWalletViewModel @Inject constructor(
) : NunchukViewModel<AddWalletState, AddWalletEvent>() {

    override val initialState = AddWalletState()

    fun init() {
        updateState { initialState }
    }

    fun setStandardWalletType() {
        updateState { copy(walletType = MULTI_SIG) }
    }

    fun setEscrowWalletType() {
        updateState { copy(walletType = ESCROW) }
    }

    fun setDefaultWalletType() {
        updateState { copy(walletType = MULTI_SIG) }
    }

    fun setDefaultAddressType() {
        updateState { copy(addressType = NATIVE_SEGWIT) }
    }

    fun setNestedAddressType() {
        updateState { copy(addressType = NESTED_SEGWIT) }
    }

    fun setLegacyAddressType() {
        updateState { copy(addressType = LEGACY) }
    }

    fun setTaprootAddressType() {
        updateState { copy(addressType = TAPROOT) }
    }

    fun setNativeAddressType() {
        updateState { copy(addressType = NATIVE_SEGWIT) }
    }

    fun updateWalletName(walletName: String) {
        updateState { copy(walletName = walletName) }
    }

    fun handleContinueEvent() {
        val currentState = getState()
        if (currentState.walletName.isNotEmpty()) {
            event(
                WalletSetupDoneEvent(
                    walletName = currentState.walletName,
                    walletType = currentState.walletType,
                    addressType = currentState.addressType
                )
            )
        } else {
            event(WalletNameRequiredEvent)
        }
    }

}