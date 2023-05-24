package com.nunchuk.android.usecase

import com.nunchuk.android.domain.di.IoDispatcher
import com.nunchuk.android.nativelib.NunchukNativeSdk
import kotlinx.coroutines.CoroutineDispatcher
import javax.inject.Inject

class CheckAddressValidUseCase @Inject constructor(
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
    private val nativeSdk: NunchukNativeSdk
) : UseCase<CheckAddressValidUseCase.Params, List<String>>(ioDispatcher) {

    override suspend fun execute(parameters: Params): List<String> {
        val invalidAddressList = arrayListOf<String>()
        parameters.addresses.forEach {
            if (nativeSdk.isValidAddress(it).not()) invalidAddressList.add(it)
        }
        return invalidAddressList
    }

    data class Params(
        val addresses: List<String>
    )
}