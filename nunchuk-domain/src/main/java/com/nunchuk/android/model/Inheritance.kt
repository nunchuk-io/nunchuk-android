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
import com.nunchuk.android.type.WalletType
import kotlinx.parcelize.Parcelize

@Parcelize
class Inheritance(
    val walletId: String,
    val walletLocalId: String,
    val magic: String,
    val note: String,
    val notificationEmails: List<String> = mutableListOf(),
    val status: InheritanceStatus,
    val activationTimeMilis: Long,
    val createdTimeMilis: Long,
    val lastModifiedTimeMilis: Long,
    val bufferPeriod: Period? = null,
    val ownerId: String,
    val pendingRequests: List<InheritancePendingRequest> = arrayListOf(),
    val walletType: WalletType = WalletType.MULTI_SIG,
    val notificationPreferences: InheritanceNotificationPreferences? = null,
    val inheritanceKeys: List<InheritanceKey> = mutableListOf(),
) : Parcelable

@Parcelize
class InheritancePendingRequest(
    val id: String,
    val membershipId: String,
    val dummyTransactionId: String,
) : Parcelable

@Parcelize
class InheritanceNotificationPreferences(
    val emailMeWalletConfig: Boolean = false,
    val beneficiaryNotifications: List<BeneficiaryNotification> = arrayListOf(),
) : Parcelable

@Parcelize
class BeneficiaryNotification(
    val email: String,
    val notifyTimelockExpires: Boolean = false,
    val notifyWalletChanges: Boolean = false,
    val includeWalletConfig: Boolean = false,
) : Parcelable

@Parcelize
class InheritanceKey(
    val xfp: String,
) : Parcelable