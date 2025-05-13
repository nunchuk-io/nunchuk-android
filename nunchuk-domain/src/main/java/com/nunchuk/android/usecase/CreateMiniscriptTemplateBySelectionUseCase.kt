package com.nunchuk.android.usecase

import com.nunchuk.android.domain.di.IoDispatcher
import com.nunchuk.android.nativelib.NunchukNativeSdk
import com.nunchuk.android.type.AddressType
import kotlinx.coroutines.CoroutineDispatcher
import javax.inject.Inject

class CreateMiniscriptTemplateBySelectionUseCase @Inject constructor(
    private val nunchukNativeSdk: NunchukNativeSdk,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) : UseCase<CreateMiniscriptTemplateBySelectionUseCase.Params, String>(ioDispatcher) {

    override suspend fun execute(parameters: Params): String {
        return nunchukNativeSdk.createMiniscriptTemplateBySelection(
            multisignType = parameters.multisignType,
            m = parameters.m,
            n = parameters.n,
            newM = parameters.newM,
            newN = parameters.newN,
            timelockType = parameters.timelockType,
            timeUnit = parameters.timeUnit,
            time = parameters.time,
            addressType = parameters.addressType.ordinal,
            reuseSigner = parameters.reuseSigner,
        )
    }

    data class Params(
        val multisignType: Int,
        val m: Int,
        val n: Int,
        val newM: Int,
        val newN: Int,
        val timelockType: Int,
        val timeUnit: Int,
        val time: Long,
        val addressType: AddressType,
        val reuseSigner: Boolean,
    )
}