package com.nunchuk.android.app.onboard.unassisted

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.nunchuk.android.R
import com.nunchuk.android.compose.NcOutlineButton
import com.nunchuk.android.compose.NcPrimaryDarkButton
import com.nunchuk.android.compose.NcTopAppBar
import com.nunchuk.android.compose.NunchukTheme

@Composable
fun UnAssistedIntroScreen(
    modifier: Modifier = Modifier,
    viewModel: UnAssistedIntroViewModel = hiltViewModel(),
) {
    UnAssistedIntroContent(
        modifier = modifier,
    )
}

@Composable
private fun UnAssistedIntroContent(
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier.systemBarsPadding(),
        topBar = {
            NcTopAppBar(
                title = "",
                actions = {
                    Text(
                        modifier = Modifier
                            .align(Alignment.Top)
                            .padding(end = 16.dp),
                        text = stringResource(id = R.string.nc_text_skip),
                        style = NunchukTheme.typography.textLink
                    )
                },
            )
        },
        bottomBar = {
            Column(Modifier.padding(16.dp)) {
                Text(
                    text = stringResource(R.string.nc_quick_create_a_hot_wallet_desc),
                    style = NunchukTheme.typography.bodySmall,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .padding(horizontal = 32.dp)
                        .align(Alignment.CenterHorizontally)
                )

                NcPrimaryDarkButton(
                    modifier = Modifier
                        .padding(top = 16.dp)
                        .fillMaxWidth(),
                    onClick = { /*TODO*/ }) {
                    Text(text = stringResource(R.string.nc_create_a_hot_wallet_now))
                }

                NcOutlineButton(
                    modifier = Modifier
                        .padding(top = 16.dp)
                        .fillMaxWidth(),
                    onClick = { /*TODO*/ }) {
                    Text(text = stringResource(R.string.nc_i_ll_explore_on_my_own))
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
            Box(
                modifier = modifier
                    .align(Alignment.CenterHorizontally)
                    .padding(top = 24.dp)
                    .size(96.dp)
                    .background(
                        color = colorResource(id = R.color.nc_beeswax_tint),
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(id = R.drawable.ic_onboard_unassisted_wallet),
                    contentDescription = "Unassisted wallet",
                )
            }
            Text(
                text = stringResource(R.string.nc_unassisted_wallet),
                style = NunchukTheme.typography.heading,
                modifier = Modifier
                    .padding(top = 24.dp)
            )
            Text(
                text = stringResource(R.string.nc_unassisted_wallet_desc),
                style = NunchukTheme.typography.body,
                modifier = Modifier
                    .padding(top = 16.dp)
            )
        }
    }
}

@Preview
@Composable
fun UnAssistedIntroScreenPreview() {
    NunchukTheme {
        UnAssistedIntroScreen()
    }
}
