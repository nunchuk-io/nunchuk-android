package com.nunchuk.android.main.membership.byzantine.healthcheckreminder

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LocalMinimumInteractiveComponentEnforcement
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.os.bundleOf
import androidx.fragment.app.setFragmentResult
import androidx.fragment.app.viewModels
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.fragment.navArgs
import com.nunchuk.android.compose.NcDatePickerDialog
import com.nunchuk.android.compose.NcPrimaryDarkButton
import com.nunchuk.android.compose.NcRadioButton
import com.nunchuk.android.compose.NcTextField
import com.nunchuk.android.compose.NunchukTheme
import com.nunchuk.android.compose.greyLight
import com.nunchuk.android.compose.textPrimary
import com.nunchuk.android.core.base.BaseComposeBottomSheet
import com.nunchuk.android.main.R
import com.nunchuk.android.model.HealthReminderFrequency
import com.nunchuk.android.model.isNone
import com.nunchuk.android.model.toReadableString
import com.nunchuk.android.utils.simpleGlobalDateFormat
import dagger.hilt.android.AndroidEntryPoint
import java.util.Calendar
import java.util.Date

@AndroidEntryPoint
class HealthCheckReminderBottomSheet : BaseComposeBottomSheet() {

    private val viewModel: HealthCheckReminderBottomSheetViewModel by viewModels()
    private val args: HealthCheckReminderBottomSheetArgs by navArgs()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?,
    ): View {
        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                NunchukTheme {
                    HealthCheckReminderScreen(
                        isSelectMultiple = args.selectMultipleKeys,
                        viewModel = viewModel,
                        onDoneClick = {
                            val startDate =
                                if (viewModel.getStartDate() == 0L) Calendar.getInstance().timeInMillis else viewModel.getStartDate()
                            setFragmentResult(
                                REQUEST_KEY,
                                bundleOf(
                                    EXTRA_HEALTH_REMINDER_FREQUENCY to it.name,
                                    EXTRA_START_DAY to startDate,
                                    EXTRA_XFP to args.selectHealthReminder?.xfp
                                )
                            )
                            dismissAllowingStateLoss()
                        })
                }
            }
        }
    }

    companion object {
        const val REQUEST_KEY = "HealthCheckReminderBottomSheet"
        const val EXTRA_HEALTH_REMINDER_FREQUENCY = "EXTRA_HEALTH_REMINDER_FREQUENCY"
        const val EXTRA_START_DAY = "EXTRA_START_DAY"
        const val EXTRA_XFP = "EXTRA_XFP"
    }
}

@Composable
fun HealthCheckReminderScreen(
    isSelectMultiple: Boolean = false,
    viewModel: HealthCheckReminderBottomSheetViewModel = viewModel(),
    onDoneClick: (HealthReminderFrequency) -> Unit = {},
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    HealthCheckReminderScreenContent(
        isSelectMultiple = isSelectMultiple,
        selectReminderFrequency = state.selectedReminder,
        startDate = state.startDate,
        onDoneClick = {
            onDoneClick(viewModel.getHealthReminderFrequency())
        },
        onCheckedChange = viewModel::selectReminder,
        onStartDateChange = viewModel::setStartDate,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HealthCheckReminderScreenContent(
    isSelectMultiple: Boolean = false,
    selectReminderFrequency: HealthReminderFrequency = HealthReminderFrequency.NONE,
    startDate: Long = 0,
    onCheckedChange: (HealthReminderFrequency) -> Unit = {},
    onDoneClick: () -> Unit = {},
    onStartDateChange: (Long) -> Unit = {},
) {

    var showDatePicker by rememberSaveable {
        mutableStateOf(false)
    }

    Column(
        modifier = Modifier
            .background(
                color = MaterialTheme.colorScheme.surface,
                shape = RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp)
            )
            .padding(horizontal = 12.dp, vertical = 24.dp)
    ) {
        Text(
            text = if (isSelectMultiple) stringResource(id = R.string.nc_how_often_would_you_like_heath_check_these_keys) else stringResource(
                id = R.string.nc_how_often_would_you_like_heath_check_this_key
            ),
            style = NunchukTheme.typography.title
        )
        LazyColumn(
            modifier = Modifier,
        ) {
            items(HealthReminderFrequency.entries) { reminder ->
                Row(
                    modifier = Modifier.padding(vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val title = if (reminder.isNone()) {
                        stringResource(id = R.string.nc_no_reminder)
                    } else {
                        stringResource(
                            id = R.string.nc_repeat_every_data,
                            reminder.toReadableString()
                        )
                    }
                    Text(
                        text = title,
                        style = NunchukTheme.typography.body,
                        modifier = Modifier
                            .weight(1f, true)
                            .padding(end = 12.dp)
                    )

                    CompositionLocalProvider(LocalMinimumInteractiveComponentEnforcement provides false) {
                        NcRadioButton(
                            selected = selectReminderFrequency == reminder,
                            onClick = {
                                onCheckedChange(reminder)
                            },
                        )
                    }
                }
            }
        }

        HorizontalDivider(thickness = 0.5.dp, modifier = Modifier.padding(top = 16.dp))

        NcTextField(
            modifier = Modifier
                .padding(top = 16.dp)
                .fillMaxWidth(),
            title = stringResource(R.string.nc_start_date),
            value = if (startDate > 0 && selectReminderFrequency != HealthReminderFrequency.NONE) Date(
                startDate
            ).simpleGlobalDateFormat() else stringResource(
                id = R.string.nc_activation_date_holder
            ),
            textStyle = NunchukTheme.typography.body.copy(
                color = if (selectReminderFrequency.isNone()) colorResource(
                    id = R.color.nc_boulder_color
                ) else MaterialTheme.colorScheme.textPrimary
            ),
            onValueChange = {},
            enabled = false,
            disableBackgroundColor = if (selectReminderFrequency.isNone()) MaterialTheme.colorScheme.greyLight else MaterialTheme.colorScheme.surface,
            onClick = {
                if (selectReminderFrequency.isNone().not()) showDatePicker = true
            },
        )

        NcPrimaryDarkButton(
            modifier = Modifier
                .padding(top = 24.dp)
                .fillMaxWidth(),
            onClick = onDoneClick
        ) {
            Text(text = stringResource(id = R.string.nc_text_done))
        }

        if (showDatePicker) {
            NcDatePickerDialog(
                onDismissRequest = { showDatePicker = false },
                onConfirm = { date ->
                    showDatePicker = false
                    onStartDateChange(date)
                }
            )
        }
    }
}

@Preview
@Composable
private fun HealthCheckReminderScreenContentPreview() {
    NunchukTheme {
        HealthCheckReminderScreenContent()
    }
}