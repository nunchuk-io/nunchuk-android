package com.nunchuk.android.signer.trezor

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.ClickableText
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import com.nunchuk.android.compose.NcImageAppBar
import com.nunchuk.android.compose.NcPrimaryDarkButton
import com.nunchuk.android.compose.NunchukTheme
import com.nunchuk.android.signer.R

const val trezorSuiteIntroRoute = "trezor_suite_intro_route"
private const val TREZOR_START_URL = "https://trezor.io/start"

fun NavGraphBuilder.trezorSuiteIntro(
    onBack: () -> Unit = {},
    onContinue: () -> Unit = {}
) {
    composable(trezorSuiteIntroRoute) {
        TrezorSuiteIntroScreen(
            onBack = onBack,
            onContinue = onContinue
        )
    }
}

fun NavHostController.navigateToTrezorSuiteIntro() {
    navigate(trezorSuiteIntroRoute)
}

@Composable
private fun TrezorSuiteIntroScreen(
    onBack: () -> Unit = {},
    onContinue: () -> Unit = {}
) {
    Scaffold(
        topBar = {
            NcImageAppBar(
                backgroundRes = R.drawable.bg_trezor_illustration,
                onClosedClicked = onBack
            )
        },
        bottomBar = {
            NcPrimaryDarkButton(
                modifier = Modifier
                    .padding(horizontal = 16.dp, vertical = 12.dp)
                    .navigationBarsPadding()
                    .fillMaxWidth(),
                onClick = onContinue
            ) {
                Text(text = stringResource(id = com.nunchuk.android.core.R.string.nc_text_continue))
            }
        }
    ) { innerPadding ->
        val context = LocalContext.current
        val suiteLinkText = stringResource(id = R.string.nc_trezor_suite_mobile_link)
        val suiteLinkAnnotatedText = buildAnnotatedString {
            pushStringAnnotation(tag = "URL", annotation = TREZOR_START_URL)
            addStyle(
                style = SpanStyle(textDecoration = TextDecoration.Underline),
                start = 0,
                end = suiteLinkText.length
            )
            append(suiteLinkText)
            pop()
        }

        Column(
            modifier = Modifier
                .padding(innerPadding)
                .padding(top = 24.dp, start = 16.dp, end = 16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = stringResource(id = R.string.nc_add_your_trezor),
                style = NunchukTheme.typography.heading
            )

            Text(
                text = stringResource(id = R.string.nc_ensure_to_following_trezor),
                style = NunchukTheme.typography.body
            )

            TrezorStep(index = 1) {
                Text(
                    text = stringResource(id = R.string.nc_open_trezor_suite),
                    style = NunchukTheme.typography.title
                )
                ClickableText(
                    modifier = Modifier.padding(top = 8.dp),
                    text = suiteLinkAnnotatedText,
                    style = NunchukTheme.typography.body,
                    onClick = { offset ->
                        suiteLinkAnnotatedText.getStringAnnotations(
                            tag = "URL",
                            start = offset,
                            end = offset
                        ).firstOrNull()?.let { annotation ->
                            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(annotation.item)))
                        }
                    }
                )
                Text(
                    modifier = Modifier.padding(top = 16.dp),
                    text = stringResource(id = R.string.nc_trezor_setup_desc),
                    style = NunchukTheme.typography.body
                )
            }

            TrezorStep(index = 2) {
                Text(
                    text = stringResource(id = R.string.nc_return_to_nunchuk),
                    style = NunchukTheme.typography.title
                )
                Text(
                    modifier = Modifier.padding(top = 8.dp),
                    text = stringResource(id = R.string.nc_return_to_nunchuk_desc),
                    style = NunchukTheme.typography.body
                )
            }
        }
    }
}

@Composable
private fun TrezorStep(
    index: Int,
    content: @Composable ColumnScope.() -> Unit
) {
    Row(verticalAlignment = Alignment.Top) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .border(
                    width = 2.dp,
                    color = MaterialTheme.colorScheme.primary,
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = index.toString(),
                style = NunchukTheme.typography.titleSmall.copy(fontWeight = FontWeight.W700)
            )
        }

        Column(
            modifier = Modifier
                .padding(start = 16.dp)
                .weight(1f)
        ) {
            content()
        }
    }
}

@PreviewLightDark
@Composable
private fun TrezorSuiteIntroScreenPreview() {
    NunchukTheme {
        TrezorSuiteIntroScreen()
    }
}
