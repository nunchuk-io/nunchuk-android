package com.nunchuk.android.main.membership.honey.distribution

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.nunchuk.android.compose.HighlightMessageType
import com.nunchuk.android.compose.NcHintMessage
import com.nunchuk.android.compose.NcPrimaryDarkButton
import com.nunchuk.android.compose.NunchukTheme
import com.nunchuk.android.compose.textPrimary
import com.nunchuk.android.core.util.ClickAbleText
import com.nunchuk.android.main.R

/**
 * Confirms the one irreversible downgrade of a "do both" key: dropping the encrypted backup
 * deletes it from the server, and the owner would have to create and upload a new one to get the
 * option back. Going the other way — dropping the seed phrase — destroys nothing, since the words
 * still physically exist, so it needs no confirmation.
 *
 * NDS has no danger-button variant, so this follows the house pattern: the ordinary primary CTA
 * plus a warning banner carrying the consequence.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RemoveEncryptedBackupSheet(
    onConfirm: () -> Unit = {},
    onDismiss: () -> Unit = {},
) {
    ModalBottomSheet(
        containerColor = MaterialTheme.colorScheme.background,
        sheetState = rememberModalBottomSheetState(),
        shape = RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp),
        onDismissRequest = onDismiss,
        tonalElevation = 0.dp,
    ) {
        Column(
            modifier = Modifier
                .navigationBarsPadding()
                .padding(horizontal = 16.dp)
                .padding(bottom = 16.dp),
        ) {
            Text(
                modifier = Modifier.fillMaxWidth(),
                text = stringResource(R.string.nc_remove_encrypted_backup_title),
                style = NunchukTheme.typography.title,
                textAlign = TextAlign.Center,
            )
            Text(
                modifier = Modifier.padding(top = 12.dp),
                text = stringResource(R.string.nc_remove_encrypted_backup_desc),
                style = NunchukTheme.typography.body,
            )
            NcHintMessage(
                modifier = Modifier.padding(top = 16.dp),
                messages = listOf(ClickAbleText(content = stringResource(R.string.nc_remove_encrypted_backup_warning))),
                type = HighlightMessageType.WARNING,
            )
            NcPrimaryDarkButton(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 24.dp),
                onClick = onConfirm,
            ) {
                Text(text = stringResource(R.string.nc_remove_backup))
            }
            TextButton(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
                onClick = onDismiss,
            ) {
                Text(
                    text = stringResource(R.string.nc_cancel),
                    style = NunchukTheme.typography.title,
                    color = MaterialTheme.colorScheme.textPrimary,
                )
            }
        }
    }
}
