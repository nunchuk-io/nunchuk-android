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

package com.nunchuk.android.auth.domain

import com.nunchuk.android.core.account.AccountManager
import com.nunchuk.android.core.guestmode.SignInMode
import com.nunchuk.android.core.profile.GetUserProfileUseCase
import com.nunchuk.android.domain.di.IoDispatcher
import com.nunchuk.android.usecase.UseCase
import kotlinx.coroutines.CoroutineDispatcher
import javax.inject.Inject

class SignInViaDigitalSignatureUseCase @Inject constructor(
    @IoDispatcher dispatcher: CoroutineDispatcher,
    private val accountManager: AccountManager,
    private val getUserProfileUseCase: GetUserProfileUseCase,
    private val checkShowOnboardUseCase: CheckShowOnboardUseCase,
) : UseCase<SignInViaDigitalSignatureUseCase.Param, Unit>(dispatcher) {
    override suspend fun execute(parameters: Param) {
        val account = accountManager.getAccount()
        accountManager.storeAccount(
            account.copy(
                token = parameters.tokenId,
                activated = true,
                staySignedIn = true,
                deviceId = parameters.deviceId,
                loginType = SignInMode.EMAIL.value,
            )
        )
        runCatching {
            getUserProfileUseCase(Unit)
            checkShowOnboardUseCase(Unit)
        }
    }

    data class Param(
        val staySignedIn: Boolean,
        val tokenId: String,
        val deviceId: String
    )
}