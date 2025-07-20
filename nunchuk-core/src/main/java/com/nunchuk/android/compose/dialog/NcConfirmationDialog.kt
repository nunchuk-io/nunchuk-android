package com.nunchuk.android.compose.dialog

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment.Companion.CenterHorizontally
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.nunchuk.android.compose.NcPrimaryDarkButton
import com.nunchuk.android.compose.NcSpannedText
import com.nunchuk.android.compose.NunchukTheme
import com.nunchuk.android.compose.SpanIndicator
import com.nunchuk.android.compose.textPrimary
import com.nunchuk.android.core.R

@Composable
fun NcConfirmationDialog(
    message: String,
    title: String = stringResource(id = R.string.nc_confirmation),
    positiveButtonText: String = stringResource(id = R.string.nc_text_yes),
    negativeButtonText: String = stringResource(id = R.string.nc_text_no),
    onPositiveClick: () -> Unit,
    onDismiss: () -> Unit,
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
            NcSpannedText(
                modifier = Modifier
                    .padding(top = 12.dp)
                    .fillMaxWidth(),
                text = message,
                baseStyle = NunchukTheme.typography.body,
                textAlign = TextAlign.Center,
                styles = mapOf(SpanIndicator('B') to SpanStyle(fontWeight = FontWeight.Bold))
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 24.dp),
            ) {
                TextButton(
                    modifier = Modifier.weight(1f),
                    onClick = onDismiss
                ) {
                    Text(text = negativeButtonText, color = MaterialTheme.colorScheme.textPrimary)
                }
                NcPrimaryDarkButton(
                    modifier = Modifier.weight(1f),
                    onClick = onPositiveClick
                ) {
                    Text(text = positiveButtonText)
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun NcDialogPreview() {
    NunchukTheme {
        NcConfirmationDialog(
            message = "Are you sure you want to delete this recurring payment?",
            onPositiveClick = {},
            onDismiss = {}
        )
    }
}