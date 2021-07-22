package com.nunchuk.android.contact.pending

private const val POSITION_RECEIVED = 0
private const val POSITION_SENT = 1

enum class PendingContactTab(val position: Int) {
    RECEIVED(POSITION_RECEIVED),
    SENT(POSITION_SENT)
}
