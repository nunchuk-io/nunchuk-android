package com.nunchuk.android.main.groupwallet.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import com.nunchuk.android.compose.NcCircleImage
import com.nunchuk.android.compose.NcIcon
import com.nunchuk.android.compose.NunchukTheme
import com.nunchuk.android.compose.primaryT1
import com.nunchuk.android.compose.strokePrimary
import com.nunchuk.android.main.R
import com.nunchuk.android.main.groupwallet.avatarColors

@Composable
fun UserOnline(numberOfOnlineUsers: Int) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        NcIcon(
            modifier = Modifier.size(24.dp),
            painter = painterResource(id = R.drawable.ic_mulitsig_dark),
            contentDescription = "Key icon",
        )

        Text(
            text = stringResource(id = R.string.nc_title_signers),
            style = NunchukTheme.typography.title,
            modifier = Modifier.padding(start = 8.dp)
        )

        Spacer(modifier = Modifier.weight(1.0f))

        Box {
            repeat(numberOfOnlineUsers.coerceAtMost(2)) { index ->
                NcCircleImage(
                    modifier = Modifier
                        .padding(start = (24 * index).dp)
                        .border(
                            width = 1.dp,
                            color = MaterialTheme.colorScheme.strokePrimary,
                            shape = CircleShape
                        ),
                    iconSize = 16.dp,
                    size = 28.dp,
                    iconTintColor = Color.White,
                    resId = R.drawable.ic_user,
                    color = avatarColors[index % avatarColors.size]
                )
            }
            if (numberOfOnlineUsers > 2) {
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

@PreviewLightDark
@Composable
private fun UserOnlinePreview() {
    NunchukTheme {
        UserOnline(numberOfOnlineUsers = 1)
    }
}

@PreviewLightDark
@Composable
private fun UserOnline2Preview() {
    NunchukTheme {
        UserOnline(numberOfOnlineUsers = 2)
    }
}

@PreviewLightDark
@Composable
private fun UserOnline3Preview() {
    NunchukTheme {
        UserOnline(numberOfOnlineUsers = 3)
    }
}