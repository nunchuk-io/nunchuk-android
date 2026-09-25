package com.nunchuk.android.main.membership.honey.distribution

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import com.nunchuk.android.compose.HighlightMessageType
import com.nunchuk.android.compose.NcCircleImage
import com.nunchuk.android.compose.NcClickableText
import com.nunchuk.android.compose.NcHintMessage
import com.nunchuk.android.compose.NcIcon
import com.nunchuk.android.compose.NcOutlineButton
import com.nunchuk.android.compose.NcPrimaryDarkButton
import com.nunchuk.android.compose.NcTopAppBar
import com.nunchuk.android.compose.NunchukTheme
import com.nunchuk.android.compose.backgroundPrimary
import com.nunchuk.android.compose.fillBeeswax
import com.nunchuk.android.compose.fillSlimeT2
import com.nunchuk.android.compose.strokePrimary
import com.nunchuk.android.compose.textSecondary
import com.nunchuk.android.core.util.ClickAbleText
import com.nunchuk.android.main.R
import com.nunchuk.android.main.membership.model.ClaimOptionState
import com.nunchuk.android.core.R as CoreR

/**
 * "Do both": the two artifacts are verified separately, so the verify step is a checklist rather
 * than the single verify/skip question the one-option branches ask.
 *
 * A seed phrase backup fails from a transcription error and an encrypted backup from a
 * mis-recorded Backup Password — verifying one proves nothing about the other. Continue stays
 * enabled throughout: at claim time the Beneficiary only needs one route to work, so neither
 * check is mandatory.
 */
