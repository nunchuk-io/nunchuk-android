package com.nunchuk.android.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class ByzantineWalletConfig(
    val m: Int,
    val n: Int,
    val requiredServerKey: Boolean,
    val allowInheritance: Boolean,
) : Parcelable