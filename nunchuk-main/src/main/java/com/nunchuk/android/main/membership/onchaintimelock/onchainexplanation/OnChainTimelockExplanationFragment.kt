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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import androidx.navigation.fragment.findNavController
import com.nunchuk.android.compose.HighlightMessageType
import com.nunchuk.android.compose.NcHintMessage
import com.nunchuk.android.compose.NcIcon
import com.nunchuk.android.compose.NcImageAppBar
import com.nunchuk.android.compose.NcPrimaryDarkButton
import com.nunchuk.android.compose.NunchukTheme
import com.nunchuk.android.main.R
import com.nunchuk.android.share.membership.MembershipFragment

class OnChainTimelockExplanationFragment : MembershipFragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)

            setContent {
                OnChainTimelockExplanationScreen(
                    onContinueClicked = {
                        findNavController().navigate(
                            OnChainTimelockExplanationFragmentDirections.actionOnChainTimelockExplanationFragmentToOnChainTimelockAddKeyListFragment()
                        )
                    }
                )
            }
        }
    }
}

@Composable
private fun OnChainTimelockExplanationScreen(onContinueClicked: () -> Unit) {
    OnChainTimelockExplanationContent(
        onContinueClicked = onContinueClicked
    )
}

@Composable
private fun OnChainTimelockExplanationContent(
    onContinueClicked: () -> Unit = {}
) {
    NunchukTheme {
        Scaffold(
            modifier = Modifier.navigationBarsPadding(),
            topBar = {
                NcImageAppBar(
                    backgroundRes = R.drawable.bg_inheritance_onchain_offchain,
                    actions = {
                        IconButton(onClick = { /* TODO: Handle more actions */ }) {
                            NcIcon(
                                painter = painterResource(R.drawable.ic_more),
                                contentDescription = "More options"
                            )
                        }
                    }
                )
            },
            bottomBar = {
                Column(
                    modifier = Modifier.padding(vertical = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    NcHintMessage(
                        modifier = Modifier
                            .padding(horizontal = 16.dp)
                            .fillMaxWidth(),
                        type = HighlightMessageType.HINT
                    ) {
                        Text(
                            text = stringResource(R.string.nc_keys_added_twice_hint),
                            style = NunchukTheme.typography.bodySmall
                        )
                    }
                    NcPrimaryDarkButton(
                        modifier = Modifier
                            .padding(horizontal = 16.dp)
                            .fillMaxWidth(),
                        onClick = onContinueClicked
                    ) {
                        Text(
                            text = stringResource(id = R.string.nc_text_continue),
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            },
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .padding(innerPadding)
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                // Main heading
                Text(
                    text = stringResource(R.string.nc_how_onchain_timelock_works),
                    style = NunchukTheme.typography.heading
                )

                // Main explanation
                Text(
                    text = stringResource(R.string.nc_onchain_wallet_spending_paths_desc),
                    style = NunchukTheme.typography.body
                )

                // Bullet points
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    BulletPoint(
                        text = stringResource(R.string.nc_before_timelock_desc)
                    )
                    BulletPoint(
                        text = stringResource(R.string.nc_after_timelock_desc)
                    )
                }

                // Instruction about inheritance key
                Text(
                    text = stringResource(R.string.nc_select_inheritance_key_instruction),
                    style = NunchukTheme.typography.body
                )
            }
        }
    }
}

@Composable
private fun BulletPoint(
    text: String,
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
        onContinueClicked = { }
    )
}
