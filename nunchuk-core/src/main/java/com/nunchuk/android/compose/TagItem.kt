package com.nunchuk.android.compose

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.Checkbox
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.nunchuk.android.core.R
import com.nunchuk.android.core.coin.TagFlow
import com.nunchuk.android.core.util.hexToColor

@Preview
@Composable
fun TagItem(
    id: Int = -1,
    name: String = "",
    color: String = "",
    numCoins: Int = 0,
    checked: Boolean = false,
    tagFlow: Int = TagFlow.NONE,
    onTagClick: () -> Unit = {},
    onCheckedChange: ((Boolean) -> Unit) = {}
) {
    Row(
        modifier = Modifier
            .padding(horizontal = 16.dp)
            .clickable {
                if (tagFlow == TagFlow.VIEW) {
                    onTagClick()
                }
            },
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(48.dp, 48.dp)
                .clip(CircleShape)
                .background(color = color.hexToColor())
        )
        Column(
            modifier = Modifier
                .padding(start = 12.dp)
                .weight(1f)
        ) {
            Text(
                text = name,
                style = NunchukTheme.typography.body
            )
            Text(
                text = stringResource(id = R.string.nc_num_coins_data, numCoins.toString()),
                modifier = Modifier.padding(top = 4.dp),
                style = NunchukTheme.typography.bodySmall
            )
        }
        if (tagFlow == TagFlow.VIEW) {
            Icon(
                painter = painterResource(id = R.drawable.ic_arrow),
                contentDescription = ""
            )
        } else {
            Checkbox(checked = checked, onCheckedChange = onCheckedChange)
        }
    }
}