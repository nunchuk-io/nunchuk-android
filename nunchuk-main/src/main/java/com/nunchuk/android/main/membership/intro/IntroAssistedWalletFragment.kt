package com.nunchuk.android.main.membership.intro

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.layout.*
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.fragment.findNavController
import com.nunchuk.android.compose.NcImageAppBar
import com.nunchuk.android.compose.NcPrimaryDarkButton
import com.nunchuk.android.compose.NunchukTheme
import com.nunchuk.android.core.util.flowObserver
import com.nunchuk.android.main.R
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class IntroAssistedWalletFragment : Fragment() {
    private val viewModel: IntroAssistedWalletViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        return ComposeView(requireContext()).apply {
            setContent {
                IntroAssistedWalletScreen(viewModel)
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        flowObserver(viewModel.event) {
            findNavController().navigate(IntroAssistedWalletFragmentDirections.actionIntroAssistedWalletFragmentToAddKeyStepFragment())
        }
    }
}

@Composable
fun IntroAssistedWalletScreen(viewModel: IntroAssistedWalletViewModel = viewModel()) =
    NunchukTheme {
        NunchukTheme {
            Scaffold { innerPadding ->
                Column(
                    modifier = Modifier
                        .padding(innerPadding)
                        .navigationBarsPadding()
                ) {
                    NcImageAppBar(backgroundRes = R.drawable.nc_bg_intro_assisted_wallet)
                    Text(
                        modifier = Modifier.padding(top = 24.dp, start = 16.dp, end = 16.dp),
                        text = stringResource(R.string.nc_welcome_assisted_wallet_title),
                        style = NunchukTheme.typography.heading
                    )
                    Text(
                        modifier = Modifier.padding(16.dp),
                        text = stringResource(R.string.nc_welcome_assisted_wallet_desc),
                        style = NunchukTheme.typography.body
                    )
                    Spacer(modifier = Modifier.weight(1.0f))
                    NcPrimaryDarkButton(modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                        onClick = { viewModel.onContinueClicked() }) {
                        Text(text = stringResource(id = R.string.nc_text_continue))
                    }
                }
            }
        }
    }

@Preview
@Composable
fun IntroAssistedWalletScreenPreview() {
    IntroAssistedWalletScreen()
}
