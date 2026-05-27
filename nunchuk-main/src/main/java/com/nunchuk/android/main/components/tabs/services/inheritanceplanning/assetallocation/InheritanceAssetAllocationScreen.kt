/**************************************************************************
 * This file is part of the Nunchuk software (https://nunchuk.io/)        *
 * Copyright (C) 2022, 2023 Nunchuk                                       *
 *                                                                        *
 * This program is free software; you can redistribute it and/or          *
 * modify it under the terms of the GNU General Public License            *
 * as published by the Free Software Foundation; either version 3         *
 * of the License, or (at your option) any later version.                 *
 *                                                                        *
 * This program is distributed in the hope that it will be useful,        *
 * but WITHOUT ANY WARRANTY; without even the implied warranty of         *
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the          *
 * GNU General Public License for more details.                           *
 *                                                                        *
 * You should have received a copy of the GNU General Public License      *
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.  *
 *                                                                        *
 **************************************************************************/

package com.nunchuk.android.main.components.tabs.services.inheritanceplanning.assetallocation

import android.util.Patterns
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import com.nunchuk.android.compose.NcHighlightText
import com.nunchuk.android.compose.NcOutlineButton
import com.nunchuk.android.compose.NcPrimaryDarkButton
import com.nunchuk.android.compose.NcScaffold
import com.nunchuk.android.compose.NcTextField
import com.nunchuk.android.compose.NcToastType
import com.nunchuk.android.compose.NcTopAppBar
import com.nunchuk.android.compose.NunchukTheme
import com.nunchuk.android.compose.backgroundLightGray
import com.nunchuk.android.compose.backgroundMidGray
import com.nunchuk.android.compose.lightGray
import com.nunchuk.android.compose.showNunchukSnackbar
import com.nunchuk.android.compose.strokePrimary
import com.nunchuk.android.compose.textPrimary
import com.nunchuk.android.main.R
import com.nunchuk.android.main.components.tabs.services.inheritanceplanning.InheritanceBeneficiaryAllocation
import com.nunchuk.android.main.components.tabs.services.inheritanceplanning.view.AllocationDonutChart
import kotlinx.coroutines.launch
import kotlin.math.roundToInt
import com.nunchuk.android.widget.R as WidgetR

private enum class AssetAllocationEmailError {
    EMPTY,
    DUPLICATE,
    INVALID,
}

private data class AssetAllocationEmailValidationResult(
    val errors: Map<Int, AssetAllocationEmailError> = emptyMap(),
    val messageError: AssetAllocationEmailError? = null,
)

@Composable
internal fun InheritanceAssetAllocationScreen(
    remainTime: Int,
    initialBeneficiaries: List<InheritanceBeneficiaryAllocation> = emptyList(),
    onBackClicked: () -> Unit = {},
    onContinueClicked: (List<InheritanceBeneficiaryAllocation>) -> Unit = {},
) {
    var beneficiaries by remember {
        mutableStateOf(
            initialBeneficiaries.ifEmpty {
                listOf(
                    InheritanceBeneficiaryAllocation(email = "", allocationPercent = 50),
                    InheritanceBeneficiaryAllocation(email = "", allocationPercent = 50),
                )
            }
        )
    }
    var emailErrors by remember { mutableStateOf<Map<Int, AssetAllocationEmailError>>(emptyMap()) }
    val snackState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val emailEmptyError = stringResource(R.string.nc_asset_allocation_email_empty_error)
    val emailDuplicateError = stringResource(R.string.nc_asset_allocation_email_duplicate_error)
    val emailInvalidError = stringResource(R.string.nc_asset_allocation_email_invalid_error)
    InheritanceAssetAllocationContent(
        remainTime = remainTime,
        beneficiaries = beneficiaries,
        emailErrors = emailErrors,
        snackState = snackState,
        onEmailChanged = { index, email ->
            beneficiaries = beneficiaries.toMutableList().apply {
                this[index] = this[index].copy(email = email)
            }
            emailErrors = emailErrors - index
        },
        onAllocationChanged = { index, percent ->
            beneficiaries = beneficiaries.toMutableList().apply {
                val othersTotal = this.filterIndexed { i, _ -> i != index }.sumOf { it.allocationPercent }
                val clampedPercent = percent.coerceAtMost(100 - othersTotal)
                this[index] = this[index].copy(allocationPercent = clampedPercent)
            }
        },
        onAddBeneficiary = {
            beneficiaries = beneficiaries + InheritanceBeneficiaryAllocation(
                email = "",
                allocationPercent = 0
            )
            emailErrors = emptyMap()
        },
        onRemoveBeneficiary = { index ->
            if (beneficiaries.size > 2) {
                beneficiaries = beneficiaries.toMutableList().apply { removeAt(index) }
                emailErrors = emptyMap()
            }
        },
        onBackClicked = onBackClicked,
        onContinueClicked = {
            val validationResult = validateBeneficiaryEmails(beneficiaries)
            emailErrors = validationResult.errors
            val errorMessage = when (validationResult.messageError) {
                AssetAllocationEmailError.EMPTY -> emailEmptyError
                AssetAllocationEmailError.INVALID -> emailInvalidError
                AssetAllocationEmailError.DUPLICATE -> emailDuplicateError
                null -> null
            }
            if (errorMessage != null) {
                scope.launch {
                    snackState.showNunchukSnackbar(message = errorMessage, type = NcToastType.ERROR)
                }
            } else {
                onContinueClicked(beneficiaries.map { it.copy(email = it.email.trim()) })
            }
        },
    )
}

