package com.nunchuk.android.compose

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.nunchuk.android.core.R
import com.nunchuk.android.core.coin.CollectionFlow
import com.nunchuk.android.core.util.shorten

@Composable
fun CollectionItem(
    modifier: Modifier,
    id: Int = -1,
    name: String = "",
    numCoins: Int = 0,
    checked: Boolean = false,
    collectionFlow: Int = CollectionFlow.NONE,
    onCollectionClick: () -> Unit = {},
    onCheckedChange: ((Boolean) -> Unit) = {}
) {
    Row(
        modifier = modifier
            .clickable {
                if (collectionFlow == CollectionFlow.VIEW) {
                    onCollectionClick()
                }
            },
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(48.dp, 48.dp)
                .clip(CircleShape)
                .background(color = colorResource(id = R.color.nc_beeswax_light)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = name.shorten(),
                color = colorResource(id = R.color.nc_grey_g7),
                style = NunchukTheme.typography.title
            )
        }
        Column(
            modifier = Modifier
                .padding(start = 12.dp)
                .weight(1f)
        ) {
            Text(
                text = name, style = NunchukTheme.typography.body
            )
            Text(
                text = stringResource(id = R.string.nc_num_coins_data, numCoins.toString()),
                modifier = Modifier.padding(top = 4.dp),
                style = NunchukTheme.typography.bodySmall
            )
        }
        if (collectionFlow == CollectionFlow.VIEW) {
            Icon(
                painter = painterResource(id = R.drawable.ic_arrow), contentDescription = ""
            )
        } else {
            if (collectionFlow != CollectionFlow.MOVE) {
                Checkbox(checked = checked, onCheckedChange = onCheckedChange)
            } else {
                RadioButton(selected = checked, onClick = {
                    onCheckedChange(true)
                })
            }
        }
    }
}