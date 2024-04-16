package com.nunchuk.android.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class TapSigner(
    val cardId: String = "",
    val version: String? = null,
    val isTestnet: Boolean = false,
    val birthHeight: Int = 0,
    val isInheritance: Boolean = false,
) : Parcelable