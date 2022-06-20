package com.nunchuk.android.settings.unit

import androidx.lifecycle.viewModelScope
import com.nunchuk.android.arch.vm.NunchukViewModel
import com.nunchuk.android.core.domain.GetDisplayUnitSettingUseCase
import com.nunchuk.android.core.domain.UpdateDisplayUnitSettingUseCase
import com.nunchuk.android.core.entities.DisplayUnitSetting
import com.nunchuk.android.utils.onException
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
internal class DisplayUnitViewModel @Inject constructor(
    private val updateDisplayUnitSettingUseCase: UpdateDisplayUnitSettingUseCase,
    private val getDisplayUnitSettingUseCase: GetDisplayUnitSettingUseCase
) : NunchukViewModel<DisplayUnitState, DisplayUnitEvent>() {

    override val initialState = DisplayUnitState()

    val currentDisplayUnitSettings: DisplayUnitSetting?
        get() = state.value?.displayUnitSetting

    fun getDisplayUnitSetting() {
        viewModelScope.launch {
            getDisplayUnitSettingUseCase.execute()
                .flowOn(Dispatchers.IO)
                .onException { }
                .flowOn(Dispatchers.Main)
                .collect {
                    updateState {
                        copy(displayUnitSetting = it)
                    }

                }
        }
    }

    fun updateDisplayUnitSetting(displayUnitSetting: DisplayUnitSetting) {
        viewModelScope.launch {
            updateDisplayUnitSettingUseCase.execute(displayUnitSetting)
                .flowOn(Dispatchers.IO)
                .onException { }
                .flowOn(Dispatchers.Main)
                .collect {
                    updateState {
                        copy(displayUnitSetting = it)
                    }
                    event(DisplayUnitEvent.UpdateDisplayUnitSettingSuccessEvent(it))
                }
        }
    }

}