package com.nunchuk.android.core.data.model.payment

import com.google.gson.annotations.SerializedName
import com.nunchuk.android.model.payment.RecurringPayment
import com.nunchuk.android.model.payment.toPaymentCalculationMethod
import com.nunchuk.android.model.payment.toPaymentDestinationType
import com.nunchuk.android.model.payment.toPaymentFrequency
import com.nunchuk.android.model.payment.toRecurringPaymentType

internal data class RecurringPaymentResponse(
    @SerializedName("id")
    val id: String? = null,
    @SerializedName("name")
    val name: String? = null,
    @SerializedName("payment_type")
    val paymentType: String? = null,
    @SerializedName("payment_payload")
    val paymentPayload: PaymentPayload? = null,
    @SerializedName("destination_type")
    val destinationType: String? = null,
    @SerializedName("destination_payload")
    val destinationPayload: DestinationPayload? = null,
    @SerializedName("frequency")
    val frequency: String? = null,
    @SerializedName("start_date_millis")
    val startDateMillis: Long = 0L,
    @SerializedName("end_date_millis")
    val endDateMillis: Long = 0L,
    @SerializedName("allow_cosigning")
    val allowCosigning: Boolean? = null,
    @SerializedName("transaction_note")
    val transactionNote: String? = null,
)

internal data class PaymentPayload(
    @SerializedName("amount")
    val amount: Double? = null,
    @SerializedName("currency")
    val currency: String? = null,
    @SerializedName("calculation_method")
    val calculationMethod: String? = null,
)

internal data class DestinationPayload(
    @SerializedName("bsms")
    val bsms: String? = null,
    @SerializedName("first_address")
    val firstAddress: String? = null,
    @SerializedName("addresses")
    val addresses: List<String>? = null,
    @SerializedName("current_index")
    val currentIndex: Int = 0,
)

internal fun RecurringPaymentResponse.toModel() = RecurringPayment(
    name = name.orEmpty(),
    paymentType = paymentType.toRecurringPaymentType,
    destinationType = destinationType.toPaymentDestinationType,
    frequency = frequency.toPaymentFrequency,
    startDate = startDateMillis,
    endDate = endDateMillis,
    allowCosigning = allowCosigning ?: false,
    note = transactionNote.orEmpty(),
    amount = paymentPayload?.amount ?: 0.0,
    currency = paymentPayload?.currency,
    calculationMethod = paymentPayload?.calculationMethod.toPaymentCalculationMethod,
    addresses = destinationPayload?.addresses.orEmpty(),
    bsms = destinationPayload?.bsms,
    id = id
)

