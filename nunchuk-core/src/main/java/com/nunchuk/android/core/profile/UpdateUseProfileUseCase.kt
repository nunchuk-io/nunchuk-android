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
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

interface UpdateUseProfileUseCase {
    fun execute(name: String?, avatarUrl: String?): Flow<String>
}

internal class UpdateUseProfileUseCaseImpl @Inject constructor(
    private val accountManager: AccountManager,
    private val userRepository: UserRepository
) : UpdateUseProfileUseCase {

    override fun execute(name: String?, avatarUrl: String?) = userRepository.updateUserProfile(name, avatarUrl)
        .map {
            accountManager.storeAccount(accountManager.getAccount().copy(name = it.name.orEmpty(), avatarUrl = it.avatar.orEmpty()))
            it.chatId.orEmpty()
        }
}
