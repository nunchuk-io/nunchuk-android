package com.nunchuk.android.app.onboard.intro

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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.nunchuk.android.R
import com.nunchuk.android.compose.NcSpannedClickableText
import com.nunchuk.android.compose.NunchukTheme
import com.nunchuk.android.compose.SpanIndicator

@Composable
fun OnboardIntroScreen(
    modifier: Modifier = Modifier,
    onOpenUnassistedIntro: () -> Unit = {},
    viewModel: OnboardIntroViewModel = hiltViewModel(),
) {
    OnboardIntroContent(
        modifier = modifier,
        onOpenUnassistedIntro = onOpenUnassistedIntro,
    )
}

@Composable
fun OnboardIntroContent(
    modifier: Modifier = Modifier,
    onOpenUnassistedIntro: () -> Unit = {},
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
                        .padding(16.dp),
                    text = stringResource(id = R.string.nc_text_skip),
                    style = NunchukTheme.typography.textLink
                )
            }
        },
        bottomBar = {
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                NcSpannedClickableText(
                    modifier = Modifier
                        .padding(16.dp),
                    text = "Already have an account? [A]Sign in[/A]",
                    baseStyle = NunchukTheme.typography.body,
                    styles = mapOf(
                        SpanIndicator('A') to SpanStyle(
                            fontWeight = FontWeight.Bold,
                        )
                    ),
                    onClick = {

                    }
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
            Image(
                painter = painterResource(id = R.drawable.ic_logo_light),
                contentDescription = "Logo image",
                modifier = Modifier
                    .padding(top = 24.dp)
                    .size(84.dp)
                    .align(Alignment.CenterHorizontally)
            )
            Text(
                text = "How will you use Nunchuk?",
                style = NunchukTheme.typography.titleLarge,
                modifier = Modifier
                    .padding(vertical = 24.dp)
                    .align(Alignment.CenterHorizontally)
            )
            OptionCard(
                containerColor = colorResource(id = R.color.nc_denim_tint_color),
                title = stringResource(R.string.nc_assisted_wallet),
                description = stringResource(R.string.nc_assisted_wallet_option_desc),
                painter = painterResource(id = R.drawable.ic_onboard_assisted_wallet)
            ) {

            }
            Spacer(modifier = Modifier.height(16.dp))
            OptionCard(
                containerColor = colorResource(id = R.color.nc_beeswax_tint),
                title = stringResource(R.string.nc_unassisted_wallet),
                description = stringResource(R.string.nc_unassisted_wallet_option_desc),
                painter = painterResource(id = R.drawable.ic_onboard_unassisted_wallet),
                onClick = onOpenUnassistedIntro
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
fun OnboardIntroScreenPreview() {
    NunchukTheme {
        OnboardIntroScreen()
    }
}
