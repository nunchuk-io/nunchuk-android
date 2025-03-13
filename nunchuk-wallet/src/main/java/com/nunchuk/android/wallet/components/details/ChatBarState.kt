package com.nunchuk.android.wallet.components.details

enum class ChatBarState {
    EXPANDED,         // Chat bar is fully visible
    AUTO_COLLAPSED,   // Chat bar was collapsed automatically due to scrolling down
    USER_COLLAPSED    // Chat bar was collapsed manually by the user
}