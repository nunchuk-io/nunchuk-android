package com.nunchuk.android.main.components.tabs.services.keyrecovery.keyrecoverysuccess

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.compose.ExperimentalLifecycleComposeApi
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.fragment.navArgs
import com.nunchuk.android.compose.*
import com.nunchuk.android.core.util.ClickAbleText
import com.nunchuk.android.core.util.flowObserver
import com.nunchuk.android.main.R
import com.nunchuk.android.nav.AppNavigator
import com.nunchuk.android.nav.NunchukNavigator
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class KeyRecoverySuccessFragment : Fragment() {

    @Inject
    lateinit var navigator: NunchukNavigator

    private val viewModel: KeyRecoverySuccessViewModel by viewModels()
    private val args: KeyRecoverySuccessFragmentArgs by navArgs()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?,
    ): View {
        return ComposeView(requireContext()).apply {
            setContent {
                KeyRecoverySuccessScreen(viewModel)
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        flowObserver(viewModel.event) {
            when (it) {
                KeyRecoverySuccessEvent.GotItClick -> {
                    navigator.openSignerInfoScreen(
                        activityContext = requireActivity(),
                        id = args.signer.id,
                        masterFingerprint = args.signer.fingerPrint,
                        name = args.signer.name,
                        type = args.signer.type,
                        derivationPath = args.signer.derivationPath,
                        justAdded = true,
                        customMessage = getString(R.string.nc_tapsigner_has_been_recovered)
                    )
                    requireActivity().finish()
                }
            }
        }
    }
}

@Composable
fun KeyRecoverySuccessScreen(
    viewModel: KeyRecoverySuccessViewModel = viewModel()
) {
    KeyRecoverySuccessScreenContent(onGotItClick = {
        viewModel.onGotItClick()
    })
}

@Composable
fun KeyRecoverySuccessScreenContent(
    onGotItClick: () -> Unit = {},
    onReplaceMyKeyClick: () -> Unit = {}
) {
    NunchukTheme {
        Scaffold { innerPadding ->
            Column(
                modifier = Modifier
                    .padding(innerPadding)
                    .statusBarsPadding()
                    .navigationBarsPadding()
            ) {
                NcTopAppBar(title = "")
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.nc_green_stick),
                        contentDescription = ""
                    )
                }

                Text(
                    modifier = Modifier.padding(top = 16.dp, start = 16.dp, end = 16.dp),
                    text = stringResource(R.string.nc_tapsigner_recovered),
                    style = NunchukTheme.typography.heading
                )
                Text(
                    modifier = Modifier.padding(start = 16.dp, top = 16.dp, end = 16.dp),
                    text = stringResource(id = R.string.nc_tapsigner_recovered_desc),
                    style = NunchukTheme.typography.body
                )
                NCLabelWithIndex(
                    modifier = Modifier.padding(
                        start = 16.dp,
                        end = 16.dp,
                        top = 16.dp
                    ),
                    index = 1,
                    label = stringResource(R.string.nc_tapsigner_recovered_success_info_1)
                )
                NCLabelWithIndex(
                    modifier = Modifier.padding(
                        start = 16.dp,
                        end = 16.dp,
                        top = 24.dp
                    ),
                    index = 2,
                    label = stringResource(R.string.nc_tapsigner_recovered_success_info_2)
                )
                Spacer(modifier = Modifier.weight(1.0f))
                NcHintMessage(
                    modifier = Modifier.padding(top = 16.dp, end = 16.dp, start = 16.dp),
                    messages = listOf(ClickAbleText(content = stringResource(R.string.nc_tapsigner_recovered_success_warning))),
                    type = HighlightMessageType.WARNING,
                )
                NcPrimaryDarkButton(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    onClick = onGotItClick,
                ) {
                    Text(text = stringResource(id = R.string.nc_text_got_it))
                }
                NcOutlineButton(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 16.dp, end = 16.dp, bottom = 16.dp)
                        .height(48.dp),
                    onClick = onReplaceMyKeyClick,
                ) {
                    Text(
                        text = stringResource(R.string.nc_tapsigner_recovered_success_replace_my_key),
                    )
                }
            }
        }
    }
}

@Preview
@Composable
private fun KeyRecoverySuccessScreenContentPreview() {
    KeyRecoverySuccessScreenContent()
}