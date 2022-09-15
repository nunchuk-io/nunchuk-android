package com.nunchuk.android.signer.tapsigner.backup.verify.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Card
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.RadioButton
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.nunchuk.android.compose.NunchukTheme

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun TsVerifyBackUpOption(
    modifier: Modifier = Modifier,
    isSelected: Boolean,
    label: String,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier, onClick = onClick,
        border = BorderStroke(
            width = 2.dp, color = Color(0xFFDEDEDE)
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(modifier = Modifier.padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
            RadioButton(selected = isSelected, onClick = onClick)
            Text(text = label, style = NunchukTheme.typography.title)
        }
    }
}

@Preview
@Composable
fun TsVerifyBackUpOptionPreview() {
    NunchukTheme {
        TsVerifyBackUpOption(
            isSelected = true,
            label = "I’ll verify the backup via the app"
        ) {

        }
    }
}