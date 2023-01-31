package com.nunchuk.android.messages.components.detail.media

import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.provider.MediaStore
import android.provider.OpenableColumns
import androidx.core.database.getLongOrNull
import androidx.core.database.getStringOrNull
import com.nunchuk.android.model.matrix.AttachmentType
import com.nunchuk.android.model.matrix.BaseRoomMediaType
import com.nunchuk.android.model.matrix.RoomImageType
import com.nunchuk.android.model.matrix.RoomVideoType
import org.matrix.android.sdk.api.util.MimeTypes.isMimeTypeImage
import org.matrix.android.sdk.api.util.MimeTypes.isMimeTypeVideo

fun List<Uri>.getSelectedMediaFiles(context: Context): List<BaseRoomMediaType> {
    return mapNotNull { selectedUri ->
        val mimeType = context.contentResolver.getType(selectedUri)
        when {
            mimeType.isMimeTypeVideo() -> selectedUri.toMultiPickerVideoType(context)
            mimeType.isMimeTypeImage() -> selectedUri.toMultiPickerImageType(context)
            else -> selectedUri.toAttachmentType(context)
        }
    }
}

internal fun Uri.toMultiPickerImageType(context: Context): RoomImageType? {
    val projection = arrayOf(
        MediaStore.Images.Media.DISPLAY_NAME,
        MediaStore.Images.Media.SIZE
    )

    return context.contentResolver.query(
        this,
        projection,
        null,
        null,
        null
    )?.use { cursor ->
        val nameColumn = cursor.getColumnIndexOrNull(MediaStore.Images.Media.DISPLAY_NAME) ?: return@use null
        val sizeColumn = cursor.getColumnIndexOrNull(MediaStore.Images.Media.SIZE) ?: return@use null

        if (cursor.moveToNext()) {
            val name = cursor.getStringOrNull(nameColumn)
            val size = cursor.getLongOrNull(sizeColumn) ?: 0

            val bitmap = ImageUtils.getBitmap(context, this)
            val orientation = ImageUtils.getOrientation(context, this)

            RoomImageType(
                name,
                size,
                context.contentResolver.getType(this),
                this,
                bitmap?.width ?: 0,
                bitmap?.height ?: 0,
                orientation
            )
        } else {
            null
        }
    }
}

internal fun Uri.toMultiPickerVideoType(context: Context): RoomVideoType? {
    val projection = arrayOf(
        MediaStore.Video.Media.DISPLAY_NAME,
        MediaStore.Video.Media.SIZE
    )

    return context.contentResolver.query(
        this,
        projection,
        null,
        null,
        null
    )?.use { cursor ->
        val nameColumn = cursor.getColumnIndexOrNull(MediaStore.Video.Media.DISPLAY_NAME) ?: return@use null
        val sizeColumn = cursor.getColumnIndexOrNull(MediaStore.Video.Media.SIZE) ?: return@use null

        if (cursor.moveToNext()) {
            val name = cursor.getStringOrNull(nameColumn)
            val size = cursor.getLongOrNull(sizeColumn) ?: 0
            var duration = 0L
            var width = 0
            var height = 0
            var orientation = 0

            context.contentResolver.openFileDescriptor(this, "r")?.use { pfd ->
                val mediaMetadataRetriever = MediaMetadataRetriever()
                mediaMetadataRetriever.setDataSource(pfd.fileDescriptor)
                duration = mediaMetadataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLong() ?: 0L
                width = mediaMetadataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)?.toInt() ?: 0
                height = mediaMetadataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)?.toInt() ?: 0
                orientation = mediaMetadataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_ROTATION)?.toInt() ?: 0
            }

            RoomVideoType(
                name,
                size,
                context.contentResolver.getType(this),
                this,
                width,
                height,
                orientation,
                duration
            )
        } else {
            null
        }
    }
}

internal fun Uri.toAttachmentType(context: Context) : AttachmentType? {
    val projection = arrayOf(
        MediaStore.Video.Media.DISPLAY_NAME,
        MediaStore.Video.Media.SIZE
    )
  return context.contentResolver.query(this, projection, null, null, null)
        ?.use { cursor ->
            val nameColumn = cursor.getColumnIndexOrNull(OpenableColumns.DISPLAY_NAME) ?: return@use null
            val sizeColumn = cursor.getColumnIndexOrNull(OpenableColumns.SIZE) ?: return@use null
            if (cursor.moveToFirst()) {
                val name = cursor.getStringOrNull(nameColumn)
                val size = cursor.getLongOrNull(sizeColumn) ?: 0

                AttachmentType(
                    name,
                    size,
                    context.contentResolver.getType(this),
                    this
                )
            } else {
                null
            }
        }
}