package com.nunchuk.android.usecase

import com.nunchuk.android.domain.di.IoDispatcher
import com.nunchuk.android.model.SingleSigner
import com.nunchuk.android.nativelib.NunchukNativeSdk
import com.nunchuk.android.type.SignerType
import kotlinx.coroutines.CoroutineDispatcher
import javax.inject.Inject

class ParseJsonSignerUseCase @Inject constructor(
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
    private val nunchukNativeSdk: NunchukNativeSdk
) : UseCase<ParseJsonSignerUseCase.Params, List<SingleSigner>>(ioDispatcher) {
    override suspend fun execute(parameters: Params): List<SingleSigner> {
        return nunchukNativeSdk.parseJsonSigners(parameters.json, parameters.type)
    }

    data class Params(val json: String, val type: SignerType)
}