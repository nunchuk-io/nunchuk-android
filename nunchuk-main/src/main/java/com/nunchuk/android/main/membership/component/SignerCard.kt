package com.nunchuk.android.main.membership.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import com.nunchuk.android.compose.NcCircleImage
import com.nunchuk.android.compose.NcRadioButton
import com.nunchuk.android.compose.NcTag
import com.nunchuk.android.compose.NunchukTheme
import com.nunchuk.android.compose.provider.SignerModelProvider
import com.nunchuk.android.core.signer.SignerModel
import com.nunchuk.android.core.util.toReadableDrawableResId
import com.nunchuk.android.core.util.toReadableSignerType
import com.nunchuk.android.main.R

@Composable
fun SignerCard(
    signer: SignerModel,
    modifier: Modifier = Modifier,
    onSignerSelected: (signer: SignerModel) -> Unit = {},
    isSelected: Boolean = false,
    isSelectable: Boolean = true,
) {
    Row(
        modifier = modifier.clickable(enabled = isSelectable) { onSignerSelected(signer) },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        NcCircleImage(resId = signer.toReadableDrawableResId())
        Column(
            modifier = Modifier
                .padding(start = 12.dp)
                .weight(1.0f)
        ) {
            Text(text = signer.name, style = NunchukTheme.typography.body)
            Text(
                modifier = Modifier.padding(top = 4.dp),
                text = signer.getXfpOrCardIdLabel(),
                style = NunchukTheme.typography.bodySmall.copy(
                    color = colorResource(
                        id = R.color.nc_grey_dark_color
                    )
                ),
            )
            if (isSelectable) {
                NcTag(
                    modifier = Modifier
                        .padding(top = 6.dp),
                    label = signer.toReadableSignerType(LocalContext.current),
                )
            }
        }
        if (isSelectable) {
            NcRadioButton(selected = isSelected, onClick = { onSignerSelected(signer) })
        }
    }
}

@Preview(showBackground = true)
@Composable
fun SignerCardPreview(@PreviewParameter(SignerModelProvider::class) signer: SignerModel) {
    NunchukTheme {
        SignerCard(signer = signer)
    }
}