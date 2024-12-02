package com.nunchuk.android.wallet.personal.components.taproot

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import com.nunchuk.android.compose.NCLabelWithIndex
import com.nunchuk.android.compose.NcCircleImage
import com.nunchuk.android.compose.NcPrimaryDarkButton
import com.nunchuk.android.compose.NcScaffold
import com.nunchuk.android.compose.NcTopAppBar
import com.nunchuk.android.compose.NunchukTheme
import com.nunchuk.android.wallet.personal.R

const val TaprootWarningScreenRoute = "taproot_warning_screen"

fun NavGraphBuilder.taprootWarningScreen(
    openNextScreen: () -> Unit
) {
    composable(TaprootWarningScreenRoute) {
        TaprootWarningScreen(
            openNextScreen = openNextScreen
        )
    }
}

fun NavHostController.navigateTaprootWarningScreen() {
    navigate(TaprootWarningScreenRoute)
}

@Composable
fun TaprootWarningScreen(
    modifier: Modifier = Modifier,
    openNextScreen: () -> Unit
) {
    NunchukTheme {
        NcScaffold(
            modifier = modifier.navigationBarsPadding(),
            topBar = {
                NcTopAppBar(
                    title = stringResource(R.string.nc_text_warning),
                    textStyle = NunchukTheme.typography.titleLarge
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
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                NcCircleImage(
                    modifier = Modifier.padding(top = 24.dp),
                    size = 96.dp,
                    iconSize = 60.dp,
                    resId = R.drawable.ic_warning_outline
                )
                Text(
                    modifier = Modifier.padding(top = 8.dp),
                    text = stringResource(R.string.nc_taproot_warning_title),
                    style = NunchukTheme.typography.body
                )
                NCLabelWithIndex(
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                    index = 1,
                    title = stringResource(R.string.nc_wallet_taproot_withdraw_support_title),
                    label = stringResource(R.string.nc_wallet_taproot_withdraw_support_desc)
                )

                NCLabelWithIndex(
                    modifier = Modifier.fillMaxWidth(),
                    index = 2,
                    title = stringResource(R.string.nc_wallet_taproot_hardware_support_title),
                    label = stringResource(R.string.nc_wallet_taproot_hardware_support_desc)
                )
            }
        }
    }
}

@PreviewLightDark
@Composable
private fun TaprootWarningScreenPreview() {
    TaprootWarningScreen(
        openNextScreen = {}
    )
}