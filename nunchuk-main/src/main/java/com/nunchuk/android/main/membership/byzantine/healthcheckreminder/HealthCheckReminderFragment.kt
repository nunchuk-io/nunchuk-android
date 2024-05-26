package com.nunchuk.android.main.membership.byzantine.healthcheckreminder

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalViewConfiguration
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.clearFragmentResult
import androidx.fragment.app.setFragmentResultListener
import androidx.fragment.app.viewModels
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.fragment.findNavController
import com.nunchuk.android.compose.NcCircleImage
import com.nunchuk.android.compose.NcPrimaryDarkButton
import com.nunchuk.android.compose.NcTag
import com.nunchuk.android.compose.NcTopAppBar
import com.nunchuk.android.compose.NunchukTheme
import com.nunchuk.android.compose.border
import com.nunchuk.android.core.signer.SignerModel
import com.nunchuk.android.core.util.flowObserver
import com.nunchuk.android.core.util.showError
import com.nunchuk.android.core.util.showOrHideLoading
import com.nunchuk.android.core.util.showSuccess
import com.nunchuk.android.core.util.toReadableDrawableResId
import com.nunchuk.android.core.util.toReadableSignerType
import com.nunchuk.android.main.R
import com.nunchuk.android.main.membership.byzantine.groupdashboard.GroupDashboardState
import com.nunchuk.android.main.membership.byzantine.groupdashboard.GroupDashboardViewModel
import com.nunchuk.android.model.HealthReminder
import com.nunchuk.android.model.byzantine.AssistedWalletRole
import com.nunchuk.android.model.byzantine.KeyHealthStatus
import com.nunchuk.android.model.byzantine.isKeyHolderLimited
import com.nunchuk.android.model.isNone
import com.nunchuk.android.model.toHealthReminderFrequency
import com.nunchuk.android.model.toReadableString
import com.nunchuk.android.nav.NunchukNavigator
import com.nunchuk.android.share.membership.MembershipFragment
import com.nunchuk.android.type.SignerType
import com.nunchuk.android.utils.healthCheckLabel
import com.nunchuk.android.utils.healthCheckTimeColor
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class HealthCheckReminderFragment : MembershipFragment() {
    @Inject
    lateinit var navigator: NunchukNavigator

    private val groupDashboardViewModel: GroupDashboardViewModel by activityViewModels()
    private val viewModel: HealthCheckReminderViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?,
    ): View {
        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)

            setContent {
                val groupDashboardState by groupDashboardViewModel.state.collectAsStateWithLifecycle()
                val state by viewModel.state.collectAsStateWithLifecycle()
                HealthCheckReminderContent(
                    groupDashboardState = groupDashboardState,
                    state = state,
                    onBackPress = {
                        if (viewModel.switchEditMode(false).not()) {
                            findNavController().popBackStack()
                        }
                    },
                    onHealthCheckChanged = { signer, isChecked ->
                        viewModel.selectHealthCheck(isChecked, signer.fingerPrint)
                    },
                    onContinueClick = {
                        findNavController().navigate(
                            HealthCheckReminderFragmentDirections.actionHealthCheckReminderReminderFragmentToHealthCheckReminderBottomSheet(
                                null
                            )
                        )
                    },
                    onAddClick = {
                        viewModel.switchEditMode(true)
                    },
                    onEditHealthCheckReminder = {
                        findNavController().navigate(
                            HealthCheckReminderFragmentDirections.actionHealthCheckReminderReminderFragmentToHealthCheckReminderBottomSheet(
                                it
                            )
                        )
                    }
                )
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewModel.init(groupDashboardViewModel.getGroupId(), groupDashboardViewModel.getWalletId())
        setFragmentResultListener(HealthCheckReminderBottomSheet.REQUEST_KEY) { _, bundle ->
            val frequency =
                bundle.getString(HealthCheckReminderBottomSheet.EXTRA_HEALTH_REMINDER_FREQUENCY)
                    ?: return@setFragmentResultListener
            val startDate = bundle.getLong(HealthCheckReminderBottomSheet.EXTRA_START_DAY)
            val xfp = bundle.getString(HealthCheckReminderBottomSheet.EXTRA_XFP)
            if (frequency.toHealthReminderFrequency().isNone()) {
                viewModel.deleteHealthReminder(xfp)
            } else {
                viewModel.addOrUpdateHealthReminder(frequency, startDate, xfp)
            }
            clearFragmentResult(HealthCheckReminderBottomSheet.REQUEST_KEY)
        }

        flowObserver(viewModel.event) {
            when (it) {
                is HealthCheckReminderEvent.Error -> showError(message = it.message)
                HealthCheckReminderEvent.Success -> {
                    viewModel.switchEditMode(false)
                    showSuccess(message = getString(R.string.nc_reminders_updated))
                }

                is HealthCheckReminderEvent.Loading -> showOrHideLoading(it.loading)
            }
        }
    }
}

