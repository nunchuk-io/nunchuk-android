package com.nunchuk.android.transaction.components.details

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import com.nunchuk.android.compose.NcIcon
import com.nunchuk.android.compose.NunchukTheme
import com.nunchuk.android.compose.fillDenim2
import com.nunchuk.android.compose.provider.SignersModelProvider
import com.nunchuk.android.compose.signer.SignerCard
import com.nunchuk.android.compose.strokePrimary
import com.nunchuk.android.compose.textPrimary
import com.nunchuk.android.compose.textSecondary
import com.nunchuk.android.core.signer.SignerModel
import com.nunchuk.android.core.util.getBTCAmount
import com.nunchuk.android.core.util.getCurrencyAmount
import com.nunchuk.android.model.Amount
import com.nunchuk.android.model.KeySetStatus
import com.nunchuk.android.transaction.R
import com.nunchuk.android.type.TransactionStatus

@Composable
fun KeySetFeeView(
    modifier: Modifier = Modifier,
    keySetIndex: Int,
    keySet: KeySetStatus,
    signers: Map<String, SignerModel>,
    isValueKeySetDisable: Boolean = false,
    isSelected: Boolean = false,
    fee: Amount = Amount(0)
) {
    val isValueKeySet = keySetIndex == 0 && !isValueKeySetDisable
    Column(
        modifier = modifier
            .padding(horizontal = 16.dp)
            .padding(top = 16.dp)
            .border(
                width = 1.dp,
                color = if (isSelected) MaterialTheme.colorScheme.textPrimary else MaterialTheme.colorScheme.strokePrimary,
                shape = RoundedCornerShape(8.dp)
            )
            .background(MaterialTheme.colorScheme.background)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 12.dp)
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (isValueKeySet) {
                NcIcon(
                    painter = painterResource(R.drawable.ic_nc_star_dark),
                    contentDescription = "Value Key Set",
                    modifier = Modifier.size(16.dp)
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
                )
            }
        }

        if (isValueKeySet) {
            Text(
                text = stringResource(R.string.nc_better_privacy_and_lower_fees),
                style = NunchukTheme.typography.bodySmall,
                modifier = Modifier.padding(top = 4.dp).padding(horizontal = 16.dp).padding(start = 24.dp),
                color = MaterialTheme.colorScheme.textSecondary
            )
        }

        keySet.signerStatus.mapNotNull { signers[it.key] }
            .sortedBy { it.name }
            .forEach { signer ->
                SignerCard(
                    modifier = Modifier
                        .padding(top = 16.dp, start = 16.dp, end = 16.dp)
                        .fillMaxWidth(),
                    item = signer,
                    showValueKey = isValueKeySet,
                )
            }

        // Estimated fee section
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 12.dp)
                .background(
                    color = MaterialTheme.colorScheme.fillDenim2.copy(alpha = 0.4f),
                    shape = RoundedCornerShape(bottomStart = 8.dp, bottomEnd = 8.dp)
                )
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.nc_transaction_estimate_fee),
                    style = NunchukTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.textPrimary,
                    modifier = Modifier.weight(1f)
                )
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = fee.getBTCAmount(),
                        style = NunchukTheme.typography.body,
                        fontWeight = FontWeight.W600,
                        color = MaterialTheme.colorScheme.textPrimary
                    )
                    Text(
                        text = fee.getCurrencyAmount(),
                        style = NunchukTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.textPrimary
                    )
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
            KeySetFeeView(
                signers = signers.associateBy { it.fingerPrint },
                keySetIndex = 0,
                keySet = KeySetStatus(
                    status = TransactionStatus.PENDING_NONCE,
                    signerStatus = mapOf(
                        "79EB35F4" to false,
                        "79EB35F5" to false,
                    )
                ),
            )
            KeySetFeeView(
                signers = signers.associateBy { it.fingerPrint },
                keySetIndex = 1,
                keySet = KeySetStatus(
                    status = TransactionStatus.PENDING_SIGNATURES,
                    signerStatus = mapOf(
                        "79EB35F4" to true,
                        "79EB35F5" to false,
                    )
                ),
                isSelected = true
            )
        }
    }

}