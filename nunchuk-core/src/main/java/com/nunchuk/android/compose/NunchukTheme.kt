/**************************************************************************
 * This file is part of the Nunchuk software (https://nunchuk.io/)        *
 * Copyright (C) 2022, 2023 Nunchuk                                       *
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
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
import androidx.compose.ui.text.style.TextDecoration
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

private val LightColors = lightColorScheme(
    primary = PrimaryColor,
    onPrimary = Color.White,
    secondary = SecondaryColor,
    onSecondary = Color.White,
    error = ErrorColor,
    background = Color.White,
    surfaceVariant = Color.White,
    surface = Color.White,
)

private val defaultTypography = Typography()
private val typography = Typography(
    displayLarge = defaultTypography.displayLarge.copy(fontFamily = latoRegular),
    displayMedium = defaultTypography.displayMedium.copy(fontFamily = latoRegular),
    displaySmall = defaultTypography.displaySmall.copy(fontFamily = latoRegular),

    headlineLarge = defaultTypography.headlineLarge.copy(fontFamily = latoRegular),
    headlineMedium = defaultTypography.headlineMedium.copy(fontFamily = latoRegular),
    headlineSmall = defaultTypography.headlineSmall.copy(fontFamily = latoRegular),

    titleLarge = defaultTypography.titleLarge.copy(fontFamily = latoRegular),
    titleMedium = defaultTypography.titleMedium.copy(fontFamily = latoRegular),
    titleSmall = defaultTypography.titleSmall.copy(fontFamily = latoRegular),

    bodyLarge = defaultTypography.bodyLarge.copy(fontFamily = latoRegular),
    bodyMedium = defaultTypography.bodyMedium.copy(fontFamily = latoRegular),
    bodySmall = defaultTypography.bodySmall.copy(fontFamily = latoRegular),

    labelLarge = defaultTypography.labelLarge.copy(fontFamily = latoBold),
    labelMedium = defaultTypography.labelMedium.copy(fontFamily = latoRegular),
    labelSmall = defaultTypography.labelSmall.copy(fontFamily = latoRegular)
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
    val captionTitle: TextStyle,
    val bodySmall: TextStyle,
    val textLink: TextStyle,
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
        caption = TextStyle.Default,
        captionTitle = TextStyle.Default,
        textLink = TextStyle.Default
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
    statusBarColor: Color = Color.Transparent,
    darkIcon: Boolean = true,
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
        captionTitle = TextStyle(
            fontSize = 12.sp,
            fontFamily = latoBold,
            color = PrimaryColor,
            fontWeight = FontWeight.Bold
        ),
        textLink = TextStyle(
            fontSize = 16.sp,
            fontFamily = latoBold,
            color = PrimaryColor,
            fontWeight = FontWeight.Bold,
            textDecoration = TextDecoration.Underline
        ),
    )

    val nunchukShapes = NunchukShapes(
        medium = RoundedCornerShape(12.dp)
    )
    val systemUiController = rememberSystemUiController()
    systemUiController.setSystemBarsColor(color = statusBarColor, darkIcons = darkIcon)
    CompositionLocalProvider(
        LocalNunchukTypography provides nunchukTypography,
        LocalNunchukShapes provides nunchukShapes
    ) {
        MaterialTheme(
            colorScheme = LightColors,
            typography = typography,
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
        captionTitle = TextStyle(
            fontSize = 12.sp,
            fontFamily = latoBold,
            color = PrimaryColor,
            fontWeight = FontWeight.Bold
        ),
        textLink = TextStyle(
            fontSize = 16.sp,
            fontFamily = latoBold,
            color = PrimaryColor,
            fontWeight = FontWeight.Bold,
            textDecoration = TextDecoration.Underline
        ),
    )
    val defaultTypography = Typography()
    val typography = Typography(
        displayLarge = defaultTypography.displayLarge.copy(fontFamily = latoRegular),
        displayMedium = defaultTypography.displayMedium.copy(fontFamily = latoRegular),
        displaySmall = defaultTypography.displaySmall.copy(fontFamily = latoRegular),

        headlineLarge = defaultTypography.headlineLarge.copy(fontFamily = latoRegular),
        headlineMedium = defaultTypography.headlineMedium.copy(fontFamily = latoRegular),
        headlineSmall = defaultTypography.headlineSmall.copy(fontFamily = latoRegular),

        titleLarge = defaultTypography.titleLarge.copy(fontFamily = latoRegular),
        titleMedium = defaultTypography.titleMedium.copy(fontFamily = latoRegular),
        titleSmall = defaultTypography.titleSmall.copy(fontFamily = latoRegular),

        bodyLarge = defaultTypography.bodyLarge.copy(fontFamily = latoRegular),
        bodyMedium = defaultTypography.bodyMedium.copy(fontFamily = latoRegular),
        bodySmall = defaultTypography.bodySmall.copy(fontFamily = latoRegular),

        labelLarge = defaultTypography.labelLarge.copy(fontFamily = latoRegular),
        labelMedium = defaultTypography.labelMedium.copy(fontFamily = latoRegular),
        labelSmall = defaultTypography.labelSmall.copy(fontFamily = latoRegular)
    )
    if (isSetStatusBar) {
        val systemUiController = rememberSystemUiController()
        systemUiController.setSystemBarsColor(color = Color.Transparent, darkIcons = true)
    }
    CompositionLocalProvider(
        LocalNunchukTypography provides nunchukTypography,
    ) {
        MaterialTheme(
            colorScheme = LightColors,
            typography = typography,
            content = content,
        )
    }
}
