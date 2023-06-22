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

package com.nunchuk.android.log

import android.app.Application
import android.content.Context
import timber.log.Timber
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

class FileLogTree(application: Application) : Timber.DebugTree() {
    private val file = getLogFile(application)

    override fun log(priority: Int, tag: String?, message: String, t: Throwable?) {
        super.log(priority, tag, message, t)
        if (priority != FILE_LOG_PRIORITY) return
        try {
            FileOutputStream(file, true).use {
                it.write("${SIMPLE_DATE.format(Date())} - $message\n".toByteArray(Charsets.UTF_8))
            }
        } catch (e: Exception) {
        }
    }

    companion object {
        const val FILE_LOG_PRIORITY = 10
        private val SIMPLE_DATE = SimpleDateFormat("dd/MM/yyyy hh:mm:ss:SSS", Locale.ENGLISH)

        fun getLogFile(context: Context) = File(context.filesDir, "custom_log.txt").apply {
            if (exists().not()) {
                createNewFile()
            }
        }
    }
}

fun fileLog(message: String) = Timber.log(message = message, priority = FileLogTree.FILE_LOG_PRIORITY)