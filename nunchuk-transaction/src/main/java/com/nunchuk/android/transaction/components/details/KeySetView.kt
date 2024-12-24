package com.nunchuk.android.transaction.components.details

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import com.nunchuk.android.compose.NcIcon
import com.nunchuk.android.compose.NunchukTheme
import com.nunchuk.android.compose.backgroundMidGray
import com.nunchuk.android.compose.fillBeeswax
import com.nunchuk.android.compose.provider.SignersModelProvider
import com.nunchuk.android.compose.strokePrimary
import com.nunchuk.android.compose.textSecondary
import com.nunchuk.android.core.signer.SignerModel
import com.nunchuk.android.core.util.isPendingSignatures
import com.nunchuk.android.core.util.signDone
import com.nunchuk.android.model.KeySetStatus
import com.nunchuk.android.transaction.R
import com.nunchuk.android.type.TransactionStatus

@Composable
fun KeySetView(
    modifier: Modifier = Modifier,
    keySetIndex: Int,
    keySet: KeySetStatus,
    signers: Map<String, SignerModel>,
    requiredSignatures: Int,
    showDivider: Boolean = false,
    onSignClick: (SignerModel) -> Unit = {},
) {
    val round = if (keySet.status == TransactionStatus.PENDING_NONCE) 1 else 2
    val pendingSignatures = requiredSignatures - keySet.signerStatus.count { it.value }
    Column(
        modifier = modifier
            .padding(horizontal = 16.dp)
            .padding(top = 16.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
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

            if (pendingSignatures > 0 && keySet.status.isPendingSignatures()) {
                NcIcon(
                    painter = painterResource(R.drawable.ic_pending_signatures),
                    contentDescription = "Required Signers",
                    modifier = Modifier.padding(start = 4.dp)
                )

                Text(
                    text = pluralStringResource(
                        if (round == 1) {
                            R.plurals.nc_transaction_pending_nonce
                        } else {
                            R.plurals.nc_transaction_pending_signature
                        },
                        pendingSignatures,
                        pendingSignatures
                    ),
                    style = NunchukTheme.typography.bodySmall,
                    modifier = Modifier.padding(start = 4.dp)
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            if (keySet.status.signDone()) {
                Text(
                    text = stringResource(R.string.nc_text_completed),
                    style = NunchukTheme.typography.titleSmall.copy(
                        color = Color.White
                    ),
                    modifier = Modifier
                        .background(
                            color = colorResource(R.color.nc_slime_dark),
                            shape = RoundedCornerShape(20.dp)
                        )
                        .padding(horizontal = 8.dp)
                )
            } else {
                val color = when {
                    round == 1 && requiredSignatures == pendingSignatures -> MaterialTheme.colorScheme.backgroundMidGray
                    round == 1 -> MaterialTheme.colorScheme.fillBeeswax
                    else -> colorResource(R.color.nc_primary_y0)
                }
                Text(
                    text = stringResource(R.string.nc_round_2, round),
                    style = NunchukTheme.typography.titleSmall.copy(
                        color = colorResource(R.color.nc_grey_g7)
                    ),
                    modifier = Modifier
                        .background(
                            color = color,
                            shape = RoundedCornerShape(20.dp)
                        )
                        .padding(horizontal = 8.dp)
                )
            }
        }

        if (keySetIndex == 0) {
            Text(
                text = stringResource(R.string.nc_better_privacy_and_lower_fees),
                style = NunchukTheme.typography.bodySmall,
                modifier = Modifier.padding(top = 4.dp),
                color = MaterialTheme.colorScheme.textSecondary
            )
        }

        keySet.signerStatus.forEach { (fingerPrint, isSigned) ->
            signers[fingerPrint]?.let { signer ->
                TransactionSignerView(
                    modifier = Modifier.padding(top = 16.dp),
                    signer = signer,
                    showValueKey = keySetIndex == 0,
                    isSigned = isSigned || keySet.status.signDone(),
                    canSign = !keySet.status.signDone(),
                    onSignClick = onSignClick
                )
            }
        }

        if (showDivider) {
            HorizontalDivider(
                modifier = Modifier
                    .padding(top = 16.dp)
                    .fillMaxWidth(),
                color = MaterialTheme.colorScheme.strokePrimary,
                thickness = 1.dp
            )
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
                signers = signers.associateBy { it.fingerPrint },
                keySetIndex = 0,
                requiredSignatures = 2,
                keySet = KeySetStatus(
                    status = TransactionStatus.PENDING_NONCE,
                    signerStatus = mapOf(
                        "79EB35F4" to false,
                        "79EB35F5" to false,
                    )
                ),
                showDivider = true
            )
            KeySetView(
                signers = signers.associateBy { it.fingerPrint },
                keySetIndex = 0,
                requiredSignatures = 2,
                keySet = KeySetStatus(
                    status = TransactionStatus.PENDING_SIGNATURES,
                    signerStatus = mapOf(
                        "79EB35F4" to true,
                        "79EB35F5" to false,
                    )
                ),
                showDivider = true
            )
            KeySetView(
                signers = signers.associateBy { it.fingerPrint },
                keySetIndex = 1,
                requiredSignatures = 2,
                keySet = KeySetStatus(
                    status = TransactionStatus.PENDING_SIGNATURES,
                    signerStatus = mapOf(
                        "79EB35F4" to true,
                        "79EB35F5" to false,
                    )
                )
            )
        }
    }

}