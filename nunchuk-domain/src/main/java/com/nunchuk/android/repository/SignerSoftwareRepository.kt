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