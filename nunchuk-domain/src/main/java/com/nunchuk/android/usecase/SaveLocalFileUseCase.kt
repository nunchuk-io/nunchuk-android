package com.nunchuk.android.usecase

import android.content.ContentValues
import android.content.Context
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.webkit.MimeTypeMap
import androidx.core.net.toUri
import com.nunchuk.android.domain.di.IoDispatcher
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import javax.inject.Inject

class SaveLocalFileUseCase @Inject constructor(
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
    @ApplicationContext private val context: Context
) : UseCase<SaveLocalFileUseCase.Params, Unit>(ioDispatcher) {

    override suspend fun execute(parameters: Params) {
        val content = if (parameters.fileContent.isEmpty()) {
            withContext(ioDispatcher) {
                FileInputStream(File(parameters.filePath)).readBytes()
            }
        } else {
            parameters.fileContent.toByteArray()
        }

        val fileName = parameters.fileName.ifEmpty {
            getFileNameFromPath(parameters.filePath)
        }

        val mimeType = getMimeType(fileName)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val contentValues = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                put(MediaStore.MediaColumns.MIME_TYPE, mimeType)
                put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
            }

            val resolver = context.contentResolver
            val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
            uri?.let {
                resolver.openOutputStream(it)?.use { outputStream ->
                    outputStream.write(content)
                }
            }
        } else {
            val downloadDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            val file = File(downloadDir, fileName)
            withContext(Dispatchers.IO) {
                FileOutputStream(file).use { fos ->
                    fos.write(content)
                }
            }
        }
    }

    private fun getFileNameFromPath(filePath: String): String {
        val file = File(filePath)
        return file.name
    }

    private fun getMimeType(fileName: String): String {
        val extension = MimeTypeMap.getFileExtensionFromUrl(fileName.toUri().toString())
        return MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension) ?: "application/octet-stream"
    }

    data class Params(
        val fileName: String = "",
        val fileContent: String = "",
        val filePath: String = ""
    )
}