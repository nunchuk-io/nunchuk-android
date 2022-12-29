package com.nunchuk.android.compose

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nunchuk.android.core.R

@Composable
fun NcTag(modifier: Modifier = Modifier, label: String, backgroundColor: Color = colorResource(id = R.color.nc_whisper_color)) {
    Text(
        modifier = modifier
            .background(
                color = backgroundColor,
                shape = RoundedCornerShape(20.dp)
            )
            .padding(horizontal = 8.dp),
        text = label,
        style = TextStyle(
            color = MaterialTheme.colors.primary,
            fontSize = 10.sp, fontWeight = FontWeight.W900, fontFamily = latoBold
        )
    )
}