package com.nunchuk.android.compose.miniscript

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import com.nunchuk.android.compose.NcCircleImage
import com.nunchuk.android.compose.NcIcon
import com.nunchuk.android.compose.NunchukTheme
import com.nunchuk.android.compose.primaryT1
import com.nunchuk.android.core.R
import com.nunchuk.android.share.groupwallet.avatarColors

@Composable
fun PolicyHeader(
    modifier: Modifier = Modifier,
    numberOfOnlineUsers: Int = 1,
    showUserAvatars: Boolean = false
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        NcIcon(
            modifier = Modifier.size(24.dp),
            painter = painterResource(id = R.drawable.ic_policies),
            contentDescription = "Policies",
        )
        Text(
            text = "Policies",
            style = NunchukTheme.typography.title
        )

        Spacer(modifier = Modifier.weight(1.0f))

        if (showUserAvatars) {
            Box {
                repeat(numberOfOnlineUsers.coerceAtMost(3)) { index: Int ->
                    NcCircleImage(
                        modifier = Modifier
                            .padding(start = (24 * index).dp)
                            .border(
                                width = 1.dp,
                                color = colorResource(id = R.color.nc_stroke_primary),
                                shape = CircleShape
                            ),
                        iconSize = 16.dp,
                        size = 28.dp,
                        iconTintColor = androidx.compose.ui.graphics.Color.White,
                        resId = R.drawable.ic_user,
                        color = avatarColors[index % avatarColors.size]
                    )
                }
                if (numberOfOnlineUsers > 3) {
                    Box(
                        Modifier
                            .padding(start = 48.dp)
                            .size(28.dp)
                            .background(
                                color = colorResource(id = R.color.nc_stroke_primary),
                                shape = CircleShape
                            )
                            .padding(1.dp)
                            .background(
                                color = MaterialTheme.colorScheme.primaryT1,
                                shape = CircleShape
                            ),
                    ) {
                        Text(
                            text = "${numberOfOnlineUsers - 2}",
                            style = NunchukTheme.typography.captionTitle,
                            color = colorResource(id = R.color.nc_white_color),
                            modifier = Modifier.align(Alignment.Center)
                        )
                    }
                }
            }
        }
    }
}

@PreviewLightDark
@Composable
fun PolicyHeaderPreview() {
    NunchukTheme {
        PolicyHeader(numberOfOnlineUsers = 1)
    }
}

@PreviewLightDark
@Composable
fun PolicyHeader2Preview() {
    NunchukTheme {
        PolicyHeader(numberOfOnlineUsers = 2)
    }
}

@PreviewLightDark
@Composable
fun PolicyHeader3Preview() {
    NunchukTheme {
        PolicyHeader(numberOfOnlineUsers = 3)
    }
}

@PreviewLightDark
@Composable
fun PolicyHeaderNoAvatarsPreview() {
    NunchukTheme {
        PolicyHeader(numberOfOnlineUsers = 3, showUserAvatars = false)
    }
}