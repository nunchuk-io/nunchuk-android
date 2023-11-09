package com.nunchuk.android.compose

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarVisuals
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.nunchuk.android.core.R

@Composable
fun NcSnackBarHost(state: SnackbarHostState) {
    SnackbarHost(hostState = state) { data ->
        val customVisuals = data.visuals as NcSnackbarVisuals
        NcToastMessage(type = customVisuals.type, customVisuals.message)
    }
}

data class NcSnackbarVisuals(
    override val message: String,
    override val actionLabel: String? = null,
    override val withDismissAction: Boolean = false,
    override val duration: SnackbarDuration = if (actionLabel == null) SnackbarDuration.Short else SnackbarDuration.Indefinite,
    val type: NcToastType = NcToastType.SUCCESS,
) : SnackbarVisuals

@Composable
fun NcToastMessage(type: NcToastType = NcToastType.SUCCESS, message: String) {
    val color = when (type) {
        NcToastType.SUCCESS -> colorResource(id = R.color.nc_green_color)
        NcToastType.ERROR -> colorResource(id = R.color.nc_red_color)
        NcToastType.WARNING -> colorResource(id = R.color.nc_green_color)
    }
    val iconResId = when (type) {
        NcToastType.SUCCESS -> R.drawable.ic_info
        NcToastType.ERROR -> R.drawable.ic_info_white
        NcToastType.WARNING -> R.drawable.ic_warn
    }
    val textColor = when (type) {
        NcToastType.SUCCESS -> colorResource(id = R.color.nc_black_color)
        NcToastType.ERROR -> colorResource(id = R.color.nc_white_color)
        NcToastType.WARNING -> colorResource(id = R.color.nc_black_color)
    }
    Row(
        modifier = Modifier
            .padding(horizontal = 16.dp)
            .background(color = color, shape = RoundedCornerShape(8.dp))
            .padding(12.dp)
            .fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Image(
            modifier = Modifier.size(36.dp),
            painter = painterResource(id = iconResId),
            contentDescription = "Icon",
        )
        Text(
            text = message,
            color = textColor,
            modifier = Modifier.padding(start = 8.dp),
            style = NunchukTheme.typography.titleSmall,
        )
    }
}

enum class NcToastType {
    SUCCESS,
    ERROR,
    WARNING,
}