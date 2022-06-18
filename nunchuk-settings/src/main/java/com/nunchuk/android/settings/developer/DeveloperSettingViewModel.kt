package com.nunchuk.android.settings.developer

import androidx.lifecycle.viewModelScope
import com.nunchuk.android.arch.vm.NunchukViewModel
import com.nunchuk.android.core.domain.GetDeveloperSettingUseCase
import com.nunchuk.android.core.domain.UpdateDeveloperSettingUseCase
import com.nunchuk.android.core.entities.DeveloperSetting
import com.nunchuk.android.utils.onException
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
internal class DeveloperSettingViewModel @Inject constructor(
    private val updateDeveloperSettingUseCase: UpdateDeveloperSettingUseCase,
    private val getDeveloperSettingUseCase: GetDeveloperSettingUseCase
) : NunchukViewModel<DeveloperSettingState, DeveloperSettingEvent>() {

    override val initialState = DeveloperSettingState()

    fun getDeveloperSettings() {
        viewModelScope.launch {
            getDeveloperSettingUseCase.execute()
                .flowOn(Dispatchers.IO)
                .onException { }
                .flowOn(Dispatchers.Main)
                .collect {
                    updateState {
                        copy(developerSetting = it)
                    }
                }
        }
    }

    fun updateDeveloperSettings(developerSetting: DeveloperSetting) {
        viewModelScope.launch {
            updateDeveloperSettingUseCase.execute(developerSetting)
                .flowOn(Dispatchers.IO)
                .onException { }
                .flowOn(Dispatchers.Main)
                .collect {
                    updateState {
                        copy(developerSetting = it)
                    }
                    event(DeveloperSettingEvent.UpdateSuccessEvent(it))
                }
        }
    }

}