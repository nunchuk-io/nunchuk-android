package com.nunchuk.android.usecase.membership

import com.nunchuk.android.domain.di.IoDispatcher
import com.nunchuk.android.model.SingleSigner
import com.nunchuk.android.nativelib.NunchukNativeSdk
import com.nunchuk.android.usecase.UseCase
import kotlinx.coroutines.CoroutineDispatcher
import javax.inject.Inject

class ParseKeystoneDummyTransaction @Inject constructor(
    private val nativeSdk: NunchukNativeSdk,
    @IoDispatcher ioDispatcher: CoroutineDispatcher,
) : UseCase<ParseKeystoneDummyTransaction.Param, String>(ioDispatcher) {

    override suspend fun execute(parameters: Param): String {
        return nativeSdk.parseKeystoneDummyTransaction(parameters.signer, parameters.qrs)
    }

    data class Param(val signer: SingleSigner, val qrs: List<String>)
}