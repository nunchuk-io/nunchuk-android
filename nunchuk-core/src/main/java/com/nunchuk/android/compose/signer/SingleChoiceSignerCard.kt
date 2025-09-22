package com.nunchuk.android.compose.signer

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import com.nunchuk.android.compose.NcRadioButton
import com.nunchuk.android.compose.NunchukTheme
import com.nunchuk.android.compose.provider.SignerModelProvider
import com.nunchuk.android.core.R
import com.nunchuk.android.core.signer.SignerModel

@Composable
fun SingleChoiceSignerCard(
    modifier: Modifier = Modifier,
    signer: SignerModel,
    checkable: Boolean = true,
    isChecked: Boolean = false,
    isShowPath: Boolean = false,
    ignoreIndexCheckForAcctX: Boolean = false,
    onSelectSigner: (SignerModel) -> Unit,
    onEditPath: (SignerModel) -> Unit = {},
) {
    Row(
        modifier = modifier
            .alpha(if (checkable) 1f else 0.4f)
            .clickable(enabled = checkable) { onSelectSigner(signer) },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        SignerCard(
            modifier = Modifier.weight(1f),
            item = signer,
            ignoreIndexCheckForAcctX = ignoreIndexCheckForAcctX,
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

        if (checkable) {
            NcRadioButton(
                selected = isChecked,
                onClick = { onSelectSigner(signer) },
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun SingleChoiceSignerCardPreview(
    @PreviewParameter(SignerModelProvider::class) signer: SignerModel,
) {
    NunchukTheme {
        SingleChoiceSignerCard(signer = signer, onSelectSigner = { })
    }
}