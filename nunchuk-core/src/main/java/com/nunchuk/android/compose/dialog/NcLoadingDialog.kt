package com.nunchuk.android.compose.dialog

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment.Companion.CenterHorizontally
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.nunchuk.android.compose.NunchukTheme
import com.nunchuk.android.core.R

@Composable
fun NcLoadingDialog(
    title: String = stringResource(id = R.string.nc_please_wait),
    onDismiss: () -> Unit = {},
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(dismissOnBackPress = false, dismissOnClickOutside = false)
    ) {
        Column(
            modifier = Modifier
                .background(
                    color = MaterialTheme.colorScheme.background,
                    shape = RoundedCornerShape(24.dp)
                )
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = CenterHorizontally,
        ) {
            CircularProgressIndicator(
                modifier = Modifier
                    .width(48.dp),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.background,
            )

            Text(
                modifier = Modifier.padding(top = 24.dp),
                text = title, style = NunchukTheme.typography.body,
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun NcLoadingDialogPreview() {
    NunchukTheme {
        NcLoadingDialog(
            onDismiss = {}
        )
    }
}