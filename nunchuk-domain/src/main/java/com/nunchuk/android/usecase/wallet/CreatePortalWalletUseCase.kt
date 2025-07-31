package com.nunchuk.android.usecase.wallet

import com.nunchuk.android.usecase.wallet.AddWalletBannerStateUseCase
import com.nunchuk.android.domain.di.IoDispatcher
import com.nunchuk.android.model.Wallet
import com.nunchuk.android.nativelib.NunchukNativeSdk
import com.nunchuk.android.type.SignerType
import com.nunchuk.android.usecase.UseCase
import kotlinx.coroutines.CoroutineDispatcher
import javax.inject.Inject

class CreatePortalWalletUseCase @Inject constructor(
    private val nativeSdk: NunchukNativeSdk,
    private val getPortalSignerNameUseCase: GetPortalSignerNameUseCase,
    private val addWalletBannerStateUseCase: AddWalletBannerStateUseCase,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : UseCase<CreatePortalWalletUseCase.Params, Wallet>(ioDispatcher) {
    override suspend fun execute(parameters: Params): Wallet {
        val wallet = nativeSdk.parseWalletDescriptor(parameters.descriptor)
        val portalSigner = wallet.signers.find {
            it.masterFingerprint == parameters.portalXfp
        } ?: throw IllegalArgumentException("Portal signer not found")
        var newPortalSigner = portalSigner.copy(type = SignerType.PORTAL_NFC)
        if (!nativeSdk.hasSigner(newPortalSigner)) {
            val newPortalSignerName = getPortalSignerNameUseCase(Unit).getOrThrow()
            newPortalSigner = portalSigner.copy(
                name = newPortalSignerName,
                type = SignerType.PORTAL_NFC
            )
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
        val createdWallet = nativeSdk.createWallet2(newWallet)
        
        // Automatically set banner state based on wallet conditions
        addWalletBannerStateUseCase(createdWallet.id)
        
        return createdWallet
    }

    data class Params(
        val descriptor: String,
        val portalXfp: String,
    )
}