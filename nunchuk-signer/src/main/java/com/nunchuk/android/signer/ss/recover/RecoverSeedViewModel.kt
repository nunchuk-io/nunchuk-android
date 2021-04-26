package com.nunchuk.android.signer.ss.recover

import com.nunchuk.android.arch.vm.NunchukViewModel
import javax.inject.Inject

internal class RecoverSeedViewModel @Inject constructor(
) : NunchukViewModel<RecoverSeedState, RecoverSeedEvent>() {

    override val initialState = RecoverSeedState()

}
