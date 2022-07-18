package com.nunchuk.android.core.domain

import com.nunchuk.android.core.util.NFC_DEFAULT_NAME
import com.nunchuk.android.domain.di.IoDispatcher
import com.nunchuk.android.model.MasterSigner
import com.nunchuk.android.nativelib.NunchukNativeSdk
import com.nunchuk.android.usecase.UseCase
import kotlinx.coroutines.CoroutineDispatcher
import javax.inject.Inject

class ImportTapSignerUseCase @Inject constructor(
    @IoDispatcher dispatcher: CoroutineDispatcher,
    private val nunchukNativeSdk: NunchukNativeSdk,
) : UseCase<ImportTapSignerUseCase.Data, MasterSigner>(dispatcher) {
    override suspend fun execute(parameters: Data): MasterSigner {
        return nunchukNativeSdk.decryptBackUpKey(parameters.backUpKey, parameters.decryptionKey, NFC_DEFAULT_NAME)
    }

    class Data(val backUpKey: String, val decryptionKey: String)
}