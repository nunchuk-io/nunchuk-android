package com.nunchuk.android.core.wallet

import android.os.Parcelable
import com.nunchuk.android.model.TxOutput
import kotlinx.parcelize.Parcelize

@Parcelize
data class InvoiceInfo(
    val amountSent: String,
    val confirmTime: String,
    val transactionId: String,
    val txOutputs: List<TxOutput>,
    val estimatedFee: String,
    val changeAddress: String,
    val changeAddressAmount: String,
    val note: String,
    val isReceive: Boolean,
) : Parcelable