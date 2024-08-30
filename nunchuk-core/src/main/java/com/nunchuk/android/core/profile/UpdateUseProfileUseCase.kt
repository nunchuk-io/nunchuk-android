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
import com.nunchuk.android.domain.di.IoDispatcher
import com.nunchuk.android.usecase.UseCase
import kotlinx.coroutines.CoroutineDispatcher
import javax.inject.Inject

class UpdateUseProfileUseCase @Inject constructor(
    private val accountManager: AccountManager,
    private val userRepository: UserRepository,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) : UseCase<UpdateUseProfileUseCase.Params, Unit>(ioDispatcher) {

    override suspend fun execute(parameters: Params) {
        val response = userRepository.updateUserProfile(parameters.name, parameters.avatarUrl)
        accountManager.storeAccount(
            accountManager.getAccount()
                .copy(name = response.name.orEmpty(), avatarUrl = response.avatar.orEmpty())
        )
    }

    data class Params(val name: String? = null, val avatarUrl: String? = null)
}
