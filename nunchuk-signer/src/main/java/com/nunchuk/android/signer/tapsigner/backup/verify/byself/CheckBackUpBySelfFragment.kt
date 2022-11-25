package com.nunchuk.android.signer.tapsigner.backup.verify.byself

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.layout.*
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.material.TextButton
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
import androidx.lifecycle.flowWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.fragment.navArgs
import com.nunchuk.android.compose.*
import com.nunchuk.android.core.mapper.MasterSignerMapper
import com.nunchuk.android.core.share.IntentSharingController
import com.nunchuk.android.core.util.ClickAbleText
import com.nunchuk.android.core.util.orUnknownError
import com.nunchuk.android.core.util.showError
import com.nunchuk.android.share.membership.MembershipFragment
import com.nunchuk.android.share.membership.MembershipStepManager
import com.nunchuk.android.signer.R
import com.nunchuk.android.widget.NCWarningDialog
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class CheckBackUpBySelfFragment : MembershipFragment() {
    private val args: CheckBackUpBySelfFragmentArgs by navArgs()
    private val viewModel: CheckBackUpBySelfViewModel by viewModels()

    @Inject
    lateinit var masterSignerMapper: MasterSignerMapper

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        return ComposeView(requireContext()).apply {
            setContent {
                CheckBackUpBySelfScreen(viewModel, membershipStepManager)
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewLifecycleOwner.lifecycleScope.launchWhenStarted {
            viewModel.event.flowWithLifecycle(viewLifecycleOwner.lifecycle)
                .collect { event ->
                    when (event) {
                        OnExitSelfCheck -> requireActivity().finish()
                        OnDownloadBackUpClicked -> IntentSharingController.from(requireActivity())
                            .shareFile(args.filePath)
                        OnVerifiedBackUpClicked -> NCWarningDialog(requireActivity())
                            .showDialog(
                                title = getString(R.string.nc_confirmation),
                                message = getString(R.string.nc_confirm_verify_backup_by_self_desc),
                                onYesClick = {
                                    viewModel.setKeyVerified()
                                },
                            )
                        is ShowError -> showError(event.e?.message.orUnknownError())
                    }
                }
        }
    }
}

@OptIn(ExperimentalLifecycleComposeApi::class)
@Composable
private fun CheckBackUpBySelfScreen(
    viewModel: CheckBackUpBySelfViewModel = viewModel(),
    membershipStepManager: MembershipStepManager
) {
    val remainingTime by membershipStepManager.remainingTime.collectAsStateWithLifecycle()
    CheckBackUpBySelfContent(
        onBtnClicked = viewModel::onBtnClicked,
        remainingTime = remainingTime,
    )
}

@Composable
private fun CheckBackUpBySelfContent(
    remainingTime: Int = 0,
    onBtnClicked: (event: CheckBackUpBySelfEvent) -> Unit = {},
) {
    NunchukTheme {
        Scaffold { innerPadding ->
            Column(
                modifier = Modifier
                    .padding(innerPadding)
                    .statusBarsPadding()
                    .navigationBarsPadding(),
            ) {
                NcTopAppBar(stringResource(R.string.nc_estimate_remain_time, remainingTime))
                Text(
                    modifier = Modifier.padding(top = 24.dp, start = 16.dp, end = 16.dp),
                    text = stringResource(R.string.nc_verify_the_backup_yourself),
                    style = NunchukTheme.typography.heading
                )
                Text(
                    modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp),
                    text = stringResource(R.string.nc_verify_backup_yourself_desc),
                    style = NunchukTheme.typography.body,
                )
                NCLabelWithIndex(
                    modifier = Modifier.padding(
                        start = 16.dp,
                        end = 16.dp,
                        top = 24.dp
                    ),
                    index = 1,
                    label = stringResource(R.string.nc_verify_backup_yourself_step_one)
                )
                NCLabelWithIndex(
                    modifier = Modifier.padding(
                        start = 16.dp,
                        end = 16.dp,
                        top = 24.dp
                    ),
                    index = 2,
                    label = stringResource(R.string.nc_verify_backup_yourself_step_two)
                )
                NCLabelWithIndex(
                    modifier = Modifier.padding(
                        start = 16.dp,
                        end = 16.dp,
                        top = 24.dp
                    ),
                    index = 3,
                    label = stringResource(R.string.nc_verify_backup_yourself_step_three)
                )
                Spacer(modifier = Modifier.weight(1.0f))
                NcHintMessage(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    messages = listOf(ClickAbleText(content = stringResource(R.string.nc_self_verify_hint))),
                    type = HighlightMessageType.HINT,
                )
                NcPrimaryDarkButton(
                    modifier = Modifier
                        .padding(16.dp)
                        .fillMaxWidth(),
                    onClick = { onBtnClicked(OnDownloadBackUpClicked) },
                ) {
                    Text(text = stringResource(R.string.nc_download_backup_file))
                }
                NcOutlineButton(
                    modifier = Modifier
                        .padding(horizontal = 16.dp)
                        .fillMaxWidth(),
                    onClick = { onBtnClicked(OnVerifiedBackUpClicked) },
                ) {
                    Text(text = stringResource(R.string.nc_i_have_verified))
                }
                TextButton(
                    modifier = Modifier
                        .padding(16.dp)
                        .fillMaxWidth(),
                    onClick = { onBtnClicked(OnExitSelfCheck) },
                ) {
                    Text(text = stringResource(R.string.I_will_comeback_to_this_later))
                }
            }
        }
    }
}

@Preview
@Composable
private fun CheckBackUpBySelfScreenPreview() {
    CheckBackUpBySelfContent(

    )
}