@Composable
fun VerifyBackupsContent(
    remainTime: Int = 0,
    encryptedBackupState: ClaimOptionState = ClaimOptionState.PENDING,
    seedPhraseState: ClaimOptionState = ClaimOptionState.PENDING,
    /** Both halves dealt with — verified or deliberately skipped. Until then the owner stays here. */
    isContinueEnabled: Boolean = false,
    onVerifyEncryptedBackup: () -> Unit = {},
    onVerifySeedPhrase: () -> Unit = {},
    onChangeSharingMethod: () -> Unit = {},
    onContinueClicked: () -> Unit = {},
    onBackPressed: () -> Unit = {},
) {
    NunchukTheme {
        Scaffold(
            modifier = Modifier.navigationBarsPadding(),
            topBar = {
                NcTopAppBar(
                    title = if (remainTime <= 0) "" else stringResource(
                        R.string.nc_estimate_remain_time, remainTime
                    ),
                    onBackPress = onBackPressed,
                )
            },
            bottomBar = {
                NcPrimaryDarkButton(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    enabled = isContinueEnabled,
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
            ) {
                Text(
                    text = stringResource(R.string.nc_verify_your_backups),
                    style = NunchukTheme.typography.heading,
                )
                Text(
                    modifier = Modifier.padding(top = 8.dp),
                    text = stringResource(R.string.nc_verify_your_backups_desc),
                    style = NunchukTheme.typography.body,
                )

                val isEncryptedBackupUploaded = encryptedBackupState != ClaimOptionState.NOT_UPLOADED
                BackupChecklistItem(
                    iconRes = R.drawable.ic_cloud_upload,
                    titleRes = R.string.nc_encrypted_backup,
                    // Nothing to verify until the file is on the server, so the card first offers
                    // to make the backup.
                    descRes = if (isEncryptedBackupUploaded) {
                        R.string.nc_encrypted_backup_verify_desc
                    } else {
                        R.string.nc_encrypted_backup_upload_desc
                    },
                    actionRes = if (isEncryptedBackupUploaded) {
                        CoreR.string.nc_verify
                    } else {
                        CoreR.string.nc_upload_backup
                    },
                    state = encryptedBackupState,
                    onActionClicked = onVerifyEncryptedBackup,
                )
                BackupChecklistItem(
                    iconRes = CoreR.drawable.ic_key,
                    titleRes = R.string.nc_seed_phrase_backup,
                    descRes = R.string.nc_seed_phrase_backup_verify_desc,
                    actionRes = CoreR.string.nc_verify,
                    state = seedPhraseState,
                    onActionClicked = onVerifySeedPhrase,
                )

                NcHintMessage(
                    modifier = Modifier.padding(top = 16.dp),
                    messages = listOf(ClickAbleText(content = stringResource(R.string.nc_verify_your_backups_hint))),
                    type = HighlightMessageType.HINT,
                    textStyle = NunchukTheme.typography.bodySmall,
                    iconSize = 16.dp,
                )

                NcClickableText(
                    modifier = Modifier.padding(top = 16.dp, bottom = 16.dp),
                    messages = listOf(
                        ClickAbleText(
                            content = stringResource(R.string.nc_change_sharing_method),
                            onClick = onChangeSharingMethod,
                        )
                    ),
                    style = NunchukTheme.typography.title,
                    linkDecoration = null,
                )
            }
        }
    }
}

/**
 * One half of the checklist. The card's fill says how far it has got — plain while something is
 * still owed, green once verified, amber when the owner passed on the check — and only a verified
 * half loses its action button: a skipped one can still be verified later.
 */
@Composable
private fun BackupChecklistItem(
    iconRes: Int,
    titleRes: Int,
    descRes: Int,
    actionRes: Int,
    state: ClaimOptionState,
    onActionClicked: () -> Unit,
) {
    val shape = RoundedCornerShape(12.dp)
    val fill = when (state) {
        ClaimOptionState.VERIFIED -> MaterialTheme.colorScheme.fillSlimeT2
        ClaimOptionState.SKIPPED -> MaterialTheme.colorScheme.fillBeeswax
        ClaimOptionState.NOT_UPLOADED, ClaimOptionState.PENDING -> Color.Transparent
    }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 16.dp)
            .background(color = fill, shape = shape)
            .then(
                if (fill == Color.Transparent) {
                    Modifier.border(
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.strokePrimary),
                        shape = shape,
                    )
                } else {
                    Modifier
                }
            )
            .padding(12.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            NcCircleImage(
                size = 40.dp,
                iconSize = 24.dp,
                color = MaterialTheme.colorScheme.backgroundPrimary,
                resId = iconRes,
            )
            Text(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 12.dp),
                text = stringResource(titleRes),
                style = NunchukTheme.typography.title,
            )
            if (state == ClaimOptionState.VERIFIED) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    NcIcon(
                        painter = painterResource(id = R.drawable.nc_circle_checked),
                        contentDescription = "Verified icon",
                    )
                    Text(
                        modifier = Modifier.padding(start = 4.dp),
                        text = stringResource(R.string.nc_verified),
                        style = NunchukTheme.typography.title,
                    )
                }
            } else {
                NcOutlineButton(
                    modifier = Modifier.height(36.dp),
                    onClick = onActionClicked,
                ) {
                    Text(text = stringResource(actionRes))
                }
            }
        }
        Text(
            modifier = Modifier.padding(top = 8.dp),
            text = stringResource(descRes),
            style = NunchukTheme.typography.bodySmall.copy(
                color = MaterialTheme.colorScheme.textSecondary
            ),
        )
        if (state == ClaimOptionState.SKIPPED) {
            Text(
                modifier = Modifier.padding(top = 8.dp),
                text = stringResource(R.string.nc_verification_skipped),
                style = NunchukTheme.typography.bodySmall.copy(
                    color = MaterialTheme.colorScheme.textSecondary
                ),
            )
        }
    }
}

@PreviewLightDark
@Composable
private fun VerifyBackupsContentNotUploadedPreview() {
    VerifyBackupsContent(encryptedBackupState = ClaimOptionState.NOT_UPLOADED)
}

@PreviewLightDark
@Composable
private fun VerifyBackupsContentPartialPreview() {
    VerifyBackupsContent(encryptedBackupState = ClaimOptionState.VERIFIED)
}

@PreviewLightDark
@Composable
private fun VerifyBackupsContentSkippedPreview() {
    VerifyBackupsContent(
        encryptedBackupState = ClaimOptionState.VERIFIED,
        seedPhraseState = ClaimOptionState.SKIPPED,
        isContinueEnabled = true,
    )
}

@PreviewLightDark
@Composable
private fun VerifyBackupsContentBothVerifiedPreview() {
    VerifyBackupsContent(
        encryptedBackupState = ClaimOptionState.VERIFIED,
        seedPhraseState = ClaimOptionState.VERIFIED,
        isContinueEnabled = true,
    )
}
