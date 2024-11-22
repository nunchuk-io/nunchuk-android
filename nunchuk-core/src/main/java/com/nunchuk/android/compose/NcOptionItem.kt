package com.nunchuk.android.compose

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.nunchuk.android.core.R

@Composable
fun NcOptionItem(
    modifier: Modifier = Modifier,
    isSelected: Boolean,
    label: String,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier.fillMaxWidth(), onClick = onClick,
        border = BorderStroke(
            width = 2.dp,
            color = if (isSelected) colorResource(id = R.color.nc_primary_color) else Color(
                0xFFDEDEDE
            )
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(modifier = Modifier.padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
            NcRadioButton(modifier = Modifier.size(24.dp), selected = isSelected, onClick = onClick)
            Text(
                modifier = Modifier.padding(start = 12.dp),
                text = label,
                style = NunchukTheme.typography.title
            )
        }
    }
}

@Preview
@Composable
fun PreviewOptionItem() {
    NunchukTheme {
        NcOptionItem(
            isSelected = true,
            label = "Option Item",
            onClick = {}
        )
    }
}