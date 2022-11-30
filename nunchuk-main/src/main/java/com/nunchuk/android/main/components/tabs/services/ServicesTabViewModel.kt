package com.nunchuk.android.main.components.tabs.services

import androidx.lifecycle.viewModelScope
import com.nunchuk.android.arch.vm.NunchukViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ServicesTabViewModel @Inject constructor() :
    NunchukViewModel<ServicesTabState, ServicesTabEvent>() {

    override val initialState: ServicesTabState
        get() = ServicesTabState()

    fun onItemClick(item: ServiceTabRowItem) = viewModelScope.launch {
        event(ServicesTabEvent.ItemClick(item))
    }

    fun getItems() = getState().rowItems
}