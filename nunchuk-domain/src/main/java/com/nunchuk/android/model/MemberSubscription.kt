package com.nunchuk.android.model

data class MemberSubscription(
    val subscriptionId: String?,
    val slug: String?,
    val plan: MembershipPlan
)