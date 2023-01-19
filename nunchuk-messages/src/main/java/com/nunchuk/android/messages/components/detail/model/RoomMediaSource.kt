package com.nunchuk.android.messages.components.detail.model

import org.matrix.android.sdk.api.session.room.model.message.MessageImageContent
import org.matrix.android.sdk.api.session.room.model.message.MessageVideoContent

sealed class RoomMediaSource(open val eventId: String, open val allowNonMxcUrls: Boolean) {
    data class Image(override val eventId: String, override val allowNonMxcUrls: Boolean, val content: MessageImageContent) : RoomMediaSource(eventId, allowNonMxcUrls)
    data class AnimatedImage(override val eventId: String, override val allowNonMxcUrls: Boolean, val content: MessageImageContent) : RoomMediaSource(eventId, allowNonMxcUrls)
    data class Video(override val eventId: String, override val allowNonMxcUrls: Boolean, val content: MessageVideoContent) : RoomMediaSource(eventId, allowNonMxcUrls)
}