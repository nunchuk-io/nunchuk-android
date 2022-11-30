package com.nunchuk.android.model

import androidx.annotation.Keep

@Keep
enum class MembershipPlan {
    NONE, IRON_HAND, HONEY_BADGER
}

fun String?.toMembershipPlan() = when (this) {
    IRON_HAND_PLAN -> {
        MembershipPlan.IRON_HAND
    }
    HONEY_BADGER_PLAN -> {
        MembershipPlan.HONEY_BADGER
    }
    else -> {
        MembershipPlan.NONE
    }
}

private const val IRON_HAND_PLAN = "iron_hand"
private const val HONEY_BADGER_PLAN = "honey_badger"
