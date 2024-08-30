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

package com.nunchuk.android.usecase.campaign

import com.nunchuk.android.domain.di.IoDispatcher
import com.nunchuk.android.model.campaigns.Campaign
import com.nunchuk.android.model.campaigns.ReferrerCode
import com.nunchuk.android.repository.CampaignsRepository
import com.nunchuk.android.usecase.UseCase
import kotlinx.coroutines.CoroutineDispatcher
import javax.inject.Inject

class GetReferrerCodeByEmailUseCase @Inject constructor(
    private val repository: CampaignsRepository,
    @IoDispatcher ioDispatcher: CoroutineDispatcher
) : UseCase<GetReferrerCodeByEmailUseCase.Param, ReferrerCode?>(ioDispatcher) {
    override suspend fun execute(parameters: Param): ReferrerCode? {
        return repository.getReferrerCodesByEmail(email = parameters.email, token = parameters.token)
    }

    class Param(val email: String, val token: String?)
}