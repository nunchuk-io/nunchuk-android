package com.nunchuk.android.model

import androidx.annotation.Keep

@Keep
enum class MembershipPlan {
    NONE, IRON_HAND, HONEY_BADGER
}

fun String?.toMembershipPlan() = when (this) {
    IRON_HAND_PLAN, IRON_HAND_PLAN_TESTNET -> {
        MembershipPlan.IRON_HAND
    }
    HONEY_BADGER_PLAN, HONEY_BADGER_PLAN_TESTNET -> {
        MembershipPlan.HONEY_BADGER
    }
    else -> {
        MembershipPlan.NONE
    }
}

private const val IRON_HAND_PLAN = "iron_hand"
private const val IRON_HAND_PLAN_TESTNET = "iron_hand_testnet"
private const val HONEY_BADGER_PLAN_TESTNET = "honey_badger_testnet"
private const val HONEY_BADGER_PLAN = "honey_badger"
