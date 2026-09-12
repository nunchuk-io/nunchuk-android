package com.nunchuk.android.main.membership.backupseedphrase

import androidx.compose.foundation.Image
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
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.nunchuk.android.compose.NcPrimaryDarkButton
import com.nunchuk.android.compose.NcTopAppBar
import com.nunchuk.android.compose.NunchukTheme
import com.nunchuk.android.compose.provider.SignerModelProvider
import com.nunchuk.android.core.signer.SignerModel
import com.nunchuk.android.main.R
import kotlinx.serialization.Serializable

@Serializable
object InheritanceSeedPhraseVerified

fun NavGraphBuilder.inheritanceSeedPhraseVerifiedDestination(
    signer: SignerModel? = null,
    onContinue: () -> Unit = {},
) {
    composable<InheritanceSeedPhraseVerified> {
        InheritanceSeedPhraseVerifiedScreen(
            signer = signer,
            onContinue = onContinue,
        )
    }
}

@Composable
private fun InheritanceSeedPhraseVerifiedScreen(
    signer: SignerModel? = null,
    viewModel: BackUpSeedPhraseSharedViewModel = hiltViewModel(),
    onContinue: () -> Unit = {},
) {
    val remainTime by viewModel.remainTime.collectAsStateWithLifecycle()
    InheritanceSeedPhraseVerifiedContent(
        signer = signer,
        remainTime = remainTime,
        onContinueClicked = onContinue,
    )
}

/**
 * The restored device derived the same public key, so the seed phrase backup of the inheritance
 * key is proven.
 */
@Composable
private fun InheritanceSeedPhraseVerifiedContent(
    signer: SignerModel? = null,
    remainTime: Int = 0,
    onContinueClicked: () -> Unit = {},
) {
    NunchukTheme {
        Scaffold(
            modifier = Modifier.navigationBarsPadding(),
            topBar = {
                NcTopAppBar(
                    title = if (remainTime <= 0) "" else stringResource(
                        id = R.string.nc_estimate_remain_time,
                        remainTime
                    ),
                )
            },
            bottomBar = {
                NcPrimaryDarkButton(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    onClick = onContinueClicked,
                ) {
                    Text(text = stringResource(R.string.nc_text_continue))
                }
            },
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .padding(innerPadding)
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Image(
                    modifier = Modifier.padding(top = 72.dp),
                    painter = painterResource(id = R.drawable.nc_badge_verified),
                    contentDescription = null,
                )
                Text(
                    modifier = Modifier.padding(top = 24.dp),
                    text = stringResource(R.string.nc_seed_phrase_verified),
                    style = NunchukTheme.typography.heading,
                    textAlign = TextAlign.Center,
                )
                if (signer != null) {
                    VerificationSignerCard(
                        modifier = Modifier.padding(top = 24.dp),
                        signer = signer,
                        statusText = stringResource(R.string.nc_public_key_matches_inheritance_key),
                    )
                }
            }
        }
    }
}

@PreviewLightDark
@Composable
private fun InheritanceSeedPhraseVerifiedContentPreview(
    @PreviewParameter(SignerModelProvider::class) signer: SignerModel,
) {
    InheritanceSeedPhraseVerifiedContent(signer = signer)
}
