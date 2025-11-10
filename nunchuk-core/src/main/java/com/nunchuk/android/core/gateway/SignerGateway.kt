package com.nunchuk.android.core.gateway

import com.nunchuk.android.core.data.model.membership.SignerServerDto
import com.nunchuk.android.core.signer.toSignerTag
import com.nunchuk.android.model.SingleSigner
import com.nunchuk.android.nativelib.NunchukNativeSdk
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SignerGateway @Inject constructor(
    private val nunchukNativeSdk: NunchukNativeSdk,
) {

    /**
     * Return signer exist in local
     */
    fun saveServerSignerIfNeed(signer: SignerServerDto): Boolean {
        val hasSigner = nunchukNativeSdk.hasSigner(
            SingleSigner(
                name = signer.name.orEmpty(),
                xpub = signer.xpub.orEmpty(),
                publicKey = signer.pubkey.orEmpty(),
                derivationPath = signer.derivationPath.orEmpty(),
                masterFingerprint = signer.xfp.orEmpty(),
            )
        )
        if (hasSigner) return true
        val tapsigner = signer.tapsigner
        if (tapsigner != null) {
            nunchukNativeSdk.addTapSigner(
                cardId = tapsigner.cardId,
                name = signer.name.orEmpty(),
                xfp = signer.xfp.orEmpty(),
                version = tapsigner.version.orEmpty(),
                brithHeight = tapsigner.birthHeight,
                isTestNet = tapsigner.isTestnet,
                replace = false
            )
        } else {
            val type = nunchukNativeSdk.signerTypeFromStr(signer.type.orEmpty())
            nunchukNativeSdk.createSigner(
                name = signer.name.orEmpty(),
                xpub = signer.xpub.orEmpty(),
                publicKey = signer.pubkey.orEmpty(),
                derivationPath = signer.derivationPath.orEmpty(),
                masterFingerprint = signer.xfp.orEmpty(),
                type = type,
                tags = signer.tags.orEmpty().mapNotNull { tag -> tag.toSignerTag() },
                replace = false
            )
        }
        return false
    }
}