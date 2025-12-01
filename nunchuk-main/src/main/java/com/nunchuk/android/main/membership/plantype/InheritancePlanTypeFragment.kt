package com.nunchuk.android.main.membership.plantype

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import androidx.fragment.app.viewModels
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.fragment.findNavController
import com.nunchuk.android.compose.NcIcon
import com.nunchuk.android.compose.NcPrimaryDarkButton
import com.nunchuk.android.compose.NcRadioButtonOption
import com.nunchuk.android.compose.NcTopAppBar
import com.nunchuk.android.compose.NunchukTheme
import com.nunchuk.android.compose.lightGray
import com.nunchuk.android.compose.strokePrimary
import com.nunchuk.android.compose.textSecondary
import com.nunchuk.android.core.util.InheritancePlanType
import com.nunchuk.android.core.util.flowObserver
import com.nunchuk.android.main.MembershipNavigationDirections
import com.nunchuk.android.main.R
import com.nunchuk.android.main.membership.MembershipActivity
import com.nunchuk.android.main.membership.byzantine.ByzantineMemberFlow
import com.nunchuk.android.model.MembershipStage
import com.nunchuk.android.share.membership.MembershipFragment
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class InheritancePlanTypeFragment : MembershipFragment() {
    private val viewModel: InheritancePlanTypeViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)

            setContent {
                val uiState by viewModel.state.collectAsStateWithLifecycle()
                InheritancePlanTypeScreen(
                    viewModel = viewModel,
                    onContinueClicked = {
                        if (uiState.changeTimelockFlow != -1) {
                            findNavController().navigate(
                                InheritancePlanTypeFragmentDirections.actionInheritancePlanTypeFragmentToChangeTimeLockFragment(
                                    walletId = uiState.walletId ?: "",
                                    groupId = uiState.groupId,
                                    slug = uiState.slug,
                                    walletType = uiState.walletType,
                                    isPersonal = uiState.isPersonal,
                                    setupPreference = uiState.setupPreference,
                                    changeTimelockFlow = uiState.changeTimelockFlow
                                )
                            )
                        } else if (uiState.isPersonal) {
                            viewModel.onContinueClicked()
                        } else {
                            uiState.walletType?.let { walletType ->
                                uiState.selectedPlanType?.let { planType ->
                                    findNavController().navigate(
                                        MembershipNavigationDirections.actionGlobalByzantineInviteMembersFragment(
                                            groupId = "",
                                            members = emptyArray(),
                                            flow = ByzantineMemberFlow.SETUP,
                                            setupPreference = uiState.setupPreference ?: "",
                                            groupType = walletType,
                                            inheritancePlanType = planType.name
                                        )
                                    )
                                }
                            }
                        }
                    }
                )
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        flowObserver(viewModel.event) { event ->
            when (event) {
                is InheritancePlanTypeEvent.OnContinueClicked -> {
                    navigator.openMembershipActivity(
                        activityContext = requireActivity(),
                        groupStep = MembershipStage.NONE,
                        isPersonalWallet = true,
                        groupWalletType = viewModel.getWalletType(),
                        quickWalletParam = (requireActivity() as? MembershipActivity)?.quickWalletParam,
                        inheritanceType = event.selectedPlanType.name
                    )
                    requireActivity().finish()
                }
            }
        }
    }
}

@Composable
private fun InheritancePlanTypeScreen(
    viewModel: InheritancePlanTypeViewModel = viewModel(),
    onContinueClicked: () -> Unit = {}
) {
    val uiState by viewModel.state.collectAsStateWithLifecycle()
    InheritancePlanTypeContent(
        selectedPlanType = uiState.selectedPlanType,
        orderedPlanTypes = uiState.orderedPlanTypes,
        onPlanTypeSelected = viewModel::onPlanTypeSelected,
        onContinueClicked = onContinueClicked,
        changeTimelockFlow = uiState.changeTimelockFlow
    )
}

