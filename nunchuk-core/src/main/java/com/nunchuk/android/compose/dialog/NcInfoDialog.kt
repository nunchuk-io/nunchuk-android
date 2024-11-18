package com.nunchuk.android.compose.dialog

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment.Companion.CenterHorizontally
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.nunchuk.android.compose.NcOutlineButton
import com.nunchuk.android.compose.NcPrimaryDarkButton
import com.nunchuk.android.compose.NunchukTheme
import com.nunchuk.android.compose.textPrimary
import com.nunchuk.android.core.R

@Composable
fun NcInfoDialog(
    message: String,
    title: String = stringResource(id = R.string.nc_text_info),
    positiveButtonText: String = stringResource(id = R.string.nc_text_got_it),
    negativeButtonText: String? = null,
    isOutlineButton: Boolean = false,
    onNegativeClick: () -> Unit = {},
    onDismiss: () -> Unit,
    onPositiveClick: () -> Unit = onDismiss,
) {
    Dialog(
        onDismissRequest = onDismiss,
    ) {
        Column(
            modifier = Modifier
                .background(
                    color = MaterialTheme.colorScheme.background,
                    shape = RoundedCornerShape(24.dp)
                )
                .fillMaxWidth()
                .padding(24.dp),
        ) {
            Text(
                modifier = Modifier.align(CenterHorizontally),
                text = title,
                style = NunchukTheme.typography.title
            )
            Text(
                modifier = Modifier.padding(top = 12.dp),
                text = message,
                style = NunchukTheme.typography.body,
                textAlign = TextAlign.Center,
            )
            NcPrimaryDarkButton(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 24.dp),
                onClick = onPositiveClick
            ) {
                Text(text = positiveButtonText)
            }

            negativeButtonText?.let {
                if (isOutlineButton) {
                    NcOutlineButton(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 16.dp),
                        onClick = onNegativeClick,
                    ) {
                        Text(text = it)
                    }
                } else {
                    TextButton(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .padding(top = 16.dp),
                        onClick = onNegativeClick
                    ) {
                        Text(text = it, color = MaterialTheme.colorScheme.textPrimary)
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun NcInfoDialogPreview() {
    NunchukTheme {
        NcInfoDialog(
            message = "Are you sure you want to delete this recurring payment?",
            negativeButtonText = "Cancel",
            onPositiveClick = {},
            onDismiss = {}
        )
    }
}