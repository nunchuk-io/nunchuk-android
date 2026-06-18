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

package com.nunchuk.android.main.components.tabs.services.inheritanceplanning.releaseschedulestageedit

import com.nunchuk.android.main.components.tabs.services.inheritanceplanning.estimateRemainTimeTitle
import androidx.compose.foundation.background
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
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import com.nunchuk.android.compose.NcDatePickerDialog
import com.nunchuk.android.compose.NcHighlightText
import com.nunchuk.android.compose.NcPrimaryDarkButton
import com.nunchuk.android.compose.NcSnackBarHost
import com.nunchuk.android.compose.NcTextField
import com.nunchuk.android.compose.NcTimePickerDialog
import com.nunchuk.android.compose.NcToastType
import com.nunchuk.android.compose.NcTopAppBar
import com.nunchuk.android.compose.NunchukTheme
import com.nunchuk.android.compose.fillInputText
import com.nunchuk.android.compose.showNunchukSnackbar
import com.nunchuk.android.compose.textPrimary
import com.nunchuk.android.compose.textSecondary
import com.nunchuk.android.compose.timezone.NcTimeZoneField
import com.nunchuk.android.compose.whisper
import com.nunchuk.android.core.R as CoreR
import com.nunchuk.android.core.ui.TimeZoneDetail
import com.nunchuk.android.core.ui.toTimeZoneDetail
import com.nunchuk.android.main.R
import com.nunchuk.android.main.components.tabs.services.inheritanceplanning.releasescheduledetail.ReleaseScheduleDate
import com.nunchuk.android.main.components.tabs.services.inheritanceplanning.releasescheduledetail.ReleaseScheduleStage
import com.nunchuk.android.main.components.tabs.services.inheritanceplanning.releasescheduledetail.ReleaseScheduleTime
import com.nunchuk.android.main.components.tabs.services.inheritanceplanning.releasescheduledetail.ReleaseScheduleUiState
import com.nunchuk.android.compose.dialog.NcConfirmationDialog
import com.nunchuk.android.compose.lightGray
import com.nunchuk.android.widget.R as WidgetR
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.ZoneId
import java.util.Calendar
import java.util.TimeZone

private enum class StageEditStep {
    WITHDRAWAL_RULE,
    FIRST_WITHDRAWAL_DATE,
}

