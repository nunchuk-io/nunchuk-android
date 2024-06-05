package com.nunchuk.android.model.wallet

import androidx.annotation.Keep

@Keep
enum class WalletStatus {
    ACTIVE, LOCKED, REPLACED, DELETED
}