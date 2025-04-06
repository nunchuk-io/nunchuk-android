package com.nunchuk.android.main.components.tabs.wallet.component

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.nunchuk.android.compose.NunchukTheme
import com.nunchuk.android.main.R
import com.nunchuk.android.model.campaigns.Campaign
import com.nunchuk.android.model.campaigns.CampaignStatus
import com.nunchuk.android.model.campaigns.CampaignType
import com.nunchuk.android.model.campaigns.ReferrerCode

@Composable
internal fun WalletsTopBar(
    campaign: Campaign?,
    isHasWallet: Boolean = false,
    localReferrerCode: ReferrerCode?,
    onNfcClick: () -> Unit = {},
    onCampaignClick: () -> Unit = {}
) {
    val isCampaignVisible = campaign?.isValid() == true && campaign.isDismissed.not()
            && (campaign.type == CampaignType.DOWNLOAD || isHasWallet) && localReferrerCode?.status != CampaignStatus.COMPLETED
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(colorResource(R.color.nc_primary_color))
            .statusBarsPadding()
            .height(64.dp)
    ) {
        IconButton(
            modifier = Modifier
                .align(Alignment.CenterStart),
            onClick = onNfcClick
        ) {
            Icon(
                painter = painterResource(id = R.drawable.ic_nfc),
                contentDescription = "Nfc Icon",
                tint = Color.White
            )
        }

        Text(
            text = stringResource(R.string.nc_title_home),
            style = NunchukTheme.typography.titleLarge,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .align(Alignment.Center),
            color = Color.White,
            maxLines = 1,
        )

        if (isCampaignVisible) {
            Row(
                modifier = Modifier
                    .padding(end = 16.dp)
                    .background(
                        brush = Brush.linearGradient(
                            colors = listOf(Color(0xFFFDD95C), Color(0xFFF8AD30)),
                            start = Offset.Zero,
                            end = Offset.Infinite
                        ),
                        shape = RoundedCornerShape(20.dp)
                    )
                    .clickable(onClick = onCampaignClick)
                    .padding(horizontal = 8.dp, vertical = 4.dp)
                    .align(Alignment.CenterEnd),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Image(
                    painter = painterResource(R.drawable.ic_gift),
                    contentDescription = "Gift Icon"
                )

                Text(
                    text = campaign?.cta.orEmpty(),
                    style = NunchukTheme.typography.titleSmall,
                    color = colorResource(R.color.cl_031F2B),
                    modifier = Modifier
                        .padding(start = 4.dp)
                )
            }
        }
    }
}