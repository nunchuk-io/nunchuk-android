package com.nunchuk.android.usecase.signer

import com.nunchuk.android.domain.di.IoDispatcher
import com.nunchuk.android.model.SingleSigner
import com.nunchuk.android.nativelib.NunchukNativeSdk
import com.nunchuk.android.type.AddressType
import com.nunchuk.android.type.WalletType
import com.nunchuk.android.usecase.UseCase
import kotlinx.coroutines.CoroutineDispatcher
import javax.inject.Inject

class GetSignerFromMasterSignerByIndexUseCase @Inject constructor(
    private val nativeSdk: NunchukNativeSdk,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : UseCase<GetSignerFromMasterSignerByIndexUseCase.Param, SingleSigner?>(ioDispatcher) {

    override suspend fun execute(parameters: Param): SingleSigner? {
        return nativeSdk.getSignerFromMasterSigner(
            masterSignerId = parameters.masterSignerId,
            walletType = parameters.walletType.ordinal,
            addressType = parameters.addressType.ordinal,
            index = parameters.index
        )
    }

    data class Param(
        val masterSignerId : String,
        val walletType: WalletType,
        val addressType: AddressType,
        val index: Int,
    )
}