package com.nunchuk.android.main.membership.byzantine.intro

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.nunchuk.android.compose.NcPrimaryDarkButton
import com.nunchuk.android.compose.NcTopAppBar
import com.nunchuk.android.compose.NunchukTheme
import com.nunchuk.android.core.util.flowObserver
import com.nunchuk.android.main.R
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class GroupPendingIntroFragment : Fragment() {
    private val args: GroupPendingIntroFragmentArgs by navArgs()
    private val viewModel: GroupPendingIntroViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                GroupPendingIntroScreen(viewModel = viewModel) {
                    findNavController().navigate(
                        GroupPendingIntroFragmentDirections.actionGroupPendingIntroFragmentToAddByzantineKeyListFragment(
                            groupId = args.groupId,
                            isAddOnly = true
                        )
                    )
                }
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        flowObserver(viewModel.state) { state ->
            if (state.isViewPendingWallet) {
                findNavController().navigate(
                    GroupPendingIntroFragmentDirections.actionGroupPendingIntroFragmentToAddByzantineKeyListFragment(
                        args.groupId
                    )
                )
                viewModel.markHandleViewPendingWallet()
            }
        }
    }
}

@Composable
private fun GroupPendingIntroScreen(
    viewModel: GroupPendingIntroViewModel = viewModel(),
    onContinue: () -> Unit = {},
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    GroupPendingIntroContent(uiState = state, onContinue = onContinue)
}

@Composable
private fun GroupPendingIntroContent(
    uiState: GroupPendingIntroUiState = GroupPendingIntroUiState(),
    onContinue: () -> Unit = {},
) {
    NunchukTheme {
        Scaffold(
            modifier = Modifier
                .statusBarsPadding()
                .navigationBarsPadding(),
            topBar = {
                NcTopAppBar(title = "", isBack = false)
            },
            bottomBar = {
                NcPrimaryDarkButton(
                    modifier = Modifier
                        .padding(16.dp)
                        .fillMaxWidth(),
                    onClick = onContinue
                ) {
                    Text(text = stringResource(id = R.string.nc_text_continue))
                }
            },
        ) { innerPadding ->
            Column(
                Modifier
                    .padding(paddingValues = innerPadding)
                    .padding(horizontal = 16.dp)
            ) {
                Text(
                    text = stringResource(R.string.nc_group_wallet_creation_pending),
                    style = NunchukTheme.typography.heading
                )
                Text(
                    modifier = Modifier.padding(top = 16.dp),
                    text = stringResource(
                        R.string.nc_group_wallet_creation_pending_desc,
                        uiState.masterName
                    ),
                    style = NunchukTheme.typography.body
                )

            }
        }
    }
}

@Preview
@Composable
private fun GroupPendingIntroScreenPreview() {
    GroupPendingIntroContent(

    )
}