package com.nunchuk.android.core.data.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class TxReceipt(val address: String, val amount: Double) : Parcelable