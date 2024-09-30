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

package com.nunchuk.android.core.domain

import com.nunchuk.android.domain.di.IoDispatcher
import com.nunchuk.android.nativelib.NunchukNativeSdk
import com.nunchuk.android.repository.SettingRepository
import com.nunchuk.android.usecase.GetWalletSecuritySettingUseCase
import com.nunchuk.android.usecase.UpdateWalletSecuritySettingUseCase
import com.nunchuk.android.usecase.UseCase
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class CreateOrUpdateWalletPinUseCase @Inject constructor(
    @IoDispatcher ioDispatcher: CoroutineDispatcher,
    private val settingRepository: SettingRepository,
    private val androidNativeSdk: NunchukNativeSdk,
    private val updateWalletSecuritySettingUseCase: UpdateWalletSecuritySettingUseCase,
    private val getWalletSecuritySettingUseCase: GetWalletSecuritySettingUseCase,
) : UseCase<String, Unit>(ioDispatcher) {

    override suspend fun execute(parameters: String) {
        if (parameters.isNotBlank()) {
            settingRepository.setWalletPin(androidNativeSdk.hashSHA256(parameters))
            val walletSecuritySetting = getWalletSecuritySettingUseCase(Unit).map { it.getOrThrow() }.first()
            updateWalletSecuritySettingUseCase(
                walletSecuritySetting.copy(
                    protectWalletPassphrase = false,
                    protectWalletPassword = false
                )
            )
        } else {
            settingRepository.setWalletPin("")
        }
    }
}