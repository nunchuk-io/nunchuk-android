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
import com.nunchuk.android.main.membership.model.ClaimOptionStatus
import com.nunchuk.android.model.inheritance.ClaimOption
import com.nunchuk.android.core.R as CoreR

/**
 * Where an inheritance key lands once its sharing method is recorded: one card per method the
 * owner chose, plus the way back to that choice.
 *
 * A key that asked for both artifacts gets two cards, because they are verified independently — a
 * seed phrase backup fails from a transcription error and an encrypted backup from a mis-recorded
 * Backup Password, so verifying one proves nothing about the other. Continue is enabled once every
 * card is dealt with; skipping counts, since at claim time the Beneficiary only needs one route to
 * work.
 */
@Composable
fun VerifyBackupsContent(
    remainTime: Int = 0,
    /** The sharing methods the owner chose and how far each has got — one card each. */
    statuses: List<ClaimOptionStatus> = emptyList(),
    /** Every card dealt with — verified or deliberately skipped. Until then the owner stays here. */
    isContinueEnabled: Boolean = false,
    onVerifyEncryptedBackup: () -> Unit = {},
    onVerifySeedPhrase: () -> Unit = {},
    onChangeSharingMethod: () -> Unit = {},
    onContinueClicked: () -> Unit = {},
    onBackPressed: () -> Unit = {},
) {
    val isBothMethods = statuses.size > 1
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
                    text = stringResource(
                        if (isBothMethods) R.string.nc_verify_your_backups
                        else R.string.nc_verify_your_backup
                    ),
                    style = NunchukTheme.typography.heading,
                )
                Text(
                    modifier = Modifier.padding(top = 8.dp),
                    text = stringResource(
                        when {
                            isBothMethods -> R.string.nc_verify_your_backups_desc
                            statuses.firstOrNull()?.option == ClaimOption.ENCRYPTED_BACKUP ->
                                R.string.nc_verify_your_backup_encrypted_desc

                            else -> R.string.nc_verify_your_backup_seed_phrase_desc
                        }
                    ),
                    style = NunchukTheme.typography.body,
                )

                statuses.forEach { status ->
                    when (status.option) {
                        ClaimOption.ENCRYPTED_BACKUP -> {
                            val isUploaded = status.state != ClaimOptionState.NOT_UPLOADED
                            BackupChecklistItem(
                                iconRes = R.drawable.ic_cloud_upload,
                                titleRes = R.string.nc_encrypted_backup,
                                // Nothing to verify until the file is on the server, so the card
                                // first offers to make the backup.
                                descRes = if (isUploaded) {
                                    R.string.nc_encrypted_backup_verify_desc
                                } else {
                                    R.string.nc_encrypted_backup_upload_desc
                                },
                                actionRes = if (isUploaded) {
                                    CoreR.string.nc_verify
                                } else {
                                    CoreR.string.nc_upload_backup
                                },
                                state = status.state,
                                onActionClicked = onVerifyEncryptedBackup,
                            )
                        }

                        ClaimOption.SEED_PHRASE -> BackupChecklistItem(
                            iconRes = CoreR.drawable.ic_key,
                            titleRes = R.string.nc_seed_phrase_backup,
                            descRes = R.string.nc_seed_phrase_backup_verify_desc,
                            actionRes = CoreR.string.nc_verify,
                            state = status.state,
                            onActionClicked = onVerifySeedPhrase,
                        )
                    }
                }

                NcHintMessage(
                    modifier = Modifier.padding(top = 16.dp),
                    messages = listOf(
                        ClickAbleText(
                            content = stringResource(
                                if (isBothMethods) R.string.nc_verify_your_backups_hint
                                else R.string.nc_verify_your_backup_hint
                            )
                        )
                    ),
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

private fun bothMethods(
    encryptedBackupState: ClaimOptionState,
    seedPhraseState: ClaimOptionState,
) = listOf(
    ClaimOptionStatus(ClaimOption.ENCRYPTED_BACKUP, encryptedBackupState),
    ClaimOptionStatus(ClaimOption.SEED_PHRASE, seedPhraseState),
)

@PreviewLightDark
@Composable
private fun VerifyBackupsContentNotUploadedPreview() {
    VerifyBackupsContent(
        statuses = bothMethods(ClaimOptionState.NOT_UPLOADED, ClaimOptionState.PENDING)
    )
}

@PreviewLightDark
@Composable
private fun VerifyBackupsContentPartialPreview() {
    VerifyBackupsContent(
        statuses = bothMethods(ClaimOptionState.VERIFIED, ClaimOptionState.PENDING)
    )
}

@PreviewLightDark
@Composable
private fun VerifyBackupsContentSkippedPreview() {
    VerifyBackupsContent(
        statuses = bothMethods(ClaimOptionState.VERIFIED, ClaimOptionState.SKIPPED),
        isContinueEnabled = true,
    )
}

@PreviewLightDark
@Composable
private fun VerifyBackupsContentBothVerifiedPreview() {
    VerifyBackupsContent(
        statuses = bothMethods(ClaimOptionState.VERIFIED, ClaimOptionState.VERIFIED),
        isContinueEnabled = true,
    )
}

@PreviewLightDark
@Composable
private fun VerifyBackupsContentSeedPhraseOnlyPreview() {
    VerifyBackupsContent(
        statuses = listOf(ClaimOptionStatus(ClaimOption.SEED_PHRASE, ClaimOptionState.PENDING))
    )
}

@PreviewLightDark
@Composable
private fun VerifyBackupsContentEncryptedOnlyPreview() {
    VerifyBackupsContent(
        statuses = listOf(
            ClaimOptionStatus(ClaimOption.ENCRYPTED_BACKUP, ClaimOptionState.NOT_UPLOADED)
        )
    )
}
