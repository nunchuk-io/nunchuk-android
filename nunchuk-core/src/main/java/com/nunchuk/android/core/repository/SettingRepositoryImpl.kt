/**************************************************************************
 * This file is part of the Nunchuk software (https://nunchuk.io/)        *
 * Copyright (C) 2022, 2023 Nunchuk                                       *
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

package com.nunchuk.android.core.repository

import com.nunchuk.android.core.persistence.NcDataStore
import com.nunchuk.android.core.persistence.NcEncryptedPreferences
import com.nunchuk.android.model.setting.WalletSecuritySetting
import com.nunchuk.android.repository.SettingRepository
import com.nunchuk.android.type.Chain
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

internal class SettingRepositoryImpl @Inject constructor(
    private val ncDataStore: NcDataStore,
    private val ncEncryptedPreferences: NcEncryptedPreferences
): SettingRepository {
    override val syncEnable: Flow<Boolean>
        get() = ncDataStore.syncEnableFlow

    override val isShowNfcUniversal: Flow<Boolean>
        get() = ncDataStore.isShowNfcUniversal

    override val chain: Flow<Chain>
        get() = ncDataStore.chain

    override val syncRoomSuccess: Flow<Boolean>
        get() = ncDataStore.syncRoomSuccess

    override val qrDensity: Flow<Int>
        get() = ncDataStore.qrDensity

    override val walletSecuritySetting: Flow<WalletSecuritySetting>
        get() = ncDataStore.walletSecuritySetting

    override val walletPin: Flow<String>
        get() = ncEncryptedPreferences.getWalletPinFlow()

    override val localCurrency: Flow<String>
        get() = ncDataStore.localCurrencyFlow

    override suspend fun markSyncRoomSuccess() {
        ncDataStore.markSyncRoomSuccess()
    }

    override suspend fun setSyncEnable(isEnable: Boolean) {
        ncDataStore.setSyncEnable(isEnable)
    }

    override suspend fun setQrDensity(density: Int) {
        ncDataStore.setDensity(density)
    }

    override suspend fun markIsShowNfcUniversal() {
        ncDataStore.markIsShowNfcUniversal()
    }

    override suspend fun setWalletSecuritySetting(config: String) {
        ncDataStore.setWalletSecuritySetting(config)
    }

    override suspend fun setWalletPin(pin: String) {
        ncEncryptedPreferences.setWalletPin(pin)
    }

    override suspend fun setLocalCurrency(currency: String) {
        ncDataStore.setLocalCurrency(currency)
    }
}