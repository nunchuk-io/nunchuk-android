package com.nunchuk.android.compose

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nunchuk.android.compose.NunchukTheme
import com.nunchuk.android.core.util.toColor
import com.nunchuk.android.core.util.toName
import com.nunchuk.android.model.UnspentOutput

@Composable
fun CoinStatusBadge(output: UnspentOutput) {
    val name = output.status.toName(LocalContext.current)
    if (name.isNotEmpty()) {
        val label = if (output.isReceive) {
            "Incoming ($name)"
        } else {
            "Outgoing ($name)"
        }
        Text(
            modifier = Modifier
                .padding(start = 4.dp)
                .background(
                    Color(output.status.toColor(LocalContext.current)),
                    shape = RoundedCornerShape(20.dp)
                )
                .padding(horizontal = 8.dp),
            text = label,
            style = NunchukTheme.typography.caption.copy(fontSize = 10.sp),
        )
    }
}
