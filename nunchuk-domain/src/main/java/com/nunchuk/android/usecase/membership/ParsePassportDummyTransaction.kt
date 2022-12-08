package com.nunchuk.android.usecase.membership

import com.nunchuk.android.domain.di.IoDispatcher
import com.nunchuk.android.model.SingleSigner
import com.nunchuk.android.nativelib.NunchukNativeSdk
import com.nunchuk.android.usecase.UseCase
import kotlinx.coroutines.CoroutineDispatcher
import javax.inject.Inject

class ParsePassportDummyTransaction @Inject constructor(
    private val nativeSdk: NunchukNativeSdk,
    @IoDispatcher ioDispatcher: CoroutineDispatcher,
) : UseCase<ParsePassportDummyTransaction.Param, String>(ioDispatcher) {

    override suspend fun execute(parameters: Param): String {
        val psbt = nativeSdk.parsePassportDummyTransaction(parameters.qrs)
        return nativeSdk.getDummyTransactionSignature(parameters.signer, psbt)
    }

    data class Param(val signer: SingleSigner, val qrs: List<String>)
}