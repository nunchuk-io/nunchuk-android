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

import com.nunchuk.android.auth.api.UserTokenResponse
import com.nunchuk.android.auth.data.AuthRepository
import com.nunchuk.android.core.account.AccountManager
import com.nunchuk.android.core.profile.GetUserProfileUseCase
import com.nunchuk.android.core.profile.MarkOnBoardUseCase
import com.nunchuk.android.domain.di.IoDispatcher
import com.nunchuk.android.model.MembershipPlan
import com.nunchuk.android.usecase.byzantine.SyncGroupWalletsUseCase
import com.nunchuk.android.usecase.membership.GetUserSubscriptionUseCase
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.supervisorScope
import javax.inject.Inject

interface SignInUseCase {
    fun execute(
        email: String,
        password: String,
        staySignedIn: Boolean = true,
    ): Flow<Pair<String, String>>
}

internal class SignInUseCaseImpl @Inject constructor(
    private val authRepository: AuthRepository,
    private val accountManager: AccountManager,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
    private val getUserProfileUseCase: GetUserProfileUseCase,
    private val getUserSubscriptionUseCase: GetUserSubscriptionUseCase,
    private val syncGroupWalletsUseCase: SyncGroupWalletsUseCase,
    private val markOnBoardUseCase: MarkOnBoardUseCase
) : SignInUseCase {

    override fun execute(email: String, password: String, staySignedIn: Boolean) =
        authRepository.login(
            email = email,
            password = password
        ).map {
            storeAccount(email, it, staySignedIn)
        }.flowOn(ioDispatcher)

    private suspend fun storeAccount(
        email: String,
        response: UserTokenResponse,
        staySignedIn: Boolean,
    ): Pair<String, String> {
        val account = accountManager.getAccount()
        accountManager.storeAccount(
            account.copy(
                email = email,
                token = response.tokenId,
                activated = true,
                staySignedIn = staySignedIn,
                deviceId = response.deviceId,
            )
        )

        runCatching {
            getUserProfileUseCase(Unit)
        }

        supervisorScope {
            val subscription = async {
                getUserSubscriptionUseCase(Unit)
                    .onSuccess {
                        if (it.plan != MembershipPlan.NONE) {
                            markOnBoardUseCase(Unit)
                        }
                    }
            }

            val groupWallets = async {
                    syncGroupWalletsUseCase(Unit)
                        .onSuccess {
                            if (it) {
                                markOnBoardUseCase(Unit)
                            }
                        }
            }

            subscription.await()
            groupWallets.await()
        }

        return response.tokenId to response.deviceId
    }

}