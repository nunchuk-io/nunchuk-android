package com.nunchuk.android.model.payment

import androidx.annotation.Keep

@Keep
enum class PaymentDestinationType {
    DESTINATION_WALLET, WHITELISTED_ADDRESSES
}

val String?.toPaymentDestinationType: PaymentDestinationType
    get() = PaymentDestinationType.values().find { it.name == this } ?: PaymentDestinationType.DESTINATION_WALLET