@Composable
private fun InheritanceAssetAllocationContent(
    remainTime: Int = 0,
    beneficiaries: List<InheritanceBeneficiaryAllocation> = emptyList(),
    emailErrors: Map<Int, AssetAllocationEmailError> = emptyMap(),
    snackState: SnackbarHostState = remember { SnackbarHostState() },
    onEmailChanged: (Int, String) -> Unit = { _, _ -> },
    onAllocationChanged: (Int, Int) -> Unit = { _, _ -> },
    onAddBeneficiary: () -> Unit = {},
    onRemoveBeneficiary: (Int) -> Unit = {},
    onBackClicked: () -> Unit = {},
    onContinueClicked: () -> Unit = {},
) {
    NunchukTheme {
        NcScaffold(
            modifier = Modifier.navigationBarsPadding(),
            snackState = snackState,
            topBar = {
                NcTopAppBar(
                    title = stringResource(id = R.string.nc_estimate_remain_time, remainTime),
                    onBackPress = onBackClicked
                )
            },
            bottomBar = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.backgroundLightGray)
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    if (beneficiaries.isNotEmpty()) {
                        AllocationDonutChart(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            beneficiaries = beneficiaries,
                        )
                    }
                    val totalAllocation = beneficiaries.sumOf { it.allocationPercent }
                    NcPrimaryDarkButton(
                        modifier = Modifier
                            .fillMaxWidth(),
                        enabled = totalAllocation >= 100,
                        onClick = onContinueClicked,
                    ) {
                        Text(text = stringResource(id = R.string.nc_text_continue))
                    }
                }
            }
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .padding(innerPadding)
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp)
            ) {
                Text(
                    modifier = Modifier.padding(top = 16.dp),
                    text = stringResource(R.string.nc_asset_allocation),
                    style = NunchukTheme.typography.heading
                )
                NcHighlightText(
                    modifier = Modifier.padding(top = 16.dp),
                    text = stringResource(R.string.nc_asset_allocation_desc),
                    style = NunchukTheme.typography.body
                )

                beneficiaries.forEachIndexed { index, beneficiary ->
                    BeneficiaryCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 24.dp),
                        index = index,
                        email = beneficiary.email,
                        allocationPercent = beneficiary.allocationPercent,
                        showRemove = beneficiaries.size > 2,
                        hasEmailError = emailErrors.containsKey(index),
                        onEmailChanged = { onEmailChanged(index, it) },
                        onAllocationChanged = { onAllocationChanged(index, it) },
                        onRemoveClicked = { onRemoveBeneficiary(index) },
                    )
                }

                NcOutlineButton(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp),
                    onClick = onAddBeneficiary,
                ) {
                    Icon(
                        painter = painterResource(id = WidgetR.drawable.ic_add_dark),
                        contentDescription = null,
                        tint = Color.Unspecified
                    )
                    Text(
                        modifier = Modifier.padding(start = 8.dp),
                        text = stringResource(id = R.string.nc_add_beneficiary)
                    )
                }

            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BeneficiaryCard(
    modifier: Modifier = Modifier,
    index: Int,
    email: String,
    allocationPercent: Int,
    showRemove: Boolean = false,
    hasEmailError: Boolean = false,
    onEmailChanged: (String) -> Unit = {},
    onAllocationChanged: (Int) -> Unit = {},
    onRemoveClicked: () -> Unit = {},
) {
    Box(modifier = modifier) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    color = MaterialTheme.colorScheme.backgroundMidGray,
                    shape = RoundedCornerShape(12.dp)
                )
                .border(
                    width = 1.dp,
                    color = MaterialTheme.colorScheme.lightGray,
                    shape = RoundedCornerShape(12.dp)
                )
                .padding(16.dp)
        ) {
            Text(
                text = stringResource(R.string.nc_beneficiary_n, index + 1),
                style = NunchukTheme.typography.titleSmall
            )
            NcTextField(
                modifier = Modifier.padding(top = 8.dp),
                title = "",
                value = email,
                placeholder = {
                    Text(
                        text = stringResource(R.string.nc_enter_email),
                        style = NunchukTheme.typography.body
                    )
                },
                onValueChange = onEmailChanged,
                hasError = hasEmailError,
                singleLine = true,
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val thumbColor = MaterialTheme.colorScheme.textPrimary
                Slider(
                    modifier = Modifier.weight(1f),
                    value = allocationPercent.toFloat(),
                    onValueChange = { onAllocationChanged(it.roundToInt()) },
                    valueRange = 0f..100f,
                    colors = SliderDefaults.colors(
                        thumbColor = thumbColor,
                        activeTrackColor = MaterialTheme.colorScheme.textPrimary,
                        inactiveTrackColor = MaterialTheme.colorScheme.strokePrimary,
                    ),
                    thumb = {
                        Box(
                            modifier = Modifier
                                .size(30.dp)
                                .background(color = thumbColor, shape = CircleShape)
                        )
                    }
                )
                Spacer(modifier = Modifier.width(12.dp))
                Box(
                    modifier = Modifier
                        .border(
                            width = 1.dp,
                            color = MaterialTheme.colorScheme.strokePrimary,
                            shape = RoundedCornerShape(8.dp)
                        )
                        .padding(horizontal = 12.dp, vertical = 14.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "$allocationPercent%",
                        style = NunchukTheme.typography.title,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
        if (showRemove) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .offset(x = 8.dp, y = (-8).dp)
                    .size(24.dp)
                    .background(
                        color = MaterialTheme.colorScheme.lightGray,
                        shape = CircleShape
                    )
                    .clickable(onClick = onRemoveClicked),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    modifier = Modifier.size(16.dp),
                    painter = painterResource(id = WidgetR.drawable.ic_delete),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.textPrimary
                )
            }
        }
    }
}

