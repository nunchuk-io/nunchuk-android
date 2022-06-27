package com.nunchuk.android.wallet.shared.components.create

import com.nunchuk.android.arch.vm.NunchukViewModel
import com.nunchuk.android.type.AddressType.*
import com.nunchuk.android.type.WalletType.ESCROW
import com.nunchuk.android.type.WalletType.MULTI_SIG
import com.nunchuk.android.wallet.shared.components.create.AddSharedWalletEvent.WalletNameRequiredEvent
import com.nunchuk.android.wallet.shared.components.create.AddSharedWalletEvent.WalletSetupDoneEvent
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
internal class CreateSharedWalletViewModel @Inject constructor(
) : NunchukViewModel<AddSharedWalletState, AddSharedWalletEvent>() {

    override val initialState = AddSharedWalletState()

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