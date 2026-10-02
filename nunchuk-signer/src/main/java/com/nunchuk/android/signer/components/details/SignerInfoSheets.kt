package com.nunchuk.android.signer.components.details

import android.content.res.Configuration
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.nunchuk.android.compose.NcTextField
import com.nunchuk.android.compose.NunchukTheme
import com.nunchuk.android.signer.R
import com.nunchuk.android.signer.components.details.model.SingerOption
import com.nunchuk.android.type.SignerType
import kotlinx.coroutines.delay

@Composable
internal fun SignerOptionsSheet(
    signerType: SignerType?,
    canSignMessage: Boolean,
    canQrUnlock: Boolean,
    onOption: (SingerOption) -> Unit,
    onDismiss: () -> Unit,
) {
    val options = buildList {
        if (canQrUnlock) add(SingerOption.QR_UNLOCK to R.string.nc_jade_qr_unlock)
        if (canSignMessage) add(SingerOption.SIGN_MESSAGE to R.string.nc_signer_sign_message)
        if (signerType == SignerType.NFC) {
            add(SingerOption.TOP_UP to R.string.nc_top_up_xpubs)
            add(SingerOption.CHANGE_CVC to R.string.nc_change_cvc)
            add(SingerOption.BACKUP_KEY to R.string.nc_back_up_key)
        }
        if (signerType == SignerType.PORTAL_NFC) {
            add(SingerOption.CHECK_FIRMWARE to R.string.nc_check_firmware_verion)
            add(SingerOption.UPDATE_FIRMWARE to R.string.nc_update_firmware)
        }
        add(SingerOption.REMOVE_KEY to R.string.nc_remove_key)
    }
    SignerInfoBottomSheet(onDismiss = onDismiss) {
        Column(Modifier.verticalScroll(rememberScrollState()).padding(16.dp)) {
            options.forEach { (option, label) ->
                Text(
                    text = stringResource(label),
                    style = NunchukTheme.typography.body,
                    color = colorResource(
                        if (option == SingerOption.REMOVE_KEY) R.color.nc_orange_dark_color
                        else R.color.nc_text_primary
                    ),
                    modifier = Modifier
                        .padding(top = if (option == SingerOption.CHANGE_CVC) 0.dp else 4.dp)
                        .fillMaxWidth()
                        .clickable { onOption(option) }
                        .padding(vertical = 16.dp),
                )
            }
        }
    }
}

@Composable
internal fun SignerNameSheet(name: String, onSave: (String) -> Unit, onDismiss: () -> Unit) {
    var value by rememberSaveable(name) { mutableStateOf(name) }
    val focusRequester = remember { FocusRequester() }
    val keyboard = LocalSoftwareKeyboardController.current
    val save = {
        if (value.isNotEmpty()) {
            keyboard?.hide()
            onSave(value)
        }
    }

    SignerInfoBottomSheet(onDismiss = onDismiss) {
        Column(
            Modifier.fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(top = 8.dp, bottom = 24.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = onDismiss) {
                    Icon(
                        painter = painterResource(R.drawable.ic_close),
                        contentDescription = stringResource(R.string.nc_text_close),
                        tint = colorResource(R.color.nc_text_primary),
                    )
                }
                // Hidden (not just disabled) while empty, as in the old sheet
                if (value.isNotEmpty()) {
                    TextButton(onClick = save) {
                        Text(
                            text = stringResource(R.string.nc_text_save),
                            style = NunchukTheme.typography.textLink,
                        )
                    }
                }
            }
            NcTextField(
                title = stringResource(R.string.nc_text_signer_name),
                titleStyle = NunchukTheme.typography.body,
                value = value,
                onValueChange = { value = it },
                singleLine = true,
                maxLines = 1,
                isTransparent = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { save() }),
                modifier = Modifier
                    .padding(horizontal = 16.dp)
                    .padding(top = 8.dp)
                    .focusRequester(focusRequester),
            )
            HorizontalDivider(
                color = colorResource(R.color.nc_bg_mid_gray),
                modifier = Modifier.padding(horizontal = 16.dp),
            )
        }
        // Follow the input-sheet flow: focus after the sheet's entrance animation.
        LaunchedEffect(Unit) {
            delay(300L)
            focusRequester.requestFocus()
        }
    }
}

/** Match the original signer sheets without Material's default handle or surface tint. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun SignerInfoBottomSheet(
    onDismiss: () -> Unit,
    content: @Composable ColumnScope.() -> Unit,
) {
    ModalBottomSheet(
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
        containerColor = colorResource(R.color.nc_popup_bg),
        tonalElevation = 0.dp,
        dragHandle = {},
        content = content,
    )
}

@Preview(showBackground = true)
@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun SignerNameSheetPreview() {
    NunchukTheme {
        SignerNameSheet(name = "TAPSIGNER", onSave = {}, onDismiss = {})
    }
}

@Preview(showBackground = true)
@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun SignerOptionsSheetPreview() {
    NunchukTheme {
        SignerOptionsSheet(
            signerType = SignerType.NFC,
            canSignMessage = true,
            canQrUnlock = false,
            onOption = {},
            onDismiss = {},
        )
    }
}
