package com.nunchuk.android.main.components.tabs.services.inheritanceplanning.backupdownload

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
import com.nunchuk.android.compose.NCLabelWithIndex
import com.nunchuk.android.compose.NcHighlightText
import com.nunchuk.android.compose.NcPrimaryDarkButton
import com.nunchuk.android.compose.NcTopAppBar
import com.nunchuk.android.compose.NunchukTheme
import com.nunchuk.android.main.R

/**
 * "Learn more" for the Backup Password.
 *
 * The list covers every device that can produce an encrypted backup, not only the two the plan used
 * to allow, so a key on any other supported hardware still finds its answer here.
 *
 * When the key has both recovery methods this is step 1 of 2 — the seed phrase screen follows — so
 * the CTA continues instead of closing.
 */
@Composable
internal fun InheritanceBackUpDownloadContent(
    isFirstOfTwo: Boolean = false,
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
                    Text(
                        text = stringResource(
                            id = if (isFirstOfTwo) R.string.nc_text_continue else R.string.nc_text_got_it
                        )
                    )
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
                    text = stringResource(R.string.nc_the_backup_password),
                    style = NunchukTheme.typography.heading
                )
                NcHighlightText(
                    modifier = Modifier.padding(top = 16.dp, start = 16.dp, end = 16.dp),
                    text = stringResource(R.string.nc_the_backup_password_desc),
                    style = NunchukTheme.typography.body
                )
                NCLabelWithIndex(
                    modifier = Modifier.padding(top = 16.dp, start = 16.dp, end = 16.dp),
                    index = 1,
                    title = stringResource(R.string.nc_tapsigner),
                    label = stringResource(R.string.nc_backup_password_tapsigner_desc),
                )
                NCLabelWithIndex(
                    modifier = Modifier.padding(top = 16.dp, start = 16.dp, end = 16.dp),
                    index = 2,
                    title = stringResource(R.string.nc_coldcard_uppercase),
                    label = stringResource(R.string.nc_backup_password_coldcard_desc),
                )
                NCLabelWithIndex(
                    modifier = Modifier.padding(top = 16.dp, start = 16.dp, end = 16.dp),
                    index = 3,
                    title = stringResource(R.string.nc_keystone),
                    label = stringResource(R.string.nc_backup_password_keystone_desc),
                )
                NCLabelWithIndex(
                    modifier = Modifier.padding(top = 16.dp, start = 16.dp, end = 16.dp),
                    index = 4,
                    title = stringResource(R.string.nc_backup_password_other_devices),
                    label = stringResource(R.string.nc_backup_password_other_devices_desc),
                )
            }
        }
    }
}

@PreviewLightDark
@Composable
private fun InheritanceBackUpDownloadScreenPreview() {
    InheritanceBackUpDownloadContent()
}

@PreviewLightDark
@Composable
private fun InheritanceBackUpDownloadFirstOfTwoPreview() {
    InheritanceBackUpDownloadContent(isFirstOfTwo = true)
}
