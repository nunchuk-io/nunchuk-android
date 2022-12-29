package com.nunchuk.android.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
class InheritanceAdditional(
    val inheritance: Inheritance,
    val balance: Double
) : Parcelable