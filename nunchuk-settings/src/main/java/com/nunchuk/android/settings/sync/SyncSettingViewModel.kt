/**************************************************************************
 * This file is part of the Nunchuk software (https://nunchuk.io/)        *							          *
 * Copyright (C) 2022 Nunchuk								              *
 *                                                                        *
 * This program is free software; you can redistribute it and/or          *
 * modify it under the terms of the GNU General Public License            *
 * as published by the Free Software Foundation; either version 3         *
 * of the License, or (at your option) any later version.                 *
 *                                                                        *
 * This program is distributed in the hope that it will be useful,        *
 * but WITHOUT ANY WARRANTY; without even the implied warranty of         *
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the          *
 * GNU General Public License for more details.                           *
 *                                                                        *
 * You should have received a copy of the GNU General Public License      *
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.  *
 *                                                                        *
 **************************************************************************/

package com.nunchuk.android.settings.sync

import androidx.lifecycle.viewModelScope
import com.nunchuk.android.arch.vm.NunchukViewModel
import com.nunchuk.android.core.domain.GetSyncSettingUseCase
import com.nunchuk.android.core.domain.UpdateSyncSettingUseCase
import com.nunchuk.android.core.domain.data.SyncSetting
import com.nunchuk.android.usecase.BackupDataUseCase
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
    private val backupDataUseCase: BackupDataUseCase,
    private val updateSyncSettingUseCase: UpdateSyncSettingUseCase,
    private val getSyncSettingUseCase: GetSyncSettingUseCase,
    private val enableAutoBackupUseCase: EnableAutoBackupUseCase,
) : NunchukViewModel<SyncSettingState, SyncSettingEvent>() {

    override val initialState = SyncSettingState()

    init {
        viewModelScope.launch {
            getSyncSettingUseCase(Unit)
                .collect {
                    val isEnable = it.getOrElse { false }
                    updateState {
                        copy(syncSetting = SyncSetting(isEnable))
                    }
                    event(SyncSettingEvent.GetSyncSettingSuccessEvent(isEnable))
                }
        }
    }

    fun updateSyncSettings(syncSetting: SyncSetting) {
        viewModelScope.launch {
            updateSyncSettingUseCase(syncSetting.enable)
        }
    }

    fun enableAutoBackup(enable: Boolean) {
        viewModelScope.launch {
            enableAutoBackupUseCase.execute(enable)
                .flowOn(Dispatchers.IO)
                .onException { }
                .collect {
                    Timber.v("enableAutoBackup success")
                    event(SyncSettingEvent.EnableAutoUpdateSuccessEvent)
                }
        }
    }

    fun backupData() {
        // backup missing data if needed
        viewModelScope.launch {
            backupDataUseCase.execute()
                .flowOn(Dispatchers.IO)
                .onException { }
                .collect { Timber.v("backupDataUseCase success") }
        }
    }

}