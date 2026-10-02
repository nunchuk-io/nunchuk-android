package com.nunchuk.android.main.membership.signer

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import com.nunchuk.android.core.util.ClickAbleText
import com.nunchuk.android.compose.HighlightMessageType
import com.nunchuk.android.compose.NcHintMessage
import com.nunchuk.android.compose.NcImageAppBar
import com.nunchuk.android.compose.NcPrimaryDarkButton
import com.nunchuk.android.compose.NunchukTheme
import com.nunchuk.android.main.R
import com.nunchuk.android.signer.KeyType
import kotlinx.serialization.Serializable

/**
 * What the inheritance key is and what the owner needs off the device before adding it.
 *
 * It runs *after* the key-type picker, and only for a key type that has a seed phrase — a TAPSIGNER
 * owner has none to view or back up — and is followed by the passphrase notice. [keyType] is the
 * picked [KeyType] name, carried through so the selection resumes once both guides are done.
 */
@Serializable
data class InheritanceKeyIntroDestination(val keyType: String)

/** The passphrase warning that follows [InheritanceKeyIntroDestination], on- and off-chain. */
@Serializable
data class InheritancePassphraseNoticeDestination(val keyType: String)

fun NavGraphBuilder.inheritanceKeyIntroDestination(
    remainTime: Int = 0,
    onMoreClicked: () -> Unit = {},
    onContinueClicked: (KeyType) -> Unit = {},
) {
    composable<InheritanceKeyIntroDestination> { backStackEntry ->
        val keyType = KeyType.valueOf(backStackEntry.toRoute<InheritanceKeyIntroDestination>().keyType)
        InheritanceKeyIntroContent(
            remainTime = remainTime,
            onMoreClicked = onMoreClicked,
            onContinueClicked = { onContinueClicked(keyType) },
        )
    }
}

fun NavGraphBuilder.inheritancePassphraseNoticeDestination(
    remainTime: Int = 0,
    onMoreClicked: () -> Unit = {},
    onContinueClicked: (KeyType) -> Unit = {},
) {
    composable<InheritancePassphraseNoticeDestination> { backStackEntry ->
        val keyType = KeyType.valueOf(backStackEntry.toRoute<InheritancePassphraseNoticeDestination>().keyType)
        ImportantNoticePassphraseContent(
            remainTime = remainTime,
            onMoreClicked = onMoreClicked,
            onContinueClicked = { onContinueClicked(keyType) },
        )
    }
}

@Composable
private fun InheritanceKeyIntroContent(
    remainTime: Int = 0,
    onMoreClicked: () -> Unit = {},
    onContinueClicked: () -> Unit = {},
) {
    NunchukTheme {
        Scaffold(
            modifier = Modifier.navigationBarsPadding(),
            topBar = {
            NcImageAppBar(
                backgroundRes = R.drawable.bg_inheritance_key_illustration,
                title = if (remainTime <= 0) "" else stringResource(
                    id = R.string.nc_estimate_remain_time,
                    remainTime
                ),
                actions = {
                    IconButton(onClick = onMoreClicked) {
                        Icon(
                            painter = painterResource(id = com.nunchuk.android.signer.R.drawable.ic_more),
                            contentDescription = "More icon"
                        )
                    }
                }
            )
        },
            bottomBar = {
                Column {
                    NcHintMessage(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        messages = listOf(
                            ClickAbleText(content = stringResource(R.string.nc_inheritance_key_intro_hint))
                        ),
                        type = HighlightMessageType.HINT,
                    )
                    NcPrimaryDarkButton(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        onClick = onContinueClicked,
                    ) {
                        Text(text = stringResource(id = com.nunchuk.android.signer.R.string.nc_text_continue))
                    }
                }
            }) { innerPadding ->
            Column(
                modifier = Modifier
                    .padding(innerPadding)
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    modifier = Modifier.padding(top = 24.dp, start = 16.dp, end = 16.dp),
                    text = stringResource(R.string.nc_your_inheritance_key),
                    style = NunchukTheme.typography.heading
                )
                Text(
                    modifier = Modifier.padding(16.dp),
                    text = buildAnnotatedString {
                        append(stringResource(R.string.nc_inheritance_key_intro_purpose))
                        append("\n\n")
                        withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                            append(stringResource(R.string.nc_inheritance_key_intro_seed_phrase_bold))
                        }
                        append(" ")
                        append(stringResource(R.string.nc_inheritance_key_intro_seed_phrase))
                        append("\n\n")
                        append(stringResource(R.string.nc_inheritance_key_intro_share_backup))
                    },
                    style = NunchukTheme.typography.body
                )
            }
        }
    }
}

@PreviewLightDark
@Composable
private fun InheritanceKeyIntroContentPreview() {
    InheritanceKeyIntroContent()
}
