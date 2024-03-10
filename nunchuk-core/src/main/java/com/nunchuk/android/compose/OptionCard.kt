package com.nunchuk.android.compose

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.nunchuk.android.core.R

@Composable
fun OptionCard(
    containerColor: Color,
    title: String,
    description: String,
    painter: Painter,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {},
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = CardDefaults.shape,
        color = containerColor,
        shadowElevation = 1.dp,
        tonalElevation = 0.dp,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.07f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 24.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = title, style = NunchukTheme.typography.title)
                Text(
                    modifier = Modifier.padding(top = 8.dp),
                    text = description,
                    style = NunchukTheme.typography.bodySmall
                )
            }
            Image(
                modifier = Modifier.align(Alignment.Bottom),
                painter = painter,
                contentDescription = "Image",
            )
        }
    }
}

@Preview
@Composable
private fun OptionCardPreview() {
    NunchukTheme {
        OptionCard(
            containerColor = MaterialTheme.colorScheme.background,
            title = "Title",
            description = "Description",
            painter = painterResource(id = R.drawable.ic_logo_light)
        )
    }
}