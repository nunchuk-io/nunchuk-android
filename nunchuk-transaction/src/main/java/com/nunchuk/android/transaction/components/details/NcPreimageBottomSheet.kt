package com.nunchuk.android.transaction.components.details

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nunchuk.android.compose.NcTextField
import com.nunchuk.android.compose.NunchukTheme
import com.nunchuk.android.core.R
import com.nunchuk.android.core.miniscript.ScriptNodeType
import com.nunchuk.android.model.ScriptNode
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NcPreimageBottomSheet(
    walletId: String,
    txId: String,
    node: ScriptNode,
    sheetState: SheetState = rememberModalBottomSheetState(),
    onSuccess: (String) -> Unit,
    onDismiss: () -> Unit,
    viewModel: PreImageViewModel = hiltViewModel()
) {
    var text by remember { mutableStateOf("") }
    val focusRequester = remember { FocusRequester() }
    val state by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.reset()
        delay(300L)
        focusRequester.requestFocus()
    }

    LaunchedEffect(state.isSuccess) {
        if (state.isSuccess) {
            onSuccess(node.idString)
        }
    }

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
                        contentDescription = "Close"
                    )
                }

                TextButton(
                    onClick = {
                        viewModel.validateAndRevealPreimage(
                            walletId = walletId,
                            txId = txId,
                            node = node,
                            preimage = text
                        )
                    },
                ) {
                    Text(
                        text = stringResource(id = R.string.nc_enter),
                        style = NunchukTheme.typography.textLink,
                    )
                }
            }

            NcTextField(
                modifier = Modifier
                    .focusRequester(focusRequester)
                    .padding(horizontal = 16.dp)
                    .padding(top = 8.dp),
                title = stringResource(R.string.nc_enter_preimage_hex),
                titleStyle = NunchukTheme.typography.titleLarge,
                value = text,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Text,
                    imeAction = ImeAction.Done
                ),
                error = state.error,
                isTransparent = true
            ) {
                text = it
            }

            Spacer(modifier = Modifier.height(40.dp))
        },
        dragHandle = { }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Preview(showBackground = true)
@Composable
private fun NcPreimageBottomSheetPreview() {
    NunchukTheme {
        NcPreimageBottomSheet(
            walletId = "wallet_123",
            txId = "tx_123",
            node = ScriptNode(
                id = emptyList(),
                data = byteArrayOf(),
                type = ScriptNodeType.ANDOR.name,
                keys = listOf(),
                k = 0,
                timeLock = null,
                subs = listOf(
                    ScriptNode(
                        type = ScriptNodeType.ANDOR.name,
                        keys = listOf(),
                        subs = listOf(
                            ScriptNode(
                                type = ScriptNodeType.ANDOR.name,
                                keys = listOf("key_0_0", "key_1_0"),
                                subs = emptyList(),
                                k = 0,
                                id = emptyList(),
                                data = byteArrayOf(),
                                timeLock = null
                            )
                        ),
                        k = 0,
                        id = emptyList(),
                        data = byteArrayOf(),
                        timeLock = null
                    ),
                    ScriptNode(
                        type = ScriptNodeType.SHA256.name,
                        keys = listOf("key_0_1", "key_1_1"),
                        subs = emptyList(),
                        k = 0,
                        id = emptyList(),
                        data = byteArrayOf(),
                        timeLock = null
                    )
                )
            ),
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            onSuccess = {},
            onDismiss = {}
        )
    }
} 