@Composable
fun HealthCheckReminderContent(
    groupDashboardState: GroupDashboardState = GroupDashboardState(),
    state: HealthCheckReminderState = HealthCheckReminderState(),
    onHealthCheckChanged: (SignerModel, Boolean) -> Unit = { _, _ -> },
    onBackPress: () -> Unit = {},
    onContinueClick: () -> Unit = {},
    onAddClick: () -> Unit = {},
    onEditHealthCheckReminder: (HealthReminder) -> Unit = {},
) {
    val signers by remember(groupDashboardState.myRole, groupDashboardState.signers) {
        derivedStateOf {
            if (groupDashboardState.myRole.isKeyHolderLimited ||
                groupDashboardState.myRole == AssistedWalletRole.KEYHOLDER
            ) {
                groupDashboardState.signers.filter { it.isVisible }
                    .filter { it.type != SignerType.SERVER }
            } else {
                groupDashboardState.signers.filter { it.type != SignerType.SERVER }
            }
        }
    }

    NunchukTheme {
        Scaffold(
            modifier = Modifier
                .navigationBarsPadding()
                .statusBarsPadding(),
            topBar = {
                NcTopAppBar(
                    title = "Add reminder",
                    textStyle = NunchukTheme.typography.titleLarge,
                    onBackPress = onBackPress,
                    actions = {
                        if (state.isEditMode) {
                            val isEnable =
                                state.healthReminders != null && signers.all { it.fingerPrint in state.healthReminders }
                                    .not()
                            Box(modifier = Modifier.padding(end = 16.dp)) {
                                Icon(
                                    modifier = Modifier
                                        .size(24.dp)
                                        .clickable { if (isEnable) onAddClick() },
                                    painter = painterResource(id = R.drawable.ic_add_dark),
                                    contentDescription = "Info",
                                    tint = if (isEnable) Color.Black else colorResource(
                                        id = R.color.nc_whisper_color
                                    )
                                )
                            }
                        } else {
                            Spacer(modifier = Modifier.size(LocalViewConfiguration.current.minimumTouchTargetSize))
                        }
                    }
                )
            },
            bottomBar = {
                if (state.isEditMode.not()) {
                    NcPrimaryDarkButton(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        onClick = onContinueClick,
                        enabled = state.selectedXfps.isNotEmpty(),
                    ) {
                        Text(text = stringResource(id = R.string.nc_text_continue))
                    }
                }
            }
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .padding(innerPadding)
                    .padding(horizontal = 16.dp)
                    .fillMaxHeight(),
            ) {
                if (state.isEditMode) {
                    Text(
                        text = stringResource(id = R.string.nc_keys_have_set_periodic_reminders),
                        style = NunchukTheme.typography.title
                    )
                } else {
                    Text(
                        text = stringResource(id = R.string.nc_select_which_keys_setup_reminders),
                        style = NunchukTheme.typography.body
                    )
                }
                if (state.healthReminders != null) {
                    LazyColumn(
                        modifier = Modifier
                            .padding(top = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.Top),
                    ) {
                        items(signers.filter { if (state.isEditMode) it.fingerPrint in state.healthReminders else it.fingerPrint !in state.healthReminders }) {
                            HealthCheckReminderItem(
                                signer = it,
                                isEditMode = state.isEditMode,
                                isSelect = state.selectedXfps.contains(it.fingerPrint),
                                status = groupDashboardState.keyStatus[it.fingerPrint],
                                onHealthCheckChanged = onHealthCheckChanged,
                                reminder = state.healthReminders[it.fingerPrint],
                                onEditHealthCheckReminder = onEditHealthCheckReminder
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun HealthCheckReminderItem(
    signer: SignerModel,
    status: KeyHealthStatus?,
    reminder: HealthReminder?,
    isEditMode: Boolean = false,
    isSelect: Boolean = false,
    onHealthCheckChanged: (SignerModel, Boolean) -> Unit = { _, _ -> },
    onEditHealthCheckReminder: (HealthReminder) -> Unit = {},
) {
    val context = LocalContext.current
    val label by remember(status?.lastHealthCheckTimeMillis) {
        derivedStateOf {
            status?.lastHealthCheckTimeMillis.healthCheckLabel(context)
        }
    }
    val color = status?.lastHealthCheckTimeMillis.healthCheckTimeColor()
    Column(
        modifier = Modifier
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.border,
                shape = RoundedCornerShape(size = 12.dp)
            )
            .padding(16.dp),
        horizontalAlignment = Alignment.Start,
    ) {

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(
                modifier = Modifier.weight(1f, fill = true),
                verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterVertically)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Row(
                        modifier = Modifier
                            .background(color = color, shape = RoundedCornerShape(size = 8.dp))
                            .padding(horizontal = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp, Alignment.Start),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = label,
                            style = NunchukTheme.typography.titleSmall.copy(fontSize = 10.sp)
                        )
                    }
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.Start),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    NcCircleImage(
                        resId = signer.toReadableDrawableResId(),
                        size = 48.dp,
                        iconSize = 24.dp,
                        color = color
                    )

                    Column(
                        verticalArrangement = Arrangement.spacedBy(4.dp, Alignment.Top),
                        horizontalAlignment = Alignment.Start,
                    ) {
                        Text(
                            text = signer.name,
                            style = NunchukTheme.typography.body
                        )
                        Row {
                            NcTag(label = signer.toReadableSignerType(context))
                            if (signer.isShowAcctX()) {
                                NcTag(
                                    modifier = Modifier.padding(start = 4.dp),
                                    label = stringResource(id = R.string.nc_acct_x, signer.index)
                                )
                            }
                        }
                        Text(
                            text = signer.getXfpOrCardIdLabel(),
                            style = NunchukTheme.typography.bodySmall
                        )
                    }
                }

                if (reminder != null && isEditMode) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                color = colorResource(id = R.color.nc_grey_light),
                                shape = RoundedCornerShape(12.dp)
                            )
                            .padding(top = 8.dp, bottom = 8.dp, start = 8.dp, end = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Row(
                            modifier = Modifier.weight(1f, fill = true),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                modifier = Modifier.size(24.dp),
                                painter = painterResource(id = R.drawable.ic_scheduling_tx),
                                contentDescription = ""
                            )
                            Text(
                                modifier = Modifier
                                    .padding(start = 4.dp),
                                text = "Every ${
                                    reminder.frequency.toHealthReminderFrequency()
                                        .toReadableString()
                                }",
                                style = NunchukTheme.typography.bodySmall
                            )
                        }
                        Text(
                            modifier = Modifier.clickable { onEditHealthCheckReminder(reminder) },
                            text = stringResource(R.string.nc_edit),
                            style = NunchukTheme.typography.title,
                            textDecoration = TextDecoration.Underline
                        )
                    }
                }
            }

            if (isEditMode.not()) {
                Checkbox(checked = isSelect, onCheckedChange = {
                    onHealthCheckChanged(signer, it)
                })
            }
        }
    }
}

@Preview
@Composable
private fun HealthCheckReminderScreenPreview() {
    HealthCheckReminderContent(
        groupDashboardState = GroupDashboardState(
            signers = arrayListOf(
                SignerModel(
                    "123",
                    "Tom’s TAPSIGNER",
                    fingerPrint = "79EB35F4",
                    derivationPath = "",
                    isMasterSigner = false,
                    index = 10
                )
            )
        )
    )
}