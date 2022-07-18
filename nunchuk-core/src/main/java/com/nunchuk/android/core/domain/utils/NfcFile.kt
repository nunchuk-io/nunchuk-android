package com.nunchuk.android.core.domain.utils

import android.content.Context
import com.nunchuk.android.model.TapSignerStatus
import java.io.File

object NfcFile {
    fun storeBackupKeyToFile(context: Context, tapStatus: TapSignerStatus) : String {
        val file = File(context.filesDir, "backup.${System.currentTimeMillis()}.${tapStatus.ident.orEmpty()}.aes").apply {
            if (exists().not()) {
                createNewFile()
            }
        }
        file.outputStream().use { it.write(tapStatus.backupKey) }
        return file.path
    }
}