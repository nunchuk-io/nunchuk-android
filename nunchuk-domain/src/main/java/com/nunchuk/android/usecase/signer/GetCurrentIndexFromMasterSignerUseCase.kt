package com.nunchuk.android.usecase.signer

import com.nunchuk.android.domain.di.IoDispatcher
import com.nunchuk.android.nativelib.NunchukNativeSdk
import com.nunchuk.android.type.AddressType
import com.nunchuk.android.type.WalletType
import com.nunchuk.android.usecase.UseCase
import kotlinx.coroutines.CoroutineDispatcher
import javax.inject.Inject

class GetCurrentIndexFromMasterSignerUseCase @Inject constructor(
    private val nativeSdk: NunchukNativeSdk,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : UseCase<GetCurrentIndexFromMasterSignerUseCase.Param, Int>(ioDispatcher) {

    override suspend fun execute(parameters: Param): Int {
        return nativeSdk.getCurrentIndexFromMasterSigner(
            mastersignerId = parameters.xfp,
            walletType = parameters.walletType.ordinal,
            addressType = parameters.addressType.ordinal
        )
    }

    data class Param(
        val xfp : String,
        val walletType: WalletType,
        val addressType: AddressType,
    )
}