@Composable
internal fun InheritanceReleaseScheduleStageEditScreen(
    remainTime: Int,
    stage: ReleaseScheduleStage,
    previousStageDate: ReleaseScheduleDate? = null,
    nextStageDate: ReleaseScheduleDate? = null,
    isNewStage: Boolean = false,
    onBackClicked: () -> Unit = {},
    onDeleteClicked: (Int) -> Unit = {},
    onConfirmClicked: (ReleaseScheduleStage) -> Unit = {},
) {
    var currentStep by rememberSaveable(stage.id) { mutableStateOf(StageEditStep.WITHDRAWAL_RULE) }
    var draft by remember(stage.id) { mutableStateOf(stage.toDraft()) }
    var stepOneValidationError by rememberSaveable(stage.id) { mutableStateOf<StageEditValidationError?>(null) }
    var showDeleteConfirmation by rememberSaveable(stage.id) { mutableStateOf(false) }
    var showDatePicker by rememberSaveable(stage.id) { mutableStateOf(false) }
    var showTimePicker by rememberSaveable(stage.id) { mutableStateOf(false) }
    var showDateValidation by rememberSaveable(stage.id) { mutableStateOf(false) }
    val snackState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    val defaultTimeZone = remember(stage.id) {
        stage.timeZoneId.toTimeZoneDetail()
            ?: TimeZone.getDefault().id.toTimeZoneDetail()
            ?: TimeZoneDetail()
    }
    var selectedTimeZone by remember(stage.id) { mutableStateOf(defaultTimeZone) }
    val isStageOne = draft.stageNumber == 1
    val currentDate = remember(draft.timeZoneId) { currentReleaseScheduleDate(draft.timeZoneId) }
    val lastWithdrawalDate = remember(draft, stage) { draft.toUpdatedStage(stage).finalWithdrawalDate() }
    val dateValidationError = firstWithdrawalDateValidationError(
        firstWithdrawalDate = draft.firstWithdrawalDate,
        lastWithdrawalDate = lastWithdrawalDate,
        currentDate = currentDate,
        previousStageDate = previousStageDate,
        nextStageDate = nextStageDate,
    )
    val showDateError = showDateValidation && dateValidationError != null

    val showAmountPerReleaseError = stepOneValidationError == StageEditValidationError.AMOUNT_PER_RELEASE
    val showTotalAllocationError = stepOneValidationError == StageEditValidationError.TOTAL_STAGE_ALLOCATION
    val amountPerReleaseErrorMessage = stringResource(id = R.string.nc_release_schedule_amount_per_release_error)
    val totalAllocationErrorMessage = stringResource(id = R.string.nc_release_schedule_total_allocation_error)
    val firstBeforeCurrentMessage = stringResource(
        id = R.string.nc_release_schedule_first_withdrawal_current_error
    )
    val firstBeforePreviousMessage = stringResource(
        id = R.string.nc_release_schedule_first_withdrawal_later_error
    )
    val lastAfterFollowerMessage = stringResource(
        id = R.string.nc_release_schedule_last_withdrawal_sooner_error,
        lastWithdrawalDate.display()
    )

    NunchukTheme {
        Scaffold(
            topBar = {
                NcTopAppBar(
                    title = estimateRemainTimeTitle(remainTime),
                    onBackPress = onBackClicked
                )
            },
            snackbarHost = {
                NcSnackBarHost(state = snackState)
            },
            bottomBar = {
                when (currentStep) {
                    StageEditStep.WITHDRAWAL_RULE -> {
                        Column(
                            modifier = Modifier
                                .navigationBarsPadding()
                                .fillMaxWidth()
                                .padding(16.dp)
                        ) {
                            NcPrimaryDarkButton(
                                modifier = Modifier
                                    .fillMaxWidth(),
                                onClick = {
                                    val validationError = draft.firstValidationError()
                                    if (validationError == null) {
                                        stepOneValidationError = null
                                        currentStep = StageEditStep.FIRST_WITHDRAWAL_DATE
                                    } else {
                                        stepOneValidationError = validationError
                                        val message = when (validationError) {
                                            StageEditValidationError.AMOUNT_PER_RELEASE -> amountPerReleaseErrorMessage
                                            StageEditValidationError.TOTAL_STAGE_ALLOCATION -> totalAllocationErrorMessage
                                        }
                                        coroutineScope.launch {
                                            snackState.showNunchukSnackbar(
                                                message = message,
                                                type = NcToastType.ERROR
                                            )
                                        }
                                    }
                                }
                            ) {
                                Text(
                                    text = stringResource(
                                        id = if (isStageOne) {
                                            WidgetR.string.nc_text_continue
                                        } else {
                                            R.string.nc_release_schedule_set_first_withdrawal_date
                                        }
                                    )
                                )
                            }
                        }
                    }

                    StageEditStep.FIRST_WITHDRAWAL_DATE -> {
                        Column(
                            modifier = Modifier
                                .navigationBarsPadding()
                                .fillMaxWidth()
                                .padding(16.dp),
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Button(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(48.dp),
                                    onClick = {
                                        currentStep = StageEditStep.WITHDRAWAL_RULE
                                    },
                                    shape = RoundedCornerShape(44.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.lightGray,
                                        contentColor = MaterialTheme.colorScheme.textPrimary
                                    )
                                ) {
                                    Text(
                                        text = stringResource(id = R.string.nc_release_schedule_back),
                                        style = NunchukTheme.typography.title
                                    )
                                }

                                NcPrimaryDarkButton(
                                    modifier = Modifier.weight(1f),
                                    enabled = !showDateError,
                                    onClick = {
                                        val error = dateValidationError
                                        if (error != null) {
                                            showDateValidation = true
                                            val message = when (error) {
                                                StageDateValidationError.FIRST_BEFORE_CURRENT -> firstBeforeCurrentMessage
                                                StageDateValidationError.FIRST_BEFORE_PREVIOUS -> firstBeforePreviousMessage
                                                StageDateValidationError.LAST_AFTER_FOLLOWER -> lastAfterFollowerMessage
                                            }
                                            coroutineScope.launch {
                                                snackState.showNunchukSnackbar(
                                                    message = message,
                                                    type = NcToastType.ERROR
                                                )
                                            }
                                        } else {
                                            onConfirmClicked(draft.toUpdatedStage(stage))
                                        }
                                    }
                                ) {
                                    Text(text = stringResource(id = WidgetR.string.nc_text_confirm))
                                }
                            }
                        }
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
                StageHeader(
                    stageNumber = draft.stageNumber,
                    step = currentStep,
                    onDeleteClicked = { showDeleteConfirmation = true }
                )

                when (currentStep) {
                    StageEditStep.WITHDRAWAL_RULE -> {
                        WithdrawalRuleStep(
                            draft = draft,
                            isStageOne = isStageOne,
                            showAmountPerReleaseError = showAmountPerReleaseError,
                            showTotalAllocationError = showTotalAllocationError,
                            onAmountPerReleaseChanged = { input ->
                                stepOneValidationError = null
                                draft = draft.copy(amountPerReleasePercent = input.filter(Char::isDigit).take(3))
                            },
                            onRepeatEveryChanged = { input ->
                                stepOneValidationError = null
                                draft = draft.copy(repeatEvery = input.filter(Char::isDigit).take(3))
                            },
                            onRepeatUnitSelected = { repeatUnit ->
                                stepOneValidationError = null
                                draft = draft.copy(repeatUnit = repeatUnit)
                            },
                            onTotalAllocationChanged = { input ->
                                stepOneValidationError = null
                                draft = draft.copy(totalStageAllocationPercent = input.filter(Char::isDigit).take(3))
                            }
                        )
                    }

                    StageEditStep.FIRST_WITHDRAWAL_DATE -> {
                        FirstWithdrawalDateStep(
                            draft = draft,
                            isStageOne = isStageOne,
                            showDateError = showDateError,
                            selectedTimeZone = selectedTimeZone,
                            onTimeZoneSelected = { timeZone ->
                                selectedTimeZone = timeZone
                                draft = draft.copy(timeZoneId = timeZone.id)
                            },
                            onDateClick = { showDatePicker = true },
                            onTimeClick = { showTimePicker = true },
                        )
                    }
                }

                Spacer(modifier = Modifier.padding(bottom = 24.dp))
            }
        }
    }

    if (showDeleteConfirmation) {
        NcConfirmationDialog(
            title = stringResource(id = R.string.nc_release_schedule_delete_stage_title, draft.stageNumber),
            message = stringResource(id = R.string.nc_release_schedule_delete_stage_message),
            positiveButtonText = stringResource(id = WidgetR.string.nc_text_confirm),
            negativeButtonText = stringResource(id = CoreR.string.nc_cancel),
            onPositiveClick = {
                showDeleteConfirmation = false
                if (isNewStage) {
                    onBackClicked()
                } else {
                    onDeleteClicked(draft.stageId)
                }
            },
            onDismiss = {
                showDeleteConfirmation = false
            },
        )
    }

    if (showDatePicker) {
        NcDatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            defaultDate = draft.firstWithdrawalDate.toCalendarMillis(draft.timeZoneId),
            onConfirm = { selectedMillis ->
                draft = draft.copy(
                    firstWithdrawalDate = selectedMillis.toReleaseScheduleDate(draft.timeZoneId)
                )
                showDateValidation = false
                showDatePicker = false
            },
            convertLocalToUtc = true,
        )
    }

    if (showTimePicker) {
        NcTimePickerDialog(
            onDismissRequest = { showTimePicker = false },
            initialHour = draft.firstWithdrawalTime.hour,
            initialMinute = draft.firstWithdrawalTime.minute,
            onConfirm = { hour, minute ->
                draft = draft.copy(firstWithdrawalTime = ReleaseScheduleTime(hour = hour, minute = minute))
                showTimePicker = false
            }
        )
    }
}

