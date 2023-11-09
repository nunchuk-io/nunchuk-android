package com.nunchuk.android.main.membership.byzantine.payment

import android.os.Parcelable
import com.nunchuk.android.model.SpendingCurrencyUnit
import kotlinx.parcelize.Parcelize


@Parcelize
data class RecurringPaymentConfig(
    val name: String = "",
    val amount: String = "",
    val unit: SpendingCurrencyUnit = SpendingCurrencyUnit.CURRENCY_UNIT,
    val currency: String = "",
    val useAmount: Boolean = true,
    val calculatePercentageJustInTime: Boolean = false,
    val addresses: List<String> = emptyList(),
) : Parcelable