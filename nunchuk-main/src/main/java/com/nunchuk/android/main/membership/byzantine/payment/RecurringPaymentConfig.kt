package com.nunchuk.android.main.membership.byzantine.payment

import android.os.Parcelable
import com.nunchuk.android.model.FeeRate
import com.nunchuk.android.model.SpendingCurrencyUnit
import com.nunchuk.android.model.payment.PaymentFrequency
import kotlinx.parcelize.Parcelize


@Parcelize
data class RecurringPaymentConfig(
    val name: String = "",
    val amount: String = "",
    val unit: SpendingCurrencyUnit = SpendingCurrencyUnit.CURRENCY_UNIT,
    val useAmount: Boolean = true,
    val calculatePercentageJustInTime: Boolean? = null,
    val bsms: String? = null,
    val addresses: List<String> = emptyList(),
    val frequency: PaymentFrequency? = null,
    val startDate: Long = 0,
    val endDate: Long = 0,
    val note: String = "",
    val noEndDate: Boolean = false,
    val isCosign: Boolean? = null,
    val feeRate: FeeRate = FeeRate.PRIORITY,
) : Parcelable