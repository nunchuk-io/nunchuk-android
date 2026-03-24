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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.nunchuk.android.compose.NcCircleImage
import com.nunchuk.android.compose.NcOutlineButton
import com.nunchuk.android.compose.greyLight
import com.nunchuk.android.compose.textPrimary
import com.nunchuk.android.compose.NcPrimaryDarkButton
import com.nunchuk.android.compose.NcSelectableBottomSheet
import com.nunchuk.android.compose.NcSwitch
import com.nunchuk.android.compose.NcTextField
import com.nunchuk.android.compose.NunchukTheme
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
    val spendingLimit = policy.keyPolicy.spendingLimit
    val amountDouble = spendingLimit?.amount?.toDoubleOrNull() ?: 0.0
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
    var isCoSigningDelayEnabled by rememberSaveable { mutableStateOf(policy.keyPolicy.signingDelaySeconds > 0) }
    var coSigningDelayHours by rememberSaveable {
        mutableStateOf(
            (policy.keyPolicy.signingDelaySeconds / KeyPolicy.ONE_HOUR_TO_SECONDS).takeIf { it > 0 }?.toString().orEmpty()
        )
    }
    var coSigningDelayMinutes by rememberSaveable {
        mutableStateOf(
            ((policy.keyPolicy.signingDelaySeconds % KeyPolicy.ONE_HOUR_TO_SECONDS) / KeyPolicy.ONE_MINUTE_TO_SECONDS).takeIf { it > 0 }?.toString().orEmpty()
        )
    }
    var isAutoBroadcast by rememberSaveable { mutableStateOf(policy.keyPolicy.autoBroadcastTransaction) }
    var showTimeUnitSelector by rememberSaveable { mutableStateOf(false) }
    var showCurrencySelector by rememberSaveable { mutableStateOf(false) }

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
                        text = formatGroupSpendingLimit(
                            GroupSpendingLimit(
                                amount = amount.ifEmpty { "0" },
                                interval = interval,
                                currency = currencyUnit,
                            )
                        ),
                        style = NunchukTheme.typography.title,
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = stringResource(R.string.nc_spending_limit),
                style = NunchukTheme.typography.titleSmall,
            )

            Spacer(modifier = Modifier.height(8.dp))

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
                            text = stringResource(R.string.nc_enter_amount),
                            style = NunchukTheme.typography.body.copy(
                                color = MaterialTheme.colorScheme.textSecondary
                            ),
                        )
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    onValueChange = { amount = it },
                )
                NcTextField(
                    modifier = Modifier
                        .weight(0.4f),
                    title = "",
                    value = currencyUnit,
                    readOnly = true,
                    rightContent = {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_arrow_drop_down),
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
                        painter = painterResource(id = R.drawable.ic_arrow_drop_down),
                        contentDescription = null,
                        modifier = Modifier
                            .size(24.dp)
                            .clickable { showTimeUnitSelector = true },
                    )
                },
                onClick = { showTimeUnitSelector = true },
                onValueChange = {},
            )

            Spacer(modifier = Modifier.height(24.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(com.nunchuk.android.core.R.string.nc_enable_co_signing_delay),
                    style = NunchukTheme.typography.title,
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
                    style = NunchukTheme.typography.title,
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
                NcOutlineButton(
                    modifier = Modifier.weight(1f),
                    onClick = onDismiss,
                ) {
                    Text(text = stringResource(com.nunchuk.android.core.R.string.nc_cancel))
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
                        onSave(
                            policy.copy(
                                keyPolicy = GroupPlatformKeyPolicy(
                                    spendingLimit = GroupSpendingLimit(
                                        amount = amount.ifEmpty { "0" },
                                        interval = interval,
                                        currency = currencyUnit,
                                    ),
                                    signingDelaySeconds = delaySeconds,
                                    autoBroadcastTransaction = isAutoBroadcast,
                                )
                            )
                        )
                    },
                ) {
                    Text(text = stringResource(com.nunchuk.android.core.R.string.nc_save))
                }
            }
        }
    }

    if (showTimeUnitSelector) {
        val intervals = GroupSpendingLimitInterval.entries.toList()
        val selectedIndex = intervals.indexOf(interval)
        NcSelectableBottomSheet(
            options = intervals.map { getIntervalDisplayName(it) },
            selectedPos = selectedIndex,
            onSelected = { index ->
                interval = intervals[index]
                showTimeUnitSelector = false
            },
            onDismiss = { showTimeUnitSelector = false },
        )
    }

    if (showCurrencySelector) {
        val currencies = listOf("USD", "BTC")
        val selectedIndex = currencies.indexOf(currencyUnit)
        NcSelectableBottomSheet(
            options = currencies,
            selectedPos = selectedIndex,
            onSelected = { index ->
                currencyUnit = currencies[index]
                showCurrencySelector = false
            },
            onDismiss = { showCurrencySelector = false },
        )
    }
}

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
