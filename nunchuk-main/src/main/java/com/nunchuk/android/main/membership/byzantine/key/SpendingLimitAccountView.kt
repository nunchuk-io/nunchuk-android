package com.nunchuk.android.main.membership.byzantine.key

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.nunchuk.android.compose.NcTag
import com.nunchuk.android.compose.NcTextField
import com.nunchuk.android.compose.NumberCommaTransformation
import com.nunchuk.android.compose.NunchukTheme
import com.nunchuk.android.compose.greyLight
import com.nunchuk.android.core.util.CurrencyFormatter
import com.nunchuk.android.main.R
import com.nunchuk.android.model.SpendingTimeUnit
import com.nunchuk.android.model.byzantine.AssistedMember
import com.nunchuk.android.model.byzantine.AssistedMemberSpendingPolicy
import com.nunchuk.android.model.byzantine.AssistedWalletRole
import com.nunchuk.android.model.byzantine.InputSpendingPolicy
import com.nunchuk.android.model.byzantine.toTitle

@Composable
fun SpendingLimitAccountView(
    modifier: Modifier = Modifier,
    index: Int = 0,
    member: AssistedMemberSpendingPolicy,
    onShowCurrencyUnitOption: (email: String?) -> Unit = {},
    onShowTimeUnitOption: (email: String?) -> Unit = {},
    onLimitChange: (email: String?, limit: String) -> Unit = { _, _ -> },
) {
    val name = member.member?.name ?: return
    val email = member.member?.email ?: return
    val role = member.member?.role ?: return
    Column(modifier = modifier) {
        if (index == 0) {
            Text(text = stringResource(R.string.nc_master))
        } else {
            Text(text = "${stringResource(R.string.nc_member)} ${index.inc()}")
        }
        Column(
            modifier = Modifier
                .padding(top = 12.dp)
                .fillMaxWidth()
                .background(MaterialTheme.colors.greyLight, shape = RoundedCornerShape(12.dp))
                .padding(16.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
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
                Column(modifier = Modifier.padding(start = 12.dp)) {
                    Text(text = name, style = NunchukTheme.typography.body)
                    NcTag(
                        modifier = Modifier.padding(top = 4.dp),
                        label = role.toTitle
                    )
                    Text(
                        modifier = Modifier.padding(top = 4.dp),
                        text = email,
                        style = NunchukTheme.typography.bodySmall
                    )
                }
            }
            Text(
                modifier = Modifier.padding(top = 16.dp),
                text = stringResource(R.string.nc_cosigning_spending_limit),
                style = NunchukTheme.typography.titleSmall
            )
            Row(
                modifier = Modifier
                    .padding(top = 4.dp)
                    .height(IntrinsicSize.Max)
            ) {
                NcTextField(
                    modifier = Modifier
                        .weight(1.0f)
                        .padding(end = 16.dp),
                    title = "",
                    value = member.spendingPolicy.limit,
                    visualTransformation = NumberCommaTransformation(),
                    onValueChange = {
                        onLimitChange(
                            member.member?.email,
                            CurrencyFormatter.format(it, 2).take(15)
                        )
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
                Row(
                    modifier = Modifier
                        .fillMaxHeight()
                        .background(
                            color = MaterialTheme.colors.background,
                            shape = RoundedCornerShape(8.dp)
                        )
                        .border(
                            width = 1.dp,
                            color = Color(0xFFDEDEDE),
                            shape = RoundedCornerShape(8.dp),
                        )
                        .clickable { onShowCurrencyUnitOption(member.member?.email) }
                        .padding(horizontal = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        modifier = Modifier.padding(end = 16.dp),
                        text = member.spendingPolicy.currencyUnit,
                    )
                    Icon(
                        painter = painterResource(id = R.drawable.ic_arrow),
                        contentDescription = ""
                    )
                }
            }
            Row(
                modifier = Modifier
                    .padding(top = 16.dp)
                    .height(52.dp)
                    .clickable { onShowTimeUnitOption(member.member?.email) }
                    .fillMaxWidth()
                    .background(
                        color = MaterialTheme.colors.background,
                        shape = RoundedCornerShape(8.dp)
                    )
                    .border(
                        width = 1.dp,
                        color = Color(0xFFDEDEDE),
                        shape = RoundedCornerShape(8.dp),
                    )
                    .padding(horizontal = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    modifier = Modifier.padding(end = 16.dp),
                    text = member.spendingPolicy.timeUnit.toLabel(LocalContext.current),
                )
                Icon(
                    painter = painterResource(id = R.drawable.ic_arrow),
                    contentDescription = ""
                )
            }
        }
    }
}

@Preview
@Composable
fun SpendingLimitAccountPreview() {
    NunchukTheme {
        SpendingLimitAccountView(
            member = AssistedMemberSpendingPolicy(
                member = AssistedMember(
                    role = AssistedWalletRole.MASTER.name,
                    name = "Bob Lee",
                    email = "boblee@gmail.com"
                ),
                spendingPolicy = InputSpendingPolicy("5000", SpendingTimeUnit.DAILY, "USD")
            )
        )
    }
}