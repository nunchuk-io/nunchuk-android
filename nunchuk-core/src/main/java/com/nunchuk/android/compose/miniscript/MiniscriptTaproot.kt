package com.nunchuk.android.compose.miniscript

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.PreviewLightDark
import com.nunchuk.android.compose.NcBadgePrimary
import com.nunchuk.android.compose.NunchukTheme
import com.nunchuk.android.core.signer.SignerModel

@Composable
fun MiniscriptTaproot(
    modifier: Modifier = Modifier,
    keyPath: String,
    data: ScriptNodeData,
    signer: SignerModel?,
    onChangeBip32Path: (String, SignerModel) -> Unit,
    onActionKey: (String, SignerModel?) -> Unit,
    divider: @Composable () -> Unit = { HorizontalDivider() }
) {
    Column(
        modifier = modifier
    ) {
        NcBadgePrimary(
            text = if (keyPath.isNotEmpty()) "Key path" else "Key path disabled",
            enabled = keyPath.isNotEmpty(),
        )
        if (keyPath.isNotEmpty()) {
            CreateKeyItem(
                key = keyPath,
                signer = signer,
                position = "1",
                onChangeBip32Path = onChangeBip32Path,
                onActionKey = onActionKey,
                data = data,
                showThreadCurve = false
            )

            divider()
        }
    }
}

@PreviewLightDark
@Composable
fun MiniscriptTaprootPreview() {
    NunchukTheme {
        MiniscriptTaproot(
            keyPath = "A",
            data = ScriptNodeData(),
            signer = null,
            onChangeBip32Path = { _, _ -> },
            onActionKey = { _, _ -> }
        )
    }
}