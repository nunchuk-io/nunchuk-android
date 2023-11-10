package com.nunchuk.android.core.data.model.payment

import com.google.gson.annotations.SerializedName

internal data class RecurringPaymentResponse(
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
    val transactionNote: String? = null
)

internal data class PaymentPayload(
    @SerializedName("amount")
    val amount: Double? = null,
    @SerializedName("currency")
    val currency: String? = null,
    @SerializedName("calculation_method")
    val calculationMethod: String? = null
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

