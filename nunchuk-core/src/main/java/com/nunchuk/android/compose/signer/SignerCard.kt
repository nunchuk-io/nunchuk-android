package com.nunchuk.android.compose.signer

import androidx.compose.foundation.clickable
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

@Composable
fun SignerCard(
    item: SignerModel,
    modifier: Modifier = Modifier,
    onSignerSelected: (signer: SignerModel) -> Unit = {},
) {
    Row(
        modifier = modifier
            .clickable(onClick = { onSignerSelected(item) })
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        NcCircleImage(
            resId = item.toReadableDrawableResId(),
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
            Row {
                NcTag(
                    label = item.toReadableSignerType(context = LocalContext.current),
                    backgroundColor = colorResource(
                        id = R.color.nc_whisper_color
                    ),
                )
                if (item.isShowAcctX()) {
                    NcTag(
                        modifier = Modifier.padding(start = 4.dp),
                        label = stringResource(R.string.nc_acct_x, item.index),
                        backgroundColor = colorResource(
                            id = R.color.nc_whisper_color
                        ),
                    )
                }
            }
            Text(
                text = item.getXfpOrCardIdLabel(),
                style = NunchukTheme.typography.bodySmall
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun AddKeyListScreenHoneyBadgerPreview(
    @PreviewParameter(SignerModelProvider::class) signer: SignerModel,
) {
    NunchukTheme {
        SignerCard(item = signer)
    }
}