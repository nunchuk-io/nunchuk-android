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

package com.nunchuk.android.signer.software.components.data.repository

import com.nunchuk.android.model.PKeySignInResponse
import com.nunchuk.android.model.PKeySignUpResponse
import com.nunchuk.android.model.UserResponse
import com.nunchuk.android.repository.SignerSoftwareRepository
import com.nunchuk.android.signer.software.components.data.api.*
import javax.inject.Inject

internal class SignerSoftwareRepositoryImpl @Inject constructor(
    private val api: SignerSoftwareApi
) : SignerSoftwareRepository {

    override suspend fun getPKeyNonce(address: String?, username: String): String {
        return api.getPKeyNonce(address, username).data.nonce
    }

    override suspend fun postPKeyNonce(
        address: String?,
        username: String,
        nonce: String?,
        isChangeKey: Boolean
    ): String {
        if (isChangeKey) {
            return api.postPKeyNonceForReplace(
                PKeyNoncePayload(
                    address,
                    username,
                    nonce
                )
            ).data.nonce
        }
        return api.postPKeyNonce(PKeyNoncePayload(address, username, nonce)).data.nonce
    }

    override suspend fun pKeySignUp(
        address: String,
        username: String,
        signature: String
    ): PKeySignUpResponse {
        val payload =
            PKeySignUpPayload(address = address, username = username, signature = signature)
        return api.postPKeySignUp(payload).data
    }

    override suspend fun pKeySignIn(
        address: String?,
        username: String,
        signature: String
    ): PKeySignInResponse {
        val payload =
            PKeySignInPayload(address = address, username = username, signature = signature)
        return api.postPKeySignIn(payload).data
    }

    override suspend fun pKeyUserInfo(address: String): UserResponse {
        return api.getUserInfoPKey(address).data.user
    }

    override suspend fun pKeyCheckUsername(username: String) {
        val error = api.checkUsername(username).error
        throw error
    }

    override suspend fun pKeyChangeKey(
        newKey: String,
        oldSignedMessage: String,
        newSignedMessage: String
    ) {
        api.changePKey(PKeyChangeKeyPayload(newKey, oldSignedMessage, newSignedMessage))
    }

    override suspend fun pKeyDeleteAccount(signedMessage: String) {
        api.deletePKey(PKeyDeleteKeyPayload(signedMessage))
    }

}

