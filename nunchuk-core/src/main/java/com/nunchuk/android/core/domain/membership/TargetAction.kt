package com.nunchuk.android.core.domain.membership

import androidx.annotation.Keep

@Keep
enum class TargetAction {
    EMERGENCY_LOCKDOWN,
    DOWNLOAD_KEY_BACKUP,
    UPDATE_SECURITY_QUESTIONS,
    UPDATE_INHERITANCE_PLAN,
    UPDATE_SERVER_KEY,
    DELETE_WALLET,
    PROTECT_WALLET,
    EDIT_GROUP_MEMBERS,
    CLAIM_KEY
}