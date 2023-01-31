package com.nunchuk.android.model.matrix

import android.net.Uri

sealed class BaseRoomMediaType

data class RoomImageType(
    val displayName: String?,
    val size: Long,
    val mimeType: String?,
    val contentUri: Uri,
    val width: Int,
    val height: Int,
    val orientation: Int
) : BaseRoomMediaType()

data class RoomVideoType(
    val displayName: String?,
    val size: Long,
    val mimeType: String?,
    val contentUri: Uri,
    val width: Int,
    val height: Int,
    val orientation: Int,
    val duration: Long
): BaseRoomMediaType()

data class AttachmentType(
    val displayName: String?,
    val size: Long,
    val mimeType: String?,
    val contentUri: Uri,
) : BaseRoomMediaType()