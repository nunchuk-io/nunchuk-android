package com.nunchuk.android.transaction.components.details

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import com.nunchuk.android.compose.NcIcon
import com.nunchuk.android.compose.NcPrimaryDarkButton
import com.nunchuk.android.compose.NunchukTheme
import com.nunchuk.android.compose.provider.SignersModelProvider
import com.nunchuk.android.compose.signer.SignerCard
import com.nunchuk.android.core.signer.SignerModel
import com.nunchuk.android.transaction.R

@Composable
fun KeySetView(
    modifier: Modifier = Modifier,
    signerStatus: Map<String, Boolean>,
    signers: List<SignerModel>,
    keySetIndex: Int,
    requiredSignatures: Int,
) {
    Column(
        modifier = modifier.padding(16.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (keySetIndex == 0) {
                NcIcon(
                    painter = painterResource(R.drawable.ic_nc_star_dark),
                    contentDescription = "Value Key Set",
                )

                Text(
                    text = "Value Keyset",
                    style = NunchukTheme.typography.captionTitle,
                    modifier = Modifier.padding(start = 8.dp)
                )
            } else {
                Text(
                    text = "Keyset ${keySetIndex + 1}",
                    style = NunchukTheme.typography.captionTitle,
                    modifier = Modifier.padding(start = 8.dp)
                )
            }

            if (requiredSignatures > 0) {
                NcIcon(
                    painter = painterResource(R.drawable.ic_pending_signatures),
                    contentDescription = "Required Signers",
                    modifier = Modifier.padding(start = 4.dp)
                )

                Text(
                    text = pluralStringResource(
                        R.plurals.nc_transaction_pending_signature,
                        requiredSignatures,
                        requiredSignatures
                    ),
                    style = NunchukTheme.typography.bodySmall,
                    modifier = Modifier.padding(start = 4.dp)
                )
            }
        }

        if (keySetIndex == 0) {
            Text(
                text = stringResource(R.string.nc_better_privacy_and_lower_fees),
                style = NunchukTheme.typography.bodySmall,
                modifier = Modifier.padding(top = 4.dp)
            )
        }

        signers.forEach {
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                SignerCard(
                    modifier = Modifier.weight(1f),
                    item = it,
                )

                if (signerStatus[it.fingerPrint] == true) {
                    Text(
                        text = stringResource(R.string.nc_transaction_signed),
                        style = NunchukTheme.typography.captionTitle,
                    )

                    NcIcon(
                        modifier = Modifier.padding(start = 8.dp),
                        painter = painterResource(R.drawable.ic_check_circle_24),
                        contentDescription = "Signed",
                    )
                } else {
                    NcPrimaryDarkButton(onClick = {}, height = 36.dp) {
                        Text(stringResource(R.string.nc_sign))
                    }
                }
            }
        }
    }
}

@PreviewLightDark
@Composable
private fun KeySetViewPreview(
    @PreviewParameter(SignersModelProvider::class) signers: List<SignerModel>,
) {
    NunchukTheme {
        Column(Modifier.background(MaterialTheme.colorScheme.background)) {
            KeySetView(
                signers = signers,
                keySetIndex = 0,
                requiredSignatures = 2,
                signerStatus = mapOf(
                    "79EB35F4" to true,
                    "79EB35F5" to false,
                )
            )
            KeySetView(
                signers = signers,
                keySetIndex = 1,
                requiredSignatures = 2,
                signerStatus = mapOf(
                    "79EB35F4" to true,
                    "79EB35F5" to false,
                )
            )
        }
    }

}