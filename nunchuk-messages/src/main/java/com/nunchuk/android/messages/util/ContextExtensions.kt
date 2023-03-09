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

package com.nunchuk.android.messages.util

import android.app.Activity
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.view.View
import android.webkit.MimeTypeMap
import androidx.core.content.ContextCompat
import com.nunchuk.android.messages.R
import com.nunchuk.android.widget.NCToastMessage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okio.buffer
import okio.sink
import okio.source
import org.matrix.android.sdk.api.extensions.tryOrNull
import org.matrix.android.sdk.api.util.MimeTypes.isMimeTypeAudio
import org.matrix.android.sdk.api.util.MimeTypes.isMimeTypeImage
import org.matrix.android.sdk.api.util.MimeTypes.isMimeTypeVideo
import timber.log.Timber
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

fun View.bindNotificationBackground(highlight: Boolean) {
    background = if (highlight) {
        ContextCompat.getDrawable(context, R.drawable.nc_slime_tint_background)
    } else {
        ContextCompat.getDrawable(context, R.drawable.nc_rounded_whisper_disable_background)
    }
}

private fun appendTimeToFilename(name: String): String {
    val dateExtension = SimpleDateFormat("yyyyMMddHHmmss", Locale.getDefault()).format(Date())
    if (!name.contains(".")) return name + "_" + dateExtension

    val filename = name.substringBeforeLast(".")
    val fileExtension = name.substringAfterLast(".")

    return """${filename}_$dateExtension.$fileExtension"""
}

suspend fun saveMedia(
    context: Context,
    file: File,
    title: String,
    mediaMimeType: String?,
    currentTimeMillis: Long = System.currentTimeMillis()
) {
    withContext(Dispatchers.IO) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val filename = appendTimeToFilename(title)

            val values = ContentValues().apply {
                put(MediaStore.Images.Media.TITLE, filename)
                put(MediaStore.Images.Media.DISPLAY_NAME, filename)
                put(MediaStore.Images.Media.MIME_TYPE, mediaMimeType)
                put(MediaStore.Images.Media.DATE_ADDED, currentTimeMillis)
                put(MediaStore.Images.Media.DATE_TAKEN, currentTimeMillis)
            }
            val externalContentUri = when {
                mediaMimeType?.isMimeTypeImage() == true -> MediaStore.Images.Media.EXTERNAL_CONTENT_URI
                mediaMimeType?.isMimeTypeVideo() == true -> MediaStore.Video.Media.EXTERNAL_CONTENT_URI
                mediaMimeType?.isMimeTypeAudio() == true -> MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
                else -> MediaStore.Downloads.EXTERNAL_CONTENT_URI
            }

            val uri = context.contentResolver.insert(externalContentUri, values)
            if (uri == null) {
                throw IllegalStateException("Can not create externalContentUri")
            } else {
                val source = file.inputStream().source().buffer()
                context.contentResolver.openOutputStream(uri)?.sink()?.buffer()?.let { sink ->
                    source.use { input ->
                        sink.use { output ->
                            output.writeAll(input)
                        }
                    }
                }
            }
        } else {
            saveMediaLegacy(context, mediaMimeType, title, file, currentTimeMillis)
        }
    }
}
private fun saveMediaLegacy(
    context: Context,
    mediaMimeType: String?,
    title: String,
    file: File,
    currentTimeMillis: Long
) {
    val state = Environment.getExternalStorageState()
    if (Environment.MEDIA_MOUNTED != state) {
        throw IllegalStateException("state differ Environment.MEDIA_MOUNTED")
    }

    val dest = when {
        mediaMimeType?.isMimeTypeImage() == true -> Environment.DIRECTORY_PICTURES
        mediaMimeType?.isMimeTypeVideo() == true -> Environment.DIRECTORY_MOVIES
        mediaMimeType?.isMimeTypeAudio() == true -> Environment.DIRECTORY_MUSIC
        else -> Environment.DIRECTORY_DOWNLOADS
    }
    val downloadDir = Environment.getExternalStoragePublicDirectory(dest)
        val outputFilename = if (title.substringAfterLast('.', "").isEmpty()) {
            val extension = mediaMimeType?.let { MimeTypeMap.getSingleton().getExtensionFromMimeType(it) }
            "$title.$extension"
        } else {
            title
        }
        val savedFile = saveFileIntoLegacy(file, downloadDir, outputFilename, currentTimeMillis)
        if (savedFile != null) {
            addToGallery(savedFile, mediaMimeType, context)
        }
}

fun saveFileIntoLegacy(sourceFile: File, dstDirPath: File, outputFilename: String?, currentTimeMillis: Long): File? {
    // defines another name for the external media
    var dstFileName: String

    // build a filename is not provided
    if (null == outputFilename) {
        // extract the file extension from the uri
        val dotPos = sourceFile.name.lastIndexOf(".")
        var fileExt = ""
        if (dotPos > 0) {
            fileExt = sourceFile.name.substring(dotPos)
        }
        dstFileName = "vector_$currentTimeMillis$fileExt"
    } else {
        dstFileName = outputFilename
    }

    // remove dangerous characters from the filename
    dstFileName = dstFileName.replace(Regex("""[/\\]"""), "_")

    var dstFile = File(dstDirPath, dstFileName)

    // if the file already exists, append a marker
    if (dstFile.exists()) {
        var baseFileName = dstFileName
        var fileExt = ""
        val lastDotPos = dstFileName.lastIndexOf(".")
        if (lastDotPos > 0) {
            baseFileName = dstFileName.substring(0, lastDotPos)
            fileExt = dstFileName.substring(lastDotPos)
        }
        var counter = 1
        while (dstFile.exists()) {
            dstFile = File(dstDirPath, "$baseFileName($counter)$fileExt")
            counter++
        }
    }

    // Copy source file to destination
    var inputStream: FileInputStream? = null
    var outputStream: FileOutputStream? = null
    try {
        dstFile.createNewFile()
        inputStream = sourceFile.inputStream()
        outputStream = dstFile.outputStream()
        val buffer = ByteArray(1024 * 10)
        var len: Int
        while (inputStream.read(buffer).also { len = it } != -1) {
            outputStream.write(buffer, 0, len)
        }
        return dstFile
    } catch (failure: Throwable) {
        return null
    } finally {
        // Close resources
        tryOrNull { inputStream?.close() }
        tryOrNull { outputStream?.close() }
    }
}

private fun addToGallery(savedFile: File, mediaMimeType: String?, context: Context) {
    // MediaScannerConnection provides a way for applications to pass a newly created or downloaded media file to the media scanner service.
    var mediaConnection: MediaScannerConnection? = null
    val mediaScannerConnectionClient: MediaScannerConnection.MediaScannerConnectionClient = object : MediaScannerConnection.MediaScannerConnectionClient {
        override fun onMediaScannerConnected() {
            mediaConnection?.scanFile(savedFile.path, mediaMimeType)
        }

        override fun onScanCompleted(path: String, uri: Uri?) {
            if (path == savedFile.path) mediaConnection?.disconnect()
        }
    }
    mediaConnection = MediaScannerConnection(context, mediaScannerConnectionClient).apply { connect() }
}

fun Activity.safeStartActivity(intent: Intent) {
    try {
        startActivity(intent)
    } catch (e: Exception) {
        NCToastMessage(this).showError("Couldn't find apps to open this file")
        Timber.e(e)
    }
}