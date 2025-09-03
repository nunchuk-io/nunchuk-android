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

package com.nunchuk.android.core.util

import android.os.Bundle

fun Bundle?.getStringValue(
    key: String,
    defaultValue: String = ""
) = this?.getString(key, defaultValue).orEmpty()

fun Bundle?.getDoubleValue(
    key: String,
    defaultValue: Double = 0.0
) = this?.getDouble(key, defaultValue) ?: defaultValue

fun Bundle?.getBooleanValue(
    key: String,
    defaultValue: Boolean = true
) = this?.getBoolean(key, defaultValue) ?: defaultValue

fun Bundle?.getLongValue(
    key: String,
    defaultValue: Long = 0L
) = this?.getLong(key, defaultValue) ?: defaultValue

fun Bundle?.getIntValue(
    key: String,
    defaultValue: Int = 0
) = this?.getInt(key, defaultValue) ?: defaultValue


const val ADD_WALLET_RESULT = "ADD_WALLET_RESULT"
const val ADD_WALLET_REUSE_SIGNER_RESULT = "ADD_WALLET_REUSE_SIGNER_RESULT"