@Composable
private fun StageHeader(
    stageNumber: Int,
    step: StageEditStep,
    onDeleteClicked: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = stringResource(id = R.string.nc_release_schedule_stage_only, stageNumber),
            style = NunchukTheme.typography.heading
        )
        Spacer(modifier = Modifier.weight(1f))
        Text(
            modifier = Modifier.clickable(onClick = onDeleteClicked),
            text = stringResource(id = R.string.nc_release_schedule_delete_stage),
            style = NunchukTheme.typography.title.copy(textDecoration = TextDecoration.Underline)
        )
    }

    Row(
        modifier = Modifier.padding(top = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        repeat(2) { index ->
            val isActive = when (step) {
                StageEditStep.WITHDRAWAL_RULE -> index == 0
                StageEditStep.FIRST_WITHDRAWAL_DATE -> index == 1
            }
            Box(
                modifier = Modifier
                    .width(56.dp)
                    .height(4.dp)
                    .background(
                        color = if (isActive) MaterialTheme.colorScheme.textPrimary else MaterialTheme.colorScheme.whisper,
                        shape = RoundedCornerShape(999.dp)
                    )
            )
        }
    }
}

@Composable
private fun WithdrawalRuleStep(
    draft: ReleaseScheduleStageDraft,
    isStageOne: Boolean,
    showAmountPerReleaseError: Boolean,
    showTotalAllocationError: Boolean,
    onAmountPerReleaseChanged: (String) -> Unit,
    onRepeatEveryChanged: (String) -> Unit,
    onRepeatUnitSelected: (ReleaseRepeatUnit) -> Unit,
    onTotalAllocationChanged: (String) -> Unit,
) {
    Text(
        modifier = Modifier.padding(top = 16.dp),
        text = stringResource(
            id = if (isStageOne) {
                R.string.nc_release_schedule_withdrawal_rule
            } else {
                R.string.nc_release_schedule_withdrawal_plan
            }
        ),
        style = NunchukTheme.typography.title.copy(fontWeight = FontWeight.Bold)
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 16.dp)
            .background(
                color = MaterialTheme.colorScheme.lightGray,
                shape = RoundedCornerShape(12.dp)
            )
            .padding(16.dp)
    ) {
        Text(
            text = stringResource(id = R.string.nc_release_schedule_withdrawal_rule_desc),
            style = NunchukTheme.typography.body
        )
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        NcTextField(
            modifier = Modifier.weight(1f),
            title = stringResource(id = R.string.nc_release_schedule_amount_per_release),
            value = draft.amountPerReleasePercent,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            hasError = showAmountPerReleaseError,
            rightContent = {
                Text(
                    modifier = Modifier.padding(end = 12.dp),
                    text = "%",
                    style = NunchukTheme.typography.heading
                )
            },
            onValueChange = onAmountPerReleaseChanged
        )

        RepeatEveryField(
            modifier = Modifier.weight(1f),
            repeatEvery = draft.repeatEvery,
            selectedUnit = draft.repeatUnit,
            onRepeatEveryChanged = onRepeatEveryChanged,
            onRepeatUnitSelected = onRepeatUnitSelected,
        )
    }

    NcTextField(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 16.dp),
        title = stringResource(id = R.string.nc_release_schedule_total_stage_allocation),
        value = draft.totalStageAllocationPercent,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        hasError = showTotalAllocationError,
        rightContent = {
            Text(
                modifier = Modifier.padding(end = 12.dp),
                text = "%",
                style = NunchukTheme.typography.heading
            )
        },
        onValueChange = onTotalAllocationChanged
    )
}

