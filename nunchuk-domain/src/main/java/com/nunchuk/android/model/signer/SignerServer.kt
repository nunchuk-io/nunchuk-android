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

package com.nunchuk.android.model.signer

import android.os.Parcelable
import com.nunchuk.android.model.TapSigner
import com.nunchuk.android.model.VerifyType
import com.nunchuk.android.type.SignerType
import kotlinx.parcelize.Parcelize

@Parcelize
data class SignerServer(
    val name: String? = null,
    val xfp: String? = null,
    val derivationPath: String? = null,
    val type: SignerType = SignerType.UNKNOWN,
    val verifyType: VerifyType = VerifyType.NONE,
    val userKeyId: String? = null,
    val index: Int = 0,
    val isVisible: Boolean = true,
    val tapsigner: TapSigner? = null,
    val xpub: String? = null,
    val pubkey: String? = null,
    val tags: List<String> = emptyList(),
    val userBackUpFileName: String? = null,
) : Parcelable