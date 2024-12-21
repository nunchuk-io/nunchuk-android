package com.nunchuk.android.transaction.components.details

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nunchuk.android.compose.NcIcon
import com.nunchuk.android.compose.NcPrimaryDarkButton
import com.nunchuk.android.compose.NunchukTheme
import com.nunchuk.android.compose.provider.SignerModelProvider
import com.nunchuk.android.compose.signer.SignerCard
import com.nunchuk.android.core.signer.SignerModel
import com.nunchuk.android.core.util.canSign
import com.nunchuk.android.transaction.R

@Composable
fun TransactionSignerView(
    modifier: Modifier = Modifier,
    signer: SignerModel,
    showValueKey: Boolean,
    isSigned: Boolean,
    canSign: Boolean,
    onSignClick: (SignerModel) -> Unit
) {
    Row(
        modifier = modifier
            .fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        SignerCard(
            modifier = Modifier.weight(1f),
            item = signer,
            showValueKey = showValueKey
        )

        if (isSigned) {
            Text(
                text = stringResource(R.string.nc_transaction_signed),
                style = NunchukTheme.typography.captionTitle,
            )

            NcIcon(
                modifier = Modifier.padding(start = 8.dp),
                painter = painterResource(R.drawable.ic_check_circle_24),
                contentDescription = "Signed",
            )
        } else if (canSign && signer.type.canSign) {
            NcPrimaryDarkButton(onClick = { onSignClick(signer) }, height = 36.dp) {
                Text(
                    text = stringResource(id = R.string.nc_sign),
                    style = LocalTextStyle.current.copy(fontSize = 12.sp)
                )
            }
        }
    }
}

@PreviewLightDark
@Composable
private fun TransactionSignerViewPreview(
    @PreviewParameter(SignerModelProvider::class) signer: SignerModel,
) {
    NunchukTheme {
        TransactionSignerView(
            signer = signer,
            showValueKey = true,
            isSigned = false,
            canSign = true,
            onSignClick = {},
        )
    }
}