@Composable
private fun RepeatEveryField(
    modifier: Modifier = Modifier,
    repeatEvery: String,
    selectedUnit: ReleaseRepeatUnit,
    onRepeatEveryChanged: (String) -> Unit,
    onRepeatUnitSelected: (ReleaseRepeatUnit) -> Unit,
) {
    var showUnitMenu by rememberSaveable { mutableStateOf(false) }
    var fieldWidthDp by remember { mutableStateOf(0.dp) }
    val density = LocalDensity.current

    Box(
        modifier = modifier.onGloballyPositioned { coordinates ->
            fieldWidthDp = with(density) {
                coordinates.size.width.toDp()
            }
        }
    ) {
        NcTextField(
            title = stringResource(id = R.string.nc_release_schedule_repeat_every),
            value = repeatEvery,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            rightContent = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = selectedUnit.label(),
                        style = NunchukTheme.typography.body.copy(color = MaterialTheme.colorScheme.textSecondary)
                    )
                    Icon(
                        modifier = Modifier
                            .padding(start = 8.dp, end = 12.dp)
                            .clickable { showUnitMenu = !showUnitMenu },
                        painter = painterResource(
                            id = if (showUnitMenu) {
                                WidgetR.drawable.ic_caret_up
                            } else {
                                WidgetR.drawable.ic_caret_down
                            }
                        ),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.textPrimary
                    )
                }
            },
            onValueChange = { onRepeatEveryChanged(it.filter(Char::isDigit).take(3)) }
        )
        DropdownMenu(
            expanded = showUnitMenu,
            modifier = Modifier
                .then(if (fieldWidthDp > 0.dp) Modifier.width(fieldWidthDp) else Modifier),
            onDismissRequest = { showUnitMenu = false },
            shape = RoundedCornerShape(20.dp),
            containerColor = MaterialTheme.colorScheme.lightGray,
            tonalElevation = 0.dp,
            shadowElevation = 12.dp
        ) {
            listOf(
                ReleaseRepeatUnit.YEAR,
                ReleaseRepeatUnit.MONTH,
                ReleaseRepeatUnit.WEEK,
                ReleaseRepeatUnit.DAY,
            ).forEach { unit ->
                DropdownMenuItem(
                    text = {
                        Text(
                            text = unit.label(),
                            style = NunchukTheme.typography.body
                        )
                    },
                    contentPadding = PaddingValues(horizontal = 20.dp, vertical = 18.dp),
                    onClick = {
                        onRepeatUnitSelected(unit)
                        showUnitMenu = false
                    }
                )
            }
        }
    }
}

