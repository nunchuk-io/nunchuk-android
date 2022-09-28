package com.nunchuk.android.compose

import androidx.compose.material.MaterialTheme
import androidx.compose.material.lightColors
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.nunchuk.android.core.R

private val PrimaryColor = Color(0xff031F2B)
private val SecondaryColor = Color(0xffC1C1C1)

val montserratMedium = FontFamily(Font(R.font.montserrat_medium))
val latoRegular = FontFamily(Font(R.font.lato_regular))
val latoSemiBold = FontFamily(Font(R.font.lato_semibold))

private val LightColors = lightColors(
    primary = PrimaryColor,
    onPrimary = Color.White,
    secondary = SecondaryColor,
)

@Immutable
data class NunchukTypography(
    val heading: TextStyle,
    val title: TextStyle,
    val titleLarge: TextStyle,
    val titleSmall: TextStyle,
    val body: TextStyle,
    val bold: TextStyle,
    val bodySmall: TextStyle,
)

val LocalNunchukTypography = staticCompositionLocalOf {
    NunchukTypography(
        body = TextStyle.Default,
        title = TextStyle.Default,
        bold = TextStyle.Default,
        heading = TextStyle.Default,
        titleLarge = TextStyle.Default,
        titleSmall = TextStyle.Default,
        bodySmall = TextStyle.Default,
    )
}

object NunchukTheme {
    val typography: NunchukTypography
        @Composable
        get() = LocalNunchukTypography.current
}


@Composable
fun NunchukTheme(
    content: @Composable () -> Unit
) {
    val nunchukTypography = NunchukTypography(
        body = TextStyle(fontSize = 16.sp, fontFamily = latoRegular, color = PrimaryColor),
        title = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.SemiBold, fontFamily = latoSemiBold, color = PrimaryColor),
        bold = TextStyle(fontWeight = FontWeight.SemiBold, fontFamily = latoSemiBold, color = PrimaryColor),
        heading = TextStyle(fontSize = 24.sp, fontFamily = montserratMedium, color = PrimaryColor),
        titleLarge = TextStyle(fontSize = 18.sp, fontWeight = FontWeight.SemiBold, fontFamily = latoSemiBold, color = PrimaryColor),
        titleSmall = TextStyle(fontSize = 12.sp, fontWeight = FontWeight.SemiBold, fontFamily = latoSemiBold, color = PrimaryColor),
        bodySmall = TextStyle(fontSize = 12.sp, fontFamily = latoRegular, color = PrimaryColor),
    )

    CompositionLocalProvider(
        LocalNunchukTypography provides nunchukTypography,
    ) {
        MaterialTheme(
            colors = LightColors,
            content = content,
        )
    }
}
