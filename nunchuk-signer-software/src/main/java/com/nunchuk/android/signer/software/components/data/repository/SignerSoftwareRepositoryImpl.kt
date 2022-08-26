package com.nunchuk.android.signer.software.components.data.repository

import com.nunchuk.android.model.PKeySignInResponse
import com.nunchuk.android.model.PKeySignUpResponse
import com.nunchuk.android.model.UserResponse
import com.nunchuk.android.repository.SignerSoftwareRepository
import com.nunchuk.android.signer.software.components.data.api.PKeyChangeKeyPayload
import com.nunchuk.android.signer.software.components.data.api.PKeyDeleteKeyPayload
import com.nunchuk.android.signer.software.components.data.api.PKeyNoncePayload
import com.nunchuk.android.signer.software.components.data.api.PKeySignInPayload
import com.nunchuk.android.signer.software.components.data.api.PKeySignUpPayload
import com.nunchuk.android.signer.software.components.data.api.SignerSoftwareApi
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
        val error = api.checkUsername(username).getError()
        if (error != null) throw error
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

