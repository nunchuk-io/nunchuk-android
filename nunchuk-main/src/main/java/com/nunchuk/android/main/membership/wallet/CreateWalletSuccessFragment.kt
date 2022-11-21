package com.nunchuk.android.main.membership.wallet

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.layout.*
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.fragment.app.viewModels
import androidx.lifecycle.compose.ExperimentalLifecycleComposeApi
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.fragment.navArgs
import com.nunchuk.android.compose.NcHintMessage
import com.nunchuk.android.compose.NcImageAppBar
import com.nunchuk.android.compose.NcPrimaryDarkButton
import com.nunchuk.android.compose.NunchukTheme
import com.nunchuk.android.core.util.ClickAbleText
import com.nunchuk.android.core.util.flowObserver
import com.nunchuk.android.nav.NunchukNavigator
import com.nunchuk.android.share.membership.MembershipFragment
import com.nunchuk.android.share.membership.MembershipStepManager
import com.nunchuk.android.signer.R
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class CreateWalletSuccessFragment : MembershipFragment() {
    @Inject
    lateinit var navigator: NunchukNavigator

    private val args: CreateWalletSuccessFragmentArgs by navArgs()

    private val viewModel: CreateWalletSuccessViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        return ComposeView(requireContext()).apply {
            setContent {
                CreateWalletSuccessScreen(viewModel, membershipStepManager)
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        flowObserver(viewModel.event) {
            when (it) {
                CreateWalletSuccessEvent.ContinueStepEvent -> {
                    navigator.openWalletDetailsScreen(
                        requireActivity(),
                        args.walletId
                    )
                    requireActivity().finish()
                }
            }
        }
    }
}

@OptIn(ExperimentalLifecycleComposeApi::class)
@Composable
private fun CreateWalletSuccessScreen(
    viewModel: CreateWalletSuccessViewModel = viewModel(),
    membershipStepManager: MembershipStepManager,
) {
    val remainTime by membershipStepManager.remainingTime.collectAsStateWithLifecycle()
    CreateWalletSuccessScreenContent(viewModel::onContinueClicked, remainTime)
}

@Composable
fun CreateWalletSuccessScreenContent(onContinueClicked: () -> Unit = {}, remainTime: Int = 0) {
    NunchukTheme {
        Scaffold { innerPadding ->
            Column(
                modifier = Modifier
                    .padding(innerPadding)
                    .navigationBarsPadding()
            ) {
                NcImageAppBar(
                    backgroundRes = R.drawable.nc_bg_wallet_done,
                )
                Text(
                    modifier = Modifier.padding(top = 16.dp, start = 16.dp, end = 16.dp),
                    text = stringResource(R.string.nc_create_wallet_success),
                    style = NunchukTheme.typography.heading
                )
                Text(
                    modifier = Modifier.padding(16.dp),
                    text = stringResource(R.string.nc_create_wallet_success_desc),
                    style = NunchukTheme.typography.body
                )
                Spacer(modifier = Modifier.weight(1.0f))
                NcHintMessage(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    messages = listOf(ClickAbleText(stringResource(com.nunchuk.android.main.R.string.nc_cosigning_limit_hint)))
                )
                NcPrimaryDarkButton(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    onClick = onContinueClicked,
                ) {
                    Text(text = stringResource(id = R.string.nc_take_me_my_wallet))
                }
            }
        }
    }
}

@Preview
@Composable
private fun CreateWalletSuccessScreenPreview() {
    CreateWalletSuccessScreenContent()
}