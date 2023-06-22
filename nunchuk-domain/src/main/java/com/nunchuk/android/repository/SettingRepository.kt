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

package com.nunchuk.android.repository

import com.nunchuk.android.model.setting.WalletSecuritySetting
import com.nunchuk.android.type.Chain
import kotlinx.coroutines.flow.Flow

interface SettingRepository {
    val syncEnable: Flow<Boolean>
    val isShowNfcUniversal: Flow<Boolean>
    val chain: Flow<Chain>
    val syncRoomSuccess: Flow<Boolean>
    val qrDensity: Flow<Int>
    val walletSecuritySetting: Flow<WalletSecuritySetting>
    val walletPin: Flow<String>
    val localCurrency: Flow<String>
    suspend fun setSyncEnable(isEnable: Boolean)
    suspend fun setQrDensity(density: Int)
    suspend fun markSyncRoomSuccess()
    suspend fun markIsShowNfcUniversal()
    suspend fun setWalletSecuritySetting(config: String)
    suspend fun setWalletPin(pin: String)
    suspend fun setLocalCurrency(currency: String)
}