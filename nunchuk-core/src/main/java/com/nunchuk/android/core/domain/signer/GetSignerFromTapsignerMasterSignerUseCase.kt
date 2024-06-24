package com.nunchuk.android.core.domain.signer

import android.nfc.tech.IsoDep
import com.nunchuk.android.core.domain.BaseNfcUseCase
import com.nunchuk.android.core.domain.WaitAutoCardUseCase
import com.nunchuk.android.domain.di.IoDispatcher
import com.nunchuk.android.model.SingleSigner
import com.nunchuk.android.nativelib.NunchukNativeSdk
import com.nunchuk.android.type.AddressType
import com.nunchuk.android.type.WalletType
import kotlinx.coroutines.CoroutineDispatcher
import javax.inject.Inject

class GetSignerFromTapsignerMasterSignerUseCase @Inject constructor(
    @IoDispatcher dispatcher: CoroutineDispatcher,
    private val nunchukNativeSdk: NunchukNativeSdk,
    waitAutoCardUseCase: WaitAutoCardUseCase,
) : BaseNfcUseCase<GetSignerFromTapsignerMasterSignerUseCase.Data, SingleSigner?>(
    dispatcher,
    waitAutoCardUseCase
) {

    override suspend fun executeNfc(parameters: Data) : SingleSigner? {
       return nunchukNativeSdk.getSignerFromTapsignerMasterSigner(
            isoDep = parameters.isoDep,
            cvc = parameters.cvc,
            masterSignerId = parameters.masterSignerId,
            walletType = parameters.walletType.ordinal,
            addressType = AddressType.NATIVE_SEGWIT.ordinal,
            index = parameters.index,
        )
    }

    class Data(
        isoDep: IsoDep,
        val cvc: String,
        val masterSignerId: String,
        val index: Int = -1,
        val walletType: WalletType
    ) : BaseNfcUseCase.Data(isoDep)
}