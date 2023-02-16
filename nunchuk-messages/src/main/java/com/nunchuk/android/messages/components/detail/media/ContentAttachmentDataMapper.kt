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