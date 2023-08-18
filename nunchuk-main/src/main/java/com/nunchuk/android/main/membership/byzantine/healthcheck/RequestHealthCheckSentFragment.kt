package com.nunchuk.android.main.membership.byzantine.healthcheck

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.fragment.app.Fragment
import com.nunchuk.android.compose.NcCircleImage
import com.nunchuk.android.compose.NcPrimaryDarkButton
import com.nunchuk.android.compose.NcTopAppBar
import com.nunchuk.android.compose.NunchukTheme
import com.nunchuk.android.main.R
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class RequestHealthCheckSentFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?,
    ): View {
        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                RequestHealthCheckSentScreen {
                    requireActivity().onBackPressedDispatcher.onBackPressed()
                }
            }
        }
    }
}

@Composable
private fun RequestHealthCheckSentScreen(onGotItClicked: () -> Unit) {
    RequestHealthCheckSentContent(onGotItClicked = onGotItClicked)
}

@Composable
private fun RequestHealthCheckSentContent(
    onGotItClicked: () -> Unit = {},
) {
    NunchukTheme {
        Scaffold(topBar = {
            NcTopAppBar(title = "")
        }, bottomBar = {
            NcPrimaryDarkButton(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                onClick = onGotItClicked) {
                Text(text = stringResource(id = R.string.nc_text_got_it))
            }
        }) { innerPadding ->
            Column(modifier = Modifier
                .padding(innerPadding)
                .padding(horizontal = 16.dp)) {
                NcCircleImage(
                    modifier = Modifier.padding(top = 8.dp).align(Alignment.CenterHorizontally),
                    resId = R.drawable.ic_notifications,
                    iconSize = 60.dp,
                    size = 96.dp,
                    iconTintColor = colorResource(
                        id = R.color.nc_slime_dark
                    ),
                    color = colorResource(
                        id = R.color.nc_green_color
                    )
                )

                Text(
                    text = stringResource(R.string.nc_health_check_request_sent),
                    modifier = Modifier.padding(top = 16.dp),
                    style = NunchukTheme.typography.heading
                )

                Text(
                    text = stringResource(R.string.nc_health_check_request_sent_description),
                    modifier = Modifier.padding(top = 16.dp),
                    style = NunchukTheme.typography.body
                )
            }
        }
    }
}

@Preview
@Composable
private fun RequestHealthCheckSentScreenPreview() {
    RequestHealthCheckSentContent(

    )
}