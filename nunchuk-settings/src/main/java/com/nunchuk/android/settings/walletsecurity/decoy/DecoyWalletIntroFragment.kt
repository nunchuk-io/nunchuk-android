package com.nunchuk.android.settings.walletsecurity.decoy

import androidx.fragment.app.FragmentActivity
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.hilt.navigation.compose.hiltViewModel
import com.nunchuk.android.compose.NcHighlightText
import com.nunchuk.android.compose.NcImageAppBar
import com.nunchuk.android.compose.NcPrimaryDarkButton
import com.nunchuk.android.compose.NcScaffold
import com.nunchuk.android.compose.NunchukTheme
import com.nunchuk.android.settings.R
import com.nunchuk.android.settings.walletsecurity.DecoyWalletIntroRoute
import com.nunchuk.android.widget.NCWarningDialog

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

fun NavController.navigateToDecoyWalletIntro() {
    navigate(DecoyWalletIntroRoute)
}

fun NavGraphBuilder.decoyWalletIntroScreen(
    activity: FragmentActivity,
    onOpenDecoyPin: () -> Unit,
    onOpenCreatePin: () -> Unit,
) {
    composable<DecoyWalletIntroRoute> {
        val viewModel = hiltViewModel<DecoyWalletIntroViewModel>()
        val state by viewModel.state.collectAsStateWithLifecycle()
        DecoyWalletScreen {
            if (state.hasPin) {
                onOpenDecoyPin()
            } else {
                NCWarningDialog(activity)
                    .showDialog(
                        title = activity.getString(R.string.nc_text_confirmation),
                        message = activity.getString(R.string.nc_decoy_wallet_intro_warning),
                        onYesClick = onOpenCreatePin
                    )
            }
        }
    }
}
