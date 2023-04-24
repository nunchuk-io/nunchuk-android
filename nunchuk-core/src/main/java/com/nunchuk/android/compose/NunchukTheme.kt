/**************************************************************************
 * This file is part of the Nunchuk software (https://nunchuk.io/)        *							          *
 * Copyright (C) 2022 Nunchuk								              *
 *                                                                        *
 * This program is free software; you can redistribute it and/or          *
 * modify it under the terms of the GNU General Public License            *
 * as published by the Free Software Foundation; either version 3         *
 * of the License, or (at your option) any later version.                 *
 *                                                                        *
 * This program is distributed in the hope that it will be useful,        *
 * but WITHOUT ANY WARRANTY; without even the implied warranty of         *
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the          *
 * GNU General Public License for more details.                           *
 *                                                                        *
 * You should have received a copy of the GNU General Public License      *
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.  *
 *                                                                        *
 **************************************************************************/

package com.nunchuk.android.compose

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Typography
import androidx.compose.material.lightColors
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.accompanist.systemuicontroller.rememberSystemUiController
import com.nunchuk.android.core.R

private val PrimaryColor = Color(0xff031F2B)
private val SecondaryColor = Color(0xff031F2B)
private val ErrorColor = Color(0xffCF4018)

val montserratMedium = FontFamily(Font(R.font.montserrat_medium))
val latoRegular = FontFamily(Font(R.font.lato_regular))
val latoBold = FontFamily(Font(R.font.lato_bold))
val latoSemiBold = FontFamily(Font(R.font.lato_semibold))

private val LightColors = lightColors(
    primary = PrimaryColor,
    onPrimary = Color.White,
    secondary = SecondaryColor,
    onSecondary = Color.White,
    error = ErrorColor,
    secondaryVariant = PrimaryColor
)

@Immutable
data class NunchukTypography(
    val heading: TextStyle,
    val title: TextStyle,
    val titleLarge: TextStyle,
    val titleSmall: TextStyle,
    val body: TextStyle,
    val bold: TextStyle,
    val caption: TextStyle,
    val bodySmall: TextStyle,
)

@Immutable
data class NunchukShapes(
    val medium: Shape
)

val LocalNunchukShapes = staticCompositionLocalOf {
    NunchukShapes(
        medium = RoundedCornerShape(12.dp)
    )
}


val LocalNunchukTypography = staticCompositionLocalOf {
    NunchukTypography(
        body = TextStyle.Default,
        title = TextStyle.Default,
        bold = TextStyle.Default,
        heading = TextStyle.Default,
        titleLarge = TextStyle.Default,
        titleSmall = TextStyle.Default,
        bodySmall = TextStyle.Default,
        caption = TextStyle.Default
    )
}

object NunchukTheme {
    val typography: NunchukTypography
        @Composable
        get() = LocalNunchukTypography.current

    val shape: NunchukShapes
        @Composable
        get() = LocalNunchukShapes.current
}


@Composable
fun NunchukTheme(
    content: @Composable () -> Unit,
) {
    val nunchukTypography = NunchukTypography(
        body = TextStyle(fontSize = 16.sp, fontFamily = latoRegular, color = PrimaryColor),
        title = TextStyle(
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = latoBold,
            color = PrimaryColor
        ),
        bold = TextStyle(fontWeight = FontWeight.Bold, fontFamily = latoBold, color = PrimaryColor),
        heading = TextStyle(fontSize = 24.sp, fontFamily = montserratMedium, color = PrimaryColor),
        titleLarge = TextStyle(
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = latoBold,
            color = PrimaryColor
        ),
        titleSmall = TextStyle(
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = latoBold,
            color = PrimaryColor
        ),
        bodySmall = TextStyle(fontSize = 12.sp, fontFamily = latoRegular, color = PrimaryColor),
        caption = TextStyle(
            fontSize = 12.sp,
            fontFamily = latoRegular,
            color = PrimaryColor,
            fontWeight = FontWeight.Medium
        ),
    )

    val nunchukShapes = NunchukShapes(
        medium = RoundedCornerShape(12.dp)
    )
    val systemUiController = rememberSystemUiController()
    systemUiController.setSystemBarsColor(color = Color.Transparent, darkIcons = true)
    CompositionLocalProvider(
        LocalNunchukTypography provides nunchukTypography,
        LocalNunchukShapes provides nunchukShapes
    ) {
        MaterialTheme(
            colors = LightColors,
            typography = Typography(defaultFontFamily = latoSemiBold),
            content = content,
        )
    }
}

@Composable
fun NunchukTheme(
    isSetStatusBar: Boolean,
    content: @Composable () -> Unit,
) {
    val nunchukTypography = NunchukTypography(
        body = TextStyle(fontSize = 16.sp, fontFamily = latoRegular, color = PrimaryColor),
        title = TextStyle(
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = latoBold,
            color = PrimaryColor
        ),
        bold = TextStyle(fontWeight = FontWeight.Bold, fontFamily = latoBold, color = PrimaryColor),
        heading = TextStyle(fontSize = 24.sp, fontFamily = montserratMedium, color = PrimaryColor),
        titleLarge = TextStyle(
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = latoBold,
            color = PrimaryColor
        ),
        titleSmall = TextStyle(
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = latoBold,
            color = PrimaryColor
        ),
        bodySmall = TextStyle(fontSize = 12.sp, fontFamily = latoRegular, color = PrimaryColor),
        caption = TextStyle(
            fontSize = 12.sp,
            fontFamily = latoRegular,
            color = PrimaryColor,
            fontWeight = FontWeight.Medium
        ),
    )
    if (isSetStatusBar) {
        val systemUiController = rememberSystemUiController()
        systemUiController.setSystemBarsColor(color = Color.Transparent, darkIcons = true)
    }
    CompositionLocalProvider(
        LocalNunchukTypography provides nunchukTypography,
    ) {
        MaterialTheme(
            colors = LightColors,
            typography = Typography(defaultFontFamily = latoSemiBold),
            content = content,
        )
    }
}
