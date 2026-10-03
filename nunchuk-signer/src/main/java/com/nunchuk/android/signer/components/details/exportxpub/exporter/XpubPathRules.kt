package com.nunchuk.android.signer.components.details.exportxpub.exporter

import com.nunchuk.android.core.domain.utils.GetBip32PathUseCase
import com.nunchuk.android.type.AddressType
import com.nunchuk.android.type.WalletType
import com.nunchuk.android.usecase.IsValidDerivationPathUseCase
import javax.inject.Inject

/** The SDK's derivation-path rules the export flow relies on. */
interface XpubPathRules {
    suspend fun presetPath(isSingleSig: Boolean, addressType: AddressType, accountIndex: Int): Result<String>

    suspend fun isValid(path: String): Boolean
}

internal class SdkXpubPathRules @Inject constructor(
    private val getBip32PathUseCase: GetBip32PathUseCase,
    private val isValidDerivationPathUseCase: IsValidDerivationPathUseCase,
) : XpubPathRules {
    override suspend fun presetPath(isSingleSig: Boolean, addressType: AddressType, accountIndex: Int) =
        getBip32PathUseCase(
            GetBip32PathUseCase.Param(
                index = accountIndex,
                walletType = if (isSingleSig) WalletType.SINGLE_SIG else WalletType.MULTI_SIG,
                addressType = addressType,
            )
        )

    override suspend fun isValid(path: String) = isValidDerivationPathUseCase(path).getOrDefault(false)
}
