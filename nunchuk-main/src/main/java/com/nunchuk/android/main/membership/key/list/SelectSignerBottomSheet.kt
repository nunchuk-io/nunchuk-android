package com.nunchuk.android.main.membership.key.list

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import com.nunchuk.android.core.signer.SignerModel
import com.nunchuk.android.model.signer.SupportedSigner
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SelectSignerBottomSheet(
    sheetState: SheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    args: TapSignerListBottomSheetFragmentArgs,
    onDismiss: () -> Unit = {},
    supportedSigners: List<SupportedSigner> = emptyList(),
    onAddExistKey: (SignerModel) -> Unit = {},
    onAddNewKey: () -> Unit = {},
) {
    val coroutineScope = rememberCoroutineScope()

    ModalBottomSheet(
        sheetState = sheetState,
        onDismissRequest = onDismiss,
        dragHandle = {},
        content = {
            TapSignerListScreen(
                args = args,
                onCloseClicked = {
                    coroutineScope.launch { sheetState.hide() }
                    onDismiss()
                },
                supportedSigners = supportedSigners,
                onAddExistKey = onAddExistKey,
                onAddNewKey = onAddNewKey
            )
        }
    )
}