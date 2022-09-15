package com.nunchuk.android.compose

import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.nunchuk.android.core.R

@Composable
fun NcCircleImage(
    modifier: Modifier = Modifier,
    size: Dp = 48.dp,
    iconSize: Dp = 0.dp,
    iconTintColor: Color = MaterialTheme.colors.primary,
    color: Color = colorResource(id = R.color.nc_whisper_color),
    @DrawableRes resId: Int,
) {
    Box(
        modifier = modifier
            .size(size)
            .background(color = color, shape = CircleShape),
        contentAlignment = Alignment.Center
    ) {
        if (iconSize > 0.dp) {
            Icon(
                modifier = Modifier.size(iconSize),
                painter = painterResource(id = resId),
                tint = iconTintColor,
                contentDescription = null
            )
        } else {
            Icon(
                painter = painterResource(id = resId),
                tint = iconTintColor,
                contentDescription = null,
            )
        }
    }
}

@Preview
@Composable
fun NcCircleImagePreview() {
    NunchukTheme {
        NcCircleImage(resId = R.drawable.ic_nfc_card)
    }
}