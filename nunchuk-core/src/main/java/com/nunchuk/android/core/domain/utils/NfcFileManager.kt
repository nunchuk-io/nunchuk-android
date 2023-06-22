/**************************************************************************
 * This file is part of the Nunchuk software (https://nunchuk.io/)        *
 * Copyright (C) 2022, 2023 Nunchuk                                       *
 *                                                                        *
 * This program is free software; you can redistribute it and/or          *
 * modify it under the terms of the GNU General Public License            *
 * as published by the Free Software Foundation; either version 3         *
 * of the License, or (at your option) any later version.                 *
 *                                                                        *
 * This program is distributed in the hope that it will be useful,        *
 * but WITHOUT ANY WARRANTY; without even the implied warranty of         *
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the          *
 * GNU General Public License for more details.                           *
 *                                                                        *
 * You should have received a copy of the GNU General Public License      *
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.  *
 *                                                                        *
 **************************************************************************/

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