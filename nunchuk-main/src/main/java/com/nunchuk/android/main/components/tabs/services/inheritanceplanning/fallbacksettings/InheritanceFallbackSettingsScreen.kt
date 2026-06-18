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

package com.nunchuk.android.main.components.tabs.services.inheritanceplanning.fallbacksettings

import com.nunchuk.android.main.components.tabs.services.inheritanceplanning.estimateRemainTimeTitle
import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.nunchuk.android.compose.NcDatePickerDialog
import com.nunchuk.android.compose.NcSnackBarHost
import com.nunchuk.android.compose.NcSnackbarVisuals
import com.nunchuk.android.compose.NcTextField
import com.nunchuk.android.compose.NcPrimaryDarkButton
import com.nunchuk.android.compose.NcRadioButton
import com.nunchuk.android.compose.NcTag
import com.nunchuk.android.compose.NcToastType
import com.nunchuk.android.compose.NcTopAppBar
import com.nunchuk.android.compose.NunchukTheme
import com.nunchuk.android.compose.fillInputText
import com.nunchuk.android.compose.lightGray
import com.nunchuk.android.compose.strokePrimary
import com.nunchuk.android.compose.textPrimary
import com.nunchuk.android.compose.textSecondary
import com.nunchuk.android.main.R
import com.nunchuk.android.widget.R as WidgetR
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

enum class InheritanceFallbackOption(
    @StringRes val titleRes: Int,
    @StringRes val descRes: Int,
) {
    NO_FALLBACK(
        titleRes = R.string.nc_fallback_option_none_title,
        descRes = R.string.nc_fallback_option_none_desc,
    ),
    INACTIVITY_FALLBACK(
        titleRes = R.string.nc_fallback_option_inactivity_title,
        descRes = R.string.nc_fallback_option_inactivity_desc,
    ),
    DATE_BASED_FALLBACK(
        titleRes = R.string.nc_fallback_option_date_based_title,
        descRes = R.string.nc_fallback_option_date_based_desc,
    ),
}

enum class FallbackTriggerUnit(@StringRes val labelRes: Int) {
    YEAR(labelRes = R.string.nc_release_schedule_repeat_year),
    MONTH(labelRes = R.string.nc_release_schedule_repeat_month),
    WEEK(labelRes = R.string.nc_release_schedule_repeat_week),
    DAY(labelRes = R.string.nc_release_schedule_repeat_day),
}

data class InheritanceFallbackSettingsValue(
    val selectedOption: InheritanceFallbackOption,
    val triggerValue: String,
    val triggerUnit: FallbackTriggerUnit,
    val fallbackDate: String,
)

private enum class FallbackSettingsValidationError {
    DATE_MUST_BE_AFTER_FINAL_PAYOUT,
}

private const val FALLBACK_DATE_PATTERN = "MM/dd/yyyy"

