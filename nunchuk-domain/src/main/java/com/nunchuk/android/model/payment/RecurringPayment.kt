package com.nunchuk.android.model.payment

import android.os.Parcelable
import com.nunchuk.android.model.FeeRate
import kotlinx.parcelize.Parcelize

@Parcelize
data class RecurringPayment(
    val id: String? = null,
    val name: String,
    val paymentType: RecurringPaymentType,
    val destinationType: PaymentDestinationType,
    val frequency: PaymentFrequency,
    val startDate: Long,
    val endDate: Long,
    val allowCosigning: Boolean,
    val note: String,
    val amount: Double,
    val feeRate: FeeRate,
    val currency: String? = null,
    val bsms: String? = null,
    val calculationMethod: PaymentCalculationMethod? = null,
    val addresses: List<String> = emptyList(),
) : Parcelable