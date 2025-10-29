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
import androidx.compose.material3.Icon
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
                        if (uiState.changeTimelockFlow) {
                            findNavController().navigate(
                                InheritancePlanTypeFragmentDirections.actionInheritancePlanTypeFragmentToChangeTimeLockFragment(
                                    walletId = uiState.walletId ?: "",
                                    groupId = uiState.groupId,
                                    slug = uiState.slug,
                                    walletType = uiState.walletType,
                                    isPersonal = uiState.isPersonal,
                                    setupPreference = uiState.setupPreference
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
                        walletType = viewModel.getWalletType(),
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
        onPlanTypeSelected = viewModel::onPlanTypeSelected,
        onContinueClicked = onContinueClicked,
        changeTimelockFlow = uiState.changeTimelockFlow
    )
}

@Composable
private fun InheritancePlanTypeContent(
    selectedPlanType: InheritancePlanType? = InheritancePlanType.OFF_CHAIN,
    onPlanTypeSelected: (InheritancePlanType) -> Unit = {},
    onContinueClicked: () -> Unit = {},
    changeTimelockFlow: Boolean = false
) {
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
                        text = stringResource(id = R.string.nc_text_continue),
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
                    text = stringResource(R.string.nc_select_inheritance_plan_type),
                    style = NunchukTheme.typography.heading
                )

                // Off-chain timelock option
                NcRadioButtonOption(
                    modifier = Modifier.fillMaxWidth(),
                    isSelected = selectedPlanType == InheritancePlanType.OFF_CHAIN,
                    onClick = { 
                        if (changeTimelockFlow.not()) {
                            onPlanTypeSelected(InheritancePlanType.OFF_CHAIN)
                        }
                    },
                    showRadioButton = changeTimelockFlow.not()
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

                // On-chain timelock option
                NcRadioButtonOption(
                    modifier = Modifier.fillMaxWidth(),
                    isSelected = selectedPlanType == InheritancePlanType.ON_CHAIN,
                    onClick = { 
                        if (changeTimelockFlow.not()) {
                            onPlanTypeSelected(InheritancePlanType.ON_CHAIN)
                        }
                    },
                    showRadioButton = changeTimelockFlow.not()
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
                        text = stringResource(R.string.nc_change_plan_later),
                        style = NunchukTheme.typography.titleSmall,
                        modifier = Modifier.weight(1f)
                    )
                    Icon(
                        painter = painterResource(R.drawable.ic_arrow_right_new),
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}

@PreviewLightDark
@Composable
private fun InheritancePlanTypeScreenPreview() {
    InheritancePlanTypeContent()
}