@Composable
internal fun InheritanceFallbackSettingsScreen(
    remainTime: Int,
    finalScheduledPayoutTimeMillis: Long? = null,
    initialValue: InheritanceFallbackSettingsValue = InheritanceFallbackSettingsValue(
        selectedOption = InheritanceFallbackOption.INACTIVITY_FALLBACK,
        triggerValue = "5",
        triggerUnit = FallbackTriggerUnit.YEAR,
        fallbackDate = "05/29/2050",
    ),
    onBackClicked: () -> Unit = {},
    onContinueClicked: (InheritanceFallbackSettingsValue) -> Unit = {},
) {
    var selectedOption by rememberSaveable { mutableStateOf(initialValue.selectedOption) }
    var triggerValue by rememberSaveable { mutableStateOf(initialValue.triggerValue) }
    var triggerUnit by rememberSaveable { mutableStateOf(initialValue.triggerUnit) }
    var fallbackDate by rememberSaveable { mutableStateOf(initialValue.fallbackDate) }
    var showTriggerUnitMenu by rememberSaveable { mutableStateOf(false) }
    var showDatePicker by rememberSaveable { mutableStateOf(false) }
    var validationError by rememberSaveable {
        mutableStateOf<FallbackSettingsValidationError?>(null)
    }
    val snackState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()
    val dateLaterErrorMessage = stringResource(id = R.string.nc_fallback_date_later_error)
    val showDateValidationError =
        validationError == FallbackSettingsValidationError.DATE_MUST_BE_AFTER_FINAL_PAYOUT

    InheritanceFallbackSettingsContent(
        remainTime = remainTime,
        selectedOption = selectedOption,
        triggerValue = triggerValue,
        triggerUnit = triggerUnit,
        fallbackDate = fallbackDate,
        showDateValidationError = showDateValidationError,
        showTriggerUnitMenu = showTriggerUnitMenu,
        continueEnabled = !showDateValidationError,
        onBackClicked = onBackClicked,
        onOptionClick = {
            selectedOption = it
            showTriggerUnitMenu = false
            validationError = null
        },
        onTriggerValueChange = { value ->
            triggerValue = value.filter(Char::isDigit).take(3)
            validationError = null
        },
        onTriggerFieldClick = {
            showTriggerUnitMenu = !showTriggerUnitMenu
        },
        onTriggerUnitSelected = { unit ->
            triggerUnit = unit
            showTriggerUnitMenu = false
            validationError = null
        },
        onDismissTriggerMenu = { showTriggerUnitMenu = false },
        onDateClick = { showDatePicker = true },
        onContinueClicked = {
            val nextValidationError = validateFallbackSettings(
                selectedOption = selectedOption,
                fallbackDate = fallbackDate,
                finalScheduledPayoutTimeMillis = finalScheduledPayoutTimeMillis,
            )
            if (nextValidationError != null) {
                validationError = nextValidationError
                coroutineScope.launch {
                    snackState.showSnackbar(
                        NcSnackbarVisuals(
                            message = when (nextValidationError) {
                                FallbackSettingsValidationError.DATE_MUST_BE_AFTER_FINAL_PAYOUT ->
                                    dateLaterErrorMessage
                            },
                            type = NcToastType.ERROR,
                        )
                    )
                }
                return@InheritanceFallbackSettingsContent
            }
            validationError = null
            onContinueClicked(
                InheritanceFallbackSettingsValue(
                    selectedOption = selectedOption,
                    triggerValue = triggerValue.ifBlank { "1" },
                    triggerUnit = triggerUnit,
                    fallbackDate = fallbackDate,
                )
            )
        },
        snackState = snackState,
    )

    if (showDatePicker) {
        NcDatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            defaultDate = fallbackDate.toDateMillis(),
            onConfirm = { selectedMillis ->
                fallbackDate = selectedMillis.toDateString()
                validationError = null
                showDatePicker = false
            },
            convertLocalToUtc = true,
        )
    }
}

