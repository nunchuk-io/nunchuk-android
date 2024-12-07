package com.nunchuk.android.wallet.personal.components.taproot.configure

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.unit.dp
import com.nunchuk.android.compose.NcCheckBox
import com.nunchuk.android.compose.signer.SignerCard
import com.nunchuk.android.core.signer.SignerModel

@Composable
fun ConfigSignerItem(
    signer: SignerModel,
    checkable: Boolean = true,
    isChecked: Boolean = false,
    onSelectSigner: (SignerModel, Boolean) -> Unit,
    onEditPath: (SignerModel) -> Unit = {},
) {
    Row(
        modifier = Modifier
            .alpha(if (checkable) 1f else 0.4f)
            .clickable(enabled = checkable) { onSelectSigner(signer, !isChecked) }
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        SignerCard(
            modifier = Modifier.weight(1f),
            item = signer,
        )

        if (checkable) {
            NcCheckBox(
                checked = isChecked,
                onCheckedChange = { onSelectSigner(signer, it) },
            )
        }
    }
}
