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

package com.nunchuk.android.core.profile

import com.nunchuk.android.core.account.AccountManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import javax.inject.Inject

interface GetUserProfileUseCase {
    fun execute(): Flow<String>
}

internal class GetUserProfileUseCaseImpl @Inject constructor(
    private val accountManager: AccountManager,
    private val userProfileRepository: UserProfileRepository
) : GetUserProfileUseCase {

    override fun execute() = userProfileRepository.getUserProfile().map {
        it.chatId.orEmpty().apply {
            accountManager.storeAccount(
                accountManager.getAccount()
                    .copy(chatId = this, name = it.name.orEmpty(), avatarUrl = it.avatar.orEmpty())
            )
        }
    }.flowOn(Dispatchers.IO)
}
