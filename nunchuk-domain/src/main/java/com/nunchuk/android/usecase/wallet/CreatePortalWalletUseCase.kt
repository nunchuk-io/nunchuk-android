package com.nunchuk.android.usecase.wallet

import com.nunchuk.android.domain.di.IoDispatcher
import com.nunchuk.android.model.Wallet
import com.nunchuk.android.nativelib.NunchukNativeSdk
import com.nunchuk.android.type.SignerType
import com.nunchuk.android.usecase.UseCase
import kotlinx.coroutines.CoroutineDispatcher
import javax.inject.Inject

class CreatePortalWalletUseCase @Inject constructor(
    private val nativeSdk: NunchukNativeSdk,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : UseCase<CreatePortalWalletUseCase.Params, Wallet>(ioDispatcher) {
    override suspend fun execute(parameters: Params): Wallet {
        val wallet = nativeSdk.parseWalletDescriptor(parameters.descriptor)
        val portalSigner = wallet.signers.find {
            it.masterFingerprint == parameters.portalXfp
        } ?: throw IllegalArgumentException("Portal signer not found")
        val newPortalSigner = portalSigner.copy(
            name = "Portal",
            type = SignerType.PORTAL_NFC
        )
        if (!nativeSdk.hasSigner(newPortalSigner)) {
            nativeSdk.createSigner(
                name = newPortalSigner.name,
                xpub = newPortalSigner.xpub,
                publicKey = newPortalSigner.publicKey,
                derivationPath = newPortalSigner.derivationPath,
                masterFingerprint = newPortalSigner.masterFingerprint,
                type = newPortalSigner.type,
                tags = newPortalSigner.tags,
                replace = false
            )
        }
        val newSigners = wallet.signers - portalSigner + newPortalSigner
        val newWallet = wallet.copy(signers = newSigners)
        return nativeSdk.createWallet2(newWallet)
    }

    data class Params(
        val descriptor: String,
        val portalXfp: String,
    )
}