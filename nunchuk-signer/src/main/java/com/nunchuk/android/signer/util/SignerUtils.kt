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

package com.nunchuk.android.signer.util

import androidx.annotation.StringRes
import com.nunchuk.android.signer.R
import com.nunchuk.android.type.SignerTag

fun isTestNetPath(path: String): Boolean {
    return path.split("/").getOrNull(2) == "1h"
}

/**
 * Heading for the air-gapped add-key screens. The screens after the key-type picker know which
 * device the user chose, so they name it; a generic air-gap add has no device to name.
 */
@StringRes
fun SignerTag?.airgapAddKeyTitleRes(): Int = when (this) {
    SignerTag.SEEDSIGNER -> R.string.nc_add_seedsigner
    SignerTag.JADE -> R.string.nc_add_jade
    SignerTag.PASSPORT -> R.string.nc_add_foundation_passport
    SignerTag.KEYSTONE -> R.string.nc_add_keystone
    else -> R.string.nc_add_an_airgapped_key
}
