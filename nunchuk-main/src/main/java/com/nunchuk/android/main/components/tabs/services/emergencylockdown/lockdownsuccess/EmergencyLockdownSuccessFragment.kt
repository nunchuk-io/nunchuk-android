package com.nunchuk.android.main.components.tabs.services.emergencylockdown.lockdownsuccess

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
import com.nunchuk.android.compose.*
import com.nunchuk.android.core.util.flowObserver
import com.nunchuk.android.main.R

class LockdownSuccessFragment : Fragment() {

    private val viewModel: EmergencyLockdownSuccessViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?,
    ): View {
        return ComposeView(requireContext()).apply {
            setContent {
                LockdownSuccessScreen(viewModel)
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        flowObserver(viewModel.event) {
            when (it) {

            }
        }
    }
}

@OptIn(ExperimentalLifecycleComposeApi::class)
@Composable
fun LockdownSuccessScreen(
    viewModel: EmergencyLockdownSuccessViewModel = viewModel()
) {
    LockdownSuccessScreenContent()
}

@Composable
fun LockdownSuccessScreenContent(
    onGotItClick: () -> Unit = {},
) {
    NunchukTheme {
        Scaffold { innerPadding ->
            Column(
                modifier = Modifier
                    .padding(innerPadding)
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
                    text = stringResource(R.string.nc_emergency_lockdown_success_title),
                    style = NunchukTheme.typography.heading
                )
                Text(
                    modifier = Modifier.padding(start = 16.dp, top = 16.dp, end = 16.dp),
                    text = stringResource(id = R.string.nc_emergency_lockdown_success_info),
                    style = NunchukTheme.typography.body
                )

                Spacer(modifier = Modifier.weight(1.0f))

                NcPrimaryDarkButton(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    onClick = onGotItClick,
                ) {
                    Text(text = stringResource(id = R.string.nc_text_got_it))
                }
            }
        }
    }
}

@Preview
@Composable
private fun LockdownSuccessScreenContentPreview() {
    LockdownSuccessScreenContent()
}