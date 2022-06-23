package com.nunchuk.android.settings.sync

import androidx.lifecycle.viewModelScope
import com.nunchuk.android.arch.vm.NunchukViewModel
import com.nunchuk.android.core.domain.GetSyncSettingUseCase
import com.nunchuk.android.core.domain.UpdateSyncSettingUseCase
import com.nunchuk.android.core.domain.data.SyncSetting
import com.nunchuk.android.usecase.EnableAutoBackupUseCase
import com.nunchuk.android.utils.onException
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
internal class SyncSettingViewModel @Inject constructor(
    private val updateSyncSettingUseCase: UpdateSyncSettingUseCase,
    private val getSyncSettingUseCase: GetSyncSettingUseCase,
    private val enableAutoBackupUseCase: EnableAutoBackupUseCase,
) : NunchukViewModel<SyncSettingState, SyncSettingEvent>() {

    override val initialState = SyncSettingState()

    fun getSyncSettings() {
        viewModelScope.launch {
            getSyncSettingUseCase.execute()
                .flowOn(Dispatchers.IO)
                .onException { }
                .flowOn(Dispatchers.Main)
                .collect {
                    updateState {
                        copy(syncSetting = it)
                    }
                    event(SyncSettingEvent.UpdateSyncSettingSuccessEvent(it.enable))
                }
        }
    }

    fun updateSyncSettings(syncSetting: SyncSetting) {
        viewModelScope.launch {
            updateSyncSettingUseCase.execute(syncSetting)
                .flowOn(Dispatchers.IO)
                .onException { }
                .flowOn(Dispatchers.Main)
                .collect {
                    updateState {
                        copy(syncSetting = it)
                    }
                    event(SyncSettingEvent.UpdateSyncSettingSuccessEvent(it.enable))
                }
        }
    }

    fun enableAutoBackup(enable: Boolean) {
        viewModelScope.launch {
            enableAutoBackupUseCase.execute(enable)
                .flowOn(Dispatchers.IO)
                .onException { }
                .collect { Timber.v("enableAutoBackup success") }
        }
    }

}