package com.nunchuk.android.transaction.receive.address.used

import com.nunchuk.android.arch.vm.NunchukViewModel
import javax.inject.Inject

internal class UsedAddressViewModel @Inject constructor(
) : NunchukViewModel<Unit, Unit>() {

    override val initialState = Unit

}