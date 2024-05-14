package com.nunchuk.android.compose

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.nunchuk.android.core.R
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NcInputBottomSheet(
    sheetState: SheetState = rememberModalBottomSheetState(),
    title: String,
    onDone: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    var text by remember { mutableStateOf("") }

    ModalBottomSheet(
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp),
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.background,
        tonalElevation = 0.dp,
        content = {
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                IconButton(onClick = onDismiss) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_close),
                        contentDescription = "Icon close"
                    )
                }

                TextButton(onClick = { onDone(text) }) {
                    Text(
                        text = stringResource(id = R.string.nc_text_save),
                        style = NunchukTheme.typography.textLink,
                    )
                }
            }
            Text(
                text = title,
                modifier = Modifier.padding(start = 16.dp, end = 16.dp),
                style = NunchukTheme.typography.titleLarge
            )

            NcTextField(
                modifier = Modifier
                    .padding(top = 8.dp, start = 4.dp, end = 4.dp),
                title = "",
                value = text,
                minLines = 5,
                borderColor = Color.Transparent,
            ) {
                text = it
            }

            Spacer(modifier = Modifier.height(24.dp))
        },
        dragHandle = { }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Preview(showBackground = true)
@Composable
private fun NcInputBottomSheetPreview() {
    NunchukTheme {
        NcInputBottomSheet(
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            title = "Title",
            onDone = {},
            onDismiss = {}
        )
    }
}