private fun validateBeneficiaryEmails(
    beneficiaries: List<InheritanceBeneficiaryAllocation>,
): AssetAllocationEmailValidationResult {
    val emptyEmailErrors = beneficiaries.mapIndexedNotNull { index, beneficiary ->
        index.takeIf { beneficiary.email.isBlank() }
    }.associateWith { AssetAllocationEmailError.EMPTY }
    if (emptyEmailErrors.isNotEmpty()) {
        return AssetAllocationEmailValidationResult(
            errors = emptyEmailErrors,
            messageError = AssetAllocationEmailError.EMPTY,
        )
    }

    val invalidEmailErrors = beneficiaries.mapIndexedNotNull { index, beneficiary ->
        index.takeIf { beneficiary.email.trim().isValidBeneficiaryEmail().not() }
    }.associateWith { AssetAllocationEmailError.INVALID }
    if (invalidEmailErrors.isNotEmpty()) {
        return AssetAllocationEmailValidationResult(
            errors = invalidEmailErrors,
            messageError = AssetAllocationEmailError.INVALID,
        )
    }

    val seenEmails = mutableSetOf<String>()
    val duplicateEmailErrors = beneficiaries.mapIndexedNotNull { index, beneficiary ->
        val normalizedEmail = beneficiary.email.trim().lowercase()
        index.takeIf { seenEmails.add(normalizedEmail).not() }
    }.associateWith { AssetAllocationEmailError.DUPLICATE }
    if (duplicateEmailErrors.isNotEmpty()) {
        return AssetAllocationEmailValidationResult(
            errors = duplicateEmailErrors,
            messageError = AssetAllocationEmailError.DUPLICATE,
        )
    }

    return AssetAllocationEmailValidationResult()
}

private fun String.isValidBeneficiaryEmail(): Boolean {
    return Patterns.EMAIL_ADDRESS.matcher(this).matches()
}

@PreviewLightDark
@Composable
private fun InheritanceAssetAllocationScreenPreview() {
    InheritanceAssetAllocationContent(
        remainTime = 18,
        beneficiaries = listOf(
            InheritanceBeneficiaryAllocation(email = "", allocationPercent = 50),
            InheritanceBeneficiaryAllocation(email = "", allocationPercent = 50),
        )
    )
}

@PreviewLightDark
@Composable
private fun InheritanceAssetAllocationThreeBeneficiariesPreview() {
    InheritanceAssetAllocationContent(
        remainTime = 18,
        beneficiaries = listOf(
            InheritanceBeneficiaryAllocation(email = "wife@gmail.com", allocationPercent = 50),
            InheritanceBeneficiaryAllocation(email = "son@gmail.com", allocationPercent = 25),
            InheritanceBeneficiaryAllocation(email = "daughter@gmail.com", allocationPercent = 25),
        )
    )
}
