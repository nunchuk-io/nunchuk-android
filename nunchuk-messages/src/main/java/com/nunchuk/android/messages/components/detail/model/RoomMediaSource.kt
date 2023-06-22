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

package com.nunchuk.android.messages.components.detail.model

import com.nunchuk.android.messages.components.detail.NunchukMedia
import org.matrix.android.sdk.api.session.crypto.attachments.ElementToDecrypt
import org.matrix.android.sdk.api.session.crypto.attachments.toElementToDecrypt
import org.matrix.android.sdk.api.session.room.model.message.MessageImageContent
import org.matrix.android.sdk.api.session.room.model.message.MessageVideoContent
import org.matrix.android.sdk.api.session.room.model.message.getFileName
import org.matrix.android.sdk.api.session.room.model.message.getFileUrl

sealed class RoomMediaSource(override val eventId: String, open val allowNonMxcUrls: Boolean) :
    NunchukMedia {
    data class Image(
        override val eventId: String,
        override val allowNonMxcUrls: Boolean,
        val content: MessageImageContent,
        override val error: String?,
    ) : RoomMediaSource(eventId, allowNonMxcUrls) {
        override val filename: String
            get() = content.getFileName()
        override val mimeType: String?
            get() = content.mimeType
        override val url: String?
            get() = content.url
        override val elementToDecrypt: ElementToDecrypt?
            get() = content.encryptedFileInfo?.toElementToDecrypt()
    }

    data class AnimatedImage(
        override val eventId: String,
        override val allowNonMxcUrls: Boolean,
        val content: MessageImageContent,
        override val error: String?,
    ) : RoomMediaSource(eventId, allowNonMxcUrls) {
        override val filename: String
            get() = content.getFileName()
        override val mimeType: String?
            get() = content.mimeType
        override val url: String?
            get() = content.url
        override val elementToDecrypt: ElementToDecrypt?
            get() = content.encryptedFileInfo?.toElementToDecrypt()
    }

    data class Video(
        override val eventId: String,
        override val allowNonMxcUrls: Boolean,
        val content: MessageVideoContent,
        val thumbnail: NunchukMedia,
        override val error: String?,
    ) : RoomMediaSource(eventId, allowNonMxcUrls) {
        override val filename: String
            get() = content.body
        override val mimeType: String?
            get() = content.mimeType
        override val url: String?
            get() = content.getFileUrl()
        override val elementToDecrypt: ElementToDecrypt?
            get() = content.encryptedFileInfo?.toElementToDecrypt()
    }
}