@Composable
private fun FirstWithdrawalDateStep(
    draft: ReleaseScheduleStageDraft,
    isStageOne: Boolean,
    showDateError: Boolean,
    selectedTimeZone: TimeZoneDetail,
    onTimeZoneSelected: (TimeZoneDetail) -> Unit,
    onDateClick: () -> Unit,
    onTimeClick: () -> Unit,
) {
    Text(
        modifier = Modifier.padding(top = 16.dp),
        text = stringResource(id = R.string.nc_release_schedule_first_withdrawal_date_title),
        style = NunchukTheme.typography.title.copy(fontWeight = FontWeight.Bold)
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 16.dp)
            .background(
                color = MaterialTheme.colorScheme.lightGray,
                shape = RoundedCornerShape(12.dp)
            )
            .padding(16.dp)
    ) {
        NcHighlightText(
            text = stringResource(
                id = if (isStageOne) {
                    R.string.nc_release_schedule_first_withdrawal_date_desc
                } else {
                    R.string.nc_release_schedule_first_withdrawal_date_stage_2_desc
                }
            ),
            style = NunchukTheme.typography.body
        )
    }

    if (isStageOne) {
        NcTimeZoneField(
            modifier = Modifier.padding(top = 16.dp),
            selectedTimeZone = selectedTimeZone,
            onTimeZoneSelected = onTimeZoneSelected
        )
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        NcTextField(
            modifier = Modifier.weight(1f),
            title = stringResource(id = R.string.nc_release_schedule_first_withdrawal_date_field),
            value = draft.firstWithdrawalDate.display(),
            readOnly = true,
            enabled = false,
            disableBackgroundColor = MaterialTheme.colorScheme.fillInputText,
            hasError = showDateError,
            onClick = onDateClick,
            rightContent = {
                Icon(
                    modifier = Modifier
                        .padding(end = 12.dp)
                        .clickable(onClick = onDateClick),
                    painter = painterResource(id = WidgetR.drawable.ic_calendar_blank),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.textPrimary
                )
            },
            onValueChange = {}
        )

        NcTextField(
            modifier = Modifier.weight(1f),
            title = stringResource(id = CoreR.string.nc_time),
            value = draft.firstWithdrawalTime.display(),
            readOnly = true,
            enabled = false,
            disableBackgroundColor = MaterialTheme.colorScheme.fillInputText,
            onClick = onTimeClick,
            rightContent = {
                Icon(
                    modifier = Modifier
                        .padding(end = 12.dp)
                        .clickable(onClick = onTimeClick),
                    painter = painterResource(id = WidgetR.drawable.ic_clock),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.textPrimary
                )
            },
            onValueChange = {}
        )
    }
}

