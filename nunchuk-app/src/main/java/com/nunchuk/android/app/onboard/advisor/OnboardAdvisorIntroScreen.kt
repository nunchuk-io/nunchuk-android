package com.nunchuk.android.app.onboard.advisor

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nunchuk.android.R
import com.nunchuk.android.compose.NcPrimaryDarkButton
import com.nunchuk.android.compose.NcSnackbarVisuals
import com.nunchuk.android.compose.NcSpannedClickableText
import com.nunchuk.android.compose.NcToastType
import com.nunchuk.android.compose.NcTopAppBar
import com.nunchuk.android.compose.NunchukTheme
import com.nunchuk.android.compose.SpanIndicator

@Composable
fun OnboardAdvisorIntroScreen(
    modifier: Modifier = Modifier,
    viewModel: OnboardAdvisorIntroViewModel = hiltViewModel(),
    onOpenOnboardAdvisorInputScreen: () -> Unit = {},
    onSkip: () -> Unit = {},
    onSignIn: () -> Unit = {},
    onCreateAccount: () -> Unit = {},
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(state.openMainScreen) {
        if (state.openMainScreen) {
            onSkip()
            viewModel.handledOpenMainScreen()
        }
    }

    OnboardAdvisorIntroContent(
        modifier = modifier,
        uiState = state,
        onOpenOnboardAdvisorInputScreen = onOpenOnboardAdvisorInputScreen,
        onSkip = {
            viewModel.markOnboardDone()
        },
        onSignIn = onSignIn,
        onCreateAccount = onCreateAccount
    )
}

@Composable
fun OnboardAdvisorIntroContent(
    modifier: Modifier = Modifier,
    uiState: OnboardAdvisorIntroUiState = OnboardAdvisorIntroUiState(),
    onSkip: () -> Unit = {},
    onSignIn: () -> Unit = {},
    onCreateAccount: () -> Unit = {},
    onOpenOnboardAdvisorInputScreen: () -> Unit = {},
) {
    Scaffold(
        modifier = modifier.systemBarsPadding(),
        topBar = {
            NcTopAppBar(
                title = "",
                textStyle = NunchukTheme.typography.titleLarge,
                actions = {
                    Text(
                        modifier = Modifier
                            .padding(16.dp)
                            .clickable(onClick = onSkip),
                        text = stringResource(id = R.string.nc_text_skip),
                        style = NunchukTheme.typography.textLink
                    )
                },
            )
        },
        bottomBar = {
            if (!uiState.isLoggedIn) {
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
            Spacer(modifier = Modifier.height(16.dp))

            Text(text = stringResource(id = R.string.nc_have_an_advisor), style = NunchukTheme.typography.heading)

            Spacer(modifier = Modifier.height(16.dp))

            if (!uiState.isLoggedIn) {
                Text(
                    text = stringResource(id = R.string.nc_have_advisor_desc),
                    style = NunchukTheme.typography.body
                )
            } else {
                Text(
                    text = stringResource(id = R.string.nc_have_advisor_desc_logged_in),
                    style = NunchukTheme.typography.body
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (!uiState.isLoggedIn) {
                NcPrimaryDarkButton(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = onCreateAccount,
                    content = { Text(text = stringResource(id = R.string.nc_text_create_an_account)) }
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        onOpenOnboardAdvisorInputScreen()
                    },
                textAlign = TextAlign.Center,
                text = stringResource(id = R.string.nc_dont_have_an_advisor),
                style = NunchukTheme.typography.title
            )
        }
    }
}

@Composable
fun OptionCard(
    containerColor: Color,
    title: String,
    description: String,
    painter: Painter,
    onClick: () -> Unit = {},
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = containerColor
        ),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 24.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = title, style = NunchukTheme.typography.title)
                Text(
                    modifier = Modifier.padding(top = 8.dp),
                    text = description,
                    style = NunchukTheme.typography.bodySmall
                )
            }
            Image(
                modifier = Modifier.align(Alignment.Bottom),
                painter = painter,
                contentDescription = "Image",
            )
        }
    }
}

@Preview
@Composable
fun OnboardAdvisorScreenPreview() {
    NunchukTheme {
        OnboardAdvisorIntroScreen()
    }
}
