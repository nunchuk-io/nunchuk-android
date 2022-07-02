package com.nunchuk.android.core.domain

import android.content.Context
import android.nfc.tech.IsoDep
import com.nunchuk.android.core.domain.data.WaitTapSignerUseCase
import com.nunchuk.android.domain.di.IoDispatcher
import com.nunchuk.android.nativelib.NunchukNativeSdk
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
import java.io.File
import javax.inject.Inject

@Suppress("BlockingMethodInNonBlockingContext")
class GetTapSignerBackupUseCase @Inject constructor(
    @IoDispatcher dispatcher: CoroutineDispatcher,
    @ApplicationContext private val context: Context,
    private val nunchukNativeSdk: NunchukNativeSdk,
    waitTapSignerUseCase: WaitTapSignerUseCase
) : BaseNfcUseCase<GetTapSignerBackupUseCase.Data, String>(dispatcher, waitTapSignerUseCase) {

    override fun executeNfc(parameters: Data): String {
        val tapStatus = nunchukNativeSdk.getBackupTapSignerKey(
            isoDep = parameters.isoDep,
            cvc = parameters.cvc
        )
        val file = File(context.filesDir, "backup.${tapStatus.ident.orEmpty()}.${System.currentTimeMillis()}.aes").apply {
            if (exists().not()) {
                createNewFile()
            }
        }
        file.outputStream().use { it.write(tapStatus.backupKey) }
        return file.path
    }

    class Data(isoDep: IsoDep, val cvc: String) : BaseNfcUseCase.Data(isoDep)
}