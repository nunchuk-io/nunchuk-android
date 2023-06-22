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

package com.nunchuk.android.wallet.components.review

import android.os.Parcelable
import com.nunchuk.android.model.SingleSigner
import kotlinx.parcelize.Parcelize

@Parcelize
internal data class ParcelizeSingleSigner(
    var name: String = "",
    var xpub: String = "",
    var publicKey: String = "",
    var derivationPath: String = "",
    var masterFingerprint: String = "",
    var lastHealthCheck: Long = 0L,
    var masterSignerId: String = "",
    var used: Boolean = false
) : Parcelable

internal fun List<SingleSigner>.parcelize() = map(SingleSigner::parcelize) as ArrayList<ParcelizeSingleSigner>

internal fun List<ParcelizeSingleSigner>.deparcelize() = map(ParcelizeSingleSigner::deparcelize)

internal fun SingleSigner.parcelize() = ParcelizeSingleSigner(
    name = name,
    xpub = xpub,
    publicKey = publicKey,
    derivationPath = derivationPath,
    masterFingerprint = masterFingerprint,
    lastHealthCheck = lastHealthCheck,
    masterSignerId = masterSignerId,
    used = used
)

internal fun ParcelizeSingleSigner.deparcelize() = SingleSigner(
    name = name,
    xpub = xpub,
    publicKey = publicKey,
    derivationPath = derivationPath,
    masterFingerprint = masterFingerprint,
    lastHealthCheck = lastHealthCheck,
    masterSignerId = masterSignerId,
    used = used
)
