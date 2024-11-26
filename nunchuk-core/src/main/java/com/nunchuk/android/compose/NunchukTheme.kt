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
import androidx.compose.material3.darkColorScheme
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

private val DarkColors = darkColorScheme(
    primary = Color.Black,
    onPrimary = Color.White,
    background = Color(0xFF1c1c1e),
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

val LocalNunchukThemeMode = staticCompositionLocalOf {
    true
}

object NunchukTheme {
    val typography: NunchukTypography
        @Composable
        get() = LocalNunchukTypography.current

    val shape: NunchukShapes
        @Composable
        get() = LocalNunchukShapes.current

    val isDark: Boolean
        @Composable
        get() = LocalNunchukThemeMode.current
}

@Composable
fun NunchukTheme(
    content: @Composable () -> Unit,
) {
//    val view = LocalView.current
//    if (view.isInEditMode) {
//        NunchukThemeContent(
//            isDark = isSystemInDarkTheme(),
//            content = content
//        )
//    } else {
//        val viewModel: ThemeViewModel = hiltViewModel()
//        val mode by viewModel.mode.collectAsStateWithLifecycle()
//        if (mode != null) {
//            val isDark = when (mode!!) {
//                ThemeMode.Light -> false
//                ThemeMode.Dark -> true
//                ThemeMode.System -> isSystemInDarkTheme()
//            }
//            NunchukThemeContent(
//                isDark = isDark,
//                content = content
//            )
//        }
//    }
    NunchukThemeContent(
        isDark = false,
        content = content
    )
}

@Composable
private fun NunchukThemeContent(
    isDark: Boolean,
    content: @Composable () -> Unit,
) {
    val colorScheme = if (isDark) DarkColors else LightColors
    val textColor = if (isDark) Color.White else PrimaryColor
    val nunchukTypography = NunchukTypography(
        body = TextStyle(fontSize = 16.sp, fontFamily = latoRegular, color = textColor),
        title = TextStyle(
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = latoBold,
            color = textColor
        ),
        bold = TextStyle(
            fontWeight = FontWeight.Bold,
            fontFamily = latoBold,
            color = textColor
        ),
        heading = TextStyle(
            fontSize = 24.sp,
            fontFamily = montserratMedium,
            color = textColor
        ),
        titleLarge = TextStyle(
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = latoBold,
            color = textColor
        ),
        titleSmall = TextStyle(
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = latoBold,
            color = textColor
        ),
        bodySmall = TextStyle(fontSize = 12.sp, fontFamily = latoRegular, color = textColor),
        caption = TextStyle(
            fontSize = 12.sp,
            fontFamily = latoBold,
            color = textColor,
            fontWeight = FontWeight.Medium
        ),
        captionTitle = TextStyle(
            fontSize = 12.sp,
            fontFamily = latoBold,
            color = textColor,
            fontWeight = FontWeight.Bold
        ),
        textLink = TextStyle(
            fontSize = 16.sp,
            fontFamily = latoBold,
            color = textColor,
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
    CompositionLocalProvider(
        LocalNunchukTypography provides nunchukTypography,
        LocalNunchukThemeMode provides isDark,
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = typography,
            content = content,
        )
    }
}