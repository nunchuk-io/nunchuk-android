package com.nunchuk.android.main.membership.byzantine.payment.list

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import com.nunchuk.android.compose.NunchukTheme
import com.nunchuk.android.compose.greyDark
import com.nunchuk.android.main.R
import com.nunchuk.android.main.membership.byzantine.formatAmount
import com.nunchuk.android.main.membership.byzantine.payment.RecurringPaymentProvider
import com.nunchuk.android.main.membership.byzantine.payment.toResId
import com.nunchuk.android.model.payment.RecurringPayment
import com.nunchuk.android.model.payment.RecurringPaymentType
import com.nunchuk.android.utils.simpleGlobalDateFormat
import java.util.Date

@Composable
fun RecurringPaymentItemView(
    recurringPayment: RecurringPayment,
    modifier: Modifier = Modifier,
    onClick : (String) -> Unit = {},
) {
    Row(
        modifier = modifier
            .clickable { onClick(recurringPayment.id.orEmpty()) }
            .padding(12.dp)
            .fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = recurringPayment.name,
                style = NunchukTheme.typography.body,
                color = MaterialTheme.colorScheme.greyDark,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )

            if (recurringPayment.paymentType == RecurringPaymentType.PERCENTAGE) {
                Text(
                    text = "${recurringPayment.formatAmount}% / ${
                        stringResource(
                            recurringPayment.frequency.toResId()
                        ).removePrefix("Every ")
                    }",
                    style = NunchukTheme.typography.title,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            } else {
                Text(
                    text = "${recurringPayment.currency} ${recurringPayment.formatAmount} / ${
                        stringResource(
                            recurringPayment.frequency.toResId()
                        ).removePrefix("Every ")
                    }",
                    style = NunchukTheme.typography.title,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            Text(
                text = "Start date: ${Date(recurringPayment.startDate).simpleGlobalDateFormat()}",
                style = NunchukTheme.typography.caption,
                color = MaterialTheme.colorScheme.greyDark,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }

        Image(
            modifier = Modifier.size(24.dp),
            painter = painterResource(id = R.drawable.ic_right_arrow_dark),
            contentDescription = "Arrow"
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun RecurringPaymentItemViewPreview(
    @PreviewParameter(RecurringPaymentProvider::class) recurringPayment: RecurringPayment,
) {
    NunchukTheme {
        RecurringPaymentItemView(recurringPayment = recurringPayment)
    }
}