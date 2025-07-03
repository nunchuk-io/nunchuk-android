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

package com.nunchuk.android.core.profile

import com.nunchuk.android.core.account.AccountManager
import com.nunchuk.android.core.guestmode.SignInMode
import com.nunchuk.android.domain.di.IoDispatcher
import com.nunchuk.android.usecase.UseCase
import kotlinx.coroutines.CoroutineDispatcher
import javax.inject.Inject

class GetUserProfileUseCase @Inject constructor(
    private val accountManager: AccountManager,
    private val userRepository: UserRepository,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : UseCase<Unit, String>(ioDispatcher) {

    override suspend fun execute(parameters: Unit): String {
        val user = userRepository.getUserProfile()
        accountManager.storeAccount(
            accountManager.getAccount()
                .copy(
                    chatId = user.chatId.orEmpty(),
                    name = user.name.orEmpty(),
                    email = user.email.orEmpty(),
                    avatarUrl = user.avatar.orEmpty(),
                    id = user.id.orEmpty(),
                    loginTypeOriginal = convertLoginTypeToInt(user.loginType)
                )
        )
        return user.chatId.orEmpty()
    }

    private fun convertLoginTypeToInt(loginType: String?): Int {
        return when (loginType?.lowercase()) {
            "email" -> SignInMode.EMAIL.value
            "primary_key" -> SignInMode.PRIMARY_KEY.value
            "guest" -> SignInMode.GUEST_MODE.value
            "openid" -> SignInMode.OPENID.value
            else -> SignInMode.UNKNOWN.value
        }
    }
}
