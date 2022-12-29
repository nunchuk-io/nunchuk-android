package com.nunchuk.android.core.domain.utils

import android.content.Context
import android.util.Base64
import com.nunchuk.android.model.TapSignerStatus
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NfcFileManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    fun storeBackupKeyToFile(tapStatus: TapSignerStatus): String {
        val file = File(
            context.filesDir,
            "backup.${System.currentTimeMillis()}.${tapStatus.ident.orEmpty()}.aes"
        ).apply {
            if (exists().not()) {
                createNewFile()
            }
        }
        file.outputStream().use { it.write(tapStatus.backupKey) }
        return file.path
    }

    fun storeServerBackupKeyToFile(id: String, backUpBase64: String): String {
        val file = File(context.filesDir, "${id}.aes").apply {
            if (exists().not()) {
                createNewFile()
            }
        }
        file.outputStream().use { it.write(Base64.decode(backUpBase64, Base64.DEFAULT)) }
        return file.path
    }

    fun buildFilePath(id: String): String = File(context.filesDir, "${id}.aes").path
}