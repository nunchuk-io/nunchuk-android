package com.nunchuk.android.usecase

import com.nunchuk.android.domain.di.IoDispatcher
import com.nunchuk.android.model.MiniscriptTemplateResult
import com.nunchuk.android.nativelib.NunchukNativeSdk
import com.nunchuk.android.type.AddressType
import kotlinx.coroutines.CoroutineDispatcher
import javax.inject.Inject

class CreateMiniscriptTemplateByCustomUseCase @Inject constructor(
    private val nunchukNativeSdk: NunchukNativeSdk,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) : UseCase<CreateMiniscriptTemplateByCustomUseCase.Params, MiniscriptTemplateResult>(ioDispatcher) {

    override suspend fun execute(parameters: Params): MiniscriptTemplateResult {
        return nunchukNativeSdk.createMiniscriptTemplateByCustom(
            input = parameters.template,
            addressType = parameters.addressType.ordinal
        )
    }

    data class Params(
        val template: String,
        val addressType: AddressType,
    )
}