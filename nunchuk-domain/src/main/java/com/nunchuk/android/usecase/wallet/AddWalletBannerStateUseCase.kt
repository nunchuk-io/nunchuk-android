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

package com.nunchuk.android.usecase.wallet

import com.nunchuk.android.domain.di.IoDispatcher
import com.nunchuk.android.model.BannerState
import com.nunchuk.android.nativelib.NunchukNativeSdk
import com.nunchuk.android.repository.SettingRepository
import com.nunchuk.android.type.AddressType
import com.nunchuk.android.usecase.UseCase
import kotlinx.coroutines.CoroutineDispatcher
import javax.inject.Inject

/**
 * Use case for adding a new wallet banner state based on wallet conditions.
 * Determines the appropriate banner state based on wallet type and address type:
 * - Multisig wallets: BACKUP_AND_REGISTER
 * - Singlesig + Native Segwit: BACKUP_ONLY  
 * - Singlesig + Non-Native Segwit: No banner saved
 */
class AddWalletBannerStateUseCase @Inject constructor(
    private val settingRepository: SettingRepository,
    private val nativeSdk: NunchukNativeSdk,
    @IoDispatcher private val dispatcher: CoroutineDispatcher,
) : UseCase<String, Unit>(dispatcher) {

    override suspend fun execute(parameters: String) {
        // First, retrieve the wallet using walletId
        val wallet = nativeSdk.getWallet(parameters)
        
        // Determine BannerState based on wallet conditions
        val bannerState = when {
            // If wallet is multisig → BACKUP_AND_REGISTER
            wallet.signers.size > 1 -> BannerState.BACKUP_AND_REGISTER
            
            // If wallet is singlesig and addressType == Native Segwit and derivationPath == "m/84h/0h/0h" → BACKUP_ONLY  
            wallet.signers.size == 1 && 
            wallet.addressType == AddressType.NATIVE_SEGWIT && 
            wallet.signers.first().derivationPath == "m/84h/0h/0h" -> BannerState.BACKUP_ONLY
            
            // If wallet is singlesig and conditions are not met → do not save anything
            else -> return // Exit early without saving anything
        }
        
        // Save the determined banner state
        settingRepository.addWalletBannerState(parameters, bannerState)
    }
} 