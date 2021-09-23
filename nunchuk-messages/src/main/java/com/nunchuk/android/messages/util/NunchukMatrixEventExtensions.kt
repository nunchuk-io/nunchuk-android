package com.nunchuk.android.messages.util

import com.nunchuk.android.model.NunchukMatrixEvent

fun NunchukMatrixEvent.isLocalEvent()  = eventId.startsWith("\$local.")