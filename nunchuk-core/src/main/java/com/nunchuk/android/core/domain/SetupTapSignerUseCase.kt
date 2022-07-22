package com.nunchuk.android.core.domain

import android.content.Context
import android.nfc.tech.IsoDep
import com.nunchuk.android.core.domain.data.WaitTapSignerUseCase
import com.nunchuk.android.core.domain.utils.NfcFile
import com.nunchuk.android.core.util.NFC_DEFAULT_NAME
import com.nunchuk.android.domain.di.IoDispatcher
import com.nunchuk.android.model.MasterSigner
import com.nunchuk.android.nativelib.NunchukNativeSdk
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.ensureActive
import javax.inject.Inject
import kotlin.coroutines.coroutineContext

@Suppress("BlockingMethodInNonBlockingContext")
class SetupTapSignerUseCase @Inject constructor(
    @IoDispatcher dispatcher: CoroutineDispatcher,
    @ApplicationContext private val context: Context,
    private val nunchukNativeSdk: NunchukNativeSdk,
    waitTapSignerUseCase: WaitTapSignerUseCase
) : BaseNfcUseCase<SetupTapSignerUseCase.Data, SetupTapSignerUseCase.Result>(dispatcher, waitTapSignerUseCase) {

    override suspend fun executeNfc(parameters: Data): Result {
        val tapStatus = nunchukNativeSdk.setupTapSigner(parameters.isoDep, parameters.oldCvc, parameters.newCvc, parameters.chainCode)
        coroutineContext.ensureActive()
        val masterSigner = nunchukNativeSdk.createTapSigner(parameters.isoDep, parameters.newCvc, NFC_DEFAULT_NAME)
        val filePath = NfcFile.storeBackupKeyToFile(context, tapStatus)
        return Result(filePath, masterSigner)
    }

    class Data(isoDep: IsoDep, val oldCvc: String, val newCvc: String, val chainCode: String) : BaseNfcUseCase.Data(isoDep)
    class Result(val backUpKeyPath: String, val masterSigner: MasterSigner)
}