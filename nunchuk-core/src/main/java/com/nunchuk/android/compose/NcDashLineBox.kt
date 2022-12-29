package com.nunchuk.android.compose

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.nunchuk.android.compose.dashedBorder
import com.nunchuk.android.core.R

@Composable
fun NcDashLineBox(
    modifier: Modifier = Modifier,
    width: Dp = 2.dp,
    dashWidth: Dp = 8.dp,
    color: Color = colorResource(id = R.color.nc_primary_color),
    content: @Composable BoxScope.() -> Unit
) {
        Box(
            modifier = modifier.dashedBorder(width, color, RoundedCornerShape(8.dp), dashWidth, dashWidth), contentAlignment = Alignment.Center
        ) {
            content()
        }
}

@Preview
@Composable
fun NcDashLineBoxPreview() {
    NcDashLineBox(Modifier.fillMaxWidth().height(height = 100.dp)) {
        Text(textAlign = TextAlign.Center, text = "Demo Text Content")
    }
}
