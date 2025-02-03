package com.nunchuk.android.main.groupwallet.component

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.nunchuk.android.compose.NcCircleImage
import com.nunchuk.android.compose.NcDashLineBox
import com.nunchuk.android.compose.NcOutlineButton
import com.nunchuk.android.compose.NunchukTheme
import com.nunchuk.android.compose.signer.SignerCard
import com.nunchuk.android.compose.strokePrimary
import com.nunchuk.android.core.signer.SignerModel
import com.nunchuk.android.main.R
import com.nunchuk.android.main.groupwallet.avatarColors

@Composable
fun FreeAddKeyCard(
    index: Int,
    modifier: Modifier = Modifier,
    signer: SignerModel? = null,
    onAddClicked: () -> Unit,
    onRemoveClicked: () -> Unit,
) {
    if (signer != null) {
        Row(
            modifier = Modifier
                .border(
                    width = 1.dp,
                    color = MaterialTheme.colorScheme.strokePrimary,
                    shape = RoundedCornerShape(8.dp)
                )
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (signer.isVisible) {
                SignerCard(item = signer, modifier = Modifier.weight(1.0f))
            } else {
                NcCircleImage(
                    iconSize = 48.dp,
                    resId = R.drawable.ic_user,
                    color = avatarColors[index % avatarColors.size]
                )
                Column(
                    modifier = Modifier
                        .weight(1.0f)
                        .padding(start = 8.dp)
                ) {
                    Text(
                        text = signer.getXfpOrCardIdLabel(),
                        style = NunchukTheme.typography.body
                    )
                }
            }
            if (signer.isVisible) {
                NcOutlineButton(
                    modifier = Modifier.height(36.dp),
                    onClick = onRemoveClicked,
                ) {
                    Text(text = stringResource(id = R.string.nc_remove))
                }
            }
        }
    } else {
        NcDashLineBox(
            modifier = modifier,
            content = {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    NcCircleImage(resId = R.drawable.ic_key, iconSize = 24.dp)
                    Column(
                        modifier = Modifier
                            .weight(1.0f)
                            .padding(start = 8.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.nc_key_with_index, "#${index + 1}"),
                            style = NunchukTheme.typography.body
                        )
                    }
                    NcOutlineButton(
                        modifier = Modifier.height(36.dp),
                        onClick = onAddClicked,
                    ) {
                        Text(text = stringResource(id = R.string.nc_add_key))
                    }
                }
            }
        )
    }
}