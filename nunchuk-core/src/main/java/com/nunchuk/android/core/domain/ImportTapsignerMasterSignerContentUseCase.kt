package com.nunchuk.android.core.domain

import com.nunchuk.android.domain.di.IoDispatcher
import com.nunchuk.android.model.MasterSigner
import com.nunchuk.android.nativelib.NunchukNativeSdk
import com.nunchuk.android.usecase.UseCase
import kotlinx.coroutines.CoroutineDispatcher
import javax.inject.Inject

class ImportTapsignerMasterSignerContentUseCase @Inject constructor(
    @IoDispatcher dispatcher: CoroutineDispatcher,
    private val nunchukNativeSdk: NunchukNativeSdk,
) : UseCase<ImportTapsignerMasterSignerContentUseCase.Param, MasterSigner>(dispatcher) {
    override suspend fun execute(parameters: Param): MasterSigner {
        return nunchukNativeSdk.importTapsignerMasterSignerContent(
            content = parameters.content,
            backUpKey = parameters.backUpKey,
            rawName = parameters.rawName
        )
    }

    class Param(val content: ByteArray, val backUpKey: String, val rawName: String)
}