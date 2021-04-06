package com.nunchuk.android.wallet.add

import com.nunchuk.android.arch.vm.NunchukViewModel
import javax.inject.Inject

internal class AddWalletViewModel @Inject constructor(
) : NunchukViewModel<Unit, AddWalletEvent>() {

    override val initialState = Unit

    fun handleAddWallet() {

    }

}