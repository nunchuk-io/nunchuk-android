package com.nunchuk.android.main.groupwallet.keypolicies

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.nunchuk.android.core.signer.SignerModel
import com.nunchuk.android.model.GroupPlatformKeyPolicy
import com.nunchuk.android.model.GroupSpendingLimit
import com.nunchuk.android.model.SpendingPolicy
import com.nunchuk.android.model.SpendingTimeUnit
import com.nunchuk.android.type.GroupSpendingLimitInterval

private const val DEFAULT_LIMIT_AMOUNT = "0"
private const val DEFAULT_LIMIT_CURRENCY = "USD"

internal fun defaultGroupSpendingLimit(): GroupSpendingLimit {
    return GroupSpendingLimit(
        amount = DEFAULT_LIMIT_AMOUNT,
        interval = GroupSpendingLimitInterval.DAILY,
        currency = DEFAULT_LIMIT_CURRENCY,
    )
}

internal fun normalizeGroupSpendingLimit(limit: GroupSpendingLimit?): GroupSpendingLimit {
    val base = limit ?: defaultGroupSpendingLimit()
    return base.copy(
        amount = base.amount.takeIf { it.isNotBlank() } ?: DEFAULT_LIMIT_AMOUNT,
        currency = base.currency.takeIf { it.isNotBlank() } ?: DEFAULT_LIMIT_CURRENCY,
    )
}

internal fun defaultGroupPlatformKeyPolicy(): GroupPlatformKeyPolicy {
    return GroupPlatformKeyPolicy(
        spendingLimit = defaultGroupSpendingLimit(),
        signingDelaySeconds = 0,
        autoBroadcastTransaction = false,
    )
}

internal fun normalizeGroupPlatformKeyPolicy(policy: GroupPlatformKeyPolicy?): GroupPlatformKeyPolicy {
    val base = policy ?: defaultGroupPlatformKeyPolicy()
    return base.copy(
        spendingLimit = normalizeGroupSpendingLimit(base.spendingLimit),
    )
}

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

@Composable
internal fun formatGroupSpendingLimit(limit: GroupSpendingLimit): String {
    val normalizedLimit = normalizeGroupSpendingLimit(limit)
    val intervalText = when (normalizedLimit.interval) {
        GroupSpendingLimitInterval.DAILY -> "Day"
        GroupSpendingLimitInterval.WEEKLY -> "Week"
        GroupSpendingLimitInterval.MONTHLY -> "Month"
    }
    val amountDouble = normalizedLimit.amount.toDoubleOrNull() ?: 0.0
    val amountText = if (amountDouble == 0.0) "0" else {
        if (amountDouble % 1.0 == 0.0) {
            amountDouble.toLong().toString()
        } else {
            normalizedLimit.amount
        }
    }
    return "${normalizedLimit.currency} $amountText / $intervalText"
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

@Composable
internal fun getIntervalDisplayName(interval: GroupSpendingLimitInterval): String {
    return when (interval) {
        GroupSpendingLimitInterval.DAILY -> stringResource(com.nunchuk.android.core.R.string.nc_daily)
        GroupSpendingLimitInterval.WEEKLY -> stringResource(com.nunchuk.android.core.R.string.nc_weekly)
        GroupSpendingLimitInterval.MONTHLY -> stringResource(com.nunchuk.android.core.R.string.nc_monthly)
    }
}
