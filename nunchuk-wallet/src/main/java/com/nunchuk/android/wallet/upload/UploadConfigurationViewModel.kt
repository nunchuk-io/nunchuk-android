package com.nunchuk.android.wallet.upload

import com.nunchuk.android.arch.vm.NunchukViewModel
import javax.inject.Inject

internal class UploadConfigurationViewModel @Inject constructor(

) : NunchukViewModel<UploadConfigurationState, UploadConfigurationEvent>() {

    override val initialState = UploadConfigurationState()

    fun init() {
        updateState { initialState }
    }

}