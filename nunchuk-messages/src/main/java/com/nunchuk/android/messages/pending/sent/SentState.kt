package com.nunchuk.android.messages.pending.sent

import com.nunchuk.android.messages.model.SentContact

data class SentState(val contacts: List<SentContact> = emptyList())

sealed class SentEvent