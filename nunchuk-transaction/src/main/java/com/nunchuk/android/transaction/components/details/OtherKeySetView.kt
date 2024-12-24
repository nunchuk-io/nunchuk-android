package com.nunchuk.android.transaction.components.details

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nunchuk.android.compose.NcIcon
import com.nunchuk.android.compose.NunchukTheme
import com.nunchuk.android.compose.backgroundLightGray
import com.nunchuk.android.compose.strokePrimary
import com.nunchuk.android.transaction.R

@Composable
fun OtherKeySetView(
    modifier: Modifier = Modifier,
    toggleExpand: () -> Unit,
    count: Int = 1,
    isExpanded: Boolean = false
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
    ) {
        HorizontalDivider(
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.strokePrimary,
            thickness = 1.dp
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            NcIcon(
                painter = painterResource(id = R.drawable.ic_mulitsig_dark),
                contentDescription = "Expand",
                modifier = Modifier.size(20.dp)
            )

            Text(
                modifier = Modifier.padding(start = 8.dp),
                text = "Other keysets",
                style = NunchukTheme.typography.titleSmall
            )

            Text(
                modifier = Modifier
                    .padding(start = 8.dp)
                    .background(
                        color = MaterialTheme.colorScheme.backgroundLightGray,
                        shape = RoundedCornerShape(20.dp)
                    )
                    .border(
                        width = 1.dp,
                        color = MaterialTheme.colorScheme.strokePrimary,
                        shape = RoundedCornerShape(20.dp)
                    )
                    .padding(horizontal = 8.dp),
                text = count.toString(),
                style = NunchukTheme.typography.captionTitle.copy(
                    fontSize = 10.sp,
                    fontWeight = FontWeight.W900
                )
            )

            Spacer(modifier = Modifier.weight(1f))
            Text(
                modifier = Modifier
                    .clickable(onClick = toggleExpand)
                    .padding(start = 8.dp),
                text = "View all",
                style = NunchukTheme.typography.titleSmall.copy(
                    textDecoration = TextDecoration.Underline
                )
            )

            NcIcon(
                painter = painterResource(id = if (isExpanded) R.drawable.ic_caret_up else R.drawable.ic_caret_down),
                contentDescription = "Expand",
                modifier = Modifier
                    .clickable(onClick = toggleExpand)
                    .padding(start = 4.dp)
                    .size(20.dp)
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun OtherKeySetViewPreview() {
    NunchukTheme {
        OtherKeySetView(toggleExpand = {})
    }
}