package com.nunchuk.android.messages.components.detail

import org.matrix.android.sdk.api.session.crypto.attachments.ElementToDecrypt

interface NunchukMedia {
    val filename: String
    val eventId: String
    val mimeType: String?
    val url: String?
    val elementToDecrypt: ElementToDecrypt?
    val error: String?
}