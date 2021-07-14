package com.nunchuk.android.messages.pending.receive

import com.nunchuk.android.messages.model.ReceiveContact

data class ReceivedState(val contacts: List<ReceiveContact> = emptyList())

sealed class ReceivedEvent