package com.nunchuk.android.settings.walletsecurity.decoy

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.nunchuk.android.compose.NcImageAppBar
import com.nunchuk.android.compose.NcPrimaryDarkButton
import com.nunchuk.android.compose.NcScaffold
import com.nunchuk.android.compose.NunchukTheme
import com.nunchuk.android.settings.R
import com.nunchuk.android.settings.walletsecurity.DecoyWalletSuccessRoute

@Composable
fun DecoyWalletSuccessView(
    modifier: Modifier = Modifier,
    onContinueClicked: () -> Unit = {}
) {
    NunchukTheme {
        NcScaffold(
            modifier = modifier.navigationBarsPadding(),
            topBar = {
                NcImageAppBar(backgroundRes = R.drawable.bg_create_decoy_wallet_success)
            },
            bottomBar = {
                NcPrimaryDarkButton(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    onClick = onContinueClicked
                ) {
                    Text(text = stringResource(id = R.string.nc_text_continue))
                }
            },
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .padding(innerPadding)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = stringResource(id = R.string.nc_text_decoy_wallet_success),
                    style = NunchukTheme.typography.heading,
                    modifier = Modifier.padding(top = 16.dp)
                )

                Text(
                    text = stringResource(id = R.string.nc_text_decoy_wallet_success_description),
                    style = NunchukTheme.typography.body
                )
            }
        }
    }
}

@Preview
@Composable
private fun DecoyWalletSuccessPreview() {
    DecoyWalletSuccessView()
}

fun NavController.navigateToDecoyWalletSuccess() {
    navigate(DecoyWalletSuccessRoute)
}

fun NavGraphBuilder.decoyWalletSuccessScreen(
    onContinueClick: () -> Unit,
) {
    composable<DecoyWalletSuccessRoute> {
        DecoyWalletSuccessView(
            onContinueClicked = onContinueClick,
        )
    }
}
