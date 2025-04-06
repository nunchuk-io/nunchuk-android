package com.nunchuk.android.main.components.tabs.wallet.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidViewBinding
import com.bumptech.glide.Glide
import com.nunchuk.android.core.util.formatMMMddyyyyDate
import com.nunchuk.android.main.R
import com.nunchuk.android.main.databinding.ContainerNonSubscriberBinding
import com.nunchuk.android.model.banner.Banner

@Composable
internal fun NonSubscriberBanner(
    modifier: Modifier = Modifier,
    banner: Banner,
    onClick: (Banner) -> Unit = {},
) {
    AndroidViewBinding(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = { onClick(banner) }),
        factory = ContainerNonSubscriberBinding::inflate
    ) {
        Glide.with(ivNonSubscriber)
            .load(banner.content.imageUrl)
            .override(ivNonSubscriber.width)
            .into(ivNonSubscriber)
        tvNonSubscriber.text = banner.content.title
        tvNonSubscriberExpired.text = String.format(
            root.context.getString(R.string.nc_banner_expired_time),
            banner.payload.expiryAtMillis.formatMMMddyyyyDate
        )
    }
}