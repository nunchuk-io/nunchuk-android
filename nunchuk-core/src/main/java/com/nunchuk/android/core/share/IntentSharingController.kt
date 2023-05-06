/**************************************************************************
 * This file is part of the Nunchuk software (https://nunchuk.io/)        *							          *
 * Copyright (C) 2022 Nunchuk								              *
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

package com.nunchuk.android.core.share

import android.app.Activity
import android.content.Intent
import android.net.Uri
import androidx.activity.result.ActivityResultLauncher
import androidx.core.content.FileProvider
import java.io.File

class IntentSharingController private constructor(
    private val activityContext: Activity,
    private val launcher: ActivityResultLauncher<Intent>? = null,
) {

    fun share(intent: Intent, title: String = "Nunchuk") {
        activityContext.startActivity(
            Intent.createChooser(
                intent,
                title,
            )
        )
    }

    fun shareFile(filePath: String) {
        val context = activityContext.applicationContext
        val intent = Intent(Intent.ACTION_SEND)
        val uri: Uri = FileProvider.getUriForFile(
            context,
            context.packageName.toString() + ".provider",
            File(filePath)
        )
        intent.apply {
            putExtra(Intent.EXTRA_STREAM, uri)
            setDataAndType(data, "*/*")
            intent.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION or Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        val createChooser = Intent.createChooser(intent, "Nunchuk")
        launcher?.launch(createChooser) ?: activityContext.startActivity(createChooser)
    }

    fun shareText(text: String) {
        share(Intent(Intent.ACTION_SEND).apply {
            putExtra(Intent.EXTRA_TEXT, text)
            type = "text/plain"
        })
    }

    companion object {
        fun from(
            activityContext: Activity,
            launcher: ActivityResultLauncher<Intent>? = null
        ) = IntentSharingController(activityContext, launcher)
    }
}