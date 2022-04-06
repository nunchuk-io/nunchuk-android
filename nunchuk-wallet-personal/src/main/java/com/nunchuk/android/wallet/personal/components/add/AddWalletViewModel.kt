package com.nunchuk.android.wallet.personal.components.add

import com.nunchuk.android.arch.vm.NunchukViewModel
import com.nunchuk.android.type.AddressType.*
import com.nunchuk.android.type.WalletType.ESCROW
import com.nunchuk.android.type.WalletType.MULTI_SIG
import com.nunchuk.android.wallet.personal.components.add.AddWalletEvent.WalletNameRequiredEvent
import com.nunchuk.android.wallet.personal.components.add.AddWalletEvent.WalletSetupDoneEvent
import javax.inject.Inject

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