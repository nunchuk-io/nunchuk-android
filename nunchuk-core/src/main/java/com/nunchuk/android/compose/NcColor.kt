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

import androidx.compose.material3.ColorScheme
import androidx.compose.ui.graphics.Color

object NcColor {
    val greyDark = Color(0xFF595959)
    val boulder = Color(0xFF757575)
    val greyLight = Color(0xFFF5F5F5)
    val white = Color(0xFFFFFFFF)
    val beeswaxLight = Color(0xFFFDD95C)
}

val ColorScheme.isDark: Boolean
    get() = primary == Color.Black

val ColorScheme.border: Color
    get() = if (isDark) Color(0xFF595959) else Color(0xFFDEDEDE)

val ColorScheme.greyLight: Color
    get() = if (isDark) Color(0xFF2C2C2C) else Color(0xFFF5F5F5)

val ColorScheme.lightGray: Color
    get() = greyLight

val ColorScheme.greyDark: Color
    get() = NcColor.greyDark


val ColorScheme.whisper: Color
    get() = if (isDark) Color(0xFF5b5b5b) else Color(0xFFEAEAEA)

val ColorScheme.everglade: Color
    get() = Color(0xFF1C4A21)

val ColorScheme.ming: Color
    get() = Color(0xFF2F766D)

val ColorScheme.fillBeewax: Color
    get() = if (isDark) Color(0xFFa66800) else Color(0xFFFDEBD2)

val ColorScheme.quickSilver: Color
    get() = Color(0xFFA6A6A6)

val ColorScheme.textPrimary: Color
    get() = if (isDark) Color.White else Color(0xFF031F2B)

val ColorScheme.textSecondary: Color
    get() = if (isDark) Color(0xFFA6A6A6) else Color(0xFF757575)

val ColorScheme.strokePrimary: Color
    get() = if (isDark) Color(0xFF595959) else Color(0xFFDEDEDE)

val ColorScheme.controlFillPrimary: Color
    get() = if (isDark) Color(0xFFFFFFFF) else Color(0xFF031F2B)

val ColorScheme.controlTextPrimary: Color
    get() = if (isDark) Color(0xFF031F2B) else Color(0xFFFFFFFF)

val ColorScheme.fillDenim: Color
    get() = if (isDark) Color(0xFF2f466c) else Color(0xFFd0e2ff)

val ColorScheme.fillBeeswax: Color
    get() = if (isDark) Color(0xFFa66800) else Color(0xFFFDEBD2)

val ColorScheme.controlActivated: Color
    get() = if (isDark) Color(0xFFffcb2e) else Color(0xFF031f2b)

val ColorScheme.controlDefault: Color
    get() = if (isDark) Color(0xFFA6A6A6) else Color(0xFF595959)

val ColorScheme.controlFillSecondary: Color
    get() = if (isDark) Color(0xFF393939) else Color(0xFF032b3c)

val ColorScheme.fillInputText: Color
    get() = if (isDark) Color(0x4D000000) else Color(0xFFFFFFFF)

val ColorScheme.backgroundMidGray: Color
    get() = if (isDark) Color(0xFF393939) else Color(0xFFEAEAEA)

val ColorScheme.backgroundLightGray: Color
    get() = greyLight


