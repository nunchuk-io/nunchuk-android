package com.nunchuk.android.wallet.confirm

import com.nunchuk.android.arch.vm.NunchukViewModel
import javax.inject.Inject

internal class WalletConfirmViewModel @Inject constructor(
) : NunchukViewModel<WalletConfirmState, WalletConfirmEvent>() {

    override val initialState = WalletConfirmState()

    fun init() {

    }

}