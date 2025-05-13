package com.nunchuk.android.compose.miniscript

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import com.nunchuk.android.compose.NcIcon
import com.nunchuk.android.compose.NunchukTheme
import com.nunchuk.android.core.R

@Composable
fun PolicyHeader(
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        NcIcon(
            modifier = Modifier.size(24.dp),
            painter = painterResource(id = R.drawable.ic_policies),
            contentDescription = "Policies",
            tint = MaterialTheme.colorScheme.primary
        )
        Text(
            text = "Policies",
            style = NunchukTheme.typography.title
        )
    }
}

@PreviewLightDark
@Composable
fun PolicyHeaderPreview() {
    NunchukTheme {
        PolicyHeader()
    }
}