package com.nunchuk.android.main.components.tabs.services.inheritanceplanning.findbackup

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import androidx.lifecycle.flowWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.nunchuk.android.compose.NcHighlightText
import com.nunchuk.android.compose.NcImageAppBar
import com.nunchuk.android.compose.NcPrimaryDarkButton
import com.nunchuk.android.compose.NunchukTheme
import com.nunchuk.android.main.R
import com.nunchuk.android.share.membership.MembershipFragment
import dagger.hilt.android.AndroidEntryPoint

@OptIn(ExperimentalLifecycleComposeApi::class)
@AndroidEntryPoint
class FindBackupPasswordFragment : MembershipFragment() {
    private val viewModel: FindBackupPasswordViewModel by viewModels()
    private val args: FindBackupPasswordFragmentArgs by navArgs()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        return ComposeView(requireContext()).apply {
            setContent {
                val remainTime by viewModel.remainTime.collectAsStateWithLifecycle()
                FindBackupPasswordContent(remainTime) {
                    findNavController().navigate(
                        FindBackupPasswordFragmentDirections.actionFindBackupPasswordFragmentToInheritanceKeyTipFragment(
                            magicalPhrase = args.magicalPhrase
                        )
                    )
                }
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewLifecycleOwner.lifecycleScope.launchWhenStarted {
            viewModel.event.flowWithLifecycle(viewLifecycleOwner.lifecycle)
                .collect { event ->

                }
        }
    }
}

@Composable
private fun FindBackupPasswordContent(
    remainTime: Int = 0,
    onContinueClicked: () -> Unit = {},
) {
    NunchukTheme {
        Scaffold { innerPadding ->
            Column(
                modifier = Modifier
                    .padding(innerPadding)
                    .fillMaxHeight()
                    .verticalScroll(rememberScrollState())
                    .navigationBarsPadding()
            ) {
                NcImageAppBar(
                    backgroundRes = R.drawable.nc_bg_tap_signer_explain,
                    title = stringResource(
                        id = R.string.nc_estimate_remain_time,
                        remainTime
                    ),
                )
                Text(
                    modifier = Modifier.padding(top = 16.dp, start = 16.dp, end = 16.dp),
                    text = stringResource(R.string.nc_find_backup_password),
                    style = NunchukTheme.typography.heading
                )
                NcHighlightText(
                    modifier = Modifier.padding(top = 16.dp, start = 16.dp, end = 16.dp),
                    text = stringResource(R.string.nc_find_backup_password_desc),
                    style = NunchukTheme.typography.body
                )
                Spacer(modifier = Modifier.weight(1.0f))
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
}

@Preview
@Composable
private fun FindBackupPasswordScreenPreview() {
    FindBackupPasswordContent(

    )
}