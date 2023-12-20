package com.nunchuk.android.main.membership.byzantine.payment

import androidx.compose.ui.tooling.preview.datasource.CollectionPreviewParameterProvider
import com.nunchuk.android.model.FeeRate
import com.nunchuk.android.model.payment.PaymentDestinationType
import com.nunchuk.android.model.payment.PaymentFrequency
import com.nunchuk.android.model.payment.RecurringPayment
import com.nunchuk.android.model.payment.RecurringPaymentType

internal class RecurringPaymentProvider : CollectionPreviewParameterProvider<RecurringPayment>(
    listOf(
        RecurringPayment(
            name = "Test",
            amount = 100.0,
            frequency = PaymentFrequency.DAILY,
            currency = "USD",
            startDate = System.currentTimeMillis(),
            endDate = System.currentTimeMillis(),
            allowCosigning = false,
            note = "",
            paymentType = RecurringPaymentType.PERCENTAGE,
            destinationType = PaymentDestinationType.WHITELISTED_ADDRESSES,
            addresses = listOf("xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx"),
            feeRate = FeeRate.PRIORITY,
        )
    )
)