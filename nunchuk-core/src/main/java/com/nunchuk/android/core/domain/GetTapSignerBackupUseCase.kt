package com.nunchuk.android.core.domain

import android.content.Context
import android.nfc.tech.IsoDep
import com.nunchuk.android.core.util.NFC_CARD_TIMEOUT
import com.nunchuk.android.domain.di.IoDispatcher
import com.nunchuk.android.nativelib.NunchukNativeSdk
import com.nunchuk.android.usecase.UseCase
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
import java.io.File
import java.io.IOException
import javax.inject.Inject

@Suppress("BlockingMethodInNonBlockingContext")
class GetTapSignerBackupUseCase @Inject constructor(
    @IoDispatcher dispatcher: CoroutineDispatcher,
    @ApplicationContext private val context: Context,
    private val nunchukNativeSdk: NunchukNativeSdk
) : UseCase<GetTapSignerBackupUseCase.Data, String>(dispatcher) {
    override suspend fun execute(parameters: Data): String {
        val card = parameters.isoDep
        card.timeout = NFC_CARD_TIMEOUT
        card.connect()
        try {
            if (card.isConnected) {
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
        } finally {
            runCatching { card.close() }
        }
        throw IOException("Can not connect nfc card")
    }

    data class Data(val isoDep: IsoDep, val cvc: String)
}