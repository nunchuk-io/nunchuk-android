package com.nunchuk.android.app.onboard.intro

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nunchuk.android.R
import com.nunchuk.android.compose.NcSpannedClickableText
import com.nunchuk.android.compose.NunchukTheme
import com.nunchuk.android.compose.OptionCard
import com.nunchuk.android.compose.SpanIndicator

@Composable
fun OnboardIntroScreen(
    modifier: Modifier = Modifier,
    onOpenUnassistedIntro: () -> Unit = {},
    onOpenAssistedIntro: () -> Unit = {},
    openMainScreen: () -> Unit = {},
    onSignIn: () -> Unit = {},
    viewModel: OnboardIntroViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    LaunchedEffect(state.openMainScreen) {
        if (state.openMainScreen) {
            openMainScreen()
            viewModel.handledOpenMainScreen()
        }
    }
    OnboardIntroContent(
        state = state,
        modifier = modifier,
        onOpenUnassistedIntro = onOpenUnassistedIntro,
        onOpenAssistedIntro = onOpenAssistedIntro,
        onSkip = {
            viewModel.markOnBoardDone()
            openMainScreen()
        },
        onSignIn = onSignIn
    )
}

@Composable
fun OnboardIntroContent(
    modifier: Modifier = Modifier,
    state: OnboardIntroState = OnboardIntroState(),
    onSkip: () -> Unit = {},
    onSignIn: () -> Unit = {},
    onOpenUnassistedIntro: () -> Unit = {},
    onOpenAssistedIntro: () -> Unit = {}
) {
    Scaffold(
        modifier = modifier.systemBarsPadding(),
        topBar = {
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.TopEnd
            ) {
                Text(
                    modifier = Modifier
                        .padding(16.dp)
                        .clickable(onClick = onSkip),
                    text = stringResource(id = R.string.nc_text_skip),
                    style = NunchukTheme.typography.textLink
                )
            }
        },
        bottomBar = {
            if (!state.isLoggedIn) {
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    NcSpannedClickableText(
                        modifier = Modifier
                            .padding(16.dp),
                        text = stringResource(R.string.nc_already_have_an_account_sign_in),
                        baseStyle = NunchukTheme.typography.body,
                        styles = mapOf(
                            SpanIndicator('A') to SpanStyle(
                                fontWeight = FontWeight.Bold,
                            )
                        ),
                        onClick = { onSignIn() }
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
            Image(
                painter = painterResource(id = R.drawable.ic_logo_light),
                contentDescription = "Logo image",
                modifier = Modifier
                    .padding(top = 24.dp)
                    .size(84.dp)
                    .align(Alignment.CenterHorizontally)
            )
            Text(
                text = stringResource(R.string.nc_how_will_you_use_nunchuk),
                style = NunchukTheme.typography.titleLarge,
                modifier = Modifier
                    .padding(vertical = 24.dp)
                    .align(Alignment.CenterHorizontally)
            )
            OptionCard(
                containerColor = colorResource(id = R.color.nc_denim_tint_color),
                title = stringResource(R.string.nc_assisted_wallet),
                description = stringResource(R.string.nc_assisted_wallet_option_desc),
                painter = painterResource(id = R.drawable.ic_onboard_assisted_wallet),
                descTextStyle = NunchukTheme.typography.body,
                onClick = onOpenAssistedIntro
            )
            Spacer(modifier = Modifier.height(16.dp))
            OptionCard(
                containerColor = colorResource(id = R.color.nc_beeswax_tint),
                title = stringResource(R.string.nc_unassisted_wallet),
                description = stringResource(R.string.nc_unassisted_wallet_option_desc),
                painter = painterResource(id = R.drawable.ic_onboard_unassisted_wallet),
                descTextStyle = NunchukTheme.typography.body,
                onClick = onOpenUnassistedIntro
            )
        }
    }
}

@Preview
@Composable
fun OnboardIntroScreenPreview() {
    NunchukTheme {
        OnboardIntroScreen()
    }
}
