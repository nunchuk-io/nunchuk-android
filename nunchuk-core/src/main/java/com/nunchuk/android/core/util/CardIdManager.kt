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

package com.nunchuk.android.core.util

import com.nunchuk.android.core.domain.GetTapSignerStatusByIdUseCase
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CardIdManager @Inject constructor(private val getTapSignerStatusByIdUseCase: GetTapSignerStatusByIdUseCase) {

    private val tapSignerCardIds = hashMapOf<String, String>()
    private val mutex = Mutex()

    suspend fun getCardId(signerId: String): String {
        mutex.withLock {
            return tapSignerCardIds[signerId]
                ?: getTapSignerStatusByIdUseCase(signerId).getOrNull()?.ident.orEmpty()
                    .also { cardId ->
                        tapSignerCardIds[signerId] = cardId
                    }
        }
    }
}