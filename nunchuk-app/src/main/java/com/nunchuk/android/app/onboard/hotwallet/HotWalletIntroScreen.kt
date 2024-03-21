package com.nunchuk.android.app.onboard.hotwallet

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nunchuk.android.R
import com.nunchuk.android.app.onboard.unassisted.UnAssistedIntroState
import com.nunchuk.android.app.onboard.unassisted.UnAssistedIntroViewModel
import com.nunchuk.android.compose.NcImageAppBar
import com.nunchuk.android.compose.NcPrimaryDarkButton
import com.nunchuk.android.compose.NunchukTheme
import com.nunchuk.android.compose.dialog.NcLoadingDialog
import com.nunchuk.android.model.MembershipPlan

@Composable
fun HotWalletIntroScreen(
    modifier: Modifier = Modifier,
    returnToMainScreen: () -> Unit = {},
    openServiceTab: () -> Unit = {},
    viewModel: UnAssistedIntroViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    if (state.isLoading) {
        NcLoadingDialog()
    }
    LaunchedEffect(state.openMainScreen) {
        if (state.openMainScreen) {
            returnToMainScreen()
            viewModel.handledOpenMainScreen()
        }
    }
    HotWalletIntroContent(
        state = state,
        modifier = modifier,
        onCreateHotWallet = {
            viewModel.createHotWallet()
        },
        openServiceTab = openServiceTab
    )
}

@Composable
private fun HotWalletIntroContent(
    modifier: Modifier = Modifier,
    state: UnAssistedIntroState = UnAssistedIntroState(),
    onCreateHotWallet: () -> Unit = {},
    openServiceTab: () -> Unit = {},
) {
    Scaffold(
        modifier = modifier.navigationBarsPadding(),
        topBar = {
            NcImageAppBar(
                backgroundRes = R.drawable.bg_hot_wallet
            )
        },
        bottomBar = {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                NcPrimaryDarkButton(
                    modifier = Modifier
                        .padding(top = 16.dp)
                        .fillMaxWidth(),
                    onClick = onCreateHotWallet
                ) {
                    Text(text = stringResource(R.string.nc_text_continue))
                }

                if (state.plan == MembershipPlan.NONE) {
                    Text(
                        modifier = Modifier
                            .padding(16.dp)
                            .clickable(onClick = openServiceTab)
                            .align(Alignment.CenterHorizontally),
                        text = stringResource(R.string.nc_learn_more_about_assisted_wallet),
                        style = NunchukTheme.typography.title,
                    )
                }
            }
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            Text(
                text = stringResource(R.string.nc_hot_wallet),
                style = NunchukTheme.typography.heading,
                modifier = Modifier
                    .padding(top = 24.dp)
            )
            Text(
                text = stringResource(R.string.nc_hot_wallet_desc),
                style = NunchukTheme.typography.body,
                modifier = Modifier
                    .padding(top = 16.dp)
            )
        }
    }
}

@Preview
@Composable
fun HotWalletIntroScreenPreview() {
    NunchukTheme {
        HotWalletIntroContent()
    }
}
