package com.nunchuk.android.main.membership.onchaintimelock.onchainexplanation

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import androidx.fragment.app.viewModels
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.nunchuk.android.compose.NcIcon
import com.nunchuk.android.compose.NcImageAppBar
import com.nunchuk.android.compose.NcPrimaryDarkButton
import com.nunchuk.android.compose.NunchukTheme
import com.nunchuk.android.main.R
import com.nunchuk.android.main.membership.MembershipActivity
import com.nunchuk.android.share.membership.MembershipFragment
import kotlin.getValue

class OnChainTimelockExplanationFragment : MembershipFragment() {

    private val args: OnChainTimelockExplanationFragmentArgs by navArgs()

    private val viewModel: OnChainTimelockExplanationViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)

            setContent {
                val activity = requireActivity() as? MembershipActivity
                val groupId = activity?.groupId.orEmpty()
                val allowInheritance = args.config?.allowInheritance ?: false

                OnChainTimelockExplanationScreen(
                    groupId = groupId,
                    allowInheritance = allowInheritance,
                    viewModel = viewModel,
                    onContinueClicked = {
                        if (groupId.isNotEmpty()) {
                            val role = args.role.orEmpty()
                            findNavController().navigate(
                                OnChainTimelockExplanationFragmentDirections.actionOnChainTimelockExplanationFragmentToOnChainTimelockByzantineAddKeyFragment(
                                    groupId = groupId,
                                    role = role,
                                    isAddOnly = false
                                )
                            )
                        } else {
                            findNavController().navigate(
                                OnChainTimelockExplanationFragmentDirections.actionOnChainTimelockExplanationFragmentToOnChainTimelockAddKeyListFragment()
                            )
                        }
                    },
                    onMoreClicked = ::handleShowMore
                )
            }
        }
    }
}

@Composable
private fun OnChainTimelockExplanationScreen(
    viewModel: OnChainTimelockExplanationViewModel = viewModel(),
    groupId: String,
    allowInheritance: Boolean,
    onContinueClicked: () -> Unit,
    onMoreClicked: () -> Unit = {}
) {
    val remainTime by viewModel.remainTime.collectAsStateWithLifecycle()

    OnChainTimelockExplanationContent(
        groupId = groupId,
        remainTime = remainTime,
        allowInheritance = allowInheritance,
        onContinueClicked = onContinueClicked,
        onMoreClicked = onMoreClicked
    )
}

@Composable
private fun OnChainTimelockExplanationContent(
    groupId: String,
    remainTime: Int = 0,
    allowInheritance: Boolean,
    onContinueClicked: () -> Unit = {},
    onMoreClicked: () -> Unit = {}
) {
    val isGroupWallet = groupId.isNotEmpty()
    
    NunchukTheme {
        Scaffold(
            modifier = Modifier.navigationBarsPadding(),
            topBar = {
                NcImageAppBar(
                    backgroundRes = if (isGroupWallet) {
                        if (allowInheritance) {
                            R.drawable.illustration_on_chain_group_wallet
                        } else {
                            R.drawable.illustration_on_chain_without_inheritance_group_wallet
                        }
                    } else {
                        R.drawable.bg_inheritance_onchain_offchain
                    },
                    title = if (remainTime <= 0) "" else stringResource(
                        id = R.string.nc_estimate_remain_time,
                        remainTime
                    ),
                    actions = {
                        IconButton(onClick = onMoreClicked) {
                            NcIcon(
                                painter = painterResource(R.drawable.ic_more),
                                contentDescription = "More options"
                            )
                        }
                    }
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
                // Main heading
                Text(
                    text = stringResource(R.string.nc_how_onchain_timelock_works),
                    style = NunchukTheme.typography.heading
                )

                // Bullet points
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = stringResource(R.string.nc_onchain_wallet_spending_paths_desc),
                        style = NunchukTheme.typography.body
                    )
                    BulletPoint(
                        text = buildAnnotatedString {
                            withStyle(style = SpanStyle(fontWeight = FontWeight.Bold)) {
                                append(stringResource(R.string.nc_before_timelock))
                                append(" ")
                            }
                            append(if (isGroupWallet) stringResource(R.string.nc_before_timelock_group_desc) else stringResource(R.string.nc_before_timelock_desc))
                        }
                    )
                    BulletPoint(
                        text = buildAnnotatedString {
                            withStyle(style = SpanStyle(fontWeight = FontWeight.Bold)) {
                                append(stringResource(R.string.nc_after_timelock))
                                append(" ")
                            }
                            append(if (isGroupWallet) stringResource(R.string.nc_after_timelock_group_desc) else stringResource(R.string.nc_after_timelock_desc))
                        }
                    )
                }

                // Instruction about inheritance key (only show when inheritance is allowed)
                if (allowInheritance) {
                    Text(
                        text = if (isGroupWallet) stringResource(R.string.nc_select_inheritance_key_group_instruction) else stringResource(R.string.nc_select_inheritance_key_instruction),
                        style = NunchukTheme.typography.body
                    )
                }
            }
        }
    }
}

@Composable
private fun BulletPoint(
    text: AnnotatedString,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.Top
    ) {
        Text(
            text = "•",
            style = NunchukTheme.typography.body,
            modifier = Modifier.padding(end = 8.dp, top = 2.dp)
        )
        Text(
            text = text,
            style = NunchukTheme.typography.body,
            modifier = Modifier.weight(1f)
        )
    }
}

@PreviewLightDark
@Composable
private fun OnChainTimelockExplanationScreenPreview() {
    OnChainTimelockExplanationContent(
        groupId = "3232",
        allowInheritance = true,
        onContinueClicked = { },
        onMoreClicked = { }
    )
}
