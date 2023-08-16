package com.nunchuk.android.main.membership.byzantine

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.colorResource
import com.nunchuk.android.main.R
import java.util.Calendar

@Composable
fun Long?.healthCheckTimeColor() : Color {
    if (this == null) return colorResource(id = R.color.nc_red_tint_color)
    val last6Month = Calendar.getInstance().apply {
        add(Calendar.MONTH, -6)
    }.timeInMillis
    val lastYear = Calendar.getInstance().apply {
        add(Calendar.YEAR, -1)
    }.timeInMillis

    return when {
        this >= last6Month -> colorResource(id = R.color.nc_green_color)
        this in lastYear until last6Month -> colorResource(id = R.color.nc_beeswax_tint)
        else -> colorResource(id = R.color.nc_red_tint_color)
    }
}