package com.nunchuk.android.messages.components.detail.media

import com.nunchuk.android.model.matrix.AttachmentType
import com.nunchuk.android.model.matrix.BaseRoomMediaType
import com.nunchuk.android.model.matrix.RoomImageType
import com.nunchuk.android.model.matrix.RoomVideoType
import org.matrix.android.sdk.api.session.content.ContentAttachmentData
import timber.log.Timber

fun BaseRoomMediaType.toContentAttachmentData(): ContentAttachmentData {
    return when (this) {
        is RoomImageType -> toContentAttachmentData()
        is RoomVideoType -> toContentAttachmentData()
        is AttachmentType -> toContentAttachmentData()
    }
}

private fun RoomImageType.toContentAttachmentData(): ContentAttachmentData {
    if (mimeType == null) Timber.w("No mimeType")
    return ContentAttachmentData(
        mimeType = mimeType,
        type = ContentAttachmentData.Type.IMAGE,
        name = displayName,
        size = size,
        height = height.toLong(),
        width = width.toLong(),
        exifOrientation = orientation,
        queryUri = contentUri
    )
}

private fun RoomVideoType.toContentAttachmentData(): ContentAttachmentData {
    if (mimeType == null) Timber.w("No mimeType")
    return ContentAttachmentData(
        mimeType = mimeType,
        type = ContentAttachmentData.Type.VIDEO,
        size = size,
        height = height.toLong(),
        width = width.toLong(),
        duration = duration,
        name = displayName,
        queryUri = contentUri
    )
}

private fun AttachmentType.toContentAttachmentData(): ContentAttachmentData {
    if (mimeType == null) Timber.w("No mimeType")
    return ContentAttachmentData(
        mimeType = mimeType,
        type = ContentAttachmentData.Type.FILE,
        size = size,
        name = displayName,
        queryUri = contentUri
    )
}