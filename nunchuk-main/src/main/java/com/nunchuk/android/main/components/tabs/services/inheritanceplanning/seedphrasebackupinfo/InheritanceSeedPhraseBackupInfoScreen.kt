package com.nunchuk.android.main.components.tabs.services.inheritanceplanning.seedphrasebackupinfo

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
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import com.nunchuk.android.compose.HighlightMessageType
import com.nunchuk.android.compose.NCLabelWithIndex
import com.nunchuk.android.compose.NcHighlightText
import com.nunchuk.android.compose.NcHintMessage
import com.nunchuk.android.compose.NcPrimaryDarkButton
import com.nunchuk.android.compose.NcTopAppBar
import com.nunchuk.android.compose.NunchukTheme
import com.nunchuk.android.core.util.ClickAbleText
import com.nunchuk.android.main.R

/**
 * "Learn more" for the seed phrase backup.
 *
 * It exists because when the seed phrase is the key's only route, it is the only way to recover the
 * inheritance key and the app cannot reproduce it — worth stating plainly at the moment of handover.
 *
 * With [hasBothMethods] this is step 2 of the both-methods explanation: sharing advice covers the
 * passphrase case, and the closing note is about keeping the two methods together rather than about
 * keeping the seed and the Magic Phrase apart.
 */
@Composable
internal fun InheritanceSeedPhraseBackupInfoContent(
    hasBothMethods: Boolean = false,
    onContinueClicked: () -> Unit = {}
) {
    NunchukTheme {
        Scaffold(
            modifier = Modifier.navigationBarsPadding(),
            topBar = { NcTopAppBar(title = "") },
            bottomBar = {
                NcPrimaryDarkButton(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    onClick = onContinueClicked,
                ) {
                    Text(text = stringResource(id = R.string.nc_text_got_it))
                }
            }
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .padding(innerPadding)
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    modifier = Modifier.padding(start = 16.dp, end = 16.dp),
                    text = stringResource(R.string.nc_the_seed_phrase_backup),
                    style = NunchukTheme.typography.heading
                )
                NcHighlightText(
                    modifier = Modifier.padding(top = 16.dp, start = 16.dp, end = 16.dp),
                    text = stringResource(R.string.nc_the_seed_phrase_backup_desc),
                    style = NunchukTheme.typography.body
                )
                NCLabelWithIndex(
                    modifier = Modifier.padding(top = 16.dp, start = 16.dp, end = 16.dp),
                    index = 1,
                    title = stringResource(R.string.nc_seed_phrase_backup_where_title),
                    label = stringResource(R.string.nc_seed_phrase_backup_where_desc),
                )
                NCLabelWithIndex(
                    modifier = Modifier.padding(top = 16.dp, start = 16.dp, end = 16.dp),
                    index = 2,
                    title = stringResource(R.string.nc_seed_phrase_backup_share_title),
                    label = stringResource(
                        if (hasBothMethods) R.string.nc_seed_phrase_backup_share_desc_both
                        else R.string.nc_seed_phrase_backup_share_desc
                    ),
                )
                NcHintMessage(
                    modifier = Modifier.padding(top = 16.dp, start = 16.dp, end = 16.dp),
                    messages = listOf(
                        ClickAbleText(
                            content = stringResource(
                                if (hasBothMethods) R.string.nc_seed_phrase_backup_hint_both
                                else R.string.nc_seed_phrase_backup_hint
                            )
                        )
                    ),
                    type = HighlightMessageType.HINT,
                )
            }
        }
    }
}

@PreviewLightDark
@Composable
private fun InheritanceSeedPhraseBackupInfoScreenPreview() {
    InheritanceSeedPhraseBackupInfoContent()
}

@PreviewLightDark
@Composable
private fun InheritanceSeedPhraseBackupInfoBothMethodsPreview() {
    InheritanceSeedPhraseBackupInfoContent(hasBothMethods = true)
}
