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

package com.nunchuk.android.core.domain.wallet

import com.nunchuk.android.core.domain.GetAppSettingUseCase
import com.nunchuk.android.domain.di.IoDispatcher
import com.nunchuk.android.model.Wallet
import com.nunchuk.android.nativelib.NunchukNativeSdk
import com.nunchuk.android.usecase.UseCase
import kotlinx.coroutines.CoroutineDispatcher
import javax.inject.Inject
class ParseKeystoneWalletUseCase @Inject constructor(
    private val nunchukNativeSdk: NunchukNativeSdk,
    private val getAppSettingUseCase: GetAppSettingUseCase,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) : UseCase<List<String>, Wallet>(ioDispatcher) {

    override suspend fun execute(parameters: List<String>): Wallet {
        val chain = getAppSettingUseCase(Unit).getOrThrow().chain
        return nunchukNativeSdk.parseKeystoneWallet(
            chain.ordinal,
            parameters,
        ) ?: throw NullPointerException("Can not parse wallet")
    }
}