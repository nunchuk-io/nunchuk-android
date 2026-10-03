package com.nunchuk.android.signer.components.details.exportxpub.exporter

import com.nunchuk.android.core.constants.NativeErrorCode
import com.nunchuk.android.core.domain.signer.GetSignerFromTapsignerMasterSignerByPathUseCase
import com.nunchuk.android.core.util.nativeErrorCode
import com.nunchuk.android.type.SignerType
import com.nunchuk.android.usecase.GetSignerFromMasterSignerUseCase
import javax.inject.Inject

/**
 * A TAPSIGNER answers from the xpubs the phone cached; a path it hasn't seen comes back as
 * [NativeErrorCode.XPUB_NOT_CACHED] and is read off the card instead.
 */
internal class TapsignerXpubExporter @Inject constructor(
    private val getSignerFromMasterSignerUseCase: GetSignerFromMasterSignerUseCase,
    private val getSignerFromTapsignerMasterSignerByPathUseCase: GetSignerFromTapsignerMasterSignerByPathUseCase,
) : XpubExporter {
    override fun supports(key: XpubKey) = key.isMasterSigner && key.type == SignerType.NFC

    override val selectsAccount = true

    override suspend fun export(request: XpubRequest): XpubStep =
        getSignerFromMasterSignerUseCase(
            GetSignerFromMasterSignerUseCase.Params(
                masterSignerId = request.masterSignerId,
                newPath = request.path.orEmpty(),
            )
        ).fold(
            onSuccess = { XpubStep.Done(it.descriptor) },
            onFailure = {
                if (it.nativeErrorCode() == NativeErrorCode.XPUB_NOT_CACHED) {
                    XpubStep.NeedInput(XpubAction.NfcTap)
                } else {
                    XpubStep.Failed(it)
                }
            },
        )

    override suspend fun resume(request: XpubRequest, input: XpubInput): XpubStep {
        if (input !is XpubInput.Nfc) return export(request)
        return getSignerFromTapsignerMasterSignerByPathUseCase(
            GetSignerFromTapsignerMasterSignerByPathUseCase.Data(
                isoDep = input.isoDep,
                cvc = input.cvc,
                masterSignerId = request.masterSignerId,
                path = request.path.orEmpty(),
            )
        ).map { it.descriptor }.toXpubStep()
    }
}
