package com.nunchuk.android.wallet.personal.components.taproot.configure

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.nunchuk.android.compose.NcCheckBox
import com.nunchuk.android.compose.NunchukTheme
import com.nunchuk.android.compose.signer.SignerCard
import com.nunchuk.android.core.R
import com.nunchuk.android.core.signer.SignerModel

@Composable
fun ConfigSignerItem(
    signer: SignerModel,
    checkable: Boolean = true,
    isChecked: Boolean = false,
    isShowPath: Boolean = false,
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
        if (signer.isVisible) {
            SignerCard(
                modifier = Modifier.weight(1f),
                item = signer,
            ) {
                if (isShowPath && signer.derivationPath.isNotEmpty()) {
                    Text(
                        modifier = Modifier
                            .padding(top = 4.dp)
                            .clickable(onClick = { onEditPath(signer) }),
                        text = stringResource(R.string.nc_bip32_path, signer.derivationPath),
                        style = NunchukTheme.typography.bodySmall
                    )
                }
            }
        } else {
            SignerCard(
                item = signer,
                modifier = Modifier.weight(1f),
                signerIcon = R.drawable.ic_signer_empty_state,
                isShowKeyTypeBadge = false,
            ) {
                if (isShowPath && signer.derivationPath.isNotEmpty()) {
                    Text(
                        modifier = Modifier
                            .padding(top = 4.dp)
                            .clickable(onClick = { onEditPath(signer) }),
                        text = stringResource(R.string.nc_bip32_path, signer.derivationPath),
                        style = NunchukTheme.typography.bodySmall
                    )
                }
            }
        }

        if (checkable) {
            NcCheckBox(
                checked = isChecked,
                onCheckedChange = { onSelectSigner(signer, it) },
            )
        }
    }
}