@Composable
private fun ReleaseRepeatUnit.label(): String {
    return when (this) {
        ReleaseRepeatUnit.DAY -> stringResource(id = R.string.nc_release_schedule_repeat_day)
        ReleaseRepeatUnit.WEEK -> stringResource(id = R.string.nc_release_schedule_repeat_week)
        ReleaseRepeatUnit.MONTH -> stringResource(id = R.string.nc_release_schedule_repeat_month)
        ReleaseRepeatUnit.YEAR -> stringResource(id = R.string.nc_release_schedule_repeat_year)
    }
}

private fun ReleaseScheduleDate.toCalendarMillis(timeZoneId: String): Long {
    return Calendar.getInstance(TimeZone.getTimeZone(timeZoneId)).apply {
        set(Calendar.YEAR, year)
        set(Calendar.MONTH, month - 1)
        set(Calendar.DAY_OF_MONTH, day)
        set(Calendar.HOUR_OF_DAY, 12)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis
}

private fun Long.toReleaseScheduleDate(timeZoneId: String): ReleaseScheduleDate {
    val calendar = Calendar.getInstance(TimeZone.getTimeZone(timeZoneId)).apply {
        timeInMillis = this@toReleaseScheduleDate
    }
    return ReleaseScheduleDate(
        month = calendar.get(Calendar.MONTH) + 1,
        day = calendar.get(Calendar.DAY_OF_MONTH),
        year = calendar.get(Calendar.YEAR),
    )
}

private fun currentReleaseScheduleDate(timeZoneId: String): ReleaseScheduleDate {
    val zoneId = runCatching {
        if (timeZoneId.isBlank()) ZoneId.systemDefault() else ZoneId.of(timeZoneId)
    }.getOrDefault(ZoneId.systemDefault())
    val today = LocalDate.now(zoneId)
    return ReleaseScheduleDate(
        month = today.monthValue,
        day = today.dayOfMonth,
        year = today.year,
    )
}

@PreviewLightDark
@Composable
private fun InheritanceReleaseScheduleStageEditScreenPreview() {
    InheritanceReleaseScheduleStageEditScreen(
        remainTime = 16,
        stage = ReleaseScheduleUiState.defaultStages().first(),
    )
}
