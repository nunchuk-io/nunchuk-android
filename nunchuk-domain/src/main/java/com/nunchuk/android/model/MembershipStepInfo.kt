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

package com.nunchuk.android.model

import android.os.Parcelable
import com.google.gson.Gson
import com.nunchuk.android.type.SignerType
import kotlinx.parcelize.Parcelize

private val gson = Gson()

@Parcelize
data class TimelockExtra(
    val value: Long = 0L,
    val timezone: String? = null
) : Parcelable

data class MembershipStepInfo(
    val id: Long = 0,
    val step: MembershipStep,
    val masterSignerId: String = "",
    val keyIdInServer: String = "",
    val verifyType: VerifyType = VerifyType.NONE,
    val extraData: String = "",
    val plan: MembershipPlan = MembershipPlan.NONE,
    val groupId: String,
) {
    val isVerifyOrAddKey: Boolean
        get() = verifyType != VerifyType.NONE || masterSignerId.isNotEmpty()

    fun isInheritanceKeyRequireBackup(): Boolean {
        val signer = runCatching {
            gson.fromJson(
                extraData,
                SignerExtra::class.java
            )
        }.getOrNull()
        return step.isAddInheritanceKey && signer != null && signer.signerType != SignerType.NFC && (verifyType != VerifyType.NONE || signer.userKeyFileName.isNotEmpty())
    }

    fun isNFCKey(): Boolean {
        val signer = runCatching {
            gson.fromJson(
                extraData,
                SignerExtra::class.java
            )
        }.getOrNull()
        return signer?.signerType == SignerType.NFC
    }

    fun parseTimelockExtra(): TimelockExtra? {
        if (extraData.isBlank()) return null
        return runCatching {
            gson.fromJson(extraData, TimelockExtra::class.java)
        }.getOrNull()
    }
}

