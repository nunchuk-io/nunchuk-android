package com.nunchuk.android.app.onboard.advisor

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nunchuk.android.R
import com.nunchuk.android.compose.NcPrimaryDarkButton
import com.nunchuk.android.compose.NcTopAppBar
import com.nunchuk.android.compose.NunchukTheme
import com.skydoves.landscapist.ImageOptions
import com.skydoves.landscapist.glide.GlideImage

@Composable
fun OnboardAssistedWalletIntroScreen(
    modifier: Modifier = Modifier,
    viewModel: OnboardAssistedWalletIntroViewModel = hiltViewModel(),
    onOpenOnboardAdvisorInputScreen: () -> Unit = {},
    onOpenOnboardAdvisorIntroScreen: () -> Unit = {},
    onSkip: () -> Unit = {},
) {

    val state by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(state.openMainScreen) {
        if (state.openMainScreen) {
            onSkip()
            viewModel.handledOpenMainScreen()
        }
    }

    OnboardAssistedWalletIntroContent(
        modifier = modifier,
        onSkip = {
            viewModel.markOnboardDone()
        },
        uiState = state,
        onOpenOnboardAdvisorInputScreen = onOpenOnboardAdvisorInputScreen,
        onOpenOnboardAdvisorIntroScreen = onOpenOnboardAdvisorIntroScreen
    )
}

@Composable
fun OnboardAssistedWalletIntroContent(
    modifier: Modifier = Modifier,
    onSkip: () -> Unit = {},
    onOpenOnboardAdvisorInputScreen: () -> Unit = {},
    onOpenOnboardAdvisorIntroScreen: () -> Unit = {},
    uiState: OnboardAssistedWalletIntroUiState,
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
            Column(modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)) {
                NcPrimaryDarkButton(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = onOpenOnboardAdvisorIntroScreen,
                    content = { Text(text = stringResource(id = R.string.nc_have_an_advisor)) }
                )

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

            Spacer(modifier = Modifier.height(16.dp))

            Image(
                modifier = Modifier.fillMaxWidth(),
                painter = painterResource(id = R.drawable.bg_assisted_wallet_intro),
                contentDescription = ""
            )

            Text(
                modifier = Modifier.padding(vertical = 16.dp),
                text = stringResource(id = R.string.nc_assisted_wallet),
                style = NunchukTheme.typography.heading
            )

            Text(
                text = stringResource(id = R.string.nc_onboard_assisted_wallet_intro_desc),
                style = NunchukTheme.typography.body
            )

            Spacer(modifier = Modifier.height(16.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(color = colorResource(id = R.color.nc_grey_light)),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    modifier = Modifier
                        .padding(16.dp)
                        .fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    uiState.bannerPage?.items?.forEach {
                        FeatureItem(title = it.title, desc = it.desc, url = it.url)
                        Spacer(modifier = Modifier.height(24.dp))
                    }
                }
            }
        }
    }
}

@Composable
fun FeatureItem(title: String, desc: String, url: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
    ) {

        GlideImage(modifier = Modifier
            .padding(top = 12.dp)
            .size(24.dp),
            imageModel = {
                url
            }, imageOptions = ImageOptions(
                contentScale = ContentScale.Crop, alignment = Alignment.Center
            ), loading = {

            }, failure = {
            })

        Spacer(modifier = Modifier.width(24.dp))
        Column {
            Text(text = title, style = NunchukTheme.typography.title)
            Text(modifier = Modifier.padding(top = 8.dp), text = desc, style = NunchukTheme.typography.body)
        }
    }
}


@Preview
@Composable
fun OnboardAssistedWalletIntroScreenPreview() {
    NunchukTheme {
        OnboardAssistedWalletIntroScreen()
    }
}