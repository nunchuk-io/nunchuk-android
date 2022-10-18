package com.nunchuk.android

import com.nunchuk.android.domain.di.IoDispatcher
import com.nunchuk.android.model.SingleSigner
import com.nunchuk.android.nativelib.NunchukNativeSdk
import com.nunchuk.android.type.AddressType
import com.nunchuk.android.type.WalletType
import com.nunchuk.android.usecase.UseCase
import kotlinx.coroutines.CoroutineDispatcher
import javax.inject.Inject

class GetDefaultSignerFromMasterSignerUseCase @Inject constructor(
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
    private val nunchukNativeSdk: NunchukNativeSdk
) : UseCase<GetDefaultSignerFromMasterSignerUseCase.Params, SingleSigner>(ioDispatcher) {

    override suspend fun execute(parameters: Params): SingleSigner {
        return nunchukNativeSdk.getDefaultSignerFromMasterSigner(
            masterSignerId = parameters.masterSignerId,
            walletType = parameters.walletType.ordinal,
            addressType = parameters.addressType.ordinal
        )
    }

    data class Params(
        val masterSignerId: String, val walletType: WalletType, val addressType: AddressType
    )
}