package com.nunchuk.android.core.mapper

import com.nunchuk.android.core.data.model.membership.SignerServerDto
import com.nunchuk.android.core.data.model.membership.TapSignerDto
import com.nunchuk.android.model.SingleSigner
import com.nunchuk.android.nativelib.NunchukNativeSdk
import com.nunchuk.android.type.SignerTag
import com.nunchuk.android.type.SignerType
import javax.inject.Inject

internal class ServerSignerMapper @Inject constructor(
    private val nunchukNativeSdk: NunchukNativeSdk,
) {
    operator fun invoke(
        signer: SingleSigner, isInheritanceKey: Boolean,
    ) = if (signer.type == SignerType.NFC) {
        val status = nunchukNativeSdk.getTapSignerStatusFromMasterSigner(signer.masterSignerId)
        SignerServerDto(
            name = signer.name,
            xfp = signer.masterFingerprint,
            derivationPath = signer.derivationPath,
            xpub = signer.xpub,
            pubkey = signer.publicKey,
            type = SignerType.NFC.name,
            tapsigner = TapSignerDto(
                cardId = status.ident.orEmpty(),
                version = status.version.orEmpty(),
                birthHeight = status.birthHeight,
                isTestnet = status.isTestNet,
                isInheritance = isInheritanceKey
            ),
            tags = if (isInheritanceKey) listOf(
                SignerTag.INHERITANCE.name
            ) else null
        )
    } else {
        SignerServerDto(
            name = signer.name,
            xfp = signer.masterFingerprint,
            derivationPath = signer.derivationPath,
            xpub = signer.xpub,
            pubkey = signer.publicKey,
            type = signer.type.name,
            tags = signer.tags.map { it.name }.toMutableList().apply {
                if (isInheritanceKey) {
                    add(SignerTag.INHERITANCE.name)
                }
            },
        )
    }
}