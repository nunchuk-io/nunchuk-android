package com.nunchuk.android.main.components.tabs.wallet.component

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.ClickableText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import com.nunchuk.android.compose.NunchukTheme
import com.nunchuk.android.main.R
import com.nunchuk.android.wallet.R as WalletR
import com.nunchuk.android.widget.R as WidgetR

@Composable
internal fun ClaimWalletBanner(
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {},
) {
    val message = stringResource(R.string.nc_inheritance_unlocked_warning)
    val actionText = stringResource(R.string.nc_do_it_now)
    
    val annotatedString = buildAnnotatedString {
        append(message)
        append(" ")
        withStyle(
            style = SpanStyle(
                textDecoration = TextDecoration.Underline
            )
        ) {
            append(actionText)
        }
    }
    
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(
                color = colorResource(id = WidgetR.color.nc_beeswax_tint),
                shape = RoundedCornerShape(12.dp)
            )
            .clickable(onClick = onClick)
            .padding(12.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Image(
            painter = painterResource(id = WalletR.drawable.ic_claim_warning),
            contentDescription = null,
            modifier = Modifier.padding(end = 4.dp)
        )
        ClickableText(
            modifier = Modifier.weight(1f),
            text = annotatedString,
            style = NunchukTheme.typography.titleSmall.copy(
                color = colorResource(id = WidgetR.color.nc_grey_g7)
            ),
            onClick = { offset ->
                if (offset >= message.length + 1) {
                    onClick()
                }
            }
        )
    }
}

@PreviewLightDark
@Composable
private fun ClaimWalletBannerPreview() {
    NunchukTheme {
        ClaimWalletBanner(
            modifier = Modifier.padding(16.dp),
            onClick = {}
        )
    }
}
