package com.nunchuk.android.main.membership.byzantine.payment.frequent

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LocalMinimumInteractiveComponentEnforcement
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nunchuk.android.compose.NcCheckBox
import com.nunchuk.android.compose.NcDatePickerDialog
import com.nunchuk.android.compose.NcPrimaryDarkButton
import com.nunchuk.android.compose.NcRadioButton
import com.nunchuk.android.compose.NcSnackBarHost
import com.nunchuk.android.compose.NcSnackbarVisuals
import com.nunchuk.android.compose.NcTextField
import com.nunchuk.android.compose.NcToastType
import com.nunchuk.android.compose.NcTopAppBar
import com.nunchuk.android.compose.NunchukTheme
import com.nunchuk.android.compose.dialog.NcConfirmationDialog
import com.nunchuk.android.compose.whisper
import com.nunchuk.android.main.R
import com.nunchuk.android.main.membership.byzantine.payment.RecurringPaymentViewModel
import com.nunchuk.android.main.membership.byzantine.payment.toResId
import com.nunchuk.android.model.payment.PaymentFrequency
import com.nunchuk.android.utils.simpleGlobalDateFormat
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.Date

@Composable
fun PaymentFrequentRoute(
    viewModel: RecurringPaymentViewModel,
    openPaymentFeeRateScreen: () -> Unit,
) {
    val config by viewModel.config.collectAsStateWithLifecycle()
    PaymentFrequentScreen(
        frequency = config.frequency,
        onFrequencyChange = viewModel::onFrequencyChange,
        openPaymentFeeRateScreen = openPaymentFeeRateScreen,
        noEndDate = config.noEndDate,
        onNoEndDateChange = viewModel::onNoEndDateChange,
        startDate = config.startDate,
        onStartDateChange = viewModel::onStartDateChange,
        endDate = config.endDate,
        onEndDateChange = viewModel::onEndDateChange,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PaymentFrequentScreen(
    frequency: PaymentFrequency? = null,
    noEndDate: Boolean = false,
    startDate: Long = 0L,
    endDate: Long = 0L,
    onStartDateChange: (Long) -> Unit = {},
    onEndDateChange: (Long) -> Unit = {},
    onNoEndDateChange: (Boolean) -> Unit = {},
    onFrequencyChange: (PaymentFrequency) -> Unit = {},
    openPaymentFeeRateScreen: () -> Unit = {},
) {
    val calendar = Calendar.getInstance()
    var selectedDate by rememberSaveable {
        mutableLongStateOf(0L)
    }
    var showDatePicker by rememberSaveable {
        mutableStateOf(false)
    }
    var isStartDate by rememberSaveable {
        mutableStateOf(true)
    }
    var isShowDateInvalidDialog by rememberSaveable {
        mutableStateOf(false)
    }
    val snackState: SnackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current
    NunchukTheme {
        Scaffold(
            topBar = {
                NcTopAppBar(
                    title = stringResource(R.string.nc_add_recurring_payments),
                    textStyle = NunchukTheme.typography.titleLarge,
                )
            },
            bottomBar = {
                NcPrimaryDarkButton(
                    modifier = Modifier
                        .padding(16.dp)
                        .fillMaxWidth(),
                    onClick = {
                        if (!noEndDate && endDate <= startDate) {
                            coroutineScope.launch {
                                snackState.showSnackbar(
                                    NcSnackbarVisuals(
                                        type = NcToastType.ERROR,
                                        message = context.getString(R.string.nc_the_end_date_should_be_after_the_start_date),
                                    )
                                )
                            }
                        } else {
                            openPaymentFeeRateScreen()
                        }
                    },
                    enabled = frequency != null && startDate > 0L && (endDate > 0L || noEndDate),
                ) {
                    Text(text = stringResource(R.string.nc_text_continue))
                }
            },
            snackbarHost = {
                NcSnackBarHost(snackState)
            },
        ) { innerPadding ->
            CompositionLocalProvider(LocalMinimumInteractiveComponentEnforcement provides false) {
                Column(
                    modifier = Modifier
                        .padding(innerPadding)
                        .padding(horizontal = 16.dp)
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                ) {
                    Text(
                        modifier = Modifier.padding(top = 8.dp),
                        text = stringResource(R.string.nc_payment_set_frequency),
                        style = NunchukTheme.typography.body,
                    )

                    Text(
                        modifier = Modifier.padding(top = 24.dp),
                        text = stringResource(R.string.nc_repeat),
                        style = NunchukTheme.typography.title,
                    )

                    PaymentFrequency.entries.forEach {
                        FrequencyOption(
                            modifier = Modifier,
                            text = stringResource(id = it.toResId()),
                            isSelected = it == frequency,
                            onClick = { onFrequencyChange(it) }
                        )
                    }

                    Divider(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 24.dp),
                        thickness = 1.dp,
                    )

                    Text(
                        modifier = Modifier.padding(top = 24.dp),
                        text = stringResource(R.string.nc_date_range),
                        style = NunchukTheme.typography.title,
                    )

                    NcTextField(
                        modifier = Modifier
                            .padding(top = 16.dp)
                            .fillMaxSize(),
                        title = stringResource(R.string.nc_start_date),
                        value = if (startDate > 0) Date(startDate).simpleGlobalDateFormat() else stringResource(
                            id = R.string.nc_activation_date_holder
                        ),
                        onValueChange = {},
                        enabled = false,
                        onClick = {
                            isStartDate = true
                            showDatePicker = true
                        },
                    )

                    NcTextField(
                        modifier = Modifier
                            .padding(top = 16.dp)
                            .fillMaxSize(),
                        title = stringResource(R.string.nc_end_date),
                        value = if (endDate > 0) Date(endDate).simpleGlobalDateFormat() else stringResource(
                            id = R.string.nc_activation_date_holder
                        ),
                        onValueChange = {},
                        enabled = false,
                        onClick = {
                            if (!noEndDate) {
                                isStartDate = false
                                showDatePicker = true
                            }
                        },
                        readOnly = noEndDate,
                        disableBackgroundColor = if (noEndDate) MaterialTheme.colorScheme.whisper else MaterialTheme.colorScheme.background,
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text(
                            text = stringResource(R.string.nc_no_end_date),
                            style = NunchukTheme.typography.body,
                        )
                        NcCheckBox(checked = noEndDate, onCheckedChange = onNoEndDateChange)
                    }
                }
            }

            if (showDatePicker) {
                NcDatePickerDialog(
                    onDismissRequest = { showDatePicker = false },
                    onConfirm = { date ->
                        showDatePicker = false
                        if (isStartDate) {
                            if (calendar.apply { timeInMillis = date }
                                    .get(Calendar.DAY_OF_MONTH) in listOf(29, 30, 31)) {
                                selectedDate = date
                                isShowDateInvalidDialog = true
                            } else {
                                isShowDateInvalidDialog = false
                                onStartDateChange(date)
                            }
                        } else {
                            onEndDateChange(date)
                        }
                    }
                )
            }
            if (isShowDateInvalidDialog) {
                NcConfirmationDialog(
                    title = stringResource(R.string.info),
                    message = stringResource(R.string.invalid_start_date_message),
                    positiveButtonText = stringResource(R.string.nc_change_date),
                    negativeButtonText = stringResource(id = R.string.nc_text_got_it),
                    onPositiveClick = {
                        showDatePicker = true
                    },
                    onDismiss = {
                        onStartDateChange(selectedDate)
                        isShowDateInvalidDialog = false
                    }
                )
            }
        }
    }
}

@Composable
private fun FrequencyOption(
    modifier: Modifier = Modifier,
    text: String,
    isSelected: Boolean = false,
    onClick: () -> Unit = {},
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(text = text, style = NunchukTheme.typography.body)
        NcRadioButton(selected = isSelected, onClick = onClick)
    }
}

@Preview
@Composable
fun PaymentFrequentScreenPreview() {
    PaymentFrequentScreen()
}