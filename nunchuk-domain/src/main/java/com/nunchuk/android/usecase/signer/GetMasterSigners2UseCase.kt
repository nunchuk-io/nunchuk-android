package com.nunchuk.android.usecase.signer

import com.nunchuk.android.domain.di.IoDispatcher
import com.nunchuk.android.model.MasterSigner
import com.nunchuk.android.nativelib.NunchukNativeSdk
import com.nunchuk.android.usecase.UseCase
import kotlinx.coroutines.CoroutineDispatcher
import javax.inject.Inject

class GetMasterSigners2UseCase @Inject constructor(
    private val nativeSdk: NunchukNativeSdk,
    @IoDispatcher ioDispatcher: CoroutineDispatcher
) : UseCase<Unit, List<MasterSigner>>(ioDispatcher) {

    override suspend fun execute(parameters: Unit): List<MasterSigner> {
        return nativeSdk.getMasterSigners().filter { it.isVisible }
    }
}