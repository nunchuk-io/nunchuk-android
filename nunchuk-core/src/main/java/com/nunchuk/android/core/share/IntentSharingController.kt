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

import android.annotation.SuppressLint
import android.app.Activity
import android.app.PendingIntent
import android.app.PendingIntent.*
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.net.Uri
import android.os.Build
import androidx.core.content.FileProvider
import java.io.File

class IntentSharingController private constructor(val activityContext: Activity) {

    private val receiver: Intent = Intent(activityContext, IntentSharingReceiver::class.java)

    private val pendingIntent: PendingIntent = getBroadcast(activityContext, 0, receiver, FLAG_IMMUTABLE or FLAG_UPDATE_CURRENT)

    fun share(intent: Intent, title: String = "Nunchuk") {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP_MR1) {
            activityContext.startActivity(Intent.createChooser(intent, title, pendingIntent.intentSender))
        }
    }

    @SuppressLint("QueryPermissionsNeeded", "NewApi")
    fun shareFile(filePath: String) {
        val context = activityContext.applicationContext
        val intent = Intent(Intent.ACTION_SEND)
        val uri: Uri = FileProvider.getUriForFile(context, context.packageName.toString() + ".provider", File(filePath))
        intent.apply {
            putExtra(Intent.EXTRA_STREAM, uri)
            data = uri
            type = "*/*"
        }
        val createChooser = Intent.createChooser(intent, "Nunchuk", pendingIntent.intentSender)

        val resolveInfoList: List<ResolveInfo> = context.packageManager.queryIntentActivities(createChooser, PackageManager.MATCH_DEFAULT_ONLY)

        resolveInfoList
            .map { it.activityInfo.packageName }
            .forEach { context.grantUriPermission(it, uri, Intent.FLAG_GRANT_WRITE_URI_PERMISSION or Intent.FLAG_GRANT_READ_URI_PERMISSION) }

        activityContext.startActivity(createChooser)
    }

    fun shareText(text: String) {
        share(Intent(Intent.ACTION_SEND).apply {
            putExtra(Intent.EXTRA_TEXT, text)
            type = "text/plain"
        })
    }

    companion object {
        fun from(activityContext: Activity) = IntentSharingController(activityContext)
    }

}