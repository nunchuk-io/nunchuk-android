package com.nunchuk.android.main.groupwallet.keypolicies

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.nunchuk.android.core.signer.SignerModel
import com.nunchuk.android.model.SpendingPolicy
import com.nunchuk.android.model.SpendingTimeUnit

@Composable
internal fun formatSpendingLimit(policy: SpendingPolicy): String {
    val timeUnitText = when (policy.timeUnit) {
        SpendingTimeUnit.DAILY -> "Day"
        SpendingTimeUnit.WEEKLY -> "Week"
        SpendingTimeUnit.MONTHLY -> "Month"
        SpendingTimeUnit.YEARLY -> "Year"
    }
    val limitText = if (policy.limit == 0.0) "0" else {
        if (policy.limit % 1.0 == 0.0) {
            policy.limit.toLong().toString()
        } else {
            policy.limit.toString()
        }
    }
    return "${policy.currencyUnit} $limitText / $timeUnitText"
}

internal fun buildSignerLabel(signer: SignerModel?): String {
    if (signer == null) return ""
    val label = signer.getXfpOrCardIdLabel()
    return if (label.isNotEmpty()) {
        "${signer.name} ($label)"
    } else {
        signer.name
    }
}

@Composable
internal fun getTimeUnitDisplayName(timeUnit: SpendingTimeUnit): String {
    return when (timeUnit) {
        SpendingTimeUnit.DAILY -> stringResource(com.nunchuk.android.core.R.string.nc_daily)
        SpendingTimeUnit.WEEKLY -> stringResource(com.nunchuk.android.core.R.string.nc_weekly)
        SpendingTimeUnit.MONTHLY -> stringResource(com.nunchuk.android.core.R.string.nc_monthly)
        SpendingTimeUnit.YEARLY -> stringResource(com.nunchuk.android.core.R.string.nc_yearly)
    }
}
