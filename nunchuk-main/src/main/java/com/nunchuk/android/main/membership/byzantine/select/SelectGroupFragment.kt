package com.nunchuk.android.main.membership.byzantine.select

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.findNavController
import androidx.navigation.fragment.navArgs
import com.nunchuk.android.compose.NcIcon
import com.nunchuk.android.compose.NcPrimaryDarkButton
import com.nunchuk.android.compose.NcRadioOption
import com.nunchuk.android.compose.NcTag
import com.nunchuk.android.compose.NcTopAppBar
import com.nunchuk.android.compose.NunchukTheme
import com.nunchuk.android.compose.border
import com.nunchuk.android.core.util.flowObserver
import com.nunchuk.android.core.util.openExternalLink
import com.nunchuk.android.core.util.showOrHideLoading
import com.nunchuk.android.main.R
import com.nunchuk.android.model.MembershipStage
import com.nunchuk.android.model.byzantine.GroupWalletType
import com.nunchuk.android.model.wallet.WalletOption
import com.nunchuk.android.share.membership.MembershipFragment
import com.nunchuk.android.widget.NCInfoDialog
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class SelectGroupFragment : MembershipFragment() {
    private val viewModel: SelectGroupViewModel by activityViewModels()
    private val args: SelectGroupFragmentArgs by navArgs()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?,
    ): View {
        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                val uiState by viewModel.state.collectAsStateWithLifecycle()
                SelectGroupScreen(
                    uiState = uiState,
                    isPersonal = args.isPersonal,
                    onMoreClicked = ::handleShowMore,
                    onContinueClicked = { option ->
                        if (viewModel.checkGroupTypeAvailable(option.slug)) {
                            if (args.isPersonal) {
                                viewModel.setLocalMembershipPlan(option.slug, option.walletType)
                                nunchukNavigator.openMembershipActivity(
                                    activityContext = requireActivity(),
                                    groupStep = MembershipStage.NONE,
                                    isPersonalWallet = true,
                                    walletType = option.walletType
                                )
                                requireActivity().finish()
                            } else {
                                findNavController().navigate(
                                    SelectGroupFragmentDirections.actionSelectGroupFragmentToSelectWalletSetupFragment(
                                        groupType = option.walletType.name
                                    )
                                )
                            }
                        } else {
                            NCInfoDialog(requireActivity()).init(
                                message = getString(
                                    R.string.nc_run_out_of_byzantine_wallet,
                                    option.planName
                                ),
                                btnYes = getString(R.string.nc_take_me_there),
                                btnInfo = getString(R.string.nc_text_got_it),
                                onYesClick = {
                                    requireActivity().openExternalLink("https://nunchuk.io/my-plan")
                                }
                            ).show()
                        }
                    },
                )
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        flowObserver(viewModel.event) {
            when (it) {
                is SelectGroupEvent.Loading -> showOrHideLoading(it.isLoading)
            }
        }
    }

    override val allowRestartWizard: Boolean = false
}

@Composable
private fun SelectGroupScreen(
    uiState: SelectGroupUiState = SelectGroupUiState(),
    isPersonal: Boolean = false,
    onContinueClicked: (WalletOption) -> Unit = {},
    onMoreClicked: () -> Unit = {},
) {
    SelectGroupContent(
        isPersonal = isPersonal,
        options = if (isPersonal) uiState.personalOptions else uiState.groupOptions,
        onContinueClicked = onContinueClicked,
        onMoreClicked = onMoreClicked,
        defaultOption = uiState.defaultOption
    )
}

