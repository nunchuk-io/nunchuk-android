package com.nunchuk.android.signer.tapsigner.backup.explain

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
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.nunchuk.android.compose.NcHintMessage
import com.nunchuk.android.compose.NcImageAppBar
import com.nunchuk.android.compose.NcPrimaryDarkButton
import com.nunchuk.android.compose.NunchukTheme
import com.nunchuk.android.core.util.ClickAbleText
import com.nunchuk.android.core.util.flowObserver
import com.nunchuk.android.share.membership.MembershipFragment
import com.nunchuk.android.share.membership.MembershipStepManager
import com.nunchuk.android.signer.R
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class TapSignerBackUpExplainFragment : MembershipFragment() {
    private val args: TapSignerBackUpExplainFragmentArgs by navArgs()
    private val viewModel: TapSignerBackUpExplainViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        return ComposeView(requireContext()).apply {
            setContent {
                TapSignerBackUpExplainScreen(viewModel, membershipStepManager)
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        flowObserver(viewModel.event) { event ->
            when (event) {
                OnContinueClicked -> {
                    findNavController().navigate(
                        TapSignerBackUpExplainFragmentDirections.actionTapSignerBackUpExplainFragmentToTapSignerVerifyBackUpOptionFragment(
                            args.filePath,
                            args.masterSignerId
                        )
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalLifecycleComposeApi::class)
@Composable
private fun TapSignerBackUpExplainScreen(
    viewModel: TapSignerBackUpExplainViewModel = viewModel(),
    membershipStepManager: MembershipStepManager
) {
    val remainTime by membershipStepManager.remainingTime.collectAsStateWithLifecycle()
    TapSignerBackUpExplainContent(
        onContinueClicked = viewModel::onContinueClicked,
        remainTime = remainTime
    )
}

@Composable
private fun TapSignerBackUpExplainContent(
    onContinueClicked: () -> Unit = {},
    remainTime: Int = 0,
) = NunchukTheme {
    Scaffold { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .navigationBarsPadding(),
        ) {
            NcImageAppBar(
                backgroundRes = R.drawable.nc_bg_tap_signer_explain,
                title = stringResource(id = R.string.nc_estimate_remain_time, remainTime),
            )
            Text(
                modifier = Modifier.padding(top = 24.dp, start = 16.dp, end = 16.dp),
                text = stringResource(R.string.nc_what_is_the_tap_signer_backup),
                style = NunchukTheme.typography.heading
            )
            Text(
                modifier = Modifier.padding(16.dp),
                text = stringResource(R.string.nc_tap_signer_back_up_explain_desc),
                style = NunchukTheme.typography.body
            )
            Spacer(modifier = Modifier.weight(1.0f))
            NcHintMessage(
                modifier = Modifier.padding(horizontal = 16.dp),
                messages = listOf(ClickAbleText(content = stringResource(R.string.nc_back_up_tap_signer_hint)))
            )
            NcPrimaryDarkButton(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                onClick = onContinueClicked,
            ) {
                Text(text = stringResource(id = R.string.nc_text_continue))
            }
        }
    }
}

@Preview
@Composable
private fun UploadBackUpTapSignerScreenPreview() {
    TapSignerBackUpExplainContent()
}
