package com.nunchuk.android.compose.miniscript

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.nunchuk.android.compose.NcIcon
import com.nunchuk.android.compose.NunchukTheme
import com.nunchuk.android.core.R

@Composable
fun SigningStatusCard(
    modifier: Modifier = Modifier,
    coins: Int = 3,
    amount: Double = 0.00424422,
    policyStatus: PolicyStatus = PolicyStatus.INACTIVE,
    startDate: String? = null,
    endDate: String? = null,
) {
    val backgroundColor = when (policyStatus) {
        PolicyStatus.ACTIVE -> MaterialTheme.colorScheme.surface.copy(alpha = 0.1f)
        PolicyStatus.ACTIVE_WITH_DATE -> Color(0xFFFFF5E6)
        PolicyStatus.INACTIVE -> Color(0xFFF5F5F5)
    }

    Column(
        modifier = modifier
            .background(Color.White)
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .drawBehind {
                drawRoundRect(
                    color = backgroundColor,
                    cornerRadius = CornerRadius(8.dp.toPx())
                )
            }
            .padding(4.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            NcIcon(
                modifier = Modifier.size(16.dp),
                painter = painterResource(id = R.drawable.ic_btc),
                contentDescription = "Bitcoin icon"
            )
            Text(
                text = "Signing for $coins coins ($amount BTC)",
                style = NunchukTheme.typography.bodySmall
            )
        }

        if (policyStatus != PolicyStatus.INACTIVE) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                NcIcon(
                    modifier = Modifier.size(16.dp),
                    painter = painterResource(id = R.drawable.ic_timer),
                    contentDescription = "Timer icon"
                )
                Text(
                    text = when (policyStatus) {
                        PolicyStatus.ACTIVE -> "Active policy after $startDate"
                        PolicyStatus.ACTIVE_WITH_DATE -> "Active policy from $startDate until $endDate"
                        else -> ""
                    },
                    style = NunchukTheme.typography.bodySmall
                )
            }
        } else {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                NcIcon(
                    modifier = Modifier.size(16.dp),
                    painter = painterResource(id = R.drawable.ic_close),
                    contentDescription = "Close icon"
                )
                Text(
                    text = "Inactive policy",
                    style = NunchukTheme.typography.bodySmall
                )
            }
        }
    }
}

enum class PolicyStatus {
    ACTIVE,
    ACTIVE_WITH_DATE,
    INACTIVE
}

@Preview
@Composable
fun SigningStatusCardPreview() {
    NunchukTheme {
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            SigningStatusCard(policyStatus = PolicyStatus.ACTIVE, startDate = "05/29/2025")
            SigningStatusCard(
                policyStatus = PolicyStatus.ACTIVE_WITH_DATE,
                startDate = "05/29/2025",
                endDate = "06/07/2025"
            )
            SigningStatusCard(policyStatus = PolicyStatus.INACTIVE)
        }
    }
}