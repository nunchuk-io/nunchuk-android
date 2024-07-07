package com.nunchuk.android.signer.portal.wallet

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavOptions
import androidx.navigation.compose.composable
import com.nunchuk.android.compose.NcImageAppBar
import com.nunchuk.android.compose.NcPrimaryDarkButton
import com.nunchuk.android.compose.NcRadioOption
import com.nunchuk.android.compose.NcScaffold
import com.nunchuk.android.compose.NunchukTheme
import com.nunchuk.android.signer.R

const val selectWalletTypeRoute = "select_wallet_type"

fun NavGraphBuilder.selectWalletType(
    onSelectWalletType: (Boolean) -> Unit = { },
) {
    composable(selectWalletTypeRoute) {
        SelectWalletTypeScreen(
            onSelectWalletType = onSelectWalletType,
        )
    }
}

fun NavController.navigateToSelectWalletType(navOptions: NavOptions? = null) {
    navigate(selectWalletTypeRoute, navOptions)
}

@Composable
fun SelectWalletTypeScreen(
    modifier: Modifier = Modifier,
    onSelectWalletType: (Boolean) -> Unit = { },
) {
    var isSingleSig by rememberSaveable { mutableStateOf(true) }
    NcScaffold(
        modifier = modifier.navigationBarsPadding(),
        topBar = {
            NcImageAppBar(backgroundRes = R.drawable.nc_bg_multisig)
        },
        bottomBar = {
            NcPrimaryDarkButton(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                onClick = { onSelectWalletType(isSingleSig) }) {
                Text(text = stringResource(id = R.string.nc_text_continue))
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .padding(start = 16.dp, end = 16.dp, top = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Select wallet type",
                style = NunchukTheme.typography.heading,
            )

            Text(
                text = "Select the type of wallet you want to use Portal in:",
                style = NunchukTheme.typography.body,
            )

            NcRadioOption(
                modifier = Modifier.fillMaxWidth(),
                isSelected = isSingleSig,
                onClick = { isSingleSig = true }
            ) {
                Text(
                    text = "Single-sig",
                    style = NunchukTheme.typography.title,
                )
            }

            NcRadioOption(
                modifier = Modifier.fillMaxWidth(),
                isSelected = !isSingleSig,
                onClick = { isSingleSig = false }
            ) {
                Text(
                    text = "Multisig",
                    style = NunchukTheme.typography.title,
                )
            }
        }
    }
}

@Preview
@Composable
private fun SelectWalletTypeScreenPreview() {
    NunchukTheme {
        SelectWalletTypeScreen(
        )
    }
}