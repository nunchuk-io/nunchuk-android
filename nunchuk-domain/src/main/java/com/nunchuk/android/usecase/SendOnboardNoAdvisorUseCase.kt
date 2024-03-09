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

package com.nunchuk.android.usecase

import com.nunchuk.android.domain.di.IoDispatcher
import com.nunchuk.android.model.Country
import com.nunchuk.android.model.banner.Banner
import com.nunchuk.android.repository.BannerRepository
import com.nunchuk.android.usecase.UseCase
import kotlinx.coroutines.CoroutineDispatcher
import javax.inject.Inject

class SendOnboardNoAdvisorUseCase @Inject constructor(
    private val repository: BannerRepository,
    @IoDispatcher ioDispatcher: CoroutineDispatcher
) : UseCase<SendOnboardNoAdvisorUseCase.Params, Unit>(ioDispatcher) {
    override suspend fun execute(parameters: Params) {
        repository.sendOnboardingNoAdvisor(email = parameters.email, countryCode = parameters.countryCode, note = parameters.note)
    }

    class Params(
        val email: String,
        val countryCode: String,
        val note: String?
    )
}