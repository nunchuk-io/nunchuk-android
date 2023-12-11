package com.nunchuk.android.main.membership.byzantine.key

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment.Companion.CenterHorizontally
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.fragment.app.Fragment
import com.nunchuk.android.compose.NcCircleImage
import com.nunchuk.android.compose.NcPrimaryDarkButton
import com.nunchuk.android.compose.NunchukTheme
import com.nunchuk.android.main.R

class ByzantineSetupServerKeySuccessFragment : Fragment() {
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return ComposeView(requireContext()).apply {
            setContent {
                BackHandler {
                    requireActivity().finish()
                }
                ByzantineSetupServerKeySuccessContent {
                    requireActivity().finish()
                }
            }
        }
    }
}

@Composable
fun ByzantineSetupServerKeySuccessContent(onContinue: () -> Unit = {}) {
    NunchukTheme {
        Scaffold(
            modifier = Modifier
                .statusBarsPadding()
                .navigationBarsPadding(),
            bottomBar = {
                NcPrimaryDarkButton(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    onClick = onContinue,
                ) {
                    Text(text = stringResource(id = R.string.nc_text_continue))
                }
            },
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .padding(innerPadding)
                    .padding(horizontal = 16.dp)
            ) {
                NcCircleImage(
                    modifier = Modifier
                        .align(CenterHorizontally)
                        .padding(top = 100.dp),
                    resId = R.drawable.ic_info_fill,
                    iconSize = 60.dp,
                    size = 96.dp
                )
                Text(
                    text = stringResource(R.string.nc_co_signing_activation),
                    modifier = Modifier.padding(top = 24.dp),
                    style = NunchukTheme.typography.heading
                )
                Text(
                    text = stringResource(R.string.nc_co_signing_activation_desc),
                    modifier = Modifier.padding(top = 16.dp),
                    style = NunchukTheme.typography.body
                )
            }
        }
    }
}

@Preview
@Composable
fun ByzantineSetupServerKeySuccessPreview() {
    ByzantineSetupServerKeySuccessContent()
}