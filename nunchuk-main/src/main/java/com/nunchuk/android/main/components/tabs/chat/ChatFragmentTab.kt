package com.nunchuk.android.main.components.tabs.chat

import androidx.annotation.StringRes
import com.nunchuk.android.main.R

private const val POSITION_MESSAGES = 0
private const val POSITION_CONTACTS = 1

enum class ChatFragmentTab(val position: Int, @StringRes val labelId: Int) {
    MESSAGES(POSITION_MESSAGES, R.string.nc_title_message),
    CONTACTS(POSITION_CONTACTS, R.string.nc_title_contacts)
}