@Composable
private fun SelectGroupContent(
    isPersonal: Boolean = false,
    options: List<WalletOption> = emptyList(),
    onContinueClicked: (WalletOption) -> Unit = {},
    onMoreClicked: () -> Unit = {},
    defaultOption: WalletOption? = null,
) {
    var selectedOption by remember(defaultOption) {
        mutableStateOf(defaultOption)
    }
    NunchukTheme {
        Scaffold(modifier = Modifier
            .navigationBarsPadding()
            .statusBarsPadding(),
            bottomBar = {
                NcPrimaryDarkButton(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    enabled = selectedOption != null,
                    onClick = {
                        onContinueClicked(selectedOption!!)
                    }
                ) {
                    Text(text = stringResource(id = R.string.nc_text_continue))
                }
            }, topBar = {
                NcTopAppBar(title = "", actions = {
                    IconButton(onClick = onMoreClicked) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_more),
                            contentDescription = "More icon"
                        )
                    }
                })
            }) { innerPadding ->
            Column(modifier = Modifier.padding(innerPadding)) {
                Text(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    text = if (isPersonal) stringResource(R.string.nc_select_personal_wallet_type) else stringResource(R.string.nc_main_select_group_wallet_type),
                    style = NunchukTheme.typography.heading
                )
                Text(
                    modifier = Modifier.padding(start = 16.dp, top = 16.dp, end = 16.dp),
                    text = if (isPersonal) stringResource(R.string.nc_main_what_type_of_personal_wallet) else stringResource(R.string.nc_main_what_type_of_group_wallet),
                    style = NunchukTheme.typography.body
                )
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(options) {
                        GroupWalletTypeOptionView(
                            isSelected = selectedOption == it,
                            option = it,
                        ) {
                            selectedOption = it
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun GroupWalletTypeOptionView(
    modifier: Modifier = Modifier,
    isSelected: Boolean,
    option: WalletOption,
    onClick: () -> Unit = {},
) {
    NcRadioOption(modifier = modifier.fillMaxWidth(), isSelected = isSelected, onClick = onClick) {
        Row {
            if (option.walletType.allowInheritance) {
                ProBadgePlan(
                    modifier = Modifier.padding(end = 4.dp),
                    text = option.badge
                )
            } else {
                StandardBadgePlan(modifier = Modifier.padding(end = 4.dp))
            }
            if (option.recommended) {
                NcTag(
                    modifier = Modifier.padding(bottom = 4.dp),
                    label = stringResource(id = R.string.nc_recommended)
                )
            }
        }
        Text(text = option.name, style = NunchukTheme.typography.title)
        Text(
            modifier = Modifier.padding(top = 4.dp),
            text = option.description,
            style = NunchukTheme.typography.body
        )
    }
}

@Composable
fun ProBadgePlan(modifier: Modifier, text: String) {
    Row(
        modifier = modifier
            .background(
                color = colorResource(id = R.color.nc_beeswax_dark),
                shape = RoundedCornerShape(20.dp)
            )
            .padding(horizontal = 8.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            painter = painterResource(id = R.drawable.ic_check_badge),
            tint = Color.White,
            contentDescription = "Badge Icon"
        )
        Text(
            modifier = Modifier.padding(start = 4.dp),
            text = text,
            style = NunchukTheme.typography.bold.copy(
                color = Color.White,
                fontSize = 10.sp
            )
        )
    }
}

@Composable
fun StandardBadgePlan(modifier: Modifier) {
    Row(
        modifier = modifier
            .border(
                color = MaterialTheme.colorScheme.border,
                shape = RoundedCornerShape(20.dp),
                width = 1.dp,
            )
            .padding(horizontal = 8.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        NcIcon(
            painter = painterResource(id = R.drawable.ic_check_badge),
            contentDescription = "Badge Icon"
        )
        Text(
            modifier = Modifier.padding(start = 4.dp),
            text = stringResource(id = R.string.nc_standard),
            style = NunchukTheme.typography.bold.copy(fontSize = 10.sp)
        )
    }
}

@PreviewLightDark
@Composable
private fun SelectGroupScreenPreview() {
    SelectGroupContent(
        options = listOf(
            WalletOption(
                slug = "byzantine",
                recommended = true,
                name = "Byzantine",
                description = "Byzantine is a group wallet that requires 3 hardware keys to sign transactions.",
                badge = "PRO",
                walletType = GroupWalletType.TWO_OF_THREE,
                allowSoftKey = false,
                planName = "Byzantine"
            ),
            WalletOption(
                slug = "byzantine",
                recommended = true,
                name = "Byzantine",
                description = "Byzantine is a group wallet that requires 3 hardware keys to sign transactions.",
                badge = "PRO",
                walletType = GroupWalletType.TWO_OF_FOUR_MULTISIG,
                allowSoftKey = false,
                planName = "Byzantine"
            )
        )
    )
}