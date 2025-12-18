package com.nunchuk.android.compose.dialog

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment.Companion.CenterHorizontally
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.nunchuk.android.compose.NunchukTheme
import com.nunchuk.android.compose.textPrimary
import com.nunchuk.android.core.R
import kotlinx.coroutines.delay

@Composable
fun NcLoadingDialog(
    title: String = stringResource(id = R.string.nc_please_wait),
    customMessage: String? = null,
    onDismiss: () -> Unit = {},
) {
    var isShow by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        delay(100L)
        isShow = true
    }
    if (isShow) {
        Dialog(
            onDismissRequest = onDismiss,
            properties = DialogProperties(dismissOnBackPress = true, dismissOnClickOutside = false)
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
                    color = MaterialTheme.colorScheme.textPrimary,
                    trackColor = MaterialTheme.colorScheme.background,
                )

                Text(
                    modifier = Modifier.padding(top = 24.dp),
                    text = title, style = NunchukTheme.typography.body,
                )

                customMessage?.let {
                    Text(
                        modifier = Modifier.padding(top = 16.dp),
                        text = customMessage,
                        style = NunchukTheme.typography.body,
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun NcLoadingDialogPreview() {
    NunchukTheme {
        NcLoadingDialog(
            customMessage = "Custom message",
            onDismiss = {}
        )
    }
}