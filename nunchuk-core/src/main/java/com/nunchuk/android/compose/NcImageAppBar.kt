package com.nunchuk.android.compose

import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.Text
import androidx.compose.material.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalViewConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.nunchuk.android.core.R

@Composable
fun NcImageAppBar(
    modifier: Modifier = Modifier,
    @DrawableRes backgroundRes: Int,
    onClosedClicked: (() -> Unit)? = null,
    title: String? = null,
    actions: @Composable RowScope.() -> Unit = {
        Spacer(
            modifier = Modifier.size(
                LocalViewConfiguration.current.minimumTouchTargetSize,
            )
        )
    },
) {
    val onBackPressOwner = LocalOnBackPressedDispatcherOwner.current
    val defaultClosedClick = {
        onBackPressOwner?.onBackPressedDispatcher?.onBackPressed()
        Unit
    }
    val onClick = onClosedClicked ?: defaultClosedClick
    Column(modifier = modifier) {
        Box {
            Image(
                modifier = Modifier.fillMaxWidth(),
                painter = painterResource(id = backgroundRes),
                contentScale = ContentScale.Crop,
                contentDescription = "Background"
            )
            TopAppBar(
                modifier = Modifier.statusBarsPadding(),
                backgroundColor = Color.Transparent,
                elevation = 0.dp,
            ) {
                IconButton(onClick = onClick) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_back),
                        contentDescription = "Back Icon"
                    )
                }
                if (title != null) {
                    Text(
                        modifier = Modifier.weight(1.0f),
                        textAlign = TextAlign.Center,
                        text = title,
                        overflow = TextOverflow.Ellipsis,
                        maxLines = 1,
                        style = NunchukTheme.typography.titleSmall
                    )
                } else {
                    Spacer(modifier = Modifier.weight(1.0f))
                }
                actions()
            }
        }
    }
}

@Preview
@Composable
fun NcImageAppBarPreview() {
    NcImageAppBar(
        backgroundRes = R.drawable.nc_bg_tap_signer_chip,
        title = "Est. time remaining: xx minutes",
    )
}