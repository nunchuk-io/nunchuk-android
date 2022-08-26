package com.nunchuk.android.core.domain

import com.nunchuk.android.core.account.AccountInfo
import com.nunchuk.android.core.account.AccountManager
import com.nunchuk.android.core.account.PrimaryKeyInfo
import com.nunchuk.android.core.guestmode.SignInMode
import com.nunchuk.android.domain.di.IoDispatcher
import com.nunchuk.android.nativelib.NunchukNativeSdk
import com.nunchuk.android.repository.SignerSoftwareRepository
import com.nunchuk.android.usecase.UseCase
import kotlinx.coroutines.CoroutineDispatcher
import javax.inject.Inject

class SignUpPrimaryKeyUseCase @Inject constructor(
    @IoDispatcher dispatcher: CoroutineDispatcher,
    private val nunchukNativeSdk: NunchukNativeSdk,
    private val signerSoftwareRepository: SignerSoftwareRepository,
    private val accountManager: AccountManager
) : UseCase<SignUpPrimaryKeyUseCase.Param, Unit>(dispatcher) {
    override suspend fun execute(parameters: Param) {

        val resultGetNonce = signerSoftwareRepository.getPKeyNonce(
            address = parameters.address,
            username = parameters.username
        )
        if (resultGetNonce.isBlank()) throw IllegalStateException()

        val resultSignLoginMessage = nunchukNativeSdk.signLoginMessage(
            parameters.mnemonic,
            parameters.passphrase,
            "${parameters.username}${resultGetNonce}"
        )
        if (resultSignLoginMessage.isNullOrBlank()) throw IllegalStateException()

        val response = signerSoftwareRepository.pKeySignUp(
            address = parameters.address,
            username = parameters.username,
            signature = resultSignLoginMessage
        )

        val masterSigner = nunchukNativeSdk.createSoftwareSigner(
            name = parameters.signerName,
            mnemonic = parameters.mnemonic,
            passphrase = parameters.passphrase,
            isPrimary = true
        )

        accountManager.storeAccount(
            AccountInfo(
                token = response.tokenId,
                activated = true,
                staySignedIn = parameters.staySignedIn,
                name = parameters.username,
                username = parameters.username,
                deviceId = response.deviceId,
                primaryKeyInfo = PrimaryKeyInfo(xfp = masterSigner.id),
                loginType = SignInMode.PRIMARY_KEY.value
            )
        )
    }

    data class Param(
        val mnemonic: String,
        val passphrase: String,
        val address: String,
        val username: String,
        val signerName: String,
        val defaultUserName: String,
        val staySignedIn: Boolean
    )
}