package com.nunchuk.android.settings.walletsecurity.decoy

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.fragment.app.Fragment
import androidx.fragment.compose.content
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.fragment.findNavController
import com.nunchuk.android.compose.NcHighlightText
import com.nunchuk.android.compose.NcImageAppBar
import com.nunchuk.android.compose.NcPrimaryDarkButton
import com.nunchuk.android.compose.NcScaffold
import com.nunchuk.android.compose.NunchukTheme
import com.nunchuk.android.settings.R
import com.nunchuk.android.widget.NCWarningDialog
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class DecoyWalletIntroFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = content {
        val viewModel = viewModel<DecoyWalletIntroViewModel>()
        val state by viewModel.state.collectAsStateWithLifecycle()
        DecoyWalletScreen {
            if (state.hasPin) {
                findNavController().navigate(DecoyWalletIntroFragmentDirections.actionDecoyWalletIntroFragmentToDecoyPinFragment())
            } else {
                NCWarningDialog(requireActivity())
                    .showDialog(
                        title = getString(R.string.nc_text_confirmation),
                        message = getString(R.string.nc_decoy_wallet_intro_warning),
                        onYesClick = {
                            findNavController().navigate(DecoyWalletIntroFragmentDirections.actionDecoyWalletIntroFragmentToWalletSecurityCreatePinFragment())
                        }
                    )
            }
        }
    }
}


@Composable
fun DecoyWalletScreen(
    onContinueClick: () -> Unit = {},
) {
    NunchukTheme {
        NcScaffold(
            modifier = Modifier.navigationBarsPadding(),
            topBar = {
                NcImageAppBar(
                    backgroundRes = R.drawable.bg_decoy_wallet_intro,
                )
            }, bottomBar = {
                NcPrimaryDarkButton(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    onClick = onContinueClick
                ) {
                    Text(stringResource(R.string.nc_text_continue))
                }
            }
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .padding(innerPadding)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    modifier = Modifier.padding(top = 16.dp),
                    text = "Decoy Wallet",
                    style = NunchukTheme.typography.heading,
                )

                NcHighlightText(
                    text = stringResource(R.string.nc_decoy_wallet_intro_desc),
                    style = NunchukTheme.typography.body,
                )
            }
        }
    }
}

@Preview
@Composable
private fun DecoyWalletScreenPreview() {
    DecoyWalletScreen()
}