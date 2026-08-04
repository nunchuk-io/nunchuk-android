package com.nunchuk.android.main.components.tabs.wallet.component

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nunchuk.android.compose.NunchukTheme
import com.nunchuk.android.core.util.formatMMMddyyyyDate
import com.nunchuk.android.main.R
import com.nunchuk.android.model.banner.Banner
import com.skydoves.landscapist.ImageOptions
import com.skydoves.landscapist.glide.GlideImage
import com.nunchuk.android.widget.R as WidgetR

@Composable
internal fun Banner(
    modifier: Modifier = Modifier,
    banner: Banner,
    onClick: (Banner) -> Unit = {},
) {
    val fallbackIcon: @Composable () -> Unit = {
        Image(
            painter = painterResource(id = WidgetR.drawable.ic_banner_one),
            contentDescription = null,
        )
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(color = colorResource(id = WidgetR.color.nc_fill_beewax))
            .clickable(onClick = { onClick(banner) })
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        GlideImage(
            modifier = Modifier.size(48.dp),
            imageModel = { banner.content.imageUrl },
            imageOptions = ImageOptions(contentScale = ContentScale.Fit),
            loading = { fallbackIcon() },
            failure = { fallbackIcon() },
        )

        Column(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 12.dp),
        ) {
            Text(
                text = banner.content.description,
                style = NunchukTheme.typography.titleSmall,
            )
            if (banner.payload.expiryAtMillis > 0) {
                Text(
                    modifier = Modifier
                        .padding(top = 4.dp)
                        .background(
                            color = colorResource(id = WidgetR.color.nc_background),
                            shape = RoundedCornerShape(20.dp),
                        )
                        .padding(horizontal = 8.dp, vertical = 1.dp),
                    text = stringResource(
                        R.string.nc_banner_expired_time,
                        banner.payload.expiryAtMillis.formatMMMddyyyyDate,
                    ),
                    style = NunchukTheme.typography.bodySmall.copy(fontSize = 10.sp),
                )
            }
        }

        Image(
            painter = painterResource(id = WidgetR.drawable.ic_arrow),
            contentDescription = null,
        )
    }
}

@PreviewLightDark
@Composable
private fun BannerPreview() {
    NunchukTheme {
        Banner(
            banner = Banner(
                id = "id",
                type = Banner.Type.TYPE_01,
                content = Banner.Content(
                    title = "Coldcard Security Alert",
                    description = "Your assisted wallet uses a Coldcard key. Please migrate to a new assisted wallet as soon as possible.",
                    imageUrl = "",
                    action = Banner.Action(label = "", type = "", target = ""),
                ),
                payload = Banner.Payload(expiryAtMillis = 1722297600000L),
            )
        )
    }
}