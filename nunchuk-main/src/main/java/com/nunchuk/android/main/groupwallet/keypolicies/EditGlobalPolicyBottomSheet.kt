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
import com.nunchuk.android.model.KeyPolicy
import com.nunchuk.android.model.SpendingPolicy
import com.nunchuk.android.model.SpendingTimeUnit

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun EditGlobalPolicyBottomSheet(
    policy: KeyPolicyItem,
    signer: SignerModel? = null,
    isGlobalMode: Boolean = true,
    onDismiss: () -> Unit = {},
    onSave: (KeyPolicyItem) -> Unit = {},
) {
    val spendingPolicy = policy.keyPolicy.spendingPolicy
    var amount by rememberSaveable {
        mutableStateOf(
            if (spendingPolicy == null || spendingPolicy.limit == 0.0) "" else {
                if (spendingPolicy.limit % 1.0 == 0.0) {
                    spendingPolicy.limit.toLong().toString()
                } else {
                    spendingPolicy.limit.toString()
                }
            }
        )
    }
    var currencyUnit by rememberSaveable { mutableStateOf(spendingPolicy?.currencyUnit ?: "USD") }
    var timeUnit by rememberSaveable { mutableStateOf(spendingPolicy?.timeUnit ?: SpendingTimeUnit.DAILY) }
    var isCoSigningDelayEnabled by rememberSaveable { mutableStateOf(policy.keyPolicy.signingDelayInSeconds > 0) }
    var coSigningDelayHours by rememberSaveable { mutableStateOf(policy.keyPolicy.getSigningDelayInHours().takeIf { it > 0 }?.toString().orEmpty()) }
    var coSigningDelayMinutes by rememberSaveable { mutableStateOf(policy.keyPolicy.getSigningDelayInMinutes().takeIf { it > 0 }?.toString().orEmpty()) }
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
                        text = formatSpendingLimit(
                            SpendingPolicy(
                                limit = amount.toDoubleOrNull() ?: 0.0,
                                timeUnit = timeUnit,
                                currencyUnit = currencyUnit,
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
                value = getTimeUnitDisplayName(timeUnit),
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
                                keyPolicy = KeyPolicy(
                                    spendingPolicy = SpendingPolicy(
                                        limit = amount.toDoubleOrNull() ?: 0.0,
                                        timeUnit = timeUnit,
                                        currencyUnit = currencyUnit,
                                    ),
                                    signingDelayInSeconds = delaySeconds,
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
        val timeUnits = SpendingTimeUnit.entries.toList()
        val selectedIndex = timeUnits.indexOf(timeUnit)
        NcSelectableBottomSheet(
            options = timeUnits.map { getTimeUnitDisplayName(it) },
            selectedPos = selectedIndex,
            onSelected = { index ->
                timeUnit = timeUnits[index]
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
                keyPolicy = KeyPolicy(
                    spendingPolicy = SpendingPolicy(
                        limit = 5000.0,
                        timeUnit = SpendingTimeUnit.DAILY,
                        currencyUnit = "USD",
                    ),
                    signingDelayInSeconds = 2 * KeyPolicy.ONE_HOUR_TO_SECONDS,
                    autoBroadcastTransaction = true,
                ),
            )
        )
    }
}
