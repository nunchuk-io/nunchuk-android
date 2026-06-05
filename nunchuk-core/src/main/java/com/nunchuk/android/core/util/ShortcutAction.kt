package com.nunchuk.android.core.util

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

sealed interface ShortcutAction : Parcelable {
    @Parcelize
    data object Send : ShortcutAction

    @Parcelize
    data object Receive : ShortcutAction
}
