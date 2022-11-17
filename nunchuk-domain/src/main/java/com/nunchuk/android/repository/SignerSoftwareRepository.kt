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

package com.nunchuk.android.repository

import com.nunchuk.android.model.PKeySignInResponse
import com.nunchuk.android.model.PKeySignUpResponse
import com.nunchuk.android.model.UserResponse

interface SignerSoftwareRepository {
    suspend fun getPKeyNonce(address: String?, username: String): String
    suspend fun postPKeyNonce(
        address: String?,
        username: String,
        nonce: String?,
        isChangeKey: Boolean
    ): String

    suspend fun pKeySignUp(
        address: String,
        username: String,
        signature: String
    ): PKeySignUpResponse

    suspend fun pKeySignIn(
        address: String?,
        username: String,
        signature: String
    ): PKeySignInResponse

    suspend fun pKeyUserInfo(address: String): UserResponse

    suspend fun pKeyCheckUsername(username: String)

    suspend fun pKeyChangeKey(newKey: String, oldSignedMessage: String, newSignedMessage: String)

    suspend fun pKeyDeleteAccount(signedMessage: String)
}