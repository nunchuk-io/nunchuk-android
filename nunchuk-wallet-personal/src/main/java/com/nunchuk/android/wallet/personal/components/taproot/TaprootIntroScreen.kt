package com.nunchuk.android.wallet.personal.components.taproot

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import com.nunchuk.android.compose.NCLabelWithIndex
import com.nunchuk.android.compose.NcImageAppBar
import com.nunchuk.android.compose.NcPrimaryDarkButton
import com.nunchuk.android.compose.NcScaffold
import com.nunchuk.android.compose.NunchukTheme
import com.nunchuk.android.wallet.personal.R

const val TaprootIntroScreenRoute = "taproot_intro_screen"

fun NavGraphBuilder.taprootIntroScreen(
    openNextScreen: () -> Unit
) {
    composable(TaprootIntroScreenRoute) {
        TaprootIntroScreen(
            openNextScreen = openNextScreen
        )
    }
}

fun NavHostController.navigateTaprootIntroScreen() {
    navigate(TaprootIntroScreenRoute)
}

@Composable
fun TaprootIntroScreen(
    modifier: Modifier = Modifier,
    openNextScreen: () -> Unit
) {
    NunchukTheme {
        NcScaffold(
            modifier = modifier.navigationBarsPadding(),
            topBar = {
                NcImageAppBar(
                    backgroundRes = R.drawable.nc_bg_taproot_intro,
                )
            },
            bottomBar = {
                NcPrimaryDarkButton(
                    modifier = Modifier
                        .padding(16.dp)
                        .fillMaxWidth(),
                    onClick = openNextScreen,
                ) {
                    Text(stringResource(R.string.nc_text_continue))
                }
            }
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .padding(paddingValues)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Text(
                    modifier = Modifier.padding(top = 16.dp),
                    text = stringResource(R.string.nc_taproot_wallet),
                    style = NunchukTheme.typography.heading
                )
                NCLabelWithIndex(
                    modifier = Modifier.fillMaxWidth(),
                    index = 1,
                    title = stringResource(R.string.nc_single_signature_wallet),
                    label = stringResource(R.string.nc_single_signature_taproot_wallet_desc)
                )

                NCLabelWithIndex(
                    modifier = Modifier.fillMaxWidth(),
                    index = 2,
                    title = stringResource(R.string.nc_multisignature_wallet),
                    label = stringResource(R.string.nc_multisignature_taproot_wallet_desc)
                )
            }
        }
    }
}

@PreviewLightDark
@Composable
private fun TaprootIntroScreenPreview() {
    TaprootIntroScreen(
        openNextScreen = {}
    )
}