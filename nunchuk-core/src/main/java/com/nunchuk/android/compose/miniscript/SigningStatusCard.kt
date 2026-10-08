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
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
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
                contentDescription = stringResource(R.string.nc_cd_bitcoin_icon)
            )
            Text(
                text = stringResource(
                    R.string.nc_signing_for_coins,
                    pluralStringResource(R.plurals.nc_coins_with_count, coins, coins),
                    "$amount ${stringResource(R.string.nc_currency_btc)}"
                ),
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
                    contentDescription = stringResource(R.string.nc_cd_timer_icon)
                )
                Text(
                    text = when (policyStatus) {
                        PolicyStatus.ACTIVE -> stringResource(R.string.nc_miniscript_active_policy_after, startDate.orEmpty())
                        PolicyStatus.ACTIVE_WITH_DATE -> stringResource(
                            R.string.nc_miniscript_active_policy_from_until,
                            startDate.orEmpty(),
                            endDate.orEmpty()
                        )
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
                    contentDescription = stringResource(R.string.nc_cd_close_icon)
                )
                Text(
                    text = stringResource(R.string.nc_miniscript_inactive_policy),
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