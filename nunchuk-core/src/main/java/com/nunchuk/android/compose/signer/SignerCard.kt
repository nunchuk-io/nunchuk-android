package com.nunchuk.android.compose.signer

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import com.nunchuk.android.compose.NcCircleImage
import com.nunchuk.android.compose.NcTag
import com.nunchuk.android.compose.NunchukTheme
import com.nunchuk.android.compose.greyLight
import com.nunchuk.android.compose.provider.SignerModelProvider
import com.nunchuk.android.compose.textPrimary
import com.nunchuk.android.core.R
import com.nunchuk.android.core.signer.SignerModel
import com.nunchuk.android.core.util.toReadableDrawableResId
import com.nunchuk.android.core.util.toReadableSignerType
import com.nunchuk.android.type.SignerType

@Composable
fun SignerCard(
    item: SignerModel,
    modifier: Modifier = Modifier,
    showValueKey: Boolean = false,
    signerIcon: Int? = null,
    isShowKeyTypeBadge: Boolean = true,
    xfpContent: @Composable (() -> Unit)? = null,
    content: @Composable () -> Unit = {}
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        NcCircleImage(
            resId = signerIcon ?: item.toReadableDrawableResId(),
            color = MaterialTheme.colorScheme.greyLight,
            iconTintColor = MaterialTheme.colorScheme.textPrimary,
        )
        Column(
            modifier = Modifier
                .weight(1.0f)
                .padding(start = 8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = item.name,
                style = NunchukTheme.typography.body
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                if (showValueKey) {
                    NcTag(
                        label = stringResource(R.string.nc_value_key),
                        backgroundColor = colorResource(id = R.color.nc_fill_denim),
                    )
                }
                if (item.type != SignerType.SERVER && isShowKeyTypeBadge) {
                    NcTag(
                        label = item.toReadableSignerType(context = LocalContext.current),
                        backgroundColor = colorResource(
                            id = R.color.nc_bg_mid_gray
                        ),
                    )
                }
                if (item.isShowAcctX()) {
                    NcTag(
                        label = stringResource(R.string.nc_acct_x, item.index),
                        backgroundColor = colorResource(
                            id = R.color.nc_bg_mid_gray
                        ),
                    )
                }
            }
            if (item.type != SignerType.SERVER) {
                if (xfpContent != null) {
                    xfpContent()
                } else {
                    Text(
                        text = item.getXfpOrCardIdLabel(),
                        style = NunchukTheme.typography.bodySmall,
                    )
                }
            }
            content()
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun AddKeyListScreenHoneyBadgerPreview(
    @PreviewParameter(SignerModelProvider::class) signer: SignerModel,
) {
    NunchukTheme {
        SignerCard(item = signer)
    }
}