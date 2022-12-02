package com.nunchuk.android.main.components.tabs.services

import com.nunchuk.android.arch.vm.NunchukViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class ServicesTabViewModel @Inject constructor() :
    NunchukViewModel<ServicesTabState, Unit>() {

    override val initialState: ServicesTabState
        get() = ServicesTabState()

    fun getItems() = getState().rowItems
}