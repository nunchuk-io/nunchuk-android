package com.nunchuk.android.model.byzantine

import androidx.annotation.Keep

@Keep
enum class AlertType {
    NONE,
    GROUP_WALLET_PENDING,
    GROUP_WALLET_SETUP,
    GROUP_WALLET_INVITATION_DENIED,
    DRAFT_WALLET_KEY_ADDED,
    UPDATE_SERVER_KEY,
    UPDATE_SERVER_KEY_SUCCESS,
    CREATE_INHERITANCE_PLAN,
    UPDATE_INHERITANCE_PLAN,
    CANCEL_INHERITANCE_PLAN,
    UPDATE_INHERITANCE_PLAN_SUCCESS
}

fun String?.toAlertType() = AlertType.values().find { it.name == this } ?: AlertType.NONE