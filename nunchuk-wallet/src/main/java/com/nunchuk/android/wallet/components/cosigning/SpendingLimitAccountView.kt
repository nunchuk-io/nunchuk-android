package com.nunchuk.android.wallet.components.cosigning

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.text.capitalize
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.intl.Locale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.nunchuk.android.compose.NcTag
import com.nunchuk.android.compose.NunchukTheme
import com.nunchuk.android.compose.greyLight
import com.nunchuk.android.core.util.USD_FRACTION_DIGITS
import com.nunchuk.android.core.util.formatDecimalWithoutZero
import com.nunchuk.android.model.SpendingPolicy
import com.nunchuk.android.model.SpendingTimeUnit
import com.nunchuk.android.model.byzantine.AssistedMember
import com.nunchuk.android.model.byzantine.AssistedWalletRole
import com.nunchuk.android.model.byzantine.toTitle
import com.nunchuk.android.wallet.R

@Composable
fun SpendingLimitAccountView(
    modifier: Modifier = Modifier,
    member: AssistedMember,
    policy: SpendingPolicy,
) {
    val name = member.name ?: member.email
    val role = member.role
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.greyLight, shape = RoundedCornerShape(12.dp))
            .padding(16.dp), verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .background(
                    color = colorResource(id = R.color.nc_beeswax_light),
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = name.take(2),
                style = NunchukTheme.typography.title
            )
        }
        Column(modifier = Modifier.padding(start = 12.dp).weight(1f)) {
            Text(text = name, style = NunchukTheme.typography.body, maxLines = 1, overflow = TextOverflow.Ellipsis)
            NcTag(
                modifier = Modifier.padding(top = 4.dp),
                label = role.toTitle()
            )
            Text(
                modifier = Modifier.padding(top = 12.dp),
                text = "${policy.limit.formatDecimalWithoutZero(maxFractionDigits = USD_FRACTION_DIGITS)} ${policy.currencyUnit}/${
                    policy.timeUnit.name.lowercase()
                        .capitalize(Locale.current)
                }",
                style = NunchukTheme.typography.title.copy(fontWeight = FontWeight.Bold)
            )
        }
    }
}

@Preview
@Composable
fun SpendingLimitAccountPreview() {
    NunchukTheme {
        SpendingLimitAccountView(
            member = AssistedMember(
                AssistedWalletRole.MASTER.name,
                name = "Bob Lee",
                email = "khoapham@gmail.com"
            ),
            policy = SpendingPolicy(100.0, SpendingTimeUnit.DAILY, currencyUnit = "USD")
        )
    }
}