@Composable
private fun InheritancePlanTypeContent(
    selectedPlanType: InheritancePlanType? = InheritancePlanType.OFF_CHAIN,
    orderedPlanTypes: List<InheritancePlanType> = listOf(InheritancePlanType.ON_CHAIN, InheritancePlanType.OFF_CHAIN),
    onPlanTypeSelected: (InheritancePlanType) -> Unit = {},
    onContinueClicked: () -> Unit = {},
    changeTimelockFlow: Int = -1
) {
    val isChangeTimelockFlow = changeTimelockFlow != -1
    val buttonText = when (changeTimelockFlow) {
        0 -> stringResource(R.string.nc_change_to_on_chain_timelock_title)
        1 -> stringResource(R.string.nc_change_to_off_chain_timelock_title)
        else -> stringResource(id = R.string.nc_text_continue)
    }

    NunchukTheme {
        Scaffold(
            modifier = Modifier.navigationBarsPadding(),
            topBar = {
                NcTopAppBar(
                    title = ""
                )
            },
            bottomBar = {
                NcPrimaryDarkButton(
                    modifier = Modifier
                        .padding(16.dp)
                        .fillMaxWidth(),
                    onClick = onContinueClicked
                ) {
                    Text(
                        text = buttonText,
                        fontWeight = FontWeight.Bold
                    )
                }
            },
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .padding(innerPadding)
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = stringResource(if (isChangeTimelockFlow) R.string.nc_change_timelock_type else R.string.nc_select_inheritance_plan_type),
                    style = NunchukTheme.typography.heading
                )

                if (isChangeTimelockFlow) {
                    Text(
                        text = "Please review the differences between off-chain and on-chain options before changing the timelock.",
                        style = NunchukTheme.typography.body,
                    )
                }

                // Render options dynamically based on orderedPlanTypes
                orderedPlanTypes.forEach { planType ->
                    InheritancePlanTypeOption(
                        planType = planType,
                        isSelected = selectedPlanType == planType,
                        isChangeTimelockFlow = isChangeTimelockFlow,
                        onPlanTypeSelected = onPlanTypeSelected
                    )
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Image(
                        painter = painterResource(R.drawable.ic_wallet_chain_type),
                        contentDescription = null,
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = if (isChangeTimelockFlow) "Read the in-depth comparison." else stringResource(
                            R.string.nc_change_plan_later
                        ),
                        style = NunchukTheme.typography.titleSmall,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
private fun InheritancePlanTypeOption(
    planType: InheritancePlanType,
    isSelected: Boolean,
    isChangeTimelockFlow: Boolean,
    onPlanTypeSelected: (InheritancePlanType) -> Unit
) {
    when (planType) {
        InheritancePlanType.OFF_CHAIN -> {
            // Off-chain timelock option
            NcRadioButtonOption(
                modifier = Modifier.fillMaxWidth(),
                isSelected = isSelected,
                onClick = {
                    if (isChangeTimelockFlow.not()) {
                        onPlanTypeSelected(InheritancePlanType.OFF_CHAIN)
                    }
                },
                showRadioButton = isChangeTimelockFlow.not(),
                customBackgroundColor = if (isChangeTimelockFlow) MaterialTheme.colorScheme.lightGray else null
            ) {
                Column {
                    Row(
                        horizontalArrangement = Arrangement.Center
                    ) {
                        NcIcon(
                            painter = painterResource(R.drawable.ic_off_chain_timelock),
                            contentDescription = null,
                            modifier = Modifier.size(32.dp)
                        )

                        Column(modifier = Modifier.padding(start = 12.dp)) {
                            Text(
                                text = stringResource(R.string.nc_off_chain_timelock),
                                style = NunchukTheme.typography.title,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = stringResource(R.string.nc_managed_by_nunchuk),
                                style = NunchukTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.textSecondary,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }
                    }

                    HorizontalDivider(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp),
                        color = MaterialTheme.colorScheme.strokePrimary,
                        thickness = 1.dp
                    )

                    Text(
                        text = "Pros: ${stringResource(R.string.nc_off_chain_pros)}",
                        style = NunchukTheme.typography.body,
                    )
                    Text(
                        text = "Cons: ${stringResource(R.string.nc_off_chain_cons)}",
                        style = NunchukTheme.typography.body,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
            }
        }
        InheritancePlanType.ON_CHAIN -> {
            // On-chain timelock option
            NcRadioButtonOption(
                modifier = Modifier.fillMaxWidth(),
                isSelected = isSelected,
                onClick = {
                    if (isChangeTimelockFlow.not()) {
                        onPlanTypeSelected(InheritancePlanType.ON_CHAIN)
                    }
                },
                showRadioButton = isChangeTimelockFlow.not(),
                customBackgroundColor = if (isChangeTimelockFlow) MaterialTheme.colorScheme.lightGray else null
            ) {
                Column {
                    Row(
                        horizontalArrangement = Arrangement.Center
                    ) {
                        NcIcon(
                            painter = painterResource(R.drawable.ic_on_chain_timelock),
                            contentDescription = null,
                            modifier = Modifier.size(32.dp)
                        )

                        Column(modifier = Modifier.padding(start = 12.dp)) {
                            Text(
                                text = stringResource(R.string.nc_on_chain_timelock),
                                style = NunchukTheme.typography.title,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = stringResource(R.string.nc_enforced_on_bitcoin),
                                style = NunchukTheme.typography.caption,
                                color = MaterialTheme.colorScheme.textSecondary,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }
                    }

                    HorizontalDivider(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp),
                        color = MaterialTheme.colorScheme.strokePrimary,
                        thickness = 1.dp
                    )

                    Text(
                        text = "Pros: ${stringResource(R.string.nc_on_chain_pros)}",
                        style = NunchukTheme.typography.body,
                        modifier = Modifier.padding(top = 12.dp)
                    )
                    Text(
                        text = "Cons: ${stringResource(R.string.nc_on_chain_cons)}",
                        style = NunchukTheme.typography.body,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
            }
        }
    }
}

@PreviewLightDark
@Composable
private fun InheritancePlanTypeScreenPreview() {
    InheritancePlanTypeContent(
        changeTimelockFlow = 0
    )
}
