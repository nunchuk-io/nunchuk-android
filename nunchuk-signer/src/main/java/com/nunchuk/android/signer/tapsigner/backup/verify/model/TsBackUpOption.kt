package com.nunchuk.android.signer.tapsigner.backup.verify.model

import androidx.annotation.StringRes

data class TsBackUpOption(val type : TsBackUpOptionType, val isSelected: Boolean = false, @StringRes val labelId: Int)

enum class TsBackUpOptionType {
    BY_APP, BY_MYSELF
}