@Composable
private fun InheritanceFallbackSettingsContent(
    remainTime: Int = 0,
    selectedOption: InheritanceFallbackOption = InheritanceFallbackOption.INACTIVITY_FALLBACK,
    triggerValue: String = "5",
    triggerUnit: FallbackTriggerUnit = FallbackTriggerUnit.YEAR,
    fallbackDate: String = "05/29/2050",
    showDateValidationError: Boolean = false,
    showTriggerUnitMenu: Boolean = false,
    continueEnabled: Boolean = true,
    onBackClicked: () -> Unit = {},
    onOptionClick: (InheritanceFallbackOption) -> Unit = {},
    onTriggerValueChange: (String) -> Unit = {},
    onTriggerFieldClick: () -> Unit = {},
    onTriggerUnitSelected: (FallbackTriggerUnit) -> Unit = {},
    onDismissTriggerMenu: () -> Unit = {},
    onDateClick: () -> Unit = {},
    onContinueClicked: () -> Unit = {},
    snackState: SnackbarHostState = remember { SnackbarHostState() },
) {
    NunchukTheme {
        Scaffold(
            topBar = {
                NcTopAppBar(
                    title = estimateRemainTimeTitle(remainTime),
                    onBackPress = onBackClicked,
                )
            },
            snackbarHost = {
                NcSnackBarHost(state = snackState)
            },
            bottomBar = {
                Column(
                    modifier = Modifier
                        .navigationBarsPadding()
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 16.dp)
                ) {
                    NcPrimaryDarkButton(
                        modifier = Modifier.fillMaxWidth(),
                        enabled = continueEnabled,
                        onClick = onContinueClicked,
                    ) {
                        Text(text = stringResource(id = R.string.nc_text_continue))
                    }
                }
            },
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
                    text = stringResource(id = R.string.nc_fallback_settings_title),
                    style = NunchukTheme.typography.heading,
                )
                Text(
                    modifier = Modifier.padding(top = 16.dp),
                    text = stringResource(id = R.string.nc_fallback_settings_desc),
                    style = NunchukTheme.typography.body,
                )

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    FallbackOptionCard(
                        modifier = Modifier.fillMaxWidth(),
                        isSelected = selectedOption == InheritanceFallbackOption.NO_FALLBACK,
                        title = stringResource(id = InheritanceFallbackOption.NO_FALLBACK.titleRes),
                        description = stringResource(id = InheritanceFallbackOption.NO_FALLBACK.descRes),
                        onClick = { onOptionClick(InheritanceFallbackOption.NO_FALLBACK) },
                    )

                    FallbackOptionCard(
                        modifier = Modifier.fillMaxWidth(),
                        isSelected = selectedOption == InheritanceFallbackOption.INACTIVITY_FALLBACK,
                        title = stringResource(id = InheritanceFallbackOption.INACTIVITY_FALLBACK.titleRes),
                        description = stringResource(id = InheritanceFallbackOption.INACTIVITY_FALLBACK.descRes),
                        showRecommendedTag = true,
                        onClick = { onOptionClick(InheritanceFallbackOption.INACTIVITY_FALLBACK) },
                    ) {
                        InactivityFallbackConfig(
                            triggerValue = triggerValue,
                            triggerUnit = triggerUnit,
                            showUnitMenu = showTriggerUnitMenu,
                            onTriggerValueChange = onTriggerValueChange,
                            onTriggerFieldClick = onTriggerFieldClick,
                            onTriggerUnitSelected = onTriggerUnitSelected,
                            onDismissUnitMenu = onDismissTriggerMenu,
                        )
                    }

                    FallbackOptionCard(
                        modifier = Modifier.fillMaxWidth(),
                        isSelected = selectedOption == InheritanceFallbackOption.DATE_BASED_FALLBACK,
                        title = stringResource(id = InheritanceFallbackOption.DATE_BASED_FALLBACK.titleRes),
                        description = stringResource(id = InheritanceFallbackOption.DATE_BASED_FALLBACK.descRes),
                        onClick = { onOptionClick(InheritanceFallbackOption.DATE_BASED_FALLBACK) },
                    ) {
                        DateBasedFallbackConfig(
                            date = fallbackDate,
                            hasError = showDateValidationError,
                            onDateClick = onDateClick,
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

@Composable
private fun FallbackOptionCard(
    modifier: Modifier = Modifier,
    isSelected: Boolean,
    title: String,
    description: String,
    showRecommendedTag: Boolean = false,
    onClick: () -> Unit,
    selectedContent: @Composable (() -> Unit)? = null,
) {
    Box(
        modifier = modifier
            .clickable(onClick = onClick)
            .border(
                width = if (isSelected) 2.dp else 1.dp,
                color = if (isSelected) MaterialTheme.colorScheme.textPrimary else MaterialTheme.colorScheme.strokePrimary,
                shape = RoundedCornerShape(20.dp),
            )
            .padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.Top) {
            NcRadioButton(
                modifier = Modifier
                    .size(24.dp)
                    .padding(top = 2.dp),
                selected = isSelected,
                onClick = onClick,
            )
            Column(
                modifier = Modifier
                    .padding(start = 12.dp)
                    .fillMaxWidth()
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = title,
                        style = NunchukTheme.typography.title,
                    )
                    if (showRecommendedTag) {
                        NcTag(
                            modifier = Modifier.padding(start = 8.dp),
                            label = stringResource(id = R.string.nc_recommended),
                        )
                    }
                }
                Text(
                    modifier = Modifier.padding(top = 8.dp),
                    text = description,
                    style = NunchukTheme.typography.body,
                )
                if (isSelected && selectedContent != null) {
                    selectedContent()
                }
            }
        }
    }
}

@Composable
private fun InactivityFallbackConfig(
    triggerValue: String,
    triggerUnit: FallbackTriggerUnit,
    showUnitMenu: Boolean,
    onTriggerValueChange: (String) -> Unit,
    onTriggerFieldClick: () -> Unit,
    onTriggerUnitSelected: (FallbackTriggerUnit) -> Unit,
    onDismissUnitMenu: () -> Unit,
) {
    var triggerFieldWidth by remember { mutableStateOf(0.dp) }
    val density = LocalDensity.current

    Text(
        modifier = Modifier.padding(top = 16.dp),
        text = stringResource(id = R.string.nc_fallback_trigger),
        style = NunchukTheme.typography.title,
    )
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .weight(1f)
                .onGloballyPositioned { coordinates ->
                    triggerFieldWidth = with(density) { coordinates.size.width.toDp() }
                }
        ) {
            NcTextField(
                title = "",
                value = triggerValue,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                rightContent = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = stringResource(id = triggerUnit.labelRes),
                            style = NunchukTheme.typography.body.copy(color = MaterialTheme.colorScheme.textSecondary)
                        )
                        Icon(
                            modifier = Modifier
                                .padding(start = 8.dp, end = 12.dp)
                                .clickable(onClick = onTriggerFieldClick),
                            painter = painterResource(
                                id = if (showUnitMenu) {
                                    WidgetR.drawable.ic_caret_up
                                } else {
                                    WidgetR.drawable.ic_caret_down
                                }
                            ),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.textPrimary,
                        )
                    }
                },
                onValueChange = { onTriggerValueChange(it.filter(Char::isDigit).take(3)) }
            )
            DropdownMenu(
                expanded = showUnitMenu,
                modifier = Modifier
                    .then(if (triggerFieldWidth > 0.dp) Modifier.width(triggerFieldWidth) else Modifier),
                onDismissRequest = onDismissUnitMenu,
                shape = RoundedCornerShape(20.dp),
                containerColor = MaterialTheme.colorScheme.lightGray,
                tonalElevation = 0.dp,
                shadowElevation = 12.dp,
            ) {
                FallbackTriggerUnit.entries.forEach { unit ->
                    DropdownMenuItem(
                        text = {
                            Text(
                                text = stringResource(id = unit.labelRes),
                                style = NunchukTheme.typography.body,
                            )
                        },
                        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 18.dp),
                        onClick = { onTriggerUnitSelected(unit) },
                    )
                }
            }
        }

        Text(
            modifier = Modifier.padding(start = 10.dp),
            text = stringResource(id = R.string.nc_fallback_after_last_payout),
            style = NunchukTheme.typography.title,
        )
    }
}

