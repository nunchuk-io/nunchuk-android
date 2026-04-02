package com.nunchuk.android.main.groupwallet.keypolicies

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import com.nunchuk.android.compose.NcCircleImage
import com.nunchuk.android.compose.NcPrimaryDarkButton
import com.nunchuk.android.compose.NcSelectableBottomSheet
import com.nunchuk.android.compose.NcSwitch
import com.nunchuk.android.compose.NcTextField
import com.nunchuk.android.compose.NunchukTheme
import com.nunchuk.android.compose.controlFillTertiary
import com.nunchuk.android.compose.greyLight
import com.nunchuk.android.compose.strokePrimary
import com.nunchuk.android.compose.textPrimary
import com.nunchuk.android.compose.textSecondary
import com.nunchuk.android.core.signer.SignerModel
import com.nunchuk.android.core.util.toReadableDrawableResId
import com.nunchuk.android.main.R
import com.nunchuk.android.model.GroupPlatformKeyPolicy
import com.nunchuk.android.model.GroupSpendingLimit
import com.nunchuk.android.model.KeyPolicy
import com.nunchuk.android.type.GroupSpendingLimitInterval

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun EditGlobalPolicyBottomSheet(
    policy: KeyPolicyItem,
    signer: SignerModel? = null,
    isGlobalMode: Boolean = true,
    onDismiss: () -> Unit = {},
    onSave: (KeyPolicyItem) -> Unit = {},
) {
    val normalizedPolicy = normalizeGroupPlatformKeyPolicy(policy.keyPolicy)
    val spendingLimit = policy.keyPolicy.spendingLimit ?: normalizedPolicy.spendingLimit
    val amountDouble = spendingLimit?.amount?.toDoubleOrNull() ?: 0.0
    var isSpendingLimitEnabled by rememberSaveable {
        mutableStateOf(policy.keyPolicy.spendingLimit != null)
    }
    var amount by rememberSaveable {
        mutableStateOf(
            if (amountDouble == 0.0) "" else {
                if (amountDouble % 1.0 == 0.0) {
                    amountDouble.toLong().toString()
                } else {
                    spendingLimit?.amount.orEmpty()
                }
            }
        )
    }
    var currencyUnit by rememberSaveable { mutableStateOf(spendingLimit?.currency?.ifEmpty { "USD" } ?: "USD") }
    var interval by rememberSaveable { mutableStateOf(spendingLimit?.interval ?: GroupSpendingLimitInterval.DAILY) }
    var isCoSigningDelayEnabled by rememberSaveable {
        mutableStateOf(normalizedPolicy.signingDelaySeconds > 0)
    }
    var coSigningDelayHours by rememberSaveable {
        mutableStateOf(
            (normalizedPolicy.signingDelaySeconds / KeyPolicy.ONE_HOUR_TO_SECONDS).takeIf { it > 0 }?.toString().orEmpty()
        )
    }
    var coSigningDelayMinutes by rememberSaveable {
        mutableStateOf(
            ((normalizedPolicy.signingDelaySeconds % KeyPolicy.ONE_HOUR_TO_SECONDS) / KeyPolicy.ONE_MINUTE_TO_SECONDS).takeIf { it > 0 }?.toString().orEmpty()
        )
    }
    var isAutoBroadcast by rememberSaveable {
        mutableStateOf(normalizedPolicy.autoBroadcastTransaction)
    }
    var showTimeUnitSelector by rememberSaveable { mutableStateOf(false) }
    var showCurrencySelector by rememberSaveable { mutableStateOf(false) }
    val intervalOptions = remember { GroupSpendingLimitInterval.entries.toList() }
    val currencyOptions = listOf(
        CurrencyOption(
            value = "USD",
            label = stringResource(com.nunchuk.android.core.R.string.nc_currency_usd),
        ),
        CurrencyOption(
            value = "BTC",
            label = stringResource(com.nunchuk.android.core.R.string.nc_currency_btc),
        ),
        CurrencyOption(
            value = "sat",
            label = stringResource(com.nunchuk.android.core.R.string.nc_currency_sat),
        ),
    )

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.background,
        dragHandle = { },
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(top = 24.dp, bottom = 24.dp)
                .verticalScroll(rememberScrollState()),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                if (isGlobalMode) {
                    NcCircleImage(
                        resId = R.drawable.ic_policy_keys,
                        size = 36.dp,
                        iconSize = 24.dp,
                    )
                } else if (signer != null) {
                    NcCircleImage(
                        resId = signer.toReadableDrawableResId(),
                        size = 36.dp,
                        iconSize = 24.dp,
                        color = MaterialTheme.colorScheme.greyLight,
                        iconTintColor = MaterialTheme.colorScheme.textPrimary,
                    )
                }
                Column {
                    Text(
                        text = if (isGlobalMode) {
                            stringResource(R.string.nc_all_keys)
                        } else {
                            buildSignerLabel(signer)
                        },
                        style = NunchukTheme.typography.bodySmall,
                    )
                    Text(
                        text = if (isSpendingLimitEnabled) {
                            formatGroupSpendingLimit(
                                GroupSpendingLimit(
                                    amount = amount.ifEmpty { "0" },
                                    interval = interval,
                                    currency = currencyUnit,
                                )
                            )
                        } else {
                            stringResource(R.string.nc_unlimited)
                        },
                        style = NunchukTheme.typography.title,
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(R.string.nc_spending_limit),
                    style = NunchukTheme.typography.body,
                )
                NcSwitch(
                    checked = isSpendingLimitEnabled,
                    onCheckedChange = { isSpendingLimitEnabled = it },
                )
            }

            Text(
                modifier = Modifier.padding(top = 4.dp),
                text = stringResource(R.string.nc_spending_limit_desc),
                style = NunchukTheme.typography.bodySmall.copy(
                    color = MaterialTheme.colorScheme.textSecondary
                ),
            )

            Spacer(modifier = Modifier.height(8.dp))

            if (isSpendingLimitEnabled) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    NcTextField(
                        modifier = Modifier.weight(1f),
                        title = "",
                        value = amount,
                        placeholder = {
                            Text(
                                text = "0",
                                style = NunchukTheme.typography.body.copy(
                                    color = MaterialTheme.colorScheme.textSecondary
                                ),
                            )
                        },
                        keyboardOptions = KeyboardOptions(
                            keyboardType = if (currencyUnit.equals("sat", ignoreCase = true)) {
                                KeyboardType.Number
                            } else {
                                KeyboardType.Decimal
                            }
                        ),
                        onValueChange = { value ->
                            amount = if (currencyUnit.equals("sat", ignoreCase = true)) {
                                value.filter { it.isDigit() }
                            } else {
                                value
                            }
                        },
                    )
                    NcTextField(
                        modifier = Modifier
                            .weight(0.5f)
                            .width(128.dp)
                            .widthIn(min = 116.dp),
                        title = "",
                        value = currencyUnit,
                        readOnly = true,
                        singleLine = true,
                        maxLines = 1,
                        rightContent = {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_arrow_down),
                                contentDescription = null,
                                modifier = Modifier
                                    .size(24.dp)
                                    .clickable { showCurrencySelector = true },
                            )
                        },
                        onClick = { showCurrencySelector = true },
                        onValueChange = {},
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                NcTextField(
                    modifier = Modifier.fillMaxWidth(),
                    title = "",
                    value = getIntervalDisplayName(interval),
                    readOnly = true,
                    rightContent = {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_arrow_down),
                            contentDescription = null,
                            modifier = Modifier
                                .size(24.dp)
                                .clickable { showTimeUnitSelector = true },
                        )
                    },
                    onClick = { showTimeUnitSelector = true },
                    onValueChange = {},
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            HorizontalDivider(
                color = MaterialTheme.colorScheme.strokePrimary,
            )

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(com.nunchuk.android.core.R.string.nc_enable_co_signing_delay),
                    style = NunchukTheme.typography.body,
                )
                NcSwitch(
                    checked = isCoSigningDelayEnabled,
                    onCheckedChange = { isCoSigningDelayEnabled = it },
                )
            }

            Text(
                modifier = Modifier.padding(top = 4.dp),
                text = stringResource(R.string.nc_co_signing_delay_desc),
                style = NunchukTheme.typography.bodySmall.copy(
                    color = MaterialTheme.colorScheme.textSecondary
                ),
            )

            if (isCoSigningDelayEnabled) {
                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    NcTextField(
                        modifier = Modifier.weight(1f),
                        title = stringResource(com.nunchuk.android.core.R.string.nc_hours),
                        value = coSigningDelayHours,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        onValueChange = { coSigningDelayHours = it },
                    )
                    NcTextField(
                        modifier = Modifier.weight(1f),
                        title = stringResource(com.nunchuk.android.core.R.string.nc_minutes),
                        value = coSigningDelayMinutes,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        onValueChange = { coSigningDelayMinutes = it },
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(R.string.nc_auto_broadcast),
                    style = NunchukTheme.typography.body,
                )
                NcSwitch(
                    checked = isAutoBroadcast,
                    onCheckedChange = { isAutoBroadcast = it },
                )
            }

            Text(
                modifier = Modifier.padding(top = 4.dp),
                text = stringResource(R.string.nc_auto_broadcast_desc),
                style = NunchukTheme.typography.bodySmall.copy(
                    color = MaterialTheme.colorScheme.textSecondary
                ),
            )

            Spacer(modifier = Modifier.height(24.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Button(
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp),
                    onClick = onDismiss,
                    shape = RoundedCornerShape(48.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.controlFillTertiary,
                        contentColor = MaterialTheme.colorScheme.textPrimary,
                    ),
                ) {
                    Text(
                        text = stringResource(com.nunchuk.android.core.R.string.nc_cancel),
                        style = NunchukTheme.typography.title,
                    )
                }
                NcPrimaryDarkButton(
                    modifier = Modifier.weight(1f),
                    onClick = {
                        val hours = coSigningDelayHours.toIntOrNull() ?: 0
                        val minutes = coSigningDelayMinutes.toIntOrNull() ?: 0
                        val delaySeconds = if (isCoSigningDelayEnabled) {
                            hours * KeyPolicy.ONE_HOUR_TO_SECONDS + minutes * KeyPolicy.ONE_MINUTE_TO_SECONDS
                        } else {
                            0
                        }
                        val spendingLimitValue = if (isSpendingLimitEnabled) {
                            GroupSpendingLimit(
                                amount = amount.ifEmpty { "0" },
                                interval = interval,
                                currency = currencyUnit,
                            )
                        } else {
                            null
                        }
                        onSave(
                            policy.copy(
                                keyPolicy = GroupPlatformKeyPolicy(
                                    spendingLimit = spendingLimitValue,
                                    signingDelaySeconds = delaySeconds,
                                    autoBroadcastTransaction = isAutoBroadcast,
                                )
                            )
                        )
                    },
                ) {
                    Text(text = stringResource(com.nunchuk.android.core.R.string.nc_apply))
                }
            }
        }
    }

    if (showTimeUnitSelector && isSpendingLimitEnabled) {
        val selectedIndex = intervalOptions.indexOfFirst { it.name == interval.name }.coerceAtLeast(0)
        NcSelectableBottomSheet(
            options = intervalOptions.map { getIntervalDisplayName(it) },
            selectedPos = selectedIndex,
            onSelected = { index ->
                interval = intervalOptions.getOrElse(index) { intervalOptions.first() }
                showTimeUnitSelector = false
            },
            onDismiss = { showTimeUnitSelector = false },
        )
    }

    if (showCurrencySelector && isSpendingLimitEnabled) {
        val selectedIndex = currencyOptions.indexOfFirst {
            it.value.equals(currencyUnit, ignoreCase = true)
        }.coerceAtLeast(0)
        NcSelectableBottomSheet(
            options = currencyOptions.map { it.label },
            selectedPos = selectedIndex,
            onSelected = { index ->
                val selectedCurrency = currencyOptions.getOrElse(index) { currencyOptions.first() }
                currencyUnit = selectedCurrency.value
                if (currencyUnit.equals("sat", ignoreCase = true)) {
                    amount = amount.substringBefore('.').filter { it.isDigit() }
                }
                showCurrencySelector = false
            },
            onDismiss = { showCurrencySelector = false },
        )
    }
}

private data class CurrencyOption(
    val value: String,
    val label: String,
)

@PreviewLightDark
@Composable
private fun EditGlobalPolicyBottomSheetPreview() {
    NunchukTheme {
        EditGlobalPolicyBottomSheet(policy = KeyPolicyItem())
    }
}

@PreviewLightDark
@Composable
private fun EditGlobalPolicyBottomSheetWithDataPreview() {
    NunchukTheme {
        EditGlobalPolicyBottomSheet(
            policy = KeyPolicyItem(
                keyPolicy = GroupPlatformKeyPolicy(
                    spendingLimit = GroupSpendingLimit(
                        amount = "5000",
                        interval = GroupSpendingLimitInterval.DAILY,
                        currency = "USD",
                    ),
                    signingDelaySeconds = 2 * KeyPolicy.ONE_HOUR_TO_SECONDS,
                    autoBroadcastTransaction = true,
                ),
            )
        )
    }
}
