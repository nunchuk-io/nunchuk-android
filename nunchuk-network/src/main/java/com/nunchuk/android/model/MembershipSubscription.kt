/**************************************************************************
 * This file is part of the Nunchuk software (https://nunchuk.io/)        *
 * Copyright (C) 2022, 2023 Nunchuk                                       *
 *                                                                        *
 * This program is free software; you can redistribute it and/or          *
 * modify it under the terms of the GNU General Public License            *
 * as published by the Free Software Foundation; either version 3         *
 * of the License, or (at your option) any later version.                 *
 *                                                                        *
 * This program is distributed in the hope that it will be useful,        *
 * but WITHOUT ANY WARRANTY; without even the implied warranty of         *
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the          *
 * GNU General Public License for more details.                           *
 *                                                                        *
 * You should have received a copy of the GNU General Public License      *
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.  *
 *                                                                        *
 **************************************************************************/

package com.nunchuk.android.model

import com.google.gson.annotations.SerializedName

data class MembershipSubscriptions(
    @SerializedName("subscriptions") val subscriptions: List<MembershipSubscription> = emptyList()
)

// Un comment when we need that information
data class MembershipSubscription(
    @SerializedName("subscription_id") val subscriptionId: String? = null,
//    @SerializedName("user_id") val userId: String? = null,
//    @SerializedName("plan_id") val planId: String? = null,
    @SerializedName("plan") val plan: Plan? = null,
//    @SerializedName("payment_method") val paymentMethod: String? = null,
//    @SerializedName("valid_from_utc_millis") val validFromUtcMillis: Long = 0,
    @SerializedName("valid_until_utc_millis") val validUntilUtcMillis: Long = 0,
    @SerializedName("grace_valid_until_utc_millis") val graceValidUntilUtcMillis: Long = 0L,
    @SerializedName("ended_at_utc_millis") val endedAtUtcMillis: Long = 0,
    @SerializedName("status") val status: String? = null,
//    @SerializedName("payment_details") val paymentDetails: PaymentDetails? = null
)

data class Plan(
//    @SerializedName("id") val id: String? = null,
//    @SerializedName("name") val name: String? = null,
    @SerializedName("slug") val slug: String? = null,
//    @SerializedName("description") val description: String? = null,
//    @SerializedName("price") val price: Int = 0,
//    @SerializedName("currency") val currency: String? = null,
//    @SerializedName("interval") val interval: String? = null,
//    @SerializedName("env") val env: String? = null,
//    @SerializedName("features") val features: List<Features> = emptyList()
)

data class PaymentDetails(
    @SerializedName("stripe_payment_method") val stripePaymentMethod: StripePaymentMethod? = null
)

data class StripePaymentMethod(
    @SerializedName("id") val id: String? = null,
    @SerializedName("billing_details") val billingDetails: BillingDetails? = BillingDetails(),
    @SerializedName("card") val card: Card? = Card(),
    @SerializedName("created") val created: Int? = null,
    @SerializedName("customer") val customer: String? = null,
    @SerializedName("livemode") val livemode: Boolean? = null,
    @SerializedName("type") val type: String? = null
)

data class Features(
    @SerializedName("id") val id: String? = null,
    @SerializedName("slug") val slug: String? = null,
    @SerializedName("display_name") val displayName: String? = null,
    @SerializedName("description") val description: String? = null
)

data class BillingDetails(
    @SerializedName("address") val address: Address? = Address(),
    @SerializedName("email") val email: String? = null,
    @SerializedName("name") val name: String? = null,
    @SerializedName("phone") val phone: String? = null
)

data class Card(
    @SerializedName("brand") val brand: String? = null,
    @SerializedName("country") val country: String? = null,
    @SerializedName("exp_month") val expMonth: Int = 0,
    @SerializedName("exp_year") val expYear: Int = 0,
    @SerializedName("fingerprint") val fingerprint: String? = null,
    @SerializedName("funding") val funding: String? = null,
    @SerializedName("generated_from") val generatedFrom: String? = null,
    @SerializedName("last4") val last4: String? = null,
    @SerializedName("wallet") val wallet: String? = null
)

data class Address(
    @SerializedName("city") val city: String? = null,
    @SerializedName("country") val country: String? = null,
    @SerializedName("line1") val line1: String? = null,
    @SerializedName("line2") val line2: String? = null,
    @SerializedName("postal_code") val postalCode: String? = null,
    @SerializedName("state") val state: String? = null
)