@Composable
private fun DateBasedFallbackConfig(
    date: String,
    hasError: Boolean,
    onDateClick: () -> Unit,
) {
    NcTextField(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 16.dp),
        title = stringResource(id = R.string.nc_fallback_date),
        value = date,
        hasError = hasError,
        readOnly = true,
        enabled = false,
        disableBackgroundColor = MaterialTheme.colorScheme.fillInputText,
        onClick = onDateClick,
        rightContent = {
            Icon(
                modifier = Modifier
                    .padding(end = 12.dp)
                    .clickable(onClick = onDateClick),
                painter = painterResource(id = WidgetR.drawable.ic_calendar_blank),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.textPrimary,
            )
        },
        onValueChange = {},
    )
}

private fun validateFallbackSettings(
    selectedOption: InheritanceFallbackOption,
    fallbackDate: String,
    finalScheduledPayoutTimeMillis: Long?,
): FallbackSettingsValidationError? {
    if (selectedOption != InheritanceFallbackOption.DATE_BASED_FALLBACK) return null
    if (finalScheduledPayoutTimeMillis == null) return null

    val selectedDateMillis = fallbackDate.toDateMillisOrNull()
    return if (selectedDateMillis != null && selectedDateMillis > finalScheduledPayoutTimeMillis) {
        null
    } else {
        FallbackSettingsValidationError.DATE_MUST_BE_AFTER_FINAL_PAYOUT
    }
}

private fun String.toDateMillisOrNull(): Long? {
    val formatter = SimpleDateFormat(FALLBACK_DATE_PATTERN, Locale.US).apply {
        isLenient = false
    }
    val parsedDate = runCatching { formatter.parse(this) }.getOrNull() ?: return null
    return Calendar.getInstance().apply {
        time = parsedDate
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis
}

private fun String.toDateMillis(): Long {
    return toDateMillisOrNull() ?: Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis
}

private fun Long.toDateString(): String {
    val formatter = SimpleDateFormat(FALLBACK_DATE_PATTERN, Locale.US)
    return formatter.format(Date(this))
}

@PreviewLightDark
@Composable
private fun InactivityFallbackSelectedPreview() {
    InheritanceFallbackSettingsContent(
        remainTime = 20,
        selectedOption = InheritanceFallbackOption.INACTIVITY_FALLBACK,
        triggerValue = "5",
        triggerUnit = FallbackTriggerUnit.YEAR,
        showTriggerUnitMenu = false,
    )
}

@PreviewLightDark
@Composable
private fun DateBasedFallbackSelectedPreview() {
    InheritanceFallbackSettingsContent(
        remainTime = 20,
        selectedOption = InheritanceFallbackOption.DATE_BASED_FALLBACK,
        triggerValue = "5",
        triggerUnit = FallbackTriggerUnit.YEAR,
        fallbackDate = "05/29/2050",
        showTriggerUnitMenu = false,
    )
}

@PreviewLightDark
@Composable
private fun InactivityDropdownExpandedPreview() {
    InheritanceFallbackSettingsContent(
        remainTime = 20,
        selectedOption = InheritanceFallbackOption.INACTIVITY_FALLBACK,
        triggerValue = "1",
        triggerUnit = FallbackTriggerUnit.YEAR,
        showTriggerUnitMenu = true,
    )
}
